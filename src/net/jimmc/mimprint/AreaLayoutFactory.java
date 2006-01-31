/* AreaLayoutFactory.java
 *
 * Jim McBeath, Nov 7, 2005
 */

package net.jimmc.jiviewer;

public class AreaLayoutFactory {
    //TODO use a singleton of this class and make the method not static
    /** Create an empty AreaLayout of the specifed type. */
    public static AreaLayout newAreaLayout(String type) {
        if (type.equals("imageLayout"))
            return new ImagePageArea();
        else if (type.equals("gridLayout"))
            return new AreaGridLayout();
        else if (type.equals("splitLayout"))
            return new AreaSplitLayout();
        else
            throw new IllegalArgumentException("Unknown area type "+type); //TODO i18n
    }
}
