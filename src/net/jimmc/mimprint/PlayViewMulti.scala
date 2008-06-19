package net.jimmc.mimprint

import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JPanel

/** View all of the images in a PlayList. */
class PlayViewMulti(name:String, viewer:SViewer, tracker:PlayListTracker)
        extends PlayViewComp(name, viewer, tracker) {

    private var areaPage:AreaPage = _
    private var areaPageControls:AreaPageControls = _
    private var playList:PlayListS = _
    private var playListIndex:Int = _

    private var panel:JPanel = _
    private var areaPanel:JPanel = _
    private var controlPanel:JPanel = _

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
        areaPage = new AreaPage(viewer)
        p.add(areaPage,BorderLayout.CENTER)
        p
    }

    private def createControlPanel() = {
        val p = new JPanel()
        areaPageControls = new AreaPageControls(viewer,this,areaPage)
        p.add(areaPageControls)
        p
    }

    protected[mimprint] def refreshAreas() {
        areaPage.displayPlayList(playList)
        areaPage.repaint()
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
            if (playListIndex == m.index) {
                playListIndex = -1
            } else if (playListIndex > m.index)
                playListIndex = playListIndex - 1
        }
        refreshAreas()
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        playList = m.newList
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
}

sealed abstract class PlayViewMultiRequest
case class PlayViewMultiRequestFocus
case class PlayViewMultiRequestPrint
