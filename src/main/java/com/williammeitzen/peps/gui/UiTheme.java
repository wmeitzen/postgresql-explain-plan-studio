package com.williammeitzen.peps.gui;

import java.awt.Color;
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

	/** Shared dark surface — keeps menu, tabs, panels, and lists on one charcoal. */
	private static final Color DARK_BG = new Color(0x2B, 0x2D, 0x30);
	private static final Color DARK_PANEL = new Color(0x31, 0x33, 0x35);
	private static final Color DARK_ELEVATED = new Color(0x3C, 0x3F, 0x41);
	private static final Color DARK_HOVER = new Color(0x38, 0x3A, 0x3E);
	private static final Color DARK_SEPARATOR = new Color(0x6B, 0x6E, 0x75);
	private static final Color DARK_TAB_SELECTED = new Color(0x45, 0x48, 0x4C);

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
				applyDarkChromeOverrides();
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

	/**
	 * FlatDarkLaf underline tabs are hard to see; card tabs + separators make them
	 * read as tabs. Also unify panel/menu/list backgrounds so chrome is one charcoal.
	 */
	private static void applyDarkChromeOverrides() {
		UIManager.put("Panel.background", DARK_PANEL);
		UIManager.put("Viewport.background", DARK_PANEL);
		UIManager.put("ScrollPane.background", DARK_PANEL);
		UIManager.put("SplitPane.background", DARK_BG);
		UIManager.put("SplitPaneDivider.background", DARK_ELEVATED);
		UIManager.put("MenuBar.background", DARK_BG);
		UIManager.put("Menu.background", DARK_BG);
		UIManager.put("PopupMenu.background", DARK_PANEL);
		UIManager.put("ToolBar.background", DARK_PANEL);
		UIManager.put("List.background", DARK_PANEL);
		UIManager.put("Table.background", DARK_PANEL);
		UIManager.put("TableHeader.background", DARK_ELEVATED);
		UIManager.put("TextField.background", DARK_ELEVATED);
		UIManager.put("FormattedTextField.background", DARK_ELEVATED);
		UIManager.put("PasswordField.background", DARK_ELEVATED);
		UIManager.put("TextArea.background", DARK_ELEVATED);
		UIManager.put("TextPane.background", DARK_ELEVATED);
		UIManager.put("EditorPane.background", DARK_ELEVATED);
		UIManager.put("ComboBox.background", DARK_ELEVATED);
		UIManager.put("Spinner.background", DARK_ELEVATED);
		UIManager.put("CheckBox.background", DARK_PANEL);
		UIManager.put("RadioButton.background", DARK_PANEL);
		UIManager.put("Label.background", DARK_PANEL);

		UIManager.put("TabbedPane.background", DARK_BG);
		UIManager.put("TabbedPane.selectedBackground", DARK_TAB_SELECTED);
		UIManager.put("TabbedPane.hoverColor", DARK_HOVER);
		UIManager.put("TabbedPane.focusColor", DARK_TAB_SELECTED);
		UIManager.put("TabbedPane.tabSeparatorColor", DARK_SEPARATOR);
		UIManager.put("TabbedPane.contentAreaColor", DARK_SEPARATOR);
		UIManager.put("TabbedPane.showTabSeparators", Boolean.TRUE);
		UIManager.put("TabbedPane.tabSeparatorsFullHeight", Boolean.TRUE);
		UIManager.put("TabbedPane.tabType", "card");
		UIManager.put("TabbedPane.hasFullBorder", Boolean.TRUE);
		UIManager.put("TabbedPane.cardTabArc", 8);
	}

	public static void applyAndRefresh(Scheme scheme, Window window) {
		apply(scheme);
		saveScheme(scheme);
		for (Window w : Window.getWindows()) {
			if (!w.isDisplayable()) {
				continue;
			}
			SwingUtilities.updateComponentTreeUI(w);
			w.invalidate();
			w.validate();
			w.repaint();
		}
		// Custom-painted diagram/slider chrome needs an explicit repaint after LAF swap
		if (window != null) {
			repaintCustomSurfaces(window);
		}
	}

	private static void repaintCustomSurfaces(java.awt.Component root) {
		if (root instanceof SliderPanel) {
			((SliderPanel) root).reinstallSpanAwareSliderUI();
			root.revalidate();
			root.repaint();
		} else if (root instanceof PlanGraphPanel) {
			((PlanGraphPanel) root).syncThemeSurface();
			root.revalidate();
			root.repaint();
		} else if (root instanceof OperatorDetailsPanel) {
			((OperatorDetailsPanel) root).applyThemeSurface();
			root.revalidate();
			root.repaint();
		}
		if (root instanceof java.awt.Container) {
			for (java.awt.Component child : ((java.awt.Container) root).getComponents()) {
				repaintCustomSurfaces(child);
			}
		}
	}

	public static boolean isDark() {
		return FlatLaf.isLafDark();
	}

	/** Preferred panel/canvas fill for custom surfaces. */
	public static Color surfaceColor() {
		if (isDark()) {
			return DARK_PANEL;
		}
		Color panel = UIManager.getColor("Panel.background");
		return panel != null ? panel : Color.WHITE;
	}

	public static Color elevatedSurfaceColor() {
		if (isDark()) {
			return DARK_ELEVATED;
		}
		return new Color(0xF8, 0xFA, 0xFC);
	}

	public static Color subtleBorderColor() {
		if (isDark()) {
			return DARK_SEPARATOR;
		}
		Color border = UIManager.getColor("Component.borderColor");
		return border != null ? border : new Color(0xE2, 0xE8, 0xF0);
	}
}
