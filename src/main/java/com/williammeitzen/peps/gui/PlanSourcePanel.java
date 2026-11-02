package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
//import java.awt.Cursor;
import java.awt.Dimension;
//import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
//import java.awt.GridLayout;
import java.awt.Insets;
//import java.awt.Rectangle;
//import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
//import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;
//import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
//import javax.swing.ButtonGroup;
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
//import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
//import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
//import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
//import javax.swing.JViewport;
//import javax.swing.KeyStroke;
//import javax.swing.ScrollPaneConstants;
//import javax.swing.SwingUtilities;
//import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
//import org.json.simple.JsonObject;
//import model.Database;

import com.williammeitzen.peps.model.Database;

public class PlanSourcePanel extends JPanel {

	public static final String SHOW_JSON_PLAN_DIAGRAM = "Show JSON Plan Diagram";
	public static final String EXECUTE_AND_SHOW_PLAN_DIAGRAM = "Execute and Show Plan Diagram";
	
	//private JButton okBtn;
	private FormListener formListener;
	//private ButtonGroup planSourceRadioGroup;
	//private JRadioButton planSourceFromQueryRadio;
	//private JRadioButton planSourceFromJSONRadio;
	private JTextArea jsonPlanTextArea;
	private JScrollPane jsonPlanTextAreaScrollPane;
	//private JRadioButton planSourceFromPEGRadio;
	private int idExplainPlan;
	private JButton showPlanGraphButton;
	private JLabel planSourceFromQueryPlanNameLabel;
	private JTextField planSourceFromQueryPlanNameField;
	private JLabel planSourceFromQueryServerLabel;
	private JTextField planSourceFromQueryServerField;
	private JLabel planSourceFromQueryPortLabel;
	private JTextField planSourceFromQueryPortField;
	private JLabel planSourceFromQueryDatabaseLabel;
	private JTextField planSourceFromQueryDatabaseField;
	private JLabel planSourceFromQueryUserLabel;
	private JTextField planSourceFromQueryUserField;

	private JLabel planSourceFromQueryPasswordLabel;
	private JTextField planSourceFromQueryPasswordField;
	private JLabel planSourceFromQueryRememberPasswordLabel;
	private JCheckBox planSourceFromQueryRememberPasswordCheckbox;

	private JLabel planSourceFromQueryPreCommandLabel;
	private JLabel planSourceFromQueryPostCommandLabel;
	private JTextField planSourceFromQueryPreCommandField;
	private JTextArea planSourceFromQuerySQLTextArea;
	private JTextField planSourceFromQueryPostCommandField;
	private JLabel planSourceFromQuerySQLLabel;
	private JScrollPane planSourceFromQuerySQLTextAreaScrollPane;
	private JButton planSourceFromQueryButton;
	private String planName;
	private String planSourceButtonClicked;
	private JTabbedPane objCurrentPlanTabs;
	private boolean planTextIsDirty = true;
	private boolean lineThicknessByIsDirty = true;
	private boolean zoomPercentageIsDirty = true;
	private boolean percentageByIsDirty = true;
	private boolean workerIsDirty = true;
	/** Normalized JSON last used to build the Plan Diagram; null if never built. */
	private String jsonRenderedInDiagram = null;
	public int intZoomPercentage = 100;
	
	private final Insets EAST_INSETS = new Insets(2, 4, 12, 12);
	private final Insets WEST_INSETS = new Insets(2, 12, 12, 4);
	private final ArrayList<Component> labels = new ArrayList<Component>();
	private final ArrayList<Component> fields = new ArrayList<Component>();
	
