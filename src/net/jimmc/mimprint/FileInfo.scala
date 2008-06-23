/* FileInfo.scala
 *
 * Jim McBeath, June 20, 2008
 * Converted from java version of October 2005
 */

package net.jimmc.mimprint

import net.jimmc.util.FileUtilS
import net.jimmc.util.ZoneInfo

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone
import javax.swing.ImageIcon

object FileInfo {
    val MIMPRINT_EXTENSION = "mpr"

    //type values
    val DIR = 1
    val IMAGE = 2
    val MIMPRINT = 3        //our own file

    /** True if we recognize the file as being one of ours. */
    def isOurFileName(name:String):Boolean = {
        val dotPos = name.lastIndexOf('.')
        if (dotPos<0)
            return false	//no extension
        val extension = name.substring(dotPos+1).toLowerCase()
        if (extension.equals(MIMPRINT_EXTENSION)) {
            return true
        }
        return false
    }

    /** True if the file name is for an image file that we recognize. */
    def isImageFileName(name:String):Boolean = {
        val dotPos = name.lastIndexOf('.')
        if (dotPos<0)
            return false    //no extension
        val extension = name.substring(dotPos+1).toLowerCase()
        if (extension.equals("gif") || extension.equals("jpg") ||
                extension.equals("jpeg")) {
            return true
        }
        return false
    }

    def compareFileNames(s1:String, s2:String):Int = {
        val n1 = getLongFromString(s1)
        val n2 = getLongFromString(s2)
        if (n1>n2)
            return 1
        else if (n1<n2)
            return -1
        else
            return s1.compareTo(s2)
    }

    /** Get an int from the string. */
    private def getLongFromString(s:String):Long = {
        val firstDigit = s.findIndexOf(Character.isDigit(_))
        if (firstDigit<0)
            return 0    //no digits in this string
        val sRem = s.substring(firstDigit)
        val firstNonDigit = sRem.findIndexOf(!Character.isDigit(_))
        val sDigits = if (firstNonDigit<0) sRem
                      else sRem.substring(0,firstNonDigit)
        java.lang.Long.parseLong(sDigits)
    }

    /** Get the name of the text file which contains the info
     * about the specified image file.
     * @param path The path to the image file.
     * @return The text file name, or null if we can't figure it out.
     */
    def getTextFileNameForImage(path:String):String = {
        val f = new File(path)
        if (f.isDirectory())
            return path+File.separator+"summary.txt"
        val dot = path.lastIndexOf('.')
        if (dot<0)
            return null
        val textPath = path.substring(0,dot+1)+"txt"
        textPath
    }

    def countDirectories(dir:File, fileNames:Array[String]):Int =
        fileNames.filter(new File(dir,_).isDirectory).length
}

/** A representation of an image file in a list.
 */
