/*
This file is a modified version of a file from the Java Tutorial on Sun's web site,
  http://java.sun.com/docs/books/tutorial/uiswing/components/example-swing/TableSorter.java

There is no explicit copyright notice, but the tutorial does include this text in
 http://java.sun.com/docs/books/tutorial/uiswing/components/table.html :

 "You can use the TableMap and TableSorter classes when implementing your data manipulator."
*/

/**
 * A sorter for TableModels. The sorter has a model (conforming to TableModel) 
 * and itself implements TableModel. TableSorter does not store or copy 
 * the data in the TableModel, instead it maintains an array of 
 * integers which it keeps the same size as the number of rows in its 
 * model. When the model changes it notifies the sorter that something 
 * has changed eg. "rowsAdded" so that its internal array of integers 
 * can be reallocated. As requests are made of the sorter (like 
 * getValueAt(row, col) it redirects them to its model via the mapping 
 * array. That way the TableSorter appears to hold another copy of the table 
 * with the rows in a different order. The sorting algorthm used is stable 
 * which means that it does not move around rows when its comparison 
 * function returns 0 to denote that they are equivalent. 
 *
 * @version 1.5 12/17/97
 * @author Philip Milne
 * @author Jim McBeath
 */
//unsortedExtraRows and sortedExtraRows stuff added by Jim McBeath.

package net.jimmc.swing;

import net.jimmc.util.WeakNumericComparator;

import java.util.*;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;

// Imports for picking up mouse events from the JTable. 

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.util.Comparator;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

public class TableSorter extends TableMap {
    int             indexes[];
    Vector          sortingColumns = new Vector();
    boolean         ascending = true;
    int compares;

    Object[][] sortedExtraRows;
    Object[][] unsortedExtraRows;
    Comparator stringComparator;

    public TableSorter() {
        indexes = new int[0]; // for consistency
	sortedExtraRows = new Object[0][];
	unsortedExtraRows = new Object[0][];
	stringComparator = new WeakNumericComparator();
    }

    public TableSorter(TableModel model) {
	stringComparator = new WeakNumericComparator();
        setModel(model);
    }

    public void setModel(TableModel model) {
        super.setModel(model); 
        reallocateIndexes(); 
	if (sortingColumns.size()>0) {
	    //If we have sorting columns set up, sort the new data
	    sort(this);
	    super.tableChanged(new TableModelEvent(this)); 
	}
    }

    /** Add an empty row to the end of the table.
     */
    public void addRow() {
	int n = unsortedExtraRows.length;
    	Object[][] newUnsortedExtraRows = new Object[n+1][];
	System.arraycopy(unsortedExtraRows,0,newUnsortedExtraRows,0,n);
	newUnsortedExtraRows[n] = new Object[getColumnCount()];
	unsortedExtraRows = newUnsortedExtraRows;

	//Notify observers that we have a new row, but don't redo our indexes
	int r = getRowCount() - 1;
	TableModelEvent ev = new TableModelEvent(this,r,r,
			TableModelEvent.ALL_COLUMNS,TableModelEvent.INSERT);
	super.tableChanged(ev);
    }

