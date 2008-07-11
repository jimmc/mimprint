/* FileQueries.scala
 *
 * Jim McBeath, July 11, 2008
 * extracted from FileDialogs.scala
 */

package net.jimmc.util

import java.io.File
import java.io.PrintWriter

/** A collection of standard dialog methods about files. */
trait FileQueries {

    /** Put up a file open dialog. */
    def fileOpenDialog(prompt:String):Option[File]

    //I wanted to use dflt:Option[String], but scalac complained that I
    //then had two methods with the same signature after type erasure,
    //so I have changed this one back to a String which can be null.
    /** Put up a file open dialog.
     * @return None if cancelled.
     */
    def fileOpenDialog(prompt:String, dflt:String):Option[File]

    /** Put up a file open dialog.
     * @return None if cancelled.
     */
    def fileOpenDialog(prompt:String, dflt:Option[File]):Option[File]

    def fileOrDirectoryOpenDialog(prompt:String, dflt:Option[File]):Option[File]

    /** Put up a dialog to open a file or a directory.
     * @param prompt The title for the dialog.
     * @param dflt The intially default file or directory.
     * @param approveLabel The label to use on the Approve button.
     * @return The selected file or directory, or None if cancelled.
     */
    def fileOrDirectoryOpenDialog(prompt:String, dflt:Option[File],
                            approveLabel:Option[String]):Option[File]

    /** Put up a file save dialog. */
    def fileSaveDialog(prompt:String):Option[File]

    //See fileOpenDialog for comment about dflt:String
    /** Put up a file save dialog.
     * @return None if cancelled
     */
    def fileSaveDialog(prompt:String, dflt:String):Option[File]

    /** Put up a file save dialog.
     * @return None if cancelled
     */
    def fileSaveDialog(prompt:String, dflt:Option[File]):Option[File]

    /** Put up a directory save dialog.
     * @return None if cancelled
     */
    def directorySaveDialog(prompt:String, dflt:Option[File]):Option[File]

    /** Put up a dialog to select a directory.
     * @param defaultDir The directory intially displayed in the dialog.
     * @param title The title for the dialog.
     * @param approveLabel The label to use on the Approve button.
     * @return The selected directory, or None if cancelled.
     */
    def selectDirectory(defaultDir:Option[String],title:String,
                            approveLabel:Option[String]):Option[String]

    /* TBD - add saveTextToFile(String text) that asks here for
     * the output file.
     */

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, ask the user for confirmation
     * before overwriting the file.
     * @return The opened PrintWriter, or None if the user
     *         declined to open the file.
     */
    def getPrintWriterFor(filename:String):Option[PrintWriter]

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, ask the user for confirmation
     * before overwriting the file.
     * @return The opened PrintWriter, or None if the user
     *         declined to open the file.
     */
    def getPrintWriterFor(f:File):Option[PrintWriter]

    /** Save the specified text string to the specified file.
     * If the file already exists, asks the user for confirmation
     * before overwriting it.
     * @param text The text to write to the file.
     * @param filename The output file.
     * @return True if the data was written to the file,
     *         false if the file already existed and the user
     *         declined to overwrite it (i.e. cancelled),
     *         or on error.
     */
    def saveTextToFile(text:String, filename:String):Boolean
}
