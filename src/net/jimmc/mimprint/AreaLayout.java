/* AreaLayout.java
 *
 * Jim McBeath, October 25, 2005
 */

package net.jimmc.mimprint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.util.Vector;

import org.xml.sax.Attributes;

/** A collection of image areas or nested AreaLayouts.
 * Subclasses must provide a way to define the areas.
 */
public abstract class AreaLayout {
    
    //Our bounding box
    protected Rectangle bounds;

    //Margins inside our bounding box
    protected Insets margins;

    //Our internal spacing
    protected Dimension spacing;        //width is width of gap between areas

    //The thickness to draw our area borders
    protected int borderThickness;

    //Border colors
    private Color selectedColor;
    private Color highlightedColor;

    //The depth of this area within the tree
    private int treeDepth;
    //The tree location string of this area
    private String treeLocation;

    //Our parent area
    protected AreaLayout parent;

    //Our areas or sublayouts
    protected AreaLayout areas[];

    private int numAreas;       //number of areas set into areas
        //this is used when loading from an XML file

    public AreaLayout() {
        selectedColor = Color.blue;
        highlightedColor = Color.green;
        setSpacing(0);
        setMargins(0);
    }

    /** Set our parent layout.
     * @param parent Our parent layout, or null if we are the top level layout.
     */
    protected void setParent(AreaLayout parent) {
        this.parent = parent;
    }

    /** Get our parent layout.
     * @return parent Our parent layout, or null if we are the top level layout.
     */
    protected AreaLayout getParent() {
        return parent;
    }

    /** Set the tree depth of this area. */
    protected void setTreeDepth(int n) {
        this.treeDepth = n;
    }

    /** Get the tree depth of this area. */
    public int getTreeDepth() {
        return treeDepth;
    }

    /** Assign tree depths for all of our subs.
     * This assumes our own depth has already been set.
     * Recurses down the tree.
     */
    public void setSubTreeDepths() {
        if (areas==null)
            return;     //no subareas
        for (int i=0; i<areas.length; i++) {
            areas[i].setParent(this);
            areas[i].setTreeDepth(this.treeDepth+1);
            areas[i].setSubTreeDepths();
        }
    }

    /** Set the tree location of this area. */
    protected void setTreeLocation(String s) {
        if (s==null)
            s = "";
        this.treeLocation = s;
    }

    /** Get the tree location of this area. */
    public String getTreeLocation() {
        return treeLocation;
    }

    /** Assign tree locations for all of our subs.
     * This assumes our own location has already been set.
     * Recurses down the tree.
     */
    public void setSubTreeLocations() {
        if (areas==null)
            return;     //no subareas
        for (int i=0; i<areas.length; i++) {
            String sub = getSubTreeLocationPart(i);
            areas[i].setParent(this);
            areas[i].setTreeLocation(this.treeLocation+sub);
            areas[i].setSubTreeLocations();
        }
    }

    //Get the sublocation for one of our items based on the area index
    //of that item.
    protected String getSubTreeLocationPart(int index) {
        if (index<0)
            return "?";
        if (index<26)
            return "abcdefghijklmnopqrstuvwxyz".substring(index,index+1);
        return "."+Integer.toString(index+1);
    }

    /** Allocate our array of areas.
     * @param n The number of areas to allocated.
     */
    protected void allocateAreas(int n) {
        areas = new AreaLayout[n];
        numAreas = 0;
    }

    /** Add an area to our list.
     * This is used when loading from an XML file.
     */
    protected void addAreaLayout(AreaLayout area) {
        if (areas==null)
            throw new RuntimeException("areas not yet allocated");
        if (numAreas>=areas.length)
            throw new RuntimeException("too many areas added");
        if (areas[numAreas]!=null)
            throw new RuntimeException("area["+numAreas+"] already allocated");
        areas[numAreas++] = area;
    }

    /** Get a list of our sub areas, recursively.
     * Add to the specified Vector.
     */
    protected void getAreaList(Vector v) {
        if (areas==null)
            return;
        for (int i=0; i<areas.length; i++) {
            v.addElement(areas[i]);
            areas[i].getAreaList(v);
        }
    }

