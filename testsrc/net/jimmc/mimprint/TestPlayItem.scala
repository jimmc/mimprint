package net.jimmc.mimprint

import org.scalatest.Suite

import java.io.PrintWriter
import java.io.StringWriter

class TestPlayItem extends Suite {

    def testIsFinalLine() {
        assert(!PlayItem.isFinalLine(""))
        assert(!PlayItem.isFinalLine("#comment"))
        assert(!PlayItem.isFinalLine("+command"))
        assert(!PlayItem.isFinalLine("-command"))
        assert(PlayItem.isFinalLine("filename.jpg"))
        assert(PlayItem.isFinalLine("filename.jpg"))
        assert(PlayItem.isFinalLine("-empty"));
    }

    def testIsCommentLine() {
        assert(PlayItem.isCommentLine("#comment"))
        assert(!PlayItem.isCommentLine("not a comment"))
    }

    def testIsEmptyLine() {
        assert(PlayItem.isEmptyLine("-empty"))
        assert(!PlayItem.isEmptyLine("foo"))
    }

    def testImageInfoLine() {
        val item1 = new PlayItem(null,null,"foo",0)
        assert("foo"==item1.getImageInfoLine())
        assert("foo"==item1.getFileName())
        assert(0==item1.getRotFlag())

        val item2 = new PlayItem(null,null,"foo.gif",0)
        assert("foo.gif"==item2.getImageInfoLine())
        assert("foo.gif"==item2.getFileName())
        assert(0==item2.getRotFlag())

        val item3 = new PlayItem(null,null,null,0)
        assert("-empty"==item3.getImageInfoLine())
        assert(null==item3.getFileName())
        assert(0==item3.getRotFlag())
    }

    def testIsEmpty() {
        val item1 = new PlayItem(null,null,null,0)
        assert(item1.isEmpty)

        val item2 = new PlayItem(null,null,"foo.gif",0)
        assert(!item2.isEmpty)
    }

    def testApply() {
        val lines = List("#comment1","foo.jpg")
        val item1 = PlayItem(lines, null)
        assert("foo.jpg"==item1.getFileName)
        assert(0==item1.getRotFlag)
    }

/*
    def testPrintAll() {
        val comments = List("#line 1","#line 2")
        val fn = "foo.jpg"
        val rot = -1
        val item = new PlayItem(comments,null,fn,rot)
        val out = new StringWriter()
        item.printAll(new PrintWriter(out), null)
//There is now an extra comment about the base dir
        val refStr = "#line 1\n#line 2\nfoo.jpg;-r\n"
println("test is "+out.toString())
println("refstr is "+resStr)
        assert(refStr==out.toString())
    }
*/
}
