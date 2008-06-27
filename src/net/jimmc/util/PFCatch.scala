/* PFCatch.scala
 *
 * Jim McBeath, June 25, 2008
 */

package net.jimmc.util

/** A PartialFunction that catches exceptions.
 * This is intended to use in the react or receive statements
 * of Actor loops, to prevent the actor from terminating when there
 * is an exception in the processing of the react statement.
 */
class PFCatch[T](f:PartialFunction[T,Unit], name:String, ui:BasicQueries)
        extends PartialFunction[T,Unit] {
    val reportPrefix = "PFCatch caught exception"+
        (if (name.length>0) (" for "+name) else "") + ":"

    /** Execute our partial function, catch exceptions. */
    def apply(x:T) = {
        try {
            f(x)
        } catch {
            case ex:Exception => handleException(ex)
        }
    }

    def isDefinedAt(x:T) = f.isDefinedAt(x)

    def handleException(ex:Exception) {
        reportException(ex)
        pauseAfterException(ex)
    }

    /** When we catch an exception, this is how we report it to the user. */
    def reportException(ex:Exception) {
        if (ui!=null) {
            ui.exceptionDialog(new RuntimeException(reportPrefix,ex))
        } else {
            println(reportPrefix)
        }
        ex.printStackTrace()
    }

    /** After reporting an exception, we pause a bit, just in case
     * we are in an infinite loop, to prevent sucking up all of the
     * cpu time and thus not being able to get in to stop it.
     */
    def pauseAfterException(ex:Exception) {
        Thread.sleep(10)        //a short sleep
    }
}

object PFCatch {
    /** This convenience method allows us to simply wrap the argument
     * to "react" with PFCatch(...) to get the desired behavior of
     * catching exceptions in the partial function.
     */
    def apply[T](f:PartialFunction[T,Unit]) =  new PFCatch(f, "", null)

    def apply[T](f:PartialFunction[T,Unit],name:String) =
        new PFCatch(f,name, null)

    def apply[T](f:PartialFunction[T,Unit],name:String,ui:BasicQueries) =
        new PFCatch(f,name, ui)
}
