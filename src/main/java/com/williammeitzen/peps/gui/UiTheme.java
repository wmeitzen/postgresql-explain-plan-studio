package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.json.simple.JsonObject;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
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
	private static final Color DARK_MENU_FG = new Color(0xE8, 0xEA, 0xED);
	/** Near-black menu text — readable on light menu bars / Windows light title chrome. */
	private static final Color LIGHT_MENU_FG = new Color(0x11, 0x18, 0x27);

	/** UIManager keys we override for Dark; must be cleared when leaving Dark. */
	private static final String[] CHROME_OVERRIDE_KEYS = {
			"Panel.background", "Viewport.background", "ScrollPane.background",
			"SplitPane.background", "SplitPaneDivider.background",
			"MenuBar.background", "MenuBar.foreground", "Menu.background", "Menu.foreground",
			"MenuItem.foreground", "CheckBoxMenuItem.foreground", "RadioButtonMenuItem.foreground",
			"PopupMenu.background", "ToolBar.background",
			"List.background", "Table.background", "TableHeader.background",
			"TextField.background", "FormattedTextField.background", "PasswordField.background",
			"TextArea.background", "TextPane.background", "EditorPane.background",
			"ComboBox.background", "Spinner.background",
			"CheckBox.background", "RadioButton.background", "Label.background",
			"TabbedPane.background", "TabbedPane.selectedBackground", "TabbedPane.hoverColor",
			"TabbedPane.focusColor", "TabbedPane.tabSeparatorColor", "TabbedPane.contentAreaColor",
			"TabbedPane.showTabSeparators", "TabbedPane.tabSeparatorsFullHeight",
			"TabbedPane.tabType", "TabbedPane.hasFullBorder", "TabbedPane.cardTabArc",
			"TitlePane.menuBarEmbedded", "TitlePane.foreground", "TitlePane.background"
	};

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
				return DARK;
			}
			for (Scheme scheme : values()) {
				if (scheme.id.equalsIgnoreCase(id.trim())) {
					return scheme;
				}
			}
			return DARK;
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
		return Scheme.DARK;
	}

	public static void saveScheme(Scheme scheme) {
		if (scheme == null) {
			scheme = Scheme.DARK;
		}
		Database.dml("update config set color_scheme = '" + scheme.id() + "';");
	}

	public static void apply(Scheme scheme) {
		if (scheme == null) {
			scheme = Scheme.DARK;
		}
		// Developer UIManager.put() values survive LAF swaps — clear Dark leftovers first.
		clearChromeOverrides();
		try {
			switch (scheme) {
			case LIGHT:
				UIManager.setLookAndFeel(new FlatLightLaf());
				// Native Win11 title chrome was painting near-white File/View/Help on a light bar.
				applyNativeWindowDecorations(false);
				applyLightMenuContrast();
				break;
			case DARK:
				UIManager.setLookAndFeel(new FlatDarkLaf());
				applyNativeWindowDecorations(false);
				applyDarkChromeOverrides();
				break;
			case DEFAULT:
			default:
				// System LAF already uses the OS title bar / color scheme.
				applyNativeWindowDecorations(false);
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				applyLightMenuContrast();
				break;
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				applyLightMenuContrast();
			} catch (Exception fallback) {
				fallback.printStackTrace();
			}
		}
		// Show full tab titles instead of truncating with "..."
		UIManager.put("TabbedPane.tabWidthMode", "preferred");
	}

	private static void clearChromeOverrides() {
		for (String key : CHROME_OVERRIDE_KEYS) {
			UIManager.put(key, null);
			UIManager.getDefaults().remove(key);
		}
	}

	/**
	 * Force dark File/View/Help labels on a light menu bar (UIManager + later {@link #styleMenuBar}).
	 */
	private static void applyLightMenuContrast() {
		UIManager.put("TitlePane.menuBarEmbedded", Boolean.FALSE);
		UIManager.put("MenuBar.background", new Color(0xF3, 0xF3, 0xF3));
		UIManager.put("MenuBar.foreground", LIGHT_MENU_FG);
		UIManager.put("Menu.foreground", LIGHT_MENU_FG);
		UIManager.put("MenuItem.foreground", LIGHT_MENU_FG);
		UIManager.put("CheckBoxMenuItem.foreground", LIGHT_MENU_FG);
		UIManager.put("RadioButtonMenuItem.foreground", LIGHT_MENU_FG);
	}

	/**
	 * Paint File/View/Help with explicit fg/bg so FlatLaf / Windows chrome cannot wash them out.
	 * Call after the menu bar is created and again after every theme refresh.
	 */
	public static void styleMenuBar(JMenuBar menuBar) {
		if (menuBar == null) {
			return;
		}
		boolean dark = isDark();
		Color fg = dark ? DARK_MENU_FG : LIGHT_MENU_FG;
		Color bg = dark ? DARK_BG : new Color(0xF3, 0xF3, 0xF3);
		menuBar.setOpaque(true);
		menuBar.setBackground(bg);
		menuBar.setForeground(fg);
		for (Component child : menuBar.getComponents()) {
			if (child instanceof JMenu) {
				JMenu menu = (JMenu) child;
				menu.setOpaque(true);
				menu.setBackground(bg);
				menu.setForeground(fg);
			}
		}
		menuBar.revalidate();
		menuBar.repaint();
	}

	/** Style every displayable frame's menu bar after a LAF swap. */
	public static void styleAllMenuBars() {
		for (Window w : Window.getWindows()) {
			if (w instanceof JFrame) {
				styleMenuBar(((JFrame) w).getJMenuBar());
			}
		}
	}

	/**
	 * On Windows 10+, FlatLaf can use the native title bar so Light/Dark schemes still
	 * pick up the user's Windows accent and caption colors instead of a custom painted bar.
	 */
	private static void applyNativeWindowDecorations(boolean enabled) {
		if (!SystemInfo.isWindows_10_orLater) {
			return;
		}
		try {
			FlatLaf.setUseNativeWindowDecorations(enabled);
		} catch (RuntimeException ignored) {
			// Older FlatLaf / unsupported host — leave decorations alone.
		}
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
		UIManager.put("TitlePane.menuBarEmbedded", Boolean.FALSE);
		UIManager.put("MenuBar.background", DARK_BG);
		UIManager.put("MenuBar.foreground", DARK_MENU_FG);
		UIManager.put("Menu.background", DARK_BG);
		UIManager.put("Menu.foreground", DARK_MENU_FG);
		UIManager.put("MenuItem.foreground", DARK_MENU_FG);
		UIManager.put("CheckBoxMenuItem.foreground", DARK_MENU_FG);
		UIManager.put("RadioButtonMenuItem.foreground", DARK_MENU_FG);
		UIManager.put("TitlePane.foreground", DARK_MENU_FG);
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
		styleAllMenuBars();
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
		} else if (root instanceof FormPanel) {
			((FormPanel) root).refreshPlanToolbarIcons();
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
