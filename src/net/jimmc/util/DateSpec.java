/* DateSpec.java
 *
 * Jim McBeath, April 15, 2002
 */

package net.jimmc.util;

import java.util.Date;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

/** A representation of date or date/time information with specified precision.
 * This can encode year/month/day/hour/minute/second/fractionalSecond,
 * or any less precise subset.
 * There are two primary differences between this and Date or Timestamp:
 * <ol>
 * <li>DateSpec allows representing partial date information, thus
 *     can distinguish between "2002", "2002-01" (Jan 2002)
 *     and "2002-01-02" (Jan 1, 2002).
 * <li>DateSpec does not encode the time zone.  All times are local times.
 * </ol>
 */
public class DateSpec implements Cloneable, Comparable, Serializable {
    /** Our date. */
    protected Date date;
    //We used to extend the Date class, but then various other classes
    //(such as JTable) thought they knew how to display us.
    //They displayed by formatting it themselves, but we want them to
    //display by calling our toString method.

    /** The precision of our date. */
    protected int precision;
	/** No data specified. */
	public final static int PRECISION_NONE = 0;
     	/** Precision to the year. */
	public final static int PRECISION_YEAR = 1;
     	/** Precision to the month. */
	public final static int PRECISION_MONTH = 2;
     	/** Precision to the day. */
	public final static int PRECISION_DAY = 3;
     	/** Precision to the hour. */
	public final static int PRECISION_HOUR = 4;
     	/** Precision to the minute. */
	public final static int PRECISION_MINUTE = 5;
     	/** Precision to the second. */
	public final static int PRECISION_SECOND = 6;
     	/** Precision to the fractional second. */
	public final static int PRECISION_FRACTION = 7;

    /** A cached of date formats, indexed by precision value. */
    static SimpleDateFormat[] dateFormats;

    /** A cached of localized date formats, indexed by precision value. */
    static SimpleDateFormat[] localizedDateFormats;

    /** A set of additional alternate date formats for input. */
    static SimpleDateFormat[] alternateDateFormats;
    static int[] alternateDatePrecisions;

    /** A set of formats used to parse only a time, without a date. */
    static SimpleDateFormat[] timeOnlyFormats;

    /** The format for this instance. */
    protected SimpleDateFormat dateFormat;

    /** Initialize our resources.
     * Call this method before instantiating any DateSpec instances
     * or using any date formats.
     */
    public static void initResources(ResourceSource res) {
        dateFormats = initResourceFormatArray(res,"dateFormats");
        localizedDateFormats =
		initResourceFormatArray(res,"localizedDateFormats");
        alternateDateFormats =
		initResourceFormatArray(res,"alternateDateFormats");
        alternateDatePrecisions =
		initResourceIntArray(res,"alternateDatePrecisions");
        timeOnlyFormats = initResourceFormatArray(res,"timeOnlyFormats");
    }

    /** Init one of our format arrays. */
    private static SimpleDateFormat[] initResourceFormatArray(
    		ResourceSource res, String itemName) {
    	String resName = "DateSpec."+itemName;
	String resValue = res.getResourceString(resName);
	String[] formatStrings = StringUtil.toArray(resValue,'|');
	SimpleDateFormat[] formats = new SimpleDateFormat[formatStrings.length];
	for (int i=0; i<formats.length; i++) {
	    formats[i] = new SimpleDateFormat(formatStrings[i]);
	}
	return formats;
    }

    /** Init an array of ints from resources. */
    private static int[] initResourceIntArray(
    		ResourceSource res, String itemName) {
    	String resName = "DateSpec."+itemName;
	String resValue = res.getResourceString(resName);
	return StringUtil.toIntArray(resValue,'|');
    }

    /** Create a DateSpec with no data. */
    public DateSpec() {
	date = new Date(0);
	setPrecision(PRECISION_NONE);
    }

    /** Create a DateSpec from a Date and precision. */
    public DateSpec(Date date, int precision) {
	this.date = new Date(date.getTime());	//make a copy
	if (precision<PRECISION_NONE || precision>PRECISION_FRACTION) {
	    String msg = "precision "+precision;
	    throw new IllegalArgumentException(msg);
	}
	setPrecision(precision);
    }

