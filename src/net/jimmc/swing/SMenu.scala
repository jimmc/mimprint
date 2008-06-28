/* SMenu.scala
 *
 * Jim McBeath, June 27, 2008
 */

package net.jimmc.swing

import javax.swing.JMenu

class SMenu(frame:SFrame, resPrefix:String)
        extends JMenu(frame.getResourceString(resPrefix+".label"))
        with SComponent with SCompToolPrompt {

    setupToolTip(frame, resPrefix)
    setupToolPrompt(frame, resPrefix)
    setupIcon(frame, resPrefix)
    //setupActionListener(frame, action)
}
