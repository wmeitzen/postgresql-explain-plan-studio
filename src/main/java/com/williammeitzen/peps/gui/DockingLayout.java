package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.williammeitzen.peps.model.Database;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.util.TextManager;
import bibliothek.util.xml.XElement;
import bibliothek.util.xml.XIO;

/**
 * Main docking shell: Plans tool window + center Documents tabs.
 * Properties / Sliders live in {@link DiagramDockingPanel} inside Plan Diagram.
 */
public class DockingLayout {

	public static final String ID_PLANS = "plans";
	public static final String ID_DOCUMENTS = "documents";

	private static DockingLayout instance;

	private final CControl control;
	private final DefaultSingleCDockable plansDockable;
	private final DefaultSingleCDockable documentsDockable;

	private JCheckBoxMenuItem plansMenuItem;
	/** Called after Plans show/hide so nested Plan Diagram docks can re-sync. */
	private Runnable layoutChangedHook;

	public DockingLayout(JFrame owner, FormPanel plansPanel, JComponent documentsContent) {
		instance = this;
		control = new CControl(owner);
		SsmsDockingGuides.install(control);
		installShortActionTexts();

		plansPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		plansDockable = createToolDockable(ID_PLANS, "Plans", plansPanel);

		// Cap shell minimum so nested diagram docks (Properties/Sliders mins) do not
		// propagate up and lock the Plans|Documents divider when the frame is narrow.
		JPanel documentsHost = new JPanel(new BorderLayout()) {
			@Override
			public java.awt.Dimension getMinimumSize() {
				return new java.awt.Dimension(180, 100);
			}
		};
		documentsHost.add(documentsContent, BorderLayout.CENTER);
		documentsDockable = new DefaultSingleCDockable(ID_DOCUMENTS, "Documents", documentsHost);
		documentsDockable.setCloseable(false);
		documentsDockable.setExternalizable(false);
		documentsDockable.setMinimizable(false);
		documentsDockable.setMaximizable(true);

		control.addDockable(plansDockable);
		control.addDockable(documentsDockable);

		plansDockable.addCDockableStateListener(new CDockableAdapter() {
			@Override
			public void visibilityChanged(CDockable dockable) {
				syncViewMenuFromDockables();
			}
		});

		if (!restoreLayout()) {
			applyDefaultLayout();
		}
	}

	public static DockingLayout getInstance() {
		return instance;
	}

	public Component getContentArea() {
		return control.getContentArea();
	}

	/** Keep Plans / Documents docking titles aligned with the active PEPS color scheme. */
	public void syncThemeColors() {
		UiTheme.applyDockableColors(plansDockable.getColors());
		UiTheme.applyDockableColors(documentsDockable.getColors());
		Component content = control.getContentArea();
		if (content != null) {
			content.revalidate();
			content.repaint();
		}
	}

	public void installViewMenuItems(JMenu viewMenu) {
		viewMenu.addSeparator();
		plansMenuItem = new JCheckBoxMenuItem("Plans");
		viewMenu.add(plansMenuItem);
		plansMenuItem.addActionListener(e -> setPlansVisible(plansMenuItem.isSelected()));
		// Properties / Sliders / Zoom are installed by FormPanel for the active Plan Diagram.
		syncViewMenuFromDockables();
	}

	public void setLayoutChangedHook(Runnable hook) {
		this.layoutChangedHook = hook;
	}

	/**
	 * Show Plans via location (avoids a full shell deploy that scrambled nested diagram
	 * docks). Hide with {@code setVisible(false)}; if Docking Frames leaves a ghost,
	 * fall back to a Documents-only deploy then repair nested docks.
	 */
	public void setPlansVisible(boolean visible) {
		if (visible) {
			plansDockable.setLocation(CLocation.base(control.getContentArea()).normalWest(0.20));
			plansDockable.setVisible(true);
			documentsDockable.setVisible(true);
		} else {
			plansDockable.setVisible(false);
			if (plansDockable.isDockableVisible()) {
				CGrid grid = new CGrid(control);
				grid.add(0, 0, 5, 3, documentsDockable);
				control.getContentArea().deploy(grid);
				documentsDockable.setVisible(true);
			}
		}
		syncViewMenuFromDockables();
		fireLayoutChanged();
	}

