/* ImageArea.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Image;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/** A window in which we view our images.
 */
public class ImageArea extends JLabel implements KeyListener {
	/** Our App. */
	protected App app;

	/** The current full-size image */
	protected Image fullSizeImage;

	/** The current image rotation in units of 90 degrees */
	protected int currentRotation;

	/** Create an ImageArea. */
	public ImageArea(App app) {
		super();
		this.app = app;
		setPreferredSize(new Dimension(800,600));
		setHorizontalAlignment(CENTER);
		addKeyListener(this);
	}

	/** Show the contents of the specified image file.
	 */
	public void showFile(File file) {
		currentRotation = 0;
		if (file==null) {
System.out.println("showFile null file");
			setIcon(null);
			return;		//nothing there
		}
		String path = file.getAbsolutePath();
//System.out.println("showFile "+path);
		if (path==null) {
			setIcon(null);
			return;		//nothing there
		}
		PImageIcon ii = new PImageIcon(file.getAbsolutePath());
		fullSizeImage = ii.getImage();
		ii.pLoadImage(fullSizeImage);		//load the whole image
		showCurrentImage();	//rotate, scale, and display
	}

	/** Redisplay the current image. */
	public void showCurrentImage() {
		//TBD - rotate the image first
		Image scaledImage = getScaledImage(fullSizeImage);
		PImageIcon ii = new PImageIcon(scaledImage);
		ii.pLoadImage(scaledImage);	//wait for it
		setIcon(ii);
	}

	/** Rotate the current image an additional 90 degrees counter CW */
	public void rotate() {
		currentRotation++;
		if (currentRotation>=4)
			currentRotation = 0;
System.out.println("rotation is now "+currentRotation);
		showCurrentImage();
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

	class PImageIcon extends ImageIcon {
		public PImageIcon(Image im) {
			super(im);
		}
		public PImageIcon(String path) {
			super(path);
		}
		public void pLoadImage(Image im) {
			loadImage(im);
		}
	}

	/** We return true to allow keyboard focus and thus input. */
	public boolean isFocusTraversable() {
		return true;	//allow keyboard input
	}

    //The KeyListener interface
    	public void keyPressed(KeyEvent ev) {
		int keyCode = ev.getKeyCode();
System.out.println("keyPressed "+keyCode);
		switch (keyCode) {
		case KeyEvent.VK_DOWN:
			System.out.println("DOWN");
			break;
		case KeyEvent.VK_UP:
			System.out.println("UP");
			break;
		default:	//ignore
		}
	}
	public void keyReleased(KeyEvent ev) {
		int keyCode = ev.getKeyCode();
System.out.println("keyReleased "+keyCode);
	}
	public void keyTyped(KeyEvent ev) {
		char ch = ev.getKeyChar();
System.out.println("keyTyped "+ch);
		switch (ch) {
		case 'r': case 'R':	//rotate
			rotate();
			break;
		//TBD - add help popup
		default:		//do nothing
		}
	}
    //End KeyListener interface
}

/* end */
