package org.epistem.graffle.rdf;

import static org.epistem.graffle.rdf.GraffleURI.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.epistem.graffle.OGGraphic;
import org.epistem.graffle.OGSheet;
import org.epistem.graffle.OmniGraffleDoc;
import org.epistem.rdf.util.JenaToGraphviz;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.*;

/**
 * Utility to create RDF from an OG document
 *
 * @author nickmain
 */
public class GraffleToRDF {

    private String documentPrefix;    
    
    /**
     * The RDF model being updated
     */
    public final Model model;
    
    private final OmniGraffleDoc doc;
    private Resource ogEnd;
    
    /**
     * Use an existing model
     */
    public GraffleToRDF( OmniGraffleDoc doc, Model model ) {
        this.doc   = doc;
        this.model = model;
       
        translate();
    }

    /**
     * Create a new model
     */
    public GraffleToRDF( OmniGraffleDoc doc ) {
        this( doc, ModelFactory.createDefaultModel() );
    }
    
    /**
     * Translate the OG document
     */
    private void translate() {

        String subject = doc.subject();
        if( subject.startsWith( "http://" ) ) {
            documentPrefix = subject;
        }
        else {
            documentPrefix = URIPrefix.epi + doc.file().getName() + "#";
        }
        
        model.setNsPrefix( "doc", documentPrefix );
        
        for( URIPrefix prefix : URIPrefix.values() ) {
            model.setNsPrefix( prefix.name(), prefix.prefix );            
        }
        
        Resource docRes = model.createResource( og_document.uri );
        
        try {
            model.add( docRes, prop( og_fileName ), doc.file().getName() );
            model.add( docRes, prop( og_filePath ), doc.file().getParentFile().getCanonicalPath() );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }

        model.add( docRes, prop( og_createdDate  ), doc.creationDate() );
        model.add( docRes, prop( og_creator      ), doc.creator() );
        model.add( docRes, prop( og_modifiedDate ), doc.modificationDate() );
        model.add( docRes, prop( og_modifier     ), doc.modifier() );
        model.add( docRes, prop( og_copyright    ), doc.copyright() );
        model.add( docRes, prop( og_description  ), doc.description() );
        model.add( docRes, prop( og_subject      ), doc.subject() );
        model.add( docRes, prop( og_version      ), doc.version() );
        model.add( docRes, prop( og_comments     ), doc.comments() );

        for( String s : doc.authors()       ) model.add( docRes, prop( og_author       ), s );
        for( String s : doc.keywords()      ) model.add( docRes, prop( og_keyword      ), s );
        for( String s : doc.languages()     ) model.add( docRes, prop( og_language     ), s );
        for( String s : doc.organizations() ) model.add( docRes, prop( og_organization ), s );
        for( String s : doc.projects()      ) model.add( docRes, prop( og_project      ), s );

        //load prefixes from the keywords
        for( String s : doc.keywords()) {
            if( s.contains( ":" ) ) {
                String prefix = s.substring( 0, s.indexOf( ":" ) ).trim();
                String uri    = s.substring( s.indexOf( ":" ) + 1 ).trim();
                model.setNsPrefix( prefix, uri );
            }
        }
        
        for( OGSheet sheet : doc.sheets() ) {
            model.add( docRes, prop( og_sheet ), translate( sheet ) );
        }
    }

    private Resource translate( OGSheet sheet ) {
        Resource res = model.createResource();
        wireUp( res, sheet.title(), sheet.notes() );
        
        for( OGGraphic graphic : sheet.graphics() ) {
            model.add( res, prop( og_child ), translate( graphic ) );
        }
        
        return res;
    }
    
    private void wireUp( Resource shape, String text, String note ) {
        String xsdType = null;
        
        note = unWS( note );
        if( note != null && note.length() > 0 ) {
            if( note.startsWith( "xsd:" ) ) {
                xsdType = note.substring( 4 );
            }
            else {
                model.add( shape, prop( og_note ), res( note ) );
            }
        }
        
        if( text != null && text.length() > 0 ) {
            model.add( shape, prop( og_text ), text );

            text = unWS( text );
            if( text != null && text.length() > 0 ) {
                if( xsdType != null ) {
                    Literal lit = model.createTypedLiteral( text, new XSDDatatype( xsdType ) );
                    model.add( shape, prop( og_value ), lit );
                }
                else {
                    model.add( shape, prop( og_item ), res( text ) );
                }
            }
        }
    }

    private Resource res( String s ) {
        
        int colon = s.indexOf( ":" );
        if( colon > 0 ) {
            String prefix = s.substring( 0, colon ).toLowerCase();
            String rest   = s.substring( colon + 1 );
            
            String uri = model.getNsPrefixURI( prefix );
            
            return model.createResource( uri + rest );
        }
        
        return model.createResource( documentPrefix + s );
    }
    
    private String unWS( String s ) {
        if( s == null ) return null;
        s = s.trim();
        
        StringBuilder buff = new StringBuilder();
        boolean isWordStart = false;
        
        int len = s.length();
        for( int i = 0; i < len; i++ ) {
            char c = s.charAt( i );
            
            if( Character.isWhitespace( c ) ) {
                isWordStart = true;
                continue;
            }
            
            if( isWordStart ) {
                buff.append( "-" );
                isWordStart = false;
            }
            
            buff.append( c );
        }
        
        return buff.toString();
    }
    
