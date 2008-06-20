/* FileDialogs.scala
 *
 * Jim McBeath, June 10, 2008
 * extracted from StandardDialogs.scala Jun 20, 2008
 */

package net.jimmc.swing

import net.jimmc.util.BasicQueries
import net.jimmc.util.SResources
import net.jimmc.util.MoreException
import net.jimmc.util.UserException

import java.awt.Component
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import javax.swing.JFileChooser

/** A collection of standard dialog methods about files. */
trait FileDialogs { this: BasicQueries =>
    private val maxSimpleMessageLength = 80
    private val maxSimpleMessageLineCount = 30
    private val maxSimpleMessageLineWidth = 80

    /** Extending class must provide a method to get the parent component
     * for our dialogs. */
    protected def dialogParent:Component

    /** Extending class must provide resources for the dialogs. */
    val dialogRes:SResources

    /** True if we should allow debugging of UserException */
    val debugUserExceptions:Boolean

    /** Put up a file open dialog. */
    def fileOpenDialog(prompt:String):File = {
        fileOpenDialog(prompt,null.asInstanceOf[String]);
    }

    /** Put up a file open dialog.
     * @return null if cancelled.
     */
    def fileOpenDialog(prompt:String, dflt:String):File = {
        val chooser = new JFileChooser(dflt)
        chooser.setDialogTitle(prompt)
        val result = chooser.showOpenDialog(dialogParent)
        if (result!=JFileChooser.APPROVE_OPTION)
            null	//cancelled
        else
            chooser.getSelectedFile()
    }

    /** Put up a file open dialog.
     * @return null if cancelled.
     */
    def fileOpenDialog(prompt:String, dflt:File):File = {
        val chooser = new JFileChooser(dflt)
        chooser.setDialogTitle(prompt)
        val result = chooser.showOpenDialog(dialogParent)
        if (result!=JFileChooser.APPROVE_OPTION)
            return null	//cancelled
        else
            chooser.getSelectedFile()
    }

    def fileOrDirectoryOpenDialog(prompt:String, dflt:File):File = {
        fileOrDirectoryOpenDialog(prompt, dflt, null)
    }

    /** Put up a dialog to open a file or a directory.
     * @param prompt The title for the dialog.
     * @param dflt The intially default file or directory.
     * @param approveLabel The label to use on the Approve button.
     * @return The selected file or directory, or null if cancelled.
     */
    def fileOrDirectoryOpenDialog(prompt:String, dflt:File,
                            approveLabel:String):File = {
        val chooser = new JFileChooser()
        //If the specified directory does not exist, JFileChooser
        //defaults to a completely different directory.
        //In an attempt to be slightly more reasonable, we
        //look for the closest parent that does exist.
        var d = dflt
        while (d!=null && !d.exists()) {
            d = d.getParentFile()
        }
        chooser.setCurrentDirectory(d)
        chooser.setDialogType(JFileChooser.OPEN_DIALOG)
        chooser.setDialogTitle(prompt)
        chooser.setMultiSelectionEnabled(false)
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)

        val result = if (approveLabel!=null)
                chooser.showDialog(dialogParent, approveLabel) else
                chooser.showOpenDialog(dialogParent)
        if (result != JFileChooser.APPROVE_OPTION)
            null	//user cancelled
        else
            chooser.getSelectedFile()
    }

    /** Put up a file save dialog. */
    def fileSaveDialog(prompt:String):File = {
        fileSaveDialog(prompt,null.asInstanceOf[String]);
    }

    /** Put up a file save dialog.
     * @return null if cancelled
     */
    def fileSaveDialog(prompt:String, dflt:String):File = {
        val chooser = new JFileChooser(dflt)
        chooser.setDialogTitle(prompt)
        val result = chooser.showSaveDialog(dialogParent)
        if (result!=JFileChooser.APPROVE_OPTION)
            null	//cancelled
        else
            chooser.getSelectedFile()
    }

    /** Put up a file save dialog.
     * @return null if cancelled
     */
    def fileSaveDialog(prompt:String, dflt:File):File = {
        val chooser = new JFileChooser(dflt)
        chooser.setDialogTitle(prompt)
        val result = chooser.showSaveDialog(dialogParent)
        if (result!=JFileChooser.APPROVE_OPTION)
            return null	//cancelled
        else
            chooser.getSelectedFile();
    }

    /** Put up a directory save dialog.
     * @return null if cancelled
     */
    def directorySaveDialog(prompt:String, dflt:File):File = {
        val chooser = new JFileChooser(dflt)
        chooser.setDialogTitle(prompt)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        val result = chooser.showSaveDialog(dialogParent)
        if (result!=JFileChooser.APPROVE_OPTION)
            null	//cancelled
        else
            chooser.getSelectedFile()
    }

    /** Put up a dialog to select a directory.
     * @param defaultDir The directory intially displayed in the dialog.
     * @param title The title for the dialog.
     * @param approveLabel The label to use on the Approve button.
     * @return The selected directory, or null if cancelled.
     */
    def selectDirectory(defaultDir:String,title:String,
                            approveLabel:String):String = {
        val chooser = new JFileChooser()
        var d:File = if (defaultDir!=null) new File(defaultDir) else null
        //If the specified directory does not exist, JFileChooser
        //defaults to a completely different directory.
        //In an attempt to be slightly more reasonable, we
        //look for the closest parent that does exist.
        while (d!=null && !d.exists()) {
            d = d.getParentFile()
        }
        chooser.setCurrentDirectory(d)
        chooser.setDialogType(JFileChooser.OPEN_DIALOG)
        chooser.setDialogTitle(title)
        chooser.setMultiSelectionEnabled(false)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)

        if (chooser.showDialog(dialogParent, approveLabel)
                    != JFileChooser.APPROVE_OPTION)
            null	//user cancelled
        else
            chooser.getSelectedFile().toString()
    }

    /* TBD - add saveTextToFile(String text) that asks here for
     * the output file.
     */

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, ask the user for confirmation
     * before overwriting the file.
     * @return The opened PrintWriter, or null if the user
     *         declined to open the file.
     */
    def getPrintWriterFor(filename:String):PrintWriter =
        getPrintWriterFor(new File(filename))

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, ask the user for confirmation
     * before overwriting the file.
     * @return The opened PrintWriter, or null if the user
     *         declined to open the file.
     */
    def getPrintWriterFor(f:File):PrintWriter = {
        if (f.exists()) {
            val msg = dialogRes.getResourceFormatted(
                    "query.Confirm.FileExists",f.toString())
            if (!confirmDialog(msg))
                    return null 	//cancelled
        }
        try {
            val w = new PrintWriter(new FileWriter(f))
            w
        } catch {
            case ex:IOException =>
                //TBD - handle this exception?
                throw new MoreException(ex)
        }
    }

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
    def saveTextToFile(text:String, filename:String):Boolean = {
        val w = getPrintWriterFor(filename)
        if (w==null)
            false
        else {
            w.write(text)
            w.close()
            true
        }
    }
}
