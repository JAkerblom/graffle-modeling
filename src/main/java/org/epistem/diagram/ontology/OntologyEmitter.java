package org.epistem.diagram.ontology;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
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
    private static final String SELF       = "*SELF*";
    
    /**
     * The different roles that graphics can have
     */
    private static enum Role {
        RoleClass, RoleIndividual, RoleProperty, RoleObjectProperty, RoleDataProperty,
        RoleDatatype, RoleDataRange, RoleClassExpression, RoleLiteral, RoleDataPropList,
        RoleCardinality, RoleSelf, RoleLiteralTable, RoleIndividualTable,
        RoleAnnotationList, RoleComment, RoleDescription,
        RoleRelationship, RoleOther
    }
    
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();;
    private final OWLDataFactory     factory = manager.getOWLDataFactory();;

    private final Diagram diagram;
    private URI ontologyURI;
    private OWLOntology ontology;
    
    private final Map<Graphic,OWLDescription> descCache = new HashMap<Graphic, OWLDescription>();
    private final Map<URI,OWLClass>           classCache = new HashMap<URI, OWLClass>();
    private final Map<Graphic,OWLIndividual>  individualCache = new HashMap<Graphic, OWLIndividual>();
    private final Map<URI,OWLIndividual>      individualNameCache = new HashMap<URI, OWLIndividual>();
    private final Map<String,String>          uriPrefixes = new HashMap<String, String>();
    
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
        
        //TODO: find namespaces
        
        diagram.accept( new OntoDefVisitor() ); //find ontology definition
        if( ontology == null ) throw new RuntimeException( "No ontology definition found" );
        
        diagram.accept( new EntityVisitor() ); //discover entities
        
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
     * @throws OWLOntologyChangeException 
     */
    private void cleanupIndividuals() throws OWLOntologyChangeException {
        for( OWLIndividual ind : individualCache.values() ) {
            if( ind.getTypes( ontology ).isEmpty() ) {
                //System.out.println( "Untyped individual " + ind.getURI() );
                addAxiom( factory.getOWLClassAssertionAxiom( ind, factory.getOWLThing() ) );
            } 
        }
    }
    
    private OWLIndividual getOWLIndividual( URI name ) {
        OWLIndividual i = individualNameCache.get( name );
        if( i == null ) {
            i = factory.getOWLIndividual( name );
            try {
                declare( i );
            }
            catch( OWLOntologyChangeException e ) {
                throw new RuntimeException( e );
            }

            individualNameCache.put( name, i );
        }
        
        return i;
    }
    
    
    private OWLClass getOWLClass( URI classURI ) {
        OWLClass c = classCache.get( classURI );
        if( c == null ) {
            c = factory.getOWLClass( classURI );
            try {
                declare( c );
            }
            catch( OWLOntologyChangeException e ) {
                throw new RuntimeException( e );
            }

            classCache.put( classURI, c );
        }
        
        return c;
    }
    
    private void declare( OWLEntity entity ) throws OWLOntologyChangeException {
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
     * Register a class shape
     */
    private void registerClass( Shape shape ) {
        String name = makeName( shape, "Class" );
        if( name == null ) return; //a class expression

        if( ! isLocalName( name ) ) return; //imported class
        
        URI classURI = uriFromString( name );
        descCache.put( shape, getOWLClass( classURI ));
    }
    
    /**
     * Register an individual
     */
    private void registerIndividual( Shape shape ) {
        String name = makeName( shape, "Individual" );
        if( name == null ) return;

        if( ! isLocalName( name ) ) return; //imported
        
        URI indURI = uriFromString( name );
        individualCache.put( shape, getOWLIndividual( indURI ));
    }    
    
    /**
     * Whether a shape is a literal
     */
    private boolean isLiteral( Shape shape ) {
        return shape.metadata.notes != null && shape.metadata.notes.startsWith( XSD_PREFIX );
    }

    /**
     * Determine the role a graphic plays in defining the ontology
     */
    private Role determineRole( Graphic g ) {
        if( g instanceof Line  ) return Role.RoleRelationship;        
        if( g instanceof Shape ) return determineShapeRole( (Shape) g );
        if( g instanceof Table ) return determineTableRole( (Table) g );
        return Role.RoleOther;
    }
    
    private Role determineShapeRole( Shape s ) {
        if( s.parent != null && s.parent instanceof Line ) {
            
        }
        
        switch( s.type ) {
            case Circle:
                if( isEmpty( s.text ) ) return Role.RoleClassExpression;
                return Role.RoleClass;
            
            case Rectangle: 
                if( isLiteral( s ) ) return Role.RoleLiteral;
                if( SELF.equalsIgnoreCase( s.text )) return Role.RoleSelf;
                if( s.incoming.isEmpty() && s.outgoing.isEmpty() ) return Role.RoleOther;
                return Role.RoleIndividual;
                
            case RoundRect: 
                if( s.isSolid ) return Role.RoleDatatype;
                return Role.RoleDataRange;
                
            case Other:     break;
        }
        
        return Role.RoleOther;
    }
    
    private Role determineTableRole( Table t ) {
        //TODO
        
        return Role.RoleOther;
    }
    
    /**
     * TEST
     */
    public static void main(String[] args) {
		new OntologyEmitter( new File( "test-diagrams/test-owl.graffle" )).write();
	}

    /**
     * Visitor to discover entities
     */
    private class EntityVisitor extends DiagramVisitor.ShallowImpl {

        @Override
        public void visitShape( Shape shape ) {
            
            switch( determineShapeRole( shape )) {
                case RoleClass:      registerClass( shape ); break;
                case RoleIndividual: registerIndividual( shape ); break;
            }
            
        }

        @Override
        public DiagramVisitor visitGroupStart( Group group ) {
            // TODO - find individuals
            return super.visitGroupStart( group );
        }

        @Override
        public DiagramVisitor visitTableStart( Table table ) {
            // TODO - find property grids
            return super.visitTableStart( table );
        }
    }
    
    /**
     * Visitor to discover the ontology definition
     */
    private class OntoDefVisitor extends DiagramVisitor.ShallowImpl {

		@Override
		public DiagramVisitor visitGroupStart(Group group) {
			if( group.text != null ) {
				if( group.text.startsWith( "http://" ) ) {
					if( ontology != null ) throw new RuntimeException( "More than one ontology definition" );					
					try {
					    ontologyURI = URI.create( group.text );
						ontology = manager.createOntology( ontologyURI );
					} catch( Exception e ) {
						throw new RuntimeException( e );
					}					
					return this;
				}				
			}
			
			return null;
		}

		@Override
		public DiagramVisitor visitTableStart(Table table) {
			// This is a table inside a ontology declaration
		    
		    if( table.colCount() != 2 ) {
		        throw new RuntimeException( 
		            "Annotation table in an ontology declaration must have 2 columns" );
		    }
		 
		    for( Shape[] row : table.table ) {
		        String annot = row[0].text.trim();
		        String value = row[1].text;
		        
		        URI annoURI = uriFromString( annot );
		        OWLConstant anVal = factory.getOWLTypedConstant( value );
		        OWLConstantAnnotation annotation = factory.getOWLConstantAnnotation( annoURI, anVal );
		        OWLOntologyAnnotationAxiom axiom = factory.getOWLOntologyAnnotationAxiom( ontology, annotation );
		        addAxiom( axiom );
		    }
		    
			return null;
		}
    }
}
