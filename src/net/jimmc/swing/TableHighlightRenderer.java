/* TableHighlightRenderer.java
 *
 * Jim McBeath, October 29, 2001
 */

package net.jimmc.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.UIManager;

/** A TableCellRenderer that renders its text highlighted to distinguish
 * that cell from others in the table.
 */
public class TableHighlightRenderer extends DefaultTableCellRenderer {
	/** True if this renderer is being used for a header. */
	protected boolean header;

	/** True to highlight this cell. */
	protected boolean highlight;

	/** The column number of the sorting column. */
	protected int sortColumn;

	/** True if the sort column is for an ascending sort,
	 * false if for a descending sort. */
	protected boolean sortAscending;

	/** The sort-ascending icon. */
	protected Icon sortAscendingIcon;

	/** The sort-descending icon. */
	protected Icon sortDescendingIcon;

	/** True after we have attempted to load the sort icons. */
	protected boolean sortIconsLoaded;

	/** Create a renderer. */
	public TableHighlightRenderer() {
		super();
		sortColumn = -1;
	}

	/** Set the header flag.
	 * This causes the renderer to add a border around the component.
	 */
	public void setHeader(boolean header) {
		this.header = header;
	}

	/** Get the header flag. */
	public boolean isHeader() {
		return header;
	}

	/** Set the highlight flag.
	 */
	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}

	/** Get the highlight flag.
	 */
	public boolean isHighlight() {
		return highlight;
	}

	/** Set the sorting column info.
	 * @param column The column index in the model.
	 */
	public void setSortColumn(int column, boolean ascending) {
		sortColumn = column;
		sortAscending = ascending;
	}

	/** Get the Component to use to render the cell.
	 * @param column The column index in the model.
	 */
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (!sortIconsLoaded)
			loadSortIcons();
		Component comp = super.getTableCellRendererComponent(
			table,value,isSelected,hasFocus,row,column);
		if (comp instanceof JLabel) {
			((JLabel)comp).setHorizontalAlignment(JLabel.CENTER);
		}
		if (comp instanceof JLabel) {
		    int modelColumn = table.convertColumnIndexToModel(column);
		    JLabel jl = (JLabel)comp;
		    if (modelColumn==sortColumn) {
			//String sortFlag = sortAscending?"+":"-";
			//String label = sortFlag+value.toString();
			//jl.setText(label);
			jl.setIcon(sortAscending?
					sortAscendingIcon:sortDescendingIcon);
		    } else { 
		    	jl.setIcon(null);
		    }
		}
		if (isHeader()) {
			addHeader(comp,table);	//add header details to comp
		}
		if (isHighlight()) {
			addHighlight(comp);
		}
		return comp;
	}

	/** When we are being used to render a header, add the modifications
	 * for that usage.
	 */
	protected void addHeader(Component comp, JTable table) {
		JTableHeader header =
			((table==null)?null:table.getTableHeader());
		if (header!=null) {
			comp.setForeground(header.getForeground());
			comp.setBackground(header.getBackground());
			comp.setFont(header.getFont());
		}
		if (comp instanceof JComponent) {
		    ((JComponent)comp).setBorder(
			UIManager.getBorder("TableHeader.cellBorder"));
		}
	}

	/** Add our highlighting to the rendering component.
	 */
	protected void addHighlight(Component comp) {
		//comp.setForeground(Color.red);
		Font font = comp.getFont();
		comp.setFont(font.deriveFont(Font.BOLD));
	}

	/** Load the icons we use to mark the sorting column. */
	protected void loadSortIcons() {
		sortAscendingIcon = getIcon("asc.gif");
		sortDescendingIcon = getIcon("desc.gif");
		sortIconsLoaded = true;
	}

	protected ImageIcon getIcon(String name) {
		Class cl = this.getClass();
		URL url = cl.getResource(name);
		if (url==null)
			return null;
		return new ImageIcon(url);
	}
}

/* end */
