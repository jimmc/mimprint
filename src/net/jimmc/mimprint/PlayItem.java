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
public interface PlayItem extends Cloneable {
    public Object clone();

    /** Make a deep copy of this item. */
    public PlayItem copy();

    public void addTextLine(String line);

    public void printAll(PrintWriter out, File baseDir);

    /** The total number of lines of text we have,
     * including the text for the playable item. */
    public int lineCount();

    public void setBaseDir(File dir);

    public File getBaseDir();

    public boolean isOptionLine(String line);

    public void processOptionLine(String line);

    public boolean isImageInfoLine(String line);

    public void setImageInfoLine(String line);

    /** Get a line of text encoding all of the image info.
     * This is the inverse of setImageInfo. */
    public String getImageInfoLine();

    public void setFileName(String fileName);

    public String getFileName();

    /** True if we have no filename. */
    public boolean isEmpty();

    public void setRotFlag(int rot);

    public int getRotFlag();

    public String toString();
}
