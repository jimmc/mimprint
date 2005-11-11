/* ImageFileNameComparator.java
 *
 * Jim McBeath, September 19, 2001
 */

package jimmc.jiviewer;

import java.io.File;
import java.util.Comparator;

/** Compare two image file names to see how they should be ordered.
 */
public class ImageFileNameComparator implements Comparator {
        private File dir;    //the directory in which we are comparing

        public ImageFileNameComparator(File dir) {
            this.dir = dir;
        }

	/** Compare the two objects, which must be strings.
	 * We skip over leading non-numeric chars until we come to digits,
	 * then interpret those as an integer and compare those integers.
	 * If they are the same, then we fall back on a simple string comapre.
	 */
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof String) || !(o2 instanceof String))
			return 0;	//TBD - throw exception
		String s1 = (String)o1;
		String s2 = (String)o2;
		int n1 = getIntFromString(s1);
		int n2 = getIntFromString(s2);
		if (n1!=n2) {
			return (n1-n2);
		}
		return s1.compareTo(s2);
	}

	/** True if the objects are equal. */
	public boolean equals(Object that) {
		return (compare(this,that)==0);
	}

	/** Get an int from the string. */
	protected int getIntFromString(String s) {
		int len = s.length();
		int i;
		for (i=0; i<len; i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c) && c!='0')
				break;	//found a digit
		}
		int n = 0;
		for ( ; i<len; i++) {
			char c = s.charAt(i);
			if (!Character.isDigit(c))
				break;
			int n0 = Character.digit(c,10);
			n *= 10;
			if (n0>=0)
				n += n0;
		}
		return n;
	}
}

/* end */
