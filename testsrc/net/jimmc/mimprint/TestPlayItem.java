package net.jimmc.mimprint;

import net.jimmc.util.SuiteHelper;

import junit.framework.Test;
import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestPlayItem extends TestCase {
    public TestPlayItem(String name) {
    	super(name);
    }

    public static Test suite() {
	Class testClass = TestPlayItem.class;
	Class[] coverageClasses = { PlayItem.class };
	return SuiteHelper.testOrCoverage(testClass,coverageClasses);
    }

    public void testIsImageInfoLine() {
        PlayItem item = new PlayItem();
        assertFalse(item.isImageInfoLine(""));
        assertFalse(item.isImageInfoLine("#comment"));
        assertFalse(item.isImageInfoLine("+command"));
        assertFalse(item.isImageInfoLine("-command"));
        assertTrue(item.isImageInfoLine("filename.jpg"));
        assertTrue(item.isImageInfoLine("filename.gif;+r"));
    }

    public void testAddLine() {
        PlayItem item = new PlayItem();
        assertEquals(0,item.lineCount());
        item.addTextLine("line 1");
        item.addTextLine("line 2");
        assertEquals(2,item.lineCount());
    }

    public void testImageInfoLine() {
        PlayItem item = new PlayItem();
        item.setImageInfoLine("foo");
        assertEquals("foo",item.getImageInfoLine());
        assertEquals("foo",item.getFileName());
        assertEquals(0,item.getRotFlag());

        item = new PlayItem();
        item.setImageInfoLine("foo.gif;;");
        assertEquals("foo.gif",item.getImageInfoLine());
        assertEquals("foo.gif",item.getFileName());
        assertEquals(0,item.getRotFlag());

        item = new PlayItem();
        item.setImageInfoLine("bar;-r");
        assertEquals("bar;-r",item.getImageInfoLine());
        assertEquals("bar",item.getFileName());
        assertEquals(-1,item.getRotFlag());

        item = new PlayItem();
        item.setImageInfoLine("foo.jpg;+r");
        assertEquals("foo.jpg;+r",item.getImageInfoLine());
        assertEquals("foo.jpg",item.getFileName());
        assertEquals(1,item.getRotFlag());
    }

    public void testPrintAll() {
        PlayItem item = new PlayItem();
        item.addTextLine("#line 1");
        item.addTextLine("#line 2");
        item.setImageInfoLine("foo.jpg;-r");
        StringWriter out = new StringWriter();
        item.printAll(new PrintWriter(out));
        String refStr = "#line 1\n#line 2\nfoo.jpg;-r\n";
        assertEquals(refStr,out.toString());
    }

    public void testCopy() {
        PlayItem item = new PlayItem();
        item.addTextLine("#line 1");
        item.addTextLine("#line 2");
        item.setImageInfoLine("foo.jpg;-r");

        PlayItem c = (PlayItem)item.clone();    //calls copy in PlayItem
        assertEquals(2,c.lineCount());
        assertEquals("foo.jpg",c.getFileName());
        assertEquals(-1,c.getRotFlag());
    }

}
