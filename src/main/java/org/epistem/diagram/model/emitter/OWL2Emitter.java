package org.epistem.diagram.model.emitter;

import java.io.FileOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.epistem.diagram.model.Connector;
import org.epistem.diagram.model.Graphic;
import org.epistem.diagram.model.Line;
import org.epistem.diagram.model.Shape;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.OWLOntologyOutputTarget;
import org.semanticweb.owl.io.StreamOutputTarget;
import org.semanticweb.owl.model.*;

/**
 * Emitter for OWL2 ontologies
 *
 * @author nickmain
 */
public class OWL2Emitter extends ModelEmitter {

    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();;
    private final OWLDataFactory     factory = manager.getOWLDataFactory();;

    private final Map<Graphic,OWLDescription> descCache = new HashMap<Graphic, OWLDescription>();
    private final Map<String,OWLClass>  classCache = new HashMap<String, OWLClass>();

    private URI uri;
    private OWLOntology ontology;
    
    @Override
    public void generate( String defaultNamespace, Map<String, String> namespaces ) {
        uri = URI.create( defaultNamespace );
        
        try {
            ontology = manager.createOntology( uri );            
        } 
        catch( OWLOntologyCreationException e ) {
            throw new RuntimeException( e );
        }

        super.generate( defaultNamespace, namespaces );
    }

    @Override
    public void write() {
        FileOutputStream out;
        try {
            out = new FileOutputStream( outputFile );
            OWLOntologyOutputTarget target = new StreamOutputTarget( out );
            manager.saveOntology( ontology, target );            
            out.close();
        } 
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Handler("ObjectComplement")
    public void objectComplement() {

        boolean hasTarget = false;
        
        for( Connector conn : graphic.outgoing ) {
            if( ! isEmpty( ((Line) conn).metadata.notes )) continue;
            OWLDescription desc = classForGraphic( premptGraphic( notNullShape( conn.getHead() )));
            descCache.put( graphic, factory.getOWLObjectComplementOf( desc ));
            hasTarget = true;
            break;
        }
        
        if( ! hasTarget ) throw new RuntimeException( "Object complement requires a target class" );
    }

    @Handler("ObjectIntersection")
    public void objectIntersection() {

        Set<OWLDescription> classes = new HashSet<OWLDescription>();
        
        for( Connector conn : graphic.outgoing ) {
            if( ! isEmpty( ((Line) conn).metadata.notes )) continue;
            classes.add( classForGraphic( premptGraphic( notNullShape( conn.getHead() ))));
        }

        if( classes.size() < 2 ) throw new RuntimeException( "Object intersection requires 2 or more target classes" );

        descCache.put( graphic, factory.getOWLObjectIntersectionOf( classes ) );
    }
    
    @Handler("ObjectUnion")
    public void objectUnion() {
        Set<OWLDescription> classes = new HashSet<OWLDescription>();
        
        for( Connector conn : graphic.outgoing ) {
            if( ! isEmpty( ((Line) conn).metadata.notes )) continue;
            classes.add( classForGraphic( premptGraphic( notNullShape( conn.getHead() ))));
        }

        if( classes.size() < 2 ) throw new RuntimeException( "Object union requires 2 or more target classes" );

        descCache.put( graphic, factory.getOWLObjectUnionOf( classes ) );
    }
    
    @Handler("DisjointUnion")
    public void disjointUnion() {
        Set<OWLDescription> classes = new HashSet<OWLDescription>();
        
        for( Connector conn : graphic.incoming ) {
            classes.add( classForGraphic( premptGraphic( notNullShape( conn.getTail() ))));
        }

        if( classes.size() < 2 ) throw new RuntimeException( "Disjoint union requires 2 or more target classes" );        
        
        OWLClass clazz = (OWLClass) classForGraphic( premptGraphic( graphic.outgoing.iterator().next().getHead()));
        
        addAxiom( factory.getOWLDisjointUnionAxiom( clazz, classes ));
    }
    
    @Handler("DisjointClasses")
    public void disjointClasses() {
        OWLDescription class1 = classForGraphic( premptGraphic( headShape()));
        OWLDescription class2 = classForGraphic( premptGraphic( tailShape()));
        
        addAxiom( factory.getOWLDisjointClassesAxiom( class1, class2 ));
    }
        
    @Handler("PairwiseDisjointClasses")
    public void pairwiseDisjointClasses() {
        Set<OWLDescription> classes = new HashSet<OWLDescription>();
        
        for( Connector conn : graphic.outgoing ) {
            classes.add( classForGraphic( premptGraphic( notNullShape( conn.getHead() ))));
        }
        
        addAxiom( factory.getOWLDisjointClassesAxiom( classes ));
    }
        
    @Handler("EquivalentClasses")
    public void equivalentClasses() {
        OWLDescription class1 = classForGraphic( premptGraphic( headShape()));
        OWLDescription class2 = classForGraphic( premptGraphic( tailShape()));
        
        addAxiom( factory.getOWLEquivalentClassesAxiom( class1, class2 ));
    }
    
    @Handler("OWL Class")
    public void owlClass() {
        classForGraphic( graphic );        
    }

    @Handler("SubClassOf")
    public void subClass() {
        OWLDescription superClass = classForGraphic( premptGraphic( headShape()));
        OWLDescription subClass   = classForGraphic( premptGraphic( tailShape()));
        
        addAxiom( factory.getOWLSubClassAxiom( subClass, superClass ) );
    }
    
    private OWLDescription classForGraphic( Graphic g ) {
        OWLDescription desc = descCache.get( g );
        if( desc != null ) return desc;
        
        String className = makeName( g, "OWL Class" );
        OWLClass clazz = getOWLClass( className );
        descCache.put( g, clazz );
        return clazz;
    }
    
    private Shape headShape() {
        return notNullShape( notNullLine( graphic ).head );        
    }

    private Shape tailShape() {
        return notNullShape( notNullLine( graphic ).tail );        
    }
    
    private Line notNullLine( Graphic g ) {
        if( g == null ) throw new RuntimeException( "Line required" );
        if( !( g instanceof Line )) throw new RuntimeException( "Not a line" );
        return (Line) g;        
    }
    
    private Shape notNullShape( Graphic g ) {
        if( g == null ) throw new RuntimeException( "Shape required" );
        if( !( g instanceof Shape )) throw new RuntimeException( "Not a shape" );
        return (Shape) g;
    }
    
    private OWLClass getOWLClass( String name ) {
        OWLClass c = classCache.get( name );
        if( c == null ) {
            c = factory.getOWLClass( URI.create( uri + name ) );
            declare( c );
            classCache.put( name, c );
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
            throw new RuntimeException(e);
        }
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
}
