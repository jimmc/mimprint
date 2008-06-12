/* ImageUtil.java
 *
 * Jim McBeath, November 18, 2005
 */

package net.jimmc.mimprint;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.MediaTracker;

/** Misceallaneous image utility methods. */
public class ImageUtil {
    private App app;
    private MediaTracker tracker;

    public static final int ICON_SIZE = 64;

    public ImageUtil(App app, Viewer viewer) {
        this.app = app;
        tracker = new MediaTracker(viewer);
    }

    /** Create a transparent image the size of one of our image icons,
     *  suitable for dragging.
     */
    public Image createTransparentIconImage(Image origImage, String path) {
        //If no image available, just return a gray square
        if (origImage==null)
            return createTransparentGrayImage();

        loadCompleteImage(origImage);
        int width = origImage.getWidth(null);
        int height = origImage.getHeight(null);

        if (width>ICON_SIZE || height>ICON_SIZE) {
            origImage = createScaledImage(origImage,0,ICON_SIZE,ICON_SIZE,path);
            loadCompleteImage(origImage);
            width = origImage.getWidth(null);
            height = origImage.getHeight(null);
        }

        //make a semi-transparent copy of the image for dragging
        BufferedImage transImage = new BufferedImage(width,height,
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = transImage.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC,0.8f));
        g2.drawImage(origImage,null,null);
        return transImage;
    }

    /** Create a transparent gray image the size of one of our image icons,
     * suitable for dragging when we don't have an image yet.
     */
    public Image createTransparentGrayImage() {
        int width = ICON_SIZE;
        int height = ICON_SIZE;
        BufferedImage transImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = transImage.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC,0.8f));
        g2.setColor(Color.gray);
        g2.fillRect(0,0,ICON_SIZE,ICON_SIZE);
        return transImage;
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
    
    /** Create a scaled image from the given image. */
    public static Image createScaledImage(Image sourceImage, int rotation,
                int displayWidth, int displayHeight, String path) {
        if (sourceImage==null)
            return null;

        if (displayWidth==0 || displayHeight==0)
            return sourceImage;	//no scaling

        int srcWidth = sourceImage.getWidth(null);
        int srcHeight = sourceImage.getHeight(null);
        int waitCount = 0;
        while (srcWidth<0 || srcHeight<0) {
            //The image has not yet started loading, so we don't
            //know it's size.  Wait just a bit.
            if (waitCount++>50) {       //5 seconds
                System.out.println("Timed out waiting to load image "+path);
(new Exception("debug")).printStackTrace();
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

        boolean xy = (rotation==1 || rotation==3);
                //True if rotated by 90 (or 270) degrees, so the
                //horizontal and vertical axes are interchanged.
        float xScale = displayWidth/(float)(xy?srcHeight:srcWidth);
        float yScale = displayHeight/(float)(xy?srcWidth:srcHeight);
        float scale = (xScale<yScale)?xScale:yScale;
        if (scale==1.0)
            return sourceImage;	//exact size match
        int dstWidth = (int)(srcWidth * scale);
        int dstHeight = (int)(srcHeight * scale);

        Image scaledImage = sourceImage.getScaledInstance(
                    dstWidth,dstHeight,Image.SCALE_FAST);
        return scaledImage;
    }
}
