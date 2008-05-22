package net.jimmc.mimprint

object AppFactoryS extends IAppFactory {
    def newPlayItem : PlayItem = {
        new PlayItemS()
    }
}
