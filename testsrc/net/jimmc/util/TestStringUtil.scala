package net.jimmc.util

import org.scalatest.Suite

class TestStringUtil extends Suite {

    def testLongestLineWidth() {
        val s0 = null
        val n0 = StringUtil.getLongestLineWidth(s0)
        assert(0===n0)

        val s1 = "abc"
        val n1 = StringUtil.getLongestLineWidth(s1)
        assert(3===n1)

        val s2 = "a\nbc\ndef\ngh\ni\n"
        val n2 = StringUtil.getLongestLineWidth(s2)
        assert(3===n2)

        val s3 = "a\nbc\ndefg"
        val n3 = StringUtil.getLongestLineWidth(s3)
        assert(4===n3)

    }
}
