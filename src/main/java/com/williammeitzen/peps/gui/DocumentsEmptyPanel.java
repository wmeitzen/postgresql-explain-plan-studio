package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Documents landing screen when the Plans tree has no plans yet.
 */
public class DocumentsEmptyPanel extends JPanel {

	private final FormPanel formPanel;

	public DocumentsEmptyPanel(FormPanel formPanel) {
		this.formPanel = formPanel;
		setLayout(new GridBagLayout());
		setOpaque(true);

		JPanel column = new JPanel();
		column.setOpaque(false);
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

		JLabel title = new JLabel("Get started", SwingConstants.CENTER);
		title.setAlignmentX(CENTER_ALIGNMENT);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
		column.add(title);
		column.add(Box.createVerticalStrut(8));

		JLabel subtitle = new JLabel(
				"Create a blank plan, or open a large sample EXPLAIN to explore the diagram.",
				SwingConstants.CENTER);
		subtitle.setAlignmentX(CENTER_ALIGNMENT);
		subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
		column.add(subtitle);
		column.add(Box.createVerticalStrut(28));

		column.add(actionButton(
				createLargePlusIcon(),
				"New blank plan",
				"Add an empty plan. Paste EXPLAIN output or run a query from Plan Source.",
				() -> formPanel.addBlankPlan()));
		column.add(Box.createVerticalStrut(14));
		column.add(actionButton(
				createJsonIcon(),
				"Sample JSON plan",
				"Show an example JSON plan.",
				() -> formPanel.addSampleJsonPlan()));
		column.add(Box.createVerticalStrut(14));
		column.add(actionButton(
				createTextIcon(),
				"Sample text plan",
				"Show an example text plan.",
				() -> formPanel.addSampleTextPlan()));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		add(column, gbc);
		refreshChrome();
	}

	/** Re-apply colors after a theme change. */
	public void refreshChrome() {
		Color bg = UIManager.getColor("Panel.background");
		if (bg != null) {
			setBackground(bg);
		}
		Color fg = UIManager.getColor("Label.foreground");
		if (fg != null) {
			applyForeground(this, fg);
		}
		revalidate();
		repaint();
	}

	private static void applyForeground(java.awt.Container c, Color fg) {
		for (java.awt.Component child : c.getComponents()) {
			if (child instanceof JLabel) {
				child.setForeground(fg);
			} else if (child instanceof java.awt.Container) {
				applyForeground((java.awt.Container) child, fg);
			}
		}
	}

	private JButton actionButton(ImageIcon icon, String heading, String description, Runnable action) {
		JButton button = new JButton();
		button.setAlignmentX(CENTER_ALIGNMENT);
		button.setLayout(new BorderLayout(16, 0));
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setMargin(new Insets(14, 18, 14, 18));
		button.setMaximumSize(new Dimension(520, 88));
		button.setPreferredSize(new Dimension(520, 88));
		// Icon as WEST content (not setIcon) so CENTER text never paints over it.
		JLabel iconLabel = new JLabel(icon);
		iconLabel.setOpaque(false);
		button.add(iconLabel, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		JLabel head = new JLabel(heading);
		head.setAlignmentX(LEFT_ALIGNMENT);
		head.setFont(head.getFont().deriveFont(Font.BOLD, 15f));
		JLabel desc = new JLabel("<html><body style='width:360px'>" + description + "</body></html>");
		desc.setAlignmentX(LEFT_ALIGNMENT);
		desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
		text.add(head);
		text.add(Box.createVerticalStrut(4));
		text.add(desc);
		button.add(text, BorderLayout.CENTER);

		button.addActionListener(e -> action.run());
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(UIManager.getColor("Button.hoverBackground") != null
						? UIManager.getColor("Button.hoverBackground")
						: button.getBackground().brighter());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(UIManager.getColor("Button.background"));
			}
		});
		return button;
	}

	private static ImageIcon createLargePlusIcon() {
		int size = 48;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0x16, 0xA3, 0x4A));
		g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(size / 2, 10, size / 2, size - 10);
		g.drawLine(10, size / 2, size - 10, size / 2);
		g.dispose();
		return new ImageIcon(image);
	}

	private static ImageIcon createJsonIcon() {
		int size = 48;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(new Color(0x25, 0x63, 0xEB));
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
		g.drawString("{ }", 8, 32);
		g.dispose();
		return new ImageIcon(image);
	}

	private static ImageIcon createTextIcon() {
		int size = 48;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0xCA, 0x8A, 0x04));
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(12, 14, 36, 14);
		g.drawLine(12, 24, 36, 24);
		g.drawLine(12, 34, 28, 34);
		g.dispose();
		return new ImageIcon(image);
	}
}
