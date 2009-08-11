package org.epistem.diagram.model;

/**
 * Diagram visitor
 *
 * @author nickmain
 */
public interface DiagramVisitor {

    /**
     * Start a diagram visit
     * 
     * @return true to visit all pages
     */
    public boolean visitDiagramStart( Diagram diagram );

    /**
     * End of diagram visit
     */
    public void visitDiagramEnd( Diagram diagram );

    /**
     * Start of a page visit
     * 
     * @return true to visit all root graphics
     */
    public boolean visitPageStart( Page page );
    
    /**
     * End of page visit
     */
    public void visitPageEnd( Page page );
    
    /**
     * Start of a group visit
     * 
     * @return true to visit all child graphics
     */
    public boolean visitGroupStart( Group group );
    
    /**
     * End of group visit
     */
    public void visitGroupEnd( Group group );
    
    /**
     * Visit a shape
     */
    public void visitShape( Shape shape );
    
    /**
     * Visit a connector shape
     */
    public void visitConnectorShape( ConnectorShape shape );
    
    /**
     * Start a line visit
     * 
     * @return true to visit all the labels
     */
    public boolean visitLineStart( Line line );
    
    /**
     * End of a line visit
     */
    public void visitLineEnd( Line line );
    
    /**
     * Start of a table visit
     * 
     * @return true to visit all the cell shapes
     */
    public boolean visitTableStart( Table table );

    /**
     * End of a table visit
     */
    public void visitTableEnd( Table table );
}
