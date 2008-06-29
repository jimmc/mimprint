/* IconLoader.scala
 *
 * Jim McBeath, October 31, 2005
 * converted from java to scala June 24, 2008
 */

package net.jimmc.mimprint

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.Toolkit
import java.net.URL
import java.io.File
import javax.swing.ImageIcon

/** A background thread to load image icons.
 */
abstract class IconLoader(viewer:SViewer) extends Thread {

    override def run() {
//System.out.println("running iconLoader")
        while (true) {  //keep running until the app exits
            try {
                val fileInfos = getFileInfoList()
                val updatedCount = loadFileInfos(fileInfos)
                if (updatedCount==0)
                    waitForMoreIcons()
            } catch {
                case ex:Exception =>
                    viewer.exceptionDialog(ex)
                    Thread.sleep(10)
            }
        }
    }

    /** Call this from the client to notify us that
     * we should go look for more images to load.
     */
    def moreIcons() {
        synchronized {
//System.out.println("moreIcons")
            this.notify()
        }
    }

    //Wait for the client to tell us we have more work.
    private def waitForMoreIcons() {
      synchronized {
        //Before we wait, run throught the list and make sure there
        //are not any new icons that snuck in there after our last
        //pass through the list.
        val fileInfos = getFileInfoList()
        if (fileInfos!=null && fileInfos.exists(needsIcon(_)))
            return     //found one that needs loading
//System.out.println("waitForMoreIcons")
        try {
            this.wait()
        } catch {
            case ex:InterruptedException =>
                println("Wait interrupted")
        }
//System.out.println("waitForMoreIcons done")
      }
    }

    //Notify the client that it should update the display on
    //an item in its list.
    protected def iconLoaded(fileInfos:Array[FileInfo], n:Int):Boolean

    //Get the set of FileInfo objects to work on
    protected def getFileInfoList():Array[FileInfo]

    //Load all icons that are ready to be loaded
    private def loadFileInfos(fileInfos:Array[FileInfo]):Int = {
//System.out.println("loadFileInfos")
        if (fileInfos==null)
            return 0
        var updatedCount = 0
        for (i <- 0 until fileInfos.length) {
            val fi = fileInfos(i)
            if (needsIcon(fi)) {
                fi.icon = getFileIcon(fi,i)
                if (!iconLoaded(fileInfos,i)) {
                    //The list in client changed, don't
                    //bother loading the rest of these icons
                    return updatedCount
                }
                updatedCount = updatedCount + 1
            }
        }
        updatedCount
    }

    private def needsIcon(fi:FileInfo):Boolean = {
        if (fi==null)
            return false       //that list item not yet visible
        if (!fi.infoLoaded)
            return false       //info not yet loaded, it's not ready for us
        if (fi.icon!=null)
            return false       //icon already loaded
//        if (fi.type!=FileInfo.IMAGE && fi.type!=FileInfo.MIMPRINT)
//            return false       //only load icons for image files and our own files
        return true
    }

    //Load the icon for the specified file.
    private def getFileIcon(fileInfo:FileInfo, index:Int):ImageIcon = {
        if (fileInfo.isDirectory())
            return getDirectoryIcon(fileInfo)
        if (fileInfo.getPath().toLowerCase().endsWith(
                "."+FileInfo.MIMPRINT_EXTENSION))
            return getMimprintIcon(fileInfo)
        //Assume anything else is an image file
        return getImageFileIcon(fileInfo)
    }

    private def getDirectoryIcon(fileInfo:FileInfo):ImageIcon = {
        var resName =
            if (fileInfo.name==".")
                "folder-open.gif"
            else if (fileInfo.name=="..")
                "folder-up.gif"
            else
                "folder.gif"
                //TODO - if folder has a summary.txt file,
                //use icon to indicate folder containing photos
        val u:URL = getClass().getResource(resName)
        if (u==null) {
            println("No URL found for "+resName)
            return null        //TODO what here?
        }
        val toolkit = viewer.getToolkit()
        val image = toolkit.getImage(u)
        new ImageIcon(image)
    }

    private def getMimprintIcon(fileInfo:FileInfo):ImageIcon = {
        val pageLayout = new PageLayout(viewer)
        pageLayout.loadLayoutTemplate(fileInfo.getFile())
        //TODO - check to make sure it got loaded correctly,
        //catch exceptions and put in an error icon
        val desc = pageLayout.getDescription()
        if (desc!=null) {
            fileInfo.setText(desc)  //also updates html
        }
        val image = new BufferedImage(SImageUtil.ICON_SIZE,
                SImageUtil.ICON_SIZE,BufferedImage.TYPE_BYTE_INDEXED)
        val g2:Graphics2D = image.createGraphics()
        //Scale the layout to fit in the imate
        g2.setColor(Color.white)   //we use gray in the real layout background,
                //but white looks better here
        g2.fillRect(0,0,SImageUtil.ICON_SIZE,SImageUtil.ICON_SIZE) //clear to background
        SImageUtil.scaleAndTranslate(g2,
                pageLayout.getPageWidth(),pageLayout.getPageHeight(),
                SImageUtil.ICON_SIZE,SImageUtil.ICON_SIZE)
        g2.setColor(Color.white)
        g2.fillRect(0,0,pageLayout.getPageWidth(),pageLayout.getPageHeight())
        g2.setColor(Color.black)
        pageLayout.getAreaLayout().paint(g2,null,null,true)
            //paint the layout into the image
        return new ImageIcon(image)
    }

    private def getImageFileIcon(fileInfo:FileInfo):ImageIcon = {
        val toolkit = viewer.getToolkit()
        val path = fileInfo.getPath()
        val fullImage = toolkit.createImage(path)
        val scaledImage = SImageUtil.createScaledImage(fullImage,
                0,SImageUtil.ICON_SIZE,SImageUtil.ICON_SIZE,path)
        return new ImageIcon(scaledImage)
    }
}
