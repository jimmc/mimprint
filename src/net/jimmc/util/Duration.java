/* Duration.java
 *
 * Jim McBeath, November 7, 2001
 */

package net.jimmc.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;

//At first I made Duration a subclass of BigDecimal, but then when it gets
//passed to messageFormat it tries to format it as a number.

/** A Duration is an elapsed time measured in seconds. */
public class Duration implements Serializable {
    /** The default fraction precision. */
    private static final int DEFAULT_FRACTION_PRECISION=2;

    /** Our value. */
    protected BigDecimal duration;

    /** The number of fractional digits to display. */
    protected int fractionPrecision;

    /** Create a Duration from a double.
     * The fractionPrecision is set to 2.
     */
    public Duration(double duration) {
	this.duration = new BigDecimal(duration);
	setFractionPrecision(DEFAULT_FRACTION_PRECISION);
    }

    /** Create a Duration from a float.
     * The fractionPrecision is set to 2.
     */
    public Duration(float duration) {
	this.duration = new BigDecimal(duration);
	setFractionPrecision(DEFAULT_FRACTION_PRECISION);
    }

    /** Create a Duration from an int.
     * The fractionPrecision is set to 0.
     */
    public Duration(int duration) {
	this.duration = new BigDecimal((double)duration);
	setFractionPrecision(0);
    }

    /** Create a Duration from any kind of Number.
     * If the number is a Float, Double, or BigDecimal, the
     * fractionPrecision is set to 2; otherwise, it is set to 0.
     */
    public Duration(Number num) {
	if (num==null) {
	    duration = new BigDecimal(0.0);
	    return;
	}
	duration = new BigDecimal(num.doubleValue());
	if ((num instanceof Double)||
	    (num instanceof Float)||
	    (num instanceof BigDecimal)) {
	    setFractionPrecision(DEFAULT_FRACTION_PRECISION);
	}
    }

    /** Set the number of fractional digits to display. */
    public void setFractionPrecision(int digits) {
	fractionPrecision = digits;
    }

    /** Get the number of fractional digits to display. */
    public int getFractionPrecision() {
	return fractionPrecision;
    }

    /** Get the value of this duration as an int.
     */
    public int intValue() {
	return duration.intValue();
    }

    /** Get the value of this duration as a float. */
    public float floatValue() {
	return duration.floatValue();
    }

    /** Get the fractional part of our duration. */
    public double fractionalValue() {
	double d = duration.doubleValue();
	return d - Math.floor(d);
    }

    /** Convert to a string. */
    public String toString() {
	StringBuffer sb = new StringBuffer();
	int seconds = intValue();
	toString(sb,seconds);

	if (fractionPrecision>0) {
	    sb.append(".");
	    double f = fractionalValue();
	    f += 0.5*Math.pow(10.0,-fractionPrecision);
		//add a rounding factor just past the last digit
	    for (int i=0; i<fractionPrecision; i++) {
		f = f*10;
		int d = ((int)f);
		sb.append(Integer.toString(d));
		f -= d;
	    }
	}
	return sb.toString();
    }

    /** Convert seconds to a Duration string. */
    public static String toString(int seconds) {
	StringBuffer sb = new StringBuffer();
	toString(sb,seconds);
	return sb.toString();
    }

    /** Convert seconds to string in a buffer. */
    protected static void toString(StringBuffer sb, int seconds) {
	int minutes = seconds/60;
	seconds = seconds%60;
	int hours = minutes/60;
	minutes = minutes%60;
	int days = hours/24;
	hours = hours%24;
	if (days>0) {
	    sb.append(Integer.toString(days));
	    sb.append("d ");
	}
	if (days>0 || hours>0) {
	    sb.append(Integer.toString(hours/10));
	    sb.append(Integer.toString(hours%10));
	    sb.append(":");
	}
	sb.append(Integer.toString(minutes/10));
	sb.append(Integer.toString(minutes%10));
	sb.append(":");
	sb.append(Integer.toString(seconds/10));
	sb.append(Integer.toString(seconds%10));
    }

