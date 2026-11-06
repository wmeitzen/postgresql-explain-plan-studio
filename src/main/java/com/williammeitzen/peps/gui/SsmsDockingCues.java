package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Vector-drawn SSMS-style docking cues (no bitmaps). Scales cleanly with DPI.
 */
public final class SsmsDockingCues {

	public enum Direction {
		LEFT, RIGHT, TOP, BOTTOM, CENTER
	}

	/** Dark chrome similar to SSMS docking guides. */
	private static final Color CUE_BG = new Color(0x3A, 0x3A, 0x3A);
	private static final Color CUE_BG_ACTIVE = new Color(0x4A, 0x5A, 0x6A);
	private static final Color CUE_BORDER = new Color(0xC0, 0xC0, 0xC0);
	private static final Color CUE_BORDER_ACTIVE = new Color(0x6C, 0xB6, 0xF0);
	private static final Color CUE_FILL = new Color(0xB8, 0xB8, 0xB8);
	private static final Color CUE_OUTLINE = new Color(0xE0, 0xE0, 0xE0);
	private static final Color PREVIEW_FILL = new Color(0x6C, 0xB6, 0xF0, 70);
	private static final Color PREVIEW_TITLE = new Color(0x6C, 0xB6, 0xF0, 200);
	private static final Color PREVIEW_BORDER = new Color(0x3A, 0x8A, 0xD0, 220);
	private static final Color GUIDE_BACK = new Color(0x2A, 0x2A, 0x2A, 180);

	private SsmsDockingCues() {
	}

	public static void enableQuality(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	/**
	 * Drop-zone preview: translucent panel with a thick title bar (SSMS “window” metaphor).
	 */
	public static void paintWindowPreview(Graphics2D g, Rectangle bounds) {
		if (bounds == null || bounds.width < 8 || bounds.height < 8) {
			return;
		}
		enableQuality(g);
		int x = bounds.x + 1;
		int y = bounds.y + 1;
		int w = Math.max(4, bounds.width - 2);
		int h = Math.max(4, bounds.height - 2);
		int arc = Math.min(8, Math.min(w, h) / 6);
		int title = Math.max(6, Math.min(22, h / 8));

		g.setColor(PREVIEW_FILL);
		g.fillRoundRect(x, y, w, h, arc, arc);

		g.setColor(PREVIEW_TITLE);
		g.fillRoundRect(x, y, w, title, arc, arc);
		g.fillRect(x, y + title / 2, w, title - title / 2);

		g.setStroke(new BasicStroke(2f));
		g.setColor(PREVIEW_BORDER);
		g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
	}

	/**
	 * One docking cue cell (arrow + thick-top window, or center outline).
	 */
	public static void paintCue(Graphics2D g, int x, int y, int size, Direction direction, boolean active) {
		enableQuality(g);
		float s = size;
		RoundRectangle2D.Float outer = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, s - 1f, s - 1f, 4f, 4f);
		g.setColor(active ? CUE_BG_ACTIVE : CUE_BG);
		g.fill(outer);
		g.setStroke(new BasicStroke(active ? 2f : 1.2f));
		g.setColor(active ? CUE_BORDER_ACTIVE : CUE_BORDER);
		g.draw(outer);

		float pad = s * 0.14f;
		float inner = s - 2f * pad;
		float ix = x + pad;
		float iy = y + pad;

		switch (direction) {
			case LEFT -> paintSideCue(g, ix, iy, inner, true, active);
			case RIGHT -> paintSideCue(g, ix, iy, inner, false, active);
			case TOP -> paintVerticalCue(g, ix, iy, inner, true, active);
			case BOTTOM -> paintVerticalCue(g, ix, iy, inner, false, active);
			case CENTER -> paintCenterCue(g, ix, iy, inner, active);
		}
	}

