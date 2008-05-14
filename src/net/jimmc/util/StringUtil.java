/* StringUtil.java
 *
 * Jim McBeath, October 17, 2001
 */

package net.jimmc.util;

import java.util.StringTokenizer;
import java.util.Vector;

/** Utility methods for use with strings. */
public class StringUtil {
    private StringUtil(){};	//Can't instantiate this class
    static void testInit() { new StringUtil(); }; //allow full test coverage

    /** Split up a string into an array of strings on a separator char. */
    public static String[] toArray(String s, char sep) {
	return toArray(s,sep,false);
    }

    /** Split up a string into an array of strings.
     * @param s The string to split.
     * @param sep The separator character on which to split.
     * @param ignoreEmpty If true, treat multiple separators as
     *        a single separator, and ignore blank entries between
     *        separators.
     * @return The separated strings.
     */
    public static String[] toArray(String s, char sep, boolean ignoreEmpty){
	if (s==null)
	    return null;
	Vector v = new Vector();
	int len = s.length();
	if (len==0)
	    return new String[0];
	int p=0;
	while (true) {	//exit loop via break when no more sep chars
	    int x = s.indexOf(sep,p);
	    if (x<0)
		break;
	    if (!ignoreEmpty || p<x)
		v.addElement(s.substring(p,x));
	    p = x+1;
	}
	if (!ignoreEmpty || p<s.length())
	    v.addElement(s.substring(p));
	String[] ss = new String[v.size()];
	v.copyInto(ss);
	return ss;
    }
 
    /** Split up a string into an array of ints on a separator char. */
    public static int[] toIntArray(String s, char sep) {
	String[] ss = toArray(s,sep,false);
	int[] nn = new int[ss.length];
	for (int i=0; i<nn.length; i++) {
	    nn[i] = Integer.parseInt(ss[i]);
	}
	return nn;
    }

    /** Split up a string into an array of Integers on a separator char. */
    public static Integer[] toIntegerArray(String s, char sep) {
	String[] ss = toArray(s,sep,false);
	Integer[] nn = new Integer[ss.length];
	for (int i=0; i<nn.length; i++) {
	    nn[i] = Integer.valueOf(ss[i]);
	}
	return nn;
    }

    /** Given an array, convert it to a single string. */
    public static String toString(Object[] aa, char sep) {
	return toString(aa,String.valueOf(sep));
    }

    /** Given an array, convert it to a single string. */
    public static String toString(Object[] aa, String sep) {
	if (aa==null)
	    return null;
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<aa.length; i++) {
	    if (i>0)
		sb.append(sep);
	    sb.append(aa[i]);
	}
	return sb.toString();
    }

    /** Given an array, convert it to a single string. */
    public static String toString(int[] aa, String sep) {
	if (aa==null)
	    return null;
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<aa.length; i++) {
	    if (i>0)
		sb.append(sep);
	    sb.append(aa[i]);
	}
	return sb.toString();
    }

    /** Return the initials of a string.
     * @param name The string from which to get the initials.
     * @return A string consisting
     * of the first non-blank character of the string plus the first
     * non-blank character after each blank.
     */
    public static String getInitials(String name) {
	name = name.trim();
	StringTokenizer st = new StringTokenizer(name);
	StringBuffer sb = new StringBuffer();
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    sb.append(token.charAt(0));
	}
	return sb.toString();
    }

    /** Return the upper-case initials of a string.
     * @param name The string from which to get the initials.
     * @return A string consisting of the first non-blank character of
     * each space-separated token in the string which is an upper-case
     * character.
     */
    public static String getUpperCaseInitials(String name) {
	name = name.trim();
	StringTokenizer st = new StringTokenizer(name);
	StringBuffer sb = new StringBuffer();
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    char ch = token.charAt(0);
	    if (Character.isUpperCase(ch))
		sb.append(ch);
	}
	return sb.toString();
    }

    /** Get the width in chars of the longest line in the string. */
    public static int getLongestLineWidth(String s) {
	if (s==null)
	    return 0;
	int maxLen = 0;
	int b = 0;		//beginning of a line
	while (b<s.length()) {
	    int e = s.indexOf("\n",b);
	    if (e<0)
		e = s.length();
	    if (e-b>maxLen)
		maxLen = e-b;
	    b = e+1;
	}
	return maxLen;
    }

    /** Get the number of lines of text in the string. */
    public static int getLineCount(String s) {
	if (s==null || s.equals(""))
	    return 0;
	int n = 0;	//number of newlines found
	int b = 0;	//pointer to beginning of line
	while (b<s.length()) {
	    b = s.indexOf("\n",b);
	    if (b<0)
		break;
	    b++;
	    n++;
	}
	return n+1;
    }

    /** Replace named MessageFormat field references by field numbers.
     * The named field reference looks just like a regular format
     * segment, but with a string in place of the number.
     */
    public static String mapFieldNamesToNumbers(String s, String[] fieldNames) {
	for (int i=0; i<fieldNames.length; i++) {
	    String fieldName = fieldNames[i];
	    if (fieldName==null || fieldName.equals(""))
		continue;	//skip blanks in the array
	    String p = "\\{"+fieldName+"\\}";
	    String q = "{"+i+"}";
	    s = s.replaceAll(p,q);
	    p = "\\{"+fieldName+"\\,";
	    q = "{"+i+",";
	    s = s.replaceAll(p,q);
	}
	return s;
    }

    /** True if both strings are null, or both are not null and are equal. */
    public static boolean equals(String s1, String s2) {
        if (s1==s2)
            return true;        //includes the case where both are null
        if (s1==null || s2==null)
            return false;       //one is null but not the other
        return s1.equals(s2);
    }
}

/* end */
