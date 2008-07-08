/* FileDialogs.scala
 *
 * Jim McBeath, June 10, 2008
 * extracted from StandardDialogs.scala Jun 20, 2008
 */

package net.jimmc.swing

import net.jimmc.util.BasicQueries
import net.jimmc.util.SomeOrNone
import net.jimmc.util.SomeOrNone.optionNotNull
import net.jimmc.util.SResources
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

    //def toOption(f:File):Option[File] = if (f==null) None else Some(f)

    //def toOption(s:String):Option[String] = if (s==null) None else Some(s)

    private def decodeChooserResult(result:Int, chooser:JFileChooser):
            Option[File] = {
        if (result!=JFileChooser.APPROVE_OPTION)
            None	//canceled
        else
            SomeOrNone(chooser.getSelectedFile())
    }

    /** Put up a file open dialog. */
    def fileOpenDialog(prompt:String):Option[File] = {
        fileOpenDialog(prompt,None.asInstanceOf[Option[File]])
    }

    //I wanted to use dflt:Option[String], but scalac complained that I
    //then had two methods with the same signature after type erasure,
    //so I have changed this one back to a String which can be null.
    /** Put up a file open dialog.
     * @return None if cancelled.
     */
    def fileOpenDialog(prompt:String, dflt:String):Option[File] = {
        fileOpenDialog(prompt, SomeOrNone(dflt).map(new File(_)))
    }

    /** Put up a file open dialog.
     * @return None if cancelled.
     */
    def fileOpenDialog(prompt:String, dflt:Option[File]):Option[File] = {
        val chooser = new JFileChooser(dflt getOrNull)
        chooser.setDialogTitle(prompt)
        val result = chooser.showOpenDialog(dialogParent)
        decodeChooserResult(result,chooser)
    }

    def fileOrDirectoryOpenDialog(prompt:String, dflt:Option[File]):
            Option[File] = {
        fileOrDirectoryOpenDialog(prompt, dflt, None)
    }

    /** Put up a dialog to open a file or a directory.
     * @param prompt The title for the dialog.
     * @param dflt The intially default file or directory.
     * @param approveLabel The label to use on the Approve button.
     * @return The selected file or directory, or None if cancelled.
     */
    def fileOrDirectoryOpenDialog(prompt:String, dflt:Option[File],
                            approveLabel:Option[String]):Option[File] = {
        val chooser = new JFileChooser()
        //If the specified directory does not exist, JFileChooser
        //defaults to a completely different directory.
        //In an attempt to be slightly more reasonable, we
        //look for the closest parent that does exist.
        var d = dflt
        while (d.isDefined && !d.get.exists) {
            d = d.flatMap(f=>SomeOrNone(f.getParentFile))
        }
        chooser.setCurrentDirectory(d getOrNull)
        chooser.setDialogType(JFileChooser.OPEN_DIALOG)
        chooser.setDialogTitle(prompt)
        chooser.setMultiSelectionEnabled(false)
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)

        val result = if (approveLabel.isDefined)
                chooser.showDialog(dialogParent, approveLabel.get) else
                chooser.showOpenDialog(dialogParent)
        decodeChooserResult(result,chooser)
    }

    /** Put up a file save dialog. */
    def fileSaveDialog(prompt:String):Option[File] = {
        fileSaveDialog(prompt,None.asInstanceOf[Option[File]])
    }

    //See fileOpenDialog for comment about dflt:String
    /** Put up a file save dialog.
     * @return None if cancelled
     */
    def fileSaveDialog(prompt:String, dflt:String):Option[File] = {
        fileSaveDialog(prompt,SomeOrNone(dflt).map(new File(_)))
    }

    /** Put up a file save dialog.
     * @return None if cancelled
     */
    def fileSaveDialog(prompt:String, dflt:Option[File]):Option[File] = {
        val chooser = new JFileChooser(dflt getOrNull)
        chooser.setDialogTitle(prompt)
        val result = chooser.showSaveDialog(dialogParent)
        decodeChooserResult(result,chooser)
    }

    /** Put up a directory save dialog.
     * @return None if cancelled
     */
    def directorySaveDialog(prompt:String, dflt:Option[File]):Option[File] = {
        val chooser = new JFileChooser(dflt getOrNull)
        chooser.setDialogTitle(prompt)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        val result = chooser.showSaveDialog(dialogParent)
        decodeChooserResult(result,chooser)
    }

    /** Put up a dialog to select a directory.
     * @param defaultDir The directory intially displayed in the dialog.
     * @param title The title for the dialog.
     * @param approveLabel The label to use on the Approve button.
     * @return The selected directory, or None if cancelled.
     */
    def selectDirectory(defaultDir:Option[String],title:String,
                            approveLabel:Option[String]):Option[String] = {
        val chooser = new JFileChooser()
        var d = defaultDir.map(new File(_))
        //If the specified directory does not exist, JFileChooser
        //defaults to a completely different directory.
        //In an attempt to be slightly more reasonable, we
        //look for the closest parent that does exist.
        while (d.isDefined && !d.get.exists) {
            d = d.flatMap(f=>SomeOrNone(f.getParentFile))
        }
        chooser.setCurrentDirectory(d getOrNull)
        chooser.setDialogType(JFileChooser.OPEN_DIALOG)
        chooser.setDialogTitle(title)
        chooser.setMultiSelectionEnabled(false)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)

        val result = chooser.showDialog(dialogParent, approveLabel getOrNull)
        if (result != JFileChooser.APPROVE_OPTION)
            None	//user cancelled
        else
            SomeOrNone(chooser.getSelectedFile().toString())
    }

    /* TBD - add saveTextToFile(String text) that asks here for
     * the output file.
     */

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, ask the user for confirmation
     * before overwriting the file.
     * @return The opened PrintWriter, or None if the user
     *         declined to open the file.
     */
    def getPrintWriterFor(filename:String):Option[PrintWriter] =
        getPrintWriterFor(new File(filename))

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, ask the user for confirmation
     * before overwriting the file.
     * @return The opened PrintWriter, or None if the user
     *         declined to open the file.
     */
    def getPrintWriterFor(f:File):Option[PrintWriter] = {
        if (f.exists()) {
            val msg = dialogRes.getResourceFormatted(
                    "query.Confirm.FileExists",f.toString())
            if (!confirmDialog(msg))
                    return None 	//cancelled
        }
        try {
            val w = new PrintWriter(new FileWriter(f))
            Some(w)
        } catch {
            case ex:IOException =>
                //TBD - handle this exception?
                throw new RuntimeException(ex)
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
        val wOpt = getPrintWriterFor(filename)
        wOpt.foreach { w =>
            w.write(text)
            w.close()
        }
        wOpt.isDefined
    }
}
