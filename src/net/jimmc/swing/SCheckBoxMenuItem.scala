package net.jimmc.swing

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URL
import javax.swing.ImageIcon
import javax.swing.JCheckBoxMenuItem

class SCheckBoxMenuItem(frame:SFrame, resPrefix:String)(action: =>Unit)
        extends JCheckBoxMenuItem(frame.getResourceString(resPrefix+".label")) {

    //Set up the tool tip text if defined in the resources
    val ttKey = resPrefix+".toolTip"
    frame.getResourceStringOption(ttKey).foreach(setToolTipText(_))

    val tpKey = resPrefix+".toolPrompt"
    val toolPrompt = frame.getResourceStringOption(tpKey)
    val toolPrompter = frame match {
        case tp:ToolPrompter => tp
        case _ => null
    }

    //Set up the icon if defined in the resources
    val iconKey = resPrefix+".icon"
    frame.getResourceStringOption(iconKey).foreach { iconName =>
        setIcon(loadIcon(iconName))
    }

    //Wire in our action listener for button pushes
    addActionListener(new ActionListener() {
        override def actionPerformed(ev:ActionEvent) {
            actionWithCatch      //perform the action and handle exceptions
        }
    })

    if (toolPrompter!=null && !toolPrompt.isEmpty) {
        addFocusListener(new SCheckBoxMenuItemFocusListener())
        addMouseListener(new SCheckBoxMenuItemMouseListener())
    }

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

    class SCheckBoxMenuItemFocusListener() extends FocusListener {
        def focusGained(ev:FocusEvent) =
            toolPrompter.showToolPrompt(toolPrompt.get)
        def focusLost(ev:FocusEvent) =
            toolPrompter.clearToolPrompt()
    }

    class SCheckBoxMenuItemMouseListener extends MouseAdapter {
        override def mouseEntered(ev:MouseEvent) =
            toolPrompter.showToolPrompt(toolPrompt.get)
        override def mouseExited(ev:MouseEvent) =
            toolPrompter.clearToolPrompt()
    }
}
