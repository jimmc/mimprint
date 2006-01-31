/* JsTimestampField.java
 *
 * Jim McBeath, November 3, 2001
 */

package net.jimmc.swing;

import net.jimmc.util.ResourceSource;

import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/** A text editing field that accepts only Timestamp data.
 */
public class JsTimestampField extends JsTextField {
	/** Our resource source. */
	protected ResourceSource res;

	//The acceptable date/time formats
	String[] formats = {
		"MMM dd, yyyy HH:mm:ss",
		"MMM dd,yyyy HH:mm:ss",
		"MMM dd, yyyy HH:mm",
		"MMM dd,yyyy HH:mm"
	};
	String[] dFormats = {
		"MMM dd, yyyy",
		"MMM dd,yyyy"
	};

	/** Parsing format for timestamps. */
	protected SimpleDateFormat[] timestampFormats;
	protected SimpleDateFormat[] dateFormats;

	protected SimpleDateFormat timeOnlyFormat1;
	protected SimpleDateFormat timeOnlyFormat2;

	/** Create a Timestamp field with a resource source. */
	public JsTimestampField(ResourceSource rsrc, int width) {
		super(width);
		if (rsrc==null)
			throw new IllegalArgumentException("No ResourceSource");
		this.res = rsrc;
		initFormats();
	}

	/** Create a Timestamp field. */
   /*
	public JsTimestampField(int width) {
		super(width);
		initFormats();
	}
    */

	protected void initFormats() {
		//TBD - use resources to set formats
		timestampFormats = new SimpleDateFormat[formats.length];
		for (int i=0; i<formats.length; i++) {
			timestampFormats[i] = new SimpleDateFormat(formats[i]);
			timestampFormats[i].setLenient(true);
		}

		dateFormats = new SimpleDateFormat[dFormats.length];
		for (int i=0; i<dFormats.length; i++) {
			dateFormats[i] = new SimpleDateFormat(dFormats[i]);
			dateFormats[i].setLenient(true);
		}

		timeOnlyFormat1 = new SimpleDateFormat("HH:mm:ss");
		timeOnlyFormat2 = new SimpleDateFormat("HH:mm");
	}

    //The EditField interface
	/** Set our value. */
	public void setValue(Object value) {
		if (value==null) {
			setText("");
			return;
		}
		if (value instanceof Timestamp)
			setText(timestampFormats[0].format((Timestamp)value));
		else if (value instanceof Date)
			setText(dateFormats[0].format((Date)value));
		else
			setText(value.toString());
	}

	/** Get our value. */
	public Object getValue() {
		ParsePosition pos;
		Object v;
		String msg;
		String s = getText();
		if (s==null)
			return null;
		s = s.trim();
		if (s.equals(""))
			return null;	//no value specified

		//Try parsing it as a full timestamp
		for (int i=0; i<timestampFormats.length; i++) {
			pos = new ParsePosition(0);
			v = timestampFormats[i].parse(s,pos);
			if (pos.getIndex()>=s.length()) {
			    if (v instanceof Date) {
				return new Timestamp(((Date)v).getTime());
					//convert from Date to Timestamp
			    }
			    if (v!=null)
				return v;
			}
		}

		//Try parsing it as a date only
		for (int i=0; i<dateFormats.length; i++) {
			pos = new ParsePosition(0);
			v = dateFormats[i].parse(s,pos);
			if (pos.getIndex()>=s.length() && v!=null) {
				return v;
			}
		}

		//We have a non-null string, but didn't parse out
		//a timestamp or date.  See if the user specified only
		//a time without a date, and give an error message
		pos = new ParsePosition(0);
		v = timeOnlyFormat1.parse(s,pos);
		if (v==null) {
			pos = new ParsePosition(0);
			v = timeOnlyFormat2.parse(s,pos);
		}
		if (v!=null && res!=null) {
			msg = res.getResourceFormatted(
				"error.DateTimeFormatRequiresDate",s);
			throw new NumberFormatException(msg);
		}
		
		//Completely unrecognized, throw an exception
		if (res!=null) {
			Object[] args = { s,
				timestampFormats[0].toLocalizedPattern() };
			msg = res.getResourceFormatted(
				"error.BadDateTimeFormat",args);
		} else {
			msg = s;
		}
		throw new NumberFormatException(msg);
	}
    //End EditField interface
}

/* end */
