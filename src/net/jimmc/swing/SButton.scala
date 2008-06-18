/* SButton.scala
 *
 * Jim McBeath, June 10, 2008
 */

package net.jimmc.swing

import javax.swing.JButton
import javax.swing.SwingConstants

class SButton(frame:SFrame, resPrefix:String)(action: =>Unit)
        extends JButton(frame.getResourceString(resPrefix+".label"))
        with SComponent {

    setupToolTip(frame,resPrefix)

    //Set up the icon if defined in the resources.
    //We need to fiddle with some parameters, so we don't use
    //SComponent.setupIcon().
    val iconKey = resPrefix+".icon"
    frame.getResourceStringOption(iconKey).foreach { iconName =>
        setIcon(loadIcon(frame,iconName))
        //Make the text sit under the icon
        setVerticalTextPosition(SwingConstants.BOTTOM)
        setHorizontalTextPosition(SwingConstants.CENTER)
        if (frame.getResourceStringOption(resPrefix+".label").isEmpty) {
            //OK to specify no label if we have an icon
            setText(null)
        }
    }

    //Wire in our action listener for button pushes
    setupActionListener(frame,action)
}
