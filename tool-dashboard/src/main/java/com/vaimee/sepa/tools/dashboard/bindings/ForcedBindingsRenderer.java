package com.vaimee.sepa.tools.dashboard.bindings;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.tools.dashboard.utils.Utilities;

public class ForcedBindingsRenderer extends DefaultTableCellRenderer {
	private static final Logger logger = LogManager.getLogger();
	/**
	 * 
	 */
	private static final long serialVersionUID = -1541296097107576037L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {

		// Cells are by default rendered as a JLabel.
		JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		if (col == 2) {
			String v = (String) table.getValueAt(row, 1);
			String type = (String) table.getValueAt(row, 2);
			logger.trace("Row: " + row + " Col: " + col + " Value: " + v + " Type: " + type);
			if (type == null)
				l.setBackground(Color.WHITE);
			else if (Utilities.checkType(v, type)) {
				if (v.equals(""))
					l.setBackground(Color.ORANGE);
				else
					l.setBackground(Color.GREEN);
			} else
				l.setBackground(Color.RED);
		} else
			l.setBackground(Color.WHITE);

		l.setForeground(Color.BLACK);

		return l;
	}
}
