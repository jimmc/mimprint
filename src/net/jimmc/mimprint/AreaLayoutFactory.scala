/* AreaLayoutFactory.java
 *
 * Jim McBeath, Nov 7, 2005
 * Converted to scala June 21, 2008
 */

package net.jimmc.mimprint

object AreaLayoutFactory {
    /** Create an empty AreaLayout of the specifed type. */
    def newAreaLayout(layoutType:String):AreaLayout = {
        layoutType match {
            case "imageLayout" => new AreaImageLayout()
            case "gridLayout"  => new AreaGridLayout()
            case "splitLayout" => new AreaSplitLayout()
            case _ =>
                throw new IllegalArgumentException(
                        "Unknown area type "+layoutType); //TODO i18n
        }
    }

    def createDefaultTopLayout():AreaLayout = new AreaImageLayout(0,0,0,0)
}
