package org.epistem.diagram.model.emitter;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.epistem.diagram.model.*;

/**
 * Base for model emitters
 *
 * @author nickmain
 */
public abstract class ModelEmitter {
    
    /**
     * Annotation on handler methods
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Handler {
        String value();
    }

    protected Diagram diagram;
    protected File outputFile;
    protected Map<String, Method> handlers = new HashMap<String, Method>();
    
    protected String defaultNamespace;
    protected Map<String,String> namespaces;
    protected Graphic graphic; //the current graphic
    
    protected Set<Graphic> processedGraphics = new HashSet<Graphic>();
    
    protected ModelEmitter() {
        for( Method m : getClass().getMethods() ) {
            Handler handler = m.getAnnotation( Handler.class );
            if( handler != null ) {
                handlers.put( handler.value(), m );
            }
        }
    }

    /**
     * Set the output file
     */
    public void setOutputFile( File outputFile ) {
        this.outputFile = outputFile;
    }
    
    /**
     * Set the diagram
     */
    public void setDiagram( Diagram diagram ) {
        this.diagram = diagram;
    }
    
    /**
     * Generate the model
     */
    public void generate( String defaultNamespace, Map<String,String> namespaces ) {
        this.defaultNamespace = defaultNamespace;
        this.namespaces       = namespaces;
        diagram.accept( getVisitor() );
    }
    
    /**
     * Write the model to disk
     */
    public abstract void write();
    
    /**
     * Create the generation visitor
     */
    protected Visitor getVisitor() {
        return new Visitor();
    }
    
    /**
     * Get the full url from a prefixed or default string
     */
    protected String toURL( String text ) {
        if( text.startsWith( "http://" ) ) return text;
        
        int colon = text.indexOf( ":" );
        if( colon < 0 ) return defaultNamespace + text;
        
        String prefix = text.substring( 0, colon );
        String rest   = text.substring( colon + 1 );
        if( prefix.trim().length() == 0 ) return defaultNamespace + rest;
        
        String uri = namespaces.get( prefix );
        if( uri == null ) throw new RuntimeException( "Unknown namespace prefix: " + prefix );
        return uri + rest;
    }
    
    /**
     * Process a graphic if it has not been handled yet.
     * @return the same graphic
     */
    protected Graphic premptGraphic( Graphic g ) {
        if( processedGraphics.contains( g ) ) return g;
        
        Graphic currentGraphic = this.graphic;
        handleGraphic( g );
        this.graphic = currentGraphic;
        return g;
    }
    
    /**
     * Handle generation for a graphic
     */
    protected void handleGraphic( Graphic graphic ) {
        this.graphic = graphic;
        processedGraphics.add( graphic );
        
        String note = graphic.metadata.notes;
        if( note != null && note.startsWith( "*" ) ) return; //skip specials
        if( note != null && note.startsWith( "xsd:" ) ) return; //skip value
        
        Method method = handlers.get( note );
        
        if( note != null && method == null ) {
            System.err.println( "No handler found for '" + note + "'" );
        }
        
        if( method != null ) {
            try {
                method.invoke( this );
            } 
            catch( InvocationTargetException e ) {
                if( e.getCause() instanceof RuntimeException ) throw (RuntimeException) e.getCause();
                throw new RuntimeException( e );
            }
            catch( Exception e ) {
                throw new RuntimeException( e );
            }
        }
    }
    
    /**
     * Generation visitor
     */
    protected class Visitor extends DiagramVisitor.Impl {

        @Override
        public void visitConnectorShape( ConnectorShape shape ) {
            handleGraphic( shape );
        }

        @Override
        public DiagramVisitor visitGroupStart( Group group ) {
            handleGraphic( group );
            
            return super.visitGroupStart( group );
        }

        @Override
        public DiagramVisitor visitLineStart( Line line ) {
            handleGraphic( line );
            
            return super.visitLineStart( line );
        }

        @Override
        public void visitShape( Shape shape ) {
            handleGraphic( shape );
        }

        @Override
        public DiagramVisitor visitTableStart( Table table ) {
            handleGraphic( table );

            return super.visitTableStart( table );
        }        
    }
}
