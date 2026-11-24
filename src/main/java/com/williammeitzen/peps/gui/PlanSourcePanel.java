package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.util.function.BiConsumer;

import com.formdev.flatlaf.FlatClientProperties;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JLayeredPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.williammeitzen.peps.model.Database;
import com.williammeitzen.peps.model.ExplainTextPlanConverter;
import com.williammeitzen.peps.model.ExplainTextPlanConverter.ExplainTextPlanConvertException;
import com.williammeitzen.peps.model.PlanAnonymizer;
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
	private JPanel showPlanGraphButtonRow;
	private PulsingArrowCue showDiagramCue;
	/** Set by Get Started samples; applied when this panel is (re)shown in Documents. */
	private boolean diagramCuePending;
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

	private JCheckBox anonymizeCheckbox;
	private JCheckBox anonymizeAliasesCheckbox;
	private JLabel anonymizeStatusLabel;
	private JPanel anonymizeRow;
	private JLabel preCommandAnonymizeBadge;
	private JLabel postCommandAnonymizeBadge;
	private JLabel sqlAnonymizeBadge;
	private JLabel planAnonymizeBadge;
	private JComponent preCommandFieldHost;
	private JComponent postCommandFieldHost;
	private JComponent sqlFieldHost;
	private JComponent planFieldHost;
	/** True while swapping field text for anonymize view (skip dirty/original capture). */
	private boolean syncingAnonymizeView;
	/** Originals always persisted; UI may show scrubbed JIT text when anonymize is on. */
	private String originalPreCommand = "";
	private String originalPostCommand = "";
	private String originalSqlCommand = "";
	private String originalPlanText = "";
	private PlanAnonymizer.Result cachedAnonymizeResult;
	private Runnable anonymizeModeListener;

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
	/** SQL half of the SQL/plan stack; collapsed entirely in Plan text only mode. */
	private JPanel sqlBlock;
	private JSplitPane sqlJsonSplit;
	/** Outer width split (content | spacer) driven by SE corner grips. */
	private JSplitPane widthSplit;
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

		anonymizeCheckbox = new JCheckBox("Anonymize");
		anonymizeAliasesCheckbox = new JCheckBox("Anonymize aliases");
		anonymizeAliasesCheckbox.setSelected(true);
		anonymizeAliasesCheckbox.setEnabled(false);
		anonymizeStatusLabel = new JLabel("");
		anonymizeStatusLabel.setFont(anonymizeStatusLabel.getFont().deriveFont(Font.ITALIC));
		preCommandAnonymizeBadge = createAnonymizeBadge();
		postCommandAnonymizeBadge = createAnonymizeBadge();
		sqlAnonymizeBadge = createAnonymizeBadge();
		planAnonymizeBadge = createAnonymizeBadge();
		anonymizeCheckbox.addItemListener(e -> {
			if (syncingAnonymizeView) {
				return;
			}
			anonymizeAliasesCheckbox.setEnabled(anonymizeCheckbox.isSelected());
			applyAnonymizeView(true);
			persistAnonymizeFlags();
			notifyAnonymizeModeListener();
		});
		anonymizeAliasesCheckbox.addItemListener(e -> {
			if (syncingAnonymizeView || !anonymizeCheckbox.isSelected()) {
				return;
			}
			cachedAnonymizeResult = null;
			applyAnonymizeView(true);
			persistAnonymizeFlags();
			notifyAnonymizeModeListener();
		});

		planSourceFromQueryPreCommandLabel = new JLabel("Before EXPLAIN:");
		planSourceFromQueryPreCommandField = new JTextField(36);
		planSourceFromQueryPostCommandLabel = new JLabel("After EXPLAIN:");
		planSourceFromQueryPostCommandField = new JTextField(36);
		
		planSourceFromQuerySQLLabel = new JLabel("EXPLAIN (with options) and SQL command:");
		planSourceFromQuerySQLTextArea = new JTextArea(8, 40);
		planSourceFromQuerySQLTextArea.setLineWrap(true);
		planSourceFromQuerySQLTextArea.setWrapStyleWord(true);
		planSourceFromQuerySQLTextArea.setText("\nexplain (analyze, buffers, format json, verbose)\n\nselect \n");
		originalSqlCommand = planSourceFromQuerySQLTextArea.getText();
		Utils.installCrossPlatformEditBindings(planSourceFromQuerySQLTextArea);
		planSourceFromQuerySQLTextAreaScrollPane = new JScrollPane(planSourceFromQuerySQLTextArea);
		planSourceFromQuerySQLTextAreaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		jsonPlanLabel = new JLabel("Text plan or JSON plan:");
		jsonPlanTextArea = new JTextArea(8, 40);
		jsonPlanTextArea.setLineWrap(true);
		jsonPlanTextArea.setWrapStyleWord(true);
		Utils.installCrossPlatformEditBindings(jsonPlanTextArea);
		jsonPlanTextAreaScrollPane = new JScrollPane(jsonPlanTextArea);
		jsonPlanTextAreaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
				onOriginalPlanFieldEdited();
			}

			public void removeUpdate(DocumentEvent e) {
				onOriginalPlanFieldEdited();
			}

			public void changedUpdate(DocumentEvent e) {
				onOriginalPlanFieldEdited();
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
				objCurrentPlanTabs.setTitleAt(0, strNewPlanName + " - Plan Source"
						+ (isAnonymizeEnabled() ? " (anonymized)" : ""));
				FormPanel renameForm = findFormPanel();
				int diagramIdx = renameForm == null ? -1 : renameForm.getPlanDiagramTabIndex();
				if (diagramIdx >= 0) {
					objCurrentPlanTabs.setTitleAt(diagramIdx, strNewPlanName + " - Plan Diagram"
							+ (isAnonymizeEnabled() ? " (anonymized)" : ""));
				}
			}
		});

		//jsonPlanTextArea.addComponentListener(jsonPlanTextAreaListener);
		jsonPlanTextArea.getDocument().addDocumentListener(jsonPlanTextAreaDocumentListener);

		showPlanGraphButton = new JButton("Show JSON Plan Diagram");
		showPlanGraphButton.setEnabled(false);
		showDiagramCue = new PulsingArrowCue();
		showPlanGraphButtonRow = new JPanel(new BorderLayout(12, 0));
		showPlanGraphButtonRow.setOpaque(false);
		JPanel showBtnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		showBtnWrap.setOpaque(false);
		showBtnWrap.add(showPlanGraphButton);
		showPlanGraphButtonRow.add(showBtnWrap, BorderLayout.WEST);
		showPlanGraphButtonRow.add(showDiagramCue, BorderLayout.CENTER);
		showPlanGraphButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearShowDiagramCue();
				if (bolShowTiming == true) { System.out.println("before PlanSourcePanel.showPlanGraphButton.addActionListener(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
				if (!canOpenPlanDiagramTab()) {
					return;
				}
				// Wait cursor must be cleared here: paintComponent only clears when a
				// rebuild runs, so a cached diagram would otherwise leave WAIT stuck.
				PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, true);
				try {
					setPlanSourceButtonClicked(SHOW_JSON_PLAN_DIAGRAM);
					selectPlanDiagramTab();
				} finally {
					PlanGraphPanel.setDiagramBusyCursor(PlanSourcePanel.this, false);
				}
				if (bolShowTiming == true) { System.out.println("after PlanSourcePanel.showPlanGraphButton.addActionListener(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			}
		});

		DocumentListener planSourceFromQueryDocumentListener = new DocumentListener() {

			public void insertUpdate(DocumentEvent e) {
				onOriginalCommandFieldsEdited();
			}

			public void removeUpdate(DocumentEvent e) {
				onOriginalCommandFieldsEdited();
			}

			public void changedUpdate(DocumentEvent e) {
				onOriginalCommandFieldsEdited();
			}

		};
		planSourceFromQueryPreCommandField.getDocument().addDocumentListener(planSourceFromQueryDocumentListener);
		planSourceFromQueryPostCommandField.getDocument().addDocumentListener(planSourceFromQueryDocumentListener);
		planSourceFromQuerySQLTextArea.getDocument().addDocumentListener(planSourceFromQueryDocumentListener);

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
					selectPlanDiagramTab();
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
		// Grow with the split / window in both directions (divider + parent resize).
		scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
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

		WidthTrackingForm panel = new WidthTrackingForm();
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

		anonymizeRow = new JPanel(new BorderLayout(8, 0));
		anonymizeRow.setOpaque(false);
		JPanel anonymizeChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		anonymizeChecks.setOpaque(false);
		anonymizeChecks.add(anonymizeCheckbox);
		anonymizeChecks.add(anonymizeAliasesCheckbox);
		anonymizeChecks.add(anonymizeStatusLabel);
		anonymizeRow.add(anonymizeChecks, BorderLayout.WEST);
		panel.add(anonymizeRow, fullWidthGbc(row++, new Insets(8, 8, 4, 8)));

		sectionHeaderCommands = createSectionHeader("Commands");
		panel.add(sectionHeaderCommands, sectionGbc(row++));

		preCommandFieldHost = wrapWithAnonymizeBadge(planSourceFromQueryPreCommandField, preCommandAnonymizeBadge);
		postCommandFieldHost = wrapWithAnonymizeBadge(planSourceFromQueryPostCommandField, postCommandAnonymizeBadge);
		panel.add(planSourceFromQueryPreCommandLabel, labelGbc(0, row));
		panel.add(preCommandFieldHost, fieldGbc(1, row++, 3));

		panel.add(planSourceFromQueryPostCommandLabel, labelGbc(0, row));
		panel.add(postCommandFieldHost, fieldGbc(1, row++, 3));

		sqlFieldHost = wrapWithAnonymizeBadge(planSourceFromQuerySQLTextAreaScrollPane, sqlAnonymizeBadge);
		planFieldHost = wrapWithAnonymizeBadge(jsonPlanTextAreaScrollPane, planAnonymizeBadge);

		sqlBlock = new JPanel(new BorderLayout(0, 4));
		sqlBlock.setOpaque(false);
		sqlBlock.add(planSourceFromQuerySQLLabel, BorderLayout.NORTH);
		sqlBlock.add(sqlFieldHost, BorderLayout.CENTER);
		JPanel execRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		execRow.setOpaque(false);
		execRow.add(planSourceFromQueryButton);
		sqlBlock.add(execRow, BorderLayout.SOUTH);
		sqlBlock.setMinimumSize(new Dimension(120, 100));

		JPanel jsonBlock = new JPanel(new BorderLayout(0, 4));
		jsonBlock.setOpaque(false);
		jsonBlock.add(jsonPlanLabel, BorderLayout.NORTH);
		jsonBlock.add(planFieldHost, BorderLayout.CENTER);
		jsonBlock.add(showPlanGraphButtonRow, BorderLayout.SOUTH);
		jsonBlock.setMinimumSize(new Dimension(120, 100));

		sqlJsonSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlBlock, jsonBlock);
		sqlJsonSplit.setResizeWeight(0.5);
		sqlJsonSplit.setContinuousLayout(true);
		sqlJsonSplit.setBorder(null);
		sqlJsonSplit.setDividerSize(6);
		usePlainSplitDivider(sqlJsonSplit);

		// Horizontal divider on the right edge so width can be dragged like height.
		widthSplit = wrapWithHorizontalWidthSplit(sqlJsonSplit, AREA_WIDTH);
		JSplitPane sizedStack = widthSplit;
		wireSeResizeGrips();

		GridBagConstraints stackGbc = fullWidthGbc(row++, new Insets(8, 8, 4, 8));
		stackGbc.weightx = 1.0;
		stackGbc.weighty = 1.0;
		stackGbc.fill = GridBagConstraints.BOTH;
		panel.add(sizedStack, stackGbc);

		// Hide the whole SQL block in Plan text only mode (not just its children),
		// otherwise the split leaves a large empty top pane and crushes the plan field.
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
				planSourceFromQueryPreCommandLabel, preCommandFieldHost,
				planSourceFromQueryPostCommandLabel, postCommandFieldHost,
				sqlBlock
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
		collapseSqlHalfOfPlanStack(!connect);
		revalidate();
		repaint();
		if (persist) {
			saveSourceModePreference(connect ? SOURCE_MODE_CONNECT : SOURCE_MODE_PASTE);
		}
	}

	/**
	 * In Plan text only mode, give the entire vertical stack to the plan field.
	 * Hiding SQL children alone left an empty top split pane (what looked like a
	 * missing explain/plan editor on sample plans).
	 */
	private void collapseSqlHalfOfPlanStack(boolean collapse) {
		if (sqlJsonSplit == null || sqlBlock == null) {
			return;
		}
		if (collapse) {
			sqlBlock.setMinimumSize(new Dimension(0, 0));
			sqlBlock.setPreferredSize(new Dimension(0, 0));
			sqlJsonSplit.setDividerSize(0);
			sqlJsonSplit.setResizeWeight(0.0);
			sqlJsonSplit.setDividerLocation(0);
			SwingUtilities.invokeLater(() -> {
				if (sqlJsonSplit != null && !isConnectSourceMode()) {
					sqlJsonSplit.setDividerLocation(0);
					sqlJsonSplit.revalidate();
				}
			});
		} else {
			sqlBlock.setMinimumSize(new Dimension(120, 100));
			sqlBlock.setPreferredSize(null);
			sqlJsonSplit.setDividerSize(8);
			sqlJsonSplit.setResizeWeight(0.5);
			SwingUtilities.invokeLater(() -> {
				if (sqlJsonSplit != null && isConnectSourceMode()) {
					int h = Math.max(sqlJsonSplit.getHeight(), 200);
					sqlJsonSplit.setDividerLocation(h / 2);
					sqlJsonSplit.revalidate();
				}
			});
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
		if (isAnonymizeEnabled()) {
			return originalPlanText == null ? "" : originalPlanText;
		}
		return jsonPlanTextArea.getText();
	}

	/** Plan text used to build the diagram (scrubbed when Anonymize is on). */
	public String getJsonPlanTextForDiagram() {
		if (!isAnonymizeEnabled()) {
			return getJsonPlanText();
		}
		ensureAnonymizeResult();
		return cachedAnonymizeResult == null ? getJsonPlanText() : cachedAnonymizeResult.plan;
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
				objCurrentPlanTabs.setTitleAt(0, finalName + " - Plan Source"
						+ (isAnonymizeEnabled() ? " (anonymized)" : ""));
				int diagramIdx = formPanel.getPlanDiagramTabIndex();
				if (diagramIdx >= 0) {
					objCurrentPlanTabs.setTitleAt(diagramIdx, finalName + " - Plan Diagram"
							+ (isAnonymizeEnabled() ? " (anonymized)" : ""));
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
		originalPreCommand = strPreCommand == null ? "" : strPreCommand;
		cachedAnonymizeResult = null;
		if (isAnonymizeEnabled()) {
			applyAnonymizeView(false);
		} else {
			syncingAnonymizeView = true;
			try {
				planSourceFromQueryPreCommandField.setText(originalPreCommand);
			} finally {
				syncingAnonymizeView = false;
			}
		}
	}

	public String getPlanSourceFromQueryPreCommandField() {
		if (isAnonymizeEnabled()) {
			return originalPreCommand == null ? "" : originalPreCommand;
		}
		return planSourceFromQueryPreCommandField.getText();
	}

	public String getPlanSourceFromQueryPostCommandField() {
		if (isAnonymizeEnabled()) {
			return originalPostCommand == null ? "" : originalPostCommand;
		}
		return planSourceFromQueryPostCommandField.getText();
	}
	
	public void setJsonPlanText(String planText) {
		originalPlanText = planText == null ? "" : planText;
		cachedAnonymizeResult = null;
		if (isAnonymizeEnabled()) {
			applyAnonymizeView(false);
		} else {
			syncingAnonymizeView = true;
			try {
				jsonPlanTextArea.setText(originalPlanText);
			} finally {
				syncingAnonymizeView = false;
			}
		}
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

	/**
	 * Remember to show the diagram cue once this Plan Source panel is on screen
	 * (tabs are rebuilt after tree selection, which can drop an early cue).
	 */
	public void requestShowDiagramCue() {
		diagramCuePending = true;
		if (isDisplayable()) {
			showShowDiagramCue();
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (diagramCuePending) {
			SwingUtilities.invokeLater(this::showShowDiagramCue);
		}
	}

	/**
	 * Pulse a “Next” arrow beside Show Plan Diagram (used after Get Started sample plans).
	 */
	public void showShowDiagramCue() {
		if (showDiagramCue == null || showPlanGraphButton == null) {
			return;
		}
		diagramCuePending = true;
		setShowPlanGraphButton(hasUsableJsonPlan());
		Runnable apply = () -> {
			if (!diagramCuePending || showDiagramCue == null) {
				return;
			}
			showDiagramCue.startPulse();
			if (showPlanGraphButtonRow != null) {
				showPlanGraphButtonRow.revalidate();
				showPlanGraphButtonRow.repaint();
				showPlanGraphButtonRow.scrollRectToVisible(showPlanGraphButtonRow.getBounds());
			}
			revalidate();
			repaint();
			showPlanGraphButton.requestFocusInWindow();
		};
		SwingUtilities.invokeLater(apply);
		// Second pass after Documents tab layout settles.
		Timer showCueTimer = new Timer(300, e -> {
			((Timer) e.getSource()).stop();
			apply.run();
		});
		showCueTimer.setRepeats(false);
		showCueTimer.start();
	}

	public void clearShowDiagramCue() {
		diagramCuePending = false;
		if (showDiagramCue != null) {
			showDiagramCue.stopPulse();
		}
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
		FormPanel formPanel = findFormPanel();
		int diagramIdx = formPanel == null ? -1 : formPanel.getPlanDiagramTabIndex();
		if (objCurrentPlanTabs == null || diagramIdx < 0) {
			return null;
		}
		return findPlanGraphPanelInHierarchy(objCurrentPlanTabs.getComponentAt(diagramIdx));
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
		originalSqlCommand = strQueryText == null ? "" : strQueryText;
		cachedAnonymizeResult = null;
		if (isAnonymizeEnabled()) {
			applyAnonymizeView(false);
		} else {
			syncingAnonymizeView = true;
			try {
				planSourceFromQuerySQLTextArea.setText(originalSqlCommand);
			} finally {
				syncingAnonymizeView = false;
			}
		}
	}

	public String getSQLQueryText() {
		if (isAnonymizeEnabled()) {
			return originalSqlCommand == null ? "" : originalSqlCommand;
		}
		return planSourceFromQuerySQLTextArea.getText();
	}

	public void setPlanSourceFromQueryPostCommandField(String postCommand) {
		originalPostCommand = postCommand == null ? "" : postCommand;
		cachedAnonymizeResult = null;
		if (isAnonymizeEnabled()) {
			applyAnonymizeView(false);
		} else {
			syncingAnonymizeView = true;
			try {
				planSourceFromQueryPostCommandField.setText(originalPostCommand);
			} finally {
				syncingAnonymizeView = false;
			}
		}
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
		FormPanel formPanel = findFormPanel();
		if (formPanel != null) {
			formPanel.updatePlanDiagramTabEnabled();
		}
		this.planTextIsDirty = planTextIsDirty;
	}

	/** Single-plan + diagram tab present — enough to run EXPLAIN against the server. */
	private boolean canExecuteAgainstServer() {
		FormPanel formPanel = findFormPanel();
		return formPanel != null
				&& formPanel.isSinglePlanSelected()
				&& formPanel.getPlanDiagramTabIndex() >= 0;
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

	public boolean isAnonymizeEnabled() {
		return anonymizeCheckbox != null && anonymizeCheckbox.isSelected();
	}

	public boolean isAnonymizeAliasesEnabled() {
		return anonymizeAliasesCheckbox == null || anonymizeAliasesCheckbox.isSelected();
	}

	public void setAnonymizeFlags(boolean anonymize, boolean anonymizeAliases) {
		syncingAnonymizeView = true;
		try {
			anonymizeCheckbox.setSelected(anonymize);
			anonymizeAliasesCheckbox.setSelected(anonymizeAliases);
			anonymizeAliasesCheckbox.setEnabled(anonymize);
		} finally {
			syncingAnonymizeView = false;
		}
		applyAnonymizeView(false);
	}

	public void setAnonymizeModeListener(Runnable listener) {
		this.anonymizeModeListener = listener;
	}

	public Map<String, String> getAnonymizeReverseMap() {
		ensureAnonymizeResult();
		if (cachedAnonymizeResult == null) {
			return Collections.emptyMap();
		}
		return cachedAnonymizeResult.reverseMap;
	}

	private void notifyAnonymizeModeListener() {
		if (anonymizeModeListener != null) {
			anonymizeModeListener.run();
		}
	}

	private void onOriginalPlanFieldEdited() {
		if (syncingAnonymizeView || isAnonymizeEnabled()) {
			return;
		}
		originalPlanText = jsonPlanTextArea.getText();
		cachedAnonymizeResult = null;
		clearJsonPlanErrorStyle();
		setPlanTextIsDirty(true);
		schedulePlanFormatChromeRefresh();
	}

	private void onOriginalCommandFieldsEdited() {
		if (syncingAnonymizeView || isAnonymizeEnabled()) {
			return;
		}
		originalPreCommand = planSourceFromQueryPreCommandField.getText();
		originalPostCommand = planSourceFromQueryPostCommandField.getText();
		originalSqlCommand = planSourceFromQuerySQLTextArea.getText();
		cachedAnonymizeResult = null;
		setPlanTextIsDirty(true);
	}

	private void captureOriginalsFromFieldsIfEditable() {
		// When already showing scrubbed text, fields are non-editable — keep stored originals.
		if (!planSourceFromQuerySQLTextArea.isEditable() || !jsonPlanTextArea.isEditable()) {
			return;
		}
		originalPreCommand = planSourceFromQueryPreCommandField.getText();
		originalPostCommand = planSourceFromQueryPostCommandField.getText();
		originalSqlCommand = planSourceFromQuerySQLTextArea.getText();
		originalPlanText = jsonPlanTextArea.getText();
	}

	private void ensureAnonymizeResult() {
		captureOriginalsFromFieldsIfEditable();
		if (cachedAnonymizeResult != null) {
			return;
		}
		cachedAnonymizeResult = PlanAnonymizer.anonymize(
				originalPreCommand,
				originalPostCommand,
				originalSqlCommand,
				originalPlanText,
				isAnonymizeAliasesEnabled());
	}

	private void applyAnonymizeView(boolean rebuildDiagramHint) {
		boolean anonymize = isAnonymizeEnabled();
		anonymizeAliasesCheckbox.setEnabled(anonymize);
		setAnonymizeBadgesVisible(anonymize);
		if (anonymize) {
			anonymizeStatusLabel.setText("Showing anonymized text");
			// Capture any pending edits, then scrub immediately into the visible fields.
			captureOriginalsFromFieldsIfEditable();
			cachedAnonymizeResult = null;
			ensureAnonymizeResult();
			syncingAnonymizeView = true;
			try {
				planSourceFromQueryPreCommandField.setText(cachedAnonymizeResult.preCommand);
				planSourceFromQueryPostCommandField.setText(cachedAnonymizeResult.postCommand);
				planSourceFromQuerySQLTextArea.setText(cachedAnonymizeResult.sql);
				jsonPlanTextArea.setText(cachedAnonymizeResult.plan);
				setCommandFieldsEditable(false);
			} finally {
				syncingAnonymizeView = false;
			}
		} else {
			anonymizeStatusLabel.setText("");
			syncingAnonymizeView = true;
			try {
				planSourceFromQueryPreCommandField.setText(originalPreCommand);
				planSourceFromQueryPostCommandField.setText(originalPostCommand);
				planSourceFromQuerySQLTextArea.setText(originalSqlCommand);
				jsonPlanTextArea.setText(originalPlanText);
				setCommandFieldsEditable(true);
			} finally {
				syncingAnonymizeView = false;
			}
		}
		refreshPlanFormatChrome();
		updatePlanSourceTabTitleAnonymizedSuffix();
		if (rebuildDiagramHint) {
			jsonRenderedInDiagram = null;
			setPlanTextIsDirty(true);
			FormPanel formPanel = findFormPanel();
			if (formPanel != null) {
				formPanel.updatePlanDiagramTabEnabled();
			}
		}
	}

	private void setAnonymizeBadgesVisible(boolean visible) {
		setAnonymizeBadgeVisible(preCommandAnonymizeBadge, preCommandFieldHost, visible);
		setAnonymizeBadgeVisible(postCommandAnonymizeBadge, postCommandFieldHost, visible);
		setAnonymizeBadgeVisible(sqlAnonymizeBadge, sqlFieldHost, visible);
		setAnonymizeBadgeVisible(planAnonymizeBadge, planFieldHost, visible);
	}

	/**
	 * Toggle a field silhouette and re-layout its JLayeredPane overlay. Invisible
	 * badges report 0 preferred size, so without a re-layout they stay 0×0 after
	 * Anonymize is turned on.
	 */
	private static void setAnonymizeBadgeVisible(JLabel badge, JComponent host, boolean visible) {
		if (badge != null) {
			badge.setVisible(visible);
		}
		relayoutAnonymizeBadgeHost(host);
	}

	static void relayoutAnonymizeBadgeHost(JComponent host) {
		if (!(host instanceof JLayeredPane)) {
			if (host != null) {
				host.revalidate();
				host.repaint();
			}
			return;
		}
		JLayeredPane layered = (JLayeredPane) host;
		Object contentObj = layered.getClientProperty("anonymizeContent");
		Object cornerObj = layered.getClientProperty("anonymizeBadgeCorner");
		if (contentObj instanceof JComponent) {
			JComponent badge = cornerObj instanceof JComponent ? (JComponent) cornerObj : null;
			layoutFieldOverlays(layered, (JComponent) contentObj, badge);
		}
		layered.revalidate();
		layered.repaint();
	}

	private void setCommandFieldsEditable(boolean editable) {
		planSourceFromQueryPreCommandField.setEditable(editable);
		planSourceFromQueryPostCommandField.setEditable(editable);
		planSourceFromQuerySQLTextArea.setEditable(editable);
		jsonPlanTextArea.setEditable(editable);
	}

	private void persistAnonymizeFlags() {
		FormPanel formPanel = findFormPanel();
		Integer planId = formPanel == null ? null : formPanel.findPlanIdForSource(this);
		if (planId == null && formPanel != null) {
			planId = formPanel.getSelectedPlanId();
		}
		if (planId == null) {
			return;
		}
		Database.updateExplainPlanAnonymizeFlags(planId.intValue(), isAnonymizeEnabled(),
				isAnonymizeAliasesEnabled());
	}

	private void updatePlanSourceTabTitleAnonymizedSuffix() {
		FormPanel formPanel = findFormPanel();
		if (formPanel == null || objCurrentPlanTabs == null || objCurrentPlanTabs.getTabCount() == 0) {
			return;
		}
		String name = getPlanSourceFromQueryPlanNameField();
		if (name == null || name.isBlank()) {
			name = "Plan";
		}
		objCurrentPlanTabs.setTitleAt(0,
				name + " - Plan Source" + (isAnonymizeEnabled() ? " (anonymized)" : ""));
		int diagramIdx = formPanel.getPlanDiagramTabIndex();
		if (diagramIdx >= 0) {
			objCurrentPlanTabs.setTitleAt(diagramIdx,
					name + " - Plan Diagram" + (isAnonymizeEnabled() ? " (anonymized)" : ""));
		}
	}

	private void selectPlanDiagramTab() {
		if (objCurrentPlanTabs == null) {
			return;
		}
		FormPanel formPanel = findFormPanel();
		int diagramIdx = formPanel == null ? -1 : formPanel.getPlanDiagramTabIndex();
		if (diagramIdx < 0) {
			return;
		}
		objCurrentPlanTabs.setEnabled(true);
		objCurrentPlanTabs.setEnabledAt(diagramIdx, true);
		objCurrentPlanTabs.setSelectedIndex(diagramIdx);
	}

	/**
	 * Puts {@code content} on the left of a horizontal split with an empty right
	 * pane, so the user can drag left/right to change width (mirrors vertical
	 * split resizing for SQL/plan height).
	 */
	static JSplitPane wrapWithHorizontalWidthSplit(JComponent content, int initialWidth) {
		JPanel widthSpacer = new JPanel();
		widthSpacer.setOpaque(false);
		widthSpacer.setMinimumSize(new Dimension(24, 40));
		widthSpacer.setPreferredSize(new Dimension(48, 40));

		content.setMinimumSize(new Dimension(160, 80));

		JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, content, widthSpacer);
		horizontal.setResizeWeight(1.0);
		horizontal.setContinuousLayout(true);
		horizontal.setBorder(null);
		horizontal.setDividerSize(6);
		horizontal.setDividerLocation(Math.max(160, initialWidth));
		usePlainSplitDivider(horizontal);
		return horizontal;
	}

	/** Hide FlatLaf's three-dot divider grip; SE corner grips replace it visually. */
	static void usePlainSplitDivider(JSplitPane split) {
		if (split == null) {
			return;
		}
		split.putClientProperty(FlatClientProperties.STYLE, "style: plain");
	}

	private void wireSeResizeGrips() {
		wireSeResizeGrip(sqlFieldHost, (dx, dy) -> {
			nudgeWidthSplit(dx.intValue());
			nudgeSqlPlanDivider(dy.intValue());
		});
		wireSeResizeGrip(planFieldHost, (dx, dy) -> {
			nudgeWidthSplit(dx.intValue());
			if (isConnectSourceMode()) {
				// Dragging the plan field's SE corner down gives the plan more height.
				nudgeSqlPlanDivider(-dy.intValue());
			} else {
				nudgePlanPreferredHeight(dy.intValue());
			}
		});
	}

	static void wireSeResizeGrip(JComponent host, BiConsumer<Integer, Integer> onDrag) {
		if (!(host instanceof JLayeredPane)) {
			return;
		}
		Object gripObj = ((JLayeredPane) host).getClientProperty("seResizeGrip");
		if (gripObj instanceof SeResizeGrip) {
			((SeResizeGrip) gripObj).setOnDrag(onDrag);
		}
	}

	/** Nudge a split divider by {@code delta} px, keeping both sides at least {@code minSide}. */
	static void nudgeSplitDivider(JSplitPane split, int delta, int minSide) {
		if (split == null || delta == 0) {
			return;
		}
		int next = Math.max(minSide, split.getDividerLocation() + delta);
		int span = split.getOrientation() == JSplitPane.VERTICAL_SPLIT
				? split.getHeight()
				: split.getWidth();
		int max = Math.max(minSide, span - split.getDividerSize() - minSide);
		split.setDividerLocation(Math.min(next, max));
	}

	/**
	 * Overlay only the SE resize grip (no anonymize badge) — for large text areas
	 * that still sit in a height/width split stack.
	 */
	static JComponent wrapWithSeResizeGrip(JComponent content) {
		JLayeredPane layered = newTrackingLayeredPane(content);
		SeResizeGrip seGrip = new SeResizeGrip();
		layered.putClientProperty("seResizeGrip", seGrip);
		layered.add(content, JLayeredPane.DEFAULT_LAYER);
		layered.add(seGrip, JLayeredPane.DRAG_LAYER);
		layered.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutFieldOverlays(layered, content, null);
			}

			@Override
			public void componentShown(ComponentEvent e) {
				layoutFieldOverlays(layered, content, null);
			}
		});
		layoutFieldOverlays(layered, content, null);
		return layered;
	}

	private static JLayeredPane newTrackingLayeredPane(JComponent content) {
		JLayeredPane layered = new JLayeredPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return content.getPreferredSize();
			}

			@Override
			public Dimension getMinimumSize() {
				return content.getMinimumSize();
			}

			@Override
			public Dimension getMaximumSize() {
				return content.getMaximumSize();
			}

			@Override
			public boolean isOptimizedDrawingEnabled() {
				return false;
			}
		};
		layered.setOpaque(false);
		layered.putClientProperty("anonymizeContent", content);
		return layered;
	}

	private void nudgeWidthSplit(int dx) {
		nudgeSplitDivider(widthSplit, dx, 160);
	}

	private void nudgeSqlPlanDivider(int dy) {
		if (!isConnectSourceMode()) {
			return;
		}
		nudgeSplitDivider(sqlJsonSplit, dy, 40);
	}

	private void nudgePlanPreferredHeight(int dy) {
		if (jsonPlanTextAreaScrollPane == null || dy == 0) {
			return;
		}
		Dimension pref = jsonPlanTextAreaScrollPane.getPreferredSize();
		int width = Math.max(160, pref.width > 0 ? pref.width : AREA_WIDTH);
		int height = Math.max(80, (pref.height > 0 ? pref.height : AREA_HEIGHT) + dy);
		sizeTextArea(jsonPlanTextAreaScrollPane, width, height);
		if (planFieldHost != null) {
			planFieldHost.revalidate();
		}
		revalidate();
		repaint();
	}

	static JLabel createAnonymizeBadge() {
		JLabel badge = new JLabel(loadAnonymizeSilhouetteIcon());
		badge.setToolTipText("Showing anonymized text");
		badge.setVisible(false);
		badge.setOpaque(false);
		return badge;
	}

	/**
	 * Overlay a silhouette inside the top-right of a text field / scroll pane.
	 * Host tracks the content component's preferred/min/max sizes so split panes
	 * and form layout can still resize the field.
	 */
	static JComponent wrapWithAnonymizeBadge(JComponent content, JLabel badge) {
		JPanel badgeCorner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				// Keep a real size even while the badge is invisible so the first
				// layout after Anonymize-on does not leave a 0×0 overlay.
				Dimension d = super.getPreferredSize();
				int iw = 20;
				int ih = 20;
				if (badge != null && badge.getIcon() != null) {
					iw = Math.max(iw, badge.getIcon().getIconWidth());
					ih = Math.max(ih, badge.getIcon().getIconHeight());
				}
				EmptyBorder b = null;
				if (getBorder() instanceof EmptyBorder) {
					b = (EmptyBorder) getBorder();
				}
				int padX = b == null ? 6 : b.getBorderInsets().left + b.getBorderInsets().right;
				int padY = b == null ? 4 : b.getBorderInsets().top + b.getBorderInsets().bottom;
				return new Dimension(Math.max(d.width, iw + padX), Math.max(d.height, ih + padY));
			}
		};
		badgeCorner.setOpaque(false);
		badgeCorner.setBorder(new EmptyBorder(4, 0, 0, 6));
		badgeCorner.add(badge);

		JLayeredPane layered = newTrackingLayeredPane(content);
		SeResizeGrip seGrip = new SeResizeGrip();
		layered.putClientProperty("anonymizeBadgeCorner", badgeCorner);
		layered.putClientProperty("seResizeGrip", seGrip);
		layered.add(content, JLayeredPane.DEFAULT_LAYER);
		layered.add(badgeCorner, JLayeredPane.PALETTE_LAYER);
		layered.add(seGrip, JLayeredPane.DRAG_LAYER);
		layered.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutFieldOverlays(layered, content, badgeCorner);
			}

			@Override
			public void componentShown(ComponentEvent e) {
				layoutFieldOverlays(layered, content, badgeCorner);
			}
		});
		layoutFieldOverlays(layered, content, badgeCorner);
		return layered;
	}

	/** @deprecated use {@link #layoutFieldOverlays}; kept name for call sites / tests. */
	static void layoutAnonymizeBadgeOverlay(JLayeredPane layered, JComponent content,
			JComponent badgeCorner) {
		layoutFieldOverlays(layered, content, badgeCorner);
	}

	static void layoutFieldOverlays(JLayeredPane layered, JComponent content,
			JComponent badgeCorner) {
		int w = Math.max(layered.getWidth(), 0);
		int h = Math.max(layered.getHeight(), 0);
		content.setBounds(0, 0, w, h);
		if (badgeCorner != null) {
			Dimension badgeSize = badgeCorner.getPreferredSize();
			int bw = Math.max(badgeSize.width, 26);
			int bh = Math.max(badgeSize.height, 24);
			badgeCorner.setBounds(Math.max(0, w - bw), 0, bw, bh);
			badgeCorner.revalidate();
		}
		Object gripObj = layered.getClientProperty("seResizeGrip");
		if (gripObj instanceof SeResizeGrip) {
			SeResizeGrip grip = (SeResizeGrip) gripObj;
			Dimension gs = grip.getPreferredSize();
			grip.setBounds(Math.max(0, w - gs.width - 2), Math.max(0, h - gs.height - 2),
					gs.width, gs.height);
		}
	}

	static ImageIcon loadAnonymizeSilhouetteIcon() {
		java.net.URL url = PlanSourcePanel.class.getResource("/images/anonymize-silhouette.png");
		if (url == null) {
			return null;
		}
		ImageIcon raw = new ImageIcon(url);
		Image scaled = raw.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
		return new ImageIcon(scaled);
	}

	/**
	 * Form panel that stretches to the viewport width when there is extra room, so
	 * SQL/JSON areas can grow horizontally; still scrolls horizontally when narrow.
	 */
	private static final class WidthTrackingForm extends JPanel implements Scrollable {
		private static final long serialVersionUID = 1L;

		WidthTrackingForm() {
			super(new GridBagLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
			return orientation == SwingConstants.VERTICAL ? Math.max(visible.height * 9 / 10, 1)
					: Math.max(visible.width * 9 / 10, 1);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			Container parent = getParent();
			if (parent instanceof JViewport) {
				return ((JViewport) parent).getWidth() > getPreferredSize().width;
			}
			return false;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
}