    /** Parse a string into an integer-valued duration.
     * @return The number of seconds in the duration.
     */
    public static int parseIntDuration(String s) {
	int hours = 0;
	int minutes = 0;
	int seconds = 0;
	int days = 0;
	if (isAllDigits(s)) {
	    //If the number is entered as a string of digits
	    //with no punctuation, assume it is in the form
	    //DHHMMSS
	    s = "0000000"+s;
	    int l = s.length();
	    seconds = parseInt(s,l-2,l);
	    minutes = parseInt(s,l-4,l-2);
	    hours = parseInt(s,l-6,l-4);
	    days = parseInt(s,0,l-6);
	    int n = ((days*24+hours)*60+minutes)*60+seconds;
	    return n;
	}
	int dPos = s.indexOf('d');
	if (dPos>0) {
	    String dayStr = s.substring(0,dPos);
	    s = s.substring(dPos+1).trim();
	    days = Integer.parseInt(dayStr);
	}
	String[] cStrs = StringUtil.toArray(s,':');
	String msg;
	switch (cStrs.length) {
	case 0:
	    msg = "No time in duration string";
		//TBD - i18n
	    throw new NumberFormatException(msg);
	case 1:
	    msg = "No colons in duration string";
		//TBD - i18n
	    throw new NumberFormatException(msg);
	case 2:		//minutes and seconds; no hours
	    if (days>0) {
		msg = "Days but no hours in duration";
		    //TBD - i18n
		throw new NumberFormatException(msg);
	    }
	    minutes = Integer.parseInt(cStrs[0]);
	    seconds = Integer.parseInt(cStrs[1]);
	    break;
	case 3:		//hours, minutes, and seconds
	    hours = Integer.parseInt(cStrs[0]);
	    minutes = Integer.parseInt(cStrs[1]);
	    seconds = Integer.parseInt(cStrs[2]);
	    break;
	default:	//too big
	    msg = "Too many colons in duration string";
		//TBD - i18n
	    throw new NumberFormatException(msg);
	}
	int n = ((days*24+hours)*60+minutes)*60+seconds;
	return n;
    }

    /** Parse a string into a double-valued duration.
     * @return The number of seconds in the duration.
     */
    public static double parseDoubleDuration(String s) {
	return parseDoubleDuration(s,DEFAULT_FRACTION_PRECISION);
    }
    public static double parseDoubleDuration(String s,
		int fractionPrecision) {
	if (isAllDigits(s)) {
	    //If the number is entered as a string of digits
	    //with no punctuation, assume it is in the form
	    //DHHMMSSFFF where the number of digits of
	    //fraction is equal to the fractionPrecision.

	    //Set up the fractional stuff
	    int p = fractionPrecision;
	    StringBuffer sb = new StringBuffer("000000");
	    int divisor = 1;
	    for (int i=0; i<p; i++) {
		divisor = divisor*10;
		sb.append("0");
	    }
	    sb.append(s);
	    s = sb.toString();
	    int l = s.length();
	    int frac = parseInt(s,l-p,l);
	    int seconds = parseInt(s,l-p-2,l-p);
	    int minutes = parseInt(s,l-p-4,l-p-2);
	    int hours = parseInt(s,l-p-6,l-p-4);
	    int days = parseInt(s,0,l-p-6);
	    int n = ((days*24+hours)*60+minutes)*60+seconds;
	    return (double)n+((double)frac/(double)divisor);
	}
	int dotPos = s.indexOf('.');
	if (dotPos<0) {
	    //No fractional part, parse it as an int
	    return (double)parseIntDuration(s);
	}
	int n = parseIntDuration(s.substring(0,dotPos));
	double f = parseFraction(s.substring(dotPos+1));
	return ((double)n)+f;
    }

    /** Parse a fractional number.
     * @param s The fractional part, starting with the digit just after
     *          the decimal point.  Digits only.
     * @return The fractional part, between 0 inclusive and 1 exclusive.
     */
    protected static double parseFraction(String s) {
	int n = Integer.parseInt(s);
	int len = s.length();
	return ((double)n)/Math.pow(10.0,(double)len);
    }

    /** True if the string consists entirely of digits. */
    protected static boolean isAllDigits(String s) {
	char[] cc = s.toCharArray();
	for (int i=0; i<cc.length; i++) {
	    if (!Character.isDigit(cc[i]))
		return false;
	}
	return true;
    }

    /** Parse a portion of a string as an integer. */
    protected static int parseInt(String s, int from, int to) {
	return Integer.parseInt(s.substring(from,to));
    }

    /** Return a new Duration initialized to the value
     * of the specified String.
     */
    public static Duration valueOf(String s) {
	return new Duration(parseIntDuration(s));
    }

    //The Serializable interface
    private void writeObject(ObjectOutputStream s) throws IOException {
	s.writeObject(duration);
	s.writeInt(fractionPrecision);
    }

    private void readObject(ObjectInputStream s) 
	    throws IOException, ClassNotFoundException {
	duration = (BigDecimal)(s.readObject());
	setFractionPrecision(s.readInt());
    }
    //End Serializable interface
}

/* end */
