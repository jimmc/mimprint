/* ImageArea.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.JPanel;

/** A window in which we view our images.
 */
public class ImageArea extends JPanel implements ImageObserver {
	/** Our App. */
	protected App app;

	/** True if we need to get the width of the image. */
	protected boolean needWidth;

	/** True if we need to get the height of the image. */
	protected boolean needHeight;

	/** True if we need to get the image bits of the image. */
	protected boolean needImage;

	/** The displayed image. */
	protected Image image;

	/** The width of the displayed image. */
	protected int imageWidth;

	/** The height of the displayed image. */
	protected int imageHeight;

	/** Create an ImageArea. */
	public ImageArea(App app) {
		super();
		this.app = app;
		setPreferredSize(new Dimension(800,600));
	}

	/** Show the contents of the specified image file.
	 */
	public void showFile(File file) {
		needHeight = true;
		needWidth = true;
		needImage = true;
		findImage(file);
		repaint();
	}

	/** ImageObserver. */
	public boolean imageUpdate(Image img, int infoflags,
			int x,int y, int width, int height) {
		if ((infoflags & ImageObserver.WIDTH)!=0) {
			needWidth = false;
			this.imageWidth = width;
		}
		if ((infoflags & ImageObserver.HEIGHT)!=0) {
			needHeight = false;
			this.imageHeight = height;
		}
		if ((infoflags & ImageObserver.ALLBITS)!=0) {
			needImage = false;
			synchronized(this) {
				notifyAll();	//wake up the painter
			}
		}
		return (needWidth || needHeight || needImage);
	}

	/** Paint our image. */
	public void paint(Graphics g) {
		g.clearRect(0,0,getWidth(),getHeight()); //clear the window
		if (image==null)
			return;		//nothing to draw
		prepareImage();
		g.drawImage(image,0,0,null);	//TBD - center in window
	}

	/** Prepare our image for drawing. */
	protected synchronized void prepareImage() {
		Toolkit tk = getToolkit();
		boolean t = tk.prepareImage(image,-1,-1,this);
System.out.println("tk.prepareImage returns "+t);
		if (t) {
			needImage = false;
			return;		//got it
		}
		try {
			wait(100);	//wait for it just a bit
		} catch (InterruptedException ex) {} //ignore interrupt
	}

	/** Get the new image. */
	protected void findImage(File file) {
		Toolkit tk = getToolkit();
		if (file==null) {
			needImage = false;
			needWidth = false;
			needHeight = false;
			image = null;
			return;
		}
		String fileName = file.getAbsolutePath();
System.out.println("load image file "+fileName);
		image = tk.createImage(fileName);
		if (image==null) {
System.out.println("no image");
			//No image
			needImage = false;
			needWidth = false;
			needHeight = false;
			return;
		}
		imageWidth = image.getWidth(this);
		if (imageWidth>0)
			needWidth = false;
		imageHeight = image.getHeight(this);
		if (imageHeight>0)
			needHeight = false;
	}
}

/* end */
