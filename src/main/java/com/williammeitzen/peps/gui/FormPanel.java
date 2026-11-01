package com.williammeitzen.peps.gui;

import java.awt.Color;
//import java.awt.BorderLayout;
import java.awt.Component;
//import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
//import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
//import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
//import javax.swing.ListSelectionModel;
//import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.williammeitzen.peps.gui.Utils.LineThicknesses;

//import com.github.cliftonlabs.json_simple.JsonException;
//import com.github.cliftonlabs.json_simple.JsonArray;
//import com.github.cliftonlabs.json_simple.JsonObject;
//import com.github.cliftonlabs.json_simple.JsonKey;


import com.williammeitzen.peps.model.Database;

public class FormPanel extends JPanel {

	private JButton addPlanBtn;
	private JButton deletePlanBtn;
	private JButton duplicatePlanBtn;
	//private FormListener formListener;
	private JList planList;
	private JTabbedPane tabPaneToLink;
	private JScrollPane planListScrollPane;
	private String PLAN_LIST_FONT_NAME = "Arial";
	private int PLAN_LIST_FONT_STYLE = Font.PLAIN;
	private int PLAN_LIST_FONT_SIZE = 12;
	
	public Map<String, PlanEvent> plansMapper = new TreeMap<String, PlanEvent>();
	private PlanEvent planEvent;

	public FormPanel(JTabbedPane tabPane) {
		
		tabPaneToLink = tabPane;
		Dimension dim = getPreferredSize();
		//System.out.println(dim);
		dim.width = 150;
		setPreferredSize(dim);
		setMinimumSize(dim);
		String strSQL;
		JsonArray jsnRows;
		//String strPlanName;
		//JsonObject jsnObject;
		//db = new Database();
		JsonObject jsnRow;

		planList = new JList();
		DefaultListModel planModel = new DefaultListModel();
/*
		for (String planName : plansMapper.keySet()) {
			planModel.addElement(planName);
		}
*/
		//dim.setFont(new Font("Verdana", Font.PLAIN, 10));
		planList.setModel(planModel);
		planList.setFont(new Font(PLAN_LIST_FONT_NAME, PLAN_LIST_FONT_STYLE, PLAN_LIST_FONT_SIZE));

		// erase execution time, plan cost, etc. from generated plan data
		strSQL = "begin transaction;\n";
		strSQL += "delete from explain_plan_generated_values;\n";
		strSQL += "delete from explain_plan_operator_annotations;\n";
		strSQL += "delete from explain_plan_operator_outputs;\n";
		strSQL += "delete from explain_plan_operator_show_workers;\n";
		strSQL += "delete from explain_plan_operator_workers;\n";
		strSQL += "delete from explain_plan_properties;\n";
		strSQL += "commit transaction;\n";
		Database.dml(strSQL);
		
		// retrieve plans from db
		strSQL = "select id_explain_plan, plan_name, server, port, database, username, password, remember_password\n"
				+ ", pre_explain_command, explain_sql_command, post_explain_command, plan_text\n"
				+ "from explain_plans\n"
				+ "order by lower(plan_name);\n";
		jsnRows = Database.rows(strSQL);
		for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
			jsnRow = (JsonObject) jsnRows.get(intRow);

			//String newPlanName = "Plan " + nextSeq;
			String newPlanName = jsnRow.getString("plan_name");
			
			DefaultListModel baseModel = (DefaultListModel) planList.getModel();
			baseModel.addElement(newPlanName);
			planList.setSelectedIndex(baseModel.size()-1);

			PlanSourcePanel newPlanSourcePanel = new PlanSourcePanel(tabPane);
			PlanGraphPanel newPlanGraphPanel = new PlanGraphPanel(tabPane);
			//PlanEvent pEvent = new PlanEvent(newPlanSourcePanel, new PlanGraphPanel(tabPane));
			PlanEvent pEvent = new PlanEvent(newPlanSourcePanel, newPlanGraphPanel);
			//addPlanNameToDatabase(newPlanName, newPlanSourcePanel, newPlanGraphPanel);
			//newPlanSourcePanel.setJsonText(planSource4JsonText);
			newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
			newPlanGraphPanel.setIDExplainPlan(newPlanName);
			//newPlanSourcePanel.setPlanIsDirty(true);

			plansMapper.put(newPlanName, pEvent);
			//plansMapper.put(pEvent);
			
			tabPaneToLink.removeAll();
			
			tabPaneToLink.addTab(getPlanSourceTabName(newPlanName), pEvent.getPlanSourcePanel());
			
			// add "Plan Statistics" tab
			// by stas
			PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
			JScrollPane statisticsPane = new JScrollPane(statisticsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			//statisticsPane.setMinimumSize(new Dimension(320, 480));
			statisticsPane.setPreferredSize(new Dimension(320, 480));
			tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);

			// during population, get stats only on the last row retrieved
			if (intRow == jsnRows.size() - 1)
				PlanStatisticsPanel.setPlanNamesDelimited("'" + newPlanName.replace("'", "''") + "'");
			
		    // see if the user chose the Compare Plans tab
		    tabPaneToLink.addChangeListener(new ChangeListener() {
	            int COMPARE_PLANS_TAB = 1;
		        public void stateChanged(ChangeEvent e) {
		            //System.out.println("(1) Tab: " + tabPaneToLink.getSelectedIndex());
		            if (tabPaneToLink.getSelectedIndex() == COMPARE_PLANS_TAB) {
		            	//PlanStatisticsPanel.setReadStatisticsDataFlag(true);
		            	statisticsPanel.setReadStatisticsDataFlag(true);
		            	statisticsPanel.createComponents();
		            }
		        }
			});

		    // 1. create the scrollpanel as usual
			//JScrollPane planGraphScrollPane = new JScrollPane(pEvent.getPlanGraphPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		    JScrollPane planGraphScrollPane_1 = new JScrollPane(pEvent.getPlanGraphPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			//planGraphScrollPane.setPreferredSize(new Dimension(320, 480));
		    planGraphScrollPane_1.setPreferredSize(new Dimension(320, 480));
			
		    // 2. create the toolbars here with the split
		    JSplitPane toolbarPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new Toolbars(), planGraphScrollPane_1);
		    //JSplitPane toolbarPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new Toolbars(), planGraphScrollPane_1);
		    toolbarPanel.setOneTouchExpandable(true);
		    //toolbarPanel.setDividerLocation(100); // nope
		    toolbarPanel.setDividerLocation(300); // nope
		    //toolbarPanel.setDividerLocation(500); // nope
		    //toolbarPanel.setDividerLocation(1000); // nope
		    
