package org.epistem.graffle.rdf.ant;

import java.io.File;
import java.io.FileWriter;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Task to load an OG document into a named model
 *
 * @author nickmain
 */
public class WriteRDFModelTask extends RDFModelBaseTask {

    private File rdfFile;
    private String format = "N3";
    
    /**
     * Set the output format, default is N3
     */
    public void setFormat( String format ) {
        this.format = format;
    }
    
    /**
     * Set the RDF file to write
     */
    public void setFile( File rdfFile ) {
        this.rdfFile = rdfFile;
    }
    
    /** @see org.epistem.graffle.rdf.ant.RDFModelBaseTask#execute(com.hp.hpl.jena.rdf.model.Model) */
    @Override
    protected void execute( Model model ) throws Exception {
        
        if( rdfFile == null ) {
            log( "Dumping model '" + modelName + "' to stdout" );
            model.write( System.out, format );
        }
        else {
            log( "Writing model '" + modelName + "' to " + rdfFile.getName() );
            FileWriter out = new FileWriter( rdfFile );
            model.write( out, format );
            out.close();
        }        
    }
}
