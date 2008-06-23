/* ToolPrompter.scala
 *
 * Jim McBeath, June 1, 2002
 * converted from java to scala June 21, 2008
 */

package net.jimmc.swing

/** A component which can display tool prompt strings.
 */
trait ToolPrompter {
	/** Display a tool prompt string. */
	def showToolPrompt(prompt:String)

	/** Clear the current tool prompt string. */
	def clearToolPrompt()
}

/* end */
