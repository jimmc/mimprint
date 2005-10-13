/* ImagePage.java
 *
 * Jim McBeath, October 7, 2005
 */

package jimmc.jiviewer;

import java.awt.Color;
import java.awt.Component;
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
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.swing.JComponent;

/** A page to display a collection of images in different
 * areas on the page.
 */
public class ImagePage extends JComponent 
        implements ImageWindow, Printable, KeyListener, MouseListener,
        MouseMotionListener, ComponentListener {

    private static final int BORDER_THICKNESS = 10;

    private Viewer viewer;
    private String pageUnit;    //name of our units, e.g. "mil", "mm"
    private int pageWidth;      //width of the page in pageUnits
    private int pageHeight;     //height of the page in pageUnits
    private ImagePageArea[] areas;      //the areas and images to display
    private int currentAreaIndex;       //index into areas of active area
    private Color pageColor;    //color of the "paper"
    private boolean knownKeyPress;
    private String imageInfoText;

    /** Create an ImagePage with no images or layout. */
    public ImagePage(Viewer viewer) {
        super();
        this.viewer = viewer;
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

    public Component getComponent() {
        return this;
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

        //areas = new ImagePageArea[1];
        //areas[0] = new ImagePageArea(500,500,7500,10000); // 0.5 in margins

        areas = new ImagePageArea[4];
        areas[0] = new ImagePageArea(500,500,3500,4750);
        areas[1] = new ImagePageArea(4500,500,3500,4750);
        areas[2] = new ImagePageArea(500,5750,3500,4750);
        areas[3] = new ImagePageArea(4500,5750,3500,4750);

        currentAreaIndex = -1;  //start with nothing selected
    }

    private void repaintCurrentImage() {
        if (currentAreaIndex<0)
            return;
        //repaint(areas[currentAreaIndex].getBounds());
            //TODO - need to scale and translate area bounds
            // into current page coordinates
        repaint();
    }

    public void showImage(ImageBundle imageBundle, String imageInfo) {
        if (currentAreaIndex<0)
            currentAreaIndex = 0;
        imageInfoText = imageInfo;
        areas[currentAreaIndex].setImage(imageBundle);
        repaintCurrentImage();
    }

    /** Advance the current area to the next image, wrap from
     * end back to start. */
    public void advance() {
        currentAreaIndex++;
        if (currentAreaIndex>=areas.length)  //move to next image
            currentAreaIndex = 0;    //wrap back to start
    }

    public void showText(String text) {
        //TODO
    }

    //Override from JComponent
    public boolean isFocusTraversable() {
        return true;            //allow keyboard input
    }

    /** Rotate the current image. */
    public void rotate(int quarters) {
        if (currentAreaIndex<0)
            return;
        ImagePageArea currentArea = areas[currentAreaIndex];
        if (currentArea==null)
            return;
        currentArea.rotate(quarters);
        repaintCurrentImage();
    }

    /** Paint all of our images. */
    public void paint(Graphics g) {
        paint(g,getWidth(),getHeight(),true);
    }

    private void paint(Graphics g, int devWidth, int devHeight, boolean drawOutlines) {
        Graphics2D g2 = (Graphics2D)g;
        scaleAndTranslate(g2,pageWidth,pageHeight,devWidth,devHeight);
            //scale and translate the page to fit the component size
        g2.setColor(getBackground());
        g2.fillRect(0,0,devWidth,devHeight); //clear to background
        g2.setColor(pageColor);
        g2.fillRect(0,0,pageWidth,pageHeight);
        g2.setColor(getForeground());

        //paint each of our image page areas
        for (int n=0; n<areas.length; n++) {
            //pass new g2 to each area to prevent concatenation of transforms
            Graphics2D g2c = (Graphics2D)g2.create();
            areas[n].paint(g2c,BORDER_THICKNESS,n==currentAreaIndex,drawOutlines);
            g2c.dispose();
        }
    }

    /** Given an area of specified size in user space, scale it to fit into
     * the given window space, and translate it to center it top/bottom or
     * left/right for whichever dimension is smaller.
     */
    protected static void scaleAndTranslate(Graphics2D g2, int userWidth, int userHeight,
                int windowWidth, int windowHeight) {
        double xscale = ((double)windowWidth)/((double)userWidth);
        double yscale = ((double)windowHeight)/((double)userHeight);
        double scale = (xscale<yscale)?xscale:yscale;
        if (xscale<yscale)
            g2.translate(0,(yscale-xscale)*userHeight/2);
        else
            g2.translate((xscale-yscale)*userWidth/2,0);
        g2.scale(scale,scale);
    }

    public void setCursorBusy(boolean busy) {
        //TODO
    }

    private void setCursorVisible(boolean visible) {
        //TODO
    }

    /** Print this page if images. */
    public void print() {
	PrinterJob pJob = PrinterJob.getPrinterJob();
	PageFormat pageFormat = pJob.defaultPage();
	//pageFormat = pJob.validatePage(pageFormat);
	Paper oldPaper = pageFormat.getPaper();
	Paper newPaper = new PaperNoMargin();
	newPaper.setSize(oldPaper.getWidth(),oldPaper.getHeight());
	pageFormat.setPaper(newPaper);
	pJob.setPrintable(this,pageFormat);
	if (!pJob.printDialog())
	    return;		//cancelled
	try {
	    pJob.print();
	} catch (PrinterException ex) {
	    throw new RuntimeException(ex);
	}
    }

  //The Printable interface
    public int print(Graphics graphics, PageFormat pageFormat,
			int pageIndex) {
	if (pageIndex!=0)
	    return NO_SUCH_PAGE;
        Paper paper = pageFormat.getPaper();
        int paperWidth = (int)paper.getWidth();
        int paperHeight = (int)paper.getHeight();
	paint(graphics,paperWidth,paperHeight,false);
	return PAGE_EXISTS;
    }
  //End Printable interface

  //The KeyListener interface
    public void keyPressed(KeyEvent ev) {
        setCursorVisible(false);	//turn off cursor on any key
        int keyCode = ev.getKeyCode();
        knownKeyPress = true;	//assume we know it
        switch (keyCode) {
        case KeyEvent.VK_LEFT:
            setCursorVisible(true);
            viewer.moveLeft();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_RIGHT:
            setCursorVisible(true);
            viewer.moveRight();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_DOWN:
            setCursorVisible(true);
            viewer.moveDown();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_UP:
            setCursorVisible(true);
            viewer.moveUp();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_ESCAPE:
            viewer.setScreenMode(Viewer.SCREEN_NORMAL);	//back to normal size
            break;
        default:	//ignore
            knownKeyPress = false;
            break;
        }
    }
    public void keyReleased(KeyEvent ev) {
        int keyCode = ev.getKeyCode();
    }
    public void keyTyped(KeyEvent ev) {
            char ch = ev.getKeyChar();
            switch (ch) {
            case 'a':	//alternate-screen
                    viewer.setScreenMode(Viewer.SCREEN_ALT);
                    break;
            case 'f':	//full-screen
                    viewer.setScreenMode(Viewer.SCREEN_FULL);
                    break;
            case 'L'-0100:	//control-L, refresh
                    //showCurrentImage();               //TODO
                    break;
            case 'e':
                    setCursorVisible(true);	//turn on cursor
                    viewer.showImageEditDialog();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 'i':
                    setCursorVisible(true);	//turn on cursor
                    if (imageInfoText==null)
                            imageInfoText = "(No description)";	//TBD i18n
                    viewer.infoDialog(imageInfoText);
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 'o':	//file-open dialog
                    setCursorVisible(true);	//turn on cursor
                    viewer.processFileOpen();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 'p':	//the print screen
                    viewer.setScreenMode(Viewer.SCREEN_PRINT);
                    break;
            case 'r':	//rotate CCW
                    viewer.rotateCurrentImage(1);
                    break;
            case 'R':	//rotate CW
                    viewer.rotateCurrentImage(-1);
                    break;
            case 'R'-0100:	//control-R, rotate 180
                    viewer.rotateCurrentImage(2);
                    break;
            case 'x':	//exit
                    setCursorVisible(true);	//turn on cursor
                    viewer.processClose();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case '?':
                    setCursorVisible(true);	//turn on cursor
                    viewer.showHelpDialog();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            default:		//unknown key
                    if (!knownKeyPress)
                            getToolkit().beep();
                    break;
            }
    }
  //End KeyListener interface

  //The MouseListener interface
    public void mouseClicked(MouseEvent ev) {}
    public void mouseEntered(MouseEvent ev) {}
    public void mouseExited(MouseEvent ev) {}
    public void mousePressed(MouseEvent ev) {
            requestFocus();
    }
    public void mouseReleased(MouseEvent ev) {}
  //End MouseListener interface

  //The MouseMotionListener interface
    public void mouseDragged(MouseEvent ev){
            setCursorVisible(true); //turn cursor back on
    }
    public void mouseMoved(MouseEvent ev){
            setCursorVisible(true); //turn cursor back on
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

  class PaperNoMargin extends Paper {
        //Force our paper's imageable area to the full size
        public double getImageableHeight() {
            return getHeight();
        }
        public double getImageableWidth() {
            return getWidth();
        }
        public double getImageableX() {
            return 0.0;
        }
        public double getImageableY() {
            return 0.0;
        }
    }
}
