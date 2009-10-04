/* AboutWindow.scala
 *
 * Jim McBeath, June 28, 1997 (from jimmc.roots.AboutWindow)
 * converted from java to scala June 21, 2008
 */

package net.jimmc.swing

import java.awt.Frame
import javax.swing.JOptionPane

/** The "Help/About" window.
 * @param aboutTitle The title text
 * @param aboutInfo The info to display in the About dialog
 */
class AboutWindow(val aboutTitle:String, val aboutInfo:String) {
    /** Create a new AboutWindow dialog. */
    def showAboutWindow(parent:Frame) {
        JOptionPane.showMessageDialog(parent,aboutInfo,
                aboutTitle,JOptionPane.INFORMATION_MESSAGE,null)
    }
}
