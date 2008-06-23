/* PageLayout.scala
 *
 * Jim McBeath, November 3, 2005 (as PageLayout.java)
 * converted to scala June 21, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.SResources

import java.awt.Dimension
import java.awt.Insets
import java.io.File
import java.io.PrintWriter
import java.text.MessageFormat
import java.util.Stack
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.SAXException

object PageLayout {
    val BORDER_THICKNESS = 20

    //Values for page units
    val UNIT_CM = 0    //metric
    val UNIT_INCH = 1  //english
}

/** The layout for a page of images.
 */
class PageLayout(app:SResources) {
    import PageLayout._         //get all fields from our companion object

    private var description:String = _ //description of this layout

    private var pageUnit:Int = _    //name of our units, e.g. "in", "cm"
    //The actual values we store in our various dimension fields
    //are the units times the multiplier.  For example, we
    //would represent 8.5 inches as 8500.
    private var pageWidth:Int = _      //width of the page in pageUnits
    private var pageHeight:Int = _     //height of the page in pageUnits

    private var areaLayout:AreaLayout = _

    private var currentArea:AreaLayout = _     //when loading an XML file

    def setDefaultLayout() {
        pageUnit = UNIT_INCH
        pageWidth = 8500       //American standard paper size
        pageHeight = 11000

        val margin = 500       //margin on outer edges
        val spacing = 250      //spacing between areas

        areaLayout = AreaLayoutFactory.createDefaultTopLayout()

        areaLayout.setMargins(margin)
        areaLayout.setSpacing(spacing)
        areaLayout.setBorderThickness(BORDER_THICKNESS)
        setAreaLayout(areaLayout)
    }

    private def setAreaLayoutBounds() {
        if (areaLayout!=null)
            areaLayout.setBounds(0,0,pageWidth,pageHeight)
    }

    /** Set the descriptive text for this layout. */
    def setDescription(description:String) =
        this.description = description

    def getDescription():String = description

    /** Set the page units.
     * @param unit One of UNIT_CM or UNIT_INCH.
     */
    def setPageUnit(unit:Int) {
        if (unit==pageUnit)
            return     //no change
        unit match {
            case UNIT_CM => pageUnit = unit
            case UNIT_INCH => pageUnit = unit
            case _ =>
                throw new IllegalArgumentException("bad units "+unit)
        }
    }

    /** Get the current page units, either UNIT_CM or UNIT_INCH. */
    def getPageUnit():Int = pageUnit

    def setPageWidth(width:Int) {
        this.pageWidth = width
        if (areaLayout==null)
            return
        setAreaLayoutBounds()
        areaLayout.revalidate()
    }

    def getPageWidth():Int = pageWidth

    def setPageHeight(height:Int) {
        this.pageHeight = height
        if (areaLayout==null)
            return
        setAreaLayoutBounds()
        areaLayout.revalidate()
    }

    def getPageHeight():Int = pageHeight

    //Set our top-level AreaLayout
    def setAreaLayout(areaLayout:AreaLayout) {
        this.areaLayout = areaLayout
        setAreaLayoutBounds()
        areaLayout.setBorderThickness(BORDER_THICKNESS)
        areaLayout.revalidate()
        areaLayout.setParent(null)     //top level layout
        areaLayout.setTreeLocation(null)
        areaLayout.setSubTreeLocations()
        areaLayout.setTreeDepth(0)
        areaLayout.setSubTreeDepths()
        fixImageIndexes()
    }

    def fixImageIndexes() {
        areaLayout.setImageIndexes(0)
    }

    def getAreaLayout():AreaLayout = areaLayout

    /** Build a playlist. */
    protected def retrieveIntoPlayList(playList:PlayListS):PlayListS = {
        areaLayout.retrieveIntoPlayList(playList)
    }

