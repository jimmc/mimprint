/* PlayViewComp.scala
 *
 * Jim McBeath, June 17, 2008
 */

package net.jimmc.mimprint

import java.awt.Component

/** A PlayView that implements a swing component that sits inside
 * our viewer.
 */
abstract class PlayViewComp(val name:String, val viewer:SViewer,
        val tracker:PlayListTracker) extends PlayView(tracker) {

    def getComponent():Component

    def isShowing():Boolean
}
