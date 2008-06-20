package net.jimmc.mimprint

import net.jimmc.swing.SwingS

import java.awt.Color
import java.awt.Cursor
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.geom.AffineTransform
import java.awt.Graphics2D
import java.awt.Image
import java.awt.MediaTracker
import java.awt.Point
import java.io.File
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.SwingConstants

class PlayViewSingle(name:String, viewer:SViewer, tracker:PlayListTracker)
        extends PlayViewComp(name, viewer, tracker) {
    private var imageComponent:JLabel = _
    private var mediaTracker:MediaTracker = _
    private var playList:PlayListS = _
    private var currentIndex:Int = -1
    private var currentItem:PlayItemS = _

    private var cursorBusy = false
    private var cursorVisible = true
    private var invisibleCursor:Cursor = _
    private var busyCursor:Cursor = _

    def getComponent():Component = {
        imageComponent = new JLabel()
        imageComponent.setBackground(Color.gray)
        imageComponent.setForeground(Color.white)
        imageComponent.setPreferredSize(new Dimension(800,600))
        imageComponent.setHorizontalAlignment(SwingConstants.CENTER)
        //TODO - add bigFont option
        imageComponent.addKeyListener(new PlayViewSingleKeyListener())
        imageComponent.addMouseListener(new PlayViewSingleMouseListener())
        imageComponent.addMouseMotionListener(
                new PlayViewSingleMouseMotionListener())
        imageComponent.addComponentListener(
                new PlayViewSingleComponentListener())
        initCursors
        mediaTracker = new MediaTracker(imageComponent)
        imageComponent
    }

    def isShowing():Boolean = imageComponent.isShowing

    //TODO - add code to preload next/previous images?

    private def initCursors() {
        val tk = imageComponent.getToolkit
        val blankCursorImage = tk.createImage(new Array[Byte](0))
        invisibleCursor = tk.createCustomCursor(
                blankCursorImage,new Point(0,0),"invisible")
        busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    }

    protected def playListInit(m:PlayListInit) {
        playList = m.list
        currentIndex = -1
    }

    protected def playListAddItem(m:PlayListAddItem) {
        println("PlayViewSingle.playListAddItem NYI")           //TODO
    }

    protected def playListRemoveItem(m:PlayListRemoveItem) {
        println("PlayViewSingle.playListRemoveItem NYI")        //TODO
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        playList = m.newList
        if (m.index==currentIndex)
            imageSelected(m.index)
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        imageSelected(m.index)
    }

    protected def playListChangeList(m:PlayListChangeList) {
        playList = m.newList
        currentIndex = -1
        if (playList.size>0)
            imageSelected(0)
    }

    override protected val handleOtherMessage : PartialFunction[Any,Unit] = {
        case m:PlayViewSingleRequestFocus => imageComponent.requestFocus()
        case m:Any => println("Unrecognized message to PlayViewSingle")
    }

    private def requestRotate(rot:Int) {
        tracker ! PlayListRequestRotate(playList, currentIndex, rot)
    }

    private def imageSelected(index:Int) {
        if (!isShowing) {
            //If we are not showing, don't waste time loading images
            if (currentIndex >= 0) {
//println("Single "+name+" not showing")
                imageComponent.setText("")
                imageComponent.setIcon(null)
                currentIndex = -1
                currentItem = null
            }
            return
        }
        if (index<0) {
//println("Single "+name+" setting no image")
            val msg = viewer.getResourceString("error.NoImageSelected")
            imageComponent.setText(msg)
            imageComponent.setIcon(null)
            currentItem = null
        } else {
            val item = playList.getItem(index)
            if (item == currentItem) {
                //We already have this image loaded and selected
//println("Single "+name+" already showing image "+index)
                return
            }
//println("Single "+name+" loading image "+index)
            setCursorBusy(true)
            val im = getTransformedImage(index)
                //TODO - check for null im?
            val ii = new ImageIcon(im)
            imageComponent.setIcon(ii)
            imageComponent.setText(null)
            setCursorBusy(false)
            currentItem = item
        }
        currentIndex = index
        imageComponent.revalidate()
    }

    private def getTransformedImage(index:Int):Image = {
        val item = playList.getItem(index)
        val f = new File(item.baseDir,item.fileName)
        val im = imageComponent.getToolkit.createImage(f.getPath)
        val si = createScaledImage(im,item.rotFlag,f.getPath)
        loadCompleteImage(si)
        val ri = createRotatedImage(si,item.rotFlag)
        loadCompleteImage(ri)
        ri
    }
    
    private def createScaledImage(sourceImage:Image,rot:Int,path:String):
            Image = {
        SImageUtil.createScaledImage(sourceImage,rot,
            imageComponent.getWidth,imageComponent.getHeight,path)
    }

    private def createRotatedImage(sourceImage:Image,rot:Int):Image = {
        if (((rot+4)%4)==0)
            return sourceImage
        val (w, h) = getImageSize(sourceImage)
        val dstImage = imageComponent.createImage(
                if (rot%2==0) w else h,  if (rot%2==0) h else w)
        val dstG2 = dstImage.getGraphics.asInstanceOf[Graphics2D]
        var transform:AffineTransform = null
        ((rot+4)%4) match {
            case 1 => transform = new AffineTransform(
                    0.0, -1.0, 1.0, 0.0, 0.0, w)
            case 2 => transform = new AffineTransform(
                    -1.0, 0.0, 0.0, -1.0, w, h)
            case 3 => transform = new AffineTransform(
                    0.0, 1.0, -1.0, 0.0, h, 0.0)
            case 0 =>   //shouldn't happen, we checked for rot%4==0 above
                return sourceImage
        }
        dstG2.drawImage(sourceImage,transform,null)
        dstImage
    }

    private def getImageSize(sourceImage:Image):(Int,Int) = {
        var waitCount = 0
        while (sourceImage.getWidth(null)<0 || sourceImage.getHeight(null)<0) {
            //THe image has not yet started loading, so we don't
            //know it's size.  Wait just a bit.
            waitCount = waitCount + 1
            if (waitCount > 100)
                return (0, 0)   //TODO - throw exception?
            try { Thread.sleep(100) } catch { case _ => } //ignore errors here
        }
        (sourceImage.getWidth(null), sourceImage.getHeight(null))
    }

    private def loadCompleteImage(image:Image) {
        mediaTracker.addImage(image,0)
        try {
            mediaTracker.waitForID(0,20000)
        } catch {
            //TODO - better info
            case ex:InterruptedException =>
                throw new RuntimeException(ex)
        }
        mediaTracker.removeImage(image,0)
    }

    //Set the cursor to a busy cursor.
    private def setCursorBusy(busy:Boolean) {
        cursorBusy = busy
        if (busy)
            imageComponent.setCursor(busyCursor)
        else
            setCursorVisible(cursorVisible)
    }

    //Make the cursor visible or invisible.
    //If busy-cursor has been set, cursor is always visible.
    private def setCursorVisible(visible:Boolean) {
        cursorVisible = visible
        if (cursorBusy)
            return      //busy takes priority over invisible
        imageComponent.setCursor(if (visible) null else invisibleCursor)
    }

    class PlayViewSingleKeyListener extends KeyListener {
        def keyPressed(ev:KeyEvent) {
            setCursorVisible(false)    //turn off cursor on any key
            ev.getKeyCode() match {
                case KeyEvent.VK_LEFT =>
                    tracker ! PlayListRequestLeft(playList)
                case KeyEvent.VK_RIGHT =>
                    tracker ! PlayListRequestRight(playList)
                case KeyEvent.VK_UP =>
                    tracker ! PlayListRequestUp(playList)
                case KeyEvent.VK_DOWN =>
                    tracker ! PlayListRequestDown(playList)
                case KeyEvent.VK_ESCAPE =>
                    requestScreenMode(SViewer.SCREEN_PREVIOUS)
                case KeyEvent.VK_ENTER =>
                    viewer ! SViewerRequestActivate(playList)
                case _ => //ignore anything else
            }
        }
        def keyReleased(ev:KeyEvent) = ()       //ignored
        def keyTyped(ev:KeyEvent) {
            ev.getKeyChar() match {
                case ' ' => viewer ! SViewerRequestActivate(playList)
                case 'a' => requestScreenMode(SViewer.SCREEN_ALT)
                case 'e' =>
                        viewer ! SViewerRequestEditDialog(playList,currentIndex)
                case 'f' => requestScreenMode(SViewer.SCREEN_FULL)
                case 'i' =>
                        viewer ! SViewerRequestInfoDialog(playList,currentIndex)
                case 'o' => viewer ! SViewerRequestFileOpen()
                case 'p' =>
                    viewer ! SViewerRequestAddToActive(playList,currentIndex)
                case 'P' => requestScreenMode(SViewer.SCREEN_PRINT)
                case 'r' => requestRotate(1)   //rotate CCW
                case 'R' => requestRotate(-1)  //rotate CW
                case ControlR => requestRotate(2)    //rotate 180
                case 's' => requestScreenMode(SViewer.SCREEN_SLIDESHOW)
                case 'x' => viewer ! SViewerRequestClose()
                case '?' => showHelpDialog
                case ch => println("NYI key "+ch)
            }
        }
        private val ControlR = 'R' - 0100
        def requestScreenMode(mode:Int) =
            viewer ! SViewerRequestScreenMode(mode)
    }

    private def showHelpDialog() {
        val helpText = viewer.getResourceString("info.ImageHelp")
        viewer.invokeUi {
            viewer.infoDialog(helpText)
        }
    }

    class PlayViewSingleMouseListener extends MouseListener {
        def mouseClicked(ev:MouseEvent) = ()
        def mouseEntered(ev:MouseEvent) = ()
        def mouseExited(ev:MouseEvent) = ()
        def mousePressed(ev:MouseEvent) = imageComponent.requestFocus()
        def mouseReleased(ev:MouseEvent) = ()
    }

    class PlayViewSingleMouseMotionListener extends MouseMotionListener {
        def mouseDragged(ev:MouseEvent) = setCursorVisible(true)
        def mouseMoved(ev:MouseEvent) = setCursorVisible(true)
    }

    class PlayViewSingleComponentListener extends ComponentListener {
        def componentHidden(ev:ComponentEvent) = ()
        def componentMoved(ev:ComponentEvent) = ()
        def componentResized(ev:ComponentEvent) = {
            currentItem = null          //force reload of image to get new size
            imageSelected(currentIndex)
        }
        def componentShown(ev:ComponentEvent) = ()
    }
}

sealed abstract class PlayViewSingleRequest
case class PlayViewSingleRequestFocus