	public PlanSourcePanel(JTabbedPane objCurrentPlanTabs) {
		boolean bolShowTiming = false;
		Dimension dim = getPreferredSize();
		//System.out.println(dim);
		dim.width = 300;
		setMinimumSize(dim);
		//String strSQL;
		//Integer idExplainPlan = -1;
		//JsonObject objRow = null;
		
		this.objCurrentPlanTabs = objCurrentPlanTabs;
		
		//okBtn = new JButton("OK");
		
		//planSourceRadioGroup = new ButtonGroup();
		//planSourceFromQueryRadio = new JRadioButton("Retrieve plan from EXPLAIN SELECT query");
		//planSourceFromJSONRadio = new JRadioButton("Paste JSON Plan");
		//planSourceFromPEGRadio = new JRadioButton("Paste PEG Plan");
		//planSourceRadioGroup.add(planSourceFromQueryRadio);
		//planSourceRadioGroup.add(planSourceFromJSONRadio);
		//planSourceRadioGroup.add(planSourceFromPEGRadio);
		//planSourceFromJSONRadio.setSelected(true);
		
		planSourceFromQueryPlanNameLabel = new JLabel("Plan name:");
		planSourceFromQueryPlanNameField = new JTextField(20);
		planSourceFromQueryServerLabel = new JLabel("Server:");
		planSourceFromQueryServerField = new JTextField(20);
		planSourceFromQueryPortLabel = new JLabel("Port:");
		planSourceFromQueryPortField = new JTextField(7);
		planSourceFromQueryPortField.setText("5432");
		planSourceFromQueryDatabaseLabel = new JLabel("Database:");
		planSourceFromQueryDatabaseField = new JTextField(10);
		planSourceFromQueryUserLabel = new JLabel("Username:");
		planSourceFromQueryUserField = new JTextField(10);

		planSourceFromQueryPasswordLabel = new JLabel("Password:");
		planSourceFromQueryPasswordField = new JPasswordField(10);
		planSourceFromQueryRememberPasswordLabel = new JLabel("Remember password?");
		planSourceFromQueryRememberPasswordCheckbox = new JCheckBox();

		planSourceFromQueryPreCommandLabel = new JLabel("Execute before running EXPLAIN command:");
		planSourceFromQueryPreCommandField = new JTextField(40);
		planSourceFromQueryPostCommandLabel = new JLabel("Execute after running EXPLAIN command:");
		planSourceFromQueryPostCommandField = new JTextField(20);
		
		// disable editing until all of it works
		//planSourceFromQueryPlanNameField.setEnabled(false);

		// Put all labels to array
		labels.add(planSourceFromQueryPlanNameLabel);
		labels.add(planSourceFromQueryServerLabel);
		labels.add(planSourceFromQueryPortLabel);
		labels.add(planSourceFromQueryDatabaseLabel);
		labels.add(planSourceFromQueryUserLabel);
		labels.add(planSourceFromQueryPasswordLabel);
		labels.add(planSourceFromQueryRememberPasswordLabel);
		labels.add(planSourceFromQueryPreCommandLabel);
		labels.add(planSourceFromQueryPostCommandLabel);
		
		// Put all fields to array
		fields.add(planSourceFromQueryPlanNameField);
		fields.add(planSourceFromQueryServerField);
		fields.add(planSourceFromQueryPortField);
		fields.add(planSourceFromQueryDatabaseField);
		fields.add(planSourceFromQueryUserField);
		fields.add(planSourceFromQueryPasswordField);
		fields.add(planSourceFromQueryRememberPasswordCheckbox);
		fields.add(planSourceFromQueryPreCommandField);
		fields.add(planSourceFromQueryPostCommandField);
		
		planSourceFromQuerySQLLabel = new JLabel("EXPLAIN (with options) and SQL command:");
		planSourceFromQuerySQLTextArea = new JTextArea(10, 40);
		planSourceFromQuerySQLTextArea.setText("\nexplain (analyze, buffers, format json, verbose)\n\nselect \n");
		planSourceFromQuerySQLTextAreaScrollPane = new JScrollPane(planSourceFromQuerySQLTextArea);
		
		jsonPlanTextArea = new JTextArea(10, 40);
		jsonPlanTextAreaScrollPane = new JScrollPane(jsonPlanTextArea);
		
		DocumentListener jsonPlanTextAreaDocumentListener = new DocumentListener() {

			public void insertUpdate(DocumentEvent e) {
				setPlanTextIsDirty(true);
			}

			public void removeUpdate(DocumentEvent e) {
				setPlanTextIsDirty(true);
			}

			public void changedUpdate(DocumentEvent e) {
				setPlanTextIsDirty(true);
			}

		};
		
		// changing the plan name triggers a db update
		// and plan list update
		planSourceFromQueryPlanNameField.addFocusListener(new FocusAdapter() {
			String strOldPlanName;
			int oldPlanNameIndex;
			
			public void focusGained(FocusEvent e) {
				strOldPlanName = getPlanSourceFromQueryPlanNameField();
				JSplitPane jSplitPane = (JSplitPane) objCurrentPlanTabs.getParent();
				FormPanel formPanel = (FormPanel) jSplitPane.getComponent(0);
				oldPlanNameIndex = formPanel.getSelectedIndex();
			}
			
			public void focusLost(FocusEvent e) {
				String strFieldTextEntered;
				String strNewPlanName;
				String strSQL;
				boolean bolShowProperties = false;
				boolean bolShowSQL = false;
				
				if (bolShowProperties == true) { System.out.println("PlanSourcePanel.java / planSourceFromQueryPlanNameField.addFocusListener"); }
				if (bolShowProperties == true) { System.out.println("PlanSourcePanel.java / planSourceFromQueryPlanNameField.getText(): " + planSourceFromQueryPlanNameField.getText()); }
				//strOldPlanName = e.
				if (bolShowProperties == true) { System.out.println("PlanSourcePanel.java / strOldPlanName: " + strOldPlanName); }
				strFieldTextEntered = planSourceFromQueryPlanNameField.getText();

				// exit if plan name wasn't changed
				if (strOldPlanName == null || strOldPlanName.equals(strFieldTextEntered)) {
					return;
				}
				
				// make sure the new plan name isn't empty
				if (strFieldTextEntered == null || strFieldTextEntered.equals("")) {
					strFieldTextEntered = strOldPlanName;
				}
				
				// set the new name unless there's a name collision
				strNewPlanName = strFieldTextEntered;
				
				// see if the preferred name collides with an existing name
				strSQL = "select count(*) as plan_name_exists from explain_plans where plan_name = '" + strFieldTextEntered.replace("'",  "''") + "'";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				//String strNewPlanName = Database.row(strSQL).getString("plan_name_exists");
				boolean bolPlanNameExists = (Database.row(strSQL).getInteger("plan_name_exists") == 1);
				
				if (bolPlanNameExists == true) {
					
					// generate a new plan name if necessary
					strSQL = "with cteOriginalPlanName as (\n" + 
							"	select '" + strFieldTextEntered.replace("'",  "''") + "' as plan_name\n" + 
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
					if (bolShowSQL == true) { System.out.println(strSQL); }
					strNewPlanName = Database.row(strSQL).getString("new_plan_name");
				}
				
				//strPlanName = strPlanName.replace("'",  "''");
				//strNewPlanName = strNewPlanName.replace("'",  "''");
				
				strSQL = "update explain_plans set\n"
					+ "plan_name = '" + strNewPlanName.replace("'",  "''") + "'\n"
					+ "where plan_name = '" + strOldPlanName.replace("'",  "''") + "'";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);
				
				setPlanSourceFromQueryPlanNameField(strNewPlanName);
				planSourceFromQueryPlanNameField.setText(strNewPlanName);
				// call FormPanel.setSelectedPlanName(strNewPlanName);
				if (objCurrentPlanTabs.getParent() instanceof JSplitPane) {
					JSplitPane jSplitPane = (JSplitPane) objCurrentPlanTabs.getParent();
					FormPanel formPanel = (FormPanel) jSplitPane.getComponent(0);
					formPanel.updatePlanMapper(strOldPlanName, strNewPlanName);
					formPanel.setSelectedPlanName(oldPlanNameIndex, strNewPlanName);
					//((FormPanel) ((JSplitPane) objCurrentPlanTabs.getParent()).getComponent(0)).setSelectedPlanName(strNewPlanName); // weird, but equivalent
				}
				objCurrentPlanTabs.setTitleAt(0, strNewPlanName + " - Plan Source");
				objCurrentPlanTabs.setTitleAt(2, strNewPlanName + " - Plan Diagram");
			}
		});

