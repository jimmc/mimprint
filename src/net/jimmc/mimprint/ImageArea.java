/* ImageArea.java
 *
 * Jim McBeath, September 18, 2001
 */

package net.jimmc.mimprint;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** A window in which we view our images.
 */
public class ImageArea extends JLabel implements ImageWindow {
    /** Our App. */
    protected App app;

    /** Our Viewer. */
    protected Viewer viewer;

    /** Our current ImageBundle. */
    protected ImageBundle currentImage;

    /** The info text about the current image. */
    protected String imageInfoText;

    /** An invisible cursor. */
    protected Cursor invisibleCursor;

    /** A busy cursor. */
    protected Cursor busyCursor;

    /** Flag telling us the visible-cursor state. */
    protected boolean cursorVisible;

    /** Flag telling us the busy-cursor state. */
    protected boolean cursorBusy;

    /** True when we get a key press we recognize. */
    protected boolean knownKeyPress;

    /** Our worker thread. */
    protected Worker worker;

    /** True if our image should be scaled to fit the window. */
    protected boolean scaled;

    private RotationListener rotationListener;
    
    /** Create an ImageArea. */
    public ImageArea(App app, Viewer viewer) {
        super();
        this.app = app;
        this.viewer = viewer;
        scaled = true;
        setBackground(Color.gray);    //set up neutral background
        setForeground(Color.white);    //and color for status info
        setPreferredSize(new Dimension(800,600));
        setHorizontalAlignment(CENTER);
        if (app.useBigFont()) {
            Font biggerFont = new Font("Serif",Font.PLAIN,50);
            if (biggerFont!=null)
                setFont(biggerFont);
        }
        addKeyListener(new ImageAreaKeyListener());
        addMouseListener(new ImageAreaMouseListener());
        addMouseMotionListener(new ImageAreaMouseMotionListener());
        addComponentListener(new ImageAreaComponentListener());
                initCursors();

        worker = new Worker();
        worker.setPriority(worker.getPriority()-1);
        worker.start();
    }

    public void setRotationListener(RotationListener rl) {
        rotationListener = rl;
    }

    private void initCursors() {
        Toolkit toolkit = getToolkit();
        Image cursorImage = toolkit.createImage(new byte[0]);
        invisibleCursor = toolkit.createCustomCursor(
                cursorImage,new Point(0,0),"invisible");
        busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    }

    public Component getComponent() {
        return this;
    }

    /** Show text instead of an image. */
    public void showText(String text) {
        setIcon(null);
        setText(text);
    }

    /** Show a rendered image, set up text info about the image.
     */
    public void showImage(ImageBundle imageBundle, String imageInfo) {
        app.debugMsg("showImage "+imageBundle);
        if (SwingUtilities.isEventDispatchThread()) {
            //Run this outside the event thread
            Object[] data = { this, imageBundle, imageInfo };
            worker.invoke(new WorkerTask(data) {
                public void run() {
                    Object[] rData = (Object[])getData();
                    ImageArea a = (ImageArea)rData[0];
                    a.showImage(
                        (ImageBundle)rData[1],
                        (String)rData[2]);
                }
            });
            return;
        }

        //At this point we are not in the event thread
        currentImage = imageBundle;
        imageInfoText = imageInfo;
        showCurrentImage();    //rotate, scale, and display
        revalidate();
    }

    /** Redisplay the current image. */
    public void showCurrentImage() {
        if (SwingUtilities.isEventDispatchThread()) {
            //Run this outside the event thread
            worker.invoke(new WorkerTask(this) {
                public void run() {
                    ImageArea a = (ImageArea)getData();
                    a.showCurrentImage();
                }
            });
            return;
        }

        setIcon(null);
        if (currentImage==null) {
            setText("No image");    //i18n
            return;        //nothing there
        }
        setText("Loading image...");    //i18n

        //make sure the image size is correct
        if (scaled)
            currentImage.setDisplaySize(getWidth(),getHeight());
        else
            currentImage.setDisplaySize(0,0); //no scaling

        app.debugMsg("ShowCurrentImage A");
        Image xImage = currentImage.getTransformedImage();
        if (xImage==null) {
            setText("Null image");
            return;
        }
        app.debugMsg("ShowCurrentImage B");
        ImageIcon ii = new ImageIcon(xImage);
        app.debugMsg("ShowCurrentImage C");
        setIcon(ii);
        setText(null);
    }

