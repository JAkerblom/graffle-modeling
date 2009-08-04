package org.epistem.graffle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import org.epistem.graffle.OGGraphic.GraphicClass;

/**
 * An OmniGraffle document
 *
 * @author nickmain
 */
@SuppressWarnings("unchecked")
public final class OmniGraffleDoc {

    private final File file;
    private final Map<String, Object> plist;    

    private Map<Integer,OGGraphic> graphics;
    
    /**
     * @param file the document file
     */
    public OmniGraffleDoc( File file ) throws Exception {
        this.file = file;
        plist = (Map<String, Object>) PListParser.parse( file );
    }
    
    /**
     * Get the file
     */
    public File file() {
        return file;
    }
    
    /**
     * Find a graphic by id
     * @return may be null
     */
    public OGGraphic getGraphic( int id ) {
        if( graphics == null ) {
            graphics = new HashMap<Integer, OGGraphic>();
            
            for( OGSheet sheet : sheets() ) {
                registerGraphics( sheet.graphics() );
            }
        }
            
        return graphics.get( id );
    }
    
    private void registerGraphics( List<OGGraphic> gg ) {
        for( OGGraphic graphic : gg ) {
            graphics.put( graphic.id(), graphic );
            
            OGGraphic.GraphicClass type = graphic.graphicClass();
            if( type == GraphicClass.Group || type == GraphicClass.TableGroup ) {
                registerGraphics( graphic.graphics() );
            }
        }  
    }
    
    /**
     * Read the image with the given id 
     */
    public BufferedImage readImage( int id ) throws IOException {
        if( ! file.isDirectory() ) return null;
        
        File imageFile = new File( file, "image" + id + ".tiff" );        
        return ImageIO.read( imageFile );
    }
    
    /**
     * Get the document creation date
     */
    public String creationDate() { return (String) plist.get( "CreationDate" ); }

    /**
     * Get the document creator (author)
     */
    public String creator() { return (String) plist.get( "Creator" ); }

    /**
     * Get the document modification date
     */
    public String modificationDate() { return (String) plist.get( "ModificationDate" ); }

    /**
     * Get the document modifier (author)
     */
    public String modifier() { return (String) plist.get( "Modifier" ); }
    
    private List<String> userInfoList( String key ) {
        List<String> ss = null;
        
        Map<String,?> userInfo = (Map<String, ?>) plist.get( "UserInfo" );
        if( userInfo != null ) {
            ss = (List<String>) userInfo.get( key );
        }
        if( ss == null ) return Collections.emptyList();
        return ss;
    }

    private String userInfoString( String key ) {
        String s = null;
        
        Map<String,?> userInfo = (Map<String, ?>) plist.get( "UserInfo" );
        if( userInfo != null ) {
            s = (String) userInfo.get( key );
        }
        if( s == null ) return "";
        return s;
    }
    
    public List<String> authors() { return userInfoList( "kMDItemAuthors" ); }
    public List<String> keywords() { return userInfoList( "kMDItemKeywords" ); }
    public List<String> languages() { return userInfoList( "kMDItemLanguages" ); }
    public List<String> organizations() { return userInfoList( "kMDItemOrganizations" ); }
    public List<String> projects() { return userInfoList( "kMDItemProjects" ); }

    public String comments() { return userInfoString( "kMDItemComments" ); }
    public String copyright() { return userInfoString( "kMDItemCopyright" ); }
    public String description() { return userInfoString( "kMDItemDescription" ); }
    public String subject() { return userInfoString( "kMDItemSubject" ); }
    public String version() { return userInfoString( "kMDItemVersion" ); }
    
    /**
     * Get the sheets
     */
    public List<OGSheet> sheets() {
        List<Object> sheets = (List<Object>) plist.get( "Sheets" );
        
        if( sheets == null ) {
            sheets = new ArrayList<Object>();
            sheets.add( plist );
        }
        
        List<OGSheet> ogsheets = new ArrayList<OGSheet>();
        for( Object dict : sheets ) {
            ogsheets.add( new OGSheet( this, (Map<String,Object>) dict ) );
        }
        
        return ogsheets;
    }
}
