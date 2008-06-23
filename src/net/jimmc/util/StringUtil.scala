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
	if (s==null || s=="")
	    return 0
	var n = 0	//number of newlines found
	var b = 0	//pointer to beginning of line
        var brk = false
	while (b<s.length()) {
	    b = s.indexOf("\n",b)
	    if (b<0)
		brk = true
            else
                b = b+1
                n = n+1
	}
	n+1
    }
}
