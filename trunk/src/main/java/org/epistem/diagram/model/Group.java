package org.epistem.diagram.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
    
    /**
     * Accept a visitor
     */
    public void accept( DiagramVisitor visitor ) {
        DiagramVisitor childVisitor = visitor.visitGroupStart( this );
        
        if( childVisitor != null ) {
            for( Graphic g : children ) g.accept( childVisitor );
        }
        
        visitor.visitGroupEnd( this );
    }
    
    Group( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
        
        List<OGGraphic> kids = ogg.graphics();
        if( ogg.isSubgraph() ) {            
            kids.remove( kids.size() - 1 );
        }
        
        for( OGGraphic g : kids ) {
            children.add( Graphic.make( g, this, page ) );            
        }
    }    
    
    @Override
    public String toString() {
        return "Group '" + text + "'";
    }
}
