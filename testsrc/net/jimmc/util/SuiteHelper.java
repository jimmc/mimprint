package net.jimmc.util;

import net.jimmc.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hansel.CoverageDecorator;

/** A utility class for use with unit test cases. */
public class SuiteHelper {

    /** Return either a test suite for the specified test class,
     * or a coverage suite for that test class with the
     * specified coverage classes.
     * If the system property MIM_TEST_COVERAGE is set,
     * we return an instance of CoverageDecorator
     * @param testClass The test class with the test cases.
     * @param coverageClasses The list of target classes on which to do
     *        coverage testing.
     */
    public static Test testOrCoverage(Class testClass, Class[] coverageClasses){
	String s = System.getProperty("MIM_TEST_COVERAGE");
        if (s==null || s.equals("")) {
	    //No test coverage
	    return new TestSuite(testClass);
	}
	//Use Hansel to do code coverage testing on the specified classes
	return new CoverageDecorator(testClass, coverageClasses);
    }

    /** Get a File relative to the test directory for the given class.
    */
    public static File getFile(Class cl, String filename) {
        String pkg = cl.getName();
        int x = pkg.lastIndexOf('.');
        pkg = pkg.substring(0,x);
        String localDir = "testsrc"+File.separator+
                pkg.replace('.',File.separatorChar)+File.separator;
        return new File(localDir,filename);
    }

    /** Get a PrintWriter for the specified file, throw RuntimeExcpetion
     * if any problems.
     */
    public static PrintWriter getPrintWriter(File f) {
        try {
            return new PrintWriter(new FileOutputStream(f));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Given two files, assert their contents are the same.
     */
    public static void assertFilesEqual(TestCase test, File expf, File testf) {
        try {
            String expStr = FileUtil.readFile(expf);
            String testStr = FileUtil.readFile(testf);
            test.assertEquals(expStr,testStr);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Given two arrays of Strings, assert their contents are the same.
     */
    public static void assertEquals(TestCase test,
            String[] expected, String[] actual) {
        if (expected==actual)
            return;
        if (expected==null) {
            test.assertNull(actual);
            return;
        }
        test.assertNotNull(actual);
        test.assertEquals("array lengths",expected.length,actual.length);
        for (int i=0; i<expected.length; i++) {
            test.assertEquals("array["+i+"]",expected[i],actual[i]);
        }
    }
    
    private static void deleteDir(File f) {
	File[] subs = f.listFiles();
	for (int i=0; i<subs.length; i++) {
	    File sub = subs[i];
	    if (sub.isDirectory()) {
	        deleteDir(sub);
	    } else {
		sub.delete();
	    }
	}
        f.delete();
    }

    /** For cleanup, remove a file. */
    public static void deleteFile(String filename) {
        new File(filename).delete();
    }

}

//end
