/* ToolPrompter.java
 *
 * Jim McBeath, June 1, 2002
 */

package net.jimmc.swing;

/** A component which can display tool prompt strings.
 */
public interface ToolPrompter {
	/** Display a tool prompt string. */
	public void showToolPrompt(String prompt);

	/** Clear the current tool prompt string. */
	public void clearToolPrompt();
}

/* end */
