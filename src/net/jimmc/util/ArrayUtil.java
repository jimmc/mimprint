/* ArrayUtil.java
 *
 * Jim McBeath, October 25, 2001
 */

package net.jimmc.util;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

/** Utility methods for arrays. */
public class ArrayUtil {
    private ArrayUtil(){}	//Can't instantiate this class
    static void testInit() { new ArrayUtil(); } //for coverage testing

    /** True if two arrays of objects contain equal elements. */
    public static boolean equals(Object[] a, Object[] b) {
	if (a==b)
	    return true;
	if (a==null || b==null)
	    return false;
	if (a.length!=b.length)
	    return false;
	for (int i=0; i<a.length; i++) {
	    if (a[i]==b[i])
		continue;
	    if (a[i]==null || b[i]==null)
		return false;
	    if (!a[i].equals(b[i]))
		return false;
	}
	return true;
    }

    /** True if two arrays of ints contain equal elements. */
    public static boolean equals(int[] a, int[] b) {
	if (a==b)
	    return true;
	if (a==null || b==null)
	    return false;
	if (a.length!=b.length)
	    return false;
	for (int i=0; i<a.length; i++) {
	    if (a[i]!=b[i])
		return false;
	}
	return true;
    }

    /** Return the location of a string in an unsorted array of strings. */
/*
    //Not needed, use the Object[] version
    public static int OLDindexOf(String[] a, String s) {
	if (a==null)
	    return -1;
	for (int i=0; i<a.length; i++) {
	    if (a[i]==null) {
		if (s==null)
		    return i;
	    } else {
		if (a[i].equals(s))
		    return i;
	    }
	}
	return -1;
    }
*/

    /** Return the location of an object in an unsorted array of objects. */
    public static int indexOf(Object[] a, Object v) {
	if (a==null)
	    return -1;
	for (int i=0; i<a.length; i++) {
	    if (a[i]==null) {
		if (v==null)
		    return i;
	    } else {
		if (a[i].equals(v))
		    return i;
	    }
	}
	return -1;
    }

    /** Return the location of an int in an unsorted array of ints. */
    public static int indexOf(int[] a, int n) {
	if (a==null)
	    return -1;
	for (int i=0; i<a.length; i++) {
	    if (a[i]==n)
		return i;
	}
	return -1;
    }

    /** True if the unsorted array contains the string. */
    //Not needed, use the Object[] version
/*
    public static boolean OLDcontains(String[] a, String s) {
	return (indexOf(a,s)>=0);
    }
*/

    /** True if the unsorted array contains the object. */
    public static boolean contains(Object[] a, Object v) {
	return (indexOf(a,v)>=0);
    }

    /** True if the unsorted array contains the int. */
    public static boolean contains(int[] a, int n) {
	return (indexOf(a,n)>=0);
    }

    /** Convert a two dimensional array to a one dimensional list. */
    public static Object[] arrayToList(Object[][] a) {
        if (a==null)
	    return null;
	if (a.length==0)
	    return new Object[0];
	//Find the largest second dimension
	int max2 = 0;
	for (int i=0; i<a.length; i++) {
	    if (a[i]!=null && a[i].length>max2)
	        max2 = a[i].length;
	}
	//Assume a square array
	int rSize = a.length * max2;
	Object[] r = new Object[rSize];
	for (int i=0; i<a.length; i++) {
	    if (a[i]==null)
	        continue;
	    System.arraycopy(a[i],0,r,i*max2,a[i].length);
	}
	return r;
    }

    //TBD - use the Object[] version
    /** Concatenate two string arrays into a new string array. */
    public static String[] cat(String[] a1, String[] a2) {
	if (a1==null)
	    return a2;
	if (a2==null)
	    return a1;
	String[] aa = new String[a1.length+a2.length];
	System.arraycopy(a1,0,aa,0,a1.length);
	System.arraycopy(a2,0,aa,a1.length,a2.length);
	return aa;
    }

