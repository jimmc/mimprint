/* AreaLayout.scala
 *
 * Jim McBeath, October 25, 2005
 * converted to scala June 21, 2008
 */

package net.jimmc.mimprint

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import java.io.PrintWriter

import scala.collection.mutable.ArrayBuffer

import org.xml.sax.Attributes

/** A collection of image areas or nested AreaLayouts.
 * Subclasses must provide a way to define the areas.
 */
abstract class AreaLayout {
   
    //Our bounding box
    protected var bounds:Rectangle = _

    //Margins inside our bounding box
    protected var margins:Insets = _

    //Our internal spacing
    protected var spacing:Dimension = _        
            //width is width of gap between areas

    //The thickness to draw our area borders
    protected var borderThickness:Int = _

    //Border colors
    private var selectedColor = Color.blue
    private var highlightedColor = Color.green
    setSpacing(0)
    setMargins(0)

    //The depth of this area within the tree
    private var treeDepth:Int = _
    //The tree location string of this area
    private var treeLocation:String = _

    //Our parent area
    protected var parent:AreaLayout = _

    //Our areas or sublayouts
    protected var areas:Array[AreaLayout] = _

    private var numAreas:Int = _       //number of areas set into areas
        //this is used when loading from an XML file

    def getArea(n:Int):AreaLayout = {
        if (areas==null || n<0 || n>=areas.length)
            return null        //no such area
        return areas(n)
    }

    //Get the number of immediate subareas
    def getAreaCount() = if (areas==null) 0 else areas.length

    //Get the number of image areas in this area and all descendents
    def getImageAreaCount():Int = {
        if (areas==null) 0
        else ((0 /: areas)((sum:Int,a:AreaLayout) => sum + a.getImageAreaCount))
    }

    /** Set our parent layout.
     * @param parent Our parent layout, or null if we are the top level layout.
     */
    def setParent(parent:AreaLayout) = this.parent = parent

    /** Get our parent layout.
     * @return parent Our parent layout, or null if we are the top level layout.
     */
    def getParent():AreaLayout = parent

    /** Set the tree depth of this area. */
    def setTreeDepth(n:Int) = this.treeDepth = n

    /** Get the tree depth of this area. */
    def getTreeDepth() = treeDepth

    /** Assign tree depths for all of our subs.
     * This assumes our own depth has already been set.
     * Recurses down the tree.
     */
    def setSubTreeDepths() {
        if (areas==null)
            return     //no subareas
        areas.foreach { area =>
            area.setParent(this)
            area.setTreeDepth(this.treeDepth+1)
            area.setSubTreeDepths()
        }
    }

    /** Set the tree location of this area. */
    def setTreeLocation(s:String) =
        this.treeLocation = if (s==null) "" else s

    /** Get the tree location of this area. */
    def getTreeLocation() = treeLocation

    /** Assign tree locations for all of our subs.
     * This assumes our own location has already been set.
     * Recurses down the tree.
     */
    def setSubTreeLocations() {
        if (areas==null)
            return     //no subareas
        for (i <- 0 until areas.length) {
            val sub = getSubTreeLocationPart(i)
            areas(i).setParent(this)
            areas(i).setTreeLocation(this.treeLocation+sub)
            areas(i).setSubTreeLocations()
        }
    }

    //Get the sublocation for one of our items based on the area index
    //of that item.
    protected def getSubTreeLocationPart(index:Int):String = {
        if (index<0)
            return "?"
        if (index<26)
            return "abcdefghijklmnopqrstuvwxyz".substring(index,index+1)
        return "."+Integer.toString(index+1)
    }

    //Set the image indexes of all of our images.
    //Return the number of image slots we have.
    def setImageIndexes(start:Int):Int = {
        val end = (start /: areas)((sum:Int,area:AreaLayout) =>
                (sum + area.setImageIndexes(sum)))
        end - start
    }

    //Subclass must provide a method to allocate its areas with the right
    //boundaries.
    protected def allocateAreas():Array[AreaLayout]

    /** Allocate our array of areas.
     * @param n The number of areas to allocated.
     */
    protected def allocateAreas(n:Int) {
        areas = new Array[AreaLayout](n)
        numAreas = 0
    }

    //Transfer the children areas from old to new, but use the
    //bounds info from the new areas.
    private def transferAreas(fromAreas:Array[AreaLayout],
            toAreas:Array[AreaLayout]) {
        val minLen = if (fromAreas.length < toAreas.length) fromAreas.length
                     else toAreas.length
        for (i <- 0 until minLen) {
            val fromArea = fromAreas(i)
            val toArea = toAreas(i)
            fromArea.setBounds(toArea.getBounds())
                    //Change the old size to the new size
            toAreas(i) = fromArea
                    // then save the old as the new
        }
    }

