/* App.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import jimmc.swing.AboutWindow;
import jimmc.util.ResourceSource;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** A main class for parsing command line arguments and other application-wide
 * information.
 */
public class App implements ResourceSource {
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
		String aboutTitle = getResourceString("about.title");
		String aboutInfo = getResourceString("about.info");
		AboutWindow.setAboutTitle(aboutTitle);
		AboutWindow.setAboutInfo(aboutInfo);
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
				String msg = getResourceFormatted(
					"error.UnknownOption",eArgs);
				errorExit(msg);
			}
			else if (target!=null) {
				//We only understand one target
				Object[] eArgs = { args[i] };
				String msg = getResourceFormatted(
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

	/** Get a string from a resource file and format it with the given
	 * arg.
	 */
	public String getResourceFormatted(String resName, Object arg){
		String fmt = getResourceString(resName);
		return MessageFormat.format(fmt,new Object[] { arg });
	}

	/** Get a string from a resource file and format it with the given
	 * args.
	 */
	public String getResourceFormatted(String resName, Object[] args){
		String fmt = getResourceString(resName);
		return MessageFormat.format(fmt,args);
	}
}

/* end */
