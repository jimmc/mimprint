/* Items.java
 *
 * Jim McBeath, October 10, 2001
 */

package net.jimmc.util;

import java.util.Vector;

/** An order list of named items. */
public class Items {
    /** Our list of Item objects. */
    protected Vector items;

    /** Create an empty Items list. */
    public Items() {
	items = new Vector();
    }

    /** Add an item to our list.
     * @param item The item to add to our list.  We just put a pointer
     *        to this item into our list, we do not make a copy.
     */
    public void addItem(Item item) {
	items.addElement(item);
    }

    /** Add an item to our list.
     * @param name The name of the item.
     * @param value The value of the item.  We put a pointer to this
     *        value in the Item, but do not make a copy of the value.
     */
    public void addItem(String name, Object value) {
	addItem(new Item(name,value));
    }

    /** Remove an item from the list.
     * @param name The name of the item to remove.
     * @return The removed item, or null if not found.
     */
    public Item removeItem(String name) {
	synchronized (items) {
	    int n = getItemIndex(name);
	    if (n<0)
		return null;
	    Item item = getItem(n);
	    items.removeElementAt(n);
	    return item;
	}
    }

    /** Get the number of items in our list. */
    public int size() {
	return items.size();
    }

    /** Set the values of all items by name that we find in the
     * given items.
     * @param items The items to add to our set.  May be null.
     */
    public void setValues(Items items) {
	if (items==null)
	    return;
	for (int i=0; i<items.size(); i++) {
	    Item item = items.getItem(i);
	    setValue(item.getName(),item.getValue());
	}
    }

    /** Set the value of the Nth item. */
    public void setValue(int n, Object value) {
	getItem(n).setValue(value);
    }

    /** Set the value of the named item, or add it if not found. */
    public void setValue(String name, Object value) {
	Item item = getItem(name);
	if (item==null)
	    addItem(name,value);
	else
	    item.setValue(value);
    }

    /** Get the Nth item. */
    public Item getItem(int n) {
	return (Item)items.elementAt(n);
    }

    /** Get the named item, or null of no item of that name. */
    public Item getItem(String name) {
	int n = size();
	for (int i=0; i<n; i++) {
	    Item item = getItem(i);
	    if (item.getName().equals(name))
		return item;
	}
	return null;		//not found
    }

    /** Get the index of the named item.
     * @return The index of the item, or -1 if not found.
     */
    public int getItemIndex(String name) {
	int n = size();
	for (int i=0; i<n; i++) {
	    Item item = getItem(i);
	    if (item.getName().equals(name))
		return i;
	}
	return -1;		//not found
    }

    /** Get all names. */
    public String[] getNames() {
	String[] names = new String[size()];
	for (int i=0; i<size(); i++)
	    names[i] = getName(i);
	return names;
    }

    /** Get the name of the Nth item. */
    public String getName(int n) {
	return getItem(n).getName();
    }

    /** Get all values. */
    public Object[] getValues() {
	Object[] vals = new Object[size()];
	for (int i=0; i<size(); i++)
	    vals[i] = getValue(i);
	return vals;
    }

    /** Get the value of the Nth item. */
    public Object getValue(int n) {
	return getItem(n).getValue();
    }

    /** Get the value of the named item, or null if not found. */
    public Object getValue(String name) {
	Item item = getItem(name);
	if (item==null)
	    return null;
	return item.getValue();
    }

    /** Generate a string such as {name1=value1,name2=value2}
     */
    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append("{");
	toStringNoBraces(sb);
	sb.append("}");
	return sb.toString();
    }

    /** Generate a string like toString, but without the enclosing braces.
     */
    public String toStringNoBraces() {
	StringBuffer sb = new StringBuffer();
	toStringNoBraces(sb);
	return sb.toString();
    }

    /** Convert to a string without the enclosing braces. */
    public void toStringNoBraces(StringBuffer sb) {
	toString(sb,",");
    }

    /** Convert to a string using the specified separator.
     * No braces.
     */
    public void toString(StringBuffer sb, String sep) {
	for (int i=0; i<items.size(); i++) {
	    if (i>0)
		sb.append(sep);
	    sb.append(getItem(i));
	}
    }

    /** Parse a string such as produced by {@link #toString}
     * into an Items object.
     */
    public static Items parseItems(String s) {
	Items items = new Items();
	if (s==null)
	    return items;	//no items in a null string
	if (s.startsWith("{") && s.endsWith("}"))
	    s = s.substring(1,s.length()-1);
	String[] nva = StringUtil.toArray(s,',');
	for (int n=0; n<nva.length; n++) {
	    String nv = nva[n];	//one name=value string
	    int eq = nv.indexOf('=');
	    if (eq<0) {
		items.addItem(nv,"");	//blank value
	    } else {
		String name = nv.substring(0,eq).trim();
		String value = nv.substring(eq+1).trim();
		items.addItem(name,value);
	    }
	}
	return items;
    }
}

/* end */
