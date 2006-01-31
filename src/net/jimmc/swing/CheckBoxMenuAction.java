/* CheckBoxMenuAction.java
 *
 * Jim McBeath, May 28, 2002
 */

package net.jimmc.swing;

import net.jimmc.util.ExceptionHandler;
import net.jimmc.util.ResourceSource;

import javax.swing.JCheckBoxMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** An adapter class to simplify defining checkboxes with actions.
 */
public class CheckBoxMenuAction extends JCheckBoxMenuItem
			implements ActionListener {
	/** An exception handler. */
	protected ExceptionHandler exceptionHandler;

	/** Create a menu check box using resources, with an exception handler.
	 * @param resourceSource The source for resource strings.
	 * @param resourcePrefix The prefix to use to build the resource
	 *        string we use.  We append the following to the prefix:
	 * @param exceptionHandler The handler to deal with exceptions.
	 * <ul>
	 * <li>.label - to get the label for the check box.
	 * <li>.toolTip  - to get the toolTip text.
	 * </ul>
	 */
	public CheckBoxMenuAction(ResourceSource resourceSource,
			String resourcePrefix,
			ExceptionHandler exceptionHandler) {
		this(resourceSource.getResourceString(resourcePrefix+".label"));
		String resName = resourcePrefix+".toolTip";
		String toolTip = resourceSource.getResourceString(resName);
		if (toolTip!=null && !toolTip.equals(resName))
			setToolTipText(toolTip);
		setExceptionHandler(exceptionHandler);
	}

	/** Create a menu check box. */
	public CheckBoxMenuAction(String label) {
		super(label);
		addActionListener(this);
	}

	/** Set an exception handler. */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/** Here on an action event. */
	public void actionPerformed(ActionEvent ev) {
		if (exceptionHandler==null) {
			action();
		} else {
			try {
				exceptionHandler.beforeAction(this);
				action();
				exceptionHandler.afterAction(this);
			} catch (Exception ex) {
				exceptionHandler.handleException(this,ex);
			}
		}
	}

	/** The default action for this button.
	 * The subclass usually overrides this method.
	 */
	public void action() {
		/* do nothing */
	}
}

/* end */
