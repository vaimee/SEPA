package com.vaimee.sepa.tools.dashboard.tableModels;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class GraphTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	class Row {
		public String uri;
		public Integer  counter;
		
		public Row(String uri,Integer counter) {
			this.uri = uri;
			this.counter= counter;
		}
	}
	
	private static final long serialVersionUID = -72022807754650051L;
	private ArrayList<Row> graphs = new ArrayList<>();
	//private ArrayList<Integer> counterArrayList = new ArrayList<>();

	private String[] columnStrings = { "Named graph URI", "Triples" };

	public String getColumnName(int col) {
		return columnStrings[col];
	}

	@Override
	public int getRowCount() {
		return graphs.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) return graphs.get(rowIndex).uri;
		return graphs.get(rowIndex).counter;
	}

	public void addRow(String uri, Integer count) {
		graphs.add(new Row(uri,count));
		fireTableDataChanged();
	}
	
	public void removeRow(String uri) {
		for (int i=0; i < graphs.size() ; i++) if(graphs.get(i).uri.equals(uri)) {
			graphs.remove(i);
			fireTableDataChanged();
			break;
		} 
	}

	public void clear() {
		graphs.clear();
		fireTableDataChanged();
	}
}
