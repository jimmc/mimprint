/* FileInfo.java
 *
 * Jim McBeath, October 2005
 */

package net.jimmc.mimprint;

import net.jimmc.util.FileUtil;

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
    public static final String MIMPRINT_EXTENSION = "mpr";

    int index;          //the index of this entry within the containing list
    int dirCount;       //number of directories in the list
    int fileCount;      //number of files in the list
    int totalCount;     //total number of items in the list
    File dir;           //the directory containing the file
    String name;        //name of the file within the directory
    File thisFile;      //File object for (dir,name)
    int type;       //the type of this entry
        public static final int DIR = 1;
        public static final int IMAGE = 2;
        public static final int MIMPRINT = 3;        //our own file
    //The above data is initialized when the FileInfo is created

    //The following data is initialized by a call to loadInfo
    boolean infoLoaded; //true after loadInfo has been called
    String text;        //text for the image, from getFileText()
    String info;        //more info for the image, from getFileTextInfo()
    String html;        //html for the image, from getFileTextInfo()

    //The icon field is initialize by IconLoader
    ImageIcon icon;     //icon for the image, or generic icon for other file types

    public FileInfo() {}

    public FileInfo(int index, int dirCount, int fileCount,
                File dir, String name) {
        this.index = index;
        this.dirCount = dirCount;
        this.fileCount = fileCount;
        this.totalCount = dirCount + fileCount;
        this.dir = dir;
        this.name = name;

        thisFile = new File(dir,name);
        //If not a directory, assume it is an image file,
        //until we get around to implementing other stuff.
        if (thisFile.isDirectory()) {
            type = FileInfo.DIR;
        } else if (isOurFileName(name)) {
            type = FileInfo.MIMPRINT;       //our own file
        } else
            type = FileInfo.IMAGE;
    }

    public void loadInfo(boolean includeDirDates) {
        text = getFileText();
        html = getFileTextInfo(true,includeDirDates);
        info = getFileTextInfo(false,includeDirDates);
        infoLoaded = true;
    }

    /** Get the File object for this file. */
    public File getFile() {
        if (thisFile==null)
            thisFile = new File(dir,name);
        return thisFile;
    }

    /** True if this FileInfo represents a directory. */
    public boolean isDirectory() {
        return (type==DIR);
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
    protected String getFileTextInfo(boolean useHtml, boolean includeDirDates) {
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
        if (isDirectory()) {
            sb.append("; Folder "+thisIndex+" of "+dirCount);  //TBD i18n
        } else {
            int fileIndex = thisIndex - dirCount;
            sb.append("; File "+fileIndex+" of "+fileCount);  //TBD i18n
        }

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

        if (includeDirDates || type!=DIR) {
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
                    SimpleTimeZone stz = new SimpleTimeZone(zOff,tz.getID());
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
            } else {
                sb.append("; ");
                sb.append(dateStr);
            }
        }

        //Add file info text
        String fileText = text;
        if (fileText!=null) {
            if (fileText.endsWith("\n")) {
                fileText = fileText.substring(0,fileText.length()-1);
            }
            sb.append("\n");
            if (useHtml) {
                sb.append("<br>");
                fileText = fileText.replaceAll("\\n","<br>");
            }
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
        if (extension.equals(MIMPRINT_EXTENSION)) {
            return true;
        }
        return false;
    }

    /** True if the file name is for an image file that we recognize. */
    public static boolean isImageFileName(String name) {
        int dotPos = name.lastIndexOf('.');
        if (dotPos<0)
            return false;    //no extension
        String extension = name.substring(dotPos+1).toLowerCase();
        if (extension.equals("gif") || extension.equals("jpg") ||
                extension.equals("jpeg")) {
            return true;
        }
        return false;
    }

    public static int compareFileNames(String s1, String s2) {
        long n1 = getLongFromString(s1);
        long n2 = getLongFromString(s2);
        if (n1>n2)
            return 1;
        else if (n1<n2)
            return -1;
        else
            return s1.compareTo(s2);
    }

    /** Get an int from the string. */
    private static long getLongFromString(String s) {
        int len = s.length();
        int i;
        for (i=0; i<len; i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) && c!='0')
                break;    //found a digit
        }
        long n = 0;
        for ( ; i<len; i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c))
                break;
            long n0 = Character.digit(c,10);
            n *= 10;
            if (n0>=0)
                n += n0;
        }
        return n;
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
        html = getFileTextInfo(true,true);
        info = getFileTextInfo(false,true);
    }

    public static int countDirectories(File dir, String[] fileNames) {
        int dirCount = 0;
        for (int i=0; i<fileNames.length; i++) {
            File f = new File(dir,fileNames[i]);
            if (f.isDirectory())
                dirCount++;
        }
        return dirCount;
    }
}
