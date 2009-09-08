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
     * @return visitor for the pages - null to skip
     */
    public DiagramVisitor visitDiagramStart( Diagram diagram );

    /**
     * End of diagram visit
     */
    public void visitDiagramEnd( Diagram diagram );

    /**
     * Start of a page visit
     * 
     * @return visitor for the root graphics - null to skip
     */
    public DiagramVisitor visitPageStart( Page page );
    
    /**
     * End of page visit
     */
    public void visitPageEnd( Page page );
    
    /**
     * Start of a group visit
     * 
     * @return visitor for the children - null to skip
     */
    public DiagramVisitor visitGroupStart( Group group );
    
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
     * @return visitor for the labels - null to skip
     */
    public DiagramVisitor visitLineStart( Line line );
    
    /**
     * End of a line visit
     */
    public void visitLineEnd( Line line );
    
    /**
     * Start of a table visit
     * 
     * @return visitor for the cells - null to skip
     */
    public DiagramVisitor visitTableStart( Table table );

    /**
     * End of a table visit
     */
    public void visitTableEnd( Table table );
    
    /**
     * Convenience implementation that visits all children and labels
     */
    public static class Impl implements DiagramVisitor {
        public void visitConnectorShape( ConnectorShape shape ) {}
        public void visitDiagramEnd( Diagram diagram ) {}
        public DiagramVisitor visitDiagramStart( Diagram diagram ) { return this; }
        public void visitGroupEnd( Group group ) {}
        public DiagramVisitor visitGroupStart( Group group ) { return this; }
        public void visitLineEnd( Line line ) {}
        public DiagramVisitor visitLineStart( Line line ) { return this; }
        public void visitPageEnd( Page page ) {}
        public DiagramVisitor visitPageStart( Page page ) { return this; }
        public void visitShape( Shape shape ) {}
        public void visitTableEnd( Table table ) {}
        public DiagramVisitor visitTableStart( Table table ) { return this; }        
    }
    
    /**
     * Convenience implementation that does not visit children or labels but
     * does visit all pages
     */
    public static class ShallowImpl implements DiagramVisitor {
        public void visitConnectorShape( ConnectorShape shape ) {}
        public void visitDiagramEnd( Diagram diagram ) {}
        public DiagramVisitor visitDiagramStart( Diagram diagram ) { return this; }
        public void visitGroupEnd( Group group ) {}
        public DiagramVisitor visitGroupStart( Group group ) { return null; }
        public void visitLineEnd( Line line ) {}
        public DiagramVisitor visitLineStart( Line line ) { return null; }
        public void visitPageEnd( Page page ) {}
        public DiagramVisitor visitPageStart( Page page ) { return this; }
        public void visitShape( Shape shape ) {}
        public void visitTableEnd( Table table ) {}
        public DiagramVisitor visitTableStart( Table table ) { return null; }        
    }
}
