package org.epistem.graffle.rdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A file containing one or more SPARQL queries. The queries are delimited
 * by lines starting with "##". Any text following the "##" is taken to be the
 * name of the following query.
 * 
 * Any lines before the first "##" are assumed to be global prefix definitions
 * and are prepended to each query.
 * 
 * "#import filename" can be used to import other files - filename is relative
 * to the importing file.
 *
 * @author nickmain
 */
public class SparqlFile {

    /**
     * The queries keyed by name, iterates in order encountered in the file
     */
    public final Map<String, String> queries;
    
    private final File dir;
    private StringBuilder prefixBuff = new StringBuilder();
    private String prefixes;
    
    /**
     * @param file the file to read
     */
    public SparqlFile( File file ) throws IOException {
        this( file, "", new LinkedHashMap<String, String>() );
    }
    
    /**
     * @param file the file to read
     * @param prefixes prefixes from the parent
     */
    private SparqlFile( File file, String prefixesIn, Map<String, String> queries ) throws IOException {
        this.queries = queries;
        prefixBuff.append( prefixesIn );
        dir = file.getParentFile();
        
        FileReader in = new FileReader( file );
        try {
            BufferedReader reader = new BufferedReader( in );
            
            StringBuilder queryBuff = new StringBuilder();
            String queryName = null;
            
            while( true ) {
                String line = reader.readLine();
                
                if( line != null ) {                
                    if( line.startsWith( "#import " ) ) {
                        addQuery( queryName, queryBuff );
                        queryName = null;
                        
                        String importName = line.substring( 8 ).trim();
                        File importFile = new File( dir, importName );
                        if( ! importFile.exists() ) throw new IOException( "Could not find import " + importName );
                        
                        SparqlFile imp = new SparqlFile( importFile, prefixes, queries );
                        queries.putAll( imp.queries );
                        continue;
                    }
                    else if( line.startsWith( "##" ) ) {
                        addQuery( queryName, queryBuff );
                        
                        if( line.length() == 2 ) line = "query-" + (queries.size()+1);
                        else line = line.substring( 2 );
                        
                        line = line.replace( '#', ' ' ).trim();
                        queryName = line;
                        continue;
                    }
                    
                    //skip comments and empty lines
                    if( line.trim().startsWith( "#" )) continue;
                    if( line.trim().length() == 0 ) continue;
                    
                    if( queryName == null ) {
                        if( prefixBuff != null ) prefixBuff.append( line + "\n" );
                    }
                    else queryBuff.append( line + "\n" );
                }
                else {
                    addQuery( queryName, queryBuff );
                    break;
                }
            }            
        } finally {
            in.close();
        }
    }
    
    private void addQuery( String name, StringBuilder buff ) {
        if( prefixBuff != null ) {
            prefixes = prefixBuff.toString();
            prefixBuff = null;
        }
        
        if( name == null ) return;
        String query = prefixes + buff.toString();        
        queries.put( name, query );
        buff.setLength( 0 );
    }
}
