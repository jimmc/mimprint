/* EditTextDialog.scala
 *
 * Jim McBeath, June 14, 2008
 */

package net.jimmc.swing

import java.awt.Component
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea

trait EditTextDialog {
    protected def dialogParent:Component

    def editTextDialog(title:String, text:String):String = {
        val tx = new JTextArea(text)
        val scroll = new JScrollPane(tx)
        scroll.setPreferredSize(new Dimension(500,200))
        val pane = new JOptionPane(scroll,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION)
        val dlg:JDialog = pane.createDialog(dialogParent,title)
        dlg.setResizable(true)
        pane.setInitialValue(null)
        pane.selectInitialValue()
	//We want the text area to have the focus, and it seems there is no
	//easy way to do this.  See Bug 4222534, from whence came this code.
	dlg.addWindowListener(new WindowAdapter() {
	    override def windowActivated(e:WindowEvent) =
                SwingS.invokeLater(tx.requestFocus)
	})
        dlg.show()    //get user's changes

        val v:Any = pane.getValue()
        v match {
            case n:Int =>
                if (n==JOptionPane.NO_OPTION || n==JOptionPane.CANCEL_OPTION)
                    return null        //canceled
                val newText = tx.getText()
                newText
            case _ => return null       //CLOSED_OPTION
        }
    }
}
