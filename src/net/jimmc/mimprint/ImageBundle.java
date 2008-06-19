/* ImageBundle.java
 *
 * Jim McBeath, September 24, 2001
 */

package net.jimmc.mimprint;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;

/** An image in multiple sizes, plus additional information.
 */
public class ImageBundle {
    /** The ImageWindow in which our image will be displayed. */
    private ImageWindow imageWindow;

    /** Our app. */
    protected App app;

    /** Our toolkit. */
    protected Toolkit toolkit;

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

    /** The transformed image. */
    protected Image transformedImage;

    /** The index of this image in the containing list. */
    protected int listIndex;

    /** Create an image bundle for a file.
     * @param file The file containing the image.
     * @param listIndex The list index of the file.
     */
    public ImageBundle(App app, ImageWindow imageWindow,
            File file, int listIndex) {
        this.app = app;
        this.listIndex = listIndex;
        path = file.getAbsolutePath();
        setImageWindow(imageWindow);
        image = toolkit.createImage(path);
    }

    protected void finalize() {
        release();
    }

    /** Release our image resources. */
    private void release() {
        if (image!=null) {
            image.flush();
            image = null;
        }
        if (transformedImage!=null) {
            transformedImage.flush();
            transformedImage = null;
        }
    }

    public void setImageWindow(ImageWindow imageWindow) {
        this.imageWindow = imageWindow;
        this.toolkit = imageWindow.getToolkit();
        if (imageWindow instanceof ImageArea)
            setDisplaySize(imageWindow.getWidth(),imageWindow.getHeight());
        else
            setDisplaySize(0,0);
                    //imageWindow is page size, we don't know our area size
    }

    /** Set the size of the display for our image.
     * From this we calclate the scale factor.
     * @param width Width of the display,
     *              or 0 to do no scaling of the image.
     * @param height Height of the display,
     *              or 0 to do no scaling of the image.
     */
    public void setDisplaySize(int width, int height) {
        if (width==displayWidth && height==displayHeight)
            return;        //no change
        //The display size has changed, clear out the cached
        //transformed images so they will be regenerated
        transformedImage = null;
        displayWidth = width;
        displayHeight = height;
    }

    /** Increment the image rotation by the given amount.
     * @param inc Units of 90 degrees.
     */
    public void rotate(int inc) {
        rotation += inc;
        if (rotation>=4)
            rotation = 0;
        else if (rotation<0)
            rotation = 3;
        transformedImage = null;
    }

    public int getRotation() {
        return rotation;
    }

    /** Get the image object. */
    public Image getImage() {
        return image;
    }

    /** Get the path for our original image. */
    public String getPath() {
        return path;
    }
    
    /** Get our list index. */
    public int getListIndex() {
        return listIndex;
    }

    /** Get our image transformed by scale and rotation. */
    public Image getTransformedImage() {
        if (transformedImage==null)
            loadTransformedImage();
        return transformedImage;
    }

    /** Load the transformed version of our image.
     * This method is typically run in a separate image-loader thread.
     */
    public void loadTransformedImage() {
        if (transformedImage!=null)
            return;        //already loaded

        //app.debugMsg("Bundle loadTransformedImage A image="+image);
        Image si = createScaledImage(image);
        loadCompleteImage(si);
        //app.debugMsg("Bundle loadTransformedImage B scaledImage="+si);
        Image ri = createRotatedImage(si);
        si.flush();
        //app.debugMsg("Bundle loadTransformedImage C txImage="+ri);
        transformedImage = ri;
        loadCompleteImage(transformedImage);
    }

    /** Load an image, wait for it to be loaded. */
    protected void loadCompleteImage(Image image) {
        if (app!=null)
            app.getImageUtil().loadCompleteImage(image);
        else
            (new ImageUtil(null,imageWindow.getComponent())).loadCompleteImage(image);
    }

    /** Get a scaled version of the given image which fits into
     * the display area.
     */
    protected Image createScaledImage(Image sourceImage) {
        //app.debugMsg("createScaledIimage");
        return ImageUtil.createScaledImage(sourceImage,rotation,
                displayWidth,displayHeight,path);
    }

    /** Rotate the specified image by our own rotation amount. */
    protected Image createRotatedImage(Image srcImage) {
        return createRotatedImage(srcImage,rotation);
    }

    /** Rotate the specified image.
     * @param srcImage The image to rotate.  The image must already
     *        be loaded so that we can get the width and height
     *        without waiting.
     * @param rotation Quadrants counterclockwise to rotate.
     *        1 for 90 degress, 2 for 180 degrees, 3 for 270 degrees.
     * @return A new image rotated by the specified amount.
     *        If the rotation is 0, the original image is returned.
     *        The image may not yet be fully generated.
     */
    protected Image createRotatedImage(Image srcImage, int rotation) {
        //app.debugMsg("getRotatedImage");
        if (rotation==0)
            return srcImage;
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
        case 1:
            dstImage = createImage(h,w);
            dstG = dstImage.getGraphics();
            dstG2 = (Graphics2D)dstG;
            transform = new AffineTransform(
                0.0, -1.0, 1.0,  0.0, (double)0, (double)w );
            break;
        case 2:
            dstImage = createImage(w,h);
            dstG = dstImage.getGraphics();
            dstG2 = (Graphics2D)dstG;
            transform = new AffineTransform(
                -1.0,  0.0, 0.0, -1.0, (double)w, (double)h );
            break;
        case 3:
            dstImage = createImage(h,w);
            dstG = dstImage.getGraphics();
            dstG2 = (Graphics2D)dstG;
            transform = new AffineTransform(
                 0.0, 1.0, -1.0, 0.0, (double)h, (double)0 );
            break;
        default:
            return null;    //bad rotation angle
        }
        dstG2.drawImage(srcImage,transform,null);
        loadCompleteImage(dstImage);        //load the whole image
        return dstImage;
    }

    private Image createImage(int w, int h) {
        return imageWindow.createImage(w,h);
    }
}

/* end */
