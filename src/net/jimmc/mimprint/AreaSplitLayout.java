/* AreaSplitLayout.java
 *
 * Jim McBeath, October 25, 2005
 */

package jimmc.jiviewer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/** Like AWT SplitPane, split into two areas of possibly unequal size. */
public class AreaSplitLayout extends AreaLayout {

    //areas[0] is top or left, areas[1] is bottom or right
    private int orientation;
        public static final int VERTICAL = 0;   //top-bottom split
        public static final int HORIZONTAL = 1; //left-right split
    private int splitPercent;
    private boolean valid;

    public AreaSplitLayout() {
        super();
        valid = false;
        splitPercent = 50;
    }

    /** Set the split percentage.
     * @param splitPercent The location of the split point as a
     *        percentage of the distance from top to bottom for
     *        a vertical split or from left to right for a
     *        horizontal split.
     */
    public void setSplitPercentage(int splitPercent) {
        if (splitPercent<0 || splitPercent>100) {
            throw new IllegalArgumentException("splitPercent="+splitPercent);
        }
        if (splitPercent!=this.splitPercent)
            valid = false;
        this.splitPercent = splitPercent;
    }

    /** Get the split percentage. */
    public int getSplitPercentage() {
        return splitPercent;
    }

    /** Set the number of areas in each dimension.
     * @param rowCount The number of rows.
     * @param columnCount The number of columns.
     */
    public void setOrientation(int orientation) {
        switch(orientation) {
        case VERTICAL:
        case HORIZONTAL:
            break;
        default:
            throw new IllegalArgumentException("orientation="+orientation);
        }
        if (orientation!=this.orientation)
            valid = false;
        this.orientation = orientation;
    }

    public int getOrientation() {
        return orientation;
    }

    //Set up or modify our areas array
    public void revalidate() {
        if (valid)
            return;             //nothing required
        AreaLayout[] newAreas = allocateAreas();
        if (areas!=null)
            transferImages(areas,newAreas);
        areas = newAreas;
        revalidateChildren();
    }

    private AreaLayout[] allocateAreas() {
        Rectangle b = getBoundsInMargin();
        AreaLayout[] aa = new AreaLayout[2];
        switch (orientation) {
        case VERTICAL:
            int h0 = (b.height - spacing) * splitPercent / 100;
            int h1 = b.height - spacing - h0;
            int y1 = b.y + h0 + spacing;
            aa[0] = new ImagePageArea(b.x,b.y,b.width,h0);
            aa[0].setBorderThickness(getBorderThickness());
            aa[1] = new ImagePageArea(b.x,y1,b.width,h1);
            aa[1].setBorderThickness(getBorderThickness());
            break;
        case HORIZONTAL:
            int w0 = (b.width - spacing) * splitPercent / 100;
            int w1 = b.width - spacing - w0;
            int x1 = b.x + w0 + spacing;
            aa[0] = new ImagePageArea(b.x,b.y,w0,b.height);
            aa[0].setBorderThickness(getBorderThickness());
            aa[1] = new ImagePageArea(x1,b.y,w1,b.height);
            aa[1].setBorderThickness(getBorderThickness());
            break;
        }
        return aa;
    }

    private void transferImages(AreaLayout[] fromAreas,
            AreaLayout[] toAreas) {
        for (int i=0; i<2; i++) {
            AreaLayout fromArea = fromAreas[i];
            AreaLayout toArea = toAreas[i];
            fromArea.setBounds(toArea.getBounds());
                //change size of old to same as new
            toAreas[i] = fromArea;
                //then use old as new
        }
    }
}
