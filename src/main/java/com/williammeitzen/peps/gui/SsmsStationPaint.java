package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import bibliothek.gui.DockStation;
import bibliothek.gui.dock.station.StationPaint;

/**
 * SSMS-inspired docking visuals. The cue cross is always centered on the
 * station component so hit-testing in {@link SsmsDockingGuides} matches paint.
 */
public class SsmsStationPaint implements StationPaint {

	private static final Color LINE = new Color(0x3A, 0x8A, 0xD0, 230);
	private static final Color DIVIDER = new Color(0x6C, 0xB6, 0xF0, 120);

	@Override
	public void drawInsertion(Graphics g, DockStation station, Rectangle stationBounds, Rectangle dockableBounds) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			SsmsDockingCues.Direction active = SsmsDockingGuideState.getActiveCue();
			if (active == null) {
				active = SsmsDockingCues.inferDirection(stationBounds, dockableBounds);
			}

			SsmsDockingCues.paintWindowPreview(g2, dockableBounds);

			// When a cue is the drop target, SsmsDockingGuides paints the cross via a
			// click-through overlay (DF skips insertion paint for same-side / no-op drops).
			if (SsmsDockingGuideState.getActiveCue() != null) {
				return;
			}

			Component comp = SsmsDockingGuideState.stationComponent(station);
			SsmsDockingGuideState.GuideLayout local = SsmsDockingGuideState.layoutInComponent(comp);
			if (local == null) {
				local = SsmsDockingGuideState.layoutIn(stationBounds);
			}
			if (local != null) {
				SsmsDockingCues.paintGuideCross(g2, local.x, local.y, local.cell, active,
						SsmsDockingGuideState.allDirections());
			}
		} finally {
			g2.dispose();
		}
	}

	@Override
	public void drawInsertionLine(Graphics g, DockStation station, int x1, int y1, int x2, int y2) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			SsmsDockingCues.enableQuality(g2);
			g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(LINE);
			g2.drawLine(x1, y1, x2, y2);
		} finally {
			g2.dispose();
		}
	}

	@Override
	public void drawDivider(Graphics g, DockStation station, Rectangle bounds) {
		if (bounds == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setColor(DIVIDER);
			g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		} finally {
			g2.dispose();
		}
	}

	@Override
	public void drawRemoval(Graphics g, DockStation station, Rectangle stationBounds, Rectangle dockableBounds) {
		if (dockableBounds == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setColor(new Color(0xC0, 0x40, 0x40, 60));
			g2.fillRect(dockableBounds.x, dockableBounds.y, dockableBounds.width, dockableBounds.height);
		} finally {
			g2.dispose();
		}
	}
}
