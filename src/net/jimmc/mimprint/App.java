/* App.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import jimmc.swing.AboutWindow;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** A main class for parsing command line arguments and other application-wide
 * information.
 */
public class App {
	/** The info to display in the About dialog. */
	public static final String aboutInfo =
		"jiviewer\n"+
		"Copyright 2001 Jim McBeath\n"+
		"v0.0.0  September 15, 2001";

	/** Our viewer window. */
	protected Viewer viewer;

	/** The target to view. */
	protected String target;

	/** Our resources. */
	protected ResourceBundle resources;

	/** Program starts here. */
	public static void main(String[] args) {
		App m = new App();
		m.doMain(args);
	}

	/** Run the main stuff. */
	public void doMain(String[] args) {
		initResources();
		AboutWindow.setAboutTitle("About jiviewer");	//TD i18n
		AboutWindow.setAboutInfo(aboutInfo);	//TBD i18n
		viewer = new Viewer(this);
		parseArgs(args);
		if (target==null)
			target = ".";	//default is to display current dir
		viewer.show();		//open the main window
		viewer.open(target);	//display the target
	}

	/** Parse our command line arguments. */
	protected void parseArgs(String[] args) {
		for (int i=0; i<args.length; i++) {
			if (args[i].startsWith("-")) {
				Object[] eArgs = { args[i] };
				String msg = getResourceFormattedString(
					"error.UnknownOption",eArgs);
				errorExit(msg);
			}
			else if (target!=null) {
				//We only understand one target
				Object[] eArgs = { args[i] };
				String msg = getResourceFormattedString(
					"error.UnknownArgument",eArgs);
				errorExit(msg);
			}
			else {
				//Assume it is a target for us to view
				target = args[i];
			}
		}
	}

	/** Deliver an error message, then exit with error status. */
	public void errorExit(String msg) {
		System.out.println(msg);
		System.exit(1);
	}

	/** Get a string from a resource file and format it with the given
	 * args.
	 */
	public String getResourceFormattedString(String resName, Object[] args){
		String fmt = getResourceString(resName);
		return MessageFormat.format(fmt,args);
	}

	/** Set up our resources. */
	public void initResources() {
		resources = ResourceBundle.getBundle(
			"jimmc.jiviewer.JiviewerResources");
	}

	/** Get a string from our resource file. */
	public String getResourceString(String name) {
		try {
			return resources.getString(name);
		} catch (MissingResourceException ex) {
			return name;	//use the resource name
		}
	}
}

/* end */
