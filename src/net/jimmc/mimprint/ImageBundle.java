/* ImageBundle.java
 *
 * Jim McBeath, September 24, 2001
 */

package jimmc.jiviewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Image;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.RenderedImage;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.File;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

/** An image in multiple sizes, plus additional information.
 */
public class ImageBundle {
	/** The ImageArea in which our image will be displayed. */
	protected ImageArea imageArea;

	/** Our app. */
	protected App app;

	/** Our toolkit. */
	protected Toolkit toolkit;

	/** Our media tracker to load images. */
	protected MediaTracker tracker;

	/** The path to this image. */
	protected String path;

	/** The current image rotation in units of 90 degrees. */
	protected int rotation;

	/** The current image display width. */
	protected int displayWidth;

	/** The current image display height. */
	protected int displayHeight;

	/** The original image. */
	protected Image image;

	/** The renderedImage. */
	protected RenderedImage renderedImage;

	/** The transformed image. */
	protected Image transformedImage;

	/** The transformed renderedImage. */
	protected RenderedImage transformedRenderedImage;

	/** The index of this image in the containing list. */
	protected int listIndex;

	/** Create an image bundle for a file.
	 * @param c A reference component.
	 * @param file The file containing the image.
	 * @param listIndex The list index of the file.
	 */
	public ImageBundle(ImageArea imageArea, File file, int listIndex) {
		app = imageArea.app;
		this.imageArea = imageArea;
		this.listIndex = listIndex;
		this.toolkit = imageArea.getToolkit();
		tracker = new MediaTracker(imageArea);
		path = file.getAbsolutePath();
		setDisplaySize(imageArea.getWidth(),imageArea.getHeight());
		if (app.useJAI())
			renderedImage = createRenderedImage(path);
		else
			image = toolkit.createImage(path);
	}

	/** Create a renderedImage. */
	public RenderedImage createRenderedImage(String path) {
		RenderedOp rFile = JAI.create("fileLoad",path);
		return rFile;
	}

	/** Set the size of the display for our image.
	 * From this we calclate the scale factor.
	 */
	public void setDisplaySize(int width, int height) {
		if (width==displayWidth && height==displayHeight)
			return;		//no change
		//The display size has changed, clear out the cached
		//transformed images so they will be regenerated
		transformedImage = null;
		transformedRenderedImage = null;
		displayWidth = width;
		displayHeight = height;
	}

	/** Increment the image rotation by the given amount.
	 * @param rotation Units of 90 degrees.
	 */
	public void rotate(int inc) {
		rotation += inc;
		if (rotation>=4)
			rotation = 0;
		else if (rotation<0)
			rotation = 3;
		transformedImage = null;
		transformedRenderedImage = null;
	}

	/** Get the path for our original image. */
	public String getPath() {
		return path;
	}
	
	/** Get our list index. */
	public int getListIndex() {
		return listIndex;
	}

	/** Get the original image from this bundle. */
	public Image getImage() {
		return image;
	}

	/** Get the rendered image from this bundle. */
	public RenderedImage getRenderedImage() {
		return renderedImage;
	}

	/** Get our image transformed by scale and rotation. */
	public Image getTransformedImage() {
		if (transformedImage==null)
			loadTransformedImage();
		return transformedImage;
	}

	/** Get our image transformed by scale and rotation. */
	public RenderedImage getTransformedRenderedImage() {
		if (transformedRenderedImage==null)
			loadTransformedRenderedImage();
		return transformedRenderedImage;
	}

