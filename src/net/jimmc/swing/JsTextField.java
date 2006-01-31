/* JsTextField.java
 *
 * Jim McBeath, June 7, 1998
 */

package net.jimmc.swing;

import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** JsTextField is an adapter class that simplifies attaching actions to
 * text fields.
 */
public class JsTextField extends JTextField
		implements ActionListener, EditField, Cloneable {
	/** Create a text field with the specified size. */
	public JsTextField(int width) {
		super(width);
		addActionListener(this);
	}

	/** Clone us. */
	public EditField cloneEditField() {
		try {
			return (EditField)clone();
		} catch (CloneNotSupportedException ex) {
			return null;	//won't happen
		}
	}

	/** Process the action. */
	public void actionPerformed(ActionEvent ev) {
		action();
	}

	/** The action.
	 * The subclass should override.
	 */
	public void action() {
		/* do nothing */
	}

    //The EditField interface
    	/** Set our value. */
	public void setValue(Object value) {
		if (value==null)
			value = "";
		setText(value.toString());
	}

	/** Get our value. */
	public Object getValue() {
		return getText();
	}
    //End EditField interface
}

/* end */
