package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.williammeitzen.peps.model.OperatorWarnings;

/**
 * Compact Find strip for the Plan Diagram: live search, warning filters,
 * Next/Previous, occurrence count.
 */
public class DiagramFindBar extends JPanel {

	private static final long serialVersionUID = 1L;

	/** High-contrast no-match pair (readable on light and dark schemes). */
	private static final Color NO_MATCH_BG = new Color(0x9F, 0x12, 0x39);
	private static final Color NO_MATCH_FG = new Color(0xFF, 0xF1, 0xF2);

	private final JTextField queryField = new JTextField(18);
	private final JCheckBox timeWarnBox = new JCheckBox("Time");
	private final JCheckBox costWarnBox = new JCheckBox("Cost");
	private final JCheckBox rowsWarnBox = new JCheckBox("Rows");
	private final JCheckBox filterWarnBox = new JCheckBox("Filter");
	private final JCheckBox heapWarnBox = new JCheckBox("Heap");
	private final JLabel occurrenceLabel = new JLabel("Occurrence 0 / 0");

	private PlanGraphPanel graphPanel;
	private Consumer<Void> onCloseRequest;

	private List<Integer> matches = Collections.emptyList();
	private int currentIndex = -1;

	public DiagramFindBar() {
		super(new FlowLayout(FlowLayout.LEFT, 6, 2));
		setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		setOpaque(true);

		add(new JLabel("Find:"));
		queryField.setToolTipText("Search operator labels (Ctrl+F)");
		Utils.installCrossPlatformEditBindings(queryField);
		add(queryField);

		configureWarnBox(timeWarnBox,
				"Require Time warning (combined with other checked filters)");
		configureWarnBox(costWarnBox,
				"Require Cost warning (combined with other checked filters)");
		configureWarnBox(rowsWarnBox,
				"Require Rows warning (Actual vs. Estimated factor from Settings)");
		configureWarnBox(filterWarnBox,
				"Require Filter warning (removed % and Actual rows thresholds from Settings)");
		configureWarnBox(heapWarnBox,
				"Require Heap warning (Heap fetch % threshold from Settings)");
		add(timeWarnBox);
		add(costWarnBox);
		add(rowsWarnBox);
		add(filterWarnBox);
		add(heapWarnBox);

		JButton prevBtn = new JButton("Previous");
		prevBtn.setToolTipText("Find Previous (Shift+Enter / Shift+F3)");
		prevBtn.addActionListener(e -> findPrevious());
		add(prevBtn);

		JButton nextBtn = new JButton("Next");
		nextBtn.setToolTipText("Find Next (Enter / F3)");
		nextBtn.addActionListener(e -> findNext());
		add(nextBtn);

		occurrenceLabel.setPreferredSize(new Dimension(130, occurrenceLabel.getPreferredSize().height));
		add(occurrenceLabel);

		JButton closeBtn = new JButton("\u00D7");
		closeBtn.setToolTipText("Close (Esc)");
		closeBtn.setMargin(new java.awt.Insets(2, 8, 2, 8));
		closeBtn.addActionListener(e -> requestClose());
		add(closeBtn);

		queryField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				searchFromField(true);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				searchFromField(true);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				searchFromField(true);
			}
		});

		queryField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					e.consume();
					requestClose();
					return;
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					if (e.isShiftDown()) {
						findPrevious();
					} else {
						findNext();
					}
					return;
				}
				if (e.getKeyCode() == KeyEvent.VK_F3) {
					e.consume();
					if (e.isShiftDown()) {
						findPrevious();
					} else {
						findNext();
					}
				}
			}
		});

		bindWhenAncestorFocused(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext", this::findNext);
		bindWhenAncestorFocused(KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK),
				"findPrev", this::findPrevious);
		bindWhenAncestorFocused(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "findClose", this::requestClose);
		syncThemeSurfaces();
		setVisible(false);
		updateOccurrenceLabel();
	}

	private void configureWarnBox(JCheckBox box, String tip) {
		box.setOpaque(false);
		box.setToolTipText(tip);
		box.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
				searchFromField(true);
			}
		});
	}

	/** Re-apply LAF TextField/Label colors after View → color-scheme changes. */
	public void syncThemeSurfaces() {
		setOpaque(true);
		setBackground(UiTheme.uiColor(UiTheme.surfaceColor()));
		Color labelFg = UIManager.getColor("Label.foreground");
		for (Component child : getComponents()) {
			if (child instanceof JLabel) {
				if (labelFg != null) {
					child.setForeground(UiTheme.uiColor(labelFg));
				}
			} else if (child instanceof JCheckBox) {
				if (labelFg != null) {
					child.setForeground(UiTheme.uiColor(labelFg));
				}
				child.setBackground(getBackground());
			} else if (child instanceof JButton) {
				Color btnFg = UIManager.getColor("Button.foreground");
				if (btnFg != null) {
					child.setForeground(UiTheme.uiColor(btnFg));
				}
				Color btnBg = UIManager.getColor("Button.background");
				if (btnBg != null) {
					child.setBackground(UiTheme.uiColor(btnBg));
				}
			}
		}
		applyQueryFieldColors();
		revalidate();
		repaint();
	}

	public void setGraphPanel(PlanGraphPanel graphPanel) {
		this.graphPanel = graphPanel;
	}

	public void setOnCloseRequest(Consumer<Void> onCloseRequest) {
		this.onCloseRequest = onCloseRequest;
	}

	/** Show the bar and focus the field (keeps last query). */
	public void openAndFocus() {
		setVisible(true);
		revalidate();
		SwingUtilities.invokeLater(() -> {
			queryField.requestFocusInWindow();
			queryField.selectAll();
			searchFromField(true);
		});
	}

	public void closeBar() {
		setVisible(false);
		revalidate();
		if (graphPanel != null) {
			graphPanel.setFindHighlightQuery(null);
			graphPanel.requestFocusInWindow();
		}
	}

	public boolean isOpen() {
		return isVisible();
	}

	private void requestClose() {
		if (onCloseRequest != null) {
			onCloseRequest.accept(null);
		} else {
			closeBar();
		}
	}

	private void bindWhenAncestorFocused(KeyStroke stroke, String name, Runnable action) {
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, name);
		getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	private void searchFromField(boolean selectFirst) {
		String query = queryField.getText();
		boolean warnTime = timeWarnBox.isSelected();
		boolean warnCost = costWarnBox.isSelected();
		boolean warnRows = rowsWarnBox.isSelected();
		boolean warnFilter = filterWarnBox.isSelected();
		boolean warnHeap = heapWarnBox.isSelected();
		if (graphPanel == null) {
			matches = Collections.emptyList();
			currentIndex = -1;
			updateOccurrenceLabel();
			updateFieldBackground();
			return;
		}
		graphPanel.setFindHighlightQuery(query);
		matches = graphPanel.findOperatorIdsMatching(query, warnTime, warnCost, warnRows, warnFilter, warnHeap);
		if (matches.isEmpty()) {
			currentIndex = -1;
			updateOccurrenceLabel();
			updateFieldBackground();
			return;
		}
		if (selectFirst || currentIndex < 0 || currentIndex >= matches.size()) {
			currentIndex = 0;
		}
		selectCurrentMatch();
		updateOccurrenceLabel();
		updateFieldBackground();
	}

	public void findNext() {
		if (matches.isEmpty()) {
			searchFromField(true);
			return;
		}
		currentIndex = (currentIndex + 1) % matches.size();
		selectCurrentMatch();
		updateOccurrenceLabel();
	}

	public void findPrevious() {
		if (matches.isEmpty()) {
			searchFromField(true);
			return;
		}
		currentIndex = (currentIndex - 1 + matches.size()) % matches.size();
		selectCurrentMatch();
		updateOccurrenceLabel();
	}

	private void selectCurrentMatch() {
		if (graphPanel == null || currentIndex < 0 || currentIndex >= matches.size()) {
			return;
		}
		graphPanel.selectOperator(matches.get(currentIndex));
	}

	private void updateOccurrenceLabel() {
		int n = currentIndex < 0 ? 0 : currentIndex + 1;
		int m = matches.size();
		occurrenceLabel.setText("Occurrence " + n + " / " + m);
	}

	private void updateFieldBackground() {
		applyQueryFieldColors();
	}

	/**
	 * Always set {@link javax.swing.plaf.ColorUIResource} colors. Plain {@link Color}
	 * sticks across LAF/scheme swaps and leaves dark text on Light (or the reverse).
	 */
	private void applyQueryFieldColors() {
		String q = queryField.getText();
		boolean anyWarn = timeWarnBox.isSelected() || costWarnBox.isSelected() || rowsWarnBox.isSelected()
				|| filterWarnBox.isSelected() || heapWarnBox.isSelected();
		boolean searching = (q != null && !q.isEmpty()) || anyWarn;
		boolean noMatch = searching && matches.isEmpty();
		if (noMatch) {
			queryField.setBackground(UiTheme.uiColor(NO_MATCH_BG));
			queryField.setForeground(UiTheme.uiColor(NO_MATCH_FG));
			queryField.setCaretColor(NO_MATCH_FG);
			queryField.setSelectedTextColor(NO_MATCH_FG);
			queryField.setSelectionColor(new Color(0xBE, 0x12, 0x3C));
		} else {
			Color bg = UIManager.getColor("TextField.background");
			if (bg != null) {
				queryField.setBackground(UiTheme.uiColor(bg));
			}
			Color fg = UIManager.getColor("TextField.foreground");
			if (fg != null) {
				queryField.setForeground(UiTheme.uiColor(fg));
				queryField.setCaretColor(fg);
			}
			Color selBg = UIManager.getColor("TextField.selectionBackground");
			if (selBg != null) {
				queryField.setSelectionColor(selBg);
			}
			Color selFg = UIManager.getColor("TextField.selectionForeground");
			if (selFg != null) {
				queryField.setSelectedTextColor(selFg);
			}
		}
	}
}
