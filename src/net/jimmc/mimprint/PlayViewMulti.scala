package net.jimmc.mimprint

import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JPanel

/** View all of the images in a PlayList. */
class PlayViewMulti(name:String, viewer:SViewer, tracker:PlayListTracker)
        extends PlayViewComp(name, viewer, tracker) {

    private var playList:PlayListS = _
    private var currentIndex:Int = _

    private var panel:JPanel = _
    private var areaPanel:JPanel = _
    private var controlPanel:JPanel = _

    def getComponent():Component = {
        panel = new JPanel()
        panel.setLayout(new BorderLayout())
        areaPanel = createAreaPanel()
        controlPanel = createControlPanel()
        panel.add(areaPanel,BorderLayout.CENTER)
        panel.add(controlPanel,BorderLayout.NORTH)
        panel
    }

    def isShowing():Boolean = panel.isShowing

    private def createAreaPanel() = {
        val p = new JPanel()
        p.setLayout(new BorderLayout())
        p.add(new JLabel("This is the Area Panel"),BorderLayout.NORTH)    //TODO
        p.add(new AreaPage(viewer),BorderLayout.CENTER)
        p
    }

    private def createControlPanel() = {
        val p = new JPanel()
        p.add(new JLabel("This is the Control Panel")) //TODO
        p
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
        //TODO
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        currentIndex = m.index
        //TODO
    }

    protected def playListChangeList(m:PlayListChangeList) {
        playList = m.newList
        currentIndex = -1
        //TODO
    }

    override protected val handleOtherMessage : PartialFunction[Any,Unit] = {
        case m:PlayViewMultiRequestFocus => areaPanel.requestFocus()
        case m:Any => println("Unrecognized message to PlayViewMulti")
    }
}

sealed abstract class PlayViewMultiRequest
case class PlayViewMultiRequestFocus
