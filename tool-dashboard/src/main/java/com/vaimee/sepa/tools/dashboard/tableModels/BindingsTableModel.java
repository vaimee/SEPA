package com.vaimee.sepa.tools.dashboard.tableModels;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.tools.dashboard.bindings.BindingValue;

public class BindingsTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 2698789913874225961L;

	ArrayList<HashMap<String, BindingValue>> rows = new ArrayList<HashMap<String, BindingValue>>();
	ArrayList<String> columns = new ArrayList<String>();

	public void clear() {
		columns.clear();
		rows.clear();

		super.fireTableStructureChanged();
		super.fireTableDataChanged();
	}

	public void setResults(HashMap<String, JTable> subscriptionResultsTables, ARBindingsResults res, String spuid)
			throws SEPABindingsException {
		if (res == null)
			return;

		ArrayList<String> vars = res.getAddedBindings().getVariables();
		for (String var : res.getRemovedBindings().getVariables()) {
			if (!vars.contains(var))
				vars.add(var);
		}

		if (!columns.containsAll(vars) || columns.size() != vars.size()) {
			columns.clear();
			columns.addAll(vars);
			super.fireTableStructureChanged();
		}

		if (res.getRemovedBindings() != null) {
			for (Bindings sol : res.getRemovedBindings().getBindings()) {
				HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
				for (String var : sol.getVariables()) {
					row.put(var, new BindingValue(sol.getValue(var), sol.isLiteral(var), sol.getDatatype(var), false));
				}
				rows.add(row);
			}
		}

		if (res.getAddedBindings() != null) {
			for (Bindings sol : res.getAddedBindings().getBindings()) {
				HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
				for (String var : sol.getVariables()) {
					row.put(var, new BindingValue(sol.getValue(var), sol.isLiteral(var), sol.getDatatype(var), true));
				}
				rows.add(row);
			}
		}

		subscriptionResultsTables.get(spuid).changeSelection(subscriptionResultsTables.get(spuid).getRowCount() - 1, 0,
				false, false);

		super.fireTableDataChanged();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return BindingValue.class;
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex < getColumnCount())
			return columns.get(columnIndex);
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		BindingValue ret = null;
		if (rowIndex < getRowCount() && columnIndex < getColumnCount()) {
			ret = rows.get(rowIndex).get(columns.get(columnIndex));
		}
		return ret;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		super.setValueAt(aValue, rowIndex, columnIndex);

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

	public void setAddedResults(HashMap<String, JTable> subscriptionResultsTables, BindingsResults bindingsResults,
			String spuid) throws SEPABindingsException {
		if (bindingsResults == null)
			return;

		ArrayList<String> vars = bindingsResults.getVariables();

		if (bindingsResults.isAskResult()) {
			vars.add("boolean");
		}

		if (!columns.containsAll(vars) || columns.size() != vars.size()) {
			columns.clear();
			columns.addAll(vars);
			super.fireTableStructureChanged();
		}

		if (bindingsResults.isAskResult()) {
			HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
			row.put("boolean",
					new BindingValue(bindingsResults.getAskBoolean() ? "true" : "false", true, "xsd:boolean", true));
			rows.add(row);
		} else
			for (Bindings sol : bindingsResults.getBindings()) {
				HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
				for (String var : sol.getVariables()) {
					row.put(var, new BindingValue(sol.getValue(var), sol.isLiteral(var), sol.getDatatype(var), true));
				}
				rows.add(row);
			}

		if (subscriptionResultsTables.get(spuid) != null)
			subscriptionResultsTables.get(spuid).changeSelection(subscriptionResultsTables.get(spuid).getRowCount() - 1,
					0, false, false);

		super.fireTableDataChanged();
	}
}
