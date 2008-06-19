/* AreaPage.scala
 *
 * Jim McBeath, June 17, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.SResources

import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import javax.swing.JComponent

class AreaPage(viewer:SViewer) extends JComponent with Printable {
    protected[mimprint] var controls:AreaPageControls = null

    //Until we get PageLayout switched over to scala, use this class
    //as a converter.
    class ResConverter(res:SResources) extends net.jimmc.util.ResourceSource {
        def getResourceString(key:String) = res.getResourceString(key)
        def getResourceFormatted(key:String, args:Array[Object]) =
            res.getResourceFormatted(key, args.asInstanceOf[Array[Object]])
        def getResourceFormatted(key:String, arg:Object) =
            res.getResourceFormatted(key, arg.asInstanceOf[Object])
    }
    private val resCvt = new ResConverter(viewer)

    private val pageLayout = new PageLayout(resCvt)
    pageLayout.setDefaultLayout()
    private val pageColor = Color.white
    private var showOutlines:Boolean = true
    private var currentArea:AreaImageLayout = _
    var highlightedArea:AreaLayout = _

    private var playList:PlayListS = PlayListS(viewer)
    private var currentIndex:Int = -1

    private var busyCursor:Cursor = _
    private var invisibleCursor:Cursor = _
    private var cursorBusy = false
    private var cursorVisible = true

    setBackground(Color.gray)
    setForeground(Color.black)
    setPreferredSize(new Dimension(425,550))

    initListeners()
    initCursors()
    //TODO - set up drag and drop

    def areaLayout = pageLayout.getAreaLayout()
    def areaLayout_=(a:AreaLayout) = pageLayout.setAreaLayout(a)
    def pageHeight = pageLayout.getPageHeight()
    def pageHeight_=(n:Int) = pageLayout.setPageHeight(n)
    def pageWidth = pageLayout.getPageWidth()
    def pageWidth_=(n:Int) = pageLayout.setPageWidth(n)
    def pageUnit = pageLayout.getPageUnit()
    def pageUnit_=(n:Int) = pageLayout.setPageUnit(n)

    private def initListeners() {
        addKeyListener(new AreaPageKeyListener())
        addMouseListener(new AreaPageMouseListener())
        addMouseMotionListener(new AreaPageMouseMotionListener())
        addComponentListener(new AreaPageComponentListener())
    }

    private def initCursors() {
        val toolkit = getToolkit()
        val blankCursorImage = toolkit.createImage(new Array[Byte](0))
        invisibleCursor = toolkit.createCustomCursor(
            blankCursorImage, new Point(0,0), "invisible")
        busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    }

    override def paint(g:Graphics) = paint(g,getWidth,getHeight,showOutlines)

    def formatPageValue(n:Int) = PageLayout.formatPageValue(n)

    def displayPlayList(playList:PlayListS) {
        /*val n =*/ displayPlayList(areaLayout,playList,0)
        //n is the number of items from the list which are displayed
    }

    //TODO - clean this up once everthing is converted to scala.
    //This method should be in the areaLayout class, and perhaps
    //after I convert that class over to scala I will put it there.
    //listIndex tells where in the list to start for this layout;
    //we return the number of images consumed (i.e. the number of
    //image locations in this layout, if less than the remaining
    //number of images in the list).
    private def displayPlayList(aLayout:AreaLayout, list:PlayListS,
            listIndex:Int):Int = {
        var idx = listIndex
        val subAreaCount = aLayout.getAreaCount
        for (n <- 0 until subAreaCount) {
            aLayout.getArea(n) match {
            case img:AreaImageLayout =>
                //Found a leaf, process only if we have more images
                if (idx < list.size) {
                    val item = list.getItem(idx)
                    val bundle = new ImageBundle(null,
                            new AreaPageImageWindow(img),
                            new java.io.File(item.baseDir,item.fileName),0)
                    img.setImage(bundle)
                    idx = idx + 1
                } else {
                    img.setImage(null)  //clear the image from this area
                }
            case subArea =>
                val dAreaCount = displayPlayList(subArea, list, idx)
                idx = idx + dAreaCount
            }
        }
        idx - listIndex //number of items we used
    }
    //A helper class during the java-to-scala conversion.
    //Once we no longer use ImageBundle, we won't need this class.
    class AreaPageImageWindow(img:AreaImageLayout) extends ImageWindow {
        def createImage(width:Int, height:Int):java.awt.Image =
            AreaPage.this.createImage(width,height)
        def getComponent():java.awt.Component = AreaPage.this
        def getWidth():Int = img.getBounds.width
        def getHeight():Int = img.getBounds.height
        def getToolkit():java.awt.Toolkit = AreaPage.this.getToolkit()
        def requestFocus() {}
        def showText(text:String) {}
        def showImage(imageBundle:ImageBundle, imageInfo:String) {}
        def setCursorBusy(busy:Boolean) {}
    }

    /** Select the image area at the specified location. */
    def selectArea(windowPoint:Point) {
        if (controls!=null)
            controls.selectArea(windowToUser(windowPoint))
        val a:AreaImageLayout = windowToImageArea(windowPoint)
        if (a!=null)
            currentArea = a
        repaint()
    }

    /** Return the area containing the window point,
     * or null if not in an area. */
    def windowToImageArea(windowPoint:Point):AreaImageLayout = {
        val userPoint:Point = windowToUser(windowPoint)
        var aa:AreaLayout = areaLayout
        var bb:AreaLayout = null
        do {
            //Follow the tree down as far as we can
            bb = aa.getArea(userPoint)
            if (bb!=null)
                aa = bb
        } while (bb!=null)
        aa match {
            case aaa:AreaImageLayout => aaa
            case _ => null
        }
    }

    /** Transform a point in window coordinates to a point in user space. */
    private def windowToUser(p:Point):Point = {
        class DPoint(var x:Double, var y:Double)
        val userP = new DPoint(p.x, p.y)
        val xscale = getWidth.asInstanceOf[Double] /
                     pageWidth.asInstanceOf[Double]
        val yscale = getHeight.asInstanceOf[Double] /
                     pageHeight.asInstanceOf[Double]
        val scale = if (xscale<yscale) xscale else yscale
        if (xscale<yscale)
            userP.y = userP.y - (yscale-xscale)*pageHeight/2.0
        else
            userP.x = userP.x - (xscale-yscale)*pageWidth/2.0
        userP.x = userP.x * (1.0/scale)
        userP.y = userP.y * (1.0/scale)
        new Point(userP.x.asInstanceOf[Int], userP.y.asInstanceOf[Int])
    }

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

    /** Set the cursor to a busy cursor. */
    def setCursorBusy(busy:Boolean) {
        cursorBusy = busy
        if (busy) {
            setCursor(busyCursor)
        } else {
            setCursorVisible(cursorVisible)
        }
    }

    /** Make the cursor visible or invisible.
     * If busy-cursor has been set, cursor is always visible.
     */
    def setCursorVisible(visible:Boolean) {
        cursorVisible = visible
        if (cursorBusy)
            return		//busy takes priority over invisible
        if (visible)
            setCursor(null)
        else
            setCursor(invisibleCursor)
    }

    /** Print this page of images. */
    def print() {
	val pJob:PrinterJob = PrinterJob.getPrinterJob()
	val pageFormat:PageFormat = pJob.defaultPage()
	//pageFormat = pJob.validatePage(pageFormat)
	val oldPaper:Paper = pageFormat.getPaper()
        //Check the size of the printer paper to see if it matches the
        //size of the image page.
        val paperScale:Double =      //dimensions of Paper object are in points
            if (pageUnit==PageLayout.UNIT_INCH)
                72.0          //points per inch
            else
                72.0/2.54     //points per cm
        var paperWidth:Double = oldPaper.getWidth()
        var paperHeight:Double = oldPaper.getHeight()
        var paperPageWidth:Double =
                paperWidth*PageLayout.UNIT_MULTIPLIER/paperScale
        var paperPageHeight:Double =
                paperHeight*PageLayout.UNIT_MULTIPLIER/paperScale
        val widthDiff:Double = Math.abs(pageWidth - paperPageWidth)
        val heightDiff:Double = Math.abs(pageHeight - paperPageHeight)
        if (widthDiff>0.5/PageLayout.UNIT_MULTIPLIER ||
                heightDiff>0.5/PageLayout.UNIT_MULTIPLIER) {
            //Ask user if he wants to continue, and whether he wants to
            //scale the output to fill the paper, or print at the same scale
            //as if the right paper was there.
            val introStr =
                    viewer.getResourceString("prompt.Print.PageSizeMismatch")
            val pageSizeArgs = Array(
                pageWidth.asInstanceOf[Double]/PageLayout.UNIT_MULTIPLIER,
                pageHeight.asInstanceOf[Double]/PageLayout.UNIT_MULTIPLIER
            ) ++ Array(
                if (pageUnit==PageLayout.UNIT_INCH) "in" else "cm"
            )
                //The scala compiler complains about the above three data when
                //we try to put then into one array, so we split it into
                //two and concatenate them.
            val pageSizeStr = viewer.getResourceFormatted(
                    "prompt.Print.PageSizeMismatch.PageSize",pageSizeArgs)
            val paperSizeArgs = Array(
                paperPageWidth.asInstanceOf[Double]/PageLayout.UNIT_MULTIPLIER,
                paperPageHeight.asInstanceOf[Double]/PageLayout.UNIT_MULTIPLIER
            ) ++ Array(
                if (pageUnit==PageLayout.UNIT_INCH) "in" else "cm"
            )
            val paperSizeStr = viewer.getResourceFormatted(
                    "prompt.Print.PageSizeMismatch.PaperSize",paperSizeArgs)
            val prompt = introStr+"\n"+pageSizeStr+"\n"+paperSizeStr
            //TODO i18n these remaining strings in the print dialog
            val yesString = "Scale images to fiil paper"
            val noString = "Print at specified page size"
            val cancelString = "Cancel"
            val response =
                    viewer.yncDialog(prompt,yesString,noString,cancelString)
            response match {
            case 0 =>     //yes, scale to fill paper
                          //leave paper size alone
            case 1 =>     //no, use original specified size
                paperWidth = paperWidth * (pageWidth/paperPageWidth)
                paperHeight = paperHeight * (pageHeight/paperPageHeight)
            case 2 =>     //cancel, do nothing
                return
            case _ =>    //cancel, do nothing
                return
            }
            try {
                Thread.sleep(10)
                //Curious bug on MacOSX: after going through the above
                //dialog the first time and selecting cancel, the next
                //time through when selecting Yes or No, the following
                //print dialog just blinks up on the screen momentarily
                //and self-cancels.  Adding this brief sleep seems to
                //cure the problem.
            } catch {
                case ex:InterruptedException => //ignore
            }
        }
	val newPaper:Paper = new PaperNoMargin()
	newPaper.setSize(paperWidth,paperHeight)
	pageFormat.setPaper(newPaper)
	pJob.setPrintable(this,pageFormat)
	if (!pJob.printDialog())
	    return		//cancelled
	try {
	    pJob.print()
	} catch {
            case ex:PrinterException => throw new RuntimeException(ex)
	}
    }

  //The Printable interface
    def print(graphics:Graphics, pageFormat:PageFormat, pageIndex:Int):Int = {
	if (pageIndex!=0)
	    return Printable.NO_SUCH_PAGE
        val paper:Paper = pageFormat.getPaper()
        val paperWidth = paper.getWidth.asInstanceOf[Int]
        val paperHeight = paper.getHeight.asInstanceOf[Int]
	paint(graphics,paperWidth,paperHeight,false)
	Printable.PAGE_EXISTS
    }
  //End Printable interface

    class AreaPageKeyListener extends KeyListener {
        var knownKeyPress = false
        //The KeyListener interface
        def keyPressed(ev:KeyEvent) {
            setCursorVisible(false)	//turn off cursor on any key
            val keyCode = ev.getKeyCode()
            knownKeyPress = true	//assume we know it
            keyCode match {
/* TODO
            case KeyEvent.VK_LEFT =>
                tracker ! PlayListRequestLeft(playList)
            case KeyEvent.VK_RIGHT =>
                tracker ! PlayListRequestRight(playList)
            case KeyEvent.VK_DOWN =>
                tracker ! PlayListRequestDown(playList)
            case KeyEvent.VK_UP =>
                tracker ! PlayListRequestUp(playList)
*/
            case KeyEvent.VK_ESCAPE =>
                requestScreenMode(SViewer.SCREEN_PREVIOUS)
            case KeyEvent.VK_ENTER =>
                viewer ! SViewerRequestActivate(playList)
            case _ =>
                knownKeyPress = false
            }
        }
        def keyReleased(ev:KeyEvent) {
            //val keyCode = ev.getKeyCode()
        }
        def keyTyped(ev:KeyEvent) {
            val ControlL = 'L' - 0100
            val ControlR = 'R' - 0100
            ev.getKeyChar() match {
            case ' ' =>   //activate selection
                viewer ! SViewerRequestActivate(playList)
            case 'a' =>    //alternate-screen
                requestScreenMode(SViewer.SCREEN_ALT)
            case 'f' =>    //full-screen
                requestScreenMode(SViewer.SCREEN_FULL)
            case ControlL =>    //control-L, refresh
                //showCurrentImage()               //TODO
            case 'e' =>
                viewer ! SViewerRequestEditDialog(playList,currentIndex)
            case 'i' =>
                viewer ! SViewerRequestInfoDialog(playList,currentIndex)
            case 'o' =>    //file-open dialog
                viewer ! SViewerRequestFileOpen()
            case 'p' =>   //add current image to active or printable playlist
                viewer ! SViewerRequestAddToActive(playList,currentIndex)
            case 'P' =>    //the print screen
                requestScreenMode(SViewer.SCREEN_PRINT)
/* TODO
            case 'r' =>    //rotate CCW
                viewer.rotateCurrentImage(1);
            case 'R' =>    //rotate CW
                viewer.rotateCurrentImage(-1);
            case ControlR =>    //control-R, rotate 180
                viewer.rotateCurrentImage(2);
*/
            case 's' =>    //the slideshow screen
                requestScreenMode(SViewer.SCREEN_SLIDESHOW)
            case 'x' =>    //exit
                viewer ! SViewerRequestClose()
            case '?' =>
                showHelpDialog()
            case 0177 =>                  //delete
                clearCurrentArea
            case 8 =>                     //backspace
                clearCurrentArea
            case ch:Char =>        //unknown key
                if (!knownKeyPress) {
                    System.out.println("Unknown key "+ch+" ("+
                        ch.asInstanceOf[Int]+")")
                    getToolkit().beep()
                }
            }
        }
        //End KeyListener interface

        def requestScreenMode(mode:Int) =
            viewer ! SViewerRequestScreenMode(mode)

        private def clearCurrentArea = {
            if (currentArea!=null) {
                //clear image from current area
                currentArea.setImage(null)
                //repaintCurrentImage() //TODO
            }
        }

        private def showHelpDialog() {
            val helpText = viewer.getResourceString("info.ImageHelp")
            viewer.invokeUi {
                viewer.infoDialog(helpText)
            }
        }

    }

    class AreaPageMouseListener extends MouseAdapter {
        override def mousePressed(ev:MouseEvent) {
            requestFocus()
            selectArea(new Point(ev.getX(),ev.getY()))
        }
    }

    class AreaPageMouseMotionListener extends MouseMotionListener {
        //The MouseMotionListener interface
        def mouseDragged(ev:MouseEvent){
            setCursorVisible(true) //turn cursor back on
        }
        def mouseMoved(ev:MouseEvent){
            setCursorVisible(true) //turn cursor back on
        }
        //End MouseMotionListener interface
    }

    class AreaPageComponentListener extends ComponentListener {
        //The ComponentListener interface
        def componentHidden(ev:ComponentEvent){}
        def componentMoved(ev:ComponentEvent){}
        def componentResized(ev:ComponentEvent){
            //app.debugMsg("componentResized")
            //repaint()
        }
        def componentShown(ev:ComponentEvent){}
        //End ComponentListener interface
    }

    class PaperNoMargin extends Paper {
        //Force our paper's imageable area to the full size
        override def getImageableHeight():Double = getHeight()
        override def getImageableWidth():Double = getWidth()
        override def getImageableX():Double = 0.0
        override def getImageableY():Double = 0.0
    }
}
