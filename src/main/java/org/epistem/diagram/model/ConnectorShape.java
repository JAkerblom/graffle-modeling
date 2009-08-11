package org.epistem.diagram.model;

import org.epistem.graffle.OGGraphic;

/**
 * A shape that can connect
 *
 * @author nickmain
 */
public class ConnectorShape extends Shape implements Connector {
    
    public Graphic head;
    public Graphic tail;
    
    /** @see org.epistem.diagram.model.Connector#getHead() */
    public Graphic getHead() {
        return head;
    }
    
    /** @see org.epistem.diagram.model.Connector#getTail() */
    public Graphic getTail() {
        return tail;
    }
    
    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
       visitor.visitConnectorShape( this );
    }
    
    ConnectorShape( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
    }
    
    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        super.init();
        
        head = page.graphics.get( ogg.headId() );
        tail = page.graphics.get( ogg.tailId() );
        
        if( head != null ) head.incoming.add( this );
        if( tail != null ) tail.outgoing.add( this );
    }

}
