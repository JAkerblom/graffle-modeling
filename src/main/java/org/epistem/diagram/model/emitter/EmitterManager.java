package org.epistem.diagram.model.emitter;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.epistem.diagram.model.Diagram;
import org.epistem.diagram.model.DiagramVisitor;
import org.epistem.diagram.model.Shape;
import org.epistem.diagram.model.Table;
import org.epistem.graffle.OmniGraffleDoc;

/**
 * Finds and manages the emitters for a diagram
 *
 * @author nickmain
 */
public class EmitterManager {

    public static final String NAMESPACE_TABLE = "*namespaces*";
    public static final String EMITTER         = "*emitter*";
    public static final String EMITTER_CLASS   = "class";

    private final File    outputDir;
    private final Diagram diagram;
    private final Collection<ModelEmitter> emitters = new HashSet<ModelEmitter>();
    private final Map<String,String> namespaces = new HashMap<String, String>();
    private String defaultNamespace;
    
    /**
     * @param diagram the diagram to process
     */
    public EmitterManager( Diagram diagram, File outputDir ) {
        this.diagram   = diagram;
        this.outputDir = outputDir;
        outputDir.mkdirs();
        
        this.diagram.accept( new EmitterNSFinder() );
        
        if( emitters.isEmpty() ) throw new RuntimeException( "No emitters found" );
    }
    
    /**
     * Execute all the emitters
     */
    public void executeEmitters() {
        for( ModelEmitter emitter : emitters ) {
            emitter.generate( defaultNamespace, namespaces );
        }

        for( ModelEmitter emitter : emitters ) {
            emitter.write();
        }
    }
    
    public static void main( String[] args ) throws Exception {
        OmniGraffleDoc doc = new OmniGraffleDoc( new File("test-diagrams/test-owl.graffle" ));
        Diagram diagram = new Diagram( doc );
        EmitterManager manager = new EmitterManager( diagram, new File( "target" ));
        
        manager.executeEmitters();
    }
    
    /**
     * Visitor to find all the emitters and namespaces
     */
    private class EmitterNSFinder extends DiagramVisitor.Impl {

        @Override
        public void visitShape( Shape shape ) {
            if( EMITTER.equals( shape.metadata.notes ) ) {
                String emitterClass = shape.metadata.properties.get( EMITTER_CLASS );
                String emitterFile  = shape.text;
                try {
                    File file = new File( outputDir, emitterFile );
                    
                    ModelEmitter emitter = (ModelEmitter) Class.forName( emitterClass ).newInstance();
                    emitter.setOutputFile( file );
                    emitter.setDiagram( diagram );
                    emitters.add( emitter );
                }
                catch( Exception ex ) {
                    throw new RuntimeException( "Could not instantiate emitter " + emitterClass, ex );
                }
            }            
        }

        @Override
        public DiagramVisitor visitTableStart( Table table ) {
            if( NAMESPACE_TABLE.equals( table.metadata.notes )) {
                for( Shape[] row : table.table ) {
                    String prefix = row[0].text;
                    String url    = row[1].text;
                    
                    if( prefix.endsWith( "*" ) ) {
                        prefix = prefix.substring( 0, prefix.length() - 1 );
                        defaultNamespace = url;
                    }
                    
                    System.out.println( "Namespace " + prefix + " = " + url );
                    namespaces.put( prefix, url );
                }
                
                return null;
            }
            
            return super.visitTableStart( table );
        }        
    }
}
