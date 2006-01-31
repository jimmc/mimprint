/* DataExport.java
 *
 * Jim McBeath, July 13, 2005
 * Copy/mod from TableModelExport.java
 */

package net.jimmc.util;

import net.jimmc.util.ResourceSource;

import java.io.PrintWriter;
import java.util.Date;

/** Export tabular data in different formats.
 */
public abstract class DataExport {
    private ResourceSource res;
    private PrintWriter out;
    private String table;
    private String title;

    /** Create an exporter.
     * @param res Source for our resources.
     */
    public DataExport(ResourceSource res) {
	this.res = res;
    }

    /** Set the table to use for formats that can use it. */
    public void setTable(String table) {
        this.table = table;
    }

    /** Set the title to use for formats that can use it. */
    public void setTitle(String title) {
	this.title = title;
    }

    /** Get the date to use in our output.
     * Unit tests can override this method to return a constant.
     */
    protected Date getCurrentDate() {
        return new Date();
    }

    /** Export in SQL format to the specified file.
     * @see #toSql
     */
    public void exportSql(PrintWriter out) {
	this.out = out;
	if (table==null || table.equals(""))
	    table = "NO_TABLE_NAME";
	for (int r=0; hasMoreRows(r); r++) {
	    //output each
	    int numCols = getColumnCount();
	    print("INSERT into ");
	    print(table);
	    print("(");
	    for (int c=0; c<numCols; c++) {
		if (c>0)
		    print(",");
		print(getColumnName(c));
	    }
	    print(") values(");
	    for (int c=0; c<numCols; c++) {
		if (c>0)
		    print(",");
		Object v = getValueAt(r,c);
		print(toSql(v));
	    }
	    println(")");
	}
    }

    /** Export in HTML table format.
     * @param out The stream to which to export.
     */
    public void exportHtml(PrintWriter out) {
        exportHtml(out,null);
    }

    /** Export in HTML table format.
     * @param out The stream to which to export.
     * @param columnLabels The labels to use for each column, or null to use
     *        the column names.
     */
    public void exportHtml(PrintWriter out, String[] columnLabels) {
	this.out = out;
	if (title!=null) {
	    print(res.getResourceFormatted(
		"DataExport.html.headerWithTitle",title));
	} else if (table!=null && !table.equals("")) {
	    print(res.getResourceFormatted(
		"DataExport.html.headerWithTable",table));
	} else {
	    print(res.getResourceString(
		"DataExport.html.header"));
	}
	String footer = res.getResourceFormatted(
		"DataExport.html.footer",getCurrentDate());

	//Print the column names as table headers
	print(res.getResourceString(
	    "DataExport.html.table.headerRowStart"));
	String colStart = res.getResourceString(
	    "DataExport.html.table.headerColStart");
	String colEnd = res.getResourceString(
	    "DataExport.html.table.headerColEnd");
	int numCols = getColumnCount();
	for (int c=0; c<numCols; c++) {
	    print(colStart);
            if (columnLabels!=null)
                print(columnLabels[c]);
            else
                print(getColumnName(c));
	    print(colEnd);
	}
	print(res.getResourceString(
	    "DataExport.html.table.headerRowEnd"));

	String rowStart = res.getResourceString(
	    "DataExport.html.table.dataRowStart");
	String rowEnd = res.getResourceString(
	    "DataExport.html.table.dataRowEnd");
	colStart = res.getResourceString(
	    "DataExport.html.table.dataColStart");
	colEnd = res.getResourceString(
	    "DataExport.html.table.dataColEnd");

	for (int r=0; hasMoreRows(r); r++) {
	    //output the current row
	    println(rowStart);
	    numCols = getColumnCount();
	    for (int c=0; c<numCols; c++) {
		Object v = getValueAt(r,c);
		print(colStart);
		String s = (v==null)?"":v.toString();
		if (s.equals(""))
		    s = "&nbsp;";
		print(s);
		print(colEnd);
	    }
	    println(rowEnd);
	}

	print(footer);
    }

    /** Export in tab-delimited table format to the specified file. */
    public void exportTabDelimited(PrintWriter out) {
        exportDelimited(out,"\t",false);
    }

    public void exportCsv(PrintWriter out) {
        exportDelimited(out,",",true);
    }

    private void exportDelimited(PrintWriter out, String delimiter,
                boolean quoteFields) {
	this.out = out;

        //TODO - print the table nme?

        int numCols = getColumnCount();
	for (int r=0; hasMoreRows(r); r++) {
            if (r==0) {
                //Print out the column names
                for (int c=0; c<numCols; c++) {
                    if (c>0)
                        print(delimiter);
                    print(delimValue(getColumnName(c),quoteFields));
                }
                println("");
            }
	    //output the current row
	    for (int c=0; c<numCols; c++) {
		if (c>0)
		    print(delimiter);
		Object v = getValueAt(r,c);
                print(delimValue(v,quoteFields));
	    }
	    println("");
	}
    }

    private String delimValue(Object v, boolean quoteFields) {
        if (v==null)
            return "";
        String s = v.toString();
        if (quoteFields && (v instanceof String))
            return '"'+s+'"';
        return s;       //unquoted
    }

    /** Print a line to our output file. */
    public void println(String msg) {
	out.println(msg);
    }

    /** Print to our output file. */
    public void print(String msg) {
	out.print(msg);
    }

    /** Return the number of columns in the data. */
    public abstract int getColumnCount();

    /** Return the name of a column.
     * @param col 0-based column index.
     */
    public abstract String getColumnName(int col);

    /** True if there is more data to output.
     * The parameter r will be 0 on the first call and will increment
     * by one on each call.
     * For sequential access data sources,
     * this method can be used to advance the data to the next row.
     */
    public abstract boolean hasMoreRows(int r);

    /** Get a specific data value.
     * @param row 0-based row index.
     * @param col 0-based column index.
     */
    public abstract Object getValueAt(int row, int col);

    /** Convert a value to sql syntax.
     * Subclass MUST override if it will be using exportSql.
     */
    public String toSql(Object v) {
        throw new RuntimeException("toSql not implemented");
    }
}

/* end */
