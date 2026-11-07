package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.util.TextManager;

/**
 * Nested docking host for a single Plan Diagram tab: Graph, Zoom, Properties, and
 * Sliders can be rearranged only inside this panel (no float out to the desktop).
 */
public class DiagramDockingPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final CControl control;
	private final PlanDiagramToolbar toolbar;
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

	public DiagramDockingPanel(PlanGraphPanel graphPanel, PlanDiagramToolbar toolbar, int wheelUnitIncrement) {
		super(new BorderLayout(0, 4));
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		this.toolbar = toolbar;

		if (toolbar != null) {
			if (toolbar.getParent() != null) {
				toolbar.getParent().remove(toolbar);
			}
			add(toolbar, BorderLayout.NORTH);
		}

		// Detached control: dockables stay inside this tab and cannot join the main shell.
		control = new CControl();
		SsmsDockingGuides.install(control);
		installShortActionTexts(control);

		JScrollPane graphScroll = new JScrollPane(graphPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		graphScroll.setPreferredSize(new Dimension(320, 480));
		graphScroll.getVerticalScrollBar().setUnitIncrement(wheelUnitIncrement);
		graphScroll.getHorizontalScrollBar().setUnitIncrement(wheelUnitIncrement);

		OperatorDetailsPanel properties = new OperatorDetailsPanel(graphPanel);
		properties.setPreferredSize(new Dimension(420, 480));

		SliderPanel sliders = new SliderPanel(graphPanel);
		JScrollPane slidersScroll = new JScrollPane(sliders,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		// Short dock: radio row + thumb label + slider (~140px) without empty band.
		slidersScroll.setMinimumSize(new Dimension(320, 140));
		slidersScroll.setPreferredSize(new Dimension(320, 148));
		slidersScroll.getVerticalScrollBar().setUnitIncrement(wheelUnitIncrement);

		String suffix = Integer.toHexString(System.identityHashCode(graphPanel));
		graphDockable = createInnerDockable("diagram-graph-" + suffix, "Graph", graphScroll);
		graphDockable.setCloseable(false);

		DefaultSingleCDockable createdZoom = null;
		if (toolbar != null) {
			JPanel zoomSide = toolbar.getZoomSliderSidePanel();
			if (zoomSide.getParent() != null) {
				zoomSide.getParent().remove(zoomSide);
			}
			zoomSide.setVisible(true);
			createdZoom = createInnerDockable("diagram-zoom-" + suffix, "Zoom", zoomSide);
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
				}
			});
		}
		this.zoomDockable = createdZoom;

		propertiesDockable = createInnerDockable("diagram-properties-" + suffix, "Properties", properties);
		slidersDockable = createInnerDockable("diagram-sliders-" + suffix, "Sliders", slidersScroll);
		propertiesDockable.addCDockableStateListener(visibilitySyncListener(true));
		slidersDockable.addCDockableStateListener(visibilitySyncListener(false));

		control.addDockable(graphDockable);
		if (zoomDockable != null) {
			control.addDockable(zoomDockable);
		}
		control.addDockable(propertiesDockable);
		control.addDockable(slidersDockable);

		wantZoom = true;
		wantProperties = true;
		wantSliders = true;
		redeployDefaultLayout();

		add(control.getContentArea(), BorderLayout.CENTER);
	}

	/**
	 * Fresh-install diagram layout: Graph, Zoom strip, Properties, Sliders — all visible.
	 */
	public void resetToDefaultLayout() {
		wantZoom = true;
		wantProperties = true;
		wantSliders = true;
		redeployDefaultLayout();
		fireViewMenuSync();
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
		fireViewMenuSync();
	}

	public boolean isSlidersVisible() {
		return wantSliders;
	}

	public void setSlidersVisible(boolean visible) {
		wantSliders = visible;
		redeployDefaultLayout();
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
		redeployDefaultLayout();
		fireViewMenuSync();
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
