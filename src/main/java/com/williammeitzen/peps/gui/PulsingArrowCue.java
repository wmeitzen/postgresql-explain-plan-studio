package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Left-pointing arrow that pulses beside a control (e.g. Show Plan Diagram).
 */
public class PulsingArrowCue extends JComponent {

	private final Timer pulseTimer;
	private float phase = 0f;

	public PulsingArrowCue() {
		setOpaque(false);
		setPreferredSize(new Dimension(120, 36));
		setMinimumSize(new Dimension(100, 28));
		setVisible(false);
		pulseTimer = new Timer(40, e -> {
			phase += 0.12f;
			if (phase > Math.PI * 2f) {
				phase -= Math.PI * 2f;
			}
			repaint();
		});
	}

	public void startPulse() {
		setVisible(true);
		if (!pulseTimer.isRunning()) {
			pulseTimer.start();
		}
		revalidate();
		repaint();
	}

	public void stopPulse() {
		pulseTimer.stop();
		setVisible(false);
		revalidate();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (!isVisible()) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			float pulse = 0.55f + 0.45f * (float) ((Math.sin(phase) + 1.0) * 0.5);
			// Bright accent so the cue reads on dark Linux themes.
			Color accent = new Color(0x60, 0xA5, 0xFA);
			Color color = blend(accent, new Color(0xFB, 0xBF, 0x24), 0.25f + 0.35f * pulse);
			g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
					Math.round(200 + 55 * pulse)));

			int h = getHeight();
			int cy = h / 2;
			int arrowLeft = 4;
			int arrowRight = 34;
			float stroke = 2.4f + 1.2f * pulse;
			g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			// Shaft pointing left (toward the button).
			g2.drawLine(arrowRight, cy, arrowLeft + 10, cy);
			Path2D head = new Path2D.Float();
			head.moveTo(arrowLeft + 2, cy);
			head.lineTo(arrowLeft + 14, cy - 8);
			head.lineTo(arrowLeft + 14, cy + 8);
			head.closePath();
			g2.fill(head);

			g2.setFont(getFont().deriveFont(Font.BOLD, 12f + 1.5f * pulse));
			g2.drawString("Next", arrowRight + 6, cy + 5);
		} finally {
			g2.dispose();
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
