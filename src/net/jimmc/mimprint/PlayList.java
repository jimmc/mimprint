package net.jimmc.mimprint;

import net.jimmc.util.ArrayUtil;
import net.jimmc.util.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/** A playlist of images. */
public class PlayList implements Cloneable {
    private File baseDir;             //current base directory
    private PlayItem inputItem;         //item being built when loading
    private ArrayList items;

    /** Create an empty playlist. */
    public PlayList(){
        items = new ArrayList();        //no items
    }

    /** Create a playlist from the given set of filenames. */
    public PlayList(String[] filenames, int start, int length) {
        this();
        for (int i=0; i<length; i++) {
            processLine(filenames[i+start],i+1);
        }
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public Object clone() {
        return copy();
    }

    public PlayList copy() {
        PlayList c = new PlayList();
        for (int i=0; i<items.size(); i++) {
            c.addItem(((PlayItem)items.get(i)).copy());
        }
        c.baseDir = baseDir;
        return c;
    }

    public boolean equals(Object that) {
        if (!(that instanceof PlayList))
            return false;
        PlayList other = (PlayList)that;
        if ((baseDir==null || other.baseDir==null) && baseDir!=other.baseDir)
            return false;
        if (!StringUtil.equals(baseDir.getPath(),other.baseDir.getPath()))
            return false;
        return ArrayUtil.equals(items.toArray(),other.items.toArray());
    }

    /** Load a playlist from a file. */
    public static PlayList load(String filename) throws IOException {
        return load(new File(filename));
    }

    /** Load a playlist from a file. */
    public static PlayList load(File f) throws IOException {
        File dir = f.getParentFile();
        return load(new LineNumberReader(new FileReader(f)),dir);
    }

    /** Load a playlist from a stream. */
    public static PlayList load(LineNumberReader in, File baseDir)
            throws IOException {
        PlayList p = new PlayList();
        p.setBaseDir(baseDir);
        String line;
        while ((line=in.readLine())!=null) {
            p.processLine(line,in.getLineNumber());
        }
        return p;
    }

    private void processLine(String line, int lineNumber) {
        if (inputItem==null) {
            inputItem = new PlayItem();
            inputItem.setBaseDir(baseDir);
        }
        if (inputItem.isOptionLine(line)) {
            inputItem.processOptionLine(line);
        } else if (inputItem.isImageInfoLine(line)) {
            inputItem.setImageInfoLine(line);
            addItem(inputItem);
            inputItem = null;
        } else {
            inputItem.addTextLine(line);        //maintain all comments etc.
        }
    }

    protected void addItem(PlayItem item) {
        items.add(item);
    }

    /** Return the number of items in the playlist. */
    public int size() {
        return items.size();
    }

    public PlayItem getItem(int n) {
        return (PlayItem)items.get(n);
    }

    public String[] getFileNames() {
        String[] fileNames = new String[size()];
        for (int i=0; i<items.size(); i++) {
            fileNames[i] = ((PlayItem)items.get(i)).getFileName();
        }
        return fileNames;
    }

    /** Save our playlist to a file. */
    public void save(String filename) throws IOException {
        save(new File(filename));
    }

    public void save(File f) throws IOException {
        File dir = f.getParentFile();
        PrintWriter pw = new PrintWriter(f);
        save(pw,dir);
        pw.flush();
        pw.close();
    }

    public void save(PrintWriter out, File baseDir) throws IOException {
        int n = size();
        for (int i=0; i<n; i++) {
            PlayItem item = (PlayItem)items.get(i);
            item.printAll(out,baseDir);
        }
    }
}
