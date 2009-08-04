package org.epistem.graffle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.epistem.graffle.OGUtils.*;

/**
 * An omnigraffle sheet
 *
 * @author nickmain
 */
@SuppressWarnings("unchecked")
public final class OGSheet {

    private final Map<String,Object> dict;

    /**
     * The layers
     */
    public final OGLayer[] layers;
    
    public final OmniGraffleDoc document;
    
    OGSheet( OmniGraffleDoc document, Map<String,Object> dict ) {
        this.dict = dict;
        this.document = document;
        
        List<Map<String,Object>> layerMaps = (List<Map<String,Object>>) dict.get( "Layers" );
        layers = new OGLayer[ layerMaps.size() ];
        for( int i = 0; i < layers.length; i++ ) {
            layers[i] = new OGLayer( layerMaps.get( i ) );
        }
    }

    /**
     * The sheet title
     */
    public String title() {
        return (String) dict.get( "SheetTitle" );
    }
    
    /**
     * Get the graphics
     */
    public List<OGGraphic> graphics() {
        List<OGGraphic> oggraphics = new ArrayList<OGGraphic>();
        
        List<Object> graphics = (List<Object>) dict.get( "GraphicsList" );
        for( Object dict : graphics ) {
            oggraphics.add( new OGGraphic( this, null, (Map<String,Object>) dict ) );
        }
        
        return oggraphics;
    }
    
    /**
     * Get the sheet's unique id
     */
    public int id() {
        return (Integer) dict.get( "UniqueID" );
    }
    
    /**
     * Get the sheet notes
     */
    public String notes() {
        Map<String,Object> bg = (Map<String,Object>) dict.get( "BackgroundGraphic" );
        String s = unRTF( (String) bg.get( "Notes" ));
        if( s == null ) return null;
        if( s.endsWith( "\n" ) ) s = s.substring( 0, s.length() - 1 );
        return s;
    }
    
    /**
     * Get the user defined properties
     */
    public Map<String,String> userProperties() {
        Map<String,Object> bg = (Map<String,Object>) dict.get( "BackgroundGraphic" );
        return (Map<String,String>) bg.get( "UserInfo" );
    }
}
