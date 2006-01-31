/* JsTable.java
 *
 * Jim McBeath, October 31, 2001
 */

package net.jimmc.swing;

import net.jimmc.util.Duration;
import net.jimmc.util.ResourceSource;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/** An extended version of JTable which can sort on any column.
 * Table column headers use {@link TableHighlightRenderer}.
 */
public class JsTable extends JTable {
	/** Our sorter. */
	TableSorter sorter;

	/** Our frame, if set by a call to {@link #setFrame}. */
	JsFrame frame;

	/** The current sort column. */
	int sortColumn;

	/** The current sorting direction. */
	boolean sortAscending;

	/** Create a Table. */
	public JsTable(TableModel tableModel) {
		super();
		sortColumn = -1;	//no sort column
		//Allow user to sort on any column by clicking on the header
		sorter = new TableSorter(tableModel) {
			/** Sort by the specified column
			 * @param column The column index in the model.
			 */
			public boolean sortByColumn(int column) {
				boolean asc = super.sortByColumn(column);
				markColumnHeaders(column,asc);
				return asc;
			}
			/** Sort by the specified column
			 * @param column The column index in the model.
			 * @param asc True to sort ascending, false to
			 *        sort descending.
			 */
			public void sortByColumn(int column, boolean asc) {
				super.sortByColumn(column,asc);
				markColumnHeaders(column,asc);
			}
			protected void setExtraValueAt(Object a, int r, int c) {
				TableModel m = this;
				JsTable.this.setExtraValueAt(m,a,r,c);
				super.setExtraValueAt(a,r,c);
			}
			protected boolean checkNewValueAt(Object a,
					int r, int c) {
				TableModel m = this;
				if (!JsTable.this.checkNewValueAt(m,a,r,c))
					return false;
				return super.checkNewValueAt(a,r,c);
			}
		};
		super.setModel(sorter);
		sorter.addMouseListenerToHeaderInTable(this);

		//Allow user to move columns around by dragging the header
		getTableHeader().setReorderingAllowed(true);
	}

	/** Set our frame. */
	public void setFrame(JsFrame frame) {
		this.frame = frame;
	}

	/** When the user asks to set the model, we actually set the
	 * model within the sorter.
	 */
	public void setModel(TableModel model) {
		if (sorter==null)
			super.setModel(model);
		else
			sorter.setModel(model);
	}

