/* AboutWindow.java
 *
 * Jim McBeath, June 28, 1997 (from jimmc.roots.AboutWindow)
 */

package net.jimmc.swing;

import java.awt.Frame;
import javax.swing.JOptionPane;

/** The "Help/About" window.
 * The application should call {@link #setAboutTitle} and
 * {@link #setAboutInfo} during initialization.
 */
public class AboutWindow {
	/** The title text. */
	public static String aboutTitle = "About...";

	/** The info to display in the About dialog. */
	public static String aboutInfo = "No info";

	/* Can't directly instantiate. */
	private AboutWindow(){}

	/** Set the title to use on the About dialog. */
	public static void setAboutTitle(String title) {
		aboutTitle = title;
	}

	/** Set the info string to use on the About dialog. */
	public static void setAboutInfo(String info) {
		aboutInfo = info;
	}

	/** Create a new AboutWindow dialog. */
	public static void showAboutWindow(Frame parent) {
		JOptionPane.showMessageDialog(parent,aboutInfo,
			aboutTitle,JOptionPane.INFORMATION_MESSAGE,null);
	}
}

/* end */
