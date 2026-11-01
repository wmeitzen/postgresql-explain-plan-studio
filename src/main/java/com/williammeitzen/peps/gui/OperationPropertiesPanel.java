package com.williammeitzen.peps.gui;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class OperationPropertiesPanel extends JTable {

	private PlanGraphPanel planGraphPanel = null;
	private Object[] columnNames = { "Property", "Value" };
	private Object[][] data;
	
	public OperationPropertiesPanel() {
		super();
	}
	
	public OperationPropertiesPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		createComponents();
	}
	
	public void createComponents() {
		DefaultTableModel model = new DefaultTableModel(data, columnNames);
//		Dimension dim = getPreferredSize();
//		setPreferredSize(dim);
//		setMinimumSize(dim);
		this.setModel(model);
//		propertiesTable.setPreferredScrollableViewportSize(dim);
//		propertiesTable.setPreferredSize(dim);
//		propertiesTable.setMinimumSize(dim);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		propertiesTable.setFillsViewportHeight(true);
		this.getColumnModel().getColumn(0).setPreferredWidth(200);
		this.getColumnModel().getColumn(1).setPreferredWidth(200);
		
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
//				        if(width > 400)
//				            width = 400;
				        colModel.getColumn(index).setResizable(false);
				        colModel.getColumn(index).setPreferredWidth(width);
//				        dim.width = colModel.getColumn(0).getPreferredWidth() + colModel.getColumn(1).getPreferredWidth();
//				        if (dim.width < 400)
//				        	dim.width = 400;
//						propertiesTable.setPreferredScrollableViewportSize(dim);
//						propertiesTable.getParent().repaint();
//						scrollPane.repaint();
					}
	    		}
	    	}
	    });

		for (int i = 0; i < count; i++)
			tableHeader.addMouseListener(mouseListeners[i]);
		
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
	
}
