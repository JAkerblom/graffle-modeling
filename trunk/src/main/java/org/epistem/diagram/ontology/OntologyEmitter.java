package org.epistem.diagram.ontology;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.epistem.diagram.model.*;
import org.epistem.graffle.OmniGraffleDoc;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.DefaultOntologyFormat;
import org.semanticweb.owl.io.OWLOntologyOutputTarget;
import org.semanticweb.owl.io.StreamOutputTarget;
import org.semanticweb.owl.model.*;

/**
 * Reads a diagram and emits an OWL2 ontology
 *
 * @author nickmain
 */
public class OntologyEmitter {
    private static final String XSD_PREFIX = "xsd:";
    private static final String XSD_URI    = "http://www.w3.org/2001/XMLSchema#";
    
    /**
     * The graphic notes used to indicate ontology elements
     */
    private static enum OntoNote {
        Ontology, Imports, Individual, Class;
     
        /** Whether this note matches a graphic */
        public boolean matches( Graphic g ) {
            if( g == null || g.metadata.notes == null ) return false;
            return name().equalsIgnoreCase( g.metadata.notes.trim() );
        }
    }
    
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory     factory = manager.getOWLDataFactory();

    private final Diagram diagram;
    private URI ontologyURI;
    private OWLOntology ontology;
    
    private final Map<URI,OWLClass>      classCache      = new HashMap<URI, OWLClass>();
    private final Map<URI,OWLIndividual> individualCache = new HashMap<URI, OWLIndividual>();
    private final Map<String,String>     uriPrefixes     = new HashMap<String, String>();

    private final Map<Shape,OWLClass>    shapeClassCache = new HashMap<Shape, OWLClass>();

    
    private final Map<Graphic,OWLTypedConstant> constants = new HashMap<Graphic, OWLTypedConstant>();
    private final Map<String,Collection<Graphic>> noteGraphics = new HashMap<String, Collection<Graphic>>();
    private final Map<Graphic,OWLEntity> entities = new HashMap<Graphic, OWLEntity>();
    
