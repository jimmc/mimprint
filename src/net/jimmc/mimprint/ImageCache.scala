/* ImageCache.scala
 *
 * Jim McBeath, March 6, 2009
 */

package net.jimmc.mimprint

import java.awt.Image

object ImageCache {
    case class PathImage(path:String, image:Image)

    var pathCache:List[PathImage] = Nil

    def getPathImage(path:String):Option[PathImage] =
        pathCache find (_.path==path)

    def cachePathImage(path:String, image:Image) {
        val pi = PathImage(path, image)
        pathCache = pi :: (pathCache take 3)     //limit size to 4
    }


    case class ScaledImage(srcImage:Image,
            dstWidth:Int, dstHeight:Int, scaledImage:Image)

    var scaledCache:List[ScaledImage] = Nil

    //Get an scaled version of the given image file from our cache, if there.
    def getScaledImage(srcImage:Image, dstWidth:Int, dstHeight:Int):
                Option[ScaledImage] =
        scaledCache find (si =>
                    si.srcImage==srcImage &&
                    si.dstWidth==dstWidth && si.dstHeight==dstHeight)

    def cacheScaledImage(srcImage:Image, dstWidth:Int, dstHeight:Int,
            scaledImage:Image) {
        val si = ScaledImage(srcImage, dstWidth, dstHeight, scaledImage)
        scaledCache = si :: (scaledCache take 3)     //limit size to 4
    }
}
