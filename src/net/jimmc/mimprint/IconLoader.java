/* IconLoader.java
 *
 * Jim McBeath, October 31, 2005
 */

package jimmc.jiviewer;

import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.ImageIcon;

/** A background thread to load image icons.
 */
public class IconLoader extends Thread {
    private ImageLister lister;

    public IconLoader(ImageLister lister) {
        this.lister = lister;
    }

    public void run() {
System.out.println("running iconLoader");
        while (true) {  //keep running unilt the app exits
            FileInfo[] fileInfos = getFileInfoList();
            int updatedCount = loadFileInfos(fileInfos);
            if (updatedCount==0)
                waitForMoreIcons();
        }
    }

    /** Call this from the lister to notify us that
     * we should go look for more images to load.
     */
    public synchronized void moreIcons() {
System.out.println("moreIcons");
        this.notify();
    }

    //Wait for the lister to tell us we have more work.
    private synchronized void waitForMoreIcons() {
        //Before we wait, run throught the list and make sure there
        //are not any new icons that snuck in there after our last
        //pass through the list.
        FileInfo[] fileInfos = getFileInfoList();
        for (int i=0; fileInfos!=null && i<fileInfos.length; i++) {
            FileInfo fi = fileInfos[i];
            if (fi==null)
                continue;       //that list item not yet visible
            if (fi.icon!=null)
                continue;       //icon already loaded
            return;             //found one that needs loading
        }
System.out.println("waitForMoreIcons");
        try {
            this.wait();
        } catch (InterruptedException ex) {
            System.out.println("Wait interrupted");
        }
System.out.println("waitForMoreIcons done");
    }

    //Notify the lister that it should update the display on
    //an item in its list.
    private boolean notifyLister(FileInfo[] fileInfos, int n) {
System.out.println("notifyLister");
        return lister.iconLoaded(fileInfos,n);
    }

    private FileInfo[] getFileInfoList() {
        return lister.getFileInfoList();
    }

    //Load all icons that are ready to be loaded
    private int loadFileInfos(FileInfo[] fileInfos) {
System.out.println("loadFileInfos");
        if (fileInfos==null)
            return 0;
        int updatedCount = 0;
        for (int i=0; i<fileInfos.length; i++) {
            FileInfo fi = fileInfos[i];
            if (fi==null)
                continue;       //that list item not yet visible
            if (fi.icon!=null)
                continue;       //icon already loaded
            fi.icon = getFileIcon(fi.getPath());
            if (!notifyLister(fileInfos,i)) {
                //The list in lister changed, don't
                //bother loading the rest of these icons
                return updatedCount;
            }
            updatedCount++;
        }
        return updatedCount;
    }

    //Load the icon for the specified file.
    private ImageIcon getFileIcon(String filename) {
        Toolkit toolkit = lister.getToolkit();
        Image fullImage = toolkit.createImage(filename);
        Image scaledImage = ImageBundle.createScaledImage(fullImage,
                0,ImageLister.ICON_SIZE,ImageLister.ICON_SIZE);
        return new ImageIcon(scaledImage);
    }
}
