package com.williammeitzen.peps;

import javax.swing.SwingUtilities;

import com.williammeitzen.peps.gui.MainFrame;

public class PostgreSQLExplainPlanStudio {

	public static void main(String[] args) {
		
		//System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PostgreSQL Explain Plan Studio");
		//System.setProperty("apple.laf.useScreenMenuBar", "true");
		//System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PostgreSQL Explain Plan Studio");
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new MainFrame();
			}
		});
	}
}
