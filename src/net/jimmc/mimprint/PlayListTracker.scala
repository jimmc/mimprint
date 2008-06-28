/* PlayListTracker.scala
 *
 * Jim McBeath, June 10, 2008
 */

package net.jimmc.mimprint

import net.jimmc.util.ActorPublisher
import net.jimmc.util.AsyncUi
import net.jimmc.util.FileUtilS
import net.jimmc.util.PFCatch

import java.io.File;
import java.io.PrintWriter;

import scala.actors.Actor
import scala.actors.Actor.loop

/** A playlist of images. */
class PlayListTracker(val ui:AsyncUi) extends Actor
        with ActorPublisher[PlayListMessage] {
    //Our current playlist
    private var playList:PlayListS = PlayListS(ui)
    private var currentIndex:Int = -1

    /* We always start our actor right away so that clients can send
     * us subscribe requests as soon as we are created.
     */
    this.start()

    def act() {
        loop {
            react (PFCatch(handleSubscribe orElse handleOther,
                    "PlayListTracker",ui))
        }
    }
    private val handleOther : PartialFunction[Any,Unit] = {
        case m:PlayListRequestInit =>
            m.sub ! PlayListInit(this,playList)
        case m:PlayListRequestAdd =>
            if (listMatches(m.list))
                addItem(m.item)
        case m:PlayListRequestInsert =>
            if (listMatches(m.list))
                insertItem(m.index,m.item)
        case m:PlayListRequestRemove =>
            if (listMatches(m.list))
                removeItem(m.index)
        case m:PlayListRequestChange =>
            if (listMatches(m.list))
                changeItem(m.index,m.item)
        case m:PlayListRequestSetItem =>
            if (listMatches(m.list))
                setItem(m.index,m.item)
        case m:PlayListRequestRotate =>
            if (listMatches(m.list))
                rotateItem(m.index, m.rot)
        case m:PlayListRequestSelect =>
            if (listMatches(m.list))
                selectItem(m.index)
        case m:PlayListRequestUp =>
            if (listMatches(m.list))
                selectUp()
        case m:PlayListRequestDown =>
            if (listMatches(m.list))
                selectDown()
        case m:PlayListRequestLeft =>
            if (listMatches(m.list))
                selectLeft()
        case m:PlayListRequestRight =>
            if (listMatches(m.list))
                selectRight()
        case _ => println("Unrecognized message to PlayList")
    }

    private def listMatches(list:PlayListS):Boolean = {
        if (list!=playList) {
            println("Unknown or stale PlayList in tracker request")
                //Could happen, but should be rare, so we basically ignore it
        }
        (list==playList)          //OK to proceed if we have the right list
    }

    /** Add an item to our current PlayList to produce a new current PlayList,
     * publish notices about the change.
     */
    private def addItem(item:PlayItemS) {
        val newPlayList = playList.addItem(item)
        val newIndex = playList.size - 1
        publish(PlayListAddItem(this,playList,newPlayList,newIndex))
        playList = newPlayList
    }

    private def insertItem(itemIndex:Int, item:PlayItemS) {
        val newPlayList = playList.insertItem(itemIndex, item)
        publish(PlayListAddItem(this,playList,newPlayList,itemIndex))
        playList = newPlayList
    }

    private def removeItem(index:Int) {
        val newPlayList = playList.removeItem(index)
        publish(PlayListRemoveItem(this,playList,newPlayList,index))
        playList = newPlayList
    }

    private def changeItem(itemIndex:Int, item:PlayItemS) {
        val newPlayList = playList.replaceItem(itemIndex,item).
                asInstanceOf[PlayListS]
        publish(PlayListChangeItem(this,playList,newPlayList,itemIndex))
        playList = newPlayList
    }

    private def setItem(itemIndex:Int, item:PlayItemS) {
        val biggerPlayList = playList.ensureSize(itemIndex+1)
        val newPlayList = biggerPlayList.replaceItem(itemIndex,item).
                asInstanceOf[PlayListS]
        publish(PlayListChangeItem(this,playList,newPlayList,itemIndex))
        playList = newPlayList
    }

    private def rotateItem(itemIndex:Int, rot:Int) {
        val newPlayList = playList.rotateItem(itemIndex, rot).
                asInstanceOf[PlayListS]
        publish(PlayListChangeItem(this,playList,newPlayList,itemIndex))
        playList = newPlayList
    }

    private def selectItem(itemIndex:Int) {
        //no change to the playlist, we just publish a message
        publish(PlayListSelectItem(this,playList,itemIndex))
        currentIndex = itemIndex
    }

    private def selectUp() {
        if (currentIndex>0)
            selectItem(currentIndex - 1)
        else {
            val prompt = "At beginning of "+playList.baseDir+";\n"
            val newDir:File = FileUtilS.getPreviousDirectory(playList.baseDir)
            if (newDir==null) {
                val eMsg = prompt + "No previous directory"
                ui.invokeUi(ui.errorDialog(eMsg))
            } else {
                val msg = prompt + "move to previous directory "+newDir+"?"
                val leftMsg = PlayListRequestLeft(playList)
                ui.invokeUi {
                    if (ui.confirmDialog(msg))
                        this ! leftMsg
                }
            }
        }
    }

    private def selectDown() {
        if (currentIndex< playList.size - 1)
            selectItem(currentIndex + 1)
        else {
            val prompt = "At end of "+playList.baseDir+";\n"
            val newDir:File = FileUtilS.getNextDirectory(playList.baseDir)
            if (newDir==null) {
                val eMsg = prompt + "No next directory"
                ui.invokeUi(ui.errorDialog(eMsg))
            } else {
                val msg = prompt + "move to next directory "+newDir+"?"
                val leftMsg = PlayListRequestRight(playList)
                ui.invokeUi {
                    if (ui.confirmDialog(msg))
                        this ! leftMsg
                }
            }
        }
    }

    private def selectLeft() {
        val newDir:File = FileUtilS.getPreviousDirectory(playList.baseDir)
        if (newDir==null) {
            val eMsg = "No previous directory"
            ui.invokeUi(ui.errorDialog(eMsg))
        } else {
            load(newDir.getPath,true)
        }
    }

    private def selectRight() {
        val newDir:File = FileUtilS.getNextDirectory(playList.baseDir)
        if (newDir==null) {
            val eMsg = "No next directory"
            ui.invokeUi(ui.errorDialog(eMsg))
        } else {
            load(newDir.getPath,false)
        }
    }

    ///Save our playlist to a file.
    def save(filename:String) = playList.save(filename)

    def save(f:File) = playList.save(f)

    def save(out:PrintWriter, baseDir:File) = playList.save(out, baseDir)

    def load(fileName:String):Unit = load(fileName,false)

    def load(fileName:String, selectLast:Boolean) {
        val newPlayList = PlayListS.load(ui,fileName).asInstanceOf[PlayListS]
        publish(PlayListChangeList(this,playList,newPlayList))
        val idx = if (selectLast) newPlayList.size - 1 else 0
        //Auto select the first/last item in the list if it is an image file
        if (newPlayList.size>0 &&
                FileInfo.isImageFileName(newPlayList.getItem(idx).fileName))
            selectItem(idx)
        playList = newPlayList
    }
}
