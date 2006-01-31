/* MessagePanel.java
 *
 * Jim McBeath, October 23, 2001
 */

package net.jimmc.swing;

import net.jimmc.util.ResourceSource;

import java.util.Date;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

/** A panel to display messages from various sources. */
public class MessagePanel extends JScrollPane {
	/** Our containing frame. */
	protected JsFrame frame;

	/** Our resource source. */
	protected ResourceSource res;

	/** The text area. */
	protected JTextArea textArea;

	/** Create the Message panel. */
	public MessagePanel(JsFrame frame, ResourceSource res) {
		this.frame = frame;
		this.res = res;
		textArea = new JTextArea();
		setViewportView(textArea);
		textArea.setEditable(false);
	}

	/** Add a message to our display.
	 * @param source The message source string.
	 * @param message The text of the message.
	 */
	public void appendMessage(String source, String message) {
		Date now = new Date();
		Object[] args = { now, source, message };
		String msg = res.getResourceFormatted(
				"message.format.first",args);
		textArea.append(msg+"\n");
	}
}

/* end */
