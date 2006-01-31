/* JsIntegerField.java
 *
 * Jim McBeath, November 3, 2001
 */

package net.jimmc.swing;

import net.jimmc.util.StringUtil;

import java.util.Vector;

/** A text editing field that accepts only integer data.
 */
public class JsIntegerField extends JsTextField {
	//TBD - modify keymap stuff so only valid strings can be entered

	/** True if we can accept multiple integers, separated by commas,
	 * or ranges separated by dashes. */
	protected boolean multi;

	/** Create an integer field. */
	public JsIntegerField(int width) {
		super(width);
	}

	/** Set to true to allow multiple integers to be entered
	 * and displayed. */
	public void setMulti(boolean allowed) {
		multi = allowed;
	}

	/** True if we allow multiple integers. */
	public boolean isMulti() {
		return multi;
	}

    //The EditField interface
    	public void setValue(Object v) {
		if (v==null) {
			setText("");
			return;
		}
		if (v instanceof Integer) {
			setText(v.toString());
			return;
		}
		if (v instanceof Integer[]) {
			Integer[] ii = (Integer[])v;
			String s = StringUtil.toString(ii,',');
			setText(s);
			return;
		}
		setText(v.toString());	//who knows what this is?
	}

	/** Get our value.
	 * @return null if blank, and Integer if one value, or
	 *         an array of Integers if more than one number specified.
	 */
	public Object getValue() {
		String s = getText();
		if (s==null || s.equals(""))
			return null;	//no value specified
		if (!multi)
			return Integer.valueOf(s);
		String[] commaGroups = StringUtil.toArray(s,',',true);
		Vector v = new Vector();
		for (int i=0; i<commaGroups.length; i++) {
			String sn = commaGroups[i].trim();
			if (sn.length()==0)
				continue;
			//TBD - look for range syntax (a-b)
			v.addElement(Integer.valueOf(sn));
		}
		int vSize = v.size();
		if (vSize==0)
			return null;
		if (vSize==1)
			return v.elementAt(0);
		Integer[] nn = new Integer[vSize];
		v.copyInto(nn);
		return nn;
	}
    //End EditField interface

	/** Set the value of our field as an int. */
	public void setIntValue(int v) {
		setValue(new Integer(v));
	}

	/** Get the value of our field as an int. */
	public int getIntValue(int dflt) {
		Integer ii = (Integer)getValue();
		if (ii==null)
			return dflt;
		return ii.intValue();
	}
}

/* end */
