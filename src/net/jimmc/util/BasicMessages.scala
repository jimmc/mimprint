/* BasicMessages.scala
 *
 * Jim McBeath, June 13, 2008
 */

package net.jimmc.util

/** Functions for basic message output.
 * Implementations of these methods should be non-blocking.
 */
trait BasicMessages {
    def warningMessage(msg:String)
    def errorMessage(msg:String)
}
