/* PlayList.scala
 *
 * Jim McBeath, May 23, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.StandardUi

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
class PlayList(
        val ui:StandardUi,
        val baseDir:File,
        private val items:Array[PlayItem],
        private val comments:List[String]
            //Header comments for the whole file
        ) {

    if (items==null)
        throw new NullPointerException("PlayList items must not be null")

    override def equals(that:Any):Boolean = {
        that match {
        case other:PlayList =>
            if (baseDir==null || other.baseDir==null) {
		if (baseDir!=other.baseDir)
		    return false	//one was null but not the other
	    } else if (baseDir.getPath!=other.baseDir.getPath)
                return false
            return items==other.items
        case _ => false
        }
    }

    //Create a new PlayList containing the same items as ours plus the new item
    def addItem(item:PlayItem):PlayList = {
        val newItems = items ++ Array(item.usingSelfBase())
        new PlayList(ui,baseDir,newItems,comments)
    }

    //Create a new PlayList where we have added the specified item at
    //the specified index.  All items which previous had the same or
    //higher index are moved up one.
    def insertItem(itemIndex:Int, item:PlayItem): PlayList = {
        val newItems:Array[PlayItem] = Array.make(items.length+1,null)
        if (itemIndex>0)
            Array.copy(items,0,newItems,0,itemIndex)
        val n = items.length - itemIndex    //number of items after it to move
        if (n>0)
            Array.copy(items,itemIndex,newItems,itemIndex+1,n)
        newItems(itemIndex) = item.usingSelfBase()
        new PlayList(ui,baseDir,newItems,comments)
    }

    //Create a new PlayList where we have removed the item at
    //the specified index.  All items which previously had a
    //higher index are moved down one.
    def removeItem(itemIndex:Int): PlayList = {
        val newItems:Array[PlayItem] = Array.make(items.length-1,null)
        if (itemIndex>0)
            Array.copy(items,0,newItems,0,itemIndex)
        val n = items.length - itemIndex - 1  //number of items after it to move
        if (n>0)
            Array.copy(items,itemIndex+1,newItems,itemIndex,n)
        new PlayList(ui,baseDir,newItems,comments)
    }

    //Create a new PlayList containing the same items as ours except that
    //the item at the specified index has been replaced by the given item.
    def replaceItem(itemIndex:Int, item:PlayItem): PlayList = {
        val newItems:Array[PlayItem] = Array.make(items.length,null)
        Array.copy(items,0,newItems,0,items.length)
        newItems(itemIndex) = item.usingSelfBase()
        new PlayList(ui,baseDir,newItems,comments)
    }

    //Create a new PlayList containing the same items as ours except that
    //the specified image is rotated.
    def rotateItem(itemIndex:Int, rot:Int):PlayList = {
        val newItems:Array[PlayItem] = Array.make(items.length,null)
        Array.copy(items,0,newItems,0,items.length)
        newItems(itemIndex) = PlayItem.rotate(items(itemIndex),rot)
        new PlayList(ui,baseDir,newItems,comments)
    }

    //Ensure that we have at least the specified number of items in our list.
    //If so, return the current list; if not, create a new one of the
    //specified size and fill in all the new entries with empty PlayItems.
    def ensureSize(newSize:Int):PlayList = {
        if (size >= newSize)
            return this         //already big enough
        val newItems:Array[PlayItem] = Array.make(newSize,null)
        Array.copy(items,0,newItems,0,items.length)
        for (i <- items.length until newItems.length)
            newItems(i) = PlayItem.emptyItem()
        new PlayList(ui,baseDir,newItems,comments)
    }

    /** Return the number of items in the playlist. */
    def size() = items.length

    /** Count the number of non-empty items. */
    def countNonEmpty():Int =
        items.filter(item => item!=null && !item.isEmpty).length

    def getItem(n:Int) = items(n)

    def getBaseDirs():Array[File] = items.map(_.getBaseDir())

    def getFileNames():Array[String] = items.map(_.getFileName())

    /** Save our playlist to a file. */
    def save(filename:String):Boolean = save(new File(filename),false)
    def save(filename:String,absolute:Boolean):Boolean =
        save(new File(filename), absolute)

    //True if we wrote out the file, false if cancelled
    def save(f:File, absolute:Boolean):Boolean = {
        val dir =
            if (absolute) new File(File.separator)
            else f.getParentFile()
        val pwOpt = ui.getPrintWriterFor(f)
        pwOpt.foreach { pw =>
            save(pw,dir)
            pw.flush()
            pw.close()
        }
        pwOpt.isDefined         //false if user cancelled due to overwrite issue
    }

    //For consistency with the other save methods, our return type is Boolean.
    //We always return true because there is no option for the user to cancel
    //when this method is called.
    def save(out:PrintWriter, baseDir:File):Boolean = {
        comments.foreach(out.println(_))        //write out the header comments
        var itemBaseDir = baseDir
        items.foreach { itemOldBase =>
            val item = itemOldBase.usingBase(itemBaseDir)
            item.printAll(out,itemBaseDir)  //write each PlayItem
            itemBaseDir = item.getBaseDir
        }
        true
    }
}

object PlayList {
    def apply(ui:StandardUi):PlayList = {
        new PlayList(ui,new File("."),new Array[PlayItem](0),Nil)
    }

    /** Create a playlist from the given set of filenames. */
    def apply(ui:StandardUi,
            base:File, filenames:Array[String], start:Int, length:Int):
            PlayList = {
        val items = filenames.slice(start,start+length).map(
                new PlayItem(null,base,_,0)).toArray
        new PlayList(ui,base,items,Nil)
    }

    /** Load a playlist from a file. */
    def load(ui:StandardUi,
            filename:String):PlayList = load(ui,new File(filename))

    /** Load a playlist from a file. */
    def load(ui:StandardUi,f:File):PlayList = {
        val dir = f.getParentFile()
        if (f.isDirectory())
            loadDirectory(ui,f)
        else
            load(ui,new LineNumberReader(new FileReader(f)),dir)
    }

    //Given a directory, look for a file called "index.mpr" and load
    //that file; if not found, scan the directory for all files with
    //acceptable filename extensions, in alphabetical order.
    private def loadDirectory(ui:StandardUi,
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
    def load(ui:StandardUi,
            in:LineNumberReader, baseDir:File):PlayList = {
        val items = new ArrayBuffer[PlayItem]
        var listComments:List[String] = Nil
        var lines = new ListBuffer[String]
        var line:String = in.readLine()
        if (line!=null && line.startsWith("<?xml")) {
            //Make sure we don't load a template file as a playlist
            throw new IllegalArgumentException(
                    "Attempt to load xml file as PlayList")
        }
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
            if (PlayItem.isFinalLine(line)) {
                val item = PlayItem(lines.toList,itemBaseDir)
                items += item
                itemBaseDir = item.getBaseDir   //may not be what we passed in
                lines = new ListBuffer[String]
            }
            line = in.readLine()
        }
        //TODO - process trailling lines
        new PlayList(ui,baseDir,items.toArray,listComments)
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
