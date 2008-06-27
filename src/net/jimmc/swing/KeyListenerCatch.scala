/* KeyListenerCatch.scala
 *
 * Jim McBeath, June 27, 2008
 */

package net.jimmc.swing

import java.awt.event.KeyEvent
import java.awt.event.KeyListener

/** This key listener wraps a regular key listener, catches exceptions
 * on all those methods, and reports on them by calling
 * the exeptionDialog of the given frame.
 */
class KeyListenerCatch(k:KeyListener, frame:SFrame) extends KeyListener {
    def keyPressed(ev:KeyEvent) = catchAndHandle(k.keyPressed(ev))
    def keyReleased(ev:KeyEvent) = catchAndHandle(k.keyReleased(ev))
    def keyTyped(ev:KeyEvent) = catchAndHandle(k.keyTyped(ev))
            
    private def catchAndHandle(c: =>Unit) {
        try {
            c
        } catch {
            case ex:Exception => handleException(ex)
        }
    }

    protected def handleException(ex:Exception) {
        reportException(ex)
        pauseAfterException(ex)
    }

    protected def reportException(ex:Exception) = frame.exceptionDialog(ex)

    protected def pauseAfterException(ex:Exception) = Thread.sleep(10)
}
