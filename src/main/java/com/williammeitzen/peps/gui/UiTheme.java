package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;

import org.json.simple.JsonObject;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import com.williammeitzen.peps.model.Database;

import bibliothek.gui.dock.common.ColorMap;

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

	/**
	 * UIManager keys we override for Dark/Light chrome. Cleared before every scheme apply.
	 * Colors must be {@link ColorUIResource} so Look-and-Feel swaps can replace them; plain
	 * {@link Color} values stick on components and cause black-on-dark / white-on-light washouts.
	 */
	private static final String[] CHROME_OVERRIDE_KEYS = {
			"Panel.background", "Viewport.background", "ScrollPane.background",
			"SplitPane.background", "SplitPaneDivider.background",
			"MenuBar.background", "MenuBar.foreground", "Menu.background", "Menu.foreground",
			"MenuItem.background", "MenuItem.foreground",
			"CheckBoxMenuItem.background", "CheckBoxMenuItem.foreground",
			"RadioButtonMenuItem.background", "RadioButtonMenuItem.foreground",
			"PopupMenu.background", "PopupMenu.foreground", "ToolBar.background",
			"List.background", "List.foreground", "Table.background", "Table.foreground",
			"TableHeader.background", "TableHeader.foreground",
			"TextField.background", "TextField.foreground",
			"FormattedTextField.background", "FormattedTextField.foreground",
			"PasswordField.background", "PasswordField.foreground",
			"TextArea.background", "TextArea.foreground",
			"TextPane.background", "TextPane.foreground",
			"EditorPane.background", "EditorPane.foreground",
			"ComboBox.background", "ComboBox.foreground", "Spinner.background",
			"CheckBox.background", "CheckBox.foreground",
			"RadioButton.background", "RadioButton.foreground",
			"Button.background", "Button.foreground",
			"Label.background", "Label.foreground",
			"TitledBorder.titleColor",
			"TabbedPane.background", "TabbedPane.foreground", "TabbedPane.selectedBackground",
			"TabbedPane.selectedForeground", "TabbedPane.inactiveForeground", "TabbedPane.hoverForeground",
			"TabbedPane.hoverColor", "TabbedPane.focusColor", "TabbedPane.tabSeparatorColor",
			"TabbedPane.contentAreaColor",
			"TabbedPane.showTabSeparators", "TabbedPane.tabSeparatorsFullHeight",
			"TabbedPane.tabType", "TabbedPane.hasFullBorder", "TabbedPane.cardTabArc",
			"TitlePane.menuBarEmbedded", "TitlePane.foreground", "TitlePane.background",
			"Slider.tickColor", "Slider.foreground",
			"Slider.trackColor", "Slider.trackValueColor", "Slider.disabledTrackColor",
			"ToolTip.background", "ToolTip.foreground"
	};

	private static ColorUIResource ui(Color color) {
		return new ColorUIResource(color);
	}

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
		ColorUIResource menuBg = ui(new Color(0xF3, 0xF3, 0xF3));
		ColorUIResource menuFg = ui(LIGHT_MENU_FG);
		UIManager.put("TitlePane.menuBarEmbedded", Boolean.FALSE);
		UIManager.put("MenuBar.background", menuBg);
		UIManager.put("MenuBar.foreground", menuFg);
		UIManager.put("Menu.background", menuBg);
		UIManager.put("Menu.foreground", menuFg);
		UIManager.put("MenuItem.background", menuBg);
		UIManager.put("MenuItem.foreground", menuFg);
		UIManager.put("CheckBoxMenuItem.background", menuBg);
		UIManager.put("CheckBoxMenuItem.foreground", menuFg);
		UIManager.put("RadioButtonMenuItem.background", menuBg);
		UIManager.put("RadioButtonMenuItem.foreground", menuFg);
		UIManager.put("PopupMenu.background", menuBg);
		UIManager.put("PopupMenu.foreground", menuFg);
	}

	/**
	 * Paint File/View/Help and their items with explicit fg/bg so FlatLaf / Windows
	 * chrome cannot wash them out. Call after the menu bar is created and again after
	 * every theme refresh.
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
				styleMenuTree((JMenu) child, fg, bg);
			}
		}
		menuBar.revalidate();
		menuBar.repaint();
	}

	private static void styleMenuTree(JMenu menu, Color fg, Color bg) {
		menu.setOpaque(true);
		menu.setBackground(bg);
		menu.setForeground(fg);
		for (int i = 0; i < menu.getItemCount(); i++) {
			JMenuItem item = menu.getItem(i);
			if (item == null) {
				continue;
			}
			item.setOpaque(true);
			item.setBackground(bg);
			item.setForeground(fg);
			if (item instanceof JMenu) {
				styleMenuTree((JMenu) item, fg, bg);
			}
		}
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
		ColorUIResource bg = ui(DARK_BG);
		ColorUIResource panel = ui(DARK_PANEL);
		ColorUIResource elevated = ui(DARK_ELEVATED);
		ColorUIResource hover = ui(DARK_HOVER);
		ColorUIResource separator = ui(DARK_SEPARATOR);
		ColorUIResource tabSelected = ui(DARK_TAB_SELECTED);
		ColorUIResource fg = ui(DARK_MENU_FG);

		UIManager.put("Panel.background", panel);
		UIManager.put("Viewport.background", panel);
		UIManager.put("ScrollPane.background", panel);
		UIManager.put("SplitPane.background", bg);
		UIManager.put("SplitPaneDivider.background", elevated);
		UIManager.put("TitlePane.menuBarEmbedded", Boolean.FALSE);
		UIManager.put("MenuBar.background", bg);
		UIManager.put("MenuBar.foreground", fg);
		UIManager.put("Menu.background", bg);
		UIManager.put("Menu.foreground", fg);
		UIManager.put("MenuItem.background", bg);
		UIManager.put("MenuItem.foreground", fg);
		UIManager.put("CheckBoxMenuItem.background", bg);
		UIManager.put("CheckBoxMenuItem.foreground", fg);
		UIManager.put("RadioButtonMenuItem.background", bg);
		UIManager.put("RadioButtonMenuItem.foreground", fg);
		UIManager.put("TitlePane.foreground", fg);
		UIManager.put("TitlePane.background", bg);
		UIManager.put("PopupMenu.background", panel);
		UIManager.put("PopupMenu.foreground", fg);
		UIManager.put("ToolBar.background", panel);
		UIManager.put("List.background", panel);
		UIManager.put("List.foreground", fg);
		UIManager.put("Table.background", panel);
		UIManager.put("Table.foreground", fg);
		UIManager.put("TableHeader.background", elevated);
		UIManager.put("TableHeader.foreground", fg);
		UIManager.put("TextField.background", elevated);
		UIManager.put("TextField.foreground", fg);
		UIManager.put("FormattedTextField.background", elevated);
		UIManager.put("FormattedTextField.foreground", fg);
		UIManager.put("PasswordField.background", elevated);
		UIManager.put("PasswordField.foreground", fg);
		UIManager.put("TextArea.background", elevated);
		UIManager.put("TextArea.foreground", fg);
		UIManager.put("TextPane.background", elevated);
		UIManager.put("TextPane.foreground", fg);
		UIManager.put("EditorPane.background", elevated);
		UIManager.put("EditorPane.foreground", fg);
		UIManager.put("ComboBox.background", elevated);
		UIManager.put("ComboBox.foreground", fg);
		UIManager.put("Spinner.background", elevated);
		UIManager.put("CheckBox.background", panel);
		UIManager.put("CheckBox.foreground", fg);
		UIManager.put("RadioButton.background", panel);
		UIManager.put("RadioButton.foreground", fg);
		UIManager.put("Button.foreground", fg);
		UIManager.put("Label.background", panel);
		UIManager.put("Label.foreground", fg);
		UIManager.put("TitledBorder.titleColor", fg);

		UIManager.put("TabbedPane.background", bg);
		UIManager.put("TabbedPane.foreground", fg);
		UIManager.put("TabbedPane.selectedBackground", tabSelected);
		UIManager.put("TabbedPane.selectedForeground", fg);
		UIManager.put("TabbedPane.inactiveForeground", fg);
		UIManager.put("TabbedPane.hoverForeground", fg);
		UIManager.put("TabbedPane.hoverColor", hover);
		UIManager.put("TabbedPane.focusColor", tabSelected);
		UIManager.put("TabbedPane.tabSeparatorColor", separator);
		UIManager.put("TabbedPane.contentAreaColor", separator);
		UIManager.put("TabbedPane.showTabSeparators", Boolean.TRUE);
		UIManager.put("TabbedPane.tabSeparatorsFullHeight", Boolean.TRUE);
		UIManager.put("TabbedPane.tabType", "card");
		UIManager.put("TabbedPane.hasFullBorder", Boolean.TRUE);
		UIManager.put("TabbedPane.cardTabArc", 8);
		UIManager.put("Slider.foreground", fg);
		UIManager.put("Slider.tickColor", fg);
		// Visible track after Dark↔Default LAF swaps (FlatLaf reads these in FlatSliderUI).
		UIManager.put("Slider.trackColor", separator);
		UIManager.put("Slider.trackValueColor", fg);
		UIManager.put("Slider.disabledTrackColor", elevated);
	}

	public static void applyAndRefresh(Scheme scheme, Window window) {
		apply(scheme);
		saveScheme(scheme);
		for (Window w : Window.getWindows()) {
			if (!w.isDisplayable()) {
				continue;
			}
			// Plain Color setBackground/setForeground values survive LAF swaps; clear them
			// so updateComponentTreeUI can reinstall ColorUIResource defaults.
			unlockStuckColors(w);
			SwingUtilities.updateComponentTreeUI(w);
			w.invalidate();
			w.validate();
			w.repaint();
		}
		styleAllMenuBars();
		if (DockingLayout.getInstance() != null) {
			DockingLayout.getInstance().syncThemeColors();
		}
		// Custom-painted diagram/slider/docking chrome needs an explicit sync after LAF swap
		for (Window w : Window.getWindows()) {
			if (w.isDisplayable()) {
				repaintCustomSurfaces(w);
			}
		}
		if (window != null && window.isDisplayable()) {
			repaintCustomSurfaces(window);
		}
	}

	/**
	 * Swing only replaces colors that are null or {@link UIResource}. Explicit
	 * {@link Color} values (from older theme code or setBackground) stick and cause
	 * black-on-charcoal / white-on-white after scheme changes.
	 */
	static void unlockStuckColors(Component root) {
		if (root instanceof JComponent) {
			JComponent jc = (JComponent) root;
			Color bg = jc.getBackground();
			if (bg != null && !(bg instanceof UIResource)) {
				jc.setBackground(null);
			}
			Color fg = jc.getForeground();
			if (fg != null && !(fg instanceof UIResource)) {
				jc.setForeground(null);
			}
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				unlockStuckColors(child);
			}
		}
	}

	/** Match Docking Frames title/tab chrome to the active PEPS scheme. */
	public static void applyDockableColors(ColorMap colors) {
		if (colors == null) {
			return;
		}
		boolean dark = isDark();
		Color titleBg = dark ? DARK_BG : new Color(0xE8, 0xEE, 0xF4);
		Color titleFg = dark ? DARK_MENU_FG : LIGHT_MENU_FG;
		Color titleBgFocus = dark ? DARK_TAB_SELECTED : new Color(0xD0, 0xDC, 0xEA);
		Color tabBg = dark ? DARK_BG : new Color(0xF3, 0xF3, 0xF3);
		Color tabFg = titleFg;
		Color tabBgSelected = dark ? DARK_TAB_SELECTED : Color.WHITE;
		colors.setColor(ColorMap.COLOR_KEY_TITLE_BACKGROUND, titleBg);
		colors.setColor(ColorMap.COLOR_KEY_TITLE_FOREGROUND, titleFg);
		colors.setColor(ColorMap.COLOR_KEY_TITLE_BACKGROUND_FOCUSED, titleBgFocus);
		colors.setColor(ColorMap.COLOR_KEY_TITLE_FOREGROUND_FOCUSED, titleFg);
		colors.setColor(ColorMap.COLOR_KEY_TAB_BACKGROUND, tabBg);
		colors.setColor(ColorMap.COLOR_KEY_TAB_FOREGROUND, tabFg);
		colors.setColor(ColorMap.COLOR_KEY_TAB_BACKGROUND_SELECTED, tabBgSelected);
		colors.setColor(ColorMap.COLOR_KEY_TAB_FOREGROUND_SELECTED, titleFg);
		colors.setColor(ColorMap.COLOR_KEY_TAB_BACKGROUND_FOCUSED, titleBgFocus);
		colors.setColor(ColorMap.COLOR_KEY_TAB_FOREGROUND_FOCUSED, titleFg);
	}

	private static void repaintCustomSurfaces(Component root) {
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
			FormPanel form = (FormPanel) root;
			form.refreshPlanToolbarIcons();
			form.syncThemeSurfaces();
		} else if (root instanceof PlanDiagramToolbar) {
			((PlanDiagramToolbar) root).syncThemeSurfaces();
		} else if (root instanceof DiagramDockingPanel) {
			((DiagramDockingPanel) root).syncThemeColors();
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				repaintCustomSurfaces(child);
			}
		}
	}

	public static boolean isDark() {
		return FlatLaf.isLafDark();
	}

	/** Charcoal used for Dark panels — tests assert this does not linger after Light/Default. */
	public static Color darkPanelColor() {
		return DARK_PANEL;
	}

	public static Color darkBackgroundColor() {
		return DARK_BG;
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

	/** UIResource wrapper for setBackground/setForeground so later scheme swaps can replace it. */
	public static ColorUIResource uiColor(Color color) {
		return ui(color);
	}
}
