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

public class ImagePageArea extends AreaLayout {
    private Color selectedColor;
    private Color highlightedColor;

    private ImageBundle imageBundle;

    //We do not use the areas array in our parent class

    /** Create an image area. */
    public ImagePageArea(int x, int y, int width, int height) {
        super();
        setBounds(x,y,width,height);
        selectedColor = Color.blue;
        highlightedColor = Color.green;
    }

    /** We are always valid. */
    public void revalidate() {
        //do nothing
    }

    /** Get the path to our image, or null if no image. */
    public String getImagePath() {
        if (imageBundle==null)
            return null;
        return imageBundle.getPath();
    }

    /** Get the image displayed in this area. */
    public ImageBundle getImageBundle() {
        return imageBundle;
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
    public void paint(Graphics2D g2p, AreaLayout currentArea,
            AreaLayout highlightedArea, boolean drawOutlines) {
        Graphics2D g2 = (Graphics2D)g2p.create();
            //make a copy of our caller's gc so our changes don't
            //affect the caller.
        boolean isCurrent = (currentArea==this);
        boolean isHighlighted = (highlightedArea==this);
        int thickness = borderThickness;
        if (drawOutlines) {
            paintOutline(g2,null,0,thickness);
            if (isCurrent)
                paintOutline(g2,selectedColor,thickness,thickness);
            if (isHighlighted)
                paintOutline(g2,highlightedColor,2*thickness,thickness);
        }
        paintImage(g2); //this changes the transformation in g2
        g2.dispose();
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
        if (color!=null)
            g2.setColor(color);
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
    }

    private void paintImage(Graphics2D g2) {
        if (imageBundle==null)
            return;     //no image to paint
        Rectangle b = getBoundsInMargin();
        Image image = imageBundle.getTransformedImage();
        AffineTransform transform = new AffineTransform();
        g2.translate(b.x,b.y);
        ImagePage.scaleAndTranslate(g2,image.getWidth(null),image.getHeight(null),b.width,b.height);
        g2.drawImage(image,transform,null);
    }
}
