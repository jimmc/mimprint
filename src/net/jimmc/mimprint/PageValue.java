package net.jimmc.mimprint;

import java.text.NumberFormat;

public class PageValue {
    private static NumberFormat pageValueFormat;

    public final static int UNIT_MULTIPLIER = 1000;

    private static void initPageValueFormat() {
        if (pageValueFormat==null) {
            pageValueFormat = NumberFormat.getNumberInstance();
            pageValueFormat.setMaximumFractionDigits(3);
        }
    }

    public static String formatPageValue(int n) {
        initPageValueFormat();
        double d = ((double)n)/UNIT_MULTIPLIER;
        return pageValueFormat.format(new Double(d));
    }

    public static int parsePageValue(String s) {
        if (s==null) {
            //SAX eats our exceptions and doesn't print out the trace,
            //so we print it out here before returning
            NullPointerException ex = new NullPointerException(
                    "No value for parsePageValue");
            ex.printStackTrace();
            throw ex;
        }
        double d = Double.parseDouble(s);
        return (int)(d*UNIT_MULTIPLIER);
    }

    public static int parsePageValue(String s, int dflt) {
        if (s==null)
            return dflt;
        else
            return parsePageValue(s);
    }
}