		//jsonPlanTextArea.addComponentListener(jsonPlanTextAreaListener);
		jsonPlanTextArea.getDocument().addDocumentListener(jsonPlanTextAreaDocumentListener);

		showPlanGraphButton = new JButton(SHOW_JSON_PLAN_DIAGRAM);
		showPlanGraphButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bolShowTiming == true) { System.out.println("before PlanSourcePanel.showPlanGraphButton.addActionListener(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
				PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, true);
				setPlanSourceButtonClicked(SHOW_JSON_PLAN_DIAGRAM);

				objCurrentPlanTabs.setEnabled(true);
				if (objCurrentPlanTabs.getTabCount() > 2) {
					objCurrentPlanTabs.setEnabledAt(2, true);
				}
				objCurrentPlanTabs.setSelectedIndex(2);
				if (bolShowTiming == true) { System.out.println("after PlanSourcePanel.showPlanGraphButton.addActionListener(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			}
		});

		DocumentListener planSourceFromQueryDocumentListener = new DocumentListener() {

			public void insertUpdate(DocumentEvent e) {
				setPlanTextIsDirty(true);
			}

			public void removeUpdate(DocumentEvent e) {
				setPlanTextIsDirty(true);
			}

			public void changedUpdate(DocumentEvent e) {
				setPlanTextIsDirty(true);
			}

		};

		planSourceFromQueryButton = new JButton(EXECUTE_AND_SHOW_PLAN_DIAGRAM);
		planSourceFromQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bolShowProperties = false;
				boolean bolShowSQL = false;
				
				if (bolShowProperties == true) { System.out.println("planSourceFromQueryPlanNameField.addFocusListener"); }
				setPlanSourceButtonClicked(EXECUTE_AND_SHOW_PLAN_DIAGRAM);
				
				// prompt for password
				if (getPlanSourceFromQueryPasswordField().equals("") == true) {

					JPasswordField objPasswordField = new JPasswordField();
					//objPasswordField.addAncestorListener( new RequestFocusListener() );
					//objPasswordField.setVisible( true );
					//objPasswordField.requestFocusInWindow();
					int okCxl = JOptionPane.showConfirmDialog(null, objPasswordField, "Enter Password for '" + getPlanSourceFromQueryUserField() + "'", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					
					if (okCxl == JOptionPane.OK_OPTION) {
						String strPassword = new String(objPasswordField.getPassword());
						if (bolShowProperties == true) { System.out.println("strPassword: " + strPassword); }
						setPlanSourceFromQueryPasswordField(strPassword);
					} else if (okCxl == JOptionPane.CANCEL_OPTION) {
						return;
					}
				}

				PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, true);
				objCurrentPlanTabs.setEnabled(true);
				if (objCurrentPlanTabs.getTabCount() > 2) {
					objCurrentPlanTabs.setEnabledAt(2, true);
				}
				objCurrentPlanTabs.setSelectedIndex(2);
				if (bolShowTiming == true) { System.out.println("after PlanSourcePanel.planSourceFromQueryButton.addActionListener(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			}
		});

		// set up mnemonics
		//okBtn.setMnemonic(KeyEvent.VK_O); // underlines "OK" but does not press "OK". Why?
		
		layoutComponents();
	}
	
	public void setShowPlanGraphButton(boolean enable) {
		//showPlanGraphButton.enable(enable);
		showPlanGraphButton.setEnabled(enable);
	}
	
	private GridBagConstraints createGbc(int x, int y) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.weightx = 0.1;
		gbc.anchor = (x == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
		gbc.insets = (x == 0) ? EAST_INSETS : WEST_INSETS;
		return gbc;
	}

	private Component displayLabels() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = null;
		for (int i = 0; i < labels.size(); i++) {
			// Label - x = 0
			Component label = labels.get(i);
			gbc = createGbc(0, i);
			panel.add(label, gbc);
		
			// Field - x = 1
			Component field = fields.get(i);
			gbc = createGbc(1, i);
			panel.add(field, gbc);

		}

		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weightx = 100;
		gbc.anchor = GridBagConstraints.LINE_END;
		panel.add(new Container(), gbc);

		return panel;
	}

	private Component displaySQLQueryInputs() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;

		gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 1;
	    gbc.anchor = GridBagConstraints.WEST;
	    gbc.insets = WEST_INSETS;
	    panel.add(planSourceFromQuerySQLLabel, gbc);
		
	    gbc.gridy = 1;
	    panel.add(planSourceFromQuerySQLTextAreaScrollPane, gbc);
		
	    gbc.gridy = 2;
	    panel.add(planSourceFromQueryButton, gbc);
		
		return panel;
	}
		
	private Component displayJSONInputs() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		
		gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 1;
	    gbc.anchor = GridBagConstraints.WEST;
	    gbc.insets = WEST_INSETS;
	    panel.add(jsonPlanTextAreaScrollPane, gbc);

	    gbc.gridy = 1;
	    panel.add(showPlanGraphButton, gbc);

	    //gbc.gridy = 2;
	    //panel.add(planSourceFromPEGRadio, gbc);
		
		return panel;
	}
		
	public void layoutComponents() {
		setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS | BoxLayout.Y_AXIS));
		
	    panel.add(displayLabels());
	    panel.add(displaySQLQueryInputs());
	    panel.add(displayJSONInputs());

	    JScrollPane js = new JScrollPane(panel);
	    js.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    js.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

	    add(js, BorderLayout.CENTER);
	}
