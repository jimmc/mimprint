/* SImageUtil.java
 *
 * Jim McBeath, June 19, 2008
 * (Translated from ImageUtil, originally November 18, 2005)
 */

package net.jimmc.mimprint

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Component
import java.awt.geom.AffineTransform
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.MediaTracker

/** Misceallaneous image utility methods. */
object SImageUtil {

    def ICON_SIZE = 64

    /** Create a transparent image the size of one of our image icons,
     *  suitable for dragging.
     */
    def createTransparentIconImage(comp:Component,
            origImage:Image, path:String):Image = {
        //If no image available, just return a gray square
        if (origImage==null)
            return createTransparentGrayImage()

        loadCompleteImage(comp,origImage)

        val (width:Int, height:Int, scaledImage:Image) = {
            val origWidth = origImage.getWidth(null)
            val origHeight = origImage.getHeight(null)
            if (origWidth>ICON_SIZE || origHeight>ICON_SIZE) {
                val scaledImage = createScaledImage(
                        origImage,0,ICON_SIZE,ICON_SIZE,path)
                loadCompleteImage(comp,scaledImage)
                val width = scaledImage.getWidth(null)
                val height = scaledImage.getHeight(null)
                (width, height, scaledImage)
            } else {
                (origWidth, origHeight, origImage)
            }
        }

        //make a semi-transparent copy of the image for dragging
        val transImage = new BufferedImage(width,height,
                BufferedImage.TYPE_INT_ARGB_PRE)
        val g2:Graphics2D = transImage.createGraphics()
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC,0.8f))
        g2.drawImage(scaledImage,null,null)
        transImage
    }

    /** Create a transparent gray image the size of one of our image icons,
     * suitable for dragging when we don't have an image yet.
     */
    def createTransparentGrayImage():Image = {
        val width = ICON_SIZE
        val height = ICON_SIZE
        val transImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB_PRE)
        val g2:Graphics2D = transImage.createGraphics()
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC,0.8f))
        g2.setColor(Color.gray)
        g2.fillRect(0,0,ICON_SIZE,ICON_SIZE)
        transImage
    }

    /** Load an image, wait for it to be loaded. */
    def loadCompleteImage(tracker:MediaTracker, image:Image) {
        tracker.addImage(image,0)
        var loadStatus = false
        try {
            loadStatus = tracker.waitForID(0,20000)
        } catch {
            case ex:InterruptedException =>
                val msg = "Interrupted waiting for image to load"
                        //TBD i18n, include ex.getMessage()
                throw new RuntimeException(msg)
        }
        //println("Done waiting for image "+image+
         //       ", loadStatus="+loadStatus)
        tracker.removeImage(image,0)
    }
    def loadCompleteImage(comp:Component, image:Image):Unit =
        loadCompleteImage(new MediaTracker(comp), image)

    def scaleAndRotate(srcImage:Image, rot:Int, path:String,
            comp:Component):Image = {
        val si = createScaledImage(srcImage,rot,comp,path)
        loadCompleteImage(comp,si)
        val ri = createRotatedImage(si,rot,comp)
        loadCompleteImage(comp,ri)
        ri
    }

    def createScaledImage(sourceImage:Image, rotation:Int,
            comp:Component, path:String):Image = {
        createScaledImage(sourceImage, rotation,
                comp.getWidth, comp.getHeight, path)
    }

    /** Create a scaled image from the given image. */
    def createScaledImage(sourceImage:Image, rotation:Int,
                displayWidth:Int, displayHeight:Int, path:String):Image = {
        if (sourceImage==null)
            return null

        if (displayWidth==0 || displayHeight==0)
            return sourceImage	//no scaling

        val (srcWidth, srcHeight) = getImageSize(sourceImage)

        val xy = (rotation==1 || rotation==3)
                //True if rotated by 90 (or 270) degrees, so the
                //horizontal and vertical axes are interchanged.
        val xScale = displayWidth/(if (xy) srcHeight else srcWidth).asInstanceOf[Float]
        val yScale = displayHeight/(if (xy) srcWidth else srcHeight).asInstanceOf[Float]
        val scale = if (xScale<yScale) xScale else yScale
        if (scale==1.0)
            return sourceImage	//exact size match
        val dstWidth = (srcWidth * scale).asInstanceOf[Int]
        val dstHeight = (srcHeight * scale).asInstanceOf[Int]

        val scaledImage = sourceImage.getScaledInstance(
                    dstWidth,dstHeight,Image.SCALE_FAST)
        scaledImage
    }

    def createRotatedImage(sourceImage:Image,rot:Int,comp:Component):Image = {
        if (((rot+4)%4)==0)
            return sourceImage
        val (w, h) = getImageSize(sourceImage)
        val dstImage = comp.createImage(
                if (rot%2==0) w else h,  if (rot%2==0) h else w)
        val dstG2 = dstImage.getGraphics.asInstanceOf[Graphics2D]
        var transform:AffineTransform = null
        ((rot+4)%4) match {
            case 1 => transform = new AffineTransform(
                    0.0, -1.0, 1.0, 0.0, 0.0, w)
            case 2 => transform = new AffineTransform(
                    -1.0, 0.0, 0.0, -1.0, w, h)
            case 3 => transform = new AffineTransform(
                    0.0, 1.0, -1.0, 0.0, h, 0.0)
            case 0 =>   //shouldn't happen, we checked for rot%4==0 above
                return sourceImage
        }
        dstG2.drawImage(sourceImage,transform,null)
        dstImage
    }

    def getImageSize(sourceImage:Image):(Int,Int) = {
        var waitCount = 0
        while (sourceImage.getWidth(null)<0 || sourceImage.getHeight(null)<0) {
            //THe image has not yet started loading, so we don't
            //know it's size.  Wait just a bit.
            waitCount = waitCount + 1
            if (waitCount > 100)
                throw new RuntimeException("Timed out loading image")
            try { Thread.sleep(100) } catch { case _ => } //ignore errors here
        }
        (sourceImage.getWidth(null), sourceImage.getHeight(null))
    }

    /** Given an area of specified size in user space, scale it to fit into
     * the given window space, and translate it to center it top/bottom or
     * left/right for whichever dimension is smaller.
     */
    def scaleAndTranslate(g2:Graphics2D,
            userWidth:Int, userHeight:Int, windowWidth:Int, windowHeight:Int) {
        val xscale = windowWidth.asInstanceOf[Double] /
                     userWidth.asInstanceOf[Double]
        val yscale = windowHeight.asInstanceOf[Double] /
                     userHeight.asInstanceOf[Double]
        val scale = if (xscale<yscale) xscale else yscale
        if (xscale<yscale)
            g2.translate(0,(yscale-xscale)*userHeight/2)
        else
            g2.translate((xscale-yscale)*userWidth/2,0)
        g2.scale(scale,scale)
    }

}
