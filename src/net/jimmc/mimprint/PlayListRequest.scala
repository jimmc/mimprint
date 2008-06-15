package net.jimmc.mimprint

import net.jimmc.util.Subscriber

/** Subscribers wanting to make requests to change a PlayList should
 * send a message which is a subtype of PlayListRequest to the
 * PlayListTracker that owns the playlist.
 */
sealed abstract class PlayListRequest

/** A request to send an init message to the subscriber. */
case class PlayListRequestInit(sub:Subscriber[PlayListMessage])
        extends PlayListRequest

/** Request to add an item to a playlist. */
case class PlayListRequestAdd(list:PlayListS, item:PlayItemS)
        extends PlayListRequest

/** Request to remove an item from a playlist. */
//case class PlayListRequestRemove(list:PlayListS, index:Int)
//        extends PlayListRequest
//TODO - PlayListRequestRemove NYI

/** Request to replace an item with a new item. */
case class PlayListRequestChange(list:PlayListS, index:Int, item:PlayItemS)
        extends PlayListRequest

/** Request to rotate an item. */
case class PlayListRequestRotate(list:PlayListS, index:Int, rot:Int)
        extends PlayListRequest

/** Request to select an item. */
case class PlayListRequestSelect(list:PlayListS, index:Int)
        extends PlayListRequest

/** Request to select the previous item. */
case class PlayListRequestUp(list:PlayListS) extends PlayListRequest

/** Request to select the next item. */
case class PlayListRequestDown(list:PlayListS) extends PlayListRequest

/** Request to select the previous playlist. */
case class PlayListRequestLeft(list:PlayListS) extends PlayListRequest

/** Request to select the next playlist. */
case class PlayListRequestRight(list:PlayListS) extends PlayListRequest