    /**
     * @param omnigraffleFile the OG document to read
     */
    public OntologyEmitter( File omnigraffleFile ) {
        try {
            this.diagram = new Diagram( new OmniGraffleDoc( omnigraffleFile ) );
            generateOntology();
        }
        catch( RuntimeException e ) {
            throw e;
        }
        catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    /**
     * Write the ontology to the same dir and the diagram
     */
    public void write() {
        write( new File( diagram.file.getParentFile(), diagram.file.getName() + ".owl" ) );
    }
    
    /**
     * Write the ontology 
     * @param owlFile the target file
     */
    public void write( File owlFile ) {
        FileOutputStream out;
        try {
            out = new FileOutputStream( owlFile );
            OWLOntologyOutputTarget target = new StreamOutputTarget( out );
            manager.saveOntology( ontology, new DefaultOntologyFormat(), target );
            out.close();
        } 
        catch( RuntimeException e ) {
            throw e;
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }
    
    private void generateOntology() throws Exception {
    	
        initNamespaces();
        
        //find the ontology declaration and the namespaces
        diagram.accept( new OntologyFinder() ); 
        if( ontology == null ) throw new RuntimeException( "No ontology definition found" );

        //process all the graphics
        diagram.accept( new Visitor() ); 

        cleanupIndividuals();
    }
    
    /**
     * Set up the initial namespaces
     */
    private void initNamespaces() {
    	uriPrefixes.put( "rdfs"   , "http://www.w3.org/2000/01/rdf-schema#" );
    	uriPrefixes.put( "owl"    , "http://www.w3.org/2002/07/owl#" );
    	uriPrefixes.put( "rdf"    , "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
    	uriPrefixes.put( "dc"     , "http://purl.org/dc/elements/1.1/" );
    	uriPrefixes.put( "owl2xml", "http://www.w3.org/2006/12/owl2-xml#" );
    	uriPrefixes.put( "xsd"    , "http://www.w3.org/2001/XMLSchema#" );
    }
    
    /**
     * Make sure all individuals have a type (otherwise Protege will ignore)
     */
    private void cleanupIndividuals() {
        for( OWLIndividual ind : individualCache.values() ) {
            if( ind.getTypes( ontology ).isEmpty() ) {
                //System.out.println( "Untyped individual " + ind.getURI() );
                addAxiom( factory.getOWLClassAssertionAxiom( ind, factory.getOWLThing() ) );
            } 
        }
    }
    
    private OWLIndividual getOWLIndividual( URI name ) {
        OWLIndividual i = individualCache.get( name );
        if( i == null ) {
            i = factory.getOWLIndividual( name );
            declare( i );

            individualCache.put( name, i );
        }
        
        return i;
    }
    
    private OWLClass getOWLClass( Shape s ) {
        OWLClass cls = shapeClassCache.get( s );
        if( cls == null ) {            
            URI uri = uriFromString( makeName( s, "Class" ));  
            if( uri == null ) return null;
            
            cls = getOWLClass( uri );
            shapeClassCache.put( s, cls );
        }
        
        return cls;
    }
    
    private OWLClass getOWLClass( URI classURI ) {
        OWLClass c = classCache.get( classURI );
        if( c == null ) {
            c = factory.getOWLClass( classURI );
            declare( c );

            classCache.put( classURI, c );
        }
        
        return c;
    }
    
    private void declare( OWLEntity entity ) {
        OWLDeclarationAxiom ax = factory.getOWLDeclarationAxiom( entity );
        addAxiom( ax );
    }
    
    private void addAxiom( OWLAxiom axiom ) {
        try {
            manager.addAxiom( ontology, axiom );
        } 
        catch( OWLOntologyChangeException e ) {
            throw new RuntimeException( e );
        }
    }
    
    private String makeName( Shape s, String type ) {
        String text = s.text;
        
        if( text == null ) return null;
        StringBuilder buff = new StringBuilder();
        String[] words = text.split( "\\s" );
        if( words.length == 0 ) return null;
        
        for( String word : words ) {
            if( ! Character.isUpperCase( word.charAt( 0 )) ) {
                if( buff.length() > 0 ) {
                    buff.append( "-" );
                }                    
            }
            
            buff.append( word );
        }
        
        return buff.toString();
    }
    
    private boolean isEmpty( String s ) {
        if( s == null ) return true;
        return s.trim().length() == 0;
    }
    
    /**
     * Get a URI from a string that may be absolute 
     */
    private URI uriFromString( String s ) {
        if( s == null ) return null;
        s = s.trim();
        if( s.length() == 0 ) return null;
        
        if( s.startsWith( "http://" ) ) return URI.create( s );
        int colon = s.indexOf( ":" );
        
        if( colon >= 0 ) {
            String prefix = s.substring( 0, colon );
            String name   = s.substring( colon + 1 );
            String nspace = uriPrefixes.get( prefix );
            
            if( nspace == null ) throw new RuntimeException( "Unknown prefix: " + prefix + " in " + s );
            return URI.create( nspace + name );
        }
        else {
            return URI.create( ontologyURI.toString() + "#" + s );
        }
    }
    
    /**
     * Whether the given name is in the local namespace
     */
    private boolean isLocalName( String s ) {
        if( s.startsWith( "http://" ) ) return false;
        if( s.indexOf( ":" ) > 0 ) return false;
        return true;
    }

    /**
     * Whether a shape is a literal
     */
    private boolean isLiteral( Shape shape ) {
        return shape.metadata.notes != null && shape.metadata.notes.startsWith( XSD_PREFIX );
    }
    
    /**
     * Get the literal from a shape - as a string if not typed
     */
    private OWLConstant getLiteral( Shape s ) {
        String text = s.text;
        if( text == null ) text = "";
        else text = text.trim();

        String note = s.metadata.notes;
        
        if( note != null ) {
            note = note.trim();
            
            if( note.startsWith( XSD_PREFIX ) ) {
                note = note.substring( XSD_PREFIX.length() );
                
                OWLDataType type = factory.getOWLDataType( URI.create( XSD_URI + note ));
                return factory.getOWLTypedConstant( text, type );
            }
        }
                
        return factory.getOWLTypedConstant( text );
    }
    
    /**
     * TEST
     */
    public static void main(String[] args) {
		new OntologyEmitter( new File( "test-diagrams/test-owl.graffle" )).write();
	}

    /**
     * Diagram visitor that finds the ontology declaration
     */    
    private class OntologyFinder extends DiagramVisitor.ShallowImpl {

        @Override
        public DiagramVisitor visitGroupStart( Group group ) {
            if( OntoNote.Ontology.matches( group ) ) {
                ontologyURI = URI.create( group.text.trim() );
                try {
                    ontology = manager.createOntology( ontologyURI );
                }
                catch( OWLOntologyCreationException e ) {
                    throw new RuntimeException( e );
                }
                
                return this;
            }
            
            return null;
        }

        @Override
        public DiagramVisitor visitTableStart( Table table ) {
            //ontology annotations
            if( table.parent != null && table.parent instanceof Group ) {
                for( Shape[] row : table.table ) {
                    URI         uri   = uriFromString( row[0].text.trim());
                    OWLConstant value = getLiteral( row[1] );
                    
                    OWLConstantAnnotation annot = factory.getOWLConstantAnnotation( uri, value );
                    addAxiom( factory.getOWLOntologyAnnotationAxiom( ontology, annot ));
                }
                
                return null;
            }
            
            //imports
            if( OntoNote.Imports.matches( table ) ) {
                for( Shape[] row : table.table ) {
                    String prefix = row[0].text.trim();
                    URI uri = uriFromString( row[1].text.trim());
                    
                    uriPrefixes.put( prefix, uri.toString() );
                    
                    addAxiom( factory.getOWLImportsDeclarationAxiom( ontology, uri ));
                }
            }
            
            return null;
        }
    }
    
    /**
     * Diagram visitor that gathers graphics by note - also creates all
     * literals
     */
    private class Visitor implements DiagramVisitor {

        @Override
        public void visitConnectorShape( ConnectorShape shape ) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void visitDiagramEnd( Diagram diagram ) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DiagramVisitor visitDiagramStart( Diagram diagram ) {
            // TODO Auto-generated method stub
            return this;
        }

        @Override
        public void visitGroupEnd( Group group ) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DiagramVisitor visitGroupStart( Group group ) {
            // TODO Auto-generated method stub
            return this;
        }

        @Override
        public void visitLineEnd( Line line ) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DiagramVisitor visitLineStart( Line line ) {
            // TODO Auto-generated method stub
            return this;
        }

        @Override
        public void visitPageEnd( Page page ) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DiagramVisitor visitPageStart( Page page ) {
            // TODO Auto-generated method stub
            return this;
        }

        @Override
        public void visitShape( Shape shape ) {
            if( OntoNote.Class.matches( shape ) ) {
                getOWLClass( shape );
                return;
            }
            
        }

        @Override
        public void visitTableEnd( Table table ) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DiagramVisitor visitTableStart( Table table ) {
            // TODO Auto-generated method stub
            return this;
        }
        
    }
}
