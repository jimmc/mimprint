/* ImageArea.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import java.awt.Color;
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
import java.awt.image.RenderedImage;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import javax.media.jai.GraphicsJAI;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** A window in which we view our images.
 */
public class ImageArea extends JLabel
		implements KeyListener, MouseListener,
		MouseMotionListener, ComponentListener {
	/** Our App. */
	protected App app;

	/** Our Viewer. */
	protected Viewer viewer;

	/** Our ImageLister. */
	protected ImageLister imageLister;

	/** Our current ImageBundle. */
	protected ImageBundle currentImage;

	/** The current rendered image source. */
	protected RenderedImage renderedImageSource;

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

	/** Create an ImageArea. */
	public ImageArea(App app, Viewer viewer) {
		super();
		this.app = app;
		this.viewer = viewer;
		setBackground(Color.gray);	//set up neutral background
		setForeground(Color.white);	//and color for status info
		setPreferredSize(new Dimension(800,600));
		setHorizontalAlignment(CENTER);
		if (app.useBigFont()) {
			Font biggerFont = new Font("Serif",Font.PLAIN,50);
			if (biggerFont!=null)
				setFont(biggerFont);
		}
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		Toolkit toolkit = getToolkit();
		Image cursorImage = toolkit.createImage(new byte[0]);
		invisibleCursor = toolkit.createCustomCursor(
				cursorImage,new Point(0,0),"");
		busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

		worker = new Worker();
		worker.setPriority(worker.getPriority()-1);
		worker.start();
	}

	/** If we have a renderedImageSource, we paint that,
	 * otherwise we let the JLabel do its thing.
	 */
	public void paint(Graphics g) {
		if (renderedImageSource==null) {
			super.paint(g);
			return;
		}
		app.debugMsg("paint A");
		GraphicsJAI gj = GraphicsJAI.createGraphicsJAI(
			(Graphics2D)g,this);
		AffineTransform tx = new AffineTransform();
		int rw = renderedImageSource.getWidth();
		int rh = renderedImageSource.getHeight();
		double xScale = getWidth()/(double)rw;
		double yScale = getHeight()/(double)rh;
		double scale = (xScale<yScale)?xScale:yScale;
			//make the whole image fit in the display
		tx.scale(scale,scale);
		app.debugMsg("paint B");
		gj.drawRenderedImage(renderedImageSource,tx);
		app.debugMsg("paint C");
	}

	/** Set our image lister. */
	public void setImageLister(ImageLister imageLister) {
		this.imageLister = imageLister;
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
		showCurrentImage();	//rotate, scale, and display
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
			setText("No image");	//i18n
			return;		//nothing there
		}
		setText("Loading image...");	//i18n

		currentImage.setDisplaySize(getWidth(),getHeight());
			//make sure the image size is correct

		if (app.useJAI()) {
			app.debugMsg("ShowCurrentImage X");
			renderedImageSource =
				currentImage.getTransformedRenderedImage();
			repaint();
		} else {
			app.debugMsg("ShowCurrentImage A");
			Image xImage = currentImage.getTransformedImage();
			app.debugMsg("ShowCurrentImage B");
			ImageIcon ii = new ImageIcon(xImage);
			app.debugMsg("ShowCurrentImage C");
			setIcon(ii);
		}
		setText(null);
	}

	/** Rotate the current image in increments of 90 degrees. */
	public void rotate(int inc) {
		currentImage.rotate(inc);
		showCurrentImage();
	}

	/** We return true to allow keyboard focus and thus input. */
	public boolean isFocusTraversable() {
		return true;	//allow keyboard input
	}

	/** Put up a help dialog. */
	public void showHelpDialog() {
		String helpText = app.getResourceString("info.ImageHelp");
		viewer.infoDialog(helpText);
	}

	/** Put up a dialog showing the image info. */
	public void showImageInfoDialog() {
		if (imageInfoText==null)
			imageInfoText = "(No description)";	//TBD i18n
		viewer.infoDialog(imageInfoText);
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
			return;		//busy takes priority over invisible
		if (visible)
			setCursor(null);
		else
			setCursor(invisibleCursor);
	}

    //The KeyListener interface
    	public void keyPressed(KeyEvent ev) {
		setCursorVisible(false);	//turn off cursor on any key
		int keyCode = ev.getKeyCode();
		knownKeyPress = true;	//assume we know it
		switch (keyCode) {
		case KeyEvent.VK_DOWN:
			if (imageLister!=null)
				imageLister.down();
			break;
		case KeyEvent.VK_UP:
			if (imageLister!=null)
				imageLister.up();
			break;
		case KeyEvent.VK_ESCAPE:
			viewer.setFullScreen(false);	//back to normal size
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
		case 'f':	//full-screen
			viewer.setFullScreen(true);
			break;
		case 'L'-0100:	//control-L, refresh
			showCurrentImage();
			break;
		//TBD - add e to edit the text in the accopanying .txt file
		//TBD - add i to view the text in the accopanying .txt file
		//      in a popup dialog or superimposed on the image
		case 'i':
			setCursorVisible(true);	//turn on cursor
			showImageInfoDialog();
			setCursorVisible(false);	//turn cursor back off
			break;
		case 'o':	//file-open dialog
			setCursorVisible(true);	//turn on cursor
			viewer.processFileOpen();
			setCursorVisible(false);	//turn cursor back off
			break;
		case 'r':	//rotate CCW
			rotate(1);
			break;
		case 'R':	//rotate CW
			rotate(-1);
			break;
		case 'R'-0100:	//control-R, rotate 180
			rotate(2);
			break;
		case 'x':	//exit
			setCursorVisible(true);	//turn on cursor
			viewer.processClose();
			setCursorVisible(false);	//turn cursor back off
			break;
		case '?':
			setCursorVisible(true);	//turn on cursor
			showHelpDialog();
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
		setCursorVisible(true);	//turn cursor back on
	}
	public void mouseMoved(MouseEvent ev){
		setCursorVisible(true);	//turn cursor back on
	}
    //End MouseMotionListener interface

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

/* end */
