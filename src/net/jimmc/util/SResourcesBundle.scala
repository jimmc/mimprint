/* SResourcesBundle.scala
 *
 * Jim McBeath, June 10, 2008
 */

package net.jimmc.util

import java.text.MessageFormat
import java.util.MissingResourceException
import java.util.ResourceBundle

/** An implementation of SResources that uses a java ResourceBundle.
 * The extending class must call the initResources method before any
 * of the getResource methods are called.
 */
trait SResourcesBundle extends SResources {
    /* See SResources.scala for method comments.  */

    //Our resources, set by a call to initResources.
    private var resources : ResourceBundle = _

    /** Set up resources based on our package name for the specified object.
     * This method should only be called from the extending class.
     */
    protected def initResources(app:AnyRef) {
        val pkgName = app.asInstanceOf[Object].getClass.getPackage.getName
        resources = ResourceBundle.getBundle(pkgName+".Resources")
    }

    def getResourceString(key:String) : String = {
        try {
            resources.getString(key)
        } catch {
            case (ex:MissingResourceException) => key
        }
    }

    /** Like getResourceString, but returns an Option.
     * This is useful if the application wants to check the value.
     */
    def getResourceStringOption(key: String) : Option[String] = {
        try {
            Some(resources.getString(key))
        } catch {
            case (ex:MissingResourceException) => None
        }
    }

    /** Like getResourceString, but uses the string from the resource
     * file as a message format.
     * If the resource key is not found, the resulting string will be
     * the key string, as with getResourceString.
     */
    def getResourceFormatted(key: String, arg: Any) : String = {
        val fmt = getResourceString(key)
        MessageFormat.format(fmt, arg.asInstanceOf[Object])
    }

    /** Like getResourceString, but uses the string from the resource
     * file as a message format.
     * If the resource key is not found, the resulting string will be
     * the key string, as with getResourceString.
     */
    def getResourceFormatted(key: String, args: Array[Any]) : String = {
        val fmt = getResourceString(key)
        MessageFormat.format(fmt, args.asInstanceOf[Array[Object]]:_*)
    }
}
