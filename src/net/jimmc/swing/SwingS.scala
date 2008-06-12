package net.jimmc.swing

import javax.swing.SwingUtilities

object SwingS {
    def invokeLater(block: => Unit) {
        SwingUtilities.invokeLater(new Runnable() {
            override def run() = block
        })
    }
}
