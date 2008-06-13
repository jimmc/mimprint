package net.jimmc.mimprint

import net.jimmc.swing.SwingS
import net.jimmc.util.FileUtilS

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.AbstractListModel
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane

import scala.util.Sorting

class PlayViewList(tracker:PlayListTracker) extends PlayView(tracker) {
    //TODO - add modes (display icons; include file info)
    //TODO - add thread that loads image icons
    //TODO - implement dragging to Printable view or other PlayViewList
    //TODO - add support for viewing our MIMPRINT (mpr) template files
    private val dirBgColor = new Color(0.9f, 0.8f, 0.8f)
    private var includeDirectoryDates = false

    private var playList:PlayListS = _
    private var targetDirectory:File = _
    private var playableFileNames:Array[String] = _
    private var dirNames:Array[String] = _
    private var dirCount:Int = _
    private var fileNames:Array[String] = _
    private var fileCount:Int = _
    private var fileInfos:Array[FileInfo] = _
    private var fileNameList:JList = _
    private var fileNameListModel:ArrayListModel = _
    private var currentSelection = -1

    def getComponent():Component = {
        val p = new JPanel()
        fileNameList = new JList()
        fileNameList.setCellRenderer(new PlayViewListCellRenderer())
        fileNameList.addListSelectionListener(
                new PlayViewListSelectionListener())
        val listScrollPane = new JScrollPane(fileNameList)
        listScrollPane.setPreferredSize(new Dimension(250,400))
        p.setLayout(new BorderLayout())
        p.add(listScrollPane)
        p
    }

    protected def playListInit(m:PlayListInit) {
        playList = m.list
        currentSelection = -1
        redisplayList
    }

    protected def playListAddItem(m:PlayListAddItem) {
        println("PlayViewList.playListAddItem NYI")     //TODO
    }

    protected def playListRemoveItem(m:PlayListRemoveItem) {
        println("PlayViewList.playListRemoveItem NYI")  //TODO
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        playList = m.newList
        redisplayList
        val newSelection = m.index + dirCount
        //If the selected item got changed, we have to re-highlight it
        if (newSelection==currentSelection)
            setSelectedIndex(currentSelection)
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        val newSelection = m.index + dirCount
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
    private def redisplayList() {
        targetDirectory = playList.baseDir
        playableFileNames = playList.getFileNames
        dirNames = getDirNames(targetDirectory)
        dirCount = dirNames.length
        fileCount = playableFileNames.length
        fileNames = dirNames ++ playableFileNames
        val newFileInfos = new Array[FileInfo](fileNames.length)
        for (i <- 0 until fileNames.length) {
            newFileInfos(i) =
                new FileInfo(i,dirCount,fileCount,targetDirectory,fileNames(i))
        }
        //Do the actual updating on the event thread to avoid race conditions
        SwingS.invokeLater {
            try {
                appIsUpdatingModel = true
                fileInfos = newFileInfos
                fileNameListModel = new ArrayListModel(fileNames)
                fileNameList.setModel(fileNameListModel)
                fileNameList.updateUI()
            } finally {
                appIsUpdatingModel = false
            }
        }
    }
    private var appIsUpdatingModel = false

    private def getDirNames(dir:File):Array[String] = {
        if (dir==null)
            return Array()
        //Get the list of all subdirectories except current and parent
        val subdirs = FileUtilS.listDir(dir,(
                (dir,name)=>new File(dir,name).isDirectory &&
                name!="." && name!=".."))
        Sorting.stableSort(subdirs,
                    (s1:String,s2:String)=>FileInfo.compareFileNames(s1,s2)<0)
        //Put current first, parent second, followed by sorted subdirs
        Array(".","..") ++ subdirs
    }

    //Get the FileInfo for the specified file in the fileNames list
    private def getFileInfo(index:Int):FileInfo = {
        val fileInfo = fileInfos(index)
        if (fileInfo==null || !fileInfo.infoLoaded) {
            //not loaded, need to load it
            fileInfo.loadInfo(includeDirectoryDates)
            //leave icon null, let iconLoader fill it in
          /* TODO
            if (listMode==MODE_FULL)
                iconLoader.moreIcons();         //tell iconLoader to load icons
          */
        }
        fileInfo
    }

    class PlayViewListCellRenderer extends DefaultListCellRenderer {
        override def getListCellRendererComponent(list:JList,
                value:Object, index:Int,
                isSelected:Boolean, cellHasFocus:Boolean):Component = {
            val cell = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus).
                        asInstanceOf[JLabel]
            val fileInfo:FileInfo = getFileInfo(index)
            val fileInfoText = fileInfo.html;
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
        /* TODO
            if (listMode==MODE_FULL)
                cell.setIcon(fileInfo.icon)
        */
            //If this item is a directory folder rather than an image,
            //color it differently in the list.
            if (!isSelected && fileInfo.isDirectory()) {
                cell.setBackground(dirBgColor)
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

    private def listValueChanged() {
        currentSelection = fileNameList.getSelectedIndex()
        if (currentSelection<0)
            return              //nothing selected now
        if (currentSelection>=dirCount) {
            //we have selected a file
            //TODO - check to see if it is an MPR file (e.g. our template)
            val listIndex = currentSelection - dirCount
            //Send the select request to our PlayListTracker
            tracker ! PlayListRequestSelect(playList,listIndex)
        } else {
            //A directory has been selected
            if (!appIsSelecting) {
                //If user clicked (rather than using up or down arrows
                //in the main window), then we open that directory.
                val fileInfo = getFileInfo(currentSelection)
                tracker.load(fileInfo.getPath)
            }
        }
    }
}
