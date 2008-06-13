package net.jimmc.mimprint

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

class PlayViewSingle(tracker:PlayListTracker) extends PlayView(tracker) {
    private var imageComponent:JLabel = _
    private var mediaTracker:MediaTracker = _
    private var playList:PlayListS = _
    private var currentIndex:Int = -1

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

    private def requestRotate(rot:Int) {
        tracker ! PlayListRequestRotate(playList, currentIndex, rot)
    }

    private def imageSelected(index:Int) {
        currentIndex = index
        if (index<0) {
            //val msg = getResourceString("error.NoImageSelected")
                //TODO - need an app to get resources from
            val msg = "No image"
            imageComponent.setText(msg)
            imageComponent.setIcon(null)
        } else {
            setCursorBusy(true)
            val im = getTransformedImage(index)
                //TODO - check for null im?
            val ii = new ImageIcon(im)
            imageComponent.setIcon(ii)
            imageComponent.setText(null)
            setCursorBusy(false)
        }
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
        ImageUtil.createScaledImage(sourceImage,rot,
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
                //TODO - implement ESC command
                case KeyEvent.VK_ESCAPE => println("ESCAPE NYI")
                case _ => //ignore anything else
            }
        }
        def keyReleased(ev:KeyEvent) = ()       //ignored
        def keyTyped(ev:KeyEvent) {
            ev.getKeyChar() match {
                case 'r' => requestRotate(1)   //rotate CCW
                case 'R' => requestRotate(-1)  //rotate CW
                case ControlR => requestRotate(2)    //rotate 180
                //TODO - add other key commands
                case ch => println("NYI key "+ch)
            }
        }
        private val ControlR = 'R' - 0100
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
        def componentResized(ev:ComponentEvent) = imageSelected(currentIndex)
        def componentShown(ev:ComponentEvent) = ()
    }
}
