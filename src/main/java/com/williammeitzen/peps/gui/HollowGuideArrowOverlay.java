package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Glass-pane overlay: animated hollow arrow from one component toward another
 * (e.g. “New blank plan” → Plans green plus). Never intercepts mouse input.
 */
public final class HollowGuideArrowOverlay extends JComponent {

	private static HollowGuideArrowOverlay installed;

	private Component from;
	private Component to;
	private final Timer animTimer;
	private float phase = 0f;
	private ComponentAdapter resizeListener;
	private HierarchyListener hierarchyListener;
	private JRootPane boundRoot;

	private HollowGuideArrowOverlay() {
		setOpaque(false);
		animTimer = new Timer(33, e -> {
			phase += 0.16f;
			if (phase > Math.PI * 2f) {
				phase -= Math.PI * 2f;
			}
			repaint();
		});
	}

	/** Show (or retarget) the guide while both components are on-screen. */
	public static void show(Component from, Component to) {
		if (from == null || to == null) {
			dismiss();
			return;
		}
		SwingUtilities.invokeLater(() -> {
			Window window = SwingUtilities.getWindowAncestor(from);
			if (window == null) {
				window = SwingUtilities.getWindowAncestor(to);
			}
			if (!(window instanceof JFrame)) {
				dismiss();
				return;
			}
			JFrame frame = (JFrame) window;
			JRootPane root = frame.getRootPane();
			if (root == null) {
				dismiss();
				return;
			}
			HollowGuideArrowOverlay overlay = ensureInstalled(root);
			overlay.from = from;
			overlay.to = to;
			overlay.syncBounds();
			overlay.setVisible(true);
			if (!overlay.animTimer.isRunning()) {
				overlay.animTimer.start();
			}
			overlay.repaint();
		});
	}

	public static void dismiss() {
		SwingUtilities.invokeLater(() -> {
			if (installed == null) {
				return;
			}
			installed.animTimer.stop();
			installed.from = null;
			installed.to = null;
			installed.setVisible(false);
			installed.repaint();
		});
	}

