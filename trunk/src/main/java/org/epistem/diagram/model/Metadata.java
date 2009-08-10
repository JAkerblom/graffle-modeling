package org.epistem.diagram.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Page or graphic metadata
 *
 * @author nickmain
 */
public class Metadata {

    /** Null if none or blank */
    public final String notes;
    
    /** Not null */
    public final Map<String,String> properties;
    
    Metadata( String notes, Map<String,String> properties ) {
        if( notes != null ) {
            notes = notes.trim();
            if( notes.length() == 0 ) notes = null;
        }
        this.notes = notes;
        
        if( properties == null ) properties = new HashMap<String, String>();
        this.properties = Collections.unmodifiableMap( properties );
    }    
}
