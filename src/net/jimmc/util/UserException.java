/* UserException.java
 *
 * Jim McBeath, October 24, 2001
 */

package net.jimmc.util;

/** An exception whose message is intended to be displayed to the user.
 * This is typically to report an error having to do with data that the
 * user is editing.
 */
public class UserException extends RuntimeException {
    /** Create a UserException. */
    public UserException(String message) {
	super(message);
    }
}

/* end */
