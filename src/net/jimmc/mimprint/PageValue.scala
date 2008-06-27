/* PageValue.scala
 *
 * Jim McBeath, June 21, 2008
 */

package net.jimmc.mimprint

import java.text.NumberFormat

object PageValue {
    private var pageValueFormat = NumberFormat.getNumberInstance()
    pageValueFormat.setMaximumFractionDigits(3)

    val UNIT_MULTIPLIER = 1000

    def formatPageValue(n:int):String = {
        val d:Double = n.asInstanceOf[Double]/UNIT_MULTIPLIER
        pageValueFormat.format(d)
    }

    def parsePageValue(s:String):Int = {
        if (s==null) {
            //SAX eats our exceptions and doesn't print out the trace,
            //so we print it out here before returning
            val ex = new NullPointerException("No value for parsePageValue")
            ex.printStackTrace()
            throw ex
        }
        val d = java.lang.Double.parseDouble(s)
        (d*UNIT_MULTIPLIER).asInstanceOf[Int]
    }

    def parsePageValue(s:String, dflt:Int):Int = {
        if (s==null)
            dflt
        else
            parsePageValue(s)
    }
}
