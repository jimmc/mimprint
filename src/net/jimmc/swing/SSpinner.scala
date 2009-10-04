/* SSpinner.scala
 *
 * Jim McBeath, June 16, 2008
 */

package net.jimmc.swing

import javax.swing.JSpinner
import javax.swing.SpinnerModel

class SSpinner(frame:SFrame, resPrefix:String, model:SpinnerModel)(
        action: (SSpinner)=>Unit)
        extends JSpinner(model)
        with SComponent with SCompToolPrompt {

    setupToolTip(frame, resPrefix)
    setupToolPrompt(frame, resPrefix)
    setupChangeListener(frame, action(this))
}
