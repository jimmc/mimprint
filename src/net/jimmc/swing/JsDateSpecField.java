/* JsDateSpecField.java
 *
 * Jim McBeath, April 15, 2002
 */

package net.jimmc.swing;

import net.jimmc.util.DateSpec;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/** A text editing field that accepts only DateSpec data.
 */
public class JsDateSpecField extends JsTextField {
	/** Our parsing formats for dates. */
	static SimpleDateFormat[] dateFormats;

	/** Alternate formats for input parsing */
	static FormatAndPrecision[] altFormats;

	/** Create a DateSpec field. */
	public JsDateSpecField(int width) {
		super(width);
		if (dateFormats==null)
			initDateFormats();
	}

	/** Initialize our date formats. */
	protected void initDateFormats() {
		dateFormats =
			new SimpleDateFormat[DateSpec.PRECISION_FRACTION+1];
		//One-time initialization of our formats
		//TBD - allow resources to override these formats
		dateFormats[DateSpec.PRECISION_YEAR] =
			new SimpleDateFormat("yyyy");
		dateFormats[DateSpec.PRECISION_MONTH] =
			new SimpleDateFormat("MMM, yyyy");
		dateFormats[DateSpec.PRECISION_DAY] =
			new SimpleDateFormat("MMM dd, yyyy");
		dateFormats[DateSpec.PRECISION_HOUR] =
			new SimpleDateFormat("MMM dd, yyyy HH");
		dateFormats[DateSpec.PRECISION_MINUTE] =
			new SimpleDateFormat("MMM dd, yyyy HH:mm");
		dateFormats[DateSpec.PRECISION_SECOND] =
			new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
		dateFormats[DateSpec.PRECISION_FRACTION] =
			new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.S");
		for (int i=DateSpec.PRECISION_YEAR;
		     i<=DateSpec.PRECISION_FRACTION;
		     i++) {
		     	dateFormats[i].setLenient(true);
		}

		//Set up the alternate formats
		altFormats = new FormatAndPrecision[3];
		for (int a=0; a<altFormats.length; a++)
			altFormats[a] = new FormatAndPrecision();

		altFormats[0].format =
			new SimpleDateFormat("MMM yyyy");
		altFormats[0].format.setLenient(true);
		altFormats[0].precision = 2;

		altFormats[1].format =
			new SimpleDateFormat("MMM dd yyyy");
		altFormats[1].precision = 3;
		altFormats[1].format.setLenient(true);

		//Add a format for no space between comma and year
		altFormats[2].format =
			new SimpleDateFormat("MMM dd,yyyy");
		altFormats[2].precision = 3;
		altFormats[2].format.setLenient(true);

		//When adding new alternate formats, remember to change
		//the size of the allocated array
	}

    //The EditField interface
	/** Set our value. */
	public void setValue(Object value) {
		if (value==null) {
			setText("");
			return;
		}
		if (value instanceof String)
			value = new DateSpec((String)value);
		if (value instanceof DateSpec) {
			DateSpec d = (DateSpec)value;
			int p = d.getPrecision();
			if (p==DateSpec.PRECISION_NONE)
				setText("");
			else
				setText(dateFormats[p].format(d.getDate()));
		}
		else
			setText(value.toString());
	}

	/** Get our value. */
	public Object getValue() {
		String s = getText();
		if (s==null || s.equals(""))
			return null;

		s = s.trim();
		int sLen = s.length();
		ParsePosition pos = new ParsePosition(0);

		//Start with our standard formats
		for (int p=DateSpec.PRECISION_FRACTION;
		     p>=DateSpec.PRECISION_YEAR;
		     p--) {
			SimpleDateFormat dateFmt = dateFormats[p];
			pos.setIndex(0);
			Date d = (Date)dateFmt.parseObject(s,pos);
			int x = pos.getIndex();
			if (x>=sLen && d!=null)
				return new DateSpec(d,p);	//got a parse
		}

		//The standard formats failed, try a few alternate formats
		for (int a=0; a<altFormats.length; a++) {
			SimpleDateFormat dateFmt = altFormats[a].format;
			pos.setIndex(0);
			Date d = (Date)dateFmt.parseObject(s,pos);
			int x = pos.getIndex();
			if (x>=sLen && d!=null) {
				int p = altFormats[a].precision;
				return new DateSpec(d,p);
			}
		}

		//Unable to parse any format
		//TBD - use resources
		String msg = "Can't parse date \""+s+"\"\n"+
			"Use this date/time format: "+
			"MMM DD, YYYY HH:MM:SS.FFF";
		throw new IllegalArgumentException(msg);
	}
    //End EditField interface

	class FormatAndPrecision {
		SimpleDateFormat format;
		int precision;
	}
}

/* end */
