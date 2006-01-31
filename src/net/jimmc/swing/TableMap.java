/*
This file is a modified version of a file from the Java Tutorial on Sun's web site,
  http://java.sun.com/docs/books/tutorial/uiswing/components/example-swing/TableMap.java

There is no explicit copyright notice, but the tutorial does include this text in
 http://java.sun.com/docs/books/tutorial/uiswing/components/table.html :

 "You can use the TableMap and TableSorter classes when implementing your data manipulator."
*/

/** 
 * In a chain of data manipulators some behaviour is common. TableMap
 * provides most of this behavour and can be subclassed by filters
 * that only need to override a handful of specific methods. TableMap 
 * implements TableModel by routing all requests to its model, and
 * TableModelListener by routing all events to its listeners. Inserting 
 * a TableMap which has not been subclassed into a chain of table filters 
 * should have no effect.
 *
 * @version 1.4 12/17/97
 * @author Philip Milne */

package net.jimmc.swing;

import javax.swing.table.*; 
import javax.swing.event.TableModelListener; 
import javax.swing.event.TableModelEvent; 

public class TableMap extends AbstractTableModel 
                      implements TableModelListener {
    protected TableModel model; 

    public TableModel getModel() {
        return model;
    }

    public void setModel(TableModel model) {
        this.model = model; 
        model.addTableModelListener(this); 
    }

    // By default, implement TableModel by forwarding all messages 
    // to the model. 

    public Object getValueAt(int aRow, int aColumn) {
        return model.getValueAt(aRow, aColumn); 
    }
        
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        model.setValueAt(aValue, aRow, aColumn); 
    }

    public int getRowCount() {
        return (model == null) ? 0 : model.getRowCount(); 
    }

    public int getColumnCount() {
        return (model == null) ? 0 : model.getColumnCount(); 
    }
        
    public String getColumnName(int aColumn) {
        return model.getColumnName(aColumn); 
    }

    public Class getColumnClass(int aColumn) {
        return model.getColumnClass(aColumn); 
    }
        
    public boolean isCellEditable(int row, int column) { 
         return model.isCellEditable(row, column); 
    }
//
// Implementation of the TableModelListener interface, 
//
    // By default forward all events to all the listeners. 
    public void tableChanged(TableModelEvent e) {
        fireTableChanged(e);
    }
}
