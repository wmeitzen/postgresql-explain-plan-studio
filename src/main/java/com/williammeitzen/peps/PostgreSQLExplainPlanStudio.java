package com.williammeitzen.peps;

import javax.swing.SwingUtilities;

import com.williammeitzen.peps.gui.MainFrame;

public class PostgreSQLExplainPlanStudio {

	public static void main(String[] args) {
		// Must be set before AWT/Swing init — otherwise the Mac menu bar uses the
		// main class name (PostgreSQLExplainPlanStudio) with no spaces.
		System.setProperty("apple.awt.application.name", "PostgreSQL Explain Plan Studio");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name",
				"PostgreSQL Explain Plan Studio");
		// Ubuntu/VMware: the XRender pipeline caches same-size BufferedImage icons by
		// dimensions only, so Plans toolbar glyphs all paint as the settings gear and
		// Get Started cards all share one icon. Disable before Toolkit/Swing starts.
		System.setProperty("sun.java2d.xrender", "false");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new MainFrame();
			}
		});
	}
}
