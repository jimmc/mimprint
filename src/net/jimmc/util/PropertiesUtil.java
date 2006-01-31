/* PropertiesUtil.java
 *
 * Jim McBeath, October 6, 2001
 */

package net.jimmc.util;

import java.util.Properties;

/** Utility methods dealing with Properties.
 */
public class PropertiesUtil {
    /** Don't instantiate this class. */
    protected PropertiesUtil(){};	//available for coverage test

    /** Set a system property. */
    public static void setSystemProperty(String name, String value) {
	Properties sysProps = System.getProperties();
	sysProps.setProperty(name,value);
	System.setProperties(sysProps);
    }

    /** Set a system property.
     * @param nameValue A string with the property name, and equals sign,
     *        and the value of the property.  If there is no equals sign,
     *        then the value is set to the string "1".
     */
    public static void setSystemProperty(String nameValue) {
	if (nameValue==null || nameValue.equals(""))
	    return;		//ignore blanks
	int eq = nameValue.indexOf('=');
	if (eq<0) {
	    //No equals in the string, use value of "1"
	    setSystemProperty(nameValue,"1");
	    return;
	}
	String name = nameValue.substring(0,eq);
	String value = nameValue.substring(eq+1);
	setSystemProperty(name,value);
    }
}

/* end */
