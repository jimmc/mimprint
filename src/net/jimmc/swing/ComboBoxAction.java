/* ComboBoxAction.java
 *
 * Jim McBeath, June 9, 1998
 */

package net.jimmc.swing;

import net.jimmc.util.ResourceSource;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** An adapter class to simplify defining ComboBoxes with actions.
 */
public class ComboBoxAction extends JComboBox
			implements ActionListener, EditField, Cloneable {
	/** Our resource source for error messages. */
	protected ResourceSource res;

	/** Our values, if different from the displayed value. */
	protected Object[] values;

	/** Create a button. */
	public ComboBoxAction(ResourceSource res) {
		super();
		this.res = res;
		addActionListener(this);
	}

	/** Create a button with no resource source. */
	public ComboBoxAction() {
		this(null);
	}

	/** Clone us. */
	public EditField cloneEditField() {
		try {
			return (EditField)clone();
		} catch (CloneNotSupportedException ex) {
			return null;	//won't happen
		}
	}

	/** Set all our choices at once.
	 * This is faster than using addItem for each one.
	 */
	public void setItems(Object[] displays) {
		values = null;	//no values separate from displayed data
		setModel(new DefaultComboBoxModel(displays));
	}

	/** Set values and separate displayed data.
	 * @param values The values to be set with {@link #setValue} and
	 *        returned by {@link #getValue}.
	 * @param displays The data to be displayed on the screen.
	 */
	public void setChoices(Object[] values, Object[] displays) {
		if (values!=null && (values.length!=displays.length)) {
			Object[] args = { new Integer(values.length),
					  new Integer(displays.length) };
			String msg;
			if (res==null)
				msg = "Choice length mismatch";
			else
				msg = res.getResourceFormatted(
					"error.ChoiceLengthMismatch",args);
			throw new RuntimeException(msg);
		}
		setItems(displays);
		this.values = values;
	}

	/** Get the currently selected and displayed string. */
	public String getSelectedString() {
		return (String)getSelectedItem();
	}

	/** Set the current value.
	 * If {@link #setChoices} has been called, the value here must be
	 * one of the values from the values list passed to setChoices.
	 * If {@link #setItems} has been called, or neither has been called,
	 * the value here must be one of the displayed values.
	 */
	public void setValue(Object value) {
		if (value==null) {
			setSelectedIndex(-1);	//nothing selected
			return;
		}
		if (values!=null) {
			//We have a values list, look in it
			for (int i=0; i<values.length; i++) {
				if (values[i].equals(value)) {
					setSelectedIndex(i);
					return;
				}
			}
			//Not found in our list of values, try direct
		}
		//No values list, set it directly
		setSelectedItem(value);
	}

	/** If there is only one non-null and non-blank value in our choicelist,
	 * select it.
	 * If there is more than one non-null/non-blank value, do nothing.
	 */
	public void autoSelectSingle() {
		if (values==null || values.length==0)
			return;
		int nonNullIndex = -1;
		for (int i=0; i<values.length; i++) {
			if (values[i]!=null && !values[i].equals("")) {
				//Found a non-null/non-blank value
				if (nonNullIndex>=0)
					return;	//more than one, skip it
				nonNullIndex = i;
			}
		}
		if (nonNullIndex>=0)
			setSelectedIndex(nonNullIndex);
	}

	/** Get the currently selected value.
	 * If {@link #setChoices} has been called, returns one of the values
	 * rather than the displayed data.
	 * If {@link #setItems} has been called, or neither has been called,
	 * returns the displayed data.
	 */
	public Object getValue() {
		int i = getSelectedIndex();
		if (i<0)
			return null;	//nothing selected
		if (values!=null) {
			return values[i];
		}
		return getSelectedItem();
	}

	/** Get the actual value for a specified display value. */
	public Object getValueForDisplay(Object display) {
		if (values==null)
			return display;	//if no values, display is the value
		for (int n=getItemCount()-1; n>=0; n--) {
			Object d = getItemAt(n);
			if (d==null) {
				if (display==null)
					return values[n];
			} else {
				if (d.equals(display))
					return values[n];
			}
		}
		return null;	//can't find a match for it
	}

	/** Get the display value for a specified actual value. */
	public Object getDisplayForValue(Object value) {
		if (values==null)
			return value;	//if no values, display is the value
		for (int n=values.length-1; n>=0; n--) {
			Object v = values[n];
			if (v==null) {
				if (value==null)
					return getItemAt(n);
			} else {
				if (v.equals(value))
					return getItemAt(n);
			}
		}
		return null;	//can't find a match for it
	}

	/** Here on an action event. */
	public void actionPerformed(ActionEvent ev) {
		action();
	}

	/** The default action for this button.
	 * The subclass usually overrides this method.
	 */
	public void action() {
		/* do nothing */
	}
}

/* end */
