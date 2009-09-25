/* App.scala
 *
 * Jim McBeath, May 22, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.AboutWindow
import net.jimmc.util.SResourcesBundle 

import org.apache.log4j.xml.DOMConfigurator

class App extends SResourcesBundle {
    val LOG4J_WATCH_INTERVAL = 5000

    def doMain(args: Array[String]) {
	initLog4j()
        initResources(this)

        val aboutTitle = getResourceString("about.title")
        val aboutInfo = getResourceString("about.info")
        AboutWindow.setAboutTitle(aboutTitle)
        AboutWindow.setAboutInfo(aboutInfo)

        val viewer = new SViewer(this)
        viewer.show             //open the main window
        viewer.start

        //TODO - do a better job of parsing the command line args
        for (i <- 0 until args.length) {
            args(i) match {
                case "-help" =>
                    //Print out the help text and exit
                    val help = viewer.getResourceString("info.CommandHelp")
                    println(help)
                    System.exit(0)
                case "-new" =>  //ignore
                case "-toolbar" =>
                    viewer.setToolBarVisible()
                case fn =>       //assume filename
                    viewer.mainOpen(fn)
            }
        }
    }

    def initLog4j() {
	val cfName = "mimprint.log4j.configfile"
	val configFile = System.getProperty(cfName)
	if (configFile==null)
	    throw new IllegalArgumentException(cfName+" must be set")
	DOMConfigurator.configureAndWatch(configFile,LOG4J_WATCH_INTERVAL)
    }
}

//When we use "object App" and we also have a class App, we get an
//error on startup saying we have no "main" method.
object AppStart {
    def main(args: Array[String]) {
        val app = new App()
        app.doMain(args)
    }
}
