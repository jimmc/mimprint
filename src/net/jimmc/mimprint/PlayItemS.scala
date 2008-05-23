package net.jimmc.mimprint

import java.io.File
import java.io.PrintWriter

import scala.collection.mutable.ListBuffer

/** A class representing one item from a PlayList.
 * In addition to keeping information about the playable item,
 * we also store the text, such as comments, that preceeds the item
 * in the playlist file.
 */
class PlayItemS(
        val comments:List[String],  //comment lines preceding our data
        val baseDir:File,       //the base directory for this entry
        val fileName:String,    //file name relative to the base directory
        val rotFlag:Int         //0=no rotation, 1 for +r ccw, 01 for -w cw,
                                //  2 for +rr 180 degrees
        ) extends PlayItem with Cloneable {

    //We are not trying to use these in a hash table, so we do
    //not provide a hashCode method.
    override def equals(that:Any) = {
        that match {
            case other : PlayItemS =>
                (comments==other.comments) &&
                (baseDir==other.baseDir) &&
                (fileName==other.fileName) &&
                (rotFlag==other.rotFlag)
            case x => false
        }
    }

    def printAll(out:PrintWriter, baseDir:File) : Unit = {
//TODO - need to print our basedir if it is different from the previous item
out.println("#Our basedir="+this.baseDir+"; list baseDir="+baseDir);
        comments.foreach(out.println(_))
        out.println(getImageInfoLine())
    }

    /** Get a line of text encoding all of the image info.
     */
    def getImageInfoLine() = {
        if (fileName==null)
            "-empty"
        else {
            val sb = new StringBuffer()
            sb.append(fileName)
            if (rotFlag!=0) {
                sb.append(";")
                val rStr = rotStrs((rotFlag+1)%4)
                sb.append(rStr)
            }
            sb.toString()
        }
    }
    private val rotStrs = Array( "-r", "", "+r", "+rr" )

    def getRotFlag = rotFlag
    def getFileName = fileName
    def getBaseDir = baseDir

    /** True if we have no filename. */
    def isEmpty() = fileName==null || fileName==""

    override def toString() = getImageInfoLine()      //TODO - add basedir?
}

object PlayItemS {
    /** Create an item from a list of strings.
     * The last string must be a final line as determined by isFinalLine,
     * and none of the preceding lines may be a final line.
     */
    def apply(lines:List[String], listBaseDir:File) : PlayItemS = {
        val comments = new ListBuffer[String]
        var baseDir:File = listBaseDir
        var fileName:String = null
        var rotFlag:Int = 0

        //Throw an exception if the text lines are not valid
        def validateFinalLines(lines:List[String]) = {
            if (lines.length==0)
                throw new IllegalArgumentException(
                    "No item text lines given for item")
            if (lines.length>0 && !isFinalLine(lines.last))
                throw new IllegalArgumentException(
                    "Last line in item text is not a valid Final line");
            if (lines.init.exists(isFinalLine(_)))
                throw new IllegalArgumentException(
                    "A non-last line is a Final line")
        }

        //Parse the filename line
        def setImageInfoLine(line:String) {
            val lineTrimmed = line.trim()
            val parts = lineTrimmed.split(";");
            fileName = parts(0)
            for (i <- 1 until parts.length) {
                setImageOption(parts(i))
            }
        }

        //process one image option
        def setImageOption(opt:String) {
            val optTrimmed = opt.trim()
            if (optTrimmed=="")
                return             //ignore empty options
            if (optTrimmed=="+r")
                rotFlag = 1;
            else if (optTrimmed=="-r")
                rotFlag = -1;
            else if (optTrimmed=="+rr")
                rotFlag = 2;
            else throw new IllegalArgumentException(
                    "Unknown image file option '"+ opt + "'");
        }

        //True if the line is an option line
        def isOptionLine(line:String) : Boolean = {
            val lineTrimmed = line.trim()
            if (lineTrimmed.length()==0)
                return false       //empty line
            val firstChar = lineTrimmed.charAt(0)
            return firstChar=='-'  //option lines start with a dsah
        }

        //Process one option line
        def processOptionLine(line:String) {
            val lineTrimmed = line.trim()
            val words = lineTrimmed.split(" ")
            if (words.length==0)
                return             //nothing there to process
            if (words(0).startsWith("-base=")) {
                val base = words(0).substring("-base=".length()).trim()
                if (base.equals(""))
                    throw new IllegalArgumentException("No value for -base option")
                baseDir = new File(base)
            }
            else
                throw new IllegalArgumentException("Unknown option line "+line)
        }

        //Process one line of text
        def processLine(line:String) {
            if (isCommentLine(line))
                comments += line
            else if (isImageInfoLine(line))
                setImageInfoLine(line)
            else if (isEmptyLine(line))
                fileName = null         //leave fileName null to signify empty
            else if (isOptionLine(line))
                processOptionLine(line)
            else
                throw new IllegalArgumentException(
                    "Bad item line '"+line+"'")
        }

        validateFinalLines(lines)
        lines.foreach(processLine(_))
        new PlayItemS(comments.toList,baseDir,fileName,rotFlag)
    }

    /** True if this line is a comment line.
     * Comment lines start with a pound sign (octothorp, #).
     */
    private[mimprint] def isCommentLine(line:String) : Boolean =
            line.trim.startsWith("#")
     
    /** True if this line of text is the final line of an item,
     * in other words it is either a filename line or a -empty line.
     */
    def isFinalLine(line:String) : Boolean = {
        isImageInfoLine(line) || isEmptyLine(line)
    }

    private[mimprint] def isImageInfoLine(line:String) : Boolean = {
        val lineTrimmed = line.trim()
        if (lineTrimmed.length()==0)
            return false       //empty line
        val firstChar = lineTrimmed.charAt(0);
        if (Character.isLetterOrDigit(firstChar))
            return true        //first char alpha, assume a fileName
        if (firstChar=='.' || firstChar=='_')
            return true        //assume leading . or ..
        return false           //assume comment or something else
    }

    private[mimprint] def isEmptyLine(line:String) : Boolean =
            line.trim=="-empty"

    /** Create a new item identical to the given one but rotated by
     * the specified amount.
     */
    def rotate(item:PlayItemS, inc:Int) : PlayItemS = {
        val rot = (item.getRotFlag()+inc+1)%4 - 1
        new PlayItemS(item.comments,item.baseDir,item.fileName,rot)
    }
}
