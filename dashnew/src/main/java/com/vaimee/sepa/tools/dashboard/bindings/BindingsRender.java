package com.vaimee.sepa.tools.dashboard.bindings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class BindingsRender extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 3932800852596396532L;

	DefaultTableModel namespaces;
	private boolean showAsQname = true;
	private boolean showDataType = true;

	public BindingsRender() {
		super();
	}

	public void setNamespaces(DefaultTableModel namespaces) {
		this.namespaces = namespaces;
	}

	public void showAsQName(boolean set) {
		showAsQname = set;
	}

	public void showDataType(boolean show) {
		showDataType = show;
	}

	private String qName(String value, boolean literal, String dataType) {
		if (namespaces == null)
			return value;
		if (value == null)
			return null;

		if (!literal) {
			for (int row = 0; row < namespaces.getRowCount(); row++) {
				String prefix = namespaces.getValueAt(row, 0).toString();
				String ns = namespaces.getValueAt(row, 1).toString();
				if (value.startsWith(ns))
					return value.replace(ns, prefix + ":");
			}
		} else if (dataType != null && showDataType) {
			for (int row = 0; row < namespaces.getRowCount(); row++) {
				String prefix = namespaces.getValueAt(row, 0).toString();
				String ns = namespaces.getValueAt(row, 1).toString();
				if (dataType.startsWith(ns)) {
					dataType = dataType.replace(ns, prefix + ":");
					break;
				}
			}
			return value + "^^" + dataType;
		}
		return value;
	}

	@Override
	public void setValue(Object value) {
		super.setValue(value);

		if (value == null)
			return;

		BindingValue binding = (BindingValue) value;

		if (binding.isLiteral()) {
			setFont(new Font(null, Font.BOLD, 12));
			setForeground(Color.BLACK);
		} else {
			setFont(new Font(null, Font.PLAIN, 12));
			setForeground(Color.BLACK);
		}
		if (binding.isAdded()) {
			setBackground(Color.WHITE);
		} else
			setBackground(Color.LIGHT_GRAY);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		BindingValue binding = (BindingValue) value;

		if (binding == null) {
			setText("");
			return this;
		}
		if (binding.get() == null) {
			setText("");
			return this;
		}

		// Render as qname or URI
		if (showAsQname)
			setText(qName(binding.get(), binding.isLiteral(), binding.getDataType()));
		else if (binding.isLiteral() && binding.getDataType() != null && showDataType)
			setText(binding.get() + "^^" + binding.getDataType());
		else
			setText(binding.get());

		if (isSelected) {
			this.setBackground(Color.YELLOW);
		}

		return this;
	}
}
