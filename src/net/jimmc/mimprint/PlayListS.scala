package net.jimmc.mimprint

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

/** A playlist of images.  Immutable. */
class PlayListS(
        private val baseDir:File,
        private val items:Array[PlayItemS],
        private val comments:List[String]
            //Header comments for the whole file
        ) extends PlayList {

    override def equals(that:Any):Boolean = {
        that match {
        case other:PlayListS =>
            if (baseDir==null || other.baseDir==null) {
		if (baseDir!=other.baseDir)
		    return false	//one was null but not the other
	    } else if (baseDir.getPath!=other.baseDir.getPath)
                return false
            return items==other.items
        case _ => false
        }
    }

    //Create a new PlayListS containing the same items as ours plus the new item
    def addItem(item:PlayItem):PlayList = {
        val newItems = items ++ Array(item.asInstanceOf[PlayItemS])
        new PlayListS(baseDir,newItems,comments)
    }

    //Create a new PlayListS containing the same items as ours except that
    //the specified image is rotated.
    def rotateItem(itemIndex:Int, rot:Int):PlayList = {
        val newItems:Array[PlayItemS] = Array.make(items.length,null)
        Array.copy(items,0,newItems,0,items.length)
        newItems(itemIndex) = PlayItemS.rotate(items(itemIndex),rot)
        new PlayListS(baseDir,newItems,comments)
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
    def apply():PlayListS = {
        new PlayListS(null,null,null)    //TODO
    }

    /** Create a playlist from the given set of filenames. */
    def apply(base:File, filenames:Array[String], start:Int, length:Int):
            PlayListS = {
        val items = new ArrayBuffer[PlayItemS]
        for (i <- 0 until length) {
            items += new PlayItemS(null,base,filenames(i+start),0)
        }
        new PlayListS(base,items.toArray,Nil)
    }

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
        val items = new ArrayBuffer[PlayItemS]
        var listComments:List[String] = Nil
        var lines = new ListBuffer[String]
        var line:String = in.readLine()
        //Read leading list-comment lines (starting with two # chars)
        while (line!=null && isListComment(line)) {
            lines += line
            line = in.readLine()
        }
        if (lines.length>0) {
            listComments = lines.toList
            lines = new ListBuffer[String]
        }
        var itemBaseDir = baseDir;
        while (line!=null) {
            lines += line
            if (PlayItemS.isFinalLine(line)) {
                val item = PlayItemS(lines.toList,itemBaseDir)
                items += item
                itemBaseDir = item.getBaseDir   //may not be what we passed in
                lines = new ListBuffer[String]
            }
            line = in.readLine()
        }
        //TODO - process trailling lines
        new PlayListS(baseDir,items.toArray,listComments)
    }

    //True if the line is a list comment line (for the whole file)
    private def isListComment(line:String) = line.trim.startsWith("##")
}
