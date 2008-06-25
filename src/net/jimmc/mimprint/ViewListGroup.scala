/* ViewListGroup.scala
 *
 * Jim McBeath, June 24, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.SCheckBoxMenuItem
import net.jimmc.swing.SMenuItem

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSplitPane

class ViewListGroup(name:String, viewer:SViewer, tracker:PlayListTracker) {
    private var playViewList:PlayViewList =
            new PlayViewList(name+"List", viewer, tracker)
    private var playViewSingle:PlayViewSingle =
            new PlayViewSingle(name+"Single", viewer, tracker)

    private var groupComp:Component = _
    private var split:JSplitPane = _
    private var singleComp:Component = _
    private var mShowFileInfo:SCheckBoxMenuItem = _
    private var mShowDirDates:SCheckBoxMenuItem = _
    private var mShowSingleViewer:SCheckBoxMenuItem = _

    def getComponent():Component = {
        val listComp = playViewList.getComponent()
        singleComp = playViewSingle.getComponent()
        val w = listComp.getPreferredSize.width
        singleComp.setPreferredSize(new Dimension(w,w*3/4))
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                listComp,singleComp)
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
        val title = viewer.getResourceString("menu.List.title."+name)
        val label = new JLabel(title)
        mb.add(label)
        val m = new JMenu(viewer.getResourceString("menu.List.label"))
            //TODO - include a little down array by this label
            //so the user has an idea that there is a menu here
            //in this unusual place.

        //Add our menu items
        mShowFileInfo = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowFileInfo")(
                    showFileInfo(mShowFileInfo.getState))
        mShowFileInfo.setState(true)
        m.add(mShowFileInfo)

        mShowDirDates = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowDirDates")(
                    showDirDates(mShowDirDates.getState))
        mShowDirDates.setState(playViewList.includeDirectoryDates)
        m.add(mShowDirDates)

        mShowSingleViewer = new SCheckBoxMenuItem(
                viewer,"menu.List.ShowSingleViewer")(
                    showSingleViewer(mShowSingleViewer.getState))
        mShowSingleViewer.setState(true)
        m.add(mShowSingleViewer)
        showSingleViewer(mShowSingleViewer.getState)
                //make sure window state is in sync with menu item state

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

    private def showFileInfo(b:Boolean) {
        playViewList.showFileInfo(b)
        mShowDirDates.setEnabled(b)
    }

    private def showDirDates(b:Boolean) {
        playViewList.includeDirectoryDates = b
        playViewList.redisplayList()
    }

    private def showSingleViewer(b:Boolean) {
        if (b)
            split.setBottomComponent(singleComp)
        else
            split.remove(singleComp)
    }
}
