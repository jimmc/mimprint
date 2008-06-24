/* AreaGridLayout.scala
 *
 * Jim McBeath, October 25, 2005
 * converted to Scala June 21, 2008
 */

package net.jimmc.mimprint

import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.io.PrintWriter

import org.xml.sax.Attributes

/** A regular array of areas. */
class AreaGridLayout extends AreaLayout {

    //We store our areas in row-major order,
    //i.e. areas(row*columnCount+column)
    var columnCount:Int = _
    var rowCount:Int = _

    def getTemplateElementName() = "gridLayout"

    override def setXmlAttributes(attrs:Attributes) {
        val rowsStr = attrs.getValue("rows")
        val colsStr = attrs.getValue("columns")
        if (rowsStr==null || colsStr==null ||
                rowsStr.trim().equals("") || colsStr.trim().equals("")) {
            throw new IllegalArgumentException(
                "rows and columns attributes are both required on gridLayout") //TODO i18n
        }
        val rows = Integer.parseInt(rowsStr)
        val cols = Integer.parseInt(colsStr)
        setRowColumnCounts(rows,cols)
        
        super.setXmlAttributes(attrs)

        allocateAreas(rowCount*columnCount)
    }

    /** Set the number of areas in each dimension.
     * @param rowCount The number of rows.
     * @param columnCount The number of columns.
     */
    def setRowColumnCounts(rowCount:Int, columnCount:Int) {
        this.rowCount = rowCount
        this.columnCount = columnCount
    }

    private def areaIndex(row:Int, column:Int) = row*columnCount + column

    //Allocate array of areas to match columnCount and rowCount
    override def allocateAreas():Array[AreaLayout] = {
        val b:Rectangle = getBoundsInMargin()
        val aa:Array[AreaLayout] = new Array[AreaLayout](rowCount*columnCount)
        val w = (b.width - (columnCount-1)*spacing.width)/columnCount
                //width of each area
        val h = (b.height - (rowCount-1)*spacing.height)/rowCount
                //height of each area
        for (row <- 0 until rowCount; col <- 0 until columnCount) {
            val ax = b.x+col*(w+spacing.width)
            val ay = b.y+row*(h+spacing.height)
            val area = new AreaImageLayout(ax,ay,w,h)
            area.setBorderThickness(getBorderThickness())
            aa(areaIndex(row,col)) = area
        }
        aa
    }

    override protected def writeTemplateElementAttributes(
            pw:PrintWriter, indent:Int) {
        super.writeTemplateElementAttributes(pw,indent)
        pw.print(" rows=\""+rowCount+"\"")
        pw.print(" columns=\""+columnCount+"\"")
    }
}
