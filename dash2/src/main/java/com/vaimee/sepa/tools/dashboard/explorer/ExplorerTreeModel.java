package com.vaimee.sepa.tools.dashboard.explorer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class ExplorerTreeModel extends DefaultTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7082219083126306248L;

	public ExplorerTreeModel(TreeNode root) {
		super(root);
	}

	public ExplorerTreeModel() {
		this(new DefaultMutableTreeNode("owl:Thing") {
			/**
			 * 
			 */
			private static final long serialVersionUID = -2640698448157863184L;

			{
			}
		});
	}

}
