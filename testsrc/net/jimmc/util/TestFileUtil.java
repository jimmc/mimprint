package net.jimmc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;

public class TestFileUtil extends TestCase {
    public TestFileUtil(String name) {
    	super(name);
    }

    public static Test suite() {
	Class testClass = TestFileUtil.class;
	Class[] coverageClasses = { FileUtil.class };
	return SuiteHelper.testOrCoverage(testClass,coverageClasses);
    }

    public void testRead() {
        File f = SuiteHelper.getFile(getClass(),"testFileUtil1.txt");
        try {
            String fromFile = FileUtil.readFile(f);
            String exp = "This is\nfile one.\n";
            assertEquals(exp,fromFile);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    //TODO - readFile add a newline to the end of a file without one,
    //do we want this behavior?

    public void testWrite() {
        File f = SuiteHelper.getFile(getClass(),"testFileUtil2.tmp");
        String orig = "This is\nfile number two.\n";
        try {
            FileUtil.writeFile(f,orig);
            String fromFile = FileUtil.readFile(f);
            assertEquals(orig,fromFile);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
