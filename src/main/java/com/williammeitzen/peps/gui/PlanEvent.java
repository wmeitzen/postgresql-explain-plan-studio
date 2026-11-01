package com.williammeitzen.peps.gui;

//import java.awt.BorderLayout;
//import java.awt.GridBagConstraints;
//import java.awt.Insets;

//import javax.swing.BorderFactory;
//import javax.swing.JPanel;
//import javax.swing.JScrollBar;
//import javax.swing.JScrollPane;
//import javax.swing.JTabbedPane;

//import org.executequery.Constants;

//import model.Database;

/**
 * Used to pair source and graph panel
 */ 
public class PlanEvent {

	private String planName;
	private PlanSourcePanel planSourcePanel;
	private PlanGraphPanel planGraphPanel;
	//private Database db;

	//public PlanEvent(String planName, PlanSourcePanel pSourcePanel, PlanGraphPanel pGraphPanel) {
	public PlanEvent(PlanSourcePanel pSourcePanel, PlanGraphPanel pGraphPanel) {
		this.planSourcePanel = pSourcePanel;
		this.planGraphPanel = pGraphPanel;
	}

	public String getPlanName() {
		return planName;
	}
	
	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public PlanSourcePanel getPlanSourcePanel() {
		return planSourcePanel;
	}

	public PlanGraphPanel getPlanGraphPanel() {
		return planGraphPanel;
	}
}
