package org.epistem.diagram.model;

import java.util.*;

import org.epistem.graffle.OGGraphic;
import org.epistem.graffle.OGSheet;

/**
 * A page within a document
 *
 * @author nickmain
 */
public class Page implements GraphicContainer {

    public final String title;
    public final Collection<Graphic> rootGraphics = new HashSet<Graphic>();
    public final Metadata metadata;
    public final Diagram  diagram;
    
    /** @see java.lang.Iterable#iterator() */
    public Iterator<Graphic> iterator() {
        return rootGraphics.iterator();
    }
    
    Map<Integer, Graphic> graphics;
    
    Page( OGSheet sheet, Diagram diagram ) {
        
        title = sheet.title();
        this.diagram = diagram;
        metadata = new Metadata( sheet.notes(), sheet.userProperties() );
        
        graphics = new HashMap<Integer, Graphic>();
        for( OGGraphic g : sheet.graphics()) {
            rootGraphics.add( Graphic.make( g, this, this ) );            
        }
        
        for( Graphic g : graphics.values() ) {
            g.init();
            g.ogg = null;
        }
        
        graphics = null;
        
        for( Graphic g : rootGraphics ) {
            if( ! (g instanceof Shape )) continue;
            Shape s = (Shape) g;
            
            for( Graphic g2 : rootGraphics ) {
                if( g == g2 ) continue;                
                if( ! (g2 instanceof Shape )) continue;
                Shape s2 = (Shape) g2;
                
                if( s.bounds.intersects( s2.bounds ) ) {
                    s.intersectingShapes.add( s2 );
                    s2.intersectingShapes.add( s );
                }
                
                if( s.bounds.contains( s2.bounds ) ) {
                    s.containedShapes.add( s2 );
                    s2.containingShapes.add( s );
                }

                if( s2.bounds.contains( s.bounds ) ) {
                    s2.containedShapes.add( s );
                    s.containingShapes.add( s2 );
                }
            }
        }
    }       
}
