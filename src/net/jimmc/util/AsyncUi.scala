/* AsyncUi.scala
 *
 * Jim McBeath, June 13, 2008
 */

package net.jimmc.util

trait AsyncUi extends StandardUi {
    /** The invokeUi method can be called by a client to execute code
     * asynchronously from itself.  This is typically used when the
     * caller does not want to block, but needs to call a UI query
     * method.  The passd-in code can call the blocking UI query method,
     * evaluate the results, and pass along an appropriate continuatio 
     * message back to itself.
     */
    def invokeUi(code: =>Unit)
}
