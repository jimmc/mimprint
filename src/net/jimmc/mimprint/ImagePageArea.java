/* ImagePageArea.java
 *
 * Jim McBeath, October 7, 2005
 */

package jimmc.jiviewer;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

public class ImagePageArea {
    private Color selectedColor;
    private Color highlightedColor;
    private int x;      //location of this area on the page
    private int y;
    private int width;  //size of this area
    private int height;
        //the units are whatever units the ImagePage is using

    private ImageBundle imageBundle;

    /** Create an image area. */
    public ImagePageArea(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        selectedColor = Color.blue;
        highlightedColor = Color.green;
    }

    /** Get the bounds of this area. */
    public Rectangle getBounds() {
        return new Rectangle(x,y,width,height);
    }

    /** Get the path to our image, or null if no image. */
    public String getImagePath() {
        if (imageBundle==null)
            return null;
        return imageBundle.getPath();
    }

    /** True if the specified point is in our bounds. */
    public boolean hit(Point p) {
        return (p.x>=x && p.x<=x+width &&
                p.y>=y && p.y<=y+height);
    }

    /** Set the image to be displayed in this area. */
    public void setImage(ImageBundle image) {
        this.imageBundle = image;
    }

    /** Rotate our image.  Caller is responsible for refreshing the screen. */
    public void rotate(int quarters) {
        if (imageBundle==null)
            return;
        imageBundle.rotate(quarters);
    }

    /** Paint our image on the page. */
    public void paint(Graphics2D g2, int thickness, boolean isCurrent,
            boolean isHighlighted, boolean drawOutlines) {
        if (drawOutlines) {
            paintOutline(g2,null,0,thickness);
            if (isCurrent)
                paintOutline(g2,selectedColor,thickness,thickness);
            if (isHighlighted)
                paintOutline(g2,highlightedColor,2*thickness,thickness);
        }
        paintImage(g2); //this changes the transformation in g2
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
        if (color!=null)
            g2.setColor(color);
        g2.fillRect(x-expansion,y-expansion,
                width+2*expansion,thickness);   //top line and corners
        g2.fillRect(x-expansion,y+height+expansion-thickness,
                width+2*expansion,thickness);   //bottom line and corners
        g2.fillRect(x-expansion,y-expansion+thickness,
                thickness,height+2*expansion-2*thickness);
                //left line without corners
        g2.fillRect(x+width+expansion-thickness,y-expansion+thickness,
                thickness,height+2*expansion-2*thickness);
                //right line without corners
    }

    private void paintImage(Graphics2D g2) {
        if (imageBundle==null)
            return;     //no image to paint
        Image image = imageBundle.getTransformedImage();
        AffineTransform transform = new AffineTransform();
        g2.translate(x,y);
        ImagePage.scaleAndTranslate(g2,image.getWidth(null),image.getHeight(null),width,height);
        g2.drawImage(image,transform,null);
    }
}
