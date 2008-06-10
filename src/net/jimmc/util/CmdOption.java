/* CmdOption.java
 *
 * Jim McBeath, October 7, 2001
 */

package net.jimmc.util;

/** A command-line option.
 * @see CmdOptionParser
 */
public abstract class CmdOption {
    /** The name of the option, including the dash if used. */
    protected String name;

    /** The number of arguments used by this option. */
    protected int argCount;

    /** Create an option which takes no arguments. */
    public CmdOption(String name) {
	this(name,0);
    }

    /** Create an option with a specified number of required arguments. */
    public CmdOption(String name, int argumentCount) {
	this.name = name;
	this.argCount = argumentCount;
    }

    /** The action taken when an option is specified.
     * This method is called as soon as the option and its arguments
     * have been parsed.
     * @param args The arguments specified for this option.
     *        The length of this array will match the argumentCount
     *        specified to the constructor.
     *        If this option does not take any
     *        arguments, args may be null.
     */
    public abstract void action(String[] args);

    /** Get the name of this option. */
    public String getName() {
	return name;
    }

    /** Get the number of arguments required for this option. */
    public int getArgumentCount() {
	return argCount;
    }
}

/* end */
