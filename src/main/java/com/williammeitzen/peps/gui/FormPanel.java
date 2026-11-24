package com.williammeitzen.peps.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatTreeUI;
import com.williammeitzen.peps.gui.Utils.LineThicknesses;

//import com.github.cliftonlabs.json_simple.JsonException;
//import com.github.cliftonlabs.json_simple.JsonArray;
//import com.github.cliftonlabs.json_simple.JsonObject;
//import com.github.cliftonlabs.json_simple.JsonKey;


import com.williammeitzen.peps.model.Database;
import com.williammeitzen.peps.model.OperatorWarnings;

public class FormPanel extends JPanel {

	public static final String DOCUMENTS_CARD_TABS = "tabs";
	public static final String DOCUMENTS_CARD_EMPTY = "empty";

	private JButton addPlanBtn;
	private JButton addFolderBtn;
	private JButton deletePlanBtn;
	private JButton duplicatePlanBtn;
	private JButton settingsBtn;
	private JPanel planToolbar;
	private JPanel documentsHost;
	private DocumentsEmptyPanel documentsEmptyPanel;

	/** When true, sibling order is drag-arranged (sort_order); when false, sorted by name. */
	private boolean plansSortManual = false;
	/** Path under the last Plans context-menu click (Manual Add inserts here). */
	private TreePath contextMenuAnchorPath;
	/** Name sort direction for the Plans tree (By name mode / Sort Now). */
	private boolean plansSortNameAscending = true;
	/** How folders and plans are grouped when sorting by name. */
	private PlansSortGrouping plansSortGrouping = PlansSortGrouping.FOLDERS_FIRST;

	private enum PlansSortGrouping {
		FOLDERS_FIRST,
		PLANS_FIRST,
		MIXED
	}
	//private FormListener formListener;
	private JTree planTree;
	private DefaultMutableTreeNode planTreeRoot;
	private DefaultTreeModel planTreeModel;
	private boolean suppressTreeSelectionEvents;
	private static final String DEFAULT_FOLDER_NAME = "New Folder";
	private JTabbedPane tabPaneToLink;
	private PlanDiagramToolbar planDiagramToolbar;
	private JScrollPane planListScrollPane;
	private String PLAN_LIST_FONT_NAME = "Arial";
	private int PLAN_LIST_FONT_STYLE = Font.PLAIN;
	private int PLAN_LIST_FONT_SIZE = 12;
	
	/** Keyed by id_explain_plan. */
	public Map<Integer, PlanEvent> plansMapper = new TreeMap<Integer, PlanEvent>();
	private PlanEvent planEvent;
	private boolean comparePlansRefreshInProgress = false;
	private JCheckBoxMenuItem propertiesViewMenuItem;
	private JCheckBoxMenuItem slidersViewMenuItem;
	private JCheckBoxMenuItem zoomViewMenuItem;
	private boolean suppressDiagramViewMenuEvents;

	public FormPanel(JTabbedPane tabPane, PlanDiagramToolbar planDiagramToolbar) {
		this(tabPane, planDiagramToolbar, null);
	}

