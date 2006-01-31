/* JsDurationField.java
 *
 * Jim McBeath, November 4, 2001
 */

package net.jimmc.swing;

import net.jimmc.util.Duration;
import net.jimmc.util.StringUtil;

/** A text editing field that maintains an int which is a time duration
 * in seconds, displaying and parsing the value as HH:MM:SS.
 */
public class JsDurationField extends JsTextField {
	/** True if we were set with an Integer value. */
	boolean valueIsInt;

	/** Create a duration field. */
	public JsDurationField(int width) {
		super(width);
	}

    //The EditField interface
	/** Set our value. */
	public void setValue(Object iSeconds) {
		if (iSeconds==null) {
			setText("");	//no value
			return;
		}
		Duration dur;
		if (iSeconds instanceof Integer) {
			valueIsInt = true;
			int seconds = ((Integer)iSeconds).intValue();
			dur = new Duration(seconds);
		} else /*if (iSeconds instanceof Number)*/ {
			valueIsInt = false;
			double seconds = ((Number)iSeconds).doubleValue();
			dur = new Duration(seconds);
		}
		setText(dur.toString());
	}

	/** Get our value.
	 * @return The integer number of seconds in the duration,
	 *         or null if no value is specified.
	 */
	public Object getValue() {
		String s = getText();
		if (s==null || s.equals(""))
			return null;	//no value specified
		if (valueIsInt) {
			int seconds = Duration.parseIntDuration(s);
			return new Integer(seconds);
		} else {
			double seconds = Duration.parseDoubleDuration(s);
			return new Double(seconds);
		}
	}
    //End EditField interface
}

/* end */
