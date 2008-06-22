/* AreaSplitLayout.scala
 *
 * Jim McBeath, October 25, 2005
 * converted to scala June 21, 2008
 */

package net.jimmc.mimprint

import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.io.PrintWriter

import org.xml.sax.Attributes

object AreaSplitLayout {
    val VERTICAL = 0   //top-bottom split
    val HORIZONTAL = 1 //left-right split
}

/** Like AWT SplitPane, split into two areas of possibly unequal size. */
class AreaSplitLayout extends AreaLayout {

    import AreaSplitLayout._

    //areas[0] is top or left, areas[1] is bottom or right
    private var orientation:Int = 0
    private var splitPercent:Int = 50
    private var valid = false

    def getTemplateElementName() = "splitLayout"

    override def setXmlAttributes(attrs:Attributes) {
        val splitPercentStr = attrs.getValue("splitPercent")
        if (splitPercentStr!=null)
            setSplitPercentage(splitPercentStr)
        val orientationStr = attrs.getValue("orientation")
        if (orientationStr!=null)
            setOrientation(orientationStr)

        super.setXmlAttributes(attrs)

        allocateAreas(2)
    }

    def setSplitPercentage(splitPercentStr:String) {
        val splitPercent = Integer.parseInt(splitPercentStr)
        setSplitPercentage(splitPercent)
    }

    private def setOrientation(orientationStr:String) {
        if (orientationStr.trim().equalsIgnoreCase("H"))
            setOrientation(HORIZONTAL)
        else if (orientationStr.trim().equalsIgnoreCase("V"))
            setOrientation(VERTICAL)
        else
            throw new IllegalArgumentException(
                    "Bad splitLayout orientation "+orientationStr)
                //TODO i18n
    }

    /** Set the split percentage.
     * @param splitPercent The location of the split point as a
     *        percentage of the distance from top to bottom for
     *        a vertical split or from left to right for a
     *        horizontal split.
     */
    def setSplitPercentage(splitPercent:Int) {
        if (splitPercent<0 || splitPercent>100) {
            throw new IllegalArgumentException("splitPercent="+splitPercent)
        }
        if (splitPercent!=this.splitPercent)
            valid = false
        this.splitPercent = splitPercent
    }

    /** Get the split percentage. */
    def getSplitPercentage() = splitPercent

    /** Set the orientation of the split.
     * @param orientation The relative position of the two halves
     *        of the split, either VERTICAL or HORIZONTAL.
     */
    def setOrientation(orientation:Int) {
        if (orientation!=VERTICAL && orientation!=HORIZONTAL)
            throw new IllegalArgumentException("orientation="+orientation)
        if (orientation!=this.orientation)
            valid = false
        this.orientation = orientation
    }

    def getOrientation() = orientation

    //Set up or modify our areas array
    def revalidate() {
        if (valid)
            return             //nothing required
        val newAreas:Array[AreaLayout] = allocateAreas()
        if (areas!=null)
            transferImages(areas,newAreas)
        areas = newAreas
        revalidateChildren()
    }

    private def allocateAreas():Array[AreaLayout] = {
        val b:Rectangle = getBoundsInMargin()
        val aa:Array[AreaLayout] = new Array[AreaLayout](2)
        orientation match {
            case VERTICAL =>
                val h0 = (b.height - spacing.height) * splitPercent / 100
                val h1 = b.height - spacing.height - h0
                val y1 = b.y + h0 + spacing.height
                aa(0) = new AreaImageLayout(b.x,b.y,b.width,h0)
                aa(0).setBorderThickness(getBorderThickness())
                aa(1) = new AreaImageLayout(b.x,y1,b.width,h1)
                aa(1).setBorderThickness(getBorderThickness())
            case HORIZONTAL =>
                val w0 = (b.width - spacing.width) * splitPercent / 100
                val w1 = b.width - spacing.width - w0
                val x1 = b.x + w0 + spacing.width
                aa(0) = new AreaImageLayout(b.x,b.y,w0,b.height)
                aa(0).setBorderThickness(getBorderThickness())
                aa(1) = new AreaImageLayout(x1,b.y,w1,b.height)
                aa(1).setBorderThickness(getBorderThickness())
        }
        aa
    }

    private def transferImages(fromAreas:Array[AreaLayout],
            toAreas:Array[AreaLayout]) {
        for (i <- 0 until 2) {
            val fromArea:AreaLayout = fromAreas(i)
            val toArea:AreaLayout = toAreas(i)
            fromArea.setBounds(toArea.getBounds())
                //change size of old to same as new
            toAreas(i) = fromArea
                //then use old as new
        }
    }

    override protected def writeTemplateElementAttributes(
            pw:PrintWriter, indent:Int) {
        super.writeTemplateElementAttributes(pw,indent)
        val orientationStr = if (orientation==VERTICAL) "V" else "H"
        pw.print(" orientation=\""+orientationStr+"\"")
        pw.print(" splitPercent=\""+splitPercent+"\"")
    }
}
