/* SwingS.scala
 *
 * Jim McBeath, June 12, 2008
 */

package net.jimmc.swing

import javax.swing.SwingUtilities

object SwingS {
    def invokeLater(block: => Unit) {
        SwingUtilities.invokeLater(new Runnable() {
            override def run() = block
        })
    }
}
