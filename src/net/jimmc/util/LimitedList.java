/* LimitedList.java
 *
 * Jim McBeath, October 20, 2001
 */

package net.jimmc.util;

import java.util.ArrayList;

/** A list with a limited size.
 * When an item is appended to the list past the limit size,
 * an item from the start of the list is removed to maintain the list size.
 * <b>NOTE:</b> Only the {@link #addLimited(Object)} method currently
 * implements the limit checking.
 */
public class LimitedList extends ArrayList {
    /** The current maximum size of the list. */
    protected int limit;

    /** Create a LimitedList.
     * @param initialLimit The initial capacity and limit.
     */
    public LimitedList(int initialLimit) {
	super(initialLimit);
	setLimit(initialLimit);
    }

    /** Set the maximum size of the list. */
    public void setLimit(int limit) {
	if (limit<this.limit) {
	    //The limit is being lowered, so we need to remove
	    //some items.  Delete items from the start of the
	    //list to bring it to size.
	    int n = this.limit - limit; //number of items to remove
	    removeRange(0,n);
	}
	this.limit = limit;
    }

    /** Get the current maximum size of the list. */
    public int getLimit() {
	return limit;
    }

    /** Add an element to the list. */
    public void addLimited(Object obj) {
	if (size()>=limit) {
	    //Too many objects, remove enough to get to limit-1
	    int n = size()-limit+1;
	    removeRange(0,n);
	}
	add(obj);
    }

    /** Remove all items from the specified position, inclusive,
     * to the end of the list.
     */
    public void truncate(int position) {
	removeRange(position,size());
    }
}

/* end */
