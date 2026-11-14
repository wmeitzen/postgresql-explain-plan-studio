package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import java.awt.Dimension;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class OperationPropertiesPanel extends JTable {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_COLUMN_WIDTH = 200;
	private static final int MIN_COLUMN_WIDTH = 40;

	private PlanGraphPanel planGraphPanel = null;
	private Object[] columnNames = { "Property", "Value" };
	private Object[][] data;
	private int selectedOperatorPropertyId = Integer.MIN_VALUE;
	private final Set<String> expandedLists = new HashSet<>();
	private boolean workersExpanded = false;
	/** When true, expandable lists/Workers stay expanded across operator clicks. */
	private boolean expandAll = false;
	private Consumer<String> notesListener = null;
	private Consumer<Boolean> expandAllListener = null;
	/** Remember Property/Value widths across model refreshes (expand all, operator clicks). */
	private int propertyColumnWidth = DEFAULT_COLUMN_WIDTH;
	private int valueColumnWidth = DEFAULT_COLUMN_WIDTH;

	public OperationPropertiesPanel() {
		super();
	}

	public OperationPropertiesPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		createComponents();
	}

	public void setPlanGraphPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		selectedOperatorPropertyId = Integer.MIN_VALUE;
		expandedLists.clear();
		workersExpanded = false;
		if (getModel() instanceof DefaultTableModel) {
			((DefaultTableModel) getModel()).setDataVector(new Object[0][0], columnNames);
			applyColumnWidths(DEFAULT_COLUMN_WIDTH, DEFAULT_COLUMN_WIDTH);
		}
		if (notesListener != null) {
			notesListener.accept("");
		}
	}

	public void createComponents() {
		DefaultTableModel model = new DefaultTableModel(data, columnNames) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return OperationPropertiesPanel.this.isValueCellSelectable(row, column);
			}

			@Override
			public void setValueAt(Object aValue, int row, int column) {
				// Values are plan data — allow select/copy in the editor, but do not persist edits.
			}
		};
		this.setModel(model);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.setAutoCreateColumnsFromModel(true);
		applyColumnWidths(propertyColumnWidth, valueColumnWidth);
		this.setShowGrid(true);
		this.setShowHorizontalLines(true);
		this.setShowVerticalLines(true);
		this.setGridColor(new Color(0xD1, 0xD5, 0xDB)); // light grey
		this.setIntercellSpacing(new java.awt.Dimension(1, 1));

		// Single click enters the value cell so the user can select/copy text.
		JTextField valueField = new JTextField();
		DefaultCellEditor valueEditor = new DefaultCellEditor(valueField) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
					int row, int column) {
				Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
				if (c instanceof JTextField) {
					JTextField field = (JTextField) c;
					field.setEditable(true);
					// Defer select-all so caret placement from the click does not clear it.
					javax.swing.SwingUtilities.invokeLater(field::selectAll);
				}
				return c;
			}
		};
		valueEditor.setClickCountToStart(1);
		this.getColumnModel().getColumn(1).setCellEditor(valueEditor);

		installColumnResizeSupport();

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int viewRow = rowAtPoint(e.getPoint());
				int viewCol = columnAtPoint(e.getPoint());
				if (viewRow < 0 || planGraphPanel == null || selectedOperatorPropertyId == Integer.MIN_VALUE) {
					return;
				}
				// Let the value-cell editor handle clicks on selectable values.
				if (viewCol == 1 && isValueCellSelectable(viewRow, 1)) {
					return;
				}
				Object property = getValueAt(viewRow, 0);
				if (property == null) {
					return;
				}
				String propertyName = property.toString();
				if (PlanGraphPanel.isExpressionListProperty(propertyName)
						|| PlanGraphPanel.isSortGroupProperty(propertyName)) {
					toggleExpandedList(propertyName);
				} else if ("Workers".equals(propertyName)) {
					toggleWorkersExpanded();
				} else if (viewCol == 1) {
					Object value = getValueAt(viewRow, 1);
					if (value != null) {
						String valueText = value.toString();
						if ((valueText.startsWith("▼ ") || valueText.startsWith("▲ "))
								&& valueText.contains(" worker")) {
							toggleWorkersExpanded();
						} else if ((valueText.startsWith("▼ ") || valueText.startsWith("▲ "))
								&& valueText.contains(" group")
								&& PlanGraphPanel.isSortGroupProperty(propertyName)) {
							toggleExpandedList(propertyName);
						}
					}
				}
			}
		});
	}

	/**
	 * Keep both columns freely resizable (no strip of LAF header listeners), remember
	 * widths, and revalidate ancestors so the outer scroll pane can grow/shrink.
	 */
	private void installColumnResizeSupport() {
		JTableHeader tableHeader = this.getTableHeader();
		tableHeader.setReorderingAllowed(false);

		// Double-click near a column edge → pack that column to content (does not remove LAF listeners).
		tableHeader.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (evt.getClickCount() <= 1) {
					return;
				}
				if (tableHeader.getCursor() != Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
					return;
				}
				TableColumnModel colModel = getColumnModel();
				Rectangle headerRect0 = tableHeader.getHeaderRect(0);
				Rectangle headerRect1 = tableHeader.getHeaderRect(1);
				int index = -1;
				if (evt.getX() > headerRect0.width - 10 && evt.getX() < headerRect0.width + 10) {
					index = 0;
				} else if (evt.getX() > headerRect1.x + headerRect1.width - 10
						&& evt.getX() < headerRect1.x + headerRect1.width + 10) {
					index = 1;
				}
				if (index < 0) {
					return;
				}
				int width = MIN_COLUMN_WIDTH;
				for (int row = 0; row < getRowCount(); row++) {
					TableCellRenderer renderer = getCellRenderer(row, index);
					Component comp = prepareRenderer(renderer, row, index);
					width = Math.max(comp.getPreferredSize().width + 8, width);
				}
				TableColumn col = colModel.getColumn(index);
				col.setMinWidth(MIN_COLUMN_WIDTH);
				col.setMaxWidth(Integer.MAX_VALUE);
				col.setPreferredWidth(width);
				rememberColumnWidths();
				revalidateAncestors();
			}
		});

		getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnMarginChanged(ChangeEvent e) {
				rememberColumnWidths();
				revalidateAncestors();
			}

			@Override
			public void columnAdded(TableColumnModelEvent e) {
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
			}
		});
	}

	private void rememberColumnWidths() {
		if (getColumnCount() < 2) {
			return;
		}
		propertyColumnWidth = Math.max(MIN_COLUMN_WIDTH, getColumnModel().getColumn(0).getPreferredWidth());
		valueColumnWidth = Math.max(MIN_COLUMN_WIDTH, getColumnModel().getColumn(1).getPreferredWidth());
	}

	private void applyColumnWidths(int propertyWidth, int valueWidth) {
		if (getColumnCount() < 2) {
			return;
		}
		propertyColumnWidth = Math.max(MIN_COLUMN_WIDTH, propertyWidth);
		valueColumnWidth = Math.max(MIN_COLUMN_WIDTH, valueWidth);
		TableColumn propertyCol = getColumnModel().getColumn(0);
		TableColumn valueCol = getColumnModel().getColumn(1);
		for (TableColumn col : new TableColumn[] { propertyCol, valueCol }) {
			col.setMinWidth(MIN_COLUMN_WIDTH);
			col.setMaxWidth(Integer.MAX_VALUE);
			col.setResizable(true);
		}
		propertyCol.setPreferredWidth(propertyColumnWidth);
		valueCol.setPreferredWidth(valueColumnWidth);
		// Re-attach value editor after setDataVector recreates columns.
		if (valueCol.getCellEditor() == null) {
			JTextField valueField = new JTextField();
			DefaultCellEditor valueEditor = new DefaultCellEditor(valueField) {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
						int row, int column) {
					Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
					if (c instanceof JTextField) {
						JTextField field = (JTextField) c;
						javax.swing.SwingUtilities.invokeLater(field::selectAll);
					}
					return c;
				}
			};
			valueEditor.setClickCountToStart(1);
			valueCol.setCellEditor(valueEditor);
		}
	}

	private void revalidateAncestors() {
		revalidate();
		repaint();
		Container parent = getParent();
		while (parent != null) {
			parent.revalidate();
			parent.repaint();
			parent = parent.getParent();
			if (parent instanceof javax.swing.JScrollPane) {
				break;
			}
		}
	}

	/**
	 * Value cells that hold real plan text are selectable for copy. Expandable headers
	 * (Output / Keys / Workers / groups) and empty values are not.
	 */
	boolean isValueCellSelectable(int row, int column) {
		if (column != 1 || row < 0 || row >= getRowCount()) {
			return false;
		}
		Object value = getValueAt(row, 1);
		if (value == null) {
			return false;
		}
		String valueText = value.toString();
		if (valueText.isBlank()) {
			return false;
		}
		if (isExpandableSummaryValue(valueText)) {
			return false;
		}
		Object property = getValueAt(row, 0);
		if (property != null) {
			String propertyName = property.toString();
			if (PlanGraphPanel.isExpressionListProperty(propertyName)
					|| PlanGraphPanel.isSortGroupProperty(propertyName)
					|| "Workers".equals(propertyName)) {
				return false;
			}
		}
		return true;
	}

	private void toggleExpandedList(String propertyName) {
		if (expandedLists.contains(propertyName)) {
			expandedLists.remove(propertyName);
			clearExpandAllFlag();
		} else {
			expandedLists.add(propertyName);
		}
		refreshFromGraph();
	}

	private void toggleWorkersExpanded() {
		workersExpanded = !workersExpanded;
		if (!workersExpanded) {
			clearExpandAllFlag();
		}
		refreshFromGraph();
	}

	private void applyExpandAll() {
		expandedLists.clear();
		for (String name : PlanGraphPanel.EXPRESSION_LIST_PROPERTIES) {
			expandedLists.add(name);
		}
		for (String name : PlanGraphPanel.SORT_GROUP_PROPERTIES) {
			expandedLists.add(name);
		}
		workersExpanded = true;
	}

	private void clearExpandAllFlag() {
		if (!expandAll) {
			return;
		}
		expandAll = false;
		if (expandAllListener != null) {
			expandAllListener.accept(false);
		}
	}

	public boolean isExpandAll() {
		return expandAll;
	}

	public void setExpandAll(boolean expandAll) {
		if (this.expandAll == expandAll) {
			return;
		}
		this.expandAll = expandAll;
		if (expandAll) {
			applyExpandAll();
		} else {
			expandedLists.clear();
			workersExpanded = false;
		}
		refreshFromGraph();
	}

	public void setNotesListener(Consumer<String> notesListener) {
		this.notesListener = notesListener;
	}

	public void setExpandAllListener(Consumer<Boolean> expandAllListener) {
		this.expandAllListener = expandAllListener;
	}

	public void showOperatorProperties(int operatorPropertyId) {
		if (selectedOperatorPropertyId != operatorPropertyId) {
			selectedOperatorPropertyId = operatorPropertyId;
			if (expandAll) {
				applyExpandAll();
			} else {
				expandedLists.clear();
				workersExpanded = false;
			}
		}
		refreshFromGraph();
	}

	private void refreshFromGraph() {
		if (planGraphPanel == null || selectedOperatorPropertyId == Integer.MIN_VALUE) {
			return;
		}
		Object[][] operationData = planGraphPanel.getOperatorProperties(selectedOperatorPropertyId, expandedLists, workersExpanded);
		setPropertiesDetails(operationData);
		if (notesListener != null) {
			notesListener.accept(planGraphPanel.getOperatorNotes(selectedOperatorPropertyId));
		}
	}

	public void setPropertiesDetails(Object[][] data) {
		boolean bolShowFunctionName = false;
		if (bolShowFunctionName == true) { System.out.println("PlanPropertiesPanel.setPropertiesDetails(Object[][] data)"); }
		rememberColumnWidths();
		this.data = data;
		DefaultTableModel model = (DefaultTableModel) this.getModel();
		model.setDataVector(data, columnNames);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		applyColumnWidths(propertyColumnWidth, valueColumnWidth);
		revalidateAncestors();
	}

	/**
	 * Do not let BorderLayout/BoxLayout stretch this table wider than its columns.
	 * Stretching made the Property/Value divider feel capped by the other column's width.
	 */
	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		int width = 0;
		TableColumnModel cols = getColumnModel();
		for (int i = 0; i < cols.getColumnCount(); i++) {
			width += cols.getColumn(i).getPreferredWidth();
		}
		width += getIntercellSpacing().width * Math.max(0, cols.getColumnCount());
		if (getRowCount() == 0) {
			width = Math.max(width, propertyColumnWidth + valueColumnWidth);
		}
		d.width = Math.max(width, MIN_COLUMN_WIDTH * 2);
		return d;
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(MIN_COLUMN_WIDTH * 2, super.getMinimumSize().height);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component component = super.prepareRenderer(renderer, row, column);
		if (column == 1 && component instanceof JLabel) {
			Object value = getValueAt(row, column);
			if (value != null) {
				String text = value.toString();
				if (isExpandableSummaryValue(text)) {
					int paren = text.indexOf('(');
					if (paren >= 0) {
						String caret = text.substring(0, paren);
						String summary = text.substring(paren);
						((JLabel) component).setText("<html>" + escapeHtml(caret)
								+ "<i>" + escapeHtml(summary) + "</i></html>");
					}
				}
			}
		}
		return component;
	}

	static boolean isExpandableSummaryValue(String valueText) {
		if (valueText == null) {
			return false;
		}
		if (!(valueText.startsWith("▼ ") || valueText.startsWith("▲ "))) {
			return false;
		}
		return valueText.contains(" column")
				|| valueText.contains(" key")
				|| valueText.contains(" worker")
				|| valueText.contains(" group");
	}

	private static String escapeHtml(String text) {
		return text
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

}
