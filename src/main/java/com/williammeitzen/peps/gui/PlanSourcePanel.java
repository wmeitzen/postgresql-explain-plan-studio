package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.williammeitzen.peps.model.Database;

public class PlanSourcePanel extends JPanel {

	public static final String SHOW_JSON_PLAN_DIAGRAM = "Show JSON Plan Diagram";
	public static final String EXECUTE_AND_SHOW_PLAN_DIAGRAM = "Execute and Show Plan Diagram";
	public static final String AUTH_USERNAME_PASSWORD = "Username/password";
	public static final String AUTH_TRUST = "Trust / empty password";
	/** Values stored in explain_plans.auth_type */
	public static final String AUTH_TYPE_PASSWORD = "password";
	public static final String AUTH_TYPE_TRUST = "trust";
	
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
	private JCheckBox planSourceFromQueryRememberPasswordCheckbox;

	private JLabel connectByLabel;
	private JRadioButton connectByHostRadio;
	private JRadioButton connectByUrlRadio;
	private JLabel jdbcUrlLabel;
	private JTextField jdbcUrlField;
	private JLabel authTypeLabel;
	private JComboBox<String> authTypeCombo;
	private boolean syncingConnectionFields = false;

	private JLabel planSourceFromQueryPreCommandLabel;
	private JLabel planSourceFromQueryPostCommandLabel;
	private JTextField planSourceFromQueryPreCommandField;
	private JTextArea planSourceFromQuerySQLTextArea;
	private JTextField planSourceFromQueryPostCommandField;
	private JLabel planSourceFromQuerySQLLabel;
	private JLabel jsonPlanLabel;
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

	private static final Pattern JDBC_URL_PATTERN = Pattern.compile(
			"(?i)^jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]*)");

	private final Insets LABEL_INSETS = new Insets(4, 8, 4, 8);
	private final Insets FIELD_INSETS = new Insets(4, 0, 4, 8);
	private final Insets SECTION_INSETS = new Insets(14, 8, 6, 8);

	/** Same wheel step as Plan Diagram / OperatorDetailsPanel scrolling. */
	private static final int WHEEL_UNIT_INCREMENT = 48;

	/** Hybrid widths: connection fields stay compact; SQL/JSON can be wider. */
	private static final int FIELD_WIDTH = 360;
	private static final int HOST_WIDTH = 280;
	private static final int PORT_WIDTH = 72;
	private static final int PASSWORD_WIDTH = 280;
	private static final int COMBO_WIDTH = 220;
	private static final int AREA_WIDTH = 680;
	private static final int AREA_HEIGHT = 150;
	
