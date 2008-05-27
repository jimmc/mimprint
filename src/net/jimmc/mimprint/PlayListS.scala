package net.jimmc.mimprint

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

/** A playlist of images. */
class PlayListS() extends PlayList {
    private var baseDir:File = _             //current base directory
    private var items:ArrayBuffer[PlayItemS] = _
    private var comments:List[String] = Nil
        //Header comments for the whole file

    /** Create an empty playlist. */
    items = new ArrayBuffer[PlayItemS]        //no items

    /** Create a playlist from the given set of filenames. */
    def this(base:File, filenames:Array[String], start:Int, length:Int) {
        this()
        for (i <- 0 until length) {
            addItem(new PlayItemS(null,base,filenames(i+start),0))
        }
    }

    def setListComments(commentLines:List[String]) = comments = commentLines

    def setBaseDir(baseDir:File) = this.baseDir = baseDir

    override def clone():Object = copy

    def copy():PlayList = {
        val c = new PlayListS()
        c.baseDir = baseDir
        c.items ++= items
        c
    }

    override def equals(that:Any):Boolean = {
        that match {
        case other:PlayListS =>
            if ((baseDir==null || other.baseDir==null) && baseDir!=other.baseDir)
                return false
            if (baseDir.getPath!=other.baseDir.getPath)
                return false
            return items==other.items
        case x => false
        }
    }

    def addItem(item:PlayItem) = items += item.asInstanceOf[PlayItemS]

    def addEmptyItem() = addItem(new PlayItemS(null,null,null,0))

    def rotateItem(itemIndex:Int, rot:Int) {
        val item = items(itemIndex)
        val newItem = PlayItemS.rotate(item,rot)
        items(itemIndex) = newItem
    }

    /** Return the number of items in the playlist. */
    def size() = items.length

    /** Count the number of non-empty items. */
    def countNonEmpty():Int =
        items.filter(item => item!=null && !item.isEmpty).length

    def getItem(n:Int) = items(n)

    def getFileNames():Array[String] = {
        val fileNames = new Array[String](size())
        for (i <- 0 until items.length) {
            fileNames(i) = items(i).getFileName()
        }
        fileNames
    }

    /** Save our playlist to a file. */
    def save(filename:String):Unit = save(new File(filename))

    def save(f:File) {
        val dir = f.getParentFile()
        val pw = new PrintWriter(f)
        save(pw,dir)
        pw.flush()
        pw.close()
    }

    def save(out:PrintWriter, baseDir:File) {
        comments.foreach(out.println(_))        //write out the header comments
        var itemBaseDir = baseDir
        items.foreach { itemOldBase =>
            val item = itemOldBase.usingBase(itemBaseDir)
            item.printAll(out,itemBaseDir)  //write each PlayItem
            itemBaseDir = item.getBaseDir
        }
    }
}

object PlayListS {
    /** Load a playlist from a file. */
    def load(filename:String):PlayList = {
        return load(new File(filename))
    }

    /** Load a playlist from a file. */
    def load(f:File):PlayList = {
        val dir = f.getParentFile()
        return load(new LineNumberReader(new FileReader(f)),dir)
    }

    /** Load a playlist from a stream. */
    def load(in:LineNumberReader, baseDir:File):PlayList = {
        val p = new PlayListS()
        p.setBaseDir(baseDir)
        var lines = new ListBuffer[String]
        var line:String = in.readLine()
        //Read leading list-comment lines (starting with two # chars)
        while (line!=null && isListComment(line)) {
            lines += line
            line = in.readLine()
        }
        if (lines.length>0) {
            p.setListComments(lines.toList)
            lines = new ListBuffer[String]
        }
        var itemBaseDir = baseDir;
        while (line!=null) {
            lines += line
            if (PlayItemS.isFinalLine(line)) {
                val item = PlayItemS(lines.toList,itemBaseDir)
                p.addItem(item)
                itemBaseDir = item.getBaseDir   //may not be what we passed in
                lines = new ListBuffer[String]
            }
            line = in.readLine()
        }
        //TODO - process trailling lines
        p
    }

    //True if the line is a list comment line (for the whole file)
    private def isListComment(line:String) = line.trim.startsWith("##")
}
