/* PlayViewList.scala
 *
 * Jim McBeath, June 12, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.SwingS
import net.jimmc.swing.SDragSource
import net.jimmc.swing.SMenuItem
import net.jimmc.util.FileUtilS
import net.jimmc.util.StdLogger

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.dnd.DragSourceAdapter
import java.awt.dnd.DragSourceDropEvent
import java.awt.dnd.DragSourceListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Image
import java.awt.Point
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.AbstractListModel
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane

import scala.util.Sorting

class PlayViewList(name:String,viewer:SViewer,tracker:PlayListTracker)
        extends PlayViewComp(name,viewer,tracker)
	with SDragSource with StdLogger {
    //TODO - add modes (display icons; include file info)
    //TODO - add thread that loads image icons
    //TODO - implement dropping into this list
    //TODO - add support for viewing our MIMPRINT (mpr) template files
    private val pathBgColor = new Color(0.9f, 0.9f, 0.8f)
    private val subdirBgColor = new Color(0.9f, 0.8f, 0.8f)
    val includeDirectories = true
    protected[mimprint] var includeDirectoryDates = false
    protected[mimprint] var includeIcons = false
    private var showingFileInfo = false

    private var playList:PlayList = _
    private var targetDirectory:File = _
    private var playableFileNames:Array[String] = _
    private var pathNames:Array[String] = _
    private var pathCount:Int = _
    private var subdirNames:Array[String] = _
    private var subdirCount:Int = _
    private var fileNames:Array[String] = _
    private var fileCount:Int = _
    private var fileInfos:Array[FileInfo] = _
    private var ourComponent:Component = _
    private var fileNameList:JList = _
    private var fileNameListModel:ArrayListModel = _
    private var currentSelection = -1
        //JList index of selected item; if an image is selected, this value
        // is == imageIndex+pathCount+subdirCount
    private def currentImageSelection =
        currentSelection - (pathCount + subdirCount)
    def baseDir = playList.baseDir

    private var noContextMenu:JPopupMenu = _
    private var pathContextMenu:JPopupMenu = _
    private var subdirContextMenu:JPopupMenu = _
    private var fileContextMenu:JPopupMenu = _
    private var fileContextMenuTitle:JLabel = _

    private val iconLoader = new IconLoader(viewer) {
        def iconLoaded(fileInfos:Array[FileInfo], n:Int):Boolean =
            PlayViewList.this.iconLoaded(fileInfos,n)
        def getFileInfoList():Array[FileInfo] = PlayViewList.this.fileInfos
    }
    iconLoader.setPriority(iconLoader.getPriority() - 3)

    def getComponent():Component = {
        val p = new JPanel()
        fileNameList = new JList()
        fileNameList.setAutoscrolls(false)
            //Autoscroll and drag-and-drop interfere with each other
            //(see <http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4449146>)
            //so turn off autoscroll.
        showFileInfo(true)
        fileNameList.addMouseListener(new PlayViewListMouseListener())
        fileNameList.addListSelectionListener(
                new PlayViewListSelectionListener())
        val listScrollPane = new JScrollPane(fileNameList)
        listScrollPane.setPreferredSize(new Dimension(250,400))
        p.setLayout(new BorderLayout())
        p.add(listScrollPane)
        setupDrag(fileNameList,DnDConstants.ACTION_COPY)
        ourComponent = p

        noContextMenu = createNoContextMenu()
        pathContextMenu = createPathContextMenu()
        subdirContextMenu = createSubdirContextMenu()
        fileContextMenu = createFileContextMenu()

        iconLoader.start()

        p
    }

    private def createNoContextMenu():JPopupMenu = {
        val m = new JPopupMenu()

        m.add(new JLabel("This is the No-selection popup menu"))

        m
    }

    private def createPathContextMenu():JPopupMenu = {
        val m = new JPopupMenu()

        m.add(new JLabel("This is the Path popup menu"))

        m
    }

    private def createSubdirContextMenu():JPopupMenu = {
        val m = new JPopupMenu()

        m.add(new JLabel("This is the Subdir popup menu"))

        m
    }

    private def createFileContextMenu():JPopupMenu = {
        val m = new JPopupMenu()

        fileContextMenuTitle = new JLabel("Image")
        m.add(fileContextMenuTitle)

        m.add(new SMenuItem(viewer,"menu.ListContext.Image.Remove")(
                requestRemove()))

        m
    }

    def viewPlayList(rootDir:String) {
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        playList.save(pw,new File(rootDir))
        val s = sw.toString
        viewer.infoDialog("PlayList Contents, baseDir="+rootDir+":\n"+s) //TODO i18n
        pw.close()
        sw.close()
    }

    def isShowing():Boolean = ourComponent.isShowing

    protected def playListInit(m:PlayListInit) {
        playList = m.list
        currentSelection = -1
        redisplayList
    }

    protected def playListAddItem(m:PlayListAddItem) {
        playList = m.newList
        if (m.index<=currentImageSelection)
            currentSelection += 1
        redisplayList
        setSelectedIndex(currentSelection)
    }

    protected def playListRemoveItem(m:PlayListRemoveItem) {
	logger.debug("enter PlayViewList.playListRemoveItem")
        playList = m.newList
	if (currentSelection > playList.size - 1)
            currentSelection = -1
        //else if (m.index==currentImageSelection)
            //currentSelection = -1
        else if (m.index<currentImageSelection)
            currentSelection -= 1
        redisplayList
        setSelectedIndex(currentSelection)
	logger.debug("leave PlayViewList.playListRemoveItem")
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        playList = m.newList
        redisplayList
        //If our selected item got changed, we have to re-highlight it
        if (m.index==currentImageSelection)
            setSelectedIndex(currentSelection)
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        val newSelection = m.index + pathCount + subdirCount
        if (newSelection==currentSelection)
            return              //no change
        setSelectedIndex(newSelection)
    }
    private var appIsSelecting = false
    private def setSelectedIndex(n:Int) {
        SwingS.invokeLater {        //run this on the event thread
            try {
                appIsSelecting = true
                fileNameList.setSelectedIndex(n)
                if (n>=0)
                    fileNameList.ensureIndexIsVisible(n)
            } finally {
                appIsSelecting = false
            }
        }
    }

    protected def playListChangeList(m:PlayListChangeList) {
        playList = m.newList
        currentSelection = -1
        redisplayList
    }

    //Here after updating playList.
    //Calculate the other data required for our list,
    //and redisplay our list.
    protected[mimprint] def redisplayList() {
        targetDirectory = playList.baseDir
        val playableDirs = playList.getBaseDirs
        playableFileNames = playList.getFileNames
        pathNames = getPathNames(targetDirectory)
        val pathParents = getPathParents(pathNames)
        pathCount = pathNames.length
        subdirNames = getSubdirNames(targetDirectory)
        subdirCount = subdirNames.length
        fileCount = playableFileNames.length
        fileNames = pathNames ++ subdirNames ++ playableFileNames
        val dirCount = pathCount + subdirCount
        val newFileInfos:Array[FileInfo] =
            (0 until fileNames.length).map((i:Int) => {
                val ii = i - dirCount
                val fDir = if (i<pathCount) pathParents(i)
                    else if (ii<0) targetDirectory
                    else if (playableDirs(ii).isAbsolute) playableDirs(ii)
                    else new File(targetDirectory,playableDirs(ii).getPath)
                new FileInfo(i,pathCount,subdirCount,fileCount,
                        fDir,fileNames(i))}
            ).toArray
        //Do the actual updating on the event thread to avoid race conditions
        val displayableNames = fileNames.map((s:String) =>
            if (s==null || s=="") "-empty" else s)
        SwingS.invokeLater {
	    logger.debug("enter PlayViewList.redisplayList#invokeLater")
            try {
                appIsUpdatingModel = true
                fileInfos = newFileInfos
                fileNameListModel = new ArrayListModel(displayableNames)
		logger.debug("leave PlayViewList.redisplayList#invokeLater:A")
                fileNameList.setModel(fileNameListModel)
		logger.debug("leave PlayViewList.redisplayList#invokeLater:B")
                fileNameList.updateUI()
            } finally {
                appIsUpdatingModel = false
            }
	    logger.debug("leave PlayViewList.redisplayList#invokeLater")
        }
    }
    private var appIsUpdatingModel = false

    private def getPathNames(f:File):Array[String] = {
        if (f==null || !includeDirectories)
            return Array()
        val dir = if (f.isDirectory) f else f.getParentFile
	//On Windows, File.separator is a backslash, which we have to quote
	//with a backslash in order to work as a regexp for the split call.
        val a = dir.getCanonicalPath.split(
		if (File.separator=="\\") "\\\\" else File.separator)
        //Trim off leading blank
        if (a.length>0 && a(0)=="")
            a.slice(0,a.length)
        else
            a
    }

    private def getPathParents(p:Array[String]):Array[File] = {
        var d = new File(File.separator)        //root
        val dd:Array[File] = p map { s =>
            val f = new File(d,s)
            d = f
            if (f.getParentFile==null) f else f.getParentFile
        }
        dd
    }

    private def getSubdirNames(dir:File):Array[String] = {
        if (dir==null || !dir.isDirectory || !includeDirectories)
            return Array()
        //Get the list of all subdirectories except current and parent
        val subdirs = FileUtilS.listDir(dir,(
                (dir,name)=>new File(dir,name).isDirectory &&
                name!="." && name!=".."))
        Sorting.stableSort(subdirs,
                    (s1:String,s2:String)=>FileInfo.compareFileNames(s1,s2)<0)
        subdirs
    }

    //Get the FileInfo for the specified file in the fileNames list
    private def getFileInfo(index:Int):FileInfo = {
        val fileInfo = fileInfos(index)
        if (fileInfo==null || !fileInfo.infoLoaded) {
            //not loaded, need to load it
            fileInfo.loadInfo(includeDirectoryDates)
            //leave icon null, let iconLoader fill it in
            if (includeIcons)
                iconLoader.moreIcons()         //tell iconLoader to load icons
        }
        fileInfo
    }

    private def iconLoaded(fileInfos:Array[FileInfo], n:Int):Boolean = {
        if (fileInfos!=this.fileInfos) {
            //Our list of files has changes, ignore this call
            return false
        }
        //Refresh the list item
        //We just want to tell the list to recalculate the size
        //of the cell we have just updated, but we can't fire off
        //a change notification here because that method of the
        //model is protected.  Calling revalidate doesn't do the
        //trick, so we hack it by setting the renderer again.
        //Need to check our list mode and only do this if we are
        //in the right mode; need to do that check within a sync
        //block to prevent race condition.
        synchronized {
            if (showingFileInfo)
                fileNameList.setCellRenderer(new PlayViewListCellRenderer())
        }
        true
    }

    override protected val handleOtherMessage : PartialFunction[Any,Unit] = {
        case m:PlayViewListRequestActivate => listValueChanged()
        case m:Any => println("Unrecognized message to PlayViewList")
    }

    def showFileInfo(b:Boolean) {
        if (b)
            fileNameList.setCellRenderer(new PlayViewListCellRenderer())
        else
            fileNameList.setCellRenderer(new DefaultListCellRenderer())
        showingFileInfo = b
    }

    def showFileIcons(b:Boolean)  = includeIcons = b
        //caller should call redisplayList

    class PlayViewListCellRenderer extends DefaultListCellRenderer {
        override def getListCellRendererComponent(list:JList,
                value:Object, index:Int,
                isSelected:Boolean, cellHasFocus:Boolean):Component = {
            val cell = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus).
                        asInstanceOf[JLabel]
            val fileInfo:FileInfo = getFileInfo(index)
            val fileInfoText = fileInfo.html
            val labelText =
                if (fileInfoText==null) {
                    fileInfo.name
                } else {
                    fileInfoText
                        //label doesn't normally do newlines, so we use html and
                        //<br> tags instead.
                }
            cell.setText(labelText)
            //cell.setVerticalAlignment(TOP)      //put text at top left
            //cell.setHorizontalAlignment(LEFT)
            if (includeIcons)
                cell.setIcon(fileInfo.icon)
            //If this item is a directory folder rather than an image,
            //color it differently in the list.
            if (!isSelected && fileInfo.isDirectory()) {
                val c = if (index<pathCount) pathBgColor else subdirBgColor
                cell.setBackground(c)
            }
            cell.setBorder(BorderFactory.createLineBorder(Color.black))
            cell
        }
    }

    class ArrayListModel(a:Array[String]) extends AbstractListModel {
        private var array:Array[String] = a
        def getSize():Int = array.length
        def getElementAt(index:Int) = array(index)
        def FireItemChanged(index:Int):Unit =
                fireContentsChanged(this,index,index)
    }

    class PlayViewListSelectionListener extends ListSelectionListener {
        def valueChanged(ev:ListSelectionEvent) {
            if (!appIsUpdatingModel && !ev.getValueIsAdjusting)
                listValueChanged
        }
    }

    //Send out a select request to cause our listeners to refresh themselves
    def requestSelect() {
        if (tracker!=null && playList!=null)
            tracker ! PlayListRequestSelect(playList,currentImageSelection)
    }

    def requestRemove() {
        if (tracker!=null && playList!=null)
            tracker ! PlayListRequestRemove(playList,currentImageSelection)
    }

    def load(path:String) = tracker.load(path)

    def save(path:String) = tracker.save(path)
    def save(path:String,absolute:Boolean) = tracker.save(path,absolute)

    private def listValueChanged() {
        currentSelection = fileNameList.getSelectedIndex()
        if (currentSelection<0)
            return              //nothing selected now
        if (currentSelection >= (pathCount+subdirCount)) {
            //we have selected a file
            //TODO - check to see if it is an MPR file (e.g. our template)
            //Send the select request to our PlayListTracker
            tracker ! PlayListRequestSelect(playList,currentImageSelection)
        } else {
            //A directory has been selected (path or subdir)
            if (!appIsSelecting) {
                //If user clicked (rather than using up or down arrows
                //in the main window), then we open that directory.
                val fileInfo = getFileInfo(currentSelection)
                tracker.load(fileInfo.getPath)
            }
        }
        viewer ! SViewerRequestFocus(playList)
    }

    //The SDragSource trait
    class PlayViewListDragGestureListener extends DragGestureListener {
        def dragGestureRecognized(ev:DragGestureEvent) {
            val index = fileNameList.getSelectedIndex()
            if (index == -1)
                return         //no item selected for dragging

            //Whether or not the drag succeeds, we have now selected this
            //item in the list.  In order to keep things in sync, send
            //out a select notification.
            val selIndex = index - (pathCount + subdirCount)
            //Send a select request to our PlayListTracker
            if (selIndex >= 0)
                tracker ! PlayListRequestSelect(playList,selIndex)

            val fileInfo = getFileInfo(index)
            //String path = new File(targetDirectory,fileNames[index]).toString()
            val path = fileInfo.getPath()
            val icon:ImageIcon = fileInfo.icon
            //If the platform does not support image dragging, don't do it
            val iconImage:Image = if (icon==null) null else icon.getImage()
            val (image:Image, offset:Point) =
            if (DragSource.isDragImageSupported()) {
                val im = ImageUtil.createTransparentIconImage(
                        ourComponent,iconImage,path)
                val width = im.getWidth(null)
                val height = im.getHeight(null)
                val p = new Point(-width/2, -height/2)
                (im, p)
            } else {
                (null, null)   //image dragging not supported
            }
            startImageDrag(ev,image,offset,path)
        }
    }
    protected def getMyDragGestureListener():DragGestureListener =
        new PlayViewListDragGestureListener()

    protected def getMyDragSourceListener():DragSourceListener = {
        new DragSourceAdapter() {
            override def dragDropEnd(ev:DragSourceDropEvent) =
                viewer ! SViewerRequestFocus(playList)
        }
    }
    //end SDragSource trait

    class PlayViewListMouseListener extends MouseAdapter {
        override def mousePressed(ev:MouseEvent) = maybeShowPopup(ev)
        override def mouseReleased(ev:MouseEvent) = maybeShowPopup(ev)
        private def maybeShowPopup(ev:MouseEvent):Boolean = {
            if (ev.isPopupTrigger()) {
                val n = fileNameList.locationToIndex(new Point(ev.getX,ev.getY))
                val m =
                    if (n<0) noContextMenu
                    else if (n<pathCount) pathContextMenu
                    else if (n<pathCount+subdirCount) subdirContextMenu
                    else fileContextMenu
                //For now, only show the fileContextMenu
                if (m==fileContextMenu) {
                    val x = n - (pathCount+subdirCount)
                    val item = playList.getItem(x)
                    fileContextMenuTitle.setText(item.fileName)
                    m.show(ev.getComponent,ev.getX,ev.getY)
                    true
                }
                else true
            } else false
        }
    }
}

sealed abstract class PlayViewListRequest
case class PlayViewListRequestActivate(list:PlayList)
