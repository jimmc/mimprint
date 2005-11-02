/* FileInfo.java
 *
 * Jim McBeath, October 2005
 */

package jimmc.jiviewer;

import java.io.File;
import javax.swing.ImageIcon;

/** A representation of an image file in a list.
 */
public class FileInfo {
    File dir;           //the directory containing the file
    String name;        //name of the file within the directory
    String text;        //text for the image, from getFileTextInfo()
    ImageIcon icon;     //icon for the image, or generic icon for other file types

    public String getPath() {
        return new File(dir,name).toString();
    }
}