    /** Create a DateSpec from a string. */
    public DateSpec(String s) {
	if (s==null)
	    s="";
	s = s.trim();
	int sLen = s.length();
	if (sLen==0) {
	    date = new Date(0);
	    setPrecision(PRECISION_NONE);
	    return;
	}

	ParsePosition pos = new ParsePosition(0);
	//Try parsing using a localized format
	for (int p=PRECISION_FRACTION; p>=PRECISION_YEAR; p--) {
	    SimpleDateFormat dateFmt = getLocalizedDateFormat(p);
	    if (parseUsingFormat(s,dateFmt,p,pos))
	        return;
	}
	//Try parsing using a standard normalized format
	for (int p=PRECISION_FRACTION; p>=PRECISION_YEAR; p--) {
	    SimpleDateFormat dateFmt = getDateFormat(p);
	    if (parseUsingFormat(s,dateFmt,p,pos))
	        return;
	}
	//Try parsing using an alternate format
	getAlternateDateFormat(0);	//make sure it's initialized
	for (int x=0; x<alternateDateFormats.length; x++) {
	    SimpleDateFormat dateFmt = getAlternateDateFormat(x);
	    if (parseUsingFormat(s,dateFmt,getAlternateDatePrecision(x),pos))
	        return;
	}

	//Unable to parse any format
	String msg = "Unable to parse: "+s;	//TBD i18n
	throw new IllegalArgumentException(msg);
	//TBD - give a sample format in the error message
    }

    /** Parse a string using the specified format.
     */
    private boolean parseUsingFormat(String s, SimpleDateFormat dateFmt,
    		int precision, ParsePosition pos) {
	pos.setIndex(0);
	Date d = (Date)dateFmt.parseObject(s,pos);
	int x = pos.getIndex();
	if (x>=s.length() && d!=null) {
	    //Parsed it all
	    date = new Date(d.getTime());
	    setPrecision(precision);
	    return true;	//successful parse
	}
	return false;	//no parse
    }

    /** Parse a string looking for only a time. */
    public static Date parseTimeOnly(String s) {
	getTimeOnlyFormat(0);	//make sure it's initialized
	int slen = s.length();
	ParsePosition pos = new ParsePosition(0);
	for (int n=0; n<timeOnlyFormats.length; n++) {
	    SimpleDateFormat dateFmt = getTimeOnlyFormat(n);
	    pos.setIndex(0);
	    Date d = (Date)dateFmt.parseObject(s,pos);
	    int x = pos.getIndex();
	    if (x>=slen && d!=null) {
		//Parsed it all
		return d;
	    }
	}
	return null;	//can't parse it
    }

    /** Create a DateSpec from a Date.
     * The precision is set to PRECISION_SECOND.
     */
    public DateSpec(Date date) {
	this(date,PRECISION_SECOND);
    }

    /** Create a DateSpec from another DateSpec.
     */
    public DateSpec(DateSpec ds) {
	date = new Date(ds.getDate().getTime());
	setPrecision(ds.getPrecision());
    }

    /** Get our date. */
    public Date getDate() {
	return date;
    }

    /** Set our precision. */
    public void setPrecision(int precision) {
	this.precision = precision;
	setDateFormat();
    }

    /** Get our precision. */
    public int getPrecision() {
	return precision;
    }

    /** Change the value of the DateSpec to be the next time value
     * of the same increment.  For example, if the precision of the
     * DateSpec is PRECISION_DAY, then add one day to the DateSpec.
     */
    public void increment() {
	int inc = 0;	//number of seconds to add to the date
	switch (precision) {
	case PRECISION_YEAR:
	    date = new Date(date.getYear()+1,date.getMonth(),
		date.getDate(),date.getHours(),
		date.getMinutes(),date.getSeconds());
		//We lose the fractional part, but we don't
		//really care about it when precision = year
	    return;
	case PRECISION_MONTH:
	    int month = date.getMonth();
	    int year = date.getYear();
	    if (++month>12) {
		month = 1;
		year++;
	    }
	    date = new Date(year, month,
		date.getDate(),date.getHours(),
		date.getMinutes(),date.getSeconds());
	    return;
	case PRECISION_DAY:
	    inc = 24 * 60 * 60;
	    break;
	case PRECISION_HOUR:
	    inc = 60 * 60;
	    break;
	case PRECISION_MINUTE:
	    inc = 60;
	    break;
	case PRECISION_SECOND:
	    inc = 1;
	    break;
	default:	//including PRECISION_NONE
	case PRECISION_FRACTION:
	    inc = 0;
	    break;
	}
	date = new Date(date.getTime()+1000*inc);
    }

    /** Get the format pattern string for a normalized date without time. */
    public static String getNormalizedDatePattern() {
        return dateFormats[PRECISION_DAY].toPattern();
    }

