package org.epistem.diagram.model;

import java.util.*;

import org.epistem.graffle.OGGraphic;

/**
 * A Line
 *
 * @author nickmain
 */
public class Line extends Graphic implements GraphicContainer, Connector {

    TreeMap<Double, Shape> labelMap = new TreeMap<Double, Shape>();

    public Graphic head;
    public Graphic tail;
    
    /** Ordered, from tail to head */
    public Collection<Shape> labels = labelMap.values();            
    
    /** @see org.epistem.diagram.model.Connector#getHead() */
    public Graphic getHead() {
        return head;
    }
    
    /** @see org.epistem.diagram.model.Connector#getTail() */
    public Graphic getTail() {
        return tail;
    }

    /** @see java.lang.Iterable#iterator() */
    public Iterator<Graphic> iterator() {
        return new ArrayList<Graphic>( labels ).iterator();
    }
    
    Line( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
    }

    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        head = page.graphics.get( ogg.headId() );
        tail = page.graphics.get( ogg.tailId() );
        
        if( head != null ) head.incoming.add( this );
        if( tail != null ) tail.outgoing.add( this );
    }
}
