package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Property/Value table with Expand all above and Notes underneath in the same
 * scroll view (not a separate split pane).
 */
public class OperatorDetailsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** ~3× typical default unit increment so the mouse wheel moves farther. */
	private static final int WHEEL_UNIT_INCREMENT = 48;

	private final OperationPropertiesPanel propertiesPanel;
	private final JCheckBox expandAllCheckbox;
	private final JLabel notesHeading;
	private final JTextArea notesArea;
	private final JPanel notesBlock;
	private final JPanel tableWithHeader;
	private final JPanel propertiesBlock;
	private final JPanel columnView;
	private final JScrollPane scroll;

	public OperatorDetailsPanel(PlanGraphPanel graphPanel) {
		super(new BorderLayout());
		propertiesPanel = new OperationPropertiesPanel(graphPanel);

		Color notesBg = new Color(0xF5, 0xF5, 0xF5);

		expandAllCheckbox = new JCheckBox("Expand all");
		expandAllCheckbox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		expandAllCheckbox.setOpaque(false);
		expandAllCheckbox.setFocusable(false);
		expandAllCheckbox.addActionListener(e ->
				propertiesPanel.setExpandAll(expandAllCheckbox.isSelected()));

		JPanel expandAllBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
		expandAllBar.setOpaque(true);
		expandAllBar.setBackground(notesBg);
		expandAllBar.setBorder(new EmptyBorder(2, 0, 0, 0));
		expandAllBar.add(expandAllCheckbox);

		notesHeading = new JLabel("Notes:");
		notesHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		notesHeading.setBorder(new EmptyBorder(8, 8, 2, 8));
		notesHeading.setOpaque(true);
		notesHeading.setBackground(notesBg);

		notesArea = new JTextArea();
		notesArea.setEditable(false);
		notesArea.setFocusable(false);
		// Wrap only when a line exceeds the visible width; otherwise keep one line
		notesArea.setLineWrap(true);
		notesArea.setWrapStyleWord(true);
		notesArea.setOpaque(true);
		notesArea.setBackground(notesBg);
		notesArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		notesArea.setBorder(new EmptyBorder(0, 8, 8, 8));
		notesArea.setText("");
		notesArea.setRows(1);

		notesBlock = new JPanel(new BorderLayout());
		notesBlock.setBackground(notesBg);
		notesBlock.setAlignmentX(LEFT_ALIGNMENT);
		notesBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		notesBlock.add(notesHeading, BorderLayout.NORTH);
		notesBlock.add(notesArea, BorderLayout.CENTER);

		tableWithHeader = new JPanel(new BorderLayout());
		tableWithHeader.add(propertiesPanel.getTableHeader(), BorderLayout.NORTH);
		tableWithHeader.add(propertiesPanel, BorderLayout.CENTER);

		propertiesBlock = new JPanel(new BorderLayout());
		propertiesBlock.setBackground(notesBg);
		propertiesBlock.setAlignmentX(LEFT_ALIGNMENT);
		propertiesBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		propertiesBlock.add(expandAllBar, BorderLayout.NORTH);
		propertiesBlock.add(tableWithHeader, BorderLayout.CENTER);

		// Stack grid then Notes directly underneath (avoid BorderLayout.SOUTH gap)
		columnView = new JPanel();
		columnView.setLayout(new BoxLayout(columnView, BoxLayout.Y_AXIS));
		columnView.setBackground(notesBg);
		columnView.add(propertiesBlock);
		columnView.add(notesBlock);
		columnView.add(Box.createVerticalGlue());

		scroll = new JScrollPane(columnView,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.getViewport().setBackground(notesBg);
		scroll.getVerticalScrollBar().setUnitIncrement(WHEEL_UNIT_INCREMENT);
		scroll.getHorizontalScrollBar().setUnitIncrement(WHEEL_UNIT_INCREMENT);
		add(scroll, BorderLayout.CENTER);

		propertiesPanel.setNotesListener(this::setNotesText);
		propertiesPanel.setExpandAllListener(selected -> {
			if (expandAllCheckbox.isSelected() != selected) {
				expandAllCheckbox.setSelected(selected);
			}
		});
		setNotesText("");
	}

	private void setNotesText(String text) {
		String notes = (text == null) ? "" : text.trim();
		notesArea.setText(notes);
		notesBlock.setVisible(!notes.isEmpty());
		if (!notes.isEmpty()) {
			int lines = Math.max(3, notes.split("\n").length + 1);
			notesArea.setRows(lines);
		}
		columnView.revalidate();
		columnView.repaint();
	}

	private void scrollToTop() {
		SwingUtilities.invokeLater(() -> {
			scroll.getVerticalScrollBar().setValue(0);
			scroll.getHorizontalScrollBar().setValue(0);
			scroll.getViewport().setViewPosition(new Point(0, 0));
		});
	}

	public OperationPropertiesPanel getPropertiesPanel() {
		return propertiesPanel;
	}

	public void showOperatorProperties(int operatorPropertyId) {
		propertiesPanel.showOperatorProperties(operatorPropertyId);
		scrollToTop();
	}
}
