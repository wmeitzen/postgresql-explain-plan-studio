package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.williammeitzen.peps.model.Database;
import com.williammeitzen.peps.model.ExplainTextPlanConverter;
import com.williammeitzen.peps.model.ExplainTextPlanConverter.ExplainTextPlanConvertException;
import com.williammeitzen.peps.model.PlanFormat;

public class PlanSourcePanel extends JPanel {

	/** Stable action id (not the visible button caption). */
	public static final String SHOW_JSON_PLAN_DIAGRAM = "SHOW_PLAN_DIAGRAM";
	/** Stable action id (not the visible button caption). */
	public static final String EXECUTE_AND_SHOW_PLAN_DIAGRAM = "EXECUTE_AND_SHOW_PLAN_DIAGRAM";
	public static final String AUTH_USERNAME_PASSWORD = "Username/password";
	public static final String AUTH_TRUST = "Trust / empty password";
	/** Values stored in explain_plans.auth_type */
	public static final String AUTH_TYPE_PASSWORD = "password";
	public static final String AUTH_TYPE_TRUST = "trust";

	/** Plan Source UI: plan name + pasted plan only (no connection / EXPLAIN). */
	public static final String SOURCE_MODE_PASTE = "paste";
	/** Plan Source UI: full connection + EXPLAIN fields. */
	public static final String SOURCE_MODE_CONNECT = "connect";
	
	//private JButton okBtn;
	private FormListener formListener;
	//private ButtonGroup planSourceRadioGroup;
	//private JRadioButton planSourceFromQueryRadio;
	//private JRadioButton planSourceFromJSONRadio;
	private JTextArea jsonPlanTextArea;
	private JScrollPane jsonPlanTextAreaScrollPane;
	private Timer planFormatChromeTimer;
	//private JRadioButton planSourceFromPEGRadio;
	private int idExplainPlan;
	private JButton showPlanGraphButton;
	private JLabel planFolderLabel;
	private JComboBox<FormPanel.FolderChoice> planFolderCombo;
	private JButton planFolderSaveBtn;
	private Integer savedPlanFolderId;
	private boolean syncingPlanFolderCombo;
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
	private JLabel sourceModeLabel;
	private JRadioButton sourceModePasteRadio;
	private JRadioButton sourceModeConnectRadio;
	private JLabel sectionHeaderServer;
	private JLabel sectionHeaderAuthentication;
	private JLabel sectionHeaderCommands;
	private JPanel connectByPanel;
	private JPanel passwordRow;
	/** Components shown only in Connect & run EXPLAIN mode. */
	private Component[] connectModeOnlyComponents;
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

	private static final Color JSON_ERROR_BORDER = new Color(0xC0, 0x28, 0x28);
	private static final Color JSON_ERROR_BORDER_PEAK = new Color(0xFF, 0x45, 0x45);
	private Border jsonPlanDefaultBorder;
	private Timer jsonErrorPulseTimer;
	private boolean jsonPlanShowingError = false;

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
		
		planFolderLabel = new JLabel("Folder:");
		planFolderCombo = new JComboBox<>();
		planFolderSaveBtn = new JButton("Save");
		planFolderSaveBtn.setEnabled(false);
		planFolderSaveBtn.setToolTipText("Move this plan into the selected folder");
		planFolderCombo.addActionListener(e -> updatePlanFolderSaveEnabled());
		planFolderSaveBtn.addActionListener(e -> savePlanFolderSelection());

		planSourceFromQueryPlanNameLabel = new JLabel("Plan name:");
		planSourceFromQueryPlanNameField = new JTextField(24);

