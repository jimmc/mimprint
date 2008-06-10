package net.jimmc.mimprint

/** Subscribers wanting to make requests to change a PlayList should
 * send a message which is a subtype of PlayListRequest to the
 * PlayListTracker that owns the playlist.
 */
sealed abstract class PlayListRequest

/** PlayListRequestAdd asks to add an item to a playlist. */
case class PlayListRequestAdd(list:PlayListS, item:PlayItemS)

/** PlayListRequestRemove asks to remove an item from a playlist. */
//case class PlayListRequestRemove(list:PlayListS, index:Int)
//TODO - NYI

/** PlayListRequestChange asks to replace an item with a new item. */
//case class PlayListRequestChange(list:PlayListS, index:Int, item:PlayItemS)
//TODO - NYI

/** PlayListRequestRotate asks to rotate an item. */
case class PlayListRequestRotate(list:PlayListS, index:Int, rot:Int)
