package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelListener;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.williammeitzen.peps.model.Database;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.event.CDockableLocationEvent;
import bibliothek.gui.dock.common.event.CDockableLocationListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.util.TextManager;
import bibliothek.util.xml.XElement;
import bibliothek.util.xml.XIO;

/**
 * Nested docking host for a single Plan Diagram tab: Graph, Zoom, Properties, and
 * Sliders can be rearranged only inside this panel (no float out to the desktop).
 * Layout XML is shared via {@code config.diagram_docking_layout}.
 */
public class DiagramDockingPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Stable IDs so layout XML restores across sessions and new plan tabs. */
	private static final String ID_GRAPH = "diagram-graph";
	private static final String ID_ZOOM = "diagram-zoom";
	private static final String ID_PROPERTIES = "diagram-properties";
	private static final String ID_SLIDERS = "diagram-sliders";

	private final CControl control;
	private final PlanDiagramToolbar toolbar;
	private final PlanGraphPanel graphPanel;
	private final DiagramFindBar findBar;
	private final DefaultSingleCDockable graphDockable;
	private final DefaultSingleCDockable propertiesDockable;
	private final DefaultSingleCDockable slidersDockable;
	private final DefaultSingleCDockable zoomDockable;
	private Runnable viewMenuSyncHook;

	/** Intended visibility — Docking Frames isVisible/isDockableVisible can desync after shell/theme changes. */
	private boolean wantZoom = true;
	private boolean wantProperties = true;
	private boolean wantSliders = true;
	private boolean suppressVisibilityTracking;
	private boolean suppressLayoutSave;
	private boolean layoutSaveScheduled;

	public DiagramDockingPanel(PlanGraphPanel graphPanel, PlanDiagramToolbar toolbar, int wheelUnitIncrement) {
		super(new BorderLayout(0, 4));
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		this.toolbar = toolbar;
		this.graphPanel = graphPanel;

		JPanel northStack = new JPanel();
		northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
		northStack.setOpaque(false);
		if (toolbar != null) {
			if (toolbar.getParent() != null) {
				toolbar.getParent().remove(toolbar);
			}
			toolbar.setAlignmentX(LEFT_ALIGNMENT);
			northStack.add(toolbar);
		}
		findBar = new DiagramFindBar();
		findBar.setGraphPanel(graphPanel);
		findBar.setOnCloseRequest(v -> hideFind());
		findBar.setAlignmentX(LEFT_ALIGNMENT);
		// Cap height so BoxLayout does not stretch the strip; width can grow.
		findBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		northStack.add(findBar);
		add(northStack, BorderLayout.NORTH);
		// Find checkbox lives on the shared toolbar; FormPanel routes toggles to the
		// active diagram. Do not overwrite that listener here (last-created panel won).
		installFindShortcuts();

		// Detached control: dockables stay inside this tab and cannot join the main shell.
		control = new CControl();
		SsmsDockingGuides.install(control);
		installShortActionTexts(control);

		JScrollPane graphScroll = new JScrollPane(graphPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		graphScroll.setPreferredSize(new Dimension(320, 480));
		graphScroll.getVerticalScrollBar().setUnitIncrement(wheelUnitIncrement);
		graphScroll.getHorizontalScrollBar().setUnitIncrement(wheelUnitIncrement);
		// Ctrl+wheel (Cmd+wheel on macOS) zooms the diagram; plain wheel still scrolls.
		MouseWheelListener ctrlZoom = e -> {
			if (!e.isControlDown() && !e.isMetaDown()) {
				return;
			}
			int notches = e.getWheelRotation();
			if (notches == 0) {
				return;
			}
			e.consume();
			// Wheel down / toward user → zoom out (same ±10 steps as toolbar +/-).
			int next = graphPanel.getZoomPercentage() + (-notches * 10);
			graphPanel.setZoomPercentage(next);
			graphPanel.refreshScrollableSizeAfterZoom(true);
			if (toolbar != null) {
				toolbar.syncZoomPercentage(graphPanel.getZoomPercentage());
			}
		};
		graphScroll.addMouseWheelListener(ctrlZoom);
		graphPanel.addMouseWheelListener(ctrlZoom);

		OperatorDetailsPanel properties = new OperatorDetailsPanel(graphPanel);
		properties.setPreferredSize(new Dimension(420, 480));

		SliderPanel sliders = new SliderPanel(graphPanel);
		JScrollPane slidersScroll = new JScrollPane(sliders,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		// Bottom dock: short strip. Side dock: keep wide enough for vertical thumb + tick labels.
		slidersScroll.setMinimumSize(new Dimension(SliderPanel.PANEL_MINIMUM_WIDTH, 140));
		slidersScroll.setPreferredSize(new Dimension(320, 148));
		slidersScroll.getVerticalScrollBar().setUnitIncrement(wheelUnitIncrement);

		graphDockable = createInnerDockable(ID_GRAPH, "Plan Diagram", graphScroll);
		graphDockable.setCloseable(false);

		DefaultSingleCDockable createdZoom = null;
		if (toolbar != null) {
			JPanel zoomSide = toolbar.getZoomSliderSidePanel();
			if (zoomSide.getParent() != null) {
				zoomSide.getParent().remove(zoomSide);
			}
			zoomSide.setVisible(true);
			createdZoom = createInnerDockable(ID_ZOOM, "Zoom", zoomSide);
			createdZoom.setCloseable(true);
			createdZoom.setMaximizable(false);
			toolbar.refreshZoomSliderFromGraph(graphPanel);

			toolbar.setZoomSliderVisibilityListener(show -> {
				setZoomVisible(Boolean.TRUE.equals(show));
				if (Boolean.TRUE.equals(show)) {
					toolbar.refreshZoomSliderFromGraph(graphPanel);
				}
			});
			createdZoom.addCDockableStateListener(new CDockableAdapter() {
				@Override
				public void visibilityChanged(CDockable dockable) {
					if (toolbar.isZoomSliderCheckboxEventSuppressed() || suppressVisibilityTracking) {
						return;
					}
					wantZoom = dockable.isVisible();
					toolbar.setZoomSliderCheckboxSelected(wantZoom);
					if (wantZoom) {
						toolbar.refreshZoomSliderFromGraph(graphPanel);
					}
					fireViewMenuSync();
					scheduleSaveLayout();
				}
			});
		}
		this.zoomDockable = createdZoom;

		propertiesDockable = createInnerDockable(ID_PROPERTIES, "Properties", properties);
		slidersDockable = createInnerDockable(ID_SLIDERS, "Sliders", slidersScroll);
		propertiesDockable.addCDockableStateListener(visibilitySyncListener(true));
		slidersDockable.addCDockableStateListener(visibilitySyncListener(false));

		control.addDockable(graphDockable);
		if (zoomDockable != null) {
			control.addDockable(zoomDockable);
		}
		control.addDockable(propertiesDockable);
		control.addDockable(slidersDockable);

		installLayoutPersistenceListeners(graphDockable);
		installLayoutPersistenceListeners(propertiesDockable);
		installLayoutPersistenceListeners(slidersDockable);
		if (zoomDockable != null) {
			installLayoutPersistenceListeners(zoomDockable);
		}

		wantZoom = true;
		wantProperties = true;
		wantSliders = true;
		if (!restoreLayout()) {
			redeployDefaultLayout();
		} else {
			syncWantsFromDockables();
		}

		add(control.getContentArea(), BorderLayout.CENTER);
	}

	/**
	 * Fresh-install diagram layout: Graph, Zoom strip, Properties, Sliders — all visible.
	 * Clears any persisted diagram docking XML so the next launch stays reset too.
	 */
	public void resetToDefaultLayout() {
		clearSavedLayout();
		wantZoom = true;
		wantProperties = true;
		wantSliders = true;
		redeployDefaultLayout();
		saveLayout();
		fireViewMenuSync();
	}

	/** Persist current Graph / Zoom / Properties / Sliders arrangement. */
	public void saveLayout() {
		if (suppressLayoutSave) {
			return;
		}
		try {
			XElement root = new XElement("root");
			control.writeXML(root);
			StringWriter writer = new StringWriter();
			XIO.write(root, writer);
			Database.saveDiagramDockingLayout(writer.toString());
		} catch (Exception ex) {
			System.out.println("DiagramDockingPanel.saveLayout()");
			ex.printStackTrace();
		}
	}

	private boolean restoreLayout() {
		String xml = Database.loadDiagramDockingLayout();
		if (xml == null || xml.isBlank()) {
			return false;
		}
		suppressLayoutSave = true;
		suppressVisibilityTracking = true;
		try {
			XElement root = XIO.read(new StringReader(xml));
			control.readXML(root);
			if (!graphDockable.isVisible()) {
				clearSavedLayout();
				return false;
			}
			return true;
		} catch (Exception ex) {
			System.out.println("DiagramDockingPanel.restoreLayout(): using default layout");
			ex.printStackTrace();
			clearSavedLayout();
			return false;
		} finally {
			suppressVisibilityTracking = false;
			suppressLayoutSave = false;
		}
	}

	private void clearSavedLayout() {
		try {
			Database.saveDiagramDockingLayout(null);
		} catch (Exception ignored) {
			// best-effort
		}
	}

	private void syncWantsFromDockables() {
		wantProperties = propertiesDockable.isVisible();
		wantSliders = slidersDockable.isVisible();
		if (zoomDockable != null) {
			wantZoom = zoomDockable.isVisible();
			if (toolbar != null) {
				toolbar.setZoomSliderCheckboxSelected(wantZoom);
			}
		}
	}

	private void installLayoutPersistenceListeners(DefaultSingleCDockable dockable) {
		if (dockable == null) {
			return;
		}
		dockable.addCDockableLocationListener(new CDockableLocationListener() {
			@Override
			public void changed(CDockableLocationEvent event) {
				if (suppressLayoutSave || suppressVisibilityTracking) {
					return;
				}
				if (event == null) {
					return;
				}
				scheduleSaveLayout();
			}
		});
	}

	private void scheduleSaveLayout() {
		if (suppressLayoutSave || layoutSaveScheduled) {
			return;
		}
		layoutSaveScheduled = true;
		SwingUtilities.invokeLater(() -> {
			layoutSaveScheduled = false;
			if (!suppressLayoutSave) {
				saveLayout();
			}
		});
	}

	/**
	 * Redeploy Graph | Zoom | Properties / Sliders from {@link #wantZoom} /
	 * {@link #wantProperties} / {@link #wantSliders}. Closing a dockable clears Docking
	 * Frames location history; plain {@code setVisible(true)} often restores it in the
	 * wrong split. Redeploying puts each panel back in the intended strip.
	 */
	private void redeployDefaultLayout() {
		suppressVisibilityTracking = true;
		try {
			CGrid grid = new CGrid(control);
			if (zoomDockable != null) {
				grid.add(0, 0, 4.0, 5.0, graphDockable);
				grid.add(4.0, 0, 0.7, 5.0, zoomDockable);
				grid.add(4.7, 0, 2.0, 6.0, propertiesDockable);
				grid.add(0, 5.0, 4.7, 1.0, slidersDockable);
			} else {
				grid.add(0, 0, 4.0, 5.0, graphDockable);
				grid.add(4.0, 0, 2.0, 6.0, propertiesDockable);
				grid.add(0, 5.0, 4.0, 1.0, slidersDockable);
			}
			control.getContentArea().deploy(grid);

			graphDockable.setVisible(true);
			propertiesDockable.setVisible(wantProperties);
			slidersDockable.setVisible(wantSliders);
			if (zoomDockable != null) {
				zoomDockable.setVisible(wantZoom);
				if (toolbar != null) {
					toolbar.setZoomSliderCheckboxSelected(wantZoom);
				}
			}
		} finally {
			suppressVisibilityTracking = false;
		}
	}

	public void setViewMenuSyncHook(Runnable hook) {
		this.viewMenuSyncHook = hook;
	}

	public boolean isPropertiesVisible() {
		return wantProperties;
	}

	public void setPropertiesVisible(boolean visible) {
		wantProperties = visible;
		redeployDefaultLayout();
		scheduleSaveLayout();
		fireViewMenuSync();
	}

	public boolean isSlidersVisible() {
		return wantSliders;
	}

	public void setSlidersVisible(boolean visible) {
		wantSliders = visible;
		redeployDefaultLayout();
		scheduleSaveLayout();
		fireViewMenuSync();
	}

	public boolean hasZoomDockable() {
		return zoomDockable != null;
	}

	public boolean isZoomVisible() {
		return zoomDockable != null && wantZoom;
	}

	public void setZoomVisible(boolean visible) {
		if (zoomDockable == null) {
			return;
		}
		wantZoom = visible;
		redeployDefaultLayout();
		scheduleSaveLayout();
		if (toolbar != null) {
			toolbar.setZoomSliderCheckboxSelected(visible);
		}
		fireViewMenuSync();
	}

	/**
	 * After Look-and-Feel / theme swaps or shell (Plans) layout changes, Docking Frames
	 * can leave panels painted while flags are wrong. Redeploy from intended wants.
	 */
	public void repairAfterThemeChange() {
		suppressLayoutSave = true;
		try {
			if (!restoreLayout()) {
				redeployDefaultLayout();
			} else {
				syncWantsFromDockables();
			}
		} finally {
			suppressLayoutSave = false;
		}
		syncThemeColors();
		fireViewMenuSync();
	}

	/** Keep Plan Diagram / Zoom / Properties / Sliders docking titles on the active scheme. */
	public void syncThemeColors() {
		UiTheme.applyDockableColors(graphDockable.getColors());
		UiTheme.applyDockableColors(propertiesDockable.getColors());
		UiTheme.applyDockableColors(slidersDockable.getColors());
		if (zoomDockable != null) {
			UiTheme.applyDockableColors(zoomDockable.getColors());
		}
		if (findBar != null) {
			findBar.syncThemeSurfaces();
		}
		if (toolbar != null) {
			toolbar.syncThemeSurfaces();
		}
		revalidate();
		repaint();
	}

	private CDockableAdapter visibilitySyncListener(boolean propertiesNotSliders) {
		return new CDockableAdapter() {
			@Override
			public void visibilityChanged(CDockable dockable) {
				if (suppressVisibilityTracking) {
					return;
				}
				if (propertiesNotSliders) {
					wantProperties = dockable.isVisible();
				} else {
					wantSliders = dockable.isVisible();
				}
				fireViewMenuSync();
				scheduleSaveLayout();
			}
		};
	}

	private void fireViewMenuSync() {
		if (viewMenuSyncHook != null) {
			viewMenuSyncHook.run();
		}
	}

	private static DefaultSingleCDockable createInnerDockable(String id, String title, java.awt.Component content) {
		DefaultSingleCDockable dockable = new DefaultSingleCDockable(id, title, content);
		dockable.setCloseable(true);
		dockable.setExternalizable(false); // keep repositioning inside this tab
		dockable.setMinimizable(false);
		dockable.setMaximizable(true);
		return dockable;
	}

	private static void installShortActionTexts(CControl control) {
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
		putText(texts, "split.maximize", "Maximize");
		putText(texts, "split.maximize.tooltip", "Maximize");
		putText(texts, "split.normalize", "Restore");
		putText(texts, "split.normalize.tooltip", "Restore");
		putText(texts, "close", "Close");
		putText(texts, "close.tooltip", "Close");
	}

	private static void putText(TextManager texts, String key, String value) {
		texts.put(Priority.CLIENT, key, value);
	}

	/** Show Find bar and focus the query field (Ctrl/Cmd+F or Find checkbox). */
	public void showFind() {
		if (toolbar != null) {
			toolbar.setFindCheckboxSelected(true);
		}
		findBar.openAndFocus();
		revalidate();
		repaint();
	}

	public void hideFind() {
		if (toolbar != null) {
			toolbar.setFindCheckboxSelected(false);
		}
		findBar.closeBar();
		revalidate();
		repaint();
		if (graphPanel != null) {
			graphPanel.requestFocusInWindow();
		}
	}

	/**
	 * Match this tab's Find strip to the shared toolbar checkbox. Needed when
	 * switching plans while Find is already checked — each diagram has its own bar.
	 */
	public void syncFindBarFromToolbar() {
		if (toolbar == null || findBar == null) {
			return;
		}
		boolean wantOpen = toolbar.isFindCheckboxSelected();
		if (wantOpen) {
			if (!findBar.isOpen()) {
				findBar.openAndFocus();
				revalidate();
				repaint();
			}
		} else if (findBar.isOpen()) {
			findBar.closeBar();
			revalidate();
			repaint();
		}
	}

	public DiagramFindBar getFindBar() {
		return findBar;
	}

	@Override
	public void addNotify() {
		super.addNotify();
		SwingUtilities.invokeLater(this::syncFindBarFromToolbar);
	}

	/**
	 * Ctrl/Cmd+F opens Find when this diagram tab is showing.
	 * Esc / F3 / Shift+F3 also work while Find is open even if focus is on the graph.
	 */
	private void installFindShortcuts() {
		int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask), "diagramFind");
		if (menuMask != KeyEvent.CTRL_DOWN_MASK) {
			getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
					KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "diagramFind");
		}
		getActionMap().put("diagramFind", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isShowing()) {
					return;
				}
				showFind();
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "diagramFindClose");
		getActionMap().put("diagramFindClose", new AbstractAction() {
			@Override
			public boolean isEnabled() {
				return isShowing() && findBar.isOpen();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				hideFind();
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "diagramFindNext");
		getActionMap().put("diagramFindNext", new AbstractAction() {
			@Override
			public boolean isEnabled() {
				return isShowing() && findBar.isOpen();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				findBar.findNext();
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK), "diagramFindPrev");
		getActionMap().put("diagramFindPrev", new AbstractAction() {
			@Override
			public boolean isEnabled() {
				return isShowing() && findBar.isOpen();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				findBar.findPrevious();
			}
		});
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		try {
			control.destroy();
		} catch (RuntimeException ignored) {
			// tab tear-down best-effort
		}
	}
}
