/* AreaLayout.java
 *
 * Jim McBeath, October 25, 2005
 */

package jimmc.jiviewer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/** A collection of image areas or nested AreaLayouts.
 * Subclasses must provide a way to define the areas.
 */
public abstract class AreaLayout {
    
    //Our bounding box
    protected Rectangle bounds;

    //Margins inside our bounding box
    protected int margin;

    //Our internal spacing
    protected int spacing;

    //The thickness to draw our area borders
    protected int borderThickness;

    //Our areas or sublayouts
    protected AreaLayout areas[];

    public AreaLayout() {
    }

    /** Set the bounding area for this AreaLayout within our parent. */
    public void setBounds(int x, int y, int width, int height) {
        bounds = new Rectangle(x, y, width, height);
    }

    /** Set the bounding area for this AreaLayout within our parent. */
    public void setBounds(Rectangle bounds) {
        this.bounds = new Rectangle(bounds);
    }

    /** Get the bounds of this area. */
    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    /** Get the bounds of this area once the margin is taken into account. */
    public Rectangle getBoundsInMargin() {
        Rectangle b = new Rectangle(bounds);
        b.x += margin;
        b.y += margin;
        b.width -= 2*margin;
        b.height -= 2*margin;
        return b;
    }

    /** Set the margins.
     * @param margin The margin value to use on all four sides
     *        of our area.
     */
    public void setMargin(int margin) {
        this.margin = margin;
    }

    /** Get the margin as set by a call to setMargin. */
    public int getMargin() {
        return margin;
    }

    /** Set the internal spacing between areas.
     * @param spacing The spacing between areas.
     */
    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    /** Get the internal spacing between areas. */
    public int getSpacing() {
        return spacing;
    }

    /** Set the thickness to draw our borders. */
    public void setBorderThickness(int thickness) {
        this.borderThickness = thickness;
    }

    /** Get our border thickness. */
    public int getBorderThickness() {
        return borderThickness;
    }

    /** Replace one of our areas with a new area.
     * @param oldArea The area to be replaced.
     * @param newArea The new area to put in its place.
     * @return True if we found and replaced the old area,
     *         false if we did not find the old area.
     */
    public boolean replaceArea(AreaLayout oldArea, AreaLayout newArea) {
        for (int i=0; i<areas.length; i++) {
            if (areas[i]==oldArea) {
                areas[i] = newArea;
                return true;
            }
        }
        return false;   //not found
    }

    /** Make sure our areas are correct.
     * Call this after calling any of the setXxx methods
     * that change any geometry parameters.
     */
    public abstract void revalidate();

    /** Revalidate all of our children areas. */
    protected void revalidateChildren() {
        for (int i=0; i<areas.length; i++)
            areas[i].revalidate();
    }

    /** True if the specified point is within our bounds and margin.
     */
    public boolean hit(Point p) {
        return (p.x>=bounds.x+margin && p.x<=bounds.x+bounds.width-margin &&
                p.y>=bounds.y+margin && p.y<=bounds.y+bounds.height-margin);
    }

    /** Get the area containing the specified point.
     * @param point A point in our coordinate space.
     * @return An area from our list that contains the point,
     *         or null if none of our areas contain the point.
     */
    public AreaLayout getArea(Point point) {
        if (areas==null)
            return null;
        for (int i=0; i<areas.length; i++) {
            if (areas[i].hit(point)) {
                return areas[i];
            }
        }
        return null;
    }

    /** Paint all of our areas. */
    public void paint(Graphics2D g2, AreaLayout currentArea,
            AreaLayout highlightedArea, boolean drawOutlines) {
        //paint each of our image page areas
        if (areas==null)
            return;
        for (int i=0; i<areas.length; i++) {
            areas[i].paint(g2,currentArea,highlightedArea,drawOutlines);
        }
    }
}
