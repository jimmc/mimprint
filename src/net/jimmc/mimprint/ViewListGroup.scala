/* ViewListGroup.scala
 *
 * Jim McBeath, June 24, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.SCheckBoxMenuItem
import net.jimmc.swing.SMenu
import net.jimmc.swing.SMenuItem
import net.jimmc.util.AbledPublisher
import net.jimmc.util.AbledPublisher._
import net.jimmc.util.SomeOrNone
import net.jimmc.util.SomeOrNone.optionOrNull

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingConstants

/** ViewListGroup is a collection of related UI components:
 * a menu bar;
 * a PlayViewList that displays the PlayList being tracked;
 * a PlayViewSingle single-image viewer that shows the currently selected
 *  item in the tracked PlayList.
 */
class ViewListGroup(name:String, viewer:SViewer, tracker:PlayListTracker) {
    vlg:ViewListGroup =>

    val includeDirectories = true

    private val showFileInfoPublisher = new AbledPublisher
    private val showSingleViewerPublisher = new AbledPublisher
    private val showDirectoriesPublisher = new AbledPublisher

    private val playViewList:PlayViewList =
            new PlayViewList(name+"List", viewer, tracker) {
		override val includeDirectories = vlg.includeDirectories
                override protected def playListChangeList(m:PlayListChangeList){
                    super.playListChangeList(m)
                    updateFileLabel
                }
	    }
    private val playViewSingle:PlayViewSingle =
            new PlayViewSingle(name+"Single", viewer, tracker)
    private var fileLabel:JLabel = _

    def getComponent():Component = {
        val listComp = playViewList.getComponent()
        val singleComp = playViewSingle.getComponent()
	showSingleViewerPublisher.subscribe((ev)=> {
	    singleComp.setVisible(ev.state)
	    singleComp.getParent.asInstanceOf[JSplitPane].resetToPreferredSizes()
	})
        val w = listComp.getPreferredSize.width
        singleComp.setPreferredSize(new Dimension(w,w*3/4))
        val split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                listComp,singleComp)
        split.setResizeWeight(0.8)
        val panel = new JPanel()
        panel.setLayout(new BorderLayout())
        panel.add(split,BorderLayout.CENTER)

        val menuPanel = new JPanel()
        //menuPanel.setLayout(new BoxLayout(menuPanel,BoxLayout.Y_AXIS))
        menuPanel.setLayout(new BorderLayout())
        fileLabel = new JLabel("File Name Goes Here")
        val mb = createOptionsMenuBar()
        menuPanel.add(fileLabel,BorderLayout.NORTH)
        menuPanel.add(mb,BorderLayout.CENTER)

        panel.add(menuPanel,BorderLayout.NORTH)
        panel		//This is our group component
    }

    //Create a menu bar with one menu, our Options menu
    private def createOptionsMenuBar():JMenuBar = {
        val mb = new JMenuBar()
        val m = new SMenu(viewer,"menu.List."+name)
        m.setHorizontalTextPosition(SwingConstants.LEFT)

        //Add our menu items
        val mShowFileInfo = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowFileInfo")((cb)=>
                    showFileInfo(cb.getState))
        mShowFileInfo.setState(true)
	showFileInfoPublisher.subscribe((ev)=>
	    mShowFileInfo.setState(ev.state)
	)
        m.add(mShowFileInfo)

        val mShowFileIcons = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowFileIcons")((cb)=>
                    showFileIcons(cb.getState))
        mShowFileIcons.setState(false)
	showFileInfoPublisher.subscribe((ev)=>
	    mShowFileIcons.setState(ev.state)
	)
        m.add(mShowFileIcons)

        val mShowDirDates = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowDirDates")((cb)=>
                    showDirDates(cb.getState))
        mShowDirDates.setState(playViewList.includeDirectoryDates)
        mShowDirDates.setVisible(includeDirectories)
	showFileInfoPublisher.subscribe((ev)=>
	    mShowDirDates.setState(ev.state)
	)
	showDirectoriesPublisher.subscribe((ev)=>
	    mShowDirDates.setVisible(ev.state)
	)
        m.add(mShowDirDates)

        val mShowSingleViewer:SCheckBoxMenuItem = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowSingleViewer")((cb)=>
                    showSingleViewer(cb.getState))
        mShowSingleViewer.setState(true)
	showSingleViewerPublisher.subscribe((ev)=>
	    mShowSingleViewer.setState(ev.state)
	)
        m.add(mShowSingleViewer)
        showSingleViewer(mShowSingleViewer.getState)
                //make sure window state is in sync with menu item state

        m.add(new SMenuItem(viewer,"menu.List.Open")(processOpen))

        val sm = new SMenu(viewer,"menu.List.Save")
        sm.add(new SMenuItem(viewer,"menu.List.Save.Absolute")(
                processSave(true)))
        sm.add(new SMenuItem(viewer,"menu.List.Save.Relative")(
                processSave(false)))
        m.add(sm)

        val sma = new SMenu(viewer,"menu.List.SaveAs")
        sma.add(new SMenuItem(viewer,"menu.List.Save.Absolute")(
                processSaveAs(true)))
        sma.add(new SMenuItem(viewer,"menu.List.Save.Relative")(
                processSaveAs(false)))
        m.add(sma)

        val vcm = new SMenu(viewer,"menu.List.ViewContents")

        vcm.add(new SMenuItem(viewer,"menu.List.ViewContents.Absolute")(
                playViewList.viewPlayList("/")))
        vcm.add(new SMenuItem(viewer,"menu.List.ViewContents.RelativeCurrent")(
                playViewList.viewPlayList(".")))
        m.add(vcm)

        mb.add(m)
        mb
    }

    private def updateFileLabel {
        val fileName = tracker.fileName.getOrElse("(None)")
        fileLabel.setText(fileName)
        fileLabel.setToolTipText(fileName)   //display might be truncated
    }

    def start() {
        playViewList.start()
        playViewSingle.start()
    }

    def requestActivate(playList:PlayList) {
        playViewList ! PlayViewListRequestActivate(playList)
    }

    def showFileInfo(b:Boolean) {
        playViewList.showFileInfo(b)
	showFileInfoPublisher.publish(Abled(b))
    }

    def showFileIcons(b:Boolean) {
        playViewList.showFileIcons(b)
        playViewList.redisplayList()
    }

    def showDirDates(b:Boolean) {
        playViewList.includeDirectoryDates = b
        playViewList.redisplayList()
    }

    def showSingleViewer(b:Boolean) {
	showSingleViewerPublisher.publish(Abled(b))
        playViewList.requestSelect
    }

    /*
    def showDirectories(b:Boolean) {
        playViewList.includeDirectories = b
	showDirectoriesPublisher.publish(Abled(b))
        this.includeDirectories = b
    }
    */

    private def processOpen() {
        val msg = viewer.getResourceString("query.PlayListToOpen")
        val newFile = viewer.fileOrDirectoryOpenDialog(
                msg,SomeOrNone(playViewList.baseDir))
        newFile.foreach(f => playViewList.load(f.getPath))
    }

    private def processSave(absolute:Boolean) {
        tracker.save(absolute)
    }

    private def processSaveAs(absolute:Boolean) {
        val msg = viewer.getResourceString("query.PlayListToSave")
        val newFile = viewer.fileSaveDialog(
                msg,SomeOrNone(playViewList.baseDir))
        newFile.foreach(f => tracker.save(f.getPath,absolute))
    }
}
