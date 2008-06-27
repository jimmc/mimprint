/* StringUtil.scala
 *
 * Jim McBeath, Jun 22, 2008
 * converted from part of StringUtil.java of October 17, 2001
 */

package net.jimmc.util

object StringUtil {

    /** Get the width in chars of the longest line in the string. */
    def getLongestLineWidth(s:String):Int = {
	if (s==null)
	    return 0
        def max(a:Int,b:Int) = (if (a>b) a else b)
        (0 /: s.split("\n"))((a:Int,b:String) => max(a,b.length))
    }

    /** Get the number of lines of text in the string. */
    def getLineCount(s:String):Int = {
        s.filter(_=='\n').length + 1
    }
}
