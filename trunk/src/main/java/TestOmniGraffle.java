import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;

import org.epistem.graffle.OGGraphic;
import org.epistem.graffle.OGLayer;
import org.epistem.graffle.OGSheet;
import org.epistem.graffle.OmniGraffleDoc;
import org.epistem.io.IndentingPrintWriter;


public class TestOmniGraffle {

    private final static IndentingPrintWriter out = IndentingPrintWriter.SYSOUT;
    
    private static OmniGraffleDoc og;
    
    private static List<BufferedImage> images = new ArrayList<BufferedImage>();
    private static JFrame frame;
    
    public static void main(String[] args) throws Exception {
        
        og = new OmniGraffleDoc( new File( "test-diagrams/test.graffle" ) );
        
        out.println( "Created  --> " + og.creationDate() );
        out.println( "Creator  --> " + og.creator() );
        out.println( "Mod date --> " + og.modificationDate() );
        out.println( "Modifier --> " + og.modifier() );
        
        out.println( "Authors       --> " + og.authors() );
        out.println( "Keywords      --> " + og.keywords() );
        out.println( "Languages     --> " + og.languages() );
        out.println( "Organizations --> " + og.organizations() );
        out.println( "Projects      --> " + og.projects() );
        
        out.println( "Copyright     --> " + og.copyright() );
        out.println( "Description   --> " + og.description() );
        out.println( "Subject       --> " + og.subject() );
        out.println( "Version       --> " + og.version() );
        out.println( "Comments      --> " + og.comments() );
        
        for( OGSheet sheet : og.sheets() ) {
            out.println( "Sheet --> " + sheet.title() );
            out.indent();
            out.println( "Unique id --> " + sheet.id() );
            for( OGLayer layer : sheet.layers ) out.println( layer );
            
            String note = sheet.notes();
            if( note != null ) {
                out.println( "Note --> " + note );
            }
            
            Map<String,String> props = sheet.userProperties();
            if( props != null ) {
                for( String key : props.keySet() ) {
                    out.println( "prop " + key + " = " + props.get( key ));
                }
            }
            
            for( OGGraphic graphic : sheet.graphics() ) {
                printGraphic( graphic );
            }
            
            out.unindent();
        }
        
        out.flush();
        
        if( frame != null ) frame.setVisible( true );
    }
    
    private static void printGraphic( OGGraphic graphic ) throws IOException {
        out.println( graphic.id() + " --> " + graphic.graphicClass() );
        out.indent();
        
        switch( graphic.graphicClass() ) {
            case Group:
            case TableGroup:
                if( graphic.isSubgraph() ) out.println( "isSubgraph" );                
                for( OGGraphic g : graphic.graphics() ) {
                    printGraphic( g );
                }
                break;
                
            case LineGraphic:
                out.println( "head : " + graphic.headArrow() );
                out.println( "tail : " + graphic.tailArrow() );
                out.println( "from " + graphic.tailId() + " to " + graphic.headId() );                
                for( Point2D p : graphic.points() ) {
                    out.println( "{ " + p.getX() + ", " + p.getY() + " }" );
                }                
                break;
                
            case ShapedGraphic:                
                out.println( "shape=" + graphic.shape() + " " + graphic.bounds() );
                int label = graphic.labelLineId();
                if( label > 0 ) {
                    out.println( "Label for " + label + " at " + graphic.labelPosition() );
                }

                out.println( "from " + graphic.tailId() + " to " + graphic.headId() );                
                out.println( "text --> " + graphic.text() );
                
//                DefaultStyledDocument doc = graphic.styledText();
//                if( doc != null ) {
//                    out.println( "style text --> ");
//                    printElement( doc.getDefaultRootElement());
//                }
                break;
        }
        
        String note = graphic.notes();
        if( note != null ) {
            out.println( "Note --> " + note );
        }
        
        Map<String,String> props = graphic.userProperties();
        if( props != null ) {
            for( String key : props.keySet() ) {
                out.println( "prop " + key + " = " + props.get( key ));
            }
        }
        
        Integer imgId = graphic.imageId();
        if( false ) {// imgId != null ) {
            out.println( "Image --> " + imgId );
            if( frame == null ) {
            
                Canvas canvas = new Canvas() {
                    @Override public void paint( Graphics g ) {
                        int x = 0;
                        
                        for( BufferedImage image : images ) {
                            if( image == null ) continue;
                            g.drawImage( image, x, 0, null );    
                            x += image.getWidth();
                            
                            if( frame.getHeight() < image.getHeight() ) frame.setSize( frame.getWidth(), image.getHeight() );
                        }
                        
                        if( frame.getWidth() < x ) frame.setSize( x + 10, frame.getHeight() );                        
                    } 
                };
                
                frame = new JFrame( "Images" );
                frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
                frame.setSize( 500, 500 );
                frame.getContentPane().add( canvas );
                canvas.setSize( 500, 500 );
            }
            
            BufferedImage img = og.readImage( imgId );
            images.add( img );
        }
        
        out.println( graphic.layer() );
        
        out.unindent();        
    }
    
    private static void printElement( Element elem ) {
        String tag = elem.getName();
        out.startElement( tag );
        
        AttributeSet attrs = elem.getAttributes();
        for( Enumeration<?> names = attrs.getAttributeNames(); names.hasMoreElements(); ) {
            Object name = names.nextElement();
            
            out.attribute( name.toString(), attrs.getAttribute( name ) );
        }
        
        if( elem.isLeaf() ) {
            int start = elem.getStartOffset();
            int end   = elem.getEndOffset();
            try {
                String text = elem.getDocument().getText( start, end - start );
                StringBuilder buff = new StringBuilder();
                for( char c : text.toCharArray() ) {
                    if( c > 127 || c < 32 ) {
                        buff.append( "{" );
                        buff.append( (int) c );
                        buff.append( "}" );
                    }
                    else {
                        buff.append( c );
                    }
                }
                text = buff.toString();
                
                out.write( text );
            } catch( BadLocationException e ) {
                throw new RuntimeException( e );
            }
        }
        else {
            int count = elem.getElementCount();
            for( int i = 0; i < count; i++ ) {
                Element child = elem.getElement( i );
                printElement( child );
            }
        }
        
        out.endElement();
    }
}
