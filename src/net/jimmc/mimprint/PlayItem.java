package net.jimmc.mimprint;

import java.util.ArrayList;
import java.io.PrintWriter;

/** A class representing one item from a PlayList.
 * In addition to keeping information about the playable item,
 * we also store the text, such as comments, that preceeds the item
 * in the playlist file.
 */
public class PlayItem {
    private ArrayList textLines = new ArrayList();
        //the text lines that preceded our filename line
    private String filename;    //the image filename
    private int rotFlag;        //0 for no rotation, 1 for +r ccw, -1 for -r cw

    public PlayItem() {
    }

    public void addTextLine(String line) {
        textLines.add(line);
    }

    public void printAll(PrintWriter out) {
        int n = textLines.size();
        for (int i=0; i<n; i++) {
            out.println(textLines.get(i));
        }
        if (filename!=null)
            out.println(getImageInfoLine());
    }

    /** The total number of lines of text we have,
     * including the text for the playable item. */
    public int lineCount() {
        return textLines.size();
    }

    public boolean isImageInfoLine(String line) {
        line = line.trim();
        if (line.length()==0)
            return false;       //empty line
        char firstChar = line.charAt(0);
        if (Character.isLetterOrDigit(firstChar))
            return true;        //first char alpha, assume a filename
        if (firstChar=='.' || firstChar=='_')
            return true;        //assume leading . or ..
        return false;           //assume comment or something else
    }

    public void setImageInfoLine(String line) {
        line = line.trim();
        String[] parts = line.split(";");
        filename = parts[0];
        for (int i=1; i<parts.length; i++) {
            setImageOption(parts[i]);
        }
    }

    private void setImageOption(String opt) {
        opt = opt.trim();
        if (opt.equals(""))
            return;             //ignore empty options
        if (opt.equals("+r"))
            rotFlag = 1;
        else if (opt.equals("-r"))
            rotFlag = -1;
        else throw new IllegalArgumentException("Unknown image file option '"+
                opt + "'");
    }

    /** Get a line of text encoding all of the image info.
     * This is the inverse of setImageInfo. */
    public String getImageInfoLine() {
        StringBuffer sb = new StringBuffer();
        sb.append(filename);
        if (rotFlag!=0) {
            sb.append(";");
            sb.append(rotFlag>0?"+r":"-r");
        }
        return sb.toString();
    }

    public String getFileName() {
        return filename;
    }

    public int getRotFlag() {
        return rotFlag;
    }
}
