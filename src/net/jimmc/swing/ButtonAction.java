/* ButtonAction.java
 *
 * Jim McBeath, June 7, 1998
 */

package net.jimmc.swing;

import net.jimmc.util.ExceptionHandler;
import net.jimmc.util.ResourceSource;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.AbstractButton;
import javax.swing.JButton;

/** An adapter class to simplify defining buttons.
 */
public class ButtonAction extends JButton implements ActionListener {
    /** An exception handler. */
    protected ExceptionHandler exceptionHandler;

    /** Create a button using resources, with an exception handler.
     * @param resourceSource The source for resource strings.
     * @param resourcePrefix The prefix to use to build the resource
     *        string we use.  We append the following to the prefix:
     * @param exceptionHandler The handler to deal with exceptions.
     * <ul>
     * <li>.label - the string to display on the button.
     * <li>.icon - the path to the file containing the icon for the button.
     * <li>.toolTip  - the toolTip text.
     * </ul>
     */
    public ButtonAction(ResourceSource resourceSource,
            String resourcePrefix, ExceptionHandler exceptionHandler) {
        this(resourceSource.getResourceString(resourcePrefix+".label"));

        String resName = resourcePrefix+".toolTip";
        String toolTip = resourceSource.getResourceString(resName);
        if (toolTip!=null && !toolTip.equals(resName))
            setToolTipText(toolTip);

        String iconResName = resourcePrefix+".icon";
        String iconName = resourceSource.getResourceString(iconResName);
        if (iconName!=null && !iconName.equals(iconResName)) {
            setIcon(loadIcon(iconName));
            //Make the text sit under the icon
            setVerticalTextPosition(AbstractButton.BOTTOM);
            setHorizontalTextPosition(AbstractButton.CENTER);
            if (resourceSource.getResourceString(resourcePrefix+".label").
                    equals(resourcePrefix+".label")) {
                //No label string specified, but since we have an icon,
                //that's OK.  Set the label to null.
                setText(null);
            }
        }

        setExceptionHandler(exceptionHandler);
    }

    public ButtonAction(ResourceSource resourceSource, String resourcePrefix) {
        this(resourceSource, resourcePrefix, null);
    }

    /** Create a button. */
    public ButtonAction(String label) {
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

    private ImageIcon loadIcon(String iconName) {
        Class cl = this .getClass();
        URL url = cl.getResource(iconName);
        if (url==null)
            return null;
        return new ImageIcon(url);
    }
}

/* end */
