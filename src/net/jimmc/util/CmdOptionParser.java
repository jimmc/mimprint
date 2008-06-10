/* CmdOptionParser.java
 *
 * Jim McBeath, October 7, 2001
 */

package net.jimmc.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Parse options and arguments from an array of strings.
 * @see CmdOption
 */
public class CmdOptionParser {
    /** Our defined options. */
    protected Map options;

    /** Our resource source. */
    protected ResourceSource res;

    /** Create an option parser.
     * After this, the application typically calls
     * {@link #addOption} for each defined option, then
     * {@link #parseOptions} to parse the given options and execute
     * the action method of any specified option.
     */
    public CmdOptionParser(ResourceSource resourceSource) {
	options = new HashMap();
	this.res = resourceSource;
    }

    /** Define an option. */
    public void addOption(CmdOption option) {
	String name = option.getName();
	name = name.toLowerCase();
	options.put(name,option);
    }

    /** Get an option given its name. */
    public CmdOption getOption(String name) {
	return (CmdOption)options.get(name);
    }

    /** Get the names of all options. */
    public String[] getOptionNames() {
	String[] names =
	    (String[])options.keySet().toArray(new String[0]);
	Arrays.sort(names);
	return names;
    }

    /** Parse the given options. */
    public void parseOptions(String[] args) {
	int i = 0;
	while (i<args.length) {
	    String name = args[i];
	    CmdOption option = (CmdOption)options.get(name.toLowerCase());
	    if (option==null) {
		String resName;
		if (name.startsWith("-"))
		    resName = "error.UnknownOption";
		else
		    resName = "error.UnknownArgument";
		String msg = res.getResourceFormatted(
		    resName, name);
		throw new RuntimeException(msg);
	    }
	    i++;	//point to first arg
	    int argCount = option.getArgumentCount();
	    if (i+argCount>args.length) {
		String msg = res.getResourceFormatted(
		    "error.NotEnoughArguments", name);
		throw new RuntimeException(msg);
	    }
	    String[] optArgs = new String[argCount];
	    System.arraycopy(args,i,optArgs,0,argCount);
	    option.action(optArgs);
	    i += argCount;
	}
    }
}

/* end */
