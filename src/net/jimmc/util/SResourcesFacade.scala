package net.jimmc.util

/** An implementation of SResources that passes the requiests through
 * to another instance of SResources.
 * The extending class must define a value for sResourcesBase.
 */
trait SResourcesFacade extends SResources {
    /* See SResources.scala for method comments. */

    //Extending class must define this value
    protected val sResourcesBase : SResources

    def getResourceString(key:String) =
            sResourcesBase.getResourceString(key)

    def getResourceStringOption(key: String) =
            sResourcesBase.getResourceStringOption(key)

    def getResourceFormatted(key: String, arg: Any) =
            sResourcesBase.getResourceFormatted(key, arg)

    def getResourceFormatted(key: String, args: Array[Any]) =
            sResourcesBase.getResourceFormatted(key, args)
}
