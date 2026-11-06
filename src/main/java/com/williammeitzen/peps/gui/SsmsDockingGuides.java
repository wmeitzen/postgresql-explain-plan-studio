package com.williammeitzen.peps.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import bibliothek.gui.DockStation;
import bibliothek.gui.DockTheme;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.control.DockRelocator;
import bibliothek.gui.dock.control.relocator.Inserter;
import bibliothek.gui.dock.control.relocator.InserterSource;
import bibliothek.gui.dock.control.relocator.MultiInserter;
import bibliothek.gui.dock.control.relocator.VetoableDockRelocatorAdapter;
import bibliothek.gui.dock.station.NoStationDropOperation;
import bibliothek.gui.dock.station.StationDropItem;
import bibliothek.gui.dock.station.StationDropOperation;
import bibliothek.gui.dock.station.support.CombinerTarget;
import bibliothek.gui.dock.displayer.DisplayerCombinerTarget;

/**
 * Installs SSMS-style vector docking cues and makes each cue an interactive drop
 * target via a Docking Frames {@link Inserter}.
 */
public final class SsmsDockingGuides {

	private SsmsDockingGuides() {
	}

	public static void install(CControl control) {
		if (control == null) {
			return;
		}
		control.putProperty(DockTheme.STATION_PAINT, new SsmsStationPaint());

		DockRelocator relocator = control.getController().getRelocator();
		installInserter(relocator, new CueInserter());
		relocator.addVetoableDockRelocatorListener(new VetoableDockRelocatorAdapter() {
			@Override
			public void dropped(bibliothek.gui.dock.control.relocator.DockRelocatorEvent event) {
				CueOverlay.dismiss();
				SsmsDockingGuideState.clear();
			}

			@Override
			public void canceled(bibliothek.gui.dock.control.relocator.DockRelocatorEvent event) {
				CueOverlay.dismiss();
				SsmsDockingGuideState.clear();
			}
		});
	}

	private static void installInserter(DockRelocator relocator, Inserter cueInserter) {
		Inserter existing = relocator.getInserter();
		MultiInserter multi = new MultiInserter();
		multi.add(cueInserter);
		if (existing instanceof MultiInserter existingMulti) {
			for (int i = 0; i < existingMulti.size(); i++) {
				multi.add(existingMulti.get(i));
			}
		} else if (existing != null) {
			multi.add(existing);
		}
		relocator.setInserter(multi);
	}

	/**
	 * If the pointer is over a cue, prepare the drop as that side/center.
	 * Same-side / no-op drops normally paint nothing in Docking Frames; those are
	 * wrapped so the cue cross stays visible.
	 */
	private static final class CueInserter implements Inserter {
		@Override
		public StationDropOperation before(InserterSource source) {
			return redirectIfOverCue(source);
		}

		@Override
		public StationDropOperation after(InserterSource source) {
			return redirectIfOverCue(source);
		}

		private static StationDropOperation redirectIfOverCue(InserterSource source) {
			DockStation station = source.getParent();
			StationDropItem item = source.getItem();
			if (station == null || item == null) {
				SsmsDockingGuideState.setActiveCue(null);
				return null;
			}

			Component hoverComp = SsmsDockingGuideState.stationComponent(station);
			SsmsDockingGuideState.GuideLayout guide = SsmsDockingGuideState.layoutOnScreen(hoverComp);
			if (guide == null) {
				SsmsDockingGuideState.setActiveCue(null);
				return null;
			}

			SsmsDockingCues.Direction cue = guide.hit(item.getMouseX(), item.getMouseY());
			SsmsDockingGuideState.setActiveCue(cue);
			if (cue == null) {
				CueOverlay.dismiss();
				return null;
			}

			DockStation dropStation = SsmsDockingGuideState.resolveDropStation(station, cue);
			Component dropComp = SsmsDockingGuideState.stationComponent(dropStation);
			if (dropStation == null || dropComp == null || !dropComp.isShowing()) {
				return null;
			}

			Point edge = SsmsDockingGuideState.edgeMouseOnScreen(dropComp, cue);
			StationDropItem redirected = new StationDropItem(
					edge.x,
					edge.y,
					item.getTitleX(),
					item.getTitleY(),
					item.getDockable(),
					item.getOriginalSize(),
					item.getMinimumSize());

			StationDropOperation prepared;
			try {
				prepared = dropStation.prepareDrop(redirected);
			} catch (RuntimeException ex) {
				prepared = null;
			}

			// Always wrap cue redirects. Same-side / no-effect drops paint nothing in DF;
			// the wrapper keeps the cue overlay visible in those cases too.
			return new CueFeedbackOperation(dropStation, item.getDockable(), dropComp, cue, prepared);
		}
	}

	/**
	 * Delegates real drop behavior when available, and always keeps the cue overlay
	 * painted — including when Docking Frames would paint nothing (already there).
	 */
	private static final class CueFeedbackOperation implements StationDropOperation {
		private final DockStation target;
		private final Dockable item;
		private final Component stationComp;
		private final SsmsDockingCues.Direction cue;
		private final StationDropOperation delegate;

