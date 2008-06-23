/* UserException.scala
 *
 * Jim McBeath, October 24, 2001
 * converted from java to scala June 22, 2008
 */

package net.jimmc.util

/** An exception whose message is intended to be displayed to the user.
 * This is typically to report an error having to do with data that the
 * user is editing.
 */
class UserException(msg:String) extends RuntimeException(msg)
