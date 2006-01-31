/* JsSpinner.java
 *
 * Jim McBeath, October 28, 2005
 */

package net.jimmc.swing;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.SpinnerModel;

/** JsSpinner is an adapter class that simplifies attaching actions to
 * spinner fields.
 */
public class JsSpinner extends JSpinner
		implements ChangeListener, EditField, Cloneable {
	/** Create a spinner with a default model. */
	public JsSpinner() {
		super();
		addChangeListener(this);
	}

	/** Create a spinner with the specified model. */
	public JsSpinner(SpinnerModel model) {
		super(model);
		addChangeListener(this);
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
	public void stateChanged(ChangeEvent ev) {
		action();
	}

	/** The action.
	 * The subclass should override.
	 */
	public void action() {
		/* do nothing */
	}

    //The EditField interface is directly implemented by JSpinner
}

/* end */