/*
	public void setPlanName(String planName) {
		this.planName = planName;
	}
	
	public String getPlanName() {
		return planName;
	}
*/
/*
	public int getIdExplainPlan() {
		return idExplainPlan;
	}
	
	public void setIdExplainPlan(int idExplainPlan) {
		this.idExplainPlan = idExplainPlan;
	}
*/
	public String getJsonPlanText() {
		//return jsonText;
		return jsonPlanTextArea.getText();
	}
	
	public void setPlanSourceFromQueryPlanNameField(String strPlanName) {
		planSourceFromQueryPlanNameField.setText(strPlanName);
	}

	public String getPlanSourceFromQueryPlanNameField() {
		return planSourceFromQueryPlanNameField.getText();
	}

	public void setPlanSourceFromQueryServerField(String strServer) {
		planSourceFromQueryServerField.setText(strServer);
	}

	public String getPlanSourceFromQueryServerField() {
		return planSourceFromQueryServerField.getText();
	}

	public void setPlanSourceFromQueryPortField(String strPort) {
		planSourceFromQueryPortField.setText(strPort);
	}

	public String getPlanSourceFromQueryPortField() {
		return planSourceFromQueryPortField.getText();
	}

	public void setPlanSourceFromQueryDatabaseField(String strDatabase) {
		planSourceFromQueryDatabaseField.setText(strDatabase);
	}

	public String getPlanSourceFromQueryDatabaseField() {
		return planSourceFromQueryDatabaseField.getText();
	}

	public void setPlanSourceFromQueryUserField(String strUser) {
		planSourceFromQueryUserField.setText(strUser);
	}

	public String getPlanSourceFromQueryUserField() {
		return planSourceFromQueryUserField.getText();
	}

	public void setPlanSourceFromQueryPasswordField(String strPassword) {
		planSourceFromQueryPasswordField.setText(strPassword);
	}

	public String getPlanSourceFromQueryPasswordField() {
		return planSourceFromQueryPasswordField.getText();
	}

	public void setPlanSourceFromQueryRememberPasswordCheckbox(boolean selected) {
		planSourceFromQueryRememberPasswordCheckbox.setSelected(selected);
	}

	public boolean getPlanSourceFromQueryRememberPasswordCheckbox() {
		return planSourceFromQueryRememberPasswordCheckbox.isSelected();
	}

	public void setPlanSourceFromQueryPreCommandField(String strPreCommand) {
		planSourceFromQueryPreCommandField.setText(strPreCommand);
	}

	public String getPlanSourceFromQueryPreCommandField() {
		return planSourceFromQueryPreCommandField.getText();
	}

	public String getPlanSourceFromQueryPostCommandField() {
		return planSourceFromQueryPostCommandField.getText();
	}
	
	public void setJsonPlanText(String planText) {
		//this.jsonText = planText;
		jsonPlanTextArea.setText(planText);
	}

	public void setSQLQueryText(String strQueryText) {
		//this.queryText = strQueryText;
		planSourceFromQuerySQLTextArea.setText(strQueryText);
		//jsonPlanTextArea.setText(strQueryText);
	}

	public String getSQLQueryText() {
		//return this.queryText;
		return planSourceFromQuerySQLTextArea.getText();
	}

	public void setPlanSourceFromQueryPostCommandField(String postCommand) {
		//return planSourceFromQueryPostCommandField.getText();
		planSourceFromQueryPostCommandField.setText(postCommand);
	}
	
	public String getPlanSourceButtonClicked() {
		return planSourceButtonClicked;
	}

	public void setPlanSourceButtonClicked(String planSourceButtonClicked) {
		this.planSourceButtonClicked = planSourceButtonClicked;
	}

	public void setPlanTextIsDirty(boolean planTextIsDirty) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("setPlanTextIsDirty(" + planTextIsDirty + ")"); }
		// Keep Plan Diagram tab enabled so selecting it can generate the diagram
		if (objCurrentPlanTabs != null && objCurrentPlanTabs.getTabCount() > 2) {
			objCurrentPlanTabs.setEnabledAt(2, true);
		}
		this.planTextIsDirty = planTextIsDirty;
	}

	public boolean getPlanTextIsDirty() {
		return planTextIsDirty;
	}

	/** Strip psql wrappers the same way Plan Diagram construction does. */
	public static String normalizeJsonPlanText(String planText) {
		if (planText == null) {
			return "";
		}
		String normalized = planText;
		normalized = normalized.replaceAll(" {5,}", " ");
		normalized = normalized.replaceAll("^ *" + Pattern.quote("QUERY PLAN"), "");
		normalized = normalized.replaceAll("\\-{10,}", "");
		normalized = normalized.replaceAll(Pattern.quote("+") + " *[\r\n]", System.getProperty("line.separator"));
		normalized = normalized.replaceAll(Pattern.quote("(") + "\\d+ row.*$", "");
		return normalized;
	}

	/** True when the diagram has never been built, or the JSON text differs from the last build. */
	public boolean shouldRebuildDiagramFromCurrentJson() {
		return shouldRebuildDiagramFromJson(normalizeJsonPlanText(getJsonPlanText()));
	}

	public boolean shouldRebuildDiagramFromJson(String normalizedJson) {
		return jsonRenderedInDiagram == null || !Objects.equals(jsonRenderedInDiagram, normalizedJson);
	}

	public void markDiagramRenderedFromJson(String normalizedJson) {
		jsonRenderedInDiagram = normalizedJson == null ? "" : normalizedJson;
	}

	public void setLineThicknessByIsDirty(boolean lineThicknessByIsDirty) {
		this.lineThicknessByIsDirty = lineThicknessByIsDirty;
	}

	public boolean getLineThicknessByIsDirty() {
		return this.lineThicknessByIsDirty;
	}

	public void setZoomPercentageIsDirty(boolean zoomPercentageIsDirty) {
		this.zoomPercentageIsDirty = zoomPercentageIsDirty;
	}

	public boolean getZoomPercentageIsDirty() {
		return this.zoomPercentageIsDirty;
	}

	public void setPercentageByIsDirty(boolean percentageByIsDirty) {
		this.percentageByIsDirty = percentageByIsDirty;
	}

	public boolean getPercentageByIsDirty() {
		return this.percentageByIsDirty;
	}

	public void setWorkerIsDirty(boolean workerIsDirty) {
		this.workerIsDirty = workerIsDirty;
	}

	public boolean getWorkerIsDirty() {
		return this.workerIsDirty;
	}

	
}