		sourceModeLabel = new JLabel("Show:");
		sourceModePasteRadio = new JRadioButton("Plan text only");
		sourceModeConnectRadio = new JRadioButton("Connection info and plan");
		ButtonGroup sourceModeGroup = new ButtonGroup();
		sourceModeGroup.add(sourceModePasteRadio);
		sourceModeGroup.add(sourceModeConnectRadio);
		sourceModePasteRadio.setSelected(true);
		sourceModePasteRadio.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				applySourceModeVisibility(true);
			}
		});
		sourceModeConnectRadio.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				applySourceModeVisibility(true);
			}
		});

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
		Utils.installCrossPlatformEditBindings(planSourceFromQuerySQLTextArea);
		planSourceFromQuerySQLTextAreaScrollPane = new JScrollPane(planSourceFromQuerySQLTextArea);

		jsonPlanLabel = new JLabel("Text plan or JSON plan:");
		jsonPlanTextArea = new JTextArea(8, 40);
		Utils.installCrossPlatformEditBindings(jsonPlanTextArea);
		jsonPlanTextAreaScrollPane = new JScrollPane(jsonPlanTextArea);
		jsonPlanDefaultBorder = jsonPlanTextAreaScrollPane.getBorder();

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
				clearJsonPlanErrorStyle();
				setPlanTextIsDirty(true);
				schedulePlanFormatChromeRefresh();
			}

			public void removeUpdate(DocumentEvent e) {
				clearJsonPlanErrorStyle();
				setPlanTextIsDirty(true);
				schedulePlanFormatChromeRefresh();
			}

			public void changedUpdate(DocumentEvent e) {
				clearJsonPlanErrorStyle();
				setPlanTextIsDirty(true);
				schedulePlanFormatChromeRefresh();
			}

		};
		
		// changing the plan name triggers a db update
		// and plan list update
		planSourceFromQueryPlanNameField.addFocusListener(new FocusAdapter() {
			String strOldPlanName;
			int oldPlanNameIndex;
			Integer oldPlanId;
			
			public void focusGained(FocusEvent e) {
				strOldPlanName = getPlanSourceFromQueryPlanNameField();
				FormPanel formPanel = findFormPanel();
				oldPlanNameIndex = formPanel != null ? formPanel.getSelectedIndex() : -1;
				// this panel's plan (same id as its PlanGraphPanel.getIdExplainPlan()); fall back to the selected plan
				oldPlanId = formPanel != null ? formPanel.findPlanIdForSource(PlanSourcePanel.this) : null;
				if (oldPlanId == null && formPanel != null) {
					oldPlanId = formPanel.getSelectedPlanId();
				}
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
				
				// plan names are unique within a folder only: find this plan's folder
				Integer renamePlanId = oldPlanId;
				Object planFolderIdValue = null;
				if (renamePlanId != null) {
					org.json.simple.JsonObject folderRow = Database.row("select folder_id from explain_plans where id_explain_plan = " + renamePlanId.intValue());
					planFolderIdValue = folderRow == null ? null : folderRow.get("folder_id");
				}
				String strFolderScopeSQL = "ifnull(folder_id, -1) = "
						+ (planFolderIdValue instanceof Number ? String.valueOf(((Number) planFolderIdValue).intValue()) : "-1");

				// see if the preferred name collides with an existing name in the same folder
				strSQL = "select count(*) as plan_name_exists from explain_plans where plan_name = '" + strFieldTextEntered.replace("'",  "''") + "'\n"
						+ "and " + strFolderScopeSQL;
				if (renamePlanId != null) {
					strSQL += "\nand id_explain_plan <> " + renamePlanId.intValue();
				}
				if (bolShowSQL == true) { System.out.println(strSQL); }
				//String strNewPlanName = Database.row(strSQL).getString("plan_name_exists");
				boolean bolPlanNameExists = (Database.row(strSQL).getInteger("plan_name_exists") > 0);
				
				if (bolPlanNameExists == true) {
					// "Name (N)" — same numbering used by Duplicate
					strSQL = "select plan_name from explain_plans where " + strFolderScopeSQL;
					if (renamePlanId != null) {
						strSQL += " and id_explain_plan <> " + renamePlanId.intValue();
					}
					if (bolShowSQL == true) { System.out.println(strSQL); }
					java.util.List<String> existingNames = new java.util.ArrayList<>();
					org.json.simple.JsonArray nameRows = Database.rows(strSQL);
					if (nameRows != null) {
						for (int i = 0; i < nameRows.size(); i++) {
							Object value = ((org.json.simple.JsonObject) nameRows.get(i)).get("plan_name");
							if (value != null) {
								existingNames.add(String.valueOf(value));
							}
						}
					}
					strNewPlanName = FormPanel.nextCopyName(strFieldTextEntered, existingNames);
				}
				
				//strPlanName = strPlanName.replace("'",  "''");
				//strNewPlanName = strNewPlanName.replace("'",  "''");
				
				strSQL = "update explain_plans set\n"
					+ "plan_name = '" + strNewPlanName.replace("'",  "''") + "'\n";
				if (renamePlanId != null) {
					strSQL += "where id_explain_plan = " + renamePlanId.intValue();
				} else {
					strSQL += "where plan_name = '" + strOldPlanName.replace("'",  "''") + "' and " + strFolderScopeSQL;
				}
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);
				
				setPlanSourceFromQueryPlanNameField(strNewPlanName);
				planSourceFromQueryPlanNameField.setText(strNewPlanName);
				// call FormPanel.setSelectedPlanName(strNewPlanName);
				FormPanel formPanel = findFormPanel();
				if (formPanel != null) {
					if (renamePlanId != null) {
						// update tree node + plansMapper entry for this plan id
						formPanel.applyPlanRename(renamePlanId.intValue(), strNewPlanName);
					} else {
						formPanel.updatePlanMapper(strOldPlanName, strNewPlanName);
						formPanel.setSelectedPlanName(oldPlanNameIndex, strNewPlanName);
					}
				}
				objCurrentPlanTabs.setTitleAt(0, strNewPlanName + " - Plan Source");
				objCurrentPlanTabs.setTitleAt(2, strNewPlanName + " - Plan Diagram");
			}
		});

		//jsonPlanTextArea.addComponentListener(jsonPlanTextAreaListener);
		jsonPlanTextArea.getDocument().addDocumentListener(jsonPlanTextAreaDocumentListener);

		showPlanGraphButton = new JButton("Show JSON Plan Diagram");
		showPlanGraphButton.setEnabled(false);
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

		planSourceFromQueryButton = new JButton("Execute and Show Plan Diagram");
		planSourceFromQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean bolShowProperties = false;
				
				if (bolShowProperties == true) { System.out.println("planSourceFromQueryPlanNameField.addFocusListener"); }
				
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

				// Execute fetches JSON; do not require an existing plan diagram payload.
				if (!canExecuteAgainstServer()) {
					return;
				}

				PlanGraphPanel graphPanel = findPlanGraphPanel();
				if (graphPanel == null) {
					return;
				}

				PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, true);
				boolean ok;
				try {
					ok = graphPanel.executeAndBuildDiagram(PlanSourcePanel.this);
				} finally {
					PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, false);
				}

				if (ok) {
					clearJsonPlanErrorStyle();
					FormPanel formPanel = findFormPanel();
					if (formPanel != null) {
						formPanel.updatePlanDiagramTabEnabled();
					}
					objCurrentPlanTabs.setEnabled(true);
					objCurrentPlanTabs.setEnabledAt(2, true);
					objCurrentPlanTabs.setSelectedIndex(2);
				} else if (!isJsonPlanShowingError()) {
					// Unexpected failure without a DB error string — keep user on Plan Source.
					showJsonPlanExecutionError("Could not build the plan diagram. Check connection settings and SQL.");
				}
			}
		});

		// set up mnemonics
		//okBtn.setMnemonic(KeyEvent.VK_O); // underlines "OK" but does not press "OK". Why?
		
		layoutComponents();
		refreshPlanFormatChrome();
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

	/** Left-aligned row spanning the form (EXPLAIN / JSON blocks). */
	private GridBagConstraints fullWidthGbc(int y, Insets insets) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 5;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = insets;
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
		sizeField(planFolderCombo, FIELD_WIDTH - 88);
		sizeField(planFolderSaveBtn, 80);
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

		panel.add(planFolderLabel, labelGbc(0, row));
		JPanel folderRow = new JPanel();
		folderRow.setLayout(new BoxLayout(folderRow, BoxLayout.X_AXIS));
		folderRow.setOpaque(false);
		folderRow.add(planFolderCombo);
		folderRow.add(Box.createHorizontalStrut(8));
		folderRow.add(planFolderSaveBtn);
		panel.add(folderRow, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryPlanNameLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryPlanNameField, fieldGbc(1, row++, 3));

		panel.add(sourceModeLabel, labelGbc(0, row));
		JPanel sourceModePanel = new JPanel();
		sourceModePanel.setLayout(new BoxLayout(sourceModePanel, BoxLayout.X_AXIS));
		sourceModePanel.setOpaque(false);
		sourceModePanel.add(sourceModePasteRadio);
		sourceModePanel.add(Box.createHorizontalStrut(12));
		sourceModePanel.add(sourceModeConnectRadio);
		panel.add(sourceModePanel, fieldGbc(1, row++, 3));

		sectionHeaderServer = createSectionHeader("Server");
		panel.add(sectionHeaderServer, sectionGbc(row++));

		panel.add(connectByLabel, labelGbc(0, row));
		connectByPanel = new JPanel();
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

		sectionHeaderAuthentication = createSectionHeader("Authentication");
		panel.add(sectionHeaderAuthentication, sectionGbc(row++));

		panel.add(authTypeLabel, labelGbc(0, row));
		panel.add(authTypeCombo, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryUserLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryUserField, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryPasswordLabel, labelGbc(0, row));
		passwordRow = new JPanel();
		passwordRow.setLayout(new BoxLayout(passwordRow, BoxLayout.X_AXIS));
		passwordRow.setOpaque(false);
		passwordRow.add(planSourceFromQueryPasswordField);
		passwordRow.add(Box.createHorizontalStrut(10));
		passwordRow.add(planSourceFromQueryRememberPasswordCheckbox);
		panel.add(passwordRow, fieldGbc(1, row++, 3));

		sectionHeaderCommands = createSectionHeader("Commands");
		panel.add(sectionHeaderCommands, sectionGbc(row++));

		panel.add(planSourceFromQueryPreCommandLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryPreCommandField, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryPostCommandLabel, labelGbc(0, row));
		panel.add(planSourceFromQueryPostCommandField, fieldGbc(1, row++, 3));

		// Full-width, left-aligned (not indented under the label/field columns).
		panel.add(planSourceFromQuerySQLLabel, fullWidthGbc(row++, new Insets(10, 8, 2, 8)));

		GridBagConstraints sqlAreaGbc = fullWidthGbc(row++, new Insets(0, 8, 4, 8));
		sqlAreaGbc.weighty = 0.45;
		sqlAreaGbc.fill = GridBagConstraints.BOTH;
		panel.add(planSourceFromQuerySQLTextAreaScrollPane, sqlAreaGbc);

		panel.add(planSourceFromQueryButton, fullWidthGbc(row++, new Insets(4, 8, 4, 8)));

		panel.add(jsonPlanLabel, fullWidthGbc(row++, new Insets(10, 8, 2, 8)));

		GridBagConstraints jsonAreaGbc = fullWidthGbc(row++, new Insets(0, 8, 4, 8));
		jsonAreaGbc.weighty = 0.45;
		jsonAreaGbc.fill = GridBagConstraints.BOTH;
		panel.add(jsonPlanTextAreaScrollPane, jsonAreaGbc);

		panel.add(showPlanGraphButton, fullWidthGbc(row++, new Insets(4, 8, 4, 8)));

		connectModeOnlyComponents = new Component[] {
				sectionHeaderServer,
				connectByLabel, connectByPanel,
				jdbcUrlLabel, jdbcUrlField,
				planSourceFromQueryServerLabel, planSourceFromQueryServerField,
				planSourceFromQueryPortLabel, planSourceFromQueryPortField,
				planSourceFromQueryDatabaseLabel, planSourceFromQueryDatabaseField,
				sectionHeaderAuthentication,
				authTypeLabel, authTypeCombo,
				planSourceFromQueryUserLabel, planSourceFromQueryUserField,
				planSourceFromQueryPasswordLabel, passwordRow,
				sectionHeaderCommands,
				planSourceFromQueryPreCommandLabel, planSourceFromQueryPreCommandField,
				planSourceFromQueryPostCommandLabel, planSourceFromQueryPostCommandField,
				planSourceFromQuerySQLLabel, planSourceFromQuerySQLTextAreaScrollPane,
				planSourceFromQueryButton
		};

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

	/** True when Connection and EXPLAIN fields are visible. */
	public boolean isConnectSourceMode() {
		return sourceModeConnectRadio != null && sourceModeConnectRadio.isSelected();
	}

	private void applySourceModeVisibility(boolean persist) {
		boolean connect = isConnectSourceMode();
		if (connectModeOnlyComponents != null) {
			for (Component c : connectModeOnlyComponents) {
				if (c != null) {
					c.setVisible(connect);
				}
			}
		}
		revalidate();
		repaint();
		if (persist) {
			saveSourceModePreference(connect ? SOURCE_MODE_CONNECT : SOURCE_MODE_PASTE);
		}
	}

	private void loadSourceModePreference() {
		String mode = SOURCE_MODE_PASTE;
		try {
			org.json.simple.JsonObject row = Database.row("select plan_source_mode from config;");
			if (row != null && row.get("plan_source_mode") != null) {
				String saved = String.valueOf(row.get("plan_source_mode")).trim();
				if (SOURCE_MODE_CONNECT.equalsIgnoreCase(saved)) {
					mode = SOURCE_MODE_CONNECT;
				}
			}
		} catch (Exception ignored) {
			// Column may not exist yet during early init
		}
		if (SOURCE_MODE_CONNECT.equals(mode)) {
			sourceModeConnectRadio.setSelected(true);
		} else {
			sourceModePasteRadio.setSelected(true);
		}
		applySourceModeVisibility(false);
	}

	private void saveSourceModePreference(String mode) {
		if (mode == null || mode.isBlank()) {
			mode = SOURCE_MODE_PASTE;
		}
		try {
			Database.dml("update config set plan_source_mode = '" + mode.replace("'", "''") + "';");
		} catch (Exception ignored) {
			// Ignore if column missing; next startup ensureConfigColumn will add it.
		}
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
		loadSourceModePreference();
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

	/**
	 * Rebuild the Folder dropdown from the Plans tree and select this plan's current folder.
	 * Save stays disabled until the user picks a different folder.
	 */
	public void refreshFolderChooser() {
		if (planFolderCombo == null) {
			return;
		}
		FormPanel formPanel = findFormPanel();
		Integer planId = formPanel == null ? null : formPanel.findPlanIdForSource(this);
		if (formPanel == null || planId == null) {
			syncingPlanFolderCombo = true;
			try {
				planFolderCombo.removeAllItems();
				planFolderCombo.addItem(new FormPanel.FolderChoice(null, "(root)"));
				planFolderCombo.setSelectedIndex(0);
				savedPlanFolderId = null;
			} finally {
				syncingPlanFolderCombo = false;
			}
			planFolderSaveBtn.setEnabled(false);
			return;
		}

		savedPlanFolderId = formPanel.getPlanFolderId(planId.intValue());
		java.util.List<FormPanel.FolderChoice> choices = formPanel.listFolderChoices();
		syncingPlanFolderCombo = true;
		try {
			planFolderCombo.removeAllItems();
			FormPanel.FolderChoice selected = null;
			for (FormPanel.FolderChoice choice : choices) {
				planFolderCombo.addItem(choice);
				if (Objects.equals(choice.getFolderId(), savedPlanFolderId)) {
					selected = choice;
				}
			}
			if (selected != null) {
				planFolderCombo.setSelectedItem(selected);
			} else if (planFolderCombo.getItemCount() > 0) {
				planFolderCombo.setSelectedIndex(0);
				savedPlanFolderId = null;
			}
		} finally {
			syncingPlanFolderCombo = false;
		}
		updatePlanFolderSaveEnabled();
	}

	private void updatePlanFolderSaveEnabled() {
		if (planFolderSaveBtn == null || syncingPlanFolderCombo) {
			return;
		}
		FormPanel.FolderChoice choice = (FormPanel.FolderChoice) planFolderCombo.getSelectedItem();
		Integer selectedId = choice == null ? null : choice.getFolderId();
		planFolderSaveBtn.setEnabled(!Objects.equals(selectedId, savedPlanFolderId));
	}

	private void savePlanFolderSelection() {
		FormPanel formPanel = findFormPanel();
		if (formPanel == null) {
			return;
		}
		Integer planId = formPanel.findPlanIdForSource(this);
		if (planId == null) {
			planId = formPanel.getSelectedPlanId();
		}
		if (planId == null) {
			return;
		}
		FormPanel.FolderChoice choice = (FormPanel.FolderChoice) planFolderCombo.getSelectedItem();
		Integer newFolderId = choice == null ? null : choice.getFolderId();
		if (Objects.equals(newFolderId, savedPlanFolderId)) {
			planFolderSaveBtn.setEnabled(false);
			return;
		}

		String finalName = formPanel.movePlanToFolder(planId.intValue(), newFolderId);
		if (finalName == null) {
			return;
		}
		savedPlanFolderId = newFolderId;
		planFolderSaveBtn.setEnabled(false);

		if (!finalName.equals(getPlanSourceFromQueryPlanNameField())) {
			setPlanSourceFromQueryPlanNameField(finalName);
			planSourceFromQueryPlanNameField.setText(finalName);
			if (objCurrentPlanTabs != null && objCurrentPlanTabs.getTabCount() > 0) {
				objCurrentPlanTabs.setTitleAt(0, finalName + " - Plan Source");
				if (objCurrentPlanTabs.getTabCount() > 2) {
					objCurrentPlanTabs.setTitleAt(2, finalName + " - Plan Diagram");
				}
			}
		}
		// Keep dropdown labels/selection in sync if folders were renamed elsewhere.
		refreshFolderChooser();
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
		jsonPlanTextArea.setText(planText == null ? "" : planText);
		refreshPlanFormatChrome();
		FormPanel formPanel = findFormPanel();
		if (formPanel != null) {
			formPanel.updatePlanDiagramTabEnabled();
		} else {
			setShowPlanGraphButton(hasUsableJsonPlan());
		}
	}

	/** True when the plan field has JSON or text content that can build a diagram. */
	public boolean hasUsableJsonPlan() {
		if (jsonPlanShowingError) {
			return false;
		}
		PlanFormat format = sniffPlanFormat(getJsonPlanText());
		return format == PlanFormat.JSON || format == PlanFormat.TEXT;
	}

	/** Debounce label updates so large plan pastes are not blocked on the EDT. */
	private void schedulePlanFormatChromeRefresh() {
		if (planFormatChromeTimer == null) {
			planFormatChromeTimer = new Timer(200, e -> refreshPlanFormatChrome());
			planFormatChromeTimer.setRepeats(false);
		}
		planFormatChromeTimer.restart();
	}

	/** Update field label and Show/Execute button captions from sniffed plan content. */
	public void refreshPlanFormatChrome() {
		PlanFormat format = sniffPlanFormat(getJsonPlanText());
		if (jsonPlanLabel != null) {
			if (format == PlanFormat.TEXT) {
				jsonPlanLabel.setText("Text plan:");
			} else if (format == PlanFormat.JSON) {
				jsonPlanLabel.setText("JSON plan:");
			} else {
				// EMPTY / UNKNOWN — content not yet identifiable
				jsonPlanLabel.setText("Text plan or JSON plan:");
			}
		}
		if (showPlanGraphButton != null) {
			if (format == PlanFormat.TEXT) {
				showPlanGraphButton.setText("Show Text Plan Diagram");
			} else if (format == PlanFormat.JSON) {
				showPlanGraphButton.setText("Show JSON Plan Diagram");
			} else {
				showPlanGraphButton.setText("Show Plan Diagram");
			}
		}
	}

	public static PlanFormat sniffPlanFormat(String planText) {
		return ExplainTextPlanConverter.sniff(planText);
	}

	/**
	 * Resolve the plan field to a JSON array string for diagram construction.
	 * JSON content is normalized; text EXPLAIN is converted on the fly.
	 * Raw field contents are never replaced — callers use the return value only.
	 */
	public static String resolvePlanFieldToJson(String planText) throws ExplainTextPlanConvertException {
		String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(planText);
		if (resolved == null || resolved.isBlank()) {
			throw new ExplainTextPlanConvertException(
					"Plan field is empty or not recognized as JSON or text EXPLAIN.");
		}
		return resolved;
	}

	/** Show an execute/connection error in the JSON plan field and pulse the border once. */
	public void showJsonPlanExecutionError(String errorText) {
		// setText may fire the document listener (clears prior error style) — apply style after.
		jsonPlanTextArea.setText(errorText == null ? "" : errorText);
		jsonPlanShowingError = true;
		setShowPlanGraphButton(false);
		setPlanSourceButtonClicked(null);
		FormPanel formPanel = findFormPanel();
		if (formPanel != null) {
			formPanel.updatePlanDiagramTabEnabled();
		}
		pulseJsonPlanErrorBorderOnce();
		SwingUtilities.invokeLater(() -> {
			jsonPlanTextArea.requestFocusInWindow();
			jsonPlanTextArea.setCaretPosition(0);
		});
	}

	public boolean isJsonPlanShowingError() {
		return jsonPlanShowingError;
	}

	public void clearJsonPlanErrorStyle() {
		if (!jsonPlanShowingError && (jsonErrorPulseTimer == null || !jsonErrorPulseTimer.isRunning())) {
			return;
		}
		jsonPlanShowingError = false;
		if (jsonErrorPulseTimer != null) {
			jsonErrorPulseTimer.stop();
			jsonErrorPulseTimer = null;
		}
		if (jsonPlanTextAreaScrollPane != null) {
			jsonPlanTextAreaScrollPane.setBorder(jsonPlanDefaultBorder);
		}
	}

	/** One-shot border pulse (peak then settle to a steady red error border). */
	private void pulseJsonPlanErrorBorderOnce() {
		if (jsonPlanTextAreaScrollPane == null) {
			return;
		}
		if (jsonErrorPulseTimer != null) {
			jsonErrorPulseTimer.stop();
		}
		final int steps = 10;
		final int[] step = { 0 };
		jsonErrorPulseTimer = new Timer(35, e -> {
			step[0]++;
			float t = step[0] / (float) steps;
			float intensity = t <= 0.35f ? (t / 0.35f) : Math.max(0f, 1f - ((t - 0.35f) / 0.65f));
			int thickness = 2 + Math.round(3f * intensity);
			Color color = blendColors(JSON_ERROR_BORDER, JSON_ERROR_BORDER_PEAK, intensity);
			jsonPlanTextAreaScrollPane.setBorder(BorderFactory.createLineBorder(color, thickness));
			if (step[0] >= steps) {
				((Timer) e.getSource()).stop();
				jsonErrorPulseTimer = null;
				jsonPlanTextAreaScrollPane.setBorder(BorderFactory.createLineBorder(JSON_ERROR_BORDER, 2));
			}
		});
		jsonErrorPulseTimer.setInitialDelay(0);
		jsonErrorPulseTimer.start();
	}

	private static Color blendColors(Color a, Color b, float bWeight) {
		float w = Math.max(0f, Math.min(1f, bWeight));
		int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * w);
		int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * w);
		int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * w);
		return new Color(r, g, bl);
	}

	private PlanGraphPanel findPlanGraphPanel() {
		if (objCurrentPlanTabs == null || objCurrentPlanTabs.getTabCount() <= 2) {
			return null;
		}
		return findPlanGraphPanelInHierarchy(objCurrentPlanTabs.getComponentAt(2));
	}

	private static PlanGraphPanel findPlanGraphPanelInHierarchy(Component root) {
		if (root instanceof PlanGraphPanel) {
			return (PlanGraphPanel) root;
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				PlanGraphPanel found = findPlanGraphPanelInHierarchy(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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

	/** Single-plan + diagram tab present — enough to run EXPLAIN against the server. */
	private boolean canExecuteAgainstServer() {
		if (objCurrentPlanTabs == null || objCurrentPlanTabs.getTabCount() <= 2) {
			return false;
		}
		FormPanel formPanel = findFormPanel();
		return formPanel != null && formPanel.isSinglePlanSelected();
	}

	/** Show JSON Plan Diagram needs usable JSON already in the field. */
	private boolean canOpenPlanDiagramTab() {
		return canExecuteAgainstServer() && hasUsableJsonPlan();
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

