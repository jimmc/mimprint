package net.jimmc.mimprint;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/** A playlist of images. */
public interface PlayList extends Cloneable {

    public void setBaseDir(File baseDir);

    public PlayList copy();

    public void addItem(PlayItem item);

    /** Return the number of items in the playlist. */
    public int size();

    /** Count the number of non-empty items. */
    public int countNonEmpty();

    public PlayItem getItem(int n);

    public String[] getFileNames();

    /** Save our playlist to a file. */
    public void save(String filename) throws IOException;

    public void save(File f) throws IOException;

    public void save(PrintWriter out, File baseDir) throws IOException;
}
