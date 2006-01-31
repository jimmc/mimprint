/* ResourceSource.java
 *
 * Jim McBeath, October 7, 2001
 */

package net.jimmc.util;

/** A source for strings from resource files. */
public interface ResourceSource {
    /** Get a string from resources.
     * @param name The resource name.
     * @return The value of the resource,
     *         or the name if no value is found.
     */
    public String getResourceString(String name);

    /** Get a string from a resource and pass it to MessageFormat.format
     * with the specified arguments, returning the result.
     * @param name The resource name.
     * @param args The args to MessageFormat.format.
     * @return The formatted resource string.
     */
    public String getResourceFormatted(String name, Object[] args);

    /** Get a string from a resource and pass it to MessageFormat.format
     * with the specified one argument put into a new Object[1],
     * returning the result.
     * @param name The resource name.
     * @param arg The argument to MessageFormat.format.
     * @return The formatted resource string.
     */
    public String getResourceFormatted(String name, Object arg);
}

/* end */
