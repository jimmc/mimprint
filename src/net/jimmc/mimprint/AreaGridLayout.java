/* AreaGridLayout.java
 *
 * Jim McBeath, October 25, 2005
 */

package jimmc.jiviewer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;

import org.xml.sax.Attributes;

/** A regular array of areas. */
public class AreaGridLayout extends AreaLayout {

    //We store our areas in row-major order,
    //i.e. areas[row*columnCount+column]
    private int columnCount;
    private int rowCount;

    //These track the values for the currently allocated areas array
    private int areasColumnCount;
    private int areasRowCount;

    public AreaGridLayout() {
        super();
    }

    public String getTemplateElementName() {
        return "gridLayout";
    }

    public void setXmlAttributes(Attributes attrs) {
        String rowsStr = attrs.getValue("rows");
        String colsStr = attrs.getValue("columns");
        if (rowsStr==null || colsStr==null ||
                rowsStr.trim().equals("") || colsStr.trim().equals("")) {
            throw new IllegalArgumentException(
                "rows and columns attributes are both required on gridLayout"); //TODO i18n
        }
        int rows = Integer.parseInt(rowsStr);
        int cols = Integer.parseInt(colsStr);
        setRowColumnCounts(rows,cols);
        
        super.setXmlAttributes(attrs);

        allocateAreas(rowCount*columnCount);
        areasColumnCount = columnCount;
        areasRowCount = rowCount;
    }

    /** Set the number of areas in each dimension.
     * @param rowCount The number of rows.
     * @param columnCount The number of columns.
     */
    public void setRowColumnCounts(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    private int areaIndex(int row, int column) {
        return row*columnCount + column;
    }

    //Set up or modify our areas array
    public void revalidate() {
        if (columnCount==0 || rowCount==0) {
            areas = null;
            areasRowCount = 0;
            areasColumnCount = 0;
            return;
        }
        AreaLayout[] newAreas = allocateAreas();
        if (areas!=null)
            transferImages(areas,newAreas);
        areas = newAreas;
        revalidateChildren();
        areasRowCount = rowCount;
        areasColumnCount = columnCount;
    }

    //Allocate array of areas to match columnCount and rowCount
    private AreaLayout[] allocateAreas() {
        Rectangle b = getBoundsInMargin();
        AreaLayout[] aa = new AreaLayout[rowCount*columnCount];
        int w = (b.width - (columnCount-1)*spacing)/columnCount;    //width of each area
        int h = (b.height - (rowCount-1)*spacing)/rowCount;   //height of each area
        for (int row=0; row<rowCount; row++) {
            for (int col=0; col<columnCount; col++) {
                int ax = b.x+col*(w+spacing);
                int ay = b.y+row*(h+spacing);
                ImagePageArea area = new ImagePageArea(ax,ay,w,h);
                area.setBorderThickness(getBorderThickness());
                aa[areaIndex(row,col)] = area;
            }
        }
        return aa;
    }

    //Transfer the images from the old areas to the new.
    private void transferImages(AreaLayout[] fromAreas,
            AreaLayout[] toAreas) {
        boolean rearrange =
                (rowCount>areasRowCount && columnCount<areasColumnCount) ||
                (rowCount<areasRowCount && columnCount>areasColumnCount);
        if (rearrange) {
            //TODO - pack them in, don't preserve row/col position
        }
        //Transfer to same x/y position, whatever fits
        int cc = (columnCount<areasColumnCount)?columnCount:areasColumnCount;     //min
        int rr = (rowCount<areasRowCount)?rowCount:areasRowCount;     //min
        for (int row=0; row<rr; row++) {
            for (int col=0; col<cc; col++) {
                AreaLayout fromArea = fromAreas[row*areasColumnCount+col];
                AreaLayout toArea = toAreas[row*columnCount+col];
                fromArea.setBounds(toArea.getBounds());
                    //Change the old size to the new size
                toAreas[row*columnCount+col] = fromArea;
                    // then save the old as the new
            }
        }
    }

    protected void writeTemplateElementAttributes(PrintWriter pw, int indent) {
        super.writeTemplateElementAttributes(pw,indent);
        pw.print(" rows=\""+rowCount+"\"");
        pw.print(" columns=\""+columnCount+"\"");
    }
}
