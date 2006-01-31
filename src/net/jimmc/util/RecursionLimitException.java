/* RecursionLimitException.java
 *
 * Jim McBeath, June 11, 2005
 */

package net.jimmc.util;

/** An exception when a recursion limit is exceeded.
 */
public class RecursionLimitException extends RuntimeException {
    private String ourMessage;

    /** Create a RecursionLimitException. */
    public RecursionLimitException(String message) {
	super(message);
    }

    public void setMessage(String msg) {
        this.ourMessage = msg;
    }

    public String getMessage() {
        if (ourMessage!=null)
            return ourMessage;
        else
            return super.getMessage();
    }
}

/* end */
