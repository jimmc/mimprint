package net.jimmc.mimprint

import net.jimmc.swing.MenuAction
import net.jimmc.swing.SButton
import net.jimmc.swing.SFrame
import net.jimmc.swing.SwingS
import net.jimmc.util.AsyncUi
import net.jimmc.util.UserException

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.io.FileNotFoundException
import javax.swing.JToolBar
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextField

import scala.actors.Actor
import scala.actors.Actor.loop

class SViewer(app:AppS) extends SFrame("Mimprint",app) with AsyncUi with Actor {

    private val mainTracker = new PlayListTracker(this)
    private val toolBar = createToolBar()
    private var mainList:PlayViewList = _
    private var mainSingle:PlayViewSingle = _

    setJMenuBar(createMenuBar())
    initForm()
    //setScreenMode(SCREEN_MODE_DEFAULT)
    pack()

    addWindowListener()

    private def createMenuBar():JMenuBar = {
        val mb = new JMenuBar()
        mb.add(createFileMenu())
        mb
    }

    private def createFileMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.File.label"))

        val exitLabel = getResourceString("menu.File.Exit.label")
        m.add(new MenuAction(exitLabel) {
            override def action() = processFileExit
        })

        m
    }

    //Closing this window causes the app to exit
    override def processClose() = processFileExit

    //Don't ask about exiting, just do it
    override def confirmExit():Boolean = true

    private def createToolBar():JToolBar = {
        val tb = new JToolBar()
        tb.setRollover(true)

        tb.add(createModeDualButton())
        //tb.add(createModeFullButton())

        tb.addSeparator()
        //tb.add(createPreviousFolderButton())
        //TODO

        tb
    }

    private def createModeDualButton():SButton = {
        new SButton(this,"button.ModeDual")({
            infoDialog("ModeDual button was pushed")
            throw new UserException("intentional UserException")
        })
    }

    //Create the body of our form
    private def initForm() {

        mainList = new PlayViewList(this,mainTracker)
        val imageLister = mainList.getComponent()
        mainList.start();

        mainSingle = new PlayViewSingle(this,mainTracker)
        val imageArea = mainSingle.getComponent()
        mainSingle.start();

        val imagePane = new JPanel(new BorderLayout())
        imagePane.setMinimumSize(new Dimension(100,100))
        imagePane.add(imageArea,BorderLayout.CENTER)

        val mainBody = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                imageLister, imagePane)
        mainBody.setBackground(imageArea.getBackground())

        val statusLine = new JTextField()
        statusLine.setEditable(false)
        statusLine.setBackground(Color.lightGray)

        val cp = getContentPane
        cp.setLayout(new BorderLayout())
        cp.add(mainBody,BorderLayout.CENTER)
        cp.add(statusLine,BorderLayout.SOUTH)
        cp.add(toolBar,BorderLayout.NORTH)
    }

    def mainOpen(fileName:String) {
        try {
            mainTracker.load(fileName)
        } catch {
            case ex:FileNotFoundException =>
                val msg = app.getResourceFormatted("error.NoSuchFile",fileName)
                errorDialog(msg)
            case ex:Exception =>
                exceptionDialog(ex)
        }
    }

    def invokeUi(code: =>Unit) {
        SwingS.invokeLater(code)
    }

    def warningMessage(msg:String) {
        println("WARNING: "+msg)       //TODO better implementation
    }

    def errorMessage(msg:String) {
        println("ERROR: "+msg)       //TODO better implementation
    }

    //The Actor trait
    def act() {
        this ! SViewerRequestFocus(null)
        loop { react (handleMessage) }
    }

    val handleMessage : PartialFunction[Any,Unit] = {
        case m:SViewerRequestClose => processClose
        case m:SViewerRequestActivate =>
                mainList ! PlayViewListRequestActivate(m.list)
        case m:SViewerRequestFocus =>
                mainSingle ! PlayViewSingleRequestFocus()
        //TODO - messages to set screen modes
    }
}

sealed abstract class SViewerRequest
case class SViewerRequestClose() extends SViewerRequest
case class SViewerRequestActivate(list:PlayListS) extends SViewerRequest
case class SViewerRequestFocus(list:PlayListS) extends SViewerRequest

/*
vi:
*/
