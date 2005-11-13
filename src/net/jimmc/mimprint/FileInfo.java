/* FileInfo.java
 *
 * Jim McBeath, October 2005
 */

package jimmc.jiviewer;

import jimmc.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import javax.swing.ImageIcon;

/** A representation of an image file in a list.
 */
public class FileInfo {
    int index;          //the index of this entry within the containing list
    int totalCount;     //total number of items in the list
    File dir;           //the directory containing the file
    String name;        //name of the file within the directory
    File thisFile;      //File object for (dir,name)
    String text;        //text for the image, from getFileText()
    String info;        //more info for the image, from getFileTextInfo()
    String html;        //html for the image, from getFileTextInfo()
    ImageIcon icon;     //icon for the image, or generic icon for other file types
    int type;       //the type of this entry
        public static final int DIR = 1;
        public static final int IMAGE = 2;
        public static final int JIV = 3;        //our own file

    public FileInfo() {}

    public FileInfo(int index, int totalCount, File dir, String name) {
        this.index = index;
        this.totalCount = totalCount;
        this.dir = dir;
        this.name = name;
    }

    public void loadInfo() {
        thisFile = new File(dir,name);
        //If not a directory, assume it is an image file,
        //until we get around to implementing other stuff.
        if (thisFile.isDirectory()) {
            type = FileInfo.DIR;
        } else if (isOurFileName(name)) {
            type = FileInfo.JIV;       //our own file
        } else
            type = FileInfo.IMAGE;
        text = getFileText();
        html = getFileTextInfo(true);
        info = getFileTextInfo(false);
    }

    /** Get the File object for this file. */
    public File getFile() {
        if (thisFile==null)
            thisFile = new File(dir,name);
        return thisFile;
    }

    /** True if this FileInfo represents a directory. */
    public boolean isDirectory() {
        return getFile().isDirectory();
    }

    /** Get the text for the specified file. */
    private String getFileText() {
        String path = getPath();
        try {
            String textPath = getTextFileNameForImage(path);
            if (textPath==null)
                return null;
            File f = new File(textPath);
            String text = FileUtil.readFile(f);
            return text;
        } catch (FileNotFoundException ex) {
            return null;    //OK if the file is not there
        } catch (Exception ex) {
            System.out.println("Exception reading file "+path+": "+ex.getMessage());
            return null;	//on any error, ignore the file
        }
    }

    /** Get the text associated with a file.
     * @param useHtml True to format with for HTML, false for plain text.
     * @return The info about the image
     */
    protected String getFileTextInfo(boolean useHtml) {
        String path = getPath();
        if (path==null) {
            return null;	//no file, so no info
        }
        File f = new File(path);

        StringBuffer sb = new StringBuffer();

        //Start the with file name
        String fn = f.getName();
        if (fn.equals(".")) {
            try {
                fn = f.getCanonicalPath();
            } catch (IOException ex) {
                //ignore errors here, leave fn as it was
            }
        }
        else if (fn.equals(".."))
            fn = "Up to Parent";        //TODO i18n
        if (useHtml) {
            sb.append("<html>");
            sb.append("<b>");
            sb.append(fn);
            if (f.isDirectory())
                sb.append(File.separator);
            sb.append("</b>");
        } else {
            sb.append("File: ");        //TODO i18n
            sb.append(fn);
            if (f.isDirectory())
                sb.append(File.separator);
        }

        //Add (N of M)
        int thisIndex = index+1;
        sb.append("; "+thisIndex+" of "+totalCount);  //TBD i18n

        //Add file size
        long fileSize = f.length();
        String fileSizeStr;
        if (fileSize>1024*1024*10)	//>10M
            fileSizeStr = ""+(fileSize/(1024*1024))+"M";
        else if (fileSize>1024*10)	//>10K
            fileSizeStr = ""+(fileSize/1024)+"K";
        else
            fileSizeStr = ""+fileSize+"B";
        sb.append("; ");
        sb.append(fileSizeStr);

        //Add file modification date/time
        long modTimeMillis = f.lastModified();
        Date modDate = new Date(modTimeMillis);
        SimpleDateFormat dFmt =
                (SimpleDateFormat)DateFormat.getDateTimeInstance();
        String tzPath = getTimeZoneFileNameForImage(path);
        File tzFile = new File(tzPath);
        if (tzFile.exists()) {
            try {
                //What a hack... the SimpleDateFormat code doesn't
                //do the right time-zone calculations, it uses
                //TimeZone.getRawOffset, which just gets the first
                //offset in the timezone.  We need it to get the
                //offset for the specified time.
                TimeZone tz = new ZoneInfo(tzFile);
                int zOff = tz.getOffset(modTimeMillis);
                SimpleTimeZone stz =	
                        new SimpleTimeZone(zOff,tz.getID());
                dFmt.setTimeZone(stz);
                dFmt.applyPattern(dFmt.toPattern()+" zzzz");
            } catch (IOException ex) {
System.out.println("IOException reading ZoneInfo: "+ex.getMessage());
                //do nothing to change timezone or format
            }
        }
        String dateStr = dFmt.format(modDate);
        if (useHtml) {
            sb.append("<br><i>");
            sb.append(dateStr);
            sb.append("</i>");
            sb.append("<br>");
        } else {
            sb.append("; ");
            sb.append(dateStr);
        }

        //Add file info text
        String fileText = text;
        if (fileText!=null) {
            if (fileText.endsWith("\n")) {
                fileText = fileText.substring(0,fileText.length()-1);
            }
            sb.append("\n");
            if (useHtml)
                fileText = fileText.replaceAll("\\n","<br>");
            sb.append(fileText);
        }
        if (useHtml)
            sb.append("</html>");
        return sb.toString();
    }

    /** True if we recognize the file as being one of ours. */
    public static boolean isOurFileName(String name) {
        int dotPos = name.lastIndexOf('.');
        if (dotPos<0)
            return false;	//no extension
        String extension = name.substring(dotPos+1).toLowerCase();
        if (extension.equals("jiv")) {
            return true;
        }
        return false;
    }

    /** Get the name of the text file which contains the info
     * about the specified image file.
     * @param path The path to the image file.
     * @return The text file name, or null if we can't figure it out.
     */
    protected static String getTextFileNameForImage(String path) {
        File f = new File(path);
        if (f.isDirectory())
            return path+File.separator+"summary.txt";
        int dot = path.lastIndexOf('.');
        if (dot<0)
            return null;
        String textPath = path.substring(0,dot+1)+"txt";
        return textPath;
    }

    /** Get the name of the timezone file for the image.
     * @param path The path to the image file.
     * @return The timezone file name, or null if we can't figure it out.
     */
    protected String getTimeZoneFileNameForImage(String path) {
        int sl = path.lastIndexOf(File.separator);
        if (sl<0)
            path = "."+File.separator;
        else
            path = path.substring(0,sl+1);
        String tzPath = path+"TZ";
        return tzPath;
    }

    public String getPath() {
        return getFile().toString();
    }

    /** Set our text field, update the info and html fields. */
    public void setText(String text) {
        this.text = text;
        html = getFileTextInfo(true);
        info = getFileTextInfo(false);
    }
}