    /** Concatenate two object arrays into a new object array.
     * @param a1 The first array.
     * @param a2 The second array.
     * @return An array whose size of the sum of the size of a1 and a2,
     *     with the contents of a1 followed by the contents of a2.
     *     If a1 is null, a2 is returned; if a2 is null, a1 is returned.
     *     The run-time type of the returned array is the same as the
     *     run-time type of a1, unless a1 is null, in which case the
     *     run-time type of the returned array is the same as the
     *     run-time type of a2.
     */
    public static Object[] cat(Object[] a1, Object[] a2) {
	if (a1==null)
	    return a2;
	if (a2==null)
	    return a1;
	Object[] aa = ClassUtil.newArray(a1,a1.length+a2.length);
	System.arraycopy(a1,0,aa,0,a1.length);
	System.arraycopy(a2,0,aa,a1.length,a2.length);
	return aa;
    }

    /** Take the union of two unsorted sets of strings. */
    public static String[] union(String[] a1, String[] a2) {
	Vector vv = new Vector();
	for (int i=0; i<a1.length; i++) {
	    vv.addElement(a1[i]);
	}
	for (int i=0; i<a2.length; i++) {
	    if (!vv.contains(a2[i]))
		vv.addElement(a2[i]);
	}
	String[] aa = new String[vv.size()];
	vv.copyInto(aa);
	return aa;
    }

    /** Take the intersection of two sorted sets of objects.
     * The objects must implements the Comparable interface.
     * @return The set of objects from a1 which compare equals to
     *         objects in a2.  Note that intersection(a,b) may not
     *         return identical results as intersection(b,a) if the
     *         compareTo method of the underlying objects returns
     *         as equal two objects which are not identical.
     *         The run-time type of the returned array is the same as
     *         the run-time type of a1.
     */
    public static Object[] intersect(Comparable[] a1, Comparable[] a2) {
	Vector vv = new Vector();
	int i1 = 0;
	int i2 = 0;
	while (i1<a1.length && i2<a2.length) {
	    Comparable o1 = a1[i1];
	    Comparable o2 = a2[i2];
	    int x = o1.compareTo(o2);
	    if (x==0) {
		vv.addElement(o1);
		i1++;
		i2++;
	    } else if (x<0) {
		i1++;
	    } else /* x>0 */ {
		i2++;
	    }
	}
	Object[] aa = ClassUtil.newArray(a1,vv.size());
	vv.copyInto(aa);
	return aa;
    }

    //TBD - use the Object[] version
    /** Given two unsorted arrays of strings, return the list of all
     * entries which are in the first array and not in the second.
     * @param a1 All returned entries are from this list.
     * @param a2 No returned entries are in this list.
     * @return All entries in a1 and not in a2.
     *         If a1 is null, returns null.
     *         If a2 is null, returns a1.
     */
/*
    public static String[] andNot(String[] a1, String[] a2) {
	if (a1==null)
	    return null;
	if (a2==null)
	    return a1;
	Vector v = new Vector();
	for (int i=0; i<a1.length; i++) {
	    if (contains(a2,a1[i]))
		continue;	//in a2, don't add it
	    v.addElement(a1[i]);	//assume no dups in a1
	}
	String[] r = new String[v.size()];
	v.copyInto(r);
	return r;
    }
*/

