/* WeakNumericComparator.java
 *
 * Jim McBeath, February 11, 2003
 *
 * From IQvis
 */

package net.jimmc.util;

import java.util.Comparator;

/** Compare two strings using a weak numeric comparison.
 * Any place we see decimal digits, we do an integer compare rather than
 * character by character.  Thus a2 comes before a11, and both come before ab
 * or aX where X is anything other than a decimal digit.
 */
public class WeakNumericComparator implements Comparator {
    public boolean equals(Object a, Object b) {
	return (compare(a,b)==0);
    }
    public int compare(Object a, Object b) {
	if (a==b)
	    return 0;	//includes null==null case
	if (a==null)
	    return 1;	//sort null to the end
	if (b==null)
	    return -1;
	String sa = a.toString();
	String sb = b.toString();
	int xa = 0;
	int xb = 0;
	int lena = sa.length();
	int lenb = sb.length();
	while (xa<lena && xb<lenb) {
	    char ca = sa.charAt(xa);
	    char cb = sb.charAt(xb);
	    boolean isDigitA = Character.isDigit(ca);
	    boolean isDigitB = Character.isDigit(cb);
	    if (isDigitA!=isDigitB)
	        return isDigitA?-1:1;	//digits precede all other chars
	    if (!isDigitA) {	//both are non-digits
		if (ca!=cb)
		    return (ca-cb);
		xa++;
		xb++;
		continue;
	    }
	    //At this point we have a digit for both a and b,
	    //so we collect the digits into an int and compare that.
	    int na = 0;
	    int nb = 0;
	    while (xa<lena && Character.isDigit(sa.charAt(xa))) {
		na = na*10 + (sa.charAt(xa)-'0');
		xa++;
	    }
	    while (xb<lenb && Character.isDigit(sb.charAt(xb))) {
		nb = nb*10 + (sb.charAt(xb)-'0');
		xb++;
	    }
	    if (na!=nb)
		return na-nb;
	    //Numbers are the same; xa and xb already point to
	    //the first non-digit, so we leave it there for the
	    //next go around the loop.
	}
	if (xa>=lena && xb>=lenb)
	    return 0;	//ran out at the same time, so equal
	if (xa>=lena)	//if a ran out first...
	    return -1;	//a is shorter
	else /* xb>=lenb */  //else b ran out first...
	    return 1;	//b is shorter
    }
}

/* end */
