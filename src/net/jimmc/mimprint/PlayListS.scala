package net.jimmc.mimprint

import net.jimmc.util.ArrayUtil
import net.jimmc.util.StringUtil

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/** A playlist of images. */
class PlayListS() extends PlayList {
    private var baseDir:File = _             //current base directory
    private var inputItem:PlayItem = _         //item being built when loading
    private var items:ArrayList[PlayItem] = _

    /** Create an empty playlist. */
    items = new ArrayList()        //no items

    /** Create a playlist from the given set of filenames. */
    def this(filenames:Array[String], start:Int, length:Int) {
        this()
        for (i <- 0 until length) {
            processLine(filenames(i+start),i+1)
        }
    }

    def setBaseDir(baseDir:File) = this.baseDir = baseDir

    override def clone():Object = copy

    def copy():PlayList = {
        val c = new PlayListS()
        for (i <- 0 until items.size()) {
            c.addItem(items.get(i).copy())
        }
        c.baseDir = baseDir
        c
    }

    override def equals(that:Any):Boolean = {
        that match {
        case other:PlayListS =>
            if ((baseDir==null || other.baseDir==null) && baseDir!=other.baseDir)
                return false
            if (!StringUtil.equals(baseDir.getPath(),other.baseDir.getPath()))
                return false
            return ArrayUtil.equals(items.toArray(),other.items.toArray())
        case x => false
        }
    }

    private def processLine(line:String, lineNumber:Int) {
        if (inputItem==null) {
            inputItem = App.getApp().getFactory().newPlayItem()
            inputItem.setBaseDir(baseDir)
        }
        if (inputItem.isOptionLine(line)) {
            inputItem.processOptionLine(line)
        } else if (inputItem.isImageInfoLine(line)) {
            inputItem.setImageInfoLine(line)
            addItem(inputItem)
            inputItem = null
        } else {
            inputItem.addTextLine(line);        //maintain all comments etc.
        }
    }

    def addItem(item:PlayItem) = items.add(item)

    /** Return the number of items in the playlist. */
    def size() = items.size()

    /** Count the number of non-empty items. */
    def countNonEmpty():Int = {
        var n = 0
        for (i <- 0 until size) {
            val item = getItem(i)
            if (item!=null && !item.isEmpty())
                n = n+1
        }
        n
    }

    def getItem(n:Int) = items.get(n)

    def getFileNames():Array[String] = {
        val fileNames = new Array[String](size())
        for (i <- 0 until items.size()) {
            fileNames(i) = items.get(i).getFileName()
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
        val n = size()
        for (i <- 0 until n) {
            val item = items.get(i)
            item.printAll(out,baseDir)
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
        var line:String = in.readLine()
        while (line!=null) {
            p.processLine(line,in.getLineNumber())
            line = in.readLine()
        }
        p
    }
}