    //TBD - use the Object[] version
    /** Given two sorted arrays of strings, return the list of all
     * entries which are in the first array and not in the second.
     * @param a1 All returned entries are from this list.
     * @param a2 No returned entries are in this list.
     * @return All entries in a1 and not in a2.
     *         If a1 is null, returns null.
     *         If a2 is null, returns a1.
     */
/*
    public static String[] andNotSorted(String[] a1, String[] a2) {
	if (a1==null)
	    return null;
	if (a2==null)
	    return a1;
	Vector v = new Vector();
	int i1 = 0;
	int i2 = 0;
	while (i1<a1.length && i2<a2.length) {
	    int x = a1[i1].compareTo(a2[i2]);
	    if (x>0) {
		//Need to bring a2 along
		i2++;
	    } else if (x==0) {
		//The strings match, don't include it in result
		i1++;
		i2++;
	    } else {
		assert(x<0);
		//a2 is already past here, so it's not in a2
		v.addElement(a1[i1]);
		i1++;
	    }
	}
	while (i1<a1.length) {
	    //Ran out of a2, put everything into the results
	    v.addElement(a1[i1]);
	    i1++;
	}
	String[] r = new String[v.size()];
	v.copyInto(r);
	return r;
    }
*/

    /** Given two unsorted arrays of comparables, return the list of all
     * entries which are in the first array and not in the second.
     * @param a1 All returned entries are from this list.
     * @param a2 No returned entries are in this list.
     * @return All entries in a1 and not in a2.
     *         If a1 is null, returns null.
     *         If a2 is null, returns a1.
     */
    public static Object[] andNot(Comparable[] a1, Comparable[] a2) {
	if (a1==null)
	    return null;
	if (a2==null)
	    return a1;
	Vector v = new Vector();
	for (int i=0; i<a1.length; i++) {
	    if (contains(a2,a1[i]))
		continue;	//in a2, don't add it
	    v.addElement(a1[i]);	//assume no dups in a1
	}
	Object[] r = ClassUtil.newArray(a1,v.size());
	v.copyInto(r);
	return r;
    }

    /** Given two sorted arrays of objects, return the list of all
     * entries which are in the first array and not in the second.
     * @param a1 All returned entries are from this list.
     * @param a2 No returned entries are in this list.
     * @return All entries in a1 and not in a2.
     *         If a1 is null, returns null.
     *         If a2 is null, returns a1.
     */
    public static Object[] andNotSorted(Comparable[] a1, Comparable[] a2) {
	if (a1==null)
	    return null;
	if (a2==null)
	    return a1;
	Vector v = new Vector();
	int i1 = 0;
	int i2 = 0;
	while (i1<a1.length && i2<a2.length) {
	    int x = a1[i1].compareTo(a2[i2]);
	    if (x>0) {
		//Need to bring a2 along
		i2++;
	    } else if (x==0) {
		//The objects match, don't include it in result
		i1++;
		i2++;
	    } else /* x<0 */ {
		//a2 is already past here, so it's not in a2
		v.addElement(a1[i1]);
		i1++;
	    }
	}
	while (i1<a1.length) {
	    //Ran out of a2, put everything into the results
	    v.addElement(a1[i1]);
	    i1++;
	}
	Object[] r = ClassUtil.newArray(a1,v.size());
	v.copyInto(r);
	return r;
    }

    /** Given two unsorted arrays of ints, return the list of all
     * entries which are in the first array and not in the second.
     * @param a1 All returned entries are from this list.
     * @param a2 No returned entries are in this list.
     * @return All entries in a1 and not in a2.
     *         If a1 is null, returns null.
     *         If a2 is null, returns a1.
     */
    public static int[] andNot(int[] a1, int[] a2) {
	if (a1==null)
	    return null;
	if (a2==null)
	    return a1;
	boolean[] a1marks = new boolean[a1.length];
	int a1count = 0;	//number of marks in a1marks
	for (int i=0; i<a1.length; i++) {
	    if (!contains(a2,a1[i])) {
		a1marks[i] = true;
		a1count++;
	    }
	}
	int[] r = new int[a1count];
	int rx = 0;
	for (int i=0; i<a1.length; i++) {
	    if (a1marks[i])
		r[rx++] = a1[i];
	}
	return r;
    }

