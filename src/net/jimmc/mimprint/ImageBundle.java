/* ImageBundle.java
 *
 * Jim McBeath, September 24, 2001
 */

package jimmc.jiviewer;

import java.awt.Image;
import java.io.File;

/** An image in multiple sizes, plus additional information.
 */
public class ImageBundle {
	/** The ImageArea in which our image will be displayed. */
	protected ImageArea imageArea;

	/** Our app. */
	protected App app;

	/** The path to this image. */
	protected String path;

	/** The original image. */
	protected Image image;

	/** The scaled image. */
	protected Image scaledImage;

	/** The index of this image in the containing list. */
	protected int listIndex;

	/** Create an image bundle for a file.
	 * @param c A reference component.
	 * @param file The file containing the image.
	 * @param listIndex The list index of the file.
	 */
	public ImageBundle(ImageArea imageArea, File file, int listIndex) {
		this.imageArea = imageArea;
		app = imageArea.app;
		path = file.getAbsolutePath();
		image = imageArea.getToolkit().createImage(path);
		this.listIndex = listIndex;
	}

	/** Get the path for our original image. */
	public String getPath() {
		return path;
	}
	
	/** Get the original image from this bundle. */
	public Image getImage() {
		return image;
	}

	/** Get the scaled image from this bundle. */
	public Image getScaledImage() {
		return scaledImage;
	}

	/** Get our list index. */
	public int getListIndex() {
		return listIndex;
	}

	/** Load the scaled version of our image.
	 * This method is typically run in a separate image-loader thread.
	 */
	public void loadScaledImage() {
		if (scaledImage!=null)
			return;		//already loaded
		app.debugMsg("Bundle loadScaledImage A image="+image);
		imageArea.loadCompleteImage(image);
		app.debugMsg("Bundle loadScaledImage B");
		Image si = imageArea.getScaledImage(image);
		app.debugMsg("Bundle loadScaledImage C scaledImage="+si);
		imageArea.loadCompleteImage(si);
		app.debugMsg("Bundle loadScaledImage D");
		scaledImage = si;
	}
}

/* end */
