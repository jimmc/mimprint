/** SComponent.scala
 *
 * Jim McBeath, June 16, 2008
 */

package net.jimmc.swing

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.URL
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.ImageIcon
import javax.swing.JComponent

trait SComponent { this:JComponent =>
    def setupToolTip(frame:SFrame, resPrefix:String) {
        //Set up the tool tip text if defined in the resources
        val ttKey = resPrefix+".toolTip"
        frame.getResourceStringOption(ttKey).foreach(setToolTipText(_))
    }

    def setupIcon(frame:SFrame, resPrefix:String) {
        //Set up the icon if defined in the resources and we have the call
        type HasSetIconMethod = { def setIcon(icon:ImageIcon) }
        this match {
            case c:HasSetIconMethod =>
                val iconKey = resPrefix+".icon"
                frame.getResourceStringOption(iconKey).foreach { iconName =>
                    c.setIcon(loadIcon(frame,iconName))
                }
        }
    }

    def setupActionListener(frame:SFrame, action: => Unit) {
        //Wire in our action listener if we have such a thing
        type HasAddActionListenerMethod =
            { def addActionListener(a:ActionListener) }
        this match {
            case c:HasAddActionListenerMethod =>
                c.addActionListener(new ActionListener() {
                    override def actionPerformed(ev:ActionEvent) {
                        actionWithCatch(frame, action)
                                //perform the action and handle exceptions
                    }
                })
        }
    }

    def setupChangeListener(frame:SFrame, action: => Unit) {
        //Wire in our action listener if we have such a thing
        type HasAddChangeListenerMethod =
            { def addChangeListener(a:ChangeListener) }
        this match {
            case c:HasAddChangeListenerMethod =>
                c.addChangeListener(new ChangeListener() {
                    override def stateChanged(ev:ChangeEvent) {
                        actionWithCatch(frame, action)
                                //perform the action and handle exceptions
                    }
                })
        }
    }

    //Execute our action.  If we get any exceptions, pass them to the
    //frame for handling.
    private def actionWithCatch(frame:SFrame, action: =>Unit) {
        try {
            action
        } catch {
            case ex:Exception => frame.handleUiException(ex)
        }
    }

    protected def loadIcon(frame:SFrame, iconName:String):ImageIcon = {
        val cl = frame.getClass()
        val url:URL = cl.getResource(iconName)
        if (url==null)
            null
        else
            new ImageIcon(url)
    }
}
