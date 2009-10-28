/* PlayViewSingle.scala
 *
 * Jim McBeath, June 12, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.KeyListenerCatch
import net.jimmc.swing.SCheckBoxMenuItem
import net.jimmc.swing.SMenu
import net.jimmc.swing.SMenuItem
import net.jimmc.swing.SwingS
import net.jimmc.util.StdLogger

import java.awt.BorderLayout
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
import java.awt.Font
import java.awt.geom.AffineTransform
import java.awt.Graphics2D
import java.awt.Image
import java.awt.MediaTracker
import java.awt.Point
import java.io.File
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingConstants

class PlayViewSingle(name:String, viewer:SViewer, tracker:PlayListTracker)
        extends PlayViewComp(name, viewer, tracker)
        with StdLogger {
    private var imagePanel:JPanel = _
    private var imageComponent:JLabel = _
    private var captionLabel:JLabel = _
    private var includeFileInfo = false
    private var mediaTracker:MediaTracker = _
    private var playList:PlayList = _
    private var currentIndex:Int = -1
    private var currentItem:PlayItem = _

    private var contextMenu:JPopupMenu = _
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
        imageComponent.addKeyListener(
                new KeyListenerCatch(new PlayViewSingleKeyListener(),viewer))
        imageComponent.addMouseListener(new PlayViewSingleMouseListener())
        imageComponent.addMouseMotionListener(
                new PlayViewSingleMouseMotionListener())
        imageComponent.addComponentListener(
                new PlayViewSingleComponentListener())
        initCursors
        mediaTracker = new MediaTracker(imageComponent)

        contextMenu = createContextMenu()

        captionLabel = new JLabel()
        captionLabel.setHorizontalAlignment(SwingConstants.CENTER)

        imagePanel = new JPanel()
        imagePanel.setLayout(new BorderLayout())
        imagePanel.add(imageComponent,BorderLayout.CENTER)
        imagePanel.add(captionLabel,BorderLayout.SOUTH)
        captionLabel.setVisible(false)

        imagePanel
    }

    private def createContextMenu():JPopupMenu = {
        val m = new JPopupMenu()

        //TODO - add a label at the start?
        m.add(new SMenuItem(viewer,"menu.Image.PreviousImage")(
                    tracker ! PlayListRequestUp(playList)))
        m.add(new SMenuItem(viewer,"menu.Image.NextImage")(
                    tracker ! PlayListRequestDown(playList)))
        m.add(new SMenuItem(viewer,"menu.Image.PreviousDirectory")(
                    tracker ! PlayListRequestLeft(playList)))
        m.add(new SMenuItem(viewer,"menu.Image.NextDirectory")(
                    tracker ! PlayListRequestRight(playList)))
        val mr = new SMenu(viewer,"menu.Image.RotateMenu")
        m.add(mr)
        mr.add(new SMenuItem(viewer,"menu.Image.RotateMenu.R90")(
                    requestRotate(1)))
        mr.add(new SMenuItem(viewer,"menu.Image.RotateMenu.R180")(
                    requestRotate(2)))
        mr.add(new SMenuItem(viewer,"menu.Image.RotateMenu.R270")(
                    requestRotate(-1)))
        m.add(new SMenuItem(viewer,"menu.Image.AddToActive")(
                    viewer ! SViewerRequestAddToActive(playList,currentIndex)))
        m.add(new SMenuItem(viewer,"menu.Image.RemoveImage")(
                    viewer ! SViewerRequestRemoveImage(playList,currentIndex)))
        m.add(new SMenuItem(viewer,"menu.Image.ShowEditDialog")(
                    viewer ! SViewerRequestEditDialog(playList,currentIndex)))
        m.add(new SMenuItem(viewer,"menu.Image.ShowInfoDialog")(
                    viewer ! SViewerRequestInfoDialog(playList,currentIndex)))
        m.add(new SMenuItem(viewer,"menu.Image.ShowDirEditDialog")(
                    viewer ! SViewerRequestDirEditDialog(playList)))

        m.add(createCaptionMenu())

        m
    }
    
    private def createCaptionMenu() = {
        val m = new SMenu(viewer,"menu.Image.Caption")

        m.add(new SCheckBoxMenuItem(viewer,"menu.Image.Caption.ShowCaption")(
                (cb) => showCaption(cb.getState)))
        m.add(new SCheckBoxMenuItem(viewer,"menu.Image.Caption.TopCaption")(
                (cb) => setCaptionTop(cb.getState)))
        m.add(new SCheckBoxMenuItem(
                viewer,"menu.Image.Caption.IncludeFileInfo")(
                (cb) => setCaptionIncludeFileInfo(cb.getState)))
        m.add(new SMenuItem(viewer,"menu.Image.Caption.Larger")(
                resizeCaptionFont(1.5)))
        m.add(new SMenuItem(viewer,"menu.Image.Caption.Smaller")(
                resizeCaptionFont(0.67)))

        m
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
        playList = m.newList
        if (m.index<=currentIndex)
            currentIndex = currentIndex + 1
    }

    protected def playListRemoveItem(m:PlayListRemoveItem) {
        logger.debug("enter PlayViewSingle.playListRemoveItem")
        playList = m.newList
        if (currentIndex >= playList.size - 1) {
            imageSelected(-1)           //last item in the list was deleted
        } else if (m.index==currentIndex) {
            imageSelected(currentIndex)         //leave index as-is, redisplay
        } else if (m.index<currentIndex)
            currentIndex = currentIndex - 1
        logger.debug("leave PlayViewSingle.playListRemoveItem")
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        playList = m.newList
        if (m.index==currentIndex)
            imageSelected(m.index)
    }

    protected def playListUpdateItem(m:PlayListUpdateItem) {
        if (m.index==currentIndex)
            imageSelected(m.index)
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        imageSelected(m.index)
    }

    protected def playListChangeList(m:PlayListChangeList) {
        playList = m.newList
        currentIndex = -1
        //if (playList.size>0)
            //imageSelected(0)
    }

    override protected val handleOtherMessage : PartialFunction[Any,Unit] = {
        case m:PlayViewSingleRequestFocus => imageComponent.requestFocus()
        case m:Any => println("Unrecognized message to PlayViewSingle")
    }

    private def showCaption(b:Boolean) {
        captionLabel.setVisible(b)
        setCaptionText(currentIndex)
    }

    private def setCaptionTop(b:Boolean) {
        val position = if (b) BorderLayout.NORTH else BorderLayout.SOUTH
        imagePanel.remove(captionLabel)
        imagePanel.add(captionLabel,position)
        imagePanel.revalidate
    }

    private def setCaptionIncludeFileInfo(b:Boolean) {
        includeFileInfo = b
        setCaptionText(currentIndex)
    }

    private def resizeCaptionFont(scale:Double) {
        val f:Font = captionLabel.getFont
        val newFont = f.deriveFont((f.getSize2D * scale).asInstanceOf[Float])
        captionLabel.setFont(newFont)
    }

    private def setCaptionText(index:Int) {
        if (!captionLabel.isVisible)
            return
        if (index<0) {
            captionLabel.setText("")
            return
        }
        val item = playList.getItem(index)
        val fileInfo = new FileInfo(index,0,0,0,item.baseDir,item.fileName)
        fileInfo.loadInfo(false)
        val captionText0 =
            if (includeFileInfo)
                fileInfo.getFileTextInfo(true,false)
            else
                htmlify(fileInfo.text)
        val captionText = if (captionText0==null) "" else captionText0
        captionLabel.setText(captionText)
    }
    private def htmlify(s:String):String = {
        if (s==null || s=="")
            ""
        else
            "<html>"+s+"</html>"
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
                captionLabel.setText("")
                currentIndex = -1
                currentItem = null
            }
            return
        }
        currentIndex = index
        if (index<0) {
//println("Single "+name+" setting no image")
            val msg = viewer.getResourceString("error.NoImageSelected")
            imageComponent.setText(msg)
            imageComponent.setIcon(null)
            currentItem = null
            captionLabel.setText("")
        } else {
            val item = playList.getItem(index)
            if (item == currentItem) {
                //We already have this image loaded and selected
//println("Single "+name+" already showing image "+index)
                return
            }
//println("Single "+name+" loading image "+index)
            currentItem = item
            if (item.fileName.endsWith("."+FileInfo.MIMPRINT_EXTENSION)) {
                //It is one of our files, not an image
                //TODO - use some code from IconLoader to display our
                //templates?
                imageComponent.setIcon(null)
                imageComponent.setText(null)
                captionLabel.setText("")
            } else {
                setCursorBusy(true)
                val im = getTransformedImage(index)
                    //TODO - check for null im?
                val ii = new ImageIcon(im)
                imageComponent.setIcon(ii)
                imageComponent.setText(null)
                setCaptionText(index)
                preloadImage(index-1)
                preloadImage(index+1)
                setCursorBusy(false)
            }
        }
        imagePanel.revalidate()
    }

    private def preloadImage(index:Int) {
        if (index<0) return
        if (index>=playList.size) return
        val item = playList.getItem(index)
        if (item.fileName.endsWith("."+FileInfo.MIMPRINT_EXTENSION)) return
            //ignore non-image files
        val f = new File(item.baseDir,item.fileName)

        //TODO - Might want to do this part in another thread
        val im = ImageUtil.getImage(imageComponent,f.getPath)
        ImageUtil.scaleAndRotate(im,item.rotFlag,f.getPath, imageComponent)
    }

    private def getTransformedImage(index:Int):Image = {
        val item = playList.getItem(index)
        val f = new File(item.baseDir,item.fileName)
        val im = ImageUtil.getImage(imageComponent,f.getPath)
        ImageUtil.scaleAndRotate(im,item.rotFlag,f.getPath, imageComponent)
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

    def refresh() {
        currentItem = null          //force reload of image to get new size
        imageSelected(currentIndex)
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
                case 'd' =>
                    viewer ! SViewerRequestRemoveImage(playList,currentIndex)
                case 'e' =>
                        viewer ! SViewerRequestEditDialog(playList,currentIndex)
                case 'E' =>
                        viewer ! SViewerRequestDirEditDialog(playList)
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
        def mousePressed(ev:MouseEvent) = {
            if (!maybeShowPopup(ev))
                imageComponent.requestFocus()
        }
        def mouseReleased(ev:MouseEvent) = maybeShowPopup(ev)
        private def maybeShowPopup(ev:MouseEvent):Boolean = {
            if (ev.isPopupTrigger()) {
                setCursorVisible(true)
                contextMenu.show(ev.getComponent,ev.getX,ev.getY)
                true
            } else false
        }
    }

    class PlayViewSingleMouseMotionListener extends MouseMotionListener {
        def mouseDragged(ev:MouseEvent) = setCursorVisible(true)
        def mouseMoved(ev:MouseEvent) = setCursorVisible(true)
    }

    class PlayViewSingleComponentListener extends ComponentListener {
        def componentHidden(ev:ComponentEvent) = ()
        def componentMoved(ev:ComponentEvent) = ()
        def componentResized(ev:ComponentEvent) = refresh
        def componentShown(ev:ComponentEvent) = ()
    }
}

sealed abstract class PlayViewSingleRequest
case class PlayViewSingleRequestFocus()
