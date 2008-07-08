/* SomeOrNone.scala
 *
 * Jim McBeath, July 8, 2008
 */

package net.jimmc.util

/** Simplify the use of Option when interfacing with Java code that
 * uses null as an indicator of no value.
 * The source file in which this is used should include
 *   import SomeOrNone.optionNotNull
 */

object SomeOrNone {
    class OptionNotNull[T](x:Option[T]) {
        def getOrNull():T = if (x.isDefined) x.get else null.asInstanceOf[T]
    }

    implicit def optionNotNull[T](x:Option[T]) = new OptionNotNull(x)

    def apply[T](x:T) = if (x==null) None else Some(x)
}
