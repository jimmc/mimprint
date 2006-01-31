/* MoreException.java
 *
 * Jim McBeath, October 6, 2001
 */

package net.jimmc.util;

/** A wrapper class to encapsulate an exception with additional information.
 */
public class MoreException extends RuntimeException {
    /** Create an exception to wrap another exception. */
    public MoreException(Throwable throwable) {
	super(throwable);
    }

    /** Create a wrapper exception with additional information. */
    public MoreException(Throwable throwable, String message) {
	super(combineMessage(message,throwable),throwable);
    }

    static String combineMessage(String message, Throwable throwable) {
	String tMsg = throwable.getMessage();
	if (tMsg==null || tMsg.trim().equals(""))
	    return message;
	else
	    return message+"\n"+tMsg;
    }
}

/* end */
