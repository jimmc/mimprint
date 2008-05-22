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
public class PlayItemJ implements PlayItem,Cloneable {
    private ArrayList textLines = new ArrayList();
        //the text lines that preceded our fileName line
    private File baseDir;     //the base directory
    private String fileName;    //the image fileName
    private int rotFlag;        //0 for no rotation, 1 for +r ccw, -1 for -r cw
        //2 for +rr (180degree rotation)

    public PlayItemJ() {
    }

    public Object clone() {
        return copy();
    }

    /** Make a deep copy of this item. */
    public PlayItem copy() {
        PlayItemJ c = new PlayItemJ();
        c.textLines = (ArrayList)textLines.clone();
        c.fileName = fileName;
        c.rotFlag = rotFlag;
        return c;
    }

    //We are not trying to use these in a hash table, so we do
    //not provide a hashCode method.
    public boolean equals(Object that) {
        if (!(that instanceof PlayItemJ))
            return false;
        PlayItemJ other = (PlayItemJ)that;
        if (!StringUtil.equals(fileName,other.fileName))
            return false;
        if (rotFlag!=other.rotFlag)
            return false;
        if (!ArrayUtil.equals(textLines.toArray(),other.textLines.toArray()))
            return false;
        return true;
    }

    public void addTextLine(String line) {
        textLines.add(line);
    }

    public void printAll(PrintWriter out, File baseDir) {
out.println("#Our basedir="+this.baseDir+"; list baseDir="+baseDir);
        int n = textLines.size();
        for (int i=0; i<n; i++) {
            out.println(textLines.get(i));
        }
        if (fileName!=null)
            out.println(getImageInfoLine());
    }

    /** The total number of lines of text we have,
     * including the text for the playable item. */
    public int lineCount() {
        return textLines.size();
    }

    public void setBaseDir(File dir) {
        baseDir = dir;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public boolean isOptionLine(String line) {
        line = line.trim();
        if (line.length()==0)
            return false;       //empty line
        char firstChar = line.charAt(0);
        return firstChar=='-';  //option lines start with a dsah
    }

    public void processOptionLine(String line) {
        line = line.trim();
        String[] words = line.split(" ");
        if (words.length==0)
            return;             //nothing there to process
        if (words[0].startsWith("-base=")) {
            String base = words[0].substring("-base=".length()).trim();
            if (base.equals(""))
                throw new IllegalArgumentException("No value for -base option");
            setBaseDir(new File(base));
        }
        else
            throw new IllegalArgumentException("Unknown option line "+line);
    }

    public boolean isImageInfoLine(String line) {
        line = line.trim();
        if (line.length()==0)
            return false;       //empty line
        char firstChar = line.charAt(0);
        if (Character.isLetterOrDigit(firstChar))
            return true;        //first char alpha, assume a fileName
        if (firstChar=='.' || firstChar=='_')
            return true;        //assume leading . or ..
        return false;           //assume comment or something else
    }

    public void setImageInfoLine(String line) {
        line = line.trim();
        String[] parts = line.split(";");
        fileName = parts[0];
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
        else if (opt.equals("+rr"))
            rotFlag = 2;
        else throw new IllegalArgumentException("Unknown image file option '"+
                opt + "'");
    }

    /** Get a line of text encoding all of the image info.
     * This is the inverse of setImageInfo. */
    public String getImageInfoLine() {
        StringBuffer sb = new StringBuffer();
        sb.append(fileName);
        if (rotFlag!=0) {
            sb.append(";");
            String rStr = rotStrs[(rotFlag+1)%4];
            sb.append(rStr);
        }
        return sb.toString();
    }
    private final String[] rotStrs = { "-r", "", "+r", "+rr" };

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    /** True if we have no filename. */
    public boolean isEmpty() {
        return (fileName==null || fileName.equals(""));
    }

    public void setRotFlag(int rot) {
        this.rotFlag = (rot+1)%4 - 1;
    }

    public int getRotFlag() {
        return rotFlag;
    }

    public String toString() {
        return getImageInfoLine();      //TODO - add basedir?
    }
}
