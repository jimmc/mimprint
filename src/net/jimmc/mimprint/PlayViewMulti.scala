/* PlayViewMulti.scala
 *
 * Jim McBeath, June 17, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.PFCatch
import net.jimmc.util.Subscribe

import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import java.io.PrintWriter
import javax.swing.JLabel
import javax.swing.JPanel

import scala.actors.Actor.loop

/** View all of the images in a PlayList. */
class PlayViewMulti(name:String,
        viewer:SViewer,
        tracker:PlayListTracker,        //the printable list
        mainTracker:PlayListTracker)   //the main list
        extends PlayViewComp(name, viewer, tracker) {

    private var areaPage:AreaPage = _
    private var areaPageControls:AreaPageControls = _
    private var playList:PlayList = _
    private var playListIndex:Int = _

    private var panel:JPanel = _
    private var areaPanel:JPanel = _
    private var controlPanel:JPanel = _

    private var currentPage = 0

    def getComponent():Component = {
        panel = new JPanel()
        panel.setLayout(new BorderLayout())
        areaPanel = createAreaPanel()
        controlPanel = createControlPanel()
        areaPage.controls = areaPageControls
        panel.add(areaPanel,BorderLayout.CENTER)
        panel.add(controlPanel,BorderLayout.NORTH)
        panel
    }

    def isShowing():Boolean = panel.isShowing

    private def createAreaPanel() = {
        val p = new JPanel()
        p.setLayout(new BorderLayout())
        areaPage = new AreaPage(viewer, tracker)
        p.add(areaPage,BorderLayout.CENTER)
        p
    }

    private def createControlPanel() = {
        val p = new JPanel()
        areaPageControls = new AreaPageControls(viewer,this,areaPage)
        p.add(areaPageControls)
        p
    }

    protected[mimprint] def setPageNumber(n:Int) {
        val imagesPerPage = areaPage.getImageAreaCount
        val pages = (playList.size+imagesPerPage - 1)/imagesPerPage
        currentPage = if (n<0) 0 else if (n>=pages) pages - 1 else n
        refreshAreas
    }

    private def recalculatePageCount() {
        val imagesPerPage = areaPage.getImageAreaCount
        val newPageCount = (playList.size+imagesPerPage - 1)/imagesPerPage
        areaPageControls.setPageCount(newPageCount)
        if (currentPage>=newPageCount) {
            currentPage = newPageCount - 1
            if (currentPage<0)
                currentPage = 0
            areaPageControls.setPageNumberDisplay(currentPage)
        }
    }

    protected[mimprint] def refreshAreas() {
        recalculatePageCount()
        val imagesPerPage = areaPage.getImageAreaCount
        areaPage.displayPlayList(playList,currentPage*imagesPerPage)
        areaPage.repaint()
    }

    override def act() {
        tracker ! Subscribe(this)
        tracker ! PlayListRequestInit(this)
        mainTracker ! Subscribe(this)
        loop {
            react (PFCatch(
                    handleMainPlayListMessage orElse handlePlayListMessage
                    orElse handleOtherMessage,"PlayViewMulti "+name, viewer))
        }
    }

    private val handleMainPlayListMessage : PartialFunction[Any,Unit] = {
        case m:PlayListSelectItem if (m.tracker==mainTracker) =>
            mainPlayListSelect(m)
        case m:PlayListMessage if (m.tracker==mainTracker) =>
            //match and ignore any other messages from the mainTracker
    }

    //When we get a select on the main playlist, see if the file has
    //our extension.  If so, try to load it as a layout template file.
    private def mainPlayListSelect(m:PlayListSelectItem) {
        if (m.index<0 || m.index>=m.list.size)
            return      //nothing selected
        val item = m.list.getItem(m.index)
        if (item.fileName.endsWith("."+FileInfo.MIMPRINT_EXTENSION))
            loadLayoutTemplate(new File(item.baseDir,item.fileName))
    }

    protected def playListInit(m:PlayListInit) {
        playList = m.list
        playListIndex = -1
        refreshAreas()
    }

    protected def playListAddItem(m:PlayListAddItem) {
        playList = m.newList
        if (playListIndex >= 0 && playListIndex >= m.index)
            playListIndex = playListIndex + 1
        refreshAreas()
    }

    protected def playListRemoveItem(m:PlayListRemoveItem) {
        playList = m.newList
        if (playListIndex >= 0) {
	    if (playListIndex > playList.size -1) {
                playListIndex = -1
            } else if (playListIndex == m.index) {
                //playListIndex = -1
            } else if (playListIndex > m.index)
                playListIndex = playListIndex - 1
        }
        refreshAreas()
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        playList = m.newList
        refreshAreas()
    }

    protected def playListUpdateItem(m:PlayListUpdateItem) {
        refreshAreas()
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        playListIndex = m.index
        refreshAreas()
    }

    protected def playListChangeList(m:PlayListChangeList) {
        playList = m.newList
        playListIndex = -1
        refreshAreas()
    }

    override protected val handleOtherMessage : PartialFunction[Any,Unit] = {
        case m:PlayViewMultiRequestFocus => areaPage.requestFocus()
        case m:PlayViewMultiRequestPrint => areaPage.print()
        case m:Any => println("Unrecognized message to PlayViewMulti")
    }

    //Save our layout template to the specified output stream
    def saveLayoutTemplate(pw:PrintWriter) {
        areaPage.writeLayoutTemplate(pw)
    }

    //Load a layout template from the specified file
    def loadLayoutTemplate(f:File) {
        areaPage.loadLayoutTemplate(f)
    }

    //Edit the description of the current layout
    def editLayoutDescription() {
        areaPage.editLayoutDescription()
    }
}

sealed abstract class PlayViewMultiRequest
case class PlayViewMultiRequestFocus()
case class PlayViewMultiRequestPrint()
