package com.williammeitzen.peps.gui;

import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.json.simple.JsonObject;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.williammeitzen.peps.model.Database;

/**
 * Application color scheme (View menu): Default = system LAF, Light/Dark = FlatLaf.
 */
public final class UiTheme {

	public enum Scheme {
		DEFAULT("default", "Default"),
		LIGHT("light", "Light"),
		DARK("dark", "Dark");

		private final String id;
		private final String label;

		Scheme(String id, String label) {
			this.id = id;
			this.label = label;
		}

		public String id() {
			return id;
		}

		public String label() {
			return label;
		}

		public static Scheme fromId(String id) {
			if (id == null || id.isBlank()) {
				return DEFAULT;
			}
			for (Scheme scheme : values()) {
				if (scheme.id.equalsIgnoreCase(id.trim())) {
					return scheme;
				}
			}
			return DEFAULT;
		}
	}

	private UiTheme() {
	}

	public static Scheme loadSavedScheme() {
		try {
			JsonObject row = Database.row("select color_scheme from config;");
			if (row != null && row.get("color_scheme") != null) {
				return Scheme.fromId(String.valueOf(row.get("color_scheme")));
			}
		} catch (Exception ignored) {
			// Column may not exist yet during very early init
		}
		return Scheme.DEFAULT;
	}

	public static void saveScheme(Scheme scheme) {
		if (scheme == null) {
			scheme = Scheme.DEFAULT;
		}
		Database.dml("update config set color_scheme = '" + scheme.id() + "';");
	}

	public static void apply(Scheme scheme) {
		if (scheme == null) {
			scheme = Scheme.DEFAULT;
		}
		try {
			switch (scheme) {
			case LIGHT:
				UIManager.setLookAndFeel(new FlatLightLaf());
				break;
			case DARK:
				UIManager.setLookAndFeel(new FlatDarkLaf());
				break;
			case DEFAULT:
			default:
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				break;
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception fallback) {
				fallback.printStackTrace();
			}
		}
		// Show full tab titles instead of truncating with "..."
		UIManager.put("TabbedPane.tabWidthMode", "preferred");
	}

	public static void applyAndRefresh(Scheme scheme, Window window) {
		apply(scheme);
		saveScheme(scheme);
		if (window != null) {
			SwingUtilities.updateComponentTreeUI(window);
			window.invalidate();
			window.validate();
			window.repaint();
		}
		for (Window w : Window.getWindows()) {
			if (w != window && w.isDisplayable()) {
				SwingUtilities.updateComponentTreeUI(w);
			}
		}
	}

	public static boolean isDark() {
		return FlatLaf.isLafDark();
	}
}
