package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
	private JPanel planToolbar;
	//private FormListener formListener;
	private JList planList;
	private JTabbedPane tabPaneToLink;
	private PlanDiagramToolbar planDiagramToolbar;
	private JScrollPane planListScrollPane;
	private String PLAN_LIST_FONT_NAME = "Arial";
	private int PLAN_LIST_FONT_STYLE = Font.PLAIN;
	private int PLAN_LIST_FONT_SIZE = 12;
	
	public Map<String, PlanEvent> plansMapper = new TreeMap<String, PlanEvent>();
	private PlanEvent planEvent;
	private boolean comparePlansRefreshInProgress = false;
	private JCheckBoxMenuItem propertiesViewMenuItem;
	private JCheckBoxMenuItem slidersViewMenuItem;
	private JCheckBoxMenuItem zoomViewMenuItem;
	private boolean suppressDiagramViewMenuEvents;

	public FormPanel(JTabbedPane tabPane, PlanDiagramToolbar planDiagramToolbar) {
		
		tabPaneToLink = tabPane;
		this.planDiagramToolbar = planDiagramToolbar;
		// Selecting Plan Diagram generates from JSON (same as "Show JSON Plan Diagram"),
		// unless Execute-and-show already set a button action.
		tabPaneToLink.addChangeListener(new ChangeListener() {
			private static final int PLAN_DIAGRAM_TAB = 2;

			public void stateChanged(ChangeEvent e) {
				if (tabPaneToLink.getSelectedIndex() != PLAN_DIAGRAM_TAB || tabPaneToLink.getTabCount() <= PLAN_DIAGRAM_TAB) {
					return;
				}
				Component sourceComp = tabPaneToLink.getComponentAt(0);
				if (!(sourceComp instanceof PlanSourcePanel)) {
					return;
				}
				PlanSourcePanel planSource = (PlanSourcePanel) sourceComp;
				String button = planSource.getPlanSourceButtonClicked();
				boolean execute = PlanSourcePanel.EXECUTE_AND_SHOW_PLAN_DIAGRAM.equals(button);
				boolean rebuildFromJson = PlanSourcePanel.SHOW_JSON_PLAN_DIAGRAM.equals(button)
						|| (button == null && planSource.shouldRebuildDiagramFromCurrentJson());
				if (!execute && !rebuildFromJson) {
					// Diagram already matches current JSON — just show the tab
					return;
				}
				if (button == null) {
					planSource.setPlanSourceButtonClicked(PlanSourcePanel.SHOW_JSON_PLAN_DIAGRAM);
				}
				PlanGraphPanel.setDiagramBusyCursor(tabPaneToLink, true);
				repaintPlanGraphPanels(tabPaneToLink.getComponentAt(PLAN_DIAGRAM_TAB));
			}
		});
		Dimension dim = getPreferredSize();
		dim.width = 220;
		setPreferredSize(dim);
		setMinimumSize(new Dimension(140, 120));
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
				+ ", auth_type, pre_explain_command, explain_sql_command, post_explain_command, plan_text\n"
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
		            	refreshComparePlansTab(statisticsPanel);
		            }
		        }
			});

			// Properties full height on the right; graph + sliders on the left
			tabPaneToLink.addTab(getPlanDiagramTabName(newPlanName),
					createPlanDiagramTab(pEvent.getPlanGraphPanel(), 600, 600));
			
			//strSQL = "select id_explain_plan, plan_name, server, port, database, username, password\n"
			//		+ ", pre_explain_command, explain_sql_command, post_explain_command, plan_text\n"
			//		+ "from explain_plans;\n";
			//newPlanSourcePanel.setIdExplainPlan(jsnRow.getInteger("id_explain_plan"));
			newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(jsnRow.getString("plan_name"));
			newPlanSourcePanel.setPlanSourceFromQueryServerField(jsnRow.getString("server"));
			newPlanSourcePanel.setPlanSourceFromQueryPortField(jsnRow.getString("port"));
			newPlanSourcePanel.setPlanSourceFromQueryDatabaseField(jsnRow.getString("database"));
			newPlanSourcePanel.setPlanSourceFromQueryUserField(jsnRow.getString("username"));
			newPlanSourcePanel.setAuthenticationTypeCode(jsnRow.getString("auth_type"));
			newPlanSourcePanel.setPlanSourceFromQueryRememberPasswordCheckbox(Utils.objectToBoolean(jsnRow.getString("remember_password")));
			if (Utils.objectToBoolean(jsnRow.getString("remember_password")) == true
					&& !newPlanSourcePanel.isTrustAuthentication()) {
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
			
			// Plan Diagram is enabled only for a single selected plan
			updatePlanDiagramTabEnabled();
		}
		
		addPlanBtn = createPlanToolbarButton("add", "Add Plan");
		deletePlanBtn = createPlanToolbarButton("delete", "Delete Plan(s)");
		duplicatePlanBtn = createPlanToolbarButton("duplicate", "Duplicate Plan(s)");
		if (planList.getSelectedIndices().length == 0)
			deletePlanBtn.setEnabled(false); // disable until a plan is selected or is loaded from the sqlite db
		if (planList.getSelectedIndices().length == 0)
			duplicatePlanBtn.setEnabled(false); // disable until a plan is selected or is loaded from the sqlite db

		planToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		planToolbar.setOpaque(false);
		planToolbar.add(addPlanBtn);
		planToolbar.add(deletePlanBtn);
		planToolbar.add(duplicatePlanBtn);

		planListScrollPane = new JScrollPane(planList);
		planListScrollPane.setMinimumSize(new Dimension(120, 100));
		planListScrollPane.setBorder(BorderFactory.createEmptyBorder());
		planList.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		
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
				            	refreshComparePlansTab(statisticsPanel);
				            }
				        }
					});


				    //tabPaneToLink.addChangeListener(new ChangeListener() {

					if (bolShowProperties == true) { System.out.println("before createPlanDiagramTab()"); }
					JPanel diagramTab = createPlanDiagramTab(pEvent.getPlanGraphPanel(), 1000, 500);
					if (bolShowProperties == true) { System.out.println("after createPlanDiagramTab()"); }
					tabPaneToLink.addTab(getPlanDiagramTabName(selectedPlanName), diagramTab);
					
					updatePlanDiagramTabEnabled();
					
					if (planDiagramToolbar != null) {
						planDiagramToolbar.setAllDiagramOptionsEnabled(false);
					}
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

				tabPaneToLink.addTab(getPlanDiagramTabName(newPlanName),
						createPlanDiagramTab(pEvent.getPlanGraphPanel(), 1000, 500));
				duplicatePlanBtn.setEnabled(true);

				updatePlanDiagramTabEnabled();

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
						
						tabPaneToLink.addTab(getPlanDiagramTabName(newPlanName),
								createPlanDiagramTab(newPlnEvent.getPlanGraphPanel(), 1000, 500));
						
						// copy over data
						if (existingPlanSource != null) {
							newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
							newPlanSourcePanel.setPlanSourceFromQueryServerField(existingPlanSource.getPlanSourceFromQueryServerField());
							newPlanSourcePanel.setPlanSourceFromQueryPortField(existingPlanSource.getPlanSourceFromQueryPortField());
							newPlanSourcePanel.setPlanSourceFromQueryDatabaseField(existingPlanSource.getPlanSourceFromQueryDatabaseField());
							newPlanSourcePanel.setPlanSourceFromQueryUserField(existingPlanSource.getPlanSourceFromQueryUserField());
							newPlanSourcePanel.setAuthenticationTypeCode(existingPlanSource.getAuthenticationTypeCode());
							if (existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox() == true
									&& !existingPlanSource.isTrustAuthentication()) {
								newPlanSourcePanel.setPlanSourceFromQueryPasswordField(existingPlanSource.getPlanSourceFromQueryPasswordField());
							} else {
								newPlanSourcePanel.setPlanSourceFromQueryPasswordField("");
							}
							newPlanSourcePanel.setPlanSourceFromQueryRememberPasswordCheckbox(existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox());
							newPlanSourcePanel.setPlanSourceFromQueryPreCommandField(existingPlanSource.getPlanSourceFromQueryPreCommandField());
							newPlanSourcePanel.setSQLQueryText(existingPlanSource.getSQLQueryText());
							newPlanSourcePanel.setPlanSourceFromQueryPostCommandField(existingPlanSource.getPlanSourceFromQueryPostCommandField());
							newPlanSourcePanel.setJsonPlanText(existingPlanSource.getJsonPlanText());

							Database.updateExplainPlanFields(newExplainPlanID, newPlanName,
									existingPlanSource.getPlanSourceFromQueryServerField(),
									existingPlanSource.getPlanSourceFromQueryPortField(),
									existingPlanSource.getPlanSourceFromQueryDatabaseField(),
									existingPlanSource.getPlanSourceFromQueryUserField(),
									existingPlanSource.getPlanSourceFromQueryPasswordField(),
									existingPlanSource.getPlanSourceFromQueryRememberPasswordCheckbox(),
									existingPlanSource.getAuthenticationTypeCode(),
									existingPlanSource.getPlanSourceFromQueryPreCommandField(),
									existingPlanSource.getSQLQueryText(),
									existingPlanSource.getPlanSourceFromQueryPostCommandField(),
									existingPlanSource.getJsonPlanText());
						}

						//newPlanGraphPanel.setIDExplainPlan(jsnRow.getString("plan_name"));
						newPlanGraphPanel.setIDExplainPlan(newPlanName);
						
			    	}
					
				}
						
			}
		});
		
		// Dockable title is "Plans"; avoid a second titled border here.
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		
		layoutComponents();
	}
	
	public void layoutComponents() {
		setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(0, 0, 4, 0);
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		add(planToolbar, gc);

		gc.gridy = 1;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(0, 0, 0, 0);
		add(planListScrollPane, gc);
	}

	private static JButton createPlanToolbarButton(String iconKind, String tooltip) {
		JButton button = new JButton(createPlanToolbarIcon(iconKind));
		button.setToolTipText(tooltip);
		button.setMargin(new Insets(4, 6, 4, 6));
		button.setFocusable(false);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.putClientProperty("peps.planToolbarIconKind", iconKind);
		return button;
	}

	/** Refresh Add/Delete/Duplicate glyphs after a color-scheme change. */
	public void refreshPlanToolbarIcons() {
		refreshPlanToolbarButtonIcon(addPlanBtn);
		refreshPlanToolbarButtonIcon(deletePlanBtn);
		refreshPlanToolbarButtonIcon(duplicatePlanBtn);
	}

	private static void refreshPlanToolbarButtonIcon(JButton button) {
		if (button == null) {
			return;
		}
		Object kind = button.getClientProperty("peps.planToolbarIconKind");
		if (kind instanceof String) {
			button.setIcon(createPlanToolbarIcon((String) kind));
		}
	}

	private static Color planToolbarIconColor(String kind) {
		// Semantic accents that stay readable on both light and dark chrome.
		if ("add".equals(kind)) {
			return UiTheme.isDark() ? new Color(0x4A, 0xDE, 0x80) : new Color(0x16, 0xA3, 0x4A);
		}
		if ("delete".equals(kind)) {
			return UiTheme.isDark() ? new Color(0xFB, 0x71, 0x85) : new Color(0xE1, 0x1D, 0x48);
		}
		if ("duplicate".equals(kind)) {
			return UiTheme.isDark() ? new Color(0x93, 0xC5, 0xFD) : new Color(0x25, 0x63, 0xEB);
		}
		Color fg = UIManager.getColor("Button.foreground");
		return fg != null ? fg : new Color(0x33, 0x41, 0x55);
	}

	private static ImageIcon createPlanToolbarIcon(String kind) {
		int size = 16;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(planToolbarIconColor(kind));
		switch (kind) {
		case "add":
			g.drawLine(8, 3, 8, 13);
			g.drawLine(3, 8, 13, 8);
			break;
		case "delete":
			g.drawLine(4, 5, 12, 5);
			g.drawRect(5, 5, 6, 8);
			g.drawLine(7, 7, 7, 11);
			g.drawLine(9, 7, 9, 11);
			g.drawLine(6, 5, 7, 3);
			g.drawLine(10, 5, 9, 3);
			break;
		case "duplicate":
			g.drawRect(5, 5, 8, 8);
			g.drawRect(3, 3, 8, 8);
			break;
		default:
			break;
		}
		g.dispose();
		return new ImageIcon(image);
	}

	/** View → Properties / Sliders / Zoom for the active Plan Diagram dockables. */
	public void installDiagramPanelViewMenuItems(JMenu viewMenu) {
		if (viewMenu == null) {
			return;
		}
		propertiesViewMenuItem = new JCheckBoxMenuItem("Properties");
		slidersViewMenuItem = new JCheckBoxMenuItem("Sliders");
		zoomViewMenuItem = new JCheckBoxMenuItem("Zoom");
		propertiesViewMenuItem.setToolTipText("Show or hide the Properties panel in Plan Diagram");
		slidersViewMenuItem.setToolTipText("Show or hide the Sliders panel in Plan Diagram");
		zoomViewMenuItem.setToolTipText("Show or hide the Zoom slider panel in Plan Diagram");

		propertiesViewMenuItem.addActionListener(e -> {
			if (suppressDiagramViewMenuEvents) {
				return;
			}
			DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
			if (diagram != null) {
				diagram.setPropertiesVisible(propertiesViewMenuItem.isSelected());
			}
		});
		slidersViewMenuItem.addActionListener(e -> {
			if (suppressDiagramViewMenuEvents) {
				return;
			}
			DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
			if (diagram != null) {
				diagram.setSlidersVisible(slidersViewMenuItem.isSelected());
			}
		});
		zoomViewMenuItem.addActionListener(e -> {
			if (suppressDiagramViewMenuEvents) {
				return;
			}
			DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
			if (diagram != null) {
				diagram.setZoomVisible(zoomViewMenuItem.isSelected());
			} else if (planDiagramToolbar != null) {
				planDiagramToolbar.setZoomSliderCheckboxSelected(zoomViewMenuItem.isSelected());
			}
		});

		viewMenu.add(propertiesViewMenuItem);
		viewMenu.add(slidersViewMenuItem);
		viewMenu.add(zoomViewMenuItem);
		if (tabPaneToLink != null) {
			tabPaneToLink.addChangeListener(e -> syncDiagramViewMenuItems());
		}
		syncDiagramViewMenuItems();
	}

	/**
	 * Diagram dockables for the <em>selected</em> Documents tab only. Using a fixed
	 * index 2 made View → Properties/Sliders/Zoom show checked while Plan Source was
	 * active (panels not on screen), which looked like a sync bug.
	 */
	public DiagramDockingPanel findActiveDiagramDockingPanel() {
		if (tabPaneToLink == null) {
			return null;
		}
		int selected = tabPaneToLink.getSelectedIndex();
		if (selected < 0 || selected >= tabPaneToLink.getTabCount()) {
			return null;
		}
		Component tab = tabPaneToLink.getComponentAt(selected);
		if (tab instanceof DiagramDockingPanel) {
			return (DiagramDockingPanel) tab;
		}
		return null;
	}

	/** Restore Plan Diagram dockables (Graph / Zoom / Properties / Sliders) to the default layout. */
	public void resetPanelsToDefault() {
		DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
		if (diagram != null) {
			diagram.resetToDefaultLayout();
		}
		syncDiagramViewMenuItems();
	}

	/** Re-sync diagram docking after Look-and-Feel / theme changes (esp. Linux). */
	public void repairDiagramDockingAfterThemeChange() {
		if (tabPaneToLink != null) {
			for (int i = 0; i < tabPaneToLink.getTabCount(); i++) {
				Component tab = tabPaneToLink.getComponentAt(i);
				if (tab instanceof DiagramDockingPanel) {
					((DiagramDockingPanel) tab).repairAfterThemeChange();
				}
			}
		}
		syncDiagramViewMenuItems();
	}

	public void syncDiagramViewMenuItems() {
		if (propertiesViewMenuItem == null || slidersViewMenuItem == null || zoomViewMenuItem == null) {
			return;
		}
		Runnable sync = () -> {
			DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
			boolean hasDiagram = diagram != null;
			suppressDiagramViewMenuEvents = true;
			try {
				propertiesViewMenuItem.setEnabled(hasDiagram);
				slidersViewMenuItem.setEnabled(hasDiagram);
				zoomViewMenuItem.setEnabled(hasDiagram && (diagram == null || diagram.hasZoomDockable()));
				if (hasDiagram) {
					propertiesViewMenuItem.setSelected(diagram.isPropertiesVisible());
					slidersViewMenuItem.setSelected(diagram.isSlidersVisible());
					zoomViewMenuItem.setSelected(diagram.isZoomVisible());
				} else {
					// Not on Plan Diagram — leave unchecked so View matches what's on screen.
					propertiesViewMenuItem.setSelected(false);
					slidersViewMenuItem.setSelected(false);
					zoomViewMenuItem.setSelected(false);
				}
			} finally {
				suppressDiagramViewMenuEvents = false;
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			sync.run();
		} else {
			SwingUtilities.invokeLater(sync);
		}
	}
	
	private String getPlanSourceTabName(String planName) {
		return planName + " - Plan Source";
	}
	
	private String getPlanStatisticsTabName() {
		return "Compare Plans";
	}
	
	private String getPlanDiagramTabName(String planName) {
		return planName + " - Plan Diagram";
	}

	/** Plan Diagram is only meaningful for a single selected plan. */
	public boolean isSinglePlanSelected() {
		return planList != null && planList.getSelectedIndices().length == 1;
	}

	public void updatePlanDiagramTabEnabled() {
		if (tabPaneToLink == null || tabPaneToLink.getTabCount() <= 2) {
			return;
		}
		String selectedPlanName = planList == null ? null : (String) planList.getSelectedValue();
		PlanEvent pEvent = selectedPlanName == null ? null : plansMapper.get(selectedPlanName);
		PlanSourcePanel source = pEvent == null ? null : pEvent.getPlanSourcePanel();
		boolean enableDiagram = isSinglePlanSelected()
				&& source != null
				&& source.hasUsableJsonPlan();
		tabPaneToLink.setEnabledAt(2, enableDiagram);
		if (source != null) {
			source.setShowPlanGraphButton(enableDiagram);
		}
		if (!enableDiagram && tabPaneToLink.getSelectedIndex() == 2) {
			// No JSON → Plan Source; multi-select → Compare Plans
			tabPaneToLink.setSelectedIndex(isSinglePlanSelected() ? 0 : 1);
		}
	}

	/**
	 * Generate any selected plans that lack diagram data, then refresh Compare Plans.
	 * Shows a small progress dialog while diagrams are built.
	 */
	private void refreshComparePlansTab(PlanStatisticsPanel statisticsPanel) {
		if (comparePlansRefreshInProgress) {
			return;
		}
		comparePlansRefreshInProgress = true;

		List<String> plansToGenerate = new ArrayList<>();
		DefaultListModel baseModel = (DefaultListModel) planList.getModel();
		for (int selectedIndex : planList.getSelectedIndices()) {
			String planName = (String) baseModel.getElementAt(selectedIndex);
			PlanEvent mappedEvent = plansMapper.get(planName);
			if (mappedEvent == null) {
				continue;
			}
			PlanSourcePanel source = mappedEvent.getPlanSourcePanel();
			PlanGraphPanel graph = mappedEvent.getPlanGraphPanel();
			if (source == null || graph == null) {
				continue;
			}
			String json = PlanSourcePanel.normalizeJsonPlanText(source.getJsonPlanText());
			if (json == null || json.isBlank()) {
				continue;
			}
			if (!graph.hasGeneratedPlanValues() || source.shouldRebuildDiagramFromJson(json)) {
				plansToGenerate.add(planName);
			}
		}

		Runnable finishRefresh = () -> {
			try {
				PlanStatisticsPanel panel = findVisibleComparePlansPanel();
				if (panel == null) {
					panel = statisticsPanel;
				}
				if (panel != null) {
					panel.setReadStatisticsDataFlag(true);
					panel.createComponents();
				}
			} finally {
				comparePlansRefreshInProgress = false;
			}
		};

		if (plansToGenerate.isEmpty()) {
			finishRefresh.run();
			return;
		}

		// Hide residual / partial rows until every selected plan has been generated.
		if (statisticsPanel != null) {
			statisticsPanel.clearPendingDisplay();
		}
		PlanStatisticsPanel visible = findVisibleComparePlansPanel();
		if (visible != null && visible != statisticsPanel) {
			visible.clearPendingDisplay();
		}

		generatePlanDiagramsWithProgress(plansToGenerate, finishRefresh);
	}

	private PlanStatisticsPanel findVisibleComparePlansPanel() {
		if (tabPaneToLink == null || tabPaneToLink.getTabCount() <= 1) {
			return null;
		}
		Component tab = tabPaneToLink.getComponentAt(1);
		if (tab instanceof JScrollPane) {
			Component view = ((JScrollPane) tab).getViewport().getView();
			if (view instanceof PlanStatisticsPanel) {
				return (PlanStatisticsPanel) view;
			}
		}
		return null;
	}

	private void generatePlanDiagramsWithProgress(List<String> planNames, Runnable onComplete) {
		final int planCount = planNames.size();
		final String dialogTitle = planCount == 1 ? "Generating Plan" : ("Generating " + planCount + " Plans");
		final int minVisibleMs = 700;
		final AtomicBoolean cancelRequested = new AtomicBoolean(false);
		final AtomicInteger nextIndex = new AtomicInteger(0);
		final long startedAt = System.currentTimeMillis();

		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog progressDialog = new JDialog(owner, dialogTitle, Dialog.ModalityType.MODELESS);
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		JLabel statusLabel = new JLabel(dialogTitle);
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
		JProgressBar progressBar = new JProgressBar(0, planCount);
		progressBar.setValue(0);
		progressBar.setStringPainted(planCount > 1);
		if (planCount > 1) {
			progressBar.setString("1 / " + planCount);
		}
		progressBar.setPreferredSize(new Dimension(280, 20));

		JPanel content = new JPanel(new BorderLayout(8, 10));
		content.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
		content.setOpaque(true);
		content.add(statusLabel, BorderLayout.NORTH);
		content.add(progressBar, BorderLayout.CENTER);

		if (planCount > 1) {
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(e -> {
				cancelRequested.set(true);
				cancelButton.setEnabled(false);
				statusLabel.setText("Cancelling after current plan…");
				paintProgressDialogNow(progressDialog);
			});
			JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			buttonRow.setOpaque(false);
			buttonRow.add(cancelButton);
			content.add(buttonRow, BorderLayout.SOUTH);
		}

		progressDialog.setContentPane(content);
		progressDialog.setResizable(false);
		progressDialog.pack();
		progressDialog.setLocationRelativeTo(owner != null ? owner : this);

		Runnable closeAndFinish = () -> {
			progressDialog.setVisible(false);
			progressDialog.dispose();
			if (onComplete != null) {
				onComplete.run();
			}
		};

		Runnable finishWithMinVisible = () -> {
			long elapsed = System.currentTimeMillis() - startedAt;
			int remaining = (int) Math.max(0, minVisibleMs - elapsed);
			if (remaining > 0) {
				javax.swing.Timer holdOpen = new javax.swing.Timer(remaining, e -> closeAndFinish.run());
				holdOpen.setRepeats(false);
				holdOpen.start();
			} else {
				closeAndFinish.run();
			}
		};

		// One plan per EDT turn so Cancel can be processed between plans (after the current one).
		Runnable generateNext = new Runnable() {
			@Override
			public void run() {
				int i = nextIndex.get();
				if (cancelRequested.get() || i >= planCount) {
					finishWithMinVisible.run();
					return;
				}

				String planName = planNames.get(i);
				statusLabel.setText(planCount == 1
						? "Generating Plan"
						: ("Generating Plan " + (i + 1) + " of " + planCount));
				progressBar.setValue(i);
				if (planCount > 1) {
					progressBar.setString((i + 1) + " / " + planCount);
				}
				progressBar.setToolTipText(planName);
				paintProgressDialogNow(progressDialog);

				PlanEvent planEvent = plansMapper.get(planName);
				if (planEvent != null) {
					try {
						planEvent.getPlanGraphPanel().ensurePlanDataGenerated(planEvent.getPlanSourcePanel());
					} catch (RuntimeException ex) {
						System.out.println("generatePlanDiagramsWithProgress: failed for " + planName);
						ex.printStackTrace();
					}
				}

				progressBar.setValue(i + 1);
				paintProgressDialogNow(progressDialog);
				nextIndex.incrementAndGet();

				if (cancelRequested.get() || nextIndex.get() >= planCount) {
					finishWithMinVisible.run();
				} else {
					SwingUtilities.invokeLater(this);
				}
			}
		};

		progressDialog.setVisible(true);
		progressDialog.toFront();
		paintProgressDialogNow(progressDialog);
		SwingUtilities.invokeLater(generateNext);
	}

	/** Force the progress dialog to paint fully before blocking work continues on the EDT. */
	private void paintProgressDialogNow(JDialog progressDialog) {
		if (progressDialog == null || !progressDialog.isDisplayable()) {
			return;
		}
		progressDialog.validate();
		Container content = progressDialog.getContentPane();
		if (content instanceof JPanel && content.getWidth() > 0 && content.getHeight() > 0) {
			JPanel panel = (JPanel) content;
			panel.validate();
			panel.paintImmediately(0, 0, panel.getWidth(), panel.getHeight());
		}
		java.awt.Graphics g = progressDialog.getGraphics();
		if (g != null) {
			try {
				progressDialog.paintAll(g);
			} finally {
				g.dispose();
			}
		}
		java.awt.Toolkit.getDefaultToolkit().sync();
	}

	private void repaintPlanGraphPanels(Component root) {
		if (root instanceof PlanGraphPanel) {
			root.repaint();
			return;
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				repaintPlanGraphPanels(child);
			}
		}
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
	
	/** Same wheel step as OperatorDetailsPanel Property/Value scrolling. */
	private static final int DIAGRAM_WHEEL_UNIT_INCREMENT = 48;

	/**
	 * Plan Diagram tab: nested docking for Graph / Properties / Sliders
	 * (reposition only within this tab).
	 */
	private JPanel createPlanDiagramTab(PlanGraphPanel graphPanel,
			int horizontalDivider, int verticalDivider) {
		DiagramDockingPanel diagram = new DiagramDockingPanel(graphPanel, planDiagramToolbar, DIAGRAM_WHEEL_UNIT_INCREMENT);
		diagram.setViewMenuSyncHook(this::syncDiagramViewMenuItems);
		SwingUtilities.invokeLater(this::syncDiagramViewMenuItems);
		return diagram;
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
