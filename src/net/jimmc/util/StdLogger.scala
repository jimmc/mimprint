/* Simplify a bit setting up a logger.
 * See http://www.uncarved.com/blog/LogHelper.mrk
 */

package net.jimmc.util

import org.apache.log4j.Logger

trait StdLogger {
    val loggerName = this.getClass.getName
    lazy val logger = Logger.getLogger(loggerName)
}

