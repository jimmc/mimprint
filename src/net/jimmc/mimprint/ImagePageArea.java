/* ImagePageArea.java
 *
 * Jim McBeath, October 7, 2005
 */

package jimmc.jiviewer;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

public class ImagePageArea {
    private Color highlightColor;
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
        highlightColor = Color.blue;
    }

    /** Get the bounds of this area. */
    public Rectangle getBounds() {
        return new Rectangle(x,y,width,height);
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
    public void paint(Graphics2D g2, int thickness, boolean isCurrent, boolean drawOutlines) {
        if (drawOutlines) {
            paintOutline(g2,thickness);
            if (isCurrent)
                paintHighlight(g2,thickness);
        }
        paintImage(g2); //this changes the transformation in g2
    }

    private void paintOutline(Graphics2D g2, int thickness) {
        g2.fillRect(x,y,width,thickness);       //top line and corners
        g2.fillRect(x,y+height-thickness,width,thickness);
                //bottom line and corners
        g2.fillRect(x,y+thickness,thickness,height-2*thickness);
                //left line without corners
        g2.fillRect(x+width-thickness,y+thickness,thickness,height-2*thickness);
                //right line without corners
    }

    private void paintHighlight(Graphics2D g2, int thickness) {
        g2.setColor(highlightColor);
        g2.fillRect(x-thickness,y-thickness,width+2*thickness,thickness);
                //top line and corners
        g2.fillRect(x-thickness,y+height,width+2*thickness,thickness);
                //bottom line and corners
        g2.fillRect(x-thickness,y,thickness,height);
                //left line without corners
        g2.fillRect(x+width,y,thickness,height);
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