	private static HollowGuideArrowOverlay ensureInstalled(JRootPane root) {
		if (installed != null && installed.boundRoot == root) {
			return installed;
		}
		if (installed != null) {
			installed.uninstall();
		}
		HollowGuideArrowOverlay overlay = new HollowGuideArrowOverlay();
		overlay.boundRoot = root;
		root.setGlassPane(overlay);
		overlay.resizeListener = new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				overlay.syncBounds();
			}
		};
		root.addComponentListener(overlay.resizeListener);
		overlay.hierarchyListener = e -> {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
				overlay.syncBounds();
			}
		};
		root.addHierarchyListener(overlay.hierarchyListener);
		installed = overlay;
		overlay.syncBounds();
		return overlay;
	}

	private void uninstall() {
		animTimer.stop();
		if (boundRoot != null) {
			if (resizeListener != null) {
				boundRoot.removeComponentListener(resizeListener);
			}
			if (hierarchyListener != null) {
				boundRoot.removeHierarchyListener(hierarchyListener);
			}
			if (boundRoot.getGlassPane() == this) {
				JComponent empty = new JComponent() {
					{
						setOpaque(false);
					}
				};
				boundRoot.setGlassPane(empty);
			}
		}
		boundRoot = null;
		from = null;
		to = null;
		if (installed == this) {
			installed = null;
		}
	}

	private void syncBounds() {
		if (boundRoot == null) {
			return;
		}
		setBounds(0, 0, boundRoot.getWidth(), boundRoot.getHeight());
	}

	/** Pass every click/hover through to components below. */
	@Override
	public boolean contains(int x, int y) {
		return false;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (!isVisible() || from == null || to == null) {
			return;
		}
		if (!from.isShowing() || !to.isShowing()) {
			return;
		}
		Point2D.Float end = centerOnOverlay(to);
		if (end == null) {
			return;
		}
		Point2D.Float fromPt = edgeToward(from, end);
		if (fromPt == null) {
			fromPt = centerOnOverlay(from);
		}
		if (fromPt == null) {
			return;
		}
		float dx = end.x - fromPt.x;
		float dy = end.y - fromPt.y;
		float len = (float) Math.hypot(dx, dy);
		if (len < 24f) {
			return;
		}
		float ux = dx / len;
		float uy = dy / len;
		Point2D.Float toPt = new Point2D.Float(end.x - ux * 10f, end.y - uy * 10f);

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			float pulse = 0.55f + 0.45f * (float) ((Math.sin(phase) + 1.0) * 0.5);
			Color accent = new Color(0x16, 0xA3, 0x4A);
			Color glow = new Color(0x4A, 0xDE, 0x80);
			Color strokeColor = blend(accent, glow, pulse);
			int alpha = Math.round(170 + 70 * pulse);

			float midX = (fromPt.x + toPt.x) * 0.5f;
			float midY = (fromPt.y + toPt.y) * 0.5f;
			float bow = Math.min(90f, len * 0.22f);
			Point2D.Float ctrl = new Point2D.Float(midX - bow * 0.35f, midY - bow);

			Path2D curve = new Path2D.Float();
			curve.moveTo(fromPt.x, fromPt.y);
			curve.quadTo(ctrl.x, ctrl.y, toPt.x, toPt.y);

			boolean linux = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("linux");
			g2.setColor(new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), alpha));
			if (linux) {
				// Dashed Path2D strokes often clip to a stub on Ubuntu 22 — solid + polyline.
				g2.setStroke(new BasicStroke(3.4f + pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				drawQuadAsSegments(g2, fromPt, ctrl, toPt, 24);
			} else {
				float dashPhase = phase * 18f;
				float[] dash = { 10f, 8f };
				Stroke dashed = new BasicStroke(2.2f + 1.2f * pulse, BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND, 10f, dash, dashPhase);
				g2.setStroke(dashed);
				g2.draw(curve);
			}

			double tipAngle = Math.atan2(toPt.y - ctrl.y, toPt.x - ctrl.x);
			paintHollowArrowHead(g2, toPt.x, toPt.y, tipAngle, 16f + 4f * pulse, strokeColor, alpha, linux);

			float ringR = 14f + 8f * pulse;
			g2.setStroke(new BasicStroke(2f + pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(),
					Math.round(90 + 110 * (1f - pulse))));
			if (linux) {
				g2.fill(new Ellipse2D.Float(end.x - 4f, end.y - 4f, 8f, 8f));
			} else {
				g2.draw(new Ellipse2D.Float(end.x - ringR, end.y - ringR, ringR * 2f, ringR * 2f));
			}
		} finally {
			g2.dispose();
		}
	}

	private Point2D.Float centerOnOverlay(Component c) {
		try {
			Point p = SwingUtilities.convertPoint(c, c.getWidth() / 2, c.getHeight() / 2, this);
			return new Point2D.Float(p.x, p.y);
		} catch (Exception ex) {
			return null;
		}
	}

	/** Midpoint of the edge of {@code c} that faces {@code toward}. */
	private Point2D.Float edgeToward(Component c, Point2D.Float toward) {
		try {
			Point tl = SwingUtilities.convertPoint(c, 0, 0, this);
			int w = c.getWidth();
			int h = c.getHeight();
			float cx = tl.x + w / 2f;
			float cy = tl.y + h / 2f;
			float dx = toward.x - cx;
			float dy = toward.y - cy;
			if (Math.abs(dx) >= Math.abs(dy)) {
				if (dx < 0) {
					return new Point2D.Float(tl.x - 2f, cy);
				}
				return new Point2D.Float(tl.x + w + 2f, cy);
			}
			if (dy < 0) {
				return new Point2D.Float(cx, tl.y - 2f);
			}
			return new Point2D.Float(cx, tl.y + h + 2f);
		} catch (Exception ex) {
			return null;
		}
	}

	/** Approximate a quadratic Bezier with straight segments (reliable on Ubuntu OpenJDK). */
	private static void drawQuadAsSegments(Graphics2D g2, Point2D.Float p0, Point2D.Float p1,
			Point2D.Float p2, int steps) {
		float prevX = p0.x;
		float prevY = p0.y;
		int n = Math.max(4, steps);
		for (int i = 1; i <= n; i++) {
			float t = i / (float) n;
			float u = 1f - t;
			float x = u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x;
			float y = u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y;
			g2.drawLine(Math.round(prevX), Math.round(prevY), Math.round(x), Math.round(y));
			prevX = x;
			prevY = y;
		}
	}

	private static void paintHollowArrowHead(Graphics2D g2, float x, float y, double angle,
			float size, Color color, int alpha, boolean filled) {
		GeneralPath head = new GeneralPath();
		if (filled) {
			head.moveTo(0, 0);
			head.lineTo(-size, -size * 0.55f);
			head.lineTo(-size * 0.72f, 0);
			head.lineTo(-size, size * 0.55f);
			head.closePath();
		} else {
			head.moveTo(0, 0);
			head.lineTo(-size, -size * 0.55f);
			head.moveTo(0, 0);
			head.lineTo(-size, size * 0.55f);
			head.moveTo(-size * 0.72f, -size * 0.38f);
			head.lineTo(-size * 0.72f, size * 0.38f);
		}

		AffineTransform at = AffineTransform.getTranslateInstance(x, y);
		at.rotate(angle);
		Path2D drawn = new Path2D.Float(head, at);

		g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
		if (filled) {
			g2.fill(drawn);
		} else {
			g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(drawn);
		}
	}

	private static Color blend(Color a, Color b, float bWeight) {
		float w = Math.max(0f, Math.min(1f, bWeight));
		return new Color(
				Math.round(a.getRed() + (b.getRed() - a.getRed()) * w),
				Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * w),
				Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * w));
	}
}
