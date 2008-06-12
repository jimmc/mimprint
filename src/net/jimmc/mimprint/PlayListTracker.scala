package net.jimmc.mimprint

import net.jimmc.util.ActorPublisher

import java.io.File;
import java.io.PrintWriter;

import scala.actors.Actor
import scala.actors.Actor.loop

/** A playlist of images. */
class PlayListTracker() extends Actor
        with ActorPublisher[PlayListMessage] {
    //Our current playlist
    private var playList:PlayListS = PlayListS()

    /* We always start our actor right away so that clients can send
     * us subscribe requests as soon as we are created.
     */
    this.start()

    def act() {
        loop {
            react (handleSubscribe orElse handleOther)
        }
    }
    private val handleOther : PartialFunction[Any,Unit] = {
        case init:PlayListRequestInit =>
            init.view ! PlayListInit(playList)
        case add:PlayListRequestAdd =>
            if (listMatches(add.list))
                addItem(add.item)
        case rot:PlayListRequestRotate =>
            if (listMatches(rot.list))
                rotateItem(rot.index, rot.rot)
        case sel:PlayListRequestSelect =>
            if (listMatches(sel.list))
                selectItem(sel.index)
        case _ => println("Unrecognized message to PlayList")
    }

    private def listMatches(list:PlayList):Boolean = {
        if (list!=playList) {
            println("Unknown or stale PlayList in tracker request")
                //Could happen, but should be rare, so we basically ignore it
        }
        (list==playList)          //OK to proceed if we have the right list
    }

    /** Add an item to our current PlayList to produce a new current PlayList,
     * publish notices about the change.
     */
    private def addItem(item:PlayItem) {
        val newPlayList = playList.addItem(item).asInstanceOf[PlayListS]
        val newIndex = playList.size - 1
        publish(PlayListAddItem(playList,newPlayList,newIndex))
        playList = newPlayList
    }

    private def rotateItem(itemIndex:Int, rot:Int) {
        val newPlayList = playList.rotateItem(itemIndex, rot).
                asInstanceOf[PlayListS]
        publish(PlayListChangeItem(playList,newPlayList,itemIndex))
        playList = newPlayList
    }

    private def selectItem(itemIndex:Int) {
        //no change to the playlist, we just publish a message
        publish(PlayListSelectItem(playList,itemIndex))
    }

    ///Save our playlist to a file.
    def save(filename:String) = playList.save(filename)

    def save(f:File) = playList.save(f)

    def save(out:PrintWriter, baseDir:File) = playList.save(out, baseDir)

    def load(fileName:String):Unit = load(fileName,false)

    def load(fileName:String, selectLast:Boolean) {
        val newPlayList = PlayListS.load(fileName).asInstanceOf[PlayListS]
        publish(PlayListChangeList(playList,newPlayList))
        if (newPlayList.size>0)
            selectItem(if (selectLast) newPlayList.size - 1 else 0)
        playList = newPlayList
    }
}
