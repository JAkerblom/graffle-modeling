package org.epistem.graffle;

import java.io.StringReader;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * Utility methods
 *
 * @author nickmain
 */
public class OGUtils {

    /**
     * Extract text from RTF
     */
    static String unRTF( String s ) {
        if( s == null ) return null;
        
        DefaultStyledDocument doc = new DefaultStyledDocument();
        RTFEditorKit kit = new RTFEditorKit();
        
        try {
            kit.read( new StringReader( s ), doc, 0 );
        }
        catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
        
        try {
            s = doc.getText( 0, doc.getLength() );
        } catch( BadLocationException e ) {
            throw new RuntimeException( e );
        }
        
        return s;
    }
}
