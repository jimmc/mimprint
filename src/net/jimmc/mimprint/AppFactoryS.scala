package net.jimmc.mimprint

import java.io.File

object AppFactoryS extends IAppFactory {
    def newPlayItem : PlayItem = {
        new PlayItemS()
    }

    def newPlayList() = new PlayListS()

    def newPlayList(filenames:Array[String], dirCount:Int, fileCount:Int) =
        new PlayListS(filenames, dirCount, fileCount)

    /** Load a playlist from a file. */
    def loadPlayList(f:File):PlayList = PlayListS.load(f)
}
