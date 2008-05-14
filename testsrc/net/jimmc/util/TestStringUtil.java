package net.jimmc.util;

import junit.framework.Test;
import junit.framework.TestCase;

public class TestStringUtil extends TestCase {
    public TestStringUtil(String name) {
    	super(name);
    }

    public static Test suite() {
	Class testClass = TestStringUtil.class;
	Class[] coverageClasses = { StringUtil.class };
	return SuiteHelper.testOrCoverage(testClass,coverageClasses);
    }

    public void testEqual() {
        assertTrue(StringUtil.equals(null,null));
        assertTrue(StringUtil.equals("",""));
        assertTrue(StringUtil.equals("a","a"));
        assertFalse(StringUtil.equals("a",null));
        assertFalse(StringUtil.equals("a",""));
        assertFalse(StringUtil.equals(null,"b"));
        assertFalse(StringUtil.equals("","b"));
        assertFalse(StringUtil.equals(null,""));
        assertFalse(StringUtil.equals("",null));
        assertFalse(StringUtil.equals("a","b"));
        assertFalse(StringUtil.equals("a","ab"));
    }
}
