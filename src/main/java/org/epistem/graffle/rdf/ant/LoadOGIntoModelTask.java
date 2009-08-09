package org.epistem.graffle.rdf.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.epistem.graffle.OmniGraffleDoc;
import org.epistem.graffle.rdf.GraffleToRDF;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Task to load an OG document into a named model
 *
 * @author nickmain
 */
public class LoadOGIntoModelTask extends RDFModelBaseTask {

    private File ogFile;
    
    /**
     * Set the OG file to read
     */
    public void setFile( File ogFile ) {
        this.ogFile = ogFile;
    }
    
    /** @see org.epistem.graffle.rdf.ant.RDFModelBaseTask#execute(com.hp.hpl.jena.rdf.model.Model) */
    @Override
    protected void execute( Model model ) throws Exception {
        if( ogFile == null ) throw new BuildException( "File is required" );
        
        OmniGraffleDoc doc = new OmniGraffleDoc( ogFile );

        log( "Loading OG doc " + ogFile.getName() + " into RDF model '" + modelName + "'" );
        new GraffleToRDF( doc, model );
    }
}