    /** Get the format pattern string for a normalized date with time. */
    public static String getNormalizedDateTimePattern() {
        return dateFormats[PRECISION_SECOND].toPattern();
    }

    /** Get our canonical date format according to the precision. */
    protected static SimpleDateFormat getDateFormat(int precision) {
	if (dateFormats==null) {
	    throw new RuntimeException("dateFormats not initialized");
	}
	if (precision<PRECISION_NONE || precision>PRECISION_FRACTION) {
	    String msg = "precision "+precision;
	    throw new IllegalArgumentException(msg);
	}
	return dateFormats[precision];
    }

    /** Get our localized date format according to the precision. */
    public static SimpleDateFormat getLocalizedDateFormat(int precision) {
	if (localizedDateFormats==null) {
	    throw new RuntimeException("localizedDateFormats not initialized");
	}
	if (precision<PRECISION_NONE || precision>PRECISION_FRACTION) {
	    String msg = "precision "+precision;
	    throw new IllegalArgumentException(msg);
	}
	return localizedDateFormats[precision];
    }

    /** Get an alternate localized date format. */
    protected static SimpleDateFormat getAlternateDateFormat(int index) {
	if (alternateDateFormats==null) {
	    throw new RuntimeException("alternateDateFormats not initialized");
	}
	if (index<0 || index>=alternateDateFormats.length) {
	    String msg = "index "+index;
	    throw new IllegalArgumentException(msg);
	}
	return alternateDateFormats[index];
    }
    protected static int getAlternateDatePrecision(int index) {
        return alternateDatePrecisions[index];
    }

    /** Get a time-only format. */
    protected static SimpleDateFormat getTimeOnlyFormat(int index) {
	if (timeOnlyFormats==null) {
	    throw new RuntimeException("timeOnlyFormats not initialized");
	}
	if (index<0 || index>=timeOnlyFormats.length) {
	    String msg = "index "+index;
	    throw new IllegalArgumentException(msg);
	}
	return timeOnlyFormats[index];
    }

    /** Set our canonical date format according to the precision. */
    protected void setDateFormat() {
	dateFormat = getDateFormat(precision);
    }

    /** Convert to a String.
     * The format used is the same as the standard SQL format,
     * <i>YYYY-MM-DD HH:MM:SS.F</i>, or a truncated version of
     * that format when less precision has been specified.
     */
    public String toString() {
	if (precision==PRECISION_NONE)
	    return "";
	return dateFormat.format(date);
    }

    /** Get our value as a normalized date string without time. */
    public String toNormalizedDateString() {
        return getDateFormat(PRECISION_DAY).format(date);
    }

    /** Get our value as a localized string.
     * The precision is taken into account.
     */
    public String toLocalizedString() {
        return getLocalizedDateFormat(precision).format(date);
    }

    /** Create a DateSpec from a string.
     * Parses the same format of string as is created by
     * {@link #toString}.
     */
    public static DateSpec valueOf(String s) {
	if (s==null || s.trim().equals(""))
	    return null;	//no data
	return new DateSpec(s);
    }

    /** True if the argument is equal to our value. */
    public boolean equals(Object that) {
	if (that==this)
	    return true;
        if (that==null)
            return false;
	//Compare by formatting them, which takes precision into
	//account.
	//TBD - we should do this by looking at each precision and
	//comparing pieces of the date.  We might want two DateSpecs
	//to be equal even when they don't have the same precision.
	return (this.toString().equals(that.toString()));
    }

    //The Clonable interface
    public Object clone() {
	DateSpec ds = null;
	try {
	    ds = (DateSpec)super.clone();
	    ds.date = new Date(this.date.getTime());
	    ds.setPrecision(this.precision);
	} catch (CloneNotSupportedException ex) {}
	return ds;
    }
    //End Clonable interface

    //The Comparable interface
    public int compareTo(Object o) {
	int d = date.compareTo(o);
	if (d!=0)
	    return d;
	if (o instanceof DateSpec)
	    return precision - ((DateSpec)o).getPrecision();
	return 0;
    }
    //End Comparable interface

    //The Serializable interface
    private void writeObject(ObjectOutputStream s) throws IOException {
	s.writeLong(date.getTime());
	s.writeInt(precision);
    }

    private void readObject(ObjectInputStream s) 
	    throws IOException, ClassNotFoundException {
	date = new Date(s.readLong());
	setPrecision(s.readInt());
    }
    //End Serializable interface
}

/* end */
