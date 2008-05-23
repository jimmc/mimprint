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

    /** Create an empty playlist. */
    items = new ArrayBuffer[PlayItemS]        //no items

    /** Create a playlist from the given set of filenames. */
    def this(filenames:Array[String], start:Int, length:Int) {
        this()
        for (i <- 0 until length) {
            addItem(new PlayItemS(null,baseDir,filenames(i+start),0))
        }
    }

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
        items.foreach(_.printAll(out,baseDir))
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
        while (line!=null) {
            lines += line
            if (PlayItemS.isFinalLine(line)) {
                p.addItem(PlayItemS(lines.toList,baseDir))
                lines = new ListBuffer[String]
            }
            line = in.readLine()
        }
        //TODO - process trailling lines
        p
    }
}
