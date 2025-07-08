package com.vaimee.sepa.tools.dashboard.tableModels;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractListModel;

public class SortedListModel extends AbstractListModel<String> {

	/**
	* 
	*/
	private static final long serialVersionUID = -4860350252985388420L;

	SortedSet<String> model;

	public SortedListModel() {
		model = new TreeSet<String>();
	}

	public int getSize() {
		return model.size();
	}

	public String getElementAt(int index) {
		return (String) model.toArray()[index];
	}

	public void add(String element) {
		if (model.add(element)) {
			fireContentsChanged(this, 0, getSize());
		}
	}

	public void clear() {
		model.clear();
		fireContentsChanged(this, 0, getSize());
	}
}