		CueFeedbackOperation(DockStation target, Dockable item, Component stationComp,
				SsmsDockingCues.Direction cue, StationDropOperation delegate) {
			this.target = target;
			this.item = item;
			this.stationComp = stationComp;
			this.cue = cue;
			this.delegate = delegate instanceof NoStationDropOperation ? null : delegate;
		}

		@Override
		public void draw() {
			if (delegate != null) {
				delegate.draw();
			}
			CueOverlay.show(stationComp, cue);
		}

		@Override
		public void destroy(StationDropOperation next) {
			if (delegate != null) {
				StationDropOperation pass = next;
				if (next instanceof CueFeedbackOperation feedback) {
					pass = feedback.delegate;
				}
				delegate.destroy(pass);
			}
			if (!(next instanceof CueFeedbackOperation)) {
				CueOverlay.dismiss();
			}
		}

		@Override
		public boolean isMove() {
			return delegate == null || delegate.isMove();
		}

		@Override
		public void execute() {
			if (delegate != null) {
				delegate.execute();
			}
			// Same-side cue: already docked there — no-op.
		}

		@Override
		public DockStation getTarget() {
			return target;
		}

		@Override
		public Dockable getItem() {
			return item;
		}

		@Override
		public CombinerTarget getCombination() {
			return delegate != null ? delegate.getCombination() : null;
		}

		@Override
		public DisplayerCombinerTarget getDisplayerCombination() {
			return delegate != null ? delegate.getDisplayerCombination() : null;
		}
	}

	/**
	 * Click-through layered overlay so cues remain visible when DF skips insertion paint.
	 * {@link #contains(int, int)} returns false so mouse events reach the docking relocator.
	 */
	private static final class CueOverlay extends JComponent {
		private static final long serialVersionUID = 1L;
		private static CueOverlay instance;

		private SsmsDockingCues.Direction active = SsmsDockingCues.Direction.CENTER;

		private CueOverlay() {
			setOpaque(false);
		}

		static void show(Component stationComp, SsmsDockingCues.Direction cue) {
			if (stationComp == null || !stationComp.isShowing()) {
				dismiss();
				return;
			}
			RootPaneContainer root = rootOf(stationComp);
			if (root == null) {
				return;
			}
			JLayeredPane layered = root.getLayeredPane();
			if (instance == null) {
				instance = new CueOverlay();
			}
			instance.active = cue;

			Point origin = SwingUtilities.convertPoint(stationComp, 0, 0, layered);
			Rectangle bounds = new Rectangle(origin.x, origin.y, stationComp.getWidth(), stationComp.getHeight());
			if (instance.getParent() != layered) {
				Container old = instance.getParent();
				if (old != null) {
					old.remove(instance);
				}
				layered.add(instance, Integer.valueOf(JLayeredPane.DRAG_LAYER));
			}
			instance.setBounds(bounds);
			instance.setVisible(true);
			instance.repaint();
		}

		static void dismiss() {
			if (instance == null) {
				return;
			}
			Container parent = instance.getParent();
			if (parent != null) {
				parent.remove(instance);
				parent.revalidate();
				parent.repaint();
			}
			instance.setVisible(false);
		}

		private static RootPaneContainer rootOf(Component c) {
			Component w = SwingUtilities.getWindowAncestor(c);
			if (w instanceof RootPaneContainer rpc) {
				return rpc;
			}
			return null;
		}

		@Override
		public boolean contains(int x, int y) {
			// Pass mouse through to Docking Frames under the overlay.
			return false;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				SsmsDockingGuideState.GuideLayout local = SsmsDockingGuideState.layoutIn(
						new Rectangle(0, 0, getWidth(), getHeight()));
				if (local == null) {
					return;
				}
				Rectangle preview = previewBounds(getWidth(), getHeight(), active);
				SsmsDockingCues.paintWindowPreview(g2, preview);
				SsmsDockingCues.paintGuideCross(g2, local.x, local.y, local.cell, active,
						SsmsDockingGuideState.allDirections());
			} finally {
				g2.dispose();
			}
		}

		private static Rectangle previewBounds(int w, int h, SsmsDockingCues.Direction cue) {
			int halfW = Math.max(1, w / 2);
			int halfH = Math.max(1, h / 2);
			return switch (cue) {
				case LEFT -> new Rectangle(0, 0, halfW, h);
				case RIGHT -> new Rectangle(w - halfW, 0, halfW, h);
				case TOP -> new Rectangle(0, 0, w, halfH);
				case BOTTOM -> new Rectangle(0, h - halfH, w, halfH);
				case CENTER -> new Rectangle(w / 6, h / 6, Math.max(1, w * 2 / 3), Math.max(1, h * 2 / 3));
			};
		}
	}
}
