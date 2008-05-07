package net.jimmc.mimprint;

import net.jimmc.util.SuiteHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.Test;
import junit.framework.TestCase;

public class TestPlayList extends TestCase {
    public TestPlayList(String name) {
    	super(name);
    }

    public static Test suite() {
	Class testClass = TestPlayList.class;
	Class[] coverageClasses = { PlayList.class };
	return SuiteHelper.testOrCoverage(testClass,coverageClasses);
    }

    public void testSimple() {
        PlayList p = new PlayList();
        assertEquals(0,p.size());
    }

    public void testLoadSave() throws IOException {
        String testData =
            "#this is a comment line\n"+
            "file1.jpg\n"+
            "file2.jpg;+r\n";
        String[] fileNames = { "file1.jpg", "file2.jpg" };

        LineNumberReader in = new LineNumberReader(new StringReader(testData));
        PlayList p = PlayList.load(in);
        assertEquals(2,p.size());

        String[] plFileNames = p.getFileNames();
        SuiteHelper.assertEquals(this,fileNames,plFileNames);

        StringWriter out = new StringWriter();
        p.save(new PrintWriter(out));
        assertEquals(testData,out.toString());
    }
}
