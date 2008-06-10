package net.jimmc.swing

import net.jimmc.util.SResources
import net.jimmc.util.SResourcesFacade

import java.awt.Component
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.WindowConstants

/** A general Frame with some utility methods and support for Scala idioms.
 */
class SFrame(name:String, res:SResources)
        extends JFrame with SResourcesFacade with StandardDialogs {

    val sResourcesBase = res            //for SResourcesFacade
    val dialogRes = res                 //for StandardDialogs

    def addWindowListener() {
        addWindowListener(new WindowAdapter() {
            override def windowClosing(ev:WindowEvent) = processClose
        })
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    }

    def processClose() { setVisible(false); dispose() }

    def processFileExit() {
        if (confirmExit())
            System.exit(0)
    }

    /** Get the parent to use for the standard dialogs. */
    protected val dialogParent:Component = this

    /** Set to true to debug UserExceptions. */
    val debugUserExceptions = false

    /** Handle an exception thrown by a callback on a UI element. */
    def handleUiException(ex:Exception) = exceptionDialog(ex)
}
