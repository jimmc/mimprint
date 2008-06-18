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
    var highlightedArea:AreaLayout = _

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
        cursorVisible = visible;
        if (cursorBusy)
            return		//busy takes priority over invisible
        if (visible)
            setCursor(null)
        else
            setCursor(invisibleCursor)
    }

    class AreaPageKeyListener extends KeyListener {
        var knownKeyPress = false
        //The KeyListener interface
        def keyPressed(ev:KeyEvent) {
            setCursorVisible(false)	//turn off cursor on any key
            val keyCode = ev.getKeyCode()
            knownKeyPress = true	//assume we know it
/* TODO
            keyCode match {
            case KeyEvent.VK_LEFT =>
                setCursorVisible(true)
                viewer.moveLeft()
                setCursorVisible(false)
            case KeyEvent.VK_RIGHT =>
                setCursorVisible(true)
                viewer.moveRight()
                setCursorVisible(false)
            case KeyEvent.VK_DOWN =>
                setCursorVisible(true)
                viewer.moveDown()
                setCursorVisible(false)
            case KeyEvent.VK_UP =>
                setCursorVisible(true)
                viewer.moveUp()
                setCursorVisible(false)
            case KeyEvent.VK_ESCAPE =>
                viewer.restorePreviousScreenMode()
            case KeyEvent.VK_ENTER =>
                viewer.activateSelection()
            case _ =>
                knownKeyPress = false
            }
*/
        }
        def keyReleased(ev:KeyEvent) {
            //val keyCode = ev.getKeyCode()
        }
        def keyTyped(ev:KeyEvent) {
/* TODO
            char ch = ev.getKeyChar();
            switch (ch) {
            case ' ':   //activate selection
                viewer.activateSelection();
                break;
            case 'a':    //alternate-screen
                viewer.setScreenMode(Viewer.SCREEN_ALT);
                break;
            case 'f':    //full-screen
                viewer.setScreenMode(Viewer.SCREEN_FULL);
                break;
            case 'L'-0100:    //control-L, refresh
                //showCurrentImage();               //TODO
                break;
            case 'e':
                setCursorVisible(true);    //turn on cursor
                viewer.showImageEditDialog();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'i':
                setCursorVisible(true);    //turn on cursor
                if (imageInfoText==null) {
                    imageInfoText = getResourceString("query.Info.NoDescription");
                }
                viewer.infoDialog(imageInfoText);
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'o':    //file-open dialog
                setCursorVisible(true);    //turn on cursor
                viewer.processFileOpen();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 'p':   //add current image to active or printable playlist
                viewer.addCurrentImageToPlayList();
                break;
            case 'P':    //the print screen
                viewer.setScreenMode(Viewer.SCREEN_PRINT);
                break;
            case 'r':    //rotate CCW
                viewer.rotateCurrentImage(1);
                break;
            case 's':    //the slideshow screen
                viewer.setScreenMode(Viewer.SCREEN_SLIDESHOW);
                break;
            case 'R':    //rotate CW
                viewer.rotateCurrentImage(-1);
                break;
            case 'R'-0100:    //control-R, rotate 180
                viewer.rotateCurrentImage(2);
                break;
            case 'x':    //exit
                setCursorVisible(true);    //turn on cursor
                viewer.processClose();
                setCursorVisible(false);    //turn cursor back off
                break;
            case '?':
                setCursorVisible(true);    //turn on cursor
                viewer.showHelpDialog();
                setCursorVisible(false);    //turn cursor back off
                break;
            case 0177:                  //delete
            case 8:                     //backspace
                if (currentArea!=null) {
                    //clear image from current area
                    currentArea.setImage(null);
                    repaintCurrentImage();
                }
                break;
            default:        //unknown key
                if (!knownKeyPress) {
                    System.out.println("Unknown key "+ch+" ("+((int)ch)+")");
                    getToolkit().beep();
                }
                break;
            }
*/
        }
        //End KeyListener interface
    }

    class AreaPageMouseListener extends MouseAdapter {
        override def mousePressed(ev:MouseEvent) {
            requestFocus()
            //selectArea(new Point(ev.getX(),ev.getY()))
                //TODO - implement selectArea()
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
            //app.debugMsg("componentResized");
            //repaint();
        }
        def componentShown(ev:ComponentEvent){}
        //End ComponentListener interface
    }
}
