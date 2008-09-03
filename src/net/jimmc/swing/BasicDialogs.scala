/* BasicDialogs.scala
 *
 * Jim McBeath, June 10, 2008
 */

package net.jimmc.swing

import net.jimmc.util.BasicQueries
import net.jimmc.util.SomeOrNone
import net.jimmc.util.SResources
import net.jimmc.util.StringUtil
import net.jimmc.util.UserException

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.AbstractButton
import javax.swing.Icon
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea

/** A collection of standard dialog methods. */
trait BasicDialogs extends BasicQueries {
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

    /** Ask if he really wants to exit from the program. */
    def confirmExit():Boolean = {
        val msg = dialogRes.getResourceString("query.Exit.prompt")
        val title = dialogRes.getResourceString("query.Exit.title")
        val msgObj = getMessageDisplay(msg)
        val n = JOptionPane.showConfirmDialog(dialogParent,msgObj,title,
                        JOptionPane.YES_NO_OPTION)
        (n==0)
    }

    /* Put up an OK/Cancel dialog.
     * @param prompt The prompt string to display in the dialog.
     * @return True if the user selects OK,
     *         false if the users selectes Cancel.
     */
    def confirmDialog(prompt:String):Boolean = {
        val title = dialogRes.getResourceString("query.Confirm.title")
        val promptObj = getMessageDisplay(prompt)
        val n = JOptionPane.showConfirmDialog(dialogParent,promptObj,title,
                        JOptionPane.OK_CANCEL_OPTION)
        (n==0)
    }

    /** Put up an error dialog. */
    def errorDialog(errmsg:String) {
        val title = dialogRes.getResourceString("query.Error.title")
        showMessageDialog(errmsg,title,JOptionPane.ERROR_MESSAGE)
    }

    /** Put up an info dialog. */
    def infoDialog(infomsg:String) {
        val title = dialogRes.getResourceString("query.Info.title")
        showMessageDialog(infomsg,title,JOptionPane.INFORMATION_MESSAGE)
    }

    protected def showMessageDialog(msg:String,title:String,paneType:Int) {
        getMessageDisplay(msg) match {
            case s:String =>
                JOptionPane.showMessageDialog(dialogParent,s,title,paneType)
            case m:Any =>
                //We have a large string which has been wrapped up
                //in a scrolled text area.
                val pane = new JOptionPane(m,paneType)
                val dlg = pane.createDialog(dialogParent,title)
                dlg.setResizable(true)
                        //Allow this dialog box to be resized so that
                        //the user can make it larger to see more of
                        //the text at once.
                pane.setInitialValue(null)
                pane.selectInitialValue()
                dlg.show()
        }
    }

    /** Get an object to display a message.
     * For short messages, the object is the message itself.
     * For long messages, the object is a JTextArea inside s JScrollPane.
     */
    protected def getMessageDisplay(msg:String):Object = {
        if (!messageRequiresScrollPane(msg))
                return msg
        //It's a long string, so rather than putting it up as
        //a single string, we put it up as a text box with
        //wraparound turned on.
        val tx:JTextArea = getMessageDisplayTextArea(msg)
        tx.setEditable(false)
        tx.setLineWrap(true)
        val pane = new JScrollPane(tx)
        pane.setPreferredSize(new Dimension(500,200))
        pane
    }

    /** True if the message is large enough to require the use
     * of a scroll pane in the dialog.
     */
    protected def messageRequiresScrollPane(msg:String):Boolean = {
        //return (msg.length()<=maxSimpleMessageLength)
        val w = StringUtil.getLongestLineWidth(msg)
        val h = StringUtil.getLineCount(msg)
        (w>maxSimpleMessageLineWidth ||
                h>maxSimpleMessageLineCount)
    }

    /** Get the JTextArea to use as the message display box. */
    protected def getMessageDisplayTextArea(msg:String):JTextArea = {
        new JTextArea(msg)
    }

    /** Put up a string dialog. */
    def stringDialog(prompt:String):String = {
        JOptionPane.showInputDialog(prompt)
    }

    /** Put up a three-button Yes/No/Cancel dialog.
     * @return The index of the selected button:
     *         0 for yes, 1 for no, 2 for cancel.
     */
    def yncDialog(prompt:String, yes:String, no:String, cancel:String):Int = {
        val title = dialogRes.getResourceString("query.Confirm.title")
        val labels = Array(yes,no,cancel)
        multiButtonDialog(prompt,title,labels)
    }

    /** Put up a dialog with multiple buttons.
     * @param prompt The prompt string.
     * @param title The title string.
     * @param labels The labels to use on the buttons.
     * @return The index number of the selected button.
     */
    def multiButtonDialog(prompt:String, title:String,
            labels:Array[String]):Int = {
        val promptObj = getMessageDisplay(prompt)
        val dflt:String = null
        val t = JOptionPane.showOptionDialog(dialogParent,promptObj,title,
                JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,
                null, labels.asInstanceOf[Array[Object]], dflt)
        t
    }