	/** Make double-click and single-click+Enter call the editSelected
	 * method.
	 */
	public void enableEditSelected() {
		//Make double-click edit the row
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent ev) {
				if (ev.getClickCount()==2) {
					editSelectedCatch();
				}
			}
		});

		//Set up the Enter key to do the same as double-click
		ActionMap actionMap = new ActionMap();
		actionMap.setParent(getActionMap());
		setActionMap(actionMap);
		actionMap.put("editSelected",new AbstractAction(){
			public void actionPerformed(ActionEvent ev) {
				editSelectedCatch();
			}
		});
		InputMap inputMap = new InputMap();
		inputMap.setParent(getInputMap());
		setInputMap(WHEN_FOCUSED,inputMap);
		KeyStroke enterKeyStroke = KeyStroke.getKeyStroke("ENTER");
		inputMap.put(enterKeyStroke,"editSelected");
	}

	/** Set our header renderers.
	 * This method is typically called after updating the table model.
	 */
	public void setHeaderRenderers() {
		//Header renderer for non-editable columns
		TableHighlightRenderer headerRenderer =
			new TableHighlightRenderer();
		headerRenderer.setHeader(true);

		//Highlighted renderer for editable columns
		TableHighlightRenderer highlightRenderer =
			new TableHighlightRenderer();
		highlightRenderer.setHeader(true);
		highlightRenderer.setHighlight(true);

		TableColumnModel columns = getColumnModel();
		int count = columns.getColumnCount();
		for (int c=0; c<count; c++) {
			TableColumn col = columns.getColumn(c);
			int x = col.getModelIndex();
			if (isColumnEditable(x))
				col.setHeaderRenderer(highlightRenderer);
			else
				col.setHeaderRenderer(headerRenderer);
		}

		//If we are sorting on a column, mark it
		if (sortColumn>=0) {
			markColumnHeaders(sortColumn,sortAscending);
		}
	}

	/** Mark the column header with the sort flag.
	 * @param column The column index in the model.
	 * @param ascending True for ascending, false for descending sort.
	 */
	protected void markColumnHeaders(int column, boolean ascending) {
		sortColumn = column;
		sortAscending = ascending;
		TableColumnModel columns = getColumnModel();
		int count = columns.getColumnCount();
		for (int c=0; c<count; c++) {
			TableColumn col = columns.getColumn(c);
			TableCellRenderer renderer = col.getHeaderRenderer();
			if (renderer instanceof TableHighlightRenderer) {
				((TableHighlightRenderer)renderer).
					setSortColumn(column,ascending);
			}
		}
	}

	/** Sort on the specified column.
	 * @param column The column index in the model.
	 */
	public void sortByColumn(int column) {
		sorter.sortByColumn(column);
	}

	/** Sort on the specified column.
	 * @param column The column index in the model.
	 * @param asc True to sort ascending, false to sort descending.
	 */
	public void sortByColumn(int column, boolean asc) {
		sorter.sortByColumn(column, asc);
	}

	/** Given a row index to viewable data, such as is returned by
	 * a call to getSelectedIndex, return the row index in the
	 * underlying model (the target model of the sorter).
	 */
	public int convertRowIndexToModel(int row) {
		return sorter.convertRowIndexToModel(row);
	}

	/** Add an empty row to the end of the table.
	 * It will get sorted in with the rest when the user sorts by a column.
	 */
	public void addRow() {
		sorter.addRow();
	}

	/** We override setValueAt to allow returning an
	 * Exception from getCellEditorValue to
	 * mean the users input is not value, don't change data.
	 */
	public void setValueAt(Object value, int row, int col){
		if (value instanceof Exception)
			return;	//ignore it
		else
			super.setValueAt(value,row,col);
	}

	/** Extra functionality after setting a value into the model.
	 * @param tableModel The sorted table model.
	 * @param value The value to set.
	 * @param row The sorted row index.
	 * @param col The sorted col index.
	 */
	protected void setExtraValueAt(TableModel tableModel,
			Object value, int row, int col) {
		//TBD do nothing, let subclass override
	}

	/** Check the value we are setting.
	 * Throw an exception if bad.
	 * @param tableModel The sorted table model.
	 * @param value The value to set.
	 * @param row The sorted row index.
	 * @param col The sorted col index.
	 */
	protected boolean checkNewValueAt(TableModel tableModel,
			Object value, int row, int col) {
		return true;
		//TBD do nothing, let subclass override
	}

	/** Edit the current selection, catch and display any errors.
	 */
	public void editSelectedCatch() {
		if (frame==null) {
			//If no one to display errors, don't catch them
			editSelected();
			return;
		}
		try {
			editSelected();
		} catch (RuntimeException ex) {
			frame.exceptionDialog(ex);
		}
	}

	/** This method is executed after {@link #enableEditSelected} has
	 * been called when the user either double-clicks on a row or
	 * preses Enter when a row is selected.
	 */
	public void editSelected() {
		//Do nothing; let subclass override if it wants to edit
	}

	/** Note which columns are editable.
	 * By default, all columns are not editable.
	 */
	public boolean isColumnEditable(int column) {
		return false;
	}

	/* Add knowlege of Integer fields, handled by
	 * {@link JsIntegerField}.
	 */
	public void setupIntegerEditor() {
		final JsIntegerField intField = new JsIntegerField(30);
		DefaultCellEditor intEditor = new DefaultCellEditor(intField) {
			public Object getCellEditorValue() {
				return intField.getValue();
			}
		};
		setDefaultEditor(Integer.class,intEditor);
	}

	/** Add knowlege of Duration fields, handled by
	 * {@link JsDurationField}.
	 */
	public void setupDurationEditor() {
		final JsDurationField durField = new JsDurationField(30);
		durField.setHorizontalAlignment(JsDurationField.RIGHT);
		DefaultCellEditor durEditor = new DefaultCellEditor(durField) {
			public Object getCellEditorValue() {
				Object nn;
				try {
					nn = durField.getValue();
				} catch (NumberFormatException ex) {
					if (frame!=null)
						showDurationError(ex,durField);
					return ex;
				}
				if (nn==null)
					return null;	//clear the field
				Duration dur = new Duration((Number)nn);
				return dur;
			}
		};
		setDefaultEditor(Duration.class,durEditor);
		DefaultTableCellRenderer durRenderer =
				new DefaultTableCellRenderer();
		durRenderer.setHorizontalAlignment(JLabel.RIGHT);
		setDefaultRenderer(Duration.class,durRenderer);
	}

	/** Deliver the error from a format error in a duration field. */
	private void showDurationError(Exception ex,
				JsDurationField durField) {
		String msg = ex.getMessage();
		if (msg==null||msg.trim().length()==0) {
			//Use class name if the text of the message is empty
			msg = ex.getClass().getName();
		}
		ResourceSource res = frame.getResourceSource();
		String emsg;
		if (res==null) {
			//No resource for this, just display the
			//text from the exception
			emsg = msg;
		} else {
			String txt = durField.getText();
			Object[] args = { txt, msg };
			emsg = res.getResourceFormatted(
					"error.BadDurationFormat", args);
		}
		frame.errorDialog(emsg);
	}
}

/* end */
