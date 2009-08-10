package org.epistem.diagram.model;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.text.DefaultStyledDocument;

import org.epistem.graffle.OGGraphic;

/**
 * A shape
 *
 * @author nickmain
 */
public class Shape extends Graphic {

    public final String text;
    public final DefaultStyledDocument richText;
    public final Rectangle2D bounds;
    public final Collection<Shape> containedShapes    = new HashSet<Shape>();
    public final Collection<Shape> intersectingShapes = new HashSet<Shape>();
    public final Collection<Shape> containingShapes   = new HashSet<Shape>();
    
    Shape( OGGraphic ogg, GraphicContainer parent, Page page ) {
        super( ogg, parent, page );
        
        this.text = ogg.toString();
        this.richText = ogg.styledText();
        this.bounds = ogg.bounds();
    }

    /** @see org.epistem.diagram.model.Graphic#init() */
    @Override
    void init() {
        if( ogg.labelLineId() != 0 ) {
            Line line = (Line) page.graphics.get( ogg.labelLineId() );
            line.labelMap.put( ogg.labelPosition(), this );
            
            parent = line;            
            page.rootGraphics.remove( this );
        }        
    }
}
