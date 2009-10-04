/* STextField.scala
 *
 * Jim McBeath, June 14, 2008
 */

package net.jimmc.swing

import javax.swing.JTextField

class STextField(frame:SFrame, resPrefix:String, width:Int)(
	    action: (STextField)=>Unit)
        extends JTextField(width)
        with SComponent with SCompToolPrompt {

    setupToolTip(frame, resPrefix)
    setupToolPrompt(frame, resPrefix)
    setupIcon(frame, resPrefix)
    setupActionListener(frame, action(this))
}
