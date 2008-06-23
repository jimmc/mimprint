/* AboutWindow.scala
 *
 * Jim McBeath, June 28, 1997 (from jimmc.roots.AboutWindow)
 * converted from java to scala June 21, 2008
 */

package net.jimmc.swing

import java.awt.Frame
import javax.swing.JOptionPane

/** The "Help/About" window.
 * The application should call {@link #setAboutTitle} and
 * {@link #setAboutInfo} during initialization.
 */
object AboutWindow {
    /** The title text. */
    var aboutTitle = "About..."

    /** The info to display in the About dialog. */
    var aboutInfo = "No info"

    /** Set the title to use on the About dialog. */
    def setAboutTitle(title:String) = aboutTitle = title

    /** Set the info string to use on the About dialog. */
    def setAboutInfo(info:String) = aboutInfo = info

    /** Create a new AboutWindow dialog. */
    def showAboutWindow(parent:Frame) {
        JOptionPane.showMessageDialog(parent,aboutInfo,
                aboutTitle,JOptionPane.INFORMATION_MESSAGE,null)
    }
}