	public FormPanel(JTabbedPane tabPane, PlanDiagramToolbar planDiagramToolbar, JPanel documentsHost) {
		
		tabPaneToLink = tabPane;
		this.planDiagramToolbar = planDiagramToolbar;
		this.documentsHost = documentsHost;
		// One shared Find checkbox → always the selected Plan Diagram tab's strip.
		if (planDiagramToolbar != null) {
			planDiagramToolbar.setFindVisibilityListener(show -> {
				DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
				if (diagram == null) {
					return;
				}
				if (Boolean.TRUE.equals(show)) {
					diagram.showFind();
				} else {
					diagram.hideFind();
				}
			});
		}
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
		// Keep preferred width; small minimum so the Plans|Documents docking divider
		// still has travel when the application window is narrow.
		setMinimumSize(new Dimension(80, 80));
		String strSQL;
		JsonArray jsnRows;
		//String strPlanName;
		//JsonObject jsnObject;
		//db = new Database();
		JsonObject jsnRow;

		// Plans tree: invisible root; children = root folders + root plans
		planTreeRoot = new DefaultMutableTreeNode("Plans");
		planTreeModel = new DefaultTreeModel(planTreeRoot);
		planTree = new JTree(planTreeModel);
		planTree.setRootVisible(false);
		planTree.setShowsRootHandles(true);
		planTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		planTree.setFont(new Font(PLAN_LIST_FONT_NAME, PLAN_LIST_FONT_STYLE, PLAN_LIST_FONT_SIZE));
		loadPlansSortPreferences();
		installPlansTreeChrome();
		installPlansTreeDragAndDrop();
		installPlansTreeFolderRowToggle();

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
		
		// retrieve folders from db (nested)
		Map<Integer, DefaultMutableTreeNode> folderNodes = loadPlanFoldersIntoTree();

		// retrieve plans from db
		strSQL = "select id_explain_plan, plan_name, folder_id, sort_order, server, port, database, username, password, remember_password\n"
				+ ", auth_type, pre_explain_command, explain_sql_command, post_explain_command, plan_text\n"
				+ ", anonymize, anonymize_aliases\n"
				+ "from explain_plans\n"
				+ "order by sort_order, lower(plan_name), id_explain_plan;\n";
		jsnRows = Database.rows(strSQL);
		for (int intRow = 0; jsnRows != null && intRow < jsnRows.size(); intRow++) {
			jsnRow = (JsonObject) jsnRows.get(intRow);

			Integer newPlanId = toInteger(jsnRow.get("id_explain_plan"));
			if (newPlanId == null) {
				continue;
			}
			String newPlanName = jsnRow.getString("plan_name");
			Integer planFolderId = toInteger(jsnRow.get("folder_id"));
			int planSortOrder = toIntOrZero(jsnRow.get("sort_order"));
			DefaultMutableTreeNode parentNode = planFolderId == null ? null : folderNodes.get(planFolderId);
			if (parentNode == null) {
				planFolderId = null;
				parentNode = planTreeRoot;
			}
			parentNode.add(new DefaultMutableTreeNode(
					PlansTreeNode.plan(newPlanId.intValue(), planFolderId, newPlanName, planSortOrder)));
			// Selection restored after all plans load (see restoreSelectedPlanAfterLoad).

			PlanSourcePanel newPlanSourcePanel = new PlanSourcePanel(tabPane);
			PlanGraphPanel newPlanGraphPanel = new PlanGraphPanel(tabPane);
			PlanEvent pEvent = new PlanEvent(newPlanSourcePanel, newPlanGraphPanel);
			pEvent.setPlanName(newPlanName);
			newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
			newPlanGraphPanel.setIDExplainPlan(newPlanId);

			plansMapper.put(newPlanId, pEvent);
			
			tabPaneToLink.removeAll();
			
			tabPaneToLink.addTab(getPlanSourceTabName(newPlanName, pEvent.getPlanSourcePanel()),
					pEvent.getPlanSourcePanel());
			
			// add "Plan Statistics" tab
			// by stas
			PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
			JScrollPane statisticsPane = new JScrollPane(statisticsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			statisticsPane.setPreferredSize(new Dimension(320, 480));
			tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);

			// during population, get stats only on the last row retrieved
			if (intRow == jsnRows.size() - 1)
				PlanStatisticsPanel.setPlanNamesDelimited("'" + newPlanName.replace("'", "''") + "'");
			
		    // see if the user chose the Compare Plans tab
		    tabPaneToLink.addChangeListener(new ChangeListener() {
		        public void stateChanged(ChangeEvent e) {
		            if (tabPaneToLink.getSelectedIndex() == indexOfTabTitle(getPlanStatisticsTabName())) {
		            	refreshComparePlansTab(statisticsPanel);
		            }
		        }
			});

			// Properties full height on the right; graph + sliders on the left
			tabPaneToLink.addTab(getPlanDiagramTabName(newPlanName, pEvent.getPlanSourcePanel()),
					createPlanDiagramTab(pEvent.getPlanGraphPanel(), 600, 600));
			
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
			newPlanSourcePanel.setAnonymizeFlags(
					Utils.objectToBoolean(jsnRow.get("anonymize")),
					jsnRow.get("anonymize_aliases") == null
							|| Utils.objectToBoolean(jsnRow.get("anonymize_aliases")));

			newPlanGraphPanel.setIDExplainPlan(newPlanId);
			
			updatePlanDiagramTabEnabled();
		}
		planTreeModel.reload();
		sortEntirePlansTree(false);
		planTree.expandPath(new TreePath(planTreeRoot));

		addPlanBtn = createPlanToolbarButton("add", "Add Plan");
		addFolderBtn = createPlanToolbarButton("addFolder", "Add Folder");
		deletePlanBtn = createPlanToolbarButton("delete", "Delete Selected");
		duplicatePlanBtn = createPlanToolbarButton("duplicate", "Duplicate Plan(s)");
		settingsBtn = createPlanToolbarButton("settings", "Settings");
		updateAddButtonsEnabled();

		planToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		planToolbar.setOpaque(false);
		planToolbar.add(addPlanBtn);
		planToolbar.add(addFolderBtn);
		planToolbar.add(deletePlanBtn);
		planToolbar.add(duplicatePlanBtn);
		planToolbar.add(settingsBtn);

		planListScrollPane = new JScrollPane(planTree);
		planListScrollPane.setMinimumSize(new Dimension(60, 60));
		planListScrollPane.setBorder(BorderFactory.createEmptyBorder());
		planTree.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		installPlansTreeContextMenu();
		
		planTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				onPlanTreeSelectionChanged();
			}
		});
		
		addPlanBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addPlan();
			}
		});

		addFolderBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addFolder();
			}
		});
		
		deletePlanBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSelectedItems();
			}
		});

		duplicatePlanBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				duplicateSelectedPlans();
			}
		});

		settingsBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPlansSettingsDialog();
			}
		});
		
		// Dockable title is "Plans"; avoid a second titled border here.
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		restoreSelectedPlanAfterLoad();
		
		layoutComponents();
	}

	public void setDocumentsEmptyPanel(DocumentsEmptyPanel emptyPanel) {
		this.documentsEmptyPanel = emptyPanel;
	}

	/** Show the Documents landing screen when there are no plans; otherwise show tabs. */
	public void refreshDocumentsEmptyState() {
		if (documentsHost == null || !(documentsHost.getLayout() instanceof CardLayout)) {
			return;
		}
		CardLayout cards = (CardLayout) documentsHost.getLayout();
		if (plansMapper == null || plansMapper.isEmpty()) {
			if (tabPaneToLink != null) {
				tabPaneToLink.removeAll();
			}
			cards.show(documentsHost, DOCUMENTS_CARD_EMPTY);
			if (documentsEmptyPanel != null) {
				documentsEmptyPanel.refreshChrome();
			}
			SwingUtilities.invokeLater(this::showNewPlanGuideArrow);
		} else {
			HollowGuideArrowOverlay.dismiss();
			cards.show(documentsHost, DOCUMENTS_CARD_TABS);
		}
		documentsHost.revalidate();
		documentsHost.repaint();
	}

	/** Animate a hollow arrow from “New blank plan” to the Plans green plus. */
	private void showNewPlanGuideArrow() {
		if (documentsEmptyPanel == null || addPlanBtn == null) {
			HollowGuideArrowOverlay.dismiss();
			return;
		}
		if (plansMapper != null && !plansMapper.isEmpty()) {
			HollowGuideArrowOverlay.dismiss();
			return;
		}
		JButton blank = documentsEmptyPanel.getNewBlankPlanButton();
		if (blank == null || !blank.isShowing() || !addPlanBtn.isShowing()) {
			HollowGuideArrowOverlay.dismiss();
			return;
		}
		HollowGuideArrowOverlay.show(blank, addPlanBtn);
	}

	/** Plans toolbar Add Plan button (green plus). */
	public JButton getAddPlanButton() {
		return addPlanBtn;
	}

	/** Empty-state / toolbar: create a blank plan. */
	public void addBlankPlan() {
		addPlan();
		refreshDocumentsEmptyState();
	}

	/** Empty-state: create a plan filled with sample JSON EXPLAIN output and its EXPLAIN SQL. */
	public void addSampleJsonPlan() {
		addPlanWithContent("Sample JSON plan", SampleExplainPlans.jsonExplainSql(),
				SampleExplainPlans.jsonPlanText());
	}

	/** Empty-state: create a plan filled with sample text EXPLAIN output and its EXPLAIN SQL. */
	public void addSampleTextPlan() {
		addPlanWithContent("Sample text plan", SampleExplainPlans.textExplainSql(),
				SampleExplainPlans.textPlanText());
	}

	// ------------------------------------------------------------------
	// Tree selection
	// ------------------------------------------------------------------

	private void onPlanTreeSelectionChanged() {
		boolean bolShowProperties = false;

		if (suppressTreeSelectionEvents) {
			return;
		}
		updateAddButtonsEnabled();

		if (getSelectedTreeNodes().isEmpty()) {
			return;
		}

		List<PlansTreeNode> selectedPlans = getSelectedPlanNodes();
		if (selectedPlans.isEmpty()) {
			// Folder(s) only: no Plan Source content; Compare uses plans under the folder(s).
			showFolderOnlySelectionTabs();
			return;
		}

		PlansTreeNode primary = getPrimarySelectedPlan();
		if (primary == null) {
			return;
		}
		Integer selectedPlanId = primary.getPlanId();
		String selectedPlanName = primary.getName();
		Database.saveSelectedPlanId(selectedPlanId);
		Database.saveSelectedPlanName(selectedPlanName);

		if (bolShowProperties == true) { System.out.println("FormPanel selectedPlanName = " + selectedPlanName); }

		tabPaneToLink.removeAll();

		PlanEvent pEvent = plansMapper.get(selectedPlanId);

		if (bolShowProperties == true) { System.out.println("FormPanel pEvent = " + pEvent); }

		if (pEvent != null) {

			PlanSourcePanel sourcePanel = pEvent.getPlanSourcePanel();
			boolean multiPlan = selectedPlans.size() > 1;

			// Multi-select: empty/disabled Plan Source (one plan's connection info is confusing).
			if (multiPlan) {
				tabPaneToLink.addTab(getPlanSourceTabName(null), new JPanel());
				tabPaneToLink.setEnabledAt(0, false);
			} else {
				tabPaneToLink.addTab(getPlanSourceTabName(selectedPlanName, sourcePanel), sourcePanel);

				// Translate sits between Plan Source and Compare Plans, only when Anonymize is on.
				if (sourcePanel.isAnonymizeEnabled()) {
					TranslateAnonymizedPanel translatePanel = new TranslateAnonymizedPanel();
					translatePanel.setPlanSourcePanel(sourcePanel);
					tabPaneToLink.addTab(getTranslateTabName(), translatePanel);
				}
			}

			// add "Plan Statistics" tab
			// by stas
			PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
			JScrollPane statisticsPane = new JScrollPane(statisticsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			statisticsPane.setPreferredSize(new Dimension(320, 480));
			tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);

			String strPlanNamesDelimted = "";
			for (int i = 0; i < selectedPlans.size(); i++) {
				if (i > 0)
					strPlanNamesDelimted += ", ";
				strPlanNamesDelimted += "'" + selectedPlans.get(i).getName().replace("'", "''") + "'";
			}

			// plan names are already SQ delimited
			if (bolShowProperties == true) { System.out.println("FormPanel strPlanNamesDelimted = " + strPlanNamesDelimted); }
			PlanStatisticsPanel.setPlanNamesDelimited(strPlanNamesDelimted);

			// see if the user chose the Compare Plans tab
			tabPaneToLink.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (tabPaneToLink.getSelectedIndex() == indexOfTabTitle(getPlanStatisticsTabName())) {
						refreshComparePlansTab(statisticsPanel);
					}
				}
			});

			JPanel diagramTab = createPlanDiagramTab(pEvent.getPlanGraphPanel(), 1000, 500, sourcePanel);
			tabPaneToLink.addTab(
					multiPlan ? getPlanDiagramTabName(null, sourcePanel)
							: getPlanDiagramTabName(selectedPlanName, sourcePanel),
					diagramTab);

			if (!multiPlan) {
				sourcePanel.setAnonymizeModeListener(() ->
						syncAnonymizeChrome(selectedPlanName, sourcePanel, diagramTab));
			}

			updatePlanDiagramTabEnabled();

			if (multiPlan) {
				// Prefer Compare Plans when several plans are selected (Source is disabled).
				int compareIdx = indexOfTabTitle(getPlanStatisticsTabName());
				if (compareIdx >= 0) {
					tabPaneToLink.setSelectedIndex(compareIdx);
				}
			}

			// Folder-only blanks the toolbar; force a refresh on next diagram paint even if
			// this is the same plan id (enable previously ran only when the id changed).
			PlanGraphPanel.requestDiagramToolbarRefresh();
			if (pEvent.getPlanGraphPanel() != null) {
				applySharedZoomToGraph(pEvent.getPlanGraphPanel());
				pEvent.getPlanGraphPanel().repaint();
			}

			if (!multiPlan) {
				sourcePanel.refreshFolderChooser();
			}
		}
	}

	/**
	 * Folder selection: hide plan source UI, disable Plan Source and Plan Diagram,
	 * keep Compare Plans for descendant plans (if any).
	 */
	private void showFolderOnlySelectionTabs() {
		if (tabPaneToLink == null) {
			return;
		}
		List<TreePath> planPaths = new ArrayList<>();
		for (DefaultMutableTreeNode node : getSelectedTreeNodes()) {
			PlansTreeNode d = data(node);
			if (d != null && d.isFolder()) {
				collectDescendantPlanPaths(node, planPaths);
			}
		}

		tabPaneToLink.removeAll();

		tabPaneToLink.addTab(getPlanSourceTabName(null), new JPanel());
		tabPaneToLink.setEnabledAt(0, false);

		PlanStatisticsPanel statisticsPanel = new PlanStatisticsPanel();
		JScrollPane statisticsPane = new JScrollPane(statisticsPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		statisticsPane.setPreferredSize(new Dimension(320, 480));
		tabPaneToLink.addTab(getPlanStatisticsTabName(), statisticsPane);

		StringBuilder delimited = new StringBuilder();
		for (int i = 0; i < planPaths.size(); i++) {
			PlansTreeNode d = data(planPaths.get(i).getLastPathComponent());
			if (d == null || !d.isPlan()) {
				continue;
			}
			if (delimited.length() > 0) {
				delimited.append(", ");
			}
			delimited.append("'").append(d.getName().replace("'", "''")).append("'");
		}
		PlanStatisticsPanel.setPlanNamesDelimited(delimited.length() == 0 ? "''" : delimited.toString());

		tabPaneToLink.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (tabPaneToLink.getSelectedIndex() == indexOfTabTitle(getPlanStatisticsTabName())) {
					refreshComparePlansTab(statisticsPanel);
				}
			}
		});

		tabPaneToLink.addTab(getPlanDiagramTabName(null), new JPanel());
		int diagramIdx = indexOfTabContaining("Plan Diagram");
		if (diagramIdx >= 0) {
			tabPaneToLink.setEnabledAt(diagramIdx, false);
		}

		tabPaneToLink.setSelectedIndex(planPaths.isEmpty() ? 0 : 1);

		if (planDiagramToolbar != null) {
			planDiagramToolbar.setAllDiagramOptionsEnabled(false);
		}
		PlanGraphPanel.requestDiagramToolbarRefresh();
	}

	/**
	 * Add Plan / Add Folder only with at most one tree item selected.
	 * Delete needs any selection; Duplicate needs at least one plan.
	 */
	private void updateAddButtonsEnabled() {
		int selectionCount = getSelectedTreeNodes().size();
		boolean allowAdd = selectionCount <= 1;
		if (addPlanBtn != null) {
			addPlanBtn.setEnabled(allowAdd);
		}
		if (addFolderBtn != null) {
			addFolderBtn.setEnabled(allowAdd);
		}
		if (deletePlanBtn != null) {
			deletePlanBtn.setEnabled(selectionCount > 0);
		}
		if (duplicatePlanBtn != null) {
			duplicatePlanBtn.setEnabled(!getSelectedPlanNodes().isEmpty());
		}
	}

	// ------------------------------------------------------------------
	// Toolbar actions
	// ------------------------------------------------------------------

	private void addPlan() {
		addPlanWithContent(null, null, null);
	}

	/**
	 * Create a plan at the current add target. Optional SQL / plan text are filled and persisted
	 * (used by the Documents empty-state sample buttons).
	 */
	private void addPlanWithContent(String preferredName, String explainSql, String planText) {
		if (getSelectedTreeNodes().size() > 1) {
			return;
		}
		TreePath addAnchor = contextMenuAnchorPath;
		DefaultMutableTreeNode targetFolder = resolveAddTargetFolderNode();
		Integer folderId = folderIdOf(targetFolder);
		Integer insertIndex = resolveManualInsertIndex(targetFolder, addAnchor);

		PlanSourcePanel newPlanSourcePanel = new PlanSourcePanel(tabPaneToLink);
		PlanGraphPanel newPlanGraphPanel = new PlanGraphPanel(tabPaneToLink);
		PlanEvent pEvent = new PlanEvent(newPlanSourcePanel, newPlanGraphPanel);
		Integer newPlanId = preferredName == null
				? addPlanToDatabase(folderId, newPlanSourcePanel, newPlanGraphPanel)
				: addNamedPlanToDatabase(preferredName, folderId, newPlanSourcePanel, newPlanGraphPanel);
		if (newPlanId == null) {
			return;
		}
		JsonObject nameRow = Database.row("select plan_name from explain_plans where id_explain_plan = " + newPlanId);
		String newPlanName = nameRow == null ? ("Plan " + newPlanId) : String.valueOf(nameRow.get("plan_name"));

		pEvent.setPlanName(newPlanName);
		newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
		if (explainSql != null) {
			newPlanSourcePanel.setSQLQueryText(explainSql);
		}
		if (planText != null) {
			newPlanSourcePanel.setJsonPlanText(planText);
		}
		newPlanGraphPanel.setIDExplainPlan(newPlanId);
		plansMapper.put(newPlanId, pEvent);

		if (explainSql != null || planText != null) {
			Database.updateExplainPlanFields(newPlanId.intValue(), newPlanName,
					newPlanSourcePanel.getPlanSourceFromQueryServerField(),
					newPlanSourcePanel.getPlanSourceFromQueryPortField(),
					newPlanSourcePanel.getPlanSourceFromQueryDatabaseField(),
					newPlanSourcePanel.getPlanSourceFromQueryUserField(),
					newPlanSourcePanel.getPlanSourceFromQueryPasswordField(),
					newPlanSourcePanel.getPlanSourceFromQueryRememberPasswordCheckbox(),
					newPlanSourcePanel.getAuthenticationTypeCode(),
					newPlanSourcePanel.getPlanSourceFromQueryPreCommandField(),
					newPlanSourcePanel.getSQLQueryText(),
					newPlanSourcePanel.getPlanSourceFromQueryPostCommandField(),
					newPlanSourcePanel.getJsonPlanText());
		}

		int sortOrder = readPlanSortOrder(newPlanId.intValue());
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(
				PlansTreeNode.plan(newPlanId.intValue(), folderId, newPlanName, sortOrder));
		insertTreeNode(targetFolder, node, insertIndex);
		refreshDocumentsEmptyState();
		// Selection listener builds the Plan Source / Compare / Diagram tabs.
		if (planText != null) {
			newPlanSourcePanel.requestShowDiagramCue();
		}
		selectTreeNode(node);
		if (planText != null) {
			SwingUtilities.invokeLater(newPlanSourcePanel::showShowDiagramCue);
		}
	}

	private void addFolder() {
		if (getSelectedTreeNodes().size() > 1) {
			return;
		}
		TreePath addAnchor = contextMenuAnchorPath;
		DefaultMutableTreeNode targetFolder = resolveAddTargetFolderNode();
		Integer parentId = folderIdOf(targetFolder);
		Integer insertIndex = resolveManualInsertIndex(targetFolder, addAnchor);

		Object input = JOptionPane.showInputDialog(this, "Folder name:", "Add Folder",
				JOptionPane.PLAIN_MESSAGE, null, null, DEFAULT_FOLDER_NAME);
		if (input == null) {
			return; // cancelled
		}
		String requested = String.valueOf(input).trim();
		if (requested.isEmpty()) {
			requested = DEFAULT_FOLDER_NAME;
		}
		String folderName = uniqueFolderName(requested, parentId);
		Integer newFolderId = insertFolderToDatabase(folderName, parentId);
		if (newFolderId == null) {
			return;
		}
		int sortOrder = readFolderSortOrder(newFolderId.intValue());
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(
				PlansTreeNode.folder(newFolderId.intValue(), folderName, sortOrder));
		insertTreeNode(targetFolder, node, insertIndex);
		selectTreeNode(node);
	}

	private void deleteSelectedItems() {
		List<DefaultMutableTreeNode> topLevel = getTopLevelSelectedNodes();
		if (topLevel.isEmpty()) {
			return;
		}

		// select the Plan Source tab before we start deleting plans
		// otherwise we may get exceptions when we try to show plan info from
		// plans that suddenly disappear
		if (tabPaneToLink.getTabCount() > 0) {
			tabPaneToLink.setSelectedIndex(0);
		}

		int planCount = 0;
		int folderCount = 0;
		int descendantCount = 0;
		String singleFolderName = null;
		for (DefaultMutableTreeNode node : topLevel) {
			PlansTreeNode d = data(node);
			if (d == null) {
				continue;
			}
			if (d.isPlan()) {
				planCount++;
			} else {
				folderCount++;
				singleFolderName = d.getName();
				descendantCount += countDescendantItems(node);
			}
		}

		String msg;
		int messageType = JOptionPane.QUESTION_MESSAGE;
		if (folderCount == 0) {
			msg = "Are you sure you want to delete ";
			msg += planCount == 1 ? "this plan?" : String.format("%d plans?", planCount);
		} else if (topLevel.size() == 1 && descendantCount == 0) {
			msg = "Are you sure you want to delete the empty folder \"" + singleFolderName + "\"?";
		} else if (topLevel.size() == 1) {
			msg = "The folder \"" + singleFolderName + "\" is not empty.\n"
					+ "It contains " + descendantCount + (descendantCount == 1 ? " item" : " items")
					+ " (plans and subfolders).\n\n"
					+ "Delete the folder and everything inside it?";
			messageType = JOptionPane.WARNING_MESSAGE;
		} else {
			msg = String.format("Are you sure you want to delete the %d selected items?", topLevel.size());
			if (descendantCount > 0) {
				msg += "\n\nThe selected folder(s) contain " + descendantCount
						+ (descendantCount == 1 ? " item" : " items")
						+ " (plans and subfolders) that will also be deleted.";
				messageType = JOptionPane.WARNING_MESSAGE;
			}
		}

		JDialog.setDefaultLookAndFeelDecorated(true);
		int response = JOptionPane.showConfirmDialog(null, msg, "Confirm",
				JOptionPane.YES_NO_OPTION, messageType);
		if (response != JOptionPane.YES_OPTION) {
			return;
		}

		DefaultMutableTreeNode firstNode = topLevel.get(0);
		DefaultMutableTreeNode neighborParent = firstNode.getParent() instanceof DefaultMutableTreeNode
				? (DefaultMutableTreeNode) firstNode.getParent() : planTreeRoot;
		int neighborIndex = neighborParent.getIndex(firstNode);

		suppressTreeSelectionEvents = true;
		try {
			planTree.clearSelection();
			for (DefaultMutableTreeNode node : topLevel) {
				List<Integer> planIds = new ArrayList<>();
				List<Integer> folderIds = new ArrayList<>();
				collectSubtreeIds(node, planIds, folderIds);
				deleteItemsFromDatabase(planIds, folderIds);
				for (Integer planId : planIds) {
					plansMapper.remove(planId);
				}
				planTreeModel.removeNodeFromParent(node);
			}
		} finally {
			suppressTreeSelectionEvents = false;
		}

		DefaultMutableTreeNode next = null;
		if (neighborParent.getChildCount() > 0) {
			next = (DefaultMutableTreeNode) neighborParent.getChildAt(
					Math.min(Math.max(neighborIndex, 0), neighborParent.getChildCount() - 1));
		} else if (neighborParent != planTreeRoot) {
			next = neighborParent;
		} else if (planTreeRoot.getChildCount() > 0) {
			next = (DefaultMutableTreeNode) planTreeRoot.getChildAt(planTreeRoot.getChildCount() - 1);
		}

		if (next != null) {
			selectTreeNode(next);
		} else {
			// everything is gone — remove the tabs, too
			tabPaneToLink.removeAll();
			Database.saveSelectedPlanId(null);
			updateAddButtonsEnabled();
			refreshDocumentsEmptyState();
		}
	}

	private void duplicateSelectedPlans() {
		boolean bolShowSQL = false;
		List<DefaultMutableTreeNode> planNodes = new ArrayList<>();
		for (DefaultMutableTreeNode node : getSelectedTreeNodes()) {
			PlansTreeNode d = data(node);
			if (d != null && d.isPlan()) {
				planNodes.add(node);
			}
		}

		DefaultMutableTreeNode lastNewNode = null;
		for (DefaultMutableTreeNode sourceNode : planNodes) {
			PlansTreeNode sourceData = data(sourceNode);
			Integer sourcePlanId = sourceData.getPlanId();
			String planNameValue = sourceData.getName();

			PlanSourcePanel existingPlanSource = null;
			PlanEvent existingEvent = plansMapper.get(sourcePlanId);
			if (existingEvent != null) {
				existingPlanSource = existingEvent.getPlanSourcePanel();
			}

			// duplicate lives in the same folder as the source; name is unique within that folder
			Integer folderId = sourceData.getFolderId();
			DefaultMutableTreeNode parentNode = sourceNode.getParent() instanceof DefaultMutableTreeNode
					? (DefaultMutableTreeNode) sourceNode.getParent() : planTreeRoot;
			String newPlanName = nextCopyName(planNameValue, planNamesInFolder(folderId));
			if (bolShowSQL == true) { System.out.println("duplicate plan name: " + newPlanName); }

			PlanSourcePanel newPlanSourcePanel = new PlanSourcePanel(tabPaneToLink);
			PlanGraphPanel newPlanGraphPanel = new PlanGraphPanel(tabPaneToLink);
			PlanEvent newPlnEvent = new PlanEvent(newPlanSourcePanel, newPlanGraphPanel);
			Integer newExplainPlanID = addPlanNameToDatabase(newPlanName, folderId, newPlanSourcePanel, newPlanGraphPanel);
			if (newExplainPlanID == null) {
				continue;
			}

			newPlnEvent.setPlanName(newPlanName);
			newPlanSourcePanel.setPlanSourceFromQueryPlanNameField(newPlanName);
			newPlanGraphPanel.setIDExplainPlan(newExplainPlanID);
			plansMapper.put(newExplainPlanID, newPlnEvent);

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

			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
					PlansTreeNode.plan(newExplainPlanID.intValue(), folderId, newPlanName,
							readPlanSortOrder(newExplainPlanID.intValue())));
			insertTreeNode(parentNode, newNode);
			lastNewNode = newNode;
		}

		if (lastNewNode != null) {
			// Selection listener builds the tabs for the (last) new duplicate.
			selectTreeNode(lastNewNode);
		}
	}

	// ------------------------------------------------------------------
	// Tree helpers
	// ------------------------------------------------------------------

	/** User object for a tree node, or null (invisible root / unexpected node). */
	private static PlansTreeNode data(Object treeNode) {
		if (treeNode instanceof DefaultMutableTreeNode) {
			Object o = ((DefaultMutableTreeNode) treeNode).getUserObject();
			if (o instanceof PlansTreeNode) {
				return (PlansTreeNode) o;
			}
		}
		return null;
	}

	/** Selected tree nodes in display order (never includes the invisible root). */
	private List<DefaultMutableTreeNode> getSelectedTreeNodes() {
		List<DefaultMutableTreeNode> out = new ArrayList<>();
		if (planTree == null) {
			return out;
		}
		TreePath[] paths = planTree.getSelectionPaths();
		if (paths == null) {
			return out;
		}
		List<TreePath> sorted = new ArrayList<>(Arrays.asList(paths));
		sorted.sort(Comparator.comparingInt(p -> planTree.getRowForPath(p)));
		for (TreePath path : sorted) {
			Object last = path.getLastPathComponent();
			if (last instanceof DefaultMutableTreeNode && last != planTreeRoot) {
				out.add((DefaultMutableTreeNode) last);
			}
		}
		return out;
	}

	/** Selected nodes whose ancestors are not also selected (a folder covers its contents). */
	private List<DefaultMutableTreeNode> getTopLevelSelectedNodes() {
		List<DefaultMutableTreeNode> selected = getSelectedTreeNodes();
		Set<DefaultMutableTreeNode> selectedSet = new HashSet<>(selected);
		List<DefaultMutableTreeNode> out = new ArrayList<>();
		for (DefaultMutableTreeNode node : selected) {
			boolean covered = false;
			for (TreeNode p = node.getParent(); p != null; p = p.getParent()) {
				if (selectedSet.contains(p)) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				out.add(node);
			}
		}
		return out;
	}

	/** Selected plan nodes (folders are ignored), in display order. */
	private List<PlansTreeNode> getSelectedPlanNodes() {
		List<PlansTreeNode> out = new ArrayList<>();
		for (DefaultMutableTreeNode node : getSelectedTreeNodes()) {
			PlansTreeNode d = data(node);
			if (d != null && d.isPlan()) {
				out.add(d);
			}
		}
		return out;
	}

	/** Plan ids of every selected plan node. */
	private List<Integer> getSelectedPlanIds() {
		List<Integer> ids = new ArrayList<>();
		for (PlansTreeNode d : getSelectedPlanNodes()) {
			ids.add(d.getPlanId());
		}
		return ids;
	}

	/** Lead selection if it is a plan, otherwise the first selected plan; null if no plan selected. */
	private PlansTreeNode getPrimarySelectedPlan() {
		if (planTree == null) {
			return null;
		}
		TreePath lead = planTree.getLeadSelectionPath();
		if (lead != null && planTree.isPathSelected(lead)) {
			PlansTreeNode d = data(lead.getLastPathComponent());
			if (d != null && d.isPlan()) {
				return d;
			}
		}
		List<PlansTreeNode> plans = getSelectedPlanNodes();
		return plans.isEmpty() ? null : plans.get(0);
	}

	/** Folder tree node that Add Plan / Add Folder should create into (root when nothing / a root plan is selected). */
	private DefaultMutableTreeNode resolveAddTargetFolderNode() {
		List<DefaultMutableTreeNode> selected = getSelectedTreeNodes();
		if (selected.size() != 1) {
			return planTreeRoot;
		}
		DefaultMutableTreeNode node = selected.get(0);
		PlansTreeNode d = data(node);
		if (d == null) {
			return planTreeRoot;
		}
		if (d.isFolder()) {
			return node;
		}
		TreeNode parent = node.getParent();
		return parent instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) parent : planTreeRoot;
	}

	/** Database id of a folder node, or null for the root. */
	private Integer folderIdOf(DefaultMutableTreeNode folderNode) {
		PlansTreeNode d = folderNode == null || folderNode == planTreeRoot ? null : data(folderNode);
		return d != null && d.isFolder() ? d.getFolderId() : null;
	}

	private int compareTreeData(PlansTreeNode a, PlansTreeNode b) {
		if (a == null && b == null) {
			return 0;
		}
		if (a == null) {
			return 1;
		}
		if (b == null) {
			return -1;
		}
		if (plansSortManual) {
			int orderCmp = Integer.compare(a.getSortOrder(), b.getSortOrder());
			if (orderCmp != 0) {
				return orderCmp;
			}
			if (a.isFolder() != b.isFolder()) {
				return a.isFolder() ? -1 : 1;
			}
			int nameCmp = a.getName().compareToIgnoreCase(b.getName());
			if (nameCmp != 0) {
				return nameCmp;
			}
			Integer aId = a.isFolder() ? a.getFolderId() : a.getPlanId();
			Integer bId = b.isFolder() ? b.getFolderId() : b.getPlanId();
			return Integer.compare(aId == null ? 0 : aId.intValue(), bId == null ? 0 : bId.intValue());
		}
		if (plansSortGrouping != PlansSortGrouping.MIXED && a.isFolder() != b.isFolder()) {
			if (plansSortGrouping == PlansSortGrouping.FOLDERS_FIRST) {
				return a.isFolder() ? -1 : 1;
			}
			return a.isFolder() ? 1 : -1; // plans first
		}
		int nameCmp = a.getName().compareToIgnoreCase(b.getName());
		return plansSortNameAscending ? nameCmp : -nameCmp;
	}

	/**
	 * Manual + context-menu: index among {@code targetFolder}'s children for the new item.
	 * Right-clicked plan → that sibling slot; right-clicked folder (add into it) → top (0).
	 */
	private Integer resolveManualInsertIndex(DefaultMutableTreeNode targetFolder, TreePath anchorPath) {
		if (!plansSortManual || targetFolder == null || anchorPath == null) {
			return null;
		}
		DefaultMutableTreeNode anchor = (DefaultMutableTreeNode) anchorPath.getLastPathComponent();
		if (anchor == null) {
			return null;
		}
		PlansTreeNode d = data(anchor);
		if (d != null && d.isFolder() && anchor == targetFolder) {
			return Integer.valueOf(0);
		}
		if (anchor.getParent() == targetFolder) {
			int index = targetFolder.getIndex(anchor);
			return index < 0 ? null : Integer.valueOf(index);
		}
		return null;
	}

	/** Insert per Settings (sorted by name, or appended for Manual) and expand the parent. */
	private void insertTreeNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
		insertTreeNode(parent, child, null);
	}

	/**
	 * @param forcedIndex Manual context-menu insert index, or null to append / name-sort
	 */
	private void insertTreeNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode child, Integer forcedIndex) {
		PlansTreeNode childData = data(child);
		int index = parent.getChildCount();
		if (plansSortManual && forcedIndex != null) {
			index = Math.max(0, Math.min(forcedIndex.intValue(), parent.getChildCount()));
		} else if (!plansSortManual && childData != null) {
			for (int i = 0; i < parent.getChildCount(); i++) {
				PlansTreeNode existing = data(parent.getChildAt(i));
				if (existing != null && compareTreeData(childData, existing) < 0) {
					index = i;
					break;
				}
			}
		}
		planTreeModel.insertNodeInto(child, parent, index);
		if (plansSortManual) {
			persistChildrenSortOrders(parent);
		}
		planTree.expandPath(new TreePath(parent.getPath()));
	}

	private void loadPlansSortPreferences() {
		plansSortManual = "manual".equalsIgnoreCase(Database.loadPlansSortMode());
		String nameOrder = Database.loadPlansSortNameOrder();
		plansSortNameAscending = !"desc".equalsIgnoreCase(nameOrder);
		String grouping = Database.loadPlansSortGrouping();
		if ("plans_first".equalsIgnoreCase(grouping)) {
			plansSortGrouping = PlansSortGrouping.PLANS_FIRST;
		} else if ("mixed".equalsIgnoreCase(grouping)) {
			plansSortGrouping = PlansSortGrouping.MIXED;
		} else {
			plansSortGrouping = PlansSortGrouping.FOLDERS_FIRST;
		}
	}

	private void savePlansSortPreferences() {
		Database.savePlansSortPreferences(
				plansSortManual ? "manual" : "by_name",
				plansSortNameAscending ? "asc" : "desc",
				plansSortGrouping == PlansSortGrouping.PLANS_FIRST ? "plans_first"
						: plansSortGrouping == PlansSortGrouping.MIXED ? "mixed" : "folders_first");
	}

	/** Left-aligned Warning Indicators row (no FlowLayout left inset). */
	private static JPanel warningIndicatorsRow(Component... parts) {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				row.add(Box.createHorizontalStrut(6));
			}
			row.add(parts[i]);
		}
		Dimension pref = row.getPreferredSize();
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
		return row;
	}

	/**
	 * Size a warning spinner from its text-field column count (preferredSize on
	 * JSpinner alone is ignored once the editor has laid out).
	 */
	private static void sizeWarningSpinnerColumns(JSpinner spinner, int columns) {
		JComponent editor = spinner.getEditor();
		if (editor instanceof JSpinner.DefaultEditor) {
			((JSpinner.DefaultEditor) editor).getTextField().setColumns(Math.max(1, columns));
		}
		spinner.setPreferredSize(null);
		spinner.setMinimumSize(null);
		spinner.setMaximumSize(null);
		Dimension pref = spinner.getPreferredSize();
		spinner.setPreferredSize(pref);
		spinner.setMinimumSize(pref);
		spinner.setMaximumSize(pref);
	}

	/** Rebuild open diagrams so warning chips pick up new Settings thresholds. */
	private void refreshOpenPlansAfterWarningSettingsChange() {
		for (PlanEvent planEvent : plansMapper.values()) {
			if (planEvent == null) {
				continue;
			}
			if (planEvent.getPlanSourcePanel() != null) {
				planEvent.getPlanSourcePanel().setPlanTextIsDirty(true);
			}
			if (planEvent.getPlanGraphPanel() != null) {
				planEvent.getPlanGraphPanel().repaint();
			}
		}
	}

	/**
	 * Recursively re-sort every folder (and the root) using the current Settings.
	 * {@code restoreSelection} keeps the current highlight when true.
	 */
	private void sortEntirePlansTree(boolean restoreSelection) {
		if (planTreeRoot == null || planTreeModel == null) {
			return;
		}
		List<Integer> selectedPlanIds = restoreSelection ? getSelectedPlanIds() : List.of();
		List<Integer> selectedFolderIds = new ArrayList<>();
		if (restoreSelection) {
			for (DefaultMutableTreeNode node : getSelectedTreeNodes()) {
				PlansTreeNode d = data(node);
				if (d != null && d.isFolder() && d.getFolderId() != null) {
					selectedFolderIds.add(d.getFolderId());
				}
			}
		}
		suppressTreeSelectionEvents = true;
		try {
			sortTreeNodeChildren(planTreeRoot, true);
		} finally {
			suppressTreeSelectionEvents = false;
		}
		if (restoreSelection) {
			List<TreePath> paths = new ArrayList<>();
			for (Integer planId : selectedPlanIds) {
				DefaultMutableTreeNode node = findPlanNode(planId.intValue());
				if (node != null) {
					paths.add(new TreePath(node.getPath()));
				}
			}
			for (Integer folderId : selectedFolderIds) {
				DefaultMutableTreeNode node = findFolderNode(folderId.intValue());
				if (node != null) {
					paths.add(new TreePath(node.getPath()));
				}
			}
			if (!paths.isEmpty()) {
				planTree.setSelectionPaths(paths.toArray(new TreePath[0]));
				planTree.scrollPathToVisible(paths.get(0));
			}
		}
	}

	private void sortTreeNodeChildren(DefaultMutableTreeNode parent, boolean recursive) {
		reorderChildrenInPlace(parent);
		if (!recursive) {
			return;
		}
		// Snapshot first — child count/order is stable after this level's reorder.
		List<DefaultMutableTreeNode> children = new ArrayList<>();
		for (int i = 0; i < parent.getChildCount(); i++) {
			children.add((DefaultMutableTreeNode) parent.getChildAt(i));
		}
		for (DefaultMutableTreeNode child : children) {
			PlansTreeNode d = data(child);
			if (d != null && d.isFolder()) {
				sortTreeNodeChildren(child, true);
			}
		}
	}

	/**
	 * Reorder {@code parent}'s children without emptying it first. Emptying the invisible
	 * root (or a folder) collapses it in JTree; with rootVisible=false that looks like
	 * every plan disappeared until the next restart expands the root again.
	 */
	private void reorderChildrenInPlace(DefaultMutableTreeNode parent) {
		int count = parent.getChildCount();
		if (count <= 1) {
			ensurePlansTreeParentExpanded(parent);
			return;
		}
		List<DefaultMutableTreeNode> children = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			children.add((DefaultMutableTreeNode) parent.getChildAt(i));
		}
		children.sort((left, right) -> compareTreeData(data(left), data(right)));

		TreePath parentPath = new TreePath(parent.getPath());
		List<TreePath> expandedDescendants = new ArrayList<>();
		Enumeration<TreePath> expanded = planTree.getExpandedDescendants(parentPath);
		if (expanded != null) {
			while (expanded.hasMoreElements()) {
				expandedDescendants.add(expanded.nextElement());
			}
		}

		for (int i = 0; i < children.size(); i++) {
			DefaultMutableTreeNode child = children.get(i);
			int from = parent.getIndex(child);
			if (from != i) {
				planTreeModel.removeNodeFromParent(child);
				planTreeModel.insertNodeInto(child, parent, i);
			}
		}

		ensurePlansTreeParentExpanded(parent);
		for (TreePath path : expandedDescendants) {
			planTree.expandPath(path);
		}
	}

	/** Invisible root must stay expanded or the Plans tree appears empty. */
	private void ensurePlansTreeParentExpanded(DefaultMutableTreeNode parent) {
		if (planTree == null || parent == null) {
			return;
		}
		planTree.expandPath(new TreePath(planTreeRoot));
		if (parent != planTreeRoot) {
			planTree.expandPath(new TreePath(parent.getPath()));
		}
	}

	/** Write each child's index as sort_order in memory and in the database. */
	private void persistChildrenSortOrders(DefaultMutableTreeNode parent) {
		if (parent == null) {
			return;
		}
		for (int i = 0; i < parent.getChildCount(); i++) {
			PlansTreeNode d = data(parent.getChildAt(i));
			if (d == null) {
				continue;
			}
			d.setSortOrder(i);
			if (d.isFolder() && d.getFolderId() != null) {
				Database.dml("update plan_folders set sort_order = " + i
						+ " where id_folder = " + d.getFolderId().intValue() + ";");
			} else if (d.isPlan() && d.getPlanId() != null) {
				Database.dml("update explain_plans set sort_order = " + i
						+ " where id_explain_plan = " + d.getPlanId().intValue() + ";");
			}
		}
	}

	private void persistEntireTreeSortOrders(DefaultMutableTreeNode parent) {
		persistChildrenSortOrders(parent);
		for (int i = 0; i < parent.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
			PlansTreeNode d = data(child);
			if (d != null && d.isFolder()) {
				persistEntireTreeSortOrders(child);
			}
		}
	}

	/**
	 * One-shot name sort of a single folder's immediate children (Manual mode).
	 * Updates persisted sort_order; does not change Settings mode.
	 */
	private void sortNowFolder(DefaultMutableTreeNode folderNode, boolean ascending) {
		if (folderNode == null) {
			return;
		}
		boolean savedManual = plansSortManual;
		boolean savedAsc = plansSortNameAscending;
		PlansSortGrouping savedGroup = plansSortGrouping;
		plansSortManual = false;
		plansSortNameAscending = ascending;
		plansSortGrouping = PlansSortGrouping.FOLDERS_FIRST;
		suppressTreeSelectionEvents = true;
		try {
			sortTreeNodeChildren(folderNode, false);
			ensurePlansTreeParentExpanded(folderNode);
		} finally {
			plansSortManual = savedManual;
			plansSortNameAscending = savedAsc;
			plansSortGrouping = savedGroup;
			suppressTreeSelectionEvents = false;
		}
		persistChildrenSortOrders(folderNode);
	}

	private void showPlansSettingsDialog() {
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog dialog = new JDialog(owner, "Settings", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JRadioButton modeByName = new JRadioButton("By name");
		JRadioButton modeManual = new JRadioButton("Manual");
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(modeByName);
		modeGroup.add(modeManual);
		if (plansSortManual) {
			modeManual.setSelected(true);
		} else {
			modeByName.setSelected(true);
		}

		JRadioButton nameAsc = new JRadioButton("A → Z");
		JRadioButton nameDesc = new JRadioButton("Z → A");
		ButtonGroup nameGroup = new ButtonGroup();
		nameGroup.add(nameAsc);
		nameGroup.add(nameDesc);
		if (plansSortNameAscending) {
			nameAsc.setSelected(true);
		} else {
			nameDesc.setSelected(true);
		}

		JRadioButton foldersFirst = new JRadioButton("Folders first, then plans");
		JRadioButton plansFirst = new JRadioButton("Plans first, then folders");
		JRadioButton mixed = new JRadioButton("Mix folders and plans by name");
		ButtonGroup groupingGroup = new ButtonGroup();
		groupingGroup.add(foldersFirst);
		groupingGroup.add(plansFirst);
		groupingGroup.add(mixed);
		switch (plansSortGrouping) {
		case PLANS_FIRST:
			plansFirst.setSelected(true);
			break;
		case MIXED:
			mixed.setSelected(true);
			break;
		case FOLDERS_FIRST:
		default:
			foldersFirst.setSelected(true);
			break;
		}

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

		JLabel modeLabel = new JLabel("Order");
		modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));
		modeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(modeLabel);
		body.add(Box.createVerticalStrut(6));
		modeByName.setAlignmentX(Component.LEFT_ALIGNMENT);
		modeManual.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(modeByName);
		body.add(modeManual);
		body.add(Box.createVerticalStrut(14));

		JLabel nameLabel = new JLabel("Name order");
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(nameLabel);
		body.add(Box.createVerticalStrut(6));
		nameAsc.setAlignmentX(Component.LEFT_ALIGNMENT);
		nameDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(nameAsc);
		body.add(nameDesc);
		body.add(Box.createVerticalStrut(14));

		JLabel groupLabel = new JLabel("Grouping");
		groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD));
		groupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(groupLabel);
		body.add(Box.createVerticalStrut(6));
		foldersFirst.setAlignmentX(Component.LEFT_ALIGNMENT);
		plansFirst.setAlignmentX(Component.LEFT_ALIGNMENT);
		mixed.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(foldersFirst);
		body.add(plansFirst);
		body.add(mixed);

		JLabel hint = new JLabel("<html>By name applies to the whole Plans tree.<br>"
				+ "Manual: drag to rearrange. Right-click a folder for Sort Now.</html>");
		hint.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(hint);

		body.add(Box.createVerticalStrut(16));
		JLabel warnLabel = new JLabel("Warning Indicators");
		warnLabel.setFont(warnLabel.getFont().deriveFont(Font.BOLD));
		warnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(warnLabel);
		body.add(Box.createVerticalStrut(6));

		JComboBox<String> minBasisCombo = new JComboBox<>(new String[] {
				OperatorWarnings.BASIS_LABEL_ACTUAL, OperatorWarnings.BASIS_LABEL_ESTIMATE });
		minBasisCombo.setSelectedItem(OperatorWarnings.basisLabel(Database.loadWarningMinBasis()));
		minBasisCombo.setMaximumSize(minBasisCombo.getPreferredSize());

		String minMetric = Database.loadWarningMinMetric();
		JComboBox<String> minMetricCombo = new JComboBox<>(new String[] {
				OperatorWarnings.METRIC_ROWS, OperatorWarnings.METRIC_MASS });
		minMetricCombo.setSelectedItem(minMetric);
		minMetricCombo.setMaximumSize(minMetricCombo.getPreferredSize());

		long minAmount = Math.round(Database.loadWarningMinActualRows());
		JSpinner minAmountSpinner = new JSpinner(
				new SpinnerNumberModel(Long.valueOf(minAmount), Long.valueOf(0L),
						Long.valueOf(1_000_000_000L), Long.valueOf(100L)));
		sizeWarningSpinnerColumns(minAmountSpinner, 4);

		JLabel minRowsUnitLabel = new JLabel("rows");
		JComboBox<String> minMassUnitCombo = new JComboBox<>(new String[] {
				OperatorWarnings.MASS_UNIT_KB, OperatorWarnings.MASS_UNIT_MB,
				OperatorWarnings.MASS_UNIT_GB });
		minMassUnitCombo.setSelectedItem(Database.loadWarningMinMassUnit());
		minMassUnitCombo.setMaximumSize(minMassUnitCombo.getPreferredSize());

		JPanel minRow = warningIndicatorsRow(
				minBasisCombo,
				minMetricCombo,
				new JLabel("minimum:"),
				minAmountSpinner,
				minRowsUnitLabel,
				minMassUnitCombo);
		body.add(minRow);
		body.add(Box.createVerticalStrut(10));

		Runnable syncMinUnitVisibility = () -> {
			boolean mass = OperatorWarnings.METRIC_MASS.equals(minMetricCombo.getSelectedItem());
			minRowsUnitLabel.setVisible(!mass);
			minMassUnitCombo.setVisible(mass);
			minRow.revalidate();
		};
		minMetricCombo.addActionListener(e -> syncMinUnitVisibility.run());
		syncMinUnitVisibility.run();

		String ratioMetric = Database.loadWarningRatioMetric();
		JComboBox<String> ratioMetricCombo = new JComboBox<>(new String[] {
				OperatorWarnings.METRIC_ROWS, OperatorWarnings.METRIC_MASS });
		ratioMetricCombo.setSelectedItem(ratioMetric);
		ratioMetricCombo.setMaximumSize(ratioMetricCombo.getPreferredSize());

		int ratioFactor = (int) Math.round(Database.loadWarningRowsFactor());
		JSpinner ratioFactorSpinner = new JSpinner(new SpinnerNumberModel(ratioFactor, 1, 1_000_000, 1));
		sizeWarningSpinnerColumns(ratioFactorSpinner, 4);

		JLabel ratioLabel = new JLabel("Actual vs. Estimated ratio");
		JLabel ratioAtLeastLabel = new JLabel("at least");
		JLabel ratioTimesLabel = new JLabel("times");
		JPanel ratioRow = warningIndicatorsRow(
				ratioLabel,
				ratioMetricCombo,
				ratioAtLeastLabel,
				ratioFactorSpinner,
				ratioTimesLabel);

		int filterPct = (int) Math.round(Database.loadWarningFilterPercent());
		JSpinner filterPercentSpinner = new JSpinner(new SpinnerNumberModel(filterPct, 0, 100, 1));
		sizeWarningSpinnerColumns(filterPercentSpinner, 4);
		JComboBox<String> filterMetricCombo = new JComboBox<>(new String[] {
				OperatorWarnings.METRIC_ROWS, OperatorWarnings.METRIC_MASS });
		filterMetricCombo.setSelectedItem(Database.loadWarningFilterMetric());
		filterMetricCombo.setMaximumSize(filterMetricCombo.getPreferredSize());

		JLabel filterLabel = new JLabel("Filter removes at least");
		JLabel filterOfLabel = new JLabel("% of");
		JPanel filterRow = warningIndicatorsRow(
				filterLabel,
				filterPercentSpinner,
				filterOfLabel,
				filterMetricCombo);

		int heapPct = (int) Math.round(Database.loadWarningHeapPercent());
		JSpinner heapPercentSpinner = new JSpinner(new SpinnerNumberModel(heapPct, 0, 100, 1));
		heapPercentSpinner.setMaximumSize(heapPercentSpinner.getPreferredSize());

		JLabel heapLabel = new JLabel("Heap fetch % at least");
		JPanel heapRow = warningIndicatorsRow(
				heapLabel,
				heapPercentSpinner);

		JPanel gatedBox = new JPanel();
		gatedBox.setLayout(new BoxLayout(gatedBox, BoxLayout.Y_AXIS));
		gatedBox.setOpaque(false);
		gatedBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		gatedBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 8, 0, 0),
				BorderFactory.createTitledBorder("When minimum is met")));
		gatedBox.add(ratioRow);
		gatedBox.add(Box.createVerticalStrut(8));
		gatedBox.add(filterRow);
		gatedBox.add(Box.createVerticalStrut(8));
		gatedBox.add(heapRow);
		body.add(gatedBox);

		Runnable syncGatedEnabled = () -> {
			boolean actualBasis = !OperatorWarnings.isEstimateBasis(
					OperatorWarnings.basisFromLabel(String.valueOf(minBasisCombo.getSelectedItem())));
			ratioLabel.setEnabled(actualBasis);
			ratioMetricCombo.setEnabled(actualBasis);
			ratioAtLeastLabel.setEnabled(actualBasis);
			ratioFactorSpinner.setEnabled(actualBasis);
			ratioTimesLabel.setEnabled(actualBasis);
			filterLabel.setEnabled(actualBasis);
			filterPercentSpinner.setEnabled(actualBasis);
			filterOfLabel.setEnabled(actualBasis);
			filterMetricCombo.setEnabled(actualBasis);
			heapLabel.setEnabled(actualBasis);
			heapPercentSpinner.setEnabled(actualBasis);
		};
		minBasisCombo.addActionListener(e -> syncGatedEnabled.run());
		syncGatedEnabled.run();

		Runnable syncEnabled = () -> {
			boolean byName = modeByName.isSelected();
			nameLabel.setEnabled(byName);
			nameAsc.setEnabled(byName);
			nameDesc.setEnabled(byName);
			groupLabel.setEnabled(byName);
			foldersFirst.setEnabled(byName);
			plansFirst.setEnabled(byName);
			mixed.setEnabled(byName);
		};
		modeByName.addActionListener(e -> syncEnabled.run());
		modeManual.addActionListener(e -> syncEnabled.run());
		syncEnabled.run();

		JButton applyBtn = new JButton("Apply");
		JButton saveCloseBtn = new JButton("Save and Close");
		JButton cancelBtn = new JButton("Cancel");
		Runnable applySettings = () -> {
			boolean wasManual = plansSortManual;
			plansSortManual = modeManual.isSelected();
			plansSortNameAscending = nameAsc.isSelected();
			if (plansFirst.isSelected()) {
				plansSortGrouping = PlansSortGrouping.PLANS_FIRST;
			} else if (mixed.isSelected()) {
				plansSortGrouping = PlansSortGrouping.MIXED;
			} else {
				plansSortGrouping = PlansSortGrouping.FOLDERS_FIRST;
			}
			savePlansSortPreferences();
			Database.saveWarningMinPreferences(
					OperatorWarnings.basisFromLabel(String.valueOf(minBasisCombo.getSelectedItem())),
					String.valueOf(minMetricCombo.getSelectedItem()),
					((Number) minAmountSpinner.getValue()).doubleValue(),
					String.valueOf(minMassUnitCombo.getSelectedItem()));
			Database.saveWarningRatioPreferences(
					String.valueOf(ratioMetricCombo.getSelectedItem()),
					((Number) ratioFactorSpinner.getValue()).doubleValue());
			Database.saveWarningFilterPreferences(
					((Number) filterPercentSpinner.getValue()).doubleValue(),
					String.valueOf(filterMetricCombo.getSelectedItem()));
			Database.saveWarningHeapPreferences(
					((Number) heapPercentSpinner.getValue()).doubleValue());
			refreshOpenPlansAfterWarningSettingsChange();
			if (plansSortManual) {
				if (!wasManual) {
					// Keep the current visual order as the new Manual order.
					persistEntireTreeSortOrders(planTreeRoot);
				}
			} else {
				sortEntirePlansTree(true);
				persistEntireTreeSortOrders(planTreeRoot);
			}
		};
		applyBtn.addActionListener(e -> applySettings.run());
		saveCloseBtn.addActionListener(e -> {
			applySettings.run();
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
		buttons.add(applyBtn);
		buttons.add(saveCloseBtn);
		buttons.add(cancelBtn);

		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(body, BorderLayout.CENTER);
		dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setMinimumSize(dialog.getPreferredSize());
		dialog.setLocationRelativeTo(owner != null ? owner : this);
		dialog.setVisible(true);
	}

	private void selectTreeNode(DefaultMutableTreeNode node) {
		if (node == null) {
			return;
		}
		if (node.getParent() instanceof DefaultMutableTreeNode) {
			planTree.expandPath(new TreePath(((DefaultMutableTreeNode) node.getParent()).getPath()));
		}
		TreePath path = new TreePath(node.getPath());
		planTree.setSelectionPath(path);
		planTree.scrollPathToVisible(path);
	}

	private DefaultMutableTreeNode findPlanNode(int planId) {
		Enumeration<?> e = planTreeRoot.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			PlansTreeNode d = data(node);
			if (d != null && d.isPlan() && d.getPlanId() != null && d.getPlanId().intValue() == planId) {
				return node;
			}
		}
		return null;
	}

	private DefaultMutableTreeNode findLastPlanNode() {
		DefaultMutableTreeNode last = null;
		Enumeration<?> e = planTreeRoot.preorderEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			PlansTreeNode d = data(node);
			if (d != null && d.isPlan()) {
				last = node;
			}
		}
		return last;
	}

	/** Number of plans + subfolders below a folder node. */
	private int countDescendantItems(DefaultMutableTreeNode node) {
		int count = 0;
		for (int i = 0; i < node.getChildCount(); i++) {
			count += 1 + countDescendantItems((DefaultMutableTreeNode) node.getChildAt(i));
		}
		return count;
	}

	/** Plan ids and folder ids in {@code node} and everything below it. */
	private void collectSubtreeIds(DefaultMutableTreeNode node, List<Integer> planIds, List<Integer> folderIds) {
		PlansTreeNode d = data(node);
		if (d != null) {
			if (d.isPlan() && d.getPlanId() != null) {
				planIds.add(d.getPlanId());
			} else if (d.isFolder() && d.getFolderId() != null) {
				folderIds.add(d.getFolderId());
			}
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			collectSubtreeIds((DefaultMutableTreeNode) node.getChildAt(i), planIds, folderIds);
		}
	}

	/** Load plan_folders into the tree (nested). Returns folder id -> tree node. */
	private Map<Integer, DefaultMutableTreeNode> loadPlanFoldersIntoTree() {
		Map<Integer, DefaultMutableTreeNode> nodes = new LinkedHashMap<>();
		Map<Integer, Integer> parents = new HashMap<>();
		JsonArray folderRows = Database.rows("select id_folder, folder_name, parent_id_folder, sort_order\n"
				+ "from plan_folders\n"
				+ "order by sort_order, lower(folder_name), id_folder;");
		if (folderRows == null) {
			return nodes;
		}
		for (int i = 0; i < folderRows.size(); i++) {
			JsonObject row = (JsonObject) folderRows.get(i);
			Integer id = toInteger(row.get("id_folder"));
			if (id == null) {
				continue;
			}
			Object name = row.get("folder_name");
			int sortOrder = toIntOrZero(row.get("sort_order"));
			nodes.put(id, new DefaultMutableTreeNode(PlansTreeNode.folder(
					id.intValue(), name == null ? "" : String.valueOf(name), sortOrder)));
			parents.put(id, toInteger(row.get("parent_id_folder")));
		}
		for (Map.Entry<Integer, DefaultMutableTreeNode> entry : nodes.entrySet()) {
			Integer id = entry.getKey();
			Integer parentId = parents.get(id);
			DefaultMutableTreeNode parentNode = planTreeRoot;
			if (parentId != null && !parentId.equals(id) && nodes.containsKey(parentId)
					&& !wouldCreateFolderCycle(id, parentId, parents, nodes.size())) {
				parentNode = nodes.get(parentId);
			}
			parentNode.add(entry.getValue());
		}
		return nodes;
	}

	private static boolean wouldCreateFolderCycle(Integer folderId, Integer parentId, Map<Integer, Integer> parents, int maxSteps) {
		Integer current = parentId;
		for (int steps = 0; current != null && steps <= maxSteps; steps++) {
			if (current.equals(folderId)) {
				return true;
			}
			current = parents.get(current);
		}
		return false;
	}

	private static Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return Integer.valueOf(((Number) value).intValue());
		}
		try {
			return Integer.valueOf(String.valueOf(value).trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static int toIntOrZero(Object value) {
		Integer n = toInteger(value);
		return n == null ? 0 : n.intValue();
	}

	/** Next sort_order for a new child under {@code parentFolderId} (null = root). */
	private int nextSortOrderInFolder(Integer parentFolderId, boolean forFolder) {
		String sql;
		if (forFolder) {
			sql = "select coalesce(max(sort_order), -1) + 1 as next_order from plan_folders where "
					+ folderScopeSql("parent_id_folder", parentFolderId) + ";";
		} else {
			sql = "select coalesce(max(sort_order), -1) + 1 as next_order from explain_plans where "
					+ folderScopeSql("folder_id", parentFolderId) + ";";
		}
		JsonObject row = Database.row(sql);
		return toIntOrZero(row == null ? null : row.get("next_order"));
	}

	private int readPlanSortOrder(int planId) {
		JsonObject row = Database.row("select sort_order from explain_plans where id_explain_plan = " + planId + ";");
		return toIntOrZero(row == null ? null : row.get("sort_order"));
	}

	private int readFolderSortOrder(int folderId) {
		JsonObject row = Database.row("select sort_order from plan_folders where id_folder = " + folderId + ";");
		return toIntOrZero(row == null ? null : row.get("sort_order"));
	}

	// ------------------------------------------------------------------
	// Selection persistence / lookup
	// ------------------------------------------------------------------

	/**
	 * Select the plan that was highlighted when PEPS last exited (by id, then by name).
	 * Falls back to the last plan in the tree (previous startup behavior) if missing.
	 */
	private void restoreSelectedPlanAfterLoad() {
		if (findLastPlanNode() == null) {
			return;
		}
		Integer savedId = Database.loadSelectedPlanId();
		if (savedId != null && selectPlanById(savedId.intValue())) {
			return;
		}
		String saved = Database.loadSelectedPlanName();
		if (saved != null && selectPlanByName(saved)) {
			return;
		}
		selectTreeNode(findLastPlanNode());
	}

	/** @return true if the plan was found and selected */
	public boolean selectPlanById(int planId) {
		if (planTree == null) {
			return false;
		}
		DefaultMutableTreeNode node = findPlanNode(planId);
		if (node == null) {
			return false;
		}
		selectTreeNode(node);
		return true;
	}

	/** @return true if {@code planName} was found and selected (first match in tree order) */
	public boolean selectPlanByName(String planName) {
		if (planTree == null || planName == null || planName.isBlank()) {
			return false;
		}
		Enumeration<?> e = planTreeRoot.preorderEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			PlansTreeNode d = data(node);
			if (d != null && d.isPlan() && planName.equals(d.getName())) {
				selectTreeNode(node);
				return true;
			}
		}
		return false;
	}

	/** Persist the current Plans-tree highlight (also called on window close). */
	public void saveSelectedPlanForExit() {
		PlansTreeNode primary = getPrimarySelectedPlan();
		if (primary == null) {
			return;
		}
		Database.saveSelectedPlanId(primary.getPlanId());
		Database.saveSelectedPlanName(primary.getName());
	}

	/** Persist Plan Diagram docking (Graph / Zoom / Properties / Sliders) on exit. */
	public void saveDiagramDockingLayoutForExit() {
		DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
		if (diagram != null) {
			diagram.saveLayout();
			return;
		}
		if (tabPaneToLink == null) {
			return;
		}
		for (int i = 0; i < tabPaneToLink.getTabCount(); i++) {
			DiagramDockingPanel other = findDiagramDockingPanel(tabPaneToLink.getComponentAt(i));
			if (other != null) {
				other.saveLayout();
				return;
			}
		}
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

	/** Refresh Add/Add Folder/Delete/Duplicate glyphs after a color-scheme change. */
	public void refreshPlanToolbarIcons() {
		refreshPlanToolbarButtonIcon(addPlanBtn);
		refreshPlanToolbarButtonIcon(addFolderBtn);
		refreshPlanToolbarButtonIcon(deletePlanBtn);
		refreshPlanToolbarButtonIcon(duplicatePlanBtn);
		refreshPlanToolbarButtonIcon(settingsBtn);
	}

	/** Keep Plans tree / scroll surfaces on the active scheme after View → theme changes. */
	public void syncThemeSurfaces() {
		Color listBg = UIManager.getColor("Tree.background");
		if (listBg == null) {
			listBg = UIManager.getColor("List.background");
		}
		Color listFg = UIManager.getColor("Tree.foreground");
		if (listFg == null) {
			listFg = UIManager.getColor("List.foreground");
		}
		Color bg = listBg != null ? UiTheme.uiColor(listBg) : UiTheme.uiColor(UiTheme.surfaceColor());
		Color fg = listFg != null ? UiTheme.uiColor(listFg)
				: UiTheme.uiColor(UiTheme.isDark() ? new Color(0xE8, 0xEA, 0xED) : new Color(0x11, 0x18, 0x27));
		setOpaque(true);
		setBackground(UiTheme.uiColor(UiTheme.surfaceColor()));
		if (planTree != null) {
			// Theme changes reinstall the default TreeUI — restore root-plan alignment + folder icons.
			installPlansTreeChrome();
			planTree.setOpaque(true);
			planTree.setBackground(bg);
			planTree.setForeground(fg);
			if (planTree.getCellRenderer() instanceof DefaultTreeCellRenderer) {
				DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) planTree.getCellRenderer();
				renderer.setBackgroundNonSelectionColor(bg);
				renderer.setTextNonSelectionColor(fg);
			}
		}
		if (planListScrollPane != null) {
			planListScrollPane.setOpaque(true);
			planListScrollPane.setBackground(bg);
			if (planListScrollPane.getViewport() != null) {
				planListScrollPane.getViewport().setOpaque(true);
				planListScrollPane.getViewport().setBackground(bg);
			}
		}
		revalidate();
		repaint();
	}

	/** Tree UI (root plans flush-left, folder guides) + cell renderer (folder icons). */
	private void installPlansTreeChrome() {
		if (planTree == null) {
			return;
		}
		// FlatTreeUI / BasicTreeUI read Tree.paintLines into a private field during installDefaults.
		Object previousPaintLines = UIManager.get("Tree.paintLines");
		UIManager.put("Tree.paintLines", Boolean.TRUE);
		try {
			if (UIManager.getLookAndFeel() instanceof FlatLaf) {
				planTree.setUI(new PlansFlatTreeUI());
			} else {
				planTree.setUI(new PlansBasicTreeUI());
			}
		} finally {
			UIManager.put("Tree.paintLines", previousPaintLines);
		}
		planTree.setCellRenderer(new PlansTreeCellRenderer());
	}

	/**
	 * True when {@code value} is a plan whose parent is the invisible tree root.
	 * Prefer this over {@code getPathForRow(row)} — layout often calls getRowX with row == -1.
	 */
	private boolean isRootLevelPlanNode(Object value) {
		if (!(value instanceof DefaultMutableTreeNode)) {
			return false;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getParent() != planTreeRoot) {
			return false;
		}
		PlansTreeNode d = data(node);
		return d != null && d.isPlan();
	}

	/** Pull root-level plans left by one indent so they align like the old flat list. */
	private int adjustRootPlanRowX(int baseX, Object value, int leftIndent, int rightIndent) {
		if (!isRootLevelPlanNode(value)) {
			return baseX;
		}
		return Math.max(0, baseX - leftIndent - rightIndent);
	}

	private int adjustRootPlanRowXForRow(int baseX, int row, int leftIndent, int rightIndent) {
		if (row < 0 || planTree == null) {
			return baseX;
		}
		TreePath path = planTree.getPathForRow(row);
		if (path == null) {
			return baseX;
		}
		return adjustRootPlanRowX(baseX, path.getLastPathComponent(), leftIndent, rightIndent);
	}

	/** Low-contrast guide color for folder nesting lines / dots. */
	private static Color plansTreeGuideColor() {
		return UiTheme.isDark() ? new Color(0x9A, 0xA0, 0xA6, 110) : new Color(0x6B, 0x72, 0x80, 120);
	}

	/**
	 * X of the expand/collapse caret for a row (BasicTreeUI / FlatLaf paintExpandControl).
	 * Default tree "leg" lines sit one indent to the right of this — under the folder icon.
	 */
	private static int plansTreeCaretCenterX(Rectangle rowBounds, int rightChildIndent) {
		return rowBounds.x - rightChildIndent + 1;
	}

	/** Bullet on the caret-aligned vertical guide at a plan row (folders get the stem only). */
	private void paintPlansTreeChildBullet(Graphics g, int rightChildIndent, Rectangle bounds, TreePath path) {
		if (path == null || path.getPathCount() <= 2) {
			return;
		}
		PlansTreeNode nodeData = data(path.getLastPathComponent());
		if (nodeData == null || !nodeData.isPlan()) {
			return; // no bullets next to folders
		}
		TreePath parent = path.getParentPath();
		if (parent == null) {
			return;
		}
		Rectangle parentBounds = planTree.getPathBounds(parent);
		if (parentBounds == null) {
			return;
		}
		int lineX = plansTreeCaretCenterX(parentBounds, rightChildIndent);
		int lineY = bounds.y + bounds.height / 2;
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(plansTreeGuideColor());
			int r = 2;
			g2.fillOval(lineX - r, lineY - r, r * 2 + 1, r * 2 + 1);
		} finally {
			g2.dispose();
		}
	}

	/**
	 * Vertical guide from just below the parent's caret down to the last child's mid-row.
	 * Skips the invisible root so top-level rows stay clean.
	 */
	private void paintPlansTreeVerticalGuide(Graphics g, int rightChildIndent, Rectangle clipBounds, TreePath path) {
		if (path == null || path.getPathCount() <= 1) {
			return;
		}
		Rectangle parentBounds = planTree.getPathBounds(path);
		if (parentBounds == null) {
			return;
		}
		Object parentNode = path.getLastPathComponent();
		int childCount = planTree.getModel().getChildCount(parentNode);
		if (childCount <= 0) {
			return;
		}
		TreePath lastChildPath = path.pathByAddingChild(planTree.getModel().getChild(parentNode, childCount - 1));
		Rectangle lastChildBounds = planTree.getPathBounds(lastChildPath);
		if (lastChildBounds == null) {
			return;
		}

		int lineX = plansTreeCaretCenterX(parentBounds, rightChildIndent);
		// Start just below the caret (row midline + ~half a typical chevron).
		int top = parentBounds.y + parentBounds.height / 2 + 5;
		int bottom = lastChildBounds.y + lastChildBounds.height / 2;
		if (top > bottom) {
			return;
		}
		if (lineX < clipBounds.x || lineX > clipBounds.x + clipBounds.width) {
			return;
		}
		top = Math.max(top, clipBounds.y);
		bottom = Math.min(bottom, clipBounds.y + clipBounds.height);
		if (top > bottom) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(plansTreeGuideColor());
			g2.setStroke(new BasicStroke(1f));
			g2.drawLine(lineX, top, lineX, bottom);
		} finally {
			g2.dispose();
		}
	}

	private final class PlansBasicTreeUI extends BasicTreeUI {
		@Override
		protected void installDefaults() {
			Object previousPaintLines = UIManager.get("Tree.paintLines");
			UIManager.put("Tree.paintLines", Boolean.TRUE);
			try {
				super.installDefaults();
			} finally {
				UIManager.put("Tree.paintLines", previousPaintLines);
			}
			setHashColor(plansTreeGuideColor());
		}

		/**
		 * Layout often measures nodes with row == -1 (before they have a visible row). Adjusting
		 * only via getPathForRow left some root plans indented and others flush — use the node
		 * value here so every root plan gets the same x.
		 */
		@Override
		protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
			return new NodeDimensionsHandler() {
				@Override
				public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded,
						Rectangle size) {
					Rectangle dims = super.getNodeDimensions(value, row, depth, expanded, size);
					if (dims != null) {
						dims.x = adjustRootPlanRowX(dims.x, value, getLeftChildIndent(), getRightChildIndent());
					}
					return dims;
				}
			};
		}

		@Override
		protected int getRowX(int row, int depth) {
			return adjustRootPlanRowXForRow(super.getRowX(row, depth), row, getLeftChildIndent(),
					getRightChildIndent());
		}

		@Override
		protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
				Rectangle bounds, TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded,
				boolean isLeaf) {
			paintPlansTreeChildBullet(g, getRightChildIndent(), bounds, path);
		}

		@Override
		protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets, TreePath path) {
			paintPlansTreeVerticalGuide(g, getRightChildIndent(), clipBounds, path);
		}
	}

	private final class PlansFlatTreeUI extends FlatTreeUI {
		@Override
		protected void installDefaults() {
			Object previousPaintLines = UIManager.get("Tree.paintLines");
			UIManager.put("Tree.paintLines", Boolean.TRUE);
			try {
				super.installDefaults();
			} finally {
				UIManager.put("Tree.paintLines", previousPaintLines);
			}
			setHashColor(plansTreeGuideColor());
		}

		@Override
		protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
			return new NodeDimensionsHandler() {
				@Override
				public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded,
						Rectangle size) {
					Rectangle dims = super.getNodeDimensions(value, row, depth, expanded, size);
					if (dims != null) {
						dims.x = adjustRootPlanRowX(dims.x, value, getLeftChildIndent(), getRightChildIndent());
					}
					return dims;
				}
			};
		}

		@Override
		protected int getRowX(int row, int depth) {
			return adjustRootPlanRowXForRow(super.getRowX(row, depth), row, getLeftChildIndent(),
					getRightChildIndent());
		}

		@Override
		protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
				Rectangle bounds, TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded,
				boolean isLeaf) {
			paintPlansTreeChildBullet(g, getRightChildIndent(), bounds, path);
		}

		@Override
		protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets, TreePath path) {
			paintPlansTreeVerticalGuide(g, getRightChildIndent(), clipBounds, path);
		}
	}

	/** Folder nodes show a folder glyph; plan nodes have no leading icon. */
	private final class PlansTreeCellRenderer extends DefaultTreeCellRenderer {
		private final ImageIcon folderIcon = createFolderTreeIcon();

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
				boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			PlansTreeNode d = data(value);
			if (d != null && d.isFolder()) {
				setIcon(folderIcon);
				setDisabledIcon(folderIcon);
			} else {
				setIcon(null);
				setDisabledIcon(null);
				setLeafIcon(null);
			}
			setOpenIcon(folderIcon);
			setClosedIcon(folderIcon);
			return this;
		}
	}

	private static ImageIcon createFolderTreeIcon() {
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color fill = UiTheme.isDark() ? new Color(0xFB, 0xBF, 0x24, 180) : new Color(0xF5, 0xC5, 0x42);
		Color stroke = UiTheme.isDark() ? new Color(0xF5, 0xD0, 0x67) : new Color(0xB4, 0x53, 0x09);
		g.setColor(fill);
		g.fillRoundRect(1, 5, 12, 8, 2, 2);
		g.fillRoundRect(1, 3, 5, 3, 1, 1);
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(stroke);
		g.drawRoundRect(1, 5, 12, 8, 2, 2);
		g.drawLine(1, 5, 6, 5);
		g.dispose();
		return new ImageIcon(image);
	}

	/**
	 * Dropdown entries for Plan Source folder chooser. {@code folderId == null} is the root.
	 */
	public static final class FolderChoice {
		private final Integer folderId;
		private final String label;

		public FolderChoice(Integer folderId, String label) {
			this.folderId = folderId;
			this.label = label == null ? "" : label;
		}

		public Integer getFolderId() {
			return folderId;
		}

		public String getLabel() {
			return label;
		}

		@Override
		public String toString() {
			return label;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof FolderChoice)) {
				return false;
			}
			return Objects.equals(folderId, ((FolderChoice) obj).folderId);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(folderId);
		}
	}

	/** Root first, then every folder with a "Parent / Child" path label. */
	public List<FolderChoice> listFolderChoices() {
		List<FolderChoice> out = new ArrayList<>();
		out.add(new FolderChoice(null, "(root)"));
		Enumeration<?> e = planTreeRoot.preorderEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			PlansTreeNode d = data(node);
			if (d == null || !d.isFolder()) {
				continue;
			}
			out.add(new FolderChoice(d.getFolderId(), folderPathLabel(node)));
		}
		return out;
	}

	private String folderPathLabel(DefaultMutableTreeNode folderNode) {
		List<String> parts = new ArrayList<>();
		for (TreeNode n = folderNode; n != null && n != planTreeRoot; n = n.getParent()) {
			PlansTreeNode d = data(n);
			if (d != null && d.isFolder()) {
				parts.add(0, d.getName());
			}
		}
		if (parts.isEmpty()) {
			return "Folder";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				sb.append(" / ");
			}
			sb.append(parts.get(i));
		}
		return sb.toString();
	}

	/** Containing folder of a plan (null = root), or null if the plan is unknown. */
	public Integer getPlanFolderId(int planId) {
		DefaultMutableTreeNode node = findPlanNode(planId);
		PlansTreeNode d = data(node);
		return d == null ? null : d.getFolderId();
	}

	/**
	 * Move a plan into {@code newFolderId} (null = root). Renames if the name collides in the
	 * destination. Returns the final plan name (may differ from before).
	 */
	public String movePlanToFolder(int planId, Integer newFolderId) {
		return movePlanToFolder(planId, newFolderId, true);
	}

	private String movePlanToFolder(int planId, Integer newFolderId, boolean selectAfter) {
		DefaultMutableTreeNode node = findPlanNode(planId);
		PlansTreeNode d = data(node);
		if (d == null || !d.isPlan()) {
			return null;
		}
		Integer oldFolderId = d.getFolderId();
		if (Objects.equals(oldFolderId, newFolderId)) {
			return d.getName();
		}
		DefaultMutableTreeNode newParent = newFolderId == null ? planTreeRoot : findFolderNode(newFolderId.intValue());
		if (newParent == null) {
			return d.getName();
		}

		String planName = d.getName();
		List<String> namesInTarget = planNamesInFolder(newFolderId);
		if (namesInTarget.contains(planName)) {
			planName = nextCopyName(planName, namesInTarget);
		}

		Database.dml("update explain_plans set folder_id = " + sqlFolderIdLiteral(newFolderId)
				+ ", plan_name = " + sqlQuote(planName)
				+ " where id_explain_plan = " + planId + ";");

		String previousName = d.getName();
		d.setContainingFolderId(newFolderId);
		if (!planName.equals(previousName)) {
			applyPlanRename(planId, planName);
			PlanEvent pEvent = plansMapper.get(Integer.valueOf(planId));
			if (pEvent != null && pEvent.getPlanSourcePanel() != null) {
				pEvent.getPlanSourcePanel().setPlanSourceFromQueryPlanNameField(planName);
			}
		}

		reparentTreeNode(node, newParent, selectAfter);
		if (selectAfter) {
			refreshOpenPlanFolderChoosers();
		}
		return planName;
	}

	/**
	 * Move a folder under {@code newParentFolderId} (null = root). Renames on collision.
	 * Rejects moves into itself or a descendant.
	 */
	public String moveFolderToParent(int folderId, Integer newParentFolderId) {
		return moveFolderToParent(folderId, newParentFolderId, true);
	}

	private String moveFolderToParent(int folderId, Integer newParentFolderId, boolean selectAfter) {
		DefaultMutableTreeNode node = findFolderNode(folderId);
		PlansTreeNode d = data(node);
		if (d == null || !d.isFolder()) {
			return null;
		}
		DefaultMutableTreeNode newParent = newParentFolderId == null ? planTreeRoot
				: findFolderNode(newParentFolderId.intValue());
		if (newParent == null) {
			return d.getName();
		}
		if (node == newParent || node.isNodeDescendant(newParent)) {
			return d.getName(); // cycle
		}
		Integer oldParentId = folderIdOf((DefaultMutableTreeNode) node.getParent());
		if (Objects.equals(oldParentId, newParentFolderId)) {
			return d.getName();
		}

		String folderName = d.getName();
		List<String> namesInTarget = folderNamesInParent(newParentFolderId);
		if (namesInTarget.contains(folderName)) {
			folderName = nextCopyName(folderName, namesInTarget);
		}

		Database.dml("update plan_folders set parent_id_folder = " + sqlFolderIdLiteral(newParentFolderId)
				+ ", folder_name = " + sqlQuote(folderName)
				+ " where id_folder = " + folderId + ";");

		if (!folderName.equals(d.getName())) {
			d.setName(folderName);
		}
		reparentTreeNode(node, newParent, selectAfter);
		if (selectAfter) {
			refreshOpenPlanFolderChoosers();
		}
		return folderName;
	}

	private void reparentTreeNode(DefaultMutableTreeNode node, DefaultMutableTreeNode newParent, boolean selectAfter) {
		suppressTreeSelectionEvents = true;
		try {
			planTreeModel.removeNodeFromParent(node);
			insertTreeNode(newParent, node);
			if (selectAfter) {
				TreePath path = new TreePath(node.getPath());
				planTree.setSelectionPath(path);
				planTree.scrollPathToVisible(path);
			}
		} finally {
			suppressTreeSelectionEvents = false;
		}
		planTreeModel.nodeChanged(node);
	}

	private void refreshOpenPlanFolderChoosers() {
		for (PlanEvent event : plansMapper.values()) {
			if (event != null && event.getPlanSourcePanel() != null) {
				event.getPlanSourcePanel().refreshFolderChooser();
			}
		}
	}

	private DefaultMutableTreeNode findFolderNode(int folderId) {
		Enumeration<?> e = planTreeRoot.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			PlansTreeNode d = data(node);
			if (d != null && d.isFolder() && d.getFolderId() != null && d.getFolderId().intValue() == folderId) {
				return node;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Drag and drop
	// ------------------------------------------------------------------

	private static final DataFlavor PLANS_TREE_NODES_FLAVOR =
			new DataFlavor(PlansTreeDragPayload.class, "PEPS Plans tree nodes");

	/** Ids of top-level dragged plans/folders (children of a dragged folder are implied). */
	private static final class PlansTreeDragPayload {
		final List<Integer> planIds = new ArrayList<>();
		final List<Integer> folderIds = new ArrayList<>();
	}

	private void installPlansTreeDragAndDrop() {
		if (planTree == null || PLANS_TREE_NODES_FLAVOR == null) {
			return;
		}
		planTree.setDragEnabled(true);
		planTree.setDropMode(DropMode.ON_OR_INSERT);
		planTree.setTransferHandler(new PlansTreeTransferHandler());
	}

	/**
	 * Expand/collapse a folder when clicking its row (icon/name). Clicks on the caret
	 * stay left of the row bounds and are handled by the tree UI alone — so we don't
	 * double-toggle.
	 */
	private void installPlansTreeFolderRowToggle() {
		if (planTree == null) {
			return;
		}
		planTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) {
					return;
				}
				if (e.isControlDown() || e.isShiftDown() || e.isAltDown() || e.isMetaDown()) {
					return;
				}
				TreePath path = planTree.getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				}
				PlansTreeNode d = data(path.getLastPathComponent());
				if (d == null || !d.isFolder()) {
					return;
				}
				Rectangle bounds = planTree.getPathBounds(path);
				// Caret sits to the left of the row bounds; only toggle for clicks on the row itself.
				if (bounds == null || !bounds.contains(e.getPoint())) {
					return;
				}
				if (planTree.isExpanded(path)) {
					planTree.collapsePath(path);
				} else {
					planTree.expandPath(path);
				}
			}
		});
	}

	/** Plans tree context menu: toolbar actions + Sort Now (Manual mode). */
	private void installPlansTreeContextMenu() {
		if (planTree == null) {
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem addPlanItem = new JMenuItem("Add Plan");
		JMenuItem addFolderItem = new JMenuItem("Add Folder");
		JMenuItem deleteItem = new JMenuItem("Delete Selected");
		JMenuItem duplicateItem = new JMenuItem("Duplicate Plan(s)");
		JMenuItem compareItem = new JMenuItem("Compare Plans");
		JMenuItem settingsItem = new JMenuItem("Settings");
		JMenuItem sortAsc = new JMenuItem("Sort Now: A → Z");
		JMenuItem sortDesc = new JMenuItem("Sort Now: Z → A");

		addPlanItem.addActionListener(e -> addPlan());
		addFolderItem.addActionListener(e -> addFolder());
		deleteItem.addActionListener(e -> deleteSelectedItems());
		duplicateItem.addActionListener(e -> duplicateSelectedPlans());
		compareItem.addActionListener(e -> showComparePlansTab());
		settingsItem.addActionListener(e -> showPlansSettingsDialog());

		menu.add(addPlanItem);
		menu.add(addFolderItem);
		menu.add(deleteItem);
		menu.add(duplicateItem);
		menu.add(compareItem);
		menu.addSeparator();
		menu.add(settingsItem);
		menu.addSeparator();
		menu.add(sortAsc);
		menu.add(sortDesc);

		MouseAdapter popupListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// Drag-enabled JTree often defers left-click selection; right-click must
				// highlight immediately so the menu matches the row under the pointer.
				if (SwingUtilities.isRightMouseButton(e)) {
					highlightPathForContextMenu(treePathAtPopup(e));
				}
				maybeShow(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShow(e);
			}

			private void maybeShow(MouseEvent e) {
				if (!e.isPopupTrigger()) {
					return;
				}
				TreePath pathUnderPointer = treePathAtPopup(e);
				contextMenuAnchorPath = pathUnderPointer;
				highlightPathForContextMenu(pathUnderPointer);
				updateAddButtonsEnabled();

				int selectionCount = getSelectedTreeNodes().size();
				boolean allowAdd = selectionCount <= 1;
				addPlanItem.setEnabled(allowAdd);
				addFolderItem.setEnabled(allowAdd);
				deleteItem.setEnabled(selectionCount > 0);
				duplicateItem.setEnabled(!getSelectedPlanNodes().isEmpty());
				compareItem.setEnabled(canComparePlansFromContext(pathUnderPointer));
				settingsItem.setEnabled(true);

				DefaultMutableTreeNode sortFolder = folderNodeForSortNow(pathUnderPointer);
				boolean showSortNow = plansSortManual;
				sortAsc.setVisible(showSortNow);
				sortDesc.setVisible(showSortNow);
				// Hide the separator above Sort Now when those items are hidden.
				int sortSepIndex = menu.getComponentIndex(sortAsc) - 1;
				if (sortSepIndex >= 0 && menu.getComponent(sortSepIndex) instanceof JPopupMenu.Separator) {
					menu.getComponent(sortSepIndex).setVisible(showSortNow);
				}
				if (showSortNow) {
					String folderLabel = sortNowFolderLabel(sortFolder);
					sortAsc.setText("Sort Now: A → Z (" + folderLabel + ")");
					sortDesc.setText("Sort Now: Z → A (" + folderLabel + ")");
					for (ActionListener old : sortAsc.getActionListeners()) {
						sortAsc.removeActionListener(old);
					}
					for (ActionListener old : sortDesc.getActionListeners()) {
						sortDesc.removeActionListener(old);
					}
					sortAsc.addActionListener(ae -> sortNowFolder(sortFolder, true));
					sortDesc.addActionListener(ae -> sortNowFolder(sortFolder, false));
				}

				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		};
		menu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				// anchor set in maybeShow
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// After item actions run; clear so toolbar Add does not reuse the click.
				SwingUtilities.invokeLater(() -> contextMenuAnchorPath = null);
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				contextMenuAnchorPath = null;
			}
		});
		planTree.addMouseListener(popupListener);
		if (planListScrollPane != null) {
			planListScrollPane.getViewport().addMouseListener(popupListener);
			planListScrollPane.addMouseListener(popupListener);
		}
	}

	/**
	 * Select the plan/folder under a right-click (or clear when clicking empty space)
	 * so toolbar/context actions apply to that row. If the row is already part of a
	 * multi-selection, keep the selection (needed for Compare Plans).
	 */
	private void highlightPathForContextMenu(TreePath path) {
		if (planTree == null) {
			return;
		}
		if (path == null) {
			planTree.clearSelection();
		} else if (planTree.isPathSelected(path) && planTree.getSelectionCount() > 1) {
			planTree.setLeadSelectionPath(path);
			planTree.scrollPathToVisible(path);
		} else {
			planTree.setSelectionPath(path);
			planTree.setLeadSelectionPath(path);
			planTree.setAnchorSelectionPath(path);
			planTree.scrollPathToVisible(path);
		}
		planTree.repaint();
	}

	/** True when Compare Plans can run from the current selection / right-click target. */
	private boolean canComparePlansFromContext(TreePath pathUnderPointer) {
		if (!getSelectedPlanNodes().isEmpty()) {
			return true;
		}
		DefaultMutableTreeNode folder = folderNodeAtPath(pathUnderPointer);
		if (folder != null) {
			return countPlansUnder(folder) > 0;
		}
		for (DefaultMutableTreeNode node : getSelectedTreeNodes()) {
			PlansTreeNode d = data(node);
			if (d != null && d.isFolder() && countPlansUnder(node) > 0) {
				return true;
			}
		}
		return false;
	}

	private DefaultMutableTreeNode folderNodeAtPath(TreePath path) {
		if (path == null) {
			return null;
		}
		Object last = path.getLastPathComponent();
		if (!(last instanceof DefaultMutableTreeNode)) {
			return null;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
		PlansTreeNode d = data(node);
		return d != null && d.isFolder() ? node : null;
	}

	/** Plans in {@code folder} and all nested folders. */
	private int countPlansUnder(DefaultMutableTreeNode folder) {
		List<TreePath> paths = new ArrayList<>();
		collectDescendantPlanPaths(folder, paths);
		return paths.size();
	}

	private void collectDescendantPlanPaths(DefaultMutableTreeNode node, List<TreePath> out) {
		if (node == null) {
			return;
		}
		PlansTreeNode d = data(node);
		if (d != null && d.isPlan()) {
			out.add(new TreePath(node.getPath()));
			return;
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			collectDescendantPlanPaths((DefaultMutableTreeNode) node.getChildAt(i), out);
		}
	}

	/**
	 * If the context target / selection is a folder, select every plan in that folder
	 * and its subfolders so Compare Plans can run.
	 */
	private void selectPlansForCompareFromContext() {
		List<DefaultMutableTreeNode> folders = new ArrayList<>();
		DefaultMutableTreeNode anchorFolder = folderNodeAtPath(contextMenuAnchorPath);
		if (anchorFolder != null) {
			folders.add(anchorFolder);
		} else {
			for (DefaultMutableTreeNode node : getSelectedTreeNodes()) {
				PlansTreeNode d = data(node);
				if (d != null && d.isFolder()) {
					folders.add(node);
				}
			}
		}
		if (folders.isEmpty()) {
			return;
		}
		List<TreePath> planPaths = new ArrayList<>();
		for (DefaultMutableTreeNode folder : folders) {
			collectDescendantPlanPaths(folder, planPaths);
		}
		if (planPaths.isEmpty()) {
			return;
		}
		planTree.setSelectionPaths(planPaths.toArray(new TreePath[0]));
		planTree.scrollPathToVisible(planPaths.get(0));
	}

	/** Switch to the Compare Plans tab (refreshes via the tab change listener). */
	private void showComparePlansTab() {
		if (tabPaneToLink == null) {
			return;
		}
		selectPlansForCompareFromContext();
		if (getSelectedPlanNodes().isEmpty()) {
			return;
		}
		if (tabPaneToLink.getTabCount() <= 1) {
			// Selection listener builds tabs; ensure Compare Plans exists.
			onPlanTreeSelectionChanged();
		}
		if (tabPaneToLink.getTabCount() > 1) {
			tabPaneToLink.setSelectedIndex(1);
		}
	}

	/** Tree path under a popup click (tree or scroll-pane coordinates). */
	private TreePath treePathAtPopup(MouseEvent e) {
		if (planTree == null) {
			return null;
		}
		Point treePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), planTree);
		TreePath exact = planTree.getPathForLocation(treePoint.x, treePoint.y);
		if (exact != null) {
			return exact;
		}
		// Clicks on indent/guides miss getPathForLocation — still hit the row by Y.
		int row = planTree.getClosestRowForLocation(treePoint.x, treePoint.y);
		if (row < 0) {
			return null;
		}
		Rectangle bounds = planTree.getRowBounds(row);
		if (bounds == null || treePoint.y < bounds.y || treePoint.y >= bounds.y + bounds.height) {
			return null;
		}
		return planTree.getPathForRow(row);
	}

	/** Display name for Sort Now menu items (root → {@code Root folder}). */
	private String sortNowFolderLabel(DefaultMutableTreeNode folderNode) {
		if (folderNode == null || folderNode == planTreeRoot) {
			return "Root folder";
		}
		PlansTreeNode d = data(folderNode);
		if (d == null || d.getName() == null || d.getName().isBlank()) {
			return "Root folder";
		}
		return d.getName();
	}

	/** Folder whose immediate children Sort Now should rearrange. */
	private DefaultMutableTreeNode folderNodeForSortNow(TreePath path) {
		if (path == null) {
			return planTreeRoot;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		PlansTreeNode d = data(node);
		if (d == null) {
			return planTreeRoot;
		}
		if (d.isFolder()) {
			return node;
		}
		TreeNode parent = node.getParent();
		return parent instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) parent : planTreeRoot;
	}

	/**
	 * True when the drop is ON a plan node (not an insert gap, not ON a folder).
	 * Plans are leaves — dropping ON one must not reorder/append in the parent.
	 */
	private boolean isPlansTreeDropOntoPlan(JTree.DropLocation loc) {
		if (loc == null || loc.getChildIndex() >= 0) {
			return false;
		}
		TreePath path = loc.getPath();
		if (path == null) {
			return false;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		if (node == null || node == planTreeRoot) {
			return false;
		}
		PlansTreeNode d = data(node);
		return d != null && d.isPlan();
	}

	/** Destination folder for a drop (null = root). Valid for INSERT gaps and ON folder only. */
	private Integer resolveDropTargetFolderId(JTree.DropLocation loc) {
		if (loc == null) {
			return null;
		}
		TreePath path = loc.getPath();
		if (path == null) {
			return null;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		if (node == null || node == planTreeRoot) {
			return null;
		}
		PlansTreeNode d = data(node);
		if (d == null) {
			return null;
		}
		int childIndex = loc.getChildIndex();
		if (childIndex >= 0) {
			// Inserting as a child of this path node (folder) or relative to a plan under its parent folder
			return d.getFolderId();
		}
		// Dropped ON the node — only folders are valid targets (plans rejected by isPlansTreeDropOntoPlan)
		if (d.isFolder()) {
			return d.getFolderId();
		}
		return d.getFolderId();
	}

	private boolean canDropPayloadOnFolder(PlansTreeDragPayload payload, Integer targetFolderId) {
		if (payload == null) {
			return false;
		}
		if (payload.planIds.isEmpty() && payload.folderIds.isEmpty()) {
			return false;
		}
		DefaultMutableTreeNode targetNode = targetFolderId == null ? planTreeRoot
				: findFolderNode(targetFolderId.intValue());
		if (targetNode == null) {
			return false;
		}
		for (Integer folderId : payload.folderIds) {
			DefaultMutableTreeNode dragged = findFolderNode(folderId.intValue());
			if (dragged == null) {
				return false;
			}
			// Cannot drop a folder into itself or into one of its descendants
			if (dragged == targetNode || dragged.isNodeDescendant(targetNode)) {
				return false;
			}
		}
		return true;
	}

	private boolean importPlansTreePayload(PlansTreeDragPayload payload, Integer targetFolderId, int childIndex) {
		if (!canDropPayloadOnFolder(payload, targetFolderId)) {
			return false;
		}
		DefaultMutableTreeNode targetParent = targetFolderId == null ? planTreeRoot
				: findFolderNode(targetFolderId.intValue());
		if (targetParent == null) {
			return false;
		}

		List<DefaultMutableTreeNode> nodes = new ArrayList<>();
		for (Integer folderId : payload.folderIds) {
			DefaultMutableTreeNode node = findFolderNode(folderId.intValue());
			if (node != null) {
				nodes.add(node);
			}
		}
		for (Integer planId : payload.planIds) {
			DefaultMutableTreeNode node = findPlanNode(planId.intValue());
			if (node != null) {
				nodes.add(node);
			}
		}
		if (nodes.isEmpty()) {
			return false;
		}

		boolean allAlreadyInTarget = true;
		for (DefaultMutableTreeNode node : nodes) {
			Integer currentParent = folderIdOf((DefaultMutableTreeNode) node.getParent());
			PlansTreeNode d = data(node);
			if (d != null && d.isPlan()) {
				currentParent = d.getFolderId();
			}
			if (!Objects.equals(currentParent, targetFolderId)) {
				allAlreadyInTarget = false;
				break;
			}
		}

		// By name: only cross-folder moves; sibling position is name-sorted.
		if (allAlreadyInTarget && !plansSortManual) {
			return false;
		}

		if (allAlreadyInTarget && plansSortManual) {
			return reorderNodesInParent(nodes, targetParent, childIndex);
		}

		// Cross-folder (or mixed): move membership first, then place at index when Manual.
		List<DefaultMutableTreeNode> moved = new ArrayList<>();
		Set<DefaultMutableTreeNode> affectedParents = new HashSet<>();
		affectedParents.add(targetParent);
		for (DefaultMutableTreeNode node : nodes) {
			DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
			if (oldParent != null) {
				affectedParents.add(oldParent);
			}
			PlansTreeNode d = data(node);
			if (d == null) {
				continue;
			}
			if (d.isFolder() && d.getFolderId() != null) {
				Integer currentParent = folderIdOf(oldParent);
				if (!Objects.equals(currentParent, targetFolderId)) {
					moveFolderToParent(d.getFolderId().intValue(), targetFolderId, false);
				}
				node = findFolderNode(d.getFolderId().intValue());
			} else if (d.isPlan() && d.getPlanId() != null) {
				if (!Objects.equals(d.getFolderId(), targetFolderId)) {
					movePlanToFolder(d.getPlanId().intValue(), targetFolderId, false);
				}
				node = findPlanNode(d.getPlanId().intValue());
			}
			if (node != null) {
				moved.add(node);
			}
		}
		if (moved.isEmpty()) {
			return false;
		}
		if (plansSortManual) {
			reorderNodesInParent(moved, targetParent, childIndex);
			for (DefaultMutableTreeNode parent : affectedParents) {
				if (parent != targetParent) {
					persistChildrenSortOrders(parent);
				}
			}
		}
		suppressTreeSelectionEvents = true;
		try {
			planTree.clearSelection();
			TreePath[] paths = new TreePath[moved.size()];
			for (int i = 0; i < moved.size(); i++) {
				paths[i] = new TreePath(moved.get(i).getPath());
			}
			planTree.setSelectionPaths(paths);
			planTree.scrollPathToVisible(paths[paths.length - 1]);
		} finally {
			suppressTreeSelectionEvents = false;
		}
		onPlanTreeSelectionChanged();
		refreshOpenPlanFolderChoosers();
		return true;
	}

	/**
	 * Place {@code nodes} under {@code parent} at {@code childIndex} (-1 = append).
	 * Persists Manual sort_order for that parent.
	 */
	private boolean reorderNodesInParent(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode parent,
			int childIndex) {
		if (nodes == null || nodes.isEmpty() || parent == null) {
			return false;
		}
		int index = childIndex;
		if (index < 0) {
			index = parent.getChildCount();
		}
		suppressTreeSelectionEvents = true;
		try {
			for (DefaultMutableTreeNode node : nodes) {
				DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
				if (oldParent == parent) {
					int oldIndex = parent.getIndex(node);
					if (oldIndex >= 0 && oldIndex < index) {
						index--;
					}
				}
			}
			for (DefaultMutableTreeNode node : nodes) {
				if (node.getParent() != null) {
					planTreeModel.removeNodeFromParent(node);
				}
			}
			for (int i = 0; i < nodes.size(); i++) {
				int insertAt = Math.min(index + i, parent.getChildCount());
				planTreeModel.insertNodeInto(nodes.get(i), parent, insertAt);
			}
			persistChildrenSortOrders(parent);
			planTree.clearSelection();
			TreePath[] paths = new TreePath[nodes.size()];
			for (int i = 0; i < nodes.size(); i++) {
				paths[i] = new TreePath(nodes.get(i).getPath());
			}
			planTree.setSelectionPaths(paths);
			planTree.scrollPathToVisible(paths[paths.length - 1]);
		} finally {
			suppressTreeSelectionEvents = false;
		}
		onPlanTreeSelectionChanged();
		return true;
	}

	private final class PlansTreeTransferHandler extends TransferHandler {
		@Override
		public int getSourceActions(JComponent c) {
			return MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			List<DefaultMutableTreeNode> top = getTopLevelSelectedNodes();
			if (top.isEmpty()) {
				return null;
			}
			PlansTreeDragPayload payload = new PlansTreeDragPayload();
			for (DefaultMutableTreeNode node : top) {
				PlansTreeNode d = data(node);
				if (d == null) {
					continue;
				}
				if (d.isPlan() && d.getPlanId() != null) {
					payload.planIds.add(d.getPlanId());
				} else if (d.isFolder() && d.getFolderId() != null) {
					payload.folderIds.add(d.getFolderId());
				}
			}
			if (payload.planIds.isEmpty() && payload.folderIds.isEmpty()) {
				return null;
			}
			return new PlansTreeTransferable(payload);
		}

		@Override
		public boolean canImport(TransferSupport support) {
			if (!support.isDrop() || !support.isDataFlavorSupported(PLANS_TREE_NODES_FLAVOR)) {
				return false;
			}
			JTree.DropLocation loc;
			try {
				loc = (JTree.DropLocation) support.getDropLocation();
			} catch (ClassCastException ex) {
				return false;
			}
			// ON a plan: no insert/ON indicator and no "not allowed" cursor (drop is a no-op in importData).
			// Insert gaps and ON folder keep normal drop feedback.
			if (isPlansTreeDropOntoPlan(loc)) {
				support.setShowDropLocation(false);
				return true;
			}
			support.setShowDropLocation(true);
			Integer targetFolderId = resolveDropTargetFolderId(loc);
			PlansTreeDragPayload payload = peekPayload(support);
			if (payload == null) {
				// During drag, transferable may still be readable
				return true;
			}
			if (!canDropPayloadOnFolder(payload, targetFolderId)) {
				return false;
			}
			if (!plansSortManual) {
				// By name: reject pure sibling reorders (same parent).
				boolean anyCrossFolder = false;
				for (Integer folderId : payload.folderIds) {
					DefaultMutableTreeNode node = findFolderNode(folderId.intValue());
					Integer currentParent = node == null ? null
							: folderIdOf((DefaultMutableTreeNode) node.getParent());
					if (!Objects.equals(currentParent, targetFolderId)) {
						anyCrossFolder = true;
						break;
					}
				}
				if (!anyCrossFolder) {
					for (Integer planId : payload.planIds) {
						DefaultMutableTreeNode node = findPlanNode(planId.intValue());
						PlansTreeNode d = data(node);
						if (d != null && !Objects.equals(d.getFolderId(), targetFolderId)) {
							anyCrossFolder = true;
							break;
						}
					}
				}
				if (!anyCrossFolder) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean importData(TransferSupport support) {
			if (!support.isDrop() || !support.isDataFlavorSupported(PLANS_TREE_NODES_FLAVOR)) {
				return false;
			}
			PlansTreeDragPayload payload;
			try {
				payload = (PlansTreeDragPayload) support.getTransferable().getTransferData(PLANS_TREE_NODES_FLAVOR);
			} catch (UnsupportedFlavorException | IOException | ClassCastException ex) {
				return false;
			}
			JTree.DropLocation loc = (JTree.DropLocation) support.getDropLocation();
			if (isPlansTreeDropOntoPlan(loc)) {
				return false;
			}
			Integer targetFolderId = resolveDropTargetFolderId(loc);
			int childIndex = loc == null ? -1 : loc.getChildIndex();
			return importPlansTreePayload(payload, targetFolderId, childIndex);
		}

		@Override
		protected void exportDone(JComponent source, Transferable data, int action) {
			// Move already applied in importData (same-component DnD).
		}

		private PlansTreeDragPayload peekPayload(TransferSupport support) {
			try {
				Transferable t = support.getTransferable();
				if (t != null && t.isDataFlavorSupported(PLANS_TREE_NODES_FLAVOR)) {
					return (PlansTreeDragPayload) t.getTransferData(PLANS_TREE_NODES_FLAVOR);
				}
			} catch (UnsupportedFlavorException | IOException | ClassCastException ex) {
				// Some look-and-feels deny access until drop; allow the gesture.
			}
			return null;
		}
	}

	private static final class PlansTreeTransferable implements Transferable {
		private final PlansTreeDragPayload payload;

		PlansTreeTransferable(PlansTreeDragPayload payload) {
			this.payload = payload;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { PLANS_TREE_NODES_FLAVOR };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return PLANS_TREE_NODES_FLAVOR != null && PLANS_TREE_NODES_FLAVOR.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return payload;
		}
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
		if ("addFolder".equals(kind)) {
			return UiTheme.isDark() ? new Color(0xFB, 0xBF, 0x24) : new Color(0xD9, 0x77, 0x06);
		}
		if ("settings".equals(kind)) {
			return UiTheme.isDark() ? new Color(0xCB, 0xD5, 0xE1) : new Color(0x47, 0x55, 0x69);
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
		case "delete": {
			// Filled trash — stroke-only outlines often collapse to a red X on Ubuntu 22 OpenJDK.
			g.fillRoundRect(5, 6, 6, 8, 1, 1);
			g.fillRect(4, 5, 8, 2);
			g.fillRect(7, 3, 2, 2);
			g.setComposite(java.awt.AlphaComposite.Clear);
			g.fillRect(7, 8, 1, 4);
			g.fillRect(9, 8, 1, 4);
			g.setComposite(java.awt.AlphaComposite.SrcOver);
			break;
		}
		case "duplicate": {
			// Filled stacked pages — more reliable than stroke drawRect on Linux HiDPI.
			Color c = planToolbarIconColor(kind);
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
			g.fillRoundRect(3, 3, 8, 8, 2, 2);
			g.setColor(c);
			g.fillRoundRect(5, 5, 8, 8, 2, 2);
			break;
		}
		case "addFolder":
			// folder outline (tab on the upper left) with a small plus inside
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(2, 4, 6, 4);
			g.drawLine(6, 4, 8, 6);
			g.drawRect(2, 4, 12, 9);
			g.drawLine(8, 8, 8, 11);
			g.drawLine(6, 9, 10, 9);
			break;
		case "settings": {
			// Filled cog with center hole (radial spokes read as a wagon wheel at 16px).
			final double cx = 7.5;
			final double cy = 7.5;
			final double outerR = 6.4;
			final double rootR = 4.35;
			final double holeR = 2.05;
			final int teeth = 8;
			java.awt.geom.Path2D.Double gear = new java.awt.geom.Path2D.Double();
			for (int i = 0; i < teeth; i++) {
				double mid = i * (Math.PI * 2.0 / teeth) - Math.PI / 2.0;
				double halfStep = Math.PI / teeth;
				double toothHalf = halfStep * 0.42;
				double[] angles = {
						mid - halfStep,
						mid - toothHalf,
						mid + toothHalf,
						mid + halfStep
				};
				double[] radii = { rootR, outerR, outerR, rootR };
				for (int p = 0; p < 4; p++) {
					double x = cx + Math.cos(angles[p]) * radii[p];
					double y = cy + Math.sin(angles[p]) * radii[p];
					if (i == 0 && p == 0) {
						gear.moveTo(x, y);
					} else {
						gear.lineTo(x, y);
					}
				}
			}
			gear.closePath();
			g.fill(gear);
			g.setComposite(java.awt.AlphaComposite.Clear);
			g.fill(new java.awt.geom.Ellipse2D.Double(cx - holeR, cy - holeR, holeR * 2, holeR * 2));
			break;
		}
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
		return findDiagramDockingPanel(tabPaneToLink.getComponentAt(selected));
	}

	private static DiagramDockingPanel findDiagramDockingPanel(Component root) {
		if (root instanceof DiagramDockingPanel) {
			return (DiagramDockingPanel) root;
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				DiagramDockingPanel found = findDiagramDockingPanel(child);
				if (found != null) {
					return found;
				}
			}
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
				DiagramDockingPanel diagram = findDiagramDockingPanel(tabPaneToLink.getComponentAt(i));
				if (diagram != null) {
					diagram.repairAfterThemeChange();
				}
			}
		}
		syncDiagramViewMenuItems();
	}

	public void syncDiagramViewMenuItems() {
		Runnable sync = () -> {
			DiagramDockingPanel diagram = findActiveDiagramDockingPanel();
			// Shared Find checkbox can stay checked across plans; each tab has its own bar.
			if (diagram != null) {
				diagram.syncFindBarFromToolbar();
			}
			if (propertiesViewMenuItem == null || slidersViewMenuItem == null || zoomViewMenuItem == null) {
				return;
			}
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
	
	/** Plan name only when exactly one plan is selected; null/blank → plain "Plan Source". */
	private String getPlanSourceTabName(String planName) {
		if (planName == null || planName.isBlank()) {
			return "Plan Source";
		}
		return planName + " - Plan Source";
	}

	private String getPlanSourceTabName(String planName, PlanSourcePanel source) {
		String base = getPlanSourceTabName(planName);
		return source != null && source.isAnonymizeEnabled() ? base + " (anonymized)" : base;
	}
	
	private String getPlanStatisticsTabName() {
		return "Compare Plans";
	}
	
	/** Plan name only when exactly one plan is selected; null/blank → plain "Plan Diagram". */
	private String getPlanDiagramTabName(String planName) {
		if (planName == null || planName.isBlank()) {
			return "Plan Diagram";
		}
		return planName + " - Plan Diagram";
	}

	private String getPlanDiagramTabName(String planName, PlanSourcePanel source) {
		String base = getPlanDiagramTabName(planName);
		return source != null && source.isAnonymizeEnabled() ? base + " (anonymized)" : base;
	}

	private String getTranslateTabName() {
		return "Translate";
	}

	private int indexOfTabTitle(String title) {
		if (tabPaneToLink == null || title == null) {
			return -1;
		}
		for (int i = 0; i < tabPaneToLink.getTabCount(); i++) {
			if (title.equals(tabPaneToLink.getTitleAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private int indexOfTabContaining(String fragment) {
		if (tabPaneToLink == null || fragment == null) {
			return -1;
		}
		for (int i = 0; i < tabPaneToLink.getTabCount(); i++) {
			String title = tabPaneToLink.getTitleAt(i);
			if (title != null && title.contains(fragment)) {
				return i;
			}
		}
		return -1;
	}

	/** Show/hide Translate between Plan Source and Compare Plans; refresh titles/badges. */
	private void syncAnonymizeChrome(String planName, PlanSourcePanel sourcePanel, JPanel diagramTab) {
		if (tabPaneToLink == null || sourcePanel == null) {
			return;
		}
		int translateIdx = indexOfTabTitle(getTranslateTabName());
		boolean wantTranslate = sourcePanel.isAnonymizeEnabled();
		if (wantTranslate && translateIdx < 0) {
			TranslateAnonymizedPanel translatePanel = new TranslateAnonymizedPanel();
			translatePanel.setPlanSourcePanel(sourcePanel);
			tabPaneToLink.insertTab(getTranslateTabName(), null, translatePanel, null, 1);
		} else if (!wantTranslate && translateIdx >= 0) {
			if (tabPaneToLink.getSelectedIndex() == translateIdx) {
				tabPaneToLink.setSelectedIndex(0);
			}
			tabPaneToLink.removeTabAt(translateIdx);
		} else if (wantTranslate && translateIdx >= 0) {
			Component c = tabPaneToLink.getComponentAt(translateIdx);
			if (c instanceof TranslateAnonymizedPanel) {
				((TranslateAnonymizedPanel) c).setPlanSourcePanel(sourcePanel);
			}
		}

		// Titles include the plan name only for a single selected plan.
		String titleName = isSinglePlanSelected() ? planName : null;
		if (tabPaneToLink.getTabCount() > 0) {
			tabPaneToLink.setTitleAt(0, getPlanSourceTabName(titleName, sourcePanel));
		}
		int diagramIdx = indexOfTabContaining("Plan Diagram");
		if (diagramIdx >= 0) {
			tabPaneToLink.setTitleAt(diagramIdx, getPlanDiagramTabName(titleName, sourcePanel));
		}
		refreshDiagramAnonymizeBadge(diagramTab, sourcePanel);
		updatePlanDiagramTabEnabled();
	}

	/** Index of the Plan Diagram tab for the current documents strip, or -1. */
	public int getPlanDiagramTabIndex() {
		return indexOfTabContaining("Plan Diagram");
	}

	/** True when exactly one plan node is selected (folders ignored). */
	public boolean isSinglePlanSelected() {
		return planTree != null && getSelectedPlanNodes().size() == 1;
	}

	public void updatePlanDiagramTabEnabled() {
		int diagramIdx = getPlanDiagramTabIndex();
		if (tabPaneToLink == null || diagramIdx < 0) {
			return;
		}
		PlansTreeNode primaryPlan = getPrimarySelectedPlan();
		PlanEvent pEvent = primaryPlan == null ? null : plansMapper.get(primaryPlan.getPlanId());
		PlanSourcePanel source = pEvent == null ? null : pEvent.getPlanSourcePanel();
		boolean singlePlan = isSinglePlanSelected();
		boolean enableDiagram = primaryPlan != null
				&& source != null
				&& source.hasUsableJsonPlan();
		tabPaneToLink.setEnabledAt(diagramIdx, enableDiagram);

		int sourceIdx = indexOfTabContaining("Plan Source");
		if (sourceIdx >= 0) {
			// Multi-select / folder-only: Plan Source stays disabled (empty panel).
			tabPaneToLink.setEnabledAt(sourceIdx, singlePlan);
		}

		if (source != null) {
			// Show-diagram button lives on Plan Source — only for a single selection.
			source.setShowPlanGraphButton(singlePlan && enableDiagram);
		}
		if (!enableDiagram && tabPaneToLink.getSelectedIndex() == diagramIdx) {
			// No JSON → Plan Source (single) or Compare Plans (multi)
			int compareIdx = indexOfTabTitle(getPlanStatisticsTabName());
			tabPaneToLink.setSelectedIndex(singlePlan ? 0
					: (compareIdx >= 0 ? compareIdx : 0));
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

		List<Integer> plansToGenerate = new ArrayList<>();
		for (Integer planId : getSelectedPlanIds()) {
			PlanEvent mappedEvent = plansMapper.get(planId);
			if (mappedEvent == null) {
				continue;
			}
			PlanSourcePanel source = mappedEvent.getPlanSourcePanel();
			PlanGraphPanel graph = mappedEvent.getPlanGraphPanel();
			if (source == null || graph == null) {
				continue;
			}
			if (!source.hasUsableJsonPlan()) {
				continue;
			}
			String fingerprint = PlanSourcePanel.normalizeJsonPlanText(source.getJsonPlanText());
			if (!graph.hasGeneratedPlanValues() || source.shouldRebuildDiagramFromJson(fingerprint)) {
				plansToGenerate.add(planId);
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

	private void generatePlanDiagramsWithProgress(List<Integer> planIds, Runnable onComplete) {
		final int planCount = planIds.size();
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

				Integer planId = planIds.get(i);
				DefaultMutableTreeNode planNode = planId == null ? null : findPlanNode(planId.intValue());
				PlansTreeNode planData = planNode == null ? null : data(planNode);
				String planName = planData != null ? planData.getName() : String.valueOf(planId);
				statusLabel.setText(planCount == 1
						? "Generating Plan"
						: ("Generating Plan " + (i + 1) + " of " + planCount));
				progressBar.setValue(i);
				if (planCount > 1) {
					progressBar.setString((i + 1) + " / " + planCount);
				}
				progressBar.setToolTipText(planName);
				paintProgressDialogNow(progressDialog);

				PlanEvent planEvent = plansMapper.get(planId);
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

	/** SQL fragment: {@code ifnull(<column>, -1) = <id or -1>} for folder-scoped uniqueness checks. */
	private static String folderScopeSql(String column, Integer folderId) {
		return "ifnull(" + column + ", -1) = " + (folderId == null ? "-1" : String.valueOf(folderId.intValue()));
	}

	private static String sqlFolderIdLiteral(Integer folderId) {
		return folderId == null ? "null" : String.valueOf(folderId.intValue());
	}

	private static String sqlQuote(String text) {
		return "'" + (text == null ? "" : text.replace("'", "''")) + "'";
	}

	private static List<String> queryStrings(String strSQL, String column) {
		List<String> out = new ArrayList<>();
		JsonArray rows = Database.rows(strSQL);
		if (rows == null) {
			return out;
		}
		for (int i = 0; i < rows.size(); i++) {
			Object value = ((JsonObject) rows.get(i)).get(column);
			if (value != null) {
				out.add(String.valueOf(value));
			}
		}
		return out;
	}

	/** Plan names already used directly in {@code folderId} (null = root). */
	private List<String> planNamesInFolder(Integer folderId) {
		return queryStrings("select plan_name from explain_plans where " + folderScopeSql("folder_id", folderId) + ";",
				"plan_name");
	}

	private List<String> folderNamesInParent(Integer parentFolderId) {
		return queryStrings("select folder_name from plan_folders where "
				+ folderScopeSql("parent_id_folder", parentFolderId) + ";", "folder_name");
	}

	/** Legacy duplicate suffix: "Name (copy 2)". */
	private static final Pattern COPY_SUFFIX_LEGACY = Pattern.compile("^(.*) \\(copy (\\d+)\\)$");
	/** Current duplicate suffix: "Name (2)". */
	private static final Pattern COPY_SUFFIX = Pattern.compile("^(.*) \\((\\d+)\\)$");

	/**
	 * "Name" → "Name (N)" where N is one more than the highest existing numbered
	 * (or legacy " (copy N)") sibling for the same base name.
	 */
	public static String nextCopyName(String desiredName, List<String> existingNames) {
		String base = copyNameBase(desiredName);
		int highest = 0;
		for (String existing : existingNames) {
			Integer n = copyNameNumber(existing, base);
			if (n != null) {
				highest = Math.max(highest, n.intValue());
			}
		}
		int next = highest + 1;
		String candidate = base + " (" + next + ")";
		while (existingNames.contains(candidate)) {
			next++;
			candidate = base + " (" + next + ")";
		}
		return candidate;
	}

	private static String copyNameBase(String name) {
		if (name == null) {
			return "";
		}
		Matcher legacy = COPY_SUFFIX_LEGACY.matcher(name);
		if (legacy.matches()) {
			return legacy.group(1);
		}
		Matcher numbered = COPY_SUFFIX.matcher(name);
		if (numbered.matches()) {
			return numbered.group(1);
		}
		return name;
	}

	/** Number suffix for {@code name} when it belongs to {@code base}, else null. */
	private static Integer copyNameNumber(String name, String base) {
		if (name == null || base == null) {
			return null;
		}
		if (name.equals(base)) {
			return Integer.valueOf(0);
		}
		Matcher legacy = COPY_SUFFIX_LEGACY.matcher(name);
		if (legacy.matches() && base.equals(legacy.group(1))) {
			try {
				return Integer.valueOf(Integer.parseInt(legacy.group(2)));
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		Matcher numbered = COPY_SUFFIX.matcher(name);
		if (numbered.matches() && base.equals(numbered.group(1))) {
			try {
				return Integer.valueOf(Integer.parseInt(numbered.group(2)));
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}

	private String uniquePlanNameInFolder(String desiredName, Integer folderId) {
		List<String> existing = planNamesInFolder(folderId);
		return existing.contains(desiredName) ? nextCopyName(desiredName, existing) : desiredName;
	}

	private String uniqueFolderName(String desiredName, Integer parentFolderId) {
		List<String> existing = folderNamesInParent(parentFolderId);
		return existing.contains(desiredName) ? nextCopyName(desiredName, existing) : desiredName;
	}

	/** Insert a folder under {@code parentFolderId} (null = root); returns its new id. */
	private Integer insertFolderToDatabase(String folderName, Integer parentFolderId) {
		boolean bolShowSQL = false;
		int sortOrder = nextSortOrderInFolder(parentFolderId, true);
		String strSQL = "insert into plan_folders (id_folder, folder_name, parent_id_folder, sort_order)\n"
				+ "select coalesce(max(id_folder) + 1, 1), " + sqlQuote(folderName) + ", "
				+ sqlFolderIdLiteral(parentFolderId) + ", " + sortOrder + "\n"
				+ "from plan_folders;";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		strSQL = "select id_folder from plan_folders where " + folderScopeSql("parent_id_folder", parentFolderId)
				+ " and folder_name = " + sqlQuote(folderName) + ";";
		JsonObject row = Database.row(strSQL);
		return row == null ? null : toInteger(row.get("id_folder"));
	}

	/** Insert a plan with an explicit name in {@code folderId} (used by Duplicate); returns the new plan id. */
	private Integer addPlanNameToDatabase(String planName, Integer folderId, PlanSourcePanel planSource, PlanGraphPanel planGraphPanel) {
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		Integer intLastIDExplainPlan = null;
		String strSQL;

		if (bolShowProperties == true) { System.out.println("add to explain_plans"); }
		int sortOrder = nextSortOrderInFolder(folderId, false);
		strSQL = "insert into explain_plans (id_explain_plan, plan_name, folder_id, sort_order) select coalesce(max(id_explain_plan) + 1, 1), "
				+ sqlQuote(planName) + ", " + sqlFolderIdLiteral(folderId) + ", " + sortOrder
				+ " from explain_plans";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		strSQL = "select id_explain_plan from explain_plans where plan_name = " + sqlQuote(planName)
				+ " and " + folderScopeSql("folder_id", folderId) + ";";
		JsonObject row = Database.row(strSQL);
		intLastIDExplainPlan = row == null ? null : toInteger(row.get("id_explain_plan"));
		if (intLastIDExplainPlan == null) {
			return null;
		}

		strSQL = "insert into explain_plan_generated_values (id_explain_plan, is_active) values (" + intLastIDExplainPlan + ", 0);";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		return intLastIDExplainPlan;
	}
	
	/** Insert a new "Plan N" in {@code folderId} (null = root); returns the new plan id. */
	private Integer addPlanToDatabase(Integer folderId, PlanSourcePanel planSource, PlanGraphPanel planGraphPanel) {
		JsonObject idRow = Database.row("select coalesce(max(id_explain_plan) + 1, 1) as next_id from explain_plans;");
		Integer nextId = idRow == null ? null : toInteger(idRow.get("next_id"));
		if (nextId == null) {
			return null;
		}
		return insertPlanRow(nextId, uniquePlanNameInFolder("Plan " + nextId, folderId), folderId);
	}

	private Integer addNamedPlanToDatabase(String preferredName, Integer folderId,
			PlanSourcePanel planSource, PlanGraphPanel planGraphPanel) {
		JsonObject idRow = Database.row("select coalesce(max(id_explain_plan) + 1, 1) as next_id from explain_plans;");
		Integer nextId = idRow == null ? null : toInteger(idRow.get("next_id"));
		if (nextId == null) {
			return null;
		}
		String base = (preferredName == null || preferredName.isBlank()) ? ("Plan " + nextId) : preferredName.trim();
		return insertPlanRow(nextId, uniquePlanNameInFolder(base, folderId), folderId);
	}

	private Integer insertPlanRow(Integer nextId, String planName, Integer folderId) {
		boolean bolShowSQL = false;
		int sortOrder = nextSortOrderInFolder(folderId, false);
		String strSQL = "insert into explain_plans (id_explain_plan, plan_name, folder_id, sort_order) values ("
				+ nextId + ", " + sqlQuote(planName) + ", " + sqlFolderIdLiteral(folderId) + ", "
				+ sortOrder + ")";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		strSQL = "insert into explain_plan_generated_values (id_explain_plan, is_active) values (" + nextId + ", 0);";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		return nextId;
	}

	private static String joinIds(List<Integer> ids) {
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(id.intValue());
		}
		return sb.toString();
	}

	/** Delete a single plan by id (and its generated values). */
	private void deletePlanFromDatabase(Integer planId) {
		if (planId == null) {
			return;
		}
		List<Integer> ids = new ArrayList<>();
		ids.add(planId);
		deleteItemsFromDatabase(ids, new ArrayList<Integer>());
	}

	/**
	 * Delete plans by id and folders by id. Callers pass the full subtree of any folder
	 * being removed (child plans + child folders recursively); plans still pointing at a
	 * deleted folder are removed as a safety net.
	 */
	private void deleteItemsFromDatabase(List<Integer> planIds, List<Integer> folderIds) {
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		String strSQL;
		if (bolShowProperties == true) { System.out.println("deleteItemsFromDatabase"); }

		if (!planIds.isEmpty()) {
			strSQL = "delete from explain_plans where id_explain_plan in (" + joinIds(planIds) + ");";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
		}
		if (!folderIds.isEmpty()) {
			strSQL = "delete from explain_plans where folder_id in (" + joinIds(folderIds) + ");";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);

			strSQL = "delete from plan_folders where id_folder in (" + joinIds(folderIds) + ");";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
		}

		strSQL = "delete from explain_plan_generated_values where id_explain_plan not in (select id_explain_plan from explain_plans);";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}
	
	/** Same wheel step as OperatorDetailsPanel Property/Value scrolling. */
	private static final int DIAGRAM_WHEEL_UNIT_INCREMENT = 48;

	/**
	 * Apply the shared diagram zoom preference to a plan graph and sync the toolbar slider.
	 */
	private void applySharedZoomToGraph(PlanGraphPanel graphPanel) {
		if (graphPanel == null) {
			return;
		}
		graphPanel.applyPreferredZoom();
		graphPanel.refreshScrollableSizeAfterZoom(false);
		if (planDiagramToolbar != null) {
			planDiagramToolbar.syncZoomPercentage(graphPanel.getZoomPercentage());
		}
	}

	/**
	 * Plan Diagram tab: nested docking for Graph / Properties / Sliders
	 * (reposition only within this tab).
	 */
	private JPanel createPlanDiagramTab(PlanGraphPanel graphPanel,
			int horizontalDivider, int verticalDivider) {
		return createPlanDiagramTab(graphPanel, horizontalDivider, verticalDivider, null);
	}

	private JPanel createPlanDiagramTab(PlanGraphPanel graphPanel,
			int horizontalDivider, int verticalDivider, PlanSourcePanel sourcePanel) {
		DiagramDockingPanel diagram = new DiagramDockingPanel(graphPanel, planDiagramToolbar, DIAGRAM_WHEEL_UNIT_INCREMENT);
		diagram.setViewMenuSyncHook(this::syncDiagramViewMenuItems);
		applySharedZoomToGraph(graphPanel);
		SwingUtilities.invokeLater(this::syncDiagramViewMenuItems);

		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setOpaque(false);
		wrap.add(diagram, BorderLayout.CENTER);

		JLabel badge = new JLabel(loadAnonymizeSilhouetteIcon());
		badge.setName("anonymizeBadge");
		badge.setToolTipText("Showing anonymized text");
		badge.setVisible(sourcePanel != null && sourcePanel.isAnonymizeEnabled());
		JPanel badgeCorner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		badgeCorner.setOpaque(false);
		badgeCorner.add(badge);
		wrap.add(badgeCorner, BorderLayout.NORTH);
		wrap.putClientProperty("anonymizeBadge", badge);
		return wrap;
	}

	private void refreshDiagramAnonymizeBadge(JPanel diagramTab, PlanSourcePanel sourcePanel) {
		if (diagramTab == null) {
			return;
		}
		Object badgeObj = diagramTab.getClientProperty("anonymizeBadge");
		if (badgeObj instanceof JLabel) {
			((JLabel) badgeObj).setVisible(sourcePanel != null && sourcePanel.isAnonymizeEnabled());
			diagramTab.revalidate();
			diagramTab.repaint();
		}
	}

	private static ImageIcon loadAnonymizeSilhouetteIcon() {
		java.net.URL url = FormPanel.class.getResource("/images/anonymize-silhouette.png");
		if (url == null) {
			return null;
		}
		ImageIcon raw = new ImageIcon(url);
		Image scaled = raw.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
		return new ImageIcon(scaled);
	}

	/** Plans tree (nested folders, multi-select). */
	public JTree getPlanTree() { 
		return planTree;
	}

	/** Plan id for the current (primary) plan selection, or null if no plan is selected. */
	public Integer getSelectedPlanId() {
		PlansTreeNode primary = getPrimarySelectedPlan();
		return primary == null ? null : primary.getPlanId();
	}

	/** Plan id that owns {@code source}, or null (used by PlanSourcePanel rename). */
	public Integer findPlanIdForSource(PlanSourcePanel source) {
		if (source == null) {
			return null;
		}
		for (Map.Entry<Integer, PlanEvent> entry : plansMapper.entrySet()) {
			PlanEvent event = entry.getValue();
			if (event != null && event.getPlanSourcePanel() == source) {
				return entry.getKey();
			}
		}
		return null;
	}

	/** Rename a plan in the tree and plansMapper (DB is updated by the caller). */
	public void applyPlanRename(int planId, String strNewPlanName) {
		DefaultMutableTreeNode node = findPlanNode(planId);
		if (node != null) {
			PlansTreeNode d = data(node);
			if (d != null) {
				d.setName(strNewPlanName);
			}
			planTreeModel.nodeChanged(node);
			if (!plansSortManual) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
				if (parent != null) {
					suppressTreeSelectionEvents = true;
					try {
						sortTreeNodeChildren(parent, false);
					} finally {
						suppressTreeSelectionEvents = false;
					}
					selectTreeNode(node);
				}
			}
		}
		PlanEvent pEvent = plansMapper.get(Integer.valueOf(planId));
		if (pEvent != null) {
			pEvent.setPlanName(strNewPlanName);
		}
	}

	/** Update the tree node at row {@code index} (or the selected plan) to the new name. */
	public void setSelectedPlanName(int index, String strNewPlanName) {
		PlansTreeNode target = null;
		if (index >= 0 && planTree != null) {
			TreePath path = planTree.getPathForRow(index);
			if (path != null) {
				PlansTreeNode d = data(path.getLastPathComponent());
				if (d != null && d.isPlan()) {
					target = d;
				}
			}
		}
		if (target == null) {
			target = getPrimarySelectedPlan();
		}
		if (target != null && target.getPlanId() != null) {
			applyPlanRename(target.getPlanId().intValue(), strNewPlanName);
		}
	}

	/** Tree row of the first selected item, or -1. */
	protected int getSelectedIndex() {
		return planTree == null ? -1 : planTree.getMinSelectionRow();
	}
	
	protected void updatePlanMapper(String existingPlanName, String strNewPlanName) {
		// Prefer the selected plan's id; fall back to a name match.
		Integer planId = getSelectedPlanId();
		PlansTreeNode selected = getPrimarySelectedPlan();
		if (planId == null || selected == null || !existingPlanName.equals(selected.getName())) {
			planId = null;
			Enumeration<?> e = planTreeRoot.preorderEnumeration();
			while (e.hasMoreElements()) {
				PlansTreeNode d = data(e.nextElement());
				if (d != null && d.isPlan() && existingPlanName.equals(d.getName())) {
					planId = d.getPlanId();
					break;
				}
			}
		}
		if (planId != null) {
			applyPlanRename(planId.intValue(), strNewPlanName);
		}
	}
	
}
