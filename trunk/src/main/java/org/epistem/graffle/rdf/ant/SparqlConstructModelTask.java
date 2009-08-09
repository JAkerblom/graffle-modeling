package org.epistem.graffle.rdf.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.epistem.graffle.rdf.SparqlFile;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Task to execute a SPARQL Contruct query on a model.
 * The SPARQL file is retained statically as the default for subsequent
 * invocations that omit the property.
 *
 * @author nickmain
 */
public class SparqlConstructModelTask extends RDFModelBaseTask {

    private static SparqlFile lastSparqlFile;
    
    private File   sparqlFile;
    private String targetModelName = DEFAULT_MODEL;
    private String queryName;
    
    /**
     * Set the SPARQL file to use
     */
    public void setSparql( File file ) {
        this.sparqlFile = file;
    }

    /**
     * Set the name of the query to execute
     */
    public void setQuery( String name ) {
        this.queryName = name;
    }

    /**
     * Set the name of the target model
     */
    public void setTarget( String targetModelName ) {
        this.targetModelName = targetModelName;
    }

    @Override
    protected void execute( Model model ) throws Exception {
        SparqlFile sparql = lastSparqlFile;
        
        if( sparqlFile != null ) {
            lastSparqlFile = sparql = new SparqlFile( sparqlFile );
        }
        
        if( sparql    == null ) throw new BuildException( "No Sparql file has been specified" );
        if( queryName == null ) throw new BuildException( "No Sparql file has been specified" );
        
        String query = sparql.queries.get( queryName );
        if( query == null ) throw new BuildException( "Sparql query could not be found: " + queryName );
        
        Model target = getOrMakeModel( targetModelName );
        
        log( "Executing query '" + queryName + "' on model '" + modelName + "' --> '" + targetModelName + "'" );
        QueryExecution qexec = QueryExecutionFactory.create( query, model );
        qexec.execConstruct( target ) ;
        qexec.close() ;
    }
}