    /** Used during construction from an XML file.
     */
    public void setXmlAttributes(Attributes attrs) {
        String marginStr = attrs.getValue("margin");
        if (marginStr!=null)
            setMargins(marginStr);

        /* use <margin> element instead...
        marginStr = attrs.getValue("marginLeft");
        if (marginStr!=null)
            margins.left = PageLayout.parsePageValue(marginStr);
        marginStr = attrs.getValue("marginRight");
        if (marginStr!=null)
            margins.right = PageLayout.parsePageValue(marginStr);
        marginStr = attrs.getValue("marginTop");
        if (marginStr!=null)
            margins.top = PageLayout.parsePageValue(marginStr);
        marginStr = attrs.getValue("marginBottom");
        if (marginStr!=null)
            margins.bottom = PageLayout.parsePageValue(marginStr);
        */

        String spacingStr = attrs.getValue("spacing");
        if (spacingStr!=null)
            setSpacing(spacingStr);

        /* use <spacing> element instead ....
        spacingStr = attrs.getValue("spacingWidth");
        if (spacingStr!=null)
            spacing.width = PageLayout.parsePageValue(spacingStr);
        spacingStr = attrs.getValue("spacingHeight");
        if (spacingStr!=null)
            spacing.height = PageLayout.parsePageValue(spacingStr);
        */
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
        if (margins!=null) {
            b.x += margins.left;
            b.y += margins.top;
            b.width -= (margins.left+margins.right);
            b.height -= (margins.top+margins.bottom);
        }
        return b;
    }

    /** Set the margins all to the same value.
     * @param margin The margin value to use on all four sides
     *        of our area.
     */
    public void setMargins(int margin) {
        this.margins = new Insets(margin,margin,margin,margin);
    }

    /** Set the margins to different values.
     */
    public void setMargins(String marginsStr) {
        String[] marginStrs = marginsStr.split(",");
        setMargins(PageLayout.parsePageValue(marginStrs[0]));
        if (marginStrs.length>1) {
            //If 2 or 3 margin values specified, the last value
            //is repeated for the unspecified margins
            int m = PageLayout.parsePageValue(marginStrs[1]);
            margins.right = m;
            if (marginStrs.length>2)
                m = PageLayout.parsePageValue(marginStrs[2]);
            margins.top = m;
            if (marginStrs.length>3)
                m = PageLayout.parsePageValue(marginStrs[3]);
            margins.bottom = m;
        }
    }

    /** Set the margins to the specified values.
     * @param margins The margin values to use.
     */
    public void setMargins(Insets margins) {
        this.margins = new Insets(margins.top, margins.left,
                margins.bottom, margins.right);
    }

    /** Get the margin as set by a call to setMargin. */
    public Insets getMargins() {
        return margins;
    }

    /** Set the internal spacing between areas.
     * @param spacing The spacing between areas.
     */
    public void setSpacing(int spacing) {
        this.spacing = new Dimension(spacing,spacing);
    }

    /** Set the spacings to different values.
     */
    public void setSpacing(String spacingsStr) {
        String[] spacingStrs = spacingsStr.split(",");
        setSpacing(PageLayout.parsePageValue(spacingStrs[0]));
        if (spacingStrs.length>1) {
            int m = PageLayout.parsePageValue(spacingStrs[1]);
            spacing.height = m;
        }
    }

    /** Set the spacings to the specified values.
     * @param spacing The spacing values to use.
     */
    public void setSpacing(Dimension spacing) {
        if (spacing==null)
            setSpacing(0);
        else
            this.spacing = new Dimension(spacing.width, spacing.height);
    }

