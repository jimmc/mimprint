package net.jimmc.mimprint;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/** A playlist of images. */
public class PlayList {
    private PlayItem inputItem;         //item being built when loading
    private ArrayList items;

    /** Create an empty playlist. */
    public PlayList(){
        items = new ArrayList();        //no items
    }

    /** Load a playlist from a file. */
    public static PlayList load(String filename) throws IOException {
        return load(new File(filename));
    }

    /** Load a playlist from a file. */
    public static PlayList load(File f) throws IOException {
        return load(new LineNumberReader(new FileReader(f)));
    }

    /** Load a playlist from a stream. */
    public static PlayList load(LineNumberReader in) throws IOException {
        PlayList p = new PlayList();
        String line;
        while ((line=in.readLine())!=null) {
            p.processLine(line,in.getLineNumber());
        }
        return p;
    }

    private void processLine(String line, int lineNumber) {
        if (inputItem==null)
            inputItem = new PlayItem();
        //TODO - look for global commands such as basedir
        if (inputItem.isImageInfoLine(line)) {
            inputItem.setImageInfoLine(line);
            addItem(inputItem);
            inputItem = null;
        } else {
            inputItem.addTextLine(line);        //maintain all comments etc.
        }
    }

    private void addItem(PlayItem item) {
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
        save(new PrintWriter(f));
    }

    public void save(PrintWriter out) throws IOException {
        int n = size();
        for (int i=0; i<n; i++) {
            PlayItem item = (PlayItem)items.get(i);
            item.printAll(out);
        }
    }
}
