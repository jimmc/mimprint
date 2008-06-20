/* SResources.scala
 *
 * Jim McBeath, June 10, 2008
 */

package net.jimmc.util

trait SResources {
    /** Get a string from our resource file.
     * If no value is found, we return the key.
     * This works reasonably well when the value is being displayed
     * in a GUI: if the resource key is not found, it appears in the GUI,
     * allowing the developer to quickly see the problematic key.
     * @param key The resource key.
     * @return The value from the resource file, or the key if the value
     *          was not found.
     */
    def getResourceString(key:String) : String

    /** Like getResourceString, but returns an Option.
     * This is useful if the application wants to check the value.
     */
    def getResourceStringOption(key: String) : Option[String]

    /** Like getResourceString, but uses the string from the resource
     * file as a message format.
     * If the resource key is not found, the resulting string will be
     * the key string, as with getResourceString.
     */
    def getResourceFormatted(key: String, arg: Any) : String

    /** Like getResourceString, but uses the string from the resource
     * file as a message format.
     * If the resource key is not found, the resulting string will be
     * the key string, as with getResourceString.
     */
    def getResourceFormatted(key: String, arg: Array[Any]) : String
}
