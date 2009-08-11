package org.epistem.diagram.model.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.epistem.diagram.model.*;
import org.epistem.graffle.OmniGraffleDoc;
import org.epistem.io.IndentingPrintWriter;
import org.epistem.util.GraphvizWriter;

/**
 * Diagram visitor that dumps out a Graphviz representation
 *
 * @author nickmain
 */
public class GraphvizDumper implements DiagramVisitor {

    private final GraphvizWriter gv;
    
    public GraphvizDumper( IndentingPrintWriter out ) {
        gv = new GraphvizWriter( out );
    }
    
    public void visitConnectorShape( ConnectorShape shape ) {
        gv.declareNode( "" + shape.hashCode(), "" + shape.text, "#cccccc" );

        if( shape.head != null ) gv.arc( "" + shape.hashCode(), "" + shape.head.hashCode(), "head" );
        if( shape.tail != null ) gv.arc( "" + shape.hashCode(), "" + shape.tail.hashCode(), "tail" );
    }

    public void visitDiagramEnd( Diagram diagram ) {
        gv.endDiGraph();
        gv.close();
    }

    public boolean visitDiagramStart( Diagram diagram ) {
        gv.startDiGraph( "Diagram" );
        
        gv.declareNode( "diagram", diagram.file.getName(), "#ffffcc" );
        
        return true;
    }

    public void visitGroupEnd( Group group ) {
        for( Graphic g : group.children ) {
            gv.arc( "" + group.hashCode(), "" + g.hashCode(), "child" );
        }
    }

    public boolean visitGroupStart( Group group ) {
        gv.declareNode( "" + group.hashCode(), "Group\n" + group.text, "#ffccff" );
        return true;
    }

    public void visitLineEnd( Line line ) {
        for( Graphic g : line.labels ) {
            gv.arc( "" + line.hashCode(), "" + g.hashCode(), "label" );
        }
    }

    public boolean visitLineStart( Line line ) {
        gv.declareNode( "" + line.hashCode(), "Line", "#ccccff" );

        if( line.head != null ) gv.arc( "" + line.hashCode(), "" + line.head.hashCode(), "head" );
        if( line.tail != null ) gv.arc( "" + line.hashCode(), "" + line.tail.hashCode(), "tail" );
        
        return true;
    }

    public void visitPageEnd( Page page ) {
        for( Graphic g : page.rootGraphics ) {
            gv.arc( "" + page.hashCode(), "" + g.hashCode() );
        }
    }

    public boolean visitPageStart( Page page ) {
        
        gv.declareNode( "" + page.hashCode(), page.title, "#ccffcc" );
        gv.arc( "diagram", "" + page.hashCode(), "page" );
        
        return true;
    }

    public void visitShape( Shape shape ) {
        gv.declareNode( "" + shape.hashCode(), "" + shape.text, "#ccffff" );
    }

    public void visitTableEnd( Table table ) {
        for( Graphic g : table.cells ) {
            gv.arc( "" + table.hashCode(), "" + g.hashCode(), "cell" );
        }
    }

    public boolean visitTableStart( Table table ) {
        gv.declareNode( "" + table.hashCode(), "Table", "#cccc88" );
        return true;
    }

    public static void main( String[] args ) throws Exception {
        FileWriter out = new FileWriter( "target/test.dot" );
        OmniGraffleDoc doc = new OmniGraffleDoc( new File( "test-diagrams/test.graffle" ) );
        
        GraphvizDumper gv = new GraphvizDumper( new IndentingPrintWriter( out ) );
        Diagram diag = new Diagram( doc );
        diag.accept( gv );
    }
}
