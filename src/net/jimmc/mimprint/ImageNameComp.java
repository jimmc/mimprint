/* ImageNameComp.java
 *
 * Jim McBeath, September 19, 2001
 */

package net.jimmc.mimprint;

import java.io.File;
import java.util.Comparator;

/** Compare two image file names to see how they should be ordered.
 */
public class ImageNameComp implements Comparator {
    private File dir;    //the directory in which we are comparing

    public ImageNameComp(File dir) {
        this.dir = dir;
    }

    /** Compare the two objects, which must be FileInfo objects.
     * Directories come before files.
     * We skip over leading non-numeric chars until we come to digits,
     * then interpret those as an integer and compare those integers.
     * If they are the same, then we fall back on a simple string comapre.
     */
    public int compare(Object o1, Object o2) {
        if ((o1 instanceof FileInfo) && (o2 instanceof FileInfo)) {
            FileInfo f1 = (FileInfo)o1;
            FileInfo f2 = (FileInfo)o2;
            return compareFileInfos(f1, f2);
        }
        if ((o1 instanceof String) && (o2 instanceof String)) {
            String s1 = (String)o1;
            String s2 = (String)o2;
            return compareFiles(new File(dir,s1), new File(dir,s2));
        }
        return 0;    //TBD - throw exception
    }

    private int compareFileInfos(FileInfo f1, FileInfo f2) {
        if (f1.isDirectory() != f2.isDirectory())
            return (f1.isDirectory()?-1:1);
        String s1 = f1.name;
        String s2 = f2.name;
        return FileInfo.compareFileNames(s1,s2);
    }

    private int compareFiles(File f1, File f2) {
        if (f1.isDirectory()!=f2.isDirectory())
            return (f1.isDirectory()?-1:1);
        String s1 = f1.getName();
        String s2 = f2.getName();
        return FileInfo.compareFileNames(s1,s2);
    }

    /** True if the objects are equal. */
    public boolean equals(Object that) {
        return (compare(this,that)==0);
    }
}

/* end */