		    // 3. show operator properties (no change)
			// show operator properties
			OperationPropertiesPanel operationPropertiesPanel = new OperationPropertiesPanel(pEvent.getPlanGraphPanel());
			JScrollPane propertiesPanelScrollPane = new JScrollPane(operationPropertiesPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			propertiesPanelScrollPane.setPreferredSize(new Dimension(520, 680));
			
			// 4. for horizontal split - pass in toolbarPanel
			//for horizontal split
			//JSplitPane graphAndPropertiesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, planGraphScrollPane, propertiesPanelScrollPane);
			JSplitPane graphAndPropertiesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, toolbarPanel, propertiesPanelScrollPane);
			graphAndPropertiesSplitPane.setOneTouchExpandable(true);
			graphAndPropertiesSplitPane.setDividerLocation(600);
			//graphAndPropertiesSplitPane.setDividerLocation(propertiesPanelScrollPane.getWidth() / 2);
			
			// changes by Akila
			SliderPanel sliderPanel = new SliderPanel(pEvent.getPlanGraphPanel());
			//sliderPanel.setSliderType(sliderType);
			
			JScrollPane sliderPanelScrollPane = new JScrollPane(sliderPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			
			//for vertical split
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphAndPropertiesSplitPane, sliderPanelScrollPane);
			//splitPane.setDividerLocation(500);
			splitPane.setDividerLocation(600);
			splitPane.setOneTouchExpandable(true);
			
			tabPaneToLink.addTab(getPlanGraphTabName(newPlanName), splitPane);
			
			//strSQL = "select id_explain_plan, plan_name, server, port, database, username, password\n"
			//		+ ", pre_explain_command, explain_sql_command, post_explain_command, plan_text\n"
			//		+ "from explain_plans;\n";
			//newPlanSourcePanel.setIdExplainPlan(jsnRow.getInteger("id_explain_plan"));
			newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(jsnRow.getString("plan_name"));
			newPlanSourcePanel.setPlanSourceFromQueryServerField(jsnRow.getString("server"));
			newPlanSourcePanel.setPlanSourceFromQueryPortField(jsnRow.getString("port"));
			newPlanSourcePanel.setPlanSourceFromQueryDatabaseField(jsnRow.getString("database"));
			newPlanSourcePanel.setPlanSourceFromQueryUserField(jsnRow.getString("username"));
			newPlanSourcePanel.setPlanSourceFromQueryRememberPasswordCheckbox(Utils.objectToBoolean(jsnRow.getString("remember_password")));
			if (Utils.objectToBoolean(jsnRow.getString("remember_password")) == true) {
				newPlanSourcePanel.setPlanSourceFromQueryPasswordField(jsnRow.getString("password"));
			} else {
				newPlanSourcePanel.setPlanSourceFromQueryPasswordField("");
			}
			newPlanSourcePanel.setPlanSourceFromQueryPreCommandField(jsnRow.getString("pre_explain_command"));
			newPlanSourcePanel.setSQLQueryText(jsnRow.getString("explain_sql_command"));
			newPlanSourcePanel.setPlanSourceFromQueryPostCommandField(jsnRow.getString("post_explain_command"));
			newPlanSourcePanel.setJsonPlanText(jsnRow.getString("plan_text"));

			//newPlanGraphPanel.setIDExplainPlan(jsnRow.getString("plan_name"));
			newPlanGraphPanel.setIDExplainPlan(newPlanName);
			
