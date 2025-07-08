package com.vaimee.sepa.tools.dashboard.explorer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.vaimee.sepa.api.commons.sparql.Bindings;

public class ExplorerTreeRenderer extends DefaultTreeCellRenderer {
	private JCheckBox chckbxQname;
	
	public ExplorerTreeRenderer(JCheckBox chckbxQname) {
		this.chckbxQname = chckbxQname;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 4538238852715730476L;

	DefaultTableModel namespaces;

	public void setNamespaces(DefaultTableModel namespaces) {
		this.namespaces = namespaces;
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		if (row == 0)
			return this;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Bindings nodeInfo = (Bindings) (node.getUserObject());

		String text = "";
		if (nodeInfo.getValue("class") != null) {
			text = nodeInfo.getValue("class");
		} else {
			text = nodeInfo.getValue("instance");
		}
		setToolTipText(text);

		if (chckbxQname.isSelected()) {
			for (int r = 0; r < namespaces.getRowCount(); r++) {
				String prefix = namespaces.getValueAt(r, 0).toString();
				String ns = namespaces.getValueAt(r, 1).toString();
				if (text.startsWith(ns)) {
					text = text.replace(ns, prefix + ":");
					break;
				}
			}
		}
		setText(text);

		return this;
	}
}