	/**
	 * SSMS-style cross: center + N/E/S/W. Returns the hit direction for a point in local coords, or null.
	 */
	public static Direction hitTestGuide(int localX, int localY, int cell, Direction[] available) {
		int gap = Math.max(2, cell / 10);
		int total = cell * 3 + gap * 2;
		int origin = 0;
		int pad = Math.max(3, cell / 8); // slightly larger hit target than the drawn cell
		Rectangle center = inflate(cellRect(origin, total, cell, gap, 1, 1), pad);
		Rectangle top = inflate(cellRect(origin, total, cell, gap, 1, 0), pad);
		Rectangle bottom = inflate(cellRect(origin, total, cell, gap, 1, 2), pad);
		Rectangle left = inflate(cellRect(origin, total, cell, gap, 0, 1), pad);
		Rectangle right = inflate(cellRect(origin, total, cell, gap, 2, 1), pad);

		// Prefer side cues over center when the inflated regions overlap.
		Direction[] order = { Direction.TOP, Direction.BOTTOM, Direction.LEFT, Direction.RIGHT, Direction.CENTER };
		Rectangle[] rects = { top, bottom, left, right, center };
		for (int i = 0; i < order.length; i++) {
			if (!isAvailable(available, order[i])) {
				continue;
			}
			if (rects[i].contains(localX, localY)) {
				return order[i];
			}
		}
		return null;
	}

	private static Rectangle inflate(Rectangle r, int pad) {
		return new Rectangle(r.x - pad, r.y - pad, r.width + 2 * pad, r.height + 2 * pad);
	}

	public static int guidePixelSize(int cell) {
		int gap = Math.max(2, cell / 10);
		return cell * 3 + gap * 2;
	}

	public static void paintGuideCross(Graphics2D g, int x, int y, int cell, Direction active, Direction[] available) {
		enableQuality(g);
		int gap = Math.max(2, cell / 10);
		int total = guidePixelSize(cell);

		// Soft cross plate behind the five cells
		g.setColor(GUIDE_BACK);
		int arm = cell + gap;
		g.fillRoundRect(x + cell + gap, y, cell, total, 6, 6);
		g.fillRoundRect(x, y + cell + gap, total, cell, 6, 6);

		paintIfAvailable(g, available, Direction.TOP, x + cell + gap, y, cell, active);
		paintIfAvailable(g, available, Direction.LEFT, x, y + cell + gap, cell, active);
		paintIfAvailable(g, available, Direction.CENTER, x + cell + gap, y + cell + gap, cell, active);
		paintIfAvailable(g, available, Direction.RIGHT, x + 2 * (cell + gap), y + cell + gap, cell, active);
		paintIfAvailable(g, available, Direction.BOTTOM, x + cell + gap, y + 2 * (cell + gap), cell, active);
	}

	/**
	 * Infer which guide matches Docking Frames' current insertion rectangle.
	 */
	public static Direction inferDirection(Rectangle stationBounds, Rectangle dockableBounds) {
		if (stationBounds == null || dockableBounds == null || stationBounds.isEmpty() || dockableBounds.isEmpty()) {
			return Direction.CENTER;
		}
		double sx = stationBounds.getCenterX();
		double sy = stationBounds.getCenterY();
		double dx = dockableBounds.getCenterX();
		double dy = dockableBounds.getCenterY();
		double cover = (dockableBounds.width * (double) dockableBounds.height)
				/ Math.max(1.0, stationBounds.width * (double) stationBounds.height);

		if (cover > 0.72) {
			return Direction.CENTER;
		}
		double absX = Math.abs(dx - sx);
		double absY = Math.abs(dy - sy);
		if (absX < stationBounds.width * 0.08 && absY < stationBounds.height * 0.08 && cover > 0.45) {
			return Direction.CENTER;
		}
		if (absX >= absY) {
			return dx < sx ? Direction.LEFT : Direction.RIGHT;
		}
		return dy < sy ? Direction.TOP : Direction.BOTTOM;
	}

	private static void paintIfAvailable(Graphics2D g, Direction[] available, Direction dir, int x, int y, int cell,
			Direction active) {
		if (!isAvailable(available, dir)) {
			return;
		}
		paintCue(g, x, y, cell, dir, dir == active);
	}