    /** Given two sorted arrays of ints, return the list of all
     * entries which are in the first array and not in the second.
     * @param a1 All returned entries are from this list.
     * @param a2 No returned entries are in this list.
     * @return All entries in a1 and not in a2.
     *         If a1 is null, returns null.
     *         If a2 is null, returns a1.
     */
    public static int[] andNotSorted(int[] a1, int[] a2) {
	if (a1==null)
	    return null;
	if (a2==null)
	    return a1;
	int i1 = 0;
	int i2 = 0;
	boolean[] a1marks = new boolean[a1.length];
	int a1count = 0;	//number of marks in a1marks
	while (i1<a1.length && i2<a2.length) {
	    int x = a1[i1]-a2[i2];
	    if (x>0) {
		//Need to bring a2 along
		i2++;
	    } else if (x==0) {
		//The numbers match, don't include it in result
		i1++;
		i2++;
	    } else /* x<0 */ {
		//a2 is already past here, so it's not in a2
		a1marks[i1] = true;
		a1count++;
		i1++;
	    }
	}
	while (i1<a1.length) {
	    //Ran out of a2, put everything into the results
	    a1marks[i1] = true;
	    a1count++;
	    i1++;
	}
	int[] r = new int[a1count];
	int rx = 0;
	for (i1=0; i1<a1.length; i1++) {
	    if (a1marks[i1])
		r[rx++] = a1[i1];
	}
	return r;
    }

    /** Given an array containing nulls, return a compact array with no nulls.
     * @param aa The array which may contain nulls.
     * @return An array with no top-level nulls.
     *         If the input array contained no nulls, then it is the return
     *         value.
     *         The run-time type of the returned array is the same as
     *         the type of the input array.
     */
    public static Object[] removeNulls(Object[] aa) {
	if (aa==null)
	    return null;
        int nullCount = 0;
	for (int i=0; i<aa.length; i++) {
	    if (aa[i]==null)
	        nullCount++;
	}
	if (nullCount==0)
	    return aa;		//no nulls to remove
	int n = aa.length - nullCount;
	Object[] results = ClassUtil.newArray(aa,n);
	int j = 0;
        for (int i=0; i<aa.length; i++) {
	    if (aa[i]==null)
	        continue;
	    results[j++] = aa[i];
	}
	return results;
    }

    /** Get an array of randomly ordered ints.
     * @param n The size of the array.
     * @return An array of length n, with entries from 0 to n-1 in
     *         random order.
     */
    public static int[] randomArray(int n) {
	return randomArray(new Random(), n);
    }

    //We include a version where we pass in the Random mainly to allow
    //regression testing, which we do by passing in a Random which has
    //been initialized to a known value.
    /** Get an array of randomly ordered ints.
     * @param n The size of the array.
     * @return An array of length n, with entries from 0 to n-1 in
     *         random order.
     */
    public static int[] randomArray(Random rand, int n) {
	long[] nn = new long[n];
	for (int i=0; i<n; i++) {
	    long r = rand.nextInt();
	    nn[i] = ((r<<32)&0x7FFFFFFF00000000L)+i;
	}
	Arrays.sort(nn);
	int[] results = new int[n];
	for (int i=0; i<n; i++) {
	    results[i] = (int)(nn[i]&0x7FFFFFFF); //the int portion
	}
	return results;
    }

    /** Shuffle an array in random order. */
    public static Object[] shuffle(Object[] aa) {
	return shuffle(new Random(), aa);
    }

    /** Shuffle an array in random order.
     * @param rand Randomization.
     * @param aa The array to randomize.
     * @return An array of the same type and length as aa,
     *         with the same elements in a randomized order.
     */
    public static Object[] shuffle(Random rand, Object[] aa) {
	if (aa==null)
	    return null;
	int n = aa.length;
	int[] randomIndexes = randomArray(rand, n);
	Object[] results = ClassUtil.newArray(aa,n);
	for (int i=0; i<n; i++) {
	    int x = randomIndexes[i];
	    results[i] = aa[x];
	}
	return results;
    }
}

/* end */
