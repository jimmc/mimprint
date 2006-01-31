/* MenuAction.java
 *
 * Jim McBeath, June 7, 1998
 */

package net.jimmc.swing;

import net.jimmc.util.ExceptionHandler;
import net.jimmc.util.ResourceSource;

import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/** An adapter class for menus with actions.
 */
public class MenuAction extends JMenuItem implements ActionListener,
			FocusListener, MouseListener {
	/** An exception handler. */
	protected ExceptionHandler exceptionHandler;

	/** Handler for tool prompts. */
	protected ToolPrompter toolPrompter;

	/** Tool prompt string. */
	protected String toolPrompt;

	/** Create a menu item using resources, with an exception handler.
	 * @param resourceSource The source for resource strings.
	 * @param resourcePrefix The prefix to use to build the resource
	 *        string we use.  We append the following to the prefix:
	 * @param uiHandler A handler to deal with various things.
	 *        If this object implements
	 *        {@link ExceptionHandler}, it is used to handle exceptions
	 *        happening during the action for this MenuAction.
	 *        If this object implements {@link ToolPrompter},
	 *        it is used to displays tool prompts for this MenuAction,
	 *        if defined.
	 * <ul>
	 * <li>.label - to get the string to display on the button.
	 * <li>.toolTip  - to get the toolTip text.
	 * </ul>
	 */
	public MenuAction(ResourceSource resourceSource,
			String resourcePrefix,
			Object uiHandler) {
		this(resourceSource.getResourceString(resourcePrefix+".label"));

		String resName = resourcePrefix+".toolTip";
		String toolTip = resourceSource.getResourceString(resName);
		if (toolTip!=null && !toolTip.equals(resName))
			setToolTipText(toolTip);

		resName = resourcePrefix+".toolPrompt";
		toolPrompt = resourceSource.getResourceString(resName);
		if (resName.equals(toolPrompt))
			toolPrompt = null;	//not defined

		if (uiHandler instanceof ExceptionHandler)
			setExceptionHandler((ExceptionHandler)uiHandler);
		if (uiHandler instanceof ToolPrompter)
			setToolPrompter((ToolPrompter)uiHandler);
	}

	/** Create a menu item. */
	public MenuAction(String label) {
		super(label);
		addActionListener(this);
	}

	/** Set an exception handler. */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/** Set a tool prompt handler. */
	public void setToolPrompter(ToolPrompter toolPrompter) {
		this.toolPrompter = toolPrompter;
		//TBD - need to check to make sure we are not adding twice?
		addFocusListener(this);
		addMouseListener(this);
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

	/** The default action for this menu item.
	 * The subclass usually overrides this method.
	 */
	public void action() {
		/* do nothing */
	}

    //The FocusListener interface
    	public void focusGained(FocusEvent ev) {
		if (toolPrompter!=null && toolPrompt!=null)
			toolPrompter.showToolPrompt(toolPrompt);
	}

    	public void focusLost(FocusEvent ev) {
		if (toolPrompter!=null && toolPrompt!=null)
			toolPrompter.clearToolPrompt();
	}
    //End FocusListener interface

    //The MouseListener interface
    	public void mouseClicked(MouseEvent ev) {}

	public void mouseEntered(MouseEvent ev) {
		if (toolPrompter!=null && toolPrompt!=null)
			toolPrompter.showToolPrompt(toolPrompt);
	}

	public void mouseExited(MouseEvent ev) {
		if (toolPrompter!=null && toolPrompt!=null)
			toolPrompter.clearToolPrompt();
	}

	public void mousePressed(MouseEvent ev) {}

	public void mouseReleased(MouseEvent ev) {}
    //End MouseListener interface
}

/* end */
