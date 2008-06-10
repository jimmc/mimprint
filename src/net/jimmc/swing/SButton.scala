package net.jimmc.swing

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.URL
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.SwingConstants

class SButton(frame:SFrame, resPrefix:String)(action: =>Unit)
        extends JButton(frame.getResourceString(resPrefix+".label")) {

    //Set up the tool tip text if defined in the resources
    val ttKey = resPrefix+".toolTip"
    frame.getResourceStringOption(ttKey).foreach(setToolTipText(_))

    //Set up the icon if defined in the resources
    val iconKey = resPrefix+".icon"
    frame.getResourceStringOption(iconKey).foreach { iconName =>
        setIcon(loadIcon(iconName))
        //Make the text sit under the icon
        setVerticalTextPosition(SwingConstants.BOTTOM)
        setHorizontalTextPosition(SwingConstants.CENTER)
        if (frame.getResourceStringOption(resPrefix+".label").isEmpty) {
            //OK to specify no label if we have an icon
            setText(null)
        }
    }

    //Wire in our action listener for button pushes
    addActionListener(new ActionListener() {
        override def actionPerformed(ev:ActionEvent) {
            actionWithCatch      //perform the action and handle exceptions
        }
    })

    //Execute our action.  If we get any exceptions, pass them to the
    //frame for handling.
    private def actionWithCatch() {
        try {
            action
        } catch {
            case ex:Exception => frame.handleUiException(ex)
        }
    }

    private def loadIcon(iconName:String):ImageIcon = {
        val cl = frame.getClass()
        val url:URL = cl.getResource(iconName)
        if (url==null)
            null
        else
            new ImageIcon(url)
    }
}
