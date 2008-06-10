package net.jimmc.mimprint;

import net.jimmc.util.ArrayUtil;
import net.jimmc.util.StringUtil;

import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;

/** A class representing one item from a PlayList.
 * In addition to keeping information about the playable item,
 * we also store the text, such as comments, that preceeds the item
 * in the playlist file.
 */
public interface PlayItem {
    public void printAll(PrintWriter out, File baseDir);

    public File getBaseDir();

    /** Get a line of text encoding all of the image info.
     * This is the inverse of setImageInfo. */
    public String getImageInfoLine();

    public String getFileName();

    /** True if we have no filename. */
    public boolean isEmpty();

    public int getRotFlag();

    public String toString();
}
