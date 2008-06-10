package net.jimmc.mimprint

import net.jimmc.swing.MenuAction
import net.jimmc.swing.SButton
import net.jimmc.swing.SFrame
import net.jimmc.util.UserException

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.JToolBar
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextField

class SViewer(app:AppS) extends SFrame("Mimprint",app) {

    val toolBar = createToolBar()
    setJMenuBar(createMenuBar())
    initForm()
    //setScreenMode(SCREEN_MODE_DEFAULT)
    pack()

    addWindowListener()
    //End of constructor code

    private def createMenuBar():JMenuBar = {
        val mb = new JMenuBar()
        mb.add(createFileMenu())
        mb
    }

    private def createFileMenu():JMenu = {
        val m = new JMenu(getResourceString("menu.File.label"))

        val exitLabel = getResourceString("menu.File.Exit.label")
        m.add(new MenuAction(exitLabel) {
            override def action() = processFileExit
        })

        m
    }

    //Closing this window causes the app to exit
    override def processClose() = processFileExit

    private def createToolBar():JToolBar = {
        val tb = new JToolBar()
        tb.setRollover(true)

        tb.add(createModeDualButton())
        //tb.add(createModeFullButton())

        tb.addSeparator()
        //tb.add(createPreviousFolderButton())
        //TODO

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

val imageArea = new JLabel("Placeholder for Image Area")
imageArea.setBackground(Color.gray)
val imageLister = new JLabel("Placeholder for the Image Lister")

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
}