    /** Add an area to our list.
     * This is used when loading from an XML file.
     */
    def addAreaLayout(area:AreaLayout) {
        if (areas==null)
            throw new RuntimeException("areas not yet allocated")
        if (numAreas>=areas.length)
            throw new RuntimeException("too many areas added")
        if (areas(numAreas)!=null)
            throw new RuntimeException("area("+numAreas+") already allocated")
        areas(numAreas) = area
        numAreas = numAreas + 1
    }

    /** Get a list of our sub areas, recursively.
     * Add to the specified ArrayBuffer.
     */
    def retrieveAreaList(aa:ArrayBuffer[AreaLayout]) {
        if (areas!=null) {
            areas.foreach { area =>
                aa += area
                area.retrieveAreaList(aa)
            }
        }
    }

    /** Used during construction from an XML file.
     */
    def setXmlAttributes(attrs:Attributes) {
        var marginStr = attrs.getValue("margin")
        if (marginStr!=null)
            setMargins(marginStr)

        /* use <margin> element instead...
        marginStr = attrs.getValue("marginLeft")
        if (marginStr!=null)
            margins.left = PageValue.parsePageValue(marginStr)
        marginStr = attrs.getValue("marginRight")
        if (marginStr!=null)
            margins.right = PageValue.parsePageValue(marginStr)
        marginStr = attrs.getValue("marginTop")
        if (marginStr!=null)
            margins.top = PageValue.parsePageValue(marginStr)
        marginStr = attrs.getValue("marginBottom")
        if (marginStr!=null)
            margins.bottom = PageValue.parsePageValue(marginStr)
        */

        var spacingStr = attrs.getValue("spacing")
        if (spacingStr!=null)
            setSpacing(spacingStr)

        /* use <spacing> element instead ....
        spacingStr = attrs.getValue("spacingWidth")
        if (spacingStr!=null)
            spacing.width = PageValue.parsePageValue(spacingStr)
        spacingStr = attrs.getValue("spacingHeight")
        if (spacingStr!=null)
            spacing.height = PageValue.parsePageValue(spacingStr)
        */
    }

    /** Set the bounding area for this AreaLayout within our parent. */
    def setBounds(x:Int, y:Int, width:Int, height:Int) {
        bounds = new Rectangle(x, y, width, height)
    }

    /** Set the bounding area for this AreaLayout within our parent. */
    def setBounds(bounds:Rectangle) {
        this.bounds = new Rectangle(bounds)
    }

    /** Get the bounds of this area. */
    def getBounds() = new Rectangle(bounds)

    /** Get the bounds of this area once the margin is taken into account. */
    def getBoundsInMargin():Rectangle = {
        val b = new Rectangle(bounds)
        if (margins!=null) {
            b.x += margins.left
            b.y += margins.top
            b.width -= (margins.left+margins.right)
            b.height -= (margins.top+margins.bottom)
        }
        b
    }

    /** Set the margins all to the same value.
     * @param margin The margin value to use on all four sides
     *        of our area.
     */
    def setMargins(margin:Int) {
        this.margins = new Insets(margin,margin,margin,margin)
    }

    /** Set the margins to different values.
     */
    def setMargins(marginsStr:String) {
        val marginStrs = marginsStr.split(",")
        setMargins(PageValue.parsePageValue(marginStrs(0)))
        if (marginStrs.length>1) {
            //If 2 or 3 margin values specified, the last value
            //is repeated for the unspecified margins
            var m = PageValue.parsePageValue(marginStrs(1))
            margins.right = m
            if (marginStrs.length>2)
                m = PageValue.parsePageValue(marginStrs(2))
            margins.top = m
            if (marginStrs.length>3)
                m = PageValue.parsePageValue(marginStrs(3))
            margins.bottom = m
        }
    }

    /** Set the margins to the specified values.
     * @param margins The margin values to use.
     */
    def setMargins(margins:Insets) {
        this.margins = new Insets(margins.top, margins.left,
                margins.bottom, margins.right)
    }

    /** Get the margin as set by a call to setMargin. */
    def getMargins() = margins

    /** Set the internal spacing between areas.
     * @param spacing The spacing between areas.
     */
    def setSpacing(spacing:Int) {
        this.spacing = new Dimension(spacing,spacing)
    }