	private void fireLayoutChanged() {
		if (layoutChangedHook == null) {
			return;
		}
		Runnable hook = layoutChangedHook;
		if (SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(hook);
		} else {
			SwingUtilities.invokeLater(hook);
		}
	}

	/**
	 * Fresh-install shell layout: Plans on the left, Documents filling the rest.
	 * Clears any persisted docking XML so the next launch stays reset too.
	 */
	public void resetToDefaultLayout() {
		clearSavedLayout();
		// Deploy first so dockables have locations, then ensure visibility.
		applyDefaultLayout();
		plansDockable.setVisible(true);
		documentsDockable.setVisible(true);
		syncViewMenuFromDockables();
		saveLayout();
		fireLayoutChanged();
	}

	public void saveLayout() {
		try {
			XElement root = new XElement("root");
			control.writeXML(root);
			StringWriter writer = new StringWriter();
			XIO.write(root, writer);
			Database.saveDockingLayout(writer.toString());
		} catch (Exception ex) {
			System.out.println("DockingLayout.saveLayout()");
			ex.printStackTrace();
		}
	}

	private boolean restoreLayout() {
		String xml = Database.loadDockingLayout();
		if (xml == null || xml.isBlank()) {
			return false;
		}
		try {
			XElement root = XIO.read(new StringReader(xml));
			control.readXML(root);
			if (!documentsDockable.isVisible() || control.getSingleDockable(ID_PLANS) == null) {
				clearSavedLayout();
				return false;
			}
			return true;
		} catch (Exception ex) {
			System.out.println("DockingLayout.restoreLayout(): using default layout");
			ex.printStackTrace();
			clearSavedLayout();
			return false;
		}
	}

	private void clearSavedLayout() {
		try {
			Database.saveDockingLayout(null);
		} catch (Exception ignored) {
			// best-effort
		}
	}

	private void applyDefaultLayout() {
		CGrid grid = new CGrid(control);
		grid.add(0, 0, 1, 3, plansDockable);
		grid.add(1, 0, 4, 3, documentsDockable);
		control.getContentArea().deploy(grid);
	}

	private DefaultSingleCDockable createToolDockable(String id, String title, Component content) {
		DefaultSingleCDockable dockable = new DefaultSingleCDockable(id, title, content);
		dockable.setCloseable(true);
		dockable.setExternalizable(true);
		dockable.setMinimizable(false);
		dockable.setMaximizable(true);
		return dockable;
	}

	private void installShortActionTexts() {
		TextManager texts = control.getController().getTexts();
		putText(texts, "externalize.in", "Unpin");
		putText(texts, "externalize.in.tooltip", "Unpin");
		putText(texts, "externalize.out", "Pin");
		putText(texts, "externalize.out.tooltip", "Pin");
		putText(texts, "maximize.in", "Maximize");
		putText(texts, "maximize.in.tooltip", "Maximize");
		putText(texts, "maximize.out", "Restore");
		putText(texts, "maximize.out.tooltip", "Restore");
		putText(texts, "normalize.out", "Restore");
		putText(texts, "normalize.out.tooltip", "Restore");
		putText(texts, "maximize_externalized.out", "Restore");
		putText(texts, "maximize_externalized.out.tooltip", "Restore");
		putText(texts, "split.maximize", "Maximize");
		putText(texts, "split.maximize.tooltip", "Maximize");
		putText(texts, "split.normalize", "Restore");
		putText(texts, "split.normalize.tooltip", "Restore");
		putText(texts, "screen.maximize", "Maximize");
		putText(texts, "screen.maximize.tooltip", "Maximize");
		putText(texts, "screen.normalize", "Restore");
		putText(texts, "screen.normalize.tooltip", "Restore");
		putText(texts, "close", "Close");
		putText(texts, "close.tooltip", "Close");
	}

	private static void putText(TextManager texts, String key, String value) {
		texts.put(Priority.CLIENT, key, value);
	}

	private void syncViewMenuFromDockables() {
		if (plansMenuItem == null) {
			return;
		}
		Runnable sync = () -> plansMenuItem.setSelected(plansDockable.isVisible());
		if (SwingUtilities.isEventDispatchThread()) {
			sync.run();
		} else {
			SwingUtilities.invokeLater(sync);
		}
	}
}
