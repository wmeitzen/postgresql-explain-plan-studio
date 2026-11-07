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

	public DiagramDockingPanel(PlanGraphPanel graphPanel, PlanDiagramToolbar toolbar, int wheelUnitIncrement) {
		super(new BorderLayout(0, 4));
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

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
		slidersScroll.setMinimumSize(new Dimension(320, 120));
		slidersScroll.getVerticalScrollBar().setUnitIncrement(wheelUnitIncrement);

		String suffix = Integer.toHexString(System.identityHashCode(graphPanel));
		DefaultSingleCDockable graphDockable = createInnerDockable("diagram-graph-" + suffix, "Graph", graphScroll);
		graphDockable.setCloseable(false);

		DefaultSingleCDockable zoomDockable = null;
		if (toolbar != null) {
			JPanel zoomSide = toolbar.getZoomSliderSidePanel();
			if (zoomSide.getParent() != null) {
				zoomSide.getParent().remove(zoomSide);
			}
			zoomSide.setVisible(true);
			zoomDockable = createInnerDockable("diagram-zoom-" + suffix, "Zoom", zoomSide);
			zoomDockable.setCloseable(true);
			zoomDockable.setMaximizable(false);
			toolbar.refreshZoomSliderFromGraph(graphPanel);

			DefaultSingleCDockable zoomRef = zoomDockable;
			toolbar.setZoomSliderVisibilityListener(show -> {
				zoomRef.setVisible(Boolean.TRUE.equals(show));
				if (Boolean.TRUE.equals(show)) {
					toolbar.refreshZoomSliderFromGraph(graphPanel);
				}
			});
			zoomDockable.addCDockableStateListener(new CDockableAdapter() {
				@Override
				public void visibilityChanged(CDockable dockable) {
					if (toolbar.isZoomSliderCheckboxEventSuppressed()) {
						return;
					}
					boolean visible = dockable.isVisible();
					toolbar.setZoomSliderCheckboxSelected(visible);
					if (visible) {
						toolbar.refreshZoomSliderFromGraph(graphPanel);
					}
				}
			});
		}

		DefaultSingleCDockable propertiesDockable = createInnerDockable("diagram-properties-" + suffix, "Properties", properties);
		DefaultSingleCDockable slidersDockable = createInnerDockable("diagram-sliders-" + suffix, "Sliders", slidersScroll);

		control.addDockable(graphDockable);
		if (zoomDockable != null) {
			control.addDockable(zoomDockable);
		}
		control.addDockable(propertiesDockable);
		control.addDockable(slidersDockable);

		CGrid grid = new CGrid(control);
		grid.add(0, 0, 3, 2, graphDockable);
		if (zoomDockable != null) {
			// Narrow strip between Graph and Properties (title bar matches other panes).
			grid.add(3, 0, 0.45, 2, zoomDockable);
			grid.add(3.45, 0, 1.5, 3, propertiesDockable);
		} else {
			grid.add(3, 0, 1.5, 3, propertiesDockable);
		}
		grid.add(0, 2, 3, 1, slidersDockable);
		control.getContentArea().deploy(grid);

		// setVisible requires the dockable to already be registered with CControl.
		if (zoomDockable != null && toolbar != null) {
			zoomDockable.setVisible(toolbar.isZoomSliderVisible());
		}

		add(control.getContentArea(), BorderLayout.CENTER);
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
