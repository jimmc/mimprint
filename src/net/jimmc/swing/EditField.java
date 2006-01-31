/* EditField.java
 *
 * Jim McBeath, October 8, 2001
 */

package net.jimmc.swing;

/** A generic value editor. */
public interface EditField {
	/** Set our value. */
	public void setValue(Object value);

	/** Get our value. */
	public Object getValue();

	/** Clone us. */
	public EditField cloneEditField();
}

/* end */
