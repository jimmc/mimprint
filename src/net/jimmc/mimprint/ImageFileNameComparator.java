/* ImageFileNameComparator.java
 *
 * Jim McBeath, September 19, 2001
 */

package net.jimmc.mimprint;

import java.io.File;
import java.util.Comparator;

/** Compare two image file names to see how they should be ordered.
 */
public class ImageFileNameComparator implements Comparator {
        private File dir;    //the directory in which we are comparing

        public ImageFileNameComparator(File dir) {
            this.dir = dir;
        }

	/** Compare the two objects, which must be FileInfo objects.
         * Directories come before files.
	 * We skip over leading non-numeric chars until we come to digits,
	 * then interpret those as an integer and compare those integers.
	 * If they are the same, then we fall back on a simple string comapre.
	 */
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof FileInfo) || !(o2 instanceof FileInfo))
			return 0;	//TBD - throw exception
                FileInfo f1 = (FileInfo)o1;
                FileInfo f2 = (FileInfo)o2;
                if (f1.isDirectory() != f2.isDirectory())
                    return (f1.isDirectory()?-1:1);
		String s1 = f1.name;
		String s2 = f2.name;
		long n1 = getLongFromString(s1);
		long n2 = getLongFromString(s2);
		if (n1>n2)
		    return 1;
		else if (n1<n2)
		    return -1;
		else
		    return s1.compareTo(s2);
	}

	/** True if the objects are equal. */
	public boolean equals(Object that) {
		return (compare(this,that)==0);
	}

	/** Get an int from the string. */
	private long getLongFromString(String s) {
		int len = s.length();
		int i;
		for (i=0; i<len; i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c) && c!='0')
				break;	//found a digit
		}
		long n = 0;
		for ( ; i<len; i++) {
			char c = s.charAt(i);
			if (!Character.isDigit(c))
				break;
			long n0 = Character.digit(c,10);
			n *= 10;
			if (n0>=0)
				n += n0;
		}
		return n;
	}
}

/* end */
