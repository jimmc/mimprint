/* ArrayExport.java
 *
 * Jim McBeath, July 13, 2005
 */

package net.jimmc.util;

public class ArrayExport extends DataExport {
    private Object[][] data;
    private String[] columnNames;

    public ArrayExport(ResourceSource res,
            Object[][] data, String[] columnNames) {
        super(res);
        this.data = data;
        this.columnNames = columnNames;
    }

    public boolean hasMoreRows(int r) {
        if (data==null)
            return false;
        return (r<data.length);
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int r, int c) {
        if (data==null)
            return null;
        Object[] row = data[r];
        if (row==null)
            return null;
        return row[c];
    }
}
