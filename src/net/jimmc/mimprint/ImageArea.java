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
import java.awt.MediaTracker;
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

	/** Our media tracker to load images. */
	protected MediaTracker tracker;

	/** The current unscaled and unrotated image */
	protected Image imageSource;

	/** The current rendered image source. */
	protected RenderedImage renderedImageSource;

	/** The info text about the current image. */
	protected String imageInfoText;

	/** The current image rotation in units of 90 degrees */
	protected int currentRotation;

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
		tracker = new MediaTracker(this);
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
		currentRotation = 0;
		imageInfoText = imageInfo;
		if (app.useJAI()) {
			app.debugMsg("loading current image JAI");
			renderedImageSource =
				imageBundle.getScaledRenderedImage();
			app.debugMsg("loaded current image JAI");
		} else {
			app.debugMsg("loading current image scaled");
			imageSource = imageBundle.getScaledImage();
			if (imageSource==null) {
				app.debugMsg("loading current image unscaled");
				imageSource = imageBundle.getImage();
			}
			app.debugMsg("loaded current image");
		}
		showCurrentImage();	//rotate, scale, and display
		revalidate();
	}

	/** Load an image, wait for it to be loaded. */
	public void loadCompleteImage(Image image) {
		tracker.addImage(image,0);
		boolean loadStatus=false;
		try {
			app.debugMsg("Waiting for image "+image);
			loadStatus = tracker.waitForID(0,20000);
		} catch (InterruptedException ex) {
			String msg = "Interrupted waiting for image to load";
				//TBD i18n, include ex.getMessage()
			throw new RuntimeException(msg);
		}
		app.debugMsg("Done waiting for image "+image+
			", loadStatus="+loadStatus);
		tracker.removeImage(image,0);
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
		if (imageSource==null && renderedImageSource==null) {
			setText("No image");	//i18n
			return;		//nothing there
		}
		setText("Loading image...");	//i18n

		if (renderedImageSource!=null) {
			app.debugMsg("ShowCurrentImage X");
			setText(null);
			repaint();
			//TBD - handle rotation of RenderedImages
			return;
		}

		app.debugMsg("ShowCurrentImage A imageSource="+imageSource);
		Image srcImage;
		switch (currentRotation) {
		default:
		case 0:	srcImage = imageSource; break;
		case 1: srcImage = getRotatedImage(imageSource,90); break;
		case 2: srcImage = getRotatedImage(imageSource,180); break;
		case 3: srcImage = getRotatedImage(imageSource,270); break;
		}
		app.debugMsg("ShowCurrentImage B srcImage="+srcImage);
		Image scaledImage = getScaledImage(srcImage);
		app.debugMsg("ShowCurrentImage C scaledImage="+scaledImage);
		ImageIcon ii = new ImageIcon(scaledImage);
		app.debugMsg("ShowCurrentImage D");
		loadCompleteImage(scaledImage);	//wait for it
		app.debugMsg("ShowCurrentImage E");
		setText(null);
		setIcon(ii);
		app.debugMsg("ShowCurrentImage F");
	}

	/** Rotate the current image in increments of 90 degrees. */
	public void rotate(int inc) {
		currentRotation += inc;
		if (currentRotation>=4)
			currentRotation = 0;
		else if (currentRotation<0)
			currentRotation = 3;
		showCurrentImage();
	}

	/** Rotate the specified image.
	 * @param srcImage The image to rotate.  The image must already
	 *        be loaded so that we can get the width and height
	 *        without waiting.
	 * @param rotation Degress counterclockwise to rotate.  Must be one of
	 *        90, 180, or 270.
	 * @return A new image rotated by the specified number of degrees.
	 *        The image may not yet be fully generated.
	 */
	public Image getRotatedImage(Image srcImage, int rotation) {
		app.debugMsg("getRotatedImage");
		int w = srcImage.getWidth(null);
		int h = srcImage.getHeight(null);
		int waitCount=0;
		while (w<0 || h<0) {
			//The image has not yet started loading, so we don't
			//know it's size.  Wait just a bit.
			if (waitCount++>100) {
				String msg = "Can't get image size to rotate";
				setIcon(null);
				setText(msg);
				return null;
			}
			try {
				Thread.sleep(100);
			} catch (Exception ex) {
				//ignore
			}
			w = srcImage.getWidth(null);
			h = srcImage.getHeight(null);
		}
		Image dstImage;
		Graphics dstG;
		Graphics2D dstG2;
		AffineTransform transform;
		switch (rotation) {
		case 90:
			dstImage = createImage(h,w);
			dstG = dstImage.getGraphics();
			dstG2 = (Graphics2D)dstG;
			transform = new AffineTransform(
				0.0, -1.0, 1.0,  0.0,
				(double)0, (double)w );
			break;
		case 180:
			dstImage = createImage(w,h);
			dstG = dstImage.getGraphics();
			dstG2 = (Graphics2D)dstG;
			transform = new AffineTransform(
				-1.0,  0.0, 0.0, -1.0,
				(double)w, (double)h );
			break;
		case 270:
			dstImage = createImage(h,w);
			dstG = dstImage.getGraphics();
			dstG2 = (Graphics2D)dstG;
			transform = new AffineTransform(
				 0.0, 1.0, -1.0, 0.0,
				 (double)h, (double)0 );
			break;
		default:
			String msg0 = "Bad rotation angle";	//TBD i18n
			setIcon(null);
			setText(msg0);
			return null;
		}
		dstG2.drawImage(srcImage,transform,null);
		ImageIcon ii = new ImageIcon(dstImage);
		//loadCompleteImage(dstImage);		//load the whole image
		return dstImage;
	}

	/** Get a scaled version of the given image, which fits into
	 * our window at maximum size.
	 */
	public Image getScaledImage(Image sourceImage) {
		app.debugMsg("getScaledIimage");
		if (sourceImage==null)
			return null;
		int srcWidth = sourceImage.getWidth(null);
		int srcHeight = sourceImage.getHeight(null);
		int waitCount = 0;
		while (srcWidth<0 || srcHeight<0) {
			//The image has not yet started loading, so we don't
			//know it's size.  Wait just a bit.
			if (waitCount++>100) {
				String msg = "Can't get image size";
				setIcon(null);
				setText(msg);
				return null;
			}
			try {
				Thread.sleep(100);
			} catch (Exception ex) {
				//ignore
			}
			srcWidth = sourceImage.getWidth(null);
			srcHeight = sourceImage.getHeight(null);
		}
		int winWidth = getWidth();
		int winHeight = getHeight();
		if (srcWidth==winWidth && srcHeight==winHeight)
			return sourceImage;	//exact match
		double widthRatio = ((double)winWidth)/((double)srcWidth);
		double heightRatio = ((double)winHeight)/((double)srcHeight);
		int dstWidth;
		int dstHeight;
		if (widthRatio<heightRatio) {
			dstWidth = winWidth;
			dstHeight = (int)(srcHeight * widthRatio);
		} else {
			dstWidth = (int)(srcWidth * heightRatio);
			dstHeight = winHeight;
		}
		Image scaledImage = sourceImage.getScaledInstance(
						dstWidth,dstHeight,
						Image.SCALE_FAST);
		return scaledImage;
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
		if (imageSource!=null)
			showCurrentImage();
	}
	public void componentShown(ComponentEvent ev){}
    //End ComponentListener interface
}

/* end */
