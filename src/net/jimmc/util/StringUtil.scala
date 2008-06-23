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
	var maxLen = 0
	var b = 0		//beginning of a line
	while (b<s.length()) {
	    var e = s.indexOf("\n",b)
	    if (e<0)
		e = s.length()
	    if (e-b>maxLen)
		maxLen = e-b
	    b = e+1
	}
	maxLen
    }

    /** Get the number of lines of text in the string. */
    def getLineCount(s:String):Int = {
        s.filter(_=='\n').length + 1
    }
}
