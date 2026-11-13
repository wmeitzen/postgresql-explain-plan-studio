package com.williammeitzen.peps.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Iterates Default / Light / Dark and asserts readable fg/bg contrast on:
 * menu bar, View scheme items, document tabs, Plans list, Plan Source controls,
 * diagram toolbar, and diagram canvas. Also asserts Dark charcoal does not linger
 * after switching to Light/Default (the Frankenstein theme from screenshots).
 */
class UiThemeContrastTest {

	private static final double MIN_CONTRAST = 3.0;

	private JFrame frame;
	private JMenuBar menuBar;
	private JMenu viewMenu;
	private JTabbedPane tabs;
	private PlanSourcePanel planSource;
	private JList<String> planList;
	private JScrollPane planListScroll;
	private PlanGraphPanel planGraph;
	private PlanDiagramToolbar diagramToolbar;
	private JPanel plansHost;

	@BeforeEach
	void setUp() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "Swing contrast checks need a display");
		onEdt(() -> {
			UiTheme.apply(UiTheme.Scheme.DARK);

			menuBar = new JMenuBar();
			JMenu fileMenu = new JMenu("File");
			fileMenu.add(new JMenuItem("Exit"));
			viewMenu = new JMenu("View");
			viewMenu.add(new JRadioButtonMenuItem(UiTheme.Scheme.DEFAULT.label()));
			viewMenu.add(new JRadioButtonMenuItem(UiTheme.Scheme.LIGHT.label()));
			viewMenu.add(new JRadioButtonMenuItem(UiTheme.Scheme.DARK.label()));
			JMenu helpMenu = new JMenu("Help");
			helpMenu.add(new JMenuItem("About ..."));
			menuBar.add(fileMenu);
			menuBar.add(viewMenu);
			menuBar.add(helpMenu);

			tabs = new JTabbedPane();
			planSource = new PlanSourcePanel(tabs);
			tabs.addTab("sample - Plan Source", planSource);
			tabs.addTab("Compare Plans", new JPanel());
			planGraph = new PlanGraphPanel(tabs);
			diagramToolbar = new PlanDiagramToolbar();
			JPanel diagramHost = new JPanel(new java.awt.BorderLayout());
			diagramHost.add(diagramToolbar, java.awt.BorderLayout.NORTH);
			diagramHost.add(planGraph, java.awt.BorderLayout.CENTER);
			tabs.addTab("sample - Plan Diagram", diagramHost);
			tabs.setSelectedIndex(0);

			DefaultListModel<String> model = new DefaultListModel<>();
			model.addElement("sample-plan");
			model.addElement("another-plan");
			planList = new JList<>(model);
			planList.setSelectedIndex(0);
			planListScroll = new JScrollPane(planList);
			plansHost = new JPanel(new java.awt.BorderLayout());
			plansHost.add(planListScroll, java.awt.BorderLayout.CENTER);

			JPanel shell = new JPanel(new java.awt.BorderLayout());
			shell.add(plansHost, java.awt.BorderLayout.WEST);
			shell.add(tabs, java.awt.BorderLayout.CENTER);

			frame = new JFrame("UiThemeContrastTest");
			frame.setJMenuBar(menuBar);
			frame.getContentPane().add(shell);
			frame.pack();
			frame.setSize(1000, 720);
			frame.setLocation(64, 64);
			frame.setVisible(true);
			UiTheme.styleMenuBar(menuBar);
		});
	}

	@AfterEach
	void tearDown() throws Exception {
		if (frame != null) {
			onEdt(() -> {
				frame.dispose();
				frame = null;
			});
		}
	}

	@Test
	void eachSchemeKeepsReadableContrastOnMenusTabsPlansAndPlanSource() throws Exception {
		for (UiTheme.Scheme scheme : UiTheme.Scheme.values()) {
			applyScheme(scheme);
			assertReadable("scheme=" + scheme.id(), collectTargets());
			assertTabUiManagerContrast("scheme=" + scheme.id());
			if (scheme != UiTheme.Scheme.DARK) {
				assertNoLingeringDarkChrome("scheme=" + scheme.id());
			}
		}
	}

	@Test
	void switchingSchemesBackAndForthKeepsReadableContrast() throws Exception {
		UiTheme.Scheme[] path = {
				UiTheme.Scheme.DARK,
				UiTheme.Scheme.LIGHT,
				UiTheme.Scheme.DARK,
				UiTheme.Scheme.DEFAULT,
				UiTheme.Scheme.DARK,
				UiTheme.Scheme.LIGHT,
				UiTheme.Scheme.DEFAULT,
				UiTheme.Scheme.LIGHT
		};
		for (UiTheme.Scheme scheme : path) {
			applyScheme(scheme);
			assertReadable("after->" + scheme.id(), collectTargets());
			assertTabUiManagerContrast("after->" + scheme.id());
			if (scheme != UiTheme.Scheme.DARK) {
				assertNoLingeringDarkChrome("after->" + scheme.id());
			}
		}
	}

	@Test
	void unlockClearsStuckDarkBackgroundsBeforeLightScheme() throws Exception {
		onEdt(() -> {
			// Simulate the pre-fix bug: plain Color charcoal stuck on Plans/tabs.
			Color stuck = new Color(UiTheme.darkPanelColor().getRGB());
			plansHost.setBackground(stuck);
			planList.setBackground(stuck);
			planList.setForeground(Color.BLACK);
			tabs.setBackground(stuck);
			tabs.setForeground(Color.BLACK);
		});
		applyScheme(UiTheme.Scheme.LIGHT);
		assertNoLingeringDarkChrome("after-unlock->light");
		assertReadable("after-unlock->light", collectTargets());
	}

	private void applyScheme(UiTheme.Scheme scheme) throws Exception {
		onEdt(() -> {
			UiTheme.apply(scheme);
			UiTheme.unlockStuckColors(frame);
			SwingUtilities.updateComponentTreeUI(frame);
			frame.invalidate();
			frame.validate();
			UiTheme.styleMenuBar(menuBar);
			planGraph.syncThemeSurface();
			diagramToolbar.syncThemeSurfaces();
			// Mirror FormPanel.syncThemeSurfaces for the test Plans list host.
			Color listBg = UIManager.getColor("List.background");
			Color listFg = UIManager.getColor("List.foreground");
			Color bg = listBg != null ? UiTheme.uiColor(listBg) : UiTheme.uiColor(UiTheme.surfaceColor());
			Color fg = listFg != null ? UiTheme.uiColor(listFg)
					: UiTheme.uiColor(UiTheme.isDark() ? new Color(0xE8, 0xEA, 0xED) : new Color(0x11, 0x18, 0x27));
			plansHost.setOpaque(true);
			plansHost.setBackground(UiTheme.uiColor(UiTheme.surfaceColor()));
			planList.setOpaque(true);
			planList.setBackground(bg);
			planList.setForeground(fg);
			planListScroll.getViewport().setBackground(bg);
			frame.repaint();
		});
		Thread.sleep(50);
	}

	private List<ContrastTarget> collectTargets() throws Exception {
		List<ContrastTarget> targets = new ArrayList<>();
		onEdt(() -> {
			for (Component c : menuBar.getComponents()) {
				if (c instanceof JMenu) {
					JMenu menu = (JMenu) c;
					targets.add(target("MenuBar/" + menu.getText(), menu));
					for (int i = 0; i < menu.getItemCount(); i++) {
						JMenuItem item = menu.getItem(i);
						if (item != null) {
							targets.add(target("MenuItem/" + menu.getText() + "/" + item.getText(), item));
						}
					}
				}
			}
			targets.add(target("PlansList", planList));
			targets.add(target("PlansHost", plansHost));
			targets.add(target("DocumentTabs", tabs));
			targets.add(target("PlanGraph", planGraph));
			targets.add(target("DiagramToolbar", diagramToolbar));
			collectLabeledControls(planSource, "PlanSource", targets);
			collectLabeledControls(diagramToolbar, "DiagramToolbar", targets);
		});
		return targets;
	}

	private void assertTabUiManagerContrast(String phase) {
		Color bg = UIManager.getColor("TabbedPane.background");
		Color fg = UIManager.getColor("TabbedPane.foreground");
		Color inactive = UIManager.getColor("TabbedPane.inactiveForeground");
		if (bg == null || fg == null) {
			return;
		}
		double ratio = contrastRatio(fg, bg);
		if (ratio < MIN_CONTRAST) {
			fail(String.format("%s TabbedPane UIManager fg/bg contrast %.2f < %.1f (fg=%s bg=%s)",
					phase, ratio, MIN_CONTRAST, rgb(fg), rgb(bg)));
		}
		if (inactive != null) {
			double inactiveRatio = contrastRatio(inactive, bg);
			if (inactiveRatio < MIN_CONTRAST) {
				fail(String.format("%s TabbedPane inactiveForeground contrast %.2f < %.1f (fg=%s bg=%s)",
						phase, inactiveRatio, MIN_CONTRAST, rgb(inactive), rgb(bg)));
			}
		}
	}

	private void assertNoLingeringDarkChrome(String phase) throws Exception {
		List<String> offenders = new ArrayList<>();
		onEdt(() -> findDarkChrome(frame, "frame", offenders));
		if (!offenders.isEmpty()) {
			fail(phase + " still has Dark charcoal after leaving Dark:\n" + String.join("\n", offenders));
		}
	}

	private static void findDarkChrome(Component c, String path, List<String> offenders) {
		Color darkPanel = UiTheme.darkPanelColor();
		Color darkBg = UiTheme.darkBackgroundColor();
		Color bg = c.getBackground();
		if (bg != null && c.isShowing() && (sameRgb(bg, darkPanel) || sameRgb(bg, darkBg))) {
			// Allow only when the active scheme is actually dark.
			if (!UiTheme.isDark()) {
				offenders.add(path + " bg=" + rgb(bg) + " (" + c.getClass().getSimpleName() + ")");
			}
		}
		if (c instanceof Container) {
			Component[] kids = ((Container) c).getComponents();
			for (int i = 0; i < kids.length; i++) {
				findDarkChrome(kids[i], path + "/" + kids[i].getClass().getSimpleName() + "[" + i + "]", offenders);
			}
		}
	}

	private static boolean sameRgb(Color a, Color b) {
		return a.getRed() == b.getRed() && a.getGreen() == b.getGreen() && a.getBlue() == b.getBlue();
	}

	private static void collectLabeledControls(Container root, String prefix, List<ContrastTarget> out) {
		for (Component child : root.getComponents()) {
			String name = prefix + "/" + child.getClass().getSimpleName();
			if (child instanceof JLabel
					|| child instanceof JButton
					|| child instanceof JCheckBox
					|| child instanceof JRadioButton) {
				out.add(target(name + ":" + String.valueOf(labelOf(child)), child));
			}
			if (child instanceof Container) {
				collectLabeledControls((Container) child, name, out);
			}
		}
	}

	private static String labelOf(Component c) {
		if (c instanceof JLabel) {
			return ((JLabel) c).getText();
		}
		if (c instanceof JButton) {
			return ((JButton) c).getText();
		}
		if (c instanceof JCheckBox) {
			return ((JCheckBox) c).getText();
		}
		if (c instanceof JRadioButton) {
			return ((JRadioButton) c).getText();
		}
		return "";
	}

	private static ContrastTarget target(String id, Component c) {
		Color fg = effectiveForeground(c);
		Color bg = effectiveBackground(c);
		return new ContrastTarget(id, fg, bg, contrastRatio(fg, bg));
	}

	private static Color effectiveForeground(Component c) {
		Color fg = c.getForeground();
		if (fg != null) {
			return fg;
		}
		if (c instanceof JList) {
			Color u = UIManager.getColor("List.foreground");
			if (u != null) {
				return u;
			}
		}
		if (c instanceof JMenu || c instanceof JMenuItem) {
			Color u = UIManager.getColor("MenuItem.foreground");
			if (u != null) {
				return u;
			}
		}
		Color u = UIManager.getColor("Label.foreground");
		return u != null ? u : Color.BLACK;
	}

	private static Color effectiveBackground(Component c) {
		Color bg = c.getBackground();
		if (bg != null && (c.isOpaque() || c instanceof JList || c instanceof JMenu || c instanceof JMenuItem
				|| c instanceof PlanGraphPanel || c instanceof JTabbedPane || c instanceof PlanDiagramToolbar)) {
			return bg;
		}
		Container p = c.getParent();
		while (p != null) {
			if (p.isOpaque() && p.getBackground() != null) {
				return p.getBackground();
			}
			p = p.getParent();
		}
		Color panel = UIManager.getColor("Panel.background");
		return panel != null ? panel : Color.WHITE;
	}

	private static void assertReadable(String phase, List<ContrastTarget> targets) {
		List<String> failures = new ArrayList<>();
		for (ContrastTarget t : targets) {
			if (t.fg == null || t.bg == null) {
				failures.add(phase + " " + t.id + ": missing colors fg=" + t.fg + " bg=" + t.bg);
				continue;
			}
			if (t.ratio < MIN_CONTRAST) {
				failures.add(String.format(
						"%s %s: contrast %.2f < %.1f (fg=%s bg=%s)",
						phase, t.id, t.ratio, MIN_CONTRAST, rgb(t.fg), rgb(t.bg)));
			}
		}
		if (!failures.isEmpty()) {
			fail(String.join("\n", failures));
		}
		assertTrue(targets.size() > 10, "expected a non-trivial set of UI targets, got " + targets.size());
	}

	static double contrastRatio(Color a, Color b) {
		double l1 = relativeLuminance(a);
		double l2 = relativeLuminance(b);
		double lighter = Math.max(l1, l2);
		double darker = Math.min(l1, l2);
		return (lighter + 0.05) / (darker + 0.05);
	}

	private static double relativeLuminance(Color c) {
		double r = linearize(c.getRed() / 255.0);
		double g = linearize(c.getGreen() / 255.0);
		double b = linearize(c.getBlue() / 255.0);
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private static double linearize(double channel) {
		return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
	}

	private static String rgb(Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}

	private static void onEdt(ThrowingRunnable action) throws Exception {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					action.run();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException && cause.getCause() instanceof Exception) {
				throw (Exception) cause.getCause();
			}
			if (cause instanceof Exception) {
				throw (Exception) cause;
			}
			throw e;
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	private static final class ContrastTarget {
		final String id;
		final Color fg;
		final Color bg;
		final double ratio;

		ContrastTarget(String id, Color fg, Color bg, double ratio) {
			this.id = id;
			this.fg = fg;
			this.bg = bg;
			this.ratio = ratio;
		}
	}
}