    /** Get the internal spacing between areas. */
    public Dimension getSpacing() {
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
                String sub = getSubTreeLocationPart(i);
                areas[i].setParent(this);
                areas[i].setTreeLocation(this.treeLocation+sub);
                areas[i].setSubTreeLocations();
                areas[i].setTreeDepth(this.treeDepth+1);
                areas[i].setSubTreeDepths();
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

    /** Get the name of this element in an XML file. */
    public abstract String getTemplateElementName();

    /** Revalidate all of our children areas. */
    protected void revalidateChildren() {
        for (int i=0; i<areas.length; i++)
            areas[i].revalidate();
        setSubTreeLocations();
        setSubTreeDepths();
    }

    /** True if the specified point is within our bounds and margin.
     */
    public boolean hit(Point p) {
        return (p.x>=bounds.x+margins.left &&
                p.x<=bounds.x+bounds.width-margins.right &&
                p.y>=bounds.y+margins.top &&
                p.y<=bounds.y+bounds.height-margins.bottom);
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
        //If we are the highlighted area, paint our outline
        if (highlightedArea==this)
            paintOutline(g2,highlightedColor,2*borderThickness,borderThickness);
    }

    //Paint the outlines for our area.
    protected void paintOutlines(boolean drawOutlines, Graphics2D g2,
            AreaLayout currentArea, AreaLayout highlightedArea) {
        if (!drawOutlines)
            return;
        int thickness = borderThickness;
        boolean isCurrent = (currentArea==this);
        boolean isHighlighted = (highlightedArea==this);
        paintOutline(g2,null,0,thickness);
        if (isCurrent)
            paintOutline(g2,selectedColor,thickness,thickness);
        if (isHighlighted)
            paintOutline(g2,highlightedColor,2*thickness,thickness);
    }

    /** Paint an outline box for the area.
     * @param g2 The graphics context to draw with.
     * @param color The color in which to draw the outline.
     * @param expansion The amount outside the box to draw the outline.
     *        0 means draw the outline just inside our box.
     * @param thickness The thickness of the box.
     */
    private void paintOutline(Graphics2D g2, Color color,
            int expansion, int thickness) {
        Rectangle b = getBoundsInMargin();
        Color oldColor = null;
        if (color!=null) {
            oldColor = g2.getColor();
            g2.setColor(color);
        }
        g2.fillRect(b.x-expansion,b.y-expansion,
                b.width+2*expansion,thickness);   //top line and corners
        g2.fillRect(b.x-expansion,b.y+b.height+expansion-thickness,
                b.width+2*expansion,thickness);   //bottom line and corners
        g2.fillRect(b.x-expansion,b.y-expansion+thickness,
                thickness,b.height+2*expansion-2*thickness);
                //left line without corners
        g2.fillRect(b.x+b.width+expansion-thickness,
                b.y-expansion+thickness,
                thickness,b.height+2*expansion-2*thickness);
                //right line without corners
        if (oldColor!=null)
            g2.setColor(oldColor); //restore previous color
    }

    protected void writeTemplate(PrintWriter pw, int indent) {
        pw.print(getIndentString(indent));
        pw.print("<"+getTemplateElementName());
        writeTemplateElementAttributes(pw,indent);
        boolean bodyStarted = false;

        //If margins are not all the same, write <margins> element
        if (margins.left!=margins.right || margins.right!=margins.top ||
                margins.top!=margins.bottom) {
            if (!bodyStarted) {
                pw.println(">");
                bodyStarted = true;
            }
            printlnIndented(pw,indent+1,"<margins"+
                " left=\""+PageLayout.formatPageValue(margins.left)+"\""+
                " right=\""+PageLayout.formatPageValue(margins.right)+"\""+
                " top=\""+PageLayout.formatPageValue(margins.top)+"\""+
                " bottom=\""+PageLayout.formatPageValue(margins.bottom)+"\""+
                "/>");
        }

        //If spacing not the same, write <spacing> element
        if (spacing.width!=spacing.height) {
            if (!bodyStarted) {
                pw.println(">");
                bodyStarted = true;
            }
            printlnIndented(pw,indent+1,"<spacing"+
                " width=\""+PageLayout.formatPageValue(spacing.width)+"\""+
                " height=\""+PageLayout.formatPageValue(spacing.height)+"\""+
                "/>");
        }

        if (areas!=null) {
            if (!bodyStarted) {
                pw.println(">");
                bodyStarted = true;
            }
            for (int i=0; i<areas.length; i++) {
                areas[i].writeTemplate(pw,indent+1);
            }
        }
        if (bodyStarted) {
            printlnIndented(pw,indent,"</"+getTemplateElementName()+">");
        } else {
            pw.println("/>");
        }
    }

    protected void writeTemplateElementAttributes(PrintWriter pw, int indent) {
        if (margins.left==margins.right && margins.right==margins.top &&
                margins.top==margins.bottom) {
            //All margins are the same
            pw.print(" margin=\""+
                    PageLayout.formatPageValue(margins.left)+"\"");
        } else {
            //margins are not all the same, print them all
            /* print separate <margins> element later....
            pw.print(" marginLeft=\""+
                    PageLayout.formatPageValue(margins.left)+"\"");
            pw.print(" marginRight=\""+
                    PageLayout.formatPageValue(margins.right)+"\"");
            pw.print(" marginTop=\""+
                    PageLayout.formatPageValue(margins.top)+"\"");
            pw.print(" marginBottom=\""+
                    PageLayout.formatPageValue(margins.bottom)+"\"");
            */
        }

        if (spacing.width==spacing.height) {
            //Both spacings are the same
            pw.print(" spacing=\""+
                    PageLayout.formatPageValue(spacing.width)+"\"");
        } else {
            //Spacings are different, print both
            /* Print separate <spacing> element later....
            pw.print(" spacingWidth=\""+
                    PageLayout.formatPageValue(spacing.width)+"\"");
            pw.print(" spacingHeight=\""+
                    PageLayout.formatPageValue(spacing.height)+"\"");
            */
        }
    }

    //Add all of our areas to the specified PlayList as items
    protected void addToPlayList(PlayList playList) {
        if (areas==null)
            return;     //no subareas
        for (int i=0; i<areas.length; i++) {
            areas[i].addToPlayList(playList);
        }
    }

    protected void printlnIndented(PrintWriter pw, int indent, String s) {
        pw.print(getIndentString(indent));
        pw.println(s);
    }

    protected String getIndentString(int indent) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<indent; i++)
            sb.append("    ");
        return sb.toString();
    }
}
