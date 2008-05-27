package net.jimmc.util

import java.io.File

import scala.collection.mutable.ArrayBuffer

object FileUtilS {
    /** Given a path string, remove any internal ".." path parts along
     * with the preceding path part.
     */
    def collapseRelative(path:String) : String = {
        val p1:Array[String] = path.split(File.separator)
        val parts:ArrayBuffer[String] = new ArrayBuffer
        p1.copyToBuffer(parts);
        var i = 1
        while (i < parts.length) {
            if (i > 0 && parts(i)==".." && parts(i-1)!="..") {
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
            (if (useAbs) pathFile.getAbsolutePath
            else pathFile.getPath).split(File.separator)
        val baseParts =
            (if (useAbs) base.getAbsolutePath
            else base.getPath).split(File.separator)
        val equalParts = countSameElements(pathParts,baseParts)
            //count the number of leading parts that are the same
        val differentPathParts = pathParts.drop(equalParts)
        val differentBaseParts = baseParts.drop(equalParts)
        val prefix = differentBaseParts.map((x:String)=>"..").
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
        while (!a.isEmpty && !b.isEmpty && a.headOption.get==b.headOption.get) {
            n = n + 1
            a = a.drop(1)
            b = b.drop(1)
        }
        n
    }
}
