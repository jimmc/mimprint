/* AreaPage.scala
 *
 * Jim McBeath, June 17, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.SDragSource
import net.jimmc.util.SResources

import java.awt.Color
import java.awt.Cursor
import java.awt.datatransfer.DataFlavor
import java.awt.Dimension
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext
import java.awt.dnd.DragSourceDragEvent
import java.awt.dnd.DragSourceDropEvent
import java.awt.dnd.DragSourceEvent
import java.awt.dnd.DragSourceListener
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.File
import java.io.PrintWriter
import java.io.StringReader
import javax.swing.JComponent

class AreaPage(viewer:SViewer, tracker:PlayListTracker)
        extends JComponent with Printable
        with SDragSource {
    protected[mimprint] var controls:AreaPageControls = null

    private var pageLayout = new PageLayout(viewer)
    pageLayout.setDefaultLayout()
    private val pageColor = Color.white
    private var showOutlines:Boolean = true
    private var currentArea:AreaImageLayout = _
    var highlightedArea:AreaLayout = _

    private var playList:PlayListS = PlayListS(viewer)
    private var currentIndex:Int = -1
        //index of the currentArea

    private var dragArea:AreaImageLayout = _

    private val dropActions:Int = DnDConstants.ACTION_COPY_OR_MOVE
    private val dropTargetListener:DropTargetListener =
        new AreaPageDropTargetListener()
    private val myDropTarget:DropTarget = null
        new DropTarget(this, dropActions, dropTargetListener, true)

    private var busyCursor:Cursor = _
    private var invisibleCursor:Cursor = _
    private var cursorBusy = false
    private var cursorVisible = true

    setBackground(Color.gray)
    setForeground(Color.black)
    setPreferredSize(new Dimension(425,550))

    initListeners()
    initCursors()
    setupDrag(this,DnDConstants.ACTION_COPY_OR_MOVE)

    def areaLayout = pageLayout.getAreaLayout()
    def areaLayout_=(a:AreaLayout) = pageLayout.setAreaLayout(a)
    def pageHeight = pageLayout.getPageHeight()
    def pageHeight_=(n:Int) = pageLayout.setPageHeight(n)
    def pageWidth = pageLayout.getPageWidth()
    def pageWidth_=(n:Int) = pageLayout.setPageWidth(n)
    def pageUnit = pageLayout.getPageUnit()
    def pageUnit_=(n:Int) = pageLayout.setPageUnit(n)

    def fixImageIndexes() = pageLayout.fixImageIndexes

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

    def formatPageValue(n:Int) = PageValue.formatPageValue(n)

    def displayPlayList(playList:PlayListS) {
        /*val n =*/ displayPlayList(areaLayout,playList,0)
        //n is the number of items from the list which are displayed
        this.playList = playList
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
        //Check for a top-level AreaImageLayout
        aLayout match {
            case img:AreaImageLayout =>
                if (idx < list.size) {
                    val item = list.getItem(idx)
                    if (item.fileName==null || item.fileName=="") {
                        //Found an empty item
                        img.unsetImage()
                    } else {
                        img.setImage(item,this)
                    }
                    return 1
                } else {
                    img.unsetImage()  //clear the image from this area
                    return 0
                }
            case _ =>   //fall through and process below
        }
        val subAreaCount = aLayout.getAreaCount
        for (n <- 0 until subAreaCount) {
            val subArea = aLayout.getArea(n)
            val dAreaCount = displayPlayList(subArea, list, idx)
            idx = idx + dAreaCount
        }
        idx - listIndex //number of items we used
    }

    /** Select the image area at the specified location. */
    def selectArea(windowPoint:Point) {
        if (controls!=null)
            controls.selectArea(windowToUser(windowPoint))
        val a:AreaImageLayout = windowToImageArea(windowPoint)
        if (a!=null) {
            currentArea = a
            currentIndex = a.getImageIndex
            val msg = "ImageIndex="+currentIndex
                //TODO better message; add image path
            viewer.showStatus(msg)
        } else {
            viewer.showStatus("")
        }
        repaint()
    }

    private def repaintCurrentImage() {
        if (currentArea!=null) {
            //TODO - should scale and translate the area to get the part
            //of the page that needs to be refreshed.
            repaint()   //for now, just do the whole thing
        }
    }

    //Set the currently highlighted area (the drop target)
    protected def setHighlightedArea(a:AreaLayout) {
        if (a==highlightedArea)
            return             //already set
        highlightedArea = a
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

    protected def getMyDragGestureListener():DragGestureListener =
        new AreaPageDragGestureListener()

    protected def getMyDragSourceListener():DragSourceListener =
        new AreaPageDragSourceListener()

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
                paperWidth*PageValue.UNIT_MULTIPLIER/paperScale
        var paperPageHeight:Double =
                paperHeight*PageValue.UNIT_MULTIPLIER/paperScale
        val widthDiff:Double = Math.abs(pageWidth - paperPageWidth)
        val heightDiff:Double = Math.abs(pageHeight - paperPageHeight)
        if (widthDiff>0.5/PageValue.UNIT_MULTIPLIER ||
                heightDiff>0.5/PageValue.UNIT_MULTIPLIER) {
            //Ask user if he wants to continue, and whether he wants to
            //scale the output to fill the paper, or print at the same scale
            //as if the right paper was there.
            val introStr =
                    viewer.getResourceString("prompt.Print.PageSizeMismatch")
            val pageSizeArgs = Array(
                pageWidth.asInstanceOf[Double]/PageValue.UNIT_MULTIPLIER,
                pageHeight.asInstanceOf[Double]/PageValue.UNIT_MULTIPLIER
            ) ++ Array(
                if (pageUnit==PageLayout.UNIT_INCH) "in" else "cm"
            )
                //The scala compiler complains about the above three data when
                //we try to put then into one array, so we split it into
                //two and concatenate them.
            val pageSizeStr = viewer.getResourceFormatted(
                    "prompt.Print.PageSizeMismatch.PageSize",pageSizeArgs)
            val paperSizeArgs = Array(
                paperPageWidth.asInstanceOf[Double]/PageValue.UNIT_MULTIPLIER,
                paperPageHeight.asInstanceOf[Double]/PageValue.UNIT_MULTIPLIER
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
                case KeyEvent.VK_LEFT => viewer.requestLeft
                case KeyEvent.VK_RIGHT => viewer.requestRight
                case KeyEvent.VK_DOWN => viewer.requestDown
                case KeyEvent.VK_UP => viewer.requestUp
                case KeyEvent.VK_ESCAPE =>
                    requestScreenMode(SViewer.SCREEN_PREVIOUS)
                //case KeyEvent.VK_ENTER => viewer.requestAddToActive
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
            //case ' ' =>   //activate selection
                //viewer ! SViewerRequestActivate(playList)
            case 'a' =>    //alternate-screen
                requestScreenMode(SViewer.SCREEN_ALT)
            case 'f' =>    //full-screen
                requestScreenMode(SViewer.SCREEN_FULL)
            case ControlL =>    //control-L, refresh
                //refresh from our playlist and repaint the screen
                displayPlayList(playList)
                repaint()
            /*
            case 'e' =>
                viewer ! SViewerRequestEditDialog(playList,currentIndex)
            case 'i' =>
                viewer ! SViewerRequestInfoDialog(playList,currentIndex)
            case 'o' =>    //file-open dialog
                viewer ! SViewerRequestFileOpen()
            */
            case 'p' =>   //add current image to active or printable playlist
                viewer.requestAddToActive
            case 'P' =>    //the print screen
                requestScreenMode(SViewer.SCREEN_PRINT)
            case 'r' =>    //rotate 180
                rotateCurrentImage(2);
/*
We ignore the low-order bit of the image rotation and only allow rotation
in an image area by 180 degrees, so we just use the r key for that.
            case 'R' =>    //rotate CW
                rotateCurrentImage(-1);
            case ControlR =>    //control-R, rotate 180
                rotateCurrentImage(2);
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

        def rotateCurrentImage(rot:Int) {
            if (currentArea!=null && currentArea.hasImage) {
                //currentArea.rotate(rot)
                //repaintCurrentImage()
                tracker ! PlayListRequestRotate(playList, currentIndex, rot)
            }
        }

        private def clearCurrentArea = {
            if (currentArea!=null && currentArea.hasImage) {
                //clear image from current area
                val item = PlayItemS.emptyItem()
                tracker ! PlayListRequestChange(playList, currentIndex, item)
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

  //Drag-and-drop stuff

    class AreaPageDragGestureListener extends DragGestureListener {
      //The DragGestureListener interface
        def dragGestureRecognized(ev:DragGestureEvent) {
            val p:Point = ev.getDragOrigin()
            val a:AreaImageLayout = windowToImageArea(p)
            dragArea = a
            if (a==null)
                return     //not in an area, can't start a drag here
            val path = a.path
            if (path==null)
                return     //no image in this area, can't start a drag
            val fullImage:Image =
                if (DragSource.isDragImageSupported())
                    a.image
                else
                    null   //image dragging not supported
            var image:Image = null     //image to drag
            var offset:Point = null
            if (fullImage!=null) {
                image = SImageUtil.createTransparentIconImage(
                        AreaPage.this,fullImage,path)
                SImageUtil.loadCompleteImage(AreaPage.this,image)
                val width = image.getWidth(null)
                val height = image.getHeight(null)
                offset = new Point(-width/2, -height/2)
            }
            startImageDrag(ev, image, offset, path)
        }
      //End DragGestureListener interface
    }

    class AreaPageDragSourceListener extends DragSourceListener {
      //The DragSourceListener interface
        def dragEnter(ev:DragSourceDragEvent) {
            //println("AreaSource dragEnter")
            setDragCursor(ev)
        }
        def dragOver(ev:DragSourceDragEvent) {
            //println("AreaSource dragOver")
            setDragCursor(ev)
        }
        def dragExit(ev:DragSourceEvent) {
            //println("AreaSource DragSourceListener dragExit")
        }
        def dragDropEnd(ev:DragSourceDropEvent) {
            dragArea = null
            setHighlightedArea(null)
            if (!ev.getDropSuccess()) {
                println("AreaSource DragSource DragDropEnd drop failed")
                return
            }
            val dropAction:Int = ev.getDropAction()
            if (dropAction==DnDConstants.ACTION_COPY)
                println("AreaPage DragSource DragDropEnd Copy")
            else if (dropAction==DnDConstants.ACTION_MOVE)
                println("AreaPage DragSource DragDropEnd Move")
            else
                println("DragDropEnd no action")
            //TODO - need to do anything here to handle the drag end?
        }
        def dropActionChanged(ev:DragSourceDragEvent) { }
      //End DragSourceListener interface
    }

    private def setDragCursor(ev:DragSourceDragEvent) {
        val ctx:DragSourceContext = ev.getDragSourceContext()
        val action:Int = ev.getDropAction()
        ctx.setCursor(null)
        if ((action & DnDConstants.ACTION_COPY)!=0) {
            //println("cursor Copy")
            ctx.setCursor(DragSource.DefaultCopyDrop)
            //TODO - we are getting here, but the cursor does not change.
        } else if ((action & DnDConstants.ACTION_MOVE)!=0) {
            //println("cursor Move")
            ctx.setCursor(DragSource.DefaultMoveDrop)
        } else {
            //System.out.println("cursor NoCopy")
            ctx.setCursor(DragSource.DefaultCopyNoDrop)
            //TODO - or MoveNoDrop?
        }
    }

    class AreaPageDropTargetListener extends DropTargetListener {
        private def checkDrop(ev:DropTargetDragEvent) {
            //printFlavors(ev)       //for debug
            val a:AreaImageLayout = getDropArea(ev)
            if (a==null || a==dragArea) {
                //No drop in source area or outside any area
                setHighlightedArea(null)
                //println("reject drag")
                ev.rejectDrag()
                return
            }
            setHighlightedArea(a)
            //println("accept drag")
            ev.acceptDrag(AreaPage.this.dropActions)
        }
      //The DropTargetListener interface
        def dragEnter(ev:DropTargetDragEvent) {
            checkDrop(ev)
        }
        def dragOver(ev:DropTargetDragEvent) {
            checkDrop(ev)
        }
        def dropActionChanged(ev:DropTargetDragEvent) {
            checkDrop(ev)
        }
        def dragExit(ev:DropTargetEvent) {
            setHighlightedArea(null)
            //println("DropTargetListener dragExit")
        }

        def drop(ev:DropTargetDropEvent) {
            //println("dropped")
            val flavors:Array[DataFlavor] = getDropFlavors()
            var chosenFlavor:DataFlavor = null
            flavors.find(ev.isDataFlavorSupported(_)) match {
                case Some(f) => chosenFlavor = f
                case None =>
                    ev.dropComplete(false)
                    return       //no support for any flavors
            }

            val sourceActions:Int = ev.getSourceActions()
            if ((sourceActions & AreaPage.this.dropActions)==0) {
                ev.dropComplete(false)
                return       //no actions available
            }

            val dropArea:AreaImageLayout = windowToImageArea(ev.getLocation())
            if (dropArea==null) {
                ev.dropComplete(false) //bad area
                return       //no actions available
            }

            var data:Object = null
            try {
                ev.acceptDrop(AreaPage.this.dropActions)
                data = ev.getTransferable().getTransferData(chosenFlavor)
                if (data==null) {
                    throw new IllegalArgumentException("No drop data")
                }
            } catch {
                case ex:Exception =>
                    println("Exception accepting drop: "+ex)
                    ev.dropComplete(false)
                    return
            }

            data match {
            case s:String =>
                if (dropFileName(s,dropArea)) {
                    ev.dropComplete(true)
                    return     //success
                }
            case rd:StringReader =>
                val cbuf = new Array[Char](2000)
                val n = rd.read(cbuf, 0, cbuf.length)
                val s = new String(cbuf,0,n)
                if (dropFileName(s,dropArea)) {
                    ev.dropComplete(true)
                    return     //success
                }
            case fList:java.util.List[Object] =>
                if (fList.size()>=1) {
                    val list0:Object = fList.get(0)
                    list0 match {
                    case ls:String =>
                        if (dropFileName(ls,dropArea)) {
                            ev.dropComplete(true)
                            return
                        }
                    case f:File =>
                        if (dropFileName(f.toString(),dropArea)) {
                            ev.dropComplete(true)
                            return
                        }
                    case _ =>
                        println("List item 0 is not a string")
                        println("List item 0 class is "+list0.getClass().getName())
                        println("List item 0 data is "+list0.toString())
                    }
                } else {
                    println("List is empty")
                }
            case _ =>
                //can't deal with this yet
                println("drop data class is "+data.getClass().getName())
                println("drop data is "+data.toString())
            }
            //not processed
            println("Rejecting drop")
            ev.dropComplete(false)
        }
      //End DropTargetListener interface
    }

    /** Drop a file into the currently selected area.
     * @param s The full path to the image file.
     * @return True if the file exists, false if not.
     */
    private def dropFileName(s0:String, dropArea:AreaImageLayout):Boolean = {
//println("Got drop fileName: "+s0)
        var s = s0
	if (s.startsWith("file://"))
	    s = s.substring("file://".length()) //convert URL to file path
	while ((s.endsWith("\n"))||s.endsWith("\r"))
	    s = s.substring(0,s.length()-1) //drop trailing newline
        val f = new File(s)
        if (!f.exists()) {
            println("No such file '"+s+"'")
            return false
        }
        if (f.isDirectory()) {
            println(s+" is a directory")
            return false
        }
        currentArea = dropArea
        currentIndex = currentArea.getImageIndex
        val item = new PlayItemS(Nil,f.getParentFile,f.getName,0)
        tracker ! PlayListRequestSetItem(playList,dropArea.getImageIndex,item)
        //repaintCurrentImage()
            //replaint later when the message comes back to us
//println("Accepted drop of file "+f)
        //TODO - add call to viewer.setStatus, but need to figure out when
        //to clear the status first
        true
    }

    /** Get the flavors we support for drop. */
    private def getDropFlavors():Array[DataFlavor] = {
        Array(
            DataFlavor.stringFlavor,
            DataFlavor.plainTextFlavor,
            DataFlavor.javaFileListFlavor
        )
    }

    private def getDropArea(ev:DropTargetDragEvent):AreaImageLayout = {
        val flavors = getDropFlavors()
        var chosenFlavor:DataFlavor = null
        flavors.find(ev.isDataFlavorSupported(_)) match {
            case Some(f) => chosenFlavor = f
            case None =>
                println("No supported flavor")
                return null       //no support for any flavors
        }

        val sourceActions = ev.getSourceActions()
        if ((sourceActions & AreaPage.this.dropActions)==0) {
            println("Action not supported "+sourceActions)
            return null       //no actions available
        }

        val p = ev.getLocation()
        val a = windowToImageArea(p)
        //println("area "+a)
        a               //null if no area
    }

    private def printFlavors(ev:DropTargetDragEvent) {
        val flavors = ev.getCurrentDataFlavors()
        flavors.foreach { flavor =>
            println("drop flavor "+flavor)
        }
    }

  //End Drag-and-drop stuff
   
    private def setPageLayout(newPageLayout:PageLayout) {
        pageLayout = newPageLayout
        highlightedArea = null
        currentArea = null
        controls.updateAllAreasList()
        controls.selectArea(new Point(0,0))
        displayPlayList(playList)
        repaint()
    }

    def refresh() {
        fixImageIndexes()
        displayPlayList(playList)
        repaint()
    }

    //Write our layout template out to a file
    def writeLayoutTemplate(pw:PrintWriter) {
        pageLayout.writeLayoutTemplate(pw)
    }

    //Read a layout from a template file and set it as our layout
    def loadLayoutTemplate(f:File) {
        val newPageLayout = new PageLayout(viewer)
        newPageLayout.loadLayoutTemplate(f)
        setPageLayout(newPageLayout)
    }

    def editLayoutDescription() {
        var text = pageLayout.getDescription
        if (text==null)
            text = ""
        val title = viewer.getResourceString("query.EditLayout.title");
        val newText = viewer.editTextDialog(title,text)
        if (newText==null)
            return        //cancelled
        pageLayout.setDescription(newText)
    }
}
