/* ClassUtil.java
 *
 * Jim McBeath, October 6, 2001
 */

package net.jimmc.util;

/** Utility methods for dealing with Classes.
 */
public class ClassUtil {
    private ClassUtil(){};	//Can't instantiate this class
    static void testInit() { new ClassUtil(); }; //allow full test coverage

    /** Create a new instance of a named class, verifying that it is
     * a subclass of (or identical to) the given superclass.
     * @param className The name of the class of the instance to create.
     * @param superClass The reference class.
     * @return An instance of className that can be cast to superClass.
     */
    public static Object newInstance(String className, Class superClass) {
	Class cl;

	//Convert the class name to a Class object
	try {
	    cl = Class.forName(className);
	} catch (ClassNotFoundException ex) {
	    throw new MoreException(ex);
	}

	//Make sure the class is a subclass
	if (!superClass.isAssignableFrom(cl)) {
	    //The given class name is not the right subclass
	    String msg = cl.getName() + " != "+superClass.getName();
	    throw new ClassCastException(msg);
	}

	//Create and return an instance of that class
	try {
	    return cl.newInstance();
	} catch (Exception ex) {
	   //InstantiationException, IllegalAccessException
	    throw new MoreException(ex);
	}
    }

    /** Create a new array containing elements of the same typeu
     * as a given array.
     * @param arr The array whose element type we want to copy.
     * @param len The number of elements to create in the new array.
     * @return An empty array of the specified length.
     *         The run-time type of the returned array is the
     *         same as arr.
     */
    public static Object[] newArray(Object[] arr, int len) {
    Class cl = arr.getClass().getComponentType();
    Object[] r = (Object[])java.lang.reflect.Array.newInstance(cl,len);
    	return r;
    }
}

/* end */