			// disable the graph tab until we generate a plan from a query or JSON
			tabPaneToLink.setEnabledAt(2, false);
		}
		
		addPlanBtn = new JButton("Add Plan");
		deletePlanBtn = new JButton("Delete Plan(s)");
		if (planList.getSelectedIndices().length == 0)
			deletePlanBtn.setEnabled(false); // disable until a plan is selected or is loaded from the sqlite db
		duplicatePlanBtn = new JButton("Duplicate Plan(s)");
		if (planList.getSelectedIndices().length == 0)
			duplicatePlanBtn.setEnabled(false); // disable until a plan is selected or is loaded from the sqlite db
		
		planListScrollPane = new JScrollPane(planList);
		planListScrollPane.setMinimumSize(new Dimension(140, 300));
		planListScrollPane.setPreferredSize(new Dimension(140, 300));
		
		//planList.setPreferredSize(new Dimension(110, 100));
		planList.setBorder(BorderFactory.createEtchedBorder());
		
		planList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				boolean bolShowProperties = false;
				Double fltPlanTotalTimeMs;
				String strSQL;

				if (planList.getSelectedIndices().length == 0 )
					return;
				String selectedPlanName = (String) planList.getSelectedValue();

				if (bolShowProperties == true) { System.out.println("FormPanel selectedPlanName = " + selectedPlanName); }

				deletePlanBtn.setEnabled(true);
				duplicatePlanBtn.setEnabled(true);
				tabPaneToLink.removeAll();

				PlanEvent pEvent = plansMapper.get(selectedPlanName);
				
				if (bolShowProperties == true) { System.out.println("FormPanel pEvent = " + pEvent); }

				if (pEvent != null) {
					
					tabPaneToLink.addTab(getPlanSourceTabName(selectedPlanName), pEvent.getPlanSourcePanel());
					
					// add "Plan Statistics" tab
					// by stas
					//PlanStatisticsPanel statisticsPanel = PlanStatisticsPanel.getInstance();
					PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
					JScrollPane statisticsPane = new JScrollPane(statisticsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
					//statisicsPane.setMinimumSize(new Dimension(320, 480));
					statisticsPane.setPreferredSize(new Dimension(320, 480));
					tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);

					int rows = planList.getSelectedIndices().length;
				    //String data[][] = new String[rows][2];
					String strPlanNamesDelimted = "";
				    DefaultListModel baseModel = (DefaultListModel) planList.getModel();
				    for (int i = 0; i < planList.getSelectedIndices().length; i++) {
				    	if (i > 0)
				    		strPlanNamesDelimted += ", ";
			    		//strPlanNamesDelimted += "'" + planList.getSelectedIndices()[i] + "'";
				    	String strPlanNameDelimited = (String) (baseModel.getElementAt(planList.getSelectedIndices()[i]));
			    		strPlanNamesDelimted += "'" + strPlanNameDelimited.replace("'", "''") + "'";
				    }
				    
				    // plan names are already SQ delimited
				    if (bolShowProperties == true) { System.out.println("FormPanel strPlanNamesDelimted = " + strPlanNamesDelimted); }
				    PlanStatisticsPanel.setPlanNamesDelimited(strPlanNamesDelimted);

				    // see if the user chose the Compare Plans tab
				    tabPaneToLink.addChangeListener(new ChangeListener() {
			            int COMPARE_PLANS_TAB = 1;
				        public void stateChanged(ChangeEvent e) {
				            //System.out.println("(2) Tab: " + tabPaneToLink.getSelectedIndex());
				            if (tabPaneToLink.getSelectedIndex() == COMPARE_PLANS_TAB) {
				            	//PlanStatisticsPanel.setReadStatisticsDataFlag(true);
				            	statisticsPanel.setReadStatisticsDataFlag(true);
				            	statisticsPanel.createComponents();
				            }
				        }
					});


				    //tabPaneToLink.addChangeListener(new ChangeListener() {

					JScrollPane planGraphScrollPane = new JScrollPane(pEvent.getPlanGraphPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
					//planGraphScrollPane.setMinimumSize(new Dimension(320, 480));
					planGraphScrollPane.setPreferredSize(new Dimension(320, 480));
					
					// show operator properties
					OperationPropertiesPanel operationPropertiesPanel = new OperationPropertiesPanel(pEvent.getPlanGraphPanel());
					JScrollPane propertiesPanelScrollPane = new JScrollPane(operationPropertiesPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
					//propertiesPanelScrollPane.setMinimumSize(new Dimension(320, 480));
					propertiesPanelScrollPane.setPreferredSize(new Dimension(320, 480));
					
					//for horizontal split
					JSplitPane graphAndPropertiesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, planGraphScrollPane, propertiesPanelScrollPane);
					graphAndPropertiesSplitPane.setOneTouchExpandable(true);
					graphAndPropertiesSplitPane.setDividerLocation(1000);
					
					// changes by Akila
					//SliderPanel sliderPanel = new SliderPanel();
					SliderPanel sliderPanel = new SliderPanel(pEvent.getPlanGraphPanel());
					//sliderPanel.setSliderType(sliderType);

					if (bolShowProperties == true) { System.out.println("before sliderPanel.setPlanTotalTimeMs()"); }
					
					if (bolShowProperties == true) { System.out.println("after sliderPanel.setPlanTotalTimeMs()"); }
					//sliderPanel.setCurrentPlanTimeMs(0);
					//JScrollPane timeSliderPanelScrollPane = new JScrollPane(new SliderPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
					JScrollPane timeSliderPanelScrollPane = new JScrollPane(sliderPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
					
					//for vertical split
					JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphAndPropertiesSplitPane, timeSliderPanelScrollPane);
					splitPane.setDividerLocation(500);
					splitPane.setOneTouchExpandable(true);
					
					tabPaneToLink.addTab(getPlanGraphTabName(selectedPlanName), splitPane);
					
					// disable the graph tab until we generate a plan from a query or JSON
					if (pEvent.getPlanSourcePanel().getPlanTextIsDirty() == true) {
						tabPaneToLink.setEnabledAt(2, false);
					}
					
					// TODO: disable thickness by and percentage by buttons
					//JSplitPane currentPane = (JSplitPane) objCurrentPlanTabs.getSelectedComponent();
					//JSplitPane currentPane = (JSplitPane) pEvent;
					//if (currentPane != null) {
						//if (currentPane.getParent() instanceof JTabbedPane) {
							//JTabbedPane tabbedPane = (JTabbedPane) currentPane.getParent();
							//if (tabbedPane.getParent() instanceof JSplitPane) {
								//JSplitPane splitPane = (JSplitPane) tabbedPane.getParent();
								for (Component objSplitPaneComponent : splitPane.getParent().getComponents()) {
									if (objSplitPaneComponent instanceof JPanel) {
										for (Component objJPanelItem : ((JPanel) objSplitPaneComponent).getComponents()) {
/*
											if (objJPanelItem instanceof LineThicknessByToolbar) {
												LineThicknessByToolbar lineThicknessByToolbar = (LineThicknessByToolbar) objJPanelItem;
												lineThicknessByToolbar.setLineThicknessByActualRowsButton(false);
												lineThicknessByToolbar.setLineThicknessByEstimatedRowsButton(false);
												lineThicknessByToolbar.setLineThicknessByActualMassButton(false);
												lineThicknessByToolbar.setLineThicknessByEstimatedMassButton(false);
												lineThicknessByToolbar.setLineThicknessByActualVsEstimatedRowsButton(false);
												lineThicknessByToolbar.setLineThicknessByActualVsEstimatedMassButton(false);
											}
*/
											if (objJPanelItem instanceof LineThicknessByRadioButtonsToolbar) {
												LineThicknessByRadioButtonsToolbar lineThicknessByRadioButtonsToolbar = (LineThicknessByRadioButtonsToolbar) objJPanelItem;
												lineThicknessByRadioButtonsToolbar.setLineThicknessByActualRowsButtonEnabled(false);
												lineThicknessByRadioButtonsToolbar.setLineThicknessByEstimatedRowsButtonEnabled(false);
												lineThicknessByRadioButtonsToolbar.setLineThicknessByActualMassButtonEnabled(false);
												lineThicknessByRadioButtonsToolbar.setLineThicknessByEstimatedMassButtonEnabled(false);
												lineThicknessByRadioButtonsToolbar.setLineThicknessByActualVsEstimatedRowsRatioButtonEnabled(false);
												lineThicknessByRadioButtonsToolbar.setLineThicknessByActualVsEstimatedMassRatioButtonEnabled(false);
											}
/*
											if (objJPanelItem instanceof PercentageByToolbar) {
												PercentageByToolbar percentageByToolbar = (PercentageByToolbar) objJPanelItem;
												percentageByToolbar.setPercentageByCostButton(false);
												//percentageByToolbar.setPercentageByDiskButton(false);
												//percentageByToolbar.setPercentageByDiskPlusRAMButton(false);
												//percentageByToolbar.setPercentageByRAMButton(false);
												percentageByToolbar.setPercentageByExclusiveTimeButton(false);
												percentageByToolbar.setPercentageByTimeDifferenceButton(false);
											}
*/
											if (objJPanelItem instanceof PercentageByRadioButtonsToolbar) {
												PercentageByRadioButtonsToolbar percentageByRadioButtonsToolbar = (PercentageByRadioButtonsToolbar) objJPanelItem;
												percentageByRadioButtonsToolbar.setPercentageByCostButtonEnabled(false);
												percentageByRadioButtonsToolbar.setPercentageByExclusiveTimeButtonEnabled(false);
												percentageByRadioButtonsToolbar.setPercentageByTimeDifferenceButtonEnabled(false);
											}
										}
									}
								}
							//}
						//}
					//}
				}
			}
			
		});
		
		addPlanBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//int nextSeq = plansMapper.size() + 1;

				//String newPlanName = "Plan " + nextSeq;
				String newPlanID;
				String newPlanName;

				PlanSourcePanel newPlanSourcePanel = new PlanSourcePanel(tabPane);
				PlanGraphPanel newPlanGraphPanel = new PlanGraphPanel(tabPane);
				PlanEvent pEvent = new PlanEvent(newPlanSourcePanel, newPlanGraphPanel);
				newPlanID = addPlanToDatabase(newPlanSourcePanel, newPlanGraphPanel);
				newPlanName = Database.row("select plan_name from explain_plans where id_explain_plan = " + newPlanID).getString("plan_name");

				DefaultListModel baseModel = (DefaultListModel) planList.getModel();
				baseModel.addElement(newPlanName);
				planList.setSelectedIndex(baseModel.size()-1);

				//newPlanSourcePanel.setJsonText(planSource4JsonText);
				//newPlanSourcePanel.setPlanName(newPlanName);
				newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
				newPlanGraphPanel.setIDExplainPlan(newPlanName);
				//newPlanSourcePanel.setPlanIsDirty(true);

				plansMapper.put(newPlanName, pEvent);
				//plansMapper.put(pEvent);
				
				tabPaneToLink.removeAll();
				
				tabPaneToLink.addTab(getPlanSourceTabName(newPlanName), pEvent.getPlanSourcePanel());
				
				// add "Plan Statistics" tab
				// by stas
				PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
				JScrollPane statisticsPane = new JScrollPane(statisticsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				//statisticsPane.setMinimumSize(new Dimension(320, 480));
				statisticsPane.setPreferredSize(new Dimension(320, 480));
				tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);

				JScrollPane planGraphScrollPane = new JScrollPane(pEvent.getPlanGraphPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				//planGraphScrollPane.setMinimumSize(new Dimension(320, 480));
				planGraphScrollPane.setPreferredSize(new Dimension(320, 480));
				
				// show operator properties
				OperationPropertiesPanel operationPropertiesPanel = new OperationPropertiesPanel(pEvent.getPlanGraphPanel());
				JScrollPane propertiesPanelScrollPane = new JScrollPane(operationPropertiesPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				//propertiesPanelScrollPane.setMinimumSize(new Dimension(320, 480));
				propertiesPanelScrollPane.setPreferredSize(new Dimension(320, 480));
				
				//for horizontal split
				JSplitPane graphAndPropertiesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, planGraphScrollPane, propertiesPanelScrollPane);
				graphAndPropertiesSplitPane.setOneTouchExpandable(true);
				graphAndPropertiesSplitPane.setDividerLocation(1000);
				
				// changes by Akila
				//SliderPanel sliderPanel = new SliderPanel();
				SliderPanel sliderPanel = new SliderPanel(pEvent.getPlanGraphPanel());
				//sliderPanel.setSliderType(sliderType);

				JScrollPane timeSliderPanelScrollPane = new JScrollPane(sliderPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				
				//for vertical split
				JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphAndPropertiesSplitPane, timeSliderPanelScrollPane);
				splitPane.setDividerLocation(500);
				splitPane.setOneTouchExpandable(true);
				
				tabPaneToLink.addTab(getPlanGraphTabName(newPlanName), splitPane);
				duplicatePlanBtn.setEnabled(true);

				// disable the graph tab until we generate a plan from a query or JSON
				tabPaneToLink.setEnabledAt(2, false);

			}
		});
		
		deletePlanBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

			    // select the Plan Source tab before we start deleting plans
			    // otherwise we may get exceptions when we try to show plan info from
			    // plans that suddenly disappear
			    tabPaneToLink.setSelectedIndex(0);

				// delete the selected explain plan
				int rows = planList.getSelectedIndices().length;
				String msg = "Are you sure you want to delete ";
				
				//System.out.println("59.9 sec: " + Utils.elapsed_time_ms(59.9 * 1000.0));
				
				if (rows == 1)
					msg += "this plan?";
				else if (rows > 1)
					msg += String.format("%d plans?", rows);
				else
					return;
				
				JDialog.setDefaultLookAndFeelDecorated(true);
			    int response = JOptionPane.showConfirmDialog(null, msg, "Confirm",
			        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			    if (response == JOptionPane.NO_OPTION) {
			      return;
			    } else if (response == JOptionPane.CLOSED_OPTION) {
			      return;
			    }
			    
			    DefaultListModel baseModel = (DefaultListModel) planList.getModel();
			    while (planList.getSelectedIndices().length > 0) {
			    	String selectedPlanName = (String) planList.getSelectedValue(); // "Plan 1"
			    	deletePlanNameFromDatabase(selectedPlanName);
			    	baseModel.removeElementAt(planList.getSelectedIndex());
			    }
				if (baseModel.getSize() > 0)
					planList.setSelectedIndex(baseModel.getSize() - 1);

				// if we deleted all plans, remove the tabs, too
				if (baseModel.getSize() == 0) {
					tabPaneToLink.removeAll();
					deletePlanBtn.setEnabled(false);
					duplicatePlanBtn.setEnabled(false);
				}
			}
		});


		duplicatePlanBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				String strSQL = null;
				boolean bolShowSQL = false;
				
			    if (planList.getSelectedIndices().length > 0) {
			    	for(Object planName : planList.getSelectedValuesList()) {
			    		String planNameValue = planName.toString();
			    		//System.out.println("****mytest***" + planNameValue);
			    		
			    		PlanGraphPanel existingPlanGraph = null;
			    		PlanSourcePanel existingPlanSource = null;
			    		PlanEvent existingEvent = plansMapper.get(planNameValue);
			    		if (existingEvent != null) {
			    			existingPlanGraph = existingEvent.getPlanGraphPanel();
			    			existingPlanSource = existingEvent.getPlanSourcePanel();
			    			
			    			//System.out.println("Existing explain plan id" + existingPlanGraph.getIdExplainPlan().intValue());
			    		}
			    		
			    		// generate a new plan name
			    		strSQL = "with cteOriginalPlanName as (\n" + 
			    				"	select '" + planNameValue + "' as plan_name\n" + 
			    				")\n" + 
			    				", cteOriginalPlaNameNumberPrefix as (\n" + 
			    				"	select\n" + 
			    				"		1 as order_by_attempt\n" + 
			    				"		,plan_name\n" + 
			    				"		,null as instr\n" + 
			    				"		,plan_name as plan_name_without_copy\n" + 
			    				"		, null as current_copy_number\n" + 
			    				"	from cteOriginalPlanName where plan_name not like '%(%'\n" + 
			    				"	union\n" + 
			    				"	select\n" + 
			    				"		2 as order_by_attempt\n" + 
			    				"		,plan_name\n" + 
			    				"		,instr(plan_name, ' (copy') as instr\n" + 
			    				"		,substr(plan_name, 1, instr(plan_name, ' (copy') - 1) as plan_name_without_copy\n" + 
			    				"		,cast(replace(substr(plan_name, instr(plan_name, '(copy ') + length('(copy ')), ')', '') as int) as current_copy_number\n" + 
			    				"	from cteOriginalPlanName where plan_name like '%(%'	\n" + 
			    				")\n" + 
			    				", cteAllPlansNumberPrefix as (\n" + 
			    				"	select\n" + 
			    				"		id_explain_plan\n" + 
			    				"		,1 as order_by_attempt\n" + 
			    				"		,plan_name\n" + 
			    				"		,null as instr\n" + 
			    				"		,plan_name as plan_name_without_copy\n" + 
			    				"		, null as current_copy_number\n" + 
			    				"	from explain_plans where plan_name not like '%(%'\n" + 
			    				"	union\n" + 
			    				"	select\n" + 
			    				"		id_explain_plan\n" + 
			    				"		,2 as order_by_attempt\n" + 
			    				"		,plan_name\n" + 
			    				"		,instr(plan_name, ' (copy') as instr\n" + 
			    				"		,substr(plan_name, 1, instr(plan_name, ' (copy') - 1) as plan_name_without_copy\n" + 
			    				"		,cast(replace(substr(plan_name, instr(plan_name, '(copy ') + length('(copy ')), ')', '') as int) as current_copy_number\n" + 
			    				"	from explain_plans where plan_name like '%(%'\n" + 
			    				")\n" + 
			    				", cteJoin as (\n" + 
			    				"	select O.plan_name_without_copy || ' (copy ' || max(coalesce(A.current_copy_number, 1) + 1) || ')' as new_plan_name\n" + 
			    				"	from cteOriginalPlaNameNumberPrefix as O\n" + 
			    				"	inner join cteAllPlansNumberPrefix as A on O.plan_name_without_copy = A.plan_name_without_copy\n" + 
			    				"	group by O.plan_name_without_copy\n" + 
			    				")\n" + 
			    				"select * from cteJoin\n" + 
			    				"";
			    		String newPlanName = Database.row(strSQL).getString("new_plan_name");
			    		DefaultListModel baseModel = (DefaultListModel) planList.getModel();
						baseModel.addElement(newPlanName);
						planList.setSelectedIndex(baseModel.size()-1);
						
						PlanSourcePanel newPlanSourcePanel = new PlanSourcePanel(tabPane);
						PlanGraphPanel newPlanGraphPanel = new PlanGraphPanel(tabPane);
						PlanEvent newPlnEvent = new PlanEvent(newPlanSourcePanel, newPlanGraphPanel);
						Integer newExplainPlanID = addPlanNameToDatabase(newPlanName, newPlanSourcePanel, newPlanGraphPanel);
						
						newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
						newPlanGraphPanel.setIDExplainPlan(newPlanName);
						
						plansMapper.put(newPlanName, newPlnEvent);
						
						tabPaneToLink.removeAll();
						
						tabPaneToLink.addTab(getPlanSourceTabName(newPlanName), newPlnEvent.getPlanSourcePanel());
						
						// add "Plan Statistics" tab
						// by stas
						PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
						JScrollPane statisticsPane = new JScrollPane(statisticsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
						//statisticsPane.setMinimumSize(new Dimension(320, 480));
						statisticsPane.setPreferredSize(new Dimension(320, 480));
						tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);
						
						JScrollPane planGraphScrollPane = new JScrollPane(newPlnEvent.getPlanGraphPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
						//planGraphScrollPane.setMinimumSize(new Dimension(320, 480));
						planGraphScrollPane.setPreferredSize(new Dimension(320, 480));
						
						// show operator properties
						OperationPropertiesPanel operationPropertiesPanel = new OperationPropertiesPanel(newPlnEvent.getPlanGraphPanel());
						JScrollPane propertiesPanelScrollPane = new JScrollPane(operationPropertiesPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
						//propertiesPanelScrollPane.setMinimumSize(new Dimension(320, 480));
						propertiesPanelScrollPane.setPreferredSize(new Dimension(320, 480));
						
						//for horizontal split
						JSplitPane graphAndPropertiesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, planGraphScrollPane, propertiesPanelScrollPane);
						graphAndPropertiesSplitPane.setOneTouchExpandable(true);
						graphAndPropertiesSplitPane.setDividerLocation(1000);
						
						SliderPanel sliderPanel = new SliderPanel(newPlnEvent.getPlanGraphPanel());
						//sliderPanel.setSliderType(sliderType);

						JScrollPane timeSliderPanelScrollPane = new JScrollPane(sliderPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
						
						//for vertical split
						JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphAndPropertiesSplitPane, timeSliderPanelScrollPane);
						splitPane.setDividerLocation(500);
						splitPane.setOneTouchExpandable(true);
						
						tabPaneToLink.addTab(getPlanGraphTabName(newPlanName), splitPane);
						
						// copy over data
						if (existingPlanSource != null) {
							newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
							newPlanSourcePanel.setPlanSourceFromQueryServerField(existingPlanSource.getPlanSourceFromQueryServerField());
							newPlanSourcePanel.setPlanSourceFromQueryPortField(existingPlanSource.getPlanSourceFromQueryPortField());
							newPlanSourcePanel.setPlanSourceFromQueryDatabaseField(existingPlanSource.getPlanSourceFromQueryDatabaseField());
							newPlanSourcePanel.setPlanSourceFromQueryUserField(existingPlanSource.getPlanSourceFromQueryUserField());
							if (existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox() == true) {
								newPlanSourcePanel.setPlanSourceFromQueryPasswordField(existingPlanSource.getPlanSourceFromQueryPasswordField());
							} else {
								newPlanSourcePanel.setPlanSourceFromQueryPasswordField("");
							}
							newPlanSourcePanel.setPlanSourceFromQueryRememberPasswordCheckbox(existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox());
							newPlanSourcePanel.setPlanSourceFromQueryPreCommandField(existingPlanSource.getPlanSourceFromQueryPreCommandField());
							newPlanSourcePanel.setSQLQueryText(existingPlanSource.getSQLQueryText());
							newPlanSourcePanel.setPlanSourceFromQueryPostCommandField(existingPlanSource.getPlanSourceFromQueryPostCommandField());
							newPlanSourcePanel.setJsonPlanText(existingPlanSource.getJsonPlanText());

							strSQL = "update explain_plans set\n"; 
							strSQL += "server = '" + existingPlanSource.getPlanSourceFromQueryServerField() + "'\n";
							strSQL += ",port = case when '" + existingPlanSource.getPlanSourceFromQueryPortField() + "' = '' then null else 0" + existingPlanSource.getPlanSourceFromQueryPortField() + " end\n";
							strSQL += ",database = '" + existingPlanSource.getPlanSourceFromQueryDatabaseField() + "'\n";
							strSQL += ",username = '" + existingPlanSource.getPlanSourceFromQueryUserField() + "'\n";
							if (existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox() == true) {
								strSQL += ",password = '" + existingPlanSource.getPlanSourceFromQueryPasswordField().replace("'",  "''") + "'\n";
							} else {
								strSQL += ",password = ''\n";
							}
							strSQL += ",remember_password = case when lower('" + existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox() + "') = 'true' then 1 else 0 end\n";
							strSQL += ",pre_explain_command = '" + existingPlanSource.getPlanSourceFromQueryPreCommandField().replace("'",  "''") + "'\n";
							strSQL += ",explain_sql_command = '" + existingPlanSource.getSQLQueryText().replace("'",  "''") + "'\n";
							strSQL += ",post_explain_command = '" + existingPlanSource.getPlanSourceFromQueryPostCommandField().replace("'",  "''") + "'\n";
							strSQL += ",plan_text = '" + existingPlanSource.getJsonPlanText().replace("'",  "''") + "'\n";
							strSQL += ",plan_name = '" + newPlanName.replace("'",  "''") + "'\n";
							strSQL += "where id_explain_plan = " + newExplainPlanID + "\n";
							if (bolShowSQL == true) { System.out.println(strSQL); }
							Database.dml(strSQL);
						}

						//newPlanGraphPanel.setIDExplainPlan(jsnRow.getString("plan_name"));
						newPlanGraphPanel.setIDExplainPlan(newPlanName);
						
			    	}
					
				}
						
			}
		});
		
		//setBorder(BorderFactory.createTitledBorder("Add Person"));
		Border innerBorder = BorderFactory.createTitledBorder("Plans");
		Border outerBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
		setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		
		layoutComponents();
	}
	
	public void layoutComponents() {
		setLayout(new GridBagLayout());
		
		GridBagConstraints gc = new GridBagConstraints();
		
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.insets = new Insets(0, 0, 0, 0);
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		add(planListScrollPane, gc);
		
		gc.gridy = 1;
		gc.insets = new Insets(10, 0, 0, 0);
		add(addPlanBtn, gc);
		
		gc.gridy = 2;
		gc.insets = new Insets(10, 0, 0, 0);
		add(deletePlanBtn, gc);
		
		gc.gridy = 3;
		gc.insets = new Insets(10, 0, 0, 0);
		add(duplicatePlanBtn, gc);
		
		gc.gridy = 4;
		gc.weighty = 1;
		gc.anchor = GridBagConstraints.PAGE_END;
		add(new Container(), gc);
	}
	
	private String getPlanSourceTabName(String planName) {
		String strPlan = planName;
		if (strPlan.length() > 30)
			strPlan = strPlan.substring(0, 25) + " ... " + strPlan.substring(strPlan.length() - 5);
		return strPlan + " - Plan Source";
	}
	
	private String getPlanStatisticsTabName() {
		return "Compare Plans";
	}
	
	private String getPlanGraphTabName(String planName) {
		String strPlan = planName;
		if (strPlan.length() > 30)
			strPlan = strPlan.substring(0, 25) + " ... " + strPlan.substring(strPlan.length() - 5);
		return strPlan + " - Plan Graph";
	}
	
	private Integer addPlanNameToDatabase(String planName, PlanSourcePanel planSource, PlanGraphPanel planGraphPanel) {
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
        Integer intLastIDExplainPlan = null;
		String strSQL;
		//if (bolShowProperties == true) { System.out.println("add to explain_plans"); }
		//strSQL = "insert into explain_plans (id_explain_plan, plan_name) select coalesce(max(id_explain_plan) + 1, 0), '" + planName + "' from explain_plans";
		//if (bolShowSQL == true) { System.out.println(strSQL); }
		//Database.dml(strSQL);
		
		if (bolShowProperties == true) { System.out.println("add to explain_plans"); }
		//strSQL = "insert into explain_plans (id_explain_plan, plan_name, is_active, zoom, operator_box_width_bz, operator_box_thickness) select coalesce(max(id_explain_plan) + 1, 0), '" + planName + "', 0, " + planGraphPanel.getZoomPercentage() + "*1.0 / 100, " + planGraphPanel.OPERATOR_BOX_WIDTH + ", " + planGraphPanel.OPERATOR_BOX_THICKNESS + " from explain_plans";
		strSQL = "insert into explain_plans (id_explain_plan, plan_name) select coalesce(max(id_explain_plan) + 1, 1), '" + planName + "' from explain_plans";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

        strSQL = "select max(id_explain_plan) as last_id_explain_plan from explain_plans;";
        intLastIDExplainPlan = Database.row(strSQL).getInteger("last_id_explain_plan");
        //intLastIDExplainPlan = 5; // CHANGE THIS

		strSQL = "insert into explain_plan_generated_values (id_explain_plan, is_active) values (" + intLastIDExplainPlan + ", 0);";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

        return intLastIDExplainPlan;
	}
	
	private String addPlanToDatabase(PlanSourcePanel planSource, PlanGraphPanel planGraphPanel) {
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		String strSQL;
		String strRowID = null;
        String strLastIDExplainPlan = null;

		if (bolShowProperties == true) { System.out.println("addPlanToDatabase"); }
		//strSQL = "insert into explain_plans (id_explain_plan, plan_name, is_active, zoom, operator_box_width_bz, operator_box_thickness) select coalesce(max(id_explain_plan) + 1, 0), '" + planName + "', 0, " + planGraphPanel.getZoomPercentage() + "*1.0 / 100, " + planGraphPanel.OPERATOR_BOX_WIDTH + ", " + planGraphPanel.OPERATOR_BOX_THICKNESS + " from explain_plans";
		strSQL = "insert into explain_plans (id_explain_plan, plan_name) select coalesce(max(id_explain_plan) + 1, 1), 'Plan ' || cast(coalesce(max(id_explain_plan) + 1, 1) as varchar(5)) from explain_plans";
		if (bolShowSQL == true) { System.out.println(strSQL); }
        Database.dml(strSQL);
        strSQL = "select max(id_explain_plan) as last_id_explain_plan from explain_plans;";
        
        strLastIDExplainPlan = Database.row(strSQL).getString("last_id_explain_plan");
        
        //strLastIDExplainPlan = "5";
        
		strSQL = "insert into explain_plan_generated_values (id_explain_plan, is_active) values (" + strLastIDExplainPlan + ", 0);";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

        return strLastIDExplainPlan;
	}
	
	private void deletePlanNameFromDatabase(String planName) {
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		String strSQL;
		if (bolShowProperties == true) { System.out.println("deletePlanFromDatabase"); }
		strSQL = "delete from explain_plans where plan_name = '" + planName.replace("'", "''") + "'";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		strSQL = "delete from explain_plan_generated_values where id_explain_plan not in (select id_explain_plan from explain_plans);";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

	}
	
	public JList getPlanList() { 
		return planList;
	}
	
	//public void setSelectedPlanName(String strNewPlanName) {
	public void setSelectedPlanName(int index, String strNewPlanName) {
		boolean bolShowProperties = false;
		//String selectedPlanName = (String) planList.getSelectedValue();
		//baseModel.setElementAt(strNewPlanName, planList.getSelectedIndex());
		//baseModel.
		DefaultListModel baseModel = (DefaultListModel) planList.getModel();
		//baseModel.addElement(newPlanName);
		//baseModel.setElementAt(strNewPlanName, planList.getSelectedIndex());
		baseModel.setElementAt(strNewPlanName, index);
		//planList.setSelectedIndex(baseModel.size()-1);
		//plansMapper.put(strNewPlanName, pEvent);
		if (bolShowProperties == true) { System.out.println("FormPanel.java / setSelectedPlanName() / planEvent = " + planEvent); }
    	//String selectedPlanName = (String) planList.getSelectedValue(); // "Plan 1"
    	//deletePlanNameFromDatabase(selectedPlanName);
    	//baseModel.removeElementAt(planList.getSelectedIndex());
		//if (planEvent != null)
		//	planEvent.setPlanName(strNewPlanName);
	}

	protected int getSelectedIndex() {
		return planList.getSelectedIndex();
	}
	
	protected void updatePlanMapper(String existingPlanName, String strNewPlanName) {
		PlanEvent pEvent = plansMapper.get(existingPlanName);
		//int selectedIndex = planList.getSelectedIndex();
		if (pEvent != null) {
			String selectedPlanName = (String) planList.getSelectedValue();
			plansMapper.remove(existingPlanName);
			pEvent.setPlanName(strNewPlanName);
			plansMapper.put(strNewPlanName, pEvent);
			//plansMapper.replace(existingPlanName, planEvent, pEvent); // doesn't seem to work
			//DefaultListModel baseModel = (DefaultListModel) planList.getModel();
			//baseModel.setElementAt(strNewPlanName, selectedIndex);
			//baseModel.addElement(newPlanName);
			//planList.setSelectedIndex(baseModel.size()-1);
		}
				
	}
	
}
