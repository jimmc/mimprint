package net.jimmc.mimprint

import net.jimmc.swing.AboutWindow
import net.jimmc.util.SResourcesBundle 

class AppS extends SResourcesBundle {
    def doMain(args: Array[String]) {
        initResources(this)

        val aboutTitle = getResourceString("about.title")
        val aboutInfo = getResourceString("about.info")
        AboutWindow.setAboutTitle(aboutTitle)
        AboutWindow.setAboutInfo(aboutInfo)

        val viewer = new SViewer(this)
        viewer.show             //open the main window

        //TODO - do a better job of parsing the command line args
        for (i <- 0 until args.length) {
            args(i) match {
                case "-new" =>  //ignore
                case fn =>       //assume filename
                    viewer.mainOpen(fn)
            }
        }
    }
}

//When we use "object AppS" and we also have a class AppS, we get an
//error on startup saying we have no "main" method.
object AppStart {
    def main(args: Array[String]) {
        val app = new App()
        val appS = new AppS()
        App.setApp(app);
        app.setFactory(AppFactoryS)

        if (args.length>0 && args(0)=="-new")
            appS.doMain(args)
        else
            app.doMain(args)
    }
}
