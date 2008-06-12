package net.jimmc.mimprint

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.Image
import java.awt.MediaTracker
import java.io.File
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.SwingConstants

class PlayViewSingle(tracker:PlayListTracker) extends PlayView(tracker) {
    private var imageComponent:JLabel = _
    private var mediaTracker:MediaTracker = _
    private var playList:PlayListS = _
    private var currentIndex:Int = -1

    def getComponent():Component = {
        imageComponent = new JLabel()
        imageComponent.setBackground(Color.gray)
        imageComponent.setForeground(Color.white)
        imageComponent.setPreferredSize(new Dimension(800,600))
        imageComponent.setHorizontalAlignment(SwingConstants.CENTER)
        //TODO add key, mouse, mousemotion listeners
        imageComponent.addComponentListener(
                new PlayViewSingleComponentListener())
        mediaTracker = new MediaTracker(imageComponent)
        imageComponent
    }

    protected def playListInit(m:PlayListInit) {
        playList = m.list
        currentIndex = -1
    }

    protected def playListAddItem(m:PlayListAddItem) {
        println("PlayViewSingle.playListAddItem NYI")
    }

    protected def playListRemoveItem(m:PlayListRemoveItem) {
        println("PlayViewSingle.playListRemoveItem NYI")
    }

    protected def playListChangeItem(m:PlayListChangeItem) {
        println("PlayViewSingle.playListChangeItem NYI")
    }

    protected def playListSelectItem(m:PlayListSelectItem) {
        imageSelected(m.index)
    }

    protected def playListChangeList(m:PlayListChangeList) {
        playList = m.newList
        currentIndex = -1
        if (playList.size>0)
            imageSelected(0)
    }

    private def imageSelected(index:Int) {
        currentIndex = index
        if (index<0) {
            //val msg = getResourceString("error.NoImageSelected")
                //TODO - need an app to get resources from
            val msg = "No image"
            imageComponent.setText(msg)
            imageComponent.setIcon(null)
        } else {
            val im = getTransformedImage(index)
                //TODO - check for null im?
            val ii = new ImageIcon(im)
            imageComponent.setIcon(ii)
            imageComponent.setText(null)
        }
        imageComponent.revalidate()
    }

    private def getTransformedImage(index:Int):Image = {
        val item = playList.getItem(index)
        val f = new File(item.baseDir,item.fileName)
        val im = imageComponent.getToolkit.createImage(f.getPath)
        val si = createScaledImage(im,item.rotFlag,f.getPath)
        //TODO - rotate if necessary
        loadCompleteImage(si)
        si
    }
    
    private def createScaledImage(sourceImage:Image,rot:Int,path:String):
            Image = {
        ImageUtil.createScaledImage(sourceImage,rot,
            imageComponent.getWidth,imageComponent.getHeight,path)
    }

    private def loadCompleteImage(image:Image) {
        mediaTracker.addImage(image,0)
        try {
            mediaTracker.waitForID(0,20000)
        } catch {
            //TODO - better info
            case ex:InterruptedException =>
                throw new RuntimeException(ex)
        }
        mediaTracker.removeImage(image,0)
    }

    class PlayViewSingleComponentListener extends ComponentListener {
        def componentHidden(ev:ComponentEvent) = ()
        def componentMoved(ev:ComponentEvent) = ()
        def componentResized(ev:ComponentEvent) = imageSelected(currentIndex)
        def componentShown(ev:ComponentEvent) = ()
    }
}
