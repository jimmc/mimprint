package net.jimmc.mimprint;

public class AppFactoryJ implements IAppFactory {
    public PlayItem newPlayItem() {
        return new PlayItemJ();
    }
}
