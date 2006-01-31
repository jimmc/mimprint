/* StringPrintWriter.java
 *
 * Jim McBeath, August 22, 2005
 */

package net.jimmc.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/** A PrintWriter that outputs to a string.
 */
public class StringPrintWriter extends PrintWriter {
    /** Create a PrintWriter to output to a String. */
    public StringPrintWriter() {
        super(new StringWriter());
    }

    /** Get the data that has been written to us so far. */
    public String toString() {
        StringWriter sw = (StringWriter)out;
        return sw.toString();
    }
}
