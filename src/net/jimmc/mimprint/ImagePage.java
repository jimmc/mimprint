/* ImagePage.java
 *
 * Jim McBeath, October 7, 2005
 */

package jimmc.jiviewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** A page to display a collection of images in different
 * areas on the page.
 */
public class ImagePage extends JComponent 
        implements KeyListener, MouseListener,
        MouseMotionListener, ComponentListener {

    private static final int BORDER_THICKNESS = 10;

    private String pageUnit;    //name of our units, e.g. "mil", "mm"
    private int pageWidth;      //width of the page in pageUnits
    private int pageHeight;     //height of the page in pageUnits
    private ImagePageArea[] areas;      //the areas and images to display
    private int currentAreaIndex;       //index into areas of active area
    private Color pageColor;    //color of the "paper"

    /** Create an ImagePage with no images or layout. */
    public ImagePage() {
        super();
        setBackground(Color.gray);      //neutral background
        setForeground(Color.black);     //black text
        setPageColor(Color.white);      //white "paper"
        setPreferredSize(new Dimension(425,550));
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
        setDefaultLayout();
    }

    public void setPageColor(Color pageColor) {
        if (pageColor==null)
            pageColor = Color.white;
        this.pageColor = pageColor;
    }

    public Color getPageColor() {
        return pageColor;
    }

    /** Set up the default area layout. */
    private void setDefaultLayout() {
        pageUnit = "mil";     // 1/10000 of an inch
        pageWidth = 8500;       //American standard paper size
        pageHeight = 11000;
        areas = new ImagePageArea[1];
        areas[0] = new ImagePageArea(500,500,7500,10000); // 0.5 in margins
        currentAreaIndex = 0;
    }

    public void setCurrentImage(ImageBundle imageBundle) {
        areas[currentAreaIndex].setImage(imageBundle);
        repaint(areas[currentAreaIndex].getBounds());
    }

    //Override from JComponent
    public boolean isFocusTraversable() {
        return true;            //allow keyboard input
    }

    /** Paint all of our images. */
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        int height = getHeight();       //component size
        int width = getWidth();
        g2.setColor(getBackground());
        g2.fillRect(0,0,getWidth(),getHeight()); //clear to background
        double xscale = ((double)width)/((double)pageWidth);
        double yscale = ((double)height)/((double)pageHeight);
        double scale = (xscale<yscale)?xscale:yscale;
        if (xscale<yscale)
            g2.translate(0,(yscale-xscale)*pageHeight/2);
        else
            g2.translate((xscale-yscale)*pageWidth/2,0);
        g2.scale(scale,scale);
        g2.setColor(pageColor);
        g2.fillRect(0,0,pageWidth,pageHeight);
        g2.setColor(getForeground());

        //paint each of our image page areas
        for (int n=0; n<areas.length; n++)
            areas[n].paint(g2,BORDER_THICKNESS);
    }

  //The KeyListener interface
    public void keyPressed(KeyEvent ev) {
        //TODO - handle key press
    }
    public void keyReleased(KeyEvent ev) {
        //ignore
    }
    public void keyTyped(KeyEvent ev) {
        //TODO - handle key typed
    }
  //End KeyListener interface

  //The MouseListener interface
    public void mouseClicked(MouseEvent ev) {}
    public void mouseEntered(MouseEvent ev) {}
    public void mouseExited(MouseEvent ev) {}
    public void mousePressed(MouseEvent ev) {
            //requestFocus();
    }
    public void mouseReleased(MouseEvent ev) {}
  //End MouseListener interface

  //The MouseMotionListener interface
    public void mouseDragged(MouseEvent ev){
            //setCursorVisible(true); //turn cursor back on
    }
    public void mouseMoved(MouseEvent ev){
            //setCursorVisible(true); //turn cursor back on
    }
  //End MouseMotionListener interface

  //The ComponentListener interface
    public void componentHidden(ComponentEvent ev){}
    public void componentMoved(ComponentEvent ev){}
    public void componentResized(ComponentEvent ev){
        //app.debugMsg("componentResized");
        //repaint();
    }
    public void componentShown(ComponentEvent ev){}
  //End ComponentListener interface
}
