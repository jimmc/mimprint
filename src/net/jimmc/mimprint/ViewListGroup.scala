/* ViewListGroup.scala
 *
 * Jim McBeath, June 24, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.SCheckBoxMenuItem
import net.jimmc.swing.SMenu
import net.jimmc.swing.SMenuItem
import net.jimmc.util.SomeOrNone
import net.jimmc.util.SomeOrNone.optionOrNull

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingConstants

class ViewListGroup(name:String, viewer:SViewer, tracker:PlayListTracker) {
    private var playViewList:PlayViewList =
            new PlayViewList(name+"List", viewer, tracker)
    private var playViewSingle:PlayViewSingle =
            new PlayViewSingle(name+"Single", viewer, tracker)

    private var includeDirectories = true
    private var groupComp:Component = _
    private var singleComp:Component = _
    private var mShowFileInfo:SCheckBoxMenuItem = _
    private var mShowFileIcons:SCheckBoxMenuItem = _
    private var mShowDirDates:SCheckBoxMenuItem = _
    private var mShowSingleViewer:SCheckBoxMenuItem = _

    def getComponent():Component = {
        val listComp = playViewList.getComponent()
        singleComp = playViewSingle.getComponent()
        val w = listComp.getPreferredSize.width
        singleComp.setPreferredSize(new Dimension(w,w*3/4))
        val split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                listComp,singleComp)
        split.setResizeWeight(0.8)
        val panel = new JPanel()
        panel.setLayout(new BorderLayout())
        panel.add(split,BorderLayout.CENTER)

        val mb = createOptionsMenuBar()
        panel.add(mb,BorderLayout.NORTH)
        groupComp = panel
        groupComp
    }

    //Create a menu bar with one menu, our Options menu
    private def createOptionsMenuBar():JMenuBar = {
        val mb = new JMenuBar()
        val m = new SMenu(viewer,"menu.List."+name)
        m.setHorizontalTextPosition(SwingConstants.LEFT)

        //Add our menu items
        mShowFileInfo = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowFileInfo")(
                    showFileInfo(mShowFileInfo.getState))
        mShowFileInfo.setState(true)
        m.add(mShowFileInfo)

        mShowFileIcons = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowFileIcons")(
                    showFileIcons(mShowFileIcons.getState))
        mShowFileIcons.setState(false)
        m.add(mShowFileIcons)

        mShowDirDates = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowDirDates")(
                    showDirDates(mShowDirDates.getState))
        mShowDirDates.setState(playViewList.includeDirectoryDates)
        mShowDirDates.setVisible(includeDirectories)
        m.add(mShowDirDates)

        mShowSingleViewer = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowSingleViewer")(
                    showSingleViewer(mShowSingleViewer.getState))
        mShowSingleViewer.setState(true)
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

    def start() {
        playViewList.start()
        playViewSingle.start()
    }

    def requestActivate(playList:PlayListS) {
        playViewList ! PlayViewListRequestActivate(playList)
    }

    def showFileInfo(b:Boolean) {
        playViewList.showFileInfo(b)
        mShowFileInfo.setState(b)
        mShowFileIcons.setEnabled(b)
        mShowDirDates.setEnabled(b)
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
        singleComp.setVisible(b)
        singleComp.getParent.asInstanceOf[JSplitPane].resetToPreferredSizes()
        mShowSingleViewer.setState(b)
        playViewList.requestSelect
    }

    def showDirectories(b:Boolean) {
        playViewList.includeDirectories = b
        if (mShowDirDates!=null)
            mShowDirDates.setVisible(b)
        this.includeDirectories = b
    }

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
