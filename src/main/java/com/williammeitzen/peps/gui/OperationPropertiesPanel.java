package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class OperationPropertiesPanel extends JTable {

	private static final long serialVersionUID = 1L;

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
	
	public OperationPropertiesPanel() {
		super();
	}
	
	public OperationPropertiesPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		createComponents();
	}
	
	public void createComponents() {
		DefaultTableModel model = new DefaultTableModel(data, columnNames) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		this.setModel(model);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.getColumnModel().getColumn(0).setPreferredWidth(200);
		this.getColumnModel().getColumn(1).setPreferredWidth(200);
		this.setShowGrid(true);
		this.setShowHorizontalLines(true);
		this.setShowVerticalLines(true);
		this.setGridColor(new Color(0xD1, 0xD5, 0xDB)); // light grey
		this.setIntercellSpacing(new java.awt.Dimension(1, 1));
		
		JTableHeader tableHeader = this.getTableHeader(); 
		tableHeader.setReorderingAllowed(false);
		int count = tableHeader.getMouseListeners().length;
		MouseListener mouseListeners[] = tableHeader.getMouseListeners();
		for (int i = 0; i < count; i++)
			tableHeader.removeMouseListener(mouseListeners[i]);
		
	    tableHeader.addMouseListener(new MouseAdapter() {
	    	public void mouseReleased(MouseEvent evt) {
	    		JTable table = ((JTableHeader) evt.getSource()).getTable();
	    		table.getColumnModel().getColumn(0).setResizable(true);
	    		table.getColumnModel().getColumn(1).setResizable(true);
	    	}
	    	public void mousePressed(MouseEvent evt) {
	    		int i = evt.getClickCount();
	    		if (i > 1) {
					JTable table = ((JTableHeader) evt.getSource()).getTable();
					TableColumnModel colModel = table.getColumnModel();
					Rectangle headerRect1 = table.getTableHeader().getHeaderRect(0);
					Rectangle headerRect2 = table.getTableHeader().getHeaderRect(1);
					if (tableHeader.getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
						int index = -1;
						if (evt.getX() > headerRect1.width - 10 && evt.getX() < headerRect1.width + 10) {
							index = 0;
						}
						if ((evt.getX() > headerRect2.x + headerRect2.width - 10) && (evt.getX() < headerRect2.x + headerRect2.width + 10)) {
							index = 1;
						}
						if (index == -1) return;
						
						int width = 15; // Min width
				        for (int row = 0; row < table.getRowCount(); row++) {
				            TableCellRenderer renderer = table.getCellRenderer(row, index);
				            JComponent comp = (JComponent) table.prepareRenderer(renderer, row, index);
				            width = Math.max(comp.getPreferredSize().width+5 , width);
				        }
				        colModel.getColumn(index).setResizable(false);
				        colModel.getColumn(index).setPreferredWidth(width);
					}
	    		}
	    	}
	    });

		for (int i = 0; i < count; i++)
			tableHeader.addMouseListener(mouseListeners[i]);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int viewRow = rowAtPoint(e.getPoint());
				int viewCol = columnAtPoint(e.getPoint());
				if (viewRow < 0 || planGraphPanel == null || selectedOperatorPropertyId == Integer.MIN_VALUE) {
					return;
				}
				Object property = getValueAt(viewRow, 0);
				if (property == null) {
					return;
				}
				String propertyName = property.toString();
				if (PlanGraphPanel.isExpressionListProperty(propertyName)) {
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
						}
					}
				}
			}
		});
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
		this.data = data;
		DefaultTableModel model = (DefaultTableModel) this.getModel();
		model.setDataVector(data, columnNames);
		this.getColumnModel().getColumn(0).setPreferredWidth(200);
		this.getColumnModel().getColumn(1).setPreferredWidth(200);
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
				|| valueText.contains(" worker");
	}

	private static String escapeHtml(String text) {
		return text
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

}
