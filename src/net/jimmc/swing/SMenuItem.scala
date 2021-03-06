/* SMenuItem.scala
 *
 * Jim McBeath, June 14, 2008
 */

package net.jimmc.swing

import javax.swing.JMenuItem

class SMenuItem(frame:SFrame, resPrefix:String)(action: =>Unit)
        extends JMenuItem(frame.getResourceString(resPrefix+".label"))
        with SComponent with SCompToolPrompt {

    setupToolTip(frame, resPrefix)
    setupToolPrompt(frame, resPrefix)
    setupIcon(frame, resPrefix)
    setupActionListener(frame, action)
}
