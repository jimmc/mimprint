package net.jimmc.mimprint

object AppS {
    def main(args: Array[String]) {
        val app = new App()
        App.setApp(app);
        app.setFactory(AppFactoryS)
        app.doMain(args)
    }
}
