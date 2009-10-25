/* PlayListRequest.scala
 *
 * Jim McBeath, June 10, 2008
 */

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

/** Request to add an item to the end of a playlist. */
case class PlayListRequestAdd(list:PlayList, item:PlayItem)
        extends PlayListRequest

/** Request to insert an item at the specified point in  a playlist.
 * All items at and after the given index are moved up by one, and
 * the new item is inserted at the given index point.
 */
case class PlayListRequestInsert(list:PlayList, index:Int, item:PlayItem)
        extends PlayListRequest

/** Request to remove an item from a playlist.
 * After the removal, all items above the given index are moved down by one.
 */
case class PlayListRequestRemove(list:PlayList, index:Int)
        extends PlayListRequest

/** Request to replace an item with a new item. */
case class PlayListRequestChange(list:PlayList, index:Int, item:PlayItem)
        extends PlayListRequest

/** Request for listeners to update the display of an item due to a change
 * to non-playlist related data, such as the image or accompanying text. */
case class PlayListRequestUpdate(list:PlayList, index:Int)
        extends PlayListRequest

/** Request to replace an existing item with a new item,
 * or to expand the PlayList as required to add the item at the
 * specified index. */
case class PlayListRequestSetItem(list:PlayList, index:Int, item:PlayItem)
        extends PlayListRequest

/** Request to rotate an item. */
case class PlayListRequestRotate(list:PlayList, index:Int, rot:Int)
        extends PlayListRequest

/** Request to select an item. */
case class PlayListRequestSelect(list:PlayList, index:Int)
        extends PlayListRequest

/** Request to select the previous item. */
case class PlayListRequestUp(list:PlayList) extends PlayListRequest

/** Request to select the next item. */
case class PlayListRequestDown(list:PlayList) extends PlayListRequest

/** Request to select the previous playlist. */
case class PlayListRequestLeft(list:PlayList) extends PlayListRequest

/** Request to select the next playlist. */
case class PlayListRequestRight(list:PlayList) extends PlayListRequest
