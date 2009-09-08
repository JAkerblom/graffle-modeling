package org.epistem.diagram.ontology;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.epistem.diagram.model.Diagram;
import org.epistem.diagram.model.DiagramVisitor;
import org.epistem.diagram.model.Graphic;
import org.epistem.diagram.model.Group;
import org.epistem.diagram.model.Page;
import org.epistem.diagram.model.Shape;
import org.epistem.diagram.model.Table;
import org.epistem.graffle.OmniGraffleDoc;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.DefaultOntologyFormat;
import org.semanticweb.owl.io.OWLOntologyOutputTarget;
import org.semanticweb.owl.io.StreamOutputTarget;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;

/**
 * Reads a diagram and emits an OWL2 ontology
 *
 * @author nickmain
 */
public class OntologyEmitter {

    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();;
    private final OWLDataFactory     factory = manager.getOWLDataFactory();;

    private final Diagram diagram;
    private URI uri;
    private OWLOntology ontology;
    
    private final Map<Graphic,OWLDescription> descCache = new HashMap<Graphic, OWLDescription>();
    private final Map<String,OWLClass>        classCache = new HashMap<String, OWLClass>();
    private final Map<Graphic,OWLIndividual>  individualCache = new HashMap<Graphic, OWLIndividual>();
    private final Map<String,OWLIndividual>   individualNameCache = new HashMap<String, OWLIndividual>();
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
        
        diagram.accept( new OntoDefVisitor() ); //find ontology definition
        if( ontology == null ) throw new RuntimeException( "No ontology definition found" );
        
        discoverEntities();
        
        cleanupIndividuals();
    }
    
    /**
     * Discover all the entities in the diagram
     */
    private void discoverEntities() throws Exception {
        //TODO
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
    
    private OWLIndividual getOWLIndividual( String name ) throws OWLOntologyChangeException {
        OWLIndividual i = individualNameCache.get( name );
        if( i == null ) {
            i = factory.getOWLIndividual( URI.create( uri + "#" + name ) );
            declare( i );
            individualNameCache.put( name, i );
        }
        
        return i;
    }
    
    
    private OWLClass getOWLClass( String name ) throws OWLOntologyChangeException {
        OWLClass c = classCache.get( name );
        if( c == null ) {
            c = factory.getOWLClass( URI.create( uri + "#" + name ) );
            declare( c );
            classCache.put( name, c );
        }
        
        return c;
    }
    
    private void declare( OWLEntity entity ) throws OWLOntologyChangeException {
        OWLDeclarationAxiom ax = factory.getOWLDeclarationAxiom( entity );
        addAxiom( ax );
    }
    
    private void addAxiom( OWLAxiom axiom ) throws OWLOntologyChangeException {
        manager.addAxiom( ontology, axiom );
    }
    
    private String makeName( Graphic g, String type ) {
        if( ! (g instanceof Shape) ) throw new RuntimeException( type + " graphic must be a Shape with text in sheet " + g.page.title );
        String text = ((Shape) g).text;
        
        if( text == null ) throw new RuntimeException( type + " name missing in sheet " + g.page.title );
        StringBuilder buff = new StringBuilder();
        String[] words = text.split( "\\s" );
        if( words.length == 0 ) throw new RuntimeException( "Blank " + type + " name in sheet " + g.page.title );
        
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
     * TEST
     */
    public static void main(String[] args) {
		new OntologyEmitter( new File( "test-diagrams/test-owl.graffle" )).write();
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
					    uri =  URI.create( group.text );
						ontology = manager.createOntology( uri );
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
			// TODO Auto-generated method stub
			return null;
		}
    }
}
