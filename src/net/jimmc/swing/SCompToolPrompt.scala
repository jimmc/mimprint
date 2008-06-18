/* SCompToolPrompt.scala
 *
 * Jim McBeath, June 16, 2008
 */

package net.jimmc.swing

import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

trait SCompToolPrompt { this:JComponent =>

    def setupToolPrompt(frame:SFrame, resPrefix:String) {
        val tpKey = resPrefix+".toolPrompt"
        val toolPrompt = frame.getResourceStringOption(tpKey)
        val toolPrompter = frame match {
            case tp:ToolPrompter => tp
            case _ => null
        }
        if (toolPrompter!=null && !toolPrompt.isEmpty) {
            addFocusListener(
                    new SCompToolPromptFocusListener(toolPrompter,toolPrompt))
            addMouseListener(
                    new SCompToolPromptMouseListener(toolPrompter,toolPrompt))
        }
    }

    class SCompToolPromptFocusListener(toolPrompter:ToolPrompter,
            toolPrompt:Option[String]) extends FocusListener {
        def focusGained(ev:FocusEvent) =
            toolPrompter.showToolPrompt(toolPrompt.get)
        def focusLost(ev:FocusEvent) =
            toolPrompter.clearToolPrompt()
    }

    class SCompToolPromptMouseListener(toolPrompter:ToolPrompter,
            toolPrompt:Option[String]) extends MouseAdapter {
        override def mouseEntered(ev:MouseEvent) =
            toolPrompter.showToolPrompt(toolPrompt.get)
        override def mouseExited(ev:MouseEvent) =
            toolPrompter.clearToolPrompt()
    }
}
