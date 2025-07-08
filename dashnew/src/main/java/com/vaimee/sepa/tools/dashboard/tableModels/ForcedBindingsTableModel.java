package com.vaimee.sepa.tools.dashboard.tableModels;

import java.util.ArrayList;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

public class ForcedBindingsTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8524602022439421892L;

	ArrayList<String[]> rowValues = new ArrayList<String[]>();
	ArrayList<String> rowTypes = new ArrayList<String>();
	ArrayList<String> columns = new ArrayList<String>();

	public void clearBindings() {
		rowValues.clear();
		rowTypes.clear();

		super.fireTableDataChanged();
	}

	public void addBindings(String variable, String literal, String value) {
		if (value != null)
			rowValues.add(new String[] { variable, value });
		else
			rowValues.add(new String[] { variable, "" });
		rowTypes.add(literal);

		super.fireTableDataChanged();
	}

	public ForcedBindingsTableModel() {
		columns.add("Variable");
		columns.add("Value");
		columns.add("Datatype");
	}

	@Override
	public int getRowCount() {
		return rowValues.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0 || columnIndex == 1)
			return rowValues.get(rowIndex)[columnIndex];
		return rowTypes.get(rowIndex);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex < getColumnCount())
			return columns.get(columnIndex);
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 1)
			return true;
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		super.setValueAt(aValue, rowIndex, columnIndex);

		if (rowIndex > rowValues.size() - 1)
			return;

		if (columnIndex == 1) {
			String[] currentValue = rowValues.get(rowIndex);
			currentValue[1] = (String) aValue;
			rowValues.set(rowIndex, currentValue);
		}
		if (columnIndex == 2)
			rowTypes.set(rowIndex, (String) aValue);

		super.fireTableCellUpdated(rowIndex, columnIndex);
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		super.addTableModelListener(l);
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		super.removeTableModelListener(l);
	}

}
