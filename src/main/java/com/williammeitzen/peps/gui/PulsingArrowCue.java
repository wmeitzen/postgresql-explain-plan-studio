package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Left-pointing arrow that pulses beside a control (e.g. Show Plan Diagram).
 */
public class PulsingArrowCue extends JComponent {

	private final Timer pulseTimer;
	private float phase = 0f;

	public PulsingArrowCue() {
		setOpaque(false);
		setPreferredSize(new Dimension(148, 40));
		setMinimumSize(new Dimension(120, 32));
		setVisible(false);
		// ~1.1s cycle so the beat is easy to notice without feeling frantic.
		pulseTimer = new Timer(33, e -> {
			phase += 0.19f;
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

			// 0 → 1 with a sharp peak so the flash reads clearly.
			float wave = (float) ((Math.sin(phase) + 1.0) * 0.5);
			float pulse = wave * wave; // ease toward bright peaks
			float bounce = 4f * (float) Math.sin(phase); // nudge toward the button

			int w = getWidth();
			int h = getHeight();
			int cy = h / 2;

			// Soft pill behind the cue — brightens and expands on each beat.
			float pad = 2f + 3f * pulse;
			g2.setColor(new Color(251, 191, 36, Math.round(40 + 90 * pulse)));
			g2.fill(new RoundRectangle2D.Float(pad, pad, w - pad * 2, h - pad * 2, 14, 14));

			// Expanding halo near the arrow tip.
			float haloR = 10f + 14f * pulse;
			g2.setColor(new Color(96, 165, 250, Math.round(20 + 70 * (1f - pulse))));
			g2.fill(new Ellipse2D.Float(8 + bounce - haloR * 0.35f, cy - haloR, haloR * 2f, haloR * 2f));

			Color accent = new Color(0x38, 0xBD, 0xF8);
			Color hot = new Color(0xFB, 0xBF, 0x24);
			Color color = blend(accent, hot, 0.15f + 0.75f * pulse);
			int alpha = Math.round(210 + 45 * pulse);
			g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));

			int arrowLeft = Math.round(6 + bounce);
			int arrowRight = arrowLeft + 30;
			float stroke = 2.8f + 2.4f * pulse;
			g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			// Shaft pointing left (toward the button).
			g2.drawLine(arrowRight, cy, arrowLeft + 12, cy);
			Path2D head = new Path2D.Float();
			float headSize = 8f + 3f * pulse;
			head.moveTo(arrowLeft + 1, cy);
			head.lineTo(arrowLeft + 14, cy - headSize);
			head.lineTo(arrowLeft + 14, cy + headSize);
			head.closePath();
			g2.fill(head);

			// Outer glow stroke on the arrow at peak brightness.
			if (pulse > 0.35f) {
				g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
						Math.round(60 * pulse)));
				g2.setStroke(new BasicStroke(stroke + 4f * pulse, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.drawLine(arrowRight, cy, arrowLeft + 12, cy);
			}

			g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
			float fontSize = 13f + 3.5f * pulse;
			g2.setFont(getFont().deriveFont(Font.BOLD, fontSize));
			g2.drawString("Next", arrowRight + 8, cy + 5);
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
