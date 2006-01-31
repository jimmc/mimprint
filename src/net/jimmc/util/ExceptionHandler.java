/* ExceptionHandler.java
 *
 * Jim McBeath, October 24, 2001
 */

package net.jimmc.util;

/** An interface to handle exceptions.
 */
public interface ExceptionHandler {
    /** Called before an action which may generate an exception.
     * @param source The source of the action.
     */
    public void beforeAction(Object source);

    /** Called after an action when no exception was thrown.
     * @param source The source of the action.
     */
    public void afterAction(Object source);

    /** Deal with an exception.
     * @param exception The exception or other throwable to handle.
     */
    public void handleException(Object source, Throwable exception);
}

/* end */
