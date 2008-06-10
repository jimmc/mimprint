package net.jimmc.mimprint

/** Subscribers interested in receiving messages about changes to
 * this playlist should subscribe by sending a Subscribe[PlayListMessage]
 * message to this playlist.  See the ActorPublisher trait for
 * the Subscribe and Unsubscribe messages. */
sealed abstract class PlayListMessage

/** PlayListAddItem is sent to subscribers after an item has been added to
 * the playlist at the specified index.
 * @param index The index of the new item in newList. */
case class PlayListAddItem(oldList:PlayListS, newList:PlayListS, index:Int)
        extends PlayListMessage

/** PlayListRemoveItem is sent to subscribers after an item has been removed
 * from the playst at the specifid index.
 * @param index The index of the removed item in oldList. */
case class PlayListRemoveItem(oldList:PlayListS, newlist:PlayListS, index:Int)
        extends PlayListMessage

/** PlayListChangeItem is sent to subscribers after the item at the
 * specified index has been changed. */
case class PlayListChangeItem(oldList:PlayListS, newList:PlayListS, index:Int)
        extends PlayListMessage
