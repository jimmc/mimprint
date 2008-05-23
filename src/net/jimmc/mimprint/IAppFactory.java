package net.jimmc.mimprint;

import java.io.File;

public interface IAppFactory {
    public PlayItem newPlayItem();

    public PlayList newPlayList();

    public PlayList newPlayList(String[] filenames,
         int dirCount, int fileCount);

    /** Load a playlist from a file. */
    public PlayList loadPlayList(File f);
}