class FileInfo(
    index:Int,          //the index of this entry within the containing list
    dirCount:Int,       //number of directories in the list
    fileCount:Int,      //number of files in the list
    dir:File,           //the directory containing the file
    val name:String)        //name of the file with the directory
{
    import FileInfo._   //get all the stuff from our companion object

    val totalCount = dirCount + fileCount
    val thisFile = new File(dir,name)

    //If not a directory, assume it is an image file,
    //until we get around to implementing other stuff.
    private val fType =          //the type of this entry
        if (thisFile.isDirectory()) {
            FileInfo.DIR
        } else if (isOurFileName(name)) {
            FileInfo.MIMPRINT       //our own file
        } else
            FileInfo.IMAGE

    //The following data is initialized by a call to loadInfo
    var infoLoaded = false //true after loadInfo has been called
    var text:String = _   //text for the image, from getFileText()
    var info:String = _   //more info for the image, from getFileTextInfo()
    var html:String = _   //html for the image, from getFileTextInfo()
    //TODO - make all these vars read-only from outside the class

    //The icon field is initialize by IconLoader
    var icon:ImageIcon = _     //icon for the image,
            //or generic icon for other file types

    def loadInfo(includeDirDates:Boolean) {
        text = getFileText()
        html = getFileTextInfo(true,includeDirDates)
        info = getFileTextInfo(false,includeDirDates)
        infoLoaded = true
    }

    /** Get the File object for this file. */
    def getFile() = thisFile

    /** True if this FileInfo represents a directory. */
    def isDirectory() = (fType==DIR)

    /** Get the text for the specified file. */
    private def getFileText():String = {
        val path = getPath()
        try {
            val textPath = getTextFileNameForImage(path)
            if (textPath==null)
                return null
            val f = new File(textPath)
            val text = FileUtilS.readFile(f)
            text
        } catch {
            case ex:FileNotFoundException =>
                null    //OK if the file is not there
            case ex:Exception =>
                println("Exception reading file "+path+": "+ex.getMessage())
                null	//on any error, ignore the file
        }
    }

    /** Get the text associated with a file.
     * @param useHtml True to format with for HTML, false for plain text.
     * @return The info about the image
     */
    protected def getFileTextInfo(useHtml:Boolean,
            includeDirDates:Boolean):String = {
        val path = getPath()
        if (path==null) {
            return null 	//no file, so no info
        }
        val f = new File(path)

        val sb = new StringBuffer()

        //Start the with file name
        var fn = f.getName()
        if (fn.equals(".")) {
            try {
                fn = f.getCanonicalPath()
            } catch {
                case ex:IOException =>
                    //ignore errors here, leave fn as it was
            }
        }
        else if (fn=="..")
            fn = "Up to Parent"        //TODO i18n
        if (useHtml) {
            sb.append("<html>")
            sb.append("<b>")
            sb.append(fn)
            if (f.isDirectory())
                sb.append(File.separator)
            sb.append("</b>")
        } else {
            sb.append("File: ")        //TODO i18n
            sb.append(fn)
            if (f.isDirectory())
                sb.append(File.separator)
        }

        //Add (N of M)
        val thisIndex = index+1
        if (isDirectory()) {
            sb.append("; Folder "+thisIndex+" of "+dirCount)  //TBD i18n
        } else {
            val fileIndex = thisIndex - dirCount
            sb.append("; File "+fileIndex+" of "+fileCount)  //TBD i18n
        }

        //Add file size
        val fileSize:Long = f.length()
        var fileSizeStr = 
            if (fileSize>1024*1024*10)	//>10M
                ""+(fileSize/(1024*1024))+"M"
            else if (fileSize>1024*10)	//>10K
                ""+(fileSize/1024)+"K"
            else
                ""+fileSize+"B"
        sb.append("; ")
        sb.append(fileSizeStr)

        if (includeDirDates || fType!=DIR) {
            //Add file modification date/time
            val modTimeMillis:Long = f.lastModified()
            val modDate = new Date(modTimeMillis)
            val dFmt =
                    DateFormat.getDateTimeInstance().asInstanceOf[SimpleDateFormat]
            val tzPath = getTimeZoneFileNameForImage(path)
            val tzFile = new File(tzPath)
            if (tzFile.exists()) {
                try {
                    //What a hack... the SimpleDateFormat code doesn't
                    //do the right time-zone calculations, it uses
                    //TimeZone.getRawOffset, which just gets the first
                    //offset in the timezone.  We need it to get the
                    //offset for the specified time.
                    val tz:TimeZone = new ZoneInfo(tzFile)
                    val zOff:Int = tz.getOffset(modTimeMillis)
                    val stz = new SimpleTimeZone(zOff,tz.getID())
                    dFmt.setTimeZone(stz)
                    dFmt.applyPattern(dFmt.toPattern()+" zzzz")
                } catch {
                    case ex:IOException =>
println("IOException reading ZoneInfo: "+ex.getMessage())
                    //do nothing to change timezone or format
                }
            }
            val dateStr = dFmt.format(modDate)
            if (useHtml) {
                sb.append("<br><i>")
                sb.append(dateStr)
                sb.append("</i>")
            } else {
                sb.append("; ")
                sb.append(dateStr)
            }
        }

        //Add file info text
        var fileText = text
        if (fileText!=null) {
            if (fileText.endsWith("\n")) {
                fileText = fileText.substring(0,fileText.length()-1)
            }
            sb.append("\n")
            if (useHtml) {
                sb.append("<br>")
                fileText = fileText.replaceAll("\\n","<br>")
            }
            sb.append(fileText)
        }
        if (useHtml)
            sb.append("</html>")
        sb.toString()
    }

    /** Get the name of the timezone file for the image.
     * @param path The path to the image file.
     * @return The timezone file name, or null if we can't figure it out.
     */
    protected def getTimeZoneFileNameForImage(path:String):String = {
        val sl = path.lastIndexOf(File.separator)
        val path1 =
            if (sl<0)
                "."+File.separator
            else
                path.substring(0,sl+1)
        val tzPath = path1+"TZ"
        tzPath
    }

    def getPath() = getFile().toString()

    /** Set our text field, update the info and html fields. */
    def setText(text:String) {
        this.text = text
        html = getFileTextInfo(true,true)
        info = getFileTextInfo(false,true)
    }
}
