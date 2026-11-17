package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.williammeitzen.peps.model.PlanAnonymizer;

/**
 * Paste anonymized advice SQL; live-translate back to original names using the
 * current plan's anonymize map.
 */
public class TranslateAnonymizedPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Color WARN_BG = PlanGraphPanel.HOTSPOT_HIGHLIGHT_BACKGROUND;
	private static final Color WARN_FG = PlanGraphPanel.HOTSPOT_HIGHLIGHT_FOREGROUND;
	private static final Color BORDER_RED = new Color(0xC0, 0x28, 0x28);
	private static final Color BORDER_PEAK = new Color(0xFF, 0x45, 0x45);

	private final JTextArea inputArea;
	private final JTextArea outputArea;
	private final JScrollPane outputScroll;
	private final JLabel warningLabel;
	private final JLabel inputAnonymizeBadge;
	private final Border outputDefaultBorder;
	private Timer pulseTimer;
	private boolean showingWarning;
	private PlanSourcePanel planSourcePanel;
	private boolean syncing;

	public TranslateAnonymizedPanel() {
		super(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(8, 8, 8, 8));

		warningLabel = new JLabel(" ");
		warningLabel.setOpaque(true);
		warningLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
		warningLabel.setVisible(false);

		inputArea = new JTextArea();
		inputArea.setLineWrap(true);
		inputArea.setWrapStyleWord(true);
		inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		Utils.installCrossPlatformEditBindings(inputArea);

		outputArea = new JTextArea();
		outputArea.setLineWrap(true);
		outputArea.setWrapStyleWord(true);
		outputArea.setEditable(false);
		outputArea.setFocusable(true);
		outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		Utils.installCrossPlatformEditBindings(outputArea);

		JScrollPane inputScroll = new JScrollPane(inputArea);
		outputScroll = new JScrollPane(outputArea);
		outputDefaultBorder = outputScroll.getBorder();
		sizeFlexibleTextArea(inputScroll);
		sizeFlexibleTextArea(outputScroll);

		inputAnonymizeBadge = PlanSourcePanel.createAnonymizeBadge();
		inputAnonymizeBadge.setVisible(true);
		JComponent inputHost = PlanSourcePanel.wrapWithAnonymizeBadge(inputScroll, inputAnonymizeBadge);
		// Ensure overlay has non-zero bounds after first layout pass
		javax.swing.SwingUtilities.invokeLater(() -> PlanSourcePanel.relayoutAnonymizeBadgeHost(inputHost));

		JPanel top = new JPanel(new BorderLayout(0, 4));
		top.setOpaque(false);
		top.add(new JLabel("Anonymized text to translate:"), BorderLayout.NORTH);
		top.add(inputHost, BorderLayout.CENTER);
		top.setMinimumSize(new Dimension(120, 100));

		JPanel translatedHeader = new JPanel(new BorderLayout(0, 4));
		translatedHeader.setOpaque(false);
		translatedHeader.add(new JLabel("Translated:"), BorderLayout.NORTH);
		translatedHeader.add(warningLabel, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout(0, 4));
		bottom.setOpaque(false);
		bottom.add(translatedHeader, BorderLayout.NORTH);
		bottom.add(outputScroll, BorderLayout.CENTER);
		bottom.setMinimumSize(new Dimension(120, 100));

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
		split.setResizeWeight(0.5);
		split.setContinuousLayout(true);
		split.setBorder(null);
		split.setDividerSize(8);

		JSplitPane sizedSplit = PlanSourcePanel.wrapWithHorizontalWidthSplit(split, 640);

		add(sizedSplit, BorderLayout.CENTER);

		inputArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				translateLive();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				translateLive();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				translateLive();
			}
		});
	}

	private static void sizeFlexibleTextArea(JScrollPane scrollPane) {
		scrollPane.setPreferredSize(new Dimension(640, 200));
		scrollPane.setMinimumSize(new Dimension(200, 80));
		scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
	}

	public void setPlanSourcePanel(PlanSourcePanel planSourcePanel) {
		this.planSourcePanel = planSourcePanel;
		translateLive();
	}

	private void translateLive() {
		if (syncing) {
			return;
		}
		String input = inputArea.getText();
		Map<String, String> reverse = planSourcePanel == null
				? Map.of()
				: planSourcePanel.getAnonymizeReverseMap();
		String translated = PlanAnonymizer.applyReverse(input, reverse);
		List<String> unmatched = PlanAnonymizer.unmatchedAnonymizedTokens(input, reverse);
		syncing = true;
		try {
			outputArea.setText(translated);
		} finally {
			syncing = false;
		}
		if (unmatched.isEmpty() || input.isBlank()) {
			clearWarning();
		} else {
			showWarning("Could not translate: " + String.join(", ", unmatched));
		}
	}

	private void showWarning(String message) {
		warningLabel.setText(message);
		warningLabel.setBackground(WARN_BG);
		warningLabel.setForeground(WARN_FG);
		warningLabel.setVisible(true);
		if (!showingWarning) {
			showingWarning = true;
			pulseOutputBorderOnce();
		} else {
			outputScroll.setBorder(BorderFactory.createLineBorder(BORDER_RED, 2));
		}
		revalidate();
	}

	private void clearWarning() {
		boolean wasShowing = showingWarning;
		showingWarning = false;
		warningLabel.setVisible(false);
		warningLabel.setText(" ");
		if (pulseTimer != null) {
			pulseTimer.stop();
			pulseTimer = null;
		}
		if (wasShowing || outputScroll.getBorder() != outputDefaultBorder) {
			outputScroll.setBorder(outputDefaultBorder);
		}
		revalidate();
	}

	private void pulseOutputBorderOnce() {
		if (pulseTimer != null) {
			pulseTimer.stop();
		}
		final long start = System.currentTimeMillis();
		final int durationMs = 420;
		pulseTimer = new Timer(35, e -> {
			float t = (System.currentTimeMillis() - start) / (float) durationMs;
			if (t >= 1f) {
				((Timer) e.getSource()).stop();
				pulseTimer = null;
				outputScroll.setBorder(BorderFactory.createLineBorder(BORDER_RED, 2));
				return;
			}
			float intensity = (float) Math.sin(t * Math.PI);
			Color color = blend(BORDER_RED, BORDER_PEAK, intensity);
			outputScroll.setBorder(BorderFactory.createLineBorder(color, 2));
		});
		pulseTimer.setInitialDelay(0);
		pulseTimer.start();
	}

	private static Color blend(Color a, Color b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
		int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
		int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
		return new Color(r, g, bl);
	}
}