    /** Rotate the current image in increments of 90 degrees. */
    public void rotate(int inc) {
        currentImage.rotate(inc);
        showCurrentImage();
        if (rotationListener!=null)
            rotationListener.rotate(inc);
    }

    /** We return true to allow keyboard focus and thus input. */
    public boolean isFocusTraversable() {
        return true;    //allow keyboard input
    }

    /** Set the cursor to a busy cursor. */
    public void setCursorBusy(boolean busy) {
        cursorBusy = busy;
        if (busy) {
            setCursor(busyCursor);
        } else {
            setCursorVisible(cursorVisible);
        }
    }

    /** Make the cursor visible or invisible.
     * If busy-cursor has been set, cursor is always visible.
     */
    public void setCursorVisible(boolean visible) {
        cursorVisible = visible;
        if (cursorBusy)
            return;        //busy takes priority over invisible
        if (visible)
            setCursor(null);
        else
            setCursor(invisibleCursor);
    }

    class ImageAreaKeyListener implements KeyListener {
        //The KeyListener interface
        public void keyPressed(KeyEvent ev) {
            setCursorVisible(false);    //turn off cursor on any key
            int keyCode = ev.getKeyCode();
            knownKeyPress = true;    //assume we know it
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
                viewer.restorePreviousScreenMode();
                break;
                    case KeyEvent.VK_ENTER:
                            viewer.activateSelection();
                            break;
            default:    //ignore
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
            case ' ':       //activate selection
                viewer.activateSelection();
                break;
            case 'a':    //alternate-screen
                viewer.setScreenMode(Viewer.SCREEN_ALT);
                break;
            case 'e':       //edit image text
                setCursorVisible(true);    //turn on cursor
                viewer.showImageEditDialog();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'f':    //full-screen
                viewer.setScreenMode(Viewer.SCREEN_FULL);
                break;
            case 'i':       //show image info
                setCursorVisible(true);    //turn on cursor
                viewer.showImageInfoDialog();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'o':    //file-open dialog
                setCursorVisible(true);    //turn on cursor
                viewer.processFileOpen();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'p':   //add current image to active or printable playlist
                viewer.addCurrentImageToPlayList();
                break;
            case 'P':    //show printable screen
                viewer.setScreenMode(Viewer.SCREEN_PRINT);
                break;
            case 'r':    //rotate CCW
                viewer.rotateCurrentImage(1);
                break;
            case 'R':    //rotate CW
                viewer.rotateCurrentImage(-1);
                break;
            case 'R'-0100:    //control-R, rotate 180
                viewer.rotateCurrentImage(2);
                break;
            case 's':    //slideshow mode
                viewer.setScreenMode(Viewer.SCREEN_SLIDESHOW);
                break;
            case 'S':    //toggle "scaled" flag
                scaled = !scaled;
                showCurrentImage();
                break;
            case 'x':    //exit
                setCursorVisible(true);    //turn on cursor
                viewer.processClose();
                setCursorVisible(false);    //turn cursor back off
                break;
            case '?':
                setCursorVisible(true);    //turn on cursor
                viewer.showHelpDialog();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'L'-0100:    //control-L, refresh
                showCurrentImage();
                break;
            default:        //unknown key
                if (!knownKeyPress)
                    getToolkit().beep();
                break;
            }
        }
        //End KeyListener interface
    }

    class ImageAreaMouseListener implements MouseListener {
        //The MouseListener interface
        public void mouseClicked(MouseEvent ev) {}
        public void mouseEntered(MouseEvent ev) {}
        public void mouseExited(MouseEvent ev) {}
        public void mousePressed(MouseEvent ev) {
            requestFocus();
        }
        public void mouseReleased(MouseEvent ev) {}
        //End MouseListener interface
    }

    class ImageAreaMouseMotionListener implements MouseMotionListener {
        //The MouseMotionListener interface
        public void mouseDragged(MouseEvent ev){
            setCursorVisible(true);    //turn cursor back on
        }
        public void mouseMoved(MouseEvent ev){
            setCursorVisible(true);    //turn cursor back on
        }
        //End MouseMotionListener interface
    }

    class ImageAreaComponentListener implements ComponentListener {
        //The ComponentListener interface
        public void componentHidden(ComponentEvent ev){}
        public void componentMoved(ComponentEvent ev){}
        public void componentResized(ComponentEvent ev){
            app.debugMsg("componentResized");
            showCurrentImage();
        }
        public void componentShown(ComponentEvent ev){}
        //End ComponentListener interface
    }
}

/* end */
