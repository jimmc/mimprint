package net.jimmc.util

import org.scalatest.Suite

import java.io.File

class TestFileUtilS extends Suite {

    def testCountSameElements() {
        val a = Array(1,2,3,4)
        val b = Array(1,2,5,6)
        val n = FileUtilS.countSameElements(a,b)
        assert(2===n)

        val a1 = Array(3,4)
        val b1 = Array(5,6)
        val n1 = FileUtilS.countSameElements(a1,b1)
        assert(0===n1)

        val a2 = Array("a","b","c","d")
        val b2 = Array("a","b")
        val n2 = FileUtilS.countSameElements(a2,b2)
        assert(2===n2)
    }

    def testCollapseRelative() {
        val p1 = "a/b/c"
        val c1 = FileUtilS.collapseRelative(p1)
        assert("a/b/c"===c1)

        val p2 = "a/b/c/../../d"
        val c2 = FileUtilS.collapseRelative(p2)
        assert("a/d"===c2)

        val p3 = "../../d"
        val c3 = FileUtilS.collapseRelative(p3)
        assert("../../d"===c3)

        val p4 = "/a/../b/d"
        val c4 = FileUtilS.collapseRelative(p4)
        assert("/b/d"===c4)

        val p5 = "/a/b/c/.."
        val c5 = FileUtilS.collapseRelative(p5)
        assert("/a/b"===c5)
    }

    def testRelativeTo() {
        val p1 = "a/b/c/d"
        val b1 = new File("a/b")
        val r1 = FileUtilS.relativeTo(p1,b1)
        assert("c/d"===r1)

        val p2 = "a/b/c/d"
        val b2 = new File("a/b/e/f")
        val r2 = FileUtilS.relativeTo(p2,b2)
        assert("../../c/d"===r2)

        val p3 = "../../c/d"
        val b3 = new File("a/b")
        val r3 = FileUtilS.relativeTo(p3,b3)
        assert("../../../../c/d"===r3)
    }
}
