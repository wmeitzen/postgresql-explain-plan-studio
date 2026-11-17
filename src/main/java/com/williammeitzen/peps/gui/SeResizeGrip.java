package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * HTML-textarea-style south-east resize grip: two short diagonal lines instead of
 * FlatLaf's three-dot split-pane ellipsis.
 */
public final class SeResizeGrip extends JComponent {

	private static final long serialVersionUID = 1L;

	private static final int SIZE = 14;

	private BiConsumer<Integer, Integer> onDrag;
	private int lastX;
	private int lastY;

	public SeResizeGrip() {
		setOpaque(false);
		setVisible(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
		setToolTipText("Drag to resize");
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				lastX = e.getXOnScreen();
				lastY = e.getYOnScreen();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (onDrag == null) {
					return;
				}
				int x = e.getXOnScreen();
				int y = e.getYOnScreen();
				int dx = x - lastX;
				int dy = y - lastY;
				lastX = x;
				lastY = y;
				if (dx != 0 || dy != 0) {
					onDrag.accept(Integer.valueOf(dx), Integer.valueOf(dy));
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	public void setOnDrag(BiConsumer<Integer, Integer> onDrag) {
		this.onDrag = onDrag;
		setVisible(onDrag != null);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(SIZE, SIZE);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color color = UIManager.getColor("Label.disabledForeground");
			if (color == null) {
				color = UIManager.getColor("Component.borderColor");
			}
			if (color == null) {
				color = new Color(0x90, 0x90, 0x90);
			}
			g2.setColor(color);
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int w = getWidth();
			int h = getHeight();
			// Two parallel diagonals in the SE corner (browser textarea affordance).
			drawDiag(g2, w - 3, h - 9, w - 9, h - 3);
			drawDiag(g2, w - 3, h - 5, w - 5, h - 3);
		} finally {
			g2.dispose();
		}
	}

	private static void drawDiag(Graphics2D g2, int x1, int y1, int x2, int y2) {
		g2.drawLine(x1, y1, x2, y2);
	}
}
