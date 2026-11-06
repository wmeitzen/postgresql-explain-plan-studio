package com.williammeitzen.peps.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.SplitDockStation;

/**
 * Shared layout/hit-test helpers for SSMS docking guides.
 * Guide geometry is always computed from the station component's on-screen
 * bounds so paint and hit-testing stay aligned (no dependency on paint order).
 */
final class SsmsDockingGuideState {

	private static final SsmsDockingCues.Direction[] ALL = {
			SsmsDockingCues.Direction.LEFT,
			SsmsDockingCues.Direction.RIGHT,
			SsmsDockingCues.Direction.TOP,
			SsmsDockingCues.Direction.BOTTOM,
			SsmsDockingCues.Direction.CENTER
	};

	private static volatile SsmsDockingCues.Direction activeCue;

	private SsmsDockingGuideState() {
	}

	static SsmsDockingCues.Direction[] allDirections() {
		return ALL;
	}

	static void clear() {
		activeCue = null;
	}

	static void setActiveCue(SsmsDockingCues.Direction direction) {
		activeCue = direction;
	}

	static SsmsDockingCues.Direction getActiveCue() {
		return activeCue;
	}

	static Component stationComponent(DockStation station) {
		if (station == null) {
			return null;
		}
		if (station instanceof Component) {
			return (Component) station;
		}
		Dockable asDockable = station.asDockable();
		return asDockable != null ? asDockable.getComponent() : null;
	}

	/**
	 * Side drops need a {@link SplitDockStation}. If the current target is a
	 * stack (tabs), walk up to the enclosing split.
	 */
	static DockStation resolveDropStation(DockStation station, SsmsDockingCues.Direction cue) {
		if (station == null) {
			return null;
		}
		if (cue == SsmsDockingCues.Direction.CENTER) {
			return station;
		}
		DockStation current = station;
		for (int i = 0; i < 8 && current != null; i++) {
			if (current instanceof SplitDockStation) {
				return current;
			}
			Dockable asDockable = current.asDockable();
			if (asDockable == null) {
				break;
			}
			current = asDockable.getDockParent();
		}
		return station;
	}

	static GuideLayout layoutOnScreen(Component station) {
		if (station == null || !station.isShowing()) {
			return null;
		}
		Point origin = station.getLocationOnScreen();
		return layoutIn(new Rectangle(origin.x, origin.y, station.getWidth(), station.getHeight()));
	}

	static GuideLayout layoutInComponent(Component station) {
		if (station == null) {
			return null;
		}
		return layoutIn(new Rectangle(0, 0, station.getWidth(), station.getHeight()));
	}

	static GuideLayout layoutIn(Rectangle stationBounds) {
		if (stationBounds == null || stationBounds.width < 80 || stationBounds.height < 80) {
			return null;
		}
		int cell = Math.max(34, Math.min(48, Math.min(stationBounds.width, stationBounds.height) / 9));
		int guide = SsmsDockingCues.guidePixelSize(cell);
		if (guide + 16 >= stationBounds.width || guide + 16 >= stationBounds.height) {
			return null;
		}
		int x = stationBounds.x + (stationBounds.width - guide) / 2;
		int y = stationBounds.y + (stationBounds.height - guide) / 2;
		return new GuideLayout(x, y, guide, cell);
	}

	/**
	 * Screen point firmly inside the chosen edge/center drop zone of {@code station}.
	 */
	static Point edgeMouseOnScreen(Component station, SsmsDockingCues.Direction direction) {
		Point origin = station.getLocationOnScreen();
		int w = Math.max(1, station.getWidth());
		int h = Math.max(1, station.getHeight());
		// Stay close to the border so SplitDockStation's side zones win over center/combine.
		int m = Math.max(4, Math.min(12, Math.min(w, h) / 40));
		return switch (direction) {
			case LEFT -> new Point(origin.x + m, origin.y + h / 2);
			case RIGHT -> new Point(origin.x + w - 1 - m, origin.y + h / 2);
			case TOP -> new Point(origin.x + w / 2, origin.y + m);
			case BOTTOM -> new Point(origin.x + w / 2, origin.y + h - 1 - m);
			case CENTER -> new Point(origin.x + w / 2, origin.y + h / 2);
		};
	}

	static final class GuideLayout {
		final int x;
		final int y;
		final int size;
		final int cell;

		GuideLayout(int x, int y, int size, int cell) {
			this.x = x;
			this.y = y;
			this.size = size;
			this.cell = cell;
		}

		SsmsDockingCues.Direction hit(int px, int py) {
			if (px < x || py < y || px >= x + size || py >= y + size) {
				return null;
			}
			return SsmsDockingCues.hitTestGuide(px - x, py - y, cell, ALL);
		}
	}
}
