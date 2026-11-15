package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;

/**
 * Property/Value table with Expand all / Calculated fields above, optional
 * calculated metrics below a separator, and Notes underneath in the same
 * scroll view (not a separate split pane).
 */
public class OperatorDetailsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** ~3× typical default unit increment so the mouse wheel moves farther. */
	private static final int WHEEL_UNIT_INCREMENT = 48;

	/** Session preference: Expand all survives plan switches and empty selection. */
	private static boolean expandAllPreference = false;

	/** Session preference: show calculated metrics when the operator has any. */
	private static boolean calculatedFieldsPreference = true;

	private final OperationPropertiesPanel propertiesPanel;
	private final OperationPropertiesPanel calculatedPanel;
	private final JCheckBox expandAllCheckbox;
	private final JCheckBox calculatedFieldsCheckbox;
	private final JLabel calculatedHeading;
	private final JLabel notesHeading;
	private final JTextArea notesArea;
	private final JPanel notesBlock;
	private final JPanel tableWithHeader;
	private final JPanel calculatedTableWithHeader;
	private final JPanel calculatedBlock;
	private final JPanel propertiesBlock;
	private final JPanel columnView;
	private final JScrollPane scroll;
	private final JPanel optionsBar;

	private boolean hasCalculatedFields = false;

	public static boolean isExpandAllPreference() {
		return expandAllPreference;
	}

	public static void setExpandAllPreference(boolean value) {
		expandAllPreference = value;
	}

	public static boolean isCalculatedFieldsPreference() {
		return calculatedFieldsPreference;
	}

	public static void setCalculatedFieldsPreference(boolean value) {
		calculatedFieldsPreference = value;
	}

	public OperatorDetailsPanel(PlanGraphPanel graphPanel) {
		super(new BorderLayout());
		propertiesPanel = graphPanel != null
				? new OperationPropertiesPanel(graphPanel)
				: new OperationPropertiesPanel();
		if (graphPanel == null) {
			propertiesPanel.createComponents();
		}

		calculatedPanel = new OperationPropertiesPanel();
		calculatedPanel.createComponents();

		expandAllCheckbox = new JCheckBox("Expand all");
		expandAllCheckbox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		expandAllCheckbox.setOpaque(false);
		expandAllCheckbox.setFocusable(false);
		expandAllCheckbox.setSelected(expandAllPreference);
		expandAllCheckbox.addActionListener(e ->
				propertiesPanel.setExpandAll(expandAllCheckbox.isSelected()));

		calculatedFieldsCheckbox = new JCheckBox("Calculated fields");
		calculatedFieldsCheckbox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		calculatedFieldsCheckbox.setOpaque(false);
		calculatedFieldsCheckbox.setFocusable(false);
		calculatedFieldsCheckbox.setSelected(calculatedFieldsPreference);
		calculatedFieldsCheckbox.setVisible(false);
		calculatedFieldsCheckbox.addActionListener(e -> {
			setCalculatedFieldsPreference(calculatedFieldsCheckbox.isSelected());
			updateCalculatedVisibility();
		});

		optionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
		optionsBar.setOpaque(true);
		optionsBar.setBorder(new EmptyBorder(2, 0, 0, 0));
		optionsBar.add(expandAllCheckbox);
		optionsBar.add(calculatedFieldsCheckbox);

		calculatedHeading = new JLabel("Calculated Fields");
		calculatedHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		calculatedHeading.setBorder(new EmptyBorder(8, 8, 4, 8));
		calculatedHeading.setOpaque(true);
		calculatedHeading.setAlignmentX(LEFT_ALIGNMENT);

		notesHeading = new JLabel("Red Text Notes:");
		notesHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		notesHeading.setBorder(new EmptyBorder(8, 8, 2, 8));
		notesHeading.setOpaque(true);

		notesArea = new JTextArea();
		notesArea.setEditable(false);
		notesArea.setFocusable(false);
		// Wrap to the visible viewport width (not the possibly-wider property grid)
		notesArea.setLineWrap(true);
		notesArea.setWrapStyleWord(true);
		notesArea.setOpaque(true);
		notesArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		notesArea.setBorder(new EmptyBorder(0, 8, 8, 8));
		notesArea.setText("");
		notesArea.setRows(1);

		notesBlock = new JPanel(new BorderLayout());
		notesBlock.setAlignmentX(LEFT_ALIGNMENT);
		notesBlock.add(notesHeading, BorderLayout.NORTH);
		notesBlock.add(notesArea, BorderLayout.CENTER);

		// BoxLayout (not BorderLayout.CENTER) so the grid keeps its column preferred
		// widths — BorderLayout stretches the table and made the Property/Value
		// divider feel capped by the other column.
		tableWithHeader = buildTableWithHeader(propertiesPanel);
		calculatedTableWithHeader = buildTableWithHeader(calculatedPanel);

		JSeparator calculatedSeparator = new JSeparator();
		calculatedSeparator.setAlignmentX(LEFT_ALIGNMENT);
		calculatedSeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, calculatedSeparator.getPreferredSize().height));

		calculatedBlock = new JPanel();
		calculatedBlock.setLayout(new BoxLayout(calculatedBlock, BoxLayout.Y_AXIS));
		calculatedBlock.setOpaque(false);
		calculatedBlock.setAlignmentX(LEFT_ALIGNMENT);
		calculatedBlock.setBorder(new EmptyBorder(8, 0, 0, 0));
		calculatedBlock.add(calculatedSeparator);
		calculatedBlock.add(calculatedHeading);
		calculatedBlock.add(calculatedTableWithHeader);
		calculatedBlock.setVisible(false);

		propertiesBlock = new JPanel();
		propertiesBlock.setLayout(new BoxLayout(propertiesBlock, BoxLayout.Y_AXIS));
		propertiesBlock.setOpaque(false);
		propertiesBlock.setAlignmentX(LEFT_ALIGNMENT);
		optionsBar.setAlignmentX(LEFT_ALIGNMENT);
		propertiesBlock.add(optionsBar);
		propertiesBlock.add(tableWithHeader);

		// Stack EXPLAIN grid, calculated grid, then Notes; keep content at the top.
		columnView = new JPanel();
		columnView.setLayout(new BoxLayout(columnView, BoxLayout.Y_AXIS));
		columnView.setAlignmentX(LEFT_ALIGNMENT);
		columnView.setAlignmentY(TOP_ALIGNMENT);
		columnView.add(propertiesBlock);
		columnView.add(calculatedBlock);
		columnView.add(notesBlock);
		columnView.add(Box.createVerticalGlue());

		// JScrollPane centers a short view by default — pin content to the top.
		JPanel scrollContent = new JPanel(new BorderLayout());
		scrollContent.setOpaque(false);
		scrollContent.add(columnView, BorderLayout.NORTH);

		scroll = new JScrollPane(scrollContent,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(WHEEL_UNIT_INCREMENT);
		scroll.getHorizontalScrollBar().setUnitIncrement(WHEEL_UNIT_INCREMENT);
		scroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
		add(scroll, BorderLayout.CENTER);
		applyThemeSurface();

		// Keep Notes wrap width tied to the visible viewport, even when the grid is wider
		ChangeListener viewportListener = (ChangeEvent e) -> updateNotesWrapToViewport();
		scroll.getViewport().addChangeListener(viewportListener);
		scroll.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateNotesWrapToViewport();
			}
		});

		propertiesPanel.setNotesListener(this::setNotesText);
		propertiesPanel.setExpandAllListener(selected -> {
			if (expandAllCheckbox.isSelected() != selected) {
				expandAllCheckbox.setSelected(selected);
			}
		});
		setNotesText("");
		// Empty until an operator is clicked on the diagram.
		showEmptyState();
	}

	private static JPanel buildTableWithHeader(OperationPropertiesPanel table) {
		JPanel wrap = new JPanel();
		wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
		wrap.setOpaque(false);
		JTableHeader header = table.getTableHeader();
		header.setAlignmentX(LEFT_ALIGNMENT);
		table.setAlignmentX(LEFT_ALIGNMENT);
		wrap.add(header);
		wrap.add(table);
		wrap.setAlignmentX(LEFT_ALIGNMENT);
		return wrap;
	}

	/** Hide Expand all / grid / notes until an operator is selected. */
	public void clearOperatorProperties() {
		propertiesPanel.clearOperatorProperties();
		calculatedPanel.clearOperatorProperties();
		expandAllCheckbox.setSelected(expandAllPreference);
		calculatedFieldsCheckbox.setSelected(calculatedFieldsPreference);
		hasCalculatedFields = false;
		calculatedFieldsCheckbox.setVisible(false);
		showEmptyState();
		scrollToTop();
	}

	private void showEmptyState() {
		propertiesBlock.setVisible(false);
		calculatedBlock.setVisible(false);
		notesBlock.setVisible(false);
		setNotesText("");
		columnView.revalidate();
		columnView.repaint();
	}

	private void showPropertiesChrome() {
		propertiesBlock.setVisible(true);
		columnView.revalidate();
		columnView.repaint();
	}

	/** Match panel/notes surfaces to Light vs Dark (replaces hard-coded light slate). */
	public void applyThemeSurface() {
		Color surface = UiTheme.uiColor(UiTheme.elevatedSurfaceColor());
		Color border = UiTheme.subtleBorderColor();
		Color panel = UiTheme.uiColor(UiTheme.surfaceColor());
		Color fg = UIManager.getColor("Label.foreground");
		setOpaque(true);
		setBackground(panel);
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(6, 6, 6, 6),
				BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(border),
						new EmptyBorder(4, 4, 4, 4))));
		optionsBar.setBackground(surface);
		calculatedHeading.setBackground(surface);
		notesHeading.setBackground(surface);
		if (fg != null) {
			calculatedHeading.setForeground(fg);
			notesHeading.setForeground(fg);
		}
		notesArea.setBackground(surface);
		if (fg != null) {
			notesArea.setForeground(fg);
		}
		notesBlock.setBackground(surface);
		propertiesBlock.setBackground(surface);
		calculatedBlock.setBackground(surface);
		tableWithHeader.setBackground(surface);
		calculatedTableWithHeader.setBackground(surface);
		columnView.setBackground(surface);
		scroll.getViewport().setBackground(surface);
		stylePropertiesTable(propertiesPanel, surface, fg);
		stylePropertiesTable(calculatedPanel, surface, fg);
	}

	private static void stylePropertiesTable(OperationPropertiesPanel table, Color surface, Color fg) {
		if (table == null) {
			return;
		}
		table.setBackground(surface);
		if (fg != null) {
			table.setForeground(fg);
		}
		table.setGridColor(UiTheme.subtleBorderColor());
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (scroll != null) {
			applyThemeSurface();
		}
	}

	/**
	 * Constrain Notes to the visible viewport width so wrapping follows the pane,
	 * not the (possibly wider) Property/Value grid.
	 */
	private void updateNotesWrapToViewport() {
		JViewport viewport = scroll.getViewport();
		if (viewport == null) {
			return;
		}
		int viewportWidth = viewport.getExtentSize().width;
		if (viewportWidth <= 0) {
			return;
		}
		// Leave room for the vertical scrollbar already accounted in extent width
		notesBlock.setMaximumSize(new Dimension(viewportWidth, Integer.MAX_VALUE));
		notesBlock.setPreferredSize(null);
		notesArea.setSize(viewportWidth, Short.MAX_VALUE);
		int preferredHeight = Math.max(notesArea.getPreferredSize().height, notesArea.getFontMetrics(notesArea.getFont()).getHeight());
		notesArea.setPreferredSize(new Dimension(viewportWidth, preferredHeight));
		notesBlock.revalidate();
	}

	private void setNotesText(String text) {
		String notes = (text == null) ? "" : text.trim();
		notesArea.setText(notes);
		notesBlock.setVisible(propertiesBlock.isVisible() && !notes.isEmpty());
		if (!notes.isEmpty()) {
			int lines = Math.max(3, notes.split("\n", -1).length + 1);
			notesArea.setRows(lines);
		}
		updateNotesWrapToViewport();
		columnView.revalidate();
		columnView.repaint();
	}

	private void updateCalculatedVisibility() {
		boolean show = propertiesBlock.isVisible()
				&& hasCalculatedFields
				&& calculatedFieldsPreference;
		calculatedFieldsCheckbox.setVisible(propertiesBlock.isVisible() && hasCalculatedFields);
		calculatedFieldsCheckbox.setSelected(calculatedFieldsPreference);
		calculatedBlock.setVisible(show);
		columnView.revalidate();
		columnView.repaint();
	}

	private void loadCalculatedFields(int operatorPropertyId) {
		PlanGraphPanel graph = propertiesPanel.getPlanGraphPanel();
		PlanGraphPanel.OperatorCalculatedProperties calculated = (graph == null)
				? new PlanGraphPanel.OperatorCalculatedProperties(new Object[0][2], new boolean[0])
				: graph.getOperatorCalculatedProperties(operatorPropertyId);
		hasCalculatedFields = calculated != null && calculated.rows != null && calculated.rows.length > 0;
		if (hasCalculatedFields) {
			calculatedPanel.setPropertiesDetails(calculated.rows, calculated.highlights);
		} else {
			calculatedPanel.clearOperatorProperties();
		}
		updateCalculatedVisibility();
	}

	private void scrollToTop() {
		SwingUtilities.invokeLater(() -> {
			scroll.getVerticalScrollBar().setValue(0);
			scroll.getHorizontalScrollBar().setValue(0);
			scroll.getViewport().setViewPosition(new Point(0, 0));
			updateNotesWrapToViewport();
		});
	}

	public OperationPropertiesPanel getPropertiesPanel() {
		return propertiesPanel;
	}

	public void setPlanGraphPanel(PlanGraphPanel graphPanel) {
		propertiesPanel.setPlanGraphPanel(graphPanel);
		clearOperatorProperties();
	}

	public void showOperatorProperties(int operatorPropertyId) {
		showPropertiesChrome();
		expandAllCheckbox.setSelected(expandAllPreference);
		propertiesPanel.syncExpandAllFromPreference();
		propertiesPanel.showOperatorProperties(operatorPropertyId);
		loadCalculatedFields(operatorPropertyId);
		scrollToTop();
	}
}
