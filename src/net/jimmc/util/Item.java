/* Item.java
 *
 * Jim McBeath, October 10, 2001
 */

package net.jimmc.util;

/** A named value. */
public class Item {
    /** Our name. */
    protected String name;

    /** Our value. */
    protected Object value;

    /** Create an Item. */
    public Item(String name, Object value) {
	this.name = name;
	this.value = value;
    }

    /** Set our name. */
    public void setName(String name) {
	this.name = name;
    }

    /** Get our name. */
    public String getName() {
	return name;
    }

    /** Set our value. */
    public void setValue(Object value) {
	this.value = value;
    }

    /** Get our value. */
    public Object getValue() {
	return value;
    }

    /** Generate "name=value" string. */
    public String toString() {
	return name+"="+value;
    }
}

/* end */
