package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Left-pointing arrow that slides beside a control (e.g. Show Plan Diagram).
 */
public class PulsingArrowCue extends JComponent {

	private final Timer pulseTimer;
	private float phase = 0f;

	public PulsingArrowCue() {
		setOpaque(false);
		setPreferredSize(new Dimension(148, 40));
		setMinimumSize(new Dimension(120, 32));
		setVisible(false);
		// Smooth left/right slide inside the green pill (~1.1s cycle).
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

			// Slide toward the button (negative X) and back — fixed size, no grow/shrink.
			float slide = 7f * (float) Math.sin(phase);

			int w = getWidth();
			int h = getHeight();
			int cy = h / 2;

			// Steady rounded green pill (no expanding pad / halo).
			float pad = 3f;
			g2.setColor(new Color(0x16, 0xA3, 0x4A, 200));
			g2.fill(new RoundRectangle2D.Float(pad, pad, w - pad * 2, h - pad * 2, 14, 14));

			Color color = new Color(0xEC, 0xFC, 0xF5);
			g2.setColor(color);

			int arrowLeft = Math.round(10 + slide);
			int arrowRight = arrowLeft + 28;
			// Filled shaft + head (stroke drawLine often vanishes on Ubuntu 22 OpenJDK).
			Path2D shaft = new Path2D.Float();
			float half = 2.2f;
			shaft.moveTo(arrowRight, cy - half);
			shaft.lineTo(arrowLeft + 14, cy - half);
			shaft.lineTo(arrowLeft + 14, cy + half);
			shaft.lineTo(arrowRight, cy + half);
			shaft.closePath();
			g2.fill(shaft);
			Path2D head = new Path2D.Float();
			float headSize = 8f;
			head.moveTo(arrowLeft + 1, cy);
			head.lineTo(arrowLeft + 14, cy - headSize);
			head.lineTo(arrowLeft + 14, cy + headSize);
			head.closePath();
			g2.fill(head);

			g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
			g2.drawString("Next", arrowRight + 8, cy + 5);
		} finally {
			g2.dispose();
		}
	}
}