	public PlanSourcePanel(JTabbedPane objCurrentPlanTabs) {
		boolean bolShowTiming = false;
		Dimension dim = getPreferredSize();
		dim.width = 300;
		setMinimumSize(dim);
		
		this.objCurrentPlanTabs = objCurrentPlanTabs;
		
		planSourceFromQueryPlanNameLabel = new JLabel("Plan name:");
		planSourceFromQueryPlanNameField = new JTextField(24);

		connectByLabel = new JLabel("Connect by:");
		connectByHostRadio = new JRadioButton("Host");
		connectByUrlRadio = new JRadioButton("URL");
		ButtonGroup connectByGroup = new ButtonGroup();
		connectByGroup.add(connectByHostRadio);
		connectByGroup.add(connectByUrlRadio);
		connectByHostRadio.setSelected(true);

		jdbcUrlLabel = new JLabel("URL:");
		jdbcUrlField = new JTextField(36);
		jdbcUrlField.setEnabled(false);

		planSourceFromQueryServerLabel = new JLabel("Host:");
		planSourceFromQueryServerField = new JTextField(24);
		planSourceFromQueryPortLabel = new JLabel("Port:");
		planSourceFromQueryPortField = new JTextField(7);
		planSourceFromQueryPortField.setText("5432");
		planSourceFromQueryDatabaseLabel = new JLabel("Database:");
		planSourceFromQueryDatabaseField = new JTextField(18);

		authTypeLabel = new JLabel("Authentication:");
		authTypeCombo = new JComboBox<>(new String[] { AUTH_USERNAME_PASSWORD, AUTH_TRUST });
		authTypeCombo.setSelectedItem(AUTH_USERNAME_PASSWORD);
		authTypeCombo.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				updateAuthenticationMode();
			}
		});

		planSourceFromQueryUserLabel = new JLabel("Username:");
		planSourceFromQueryUserField = new JTextField(18);

		planSourceFromQueryPasswordLabel = new JLabel("Password:");
		planSourceFromQueryPasswordField = new JPasswordField(18);
		planSourceFromQueryRememberPasswordCheckbox = new JCheckBox("Save password");

		planSourceFromQueryPreCommandLabel = new JLabel("Before EXPLAIN:");
		planSourceFromQueryPreCommandField = new JTextField(36);
		planSourceFromQueryPostCommandLabel = new JLabel("After EXPLAIN:");
		planSourceFromQueryPostCommandField = new JTextField(36);
		
		planSourceFromQuerySQLLabel = new JLabel("EXPLAIN (with options) and SQL command:");
		planSourceFromQuerySQLTextArea = new JTextArea(8, 40);
		planSourceFromQuerySQLTextArea.setText("\nexplain (analyze, buffers, format json, verbose)\n\nselect \n");
		planSourceFromQuerySQLTextAreaScrollPane = new JScrollPane(planSourceFromQuerySQLTextArea);

		jsonPlanLabel = new JLabel("JSON plan:");
		jsonPlanTextArea = new JTextArea(8, 40);
		jsonPlanTextAreaScrollPane = new JScrollPane(jsonPlanTextArea);

		DocumentListener hostFieldsListener = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { syncJdbcUrlFromHostFields(); }
			public void removeUpdate(DocumentEvent e) { syncJdbcUrlFromHostFields(); }
			public void changedUpdate(DocumentEvent e) { syncJdbcUrlFromHostFields(); }
		};
		planSourceFromQueryServerField.getDocument().addDocumentListener(hostFieldsListener);
		planSourceFromQueryPortField.getDocument().addDocumentListener(hostFieldsListener);
		planSourceFromQueryDatabaseField.getDocument().addDocumentListener(hostFieldsListener);
		jdbcUrlField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { syncHostFieldsFromJdbcUrl(); }
			public void removeUpdate(DocumentEvent e) { syncHostFieldsFromJdbcUrl(); }
			public void changedUpdate(DocumentEvent e) { syncHostFieldsFromJdbcUrl(); }
		});
		connectByHostRadio.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				updateConnectByMode();
			}
		});
		connectByUrlRadio.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				updateConnectByMode();
			}
		});
		syncJdbcUrlFromHostFields();
		updateConnectByMode();
		
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
				FormPanel formPanel = findFormPanel();
				oldPlanNameIndex = formPanel != null ? formPanel.getSelectedIndex() : -1;
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
				FormPanel formPanel = findFormPanel();
				if (formPanel != null) {
					formPanel.updatePlanMapper(strOldPlanName, strNewPlanName);
					formPanel.setSelectedPlanName(oldPlanNameIndex, strNewPlanName);
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
				if (!canOpenPlanDiagramTab()) {
					return;
				}
				PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, true);
				setPlanSourceButtonClicked(SHOW_JSON_PLAN_DIAGRAM);

				objCurrentPlanTabs.setEnabled(true);
				objCurrentPlanTabs.setEnabledAt(2, true);
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
				
				// prompt for password unless Trust / empty password is selected
				if (!isTrustAuthentication() && getPlanSourceFromQueryPasswordField().equals("")) {

					JPasswordField objPasswordField = new JPasswordField();
					int okCxl = JOptionPane.showConfirmDialog(null, objPasswordField, "Enter Password for '" + getPlanSourceFromQueryUserField() + "'", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					
					if (okCxl == JOptionPane.OK_OPTION) {
						String strPassword = new String(objPasswordField.getPassword());
						if (bolShowProperties == true) { System.out.println("strPassword: " + strPassword); }
						setPlanSourceFromQueryPasswordField(strPassword);
					} else if (okCxl == JOptionPane.CANCEL_OPTION) {
						return;
					}
				}

				if (!canOpenPlanDiagramTab()) {
					return;
				}
				PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, true);
				objCurrentPlanTabs.setEnabled(true);
				objCurrentPlanTabs.setEnabledAt(2, true);
				objCurrentPlanTabs.setSelectedIndex(2);
				if (bolShowTiming == true) { System.out.println("after PlanSourcePanel.planSourceFromQueryButton.addActionListener(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			}
		});

		// set up mnemonics
		//okBtn.setMnemonic(KeyEvent.VK_O); // underlines "OK" but does not press "OK". Why?
		
		layoutComponents();
	}
	
	public void setShowPlanGraphButton(boolean enable) {
		showPlanGraphButton.setEnabled(enable);
	}

	private JLabel createSectionHeader(String title) {
		JLabel header = new JLabel(title);
		header.setFont(header.getFont().deriveFont(Font.BOLD, header.getFont().getSize2D() + 1f));
		return header;
	}

	private GridBagConstraints labelGbc(int x, int y) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = LABEL_INSETS;
		return gbc;
	}

	private GridBagConstraints fieldGbc(int x, int y, int width) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = FIELD_INSETS;
		return gbc;
	}

	private GridBagConstraints sectionGbc(int y) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 5;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = SECTION_INSETS;
		return gbc;
	}

	private void sizeField(Component field, int width) {
		int height = Math.max(field.getPreferredSize().height, 24);
		Dimension size = new Dimension(width, height);
		field.setPreferredSize(size);
		field.setMinimumSize(new Dimension(Math.min(120, width), height));
		field.setMaximumSize(size);
	}

	private void sizeTextArea(JScrollPane scrollPane, int width, int height) {
		Dimension size = new Dimension(width, height);
		scrollPane.setPreferredSize(size);
		scrollPane.setMinimumSize(new Dimension(Math.min(280, width), 80));
		scrollPane.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
		applyWheelUnitIncrement(scrollPane);
	}

	private static void applyWheelUnitIncrement(JScrollPane scrollPane) {
		scrollPane.getVerticalScrollBar().setUnitIncrement(WHEEL_UNIT_INCREMENT);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(WHEEL_UNIT_INCREMENT);
	}

	private JPanel buildConnectionForm() {
		sizeField(planSourceFromQueryPlanNameField, FIELD_WIDTH);
		sizeField(jdbcUrlField, FIELD_WIDTH);
		sizeField(planSourceFromQueryServerField, HOST_WIDTH);
		sizeField(planSourceFromQueryPortField, PORT_WIDTH);
		sizeField(planSourceFromQueryDatabaseField, FIELD_WIDTH);
		sizeField(authTypeCombo, COMBO_WIDTH);
		sizeField(planSourceFromQueryUserField, FIELD_WIDTH);
		sizeField(planSourceFromQueryPasswordField, PASSWORD_WIDTH);
		sizeField(planSourceFromQueryPreCommandField, FIELD_WIDTH);
		sizeField(planSourceFromQueryPostCommandField, FIELD_WIDTH);
		sizeTextArea(planSourceFromQuerySQLTextAreaScrollPane, AREA_WIDTH, AREA_HEIGHT);
		sizeTextArea(jsonPlanTextAreaScrollPane, AREA_WIDTH, AREA_HEIGHT);

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;

		panel.add(createSectionHeader("Plan"), sectionGbc(row++));
		panel.add(planSourceFromQueryPlanNameLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryPlanNameField, fieldGbc(1, row++, 3));

		panel.add(createSectionHeader("Server"), sectionGbc(row++));

		panel.add(connectByLabel, labelGbc(0, row));
		JPanel connectByPanel = new JPanel();
		connectByPanel.setLayout(new BoxLayout(connectByPanel, BoxLayout.X_AXIS));
		connectByPanel.setOpaque(false);
		connectByPanel.add(connectByHostRadio);
		connectByPanel.add(Box.createHorizontalStrut(12));
		connectByPanel.add(connectByUrlRadio);
		panel.add(connectByPanel, fieldGbc(1, row++, 3));

		panel.add(jdbcUrlLabel, labelGbc(0, row));
		panel.add(jdbcUrlField, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryServerLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryServerField, fieldGbc(1, row, 1));
		panel.add(planSourceFromQueryPortLabel, labelGbc(2, row));
		panel.add(planSourceFromQueryPortField, fieldGbc(3, row++, 1));

		panel.add(planSourceFromQueryDatabaseLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryDatabaseField, fieldGbc(1, row++, 3));

		panel.add(createSectionHeader("Authentication"), sectionGbc(row++));

		panel.add(authTypeLabel, labelGbc(0, row));
		panel.add(authTypeCombo, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryUserLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryUserField, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryPasswordLabel, labelGbc(0, row));
		JPanel passwordRow = new JPanel();
		passwordRow.setLayout(new BoxLayout(passwordRow, BoxLayout.X_AXIS));
		passwordRow.setOpaque(false);
		passwordRow.add(planSourceFromQueryPasswordField);
		passwordRow.add(Box.createHorizontalStrut(10));
		passwordRow.add(planSourceFromQueryRememberPasswordCheckbox);
		panel.add(passwordRow, fieldGbc(1, row++, 3));

		panel.add(createSectionHeader("Commands"), sectionGbc(row++));

		panel.add(planSourceFromQueryPreCommandLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryPreCommandField, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryPostCommandLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryPostCommandField, fieldGbc(1, row++, 3));

		GridBagConstraints sqlLabelGbc = fieldGbc(1, row++, 3);
		sqlLabelGbc.insets = new Insets(10, 0, 2, 8);
		panel.add(planSourceFromQuerySQLLabel, sqlLabelGbc);

		GridBagConstraints sqlAreaGbc = fieldGbc(1, row++, 3);
		sqlAreaGbc.weighty = 0.45;
		sqlAreaGbc.fill = GridBagConstraints.VERTICAL;
		sqlAreaGbc.insets = new Insets(0, 0, 4, 8);
		panel.add(planSourceFromQuerySQLTextAreaScrollPane, sqlAreaGbc);

		panel.add(planSourceFromQueryButton, fieldGbc(1, row++, 3));

		GridBagConstraints jsonLabelGbc = fieldGbc(1, row++, 3);
		jsonLabelGbc.insets = new Insets(10, 0, 2, 8);
		panel.add(jsonPlanLabel, jsonLabelGbc);

		GridBagConstraints jsonAreaGbc = fieldGbc(1, row++, 3);
		jsonAreaGbc.weighty = 0.45;
		jsonAreaGbc.fill = GridBagConstraints.VERTICAL;
		jsonAreaGbc.insets = new Insets(0, 0, 4, 8);
		panel.add(jsonPlanTextAreaScrollPane, jsonAreaGbc);

		panel.add(showPlanGraphButton, fieldGbc(1, row++, 3));

		// Absorb extra horizontal space so fields stay left-aligned at hybrid widths.
		GridBagConstraints hSpacer = new GridBagConstraints();
		hSpacer.gridx = 4;
		hSpacer.gridy = 0;
		hSpacer.weightx = 1.0;
		hSpacer.fill = GridBagConstraints.HORIZONTAL;
		panel.add(Box.createHorizontalGlue(), hSpacer);

		GridBagConstraints vSpacer = new GridBagConstraints();
		vSpacer.gridx = 0;
		vSpacer.gridy = row;
		vSpacer.gridwidth = 5;
		vSpacer.weighty = 0.01;
		vSpacer.fill = GridBagConstraints.VERTICAL;
		panel.add(Box.createVerticalGlue(), vSpacer);

		return panel;
	}

	private void updateConnectByMode() {
		boolean hostMode = connectByHostRadio.isSelected();
		planSourceFromQueryServerField.setEnabled(hostMode);
		planSourceFromQueryPortField.setEnabled(hostMode);
		planSourceFromQueryDatabaseField.setEnabled(hostMode);
		jdbcUrlField.setEnabled(!hostMode);
		if (hostMode) {
			syncJdbcUrlFromHostFields();
		}
	}

	private void updateAuthenticationMode() {
		boolean trust = isTrustAuthentication();
		planSourceFromQueryPasswordField.setEnabled(!trust);
		planSourceFromQueryRememberPasswordCheckbox.setEnabled(!trust);
		if (trust) {
			planSourceFromQueryPasswordField.setText("");
			planSourceFromQueryRememberPasswordCheckbox.setSelected(false);
		}
	}

	public boolean isTrustAuthentication() {
		return AUTH_TRUST.equals(authTypeCombo.getSelectedItem());
	}

	public String getAuthenticationTypeCode() {
		return isTrustAuthentication() ? AUTH_TYPE_TRUST : AUTH_TYPE_PASSWORD;
	}

	public void setAuthenticationTypeCode(String authTypeCode) {
		if (AUTH_TYPE_TRUST.equalsIgnoreCase(authTypeCode)) {
			authTypeCombo.setSelectedItem(AUTH_TRUST);
		} else {
			authTypeCombo.setSelectedItem(AUTH_USERNAME_PASSWORD);
		}
		updateAuthenticationMode();
	}

	private void syncJdbcUrlFromHostFields() {
		if (syncingConnectionFields || connectByUrlRadio.isSelected()) {
			return;
		}
		syncingConnectionFields = true;
		try {
			String host = safeText(planSourceFromQueryServerField);
			String port = safeText(planSourceFromQueryPortField);
			String database = safeText(planSourceFromQueryDatabaseField);
			if (port.isBlank()) {
				port = "5432";
			}
			jdbcUrlField.setText("jdbc:postgresql://" + host + ":" + port + "/" + database);
		} finally {
			syncingConnectionFields = false;
		}
	}

	private void syncHostFieldsFromJdbcUrl() {
		if (syncingConnectionFields || connectByHostRadio.isSelected()) {
			return;
		}
		Matcher matcher = JDBC_URL_PATTERN.matcher(safeText(jdbcUrlField).trim());
		if (!matcher.matches()) {
			return;
		}
		syncingConnectionFields = true;
		try {
			planSourceFromQueryServerField.setText(matcher.group(1));
			String port = matcher.group(2);
			planSourceFromQueryPortField.setText(port == null || port.isBlank() ? "5432" : port);
			planSourceFromQueryDatabaseField.setText(matcher.group(3) == null ? "" : matcher.group(3));
		} finally {
			syncingConnectionFields = false;
		}
	}

	private static String safeText(JTextField field) {
		return field.getText() == null ? "" : field.getText().trim();
	}
		
	public void layoutComponents() {
		removeAll();
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel form = buildConnectionForm();
		JScrollPane js = new JScrollPane(form);
		js.setBorder(BorderFactory.createEmptyBorder());
		js.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		js.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		applyWheelUnitIncrement(js);
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
		syncJdbcUrlFromHostFields();
	}

	public String getPlanSourceFromQueryServerField() {
		return planSourceFromQueryServerField.getText();
	}

	public void setPlanSourceFromQueryPortField(String strPort) {
		planSourceFromQueryPortField.setText(strPort);
		syncJdbcUrlFromHostFields();
	}

	public String getPlanSourceFromQueryPortField() {
		return planSourceFromQueryPortField.getText();
	}

	public void setPlanSourceFromQueryDatabaseField(String strDatabase) {
		planSourceFromQueryDatabaseField.setText(strDatabase);
		syncJdbcUrlFromHostFields();
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
		if (isTrustAuthentication()) {
			return "";
		}
		return planSourceFromQueryPasswordField.getText();
	}

	public void setPlanSourceFromQueryRememberPasswordCheckbox(boolean selected) {
		planSourceFromQueryRememberPasswordCheckbox.setSelected(selected);
	}

	public boolean getPlanSourceFromQueryRememberPasswordCheckbox() {
		if (isTrustAuthentication()) {
			return false;
		}
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
		// Keep Plan Diagram enabled only for a single selection (multi-select stays greyed out).
		FormPanel formPanel = findFormPanel();
		if (formPanel != null) {
			formPanel.updatePlanDiagramTabEnabled();
		}
		this.planTextIsDirty = planTextIsDirty;
	}

	private boolean canOpenPlanDiagramTab() {
		if (objCurrentPlanTabs == null || objCurrentPlanTabs.getTabCount() <= 2) {
			return false;
		}
		FormPanel formPanel = findFormPanel();
		return formPanel != null && formPanel.isSinglePlanSelected();
	}

	/** FormPanel is a docking sibling of the tab strip, not a JSplitPane child anymore. */
	private FormPanel findFormPanel() {
		if (objCurrentPlanTabs == null) {
			return null;
		}
		Window window = SwingUtilities.getWindowAncestor(objCurrentPlanTabs);
		if (window == null) {
			return findFormPanelInHierarchy(objCurrentPlanTabs);
		}
		return findFormPanelInHierarchy(window);
	}

	private static FormPanel findFormPanelInHierarchy(Component root) {
		if (root instanceof FormPanel) {
			return (FormPanel) root;
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				FormPanel found = findFormPanelInHierarchy(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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