    /** Set the spacings to different values.
     */
    def setSpacing(spacingsStr:String) {
        val spacingStrs = spacingsStr.split(",")
        setSpacing(PageValue.parsePageValue(spacingStrs(0)))
        if (spacingStrs.length>1) {
            val m = PageValue.parsePageValue(spacingStrs(1))
            spacing.height = m
        }
    }

    /** Set the spacings to the specified values.
     * @param spacing The spacing values to use.
     */
    def setSpacing(spacing:Dimension) {
        if (spacing==null)
            setSpacing(0)
        else
            this.spacing = new Dimension(spacing.width, spacing.height)
    }

    /** Get the internal spacing between areas. */
    def getSpacing() = spacing

    /** Set the thickness to draw our borders. */
    def setBorderThickness(thickness:Int) {
        this.borderThickness = thickness
    }

    /** Get our border thickness. */
    def getBorderThickness() = borderThickness

    /** Replace one of our areas with a new area.
     * @param oldArea The area to be replaced.
     * @param newArea The new area to put in its place.
     * @return True if we found and replaced the old area,
     *         false if we did not find the old area.
     */
    def replaceArea(oldArea:AreaLayout, newArea:AreaLayout):Boolean = {
        //TODO - note that the image indexes of other areas will not
        //be correct after this, so they have to be updated separately.
        val i = areas.findIndexOf(_ == oldArea)
        if (i>=0) {
            areas(i) = newArea
            val sub = getSubTreeLocationPart(i)
            areas(i).setParent(this)
            areas(i).setTreeLocation(this.treeLocation+sub)
            areas(i).setSubTreeLocations()
            areas(i).setTreeDepth(this.treeDepth+1)
            areas(i).setSubTreeDepths()
            true
        }
        false   //not found
    }

    /** Get the name of this element in an XML file. */
    def getTemplateElementName():String

    /** Make sure our areas are correct.
     * Call this after calling any of the setXxx methods
     * that change any geometry parameters.
     */
    def revalidate():Unit = {
        val newAreas = allocateAreas()
        if (areas!=null)
            transferAreas(areas,newAreas)
        areas = newAreas
        revalidateChildren()
    }

    /** Revalidate all of our children areas. */
    protected def revalidateChildren() {
        if (areas==null)
            return
        areas.foreach(_.revalidate)
        setSubTreeLocations()
        setSubTreeDepths()
    }

    /** True if the specified point is within our bounds and margin.
     */
    def hit(p:Point):Boolean = {
        (p.x>=bounds.x+margins.left &&
                p.x<=bounds.x+bounds.width-margins.right &&
                p.y>=bounds.y+margins.top &&
                p.y<=bounds.y+bounds.height-margins.bottom)
    }

    /** Get the area containing the specified point.
     * @param point A point in our coordinate space.
     * @return An area from our list that contains the point,
     *         or null if none of our areas contain the point.
     */
    def getSubArea(point:Point):Option[AreaLayout] = {
        if (areas==null)
            return None
        areas.find(_.hit(point))
    }
    def getAreaLeaf(point:Point):Option[AreaLayout] = {
        getSubArea(point).map((a:AreaLayout)=>a.getAreaLeaf(point)
            getOrElse a) orElse (if (hit(point)) Some(this) else None)
    }

    /** Paint all of our areas. */
    def paint(g2:Graphics2D, currentArea:AreaLayout,
            highlightedArea:AreaLayout, drawOutlines:Boolean) {
        //paint each of our image page areas
        if (areas==null)
            return
        areas.foreach(_.paint(g2,currentArea,highlightedArea,drawOutlines))
        //If we are the highlighted area, paint our outline
        if (highlightedArea==this)
            paintOutline(g2,highlightedColor,2*borderThickness,borderThickness)
    }

    def printPage(g2:Graphics2D, comp:Component,
            playList:PlayList, start:Int):Int = {
        //paint each of our image page areas
        if (areas==null)
            return 0
        val end = (start /: areas)((sum:Int,area:AreaLayout) =>
                (sum + area.printPage(g2,comp,playList,sum)))
        end - start
    }

    //Paint the outlines for our area.
    protected def paintOutlines(drawOutlines:Boolean, g2:Graphics2D,
            currentArea:AreaLayout, highlightedArea:AreaLayout) {
        if (!drawOutlines)
            return
        val thickness = borderThickness
        val isCurrent = (currentArea==this)
        val isHighlighted = (highlightedArea==this)
        paintOutline(g2,null,0,thickness)
        if (isCurrent)
            paintOutline(g2,selectedColor,thickness,thickness)
        if (isHighlighted)
            paintOutline(g2,highlightedColor,2*thickness,thickness)
    }

