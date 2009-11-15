/* PlayView.scala
 *
 * Jim McBeath, June 12, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.ActorPublisher
import net.jimmc.util.PFCatch
import net.jimmc.util.Subscribe
import net.jimmc.util.Subscriber

import scala.actors.Actor
import scala.actors.Actor.loop

/** A view of a PlayList. */
abstract class PlayView(private val tracker:PlayListTracker) extends Actor
        with Subscriber[PlayListMessage] {

    def act() {
        tracker ! Subscribe(this)
        tracker ! PlayListRequestInit(this)       //send us an init message
        loop {
            react (PFCatch(handlePlayListMessage orElse handleOtherMessage,
                    "PlayView",tracker.ui))
        }
    }

    //Handle our standard messages
    protected val handlePlayListMessage : PartialFunction[Any,Unit] = {
        case m:PlayListInit => playListInit(m)
        case m:PlayListAddItem => playListAddItem(m)
        case m:PlayListRemoveItem => playListRemoveItem(m)
        case m:PlayListChangeItem => playListChangeItem(m)
        case m:PlayListUpdateItem => playListUpdateItem(m)
        case m:PlayListPreSelectItem => playListPreSelectItem(m)
        case m:PlayListSelectItem => playListSelectItem(m)
        case m:PlayListPostSelectItem => playListPostSelectItem(m)
        case m:PlayListChangeList => playListChangeList(m)
    }

    //Extending class can override this val to add processing for
    //its own message types.
    protected val handleOtherMessage : PartialFunction[Any,Unit] = {
        case m:Any => println("Unrecognized message to PlayView: "+m)
    }

    //Extending class must implement these methods to process playlist changes
    protected def playListInit(m:PlayListInit):Unit
    protected def playListAddItem(m:PlayListAddItem):Unit
    protected def playListRemoveItem(m:PlayListRemoveItem):Unit
    protected def playListChangeItem(m:PlayListChangeItem):Unit
    protected def playListUpdateItem(m:PlayListUpdateItem):Unit
    protected def playListPreSelectItem(m:PlayListPreSelectItem):Unit = {}
    protected def playListSelectItem(m:PlayListSelectItem):Unit
    protected def playListPostSelectItem(m:PlayListPostSelectItem):Unit = {}
    protected def playListChangeList(m:PlayListChangeList):Unit
}