    private Resource translate( OGGraphic graphic ) {
        Resource res = graphic( graphic.id() );
        wireUp( res, graphic.text(), graphic.notes() );

        int head = graphic.headId();
        int tail = graphic.tailId();
        
        if( head > 0 ) model.add( res, prop( og_head ), graphic( head ) );
        if( tail > 0 ) model.add( res, prop( og_tail ), graphic( tail ) );
        
        int label = graphic.labelLineId();
        if( label > 0 ) model.add( graphic( label ), prop( og_label ), res );
        
        switch( graphic.graphicClass() ) {
            case Group:
                List<OGGraphic> kids = graphic.graphics();
                
                if( graphic.isSubgraph() ) {
                    OGGraphic bg = kids.remove( kids.size() - 1 );
                    model.add( res, prop( og_background ), translate( bg ) );
                }
                
                for( OGGraphic g : kids ) {
                    model.add( res, prop( og_child ), translate( g ) );
                }
                
                break;
                
            case TableGroup:
                for( OGGraphic cell : graphic.graphics() ) {
                    model.add( res, prop( og_cell ), translate( cell ) );
                }
                
                OGGraphic[][] table = graphic.table();
                int rowSize  = table.length;
                int rowCount = table[0].length;
                
                model.add( res, prop( og_rowCount ), ""+rowCount, XSDDatatype.XSDinteger );                        
                model.add( res, prop( og_colCount ), ""+rowSize, XSDDatatype.XSDinteger );                        
                
                Resource prevRow = null;
                for( int y = 0; y < rowCount; y++ ) {
                    Resource row = model.createResource();
                    
                    if( prevRow != null ) {
                        model.add( prevRow, prop( og_nextRow ), row );
                    }
                    else {
                        model.add( res, prop( og_firstRow ), row );                        
                    }
                    
                    Resource prevCell = null;
                    for( int x = 0; x < rowSize; x++ ) {
                        Resource cell = translate( table[x][y] );
                        
                        if( prevCell != null ) {
                            model.add( prevCell, prop( og_right ), cell );
                        }
                        else {
                            model.add( row, prop( og_firstRight ), cell );                            
                        }
                        
                        model.add( cell, prop( og_row ), row );
                        
                        prevCell = cell;
                    }
                    model.add( prevCell, prop( og_right ), ogEnd() );
                    
                    prevRow = row;
                }
                model.add( prevRow, prop( og_nextRow ), ogEnd() );

                Resource prevCol = null;
                for( int x = 0; x < rowSize; x++ ) {
                    Resource col = model.createResource();
                    
                    if( prevCol != null ) {
                        model.add( prevCol, prop( og_nextCol ), col );
                    }
                    else {
                        model.add( res, prop( og_firstCol ), col );                        
                    }

                    Resource prevCell = null;
                    for( int y = 0; y < rowCount; y++ ) {
                        Resource cell = graphic( table[x][y].id() );
                        
                        if( prevCell != null ) {
                            model.add( prevCell, prop( og_down ), cell );
                        }
                        else {
                            model.add( col, prop( og_firstDown ), cell );                            
                        }
                        
                        model.add( cell, prop( og_col ), col );
                        
                        prevCell = cell;
                    }
                    model.add( prevCell, prop( og_down ), ogEnd() );
                    
                    prevCol = col;
                }
                model.add( prevCol, prop( og_nextCol ), ogEnd() );
                
                break;            
        }
        
        return res;        
    }
    
    private Resource graphic( int id ) {
        return model.createResource( documentPrefix + "graphic-" + id );
    }
    
    /**
     * Create a property with the given uri
     */
    private Property prop( GraffleURI uri ) {
        return model.createProperty( uri.uri );
    }
        
    private Resource ogEnd() {
        if( ogEnd == null ) {
            ogEnd = model.createResource( og_end.uri );
        }
        
        return ogEnd;
    }
    
    /**
     * Read an OG file and write an RDF file
     * @param graffleFile the OG filename
     * @param rdfFile the RDF filename. If suffix is n3 then write N3 otherwise RDF/XML
     */
    public static void translate( String graffleFile, String rdfFile ) throws Exception {
        OmniGraffleDoc doc = new OmniGraffleDoc( new File( graffleFile ) );
        GraffleToRDF trans = new GraffleToRDF( doc );

   //     new JenaToGraphviz( trans.model ).write( "/Users/nickmain/Desktop/test.dot" );

        if( rdfFile == null ) {
            trans.model.write( System.out, "N3" );
            System.out.flush();
            return;
        }
            
        String lang = rdfFile.endsWith( ".n3" ) ? "N3" : "RDF/XML"; 
        FileWriter out = new FileWriter( rdfFile );
        trans.model.write( out, lang );
        
        out.close();
    }
    
    public static void main( String[] args ) throws Exception {
        translate( "test-diagrams/test.graffle", null );
    }
}
