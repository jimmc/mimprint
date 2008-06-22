/* AreaImageLayout.scala
 *
 * Jim McBeath, October 7, 2005
 * converted from java June 21, 2008
 */

package net.jimmc.mimprint

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import java.io.PrintWriter

class AreaImageLayout(x:Int, y:Int, width:Int, height:Int) extends AreaLayout {
    setBounds(x,y,width,height)

    private var imageBundle:ImageBundle = _

    //We do not use the areas array in our parent class

    /** Create an image area with no size info. */
    def this () = this(0,0,0,0)

    def getTemplateElementName() = "imageLayout"

    override def getAreaCount()  = 1   //image areas always have one area

    /** We are always valid. */
    def revalidate() {
        //do nothing
    }

    /** Get the path to our image, or null if no image. */
    def getImagePath():String = {
        if (imageBundle==null)
            null
        else
            imageBundle.getPath()
    }

    /** Get our image object.  May be null. */
    def getImage():Image = {
        if (imageBundle==null)
            null
        else
            imageBundle.getImage()
    }

    /** Get the image displayed in this area. */
    def getImageBundle() = imageBundle

    def hasImage() = (imageBundle!=null)

    /** Set the image to be displayed in this area. */
    def setImage(image:ImageBundle) = this.imageBundle = image

    private var lastPlayItem:PlayItem = _
    def setImage(item:PlayItem, imgWin:ImageWindow) {
        if (lastPlayItem!=null &&
                item.toString().equals(lastPlayItem.toString())) {
            //The image is the same image as before, so ignore the update.
            return
        }
        val bundle = new ImageBundle(null, imgWin,
                new File(item.getBaseDir(),item.getFileName()),-1)
        val bImage = bundle.getImage()
        //We look at the aspect ratio of the image and
        //auto-rotate it to match the aspect ratio of
        //the image display area.
        val imgBounds:Rectangle = getBounds()
        val needsRotate =
            (bImage.getWidth(null)>bImage.getHeight(null)) ^
            (imgBounds.width<imgBounds.height)
        //We only allow playlist rotation in increments of
        //180 degrees.  The user can not rotate an image by
        //90 degrees in the printable area, if he wants that
        //he must tweak that area's size to change the
        //aspect ratio.
        val r = (item.getRotFlag() & ~1)+(if (needsRotate) 1 else 0)
        bundle.rotate(r)
        setImage(bundle)
        lastPlayItem = item
    }

    /** Rotate our image.  Caller is responsible for refreshing the screen. */
    def rotate(quarters:Int) {
        if (imageBundle!=null)
            imageBundle.rotate(quarters)
    }

    /** Paint our image on the page. */
    override def paint(g2p:Graphics2D, currentArea:AreaLayout,
            highlightedArea:AreaLayout, drawOutlines:Boolean) {
        val g2 = g2p.create().asInstanceOf[Graphics2D]
            //make a copy of our caller's gc so our changes don't
            //affect the caller.
        paintOutlines(drawOutlines,g2,currentArea,highlightedArea)
        paintImage(g2) //this changes the transformation in g2
        g2.dispose()
    }

    private def paintImage(g2:Graphics2D) {
        if (imageBundle==null)
            return     //no image to paint
        val b:Rectangle = getBoundsInMargin()
        val image:Image = imageBundle.getTransformedImage()
        val transform:AffineTransform = new AffineTransform()
        g2.translate(b.x,b.y)
        scaleAndTranslate(g2,image.getWidth(null),image.getHeight(null),b.width,b.height)
        g2.drawImage(image,transform,null)
    }

    /** Given an area of specified size in user space, scale it to fit into
     * the given window space, and translate it to center it top/bottom or
     * left/right for whichever dimension is smaller.
     */
    private def scaleAndTranslate(g2:Graphics2D, userWidth:Int, userHeight:Int,
                windowWidth:Int, windowHeight:Int) {
        val xscale = (windowWidth.asInstanceOf[Double])/
                    (userWidth.asInstanceOf[Double])
        val yscale = (windowHeight).asInstanceOf[Double]/
                    (userHeight.asInstanceOf[Double])
        val scale = if (xscale<yscale) xscale else yscale
        if (xscale<yscale)
            g2.translate(0,(yscale-xscale)*userHeight/2)
        else
            g2.translate((xscale-yscale)*userWidth/2,0)
        g2.scale(scale,scale)
    }

    private var imageIndex = -1
    override def setImageIndexes(start:Int):Int = {
        imageIndex = start
        return 1
    }

    def getImageIndex() = imageIndex

    override protected def addImageBundle(b:ImageBundle):Boolean = {
        if (imageBundle!=null)
            return false       //we already have an image
        setImage(b)
        true
    }

    //Add our area info to the specified PlayList
    override def retrieveIntoPlayList(playList:PlayList):PlayList = {
        val path = getImagePath()
        if (path==null) {
            //Create an empty item
            val item:PlayItem = PlayItemS.emptyItem()
            return playList.addItem(item)
        } else {
            val f:File = new File(path)
            val baseDir:File = f.getParentFile()
            val fileName:String = f.getName()
            val rot:Int = imageBundle.getRotation()
            val item:PlayItem = new PlayItemS(null,baseDir,fileName,rot)
            return playList.addItem(item)
        }
    }
}
