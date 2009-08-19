package org.epistem.diagram.model.component.graph;

import org.epistem.diagram.model.Graphic;

/**
 * Base for components that handle diagram graphics
 *
 * @author nickmain
 */
public class DiagramComponent {

    public final Graphic graphic;
    protected final ModelGenerationContext context;
    
    protected DiagramComponent( Graphic graphic, ModelGenerationContext context ) {
        this.graphic = graphic;
        this.context = context;
    }
    
    public void initialize() {
        //to be overridden
    }
}
