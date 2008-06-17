package net.jimmc.mimprint

import net.jimmc.util.SResources

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent

class AreaPage(res:SResources) extends JComponent {

    //Until we get PageLayout switched over to scala, use this class
    //as a converter.
    class ResConverter(res:SResources) extends net.jimmc.util.ResourceSource {
        def getResourceString(key:String) = res.getResourceString(key)
        def getResourceFormatted(key:String, args:Array[Object]) =
            res.getResourceFormatted(key, args.asInstanceOf[Array[Object]])
        def getResourceFormatted(key:String, arg:Object) =
            res.getResourceFormatted(key, arg.asInstanceOf[Object])
    }
    private val resCvt = new ResConverter(res)

    private val pageLayout = new PageLayout(resCvt)
    pageLayout.setDefaultLayout()
    private val pageColor = Color.white
    private var showOutlines:Boolean = true
    private var currentArea:AreaImageLayout = _
    private var highlightedArea:AreaLayout = _

    def areaLayout = pageLayout.getAreaLayout()
    def areaLayout_=(a:AreaLayout) = pageLayout.setAreaLayout(a)
    def pageHeight = pageLayout.getPageHeight()
    def pageHeight_==(n:Int) = pageLayout.setPageHeight(n)
    def pageWidth = pageLayout.getPageWidth()
    def pageWidth_==(n:Int) = pageLayout.setPageWidth(n)

    override def paint(g:Graphics) = paint(g,getWidth,getHeight,showOutlines)

    private def paint(g:Graphics, devWidth:Int, devHeight:Int,
            drawOutlines:Boolean) {
        val g2 = g.asInstanceOf[Graphics2D]
        g2.setColor(getBackground)
        g2.fillRect(0,0,devWidth,devHeight)     //clear to background
        scaleAndTranslate(g2,pageWidth,pageHeight,devWidth,devHeight)
            //scale and translate the page to fit the component size
        g2.setColor(pageColor)
        g2.fillRect(0,0,pageWidth,pageHeight)
        g2.setColor(getForeground)
        areaLayout.paint(g2,currentArea,highlightedArea,drawOutlines)
    }

    /** Given an area of specified size in user space, scale it to fit into
     * the given window space, and translate it to center it top/bottom or
     * left/right for whichever dimension is smaller.
     */
    protected def scaleAndTranslate(g2:Graphics2D,
            userWidth:Int, userHeight:Int, windowWidth:Int, windowHeight:Int) {
        val xscale = windowWidth.asInstanceOf[Double] /
                     userWidth.asInstanceOf[Double]
        val yscale = windowHeight.asInstanceOf[Double] /
                     userHeight.asInstanceOf[Double]
        val scale = if (xscale<yscale) xscale else yscale
        if (xscale<yscale)
            g2.translate(0,(yscale-xscale)*userHeight/2)
        else
            g2.translate((xscale-yscale)*userWidth/2,0)
        g2.scale(scale,scale)
    }
}