	private static boolean isAvailable(Direction[] available, Direction dir) {
		if (available == null || available.length == 0) {
			return true;
		}
		for (Direction d : available) {
			if (d == dir) {
				return true;
			}
		}
		return false;
	}

	private static Rectangle cellRect(int origin, int total, int cell, int gap, int col, int row) {
		return new Rectangle(origin + col * (cell + gap), origin + row * (cell + gap), cell, cell);
	}

	private static void paintSideCue(Graphics2D g, float ix, float iy, float inner, boolean left, boolean active) {
		float panelW = inner * 0.42f;
		float panelH = inner * 0.78f;
		float panelY = iy + (inner - panelH) * 0.5f;
		float panelX = left ? ix : ix + inner - panelW;
		float title = Math.max(3f, panelH * 0.22f);

		paintMiniWindow(g, panelX, panelY, panelW, panelH, title, active);

		float arrowSize = inner * 0.28f;
		float ax = left ? ix + inner - arrowSize * 0.15f - arrowSize : ix + arrowSize * 0.15f;
		float ay = iy + (inner - arrowSize) * 0.5f;
		paintArrow(g, ax, ay, arrowSize, left ? Direction.LEFT : Direction.RIGHT, active);
	}

	private static void paintVerticalCue(Graphics2D g, float ix, float iy, float inner, boolean top, boolean active) {
		float panelW = inner * 0.78f;
		float panelH = inner * 0.42f;
		float panelX = ix + (inner - panelW) * 0.5f;
		float panelY = top ? iy : iy + inner - panelH;
		float title = Math.max(3f, panelH * 0.28f);

		paintMiniWindow(g, panelX, panelY, panelW, panelH, title, active);

		float arrowSize = inner * 0.28f;
		float ax = ix + (inner - arrowSize) * 0.5f;
		float ay = top ? iy + inner - arrowSize * 0.15f - arrowSize : iy + arrowSize * 0.15f;
		paintArrow(g, ax, ay, arrowSize, top ? Direction.TOP : Direction.BOTTOM, active);
	}

	private static void paintCenterCue(Graphics2D g, float ix, float iy, float inner, boolean active) {
		float pad = inner * 0.12f;
		paintMiniWindow(g, ix + pad, iy + pad, inner - 2f * pad, inner - 2f * pad, Math.max(3f, inner * 0.16f), active);
	}

	private static void paintMiniWindow(Graphics2D g, float x, float y, float w, float h, float title, boolean active) {
		RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w, h, 3f, 3f);
		g.setColor(CUE_BG);
		g.fill(body);
		g.setColor(active ? CUE_BORDER_ACTIVE : CUE_FILL);
		g.fill(new RoundRectangle2D.Float(x, y, w, title, 3f, 3f));
		g.fillRect(Math.round(x), Math.round(y + title * 0.45f), Math.round(w), Math.round(title * 0.55f));
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(active ? CUE_BORDER_ACTIVE : CUE_OUTLINE);
		g.draw(body);
	}

	private static void paintArrow(Graphics2D g, float x, float y, float size, Direction dir, boolean active) {
		Path2D.Float path = new Path2D.Float();
		switch (dir) {
			case LEFT -> {
				path.moveTo(x + size, y);
				path.lineTo(x, y + size * 0.5f);
				path.lineTo(x + size, y + size);
			}
			case RIGHT -> {
				path.moveTo(x, y);
				path.lineTo(x + size, y + size * 0.5f);
				path.lineTo(x, y + size);
			}
			case TOP -> {
				path.moveTo(x, y + size);
				path.lineTo(x + size * 0.5f, y);
				path.lineTo(x + size, y + size);
			}
			case BOTTOM -> {
				path.moveTo(x, y);
				path.lineTo(x + size * 0.5f, y + size);
				path.lineTo(x + size, y);
			}
			default -> {
				return;
			}
		}
		path.closePath();
		g.setColor(active ? CUE_BORDER_ACTIVE : CUE_FILL);
		g.fill(path);
	}
}
