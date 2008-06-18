/* SLabel.scala
 *
 * Jim McBeath, June 16, 2008
 */

package net.jimmc.swing

import javax.swing.JLabel

class SLabel(frame:SFrame, resPrefix:String)
        extends JLabel(frame.getResourceString(resPrefix+".label"))
        with SComponent with SCompToolPrompt {

    setupToolTip(frame, resPrefix)
    setupToolPrompt(frame, resPrefix)
}
