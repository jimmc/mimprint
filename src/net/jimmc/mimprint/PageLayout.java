/* PageLayout.java
 *
 * Jim McBeath, November 3, 2005
 */

package jimmc.jiviewer;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.NumberFormat;

/** The layout for a page of images.
 */
public class PageLayout {
    private static final int BORDER_THICKNESS = 20;

    private String description; //description of this layout

    private int pageUnit;    //name of our units, e.g. "in", "cm"
        public final static int UNIT_CM = 0;    //metric
        public final static int UNIT_INCH = 1;  //english
        public final static int UNIT_MULTIPLIER = 1000;
    //The actual values we store in our various dimension fields
    //are the units times the multiplier.  For example, we
    //would represent 8.5 inches as 8500.
    private int pageWidth;      //width of the page in pageUnits
    private int pageHeight;     //height of the page in pageUnits

    private AreaLayout areaLayout;

    /** Create a PageLayout. */
    public PageLayout() {
        //nothing here
    }

    public void setDefaultLayout() {
        pageUnit = UNIT_INCH;
        pageWidth = 8500;       //American standard paper size
        pageHeight = 11000;

        int margin = 500;       //margin on outer edges
        int spacing = 250;      //spacing between areas
        int rowCount = 2;        //default number of rows
        int columnCount = 2;

        //areaLayout = new AreaGridLayout();
        areaLayout = new ImagePageArea(0,0,0,0);

        setAreaLayoutBounds();
        areaLayout.setMargin(margin);
        areaLayout.setSpacing(spacing);
        areaLayout.setBorderThickness(BORDER_THICKNESS);
        if (areaLayout instanceof AreaGridLayout)
            ((AreaGridLayout)areaLayout).setRowColumnCounts(rowCount,columnCount);
        areaLayout.revalidate();        //set up areas

        //For testing, throw in a Split
        //AreaSplitLayout splitArea = new AreaSplitLayout();
        //splitArea.setBorderThickness(BORDER_THICKNESS);
        //areaLayout.areas[3] = splitArea;        //hack
        //areaLayout.revalidate();        //make sure Split gets set up properly
    }

    private void setAreaLayoutBounds() {
        areaLayout.setBounds(0,0,pageWidth,pageHeight);
    }

    /** Set the descriptive text for this layout. */
    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** Set the page units.
     * @param unit One of UNIT_CM or UNIT_INCH.
     */
    public void setPageUnit(int unit) {
        if (unit==pageUnit)
            return;     //no change
        switch (unit) {
        case UNIT_CM:
        case UNIT_INCH:
            pageUnit = unit;
            break;
        default:
            throw new IllegalArgumentException("bad units "+unit);
        }
    }

    /** Get the current page units, either UNIT_CM or UNIT_INCH. */
    public int getPageUnit() {
        return pageUnit;
    }

    public void setPageWidth(int width) {
        this.pageWidth = width;
        setAreaLayoutBounds();
        areaLayout.revalidate();
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public void setPageHeight(int height) {
        this.pageHeight = height;
        setAreaLayoutBounds();
        areaLayout.revalidate();
    }

    public int getPageHeight() {
        return pageHeight;
    }

    protected void setAreaLayout(AreaLayout areaLayout) {
        this.areaLayout = areaLayout;
    }

    protected AreaLayout getAreaLayout() {
        return areaLayout;
    }

    /** Write the current layout template. */
    protected void writeLayoutTemplate(PrintWriter pw) {
        pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");  //XML header line
        //TODO - write out DTD line?
        String pageLineFmt = "<page width=\"{0}\" height=\"{1}\" unit=\"{2}\">";
        Object[] pageLineArgs = {
            formatPageValue(getPageWidth()),
            formatPageValue(getPageHeight()),
            (getPageUnit()==PageLayout.UNIT_CM)?"cm":"in"
        };
        pw.println(MessageFormat.format(pageLineFmt,pageLineArgs));
        if (description!=null)
            pw.println("    <description>"+description+"</description>");
        areaLayout.writeTemplate(pw,1);
        pw.println("</page>");
    }

    protected static String formatPageValue(int n) {
        if (pageValueFormat==null) {
            pageValueFormat = NumberFormat.getNumberInstance();
            pageValueFormat.setMaximumFractionDigits(3);
        }
        double d = ((double)n)/UNIT_MULTIPLIER;
        return pageValueFormat.format(new Double(d));
    }
    private static NumberFormat pageValueFormat;
}
