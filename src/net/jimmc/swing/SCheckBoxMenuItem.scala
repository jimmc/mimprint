/* SCheckBoxMenuItem.scala
 *
 * Jim McBeath, June 16, 2008
 */

package net.jimmc.swing

import javax.swing.JCheckBoxMenuItem

class SCheckBoxMenuItem(frame:SFrame, resPrefix:String)(action: =>Unit)
        extends JCheckBoxMenuItem(frame.getResourceString(resPrefix+".label"))
        with SComponent with SCompToolPrompt {

    setupToolTip(frame, resPrefix)
    setupToolPrompt(frame, resPrefix)
    setupIcon(frame, resPrefix)
    setupActionListener(frame, action)
}
