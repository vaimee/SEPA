package com.vaimee.sepa.tools.dashboard.tableModels;

import javax.swing.RowFilter;

import com.vaimee.sepa.tools.dashboard.bindings.BindingValue;

public class RowRegexFilter extends RowFilter<BindingsTableModel,Integer>{

	private String m_text;
	
	public RowRegexFilter(String text) {
		m_text = text;
	}
	
	@Override
	public boolean include(Entry<? extends BindingsTableModel, ? extends Integer> entry) {
		if (m_text.isEmpty()) return true;

		for (int i = entry.getValueCount() - 1; i >= 0; i--) {
			if (entry.getValue(i) == null) continue;
			if (((BindingValue) entry.getValue(i)).get().contains(m_text)) {
		         return true;
		       }
		     }
		     return false;
	}

}
