package com.vaimee.sepa.tools.dashboard.utils;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class CopyAction extends AbstractAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5927169526678239559L;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		final JTable tbl = (JTable) e.getSource();

		StringBuffer sbf = new StringBuffer();

		int[] rowsselected = tbl.getSelectedRows();
		int[] colsselected = tbl.getSelectedColumns();

		for (int i = 0; i < rowsselected.length; i++) {
			for (int j = 0; j < colsselected.length; j++) {
				TableCellRenderer renderer = tbl.getCellRenderer(rowsselected[i], colsselected[j]);
				final Component comp = tbl.prepareRenderer(renderer, rowsselected[i], colsselected[j]);
				String toCopy = ((JLabel) comp).getText();
				sbf.append(toCopy);
				if (j < colsselected.length - 1)
					sbf.append("\t");
			}
			if (i < rowsselected.length - 1)
				sbf.append("\n");
		}
		StringSelection stsel = new StringSelection(sbf.toString());
		Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
		system.setContents(stsel, stsel);
	}

}
