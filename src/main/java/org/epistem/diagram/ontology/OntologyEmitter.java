package org.epistem.diagram.ontology;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.epistem.diagram.model.Connector;
import org.epistem.diagram.model.ConnectorShape;
import org.epistem.diagram.model.Diagram;
import org.epistem.diagram.model.DiagramVisitor;
import org.epistem.diagram.model.Graphic;
import org.epistem.diagram.model.Group;
import org.epistem.diagram.model.Line;
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
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLConstantAnnotation;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLProperty;

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
        Ontology, Imports, 
        Individual, Class, DataProperty, ObjectProperty, DataType,
        Extends, Equivalent, Disjoint, DisjointUnion, Key,
        Union, Intersection, Complement, Member, All, PropertyGrid, Property,
        Cardinality
        ;
     
        /** Whether this note matches a graphic */
        public boolean matches( Graphic g ) {
            if( g == null || g.metadata.notes == null ) return false;
            return name().equalsIgnoreCase( g.metadata.notes.trim() );
        }
    }
    
    /**
     * Represents a property line 
     */
    private class LineAndProperty {
        OWLProperty<?,?> property;
        Graphic source;
        Graphic target;
        boolean isSolid;
        int cardinality     = -1;
        int cardinalityLow  = -1;
        int cardinalityHigh = -1;
    }
    
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory     factory = manager.getOWLDataFactory();

    private final Diagram diagram;
    private URI ontologyURI;
    private OWLOntology ontology;
    
    private final Map<URI,OWLClass>          classCache      = new HashMap<URI, OWLClass>();
    private final Map<URI,OWLIndividual>     individualCache = new HashMap<URI, OWLIndividual>();
    private final Map<URI,OWLDataProperty>   dataPropCache   = new HashMap<URI, OWLDataProperty>();
    private final Map<URI,OWLObjectProperty> objPropCache    = new HashMap<URI, OWLObjectProperty>();
    private final Map<URI,OWLDataType>       datatypeCache   = new HashMap<URI, OWLDataType>();
    private final Map<String,String>         uriPrefixes     = new HashMap<String, String>();

    private final Map<Shape,OWLClass>          shapeClassCache    = new HashMap<Shape, OWLClass>();
    private final Map<Shape,OWLIndividual>     shapeIndivCache    = new HashMap<Shape, OWLIndividual>();
    private final Map<Shape,OWLDataProperty>   shapeDataPropCache = new HashMap<Shape, OWLDataProperty>();
    private final Map<Shape,OWLObjectProperty> shapeObjPropCache  = new HashMap<Shape, OWLObjectProperty>();
    private final Map<Shape,OWLDataType>       shapeDatatypeCache = new HashMap<Shape, OWLDataType>();    
    private final Map<Shape,OWLConstant>       shapeConstants     = new HashMap<Shape, OWLConstant>();
    
    //connectors that have already been processed as part of a disjoint axiom
    private final Set<Connector> disjointConnectors = new HashSet<Connector>();
    
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

        processClassAxioms();
        processClassExpressions();
        
        cleanupIndividuals();
    }
    
    private void processClassExpressions() {
        for( Shape s : shapeClassCache.keySet() ) {
            OWLClass cls = shapeClassCache.get( s );

            //Union
            Set<OWLClass> classes = getLineTargetClasses( s, OntoNote.Union, true, true );
            if( ! classes.isEmpty() ) {
                addAxiom( factory.getOWLEquivalentClassesAxiom( cls, factory.getOWLObjectUnionOf( classes )));
            }
            classes = getLineTargetClasses( s, OntoNote.Union, false, true );
            if( ! classes.isEmpty() ) {
                addAxiom( factory.getOWLSubClassAxiom( cls, factory.getOWLObjectUnionOf( classes )));
            }
            
            //Intersection
            classes = getLineTargetClasses( s, OntoNote.Intersection, true, true );
            if( ! classes.isEmpty() ) {
                addAxiom( factory.getOWLEquivalentClassesAxiom( cls, factory.getOWLObjectIntersectionOf( classes )));
            }
            classes = getLineTargetClasses( s, OntoNote.Intersection, false, true );
            if( ! classes.isEmpty() ) {
                addAxiom( factory.getOWLSubClassAxiom( cls, factory.getOWLObjectIntersectionOf( classes )));
            }
            
            //Complement
            for( OWLClass other : getLineTargetClasses( s, OntoNote.Complement, true, true ) ) {
                addAxiom( factory.getOWLEquivalentClassesAxiom( cls, factory.getOWLObjectComplementOf( other ) ) );
            }
            for( OWLClass other : getLineTargetClasses( s, OntoNote.Complement, false, true ) ) {
                addAxiom( factory.getOWLSubClassAxiom( cls, factory.getOWLObjectComplementOf( other ) ) );
            }
            
            //OneOf
            Set<OWLIndividual> indivs = getLineTargetIndivs( s, OntoNote.Member, true, true );
            if( ! indivs.isEmpty() ) {
                addAxiom( factory.getOWLEquivalentClassesAxiom( cls, factory.getOWLObjectOneOf( indivs )));
            }
            indivs = getLineTargetIndivs( s, OntoNote.Member, false, true );
            if( ! indivs.isEmpty() ) {
                addAxiom( factory.getOWLSubClassAxiom( cls, factory.getOWLObjectOneOf( indivs )));
            }
            
            //All
            for( LineAndProperty pline : getPropLines( s, OntoNote.All, true ) ) {
                OWLClass target = getOWLClass( (Shape) pline.target );
                addAxiom( factory.getOWLEquivalentClassesAxiom( 
                    cls, factory.getOWLObjectAllRestriction( (OWLObjectProperty) pline.property, target ) ) );
            }
            for( LineAndProperty pline : getPropLines( s, OntoNote.All, false ) ) {
                OWLClass target = getOWLClass( (Shape) pline.target );
                addAxiom( factory.getOWLSubClassAxiom(  
                    cls, factory.getOWLObjectAllRestriction( (OWLObjectProperty) pline.property, target ) ) );
            }
            
            //HasValue and Some
            Collection<LineAndProperty> laps = getPropLines( s, OntoNote.Property, null );
        }
    }
    
    private void processClassAxioms() {
        for( Shape s : shapeClassCache.keySet() ) {
            OWLClass cls = shapeClassCache.get( s );
            
            //SubClassOf
            for( OWLClass supercls : getLineTargetClasses( s, OntoNote.Extends, null, true ) ) {
                addAxiom( factory.getOWLSubClassAxiom( cls, supercls ) );
            }
            
            //EquivalentClasses
            for( OWLClass eqivcls : getLineTargetClasses( s, OntoNote.Equivalent, null, true ) ) {
                addAxiom( factory.getOWLEquivalentClassesAxiom( cls, eqivcls ) );
            }
            
            //DisjointClasses
            for( Set<OWLClass> disjoints : getDisjointClasses( s ) ) {
                addAxiom( factory.getOWLDisjointClassesAxiom( disjoints ) );
            }
            
            //DisjointUnion
            Set<OWLClass> disjoints = getLineTargetClasses( s, OntoNote.DisjointUnion, null, false );
            if( ! disjoints.isEmpty() ) addAxiom( factory.getOWLDisjointUnionAxiom( cls, disjoints ) );
            
            //Key
            //TODO: OWL API does not yet support HasKey
        }
    }

    private Collection<Set<OWLClass>> getDisjointClasses( Shape start ) {
        Collection<Set<OWLClass>> disGroups = new HashSet<Set<OWLClass>>();
        
        for( Connector line : start.outgoing ) { 
            //no need to check incoming since there will always be at least one
            //shape at the tail end of a line in a group of disjoint classes
            
            if( OntoNote.Disjoint.matches( (Graphic) line) ) {
                Collection<Shape> disShapes = gatherDisjoints( line, null );
                if( disShapes.isEmpty() ) continue;
                
                Set<OWLClass> disClasses = new HashSet<OWLClass>();
                disGroups.add( disClasses );
                for( Shape s : disShapes ) {
                    OWLClass cls = shapeClassCache.get( s );
                    if( cls == null ) graphicException( s, "Target of disjoint line is not an OWL class" );

                    disClasses.add( cls );
                }
            }
        }
        
        return disGroups;
    }
    
    /**
     * Gather all the shapes that are reachable via the given line
     */
    private Collection<Shape> gatherDisjoints( Connector line, Collection<Shape> shapes ) {
        if( shapes == null ) shapes = new HashSet<Shape>();

        //avoid visiting the same line more than once
        if( disjointConnectors.contains( line ) ) return shapes;
        disjointConnectors.add( line );
        
        if( ! OntoNote.Disjoint.matches( (Graphic) line ) ) {
            graphicException( (Graphic) line, "Line is not a 'disjoint' line" );
        }        
        
        Graphic head = line.getHead();
        Graphic tail = line.getTail();
        
        if( head == null || tail == null ) graphicException( (Graphic) line, "Disjoint line is missing a head or tail" );
        
        if( head instanceof Connector ) {
            gatherDisjoints( (Connector) head, shapes );
        }
        else if( head instanceof Shape ) {
            shapes.add( (Shape) head );
        }
        else {
            graphicException( head, "Head of disjoint line is not a simple shape" );
        }
        
        if( tail instanceof Connector ) {
            gatherDisjoints( (Connector) tail, shapes );
        }
        else if( tail instanceof Shape ) {
            shapes.add( (Shape) tail );
        }
        else {
            graphicException( tail, "Tail of disjoint line is not a simple shape" );
        }
        
        for( Connector c : ((Graphic) line).incoming ) gatherDisjoints( c, shapes );
        for( Connector c : ((Graphic) line).outgoing ) gatherDisjoints( c, shapes );
        
        return shapes;
    }
    
    private Set<OWLClass> getLineTargetClasses( Shape origin, OntoNote note, Boolean solid, boolean outgoing  ) {
        Collection<Graphic> shapes = getLineTargets( origin, note, solid, outgoing );
        if( shapes.isEmpty() ) return Collections.emptySet();
        
        Set<OWLClass> classes = new HashSet<OWLClass>();
        for( Graphic s : shapes ) {            
            OWLClass cls = (s instanceof Shape) ? shapeClassCache.get( (Shape) s ) : null;
            if( cls == null ) graphicException( s, "Target of " + note + " is not an OWL class" );
            classes.add( cls );
        }
        
        return classes;
    }

    private Set<OWLIndividual> getLineTargetIndivs( Shape origin, OntoNote note, Boolean solid, boolean outgoing  ) {
        Collection<Graphic> shapes = getLineTargets( origin, note, solid, outgoing );
        if( shapes.isEmpty() ) return Collections.emptySet();
        
        Set<OWLIndividual> indivs = new HashSet<OWLIndividual>();
        for( Graphic s : shapes ) {
            
            if((s instanceof Table) && OntoNote.PropertyGrid.matches( s ) ) {
                indivs.addAll( getGridIndividuals( (Table) s ) ); 
                continue;
            }            
            
            OWLIndividual ind = shapeIndivCache.get( s );
            if( ind == null ) graphicException( s, "Target of " + note + " is not an OWL individual" );
            indivs.add( ind );
        }
        
        return indivs;
    }
    
    private void graphicException( Graphic g, String message ) {
        throw new RuntimeException( "Sheet '" + g.page.title + "' (" + ((int) g.x) + "," + ((int) g.y) + "): " + message );
    }
    
    /**
     * Get property lines
     */
    private Collection<LineAndProperty> getPropLines( Shape origin, OntoNote note, Boolean solid ) {
        Set<LineAndProperty> lines = new HashSet<LineAndProperty>();
        
        for( Connector c : origin.outgoing ) {
            if( solid != null && solid != c.isSolid() ) continue;
            if( !( c instanceof Line) ) continue;
            Line line = (Line) c;
            
            if( note.matches( line ) ) {
                LineAndProperty lap = new LineAndProperty();
                lap.source   = line.tail;
                lap.target   = line.head;
                lap.isSolid  = line.isSolid;
                
                for( Shape label : line.labels ) {

                    if( OntoNote.Cardinality.matches( label ) ) {
                        String cardStr = label.text.trim();
                        
                        
                    }

                    if( OntoNote.DataProperty.matches( label )
                     || OntoNote.ObjectProperty.matches( label )) {
                        if( lap.property != null ) graphicException( label, "More than one property on a property line" );
                        
                        OWLProperty<?,?> prop = getOWLProperty( label );                    
                        lap.property = prop;                    
                    }
                }

                lines.add( lap );
            }
        }
        
        return lines;
    }
    
    /**
     * Get the graphics targeted via lines with a given note
     * 
     * @param origin the start shape
     * @param note the note for the lines
     * @param solid whether the line are solid or dashed (null for don't care)
     * @param outgoing true for outgoing, false for incoming
     * @return graphics targeted by matching lines 
     */
    private Collection<Graphic> getLineTargets( Shape origin, OntoNote note, Boolean solid, boolean outgoing ) {
        Set<Graphic> targets = new HashSet<Graphic>();
        
        for( Connector line : (outgoing ? origin.outgoing : origin.incoming )) {
            if( solid != null && solid != line.isSolid() ) continue;
            
            if( note.matches( (Graphic) line ) ) {
                Graphic g = outgoing ? line.getHead() : line.getTail();
                if( g != null ) {
                    targets.add( g ); 
                }
            }
        }
        
        return targets;
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

    private OWLIndividual getOWLIndividual( Shape s ) {
        OWLIndividual ind = shapeIndivCache.get( s );
        if( ind == null ) {
            URI uri = uriFromString( makeName( s, "Individual" ));  
            if( uri == null ) return null;
            
            ind = getOWLIndividual( uri );
            shapeIndivCache.put( s, ind );
        }
        
        return ind;
    }
    
    private OWLIndividual getOWLIndividual( URI name ) {
        OWLIndividual i = individualCache.get( name );
        if( i == null ) {
            i = factory.getOWLIndividual( name );
            if( isLocalName( name ) ) declare( i );

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
            
            if( isLocalName( classURI ) ) declare( c );

            classCache.put( classURI, c );
        }
        
        return c;
    }
    
    private OWLDataProperty getOWLDataProperty( Shape s ) {
        OWLDataProperty p = shapeDataPropCache.get( s );
        if( p == null ) {            
            URI uri = uriFromString( makeName( s, "Data Property" ));  
            if( uri == null ) return null;
            
            p = getOWLDataProperty( uri );
            shapeDataPropCache.put( s, p );
        }
        
        return p;
    }
    
    private OWLDataProperty getOWLDataProperty( URI uri ) {
        OWLDataProperty p = dataPropCache.get( uri );
        if( p == null ) {
            p = factory.getOWLDataProperty( uri );
            if( isLocalName( uri ) ) declare( p );

            dataPropCache.put( uri, p );
        }
        
        return p;
    }

    private OWLObjectProperty getOWLObjectProperty( Shape s ) {
        OWLObjectProperty p = shapeObjPropCache.get( s );
        if( p == null ) {            
            URI uri = uriFromString( makeName( s, "Object Property" ));  
            if( uri == null ) return null;
            
            p = getOWLObjectProperty( uri );
            shapeObjPropCache.put( s, p );
        }
        
        return p;
    }
    
    private OWLObjectProperty getOWLObjectProperty( URI uri ) {
        OWLObjectProperty p = objPropCache.get( uri );
        if( p == null ) {
            p = factory.getOWLObjectProperty( uri );
            if( isLocalName( uri ) ) declare( p );

            objPropCache.put( uri, p );
        }
        
        return p;
    }
    
    //Get a cached property
    private OWLProperty<?,?> getOWLProperty( Shape s ) {
        OWLProperty<?,?> prop = shapeDataPropCache.get( s );
        if( prop != null ) return prop;
        
        prop = shapeObjPropCache.get( s );
        if( prop != null ) return prop;
        
        graphicException( s, "Not a data or object property" );
        return null;
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
        String[] words = text.replace( '\n', ' ' ).split( "\\s+" );
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
    private boolean isLocalName( URI uri ) {
        return uri.toString().startsWith( ontologyURI.toString() + "#" );
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
        OWLConstant lit = shapeConstants.get( s );
        if( lit != null ) return lit;
        
        String text = s.text;
        if( text == null ) text = "";
        else text = text.trim();

        String note = s.metadata.notes;
        
        if( note != null ) {
            note = note.trim();
            
            if( note.startsWith( XSD_PREFIX ) ) {
                note = note.substring( XSD_PREFIX.length() );
                
                OWLDataType type = factory.getOWLDataType( URI.create( XSD_URI + note ));
                lit = factory.getOWLTypedConstant( text, type );
            }
        }
        
        if( lit == null ) lit = factory.getOWLTypedConstant( text );
        shapeConstants.put( s, lit );
        
        return lit;
    }
    
    /**
     * Get the subject individuals from a property grid
     */
    private Collection<OWLIndividual> getGridIndividuals( Table table ) {
        Collection<OWLIndividual> inds = new ArrayList<OWLIndividual>();
        
        int numRows = table.rowCount();
        
        for( int i = 1; i < numRows; i++ ) { //skip top row
            Shape[] row = table.table[i];            
            Shape indShape = row[0];
            inds.add( getOWLIndividual( indShape ) );
        }                

        return inds;
    }
    
    //Add a property assertion triple
    private void makePropertyAssertion( Shape ind, Shape prop, Shape value ) {
        OWLIndividual individual = getOWLIndividual( ind );
        OWLProperty<?,?> owlProp = getOWLProperty( prop );
        
        if( owlProp instanceof OWLDataProperty ) {
            OWLDataProperty dataProp = (OWLDataProperty) owlProp;
            OWLConstant constant = getLiteral( value );
            addAxiom( factory.getOWLDataPropertyAssertionAxiom( individual, dataProp, constant ) );
        }
        else if( owlProp instanceof OWLObjectProperty ) {
            OWLObjectProperty objProp = (OWLObjectProperty) owlProp;
            OWLIndividual target = getOWLIndividual( value );
            addAxiom( factory.getOWLObjectPropertyAssertionAxiom( individual, objProp, target ) );
        }
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

            if( OntoNote.Individual.matches( shape ) ) {
                getOWLIndividual( shape );
                return;
            }
            
            if( OntoNote.DataProperty.matches( shape ) ) {
                getOWLDataProperty( shape );
                return;
            }

            if( OntoNote.ObjectProperty.matches( shape ) ) {
                getOWLObjectProperty( shape );
                return;
            }
        }

        @Override
        public void visitTableEnd( Table table ) {
            if( OntoNote.PropertyGrid.matches( table ) ) {
                int numCols = table.colCount();
                int numRows = table.rowCount();
                
                Shape[] row0 = table.table[0];
                
                for( int i = 1; i < numRows; i++ ) {
                    Shape[] row = table.table[i];
                    
                    Shape indShape = row[0];
                    
                    for( int j = 1; j < numCols; j++ ) {
                        Shape propShape = row0[j];
                        Shape valShape  = row[j];
                        
                        makePropertyAssertion( indShape, propShape, valShape );
                    }
                }                
            }            
        }

        @Override
        public DiagramVisitor visitTableStart( Table table ) {

            //detect embedded individuals and properties
            return this;
        }
        
    }
}