	/** Load the transformed version of our image.
	 * This method is typically run in a separate image-loader thread.
	 */
	public void loadTransformedImage() {
		if (renderedImage!=null) {
			//Produce a transformed version of the renderedImage
			loadTransformedRenderedImage();
			return;
		}

		if (transformedImage!=null)
			return;		//already loaded

		app.debugMsg("Bundle loadTransformedImage A image="+image);
		Image si = createScaledImage(image);
		app.debugMsg("Bundle loadTransformedImage B scaledImage="+si);
		Image ri = createRotatedImage(si);
		app.debugMsg("Bundle loadTransformedImage C txImage="+ri);
		transformedImage = ri;
		loadCompleteImage(transformedImage);
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

	/** Load the transformed version of our renderedImage.
	 */
	public void loadTransformedRenderedImage() {
		if (transformedRenderedImage!=null)
			return;

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(renderedImage);

		//scale the image to fit into the imageArea
		int rw = renderedImage.getWidth();
		int rh = renderedImage.getHeight();
		float xScale = imageArea.getWidth()/(float)rw;
		float yScale = imageArea.getHeight()/(float)rh;
		float scale = (xScale<yScale)?xScale:yScale;
		float zero = (float)0.0;
		pb.add(scale);	//x scale
		pb.add(scale);	//y scale
		pb.add(zero);	//x translation
		pb.add(zero);	//y translation
		pb.add(Interpolation.getInstance(
			Interpolation.INTERP_BILINEAR));
		//TBD handle rotation
		transformedRenderedImage = JAI.create("scale",pb);
	}

	/** Get a scaled version of the given image, which fits into
	 * the imageArea window at maximum size.
	 */
	public Image createScaledImage(Image sourceImage) {
		app.debugMsg("createScaledIimage");
		if (sourceImage==null)
			return null;
		int srcWidth = sourceImage.getWidth(null);
		int srcHeight = sourceImage.getHeight(null);
		int waitCount = 0;
		while (srcWidth<0 || srcHeight<0) {
			//The image has not yet started loading, so we don't
			//know it's size.  Wait just a bit.
			if (waitCount++>100) {
				return null;	//can't get it
			}
			try {
				Thread.sleep(100);
			} catch (Exception ex) {
				//ignore
			}
			srcWidth = sourceImage.getWidth(null);
			srcHeight = sourceImage.getHeight(null);
		}
		int winWidth = imageArea.getWidth();
		int winHeight = imageArea.getHeight();
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

	/** Rotate the specified image by our own rotation amount. */
	public Image createRotatedImage(Image srcImage) {
		switch (rotation) {
		default:
		case 0:	return srcImage;
		case 1: return createRotatedImage(srcImage,90);
		case 2: return createRotatedImage(srcImage,180);
		case 3: return createRotatedImage(srcImage,270);
		}
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
	public Image createRotatedImage(Image srcImage, int rotation) {
		app.debugMsg("getRotatedImage");
		int w = srcImage.getWidth(null);
		int h = srcImage.getHeight(null);
		int waitCount=0;
		while (w<0 || h<0) {
			//The image has not yet started loading, so we don't
			//know it's size.  Wait just a bit.
			if (waitCount++>100) {
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
			dstImage = imageArea.createImage(h,w);
			dstG = dstImage.getGraphics();
			dstG2 = (Graphics2D)dstG;
			transform = new AffineTransform(
				0.0, -1.0, 1.0,  0.0,
				(double)0, (double)w );
			break;
		case 180:
			dstImage = imageArea.createImage(w,h);
			dstG = dstImage.getGraphics();
			dstG2 = (Graphics2D)dstG;
			transform = new AffineTransform(
				-1.0,  0.0, 0.0, -1.0,
				(double)w, (double)h );
			break;
		case 270:
			dstImage = imageArea.createImage(h,w);
			dstG = dstImage.getGraphics();
			dstG2 = (Graphics2D)dstG;
			transform = new AffineTransform(
				 0.0, 1.0, -1.0, 0.0,
				 (double)h, (double)0 );
			break;
		default:
			return null;	//bad rotation angle
		}
		dstG2.drawImage(srcImage,transform,null);
		loadCompleteImage(dstImage);		//load the whole image
		return dstImage;
	}
}

/* end */
