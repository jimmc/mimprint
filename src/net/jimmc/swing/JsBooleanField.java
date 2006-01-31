/* JsBooleanField.java
 *
 * Jim McBeath, August 14, 2002
 */

package net.jimmc.swing;

/** An editing field for boolean values with true/false/null choices
 */
public class JsBooleanField extends ComboBoxAction {
	/** Create a boolean field. */
	public JsBooleanField() {
		super();
		Object[] values= { "", new Boolean(false), new Boolean(true)};
		String[] displays = { "", "false", "true" };
			//TBD - i18n
		setChoices(values,displays);
	}

    //The EditField interface
	public Object getValue() {
		Object v = super.getValue();
		if (v==null || v.equals(""))
			return null;
		return v;
	}
	//ComboBoxAction implements a setValue which should work
    //End EditField interface
}

/* end */
