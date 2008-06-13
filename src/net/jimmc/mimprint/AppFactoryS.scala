package net.jimmc.mimprint

import java.io.File

object AppFactoryS extends IAppFactory {
    def newPlayItem(comments:Array[String], baseDir:File, fileName:String,
            rotFlag:Int) : PlayItem = {
        new PlayItemS(List.fromArray(comments), baseDir, fileName, rotFlag)
    }

    def newPlayList() = PlayListS(null)

    def newPlayList(baseDir:File, filenames:Array[String],
            dirCount:Int, fileCount:Int) =
        PlayListS(null, baseDir, filenames, dirCount, fileCount)

    /** Load a playlist from a file. */
    def loadPlayList(f:File):PlayList = PlayListS.load(null,f)
}
