/* FileUtilS.scala
 *
 * Jim McBeath, May 27, 2008
 */

package net.jimmc.util

import java.io.BufferedReader
import java.io.File
import java.io.FilenameFilter
import java.io.FileReader
import java.io.FileWriter
import java.util.Arrays

import scala.collection.mutable.ArrayBuffer

object FileUtilS {
    /** Convenience method for running the "list" method on a File,
     * allowing us to pass in a filter function rather than
     * having to create an inner class every time.
     */
    def listDir(dir:File,filter:(String)=>Boolean):Array[String] = {
        val ff = new FilenameFilter() {
            override def accept(dir:File, name:String) = filter(name)
        }
        dir.list(ff)
    }
    def listDir(dir:File,filter:(File,String)=>Boolean):Array[String] = {
        val ff = new FilenameFilter() {
            override def accept(dir:File, name:String) = filter(dir,name)
        }
        dir.list(ff)
    }

    /** Given a path string, remove any internal ".." path parts along
     * with the preceding path part.
     */
    def collapseRelative(path:String) : String = {
        val p1:Array[String] = path.split(File.separator)
        val parts:ArrayBuffer[String] = new ArrayBuffer
        p1.copyToBuffer(parts)
        var i = 0
        while (i < parts.length) {
            if (parts(i)==".")
                parts.remove(i)
            else if (i > 0 && parts(i)==".." &&
                    parts(i-1)!=".." && parts(i-1)!="") {
                parts.remove(i)
                parts.remove(i-1)
                i = i -1
            }
            else
                i = i + 1
        }
        parts.mkString(File.separator)
    }

    /** Given a path, make it relative to another directory. */
    def relativeTo(path:String, base:File) : String = {
        val pathFile = new File(path)
        val useAbs = (pathFile.isAbsolute || base.isAbsolute)
        val pathParts =
            (if (useAbs)
                pathFile.getCanonicalPath
            else
                pathFile.getPath).
            split(File.separator)
        val baseParts =
            (if (useAbs)
                base.getCanonicalPath
            else
                base.getPath).
            split(File.separator)
        val equalParts = countSameElements(pathParts,baseParts)
            //count the number of leading parts that are the same
        val differentPathParts = pathParts.drop(equalParts)
        val differentBaseParts = baseParts.drop(equalParts)
        val prefix = differentBaseParts.filter(_ != ".").
                map((x:String)=>"..").
                mkString(File.separator)
        val suffix = differentPathParts.mkString(File.separator)
        val sep =
            if (differentBaseParts.length>0 && differentPathParts.length>0)
                File.separator
            else
                ""
        prefix+sep+suffix
    }

    //Given two sequences, return the count of the number of elements
    //which are the same in both lists before finding the first element
    //which is different.
    private[util] def countSameElements(a0:Seq[Any], b0:Seq[Any]):Int = {
        var a = a0
        var b = b0
        var n = 0
        while (!a.isEmpty && !b.isEmpty && a(0)==b(0)) {
            n = n + 1
            a = a.drop(1)
            b = b.drop(1)
        }
        n
    }

    /** Given a directory, get the next sibling directory. */
    def getNextDirectory(dir:File):File = getRelativeDirectory(dir,1)

    def getPreviousDirectory(dir:File):File = getRelativeDirectory(dir,-1)

    def getRelativeDirectory(dir:File, move:Int):File = {
        var parentDir = dir.getParentFile()
        if (parentDir==null)
            parentDir = new File(".")
        var siblings = parentDir.list()
        var dirIndex=0
        if (siblings!=null) {
            Arrays.sort(siblings.asInstanceOf[Array[Object]])
            val dirName = dir.getName()
            dirIndex = Arrays.binarySearch(
                    siblings.asInstanceOf[Array[Object]],dirName) 
            if (dirIndex<0) {
                val msg = "Can't find dir "+dirName+" in parent list"
                throw new RuntimeException(msg)
            }
        }
        var newDirIndex = dirIndex + move
        while (siblings==null || newDirIndex<0 || newDirIndex>=siblings.length){
            //We are at the end/start of our sibling directories,
            //so recurse up the directory tree and move the
            //parent to the next directory.
            parentDir = getRelativeDirectory(parentDir,move)
            if (parentDir==null)
                return null
            siblings = parentDir.list()
            if (siblings!=null && siblings.length!=0) {
                Arrays.sort(siblings.asInstanceOf[Array[Object]])
                if (newDirIndex<0)    //backing up
                    newDirIndex = siblings.length-1
                else
                    newDirIndex = 0
            }
        }
        new File(parentDir,siblings(newDirIndex))
    }

    /** Read in a text file, return the contents as a string. */
    def readFile(f:File):String = {
	    //throws FileNotFoundException, IOException
	val fr = new FileReader(f)
	val br = new BufferedReader(fr)
	val sb = new StringBuffer()
	var line = br.readLine()
        while (line!=null) {
	    sb.append(line)
	    sb.append("\n")
            line = br.readLine()
	}
	br.close()
	sb.toString()
    }

    /** Write text out to a file.
     */
    def writeFile(f:File, contents:String) {
	    //throws IOException
	val writer = new FileWriter(f)
	writer.write(contents)
	writer.close()
    }
}
