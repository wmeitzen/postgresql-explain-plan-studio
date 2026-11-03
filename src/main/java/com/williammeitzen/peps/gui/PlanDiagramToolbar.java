package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.williammeitzen.peps.gui.Utils.LineThicknesses;
import com.williammeitzen.peps.gui.Utils.PercentageBy;

/**
 * Compact single-row controls for the Plan Diagram tab: Zoom, Line Thickness, Percentage By.
 */
public class PlanDiagramToolbar extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String NONE_AVAILABLE = "(none available)";

	private final JButton zoomMinusButton;
	private final JButton zoomPlusButton;
	private final JButton zoom100Button;
	private final JButton zoomFitButton;
	private final JComboBox<LineThicknesses> lineThicknessCombo;
	private final JComboBox<Object> percentageByCombo;

	private final Map<LineThicknesses, Boolean> lineThicknessEnabled = new EnumMap<>(LineThicknesses.class);
	private final Map<PercentageBy, Boolean> percentageByEnabled = new EnumMap<>(PercentageBy.class);

	private ZoomToolbarListener zoomListener;
	private LineThicknessByRadioButtonsToolbarListener lineThicknessListener;
	private PercentageByRadioButtonsToolbarListener percentageByListener;
	private boolean suppressComboEvents;
	private boolean percentageByNoneAvailable;

	public PlanDiagramToolbar() {
		super(new FlowLayout(FlowLayout.LEFT, 8, 2));
		setBorder(new EmptyBorder(2, 4, 2, 4));
		setOpaque(true);

		for (LineThicknesses value : LineThicknesses.values()) {
			lineThicknessEnabled.put(value, Boolean.TRUE);
		}
		for (PercentageBy value : PercentageBy.values()) {
			percentageByEnabled.put(value, Boolean.TRUE);
		}

		JLabel zoomLabel = new JLabel("Zoom:");
		zoomMinusButton = new JButton("-");
		zoomPlusButton = new JButton("+");
		zoom100Button = new JButton("100%");
		zoomFitButton = new JButton("Fit");
		zoomMinusButton.setMargin(new java.awt.Insets(2, 8, 2, 8));
		zoomPlusButton.setMargin(new java.awt.Insets(2, 8, 2, 8));
		zoom100Button.setMargin(new java.awt.Insets(2, 8, 2, 8));
		zoomFitButton.setMargin(new java.awt.Insets(2, 8, 2, 8));
		zoom100Button.setToolTipText("Reset zoom to the default 100% size");
		zoomFitButton.setToolTipText("Fit the entire plan diagram in the visible pane");
		zoomMinusButton.addActionListener(e -> {
			if (zoomListener != null) {
				zoomListener.zoomMinusEventOccurred();
			}
		});
		zoomPlusButton.addActionListener(e -> {
			if (zoomListener != null) {
				zoomListener.zoomPlusEventOccurred();
			}
		});
		zoom100Button.addActionListener(e -> {
			if (zoomListener != null) {
				zoomListener.zoom100EventOccurred();
			}
		});
		zoomFitButton.addActionListener(e -> {
			if (zoomListener != null) {
				zoomListener.zoomFitEventOccurred();
			}
		});

		lineThicknessCombo = new JComboBox<>(LineThicknesses.values());
		lineThicknessCombo.setRenderer(new EnumComboRenderer<>());
		lineThicknessCombo.setSelectedItem(LineThicknesses.ACTUAL_DATA_MASS);
		lineThicknessCombo.putClientProperty("lastEnabledSelection", LineThicknesses.ACTUAL_DATA_MASS);
		lineThicknessCombo.setPrototypeDisplayValue(LineThicknesses.ACTUAL_VS_ESTIMATED_MASS_RATIO);
		lineThicknessCombo.addActionListener(e -> {
			if (suppressComboEvents) {
				return;
			}
			LineThicknesses selected = (LineThicknesses) lineThicknessCombo.getSelectedItem();
			if (selected == null) {
				return;
			}
			if (!Boolean.TRUE.equals(lineThicknessEnabled.get(selected))) {
				revertLineThicknessSelection();
				return;
			}
			fireLineThickness(selected);
		});

		percentageByCombo = new JComboBox<>();
		for (PercentageBy value : PercentageBy.values()) {
			percentageByCombo.addItem(value);
		}
		percentageByCombo.setRenderer(new EnumComboRenderer<>());
		percentageByCombo.setSelectedItem(PercentageBy.COST);
		percentageByCombo.putClientProperty("lastEnabledSelection", PercentageBy.COST);
		percentageByCombo.setPrototypeDisplayValue(PercentageBy.TIME_DIFFERENCE);
		percentageByCombo.addActionListener(e -> {
			if (suppressComboEvents) {
				return;
			}
			Object selectedObj = percentageByCombo.getSelectedItem();
			if (!(selectedObj instanceof PercentageBy)) {
				return;
			}
			PercentageBy selected = (PercentageBy) selectedObj;
			if (!Boolean.TRUE.equals(percentageByEnabled.get(selected))) {
				revertPercentageBySelection();
				return;
			}
			firePercentageBy(selected);
		});

		add(zoomLabel);
		add(zoomMinusButton);
		add(zoomPlusButton);
		add(zoom100Button);
		add(zoomFitButton);
		add(new JLabel("  Line Thickness:"));
		add(lineThicknessCombo);
		add(new JLabel("  Percentage By:"));
		add(percentageByCombo);
	}

	public void setZoomToolbarListener(ZoomToolbarListener listener) {
		this.zoomListener = listener;
	}

	public void setLineThicknessListener(LineThicknessByRadioButtonsToolbarListener listener) {
		this.lineThicknessListener = listener;
	}

	public void setPercentageByListener(PercentageByRadioButtonsToolbarListener listener) {
		this.percentageByListener = listener;
	}

	public void setLineThicknessOptionEnabled(LineThicknesses option, boolean enabled) {
		lineThicknessEnabled.put(option, enabled);
		if (enabled) {
			lineThicknessCombo.setEnabled(true);
		}
		lineThicknessCombo.repaint();
	}

	public void setPercentageByOptionEnabled(PercentageBy option, boolean enabled) {
		percentageByEnabled.put(option, enabled);
		percentageByCombo.repaint();
	}

	public void setAllDiagramOptionsEnabled(boolean enabled) {
		for (LineThicknesses value : LineThicknesses.values()) {
			lineThicknessEnabled.put(value, enabled);
		}
		for (PercentageBy value : PercentageBy.values()) {
			percentageByEnabled.put(value, enabled);
		}
		lineThicknessCombo.setEnabled(enabled);
		refreshPercentageByAvailability();
		lineThicknessCombo.repaint();
	}

	/** Re-enable the combo widgets after a plan diagram has data (options may still be greyed individually). */
	public void setCombosEnabled(boolean enabled) {
		lineThicknessCombo.setEnabled(enabled);
		if (!enabled) {
			percentageByCombo.setEnabled(false);
		} else {
			refreshPercentageByAvailability();
		}
	}

	/**
	 * Rebuild Percentage By so it lists real options when any metric exists, or
	 * shows "(none available)" when costs and timing are both off.
	 */
	public void refreshPercentageByAvailability() {
		suppressComboEvents = true;
		try {
			boolean anyEnabled = false;
			for (PercentageBy value : PercentageBy.values()) {
				if (Boolean.TRUE.equals(percentageByEnabled.get(value))) {
					anyEnabled = true;
					break;
				}
			}
			percentageByNoneAvailable = !anyEnabled;
			Object previous = percentageByCombo.getSelectedItem();
			percentageByCombo.removeAllItems();
			if (!anyEnabled) {
				percentageByCombo.addItem(NONE_AVAILABLE);
				percentageByCombo.setSelectedItem(NONE_AVAILABLE);
				percentageByCombo.setEnabled(false);
				return;
			}
			for (PercentageBy value : PercentageBy.values()) {
				percentageByCombo.addItem(value);
			}
			percentageByCombo.setEnabled(true);
			PercentageBy restore = null;
			if (previous instanceof PercentageBy
					&& Boolean.TRUE.equals(percentageByEnabled.get(previous))) {
				restore = (PercentageBy) previous;
			} else {
				Object prior = percentageByCombo.getClientProperty("lastEnabledSelection");
				if (prior instanceof PercentageBy
						&& Boolean.TRUE.equals(percentageByEnabled.get(prior))) {
					restore = (PercentageBy) prior;
				}
			}
			if (restore == null) {
				for (PercentageBy value : PercentageBy.values()) {
					if (Boolean.TRUE.equals(percentageByEnabled.get(value))) {
						restore = value;
						break;
					}
				}
			}
			if (restore != null) {
				percentageByCombo.setSelectedItem(restore);
				percentageByCombo.putClientProperty("lastEnabledSelection", restore);
			}
		} finally {
			suppressComboEvents = false;
		}
		percentageByCombo.repaint();
	}

	public boolean isPercentageByNoneAvailable() {
		return percentageByNoneAvailable;
	}

	public void setSelectedLineThickness(LineThicknesses value) {
		suppressComboEvents = true;
		try {
			lineThicknessCombo.setSelectedItem(value);
			if (value != null && Boolean.TRUE.equals(lineThicknessEnabled.get(value))) {
				lineThicknessCombo.putClientProperty("lastEnabledSelection", value);
			}
		} finally {
			suppressComboEvents = false;
		}
	}

	public void setSelectedPercentageBy(PercentageBy value) {
		if (percentageByNoneAvailable) {
			return;
		}
		suppressComboEvents = true;
		try {
			percentageByCombo.setSelectedItem(value);
			if (value != null && Boolean.TRUE.equals(percentageByEnabled.get(value))) {
				percentageByCombo.putClientProperty("lastEnabledSelection", value);
			}
		} finally {
			suppressComboEvents = false;
		}
	}

	/** Select an option and notify listeners (used when falling back from a disabled mode). */
	public void selectAndFireLineThickness(LineThicknesses value) {
		setSelectedLineThickness(value);
		fireLineThickness(value);
	}

	public void selectAndFirePercentageBy(PercentageBy value) {
		setSelectedPercentageBy(value);
		firePercentageBy(value);
	}

	private void revertLineThicknessSelection() {
		LineThicknesses current = null;
		for (LineThicknesses value : LineThicknesses.values()) {
			if (Boolean.TRUE.equals(lineThicknessEnabled.get(value))) {
				// Prefer keeping prior enabled selection if still selected elsewhere isn't available;
				// fall back to first enabled option.
				if (current == null) {
					current = value;
				}
			}
		}
		Object prior = lineThicknessCombo.getClientProperty("lastEnabledSelection");
		if (prior instanceof LineThicknesses
				&& Boolean.TRUE.equals(lineThicknessEnabled.get(prior))) {
			current = (LineThicknesses) prior;
		}
		if (current != null) {
			setSelectedLineThickness(current);
		}
	}

	private void revertPercentageBySelection() {
		if (percentageByNoneAvailable) {
			suppressComboEvents = true;
			try {
				percentageByCombo.setSelectedItem(NONE_AVAILABLE);
			} finally {
				suppressComboEvents = false;
			}
			return;
		}
		PercentageBy current = null;
		for (PercentageBy value : PercentageBy.values()) {
			if (Boolean.TRUE.equals(percentageByEnabled.get(value))) {
				if (current == null) {
					current = value;
				}
			}
		}
		Object prior = percentageByCombo.getClientProperty("lastEnabledSelection");
		if (prior instanceof PercentageBy
				&& Boolean.TRUE.equals(percentageByEnabled.get(prior))) {
			current = (PercentageBy) prior;
		}
		if (current != null) {
			setSelectedPercentageBy(current);
		}
	}

	private void fireLineThickness(LineThicknesses selected) {
		if (lineThicknessListener == null || selected == null) {
			return;
		}
		lineThicknessCombo.putClientProperty("lastEnabledSelection", selected);
		switch (selected) {
		case ACTUAL_DATA_MASS:
			lineThicknessListener.lineThicknessByActualMassEventOccurred();
			break;
		case ESTIMATED_DATA_MASS:
			lineThicknessListener.lineThicknessByEstimatedMassEventOccurred();
			break;
		case ACTUAL_ROWS:
			lineThicknessListener.lineThicknessByActualRowsEventOccurred();
			break;
		case ESTIMATED_ROWS:
			lineThicknessListener.lineThicknessByEstimatedRowsEventOccurred();
			break;
		case ACTUAL_VS_ESTIMATED_ROWS_RATIO:
			lineThicknessListener.lineThicknessByActualVsEstimatedRowsRatioEventOccurred();
			break;
		case ACTUAL_VS_ESTIMATED_MASS_RATIO:
			lineThicknessListener.lineThicknessByActualVsEstimatedMassRatioEventOccurred();
			break;
		default:
			break;
		}
	}

	private void firePercentageBy(PercentageBy selected) {
		if (percentageByListener == null || selected == null) {
			return;
		}
		percentageByCombo.putClientProperty("lastEnabledSelection", selected);
		switch (selected) {
		case COST:
			percentageByListener.percentageByCostEventOccurred();
			break;
		case EXCLUSIVE_TIME:
			percentageByListener.percentageByExclusiveTimeEventOccurred();
			break;
		case TIME_DIFFERENCE:
			percentageByListener.percentageByTimeDifferenceEventOccurred();
			break;
		default:
			break;
		}
	}

	static String displayName(LineThicknesses value) {
		if (value == null) {
			return "";
		}
		switch (value) {
		case ACTUAL_DATA_MASS:
			return "Actual Mass";
		case ESTIMATED_DATA_MASS:
			return "Estimated Mass";
		case ACTUAL_ROWS:
			return "Actual Rows";
		case ESTIMATED_ROWS:
			return "Estimated Rows";
		case ACTUAL_VS_ESTIMATED_ROWS_RATIO:
			return "Actual vs Estimated Rows Ratio";
		case ACTUAL_VS_ESTIMATED_MASS_RATIO:
			return "Actual vs Estimated Mass Ratio";
		default:
			return value.name();
		}
	}

	static String displayName(PercentageBy value) {
		if (value == null) {
			return "";
		}
		switch (value) {
		case COST:
			return "Cost";
		case EXCLUSIVE_TIME:
			return "Operator Time";
		case TIME_DIFFERENCE:
			return "Time Difference";
		default:
			return value.name();
		}
	}

	private final class EnumComboRenderer<E> extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			boolean enabled = true;
			if (value instanceof LineThicknesses) {
				setText(displayName((LineThicknesses) value));
				enabled = Boolean.TRUE.equals(lineThicknessEnabled.get(value));
			} else if (value instanceof PercentageBy) {
				setText(displayName((PercentageBy) value));
				enabled = Boolean.TRUE.equals(percentageByEnabled.get(value));
			} else if (NONE_AVAILABLE.equals(value)) {
				setText(NONE_AVAILABLE);
				enabled = false;
			}
			if (!enabled) {
				setForeground(Color.GRAY);
				setFont(getFont().deriveFont(Font.ITALIC));
			}
			return c;
		}
	}
}
