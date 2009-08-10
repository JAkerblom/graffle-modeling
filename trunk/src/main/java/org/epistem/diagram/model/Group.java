package org.epistem.diagram.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.epistem.graffle.OGGraphic;

/**
 * A group of graphics
 *
 * @author nickmain
 */
public class Group extends Shape implements GraphicContainer {

    public final Collection<Graphic> children = new HashSet<Graphic>();

    /** @see java.lang.Iterable#iterator() */
    public Iterator<Graphic> iterator() {
        return children.iterator();
    }
    
    Group( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
        
        for( OGGraphic g : ogg.graphics()) {
            children.add( Graphic.make( g, this, page ) );            
        }
    }    
}
