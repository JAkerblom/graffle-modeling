package org.epistem.graffle;

import java.util.Map;

/**
 * A layer
 *
 * @author nickmain
 */
public class OGLayer {

    private final Map<String, Object> dict;
    
    OGLayer( Map<String, Object> dict ) {
        this.dict = dict;
    }
    
    /**
     * The layer name
     */
    public String name() {
        return (String) dict.get( "Name" );
    }
    
    /**
     * Whether the layer is visible
     */
    public boolean visible() {
        return "YES".equalsIgnoreCase( ((String) dict.get( "View" )));
    }
    
    @Override
    public String toString() {
        return "Layer " + name() 
               + (visible() ? " (visible)" : " (hidden)" );
    }
}
