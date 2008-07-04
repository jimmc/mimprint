/* SViewer.scala
 *
 * Jim McBeath, June 10, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.AboutWindow
import net.jimmc.swing.SButton
import net.jimmc.swing.SCheckBoxMenuItem
import net.jimmc.swing.SFrame
import net.jimmc.swing.SMenuItem
import net.jimmc.swing.SwingS
import net.jimmc.swing.ToolPrompter
import net.jimmc.util.AsyncUi
import net.jimmc.util.FileUtilS
import net.jimmc.util.PFCatch
import net.jimmc.util.Subscribe
import net.jimmc.util.Subscriber
import net.jimmc.util.UserException

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.JToolBar
import javax.swing.JWindow

import scala.actors.Actor
import scala.actors.Actor.loop

class SViewer(app:AppS) extends SFrame("Mimprint",app) with AsyncUi
        with Actor with Subscriber[PlayListMessage] with ToolPrompter {
//TODO - implement ToolPrompter interface (to get menu toolPrompts)

    private val mainTracker = new PlayListTracker(this)
    private var playList:PlayListS = _  //current main play list
    private var playListIndex:Int = -1  //currently selected item in main list

    private val printableTracker = new PlayListTracker(this)
    private var printablePlayList:PlayListS = _
    private var printablePlayListIndex:Int = -1

    private val toolBar = createToolBar()
    private var printMenuItem:SMenuItem = _
    private var layoutMenu:JMenu = _

    private var statusLine:JTextField = _
    private var mainList:ViewListGroup = _
    private var mainSingle:PlayViewSingle = _
    private var fullSingle:PlayViewSingle = _
    private var currentMainFile:Option[File] = None
    private var screenMode = SViewer.SCREEN_MODE_DEFAULT
    private var previousScreenMode = screenMode
    private var imagePane:JPanel = _
    private var mainSingleComp:Component = _
    private var fullWindow:SFrame = _

    private var mShowAlt:SCheckBoxMenuItem = _
    private var altSingle:PlayViewSingle = _
    private var altWindow:JWindow = _

    private var mShowDual:SCheckBoxMenuItem = _
    private var dualSingle:PlayViewSingle = _
    private var dualWindow:SFrame = _

    private var mShowPrintList:SCheckBoxMenuItem = _
    private var printableList:ViewListGroup = _
    private var printableLister:Component = _
    private var printableMulti:PlayViewMulti = _
    private var printableComp:Component = _

    private var lastSaveLayoutTemplateFile:Option[File] = None
    private var lastLoadLayoutTemplateFile:Option[File] = None

    setJMenuBar(createMenuBar())
    initForm()
    setScreenMode(SViewer.SCREEN_MODE_DEFAULT)
    setScreenModeButtons()
    pack()

    addWindowListener()

    override protected def dialogParent():Component = {
        if (fullWindow!=null && fullWindow.isShowing)
            fullWindow
        else
            this
    }
    
    private def createMenuBar():JMenuBar = {
        val mb = new JMenuBar()
        mb.add(createFileMenu())
        mb.add(createImageMenu())
        //TODO - create PlayList menu
        layoutMenu = createLayoutMenu()
        mb.add(layoutMenu)
        mb.add(createViewMenu())
        mb.add(createHelpMenu())

        mb
    }

    private def createFileMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.File.label"))

        m.add(new SMenuItem(this,"menu.File.Open")(processFileOpen))
        printMenuItem = new SMenuItem(this,"menu.File.Print")(processFilePrint)
        m.add(printMenuItem)
        m.add(new SMenuItem(this,"menu.File.Exit")(processFileExit))

        m
    }

    private def createImageMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.Image.label"))

        m.add(new SMenuItem(this,"menu.Image.PreviousImage")(requestUp))
        m.add(new SMenuItem(this,"menu.Image.NextImage")(requestDown))
        m.add(new SMenuItem(this,"menu.Image.PreviousDirectory")(requestLeft))
        m.add(new SMenuItem(this,"menu.Image.NextDirectory")(requestRight))

        m.add(new JSeparator())

        val mr = new JMenu(getResourceString("menu.Image.RotateMenu.label"))
        m.add(mr)

        mr.add(new SMenuItem(this,"menu.Image.RotateMenu.R90")(
                requestRotate(1)))
        mr.add(new SMenuItem(this,"menu.Image.RotateMenu.R180")(
                requestRotate(2)))
        mr.add(new SMenuItem(this,"menu.Image.RotateMenu.R270")(
                requestRotate(-1)))
        
        m.add(new SMenuItem(this,"menu.Image.AddToActive")(requestAddToActive))

        m.add(new SMenuItem(this,"menu.Image.ShowEditDialog")(
                showImageEditDialog(playList,playListIndex)))
        m.add(new SMenuItem(this,"menu.Image.ShowInfoDialog")(
                showImageInfoDialog(playList,playListIndex)))

        m
    }

    private def createLayoutMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.Layout.label"))

        m.add(new SMenuItem(this,"menu.Layout.SaveTemplateAs")(
                saveLayoutTemplateAs()))
        m.add(new SMenuItem(this,"menu.Layout.LoadTemplate")(
                loadLayoutTemplate()))
        m.add(new SMenuItem(this,"menu.Layout.EditDescription")(
                editLayoutDescription()))

        m
    }

    private def createViewMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.View.label"))

        val modeInfo = List(
            (SViewer.SCREEN_PRINT, "Print"),
            (SViewer.SCREEN_SLIDESHOW, "SlideShow"),
            (SViewer.SCREEN_FULL, "Full")
        )
        screenModeButtons = modeInfo.map { mi =>
            val (modeVal, modeName) = mi
            val resKey = "menu.View.ScreenMode"+modeName
            val checkBox = new SCheckBoxMenuItem(this,resKey)(
                    setScreenMode(modeVal))
            m.add(checkBox)
            (modeVal, checkBox)
        }

        m.add(new JSeparator())

        mShowAlt = new SCheckBoxMenuItem(this,"menu.View.ScreenMode.Alternate")(
                setScreenModeAlt(mShowAlt.getState))
        //Enable the Alternate Screen mode button only if we have an alt screen
        mShowAlt.setVisible(hasAlternateScreen)
        m.add(mShowAlt)

        mShowDual = new SCheckBoxMenuItem(this,"menu.View.ScreenMode.Dual")(
                setScreenModeDual(mShowDual.getState))
        m.add(mShowDual)

        mShowPrintList = new SCheckBoxMenuItem(this,"menu.View.ShowPrintList")(
                showPrintList(mShowPrintList.getState))
        mShowPrintList.setState(false)
        m.add(mShowPrintList)

        //TODO - add separator, then more commands for other View options
        //TODO - add "Show Area Outlines", enabled only in mode=printable
        //TODO - add "Show Help Dialog"

        m
    }
    private var screenModeButtons:List[(Int,SCheckBoxMenuItem)] = _

    private def setScreenModeButtons() {
        screenModeButtons.foreach { info =>
            val modeVal = info._1
            val cb = info._2
            cb.setState(screenMode == modeVal)
        }
        printMenuItem.setEnabled(screenMode == SViewer.SCREEN_PRINT)
        layoutMenu.setEnabled(screenMode == SViewer.SCREEN_PRINT)
    }

    private def createHelpMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.Help.label"))
        m.add(new SMenuItem(this,"menu.Help.About")(
                AboutWindow.showAboutWindow(this)))
        m
    }

    private def requestRotate(rot:Int) {
        mainTracker ! PlayListRequestRotate(playList, playListIndex, rot)
    }

    def processFileOpen() {
        val msg = getResourceString("query.FileToOpen")
        val newMainFile = fileOrDirectoryOpenDialog(msg,currentMainFile)
        if (newMainFile.isDefined) {
            currentMainFile = newMainFile
            mainTracker.load(newMainFile.get.getPath)
        }
    }

    //Closing this window causes the app to exit
    override def processClose() = processFileExit

    //Don't ask about exiting, just do it
    override def confirmExit():Boolean = true

    def processFilePrint() {
        if (screenMode == SViewer.SCREEN_PRINT)
            printableMulti ! PlayViewMultiRequestPrint()
    }

    private def createToolBar():JToolBar = {
        val tb = new JToolBar()
        tb.setRollover(true)

        /* TODO - need to make this a toggle button and control its state
         * when the dual window gets opened or closed...
        tb.add(new SButton(this,"button.ModeDual")(
                setScreenMode(SViewer.SCREEN_DUAL)))
        */
        tb.add(new SButton(this,"button.ModeFull")(
                setScreenMode(SViewer.SCREEN_FULL)))

        tb.addSeparator()
        tb.add(new SButton(this,"button.PreviousFolder")(requestLeft))
        tb.add(new SButton(this,"button.PreviousImage")(requestUp))
        tb.add(new SButton(this,"button.NextImage")(requestDown))
        tb.add(new SButton(this,"button.NextFolder")(requestRight))

        tb.addSeparator()
        tb.add(new SButton(this,"button.RotateCcw")(requestRotate(1)))
        tb.add(new SButton(this,"button.RotateCw")(requestRotate(-1)))
        //We don't put in a rotate-180 button, let user push the other twice

        tb
    }

    //Create the body of our form
    private def initForm() {

        mainList = new ViewListGroup("mainList",this,mainTracker)
        val imageLister = mainList.getComponent()
        mainList.start()

        printableList = new ViewListGroup("printableList",this,printableTracker)
        printableLister = printableList.getComponent()
        printableLister.setPreferredSize(new Dimension(150,400))
        printableList.showFileInfo(false)
        printableList.showSingleViewer(false)
        printableList.showDirectories(false)
        printableList.start()

        mainSingle = new PlayViewSingle("main",this,mainTracker)
        mainSingleComp = mainSingle.getComponent()
        mainSingle.start()

        imagePane = new JPanel(new BorderLayout())
        imagePane.setMinimumSize(new Dimension(100,100))
        imagePane.add(mainSingleComp,BorderLayout.CENTER)

        val body0 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                imagePane, printableLister)
        body0.setResizeWeight(0.8)
        val mainBody = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                imageLister, body0)
        body0.setBackground(mainSingleComp.getBackground())
        mainBody.setBackground(mainSingleComp.getBackground())

        showPrintList(mShowPrintList.getState)
        body0.setPreferredSize(mainSingleComp.getPreferredSize())

        statusLine = new JTextField()
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
            currentMainFile = Some(new File(fileName))
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

    def showStatus(msg:String) {
        statusLine.setText(msg)
        //TODO - add a status history mechanism
    }

    def warningMessage(msg:String) {
        println("WARNING: "+msg)       //TODO better implementation
    }

    def errorMessage(msg:String) {
        println("ERROR: "+msg)       //TODO better implementation
    }

    def showToolPrompt(s:String) = showStatus(s)
    def clearToolPrompt() = showStatus("")

    //The Actor trait
    def act() {
        mainTracker ! Subscribe(this)
        mainTracker ! PlayListRequestInit(this)
        printableTracker ! Subscribe(this)
        printableTracker ! PlayListRequestInit(this)
        this ! SViewerRequestFocus(null)
        loop { react (PFCatch(handleMessage,"SViewer",this)) }
    }

    val handleMessage : PartialFunction[Any,Unit] = {
        case m:PlayListMessage if (m.tracker==mainTracker) =>
            processMainPlayListMessage(m)
        case m:PlayListMessage if (m.tracker==printableTracker) =>
            processPrintablePlayListMessage(m)
        case m:PlayListMessage =>
            println("Unknown sender of PlayListMessage")
        case m:SViewerRequestClose => processClose
        case m:SViewerRequestFileOpen => processFileOpen
        case m:SViewerRequestActivate => mainList.requestActivate(m.list)
        case m:SViewerRequestFocus =>
            if (fullSingle!=null && fullSingle.isShowing)
                fullSingle ! PlayViewSingleRequestFocus()
            else if (mainSingle.isShowing)
                mainSingle ! PlayViewSingleRequestFocus()
            else if (printableComp!=null && printableComp.isShowing)
                printableMulti ! PlayViewMultiRequestFocus()
            else
                println("Neither mainSingle nor fullSingle is visible")
        case m:SViewerRequestScreenMode =>
                setScreenMode(m.mode)
        case m:SViewerRequestInfoDialog =>
                showImageInfoDialog(m.list,m.index)
        case m:SViewerRequestEditDialog =>
                showImageEditDialog(m.list,m.index)
        case m:SViewerRequestAddToActive =>
                addToActive(m.list,m.index)
    }

    private def processMainPlayListMessage(msg:PlayListMessage) {
        msg match {
            case m:PlayListInit =>
                playList = m.list
                playListIndex = -1
                setTitleToFileName("")
            case m:PlayListAddItem =>
                playList = m.newList
                if (playListIndex >= 0 && playListIndex >= m.index)
                    playListIndex = playListIndex + 1
            case m:PlayListRemoveItem =>
                playList = m.newList
                if (playListIndex >= 0) {
                    if (playListIndex == m.index) {
                        playListIndex = -1
                        setTitleToFileName("")
                    } else if (playListIndex > m.index)
                        playListIndex = playListIndex - 1
                }
            case m:PlayListSelectItem =>
                playList = m.list
                playListIndex = m.index
                //We sometimes get an index out of bounds exception on startup
                //with an index value of zero.  Hmmm.
                if (playListIndex>=0 && playListIndex<playList.size) {
                    val fn = playList.getItem(playListIndex).getFileName
                    setTitleToFileName(fn)
                } else {
                    setTitleToFileName("")
                }
            case m:PlayListChangeItem =>
                playList = m.newList
                val fn = playList.getItem(playListIndex).getFileName
                setTitleToFileName(fn)
            case m:PlayListChangeList =>
                playList = m.newList
                playListIndex = -1
                setTitleToFileName("")
        }
    }

    private def processPrintablePlayListMessage(msg:PlayListMessage) {
        msg match {
            case m:PlayListInit =>
                printablePlayList = m.list
                printablePlayListIndex = -1
            case m:PlayListAddItem =>
                printablePlayList = m.newList
                if (printablePlayListIndex >= 0 &&
                        printablePlayListIndex >= m.index)
                    printablePlayListIndex = printablePlayListIndex + 1
            case m:PlayListRemoveItem =>
                printablePlayList = m.newList
                if (printablePlayListIndex >= 0) {
                    if (printablePlayListIndex == m.index) {
                        printablePlayListIndex = -1
                    } else if (printablePlayListIndex > m.index)
                        printablePlayListIndex = printablePlayListIndex - 1
                }
            case m:PlayListSelectItem =>
                printablePlayListIndex = m.index
                /* val fn = printablePlayList.getItem(
                        printablePlayListIndex).getFileName */
            case m:PlayListChangeItem =>
                printablePlayList = m.newList
                /* val fn = printablePlayList.getItem(
                        printablePlayListIndex).getFileName */
            case m:PlayListChangeList =>
                printablePlayList = m.newList
                printablePlayListIndex = -1
        }
    }

    def requestUp() = mainTracker ! PlayListRequestUp(playList)
    def requestDown() = mainTracker ! PlayListRequestDown(playList)
    def requestLeft() = mainTracker ! PlayListRequestLeft(playList)
    def requestRight() = mainTracker ! PlayListRequestRight(playList)
    def requestAddToActive() = addToActive(playList,playListIndex)

    /** Set the title on our main app window to the path of the
     * currently displayed file.
     */
    private def setTitleToFileName(fn:String) {
        val title =
            if (fn=="")
                getResourceString("title.nofile")
            else
                getResourceFormatted("title.fileName",fn)
        setTitle(title)
    }

    private def setScreenMode(reqMode:Int) {
        reqMode match {
            case SViewer.SCREEN_ALT => { setScreenModeAlt(); return }
            case SViewer.SCREEN_DUAL => { setScreenModeDual(); return }
            case _ =>   //fall through on others
        }

        val mode =
            if (reqMode == SViewer.SCREEN_PREVIOUS) previousScreenMode
            else reqMode
        if (mode == screenMode)
            return              //already in that mode
        if (mode != SViewer.SCREEN_FULL)
            this.show()

        mode match {
            case SViewer.SCREEN_FULL => setScreenModeFull()
            case SViewer.SCREEN_PRINT => setScreenModePrint()
            case SViewer.SCREEN_SLIDESHOW => setScreenModeSlideShow()
        }

        if (mode != SViewer.SCREEN_FULL && fullWindow!=null) {
            fullWindow.hide
        }
        if (mode == SViewer.SCREEN_FULL && fullWindow!=null)
            fullWindow.validate()
        else {
            this.validate()
            this.repaint()
        }

        mainTracker ! PlayListRequestSelect(playList,playListIndex)
            //send a notice to the new window to display the current image
        this ! SViewerRequestFocus(null)
            //make sure the right window has focus

        if (screenMode!=SViewer.SCREEN_FULL)
            previousScreenMode = screenMode
        screenMode = mode
        setScreenModeButtons()
    }

    private def setScreenModeAlt():Unit = setScreenModeAlt(!mShowAlt.getState)
    private def setScreenModeAlt(b:Boolean) {
        //TODO - if more than 2 configs, ask which one to use
        //For now, just use the second config

        if (altSingle == null) {
            val gc = getAlternateGraphicsConfiguration(1)
            if (gc==null) {
                getToolkit().beep()
                return         //only one display, no mode change
            }
            val altScreenBounds:Rectangle = gc.getBounds()
            altSingle = new PlayViewSingle("alt",this,mainTracker)
            val imageArea = altSingle.getComponent()
            altSingle.start()
            altWindow = new JWindow()
            altWindow.getContentPane().add(imageArea)
            altWindow.setBounds(altScreenBounds)
            altWindow.setBackground(imageArea.getBackground())
        }
        if (b) {
            altWindow.show()
            mainTracker ! PlayListRequestSelect(playList,playListIndex)
                //send a notice to the new window to display the current image
            this ! SViewerRequestFocus(null)
                //make sure the right window has focus
        } else
            altWindow.hide()
    }

    private def setScreenModeDual():Unit= setScreenModeDual(!mShowDual.getState)
    private def setScreenModeDual(b:Boolean) {
        if (dualSingle == null) {
            //Create the dualSingle window the first time
            dualSingle = new PlayViewSingle("dual",this,mainTracker)
            val wSize = new Dimension(300,200)
                //make it small, let user move it to other screen and resize
            dualWindow = initializePlayView1(dualSingle,wSize)(
                    mShowDual.setState(false))
        }
        if (b) {
            dualWindow.show()
            mainTracker ! PlayListRequestSelect(playList,playListIndex)
                //send a notice to the new window to display the current image
            this ! SViewerRequestFocus(null)
                //make sure the right window has focus
        } else
            dualWindow.hide()
    }

    private def setScreenModeFull() {
        if (fullSingle == null) {
            //Create the fullSingle window the first time
            fullSingle = new PlayViewSingle("full",this,mainTracker)
            val screenSize = getToolkit().getScreenSize()
            fullWindow = initializePlayView(fullSingle,screenSize)
        }
        fullWindow.show()
        this.hide()
    }

    private def setScreenModePrint() {
        if (printableMulti == null) {
            //Create the printableMulti window the first time
            printableMulti = new PlayViewMulti(
                    "printable",this,printableTracker,mainTracker)
            printableComp = printableMulti.getComponent()
            printableMulti.start()
        }
        //Put the printable component in place of the main image window
        imagePane.remove(mainSingleComp)
        imagePane.add(printableComp,BorderLayout.CENTER)
    }

    private def setScreenModeSlideShow() {
        if (printableComp!=null && printableComp.getParent==imagePane)
            imagePane.remove(printableComp)
        if (mainSingleComp.getParent != imagePane)
            imagePane.add(mainSingleComp,BorderLayout.CENTER)
    }

    private def initializePlayView(single:PlayViewSingle,
            windowSize:Dimension):SFrame = {
        initializePlayView1(single,windowSize)(
                setScreenMode(SViewer.SCREEN_PREVIOUS))
    }
    private def initializePlayView1(single:PlayViewSingle,
            windowSize:Dimension)(moreOnClose: =>Unit):SFrame = {
        val imageArea = single.getComponent()
        single.start()
        val sWindow = new SFrame(single.name+"Window",app) {
            override def processClose() {
                setVisible(false)
                moreOnClose
            }
        }
        sWindow.addWindowListener()
        sWindow.getContentPane().add(imageArea)
        sWindow.setBounds(0,0,windowSize.width,windowSize.height)
        sWindow.setBackground(imageArea.getBackground())
        sWindow
    }

    private def showPrintList(b:Boolean) {
        printableLister.setVisible(b)
        val split = printableLister.getParent.asInstanceOf[JSplitPane]
        split.resetToPreferredSizes()
    }

    private def hasAlternateScreen():Boolean =
        (getAlternateGraphicsConfiguration(1)!=null)

    /** Get an alternate Graphics Configuration.
     * @param n The index of the config to get, 0 is the primary screen.
     * @return The indicated config, or null if it does not exist.
     */
    private def getAlternateGraphicsConfiguration(n:int):
            GraphicsConfiguration  = {
        if (n<0)
            throw new IllegalArgumentException("negative index not valid")
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gs:Array[GraphicsDevice] = ge.getScreenDevices()

        val configs = for {
            i <- 0 until gs.length
            gd = gs(i)
            gc = gd.getConfigurations
            j <- 0 until gc.length
        } yield gc(j)
        if (configs.size < n+1)
            return null         //no such config
        configs(n)      //a GraphicsConfiguration
    }

    private def addToActive(list:PlayListS, index:Int) {
        //Add the specified item to the currently active
        //target playlist, typically the Printable playlist.
        if (index<0)
            return      //ignore it when nothing selected
        val item = list.getItem(index)
        //TODO - allow selecting other lists as the active list
        printableTracker ! PlayListRequestAdd(printablePlayList,item)
    }

    private def showImageInfoDialog(pl:PlayListS, idx:Int) {
        var fileInfo = getFileInfo(pl, idx)
        if (fileInfo==null) {
            val msg = getResourceString("error.NoImageSelected")
            errorDialog(msg)
        } else
            infoDialog(fileInfo.info)
    }

    private def showImageEditDialog(pl:PlayListS, idx:Int) {
        var fileInfo = getFileInfo(pl, idx)
        if (fileInfo==null) {
            val msg = getResourceString("error.NoImageSelected")
            errorDialog(msg)
        } else {
            val title = getResourceFormatted("prompt.TextForImage",
                    fileInfo.getPath)
            val newImageText:String = editTextDialog(title,fileInfo.text)
            if (newImageText!=null)
                setImageFileText(pl,idx,fileInfo,newImageText)
        }
    }

    private def getFileInfo(pl:PlayListS, idx:Int):FileInfo = {
        if (idx<0)
            return null         //no image selected
        val item = pl.getItem(idx)
        val itemFile = new File(item.getBaseDir,item.getFileName)
        val fileInfo:FileInfo = new FileInfo(idx,0,0,pl.size,
                itemFile.getParentFile(), itemFile.getName())
        if (!fileInfo.infoLoaded)
            fileInfo.loadInfo(true)     //TODO - includeDirectoryDates flag?
        fileInfo
    }

    private def setImageFileText(pl:PlayListS, idx:Int,
            fileInfo:FileInfo, text:String) = {
        val path = fileInfo.getPath
        writeFileText(path,text)
        val item = pl.getItem(idx)
        mainTracker ! PlayListRequestChange(pl,idx,item)
            //Make the listeners update their display of this item
    }

    //Write out new text for the specified image file
    private def writeFileText(imagePath:String, text:String) {
        if (imagePath==null)
            return              //no file, so no info
        val textToWrite = if (text.endsWith("\n")) text else text+"\n"
        val textPath = FileInfo.getTextFileNameForImage(imagePath)
        val f = new File(textPath)
        FileUtilS.writeFile(f,text)
    }

    /** Save the current layout to a named file. */
    private def saveLayoutTemplateAs() {
        val prompt = getResourceString("prompt.SaveLayoutTemplateAs")
        val fOpt = fileSaveDialog(prompt,lastSaveLayoutTemplateFile)
        if (fOpt.isEmpty)
            return
        lastSaveLayoutTemplateFile = fOpt
        val pwOpt = getPrintWriterFor(fOpt.get)
        if (pwOpt.isEmpty)
            return         //cancelled
        printableMulti.saveLayoutTemplate(pwOpt.get)
        pwOpt.get.close()
        val status = getResourceFormatted("status.SavedTemplateToFile",
                fOpt.get.toString())
        showStatus(status)
    }

    /** Load a layout from a named file. */
    private def loadLayoutTemplate() {
        val prompt = getResourceString("prompt.LoadLayoutTemplate");
        val fOpt = fileOpenDialog(prompt,lastLoadLayoutTemplateFile);
        if (fOpt.isEmpty)
            return
        lastLoadLayoutTemplateFile = fOpt
        loadLayoutTemplate(fOpt.get)
    }

    //Load the specified layout template
    def loadLayoutTemplate(f:File) {
        if (printableMulti==null) {
            val msg = getResourceString("error.NoPrintablePage");
            showStatus(msg)
            return
        }
        printableMulti.loadLayoutTemplate(f);
        val status = getResourceFormatted(
                "status.LoadedTemplateFromFile",f.toString())
        showStatus(status)
    }

    //Edit the description of the current page layout
    private def editLayoutDescription() {
        if (printableMulti==null) {
            val msg = getResourceString("error.NoPrintablePage")
            showStatus(msg)
            return
        }
        printableMulti.editLayoutDescription
    }
}

object SViewer {
    val SCREEN_PREVIOUS = -1
    val SCREEN_SLIDESHOW = 0
    val SCREEN_FULL = 1
    val SCREEN_PRINT = 2
    val SCREEN_ALT = 3
    val SCREEN_DUAL = 4
    val SCREEN_MODE_DEFAULT = SCREEN_SLIDESHOW
}

sealed abstract class SViewerRequest
case class SViewerRequestClose() extends SViewerRequest
case class SViewerRequestFileOpen() extends SViewerRequest
case class SViewerRequestActivate(list:PlayListS) extends SViewerRequest
case class SViewerRequestFocus(list:PlayListS) extends SViewerRequest
case class SViewerRequestScreenMode(mode:Int) extends SViewerRequest
case class SViewerRequestInfoDialog(list:PlayListS,index:Int)
        extends SViewerRequest
case class SViewerRequestEditDialog(list:PlayListS,index:Int)
        extends SViewerRequest
case class SViewerRequestAddToActive(list:PlayListS,index:Int)
        extends SViewerRequest

/*
vi:
*/
