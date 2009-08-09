package org.epistem.graffle.rdf.ant;

import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Base for tasks that operate on RDF models. Maintains a static pool of named 
 * models that can be accessed by such tasks. Also holds the notion of the
 * "default" model.
 *
 * @author nickmain
 */
public abstract class RDFModelBaseTask extends Task {

    protected static final String DEFAULT_MODEL = "<DEFAULT>";
    private   static final Map<String, Model> models = new HashMap<String, Model>();
    
    protected String modelName = DEFAULT_MODEL;
    
    /**
     * Set the name of the model to use (omit for the default model)
     */
    public void setModel( String modelName ) {
        this.modelName = modelName;
    }

    /**
     * Get a model, which must exist.
     * 
     * @param name the model name, null for default model.
     * @throws BuildException if the model does not exist
     */
    protected Model getModel( String name ) throws BuildException {
        if( name == null ) name = DEFAULT_MODEL;

        Model model = models.get( name );
        if( model == null ) throw new BuildException( "Model does not exist: " + name );
        return model;
    }
   
    /**
     * Get an existing model or create a new empty one
     * @param name the model name, null for default model.
     */
    protected Model getOrMakeModel( String name ) {
        if( name == null ) name = DEFAULT_MODEL;
        
        Model model = models.get( name );
        
        if( model == null ) {
            models.put( name, model = ModelFactory.createDefaultModel() );
        }
        
        return model;        
    }

    /** @see org.apache.tools.ant.Task#execute() */
    @Override
    public void execute() throws BuildException {
        Model model = getOrMakeModel( modelName );
        
        try {
            execute( model );
        } 
        catch( BuildException e ) {
            throw e;
        } 
        catch( Exception e ) {
            throw new BuildException( e );
        }
    }
    
    /**
     * Execute against the given model
     */
    protected abstract void execute( Model model ) throws Exception;
}