    /** Write the current layout template. */
    def writeLayoutTemplate(pw:PrintWriter) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>")  //XML header line
        //TODO - write out DTD line?
        val pageLineFmt = "<page width=\"{0}\" height=\"{1}\" unit=\"{2}\">"
        val pageLineArgs = Array(
            PageValue.formatPageValue(getPageWidth()),
            PageValue.formatPageValue(getPageHeight()),
            if (getPageUnit()==PageLayout.UNIT_CM) "cm" else "in"
        )
        pw.println(MessageFormat.format(pageLineFmt,pageLineArgs.
                asInstanceOf[Array[Object]]))
        if (description!=null)
            pw.println("    <description>"+description+"</description>")
        areaLayout.writeTemplate(pw,1)
        pw.println("</page>")
    }

    /** Read in the specified layout template. */
    def loadLayoutTemplate(f:File) {
        var parser:SAXParser = null
        try {
            parser = SAXParserFactory.newInstance().newSAXParser()
                //TODO - create factory only once
        } catch {
            case ex:Exception =>
                throw new RuntimeException("Exception creating SAXParser",ex)
        }
//System.out.println("Created SAXParser")
        val handler:DefaultHandler = new PageLayoutHandler()
        try {
            parser.parse(f,handler)
        } catch {
            case ex:Exception => //SAXException, IOException
                throw new RuntimeException("Error parsing xml",ex)
        }
    }

    /** Get a string from our resources. */
    def getResourceString(name:String) = app.getResourceString(name)

    /** Get a string from our resources. */
    def getResourceFormatted(name:String, arg:String) =
            app.getResourceFormatted(name, arg)

    class PageLayoutHandler extends DefaultHandler {
        private var areaStack:Stack[AreaLayout] = _
        private var lastText:String = _        //most recent parsed text

        override def startDocument() {
//System.out.println("startDocument")
            areaStack  = new Stack()
        }

        override def endDocument() {
//System.out.println("endDocument")
        }

        override def startElement(url:String, localName:String,
                qName:String, attributes:Attributes) {
//System.out.println("startElement "+uri+","+localName+","+
//                    qName+",attrs="+attributes)
            if (qName.equals("description"))
                ()       //ignore the start, pick up the text on the end
            else if (qName.equals("page"))
                loadPageAttributes(attributes)
            else if (qName.equals("margins"))
                loadMargins(attributes)
            else if (qName.equals("spacing"))
                loadSpacing(attributes)
            else {
                val newArea:AreaLayout = AreaLayoutFactory.newAreaLayout(qName)
                newArea.setBorderThickness(BORDER_THICKNESS)
                newArea.setXmlAttributes(attributes)
                //TODO - ensure that newArea.areas has been allocated
                areaStack.push(currentArea)
                currentArea = newArea
            }
        }

        private def loadMargins(attrs:Attributes) {
            val leftStr = attrs.getValue("left")
            val rightStr = attrs.getValue("right")
            val topStr = attrs.getValue("top")
            val bottomStr = attrs.getValue("bottom")
            val left = PageValue.parsePageValue(leftStr,0)
            val right = PageValue.parsePageValue(rightStr,0)
            val top = PageValue.parsePageValue(topStr,0)
            val bottom = PageValue.parsePageValue(bottomStr,0)
            if (currentArea!=null)
                currentArea.setMargins(new Insets(top,left,bottom,right))
            else
                throw new IllegalArgumentException(
                        "Can't set margins directly on a Page")
        }

        private def loadSpacing(attrs:Attributes) {
            val widthStr = attrs.getValue("width")
            val heightStr = attrs.getValue("height")
            val width = PageValue.parsePageValue(widthStr,0)
            val height = PageValue.parsePageValue(heightStr,0)
            if (currentArea!=null)
                currentArea.setSpacing(new Dimension(width,height))
            else
                throw new IllegalArgumentException(
                        "Can't set spacing directly on a Page")
        }

        private def loadPageAttributes(attrs:Attributes) {
            val heightStr = attrs.getValue("height")
            val widthStr = attrs.getValue("width")
            if (heightStr==null || widthStr==null ||
                    heightStr.trim().equals("") || widthStr.trim().equals("")) {
                val msg = getResourceString("error.PageDimensionsRequired")
                throw new IllegalArgumentException(msg)
            }
            setPageWidth(PageValue.parsePageValue(widthStr))
            setPageHeight(PageValue.parsePageValue(heightStr))
            val unitStr = attrs.getValue("unit")
            if ("cm".equalsIgnoreCase(unitStr))
                setPageUnit(UNIT_CM)
            else if ("in".equalsIgnoreCase(unitStr))
                setPageUnit(UNIT_INCH)
            else {
                val msg = getResourceString("error.BadPageUnit")
                throw new IllegalArgumentException(msg)
            }
        }

        override def characters(ch:Array[Char], start:Int, end:Int) =
            lastText = new String(ch,start,end)

        override def endElement(url:String, localName:String, qName:String) {
//System.out.println("endElement "+uri+","+localName+","+qName)
            //TODO - validate end element matches start element
            if (qName=="description") {
                if (lastText!=null)
                    setDescription(lastText)
                return
            } else if (qName=="page") {
                if (currentArea!=null) {
                    val msg = getResourceString("error.MissingEndAreaElement")
                    throw new RuntimeException(msg)
                }
                return         //done with the page
            } else if (qName=="margins") {
                return
            } else if (qName=="spacing") {
                return
            } else {
                       //nothing here
            }
            val newArea:AreaLayout = currentArea
            currentArea = areaStack.pop()
            if (currentArea==null)
                setAreaLayout(newArea) //set page layout
            else
                currentArea.addAreaLayout(newArea)
        }
    }
}
