/* AreaImageLayout.scala
 *
 * Jim McBeath, October 7, 2005
 * converted from java June 21, 2008
 */

package net.jimmc.mimprint

import java.awt.Component
import java.awt.geom.AffineTransform
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Rectangle
import java.io.File

class AreaImageLayout(x:Int, y:Int, width:Int, height:Int) extends AreaLayout {
    setBounds(x,y,width,height)

    private var item:PlayItemS = _       //the item we are displaying
    var path:String = _         //path from the item
    var image:Image = null      //the image we are displaying
    private var rot:Int = 0             //the rotation at which we display it
    private var transformedImage:Image = null
            //our image after scale and rotation to page coordinates

    //We do not use the areas array in our parent class

    /** Create an image area with no size info. */
    def this () = this(0,0,0,0)

    def getTemplateElementName() = "imageLayout"

    override def getAreaCount()  = 1   //image areas always have one area

    def allocateAreas():Array[AreaLayout] = null

    def hasImage() = image!=null

    def setImage(newItem:PlayItemS, comp:Component) {
        if (newItem==item) {
            if (transformedImage==null)
                return
            //same image, but we also want to look at the rotation
            val areaBounds:Rectangle = getBoundsInMargin()
            val imageAspect = (transformedImage.getWidth(null)>
                               transformedImage.getHeight(null))
            val areaAspect = (areaBounds.width>areaBounds.height)
            val needsRotate = imageAspect ^ areaAspect
            if (!needsRotate)
                return      //Same image and rotation as before, ignore update.
        }
        if (newItem==null) {
            path = null
            image = null
            transformedImage = null
            rot = 0
            item = null
            return
        }
        path = new File(newItem.baseDir,newItem.fileName).getPath
        image = comp.getToolkit().createImage(path)
        //We look at the aspect ratio of the image and
        //auto-rotate it to match the aspect ratio of
        //the image display area.
        SImageUtil.loadCompleteImage(comp,image) //we need the image size
        val areaBounds:Rectangle = getBoundsInMargin()
        val imageAspect = (image.getWidth(null)>image.getHeight(null))
        val areaAspect = (areaBounds.width>areaBounds.height)
        val needsRotate = imageAspect ^ areaAspect
        //We only allow playlist rotation in increments of
        //180 degrees.  The user can not rotate an image by
        //90 degrees in the printable area, if he wants that
        //he must tweak that area's size to change the
        //aspect ratio.
        rot = (newItem.getRotFlag() & ~1)+(if (needsRotate) 1 else 0)
        transformedImage = SImageUtil.scaleAndRotate(image,rot,path,comp)
        item = newItem
    }

    def unsetImage() = setImage(null,null)

    /** Rotate our image.  Caller is responsible for refreshing the screen. */
    def rotate(quarters:Int) {
        rot = ((rot + quarters +1 ) % 4) - 1
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
        if (transformedImage==null)
            return     //no image to paint
        val b:Rectangle = getBoundsInMargin()
        val transform:AffineTransform = new AffineTransform()
        g2.translate(b.x,b.y)
        scaleAndTranslate(g2,transformedImage.getWidth(null),
            transformedImage.getHeight(null),b.width,b.height)
        g2.drawImage(transformedImage,transform,null)
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
}
