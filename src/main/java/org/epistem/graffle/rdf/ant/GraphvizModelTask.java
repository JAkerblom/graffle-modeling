package org.epistem.graffle.rdf.ant;

import java.io.File;

import org.epistem.io.IndentingPrintWriter;
import org.epistem.rdf.util.JenaToGraphviz;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Task to write a model as a Graphviz file
 *
 * @author nickmain
 */
public class GraphvizModelTask extends RDFModelBaseTask {

    private File gvFile;
    
    /**
     * Set the file to write
     */
    public void setFile( File file ) {
        this.gvFile = file;
    }
    
    /** @see org.epistem.graffle.rdf.ant.RDFModelBaseTask#execute(com.hp.hpl.jena.rdf.model.Model) */
    @Override
    protected void execute( Model model ) throws Exception {
        
        if( gvFile == null ) {
            log( "Dumping Graphviz for '" + modelName + "' to stdout" );
            new JenaToGraphviz( model ).write( IndentingPrintWriter.SYSOUT );
        }
        else {
            log( "Writing Graphviz for '" + modelName + "' to " + gvFile.getName() );
            new JenaToGraphviz( model ).write( gvFile.getAbsolutePath() );
        }        
    }
}