    /** Paint an outline box for the area.
     * @param g2 The graphics context to draw with.
     * @param color The color in which to draw the outline.
     * @param expansion The amount outside the box to draw the outline.
     *        0 means draw the outline just inside our box.
     * @param thickness The thickness of the box.
     */
    private def paintOutline(g2:Graphics2D, color:Color,
            expansion:Int, thickness:Int) {
        val b = getBoundsInMargin()
        var oldColor:Color = null
        if (color!=null) {
            oldColor = g2.getColor()
            g2.setColor(color)
        }
        g2.fillRect(b.x-expansion,b.y-expansion,
                b.width+2*expansion,thickness)   //top line and corners
        g2.fillRect(b.x-expansion,b.y+b.height+expansion-thickness,
                b.width+2*expansion,thickness)   //bottom line and corners
        g2.fillRect(b.x-expansion,b.y-expansion+thickness,
                thickness,b.height+2*expansion-2*thickness)
                //left line without corners
        g2.fillRect(b.x+b.width+expansion-thickness,
                b.y-expansion+thickness,
                thickness,b.height+2*expansion-2*thickness)
                //right line without corners
        if (oldColor!=null)
            g2.setColor(oldColor) //restore previous color
    }

    def writeTemplate(pw:PrintWriter, indent:Int) {
        pw.print(getIndentString(indent))
        pw.print("<"+getTemplateElementName())
        writeTemplateElementAttributes(pw,indent)
        var bodyStarted = false

        //If margins are not all the same, write <margins> element
        if (margins.left!=margins.right || margins.right!=margins.top ||
                margins.top!=margins.bottom) {
            if (!bodyStarted) {
                pw.println(">")
                bodyStarted = true
            }
            printlnIndented(pw,indent+1,"<margins"+
                " left=\""+PageValue.formatPageValue(margins.left)+"\""+
                " right=\""+PageValue.formatPageValue(margins.right)+"\""+
                " top=\""+PageValue.formatPageValue(margins.top)+"\""+
                " bottom=\""+PageValue.formatPageValue(margins.bottom)+"\""+
                "/>")
        }

        //If spacing not the same, write <spacing> element
        if (spacing.width!=spacing.height) {
            if (!bodyStarted) {
                pw.println(">")
                bodyStarted = true
            }
            printlnIndented(pw,indent+1,"<spacing"+
                " width=\""+PageValue.formatPageValue(spacing.width)+"\""+
                " height=\""+PageValue.formatPageValue(spacing.height)+"\""+
                "/>")
        }

        if (areas!=null) {
            if (!bodyStarted) {
                pw.println(">")
                bodyStarted = true
            }
            areas.foreach(_.writeTemplate(pw,indent+1))
        }
        if (bodyStarted) {
            printlnIndented(pw,indent,"</"+getTemplateElementName()+">")
        } else {
            pw.println("/>")
        }
    }

    protected def writeTemplateElementAttributes(pw:PrintWriter, indent:Int) {
        if (margins.left==margins.right && margins.right==margins.top &&
                margins.top==margins.bottom) {
            //All margins are the same
            pw.print(" margin=\""+
                    PageValue.formatPageValue(margins.left)+"\"")
        } else {
            //margins are not all the same, print them all
            /* print separate <margins> element later....
            pw.print(" marginLeft=\""+
                    PageValue.formatPageValue(margins.left)+"\"")
            pw.print(" marginRight=\""+
                    PageValue.formatPageValue(margins.right)+"\"")
            pw.print(" marginTop=\""+
                    PageValue.formatPageValue(margins.top)+"\"")
            pw.print(" marginBottom=\""+
                    PageValue.formatPageValue(margins.bottom)+"\"")
            */
        }

        if (spacing.width==spacing.height) {
            //Both spacings are the same
            pw.print(" spacing=\""+
                    PageValue.formatPageValue(spacing.width)+"\"")
        } else {
            //Spacings are different, print both
            /* Print separate <spacing> element later....
            pw.print(" spacingWidth=\""+
                    PageValue.formatPageValue(spacing.width)+"\"")
            pw.print(" spacingHeight=\""+
                    PageValue.formatPageValue(spacing.height)+"\"")
            */
        }
    }

    protected def printlnIndented(pw:PrintWriter, indent:Int, s:String) {
        pw.print(getIndentString(indent))
        pw.println(s)
    }

    protected def getIndentString(indent:Int):String = {
        val sb = new StringBuffer()
        (0 until indent).foreach((x:Int) => sb.append("    "))
        return sb.toString()
    }
}
