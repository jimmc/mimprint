/* BasicQueries.scala
 *
 * Jim McBeath, June 13, 2008
 */

package net.jimmc.util

/** A set of query methods to be implemented by an application.
 */
trait BasicQueries {
    /* Ask an OK/Cancel question.
     * @param prompt The prompt string to display.
     * @return True if the user selects OK,
     *         false if the users selectes Cancel.
     */
    def confirmDialog(prompt:String):Boolean

    /** Display an error message and wait for the user to continue. */
    def errorDialog(errmsg:String)

    /** Display an info message and wait for the user to continue. */
    def infoDialog(infomsg:String)

    /** Put up a string dialog and block for input. */
    def stringDialog(prompt:String):String

    /** Put up a three-button Yes/No/Cancel dialog.
     * @return The index of the selected button:
     *         0 for yes, 1 for no, 2 for cancel.
     */
    def yncDialog(prompt:String, yes:String, no:String, cancel:String):Int

    /** Put up a dialog with multiple buttons.
     * @param prompt The prompt string.
     * @param title The title string.
     * @param labels The labels to use on the buttons.
     * @return The index number of the selected button.
     */
    def multiButtonDialog(prompt:String, title:String,
            labels:Array[String]):Int

    /** A dialog to display an exception. */
    def exceptionDialog(ex:Throwable)

    /** A dialog to display the traceback on an exception. */
    def exceptionDetailsDialog(ex:Throwable)
}
