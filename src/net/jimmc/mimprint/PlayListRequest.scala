package net.jimmc.mimprint

/** Subscribers wanting to make requests to change a PlayList should
 * send a message which is a subtype of PlayListRequest to the
 * PlayListTracker that owns the playlist.
 */
sealed abstract class PlayListRequest

/** A request to send an init message to the subscriber. */
case class PlayListRequestInit(view:PlayView)
        extends PlayListRequest

/** PlayListRequestAdd asks to add an item to a playlist. */
case class PlayListRequestAdd(list:PlayListS, item:PlayItemS)
        extends PlayListRequest

/** PlayListRequestRemove asks to remove an item from a playlist. */
//case class PlayListRequestRemove(list:PlayListS, index:Int)
//        extends PlayListRequest
//TODO - PlayListRequestRemove NYI

/** PlayListRequestChange asks to replace an item with a new item. */
//case class PlayListRequestChange(list:PlayListS, index:Int, item:PlayItemS)
//        extends PlayListRequest
//TODO - PlayListRequestChange NYI

/** PlayListRequestRotate asks to rotate an item. */
case class PlayListRequestRotate(list:PlayListS, index:Int, rot:Int)
        extends PlayListRequest

/** PlayListRequestSelect asks to select an item. */
case class PlayListRequestSelect(list:PlayListS, index:Int)
        extends PlayListRequest
