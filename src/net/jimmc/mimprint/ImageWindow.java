/* ImageWindow.java
 *
 * Jim McBeath, October 11, 2005
 */

package net.jimmc.jiviewer;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;

/** A window in which we view our images.
 */
public interface ImageWindow {
        /** Create an image of the specified width and height. */
        public Image createImage(int width, int height);

        /** Get the component on which this area is displayed. */
        public Component getComponent();

        /** Get the width of this image area. */
        public int getWidth();

        /** Get the height of this image area. */
        public int getHeight();

        /** Get a toolkit for use with this image area. */
        public Toolkit getToolkit();

        /** Request focus for this area. */
        public void requestFocus();

	/** Show text instead of an image. */
	public void showText(String text);

	/** Show an image, set up text info about the image. */
	public void showImage(ImageBundle imageBundle, String imageInfo);

	/** Set the cursor to a busy cursor. */
	public void setCursorBusy(boolean busy);
}

/* end */
