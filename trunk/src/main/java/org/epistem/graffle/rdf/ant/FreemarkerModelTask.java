package org.epistem.graffle.rdf.ant;

import java.io.*;
import java.util.*;

import org.epistem.graffle.rdf.SparqlFile;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import freemarker.template.*;

/**
 * Task to apply a Freemarker template to a model
 *
 * @author nickmain
 */
public class FreemarkerModelTask extends RDFModelBaseTask {

    private SparqlFile sparqlFile;
    private File freemarkerFile;
    private File outputFile;
    
    /**
     * Set the SPARQL file to use
     */
    public void setSparql( File file ) throws IOException {
        this.sparqlFile = new SparqlFile( file );
    }
    
    /**
     * Set the file to write
     */
    public void setOut( File file ) {
        this.outputFile = file;
    }

    /**
     * Set the template file
     */
    public void setTemplate( File file ) {
        this.freemarkerFile = file;
    }

    
    /** @see org.epistem.graffle.rdf.ant.RDFModelBaseTask#execute(com.hp.hpl.jena.rdf.model.Model) */
    @Override
    protected void execute( Model model ) throws Exception {
        
        Configuration configuration = new Configuration();
        configuration.setDirectoryForTemplateLoading( freemarkerFile.getParentFile() );
        configuration.setObjectWrapper( new DefaultObjectWrapper() );
        
        Template template = configuration.getTemplate( freemarkerFile.getName() );

        Map<String,Object> root = new HashMap<String, Object>();
        root.put( "query", new SparqlQueryMethod( model ) );

        Writer out = null;
        
        if( outputFile == null ) {
            log( "Dumping template output for model '" + modelName + "' to stdout" );
            out = new OutputStreamWriter( System.out );
        }
        else {
            log( "Writing template output for model '" + modelName + "' to " + outputFile.getName() );
            out = new FileWriter( outputFile );
        }        
        
        template.process( root, out );        
    }
    
    private class SparqlQueryMethod implements TemplateMethodModel {

        private final Model model;
        
        public SparqlQueryMethod( Model model ) {
            this.model = model; 
        }
        
        /** @see freemarker.template.TemplateMethodModel#exec(java.util.List) */
        @SuppressWarnings("unchecked")
        public Object exec( List args ) throws TemplateModelException {
            if( args.size() != 1 ) throw new TemplateModelException( "Only one arg expected" );
             
            String queryName   = args.get( 0 ).toString();
            String queryString = sparqlFile.queries.get( queryName );
 
            List<Map<String,RDFNode>> resultList = new ArrayList<Map<String,RDFNode>>();
            
            Query          query = QueryFactory.create( queryString );
            QueryExecution qexec = QueryExecutionFactory.create( query, model );
            try {
                for( ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
                    QuerySolution soln = results.nextSolution() ;
                    
                    Map<String,RDFNode> row = new HashMap<String, RDFNode>();
                    resultList.add( row );
                    
                    for( Iterator<String> it = soln.varNames(); it.hasNext(); ) {
                        String  varName = it.next();
                        RDFNode value   = soln.get( varName );
                        
                        row.put( varName, value );
                    }
                }
            } 
            finally { 
                qexec.close() ; 
            }
            
            return new SimpleSequence( resultList );
        }        
    }
}
