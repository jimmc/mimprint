/* App.java
 *
 * Jim McBeath, September 18, 2001
 */

package net.jimmc.mimprint;

import net.jimmc.swing.AboutWindow;
import net.jimmc.util.ResourceSource;

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

    /** True to use a big font for dialogs.
     * This is useful when we are watching on a TV screen.
     * @see #useBigFont
     */
    protected boolean bigFontP;

    /** True to use lookahead (default).
     * @see #useLookAhead
     */
    protected boolean lookAheadP=true;

    /** True if the -debug command line option was specified.
     * @see #debug
     */
    protected boolean debugP;

    /** The time we started running. */
    protected long startTime;

        private ImageUtil imageUtil;

    /** Program starts here. */
    public static void main(String[] args) {
        App m = new App();
        m.doMain(args);
    }

    /** Run the main stuff. */
    public void doMain(String[] args) {
        startTime = System.currentTimeMillis();
        initResources();
        String aboutTitle = getResourceString("about.title");
        String aboutInfo = getResourceString("about.info");
        AboutWindow.setAboutTitle(aboutTitle);
        AboutWindow.setAboutInfo(aboutInfo);
        parseArgs(args);
        if (target==null)
            target = ".";    //default is to display current dir
        viewer = new Viewer(this);
        imageUtil = new ImageUtil(this,viewer);
        viewer.show();        //open the main window
        viewer.open(target);    //display the target
    }

    /** Parse our command line arguments. */
    protected void parseArgs(String[] args) {
        for (int i=0; i<args.length; i++) {
            if (args[i].equalsIgnoreCase("-bigFont")) {
                bigFontP = true;
            }
            else if (args[i].equalsIgnoreCase("-debug")) {
                debugP = true;
            }
            else if (args[i].equalsIgnoreCase("-help")) {
                //Print out the help text
                String help = getResourceString(
                    "info.CommandHelp");
                System.out.println(help);
                System.exit(0);
            }
            else if (args[i].equalsIgnoreCase("-noLookAhead")) {
                lookAheadP = false;
            }
            else if (args[i].startsWith("-")) {
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

        /** Get our ImageUtil object. */
        public ImageUtil getImageUtil() {
            return imageUtil;
        }

    /** True if we should be using a big font. */
    public boolean useBigFont() {
        return bigFontP;
    }

    /** True if we should use lookahead when loading images. */
    public boolean useLookAhead() {
        return lookAheadP;
    }

    /** True if we are debugging. */
    public boolean debug() {
        return debugP;
    }

    /** Print out a debugging message if the debug flag is on. */
    public void debugMsg(String s) {
        if (debug()) {
            long now = System.currentTimeMillis();
            long delta = now - startTime;
            System.out.print(delta);
            System.out.print(" ");
            System.out.println(s);
        }
    }

    /** Deliver an error message, then exit with error status. */
    public void errorExit(String msg) {
        System.out.println(msg);
        System.exit(1);
    }

    /** Set up our resources. */
    public void initResources() {
                //Look for our resources in the same package
                String pkgName = this.getClass().getPackage().getName();
        resources = ResourceBundle.getBundle(
            pkgName+".Resources");
    }

    /** Get a string from our resource file. */
    public String getResourceString(String name) {
        try {
            return resources.getString(name);
        } catch (MissingResourceException ex) {
            return name;    //use the resource name
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
