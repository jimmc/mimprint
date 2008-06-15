package net.jimmc.mimprint

import net.jimmc.swing.AboutWindow
import net.jimmc.swing.SButton
import net.jimmc.swing.SFrame
import net.jimmc.swing.SMenuItem
import net.jimmc.swing.SwingS
import net.jimmc.util.AsyncUi
import net.jimmc.util.FileUtil
import net.jimmc.util.Subscribe
import net.jimmc.util.Subscriber
import net.jimmc.util.UserException

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.io.File
import java.io.FileNotFoundException
import javax.swing.JToolBar
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JSplitPane
import javax.swing.JTextField

import scala.actors.Actor
import scala.actors.Actor.loop

class SViewer(app:AppS) extends SFrame("Mimprint",app) with AsyncUi
        with Actor with Subscriber[PlayListMessage] {
//TODO - implement ToolPrompter interface (to get menu toolPrompts)

    private val mainTracker = new PlayListTracker(this)
    private val toolBar = createToolBar()
    private var mainList:PlayViewList = _
    private var mainSingle:PlayViewSingle = _
    private var currentMainFile:File = _
    private var playList:PlayListS = _  //current main play list
    private var playListIndex:Int = -1  //currently selected item in main list

    setJMenuBar(createMenuBar())
    initForm()
    setScreenMode(SViewer.SCREEN_MODE_DEFAULT)
    pack()

    addWindowListener()

    private def createMenuBar():JMenuBar = {
        val mb = new JMenuBar()
        mb.add(createFileMenu())
        mb.add(createImageMenu())
        //TODO - create PlayList menu
        //TODO - create Layout menu
        //TODO - create View menu
        mb.add(createHelpMenu())

        mb
    }

    private def createFileMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.File.label"))

        m.add(new SMenuItem(this,"menu.File.Open")(processFileOpen))
        m.add(new SMenuItem(this,"menu.File.Exit")(processFileExit))
        //TODO - add Print menu item

        m
    }

    private def createImageMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.Image.label"))

        m.add(new SMenuItem(this,"menu.Image.PreviousImage")(
                mainTracker ! PlayListRequestUp(playList)))
        m.add(new SMenuItem(this,"menu.Image.NextImage")(
                mainTracker ! PlayListRequestDown(playList)))
        m.add(new SMenuItem(this,"menu.Image.PreviousDirectory")(
                mainTracker ! PlayListRequestLeft(playList)))
        m.add(new SMenuItem(this,"menu.Image.NextDirectory")(
                mainTracker ! PlayListRequestRight(playList)))

        m.add(new JSeparator())

        val mr = new JMenu(getResourceString("menu.Image.RotateMenu.label"))
        m.add(mr)

        mr.add(new SMenuItem(this,"menu.Image.RotateMenu.R90")(
                requestRotate(1)))
        mr.add(new SMenuItem(this,"menu.Image.RotateMenu.R180")(
                requestRotate(2)))
        mr.add(new SMenuItem(this,"menu.Image.RotateMenu.R270")(
                requestRotate(-1)))
        
        //TODO - add AddCurrentToActive command

        m.add(new SMenuItem(this,"menu.Image.ShowEditDialog")(
                showImageEditDialog(playList,playListIndex)))
        m.add(new SMenuItem(this,"menu.Image.ShowInfoDialog")(
                showImageInfoDialog(playList,playListIndex)))

        m
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
        if (newMainFile!=null) {
            currentMainFile = newMainFile
            mainTracker.load(newMainFile.getPath)
        }
    }

    //Closing this window causes the app to exit
    override def processClose() = processFileExit

    //Don't ask about exiting, just do it
    override def confirmExit():Boolean = true

    private def createToolBar():JToolBar = {
        val tb = new JToolBar()
        tb.setRollover(true)

        tb.add(new SButton(this,"button.ModeDual")(
                setScreenMode(SViewer.SCREEN_DUAL_WINDOW)))
        tb.add(new SButton(this,"button.ModeFull")(
                setScreenMode(SViewer.SCREEN_FULL)))

        tb.addSeparator()
        tb.add(new SButton(this,"button.PreviousFolder")(
                mainTracker ! PlayListRequestLeft(playList)))
        tb.add(new SButton(this,"button.PreviousImage")(
                mainTracker ! PlayListRequestUp(playList)))
        tb.add(new SButton(this,"button.NextImage")(
                mainTracker ! PlayListRequestDown(playList)))
        tb.add(new SButton(this,"button.NextFolder")(
                mainTracker ! PlayListRequestRight(playList)))

        tb.addSeparator()
        tb.add(new SButton(this,"button.RotateCcw")(
                requestRotate(1)))
        tb.add(new SButton(this,"button.RotateCw")(
                requestRotate(-1)))
        //We don't put in a rotate-180 button, let user push the other twice

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
            currentMainFile = new File(fileName)
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
        mainTracker ! Subscribe(this)
        mainTracker ! PlayListRequestInit(this)
        this ! SViewerRequestFocus(null)
        loop { react (handleMessage) }
    }

    val handleMessage : PartialFunction[Any,Unit] = {
        case m:PlayListMessage => processPlayListMessage(m)
        case m:SViewerRequestClose => processClose
        case m:SViewerRequestFileOpen => processFileOpen
        case m:SViewerRequestActivate =>
                mainList ! PlayViewListRequestActivate(m.list)
        case m:SViewerRequestFocus =>
                mainSingle ! PlayViewSingleRequestFocus()
        case m:SViewerRequestScreenMode =>
                setScreenMode(m.mode)
        case m:SViewerRequestInfoDialog =>
                showImageInfoDialog(m.list,m.index)
        case m:SViewerRequestEditDialog =>
                showImageEditDialog(m.list,m.index)
        case m:SViewerRequestAddToActive =>
                println("RequestAddToActive NYI")        //TODO
    }

    private def processPlayListMessage(msg:PlayListMessage) {
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
                playListIndex = m.index
                val fn = playList.getItem(playListIndex).getFileName
                setTitleToFileName(fn)
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

    private def setScreenMode(mode:Int) {
        println("screen modes NYI (mode "+mode+")")   //TODO
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
        val fileInfo:FileInfo = new FileInfo(idx,0,pl.size,
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
        FileUtil.writeFile(f,text)
    }
}

object SViewer {
    val SCREEN_PREVIOUS = -1
    val SCREEN_SLIDESHOW = 0
    val SCREEN_FULL = 1
    val SCREEN_PRINT = 2
    val SCREEN_ALT = 3
    val SCREEN_DUAL_WINDOW = 4
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
