/* JsFloatField.java
 *
 * Jim McBeath, August 8, 2003
 */

package net.jimmc.swing;

import net.jimmc.util.StringUtil;

import java.util.Vector;

/** A text editing field that accepts only floating point data.
 */
public class JsFloatField extends JsTextField {
	//TBD - modify keymap stuff so only valid strings can be entered

	/** True if we can accept multiple numbers, separated by commas,
	 * or ranges separated by dashes. */
	protected boolean multi;

	/** Create a float field. */
	public JsFloatField(int width) {
		super(width);
	}

	/** Set to true to allow multiple numbers to be entered
	 * and displayed. */
	public void setMulti(boolean allowed) {
		multi = allowed;
	}

	/** True if we allow multiple numbers. */
	public boolean isMulti() {
		return multi;
	}

    //The EditField interface
    	public void setValue(Object v) {
		if (v==null) {
			setText("");
			return;
		}
		if (v instanceof Number) {
			setText(v.toString());
			return;
		}
		if (v instanceof Number[]) {
			Number[] ii = (Number[])v;
			String s = StringUtil.toString(ii,',');
			setText(s);
			return;
		}
		setText(v.toString());	//who knows what this is?
	}

	/** Get our value.
	 * @return null if blank, and Float if one value, or
	 *         an array of Floats if more than one number specified.
	 */
	public Object getValue() {
		String s = getText();
		if (s==null || s.equals(""))
			return null;	//no value specified
		if (!multi)
			return Double.valueOf(s);
		String[] commaGroups = StringUtil.toArray(s,',',true);
		Vector v = new Vector();
		for (int i=0; i<commaGroups.length; i++) {
			String sn = commaGroups[i].trim();
			if (sn.length()==0)
				continue;
			//TBD - look for range syntax (a-b)
			v.addElement(Double.valueOf(sn));
		}
		int vSize = v.size();
		if (vSize==0)
			return null;
		if (vSize==1)
			return v.elementAt(0);
		Double[] nn = new Double[vSize];
		v.copyInto(nn);
		return nn;
	}
    //End EditField interface

	/** Set the value of our field as a double. */
	public void setDoubleValue(double v) {
		setValue(new Double(v));
	}

	/** Get the value of our field as a double. */
	public double getDoubleValue(double dflt) {
		Double dd = (Double)getValue();
		if (dd==null)
			return dflt;
		return dd.doubleValue();
	}
}

/* end */
