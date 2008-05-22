package net.jimmc.mimprint

import java.io.File
import java.io.PrintWriter

/** A class representing one item from a PlayList.
 * In addition to keeping information about the playable item,
 * we also store the text, such as comments, that preceeds the item
 * in the playlist file.
 */
class PlayItemS extends PlayItem with Cloneable {

    var textLines : List[String] = Nil
        //the text lines that preceded our fileName line
    var  baseDir : File = _     //the base directory
    var fileName : String = ""    //the image fileName
    var rotFlag : Int = 0        //0 for no rotation, 1 for +r ccw, -1 for -r cw
        //2 for +rr (180degree rotation)

    override def clone : Object = copy

    /** Make a deep copy of this item. */
    def copy : PlayItem = {
        var c = new PlayItemS()
        c.textLines = textLines;
        c.fileName = fileName
        c.rotFlag = rotFlag
        c
    }

    //We are not trying to use these in a hash table, so we do
    //not provide a hashCode method.
    override def equals(that:Any) = {
        that match {
            case other : PlayItemS =>
                (fileName==other.fileName) &&
                (rotFlag==other.rotFlag) &&
                (textLines==other.textLines)
            case x => false
        }
    }

    //TODO - this is n^2 behavior, we should not be appending onto a List
    def addTextLine(line:String) : Unit = textLines = textLines ::: List(line)

    def printAll(out:PrintWriter, baseDir:File) : Unit = {
out.println("#Our basedir="+this.baseDir+"; list baseDir="+baseDir);
        textLines.foreach(out.println(_))
        if (fileName!=null)
            out.println(getImageInfoLine())
    }

    /** The total number of lines of text we have,
     * including the text for the playable item. */
    def lineCount() = textLines.length

    def setBaseDir(dir:File) = { baseDir = dir }

    def getBaseDir() = baseDir

    def isOptionLine(line:String) : Boolean = {
        val lineTrimmed = line.trim()
        if (lineTrimmed.length()==0)
            return false       //empty line
        val firstChar = lineTrimmed.charAt(0)
        return firstChar=='-'  //option lines start with a dsah
    }

    def processOptionLine(line:String) {
        val lineTrimmed = line.trim()
        val words = lineTrimmed.split(" ")
        if (words.length==0)
            return             //nothing there to process
        if (words(0).startsWith("-base=")) {
            val base = words(0).substring("-base=".length()).trim()
            if (base.equals(""))
                throw new IllegalArgumentException("No value for -base option")
            setBaseDir(new File(base))
        }
        else
            throw new IllegalArgumentException("Unknown option line "+line)
    }

    def isImageInfoLine(line:String) : Boolean = {
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

    def setImageInfoLine(line:String) {
        val lineTrimmed = line.trim()
        val parts = lineTrimmed.split(";");
        fileName = parts(0)
        for (i <- 1 until parts.length) {
            setImageOption(parts(i))
        }
    }

    private def setImageOption(opt:String) {
        val optTrimmed = opt.trim()
        if (optTrimmed=="")
            return             //ignore empty options
        if (optTrimmed=="+r")
            rotFlag = 1;
        else if (optTrimmed=="-r")
            rotFlag = -1;
        else if (optTrimmed=="+rr")
            rotFlag = 2;
        else throw new IllegalArgumentException("Unknown image file option '"+
                opt + "'");
    }

    /** Get a line of text encoding all of the image info.
     * This is the inverse of setImageInfo. */
    def getImageInfoLine() = {
        val sb = new StringBuffer()
        sb.append(fileName)
        if (rotFlag!=0) {
            sb.append(";")
            val rStr = rotStrs((rotFlag+1)%4)
            sb.append(rStr)
        }
        sb.toString()
    }
    private val rotStrs = Array( "-r", "", "+r", "+rr" )

    def setFileName(fileName:String) = this.fileName = fileName

    def getFileName() = fileName

    /** True if we have no filename. */
    def isEmpty() = fileName==null || fileName==""

    def setRotFlag(rot:int) {
        this.rotFlag = (rot+1)%4 - 1
    }

    def getRotFlag() = rotFlag

    override def toString() = getImageInfoLine()      //TODO - add basedir?
}
