package com.vaimee.sepa.tools.dashboard;

import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

public class DashboardFileFilter extends FileFilter {
	private ArrayList<String> extensions = new ArrayList<String>();
	private String title = "Title";

	public DashboardFileFilter(String title, String ext) {
		super();
		extensions.add(ext);
		this.title = title;
	}

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		for (String ext : extensions)
			if (f.getName().contains(ext))
				return true;
		return false;
	}

	@Override
	public String getDescription() {
		return title;
	}
	
	
}
