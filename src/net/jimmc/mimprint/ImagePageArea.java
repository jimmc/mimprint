/* ImagePageArea.java
 *
 * Jim McBeath, October 7, 2005
 */

package jimmc.jiviewer;

import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

public class ImagePageArea {
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
    }

    /** Get the bounds of this area. */
    public Rectangle getBounds() {
        return new Rectangle(x,y,width,height);
    }

    /** Set the image to be displayed in this area. */
    public void setImage(ImageBundle image) {
        this.imageBundle = image;
    }

    /** Paint our image on the page. */
    public void paint(Graphics2D g2, int thickness) {
        paintOutline(g2,thickness);
        paintImage(g2);
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

    private void paintImage(Graphics2D g2) {
        if (imageBundle==null)
            return;     //no image to paint
        Image image = imageBundle.image;
        AffineTransform transform = new AffineTransform();
        g2.translate(x,y);
        //TODO - scale image size to area size, offset same as for ImagePage as a whole
        g2.drawImage(image,transform,null);
    }
}
