/* ImageArea.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/** A window in which we view our images.
 */
public class ImageArea extends JLabel
		implements KeyListener, ComponentListener {
	/** Our App. */
	protected App app;

	/** Our Viewer. */
	protected Viewer viewer;

	/** Our ImageLister. */
	protected ImageLister imageLister;

	/** Our media tracker to load images. */
	protected MediaTracker tracker;

	/** The current full-size image */
	protected Image fullSizeImage;

	/** The current image rotation in units of 90 degrees */
	protected int currentRotation;

	/** Create an ImageArea. */
	public ImageArea(App app, Viewer viewer) {
		super();
		this.app = app;
		this.viewer = viewer;
		setPreferredSize(new Dimension(800,600));
		setHorizontalAlignment(CENTER);
		addKeyListener(this);
		addComponentListener(this);
		tracker = new MediaTracker(this);
	}

	/** Set our image lister. */
	public void setImageLister(ImageLister imageLister) {
		this.imageLister = imageLister;
	}

	/** Show the contents of the specified image file.
	 */
	public void showFile(File file) {
		setIcon(null);
		currentRotation = 0;
		if (file==null) {
			setText("No file");
			return;		//nothing there
		}
		String path = file.getAbsolutePath();
		if (path==null) {
			setText("No file path");
			return;		//nothing there
		}
		setText("Loading image...");
		ImageIcon ii = new ImageIcon(file.getAbsolutePath());
		fullSizeImage = ii.getImage();
		loadCompleteImage(fullSizeImage);	//load the whole image
		showCurrentImage();	//rotate, scale, and display
	}

	/** Load an image, wait for it to be loaded. */
	public void loadCompleteImage(Image image) {
		tracker.addImage(image,0);
		try {
			tracker.waitForID(0,20000);
		} catch (InterruptedException ex) {
			String msg = "Interrupted waiting for image to load";
				//TBD i18n, include ex.getMessage()
			throw new RuntimeException(msg);
		}
		//TBD - check to see if it loaded properly
		tracker.removeImage(image,0);
	}

	/** Redisplay the current image. */
	public void showCurrentImage() {
		Image srcImage;
		switch (currentRotation) {
		default:
		case 0:	srcImage = fullSizeImage; break;
		case 1: srcImage = rotate(fullSizeImage,90); break;
		case 2: srcImage = rotate(fullSizeImage,180); break;
		case 3: srcImage = rotate(fullSizeImage,270); break;
		}
		Image scaledImage = getScaledImage(srcImage);
		ImageIcon ii = new ImageIcon(scaledImage);
		loadCompleteImage(scaledImage);	//wait for it
		setText(null);
		setIcon(ii);
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
	public Image rotate(Image srcImage, int rotation) {
		int w = srcImage.getWidth(null);
		int h = srcImage.getHeight(null);
		if (w<=0 || h<=0) {
			String msg = "No width or height for image"; //TBD i18n
			throw new RuntimeException(msg);
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
			throw new RuntimeException(msg0);
		}
		dstG2.drawImage(srcImage,transform,null);
		ImageIcon ii = new ImageIcon(dstImage);
		loadCompleteImage(dstImage);		//load the whole image
		return dstImage;
	}

	/** Get a scaled version of the given image, which fits into
	 * our window at maximum size.
	 */
	protected Image getScaledImage(Image sourceImage) {
		int srcWidth = sourceImage.getWidth(null);
		int srcHeight = sourceImage.getHeight(null);
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
						dstWidth,dstHeight,0);
		return scaledImage;
	}

	/** We return true to allow keyboard focus and thus input. */
	public boolean isFocusTraversable() {
		return true;	//allow keyboard input
	}

    //The KeyListener interface
    	public void keyPressed(KeyEvent ev) {
		int keyCode = ev.getKeyCode();
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
		case 'o':	//file-open dialog
			viewer.processFileOpen();
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
			viewer.processClose();
			break;
		//TBD - add help popup
		default:		//do nothing    TBD- beep?
		}
	}
    //End KeyListener interface

    //The ComponentListener interface
	public void componentHidden(ComponentEvent ev){}
	public void componentMoved(ComponentEvent ev){}
	public void componentResized(ComponentEvent ev){
		showCurrentImage();
	}
	public void componentShown(ComponentEvent ev){}
    //End ComponentListener interface
}

/* end */
