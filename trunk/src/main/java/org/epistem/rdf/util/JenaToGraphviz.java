package org.epistem.rdf.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.epistem.io.IndentingPrintWriter;
import org.epistem.util.GraphvizWriter;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Utilily to generate a Graphviz diagram from a Jena RDF model
 *
 * @author nickmain
 */
public class JenaToGraphviz {

    private final Model model;
    private GraphvizWriter gviz;
    
    public JenaToGraphviz( Model model ) {
        this.model = model;
    }
    
    /**
     * Write the graph
     */
    public void write( IndentingPrintWriter ipw ) {
        this.gviz = new GraphvizWriter( ipw );
        write();
    }
    
    /**
     * Write the graph
     */
    public void write( String filename ) throws IOException {
        write( new IndentingPrintWriter( new FileWriter( filename ) ) );
    }
    
    private void write() {
        gviz.startDiGraph( "model" );
        
        for( NodeIterator it = model.listObjects(); it.hasNext(); ) {
            RDFNode node = it.nextNode();
            
            if( node.isAnon() ) {
                gviz.declareNode( "node" + node.hashCode(), "node", "#ffcccc" );
            }
            else if( node.isURIResource() ) {
                Resource res = (Resource) node;
                gviz.declareNode( "node" + node.hashCode(), res.getLocalName(), "#ccccff" );
            }
            else if( node.isLiteral() ) {
                Literal lit = (Literal) node;
                gviz.declareNode( "lit-" + lit.toString().hashCode(), lit.toString(), "#ffffcc" );
            }
            else if( node.isResource() ) {
                Resource res = (Resource) node;
                gviz.declareNode( "node" + node.hashCode(), res.getLocalName(), "#ccccff" );                
            }
        }

        for( StmtIterator it = model.listStatements(); it.hasNext(); ) {
            Statement stat = it.nextStatement();
         
            RDFNode object = stat.getObject();
            String target = "node" + object.hashCode();
         
            if( object instanceof Literal ) {
                target = "lit-" + object.toString().hashCode();
            }
            
            gviz.arc( "node" + stat.getSubject().hashCode(),
                      target,
                      stat.getPredicate().getLocalName());
        }
        
        gviz.endDiGraph();
        gviz.close();
    }
}
