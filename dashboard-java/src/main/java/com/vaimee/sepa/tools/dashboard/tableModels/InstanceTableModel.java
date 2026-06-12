package com.vaimee.sepa.tools.dashboard.tableModels;

import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.commons.sparql.RDFTermURI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.tools.dashboard.DashboadApp;

public class InstanceTableModel extends AbstractTableModel {
	private static final Logger logger = LogManager.getLogger();
	
	private String[] columnStrings = { "Predicate", "Object", "Datatype" };
	private ArrayList<Bindings> rows = new ArrayList<Bindings>();

	private static final long serialVersionUID = 1L;

	private DashboadApp sepaClient;
	
	private GraphTableModel graphs;
	private JLabel currentSubject;
	private JTable graphsTable;
	
	public InstanceTableModel(GraphTableModel graphs, JLabel currentSubject,
	 JTable graphsTable) {
		this.graphs = graphs;
		this.currentSubject = currentSubject;
		this.graphsTable = graphsTable;
	}
	
	public String getColumnName(int col) {
		return columnStrings[col];
	}

	public int getRowCount() {
		return rows.size();
	}

	public int getColumnCount() {
		return columnStrings.length;
	}

	public Object getValueAt(int row, int col) {
		if (col == 0)
			return rows.get(row).getValue("predicate");
		else if (col == 1)
			return rows.get(row).getValue("object");
		else {
			try {
				if (rows.get(row).getRDFTerm("object").isURI())
					return "URI";
				if (rows.get(row).getRDFTerm("object").isBNode())
					return "BNODE";
			} catch (SEPABindingsException e) {
				logger.error(e.getMessage());
				return "???";
			}
			return rows.get(row).getDatatype("object");
		}
	}

	public boolean isCellEditable(int row, int col) {
		if (col == 1)
			return true;
		return false;
	}

	public void setValueAt(Object value, int row, int col) {
		Bindings newBindings = new Bindings();

		String graphString = (String) graphs.getValueAt(graphsTable.getSelectedRow(), 0);

		newBindings.addBinding("graph", new RDFTermURI(graphString));

		try {
			newBindings.addBinding("predicate", rows.get(row).getRDFTerm("predicate"));
			newBindings.addBinding("subject", new RDFTermURI(currentSubject.getText()));

			if (rows.get(row).getRDFTerm("object").isLiteral()) {
				newBindings.addBinding("object", new RDFTermLiteral((String) value));
				if (sepaClient == null) {
					JOptionPane.showMessageDialog(null, "You need to sign in first", "Warning: not authorized",
							JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				sepaClient.updateLiteral(newBindings);
			} else {
				newBindings.addBinding("object", new RDFTermURI((String) value));
//				if (sepaClient == null) {
//					JOptionPane.showMessageDialog(null, "You need to sign in first", "Warning: not authorized",
//							JOptionPane.INFORMATION_MESSAGE);
//					return;
//				}
				sepaClient.updateUri(newBindings);
			}
		} catch (SEPABindingsException e) {
			logger.error(e.getMessage());
			if (logger.isTraceEnabled())
				e.printStackTrace();
			return;
		}
		rows.remove(row);
		rows.add(row, newBindings);
		fireTableCellUpdated(row, col);
	}

	public void clear() {
		rows.clear();
		super.fireTableDataChanged();
	}

	public void addRow(Bindings b) {
		rows.add(b);
	}

	public void setSepaClient(DashboadApp sepaClient) {
		this.sepaClient = sepaClient;
	}
}
