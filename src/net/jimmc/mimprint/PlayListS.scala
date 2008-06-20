/* PlayListS.scala
 *
 * Jim McBeath, May 23, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.BasicUi

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.util.Sorting

/** A playlist of images.  Immutable. */
class PlayListS(
        val ui:BasicUi,
        val baseDir:File,
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
        new PlayListS(ui,baseDir,newItems,comments)
    }

    //Create a new PlayListS containing the same items as ours except that
    //the item at the specified index has been replaced by the given item.
    def replaceItem(itemIndex:Int, item:PlayItem): PlayListS = {
        val newItems:Array[PlayItemS] = Array.make(items.length,null)
        Array.copy(items,0,newItems,0,items.length)
        newItems(itemIndex) = item.asInstanceOf[PlayItemS].usingBase(baseDir)
        new PlayListS(ui,baseDir,newItems,comments)
    }

    //Create a new PlayListS containing the same items as ours except that
    //the specified image is rotated.
    def rotateItem(itemIndex:Int, rot:Int):PlayList = {
        val newItems:Array[PlayItemS] = Array.make(items.length,null)
        Array.copy(items,0,newItems,0,items.length)
        newItems(itemIndex) = PlayItemS.rotate(items(itemIndex),rot)
        new PlayListS(ui,baseDir,newItems,comments)
    }

    //Ensure that we have at least the specified number of items in our list.
    //If so, return the current list; if not, create a new one of the
    //specified size and fill in all the new entries with empty PlayItems.
    def ensureSize(newSize:Int):PlayListS = {
        if (size >= newSize)
            return this         //already big enough
        val newItems:Array[PlayItemS] = Array.make(newSize,null)
        Array.copy(items,0,newItems,0,items.length)
        for (i <- items.length until newItems.length)
            newItems(i) = PlayItemS.emptyItem()
        new PlayListS(ui,baseDir,newItems,comments)
    }

    /** Return the number of items in the playlist. */
    def size() = if (items==null) 0 else items.length

    /** Count the number of non-empty items. */
    def countNonEmpty():Int =
        items.filter(item => item!=null && !item.isEmpty).length

    def getItem(n:Int) = items(n)

    def getFileNames():Array[String] = {
        val fileNames = new Array[String](size())
        if (size>0) {
            for (i <- 0 until items.length) {
                fileNames(i) = items(i).getFileName()
            }
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
    def apply(ui:BasicUi):PlayListS = {
        new PlayListS(ui,new File("."),new Array[PlayItemS](0),Nil)
    }

    /** Create a playlist from the given set of filenames. */
    def apply(ui:BasicUi,
            base:File, filenames:Array[String], start:Int, length:Int):
            PlayListS = {
        val items = new ArrayBuffer[PlayItemS]
        for (i <- 0 until length) {
            items += new PlayItemS(null,base,filenames(i+start),0)
        }
        new PlayListS(ui,base,items.toArray,Nil)
    }

    /** Load a playlist from a file. */
    def load(ui:BasicUi,
            filename:String):PlayList = load(ui,new File(filename))

    /** Load a playlist from a file. */
    def load(ui:BasicUi,f:File):PlayList = {
        val dir = f.getParentFile()
        if (f.isDirectory())
            loadDirectory(ui,f)
        else
            load(ui,new LineNumberReader(new FileReader(f)),dir)
    }

    //Given a directory, look for a file called "index.mpr" and load
    //that file; if not found, scan the directory for all files with
    //acceptable filename extensions, in alphabetical order.
    private def loadDirectory(ui:BasicUi,
            dir:File):PlayList = {
        val indexFileName = "index."+FileInfo.MIMPRINT_EXTENSION
        val indexFile = new File(dir,indexFileName)
        if (indexFile.exists)
            load(ui,indexFile)
        else {
            //No index file, scan the directory
            val fileNames:Array[String] = getPlayableFileNames(dir)
            apply(ui,dir,fileNames,0,fileNames.length)
        }
    }

    /** Load a playlist from a stream. */
    def load(ui:BasicUi,
            in:LineNumberReader, baseDir:File):PlayList = {
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
        new PlayListS(ui,baseDir,items.toArray,listComments)
    }

    //True if the line is a list comment line (for the whole file)
    private def isListComment(line:String) = line.trim.startsWith("##")

    //Return a list of all the filenames in a directory that
    //we want to include in our PlayList.
    private def getPlayableFileNames(dir:File):Array[String] = {
        val filter = new FilenameFilter() {
            override def accept(dir:File, name:String):Boolean =
                    FileInfo.isImageFileName(name) ||
                    FileInfo.isOurFileName(name)
        }
        val list = dir.list(filter);
        Sorting.stableSort(list,
            (s1:String,s2:String)=>FileInfo.compareFileNames(s1,s2)<0)
        list
    }
}