    public int compareRowsByColumn(int row1, int row2, int column) {
        Class type = model.getColumnClass(column);

        // Check for nulls.

        Object o1 = getModelValueAt(row1, column);
        Object o2 = getModelValueAt(row2, column); 

        // If both values are null, return 0.
        if (o1 == null && o2 == null) {
            return 0; 
        } else if (o1 == null) { // Define null less than everything. 
            return -1; 
        } else if (o2 == null) { 
            return 1; 
        }

        /*
         * We copy all returned values from the getValue call in case
         * an optimised model is reusing one object to return many
         * values.  The Number subclasses in the JDK are immutable and
         * so will not be used in this way but other subclasses of
         * Number might want to do this to save space and avoid
         * unnecessary heap allocation.
         */

	if (Number.class.isAssignableFrom(type)) {
            Number n1 = (Number)o1;
            double d1 = n1.doubleValue();
            Number n2 = (Number)o2;
            double d2 = n2.doubleValue();

            if (d1 < d2) {
                return -1;
            } else if (d1 > d2) {
                return 1;
            } else {
                return 0;
            }
	} else if (Date.class.isAssignableFrom(type)) {
            Date d1 = (Date)o1;
            long n1 = d1.getTime();
            Date d2 = (Date)o2;
            long n2 = d2.getTime();

            if (n1 < n2) {
                return -1;
            } else if (n1 > n2) {
                return 1;
            } else {
                return 0;
            }
        } else if (type == String.class) {
            String s1 = o1.toString();
            String s2 = o2.toString();
            int result = stringComparator.compare(s1,s2);

            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        } else if (type == Boolean.class) {
            Boolean bool1 = (Boolean)o1;
            boolean b1 = bool1.booleanValue();
            Boolean bool2 = (Boolean)o2;
            boolean b2 = bool2.booleanValue();

            if (b1 == b2) {
                return 0;
            } else if (b1) { // Define false < true
                return 1;
            } else {
                return -1;
            }
        } else {
            String s1 = o1.toString();
            String s2 = o2.toString();
            int result = stringComparator.compare(s1,s2);

            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
        	return 0;
            }
        }
    }

    public int compare(int row1, int row2) {
	int tableCols = getColumnCount();
        compares++;
        for (int level = 0; level < sortingColumns.size(); level++) {
            Integer column = (Integer)sortingColumns.elementAt(level);
	    int col = column.intValue();
	    if (col>=tableCols)
	        continue;	//ignore (model may have changed)
            int result = compareRowsByColumn(row1, row2, col);
            if (result != 0) {
                return ascending ? result : -result;
            }
        }
        return 0;
    }

    public void reallocateIndexes() {
        int modelRowCount = model.getRowCount();
	sortedExtraRows = new Object[0][];
	unsortedExtraRows = new Object[0][];

        // Set up a new array of indexes with the right number of elements
        // for the new data model.
        indexes = new int[modelRowCount];

        // Initialise with the identity mapping.
        for (int row = 0; row < modelRowCount; row++) {
            indexes[row] = row;
        }
    }

    public void tableChanged(TableModelEvent e) {
        //System.out.println("Sorter: tableChanged"); 
        reallocateIndexes();
	if (sortingColumns.size()>0) {
	    //If we have sorting columns set up, sort the new data
	    sort(this);
	}

        super.tableChanged(e);
    }

    public void checkModel() {
        if (indexes.length != model.getRowCount()+sortedExtraRows.length) {
            System.err.println("Sorter not informed of a change in model.");
        }
    }

    //Moves all currently unsorted extra rows into the sorted extra rows,
    //expands the indexes to cover those extra rows.
    protected void checkExtraRows() {
    	if (unsortedExtraRows.length==0)
	    return;		//no unsorted rows to move

	//Add the unsortedExtraRows to the sortedExtraRows
	int n = sortedExtraRows.length + unsortedExtraRows.length;
	Object[][] newSortedExtraRows = new Object[n][];
	System.arraycopy(sortedExtraRows,0,newSortedExtraRows,0,
				sortedExtraRows.length);
	System.arraycopy(unsortedExtraRows,0,newSortedExtraRows,
			sortedExtraRows.length,unsortedExtraRows.length);

	//Expand the size of the index to include the new sortedExtraRows
        int x = indexes.length + unsortedExtraRows.length;
	int[] newIndexes = new int[x];
	System.arraycopy(indexes,0,newIndexes,0,indexes.length);
	for (int i=indexes.length; i<newIndexes.length; i++)
	    newIndexes[i] = i;	//new rows are not yet sorted

	indexes = newIndexes;
	sortedExtraRows = newSortedExtraRows;
	unsortedExtraRows = new Object[0][];	//no unsorted rows now
    }

    public void sort(Object sender) {
	checkExtraRows();
        checkModel();

        compares = 0;
        // n2sort();
        // qsort(0, indexes.length-1);
        shuttlesort((int[])indexes.clone(), indexes, 0, indexes.length);
	if (System.getProperty("JIMMC_TABLESORTER_PRINTSTATS")!=null)
		System.out.println("Sort compares: "+compares);
    }

    public void n2sort() {
        for (int i = 0; i < getSortedRowCount(); i++) {
            for (int j = i+1; j < getSortedRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) == -1) {
                    swap(i, j);
                }
            }
        }
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
    public void shuttlesort(int from[], int to[], int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high)/2;
        shuttlesort(to, from, low, middle);
        shuttlesort(to, from, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
        check to see if the elements in this subset are already
        ordered.  If so, no further comparisons are needed; the
        sub-array can just be copied.  The array must be copied rather
        than assigned otherwise sister calls in the recursion might
        get out of sinc.  When the number of elements is three they
        are partitioned so that the first set, [low, mid), has one
        element and and the second, [mid, high), has two. We skip the
        optimisation when the number of elements is three or less as
        the first compare in the normal merge will produce the same
        sequence of steps. This optimisation seems to be worthwhile
        for partially ordered lists but some analysis is needed to
        find out how the performance drops to Nlog(N) as the initial
        order diminishes - it may drop very quickly.  */

        if (high - low >= 4 && compare(from[middle-1], from[middle]) <= 0) {
            for (int i = low; i < high; i++) {
                to[i] = from[i];
            }
            return;
        }

        // A normal merge. 

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            }
            else {
                to[i] = from[q++];
            }
        }
    }

    public void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    /** Given a sorted index, convert it back to an unsorted index. */
    protected int convertRowIndexToModel(int row) {
    	return indexes[row];
    }

    /** Get data from a location in our model.
     * @param aRow Unsorted row index.
     *        Can't acces the unsorted extra rows here.
     * @param aColumn Index of a column in our model.
     */
    protected Object getModelValueAt(int aRow, int aColumn) {
	int modelRowCount = model.getRowCount();
	if (aRow<modelRowCount)
	    return model.getValueAt(aRow, aColumn);
	int sRow = aRow - modelRowCount;
	return sortedExtraRows[sRow][aColumn];
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".

    /** Get a value for our sorted model.
     * @param aRow Sorted row index.
     * @param aColumn Index of the column.
     */
    public Object getValueAt(int aRow, int aColumn) {
        checkModel();
	if (aRow<indexes.length)
	    return getModelValueAt(indexes[aRow], aColumn);
	int uRow = aRow - indexes.length;
	return unsortedExtraRows[uRow][aColumn];
    }

    /** Set data into a location in our model.
     * @param aValue The value to set.
     * @param aRow Unsorted row index.  Must be in either the underlying
     *        model or in the sorted extra rows, not in the unsorted extras.
     * @param aColumn Index of a column in our model.
     */
    protected void setModelValueAt(Object aValue, int aRow, int aColumn) {
	int modelRowCount = model.getRowCount();
	if (aRow<modelRowCount) {
	    model.setValueAt(aValue, aRow, aColumn);
	    return;
	}
	int sRow = aRow - modelRowCount;
	sortedExtraRows[sRow][aColumn] = aValue;
    }

    /** Set a value into our sorted model.
     * @param aValue The value to set.
     * @param aRow Sorted row index.
     * @param aColumn Index of the column.
     */
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        checkModel();
	if (!checkNewValueAt(aValue, aRow, aColumn)) //may throw exception
	    return;	//the check failed, don't set the value
	int modelRowCount = model.getRowCount();
	if (aRow<modelRowCount) {
	    //Set the data into our model here only if it is part of
	    //the unsorted underlying model.  Otherwise, let it get
	    //set in setExtraValueAt.
	    setModelValueAt(aValue, indexes[aRow], aColumn);
	}
	//Always call setExtraValueAt so subclasses which override will
	//get the data.
	setExtraValueAt(aValue, aRow, aColumn);
    }

    /** Return true if OK to save the data, false if not, with
     * the error already reported to the user.
     */
    protected boolean checkNewValueAt(Object aValue, int aRow, int aColumn) {
    	return true;	//let subclass override
    }

    /** Set a value which is not part of the unsorted model.
     * We make this a separate method, called from setValueAt,
     * to allow overriding.
     * @param aValue The value to set.
     * @param aRow Sorted row index.
     * @param aColumn Index of the column.
     */
    protected void setExtraValueAt(Object aValue, int aRow, int aColumn) {
	int modelRowCount = model.getRowCount();
	if (aRow<modelRowCount)
	    return;	//already set in setValueAt
	if (aRow<indexes.length) {
	    //set a value into the sorted extra rows
	    setModelValueAt(aValue, indexes[aRow], aColumn);
	} else {
	    //set a value into the unsorted extra rows
	    int uRow = aRow - indexes.length;
	    unsortedExtraRows[uRow][aColumn] = aValue;
	}
    }

    public int getRowCount() {
    	return super.getRowCount()+
	    sortedExtraRows.length+unsortedExtraRows.length;
    }

    protected int getSortedRowCount() {
    	return super.getRowCount()+sortedExtraRows.length;
    }

    /** Sort by the specified column.
     * If we are already sorting by that column, then toggle the direction
     * of the sort.
     * If we are not already sorting on that column, then sort ascending.
     * @param column The column index in the model.
     * @return True if we are now sorting ascending on that column,
     *         false if we are now sorting descending on it.
     */
    public boolean sortByColumn(int column) {
	boolean newAsc;
        if (sortingColumns.contains(new Integer(column))) {
		//This column is already being sorted, so toggle the sense
		//of the sort.
		newAsc = !this.ascending;
	} else {
		//Not currently selected, so sort ascending.
		newAsc = true;
	}
        sortByColumn(column, newAsc);
	return newAsc;
    }

    public void sortByColumn(int column, boolean ascending) {
        this.ascending = ascending;
	//Some of the code in this class is set up to handle sorting on
	//multiple columns, but the code in this method clears that list
	//so that there is only one sort column.  To make the multiple-column
	//sort really work, the "ascending" flag should be changed from a
	//single class variable to something that can represent a separate
	//ascending/descending value for each column in sortingColumns.
        sortingColumns.removeAllElements();
        sortingColumns.addElement(new Integer(column));
        sort(this);
        super.tableChanged(new TableModelEvent(this)); 
    }

    // There is no-where else to put this. 
    // Add a mouse listener to the Table to trigger a table sort 
    // when a column heading is clicked in the JTable. 
    public void addMouseListenerToHeaderInTable(JTable table) { 
        final TableSorter sorter = this; 
        final JTable tableView = table; 
        tableView.setColumnSelectionAllowed(false); 
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
                int column = tableView.convertColumnIndexToModel(viewColumn); 
                if (e.getClickCount() == 1 && column != -1) {
                    //System.out.println("Sorting ..."); 
                    //int shiftPressed= e.getModifiers()&InputEvent.SHIFT_MASK; 
                    //boolean ascending = (shiftPressed == 0); 
                    //sorter.sortByColumn(column, ascending); 
                    sorter.sortByColumn(column); 
                }
            }
        };
        JTableHeader th = tableView.getTableHeader(); 
        th.addMouseListener(listMouseListener); 
    }
}