    /** Put up a dialog with multiple buttons, using resources to
     * determine the button labels, mnemonics, and displayed mnemonic index.
     * @param prompt The prompt string.
     * @param title The title string.
     * @param prefix The resource prefix string, including the dot separator.
     * @param buttonKeys The resource keys for the buttons to be displayed.
     * @return The index number of the selected button.
     */
    def multiButtonDialogR(prompt:String, title:String, prefix:String,
            buttonKeys:Array[String]):Int = {
        val promptObj = getMessageDisplay(prompt)
        val labels = buttonKeys.map{key:String=>
                dialogRes.getResourceString(prefix+"button."+key+".label")}
        val mnemonics = buttonKeys.map{key:String=>
            dialogRes.getResourceStringOption(prefix+"button."+key+".mnemonic")}
        val mnemList = (labels zip mnemonics) filter {
                km => km._2.isDefined } map {
                km => (km._1,km._2.get.charAt(0).asInstanceOf[Int]) }
        val mnemMap = Map() ++ mnemList
        val mnemIndexes = buttonKeys.map{key:String=>
            dialogRes.getResourceStringOption(prefix+"button."+key+".mnemonicIndex")}
        val mxList = (labels zip mnemIndexes) filter {
                km => km._2.isDefined } map {
                km => (km._1,Integer.parseInt(km._2.get)) }
        val mxMap = Map() ++ mxList
        //mxMap is a map of button label to mnemonic index offset
        val icon:Icon = null
        val defaultLabel:Object = dialogRes.getResourceStringOption(
                prefix+"defaultButtonLabel").getOrElse(null)
        val pane = new JOptionPane(prompt, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, icon,
                labels.asInstanceOf[Array[Object]], defaultLabel)
        val dialog = pane.createDialog(dialogParent, title)
        addMnemonicsToButtons(pane,mnemMap,mxMap)
        dialog.show()
        pane.getValue match {
            case s:String => labels.indexOf(s)
            case _ => -1
        }
    }

    private def addMnemonicsToButtons(p:Container,
            mnemMap:Map[String,Int], mxMap:Map[String,Int]) {
        p.getComponents foreach { _ match {
            case b:AbstractButton =>
                val label = b.getText
                mnemMap.get(label) foreach { b.setMnemonic(_) }
                mxMap.get(label) foreach { b.setDisplayedMnemonicIndex(_) }
            case c:Container => addMnemonicsToButtons(c,mnemMap,mxMap)
            case _ => //ignore
        }}
    }

    /** A dialog to display an exception. */
    def exceptionDialog(ex:Throwable) {
        var error = ex.getMessage()
        if (error==null || error.trim().length()==0)
                error = ex.getClass().getName()
        if ((ex.isInstanceOf[UserException]) && debugUserExceptions) {
                //On a user exception, no debugging buttons
                //unless the debug flag has been set.
                errorDialog(error)
                return
        }
        val title = dialogRes.getResourceString("query.Error.title")
        val labels = Array(
            dialogRes.getResourceString("query.button.OK.label"),
            dialogRes.getResourceString("query.button.Details.label")
        )
        multiButtonDialog(error,title,labels) match {
            case 0 =>		//OK
                //do nothing
            case 1 =>		//Details
                exceptionDetailsDialog(ex)
            case _ =>           //ignore anything else
        }
    }

    /** A dialog to display the traceback on an exception. */
    def exceptionDetailsDialog(ex:Throwable) {
        //Print the stack trace to a string
        val sw = new StringWriter()
        ex.printStackTrace(new PrintWriter(sw))
        val stackTrace = sw.toString()

        val title= dialogRes.getResourceString("query.ErrorDetails.title")
        val labels = Array(
            dialogRes.getResourceString("query.button.OK.label"),
            dialogRes.getResourceString("query.button.PrintTraceback.label"),
            dialogRes.getResourceString(
                "query.button.SaveTracebackToFile.label")
        )
        multiButtonDialog(stackTrace,title,labels) match {
            case 0 =>		//OK
                //do nothing
            case 1 =>		//Print Traceback
                ex.printStackTrace()
            case 2 =>		//Save Traceback To File
                val prompt:String = dialogRes.getResourceString(
                        "query.SaveTracebackToFile")
                //If we don't happen to have access to the file-save dialog,
                //we can fall back on the normaml string dialog in a pinch.
                val traceFileOpt:Option[File] =
                    this match {
                        case fd:FileDialogs =>
                            fd.fileSaveDialog(prompt)
                        case _ =>
                            val fn = stringDialog(prompt)
                            if (fn==null) None else Some(new File(fn))
                    }
                traceFileOpt.foreach { traceFile =>
                    try {
                        //getPrintWriter is preferred because it checks to
                        //see if the file already exists.
                        val pwOpt = this match {
                            case fd:FileDialogs =>
                                fd.getPrintWriterFor(traceFile)
                            case _ =>
                                val w = new FileWriter(traceFile)
                                Some(new PrintWriter(w))
                        }
                        pwOpt.foreach { pw =>
                            ex.printStackTrace(pw)
                            pw.close()
                        }
                    } catch {
                        case e2:Exception =>
                            System.out.println("Error writing trace file")
                            //ignore errors in this part
                    }
                }
            //end of cases
        }
    }
}
