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
    String text;        //text for the image, from getFileText()
    String info;        //more info for the image, from getFileTextInfo()
    String html;        //html for the image, from getFileTextInfo()
    ImageIcon icon;     //icon for the image, or generic icon for other file types
    int type;       //the type of this entry
        public static final int DIR = 1;
        public static final int IMAGE = 2;
        public static final int JIV = 3;        //our own file

    public File getFile() {
        return new File(dir,name);
    }

    public String getPath() {
        return getFile().toString();
    }
}
