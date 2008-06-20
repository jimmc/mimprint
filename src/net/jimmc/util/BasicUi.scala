/* BasicUi.scala
 *
 * Jim McBeath, June 13, 2008
 */

package net.jimmc.util

/** The traits required for a basic user interface:
 * basic message output, basic queries, and resource strings.
 */
trait BasicUi extends BasicMessages with BasicQueries with SResources
