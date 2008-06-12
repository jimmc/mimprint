package net.jimmc.mimprint

import net.jimmc.util.ActorPublisher
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
            react (handleMessage)
        }
    }

    //Edit this function to add message types for new functionality
    protected val handleMessage : PartialFunction[Any,Unit] = {
        case m:PlayListInit => playListInit(m)
        case m:PlayListAddItem => playListAddItem(m)
        case m:PlayListRemoveItem => playListRemoveItem(m)
        case m:PlayListChangeItem => playListChangeItem(m)
        case m:PlayListSelectItem => playListSelectItem(m)
        case m:PlayListChangeList => playListChangeList(m)
        case m:Any => println("Unrecognized message to PlayView: "+m)
    }

    //Extending class must implement these methods to process playlist changes
    protected def playListInit(m:PlayListInit):Unit
    protected def playListAddItem(m:PlayListAddItem):Unit
    protected def playListRemoveItem(m:PlayListRemoveItem):Unit
    protected def playListChangeItem(m:PlayListChangeItem):Unit
    protected def playListSelectItem(m:PlayListSelectItem):Unit
    protected def playListChangeList(m:PlayListChangeList):Unit
}
