
package com.williammeitzen.peps.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Stroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.json.simple.DeserializationException;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import com.williammeitzen.peps.gui.Utils.LineThicknesses;
import com.williammeitzen.peps.gui.Utils.PercentageBy;
import com.williammeitzen.peps.model.Database;
import com.williammeitzen.peps.model.ExplainTextPlanConverter;
import com.williammeitzen.peps.model.PlanFormat;

//import com.github.cliftonlabs.json_simple.JsonException;
//import com.github.cliftonlabs.json_simple.JsonArray;
//import com.github.cliftonlabs.json_simple.JsonObject;
//import com.github.cliftonlabs.json_simple.Jsoner;

public class PlanGraphPanel extends JPanel implements MouseListener, MouseMotionListener {

	// will be good to study source code behind cost calculations:
	// https://github.com/postgres/postgres/blob/master/src/backend/optimizer/path/costsize.c
	
	private static final long serialVersionUID = 1L;
	//private JTabbedPane tabPane;
	private JTabbedPane objCurrentPlanTabs;
	private int operatorCount;
	private int intZoomPercentage = 100;
	private PercentageBy percentageBy = PercentageBy.COST; // update MainFrame.java.JMenu percentageByItem, *.setSelected(true)
	private LineThicknesses enmLineThicknessBy = LineThicknesses.ACTUAL_DATA_MASS; // update MainFrame.java.JMenu lineThicknessByItem, *.setSelected(true)
	private Double dblPlanTimeSliderMs = 0d;
	private Double dblPlanCostSlider = 0d;
	private int sliderType = SliderPanel.SLIDER_TYPE_COST; // SLIDER_TYPE_TIME -or- SLIDER_TYPE_COST
	
	private int maxUnitIncrement = 50;
	private Map<Integer, List<Shape>> propertyVsShapesMap = new HashMap<>();
	private Map<Integer, List<Shape>> showWorkerToggleVsShapesMap = new HashMap<>();
	private Integer hoveredOperatorId = null;
	private Integer selectedOperatorId = null;
	
	private Integer idExplainPlan;
	private Integer intXDimension = 5000;
	private Integer intYDimension = 5000;
	/** When set, paint/rebuild uses this Plan Source instead of tab 0 (Compare Plans batch generate). */
	private PlanSourcePanel planSourceOverride = null;
	private boolean suppressDiagramBusyCursor = false;

	private Integer GRAPH_TOP_MARGIN = 25; // zoom has no effect on margins
	private Integer GRAPH_LEFT_MARGIN = 0; // zoom has no effect on margins
	private Integer GRAPH_BOTTOM_MARGIN = 50; // zoom has no effect on margins
	private Integer GRAPH_RIGHT_MARGIN = 50; // zoom has no effect on margins
	private Color OPERATOR_BOX_COLOR = new Color(0x47, 0x55, 0x69); // slate
	private Integer OPERATOR_BOX_THICKNESS = 1; // zoom has no effect on operator box thickness
	private Integer OPERATOR_BOX_CORNER_RADIUS = 8;
	/** Opacity for operators with Actual Loops = 0 (never executed). */
	private static final float NEVER_EXECUTED_ALPHA = 0.42f;
	/** Property ids with Actual Loops = 0 for the diagram currently being painted. */
	private Set<Integer> neverExecutedOperatorIds = Collections.emptySet();
	private Integer OPERATOR_BOX_WIDTH = 40; // zoom affects box dimensions
	private Integer OPERATOR_BOX_HEIGHT = 40; // zoom affects box dimensions
	private Integer HORIZONTAL_SPACE = 40; // zoom affects empty space between operators
	private Integer VERTICAL_SPACE = 60; // zoom affects empty space between operators
	private String OPERATOR_FONT_NAME = Font.DIALOG; // matched to Plan Source labels via Label.font
	private int OPERATOR_FONT_STYLE = Font.PLAIN;
	private int OPERATOR_FONT_SIZE = 12;
	private Color OPERATOR_FONT_FOREGROUND_COLOR = new Color(0x1D, 0x4E, 0x89);
	private static final Color OPERATOR_FONT_FOREGROUND_COLOR_DARK = new Color(0xF1, 0xF5, 0xF9); // soft white
	
	// arrow fonts and colors
	private Color ARROW_LINE_COLOR = new Color(0x25, 0x63, 0xEB);
	private Color ARROW_LINE_COLOR_END = new Color(0x93, 0xC5, 0xFD);
	private int ARROW_MINIMUM_HORIZONTAL_DISTANCE = 15;
	
	private String ACTUAL_MASS_ARROW_FONT_NAME = "Arial";
	private int ACTUAL_MASS_ARROW_FONT_STYLE = Font.BOLD;
	private int ACTUAL_MASS_ARROW_FONT_SIZE = 13;
	private Color ACTUAL_MASS_ARROW_FONT_COLOR = new Color(0xDC, 0x26, 0x26);
	
	private String ESTIMATED_MASS_ARROW_FONT_NAME = "Arial";
	private int ESTIMATED_MASS_ARROW_FONT_STYLE = Font.PLAIN;
	private int ESTIMATED_MASS_ARROW_FONT_SIZE = 12;
	private Color ESTIMATED_MASS_ARROW_FONT_COLOR = new Color(0xDC, 0x26, 0x26);
	
	private String ACTUAL_ROWS_ARROW_FONT_NAME = "Arial";
	private int ACTUAL_ROWS_ARROW_FONT_STYLE = Font.BOLD;
	private int ACTUAL_ROWS_ARROW_FONT_SIZE = 13;
	private Color ACTUAL_ROWS_ARROW_FONT_COLOR = new Color(0xDC, 0x26, 0x26);
	
	private String ESTIMATED_ROWS_ARROW_FONT_NAME = "Arial";
	private int ESTIMATED_ROWS_ARROW_FONT_STYLE = Font.PLAIN;
	private int ESTIMATED_ROWS_ARROW_FONT_SIZE = 12;
	private Color ESTIMATED_ROWS_ARROW_FONT_COLOR = new Color(0xDC, 0x26, 0x26);
	
	private Color PLAN_WIDTH_0_ARROW_FONT_COLOR = new Color(0x33, 0x41, 0x55);
	
	private Color HIGHLIGHT_1_FONT_FOREGROUND_COLOR = Color.yellow;
	private Color HIGHLIGHT_1_FONT_BACKGROUND_COLOR = Color.red;
	private Color HIGHLIGHT_2_FONT_FOREGROUND_COLOR = Color.red;
	private Color HIGHLIGHT_2_FONT_BACKGROUND_COLOR = Color.yellow;

	/** Yellow find-match underline fill (substring highlights from Ctrl+F / Find). */
	private static final Color FIND_MATCH_BACKGROUND = new Color(0xFF, 0xEB, 0x3B);
	private static final Color FIND_MATCH_FOREGROUND = new Color(0x11, 0x18, 0x27);
	/** Active Find query; empty/null → no substring highlights. */
	private String findHighlightQuery = null;
	
	// for grey, keep all 3 numbers the same.
	// higher is lighter: (204, 204, 204) is lighter than (51, 51, 51)
	//private Color TIME_SLIDER_BACKGROUND_COLOR = Color.green;
	private Color TIME_SLIDER_BACKGROUND_COLOR = new Color(0, 190,  0); // light green
	private Color TIME_SLIDER_BACKGROUND_COLOR_FILLED = new Color(0, 125,  0); // dark green
	//private Color COST_SLIDER_BACKGROUND_COLOR = Color.darkGray;
	private Color COST_SLIDER_BACKGROUND_COLOR = new Color(190, 190, 190); // light gray
	private Color COST_SLIDER_BACKGROUND_COLOR_FILLED = new Color(125, 125, 125); // dark gray
	
	// worker fonts
	private String WORKER_TITLE_FONT_NAME = "Verdana";
	private int WORKER_TITLE_FONT_STYLE = Font.BOLD;
	private int WORKER_TITLE_FONT_SIZE = 10;
	private Color WORKER_TITLE_FONT_COLOR = Color.black;
	private String WORKER_TEXT_FONT_NAME = "Verdana";
	private int WORKER_TEXT_FONT_STYLE = Font.PLAIN;
	private int WORKER_TEXT_FONT_SIZE = 10;
	private Color WORKER_TEXT_FONT_COLOR = Color.black;
	
	private static Integer intOldIDExplainPlan;
	/** When true, next paint re-runs toolbar enable even if plan id is unchanged (e.g. after folder selection). */
	private static boolean needsDiagramToolbarRefresh = false;

	/** Call after toolbar was blanked (folder-only) so the next diagram paint restores options. */
	public static void requestDiagramToolbarRefresh() {
		needsDiagramToolbarRefresh = true;
	}
	
	public PlanGraphPanel(JTabbedPane objCurrentPlanTabs) {
		boolean bolShowProperties = false;
		this.objCurrentPlanTabs = objCurrentPlanTabs;
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		syncThemeSurface();
		if (bolShowProperties == true) { System.out.println("PlanGraphPanel constructor"); }
	}

	/** Keep the diagram canvas on the active scheme surface (avoids a light panel under Dark). */
	public void syncThemeSurface() {
		setOpaque(true);
		// ColorUIResource so a later LAF/scheme swap can replace this fill.
		setBackground(new javax.swing.plaf.ColorUIResource(UiTheme.surfaceColor()));
	}

	public void paint(Graphics g) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("paint(Graphics g)"); }
		super.paint(g);
		if (idExplainPlan != null
				&& (intOldIDExplainPlan == null || !intOldIDExplainPlan.equals(idExplainPlan)
						|| needsDiagramToolbarRefresh)) {
			if (bolShowProperties == true) { System.out.println("enableLineThicknessByAndPercentageByButtons()"); }
			enableLineThicknessByAndPercentageByButtons(idExplainPlan);
			intOldIDExplainPlan = idExplainPlan;
			needsDiagramToolbarRefresh = false;
		}
	}

	/**
	 * Wait cursor for diagram construction. Avoids the glass pane — showing it
	 * mid-paint re-enters painting and can leave the diagram blank.
	 */
	static void setDiagramBusyCursor(Component source, boolean busy) {
		if (source == null) {
			return;
		}
		Window window = SwingUtilities.getWindowAncestor(source);
		if (window == null) {
			window = source instanceof Window ? (Window) source : null;
		}
		if (window == null) {
			return;
		}
		window.setCursor(busy
				? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
				: Cursor.getDefaultCursor());
	}

	/** True when explain_plan_generated_values has a row for this plan. */
	public boolean hasGeneratedPlanValues() {
		if (idExplainPlan == null) {
			return false;
		}
		JsonObject row = Database.row(
				"select id_explain_plan from explain_plan_generated_values where id_explain_plan = "
						+ idExplainPlan);
		return row != null && row.get("id_explain_plan") != null;
	}

	/**
	 * Build sqlite annotations / generated values from the given Plan Source JSON
	 * without requiring the Plan Diagram tab to be visible. Used by Compare Plans.
	 */
	public boolean ensurePlanDataGenerated(PlanSourcePanel source) {
		if (source == null || idExplainPlan == null) {
			return false;
		}
		if (!source.hasUsableJsonPlan()) {
			return false;
		}
		String fingerprint = PlanSourcePanel.normalizeJsonPlanText(source.getJsonPlanText());
		if (hasGeneratedPlanValues() && !source.shouldRebuildDiagramFromJson(fingerprint)) {
			return true;
		}
		planSourceOverride = source;
		suppressDiagramBusyCursor = true;
		try {
			source.setPlanSourceButtonClicked(PlanSourcePanel.SHOW_JSON_PLAN_DIAGRAM);
			source.setPlanTextIsDirty(true);
			int w = Math.max(1, getWidth());
			int h = Math.max(1, getHeight());
			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = img.createGraphics();
			try {
				paintComponent(g2);
			} finally {
				g2.dispose();
			}
			return hasGeneratedPlanValues();
		} finally {
			planSourceOverride = null;
			suppressDiagramBusyCursor = false;
		}
	}

	/**
	 * Run EXPLAIN against the database and build the diagram without switching tabs.
	 * On failure, {@link PlanSourcePanel#showJsonPlanExecutionError(String)} is used.
	 */
	public boolean executeAndBuildDiagram(PlanSourcePanel source) {
		if (source == null || idExplainPlan == null) {
			return false;
		}
		planSourceOverride = source;
		suppressDiagramBusyCursor = true;
		try {
			source.clearJsonPlanErrorStyle();
			source.setPlanSourceButtonClicked(PlanSourcePanel.EXECUTE_AND_SHOW_PLAN_DIAGRAM);
			source.setPlanTextIsDirty(true);
			int w = Math.max(400, Math.max(1, getWidth()));
			int h = Math.max(300, Math.max(1, getHeight()));
			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = img.createGraphics();
			try {
				paintComponent(g2);
			} finally {
				g2.dispose();
			}
			if (source.isJsonPlanShowingError()) {
				return false;
			}
			return hasGeneratedPlanValues();
		} finally {
			planSourceOverride = null;
			suppressDiagramBusyCursor = false;
		}
	}

	private PlanSourcePanel resolvePlanSourcePanel() {
		if (planSourceOverride != null) {
			return planSourceOverride;
		}
		return (PlanSourcePanel) objCurrentPlanTabs.getComponentAt(0);
	}

	private static boolean isPlanDiagramConstructionAction(String buttonClicked) {
		return PlanSourcePanel.SHOW_JSON_PLAN_DIAGRAM.equals(buttonClicked)
				|| PlanSourcePanel.EXECUTE_AND_SHOW_PLAN_DIAGRAM.equals(buttonClicked);
	}

	public void paintComponent(Graphics g) { // what's the difference between paint and paintComponent?
		boolean bolShowSQL = false;
		boolean bolShowProperties = false;
		boolean bolShowRetrievedJsonPlan = false;
		boolean bolShowFunctionName = false;
		boolean bolShowTiming = false;
		JsonObject jsonPGDatabaseMetadata;
		JsonArray jsnPGConfigurationSettings;
		JsonObject jsnPGConfigurationSetting;
		String strPostgreSQLEnvironment = "";
		String strSQL;
		String planText = "";
		String strPlanSourceButtonClicked;
		String strPlanName;
		Object jsonObjectList = null;
		JsonArray jsonArray = null;
		boolean bolExceptionOccurred;
		String strError;
		boolean busyCursorShown = false;

		if (bolShowTiming == true) { System.out.println("before PlanGraphPanel.paintComponent(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		super.paintComponent(g);

		if (bolShowFunctionName == true) { System.out.println("called paintComponent(Graphics g)"); }

		PlanSourcePanel objComponent = resolvePlanSourcePanel();
		strPlanSourceButtonClicked = objComponent.getPlanSourceButtonClicked();

		boolean forceExecuteRebuild = PlanSourcePanel.EXECUTE_AND_SHOW_PLAN_DIAGRAM.equals(strPlanSourceButtonClicked);
		boolean showJsonAction = PlanSourcePanel.SHOW_JSON_PLAN_DIAGRAM.equals(strPlanSourceButtonClicked);
		boolean jsonNeedsRebuild = showJsonAction && objComponent.shouldRebuildDiagramFromCurrentJson();
		if (!suppressDiagramBusyCursor
				&& (forceExecuteRebuild || jsonNeedsRebuild || objComponent.getPlanTextIsDirty())) {
			setDiagramBusyCursor(this, true);
			busyCursorShown = true;
		}

		try {
		
		if (strPlanSourceButtonClicked != null) {
			
			if (bolShowTiming == true) { System.out.println("before generate a new plan name(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			strPlanName = objComponent.getPlanSourceFromQueryPlanNameField();

			// Conflict name if plan_name already exists on another row: "Name (N)"
			strSQL = "select plan_name from explain_plans where id_explain_plan <> " + idExplainPlan;
			java.util.List<String> existingNames = new java.util.ArrayList<>();
			JsonArray nameRows = Database.rows(strSQL);
			if (nameRows != null) {
				for (int i = 0; i < nameRows.size(); i++) {
					Object value = ((JsonObject) nameRows.get(i)).get("plan_name");
					if (value != null) {
						existingNames.add(String.valueOf(value));
					}
				}
			}
			String newPlanName = FormPanel.nextCopyName(strPlanName, existingNames);
			if (newPlanName == null)
				newPlanName = "";
			
			if (bolShowTiming == true) { System.out.println("after generate a new plan name(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// write fields to database
			// then get the plan name again, just in case it changed
		
			Database.updateExplainPlan(idExplainPlan, strPlanName, newPlanName,
					objComponent.getPlanSourceFromQueryServerField(),
					objComponent.getPlanSourceFromQueryPortField(),
					objComponent.getPlanSourceFromQueryDatabaseField(),
					objComponent.getPlanSourceFromQueryUserField(),
					objComponent.getPlanSourceFromQueryPasswordField(),
					objComponent.getPlanSourceFromQueryRememberPasswordCheckbox(),
					objComponent.getAuthenticationTypeCode(),
					objComponent.getPlanSourceFromQueryPreCommandField(),
					objComponent.getSQLQueryText(),
					objComponent.getPlanSourceFromQueryPostCommandField(),
					objComponent.getJsonPlanText());

			strSQL = "select plan_name from explain_plans where id_explain_plan = " + idExplainPlan + ";\n";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			strPlanName = Database.row(strSQL).getString("plan_name");

			if (bolShowSQL == true) { System.out.println(strSQL); }
			
		}
		
		// get json text from sql command
		if (strPlanSourceButtonClicked != null && strPlanSourceButtonClicked.equals(PlanSourcePanel.EXECUTE_AND_SHOW_PLAN_DIAGRAM)) {
			// pg_init(String strServer, String strPort, String strDatabase, String strUser, String strPassword, String strPreCommand) 
			//Database.pg_init();
			List<Object> objPGInit = Database.pg_init(objComponent.getPlanSourceFromQueryServerField()
				, objComponent.getPlanSourceFromQueryPortField()
				, objComponent.getPlanSourceFromQueryDatabaseField()
				, objComponent.getPlanSourceFromQueryUserField()
				, objComponent.getPlanSourceFromQueryPasswordField()
				, objComponent.getPlanSourceFromQueryPreCommandField()
			);

			//List<Boolean, String, String> objPGGetPlan = Database.pg_getPlan(strSQL);
			bolExceptionOccurred = (boolean) objPGInit.get(0);
			strError = (String) objPGInit.get(1);
			if (bolExceptionOccurred == true) {
				objComponent.showJsonPlanExecutionError(strError);
				return;
			}

			// get just version string, like 9.6.5 or 12.0, then format it like 9.0605 or 12.0000
			strSQL = "select version() as pg_version;\n";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			jsonPGDatabaseMetadata = Database.pg_row(strSQL);
			String strPGVersionEnvironment = jsonPGDatabaseMetadata.getString("pg_version");
			String strPGVersionNumber = jsonPGDatabaseMetadata.getString("pg_version");
			strPGVersionNumber = strPGVersionNumber.replace("PostgreSQL", "");
			strPGVersionNumber = strPGVersionNumber.replaceAll("on.+", "");
			strPGVersionNumber = strPGVersionNumber.replaceAll(" ", "");
			strPGVersionNumber = strPGVersionNumber.replaceAll("\\(.+", "");
			strPGVersionNumber = Utils.formatPGVersionNumber(strPGVersionNumber);

			// get pg query and resource settings
			strPostgreSQLEnvironment = "\"PostgreSQL Environment\": {\n";
			strPostgreSQLEnvironment += "\"Version Number\": " + strPGVersionNumber + "\n";
			strPostgreSQLEnvironment += ", \"Version Environment\": \"" + strPGVersionEnvironment.replace("\"", "\\") + "\"\n"; 
			strPostgreSQLEnvironment += ", \"Query\": \"" + objComponent.getSQLQueryText().replace("\"", "\\") + "\"\n";
			strSQL = "select name as setting\n" + 
					",case\n" + 
					"  when unit = '8kB' then pg_size_pretty(cast(setting as bigint) * 8192)\n" + 
					"  else setting || coalesce(' ' || unit, '')\n" + 
					"end as setting_value\n" + 
					"from pg_settings\n" + 
					"where category like 'Query%' or category like 'Resource%'\n" + 
					"order by name\n" + 
					"";
			jsnPGConfigurationSettings = Database.pg_rows(strSQL);
			if (jsnPGConfigurationSettings != null) {
				//strSQL = "";
				for (int intRow = 0; intRow < jsnPGConfigurationSettings.size(); intRow++) {
					jsnPGConfigurationSetting = (JsonObject) jsnPGConfigurationSettings.get(intRow);
					strPostgreSQLEnvironment += "\n, \"" + jsnPGConfigurationSetting.getString("setting") + "\": ";
					strPostgreSQLEnvironment += "\"" + jsnPGConfigurationSetting.getString("setting_value") + "\"";
				}
			}
			
			strPostgreSQLEnvironment += "\n}\n";
			
			strSQL = objComponent.getSQLQueryText();
			//planText = Database.pg_getPlan(strSQL);
			List<Object> objPGGetPlan = Database.pg_getPlan(strSQL);
			//List<Boolean, String, String> objPGGetPlan = Database.pg_getPlan(strSQL);
			bolExceptionOccurred = (boolean) objPGGetPlan.get(0);
			strError = (String) objPGGetPlan.get(1);
			String rawPlanFromServer = (String) objPGGetPlan.get(2);
			if (bolExceptionOccurred == true) {
				objComponent.showJsonPlanExecutionError(strError);
				return;
			}
			objComponent.clearJsonPlanErrorStyle();
			// Keep raw server result (JSON or text) in the plan field / SQLite.
			objComponent.setJsonPlanText(rawPlanFromServer);
			
			List<Object> objPGRunDML = Database.pg_dml(objComponent.getPlanSourceFromQueryPostCommandField(), ";");
			bolExceptionOccurred = (boolean) objPGRunDML.get(0);
			strError = (String) objPGRunDML.get(1);
			if (bolExceptionOccurred == true) {
				objComponent.showJsonPlanExecutionError(strError);
				return;
			}

			PlanFormat executedFormat = PlanSourcePanel.sniffPlanFormat(rawPlanFromServer);
			if (executedFormat == PlanFormat.JSON) {
				// Preserve legacy JSON behavior: embed environment metadata in stored JSON.
				planText = rawPlanFromServer.replace("\"Plan\": {", strPostgreSQLEnvironment + ", \"Plan\": {");
				objComponent.setJsonPlanText(planText);
			} else {
				// Text plans stay raw in the field; convert on the fly for the diagram pipeline.
				planText = rawPlanFromServer;
			}

			Database.updateExplainPlanText(idExplainPlan, objComponent.getJsonPlanText());

			if (bolShowRetrievedJsonPlan == true) { System.out.println(planText); }
			
			// fill json field with plan text
			//objComponent.setJsonPlanText(planText); // already set earlier
			objComponent.setShowPlanGraphButton(true);

			// parse the whole plan
			objComponent.setPlanTextIsDirty(true);
			
			objComponent.setPlanSourceButtonClicked(null);
		}

		// get json text from textbox — rebuild only when plan content changed since last diagram
		if (strPlanSourceButtonClicked != null && strPlanSourceButtonClicked.equals(PlanSourcePanel.SHOW_JSON_PLAN_DIAGRAM)) {
			String fingerprint = PlanSourcePanel.normalizeJsonPlanText(objComponent.getJsonPlanText());
			
			if (bolShowRetrievedJsonPlan == true) { System.out.println(fingerprint); }
			
			if (objComponent.shouldRebuildDiagramFromJson(fingerprint)) {
				objComponent.setPlanTextIsDirty(true);
			}
			planText = "";
			
			objComponent.setPlanSourceButtonClicked(null);
		}

		if (objComponent.getPlanTextIsDirty() == true) {
			String fingerprint = PlanSourcePanel.normalizeJsonPlanText(objComponent.getJsonPlanText());
			// Stale dirty from editors: skip full rebuild when field content is unchanged (Execute always rebuilds)
			if (!forceExecuteRebuild && !objComponent.shouldRebuildDiagramFromJson(fingerprint)) {
				objComponent.setPlanTextIsDirty(false);
			}
		}

		boolean rebuiltPlanData = false;
		if (objComponent.getPlanTextIsDirty() == true) {
			operatorCount = -1;
			rebuiltPlanData = true;

			if (bolShowTiming == true) { System.out.println("before delete stuff: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

			strSQL = "delete from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
			
			strSQL = "insert into explain_plan_generated_values (id_explain_plan, is_active) values (" + idExplainPlan + ", 0)";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
			
			strSQL = "delete from explain_plan_properties where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);

			strSQL = "delete from explain_plan_operator_annotations where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
			
			strSQL = "delete from explain_plan_operator_workers where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);

			strSQL = "delete from explain_plan_operator_show_workers where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);

			strSQL = "delete from explain_plan_operator_outputs where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);

			if (bolShowTiming == true) { System.out.println("after delete stuff: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

			try {
				String rawField = objComponent.getJsonPlanText();
				planText = PlanSourcePanel.resolvePlanFieldToJson(rawField);
				// For text plans executed this turn, optionally stamp environment onto the
				// diagram-only JSON (field remains raw text).
				if (forceExecuteRebuild && PlanSourcePanel.sniffPlanFormat(rawField) == PlanFormat.TEXT
						&& planText != null && planText.contains("\"Plan\"")) {
					planText = planText.replace("\"Plan\":{", strPostgreSQLEnvironment + ", \"Plan\":{");
					planText = planText.replace("\"Plan\": {", strPostgreSQLEnvironment + ", \"Plan\": {");
				}
			} catch (com.williammeitzen.peps.model.ExplainTextPlanConverter.ExplainTextPlanConvertException convertEx) {
				objComponent.showJsonPlanExecutionError(
						"Could not convert text plan to JSON for the diagram:\n" + convertEx.getMessage());
				rebuiltPlanData = false;
				return;
			}

			if (bolShowProperties == true) { System.out.println(planText); }
				
			if (bolShowTiming == true) { System.out.println("before generateExplainPlanPropertiesData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

			try {
				jsonArray = (JsonArray) Jsoner.deserialize(planText);
			} catch (DeserializationException e) {
			}
			
			if (jsonObjectList != null || jsonArray != null) {
				strSQL = generateExplainPlanPropertiesData(jsonArray, idExplainPlan, -1);
				// commands in a transaction block execute faster
				if (bolShowSQL == true) { System.out.println(strSQL); }
				strSQL = "begin transaction;\n" + strSQL + "commit;\n";
				Database.dml(strSQL);
			} else {
				rebuiltPlanData = false;
				return;
			}
			
			if (bolShowTiming == true) { System.out.println("after generateExplainPlanPropertiesData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

			// copy data from explain_plan_properties to explain_plan_operator_annotations, explain_plan_operator_workers, explain_plan_operator_outputs, explain_plans
			if (bolShowTiming == true) { System.out.println("before generateAnnotationsData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			generateAnnotationsData(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after generateAnnotationsData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// update explain_plan_operator_annotations so InitPlans appear as new operators
			if (bolShowTiming == true) { System.out.println("before nestInitPlansWithinAnnotationsTable: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			nestInitPlansWithinAnnotationsTable(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after nestInitPlansWithinAnnotationsTable: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// generate initial cartesian coordinates
			if (bolShowTiming == true) { System.out.println("before generateInitialCartesianData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			generateInitialCartesianData(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after generateInitialCartesianData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// correct x_cart and y_cart (not x_rect and y_rect),
			if (bolShowTiming == true) { System.out.println("before updateLayout: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateLayout(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateLayout: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// update explain_plan data, like total cost, total time, ram used, disk used
			if (bolShowTiming == true) { System.out.println("before updateExplainPlanNumericData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			setVerboseSetting(idExplainPlan); // 1
			if (bolShowTiming == true) { System.out.println("after updateExplainPlanNumericData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before setPerOperatorTotalsForPLAndWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			setPerOperatorTotalsForPLAndWorkers(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after setPerOperatorTotalsForPLAndWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

/*
			// set total execution time or cost slider
			if (getSliderType() == SliderPanel.SLIDER_TYPE_TIME) {
				setTotalExecutionTimeSlider(idExplainPlan);
			} else if (getSliderType() == SliderPanel.SLIDER_TYPE_COST) {
				setTotalExecutionCostSlider(idExplainPlan);
			} else {
				System.out.println("Error in PlanGraphPanel.java: Unknown slider type");
			}
*/
			// update explain_plan_operator_annotations with calculated data, like
  			// cost percentage, time percentage, ram percentage, disk percentage
  			// also parallel leaders
			if (bolShowTiming == true) { System.out.println("before updateRowAndMassCalcuations: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateRowAndMassCalculations(idExplainPlan); // 2
			if (bolShowTiming == true) { System.out.println("after updateRowAndMassCalcuations: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateStringUnitsPercentageAnnotationsTextData(idExplainPlan); // 2
			if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateButtonsToEnable: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateButtonsToEnable(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateButtonsToEnable: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// (NEW PLACE) set total execution time or cost slider
			if (getSliderType() == SliderPanel.SLIDER_TYPE_TIME) {
				setTotalExecutionTimeSlider(idExplainPlan);
			} else if (getSliderType() == SliderPanel.SLIDER_TYPE_COST) {
				setTotalExecutionCostSlider(idExplainPlan);
			} else {
				System.out.println("Error in PlanGraphPanel.java: Unknown slider type");
			}

			// if workers are involved, the operator is considered a parallel leader. see how much work the leader's doing, if any. 
			if (bolShowTiming == true) { System.out.println("before setOperatorParallelLeadersAnnotationsTextData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			generateParallelLeadersAnnotationsData(idExplainPlan); // 3
			if (bolShowTiming == true) { System.out.println("after setOperatorParallelLeadersAnnotationsTextData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
		}
		
		if (objComponent.getPlanTextIsDirty() == true || objComponent.getLineThicknessByIsDirty() == true || objComponent.getZoomPercentageIsDirty() == true) {
			// write all possible line thickness (arrow) text, and thickness size numbers
			if (bolShowTiming == true) { System.out.println("before updateLineThicknessTextData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateLineThicknessTextData(idExplainPlan); // 4
			if (bolShowTiming == true) { System.out.println("after updateLineThicknessTextData: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		}
	
		if (objComponent.getPlanTextIsDirty() == true) {
			if (bolShowTiming == true) { System.out.println("before copyPropertiesForSouth: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			copyPropertiesForSouth(idExplainPlan); // 5
			if (bolShowTiming == true) { System.out.println("after copyPropertiesForSouth: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before copyPropertiesForWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			copyPropertiesForWorkers(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after copyPropertiesForWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before copyPropertiesForParallelLeaders: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			copyPropertiesForParallelLeaders(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after copyPropertiesForParallelLeaders: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before calculatePercentagesBetweenWorkersAndParallelLeaders: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			calculatePercentagesBetweenWorkersAndParallelLeaders(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after calculatePercentagesBetweenWorkersAndParallelLeaders: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

			if (bolShowTiming == true) { System.out.println("before setHighlightFlagsFromWorkerAndParallelLeaderPercentageStrings: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			setHighlightFlagsFromWorkerAndParallelLeaderPercentageStrings(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after setHighlightFlagsFromWorkerAndParallelLeaderPercentageStrings: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateWorkerLabelStrings: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateWorkerLabelStrings(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateWorkerLabelStrings: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateParallelLeaderLabelStrings: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateParallelLeaderLabelStrings(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateParallelLeaderLabelStrings: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		}
		
		if (objComponent.getPercentageByIsDirty() == true || objComponent.getPlanTextIsDirty() == true || objComponent.getZoomPercentageIsDirty() == true) {
	
			if (bolShowTiming == true) { System.out.println("before updateFontMetricsNorthOptionsWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateFontMetricsNorthOptionsWidthAndHeight(idExplainPlan); // 9
			if (bolShowTiming == true) { System.out.println("after updateFontMetricsNorthOptionsWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		}
					
		if (objComponent.getPlanTextIsDirty() == true || objComponent.getZoomPercentageIsDirty() == true) {
	
			if (bolShowTiming == true) { System.out.println("before updateFontMetricsSouthWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateFontMetricsSouthWidthAndHeight(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateFontMetricsSouthWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateFontMetricsArrowOptionsWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateFontMetricsArrowOptionsWidthAndHeight(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateFontMetricsArrowOptionsWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateFontMetricWorkersWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateFontMetricWorkersWidthAndHeight(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateFontMetricWorkersWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			if (bolShowTiming == true) { System.out.println("before updateFontMetricParallelLeadersWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updateFontMetricParallelLeadersWidthAndHeight(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updateFontMetricParallelLeadersWidthAndHeight: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
		}

		if (objComponent.getPercentageByIsDirty() == true || objComponent.getPlanTextIsDirty() == true || objComponent.getZoomPercentageIsDirty() == true) {
			
			// copy text, fontmetrics, and highlight from menu settings to north
			if (bolShowTiming == true) { System.out.println("before copyPercentageByForNorth1: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			copyPercentageByForNorth1(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after copyPercentageByForNorth1: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		}

		if (objComponent.getLineThicknessByIsDirty() == true || objComponent.getPlanTextIsDirty() == true || objComponent.getZoomPercentageIsDirty() == true) {
		
			if (bolShowTiming == true) { System.out.println("before copyLineThicknessByForArrow: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			copyLineThicknessByForArrow(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after copyLineThicknessByForArrow: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		}
		
		if (objComponent.getLineThicknessByIsDirty() == true || objComponent.getPercentageByIsDirty() == true || objComponent.getPlanTextIsDirty() == true || objComponent.getWorkerIsDirty() == true || objComponent.getZoomPercentageIsDirty() == true) {
			
			// write box widths, operator widths, column widths, and include toggled worker fontmetrics width info 
			if (bolShowTiming == true) { System.out.println("before updatePerColumnOperatorAndWorkerWidths: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updatePerColumnOperatorAndWorkerWidths(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updatePerColumnOperatorAndWorkerWidths: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// write box heights, operator heights, column heights, and include toggled worker fontmetrics height info 
			if (bolShowTiming == true) { System.out.println("before updatePerRowOperatorAndWorkerHeights: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			updatePerRowOperatorAndWorkerHeights(idExplainPlan);
			if (bolShowTiming == true) { System.out.println("after updatePerRowOperatorAndWorkerHeights: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
			
			// reset clickable areas (to show properties or expand workers)
			// otherwise zooming in or out may show properties of the wrong operator
			clearClickableShapes();
		}
		
		// activate line thickness by buttons
		//if (objComponent.getPlanTextIsDirty() == true) {
		//	enableLineThicknessByAndPercentageByButtons(idExplainPlan);
		//} 
		
		// the plan's clean now, so minimize work to draw plan on screen
		boolean percentageByWasDirty = objComponent.getPercentageByIsDirty();
		if (rebuiltPlanData) {
			// Fingerprint the raw plan field (JSON or text), not the diagram-only converted JSON.
			objComponent.markDiagramRenderedFromJson(
					PlanSourcePanel.normalizeJsonPlanText(objComponent.getJsonPlanText()));
		}
		objComponent.setPlanTextIsDirty(false);
		objComponent.setLineThicknessByIsDirty(false);
		objComponent.setZoomPercentageIsDirty(false);
		objComponent.setWorkerIsDirty(false);
		objComponent.setPercentageByIsDirty(false);

		repairMissingNorthLabelsIfNeeded(idExplainPlan);

		if (bolShowTiming == true) { System.out.println("before drawOperations: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		int priorX = intXDimension;
		int priorY = intYDimension;
		drawOperations(g, idExplainPlan);
		if (bolShowTiming == true) { System.out.println("after drawOperations: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		// Preferred size is computed during draw; keep scrollbars in sync when it changes.
		if (priorX != intXDimension || priorY != intYDimension) {
			SwingUtilities.invokeLater(this::revalidateEnclosingScrollPane);
		}
		if (scrollSelectedIntoViewAfterPaint) {
			scrollSelectedIntoViewAfterPaint = false;
			SwingUtilities.invokeLater(this::ensureSelectedOperatorVisible);
		}
		// Properties grid can go stale after rebuild or Percentage By change.
		if (rebuiltPlanData || percentageByWasDirty) {
			SwingUtilities.invokeLater(this::refreshSelectedOperatorProperties);
		}

		if (bolShowTiming == true) { System.out.println("after PlanGraphPanel.paintComponent(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		} finally {
			if (busyCursorShown) {
				setDiagramBusyCursor(this, false);
			}
		}
	}

	public String generateExplainPlanPropertiesData(Object object, int intIDExplainPlan, int parentOperatorCount) {
		JsonObject jsonObject = null;
		JsonArray jsonArray = null;
		String strSQL = "";
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		boolean bolShowFinalSQL = false;

		Object objKeyName;
		String strKeyName;
		Object objInitPlanKeyName;
		String strInitPlanKeyName;
		//Object keyValue;
		Object objKeyValue;
		String strKeyValue;
		Object objInitPlanKeyValue;
		String strInitPlanKeyValue;
		
		if (object instanceof JsonObject) {
			jsonObject = (JsonObject) object;
		}

		if (object instanceof JsonArray) {
			jsonArray = (JsonArray) object;
		}

		if (object instanceof JsonObject) {

			for (Object key: jsonObject.keySet()) {
				operatorCount++;
				strKeyName = (String) key;
				objKeyValue = jsonObject.get(strKeyName);
			    //if (object.get(key) instanceof JsonObject) {
					// do something with jsonObject here
				if (objKeyValue instanceof JsonObject) {
					if (bolShowProperties == true) { System.out.print("---(" + parentOperatorCount + ", " + operatorCount + "): " + strKeyName + "\n"); }
					strSQL += "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name) values (" + intIDExplainPlan + ", " + parentOperatorCount + ", " + operatorCount + ", '" + strKeyName + "'); -- 6\n" + generateExplainPlanPropertiesData(objKeyValue, intIDExplainPlan, operatorCount);
					if (bolShowSQL == true) { System.out.println(strSQL); }
					//return strSQL;
					//db.dml(strSQL);
					//writeExplainPlan(objKeyValue, operatorCount);
				} else if (objKeyValue instanceof JsonArray) {
					if (bolShowProperties == true) { System.out.print("--(" + parentOperatorCount + ", " + operatorCount + "): " + strKeyName + "\n"); }
					strSQL += "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name) values (" + intIDExplainPlan + ", " + parentOperatorCount + ", " + operatorCount + ", '" + strKeyName + "'); -- 7\n" + generateExplainPlanPropertiesData(objKeyValue, intIDExplainPlan, operatorCount);
					if (bolShowSQL == true) { System.out.println(strSQL); }

				} else if (objKeyValue == null) {
					// JSON null (e.g. Values Scan "Relation Name": null) — store SQL NULL, not the word "null".
					strSQL += "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name, operator_value) values (" + intIDExplainPlan + ", " + parentOperatorCount + ", " + operatorCount + ", '" + strKeyName + "', null); -- 8n\n";
					if (bolShowSQL == true) { System.out.println(strSQL); }
				} else {
					strKeyValue = objKeyValue.toString().replace("'", "''"); // escape single quotes
					if (bolShowProperties == true) { System.out.print("-(" + parentOperatorCount + ", " + operatorCount + "): " + strKeyName + " "); }
					if (bolShowProperties == true) { System.out.println(": " + strKeyValue); }
					strSQL += "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name, operator_value) values (" + intIDExplainPlan + ", " + parentOperatorCount + ", " + operatorCount + ", '" + strKeyName + "', '" + strKeyValue + "'); -- 8\n";
					if (bolShowSQL == true) { System.out.println(strSQL); /* System.exit(0);*/ }
					//db.dml(strSQL);
					//return strSQL;
 				}
			
			}
		} else if (object instanceof JsonArray) {

			for (int intElement = 0; intElement < jsonArray.size(); intElement++) {
				operatorCount++;
				objKeyName = jsonArray.get(intElement);
				if (objKeyName instanceof JsonArray) {
					if (bolShowProperties == true) { System.out.println("~(" + parentOperatorCount + ", " + operatorCount + "): \n JsonArray (13): " + object.getClass().getSimpleName()); }
					//writeExplainPlan(jsonArray.get(intElement), operatorCount);
					return generateExplainPlanPropertiesData(jsonArray.get(intElement), intIDExplainPlan, operatorCount);
				} else if (objKeyName instanceof JsonObject) {
					if (bolShowProperties == true) { System.out.println("~~(" + parentOperatorCount + ", " + operatorCount + "): P_CONTAINER\n JsonObject (14): " + object.getClass().getSimpleName()); }
					strSQL += "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name) values (" + intIDExplainPlan + ", " + parentOperatorCount + ", " + operatorCount + ", 'P_CONTAINER'); -- 4\n" + generateExplainPlanPropertiesData(jsonArray.get(intElement), intIDExplainPlan, operatorCount);
					if (bolShowSQL == true) { System.out.println(strSQL); }
					//db.dml(strSQL);
					//writeExplainPlan(jsonArray.get(intElement), operatorCount);
					//return strSQL;
				} else {
					//strKeyName = objKeyName.toString();
					//strKeyName = strKeyName.replace("'", "''"); // escape single quotes
					strKeyName = objKeyName.toString().replace("'", "''"); // escape single quotes
					//System.out.println("in JsonArray loop, but unknown object");
					if (bolShowProperties == true) { System.out.println("~~~(" + parentOperatorCount + ", " + operatorCount + "): " + strKeyName + " "); }
					strSQL += "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name) values (" + intIDExplainPlan + ", " + parentOperatorCount + ", " + operatorCount + ", '" + strKeyName + "'); -- 5\n" + generateExplainPlanPropertiesData(jsonArray.get(intElement), intIDExplainPlan, operatorCount);
					//System.out.println(": " + keyValue);
					if (bolShowSQL == true) { System.out.println(strSQL); }
					//db.dml(strSQL);
					//return strSQL;
				}
			}
		} else {
			if (bolShowProperties == true) { System.out.println("object.getClass().getSimpleName() (12): " + object.getClass().getSimpleName()); }
		}

		if (bolShowFinalSQL == true) { System.out.println(strSQL); }
		return strSQL;
		
	}
	
	// REFACTOR - copy data from explain_plan_properties to explain_plan_operator_annotations
	public void generateAnnotationsData(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		boolean bolShowWorkerSQL = false;
		boolean bolShowInitPlanSQL = false;
		int intParentIDExplainPlanProperty;
		Integer /*intPARParentIDExplainPlanProperty,*/ intPARPARParentIDExplainPlanProperty;
		String strPAROperatorName, strPARPAROperatorName;
		JsonArray jsnOperatorRows;
		JsonArray jsnPropertyRows;
		//String str
		String strColumnNames;
		String strValue;
		String strValues;
		String strAnnotationsSQL;
		
		strSQL = "select parent_id_explain_plan_property\n"
				+ "from explain_plan_properties\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				//+ "and parent_id_explain_plan_property = 45\n" // output operator, remove later
				+ "group by parent_id_explain_plan_property\n"
				+ "order by parent_id_explain_plan_property\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnOperatorRows = Database.rows(strSQL);
		strAnnotationsSQL = "";
		if (jsnOperatorRows != null) {
			for (int intOperatorRow = 0; intOperatorRow < jsnOperatorRows.size(); intOperatorRow++) {
				intParentIDExplainPlanProperty = (int) ((JsonObject) jsnOperatorRows.get(intOperatorRow)).get("parent_id_explain_plan_property");
				strSQL = "select\n" + 
						"EPP.id_explain_plan\n" + 
						",PAR.parent_id_explain_plan_property as par_parent_id_explain_plan_property\n" + 
						",PAR_PAR.parent_id_explain_plan_property as par_par_parent_id_explain_plan_property\n" + 
						",PAR_PAR_PAR.parent_id_explain_plan_property as par_par_par_parent_id_explain_plan_property\n" + 
						",PAR.id_explain_plan_property as par_id_explain_plan_property\n" + 
						",PAR.operator_name as par_operator_name\n" + 
						",PAR_PAR.operator_name as par_par_operator_name\n" + 
						",EPP.parent_id_explain_plan_property\n" + 
						",EPP.id_explain_plan_property\n" + 
						",EPP.operator_name\n" + 
						",EPP.operator_value\n" + 
						"from explain_plan_properties as EPP\n" + 
						"left outer join explain_plan_properties as PAR on EPP.parent_id_explain_plan_property = PAR.id_explain_plan_property\n" + 
						"  and EPP.id_explain_plan = PAR.id_explain_plan\n" + 
						"left outer join explain_plan_properties as PAR_PAR on PAR.parent_id_explain_plan_property = PAR_PAR.id_explain_plan_property\n" + 
						"  and PAR.id_explain_plan = PAR_PAR.id_explain_plan\n" + 
						"left outer join explain_plan_properties as PAR_PAR_PAR on PAR_PAR.parent_id_explain_plan_property = PAR_PAR_PAR.id_explain_plan_property\n" + 
						"  and PAR_PAR.id_explain_plan = PAR_PAR_PAR.id_explain_plan\n" + 
						"where EPP.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
						"and EPP.parent_id_explain_plan_property = " + intParentIDExplainPlanProperty + " -- parent_id_explain_plan_property\n" + 
						"";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				if (intParentIDExplainPlanProperty >= 518 && bolShowInitPlanSQL == true) { System.out.println(strSQL); }
				jsnPropertyRows = Database.rows(strSQL);

				Map<String, String> mapAnnotations = new HashMap<String, String>();
				Map<String, String> mapWorkers = new HashMap<String, String>();
				
				strPAROperatorName = null;
				//if ((String) ((JsonObject) jsnOperatorRows.get(0)).get("par_operator_name")).equals("Output")) {
				if (((JsonObject) jsnPropertyRows.get(0)).get("par_operator_name") != null) {
					strPAROperatorName = (String) ((JsonObject) jsnPropertyRows.get(0)).get("par_operator_name");
					//if (intParentIDExplainPlanProperty >= 518 && bolShowInitPlanSQL == true) { System.out.println("intParentIDExplainPlanProperty: " + intParentIDExplainPlanProperty + ", strPAROperatorName: " + strPAROperatorName); }
				}

				strPARPAROperatorName = null;
				//if ((String) ((JsonObject) jsnOperatorRows.get(0)).get("par_operator_name")).equals("Output")) {
				if (((JsonObject) jsnPropertyRows.get(0)).get("par_par_operator_name") != null) {
					strPARPAROperatorName = (String) ((JsonObject) jsnPropertyRows.get(0)).get("par_par_operator_name");
					//if (intParentIDExplainPlanProperty >= 518 && bolShowInitPlanSQL == true) { System.out.println("intParentIDExplainPlanProperty: " + intParentIDExplainPlanProperty + ", strPARPAROperatorName: " + strPARPAROperatorName); }
				}

				//if (operator_value_where_operator_name_is_r(jsnPropertyRows, "Node Type") != null) {
				if (operator_value_where_operator_name_is(jsnPropertyRows, "Node Type") != null || intParentIDExplainPlanProperty == -1) {

					// row for explain_plan_operator_annotations table

					mapAnnotations.put("actual_loops", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Loops"));
					mapAnnotations.put("actual_rows", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Rows"));
					mapAnnotations.put("actual_startup_time_ms", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Startup Time"));
					mapAnnotations.put("actual_total_time_ms", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Total Time"));
					mapAnnotations.put("alias", operator_value_where_operator_name_is(jsnPropertyRows, "Alias"));
					mapAnnotations.put("append", operator_value_where_operator_name_is(jsnPropertyRows, "Append"));
					mapAnnotations.put("cache_evictions", operator_value_where_operator_name_is(jsnPropertyRows, "Cache Evictions"));
					mapAnnotations.put("cache_hits", operator_value_where_operator_name_is(jsnPropertyRows, "Cache Hits"));
					mapAnnotations.put("cache_key", operator_value_where_operator_name_is(jsnPropertyRows, "Cache Key"));
					mapAnnotations.put("cache_misses", operator_value_where_operator_name_is(jsnPropertyRows, "Cache Misses"));
					mapAnnotations.put("cache_mode", operator_value_where_operator_name_is(jsnPropertyRows, "Cache Mode"));
					mapAnnotations.put("cache_overflows", operator_value_where_operator_name_is(jsnPropertyRows, "Cache Overflows"));
					mapAnnotations.put("command", operator_value_where_operator_name_is(jsnPropertyRows, "Command"));
					mapAnnotations.put("cte_name", operator_value_where_operator_name_is(jsnPropertyRows, "CTE Name"));
					mapAnnotations.put("estimated_capacity", operator_value_where_operator_name_is(jsnPropertyRows, "Estimated Capacity"));
					mapAnnotations.put("estimated_distinct_lookup_keys", operator_value_where_operator_name_is(jsnPropertyRows, "Estimated Distinct Lookup Keys"));
					mapAnnotations.put("estimated_hit_percent", operator_value_where_operator_name_is(jsnPropertyRows, "Estimated Hit Percent"));
					mapAnnotations.put("estimated_lookups", operator_value_where_operator_name_is(jsnPropertyRows, "Estimated Lookups"));
					mapAnnotations.put("execution_time", operator_value_where_operator_name_is(jsnPropertyRows, "Execution Time"));
					mapAnnotations.put("filter", operator_value_where_operator_name_is(jsnPropertyRows, "Filter"));
					mapAnnotations.put("function_call", operator_value_where_operator_name_is(jsnPropertyRows, "Function Call"));
					mapAnnotations.put("hash_batches", operator_value_where_operator_name_is(jsnPropertyRows, "Hash Batches"));
					mapAnnotations.put("hash_buckets", operator_value_where_operator_name_is(jsnPropertyRows, "Hash Buckets"));
					mapAnnotations.put("hash_cond", operator_value_where_operator_name_is(jsnPropertyRows, "Hash Cond"));
					mapAnnotations.put("index_cond", operator_value_where_operator_name_is(jsnPropertyRows, "Index Cond"));
					mapAnnotations.put("index_name", operator_value_where_operator_name_is(jsnPropertyRows, "Index Name"));
					mapAnnotations.put("initplan", operator_value_where_operator_name_is(jsnPropertyRows, "InitPlan"));
					mapAnnotations.put("io_read_time", operator_value_where_operator_name_is(jsnPropertyRows, "I/O Read Time"));
					mapAnnotations.put("io_write_time", operator_value_where_operator_name_is(jsnPropertyRows, "I/O Write Time"));
					mapAnnotations.put("join_filter", operator_value_where_operator_name_is(jsnPropertyRows, "Join Filter"));
					mapAnnotations.put("join_type", operator_value_where_operator_name_is(jsnPropertyRows, "Join Type"));
					mapAnnotations.put("local_dirtied_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Dirtied Blocks"));
					mapAnnotations.put("local_hit_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Hit Blocks"));
					mapAnnotations.put("local_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Read Blocks"));
					mapAnnotations.put("local_written_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Written Blocks"));
					mapAnnotations.put("local_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Read Blocks"));
					mapAnnotations.put("merge_cond", operator_value_where_operator_name_is(jsnPropertyRows, "Merge Cond"));
					mapAnnotations.put("node_type", operator_value_where_operator_name_is(jsnPropertyRows, "Node Type"));
					mapAnnotations.put("operation", operator_value_where_operator_name_is(jsnPropertyRows, "Operation"));
					mapAnnotations.put("one_time_filter", operator_value_where_operator_name_is(jsnPropertyRows, "One-Time Filter"));
					mapAnnotations.put("parallel_aware", operator_value_where_operator_name_is(jsnPropertyRows, "Parallel Aware"));
					mapAnnotations.put("parent_relationship", operator_value_where_operator_name_is(jsnPropertyRows, "Parent Relationship"));
					mapAnnotations.put("partial_mode", operator_value_where_operator_name_is(jsnPropertyRows, "Partial Mode"));
					mapAnnotations.put("peak_memory_usage", operator_value_where_operator_name_is(jsnPropertyRows, "Peak Memory Usage"));
					mapAnnotations.put("planning_time", operator_value_where_operator_name_is(jsnPropertyRows, "Planning Time"));
					mapAnnotations.put("plan_rows", operator_value_where_operator_name_is(jsnPropertyRows, "Plan Rows"));
					mapAnnotations.put("plan_width", operator_value_where_operator_name_is(jsnPropertyRows, "Plan Width"));
					mapAnnotations.put("relation_name", operator_value_where_operator_name_is(jsnPropertyRows, "Relation Name"));
					mapAnnotations.put("rows_removed_by_join_filter", operator_value_where_operator_name_is(jsnPropertyRows, "Rows Removed by Join Filter"));
					mapAnnotations.put("rows_removed_by_filter", operator_value_where_operator_name_is(jsnPropertyRows, "Rows Removed by Filter"));
					mapAnnotations.put("rows_removed_by_index_recheck", operator_value_where_operator_name_is(jsnPropertyRows, "Rows Removed by Index Recheck"));
					mapAnnotations.put("repeatable_seed", operator_value_where_operator_name_is(jsnPropertyRows, "Repeatable Seed"));
					mapAnnotations.put("sampling_method", operator_value_where_operator_name_is(jsnPropertyRows, "Sampling Method"));
					mapAnnotations.put("scan_direction", operator_value_where_operator_name_is(jsnPropertyRows, "Scan Direction"));
					mapAnnotations.put("shared_dirtied_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Dirtied Blocks"));
					mapAnnotations.put("shared_hit_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Hit Blocks"));
					mapAnnotations.put("shared_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Read Blocks"));
					mapAnnotations.put("shared_written_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Written Blocks"));
					mapAnnotations.put("sort_method", operator_value_where_operator_name_is(jsnPropertyRows, "Sort Method"));
					mapAnnotations.put("sort_space_type", operator_value_where_operator_name_is(jsnPropertyRows, "Sort Space Type"));
					mapAnnotations.put("sort_space_used", operator_value_where_operator_name_is(jsnPropertyRows, "Sort Space Used"));
					mapAnnotations.put("startup_cost", operator_value_where_operator_name_is(jsnPropertyRows, "Startup Cost"));
					mapAnnotations.put("strategy", operator_value_where_operator_name_is(jsnPropertyRows, "Strategy"));
					mapAnnotations.put("subplan_name", operator_value_where_operator_name_is(jsnPropertyRows, "Subplan Name"));
					mapAnnotations.put("temp_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Temp Read Blocks"));
					mapAnnotations.put("temp_written_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Temp Written Blocks"));
					mapAnnotations.put("total_cost", operator_value_where_operator_name_is(jsnPropertyRows, "Total Cost"));
					mapAnnotations.put("total_runtime", operator_value_where_operator_name_is(jsnPropertyRows, "Total Runtime"));
					mapAnnotations.put("workers_launched", operator_value_where_operator_name_is(jsnPropertyRows, "Workers Launched"));
					mapAnnotations.put("workers_planned", operator_value_where_operator_name_is(jsnPropertyRows, "Workers Planned"));
					//intPARParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("par_parent_id_explain_plan_property");
					intPARPARParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("par_par_parent_id_explain_plan_property");
					strColumnNames = "id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property";
					//strValues = intIDExplainPlan + ", " + intPARParentIDExplainPlanProperty + ", " + intParentIDExplainPlanProperty;
					if (intParentIDExplainPlanProperty == -1) {
						strValues = intIDExplainPlan + ", null, " + intParentIDExplainPlanProperty;
						//mapAnnotations.put("node_type", "User");
					} else {
						strValues = intIDExplainPlan + ", " + intPARPARParentIDExplainPlanProperty + ", " + intParentIDExplainPlanProperty;
					}

					for (Map.Entry<String, String> entry : mapAnnotations.entrySet()) {
					    String key = entry.getKey();
					    if (strColumnNames != "") {
			    			strColumnNames += ", ";
					    }
					    strColumnNames += key;
					    Object value = entry.getValue();
					    if (strValues != "") {
				    		strValues += ", ";
					    }
					    if (value == null) {
				    		strValues += "null";
					    } else {
				    		strValue = (String) value;
				    		strValues += "'" + strValue.replace("'", "''") + "'";
					    }
					    
					}
					
					//if (intParentIDExplainPlanProperty >= 518 && bolShowInitPlanSQL == true) { System.out.println(strValues); System.out.println(strValues); }
					
					if (mapAnnotations.containsKey("node_type")) {
						strAnnotationsSQL += "-- Node Type: " + mapAnnotations.get("node_type") + "\n";
					}
					strAnnotationsSQL += "insert into explain_plan_operator_annotations (" + strColumnNames + ") values (" + strValues + ");\n";

				} else if (strPARPAROperatorName != null && strPARPAROperatorName.equals("Workers")) {

					// row for explain_plan_operator_workers table
					mapWorkers.put("actual_loops", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Loops"));
					mapWorkers.put("actual_rows", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Rows"));
					mapWorkers.put("actual_startup_time_ms", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Startup Time"));
					mapWorkers.put("actual_total_time_ms", operator_value_where_operator_name_is(jsnPropertyRows, "Actual Total Time"));
					mapWorkers.put("local_dirtied_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Dirtied Blocks"));
					mapWorkers.put("local_hit_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Hit Blocks"));
					mapWorkers.put("local_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Read Blocks"));
					mapWorkers.put("local_written_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Local Written Blocks"));
					mapWorkers.put("shared_dirtied_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Dirtied Blocks"));
					mapWorkers.put("shared_hit_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Hit Blocks"));
					mapWorkers.put("shared_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Read Blocks"));
					mapWorkers.put("shared_written_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Shared Written Blocks"));
					
					// I found a plan with sort workers, but the json-simple library can't retrieve it
					// where there are duplicate sort keys. I'm not sure how to change to the json-simple
					// library; led to many error messages and different variable types that I don't know
					// how to deal with.
					mapWorkers.put("sort_method", operator_value_where_operator_name_is(jsnPropertyRows, "Sort Method"));
					mapWorkers.put("sort_space_type", operator_value_where_operator_name_is(jsnPropertyRows, "Sort Space Type"));
					mapWorkers.put("sort_space_used_kb", operator_value_where_operator_name_is(jsnPropertyRows, "Sort Space Used"));
					
					mapWorkers.put("temp_read_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Temp Read Blocks"));
					mapWorkers.put("temp_written_blocks", operator_value_where_operator_name_is(jsnPropertyRows, "Temp Written Blocks"));
					mapWorkers.put("worker_number", operator_value_where_operator_name_is(jsnPropertyRows, "Worker Number"));
					intParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("parent_id_explain_plan_property");
					//intPARParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("par_parent_id_explain_plan_property");
					intPARPARParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("par_par_parent_id_explain_plan_property");
					//intPARPARPARParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("par_par_par_parent_id_explain_plan_property");
					strColumnNames = "id_explain_plan, id_explain_plan_property";
					//strValues = intIDExplainPlan + ", " + intPARParentIDExplainPlanProperty + ", " + intParentIDExplainPlanProperty;
					strValues = intIDExplainPlan + ", " + intPARPARParentIDExplainPlanProperty;
					for (Map.Entry<String, String> entry : mapWorkers.entrySet()) {
					    String key = entry.getKey();
					    if (strColumnNames != "") {
				    		strColumnNames += ", ";
					    }
					    strColumnNames += key;
					    Object value = entry.getValue();
					    if (strValues != "") {
				    		strValues += ", ";
					    }
					    if (value == null) {
					    	strValues += "null";
					    } else {
				    		strValue = (String) value;
				    		strValues += "'" + strValue.replace("'", "''") + "'";
					    }
					}
					
					strAnnotationsSQL += "-- Worker:\n";
					strAnnotationsSQL += "insert into explain_plan_operator_workers (" + strColumnNames + ") values (" + strValues + ");\n";
					if (bolShowWorkerSQL == true) { System.out.println("Columns: " + strColumnNames); }
					if (bolShowWorkerSQL == true) { System.out.println("Values: " + strValues); }
					
				} else if (strPAROperatorName != null && strPAROperatorName.equals("Output")) {
				
					intParentIDExplainPlanProperty = (Integer) ((JsonObject) jsnPropertyRows.get(0)).get("parent_id_explain_plan_property");
					
					// rows for explain_plan_operator_outputs table
					// insert directly, pretty much
					strAnnotationsSQL += "-- Output:\n"
						+ "insert into explain_plan_operator_outputs (id_explain_plan, id_explain_plan_property, column_order, column_name)\n"
						+ "select id_explain_plan, " + intParentIDExplainPlanProperty + ", id_explain_plan_property, operator_name\n"
						+ "from explain_plan_properties\n"
						+ "where id_explain_plan = " + intIDExplainPlan + "\n"
						+ "and parent_id_explain_plan_property = " + intParentIDExplainPlanProperty + ";\n";

				}
				
					
				// top-level hierarchy of JSON data
				if (intParentIDExplainPlanProperty <= 2) {
					if (operator_value_where_operator_name_is(jsnPropertyRows, "Execution Time") != null) {
	
						// row for explain_plan table
						strAnnotationsSQL += "-- explain_plan table data\n";
						strAnnotationsSQL += "update explain_plan_generated_values set\n";
						strAnnotationsSQL += "execution_time_ms = " + operator_value_where_operator_name_is(jsnPropertyRows, "Execution Time") + "\n";
						strAnnotationsSQL += "where id_explain_plan = " + intIDExplainPlan + ";\n";
						
					}
					
					if (operator_value_where_operator_name_is(jsnPropertyRows, "Planning Time") != null) {
	
						// row for explain_plan table
						strAnnotationsSQL += "-- explain_plan table data\n";
						strAnnotationsSQL += "update explain_plan_generated_values set\n";
						strAnnotationsSQL += "planning_time_ms = " + operator_value_where_operator_name_is(jsnPropertyRows, "Planning Time") + "\n";
						strAnnotationsSQL += "where id_explain_plan = " + intIDExplainPlan + ";\n";
					
					}
					
					if (operator_value_where_operator_name_is(jsnPropertyRows, "Total Runtime") != null) {
	
						// row for explain_plan table
						strAnnotationsSQL += "-- explain_plan table data\n";
						strAnnotationsSQL += "update explain_plan_generated_values set\n";
						strAnnotationsSQL += "total_runtime_ms = " + operator_value_where_operator_name_is(jsnPropertyRows, "Total Runtime") + "\n";
						strAnnotationsSQL += "where id_explain_plan = " + intIDExplainPlan + ";\n";
	
					}
				}
				
				if (strPAROperatorName != null && strPAROperatorName.equals("PostgreSQL Environment") == true) {
					if (operator_value_where_operator_name_is(jsnPropertyRows, "Version Environment") != null) {
						
						// row for explain_plan table
						strAnnotationsSQL += "-- explain_plan table data\n";
						strAnnotationsSQL += "update explain_plan_generated_values set\n";
						strAnnotationsSQL += "pg_version_environment = '" + operator_value_where_operator_name_is(jsnPropertyRows, "Version Environment").replace("'",  "''") + "'\n";
						strAnnotationsSQL += "where id_explain_plan = " + intIDExplainPlan + ";\n";
						
					}
					
					if (operator_value_where_operator_name_is(jsnPropertyRows, "Version Number") != null) {
	
						// row for explain_plan table
						strAnnotationsSQL += "-- explain_plan table data\n";
						strAnnotationsSQL += "update explain_plan_generated_values set\n";
						strAnnotationsSQL += "pg_version_number = " + operator_value_where_operator_name_is(jsnPropertyRows, "Version Number") + "\n";
						strAnnotationsSQL += "where id_explain_plan = " + intIDExplainPlan + ";\n";
						
					}
				}
			}
		}
		
		// "Total Runtime" was renamed to "Execution Time"
		strAnnotationsSQL += "-- find explain_plan max total cost and execution time\n";
		strAnnotationsSQL += "update explain_plan_generated_values as EPGV set\n";
		strAnnotationsSQL += "execution_time_ms = coalesce(execution_time_ms, total_runtime_ms)\n";
		strAnnotationsSQL += ",sum_total_costs = (select sum(total_cost*1.0) from explain_plan_operator_annotations where id_explain_plan = EPGV.id_explain_plan)\n";
		strAnnotationsSQL += ",max_total_cost = (select max(total_cost*1.0) from explain_plan_operator_annotations where id_explain_plan = EPGV.id_explain_plan)\n";
		strAnnotationsSQL += "where EPGV.id_explain_plan = " + intIDExplainPlan + ";\n";

		// add non-pk indexes after annotations are inserted
		strAnnotationsSQL += Database.explainPlanOperatorAnnotationsIndexes();

		// add non-pk indexes after workers are inserted
		strAnnotationsSQL += Database.explainPlanOperatorWorkersIndexes();

		// insert everything at once
		// is faster than running one command at a time
		strSQL = "begin transaction;\n" + strAnnotationsSQL + "commit;\n";
		//strSQL = strAnnotationsSQL;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		//PlanStatisticsPanel.setStatisticsData();
	}
/*
	public void insertRootOperatorProperties(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;
		//mapAnnotations.put("execution_time", operator_value_where_operator_name_is_r(jsnPropertyRows, "Execution Time"));
		strSQL = "insert into explain_plan_properties (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, operator_name)\n" +
				"select " + intIDExplainPlan + ", 104, max(id_explain_plan_property) + 1, 'Plans' from explain_plan_properties\n" +
				"\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}
*/

	// keep nestInitPlansWithinAnnotationsTable unoptimized until we've confirmed via a lot of
	// CTEs that it works
	public void nestInitPlansWithinAnnotationsTable(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;
		JsonObject jsnRow;
		JsonArray jsnRows;
		int intParentIDExplainPlanProperty;
		int intIDExplainPlanProperty;
		
		
		//if (intIDExplainPlan != 4)
		//	return;
		
		// RBAR over cte plans to show separate ctes
		strSQL = "select id_explain_plan_property, parent_id_explain_plan_property from explain_plan_operator_annotations\n" +
			"where id_explain_plan = " + intIDExplainPlan + " and parent_relationship = 'InitPlan'\n" +
			"order by id_explain_plan_property desc";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				intIDExplainPlanProperty = (int) jsnRow.get("id_explain_plan_property");
				intParentIDExplainPlanProperty = (int) jsnRow.get("parent_id_explain_plan_property");
		
				strSQL = "update explain_plan_operator_annotations set\n" +
						"id_explain_plan_property = id_explain_plan_property + 1\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and id_explain_plan_property = " + intIDExplainPlanProperty + ";\n";
						//"and parent_id_explain_plan_property > " + intParentIDExplainPlanProperty + ";\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);
		
				strSQL = "update explain_plan_operator_annotations set\n" +
						"parent_id_explain_plan_property = parent_id_explain_plan_property + 1\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and parent_id_explain_plan_property = " + intIDExplainPlanProperty + ";\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);

				// set CTE start and total cost to child operator's total cost,
				// and CTE start and actual total time to child operator's actual startup time and actual total time 
				strSQL = "insert into explain_plan_operator_annotations (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property, node_type, cte_name, actual_rows, plan_rows, plan_width, startup_cost, total_cost, actual_startup_time_ms, actual_total_time_ms)\n" +
						"select id_explain_plan, " + intParentIDExplainPlanProperty + ", " + intIDExplainPlanProperty + ", 'With CTE', replace(subplan_name, 'CTE', 'as'), actual_rows, plan_rows, plan_width, total_cost, total_cost, actual_total_time_ms, actual_total_time_ms from explain_plan_operator_annotations\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and id_explain_plan_property = " + intIDExplainPlanProperty + " + 1;\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);

				strSQL = "update explain_plan_operator_annotations set\n" +
						"parent_id_explain_plan_property = " + intIDExplainPlanProperty + "\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and id_explain_plan_property = " + intIDExplainPlanProperty + " + 1;\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);
		
				// update so properties can be shown
				strSQL = "update explain_plan_properties set\n" +
						"parent_id_explain_plan_property = " + intIDExplainPlanProperty + " + 1\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and parent_id_explain_plan_property = " + intIDExplainPlanProperty + ";\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);
		
				// update so workers can be shown
				strSQL = "update explain_plan_operator_workers set\n" +
						"id_explain_plan_property = " + intIDExplainPlanProperty + " + 1\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and id_explain_plan_property = " + intIDExplainPlanProperty + ";\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);
		
				// update so output can be shown
				strSQL = "update explain_plan_operator_outputs set\n" +
						"id_explain_plan_property = " + intIDExplainPlanProperty + " + 1\n" +
						"where id_explain_plan = " + intIDExplainPlan + "\n" +
						"and id_explain_plan_property = " + intIDExplainPlanProperty + ";\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				Database.dml(strSQL);

			}
		}

	}

	// REFACTOR - set initial x- and y-coordinates from explain_plan_operator_annotations
	public void generateInitialCartesianData(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;
		//JsonArray jsnRows;
		
		strSQL = "with recursive_query as (\n" +
				"  select 1 as level, id_explain_plan, \n" + 
				"  min(id_explain_plan_property) as id_explain_plan_property, parent_id_explain_plan_property\n" + 
				"  from explain_plan_operator_annotations\n" +
				"  where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"  UNION ALL\n" +
				"  select parent.level + 1, child.id_explain_plan, child.id_explain_plan_property, child.parent_id_explain_plan_property\n" + 
				"  from recursive_query as parent,\n" +
				"  explain_plan_operator_annotations as child\n" + 
				"  where parent.id_explain_plan_property = child.parent_id_explain_plan_property and parent.id_explain_plan = child.id_explain_plan\n" + 
				")\n" +
				", cartesian_coordinates as (\n" + 
				"  select row_number() over (partition by id_explain_plan, level order by id_explain_plan_property) as y_cart, level as x_cart\n" +
				"  , id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property\n" + 
				"  from recursive_query\n" + 
				")\n" + 
				", parent_cartesian_coordinates as (\n" + 
				"  select *, EPP.x_cart as parent_x_cart, EPP.y_cart as parent_y_cart\n" + 
				"  from cartesian_coordinates as CC\n" + 
				"  left outer join cartesian_coordinates as EPP on CC.parent_id_explain_plan_property = EPP.id_explain_plan_property\n" +
				"    and CC.id_explain_plan = EPP.id_explain_plan\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as EPOA set\n" +
				"(x_cart, y_cart, parent_x_cart, parent_y_cart) = (\n" + 
				"  select CC.x_cart, CC.y_cart, CC.parent_x_cart, CC.parent_y_cart\n" + 
				"  from parent_cartesian_coordinates as CC\n" +
				"  where EPOA.id_explain_plan = CC.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = CC.id_explain_plan_property\n" +
				") where exists (\n" + 
				"  select *\n" + 
				"  from parent_cartesian_coordinates as CC\n" + 
				"  where EPOA.id_explain_plan = CC.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = CC.id_explain_plan_property\n" + 
				");\n" + 
				"";

		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, parent_id_explain_plan_property, x_cart, y_cart, node_type from explain_plan_operator_annotations;
*/
	}
	
	// update x- and y-coordinates from explain_plan_operator_annotations to make layout
	// more aesthetically pleasing
	public void updateLayout(int intIDExplainPlan) {
		String strSQL;
		String strUpdateLayoutSQLCommands;
		boolean bolShowSQL = false;
		JsonArray jsnLeafRows;
		JsonArray jsnClimbRows;
		int intRunningIDExplainPlanProperty;
		int intClimbIDExplainPlanProperty;
		int intClimbNewXCart;
		int intClimbNewMaxYCart;
		
		// select all leaf operators
		strSQL = "select id_explain_plan_property\n"
			+ "from explain_plan_operator_annotations as EPOA\n"
			+ "where EPOA.id_explain_plan_property not in (\n"
			+ "  select parent_id_explain_plan_property from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan\n"
			+ "  and parent_id_explain_plan_property is not null\n"
			+ ")\n"
			+ "and EPOA.id_explain_plan = " + intIDExplainPlan + "\n"
			+ "order by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnLeafRows = Database.rows(strSQL);
		if (jsnLeafRows != null) {
			for (int intLeafRow = 0; intLeafRow < jsnLeafRows.size(); intLeafRow++) {
				intRunningIDExplainPlanProperty = (int) ((JsonObject) jsnLeafRows.get(intLeafRow)).get("id_explain_plan_property");
				// get all id_explain_plan_property and new x- and y-coordinates upward
				// until we hit a branched-off operator
				strSQL = "with recursive_query as (\n" + 
						"  select id_explain_plan, id_explain_plan_property, parent_id_explain_plan_property, x_cart, y_cart\n" + 
						"  from explain_plan_operator_annotations\n" + 
						"  where id_explain_plan_property = " + intRunningIDExplainPlanProperty + " -- running_id_explain_plan_property\n" + 
						"  and id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
						"  UNION ALL\n" + 
						"  select parent.id_explain_plan, parent.id_explain_plan_property, parent.parent_id_explain_plan_property, parent.x_cart, parent.y_cart\n" + 
						"  from recursive_query child,\n" + 
						"  explain_plan_operator_annotations parent\n" + 
						"  where child.parent_id_explain_plan_property = parent.id_explain_plan_property\n" + 
						"  and child.id_explain_plan = parent.id_explain_plan\n" + 
						")\n" + 
						", upper_intersecting_parents as (\n" + 
						"  select R.id_explain_plan_property, max(E.id_explain_plan_property) as upper_id_explain_plan_property\n" + 
						"  from recursive_query as R\n" + 
						"  left outer join explain_plan_operator_annotations as E on R.parent_id_explain_plan_property = E.parent_id_explain_plan_property\n" + 
						"    and R.id_explain_plan_property > E.id_explain_plan_property\n" + 
						"    and R.id_explain_plan = E.id_explain_plan\n" + 
						")\n" + 
						"select\n" + 
						"id_explain_plan, id_explain_plan_property, parent_id_explain_plan_property, x_cart, (select max(y_cart) from recursive_query) as y_cart\n" + 
						", (select upper_id_explain_plan_property from upper_intersecting_parents)\n" + 
						"from recursive_query as R\n" + 
						"where R.id_explain_plan_property > (select upper_id_explain_plan_property from upper_intersecting_parents)\n";
				if (bolShowSQL == true) { System.out.println(strSQL); }
				jsnClimbRows = Database.rows(strSQL);
				if (jsnClimbRows != null) {
					strUpdateLayoutSQLCommands = "";
					for (int intClimbRow = 0; intClimbRow < jsnClimbRows.size(); intClimbRow++) {
						JsonObject jsnClimbRow = (JsonObject) jsnClimbRows.get(intClimbRow);
						intClimbIDExplainPlanProperty = jsnClimbRow.getInteger("id_explain_plan_property");
						intClimbNewXCart = jsnClimbRow.getInteger("x_cart");
						intClimbNewMaxYCart = jsnClimbRow.getInteger("y_cart");

						// shift operators below intClimbIDExplainPlanProperty down
						strUpdateLayoutSQLCommands += "update explain_plan_operator_annotations set\n"
							+ "y_cart = y_cart + (\n"
							+ "  select " + intClimbNewMaxYCart + " + 1 - min(y_cart) from explain_plan_operator_annotations where x_cart = " + intClimbNewXCart + " and id_explain_plan_property > " + intClimbIDExplainPlanProperty + " and id_explain_plan = " + intIDExplainPlan + "\n"
							+ ")\n"
							+ "where x_cart = " + intClimbNewXCart + "\n"
							+ "and id_explain_plan = " + intIDExplainPlan + "\n"
							+ "and id_explain_plan_property > " + intClimbIDExplainPlanProperty + "\n"
							+ ";\n";

						// move this operator down to the lowest vertical operator found during the climb
						strUpdateLayoutSQLCommands += "update explain_plan_operator_annotations set\n"
							+ "x_cart = " + intClimbNewXCart + "\n"
							+ ",y_cart = " + intClimbNewMaxYCart + "\n"
							+ "where id_explain_plan_property = " + intClimbIDExplainPlanProperty + "\n"
							+ "and id_explain_plan = " + intIDExplainPlan + ";\n";
					}

					if (strUpdateLayoutSQLCommands != "") {
						strUpdateLayoutSQLCommands = "begin transaction;\n" + strUpdateLayoutSQLCommands + "commit;\n";
						if (bolShowSQL == true) { System.out.println(strUpdateLayoutSQLCommands); }
						Database.dml(strUpdateLayoutSQLCommands);
					}
				}
			}
		}
	}

	// Update explain_plan data, like
	// total time, ram used, disk used, verbose_setting
	// from explain_plan_operator_annotations
	// total cost is not calculated, b/c the plan data should have it
	// verbose setting is enabled if there are any actual row values (i.e., non sort values)
	public void setVerboseSetting(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;
		strSQL = "update explain_plan_generated_values as EPGV set\n"
			+ "verbose_setting = case when exists(select * from explain_plan_operator_workers where id_explain_plan = EPGV.id_explain_plan and actual_rows is not null) then 1 else 0 end\n"
			+ "where id_explain_plan = " + intIDExplainPlan + "\n"
			+ ";\n";
		//+ ", verbose_setting = (select case when exists(select * from explain_plan_operator_workers where id_explain_plan = EP.id_explain_plan) then 1 else 0 end)\n"
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}
	
	public void setTotalExecutionTimeSlider(int idExplainPlan) {
		// Off-screen Compare Plans generation must not touch the shared Sliders dockable.
		if (suppressDiagramBusyCursor) {
			return;
		}
		SliderPanel sliderPanel = findSliderPanelInSelectedTab();
		if (sliderPanel != null) {
			sliderPanel.setPlanTotalTimeMs(idExplainPlan);
			setSliderType(sliderPanel.SLIDER_TYPE_TIME);  // SLIDER_TYPE_TIME or SLIDER_TYPE_COST
			sliderPanel.refreshComponents();
		}
	}
	
	public void setTotalExecutionCostSlider(int idExplainPlan) {
		// Off-screen Compare Plans generation must not touch the shared Sliders dockable.
		if (suppressDiagramBusyCursor) {
			return;
		}
		SliderPanel sliderPanel = findSliderPanelInSelectedTab();
		if (sliderPanel != null) {
			sliderPanel.setPlanTotalCost(idExplainPlan);
			setSliderType(sliderPanel.SLIDER_TYPE_COST);
			sliderPanel.refreshComponents();
		}
	}

	private SliderPanel findSliderPanelInSelectedTab() {
		Component selected = objCurrentPlanTabs.getSelectedComponent();
		if (!(selected instanceof Container)) {
			return null;
		}
		return findComponentOfType((Container) selected, SliderPanel.class);
	}
	
/*
*/
	
	public void updateRowAndMassCalculations(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strRetrieveSQL;
		String strSQL = "";
		JsonArray jsnRows;
		JsonObject jsnRow;
		boolean bolShowTiming = false;
		// both bolConcatenateSQL and bolUseTransaction need to be false, otherwise we seem to lose some worker values
		boolean bolConcatenateSQL = true; // trying out "true"
		boolean bolUseTransaction = true;
		
		// calculate worker info based on explain_plans.verbose_setting, parallel operators between 'Gather' / 'Gather Merge'
		// and non-parallel operators, 	
		strSQL += "with cteParallelOperators as (" +
				"  select 'Aggregate' as op_name\n" +
				"  union select 'Append'\n" +
				"  union select 'Bitmap Heap Scan'\n" +
				"  union select 'Bitmap Index Scan'\n" +
				"  union select 'Gather'\n" +
				"  union select 'Gather Merge'\n" + 
				"  union select 'Hash'\n" +
				"  union select 'Hash Join'\n" +
				"  union select 'Index Only Scan'\n" +
				"  union select 'Index Scan'\n" +
				"  union select 'Materialize'\n" +
				"  union select 'Memoize'\n" +
				"  union select 'Merge Append'\n" +
				"  union select 'Merge Join'\n" +
				"  union select 'Nested Loop'\n" +
				"  union select 'Seq Scan'\n" +
				"  union select 'Sort'\n" +
				")\n" + 
				", cteGatherAncestorWorkers as (\n" +
				"  select id_explain_plan, id_explain_plan_property\n" + 
				"  , case when node_type in ('Gather', 'Gather Merge') then id_explain_plan_property else 0 end as id_gather_ancestor_workers_planned\n" + 
				"  , case when node_type in ('Gather', 'Gather Merge') then workers_planned else null end as gather_ancestor_workers_planned\n" + 
				"  , case when node_type in ('Gather', 'Gather Merge') then id_explain_plan_property else 0 end as id_gather_ancestor_workers_launched\n" + 
				"  , case when node_type in ('Gather', 'Gather Merge') then workers_launched else null end as gather_ancestor_workers_launched\n" + 
				"  , case when node_type not in (select op_name from cteParallelOperators) then id_explain_plan_property else 0 end as id_block_summing\n" +
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  where parent_id_explain_plan_property = -1\n" + 
				"  and id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"  UNION ALL\n" + 
				"  select T.id_explain_plan, T.id_explain_plan_property\n" + 
				"  , case when T.node_type in ('Gather', 'Gather Merge') then T.id_explain_plan_property else A.id_gather_ancestor_workers_planned end\n" + 
				"  , case when T.node_type in ('Gather', 'Gather Merge') then T.workers_planned else A.gather_ancestor_workers_planned end\n" + 
				"  , case when T.node_type in ('Gather', 'Gather Merge') then T.id_explain_plan_property else A.id_gather_ancestor_workers_launched end\n" + 
				"  , case when T.node_type in ('Gather', 'Gather Merge') then T.workers_launched else A.gather_ancestor_workers_launched end\n" + 
				"  , case when T.node_type not in (select op_name from cteParallelOperators) then T.id_explain_plan_property else A.id_block_summing end\n" +
				"  from explain_plan_operator_annotations as T\n" + 
				"  inner join cteGatherAncestorWorkers as A on T.parent_id_explain_plan_property = A.id_explain_plan_property\n" + 
				"    and T.id_explain_plan = A.id_explain_plan\n" + 
				")\n" + 
				", cteWorkersAndBlockers as (\n" + 
				"  select\n" + 
				"  id_explain_plan\n" + 
				"  ,id_explain_plan_property\n" + 
				"  ,id_gather_ancestor_workers_planned\n" + 
				"  ,id_gather_ancestor_workers_launched\n" + 
				"  ,id_block_summing\n" + 
				"  ,gather_ancestor_workers_planned\n" + 
				"  ,gather_ancestor_workers_launched\n" + 
				"  from cteGatherAncestorWorkers\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as SET_EPOA set\n" + 
				"(ancestor_workers_planned, ancestor_workers_launched) = (select\n" + 
				"  case when CTE.id_block_summing < CTE.id_gather_ancestor_workers_planned then CTE.gather_ancestor_workers_planned else null end\n" + 
				"  ,case when CTE.id_block_summing < CTE.id_gather_ancestor_workers_launched then CTE.gather_ancestor_workers_launched else null end\n" +
				"  from cteWorkersAndBlockers as CTE\n" + 
				"  where SET_EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"    and SET_EPOA.id_explain_plan_property = CTE.id_explain_plan_property\n" + 
				");\n" + 
				"update explain_plan_operator_annotations set\n" +
				"ancestor_workers_planned = null\n" +
				",ancestor_workers_launched = null\n" +
				"where node_type in ('Gather', 'Gather Merge')\n" +
				"and id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// set explain_plan_operator_annotations.exact_actual_worker_count to # of workers if verbose_setting = 1
		// used by "~#### rows removed by filter"; might be used by other operators
		strSQL += "update explain_plan_operator_annotations as SET_EPOA set\n" +
				"exact_actual_worker_count = (select count(*) from explain_plan_operator_workers where id_explain_plan = SET_EPOA.id_explain_plan and id_explain_plan_property = SET_EPOA.id_explain_plan_property and actual_rows is not null)\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"and exists (select * from explain_plan_generated_values where id_explain_plan = " + intIDExplainPlan + " and verbose_setting = 1)\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// actual_rows is not correct if there are parallel workers
		// so, sum the worker rows for the current operator
		// Setting analyze true and verbose false cause explains plan to omit worker actual rows.
		// explain_plans.verbose_setting is 1 if _any_ workers exist.
		// So, when verbose_setting = 0, sum_workers_actual_rows will be null, resulting in estimates
		// When verbose_setting = 1, if sum_workers_actual_rows is null, set it to 0, b/c there are simply no workers for this operator.
		strSQL += "with cteEPOW as (\n" +
				"  select EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" +
				"  , EPOW.actual_rows\n" +
				"  from explain_plan_operator_workers as EPOW\n" + 
				"  inner join explain_plan_operator_annotations as EPOA on EPOW.id_explain_plan = EPOA.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as SET_EPOA set\n" + 
				"(sum_workers_actual_rows) = (select\n" +
				"  sum(cteEPOW.actual_rows)\n" +
				"  from cteEPOW\n" + 
				"  where SET_EPOA.id_explain_plan = cteEPOW.id_explain_plan\n" +
				"    and SET_EPOA.id_explain_plan_property = cteEPOW.id_explain_plan_property\n" + 
				"    and SET_EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"    and SET_EPOA.ancestor_workers_launched is not null\n" +
				");\n" +
				"update explain_plan_operator_annotations as SET_EPOA set\n" +
				"sum_workers_actual_rows = 0\n" +
				"where exists(select * from explain_plan_generated_values where verbose_setting = 1 and id_explain_plan = SET_EPOA.id_explain_plan)\n" +
				"and sum_workers_actual_rows is null\n" +
				"and ancestor_workers_launched is not null\n" +
				"and id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// set row calculations
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"actual_rows_plus_sum_workers_actual_rows = actual_rows + sum_workers_actual_rows\n" +
				",actual_rows_times_ancestor_workers_planned = actual_rows * (1 + ancestor_workers_planned)\n" +
				",actual_rows_times_ancestor_workers_launched = actual_rows * (1 + ancestor_workers_launched)\n" +
				",plan_rows_times_ancestor_workers_planned = plan_rows * (1 + ancestor_workers_planned)\n" +
				",plan_rows_times_ancestor_workers_launched = plan_rows * (1 + ancestor_workers_launched)\n" +
				",rows_removed_by_filter_times_ancestor_workers_planned = rows_removed_by_filter * (1 + ancestor_workers_planned)\n" +
				",rows_removed_by_filter_times_ancestor_workers_launched = rows_removed_by_filter * (1 + ancestor_workers_launched)\n" +
				",rows_removed_by_filter_times_exact_actual_worker_count = rows_removed_by_filter * (1 + exact_actual_worker_count)\n" +
				",rows_removed_by_join_filter_times_ancestor_workers_planned = rows_removed_by_join_filter * (1 + ancestor_workers_planned)\n" +
				",rows_removed_by_join_filter_times_ancestor_workers_launched = rows_removed_by_join_filter * (1 + ancestor_workers_launched)\n" +
				",rows_removed_by_join_filter_times_exact_actual_worker_count = rows_removed_by_join_filter * (1 + exact_actual_worker_count)\n" +
				",rows_removed_by_index_recheck_times_ancestor_workers_planned = rows_removed_by_index_recheck * (1 + ancestor_workers_planned)\n" +
				",rows_removed_by_index_recheck_times_ancestor_workers_launched = rows_removed_by_index_recheck * (1 + ancestor_workers_launched)\n" +
				",rows_removed_by_index_recheck_times_exact_actual_worker_count = rows_removed_by_index_recheck * (1 + exact_actual_worker_count)\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		
		// set data mass calculations
		strSQL += "update explain_plan_operator_annotations set\n" +
				"actual_mass_times_sum_workers_actual_rows = plan_width * actual_rows_plus_sum_workers_actual_rows\n" +
				",actual_mass_times_ancestor_workers_planned = plan_width * actual_rows_times_ancestor_workers_planned\n" +
				",actual_mass_times_ancestor_workers_launched = plan_width * actual_rows_times_ancestor_workers_launched\n" +
				",estimated_mass_times_ancestor_workers_planned = plan_width * plan_rows_times_ancestor_workers_planned\n" +
				",estimated_mass_times_ancestor_workers_launched = plan_width * plan_rows_times_ancestor_workers_launched\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// set total_actual_rows to the most accurate value known
		// set total_estimated_rows to the most accurate value known, but don't include worker actual rows
		// set total_actual_rows_is_exact accordingly
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"total_actual_rows = coalesce(actual_rows_plus_sum_workers_actual_rows, actual_rows_times_ancestor_workers_launched, actual_rows_times_ancestor_workers_planned, actual_rows)\n" +
				",total_estimated_rows = coalesce(plan_rows_times_ancestor_workers_launched, plan_rows_times_ancestor_workers_planned, plan_rows)\n" +
				",total_rows_removed_by_filter = coalesce(rows_removed_by_filter_times_exact_actual_worker_count, rows_removed_by_filter_times_ancestor_workers_planned, rows_removed_by_filter_times_ancestor_workers_launched, rows_removed_by_filter)\n" +
				",total_rows_removed_by_join_filter = coalesce(rows_removed_by_join_filter_times_exact_actual_worker_count, rows_removed_by_join_filter_times_ancestor_workers_planned, rows_removed_by_join_filter_times_ancestor_workers_launched, rows_removed_by_join_filter)\n" +
				",total_rows_removed_by_index_recheck = coalesce(rows_removed_by_index_recheck_times_exact_actual_worker_count, rows_removed_by_index_recheck_times_ancestor_workers_planned, rows_removed_by_index_recheck_times_ancestor_workers_launched, rows_removed_by_index_recheck)\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"total_actual_rows_is_exact = case when actual_rows_plus_sum_workers_actual_rows is not null or node_type in ('Gather', 'Gather Merge') or ancestor_workers_launched is null then 1 else 0 end\n" +
				",total_rows_removed_by_filter_is_exact = case when coalesce(total_rows_removed_by_filter*1.0, 0) = coalesce(rows_removed_by_filter*1.0, 0) then 1 else 0 end\n" +
				",total_rows_removed_by_join_filter_is_exact = case when coalesce(total_rows_removed_by_join_filter*1.0, 0) = coalesce(rows_removed_by_join_filter*1.0, 0) then 1 else 0 end\n" +
				",total_rows_removed_by_index_recheck_is_exact = case when coalesce(total_rows_removed_by_index_recheck*1.0, 0) = coalesce(rows_removed_by_index_recheck*1.0, 0) then 1 else 0 end\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"and total_actual_rows is not null\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// set actual_data_size_bytes to the most accurate value known
		// set estimated_data_size_bytes to the most accurate value known, but don't include worker actual rows
		// set total_actual_mass_is_exact accordingly
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"actual_data_size_bytes = coalesce(actual_mass_times_sum_workers_actual_rows, actual_mass_times_ancestor_workers_launched, actual_mass_times_ancestor_workers_planned, total_actual_rows * plan_width)\n" +
				",estimated_data_size_bytes = coalesce(estimated_mass_times_ancestor_workers_launched, estimated_mass_times_ancestor_workers_planned, plan_rows * plan_width)\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// set total_actual_mass_is_exact
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"total_actual_mass_is_exact = case when actual_mass_times_sum_workers_actual_rows is not null or node_type in ('Gather', 'Gather Merge') or ancestor_workers_launched is null then 1 else 0 end\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"and actual_data_size_bytes is not null\n" +
				";\n";

		if (bolUseTransaction == true) { strSQL = "begin transaction;\n" + strSQL + "commit;\n"; }

		if (bolShowSQL == true) { System.out.println(strSQL); }

		if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		if (bolShowTiming == true) { System.out.println("after setExclusiveTimeForPLAndWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
	}
	
	
	public void setPerOperatorTotalsForPLAndWorkers(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL = "";
		boolean bolConcatenateSQL = true; // trying out "true"
		boolean bolUseTransaction = true;
		boolean bolShowTiming = false;

		if (bolShowTiming == true) { System.out.println("before setPerOperatorTotalsForPLAndWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		
		// the next "copy startup" block is not needed b/c the PL is not the same as
		// the operator's actual_startup_time_ms or min_actual_startup_time_ms
		// copy explain_plan_operator_annotations.actual_total_time_ms to
		// explain_plan_operator_annotations.parallel_leader_actual_total_time_ms
		// and explain_plan_operator_annotations.actual_startup_time_ms to
		// explain_plan_operator_annotations.parallel_leader_actual_startup_time_ms
		strSQL += "update explain_plan_operator_annotations set\n" +
				"parallel_leader_actual_startup_time_ms = actual_startup_time_ms\n" +
				",parallel_leader_actual_total_time_ms = actual_total_time_ms\n" +
				"where id_explain_plan = " + intIDExplainPlan + "; -- id_explain_plan\n";

		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// calc PL time difference
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"parallel_leader_difference_time_ms = parallel_leader_actual_total_time_ms - parallel_leader_actual_startup_time_ms\n" +
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";

		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// calc PL exclusive time, which (so far) looks like it'll always be equal (PL = W1 = W2 ...)
		strSQL += "update explain_plan_operator_annotations as SET_EPOA set\n" + 
				"parallel_leader_exclusive_time_ms = (\n" + 
				"  select EPOA.parallel_leader_actual_total_time_ms - sum(SUB_EPOA.parallel_leader_actual_total_time_ms)\n" +
				"  from explain_plan_operator_annotations as EPOA\n" +
				//"  left outer join explain_plan_operator_annotations as SUB_EPOA\n" +
				"  inner join explain_plan_operator_annotations as SUB_EPOA\n" +
				"    on EPOA.id_explain_plan = SUB_EPOA.id_explain_plan\n" +
				"    and EPOA.id_explain_plan_property = SUB_EPOA.parent_id_explain_plan_property\n" +
				"  where EPOA.id_explain_plan = SUB_EPOA.id_explain_plan\n" +
				"    and EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" +
				"    and SET_EPOA.id_explain_plan_property = EPOA.id_explain_plan_property\n" +
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" +
				")\n" +
				"where id_explain_plan = " + intIDExplainPlan + "; -- id_explain_plan\n" +
				"update explain_plan_operator_annotations set\n" + 
				"parallel_leader_exclusive_time_ms = parallel_leader_actual_total_time_ms\n" + 
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"and parallel_leader_exclusive_time_ms is null;\n" +
				"update explain_plan_operator_annotations set\n" + 
				"parallel_leader_exclusive_time_ms = 0\n" + 
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"and parallel_leader_exclusive_time_ms < 0;\n" +
				";\n";

		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// calc per-worker exclusive time, which (so far) looks like it'll always be equal (PL = W1 = W2 ...) 
		strSQL += "update explain_plan_operator_workers as SET_EPOW set\n" + 
				"exclusive_time_ms = (\n" + 
				"  select EPOA.actual_total_time_ms - sum(SUB_EPOA.actual_total_time_ms)\n" + 
				"  from explain_plan_operator_annotations as EPOA\n" + 
				//"  left outer join explain_plan_operator_annotations as SUB_EPOA\n" + 
				"  inner join explain_plan_operator_annotations as SUB_EPOA\n" +
				"    on EPOA.id_explain_plan = SUB_EPOA.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SUB_EPOA.parent_id_explain_plan_property\n" + 
				"  where EPOA.id_explain_plan = SET_EPOW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan = SUB_EPOA.id_explain_plan\n" + 
				"    and SET_EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				")\n" + 
				"where id_explain_plan = " + intIDExplainPlan + "; -- id_explain_plan\n" + 
				"update explain_plan_operator_workers set\n" + 
				"exclusive_time_ms = actual_total_time_ms\n" + 
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"and exclusive_time_ms is null;\n" +
				"update explain_plan_operator_workers set\n" + 
				"exclusive_time_ms = 0\n" + 
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"and exclusive_time_ms < 0;\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		strSQL += "update explain_plan_operator_workers set\n" + 
				"difference_time_ms = actual_total_time_ms - actual_startup_time_ms\n" +
				"where id_explain_plan = " + intIDExplainPlan + "; -- id_explain_plan\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }


		strSQL += "update explain_plan_operator_annotations as SET_EPOA set\n" +
				"exclusive_time_ms = (\n" +
				"  select coalesce(max(EPOA.parallel_leader_exclusive_time_ms, max(EPOW.exclusive_time_ms)), EPOA.parallel_leader_exclusive_time_ms, max(EPOA.actual_total_time_ms, EPOW.actual_total_time_ms))\n" +
				"  from explain_plan_operator_annotations as EPOA\n" +
				"  left outer join explain_plan_operator_workers as EPOW\n" +
				"    on EPOA.id_explain_plan = EPOW.id_explain_plan\n" +
				"    and EPOA.id_explain_plan_property = EPOW.id_explain_plan_property\n" +
				"  where EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" +
				"    and SET_EPOA.id_explain_plan_property = EPOA.id_explain_plan_property\n" +
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" +
				")\n" + 
				"where id_explain_plan = " + intIDExplainPlan + "; -- id_explain_plan\n" + 
				"update explain_plan_operator_annotations as EPOA set\n" +
				"exclusive_time_ms = (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan)\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"and id_explain_plan_property = -1;\n" +
				"update explain_plan_operator_annotations as EPOA set\n" +
				"exclusive_time_ms = 0\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"and exclusive_time_ms*1.0 < 0;\n" +
				"\n";
		//strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// set min and max times
		// min for parallel / worker, max for parallel / worker
		strSQL += "update explain_plan_operator_annotations as SET_EPOA set\n" +
				"(min_actual_startup_time_ms, max_actual_total_time_ms) = (\n" + 
				"  select\n" +
				"    coalesce(min(EPOA.parallel_leader_actual_startup_time_ms, min(EPOW.actual_startup_time_ms)), EPOA.parallel_leader_actual_startup_time_ms)\n" +
				"    ,coalesce(max(EPOA.parallel_leader_actual_total_time_ms, max(EPOW.actual_total_time_ms)), EPOA.parallel_leader_actual_total_time_ms)\n" +
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  left outer join explain_plan_operator_workers as EPOW on EPOA.id_explain_plan = EPOW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = EPOW.id_explain_plan_property\n" + 
				"  where EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SET_EPOA.id_explain_plan_property\n" + 
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" +
				"  order by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" +
				")\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";";

		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		// ADD: PARALLEL EXCLUSIVE TIME
		// idea, start it from exclusive_time_ms, then subtract (?) parallel total time ms and parallel startup time ms
/*
		strSQL += "update explain_plan_operator_annotations set\n" +
				"parallel_leader_exclusive_time_ms = max_actual_total_time_ms - parallel_leader_actual_total_time_ms\n" +
				"where id_explain_plan = " + intIDExplainPlan + "; -- id_explain_plan\n";
*/
		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (2): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		// update amount of time spent per operator
		// (1) exclusive time and (2) time difference
		// (1) exclusive time

		// (2) time difference
		strSQL += "update explain_plan_operator_annotations set\n" + 
				"difference_time_ms = max_actual_total_time_ms - min_actual_startup_time_ms\n" + 
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";";
		
		if (bolUseTransaction == true) { strSQL = "begin transaction;\n" + strSQL + "commit;\n"; }

		if (bolShowSQL == true) { System.out.println(strSQL); }

		if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		if (bolShowTiming == true) { System.out.println("after setPerOperatorTotalsForPLAndWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
	}
	
	public void updateStringUnitsPercentageAnnotationsTextData(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strRetrieveSQL;
		String strSQL = "";
		JsonArray jsnRows;
		JsonObject jsnRow;
		boolean bolShowTiming = false;
		// both bolConcatenateSQL and bolUseTransaction need to be false, otherwise we seem to lose some worker values or exclusive/diff times
		boolean bolConcatenateSQL = false;
		boolean bolUseTransaction = false;

		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (1): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		
		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (1): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		
		strSQL += "with cteGatherAncestorActualLoops as (\n" + 
				"  select id_explain_plan, id_explain_plan_property\n" + 
				"  , case when node_type in ('Gather', 'Gather Merge') then actual_loops else null end as gather_ancestor_actual_loops\n" +
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  where parent_id_explain_plan_property = -1\n" + 
				"  and id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				"  UNION ALL\n" + 
				"  select T.id_explain_plan, T.id_explain_plan_property\n" + 
				"  , case when T.node_type in ('Gather', 'Gather Merge') then T.actual_loops else A.gather_ancestor_actual_loops end\n" +  
				"  from explain_plan_operator_annotations as T\n" + 
				"  inner join cteGatherAncestorActualLoops as A on T.parent_id_explain_plan_property = A.id_explain_plan_property\n" + 
				"    and T.id_explain_plan = A.id_explain_plan\n" +
				")\n" +
				"update explain_plan_operator_annotations as SET_EPOA set\n" + 
				"gather_ancestor_actual_loops = (\n" + 
				"  select gather_ancestor_actual_loops\n" + 
				"  from cteGatherAncestorActualLoops as CTE\n" + 
				"  where SET_EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"  and SET_EPOA.id_explain_plan_property = CTE.id_explain_plan_property\n" +
				");\n";

/*
select id_explain_plan_property, node_type, actual_loops, gather_ancestor_actual_loops
from explain_plan_operator_annotations
order by id_explain_plan_property
*/

		//strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		
		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (8): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }


/*
select id_explain_plan, id_explain_plan_property, node_type, actual_startup_time_ms, actual_total_time_ms, exclusive_time_ms
from explain_plan_operator_annotations
order by id_explain_plan_property
*/

		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (3): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		// set elapsed time and units string numerics for each operator
		// doing it RBAR style b/c I'm too lazy to write
		// sql code to convert time/pages/bytes to units
		// formatted prettily
		strRetrieveSQL = "select\n"
				+ "id_explain_plan_property, actual_data_size_bytes\n"
				+ ", difference_time_ms\n"
				+ ", parallel_leader_difference_time_ms\n"
				+ ", estimated_data_size_bytes\n"
				+ ", exclusive_time_ms\n"
				+ ", parallel_leader_exclusive_time_ms\n"
				+ ", total_actual_rows\n"
				+ ", total_estimated_rows\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				;

		if (bolShowSQL == true) { System.out.println(strRetrieveSQL); }
	
		jsnRows = Database.rows(strRetrieveSQL);
		if (jsnRows != null) {
			//strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_annotations set\n";
				strSQL += "actual_data_size_units_string = ";
				if (jsnRow.get("actual_data_size_bytes") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.bytes(jsnRow.getDouble("actual_data_size_bytes"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("actual_data_size_bytes") + " bytes";
				}
				strSQL += "\n";
				
				// time difference
				// ADD: NON-PARALLEL TIME DIFFERENCE -- done
				strSQL += ", difference_time_units_string = ";
				if (jsnRow.get("difference_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("difference_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("difference_time_ms") + " ms";
				}
				strSQL += "\n";
				strSQL += ", parallel_leader_difference_time_units_string = ";
				if (jsnRow.get("parallel_leader_difference_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("parallel_leader_difference_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("parallel_leader_difference_time_ms") + " ms";
				}
				strSQL += "\n";
				strSQL += ", estimated_data_size_units_string = ";
				if (jsnRow.get("estimated_data_size_bytes") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.bytes(jsnRow.getDouble("estimated_data_size_bytes"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("estimated_data_size_bytes") + " bytes";
				}
				strSQL += "\n";
				
				// exclusive time
				strSQL += ", exclusive_time_units_string = ";
				if (jsnRow.get("exclusive_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("exclusive_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("exclusive_time_ms") + " ms";
				}
				strSQL += "\n";
				// ADD: PARALLEL EXCLUSIVE TIME -- done
				strSQL += ", parallel_leader_exclusive_time_units_string = ";
				if (jsnRow.get("parallel_leader_exclusive_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("parallel_leader_exclusive_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("parallel_leader_exclusive_time_ms") + " ms";
				}
				strSQL += "\n";
				strSQL += ", actual_rows_units_string = ";
				if (jsnRow.get("total_actual_rows") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.units(jsnRow.getDouble("total_actual_rows"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("total_actual_rows") + " rows";
				}
				strSQL += "\n";
				strSQL += ", estimated_rows_units_string = ";
				if (jsnRow.get("total_estimated_rows") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.units(jsnRow.getDouble("total_estimated_rows"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("total_estimated_rows") + " rows";
				}
				strSQL += "\n";
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property");
				strSQL += ";\n";
			}
			if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
			if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		}
		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (3): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		
		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (4): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		// set percentage string numerics
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
		
				// cost
				"cost_percentage_string = case\n" + 
				"  when (select sum_total_costs from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) is null then null\n" + 
				"  when (select sum_total_costs from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) = 0 then 0\n" + 
				"  when total_cost*1.0 / (select sum_total_costs from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) < 0.001 then 0\n" + 
				"  when total_cost*1.0 / (select sum_total_costs from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) < 0.1 then round(total_cost / (select sum_total_costs from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan), 3) * 100\n" + 
				"  else cast(total_cost*1.0 / (select sum_total_costs from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				
				// time difference
				// ADD: NON-PARALLEL TIME DIFFERENCE -- done
				", difference_time_percentage_string = case\n" +
				"  when (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) is null then null\n" +
				"  when (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) = 0 then 0\n" +
				"  when difference_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) < 0.001 then 0\n" +
				"  when difference_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) < 0.1 then round(difference_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan), 3) * 100\n" +
				"  else cast(difference_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) * 100 + 0.5 as integer)\n" +
				"end\n" +

				// exclusive time
				", exclusive_time_percentage_string = case\n" + 
				"  when (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) is null then null\n" + 
				"  when (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) = 0 then 0\n" + 
				"  when exclusive_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) < 0.001 then 0\n" + 
				"  when exclusive_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) < 0.1 then round(exclusive_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan), 3) * 100\n" + 
				"  else cast(exclusive_time_ms*1.0 / (select execution_time_ms from explain_plan_generated_values where id_explain_plan = EPOA.id_explain_plan) * 100 + 0.5 as integer)\n" + 
				"end\n" + 
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (4): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

/*
select max_total_cost from explain_plan_generated_values;

select execution_time_ms from explain_plan_generated_values where id_explain_plan = 3;

select EP.id_explain_plan, EP.plan_name, EPOA.id_explain_plan_property, EPOA.node_type, EPOA.cost_percentage_string, EPOA.exclusive_time_ms, EPOA.exclusive_time_percentage_string
from explain_plan_operator_annotations as EPOA
inner join explain_plans as EP on EPOA.id_explain_plan = EP.id_explain_plan
where EP.plan_name = 'all_cellline_view'
order by EP.id_explain_plan desc, EPOA.id_explain_plan_property;
*/
		// set units string numerics
/*
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" + 
				"disk_units_string = \n" + 
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
*/		
		
		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (5): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		// set actual and estimated strings
		// just doing a straight copy; we might add stuff to the label later
		// this is separated from other label strings only because
		// actual and estimated strings don't need highlight_1 and highlight_2
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" + 
				"actual_data_size_label_string = actual_data_size_units_string\n" + 
				", estimated_data_size_label_string = estimated_data_size_units_string\n" + 
				", actual_rows_label_string = actual_rows_units_string\n" + 
				", estimated_rows_label_string = estimated_rows_units_string\n" + 
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (5): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

/*
select id_explain_plan_property, actual_data_size_label_string, actual_rows_label_string, estimated_data_size_label_string
, estimated_rows_label_string
from explain_plan_operator_annotations
*/

		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (6): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		// set labels and highlights
		// SQLite: null || 'x' => null, so missing cost/time metrics must not erase the operator number.
		// When a metric is unavailable, fall back to "(id) Node Type" above the icon.
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
		
				// cost
				"cost_label_string = case\n" +
				"    when cost_percentage_string is null then '(' || id_explain_plan_property || ')' || case when node_type is null or node_type = '' then '' else ' ' || node_type end\n" +
				"    else '(' || id_explain_plan_property || ') Cost: ' || cost_percentage_string || '%'\n" +
				"  end\n" + 
				", cost_highlight_1 = case\n" + 
				"  	 when cost_percentage_string*1.0 = (select max(cost_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1) then 1\n" + 
				"    else 0\n" + 
				"  end\n" + 
				", cost_highlight_2 = case\n" + 
				"    when cost_percentage_string*1.0 = (select max(cost_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1 and cost_percentage_string*1.0 < (select max(cost_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1)) then 1\n" + 
				"    else 0\n" + 
				"  end\n" +
				
				// time difference
				", difference_time_label_string = case\n" +
				"    when difference_time_percentage_string is null and difference_time_units_string is null then '(' || id_explain_plan_property || ')' || case when node_type is null or node_type = '' then '' else ' ' || node_type end\n" +
				"    when difference_time_percentage_string is null then '(' || id_explain_plan_property || ') Time Diff: (' || difference_time_units_string || ')'\n" +
				"    when difference_time_units_string is null then '(' || id_explain_plan_property || ') Time Diff: ' || difference_time_percentage_string || '%'\n" +
				"    else '(' || id_explain_plan_property || ') Time Diff: ' || difference_time_percentage_string || '% (' || difference_time_units_string || ')'\n" +
				"  end\n" + 
				", difference_time_highlight_1 = case\n" + 
				"    when difference_time_percentage_string*1.0 = (select max(difference_time_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1) then 1\n" + 
				"    else 0\n" + 
				"  end\n" + 
				", difference_time_highlight_2 = case\n" + 
				"    when difference_time_percentage_string*1.0 = (select max(difference_time_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1 and difference_time_percentage_string*1.0 < (select max(difference_time_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1)) then 1\n" + 
				"    else 0\n" + 
				"  end\n" +
				
				// exclusive / operator time
				", exclusive_time_label_string = case\n" +
				"    when exclusive_time_percentage_string is null and exclusive_time_units_string is null then '(' || id_explain_plan_property || ')' || case when node_type is null or node_type = '' then '' else ' ' || node_type end\n" +
				"    when exclusive_time_percentage_string is null then '(' || id_explain_plan_property || ') Opr Time: (' || exclusive_time_units_string || ')'\n" +
				"    when exclusive_time_units_string is null then '(' || id_explain_plan_property || ') Opr Time: ' || exclusive_time_percentage_string || '%'\n" +
				"    else '(' || id_explain_plan_property || ') Opr Time: ' || exclusive_time_percentage_string || '% (' || exclusive_time_units_string || ')'\n" +
				"  end\n" + 
				", exclusive_time_highlight_1 = case\n" + 
				"    when exclusive_time_percentage_string*1.0 = (select max(exclusive_time_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1) then 1\n" + 
				"    else 0\n" + 
				"  end\n" + 
				", exclusive_time_highlight_2 = case\n" + 
				"    when exclusive_time_percentage_string*1.0 = (select max(exclusive_time_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1 and exclusive_time_percentage_string*1.0 < (select max(exclusive_time_percentage_string*1.0) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property <> -1)) then 1\n" + 
				"    else 0\n" + 
				"  end\n" +
				
				
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				"and EPOA.id_explain_plan_property <> -1\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (6): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }


		if (bolShowTiming == true) { System.out.println("before updateStringUnitsPercentageAnnotationsTextData (7): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		// unhighlight where values are zero
		// for one large plan, the full update command took ~3.5 sec; after removing this part of the update code took ~2 sec
		strSQL += "update explain_plan_operator_annotations set\n" +
				"cost_highlight_1 = 0, cost_highlight_2 = 0\n" +
				"where coalesce(cost_percentage_string*1.0, 0) = 0\n" +
				"and id_explain_plan = " + intIDExplainPlan + ";\n" +
				
				// shoudn't be updating PL highlight here
				//"update explain_plan_operator_annotations set\n" +
				//"parallel_leader_difference_time_highlight_1 = 0, parallel_leader_difference_time_highlight_2 = 0\n" +
				//"parallel_leader_difference_time_highlight = 0\n" +
				//"where coalesce(parallel_leader_difference_time_percentage_string*1.0, 0) = 0\n" +
				//"and id_explain_plan = " + intIDExplainPlan + ";\n" +
				
				"update explain_plan_operator_annotations set\n" +
				"exclusive_time_highlight_1 = 0, exclusive_time_highlight_2 = 0\n" +
				"where coalesce(exclusive_time_percentage_string*1.0, 0) = 0\n" +
				"and id_explain_plan = " + intIDExplainPlan + ";\n" +
				
				"update explain_plan_operator_annotations set\n" +
				//"parallel_leader_exclusive_time_highlight_1 = 0, parallel_leader_exclusive_time_highlight_2 = 0\n" +
				"parallel_leader_exclusive_time_highlight = 0\n" +
				"where coalesce(parallel_leader_exclusive_time_percentage_string*1.0, 0) = 0\n" +
				"and id_explain_plan = " + intIDExplainPlan + ";\n" +

				// Never-executed (Actual Loops = 0): no hotspot highlighting
				"update explain_plan_operator_annotations set\n" +
				"cost_highlight_1 = 0, cost_highlight_2 = 0\n" +
				", exclusive_time_highlight_1 = 0, exclusive_time_highlight_2 = 0\n" +
				", difference_time_highlight_1 = 0, difference_time_highlight_2 = 0\n" +
				", north_1_highlight_1 = 0, north_1_highlight_2 = 0\n" +
				"where id_explain_plan = " + intIDExplainPlan + "\n" +
				"and actual_loops is not null and trim(actual_loops) <> ''\n" +
				"and cast(actual_loops as real) = 0;\n" +
				"\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
		if (bolUseTransaction == true) { strSQL = "begin transaction;\n" + strSQL + "commit;\n"; strSQL = ""; }
		//Database.dml(strSQL);
		if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }

		if (bolShowTiming == true) { System.out.println("after updateStringUnitsPercentageAnnotationsTextData (7): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
	}

	public void updateButtonsToEnable(int intIDExplainPlan) {
		
		boolean bolShowSQL = false;
		String strRetrieveSQL;
		String strSQL = "";
		boolean bolConcatenateSQL = true; // trying out "true"
		boolean bolUseTransaction = true;

		// set which "Line Thickness By" and "Percentage By" buttons to enable
		strSQL += "update explain_plan_generated_values as EPGV set\n" +
				"actual_rows_enabled = case when exists(select * from explain_plan_operator_annotations where total_actual_rows is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", estimated_rows_enabled = case when exists(select * from explain_plan_operator_annotations where total_estimated_rows is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", actual_mass_enabled = case when exists(select * from explain_plan_operator_annotations where actual_data_size_bytes is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", estimated_mass_enabled = case when exists(select * from explain_plan_operator_annotations where estimated_data_size_bytes is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", actual_vs_estimated_rows_ratio_enabled = case when exists(select * from explain_plan_operator_annotations where total_actual_rows is not null and total_estimated_rows is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", actual_vs_estimated_mass_ratio_enabled = case when exists(select * from explain_plan_operator_annotations where actual_data_size_bytes is not null and estimated_data_size_bytes is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", cost_enabled = case when exists(select * from explain_plan_operator_annotations where total_cost is not null and id_explain_plan = EPGV.id_explain_plan) then 1 else 0 end\n" +
				", exclusive_time_enabled = case when exists(select * from explain_plan_operator_annotations where exclusive_time_ms is not null and id_explain_plan = EPGV.id_explain_plan and id_explain_plan_property <> -1) then 1 else 0 end\n" +
				", time_difference_enabled = case when exists(select * from explain_plan_operator_annotations where parallel_leader_difference_time_ms is not null and id_explain_plan = EPGV.id_explain_plan and id_explain_plan_property <> -1) then 1 else 0 end\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolConcatenateSQL == false && bolShowSQL == true) { System.out.println(strSQL); }
		if (bolConcatenateSQL == false && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		
		if (bolUseTransaction == true) { strSQL = "begin transaction;\n" + strSQL + "commit;\n"; }
		if (bolConcatenateSQL == true && strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		
	}
	
/*
select id_explain_plan_property, cost_percentage_string, cost_label_string, disk_label_string
, disk_plus_ram_label_string, ram_label_string, exclusive_time_label_string
from explain_plan_operator_annotations
*/
	public void generateParallelLeadersAnnotationsData(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strRetrieveSQL;
		String strSQL = "";
		JsonArray jsnRows;
		JsonObject jsnRow;
		boolean bolShowTiming = false;
		
		if (bolShowTiming == true) { System.out.println("before generateParallelLeadersAnnotationsData (1): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		// write calculated parallel page / block values
		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"parallel_leader_actual_rows = actual_rows\n" +
				//", parallel_leader_estimated_rows = plan_rows\n" + // NEW
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				";\n" +
				"\n";
		
		if (bolShowSQL == true) { System.out.println(strSQL); }
		if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }
/*
select id_explain_plan_property, actual_rows, parallel_leader_actual_rows
from explain_plan_operator_annotations;
*/

		strSQL += "update explain_plan_operator_annotations as EPOA set\n" +
				"parallel_leader_actual_data_size_bytes = parallel_leader_actual_rows*1.0 * plan_width*1.0\n" + 
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				";\n" +
				";\n";
		
		if (bolShowSQL == true) { System.out.println(strSQL); }
		if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }
/*
select id_explain_plan_property, parallel_leader_actual_rows, plan_width, parallel_leader_actual_data_size_bytes
from explain_plan_operator_annotations;
*/
		if (bolShowTiming == true) { System.out.println("after generateParallelLeadersAnnotationsData (1): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
	}
		
	// write all possible line thickness (arrow) text, and thickness size numbers
	public void updateLineThicknessTextData(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		boolean bolShowProperties = false;
		String strSQL;
		JsonObject jsnRow;
		int intMaxLineThickness = 13;
		Double dblMaxPlanRows;
		Double dblMaxPlanDataMassBytes;
		
		//setLineThicknessByActualRowsButton(true);

		strSQL = "select max(max(coalesce(total_estimated_rows, 0)), max(coalesce(total_actual_rows, 0))) as max_plan_rows\n" + 
				", max(max(coalesce(estimated_data_size_bytes, 0)), max(coalesce(actual_data_size_bytes, 0))) as max_plan_data_mass_bytes\n" + 
				"from explain_plan_operator_annotations where id_explain_plan = " + intIDExplainPlan; 
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRow = Database.row(strSQL);
		dblMaxPlanRows = Utils.nullObjectToNullDouble(jsnRow.get("max_plan_rows"));
		//dblMaxPlanRows = (Double) jsnRow.get("max_plan_rows");
		dblMaxPlanDataMassBytes = Utils.nullObjectToNullDouble(jsnRow.get("max_plan_data_mass_bytes"));
		//dblMaxPlanDataMassBytes = (Double) jsnRow.get("max_plan_data_mass_bytes");
		//Double dblMaxPlanRows = jsnRow.get("max_plan_rows");
		//Double dblMaxPlanRows = (Double) jsnRow.get("max_plan_rows");
		//Double dblMaxPlanDataMassBytes = jsnRow.getDouble("max_plan_data_mass_bytes");
		//Double dblMaxPlanDataMassBytes = (Double) jsnRow.get("max_plan_data_mass_bytes");
		if (bolShowProperties == true) { System.out.println("dblMaxPlanRows: " + dblMaxPlanRows); }
		if (bolShowProperties == true) { System.out.println("dblMaxPlanDataMassBytes: " + dblMaxPlanDataMassBytes); }
		if (bolShowProperties == true) { System.out.println("Math.pow(dblMaxPlanDataMassBytes, 2): " + Math.pow(Utils.nullDoubleToZero(dblMaxPlanDataMassBytes), 2)); }
		if (bolShowProperties == true) { System.out.println("Math.log(Math.pow(dblMaxPlanDataMassBytes, 2)): " + Math.log(Math.pow(Utils.nullDoubleToZero(dblMaxPlanDataMassBytes), 2))); }
		double maxPlanRowsLogPow1 = Math.log(Math.pow(Utils.nullDoubleToZero(dblMaxPlanRows), 1));
		if (maxPlanRowsLogPow1 == Double.NEGATIVE_INFINITY) {
			maxPlanRowsLogPow1 = 0;
		}
		double maxPlanDataMassBytesLogPow2 = Math.log(Math.pow(Utils.nullDoubleToZero(dblMaxPlanDataMassBytes), 2));
		if (maxPlanDataMassBytesLogPow2 == Double.NEGATIVE_INFINITY) {
			maxPlanDataMassBytesLogPow2 = 0;
		}
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" + 
				"actual_data_size_line_thickness = max(case\n" +
				"    when actual_data_size_bytes is null then " + withZoomMinimumOne(1) + "\n" + 
				"    when '" + Utils.nullDoubleToEmptyString(dblMaxPlanDataMassBytes) + "' = '' or '" + Utils.nullDoubleToEmptyString(dblMaxPlanDataMassBytes) + "' = '0.0' then " + withZoomMinimumOne(1) + "\n" +
				"    else max(actual_data_size_bytes / 0" + Utils.nullDoubleToEmptyString(dblMaxPlanDataMassBytes) + " * min(" + intMaxLineThickness + ", " + maxPlanDataMassBytesLogPow2 + "), 1)\n" + 
				"  end * " + getZoomPercentage() + " / 100.0, 1)\n" +
				", actual_rows_line_thickness = max(case\n" +
				"    when total_actual_rows is null then " + withZoomMinimumOne(1) + "\n" + 
				"    when '" + Utils.nullDoubleToEmptyString(dblMaxPlanRows) + "' = '' then " + withZoomMinimumOne(1) + "\n" + 
				"    else max(total_actual_rows / 0" + Utils.nullDoubleToEmptyString(dblMaxPlanRows) + " * min(" + intMaxLineThickness + ", " + maxPlanRowsLogPow1 + "), 1)\n" + 
				"  end * " + getZoomPercentage() + " / 100.0, 1)\n" +

				", actual_vs_estimated_rows_ratio_line_thickness = case\n" +
				"    when total_actual_rows is null or total_estimated_rows is null then " + withZoomMinimumOne(1) + "\n" +
				"    when total_actual_rows*1.0 + total_estimated_rows*1.0 < 10000 then " + withZoomMinimumOne(1) + "\n" + // if < 10k rows total, ignore
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.1) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.1) then " + withZoomMinimumOne((int) ((1.0-0.1) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.2) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.2) then " + withZoomMinimumOne((int) ((1.0-0.2) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.3) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.3) then " + withZoomMinimumOne((int) ((1.0-0.3) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.4) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.4) then " + withZoomMinimumOne((int) ((1.0-0.4) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.5) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.5) then " + withZoomMinimumOne((int) ((1.0-0.5) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.6) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.6) then " + withZoomMinimumOne((int) ((1.0-0.6) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.7) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.7) then " + withZoomMinimumOne((int) ((1.0-0.7) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.8) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.8) then " + withZoomMinimumOne((int) ((1.0-0.8) * intMaxLineThickness)) + "\n" + 
				"    when (total_estimated_rows*1.0 < total_actual_rows*1.0 and total_estimated_rows*1.0 / total_actual_rows*1.0 < 0.9) or (total_actual_rows*1.0 < total_estimated_rows*1.0 and total_actual_rows*1.0 / total_estimated_rows*1.0 < 0.9) then " + withZoomMinimumOne((int) ((1.0-0.9) * intMaxLineThickness)) + "\n" + 
				"    else " + withZoomMinimumOne(1) + "\n" +
				"  end\n" +

				", actual_vs_estimated_mass_ratio_line_thickness = case\n" +
				"    when actual_data_size_bytes is null or estimated_data_size_bytes is null then " + withZoomMinimumOne(1) + "\n" +
				"    when actual_data_size_bytes*1.0 + estimated_data_size_bytes*1.0 < 50000 then " + withZoomMinimumOne(1) + "\n" + // if < 50k mass total, ignore
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.1) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.1) then " + withZoomMinimumOne((int) ((1.0-0.1) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.2) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.2) then " + withZoomMinimumOne((int) ((1.0-0.2) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.3) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.3) then " + withZoomMinimumOne((int) ((1.0-0.3) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.4) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.4) then " + withZoomMinimumOne((int) ((1.0-0.4) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.5) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.5) then " + withZoomMinimumOne((int) ((1.0-0.5) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.6) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.6) then " + withZoomMinimumOne((int) ((1.0-0.6) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.7) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.7) then " + withZoomMinimumOne((int) ((1.0-0.7) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.8) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.8) then " + withZoomMinimumOne((int) ((1.0-0.8) * intMaxLineThickness)) + "\n" + 
				"    when (estimated_data_size_bytes*1.0 < actual_data_size_bytes*1.0 and estimated_data_size_bytes*1.0 / actual_data_size_bytes*1.0 < 0.9) or (actual_data_size_bytes*1.0 < estimated_data_size_bytes*1.0 and actual_data_size_bytes*1.0 / estimated_data_size_bytes*1.0 < 0.9) then " + withZoomMinimumOne((int) ((1.0-0.9) * intMaxLineThickness)) + "\n" + 
				"    else " + withZoomMinimumOne(1) + "\n" +
				"  end\n" +

				", estimated_data_size_line_thickness = max(case\n" +
				"    when estimated_data_size_bytes is null then " + withZoomMinimumOne(1) + "\n" + 
				"    when '" + Utils.nullDoubleToEmptyString(dblMaxPlanDataMassBytes) + "' = '' or '" + Utils.nullDoubleToEmptyString(dblMaxPlanDataMassBytes) + "' = '0.0' then " + withZoomMinimumOne(1) + "\n" +
				"    else max(estimated_data_size_bytes / 0" + Utils.nullDoubleToEmptyString(dblMaxPlanDataMassBytes) + " * min(" + intMaxLineThickness + ", " + maxPlanDataMassBytesLogPow2 + "), 1)\n" + 
				"  end * " + getZoomPercentage() + " / 100.0, 1)\n" +
				", estimated_rows_line_thickness = max(case\n" +
				"    when total_estimated_rows is null then " + withZoomMinimumOne(1) + "\n" + 
				"    when '" + Utils.nullDoubleToEmptyString(dblMaxPlanRows) + "' = '' then " + withZoomMinimumOne(1) + "\n" + 
				"    else max(total_estimated_rows / 0" + Utils.nullDoubleToEmptyString(dblMaxPlanRows) + " * min(" + intMaxLineThickness + ", " + maxPlanRowsLogPow1 + "), 1)\n" + 
				"  end * " + getZoomPercentage() + " / 100.0, 1)\n" +

				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}

/*
select id_explain_plan_property, actual_data_size_bytes, actual_data_size_line_thickness
from explain_plan_operator_annotations
*/

	// lots of operators show how many rows were filtered out
	private String rowsRemovedByFilterOrIndexRecheck(Double dblRowsRemovedByFilter, boolean bolRowsRemovedByFilterIsExact, Double dblRowsRemovedByIndexRecheck, boolean bolRowsRemovedByIndexRecheckIsExact) {
		if (dblRowsRemovedByFilter != null && dblRowsRemovedByIndexRecheck == null) {
			return rowsRemovedPhrase(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, "removed by filter");
		}
		if (dblRowsRemovedByFilter == null && dblRowsRemovedByIndexRecheck != null) {
			return rowsRemovedPhrase(dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact, "removed by index recheck");
		}
		if (dblRowsRemovedByFilter != null && dblRowsRemovedByIndexRecheck != null) {
			if (dblRowsRemovedByFilter > dblRowsRemovedByIndexRecheck) {
				return rowsRemovedPhrase(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, "removed by filter");
			}
			return rowsRemovedPhrase(dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact, "removed by index recheck");
		}
		return "";
	}

	/** e.g. {@code 100 removed by join filter}, {@code ~1.2k removed by filter}; omit when 0. */
	private String rowsRemovedPhrase(Double count, boolean exact, String phrase) {
		if (count == null || count <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (!exact) {
			sb.append('~');
		}
		sb.append(Utils.units(count)).append(' ').append(phrase);
		return sb.toString();
	}

	/**
	 * Incremental Sort south line from Presorted Key children.
	 * One key → {@code Presorted by x.proname}; several → {@code Presorted by 3 values}.
	 */
	private String presortedBySouthLine(int idExplainPlan, int idExplainPlanProperty) {
		String strSQL = "select ITEM.operator_name as key_expr\n"
				+ "from explain_plan_properties as LIST_PROP\n"
				+ "inner join explain_plan_properties as ITEM on LIST_PROP.id_explain_plan = ITEM.id_explain_plan\n"
				+ "  and LIST_PROP.id_explain_plan_property = ITEM.parent_id_explain_plan_property\n"
				+ "where LIST_PROP.id_explain_plan = " + idExplainPlan + "\n"
				+ "and LIST_PROP.parent_id_explain_plan_property = " + idExplainPlanProperty + "\n"
				+ "and LIST_PROP.operator_name = 'Presorted Key'\n"
				+ "order by ITEM.id_explain_plan_property\n";
		JsonArray rows = Database.rows(strSQL);
		if (rows == null || rows.isEmpty()) {
			return "";
		}
		if (rows.size() == 1) {
			String key = ((JsonObject) rows.get(0)).getString("key_expr");
			if (key == null || key.isBlank()) {
				return "";
			}
			key = removeDatatypes(key);
			key = removeParenthesis(key);
			return prefaceAndTruncate("Presorted by ", key, 40);
		}
		return "Presorted by " + rows.size() + " values";
	}

	/** Prefer join-filter removals when present; else filter / index-recheck phrasing. */
	private String rowsRemovedSouthLine(Double filter, boolean filterExact,
			Double joinFilter, boolean joinExact,
			Double recheck, boolean recheckExact) {
		String joinLine = rowsRemovedPhrase(joinFilter, joinExact, "removed by join filter");
		if (joinLine != null && !joinLine.isEmpty()) {
			return joinLine;
		}
		return rowsRemovedByFilterOrIndexRecheck(filter, filterExact, recheck, recheckExact);
	}

	/** Parse annotation numeric fields without Integer overflow (huge EXPLAIN values). */
	private Double annotationDouble(JsonObject jsnObject, String key) {
		if (jsnObject == null || key == null) {
			return null;
		}
		Object value = jsnObject.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			return Double.isFinite(d) ? d : Double.POSITIVE_INFINITY;
		}
		String text = String.valueOf(value).trim();
		if (text.isEmpty() || text.equalsIgnoreCase("null")) {
			return null;
		}
		try {
			double d = Double.parseDouble(text);
			return Double.isFinite(d) ? d : Double.POSITIVE_INFINITY;
		} catch (NumberFormatException ex) {
			// Digit soup beyond Double / non-numeric → treat as over display cap
			return Double.POSITIVE_INFINITY;
		}
	}

	/**
	 * Prefer the larger of a computed total and the raw EXPLAIN property string.
	 * Protects against SQLite/Java int clamping of absurd values.
	 */
	private Double preferLargerAnnotationDouble(JsonObject jsnObject, String totalKey, String rawKey) {
		Double total = annotationDouble(jsnObject, totalKey);
		Double raw = annotationDouble(jsnObject, rawKey);
		if (total == null) {
			return raw;
		}
		if (raw == null) {
			return total;
		}
		return raw > total ? raw : total;
	}

	// REFACTOR - also updates field w/ space of entire operator,
	// operator_graphics_width
	//public void distributeUserSettingsToSouthFields(int intIDExplainPlan) {
	public void copyPropertiesForSouth(int intIDExplainPlan) {
		String strNodeType;
		String strSouth1Text, strSouth2Text, strSouth3Text, strSouth4Text, strSouth5Text;
		String strRelationName, strIndexName, strAlias, strSubplanName, strPeakMemoryUsage;
		String strStrategy, strCommand, strCTEName, strPartialMode;
		Double dblRowsRemovedByFilter, dblRowsRemovedByJoinFilter, dblRowsRemovedByIndexRecheck;
		boolean bolRowsRemovedByFilterIsExact, bolRowsRemovedByJoinFilterIsExact, bolRowsRemovedByIndexRecheckIsExact;
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnObject;
		Double dblTotalExecutionTime;
		//FontMetrics defaultFontMetrics;
		
		//defaultFontMetrics = getFontMetrics(new Font(OPERATOR_FONT_NAME, OPERATOR_FONT_STYLE, withZoom(OPERATOR_FONT_SIZE)));

		strSQL = "select\n"
				+ "*\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
	
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnObject = (JsonObject) jsnRows.get(intRow);

				strSouth1Text = "";
				strSouth2Text = "";
				strSouth3Text = "";
				strSouth4Text = "";
				strSouth5Text = "";

				strNodeType = jsnObject.getString("node_type");
				if (strNodeType == null) strNodeType = "";
				boolean textNativeNodeType = isTextNativeNodeTypeLabel(strNodeType);

				// add "Parallel" and/or "Reverse" and/or hash strategy where necessary
				// (skip when Node Type already includes them — typical of text EXPLAIN names)
				if (!textNativeNodeType
						&& jsnObject.getString("parallel_aware") != null
						&& jsnObject.getString("parallel_aware").matches("true") == true) {
					strSouth1Text += "Parallel ";
				}
				if (!textNativeNodeType && jsnObject.getString("partial_mode") != null) {
					strSouth1Text += jsnObject.getString("partial_mode") + " ";
				}
				if (!textNativeNodeType && jsnObject.getString("strategy") != null
						&& jsnObject.getString("strategy").matches("Plain") != true) {
					strSouth1Text += jsnObject.getString("strategy") + " ";
				}
				if (jsnObject.getString("scan_direction") != null && jsnObject.getString("scan_direction").matches("Forward") != true) {
					strSouth1Text += jsnObject.getString("scan_direction") + " ";
				}
				if (jsnObject.getString("sampling_method") != null && jsnObject.getString("sampling_method") != "") {
					String strSamplingMethod = jsnObject.getString("sampling_method");
					strSouth1Text += strSamplingMethod.substring(0, 1).toUpperCase() + strSamplingMethod.substring(1) + " ";
				}

				strSouth1Text += displayNodeTypeName(strNodeType);

				if (jsnObject.getString("filter") != null || jsnObject.getString("join_filter") != null) {
					strSouth1Text += " w/Filter";
				}
				
				if (jsnObject.getString("rows_removed_by_index_recheck") != null) {
					strSouth1Text += " w/Recheck";
				}
				
				strAlias = blankIfNullish(jsnObject.getString("alias"));
				strCommand = blankIfNullish(jsnObject.getString("command"));
				strCTEName = blankIfNullish(jsnObject.getString("cte_name"));
				strIndexName = blankIfNullish(jsnObject.getString("index_name"));
				strPeakMemoryUsage = blankIfNullish(jsnObject.getString("peak_memory_usage"));
				// Values Scan often has no Relation Name; never let Java null become the label "null".
				strRelationName = blankIfNullish(jsnObject.getString("relation_name"));
				strPartialMode = blankIfNullish(jsnObject.getString("partial_mode"));
				dblRowsRemovedByFilter = null;
				dblRowsRemovedByJoinFilter = null;
				dblRowsRemovedByIndexRecheck = null;
				bolRowsRemovedByFilterIsExact = true;
				bolRowsRemovedByJoinFilterIsExact = true;
				bolRowsRemovedByIndexRecheckIsExact = true;

				if (jsnObject.getString("total_rows_removed_by_filter") != null
						|| jsnObject.getString("rows_removed_by_filter") != null) {
					dblRowsRemovedByFilter = preferLargerAnnotationDouble(jsnObject,
							"total_rows_removed_by_filter", "rows_removed_by_filter");
					Integer exactFilter = jsnObject.getInteger("total_rows_removed_by_filter_is_exact");
					bolRowsRemovedByFilterIsExact = (exactFilter != null && exactFilter == 1);
				}
				if (jsnObject.getString("total_rows_removed_by_join_filter") != null
						|| jsnObject.getString("rows_removed_by_join_filter") != null) {
					dblRowsRemovedByJoinFilter = preferLargerAnnotationDouble(jsnObject,
							"total_rows_removed_by_join_filter", "rows_removed_by_join_filter");
					Integer exactJoin = jsnObject.getInteger("total_rows_removed_by_join_filter_is_exact");
					bolRowsRemovedByJoinFilterIsExact = (exactJoin != null && exactJoin == 1);
				}

				if (jsnObject.getString("total_rows_removed_by_index_recheck") != null
						|| jsnObject.getString("rows_removed_by_index_recheck") != null) {
					dblRowsRemovedByIndexRecheck = preferLargerAnnotationDouble(jsnObject,
							"total_rows_removed_by_index_recheck", "rows_removed_by_index_recheck");
					Integer exactRecheck = jsnObject.getInteger("total_rows_removed_by_index_recheck_is_exact");
					bolRowsRemovedByIndexRecheckIsExact = (exactRecheck != null && exactRecheck == 1);
				}
				strStrategy = jsnObject.getString("strategy");
				strSubplanName = jsnObject.getString("subplan_name");
				if (strAlias == null)
					strAlias = "";
				// Literal compare — relation names like *VALUES* are not regexes (matches() would throw).
				if (strAlias != null && strRelationName != null && strAlias.equalsIgnoreCase(strRelationName)) {
					strAlias = "";
				}
				if (strAlias != "") { strAlias = "as " + strAlias; }
				
				if (strNodeType.equals("Aggregate") == true
						|| strNodeType.matches("(?i)(Hash|Group|Mixed)Aggregate")
						|| strNodeType.matches("(?i)(Partial|Finalize)\\s+(Hash|Group|Mixed)?\\s*Aggregate")) {
					strSouth2Text = jsnObject.getString("filter");
					strSouth2Text = removeDatatypes(strSouth2Text);
					strSouth2Text = removeParenthesis(strSouth2Text);
					strSouth2Text = prefaceAndTruncate("Filter: ", strSouth2Text, 40);
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Append") == true) {
					if (strIndexName != null)
						strSouth2Text = strIndexName;
				} else if (strNodeType.equals("BitmapAnd") == true) {
				} else if (strNodeType.equals("BitmapOr") == true) {
				} else if (strNodeType.equals("Bitmap Heap Scan") == true) {
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					strSouth4Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Bitmap Index Scan") == true) {
					strSouth2Text = strIndexName;
					strSouth3Text = jsnObject.getString("index_cond");
					strSouth3Text = removeDatatypes(strSouth3Text);
					strSouth3Text = removeParenthesis(strSouth3Text);
					strSouth3Text = prefaceAndTruncate("Index cond: ", strSouth3Text, 40);
					strSouth4Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("With CTE") == true) {
					if (strCTEName != null)
						strSouth2Text = strCTEName;
				} else if (strNodeType.equals("CTE Scan") == true) {
					strSouth2Text = jsnObject.getString("cte_name");
					strSouth3Text = strAlias;
					strSouth4Text = jsnObject.getString("filter");
					strSouth4Text = removeDatatypes(strSouth4Text);
					strSouth4Text = removeParenthesis(strSouth4Text);
					strSouth4Text = prefaceAndTruncate("Filter: ", strSouth4Text, 40);
					strSouth5Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Custom Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Foreign Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Function Scan") == true) {
					if (strAlias.equalsIgnoreCase(jsnObject.getString("function_name")))
						strSouth2Text = jsnObject.getString("function_call");
					else
						strSouth2Text = strAlias;
				} else if (strNodeType.equals("Gather") == true || strNodeType.equals("Gather Merge") == true) {
					strSouth2Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					if (jsnObject.getString("workers_planned") != null) {
						strSouth3Text = "Workers planned: " + jsnObject.getString("workers_planned");
					}
					if (jsnObject.getString("workers_launched") != null) {
						strSouth4Text = "Workers launched: " + jsnObject.getString("workers_launched");
					}
					if (jsnObject.getString("workers_planned") != null && jsnObject.getString("workers_launched") != null) {
						if (jsnObject.getString("workers_planned").equals(jsnObject.getString("workers_launched"))) {
							strSouth4Text = "";
							strSouth3Text = "Workers planned & launched: " + jsnObject.getString("workers_launched");
						}
					}
				} else if (strNodeType.equals("Group") == true) {
					strSouth2Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Hash") == true) {
					if (strPeakMemoryUsage != null && !strPeakMemoryUsage.isBlank()) {
						strSouth2Text = Utils.kilobytes(Double.parseDouble(strPeakMemoryUsage)) + " RAM";
						if (jsnObject.getString("parallel_aware") != null && jsnObject.getString("parallel_aware").matches("true") == true)
							strSouth2Text += " shared";
						else
							strSouth2Text += " used";
					}
				} else if (strNodeType.equals("Hash Join") == true
						|| strNodeType.matches("(?i)(Parallel\\s+)?Hash(\\s+\\w+)?\\s+Join")) {
					if (!isTextNativeNodeTypeLabel(strNodeType)) {
						strSouth2Text = jsnObject.getString("join_type");
						strSouth2Text = "(" + strSouth2Text + " Join)";
						strSouth3Text = jsnObject.getString("hash_cond");
						strSouth3Text = removeDatatypes(strSouth3Text);
						strSouth3Text = removeParenthesis(strSouth3Text);
						strSouth3Text = prefaceAndTruncate("Hash cond: ", strSouth3Text, 40);
						strSouth4Text = jsnObject.getString("join_filter");
						strSouth4Text = removeDatatypes(strSouth4Text);
						strSouth4Text = removeParenthesis(strSouth4Text);
						strSouth4Text = prefaceAndTruncate("Filter: ", strSouth4Text, 40);
						strSouth5Text = rowsRemovedSouthLine(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact,
								dblRowsRemovedByJoinFilter, bolRowsRemovedByJoinFilterIsExact,
								dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					} else {
						// e.g. "Parallel Hash Anti Join" — Hash Cond if present
						strSouth2Text = jsnObject.getString("hash_cond");
						strSouth2Text = removeDatatypes(strSouth2Text);
						strSouth2Text = removeParenthesis(strSouth2Text);
						strSouth2Text = prefaceAndTruncate("Hash cond: ", strSouth2Text, 40);
						strSouth3Text = jsnObject.getString("join_filter");
						strSouth3Text = removeDatatypes(strSouth3Text);
						strSouth3Text = removeParenthesis(strSouth3Text);
						strSouth3Text = prefaceAndTruncate("Filter: ", strSouth3Text, 40);
						strSouth4Text = rowsRemovedSouthLine(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact,
								dblRowsRemovedByJoinFilter, bolRowsRemovedByJoinFilterIsExact,
								dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					}
				} else if (strNodeType.equals("Index Only Scan") == true) {
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					strSouth4Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					strSouth5Text = strIndexName;
				} else if (strNodeType.equals("Index Scan") == true
						|| strNodeType.matches("(?i)Parallel\\s+Index Scan")) {
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					String indexCond = jsnObject.getString("index_cond");
					indexCond = removeDatatypes(indexCond);
					indexCond = removeParenthesis(indexCond);
					indexCond = prefaceAndTruncate("Index cond: ", indexCond, 40);
					String removed = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					if (indexCond != null && !indexCond.isBlank()) {
						// Index Cond at the bottom; Index Name just above it when both exist
						strSouth4Text = (strIndexName != null && !strIndexName.isBlank()) ? strIndexName : removed;
						strSouth5Text = indexCond;
					} else {
						strSouth4Text = removed;
						strSouth5Text = strIndexName;
					}
				} else if (strNodeType.equals("Incremental Sort") == true
						|| strNodeType.matches("(?i)Parallel\\s+Incremental Sort")) {
					Integer idProp = jsnObject.getInteger("id_explain_plan_property");
					if (idProp != null) {
						strSouth2Text = presortedBySouthLine(intIDExplainPlan, idProp.intValue());
					}
				} else if (strNodeType.equals("Limit") == true) {
					if (strSubplanName != null) strSouth2Text = strSubplanName;
				} else if (strNodeType.equals("LockRows") == true) {
				} else if (strNodeType.equals("Materialize") == true) {
				} else if (strNodeType.equals("Memoize") == true) {
					String cacheKey = jsnObject.getString("cache_key");
					if (cacheKey != null && !cacheKey.isBlank()) {
						strSouth2Text = removeDatatypes(cacheKey);
						strSouth2Text = removeParenthesis(strSouth2Text);
						strSouth2Text = prefaceAndTruncate("", strSouth2Text, 40);
					}
					if (strPeakMemoryUsage != null && !strPeakMemoryUsage.isBlank()) {
						strSouth3Text = Utils.kilobytes(Double.parseDouble(strPeakMemoryUsage)) + " RAM used";
					}
				} else if (strNodeType.equals("Merge Append") == true) {
				} else if (strNodeType.equals("Merge Join") == true
						|| strNodeType.matches("(?i)(Parallel\\s+)?Merge(\\s+\\w+)?\\s+Join")) {
					if (!isTextNativeNodeTypeLabel(strNodeType)) {
						strSouth2Text = jsnObject.getString("join_type");
						strSouth2Text = "(" + strSouth2Text + " Join)";
						strSouth3Text = jsnObject.getString("merge_cond");
						strSouth3Text = removeDatatypes(strSouth3Text);
						strSouth3Text = removeParenthesis(strSouth3Text);
						strSouth3Text = prefaceAndTruncate("Merge cond: ", strSouth3Text, 40);
						strSouth4Text = jsnObject.getString("join_filter");
						strSouth4Text = removeDatatypes(strSouth4Text);
						strSouth4Text = removeParenthesis(strSouth4Text);
						strSouth4Text = prefaceAndTruncate("Filter: ", strSouth4Text, 40);
						strSouth5Text = rowsRemovedSouthLine(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact,
								dblRowsRemovedByJoinFilter, bolRowsRemovedByJoinFilterIsExact,
								dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					} else {
						// Merge Cond if present (e.g. plain or Parallel Merge … Join)
						strSouth2Text = jsnObject.getString("merge_cond");
						strSouth2Text = removeDatatypes(strSouth2Text);
						strSouth2Text = removeParenthesis(strSouth2Text);
						strSouth2Text = prefaceAndTruncate("Merge cond: ", strSouth2Text, 40);
						strSouth3Text = jsnObject.getString("join_filter");
						strSouth3Text = removeDatatypes(strSouth3Text);
						strSouth3Text = removeParenthesis(strSouth3Text);
						strSouth3Text = prefaceAndTruncate("Filter: ", strSouth3Text, 40);
						strSouth4Text = rowsRemovedSouthLine(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact,
								dblRowsRemovedByJoinFilter, bolRowsRemovedByJoinFilterIsExact,
								dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					}
				} else if (strNodeType.equals("ModifyTable") == true
						|| strNodeType.equalsIgnoreCase("Insert")
						|| strNodeType.equalsIgnoreCase("Update")
						|| strNodeType.equalsIgnoreCase("Delete")
						|| strNodeType.equalsIgnoreCase("Merge")) {
					String operation = jsnObject.getString("operation");
					if (operation == null || operation.isBlank()) {
						operation = strNodeType;
					}
					strSouth2Text = operation;
					if (strSouth2Text.equalsIgnoreCase("insert") == true) {
						strSouth2Text += " into";
					} else if (strSouth2Text.equalsIgnoreCase("delete") == true) {
						strSouth2Text += " from";
					}
					if (strRelationName != null && !strRelationName.isBlank()) {
						strSouth2Text += " " + strRelationName;
					}
					// Merge: keep alias on its own south line ("as t")
					if (operation.equalsIgnoreCase("merge") == true) {
						strSouth3Text = strAlias;
					} else if (strAlias != null && !strAlias.isBlank()) {
						strSouth2Text += " " + strAlias;
					}
				} else if (strNodeType.equals("Named Tuplestore Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Nested Loop") == true
						|| strNodeType.matches("(?i)Nested Loop(\\s+\\w+\\s+Join)?")) {
					if (!isTextNativeNodeTypeLabel(strNodeType)) {
						strSouth2Text = jsnObject.getString("join_type");
						strSouth2Text = "(" + strSouth2Text + " Join)";
						strSouth3Text = jsnObject.getString("join_filter");
						strSouth3Text = removeDatatypes(strSouth3Text);
						strSouth3Text = removeParenthesis(strSouth3Text);
						strSouth3Text = prefaceAndTruncate("Filter: ", strSouth3Text, 40);
						strSouth4Text = rowsRemovedSouthLine(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact,
								dblRowsRemovedByJoinFilter, bolRowsRemovedByJoinFilterIsExact,
								dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					} else {
						strSouth2Text = jsnObject.getString("join_filter");
						strSouth2Text = removeDatatypes(strSouth2Text);
						strSouth2Text = removeParenthesis(strSouth2Text);
						strSouth2Text = prefaceAndTruncate("Filter: ", strSouth2Text, 40);
						strSouth3Text = rowsRemovedSouthLine(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact,
								dblRowsRemovedByJoinFilter, bolRowsRemovedByJoinFilterIsExact,
								dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
					}
				} else if (strNodeType.equals("ProjectSet") == true) {
				} else if (strNodeType.equals("Recursive Union") == true) {
				} else if (strNodeType.equals("Result") == true) {
					strSouth2Text = jsnObject.getString("one_time_filter");
					strSouth2Text = removeDatatypes(strSouth2Text);
					strSouth2Text = removeParenthesis(strSouth2Text);
					strSouth2Text = prefaceAndTruncate("", strSouth2Text, 40);
					if (strSubplanName != null)
						strSouth3Text = strSubplanName;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Sample Scan") == true) {
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					strSouth4Text = jsnObject.getString("filter");
					strSouth4Text = removeDatatypes(strSouth4Text);
					strSouth4Text = removeParenthesis(strSouth4Text);
					strSouth4Text = prefaceAndTruncate("", strSouth4Text, 40);
					strSouth5Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Seq Scan") == true
						|| strNodeType.matches("(?i)Parallel\\s+Seq Scan")) {
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					strSouth4Text = jsnObject.getString("filter");
					strSouth4Text = removeDatatypes(strSouth4Text);
					strSouth4Text = removeParenthesis(strSouth4Text);
					strSouth4Text = prefaceAndTruncate("Filter: ", strSouth4Text, 40);
					strSouth5Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("SetOp") == true) {
					if (strCommand != null)
						strSouth2Text = strCommand;
				} else if (strNodeType.equals("Sort") == true) {
					if (jsnObject.getString("sort_space_type") != null)
						strSouth1Text = jsnObject.getString("sort_space_type") + " " + strSouth1Text;
					strSouth2Text = jsnObject.getString("sort_space_used");
					if (strSouth2Text == null || strSouth2Text.isEmpty()) {
						strSouth2Text = "";
					} else {
						strSouth2Text = Utils.kilobytes(Double.parseDouble(strSouth2Text)); // it's in kb, not pages
						strSouth2Text += " ";
						if (jsnObject.getString("sort_space_type") != null
								&& jsnObject.getString("sort_space_type").equalsIgnoreCase("Memory")) {
							strSouth2Text += "RAM";
						} else {
							strSouth2Text += "space";
						}
						strSouth2Text += " used";
					}
					if (jsnObject.getString("sort_method") != null) {
						strSouth3Text = jsnObject.getString("sort_method");
					}
				} else if (strNodeType.equals("Subquery Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = jsnObject.getString("filter");
					strSouth3Text = removeDatatypes(strSouth3Text);
					strSouth3Text = removeParenthesis(strSouth3Text);
					strSouth3Text = prefaceAndTruncate("Filter: ", strSouth3Text, 40);
					strSouth4Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Table Function Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Tid Range Scan") == true) {
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					strSouth4Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Tid Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("Unique") == true) {
				} else if (strNodeType.equals("Values Scan") == true) {
					// south_1 = "Values Scan"; south_2 = Relation Name; south_3 = as Alias
					strSouth2Text = strRelationName;
					strSouth3Text = strAlias;
					strSouth4Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				} else if (strNodeType.equals("WindowAgg") == true) {
				} else if (strNodeType.equals("WorkTable Scan") == true) {
					strSouth2Text = strAlias;
					strSouth3Text = rowsRemovedByFilterOrIndexRecheck(dblRowsRemovedByFilter, bolRowsRemovedByFilterIsExact, dblRowsRemovedByIndexRecheck, bolRowsRemovedByIndexRecheckIsExact);
				}
								
				// Strip any remaining PG casts (NULL::name, ::anyarray, ::real[], …)
				strSouth1Text = removeDatatypes(strSouth1Text);
				strSouth2Text = removeDatatypes(strSouth2Text);
				strSouth3Text = removeDatatypes(strSouth3Text);
				strSouth4Text = removeDatatypes(strSouth4Text);
				strSouth5Text = removeDatatypes(strSouth5Text);

				// "" not null — Java string concat turns null into the literal "null" in SQL.
				strSouth1Text = blankIfNullish(strSouth1Text).replace("'", "''");
				strSouth2Text = blankIfNullish(strSouth2Text).replace("'", "''");
				strSouth3Text = blankIfNullish(strSouth3Text).replace("'", "''");
				strSouth4Text = blankIfNullish(strSouth4Text).replace("'", "''");
				strSouth5Text = blankIfNullish(strSouth5Text).replace("'", "''");
		
				// shift south text up where necessary
				if (strSouth3Text == "" && strSouth4Text != "") {
					strSouth3Text = strSouth4Text;
					strSouth4Text = "";
				}
				if (strSouth2Text == "" && strSouth3Text != "") {
					strSouth2Text = strSouth3Text;
					strSouth3Text = "";
				}
				// repeated, b/c 3 may be blank again
				if (strSouth3Text == "" && strSouth4Text != "") {
					strSouth3Text = strSouth4Text;
					strSouth4Text = "";
				}

				strSQL += "update explain_plan_operator_annotations set\n";
				strSQL += "south_1_text = '" + strSouth1Text + "'\n";
				strSQL += ", south_2_text = '" + strSouth2Text + "'\n";
				strSQL += ", south_3_text = '" + strSouth3Text + "'\n";
				strSQL += ", south_4_text = '" + strSouth4Text + "'\n";
				strSQL += ", south_5_text = '" + strSouth5Text + "'\n";
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += jsnObject.get("id_explain_plan_property");
				strSQL += ";\n";
			}

			strSQL += "update explain_plan_operator_annotations set\n";
			strSQL += "south_1_text = null where south_1_text = '' or lower(south_1_text) = 'null'\n";
			strSQL += "and id_explain_plan = " + intIDExplainPlan + "\n";
			strSQL += ";\n";
			strSQL += "update explain_plan_operator_annotations set\n";
			strSQL += "south_2_text = null where south_2_text = '' or lower(south_2_text) = 'null'\n";
			strSQL += "and id_explain_plan = " + intIDExplainPlan + "\n";
			strSQL += ";\n";
			strSQL += "update explain_plan_operator_annotations set\n";
			strSQL += "south_3_text = null where south_3_text = '' or lower(south_3_text) = 'null'\n";
			strSQL += "and id_explain_plan = " + intIDExplainPlan + "\n";
			strSQL += ";\n";
			strSQL += "update explain_plan_operator_annotations set\n";
			strSQL += "south_4_text = null where south_4_text = '' or lower(south_4_text) = 'null'\n";
			strSQL += "and id_explain_plan = " + intIDExplainPlan + "\n";
			strSQL += ";\n";
			strSQL += "update explain_plan_operator_annotations set\n";
			strSQL += "south_5_text = null where south_5_text = '' or lower(south_5_text) = 'null'\n";
			strSQL += "and id_explain_plan = " + intIDExplainPlan + "\n";
			strSQL += ";\n";
			
			// update south text of root operator
			strSQL += "update explain_plan_operator_annotations as EPOA set\n";
			strSQL += "south_1_text = 'Actual rows: ' || case\n";
			strSQL += "  when (select total_actual_rows from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) is null then 'n/a' else (select printf('%,d', total_actual_rows) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) end\n";
			strSQL += ", south_2_text = 'Estimated rows: ' || case\n";
			strSQL += "  when (select plan_rows from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) is null then 'n/a' else (select printf('%,d', plan_rows) from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) end\n";
			strSQL += ", south_3_text = 'Actual data mass: ' || case\n";
			strSQL += "  when (select actual_data_size_units_string from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) is null then 'n/a' else (select actual_data_size_units_string from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) end\n";
			strSQL += ", south_4_text = 'Estimated data mass: ' || case\n";
			strSQL += "  when (select estimated_data_size_units_string from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) is null then 'n/a' else (select estimated_data_size_units_string from explain_plan_operator_annotations where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property > -1 order by id_explain_plan_property limit 1) end\n";
			strSQL += "where EPOA.id_explain_plan = " + intIDExplainPlan + "\n";
			strSQL += "and EPOA.id_explain_plan_property = -1;\n";
		
			dblTotalExecutionTime = (Double) Database.row("select execution_time_ms from explain_plan_generated_values where id_explain_plan = " + intIDExplainPlan).get("execution_time_ms");
			
			if (dblTotalExecutionTime != null) {
				strSQL += "update explain_plan_operator_annotations as EPOA set\n";
				strSQL += "south_5_text = 'Total execution time: " + Utils.elapsed_time_ms_one_decimal((double) dblTotalExecutionTime) + "'\n";
				strSQL += "where EPOA.id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and EPOA.id_explain_plan_property = -1;\n";
			} else {
				strSQL += "update explain_plan_operator_annotations as EPOA set\n";
				strSQL += "south_5_text = null\n";
				strSQL += "where EPOA.id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and EPOA.id_explain_plan_property = -1;\n";
			}
			
			strSQL = "begin transaction;\n" + strSQL + "commit;\n";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
		}
/*		
		strSQL = "select * from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		strDrawString = "Total execution time: ";
		if (jsnRow.get("execution_time_ms") != null) {
			strDrawString += Utils.elapsed_time_ms((double) jsnRow.get("execution_time_ms"));
		} else {
			strDrawString += "(not found in query plan)";
		}
*/		
		
	}

/*
select cost_label_string, disk_plus_ram_label_string, disk_label_string, ram_label_string, exclusive_time_label_string
, arrow_text
from explain_plan_operator_annotations
*/

	/*
	method name:
		copyPropertiesForWorkers
	description:
	update in explain_plan_operator_workers:
		actual_data_size_bytes, disk_pages, disk_plus_ram_pages, ram_pages, exclusive_time_ms
		, actual_data_size_units_string, actual_rows_units_string, disk_units_string, disk_plus_ram_units_string, exclusive_time_units_string, ram_units_string 
	*/
	public void copyPropertiesForWorkers(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		boolean bolShowTiming = false;
		String strSQL;
		JsonArray jsnRows;
		JsonObject jsnRow;
		
		// consider concatenating sql commands later
		
		if (bolShowTiming == true) { System.out.println("before copyPropertiesForWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		// set per-operator totals
		strSQL =
/*
				"update explain_plan_operator_workers set\n" +
				//"actual_data_size_bytes = cast(actual_rows as numeric) * cast(actual_loops as numeric) * 8192\n" + 
				//"actual_data_size_bytes = actual_rows*1.0 * actual_loops * 8192\n" +
				"disk_pages = coalesce(shared_read_blocks, 0) + coalesce(shared_dirtied_blocks, 0) + coalesce(shared_written_blocks, 0) + coalesce(temp_read_blocks, 0) + coalesce(temp_written_blocks, 0)\n" + 
				", disk_plus_ram_pages = coalesce(shared_read_blocks, 0) + coalesce(shared_dirtied_blocks, 0) + coalesce(shared_written_blocks, 0) + coalesce(temp_read_blocks, 0) + coalesce(temp_written_blocks, 0) + coalesce(local_hit_blocks, 0) + coalesce(local_read_blocks, 0) + coalesce(local_dirtied_blocks, 0) + coalesce(local_written_blocks, 0) + coalesce(shared_hit_blocks, 0)\n" +
				", ram_pages = coalesce(local_hit_blocks, 0) + coalesce(local_read_blocks, 0) + coalesce(local_dirtied_blocks, 0) + coalesce(local_written_blocks, 0) + coalesce(shared_hit_blocks, 0)\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n" +
*/				
				// workers do not have "plan width", so we need to get it from the parallel leader
				"update explain_plan_operator_workers as SET_EPOW set\n" +
				"actual_data_size_bytes = (select\n" +
				"  EPOW.actual_rows*1.0 * EPOA.plan_width\n" +
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  inner join explain_plan_operator_workers as EPOW on EPOA.id_explain_plan = EPOW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = EPOW.id_explain_plan_property\n" + 
				"  where EPOA.id_explain_plan = SET_EPOW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SET_EPOW.id_explain_plan_property\n" + 
				"    and EPOW.worker_number = SET_EPOW.worker_number\n" + 
				")\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";

				// old way
				// update amount of time spent per worker
				//"update explain_plan_operator_workers set\n" + 
				//"exclusive_time_ms = actual_total_time_ms\n" + 
				//"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				//";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }
				
/*
select id_explain_plan_property, worker_number, actual_data_size_bytes
, disk_pages, disk_plus_ram_pages, exclusive_time_ms, ram_pages
from explain_plan_operator_workers order by id_explain_plan_property, worker_number
*/
		
		// set elapsed time and units string numerics for each operator
		// doing it RBAR style b/c I'm too lazy to write
		// sql code to convert time/pages/bytes to units
		// formatted prettily
		strSQL = "select\n"
				+ "id_explain_plan_property, worker_number, actual_data_size_bytes, actual_rows, difference_time_ms\n"
				+ ", actual_loops\n"
				+ ", exclusive_time_ms\n"
				+ "from explain_plan_operator_workers\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
	
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_workers set\n";
				strSQL += "actual_data_size_units_string = ";
				if (jsnRow.get("actual_data_size_bytes") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.bytes(jsnRow.getDouble("actual_data_size_bytes"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("actual_data_size_bytes") + " bytes";
				}
				strSQL += "\n";
				strSQL += ", actual_rows_units_string = ";
				if (jsnRow.get("actual_rows") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.units(jsnRow.getDouble("actual_rows"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("actual_rows") + " rows";
				}
				strSQL += "\n";
				strSQL += ", difference_time_units_string = ";
				if (jsnRow.get("difference_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("difference_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("difference_time_ms") + " ms";
				}
				strSQL += "\n";
				strSQL += ", exclusive_time_units_string = ";
				if (jsnRow.get("exclusive_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("exclusive_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("exclusive_time_ms") + " ms";
				}
				strSQL += "\n";
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property") + "\n";
				strSQL += "and worker_number = ";
				strSQL += (Integer) jsnRow.get("worker_number");
				strSQL += ";\n";
			}
			strSQL = "begin transaction;\n" + strSQL + "commit;\n";
			if (bolShowSQL == true) { System.out.println(strSQL); }
			Database.dml(strSQL);
			
/*
select id_explain_plan_property, worker_number
--, actual_data_size_bytes, actual_data_size_units_string
--, actual_rows, actual_rows_units_string
, disk_pages, disk_units_string
--, disk_plus_ram_pages, disk_plus_ram_units_string
--, exclusive_time_ms, exclusive_time_units_string
--, ram_pages, ram_units_string
from explain_plan_operator_workers order by id_explain_plan_property, worker_number
*/
			if (bolShowTiming == true) { System.out.println("after copyPropertiesForWorkers: " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		}
	}
	
	/*
	method name:
		copyPropertiesForParallelLeaders
	description:
	update in explain_plan_operator_annotations:
	 	parallel_leader_local_dirtied_blocks, parallel_leader_local_hit_blocks, parallel_leader_local_read_blocks, parallel_leader_local_written_blocks
	 	, parallel_leader_shared_dirtied_blocks, parallel_leader_shared_hit_blocks, parallel_leader_shared_read_blocks, parallel_leader_shared_written_blocks
	 	, parallel_leader_temp_read_blocks, parallel_leader_temp_written_blocks
	 	(parallel_leader_*_blocks above depend on explain_plan_operator_workers.*_blocks)
	 	, parallel_leader_actual_rows, // no need to update; it's already in the json plan  
		, parallel_leader_actual_data_size_bytes, parallel_leader_disk_pages, parallel_leader_disk_plus_ram_pages, parallel_leader_ram_pages
		// below is rbar
		, parallel_leader_actual_rows_units_string, parallel_leader_actual_data_size_units_string, parallel_leader_disk_plus_ram_units_string
		, parallel_leader_disk_units_string, parallel_leader_exclusive_time_units_string, parallel_leader_ram_units_string
	*/
	public void copyPropertiesForParallelLeaders(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strRetrieveSQL;
		String strSQL = "";
		JsonArray jsnRows;
		JsonObject jsnRow;
		boolean bolShowTiming = false;
		
		if (bolShowTiming == true) { System.out.println("before setOperatorParallelLeaders (1): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		strRetrieveSQL = "select\n" +
				"id_explain_plan_property, parallel_leader_actual_data_size_bytes, parallel_leader_actual_rows\n" +
				", parallel_leader_exclusive_time_ms\n" +
				"from explain_plan_operator_annotations as EPOA\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n";

		if (bolShowSQL == true) { System.out.println(strRetrieveSQL); }
	
		jsnRows = Database.rows(strRetrieveSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_annotations set\n";
				strSQL += "parallel_leader_actual_rows_units_string = ";
				if (jsnRow.get("parallel_leader_actual_rows") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.units(jsnRow.getDouble("parallel_leader_actual_rows"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("parallel_leader_actual_rows") + " rows";
				}
				strSQL += "\n";
				strSQL += ", parallel_leader_actual_data_size_units_string = ";
				if (jsnRow.get("parallel_leader_actual_data_size_bytes") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.bytes(jsnRow.getDouble("parallel_leader_actual_data_size_bytes"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("parallel_leader_actual_data_size_bytes") + " bytes";
				}
				strSQL += "\n";
				strSQL += ", parallel_leader_exclusive_time_units_string = ";
				if (jsnRow.get("parallel_leader_exclusive_time_ms") == null) {
					strSQL += "null";
				} else {
					strSQL += "'";
					strSQL += Utils.elapsed_time_ms(jsnRow.getDouble("parallel_leader_exclusive_time_ms"));
					strSQL += "'";
					//strSQL += " -- " + jsnRow.get("parallel_leader_exclusive_time_ms") + " ms";
				}
				strSQL += "\n";
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property");
				strSQL += ";\n";
			}
			if (bolShowSQL == true) { System.out.println(strSQL); }
			if (strSQL != "") { Database.dml(strSQL); strSQL = ""; }
		}
	}
	
/*
method name:
	calculatePercentagesBetweenWorkersAndParallelLeaders
summary:
	calculate PL and worker percentage strings: mass, rows, time diff., excl. time
description:
update in explain_plan_operator_annotations:
parallel_leader_exclusive_time_percentage_string, parallel_leader_actual_rows_percentage_string
, parallel_leader_actual_data_size_percentage_string
update in explain_plan_operator_workers:
	actual_data_size_percentage_string, actual_rows_percentage_string, exclusive_time_percentage_string
*/
	public void calculatePercentagesBetweenWorkersAndParallelLeaders(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;
		//JsonArray jsnRows;
		//JsonObject jsnRow;

		// set percentage string numerics for explain_plan_operator_annotations / exclusive_time_ms
		strSQL = "update explain_plan_operator_annotations set\n" + 
				"(parallel_leader_actual_data_size_percentage_string, parallel_leader_actual_rows_percentage_string\n" +
				", parallel_leader_difference_time_percentage_string\n" +
				", parallel_leader_exclusive_time_percentage_string\n" +
				") = (select \n" +
				"case\n" + 
				"  when parallel_leader_actual_data_size_bytes is null or actual_data_size_bytes = 0 then null\n" + 
				//"  when parallel_leader_actual_data_size_bytes is null or actual_data_size_bytes = 0 then 0\n" +
				"  when parallel_leader_actual_data_size_bytes*1.0 / actual_data_size_bytes < 0.01 then 0\n" + 
				"  when parallel_leader_actual_data_size_bytes*1.0 / actual_data_size_bytes < 0.1 then round(parallel_leader_actual_data_size_bytes*1.0 / actual_data_size_bytes, 3) * 100\n" + 
				"  else cast((parallel_leader_actual_data_size_bytes*1.0 / actual_data_size_bytes) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				", case\n" + 
				"  when parallel_leader_actual_rows is null or total_actual_rows = 0 then null\n" + 
				//"  when parallel_leader_actual_rows is null or total_actual_rows = 0 then 0\n" +
				"  when parallel_leader_actual_rows*1.0 / total_actual_rows < 0.01 then 0\n" + 
				"  when parallel_leader_actual_rows*1.0 / total_actual_rows < 0.1 then round(parallel_leader_actual_rows*1.0 / total_actual_rows, 3) * 100\n" + 
				"  else cast((parallel_leader_actual_rows*1.0 / total_actual_rows) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				", case\n" + 
				"  when parallel_leader_difference_time_ms is null or difference_time_ms = 0 then null\n" + 
				//"  when parallel_leader_difference_time_ms is null or difference_time_ms = 0 then 0\n" +
				"  when parallel_leader_difference_time_ms*1.0 / difference_time_ms < 0.01 then 0\n" + 
				"  when parallel_leader_difference_time_ms*1.0 / difference_time_ms < 0.1 then round(parallel_leader_difference_time_ms*1.0 / difference_time_ms, 3) * 100\n" + 
				"  else cast((parallel_leader_difference_time_ms*1.0 / difference_time_ms) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				", case\n" + 
				"  when parallel_leader_exclusive_time_ms is null or exclusive_time_ms = 0 then null\n" +
				//"  when parallel_leader_exclusive_time_ms is null or exclusive_time_ms = 0 then 0\n" +
				"  when parallel_leader_exclusive_time_ms*1.0 / exclusive_time_ms < 0.01 then 0\n" + 
				"  when parallel_leader_exclusive_time_ms*1.0 / exclusive_time_ms < 0.1 then round(parallel_leader_exclusive_time_ms*1.0 / exclusive_time_ms, 3) * 100\n" + 
				"  else cast((parallel_leader_exclusive_time_ms*1.0 / exclusive_time_ms) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				");\n";
				
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// set percentage string numerics for explain_plan_operator_workers:
		// actual_data_size_percentage_string, exclusive_time_percentage_string
		strSQL = "update explain_plan_operator_workers as EPOW set\n" + 
				"(actual_data_size_percentage_string, actual_rows_percentage_string, difference_time_percentage_string, exclusive_time_percentage_string\n" +
				//", ram_percentage_string\n" +
				") = (select\n" +
				"case\n" + 
				"  when EPOW.actual_data_size_bytes is null or EPOA.actual_data_size_bytes = 0 then null\n" + 
				"  when EPOW.actual_data_size_bytes*1.0 / EPOA.actual_data_size_bytes < 0.01 then 0\n" + 
				"  when EPOW.actual_data_size_bytes*1.0 / EPOA.actual_data_size_bytes < 0.1 then round(EPOW.actual_data_size_bytes*1.0 / EPOA.actual_data_size_bytes, 3) * 100\n" + 
				"  else cast((EPOW.actual_data_size_bytes*1.0 / EPOA.actual_data_size_bytes) * 100 + 0.5 as integer)\n" + 
				"end\n" + 
				", case\n" + 
				"  when EPOW.actual_rows is null or EPOA.actual_rows = 0 then null\n" + 
				"  when EPOW.actual_rows*1.0 / EPOA.total_actual_rows < 0.01 then 0\n" + 
				"  when EPOW.actual_rows*1.0 / EPOA.total_actual_rows < 0.1 then round(EPOW.actual_rows*1.0 / EPOA.total_actual_rows, 3) * 100\n" + 
				"  else cast((EPOW.actual_rows*1.0 / EPOA.total_actual_rows) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				// worker time difference % = worker time difference / operator time difference
				", case\n" + 
				"  when EPOW.difference_time_ms is null or EPOA.difference_time_ms = 0 then null\n" + 
				"  when EPOW.difference_time_ms*1.0 / EPOA.difference_time_ms < 0.01 then 0\n" + 
				"  when EPOW.difference_time_ms*1.0 / EPOA.difference_time_ms < 0.1 then round(EPOW.difference_time_ms*1.0 / EPOA.difference_time_ms, 3) * 100\n" + 
				"  else cast((EPOW.difference_time_ms*1.0 / EPOA.difference_time_ms) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				
				", case\n" + 
				"  when EPOW.exclusive_time_ms is null or EPOA.exclusive_time_ms = 0 then null\n" + 
				"  when EPOW.exclusive_time_ms*1.0 / EPOA.exclusive_time_ms < 0.01 then 0\n" + 
				"  when EPOW.exclusive_time_ms*1.0 / EPOA.exclusive_time_ms < 0.1 then round(EPOW.exclusive_time_ms*1.0 / EPOA.exclusive_time_ms, 3) * 100\n" + 
				"  else cast((EPOW.exclusive_time_ms*1.0 / EPOA.exclusive_time_ms) * 100 + 0.5 as integer)\n" + 
				"end\n" +
				"from explain_plan_operator_annotations as EPOA\n" +
				"where EPOW.id_explain_plan = EPOA.id_explain_plan and EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				"and EPOW.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				")\n" +
				"where exists (\n" +
				"  select EPOA.exclusive_time_ms\n" +
				"  from explain_plan_operator_annotations as EPOA\n" +
				"  where EPOW.id_explain_plan = EPOA.id_explain_plan and EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				"  and EPOW.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				");\n";
				
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

	}

	// set highlights. There are different rules for mass, rows, time diff, and excl time.
	public void setHighlightFlagsFromWorkerAndParallelLeaderPercentageStrings(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;
		int maxDeviationPercentage = 25;
		
		// PL time difference and exclusive time: should be close to 100%
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" + 
				"parallel_leader_difference_time_highlight = case\n" +
				"    when difference_time_ms > 0 and (parallel_leader_difference_time_percentage_string*1.0 < 80 or parallel_leader_difference_time_percentage_string*1.0 >= 110) then 1\n" + 
				"    else 0\n" +
				"  end\n" +
				", parallel_leader_exclusive_time_highlight = case\n" +
				"    when exclusive_time_ms > 0 and (parallel_leader_exclusive_time_percentage_string*1.0 < 80 or parallel_leader_exclusive_time_percentage_string*1.0 >= 110) then 1\n" + 
				"    else 0\n" +
				"  end\n" +
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan" +
				";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// PL mass and rows: should be evenly divided between PL and workers
		// calculate what min and max values to start highlighting based
		// on # of workers + parallel leader, for explain_plan_operator_annotations
		strSQL = "with cteDeviationsForDelegatedWork as (\n" + 
				"  select id_explain_plan, id_explain_plan_property, count(*) as num_workers\n" + 
				"  , (100.0/(count(*)+1)) - (100.0/(count(*)+1)) * (" + maxDeviationPercentage + "*1.0)/100 as min_dev\n" +
				"  , (100.0/(count(*)+1)) + (100.0/(count(*)+1)) * (" + maxDeviationPercentage + "*1.0)/100 as max_dev\n" +
				"  from explain_plan_operator_workers\n" +
				"  group by id_explain_plan, id_explain_plan_property\n" + 
				")\n" +
				"update explain_plan_operator_annotations as EPOA set\n" + 
				"(parallel_leader_actual_data_size_highlight, parallel_leader_actual_rows_highlight\n" +
				") = (select\n" +
				"case\n" +
				"  when parallel_leader_actual_data_size_percentage_string*1.0 < CTE.min_dev or parallel_leader_actual_data_size_percentage_string*1.0 > CTE.max_dev then 1\n" + 
				"  else 0\n" +
				"end\n" +
				",case\n" +
				"  when parallel_leader_actual_rows_percentage_string*1.0 < CTE.min_dev or parallel_leader_actual_rows_percentage_string*1.0 > CTE.max_dev then 1\n" + 
				"  else 0\n" +
				"end\n" +
				"from cteDeviationsForDelegatedWork as CTE where EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"  and EPOA.id_explain_plan_property = CTE.id_explain_plan_property\n" +
				")\n" +
				"where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// worker time difference and exclusive time: should be close to 100%
		// from explain_plan_operator_workers / parallel_leader_exclusive_time_highlight:
		strSQL = "update explain_plan_operator_workers set\n" +
				"difference_time_highlight_1 = case\n" +
				"    when difference_time_percentage_string*1.0 < 80 or difference_time_percentage_string*1.0 >= 110 then 1\n" + 
				"    else 0\n" +
				"  end\n" +
				", exclusive_time_highlight_1 = case\n" +
				"    when exclusive_time_percentage_string*1.0 < 80 or exclusive_time_percentage_string*1.0 >= 110 then 1\n" + 
				"    else 0\n" +
				"  end\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan" +
				";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// worker mass and rows: should be evenly divided between PL and workers
		// calculate what min and max values to start highlighting based
		// on # of workers + parallel leader, for explain_plan_operator_workers
		// NEXT: update actual_mass_highlight_1
		strSQL = "with cteDeviationsForDelegatedWork as (\n" + 
				"  select id_explain_plan, id_explain_plan_property, count(*) as num_workers\n" + 
				"  , (100.0/(count(*)+1)) - (100.0/(count(*)+1)) * (" + maxDeviationPercentage + "*1.0)/100 as min_dev\n" + 
				"  , (100.0/(count(*)+1)) + (100.0/(count(*)+1)) * (" + maxDeviationPercentage + "*1.0)/100 as max_dev\n" + 
				"  from explain_plan_operator_workers\n" + 
				"  group by id_explain_plan, id_explain_plan_property\n" + 
				")\n" + 
				"update explain_plan_operator_workers as EPOW set\n" + 
				"(actual_data_size_highlight_1, actual_rows_highlight_1\n" +
				") = (select\n" + 
				"case\n" + 
				"  when actual_data_size_percentage_string*1.0 < CTE.min_dev or actual_data_size_percentage_string*1.0 > CTE.max_dev then 1\n" + 
				"  else 0\n" + 
				"end\n" + 
				",case\n" + 
				"  when actual_rows_percentage_string*1.0 < CTE.min_dev or actual_rows_percentage_string*1.0 > CTE.max_dev then 1\n" + 
				"  else 0\n" + 
				"end\n" +
				"from cteDeviationsForDelegatedWork as CTE where EPOW.id_explain_plan = CTE.id_explain_plan\n" + 
				"  and EPOW.id_explain_plan_property = CTE.id_explain_plan_property\n" + 
				")\n" + 
				"where EPOW.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan" +
				";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		

/*
select id_explain_plan_property, worker_number
--, actual_data_size_label_string, actual_data_size_highlight_1
--, actual_rows_label_string, actual_rows_highlight_1
--, disk_label_string, disk_highlight_1
--, disk_plus_ram_label_string, disk_plus_ram_highlight_1
--, exclusive_time_label_string, exclusive_time_highlight_1
, ram_label_string, ram_highlight_1
from explain_plan_operator_workers order by id_explain_plan_property, worker_number
*/		
	}
	

	/*
	method name:
		returnLabelStringColumnAssignment
	description:
	accepts different strings if % >0 or % <=0, and column names
	returns a formatted sql string for updating a label_string (like actual_rows_label_string, parallel_leader_exclusive_time_label_string, et al).
	returned strings can get pretty long, reduces potential for errors (esp. column names) and simplifies formatting changes
	*/
	private String returnLabelStringColumnAssignment(String aboveZeroPercentString, String nonAboveZeroPercentString, String percentageColumn, String unitsColumn) {
		String strCase;
		strCase = "replace("
			+ "case when coalesce(" + percentageColumn + ", 0)*1.0 > 0 then replace('" + aboveZeroPercentString + "', '{units}', coalesce(" + unitsColumn + ", 0)) "
			+ "else replace('" + nonAboveZeroPercentString + "', '{units}', coalesce(" + unitsColumn + ", 0)) end"
			+ ", '{percentage}', case when coalesce(" + percentageColumn + ", 0)*1.0 > 1000 then '> 1000' else coalesce(" + percentageColumn + ", 0) end)";
		return strCase;
	}
	
	/*
	method name:
		updateWorkerLabelStrings
	description:
	update in explain_plan_operator_workers:
	worker_title_label_string, actual_data_size_label_string, actual_rows_label_string, disk_label_string, disk_plus_ram_label_string
	, exclusive_time_label_string, ram_label_string
	*/
	public void updateWorkerLabelStrings(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;

		// set worker title to south_1 text + " worker # " + worker_number
		strSQL = "update explain_plan_operator_workers as EPOW set\n" +
				"worker_title_label_string = (\n" +
				"  select node_type from explain_plan_operator_annotations\n" +
				"  where id_explain_plan = EPOW.id_explain_plan\n" +
				"    and id_explain_plan_property = EPOW.id_explain_plan_property\n" +
				") || ' worker # ' || worker_number\n" +
				"where EPOW.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, worker_number, worker_title_label_string
from explain_plan_operator_workers
*/

		// assign values
		strSQL = "update explain_plan_operator_workers as EPOW set\n"
			+ "actual_data_size_label_string = " + returnLabelStringColumnAssignment("Actual Mass: {percentage}% ({units})", "Actual Mass: {units}", "actual_data_size_percentage_string", "actual_data_size_units_string") + "\n"
			+ ", actual_rows_label_string = " + returnLabelStringColumnAssignment("Actual Rows: {percentage}% ({units})", "Actual Rows: {units}", "actual_rows_percentage_string", "actual_rows_units_string") + "\n"
			+ ", difference_time_label_string = " + returnLabelStringColumnAssignment("Time Diff: {percentage}% ({units})", "Time Diff: {units}", "difference_time_percentage_string", "difference_time_units_string") + "\n" 
			+ ", exclusive_time_label_string = " + returnLabelStringColumnAssignment("Opr Time: {percentage}% ({units})", "Opr Time: {units}", "exclusive_time_percentage_string", "exclusive_time_units_string") + "\n" 
			+ "where EPOW.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n"
			+ ";\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

/*
select id_explain_plan_property, worker_number
, actual_data_size_percentage_string, actual_data_size_units_string, actual_data_size_label_string
, exclusive_time_percentage_string, exclusive_time_units_string, exclusive_time_label_string
from explain_plan_operator_workers
*/

	}
	
	/*
	method name:
		updateParallelLeaderLabelStrings
	description:
	update in explain_plan_operator_annotations:
	, parallel_leader_actual_data_size_label_string, parallel_leader_actual_rows_label_string, parallel_leader_disk_label_string
	, parallel_leader_disk_plus_ram_label_string, parallel_leader_exclusive_time_label_string, parallel_leader_ram_label_string
	, parallel_leader_title_label_string
	*/
	public void updateParallelLeaderLabelStrings(int intIDExplainPlan) {
		boolean bolShowSQL = false;
		String strSQL;

		// set worker title to south_1 text + " worker # " + worker_number
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" +
				"parallel_leader_title_label_string = node_type || ' parallel leader'\n" +
				"where id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" +
				";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, parallel_leader_title_label_string
, parallel_leader_actual_data_size_label_string, parallel_leader_exclusive_time_label_string 
from explain_plan_operator_annotations
*/
		// assign values
		strSQL = "update explain_plan_operator_annotations as EPOA set\n"
			+ "parallel_leader_actual_data_size_label_string = " + returnLabelStringColumnAssignment("Actual Mass: {percentage}% ({units})", "Actual Mass: {units}", "parallel_leader_actual_data_size_percentage_string", "parallel_leader_actual_data_size_units_string") + "\n"
			+ ", parallel_leader_actual_rows_label_string = " + returnLabelStringColumnAssignment("Actual Rows: {percentage}% ({units})", "Actual Rows: {units}", "parallel_leader_actual_rows_percentage_string", "parallel_leader_actual_rows_units_string") + "\n"
			+ ", parallel_leader_difference_time_label_string = " + returnLabelStringColumnAssignment("Time Diff: {percentage}% ({units})", "Time Diff: {units}", "parallel_leader_difference_time_percentage_string", "parallel_leader_difference_time_units_string") + "\n" // already set, I think
			+ ", parallel_leader_exclusive_time_label_string = " + returnLabelStringColumnAssignment("Opr Time: {percentage}% ({units})", "Opr Time: {units}", "parallel_leader_exclusive_time_percentage_string", "parallel_leader_exclusive_time_units_string") + "\n" 
			+ "where EPOA.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n"
			+ ";\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

/*
select id_explain_plan_property, worker_number
--, actual_data_size_bytes, actual_data_size_label_string
--, actual_rows, actual_rows_label_string
--, disk_pages, disk_label_string
--, disk_plus_ram_pages, disk_plus_ram_label_string
--, exclusive_time_ms, exclusive_time_label_string
, ram_pages, ram_label_string
from explain_plan_operator_annotations order by id_explain_plan_property, worker_number
*/
		}
	
/*
	update in explain_plan_operator_annotations:
	, parallel_leader_cost_highlight, parallel_leader_disk_highlight, parallel_leader_disk_plus_ram_highlight
	, parallel_leader_exclusive_time_highlight, parallel_leader_ram_highlight
	update in explain_plan_operator_workers:
	, actual_data_size_highlight_1, actual_rows_highlight_1, disk_highlight_1, disk_plus_ram_highlight_1
	, ram_highlight_1, exclusive_time_highlight_1
*/

	public void updateFontMetricsNorthOptionsWidthAndHeight(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnRow;
		FontMetrics defaultFontMetrics;
		//FontMetrics arrowFontMetrics;
		//FontMetrics actualMassArrowFontMetrics;
		
		defaultFontMetrics = getFontMetrics(operatorLabelFont());
		//arrowFontMetrics = getFontMetrics(new Font(ARROW_TEXT_FONT_NAME, ARROW_TEXT_FONT_STYLE, withZoom(ARROW_TEXT_FONT_SIZE)));
		//actualMassArrowFontMetrics = getFontMetrics(new Font(ARROW_TEXT_FONT_NAME, ARROW_TEXT_FONT_STYLE, withZoom(ARROW_TEXT_FONT_SIZE)));
		
		int defaultMaxAscent = defaultFontMetrics.getMaxAscent();
		int defaultMaxDescent = defaultFontMetrics.getMaxDescent();
		//int arrowMaxAscent = arrowFontMetrics.getMaxAscent();
		//int actualMassArrowMaxAscent = actualMassArrowFontMetrics.getMaxAscent();

		strSQL = "select\n"
				+ "id_explain_plan_property\n"
				+ ", cost_label_string\n"
				+ ", difference_time_label_string\n"
				+ ", exclusive_time_label_string\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
	
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_annotations set\n";
				strSQL += "cost_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "cost_label_string", true, defaultFontMetrics, null, 0);
				strSQL += ", cost_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "cost_label_string", false, null, defaultMaxAscent, 0);
				strSQL += ", difference_time_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "difference_time_label_string", true, defaultFontMetrics, null, 0);
				strSQL += ", difference_time_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "difference_time_label_string", false, null, defaultMaxAscent, 0);
				strSQL += ", exclusive_time_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "exclusive_time_label_string", true, defaultFontMetrics, null, 0);
				strSQL += ", exclusive_time_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "exclusive_time_label_string", false, null, defaultMaxAscent, 0);
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property");
				strSQL += ";\n";
			}
		}

		
		strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		
	}

	// REFACTOR - write all possible operator label font metrics (like disk_label_string, disk_plus_ram_percentage_string, etc.)
	// i.e., write width (say, of disk_label_string) to font metrics (say, to disk_label_fontmetrics_width)
	public void updateFontMetricsSouthWidthAndHeight(int intIDExplainPlan) {
		String strSQL, strRetrieveSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnRow;
		FontMetrics defaultFontMetrics;
		//FontMetrics arrowFontMetrics;
		
		defaultFontMetrics = getFontMetrics(operatorLabelFont());
		//arrowFontMetrics = getFontMetrics(new Font(ARROW_TEXT_FONT_NAME, ARROW_TEXT_FONT_STYLE, withZoom(ARROW_TEXT_FONT_SIZE)));
		
		int defaultMaxAscent = defaultFontMetrics.getMaxAscent();
		int defaultMaxDescent = defaultFontMetrics.getMaxDescent();
		//int arrowMaxAscent = arrowFontMetrics.getMaxAscent();
		
		strSQL = "";
		

		strRetrieveSQL = "select\n"
				+ "id_explain_plan_property\n"
				+ ", south_1_text, south_2_text, south_3_text, south_4_text, south_5_text\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strRetrieveSQL); }
	
		jsnRows = Database.rows(strRetrieveSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_annotations set\n";
				for (int intNum = 1; intNum <= 5; intNum++) { // there are 5 south text positions
					if (intNum > 1) {
						strSQL += ", ";
					}
					strSQL += "south_" + intNum + "_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "south_" + intNum + "_text", true, defaultFontMetrics, null, 0);
					strSQL += ", south_" + intNum + "_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "south_" + intNum + "_text", false, null, defaultMaxAscent, 0);
				}
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property");
				strSQL += ";\n";
			}
		}

		strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		
	}

	// return one string if object == null, or a different string if not null
	// if use_fontmetrics == true: use objFM to determine string dimensions (width)
	// if use_fontmetrics == false: use stringHeight to determine string dimensions (height)
	private String returnFontMetricsNullOrNotNull(JsonObject jsnRow, String label_string, boolean use_fontmetrics, FontMetrics objFM, Integer stringHeight, int null_return) {
		String strLabelString = (String) jsnRow.get(label_string);
		if (strLabelString != null)
			strLabelString = strLabelString.replace("'",  "''").replace("\n", "").replace("\r", "");
		if (strLabelString == null) {
			return String.valueOf(null_return) + "\n";
		} else if (use_fontmetrics == true && strLabelString != null) {
			//return objFM.stringWidth(strLabelString) + " -- '" + strLabelString + "'\n";
			return objFM.stringWidth(strLabelString) + "\n";
		} else {
			//return String.valueOf(stringHeight) + " -- '" + strLabelString + "'\n";
			return String.valueOf(stringHeight) + "\n";
		}
	}
	

	// font metrics for:
	// arrow text: actual/estimated mass/rows
	// ALREADY DONE ABOVE (north 1 text eventually:) cost label text, time difference label text, exclusive time label text
	// NOT HERE: PL info in worker box: title label text, actual mass label text, actual rows label text, time difference label text, exclusive time label text
	public void updateFontMetricsArrowOptionsWidthAndHeight(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnRow;
		FontMetrics defaultFontMetrics;
		//FontMetrics arrowFontMetrics;
		FontMetrics actualMassArrowFontMetrics;
		FontMetrics estimatedMassArrowFontMetrics;
		FontMetrics actualRowsArrowFontMetrics;
		FontMetrics estimatedRowsArrowFontMetrics;
		
		defaultFontMetrics = getFontMetrics(operatorLabelFont());
		actualMassArrowFontMetrics = getFontMetrics(new Font(ACTUAL_MASS_ARROW_FONT_NAME, ACTUAL_MASS_ARROW_FONT_STYLE, withZoom(ACTUAL_MASS_ARROW_FONT_SIZE)));
		estimatedMassArrowFontMetrics = getFontMetrics(new Font(ESTIMATED_MASS_ARROW_FONT_NAME, ESTIMATED_MASS_ARROW_FONT_STYLE, withZoom(ESTIMATED_MASS_ARROW_FONT_SIZE)));
		actualRowsArrowFontMetrics = getFontMetrics(new Font(ACTUAL_ROWS_ARROW_FONT_NAME, ACTUAL_ROWS_ARROW_FONT_STYLE, withZoom(ACTUAL_ROWS_ARROW_FONT_SIZE)));
		estimatedRowsArrowFontMetrics = getFontMetrics(new Font(ESTIMATED_ROWS_ARROW_FONT_NAME, ESTIMATED_ROWS_ARROW_FONT_STYLE, withZoom(ESTIMATED_ROWS_ARROW_FONT_SIZE)));
		
		int actualRowsArrowMaxAscent = actualRowsArrowFontMetrics.getMaxAscent();
		int estimatedRowsArrowMaxAscent = estimatedRowsArrowFontMetrics.getMaxAscent();
		
		// TODO: might be more efficient to retrieve and set font widths for both
		// operator and parallel leaders (e.g., difference_time_label_string
		// and parallel_leader_difference_time_label_string), since we're
		// doing RBAR updates on explain_plan_operator_annotations
		strSQL = "select\n"
				+ "id_explain_plan_property\n"
				+ ", actual_data_size_label_string\n"
				+ ", actual_rows_label_string\n"
				+ ", estimated_data_size_label_string\n"
				+ ", estimated_rows_label_string\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
	
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_annotations set\n";

				strSQL += "actual_data_size_label_fontmetrics_width = ";
				if (jsnRow.get("actual_data_size_label_string") == null) {
					strSQL += "0";
				} else {
					strSQL += "case when total_actual_mass_is_exact = 0 then ";
					strSQL += estimatedRowsArrowFontMetrics.stringWidth(jsnRow.getString("actual_data_size_label_string"));
					strSQL += " else ";
					strSQL += actualRowsArrowFontMetrics.stringWidth(jsnRow.getString("actual_data_size_label_string"));
					strSQL += " end";
					//strSQL += " -- '" + jsnRow.get("actual_data_size_label_string") + "'";
				}
				strSQL += "\n";
				
				strSQL += ", actual_data_size_label_fontmetrics_height = ";
				if (jsnRow.get("actual_data_size_label_string") == null) {
					strSQL += "0";
				} else {
					strSQL += "case when total_actual_mass_is_exact = 0 then ";
					strSQL += estimatedRowsArrowMaxAscent;
					strSQL += " else ";
					strSQL += actualRowsArrowMaxAscent;
					strSQL += " end";
					//strSQL += " -- '" + jsnRow.get("actual_data_size_label_string") + "'";
				}
				strSQL += "\n";

				strSQL += ", actual_rows_label_fontmetrics_width = ";
				if (jsnRow.get("actual_rows_label_string") == null) {
					strSQL += "0";
				} else {
					strSQL += "case when total_actual_rows_is_exact = 0 then ";
					strSQL += estimatedRowsArrowFontMetrics.stringWidth(jsnRow.getString("actual_rows_label_string"));
					strSQL += " else ";
					strSQL += actualRowsArrowFontMetrics.stringWidth(jsnRow.getString("actual_rows_label_string"));
					strSQL += " end";
					//strSQL += " -- '" + jsnRow.get("actual_rows_label_string") + "'";
				}
				strSQL += "\n";
				
				strSQL += ", actual_rows_label_fontmetrics_height = ";
				if (jsnRow.get("actual_rows_label_string") == null) {
					strSQL += "0";
				} else {
					strSQL += "case when total_actual_rows_is_exact = 0 then ";
					strSQL += estimatedRowsArrowMaxAscent;
					strSQL += " else ";
					strSQL += actualRowsArrowMaxAscent;
					strSQL += " end";
					strSQL += " -- '" + jsnRow.get("actual_rows_label_string") + "'";
				}

				strSQL += "\n";
				strSQL += ", estimated_data_size_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "estimated_data_size_label_string", true, estimatedRowsArrowFontMetrics, null, 0);
				strSQL += ", estimated_data_size_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "estimated_data_size_label_string", false, null, estimatedRowsArrowMaxAscent, 0);
				strSQL += ", estimated_rows_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "estimated_rows_label_string", true, estimatedRowsArrowFontMetrics, null, 0);
				strSQL += ", estimated_rows_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "estimated_rows_label_string", false, null, estimatedRowsArrowMaxAscent, 0);
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property");
				strSQL += ";\n";
			}
		}
		strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}
	
	// write all possible worker label font metrics (like disk_label_string, disk_plus_ram_percentage_string, etc.)
	// i.e., write width (say, of disk_label_string) to font metrics (say, to disk_label_fontmetrics_width)
	public void updateFontMetricWorkersWidthAndHeight(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnRow;
		FontMetrics workerTitleFontMetrics;
		FontMetrics workerFontMetrics;
		
		workerTitleFontMetrics = getFontMetrics(new Font(WORKER_TITLE_FONT_NAME, WORKER_TITLE_FONT_STYLE, withZoom(WORKER_TITLE_FONT_SIZE)));
		workerFontMetrics = getFontMetrics(new Font(WORKER_TEXT_FONT_NAME, WORKER_TEXT_FONT_STYLE, withZoom(WORKER_TEXT_FONT_SIZE)));
		
		int workerMaxAscent = workerFontMetrics.getMaxAscent();
		int workerTitleMaxAscent = workerTitleFontMetrics.getMaxAscent();
		
		strSQL = "select\n"
				+ "id_explain_plan_property, worker_number\n"
				+ ", worker_title_label_string, actual_data_size_label_string, actual_rows_label_string\n"
				+ ", difference_time_label_string\n"
				+ ", exclusive_time_label_string\n"
				+ "from explain_plan_operator_workers\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
	
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_workers set\n";
				strSQL += "worker_title_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "worker_title_label_string", true, workerTitleFontMetrics, null, 0);
				strSQL += ", worker_title_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "worker_title_label_string", false, null, workerTitleMaxAscent, 0);
				strSQL += ", actual_data_size_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "actual_data_size_label_string", true, workerFontMetrics, null, 0);
				strSQL += ", actual_data_size_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "actual_data_size_label_string", false, null, workerMaxAscent, 0);
				strSQL += ", actual_rows_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "actual_rows_label_string", true, workerFontMetrics, null, 0);
				strSQL += ", actual_rows_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "actual_rows_label_string", false, null, workerMaxAscent, 0);
				strSQL += ", difference_time_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "difference_time_label_string", true, workerFontMetrics, null, 0);
				strSQL += ", difference_time_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "difference_time_label_string", false, null, workerMaxAscent, 0);
				strSQL += ", exclusive_time_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "exclusive_time_label_string", true, workerFontMetrics, null, 0);
				strSQL += ", exclusive_time_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "exclusive_time_label_string", false, null, workerMaxAscent, 0);
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property") + "\n";
				strSQL += "and worker_number = ";
				strSQL += (Integer) jsnRow.get("worker_number") + "\n";
				strSQL += ";\n";
			}
		}

		strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}
	
	public void updateFontMetricParallelLeadersWidthAndHeight(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnRow;
		FontMetrics parallelLeaderTitleFontMetrics;
		FontMetrics parallelLeaderFontMetrics;
		
		parallelLeaderTitleFontMetrics = getFontMetrics(new Font(WORKER_TITLE_FONT_NAME, WORKER_TITLE_FONT_STYLE, withZoom(WORKER_TITLE_FONT_SIZE)));
		parallelLeaderFontMetrics = getFontMetrics(new Font(WORKER_TEXT_FONT_NAME, WORKER_TEXT_FONT_STYLE, withZoom(WORKER_TEXT_FONT_SIZE)));
		
		int parallelLeaderMaxAscent = parallelLeaderFontMetrics.getMaxAscent();
		int parallelLeaderTitleMaxAscent = parallelLeaderTitleFontMetrics.getMaxAscent();
		
		strSQL = "select\n"
				+ "id_explain_plan_property\n"
				+ ", parallel_leader_actual_data_size_label_string, parallel_leader_actual_rows_label_string\n"
				+ ", parallel_leader_difference_time_label_string\n"
				+ ", parallel_leader_exclusive_time_label_string\n"
				+ ", parallel_leader_title_label_string\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
	
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			strSQL = "";
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				strSQL += "update explain_plan_operator_annotations set\n";
				strSQL += "parallel_leader_title_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_title_label_string", true, parallelLeaderTitleFontMetrics, null, 0);
				strSQL += ", parallel_leader_title_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_title_label_string", false, null, parallelLeaderTitleMaxAscent, 0);
				strSQL += ", parallel_leader_actual_data_size_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_actual_data_size_label_string", true, parallelLeaderFontMetrics, null, 0);
				strSQL += ", parallel_leader_actual_data_size_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_actual_data_size_label_string", false, null, parallelLeaderMaxAscent, 0);
				strSQL += ", parallel_leader_difference_time_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_difference_time_label_string", true, parallelLeaderFontMetrics, null, 0);
				strSQL += ", parallel_leader_difference_time_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_difference_time_label_string", false, null, parallelLeaderMaxAscent, 0);
				strSQL += ", parallel_leader_actual_rows_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_actual_rows_label_string", true, parallelLeaderFontMetrics, null, 0);
				strSQL += ", parallel_leader_actual_rows_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_actual_rows_label_string", false, null, parallelLeaderMaxAscent, 0);
				strSQL += ", parallel_leader_exclusive_time_label_fontmetrics_width = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_exclusive_time_label_string", true, parallelLeaderFontMetrics, null, 0);
				strSQL += ", parallel_leader_exclusive_time_label_fontmetrics_height = " + returnFontMetricsNullOrNotNull(jsnRow, "parallel_leader_exclusive_time_label_string", false, null, parallelLeaderMaxAscent, 0);
				strSQL += "where id_explain_plan = " + intIDExplainPlan + "\n";
				strSQL += "and id_explain_plan_property = ";
				strSQL += (Integer) jsnRow.get("id_explain_plan_property") + "\n";
				strSQL += ";\n";
			}
		}

		strSQL = "begin transaction;\n" + strSQL + "commit;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
	}
	
	/**
	 * Plans whose JSON omits costs/timings (or older builds that null-concatenated
	 * labels) can end up with null / id-only north_1_text. Repair in place so
	 * "(id) Node Type" appears above operators without a full JSON rebuild.
	 */
	private void repairMissingNorthLabelsIfNeeded(int intIDExplainPlan) {
		if (intIDExplainPlan < 0) {
			return;
		}
		String strSQL = "select count(*) as c from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				+ "and id_explain_plan_property > -1\n"
				+ "and (\n"
				+ "  north_1_text is null\n"
				+ "  or (node_type is not null and node_type <> ''\n"
				+ "      and north_1_text = '(' || id_explain_plan_property || ')')\n"
				+ ")";
		JsonObject row = Database.row(strSQL);
		if (row == null || row.get("c") == null || row.getInteger("c").intValue() == 0) {
			return;
		}
		updateStringUnitsPercentageAnnotationsTextData(intIDExplainPlan);
		updateFontMetricsNorthOptionsWidthAndHeight(intIDExplainPlan);
		copyPercentageByForNorth1(intIDExplainPlan);
	}

	public void copyPercentageByForNorth1(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		String strGetPercentageBy, strRootOperator, strWithCTEOperator;
		FontMetrics defaultFontMetrics;

		defaultFontMetrics = getFontMetrics(operatorLabelFont());
		int defaultMaxAscent = defaultFontMetrics.getMaxAscent();

		// copy PercentageBy labels to north fields
		strGetPercentageBy = getPercentageBy().toString();
		strSQL = "update explain_plan_operator_annotations set\n"
				+ "north_1_text = case\n"
				+ " when '" + strGetPercentageBy + "' = 'COST' then cost_label_string\n"  
				+ " when '" + strGetPercentageBy + "' = 'EXCLUSIVE_TIME' then exclusive_time_label_string\n"  
				+ " when '" + strGetPercentageBy + "' = 'TIME_DIFFERENCE' then difference_time_label_string\n"  
				+ "end\n"
				+ ", north_1_fontmetrics_width = case\n"
				+ " when '" + strGetPercentageBy + "' = 'COST' then cost_label_fontmetrics_width\n"  
				+ " when '" + strGetPercentageBy + "' = 'EXCLUSIVE_TIME' then exclusive_time_label_fontmetrics_width\n"
				+ " when '" + strGetPercentageBy + "' = 'TIME_DIFFERENCE' then difference_time_label_fontmetrics_width\n"
				+ "end\n"
				+ ", north_1_fontmetrics_height = case\n"
				+ " when '" + strGetPercentageBy + "' = 'COST' then cost_label_fontmetrics_height\n"  
				+ " when '" + strGetPercentageBy + "' = 'EXCLUSIVE_TIME' then exclusive_time_label_fontmetrics_height\n"  
				+ " when '" + strGetPercentageBy + "' = 'TIME_DIFFERENCE' then difference_time_label_fontmetrics_height\n"  
				+ "end\n"
				+ ", north_1_highlight_1 = case\n"
				+ " when '" + strGetPercentageBy + "' = 'COST' then cost_highlight_1\n"  
				+ " when '" + strGetPercentageBy + "' = 'EXCLUSIVE_TIME' then exclusive_time_highlight_1\n"  
				+ " when '" + strGetPercentageBy + "' = 'TIME_DIFFERENCE' then difference_time_highlight_1\n"  
				+ "end\n"
				+ ", north_1_highlight_2 = case\n"
				+ " when '" + strGetPercentageBy + "' = 'COST' then cost_highlight_2\n"  
				+ " when '" + strGetPercentageBy + "' = 'EXCLUSIVE_TIME' then exclusive_time_highlight_2\n"
				+ " when '" + strGetPercentageBy + "' = 'TIME_DIFFERENCE' then difference_time_highlight_2\n"
				+ "end\n"
				+ "where id_explain_plan = " + intIDExplainPlan + ";\n";

		// Keep "(id) Node Type" visible when the selected metric has no value
		strSQL += "update explain_plan_operator_annotations set\n"
				+ "north_1_text = '(' || id_explain_plan_property || ')'"
				+ " || case when node_type is null or node_type = '' then '' else ' ' || node_type end\n"
				+ ", north_1_fontmetrics_height = " + defaultMaxAscent + "\n"
				+ ", north_1_highlight_1 = 0\n"
				+ ", north_1_highlight_2 = 0\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				+ "and id_explain_plan_property > -1\n"
				+ "and north_1_text is null;\n";

		// set blank string for "with cte" operators
		strWithCTEOperator = "CTE";
		strSQL += "update explain_plan_operator_annotations set\n"
				+ "north_1_text = '" + strWithCTEOperator + "'\n"  
				+ ", north_1_fontmetrics_width = " + defaultFontMetrics.stringWidth(strWithCTEOperator) + "\n"
				+ ", north_1_fontmetrics_height = " + defaultMaxAscent + "\n"
				+ ", north_1_highlight_1 = 0\n"
				+ ", north_1_highlight_2 = 0\n"
				+ "where node_type = 'With CTE'\n"
				+ "and id_explain_plan = " + intIDExplainPlan + "\n"
				+ "and id_explain_plan_property > -1;\n";

		// set root operator north_1 text
		strRootOperator = "User or Application";
		strSQL += "update explain_plan_operator_annotations set\n"
				+ "north_1_text = '" + strRootOperator + "'\n"  
				+ ", north_1_fontmetrics_width = " + defaultFontMetrics.stringWidth(strRootOperator) + "\n"
				+ ", north_1_fontmetrics_height = " + defaultMaxAscent + "\n"
				+ ", north_1_highlight_1 = 0\n"
				+ ", north_1_highlight_2 = 0\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				+ "and id_explain_plan_property = -1;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// Font widths for id / id+node-type fallback labels (e.g. "(81) Gather")
		JsonArray idOnlyRows = Database.rows(
				"select id_explain_plan_property, north_1_text from explain_plan_operator_annotations\n"
						+ "where id_explain_plan = " + intIDExplainPlan + "\n"
						+ "and id_explain_plan_property > -1\n"
						+ "and north_1_text is not null\n"
						+ "and north_1_text like '(' || id_explain_plan_property || '%'\n"
						+ "and north_1_text not like '%Cost:%'\n"
						+ "and north_1_text not like '%Opr Time:%'\n"
						+ "and north_1_text not like '%Time Diff:%'\n"
						+ "and coalesce(north_1_fontmetrics_width, 0) = 0");
		if (idOnlyRows != null && !idOnlyRows.isEmpty()) {
			StringBuilder widthSQL = new StringBuilder();
			for (int i = 0; i < idOnlyRows.size(); i++) {
				JsonObject idRow = (JsonObject) idOnlyRows.get(i);
				Integer id = idRow.getInteger("id_explain_plan_property");
				String label = idRow.getString("north_1_text");
				if (id == null || label == null) {
					continue;
				}
				widthSQL.append("update explain_plan_operator_annotations set\n")
						.append("north_1_fontmetrics_width = ").append(defaultFontMetrics.stringWidth(label)).append("\n")
						.append("where id_explain_plan = ").append(intIDExplainPlan).append("\n")
						.append("and id_explain_plan_property = ").append(id).append(";\n");
			}
			if (widthSQL.length() > 0) {
				Database.dml(widthSQL.toString());
			}
		}
	}
		

	public void copyLineThicknessByForArrow(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		String strLineThicknessBy;
			
		// copy LineThicknessBy labels to arrow fields
		strLineThicknessBy = getLineThicknessBy().toString();
		strSQL = "";
		strSQL += "update explain_plan_operator_annotations set\n"
				+ "arrow_text = case\n"
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_DATA_MASS' then actual_data_size_label_string\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_ROWS' then actual_rows_label_string\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_ROWS_RATIO' then null\n" // dne by design
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_MASS_RATIO' then null\n" // dne by design
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_DATA_MASS' then estimated_data_size_label_string\n"  
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_ROWS' then estimated_rows_label_string\n"
				+ "end\n"
				+ ", arrow_text_fontmetrics_width = case\n"
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_DATA_MASS' then actual_data_size_label_fontmetrics_width\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_ROWS' then actual_rows_label_fontmetrics_width\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_ROWS_RATIO' then 0\n" // zero by design
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_MASS_RATIO' then 0\n" // zero by design
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_DATA_MASS' then estimated_data_size_label_fontmetrics_width\n"  
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_ROWS' then estimated_rows_label_fontmetrics_width\n"
				+ "end\n"
				+ ", arrow_text_fontmetrics_height = case\n"
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_DATA_MASS' then actual_data_size_label_fontmetrics_height\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_ROWS' then actual_rows_label_fontmetrics_height\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_ROWS_RATIO' then 0\n" // zero by design
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_MASS_RATIO' then 0\n" // zero by design
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_DATA_MASS' then estimated_data_size_label_fontmetrics_height\n"  
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_ROWS' then estimated_rows_label_fontmetrics_height\n"
				+ "end\n"
				+ ", arrow_line_thickness = case\n"
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_DATA_MASS' then actual_data_size_line_thickness\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_ROWS' then actual_rows_line_thickness\n"  
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_ROWS_RATIO' then actual_vs_estimated_rows_ratio_line_thickness\n"
				+ " when '" + strLineThicknessBy + "' = 'ACTUAL_VS_ESTIMATED_MASS_RATIO' then actual_vs_estimated_mass_ratio_line_thickness\n"
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_DATA_MASS' then estimated_data_size_line_thickness\n"  
				+ " when '" + strLineThicknessBy + "' = 'ESTIMATED_ROWS' then estimated_rows_line_thickness\n"
				+ "end\n"
				+ "where id_explain_plan = " + intIDExplainPlan + ";\n"
				// Never-executed nodes: don't show misleading "0 bytes" / "0 rows"
				+ "update explain_plan_operator_annotations set\n"
				+ "arrow_text = 'Never executed'\n"
				+ ", arrow_line_thickness = min(coalesce(arrow_line_thickness, 2), max(1, "
				+ withZoom(2) + "))\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				+ "and actual_loops is not null and trim(actual_loops) <> ''\n"
				+ "and cast(actual_loops as real) = 0;\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// Font metrics for the replacement arrow label
		FontMetrics neverFm = getFontMetrics(new Font(
				ACTUAL_MASS_ARROW_FONT_NAME, ACTUAL_MASS_ARROW_FONT_STYLE, withZoom(ACTUAL_MASS_ARROW_FONT_SIZE)));
		int neverWidth = neverFm.stringWidth("Never executed");
		int neverHeight = neverFm.getAscent();
		Database.dml("update explain_plan_operator_annotations set\n"
				+ "arrow_text_fontmetrics_width = " + neverWidth + "\n"
				+ ", arrow_text_fontmetrics_height = " + neverHeight + "\n"
				+ "where id_explain_plan = " + intIDExplainPlan + "\n"
				+ "and actual_loops is not null and trim(actual_loops) <> ''\n"
				+ "and cast(actual_loops as real) = 0;\n");
		
	}

/*
select cost_label_string, disk_plus_ram_label_string, disk_label_string, ram_label_string, exclusive_time_label_string
, arrow_text, north_1_text
from explain_plan_operator_annotations
*/

	
	// zoom is already in effect by this point
	public void updatePerColumnOperatorAndWorkerWidths(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnObject;

		// zero worker fontmetrics width, in case they aren't in explain_plan_operator_show_workers
		strSQL = "update explain_plan_operator_annotations set\n" + 
				"workers_fontmetrics_width = 0\n" + 
				"where id_explain_plan = " + intIDExplainPlan + "\n;";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		
		// find max fontmetrics width of worker text for each operator
		strSQL = "with cteWorkers as (\n" + 
				"  select EPOW.id_explain_plan, EPOW.id_explain_plan_property, max(\n" + 
				"      EPOW.actual_data_size_label_fontmetrics_width\n" + 
				"      , EPOW.actual_rows_label_fontmetrics_width\n" + 
				"      , EPOW.difference_time_label_fontmetrics_width\n" + 
				"      , EPOW.exclusive_time_label_fontmetrics_width\n" + 
				"      , EPOW.worker_title_label_fontmetrics_width\n" + 
				"      ) as max_worker_fontmetrics_width\n" + 
				"    from explain_plan_operator_workers as EPOW\n" + 
				"    inner join explain_plan_operator_show_workers as EPOSW on EPOW.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"      and EPOW.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"    group by EPOW.id_explain_plan, EPOW.id_explain_plan_property\n" + 
				")\n" + 
				", cteParallelLeaders as (\n" + 
				"   select EPOA.id_explain_plan, EPOA.id_explain_plan_property, max(\n" + 
				"     EPOA.parallel_leader_actual_data_size_label_fontmetrics_width\n" + 
				"      , EPOA.parallel_leader_actual_rows_label_fontmetrics_width\n" + 
				"      , EPOA.parallel_leader_difference_time_label_fontmetrics_width\n" + 
				"      , EPOA.parallel_leader_exclusive_time_label_fontmetrics_width\n" + 
				"      , EPOA.parallel_leader_title_label_fontmetrics_width\n" + 
				"    ) as max_parallel_leader_fontmetrics_width\n" + 
				"    from explain_plan_operator_annotations as EPOA\n" + 
				"    inner join explain_plan_operator_show_workers as EPOSW on EPOA.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"      and EPOA.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"    group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				")\n" + 
				", cteMaxWorkersFontmetricsWidth as (\n" + 
				"   select SUB_EPOW.id_explain_plan, SUB_EPOW.id_explain_plan_property\n" + 
				"     , max(P.max_parallel_leader_fontmetrics_width, W.max_worker_fontmetrics_width) as max_workers_fontmetrics_width\n" + 
				"   from explain_plan_operator_workers as SUB_EPOW\n" + 
				"   inner join cteParallelLeaders as P on SUB_EPOW.id_explain_plan = P.id_explain_plan\n" + 
				"     and SUB_EPOW.id_explain_plan_property = P.id_explain_plan_property\n" + 
				"   inner join cteWorkers as W on SUB_EPOW.id_explain_plan = W.id_explain_plan\n" + 
				"     and SUB_EPOW.id_explain_plan_property = W.id_explain_plan_property\n" + 
				")\n" +
				"update explain_plan_operator_annotations as EPOA set\n" + 
				"workers_fontmetrics_width = (select max_workers_fontmetrics_width from cteMaxWorkersFontmetricsWidth where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property = EPOA.id_explain_plan_property)\n" + 
				"where exists (\n" + 
				"  select max_workers_fontmetrics_width from cteMaxWorkersFontmetricsWidth where id_explain_plan = EPOA.id_explain_plan and id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				")\n" + 
				"and id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				";\n" +
				"update explain_plan_operator_annotations set\n" + 
				"workers_fontmetrics_width = 0 where workers_fontmetrics_width is null\n" + 
				"and id_explain_plan = " + intIDExplainPlan + "\n;";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select explain_plan_operator_annotations.id_explain_plan_property, max(
EPOW.actual_data_size_label_fontmetrics_width
, EPOW.actual_rows_label_fontmetrics_width
, EPOW.disk_label_fontmetrics_width
, EPOW.exclusive_time_label_fontmetrics_width
, EPOW.ram_label_fontmetrics_width
, EPOW.worker_title_label_fontmetrics_width
) from explain_plan_operator_workers as EPOW
inner join explain_plan_operator_annotations on EPOW.id_explain_plan = explain_plan_operator_annotations.id_explain_plan
  and EPOW.id_explain_plan_property = explain_plan_operator_annotations.id_explain_plan_property
group by explain_plan_operator_annotations.id_explain_plan, explain_plan_operator_annotations.id_explain_plan_property
order by explain_plan_operator_annotations.id_explain_plan_property;

select id_explain_plan_property, x_cart, y_cart, workers_fontmetrics_width from explain_plan_operator_annotations;
*/

		// set width of each box, from left side
		// to the right side
		// max of worker (if any), or OPERATOR_BOX_WIDTH
		strSQL = "update explain_plan_operator_annotations set\n" + 
				"box_width = max(\n" + 
				withZoom(OPERATOR_BOX_WIDTH) + "\n" +
				"    , workers_fontmetrics_width + (select inside_box_left_margin from config) + (select inside_box_right_margin from config)\n" + 
				")\n" + 
				"where id_explain_plan = " + intIDExplainPlan + "\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, x_cart, y_cart, workers_fontmetrics_width, box_width from explain_plan_operator_annotations;
*/

		// set width of each operator, from left edge (max of north, south, and arrow text against box)
		// to the right edge (probably the right side of north or south text)
		strSQL = "update explain_plan_operator_annotations set\n" + 
				"operator_width = max(\n" + 
				"    north_1_fontmetrics_width\n" + 
				"    , south_1_fontmetrics_width\n" +
				"    , south_2_fontmetrics_width\n" + 
				"    , south_3_fontmetrics_width\n" + 
				"    , south_4_fontmetrics_width\n" + 
				"    , south_5_fontmetrics_width\n" + 
				//"    , box_width + arrow_text_fontmetrics_width * 2 -- + " + withZoom(40) + "\n" + // not sure why the withZoom(40) or "* 2" works...
				"    , box_width + max(arrow_text_fontmetrics_width, " + withZoom(ARROW_MINIMUM_HORIZONTAL_DISTANCE) + ") * 2 -- + " + withZoom(40) + "\n" + // not sure why the withZoom(40) or "* 2" works...
				")\n" + 
				"where id_explain_plan = " + intIDExplainPlan + "\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, north_1_fontmetrics_width, south_3_fontmetrics_width
, south_4_fontmetrics_width, box_width, arrow_text_fontmetrics_width
, operator_width from explain_plan_operator_annotations where id_explain_plan_property in (10, 139, 342);
select id_explain_plan_property, x_cart, y_cart, north_1_fontmetrics_width, south_4_fontmetrics_width, workers_fontmetrics_width, arrow_text_fontmetrics_width, box_width + arrow_text_fontmetrics_width, operator_width from explain_plan_operator_annotations where id_explain_plan_property = 342;
*/
		// set column_widths, based on the max width of all operators in that column
		strSQL = "with cteMaxWidth as (\n" + 
				"  select SUB_EPOA.id_explain_plan, SUB_EPOA.id_explain_plan_property, SUB_EPOA.x_cart\n" + 
				"  , max(SUB_EPOA.operator_width) as column_width\n" + // as max_width
				"  from explain_plan_operator_annotations as SUB_EPOA\n" + 
				"  group by SUB_EPOA.id_explain_plan, SUB_EPOA.x_cart\n" + 
				")\n" +
				"update explain_plan_operator_annotations as EPOA set\n" + 
				"column_width = (\n" +
				"  select column_width\n" +
				"  from cteMaxWidth\n" +
				"  where id_explain_plan = EPOA.id_explain_plan\n" +
				"  and x_cart = EPOA.x_cart\n" +
				")\n" +
				"where exists (\n" + 
				"  select column_width\n" +
				"  from cteMaxWidth\n" +
				"  where id_explain_plan = EPOA.id_explain_plan\n" +
				"  and x_cart = EPOA.x_cart\n" +
				")\n" + 
				"and id_explain_plan = " + intIDExplainPlan + ";\n" + 
				//"update explain_plan_operator_annotations set\n" +
				//"column_width = operator_width where column_width is null and\n" +
				//"id_explain_plan = " + intIDExplainPlan + "\n"
				""
				;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select SUB_EPOA.id_explain_plan, SUB_EPOA.id_explain_plan_property, SUB_EPOA.x_cart, SUB_EPOA.y_cart
, SUB_EPOA.operator_width
from explain_plan_operator_annotations as SUB_EPOA;

select id_explain_plan_property, x_cart, y_cart, operator_width, column_width from explain_plan_operator_annotations;
*/

		// add horizontal margin
		
		// set left side of each column, starting at leftmost edge		
		strSQL = "with cteUpdateHorizontalSpace as (\n" + 
				"  select CTE.id_explain_plan, CTE.x_cart\n" + 
				"  , sum(CTE.column_width) over (partition by CTE.id_explain_plan order by CTE.id_explain_plan, CTE.x_cart) as column_boundary_left\n" + 
				"  from explain_plan_operator_annotations as CTE\n" + 
				"  group by CTE.id_explain_plan, CTE.x_cart\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as EPOA set\n" + 
				"column_boundary_left = (\n" + 
				"  select (\n" + 
				"    select coalesce(CTE.column_boundary_left, 0) as column_boundary_left\n" + 
				"    from cteUpdateHorizontalSpace as CTE\n" + 
				"    where EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"      and EPOA.x_cart - 1 = CTE.x_cart\n" + // may be "and EPOA.x_cart = CTE.x_cart" 
				"    )\n" + 
				")\n" + 
				"where exists (\n" + 
				"  select CTE.column_boundary_left\n" + 
				"  from cteUpdateHorizontalSpace as CTE\n" + 
				"  where EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"    and EPOA.x_cart = CTE.x_cart\n" + 
				")\n" + 
				"and id_explain_plan = " + intIDExplainPlan + ";\n" + 
				"update explain_plan_operator_annotations set\n" +
				"column_boundary_left = 0 where column_boundary_left is null and\n" +
				"id_explain_plan = " + intIDExplainPlan + "\n" +
				"";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		
/*
select id_explain_plan_property, x_cart, y_cart, column_width, previous_x_rect, x_rect from explain_plan_operator_annotations;
*/
		
/*
Before moving over column_boundary_left:
select id_explain_plan_property, x_cart, y_cart, column_boundary_left from explain_plan_operator_annotations;
*/
		// bump over column_boundary_left by HORIZONTAL_SPACE
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" +
				"column_boundary_left = column_boundary_left + " + withZoom(HORIZONTAL_SPACE) + " * x_cart\n" +
				"where id_explain_plan = " + intIDExplainPlan + ";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		
/*
After moving over column_boundary_left:
select id_explain_plan_property, x_cart, y_cart, column_boundary_left from explain_plan_operator_annotations;
*/
		
		// bump over column_boundary_right by the column_width 
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" +
				"column_boundary_right = column_boundary_left + column_width\n" +
				"where id_explain_plan = " + intIDExplainPlan + ";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
		
		// centered in each column
		// operator_boundary_left, operator_boundary_right, centered between
		// operator_width and column_width

		// set left and right side of each operator box, top to bottom
		// other methods will position north, south, and arrow text dynamically
		strSQL = "update explain_plan_operator_annotations set\n" +
		"box_left = column_boundary_left + (column_boundary_right - column_boundary_left) / 2 - box_width / 2\n" +
		", box_right = column_boundary_left + (column_boundary_right - column_boundary_left) / 2 + box_width / 2\n" +
		"where id_explain_plan = " + intIDExplainPlan + ";\n"; 
		
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

/*
select id_explain_plan_property, previous_x_rect, x_rect, operator_box_left_x_rect, operator_box_right_x_rect
from explain_plan_operator_annotations
order by id_explain_plan_property;
*/


	}
	
	// determine heights of workers and operators
	public void updatePerRowOperatorAndWorkerHeights(int intIDExplainPlan) {
		String strSQL;
		boolean bolShowSQL = false;
		JsonArray jsnRows;
		JsonObject jsnObject;
		
		// zero worker fontmetrics heights
		strSQL = "update explain_plan_operator_annotations set\n" + 
				"workers_fontmetrics_height = 0\n" + 
				"where id_explain_plan = " + intIDExplainPlan + "\n;";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// concatenate later

		// add parallel_leader font height to all worker font heights.
		// retrieving the parallel leader font height, with one row, is different than
		// retrieving the worker font heights, with 0 or more rows, so i'm separating them to make
		// it less messy and easier to update later.
		// put the sum into explain_plan_operator_annotations.workers_fontmetrics_height.
		
		// calculate parallel leader font heights
		strSQL = "with cteEPWorkerInfo as (\n" + 
				"  select \n" + 
				"  SUB_EPGV.id_explain_plan\n" + 
				"  , SUB_EPGV.show_worker_info\n" + 
				"  , SUB_EPGV.show_worker_actual_data_size\n" + 
				"  , SUB_EPGV.show_worker_actual_rows\n" + 
				"  , SUB_EPGV.show_worker_difference_time\n" + 
				"  , SUB_EPGV.show_worker_exclusive_time\n" + 
				"  from explain_plan_generated_values as SUB_EPGV\n" + 
				"  where SUB_EPGV.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as SET_EPOA set\n" + 
				"workers_fontmetrics_height = workers_fontmetrics_height + (\n" + 
				"select\n" + 
				"  sum(\n" + 
				"    EPOA.parallel_leader_actual_data_size_label_fontmetrics_height * CTEW.show_worker_actual_data_size\n" + 
				"    + EPOA.parallel_leader_actual_rows_label_fontmetrics_height * CTEW.show_worker_actual_rows\n" + 
				"    + EPOA.parallel_leader_difference_time_label_fontmetrics_height * CTEW.show_worker_difference_time\n" + 
				"    + EPOA.parallel_leader_title_label_fontmetrics_height * CTEW.show_worker_info\n" + 
				"    + " + withZoom(WORKER_TEXT_FONT_SIZE) + "\n" + // we need a little space for parallel leader
				"  ) as workers_fontmetrics_height_part_1\n" + 
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  inner join explain_plan_operator_show_workers as EPOSW on EPOA.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"  inner join cteEPWorkerInfo as CTEW on EPOA.id_explain_plan = CTEW.id_explain_plan\n" + 
				"  where CTEW.show_worker_info = 1\n" + 
				"    and EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SET_EPOA.id_explain_plan_property\n" + 
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				")\n" + 
				"where exists (\n" + 
				"  select EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  inner join explain_plan_operator_show_workers as EPOSW on EPOA.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"  inner join cteEPWorkerInfo as CTEW on EPOA.id_explain_plan = CTEW.id_explain_plan\n" + 
				"  where CTEW.show_worker_info = 1\n" + 
				"    and EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SET_EPOA.id_explain_plan_property\n" + 
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				")";
 		if (bolShowSQL == true) { System.out.println(strSQL); }
 		Database.dml(strSQL);
		 
		// calculate worker font heights
		strSQL = "with cteEPWorkerInfo as (\n" + 
				"  select \n" + 
				"  SUB_EPGV.id_explain_plan\n" + 
				"  , SUB_EPGV.show_worker_info\n" + 
				"  , SUB_EPGV.show_worker_actual_data_size\n" + 
				"  , SUB_EPGV.show_worker_actual_rows\n" + 
				"  , SUB_EPGV.show_worker_difference_time\n" + 
				"  , SUB_EPGV.show_worker_exclusive_time\n" + 
				"  from explain_plan_generated_values as SUB_EPGV\n" + 
				"  where SUB_EPGV.id_explain_plan = " + intIDExplainPlan + " -- id_explain_plan\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as SET_EPOA set\n" + 
				"workers_fontmetrics_height = workers_fontmetrics_height + (\n" + 
				"select\n" + 
				"  sum(\n" + 
				"    EPOW.actual_data_size_label_fontmetrics_height * CTEW.show_worker_actual_data_size\n" + 
				"    + EPOW.actual_rows_label_fontmetrics_height * CTEW.show_worker_actual_rows\n" + 
				"    + EPOW.difference_time_label_fontmetrics_height * CTEW.show_worker_difference_time\n" + 
				"    + EPOW.worker_title_label_fontmetrics_height * CTEW.show_worker_info\n" + 
				"    + " + withZoom(WORKER_TEXT_FONT_SIZE) + "\n" + // we need a little space between worker information "blocks"
				"  )\n" + 
				"  from explain_plan_operator_workers as EPOW\n" + 
				"  inner join explain_plan_operator_annotations as EPOA on EPOW.id_explain_plan = EPOA.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				"  inner join explain_plan_operator_show_workers as EPOSW on EPOW.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"  left outer join cteEPWorkerInfo as CTEW on EPOW.id_explain_plan = CTEW.id_explain_plan\n" + 
				"  where CTEW.show_worker_info = 1\n" + 
				"    and EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SET_EPOA.id_explain_plan_property\n" + 
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				"  )\n" + 
				"where exists (\n" + 
				"select\n" + 
				"  EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				"  from explain_plan_operator_workers as EPOW\n" + 
				"  inner join explain_plan_operator_annotations as EPOA on EPOW.id_explain_plan = EPOA.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				"  inner join explain_plan_operator_show_workers as EPOSW on EPOW.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"  left outer join cteEPWorkerInfo as CTEW on EPOW.id_explain_plan = CTEW.id_explain_plan\n" + 
				"  where CTEW.show_worker_info = 1\n" + 
				"    and EPOA.id_explain_plan = SET_EPOA.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = SET_EPOA.id_explain_plan_property\n" + 
				"  group by EPOA.id_explain_plan, EPOA.id_explain_plan_property\n" + 
				")";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);		

		/*
select id_explain_plan_property, workers_fontmetrics_height
from explain_plan_operator_annotations
order by id_explain_plan_property;
*/

		// set height of each box, from top edge
		// to the bottom side
		// max of worker (if any), or OPERATOR_BOX_HEIGHT
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" + 
				"box_height = max(\n" + 
				withZoom(OPERATOR_BOX_HEIGHT) + "\n" +
				"    , workers_fontmetrics_height + (select inside_box_top_margin from config) + (select inside_box_bottom_margin from config)\n" + 
				")\n" + 
				"where EPOA.id_explain_plan = " + intIDExplainPlan + "\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

/*
select id_explain_plan_property, x_cart, y_cart, box_height from explain_plan_operator_annotations;
*/

		// set height of each operator, from top edge (probably the top side of the north_1 text)
		// to the bottom edge (probably the bottom side of south_4 text)
		strSQL = "update explain_plan_operator_annotations set\n" + 
				"operator_height = \n" + 
				"    north_1_fontmetrics_height\n" + 
				"    + (select outside_box_top_margin from config)\n" +
				"    + box_height\n" +
				"    + south_2_fontmetrics_height\n" + 
				"    + south_3_fontmetrics_height\n" + 
				"    + south_4_fontmetrics_height\n" + 
				"    + south_5_fontmetrics_height\n" + 
				"where id_explain_plan = " + intIDExplainPlan + ";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, x_cart, y_cart, operator_height from explain_plan_operator_annotations;
*/
		// set row_height, based on the max height of all operators in that row
		strSQL = "with cteMaxHeight as (\n" + 
				"  select SUB_EPOA.id_explain_plan, SUB_EPOA.id_explain_plan_property, SUB_EPOA.y_cart\n" + 
				"  , max(SUB_EPOA.operator_height) as row_height\n" +
				"  from explain_plan_operator_annotations as SUB_EPOA\n" + 
				"  group by SUB_EPOA.id_explain_plan, SUB_EPOA.y_cart\n" + 
				")\n" +
				"update explain_plan_operator_annotations as EPOA set\n" + 
				"row_height = (\n" +
				"  select row_height\n" +
				"  from cteMaxHeight\n" +
				"  where id_explain_plan = EPOA.id_explain_plan\n" +
				"  and y_cart = EPOA.y_cart\n" +
				")\n" +
				"where exists (\n" + 
				"  select row_height\n" +
				"  from cteMaxHeight\n" +
				"  where id_explain_plan = EPOA.id_explain_plan\n" +
				"  and y_cart = EPOA.y_cart\n" +
				")\n" + 
				"and id_explain_plan = " + intIDExplainPlan + ";\n" + 
				""
				;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, x_cart, y_cart, operator_height, row_height from explain_plan_operator_annotations;
*/
		
		// add vertical margin

		// set top of each row, starting at topmost edge
		strSQL = "with cteUpdateVerticalSpace as (\n" + 
				"  select CTE.id_explain_plan, CTE.y_cart\n" + 
				"  , sum(CTE.row_height) over (partition by CTE.id_explain_plan order by CTE.id_explain_plan, CTE.y_cart) as row_boundary_top\n" + 
				"  from explain_plan_operator_annotations as CTE\n" + 
				"  group by CTE.id_explain_plan, CTE.y_cart\n" + 
				")\n" + 
				"update explain_plan_operator_annotations as EPOA set\n" + 
				"row_boundary_top = (\n" + 
				"  select (\n" + 
				"    select coalesce(CTE.row_boundary_top, 0) as row_boundary_top\n" + 
				"    from cteUpdateVerticalSpace as CTE\n" + 
				"    where EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"      and EPOA.y_cart - 1 = CTE.y_cart\n" + // may be "and EPOA.y_cart = CTE.y_cart" 
				"    )\n" + 
				")\n" + 
				"where exists (\n" + 
				"  select CTE.row_boundary_top\n" + 
				"  from cteUpdateVerticalSpace as CTE\n" + 
				"  where EPOA.id_explain_plan = CTE.id_explain_plan\n" + 
				"    and EPOA.y_cart = CTE.y_cart\n" + 
				")\n" + 
				"and id_explain_plan = " + intIDExplainPlan + ";\n" + 
				"update explain_plan_operator_annotations set\n" +
				"row_boundary_top = 0 where row_boundary_top is null and\n" +
				"id_explain_plan = " + intIDExplainPlan + "\n" +
				"";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

/*
select id_explain_plan_property, x_cart, y_cart, row_height, row_boundary_top from explain_plan_operator_annotations order by id_explain_plan_property;
*/

/*
Before moving over column_boundary_left:
select id_explain_plan_property, x_cart, y_cart, column_boundary_left from explain_plan_operator_annotations;
*/
		// bump over row_boundary_top by VERTICAL_SPACE
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" +
				"row_boundary_top = row_boundary_top + " + withZoom(VERTICAL_SPACE) + " * (y_cart - 1)\n" +
				"where id_explain_plan = " + intIDExplainPlan + ";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);

		// set row_boundary_bottom
		strSQL = "update explain_plan_operator_annotations as EPOA set\n" +
				"row_boundary_bottom = row_boundary_top + row_height\n" +
				"where id_explain_plan = " + intIDExplainPlan + ";\n";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, x_cart, y_cart, row_height, previous_y_rect, y_rect from explain_plan_operator_annotations order by id_explain_plan_property;
*/

		// set top and bottom side of each operator box 
		strSQL = "update explain_plan_operator_annotations set\n" +
			"box_top = row_boundary_top + north_1_fontmetrics_height\n" +
			"where id_explain_plan = " + intIDExplainPlan + ";\n" + 
			"update explain_plan_operator_annotations set\n" +
			"box_bottom = box_top + box_height\n" +
			"where id_explain_plan = " + intIDExplainPlan + ";\n"; 
		if (bolShowSQL == true) { System.out.println(strSQL); }
		Database.dml(strSQL);
/*
select id_explain_plan_property, previous_y_rect, y_rect, operator_box_top_y_rect, operator_box_bottom_y_rect
from explain_plan_operator_annotations
order by id_explain_plan_property;
*/
	}

	public void clearClickableShapes() {
		propertyVsShapesMap = new HashMap<>();
		showWorkerToggleVsShapesMap = new HashMap<>();
	}
	
	// include time slider
	public void drawOperatorBox(Graphics2D g, int id_explain_plan_property, int box_left, int box_right, int box_top, int box_bottom
		, boolean has_workers, boolean workers_expanded, int intSliderType, Double sliderMs, Double startMs, Double durationMs, Double costSlider, Double startupCost, Double costDifferential) {

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		float arc = withZoom(OPERATOR_BOX_CORNER_RADIUS);
		float x = GRAPH_LEFT_MARGIN + box_left;
		float y = GRAPH_TOP_MARGIN + box_top;
		float w = box_right - box_left;
		float h = box_bottom - box_top;
		RoundRectangle2D rctOperatorBox = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

		// Soft drop shadow (not used for hit-testing)
		int shadowOffset = Math.max(1, withZoom(2));
		g.setColor(new Color(0, 0, 0, UiTheme.isDark() ? 90 : 45));
		g.fill(new RoundRectangle2D.Float(x + shadowOffset, y + shadowOffset, w, h, arc, arc));

		boolean hovered = hoveredOperatorId != null && hoveredOperatorId.intValue() == id_explain_plan_property;
		boolean selected = selectedOperatorId != null && selectedOperatorId.intValue() == id_explain_plan_property;
		// Hover may tint the box; selection uses the outer rounded outline only (no fill).
		if (hovered && !selected) {
			g.setColor(new Color(0xF1, 0xF5, 0xF9, UiTheme.isDark() ? 80 : 160));
			g.fill(rctOperatorBox);
		}

		g.setStroke(new BasicStroke(selected ? OPERATOR_BOX_THICKNESS + 1 : OPERATOR_BOX_THICKNESS));

		// partially fill box if timer / cost slider is being used
		if (getSliderType() == SliderPanel.SLIDER_TYPE_TIME && sliderMs > 0 && startMs != null) {
			double sliderMsMinusStartMs = sliderMs - startMs;
			double percentageOfBoxFilled = sliderMsMinusStartMs / durationMs; 
			if (percentageOfBoxFilled > 1.0)
				percentageOfBoxFilled = 1.0;
			int totalBoxWidth = box_right - box_left;
			int pixelsFilled = (int) ((double) totalBoxWidth * percentageOfBoxFilled);
			int pixelsBoxLeft = box_right - pixelsFilled;
			
			if (percentageOfBoxFilled == 1.0)
				g.setColor(TIME_SLIDER_BACKGROUND_COLOR_FILLED);
			else
				g.setColor(TIME_SLIDER_BACKGROUND_COLOR);

			Shape priorClip = g.getClip();
			g.clip(rctOperatorBox);
			g.fillRect(GRAPH_LEFT_MARGIN + pixelsBoxLeft, GRAPH_TOP_MARGIN + box_top, box_right - pixelsBoxLeft, box_bottom - box_top);
			g.setClip(priorClip);
		} else if (getSliderType() == SliderPanel.SLIDER_TYPE_COST && costSlider > 0 && startupCost != null) {
			double sliderCostMinusStartupCost = costSlider - startupCost;
			double percentageOfBoxFilled = sliderCostMinusStartupCost / costDifferential; 
			if (percentageOfBoxFilled > 1.0)
				percentageOfBoxFilled = 1.0;
			int totalBoxWidth = box_right - box_left;
			int pixelsFilled = (int) ((double) totalBoxWidth * percentageOfBoxFilled);
			int pixelsBoxLeft = box_right - pixelsFilled;
			
			if (percentageOfBoxFilled == 1.0)
				g.setColor(COST_SLIDER_BACKGROUND_COLOR_FILLED);
			else
				g.setColor(COST_SLIDER_BACKGROUND_COLOR);

			Shape priorClip = g.getClip();
			g.clip(rctOperatorBox);
			g.fillRect(GRAPH_LEFT_MARGIN + pixelsBoxLeft, GRAPH_TOP_MARGIN + box_top, box_right - pixelsBoxLeft, box_bottom - box_top);
			g.setClip(priorClip);
		}

		Color boxStroke = UiTheme.isDark() ? new Color(0xCB, 0xD5, 0xE1) : OPERATOR_BOX_COLOR;
		if (selected) {
			boxStroke = new Color(0x25, 0x63, 0xEB);
		}
		g.setColor(boxStroke);
		updateShapesMap(id_explain_plan_property, rctOperatorBox);
		g.draw(rctOperatorBox);
		
		// Worker toggle is drawn after operator icons so it stays on top.
	}

	/** Draws the worker expand/collapse control above icons (box + up/down caret). */
	private void drawWorkerToggleControl(Graphics2D g, int id_explain_plan_property, int box_right, int box_top, boolean workers_expanded) {
		g.setStroke(new BasicStroke(OPERATOR_BOX_THICKNESS));
		float arc = withZoom(4);
		RoundRectangle2D rctWorkerToggleBox = new RoundRectangle2D.Float(
				GRAPH_LEFT_MARGIN + box_right - withZoom(15), GRAPH_TOP_MARGIN + box_top, withZoom(15), withZoom(15), arc, arc);
		updateWorkerToggleShapesMap(id_explain_plan_property, rctWorkerToggleBox);
		Color previous = g.getColor();
		g.setColor(UiTheme.isDark() ? new Color(0x1E, 0x29, 0x3B) : Color.WHITE);
		g.fill(rctWorkerToggleBox);
		g.setColor(UiTheme.isDark() ? new Color(0xCB, 0xD5, 0xE1) : OPERATOR_BOX_COLOR);
		g.draw(rctWorkerToggleBox);
		drawWorkerToggleCaret(g, rctWorkerToggleBox, workers_expanded);
		g.setColor(previous);
	}

	private void drawWorkerToggleCaret(Graphics2D g, RectangularShape box, boolean expanded) {
		double cx = box.getCenterX();
		double cy = box.getCenterY();
		double halfWidth = box.getWidth() * 0.28;
		double halfHeight = box.getHeight() * 0.22;
		Path2D caret = new Path2D.Double();
		if (expanded) {
			// point up — click to collapse
			caret.moveTo(cx, cy - halfHeight);
			caret.lineTo(cx + halfWidth, cy + halfHeight);
			caret.lineTo(cx - halfWidth, cy + halfHeight);
		} else {
			// point down — click to expand
			caret.moveTo(cx, cy + halfHeight);
			caret.lineTo(cx - halfWidth, cy - halfHeight);
			caret.lineTo(cx + halfWidth, cy - halfHeight);
		}
		caret.closePath();
		g.fill(caret);
	}
	
	// map node type to icon file
	private String planIconFile(String nodeType, String joinType, String operation, String strategy, String command) {
		String iconFileName = null;
		// Text plans keep text-native Node Type in properties; icons use JSON base names.
		nodeType = ExplainTextPlanConverter.resolveIconNodeType(nodeType, joinType, operation, strategy);
		if (nodeType == null) { nodeType = ""; }
		if (joinType == null) { joinType = ""; }
		if (operation == null) { operation = ""; }
		if (strategy == null) { strategy = ""; }
		if (command == null) { command = ""; }
		if (nodeType == "" && joinType == "" && operation == "" && strategy == "" && command == "") {
			iconFileName = "Server.png";
		} else if (nodeType.equalsIgnoreCase("aggregate") == true) {
			iconFileName = "ex_aggregate.svg";
		} else if (nodeType.equalsIgnoreCase("append") == true) {
			iconFileName = "ex_append.svg";
		} else if (nodeType.equalsIgnoreCase("BitmapAnd") == true) {
			iconFileName = "ex_bmp_and.svg";
		} else if (nodeType.equalsIgnoreCase("BitmapOr") == true) {
			iconFileName = "ex_bmp_or.svg";
		} else if (nodeType.equalsIgnoreCase("bitmap heap scan") == true) {
			iconFileName = "ex_bmp_heap.svg";
		} else if (nodeType.equalsIgnoreCase("bitmap index scan") == true) {
			iconFileName = "ex_bmp_index.svg";
		} else if (nodeType.equalsIgnoreCase("cte scan") == true || nodeType.equalsIgnoreCase("with cte") == true) {
			iconFileName = "ex_cte_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Custom Scan") == true) {
			iconFileName = "ex_custom_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Foreign scan") == true) {
			iconFileName = "ex_foreign_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Function Scan") == true) {
			iconFileName = "ex_table_func_scan.svg";
		} else if (nodeType.equalsIgnoreCase("gather") == true) {
			iconFileName = "ex_gather_motion.svg";
		} else if (nodeType.equalsIgnoreCase("gather merge") == true) {
			iconFileName = "ex_gather_merge.svg";
		} else if (nodeType.equalsIgnoreCase("group") == true) {
			iconFileName = "ex_group.svg";
		} else if (nodeType.equalsIgnoreCase("hash") == true) {
			iconFileName = "ex_hash.svg";
		} else if (nodeType.equalsIgnoreCase("hash join") == true && (joinType.isBlank()
				|| joinType.equalsIgnoreCase("full") || joinType.equalsIgnoreCase("inner")
				|| joinType.equalsIgnoreCase("anti"))) {
			// Parallel Hash Join often has no Join Type (defaults to Inner); Anti shares this icon
			iconFileName = "ex_join.svg";
		} else if (nodeType.equalsIgnoreCase("hash join") == true && (joinType.equalsIgnoreCase("left") == true || joinType.equalsIgnoreCase("right") == true || joinType.equalsIgnoreCase("semi") == true)) {
			iconFileName = "ex_hash_semi_join.svg";
		} else if (nodeType.equalsIgnoreCase("Incremental Sort") == true) {
			iconFileName = "ex_incremental_sort.svg";
		} else if (nodeType.equalsIgnoreCase("index only scan") == true) {
			iconFileName = "ex_index_only_scan.svg";
		} else if (nodeType.equalsIgnoreCase("index scan") == true) {
			iconFileName = "ex_index_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Limit") == true) {
			iconFileName = "ex_limit.svg";
		} else if (nodeType.equalsIgnoreCase("LockRows") == true) {
			iconFileName = "ex_lock_rows.svg";
		} else if (nodeType.equalsIgnoreCase("materialize") == true) {
			iconFileName = "ex_materialize.svg";
		} else if (nodeType.equalsIgnoreCase("Memoize") == true) {
			iconFileName = "ex_memoize.svg";
		} else if (nodeType.equalsIgnoreCase("Merge Append") == true) {
			iconFileName = "ex_merge_append.svg";
		} else if (nodeType.equalsIgnoreCase("merge join") == true && (joinType.equalsIgnoreCase("anti") == true)) {
			iconFileName = "ex_merge_anti_join.svg";
		} else if (nodeType.equalsIgnoreCase("merge join") == true && (joinType.isBlank()
				|| joinType.equalsIgnoreCase("full") || joinType.equalsIgnoreCase("inner"))) {
			iconFileName = "ex_merge.svg";
		} else if (nodeType.equalsIgnoreCase("merge join") == true && (joinType.equalsIgnoreCase("left") == true || joinType.equalsIgnoreCase("right") == true || joinType.equalsIgnoreCase("semi") == true)) {
			iconFileName = "ex_merge_semi_join.svg";
		} else if (nodeType.equalsIgnoreCase("modifytable") == true && operation.equalsIgnoreCase("delete") == true) {
			iconFileName = "ex_delete.svg";
		} else if (nodeType.equalsIgnoreCase("modifytable") == true && operation.equalsIgnoreCase("insert") == true) {
			iconFileName = "ex_insert.svg";
		} else if (nodeType.equalsIgnoreCase("modifytable") == true && operation.equalsIgnoreCase("merge") == true) {
			iconFileName = "ex_merge_modify.svg";
		} else if (nodeType.equalsIgnoreCase("modifytable") == true && operation.equalsIgnoreCase("update") == true) {
			iconFileName = "ex_update.svg";
		} else if (nodeType.equalsIgnoreCase("Named Tuplestore Scan") == true) {
			iconFileName = "ex_named_tuplestore_scan.svg";
		} else if (nodeType.equalsIgnoreCase("nested loop") == true && joinType.equalsIgnoreCase("anti") == true) {
			iconFileName = "ex_nested_loop_anti_join.svg";
		} else if (nodeType.equalsIgnoreCase("nested loop") == true && (joinType.isBlank()
				|| joinType.equalsIgnoreCase("full") || joinType.equalsIgnoreCase("inner"))) {
			iconFileName = "ex_nested.svg";
		} else if (nodeType.equalsIgnoreCase("nested loop") == true && (joinType.equalsIgnoreCase("left") == true || joinType.equalsIgnoreCase("right") == true || joinType.equalsIgnoreCase("semi") == true)) {
			iconFileName = "ex_nested_loop_semi_join.svg";
		} else if (nodeType.equalsIgnoreCase("ProjectSet") == true) {
			iconFileName = "ex_projectset.svg";
		} else if (nodeType.equalsIgnoreCase("Recursive Union") == true) {
			iconFileName = "ex_recursive_union.svg";
		} else if (nodeType.equalsIgnoreCase("Result") == true) {
			iconFileName = "ex_result.svg";
		} else if (nodeType.equalsIgnoreCase("Sample Scan") == true) {
			iconFileName = "ex_sample_scan.svg";
		} else if (nodeType.equalsIgnoreCase("seq scan") == true) {
			iconFileName = "ex_scan.svg";
		} else if (nodeType.equalsIgnoreCase("SetOp") == true && strategy.equalsIgnoreCase("Hashed") == true && command.equalsIgnoreCase("except") == true) {
			iconFileName = "ex_hash_setop_except.svg";
		} else if (nodeType.equalsIgnoreCase("SetOp") == true && strategy.equalsIgnoreCase("Hashed") == true && command.equalsIgnoreCase("except all") == true) {
			iconFileName = "ex_hash_setop_except_all.svg";
		} else if (nodeType.equalsIgnoreCase("SetOp") == true && strategy.equalsIgnoreCase("Hashed") == true && command.equalsIgnoreCase("intersect") == true) {
			iconFileName = "ex_hash_setop_intersect.svg";
		} else if (nodeType.equalsIgnoreCase("SetOp") == true && strategy.equalsIgnoreCase("Hashed") == true && command.equalsIgnoreCase("intersect all") == true) {
			iconFileName = "ex_hash_setop_intersect_all.svg";
		} else if (nodeType.equalsIgnoreCase("SetOp") == true && strategy.equalsIgnoreCase("Hashed") == true && command.equalsIgnoreCase("unknown") == true) {
			iconFileName = "ex_hash_setop_unknown.svg";
		} else if (nodeType.equalsIgnoreCase("SetOp") == true) {
			iconFileName = "ex_setop.svg";
		} else if (nodeType.equalsIgnoreCase("sort") == true) {
			iconFileName = "ex_sort.svg";
		} else if (nodeType.equalsIgnoreCase("subquery scan") == true) {
			iconFileName = "ex_subplan.svg"; // I don't think this is right
		} else if (nodeType.equalsIgnoreCase("Table Function Scan") == true) {
			iconFileName = "ex_table_func_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Tid Range Scan") == true) {
			iconFileName = "ex_tid_range_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Tid Scan") == true) {
			iconFileName = "ex_tid_scan.svg";
		} else if (nodeType.equalsIgnoreCase("Unique") == true) {
			iconFileName = "ex_unique.svg";
		} else if (nodeType.equalsIgnoreCase("Values Scan") == true) {
			iconFileName = "ex_values_scan.svg";
		} else if (nodeType.equalsIgnoreCase("windowagg") == true) {
			iconFileName = "ex_window_aggregate.svg";
		} else if (nodeType.equalsIgnoreCase("WorkTable Scan") == true) {
			iconFileName = "ex_worktable_scan.svg";
		} else {
			iconFileName = "ex_unknown.svg";
		}
		return iconFileName;
	}
	
	// show PNG icon
	public void drawOperatorPNGIcon(Graphics2D g, int box_left, int box_right, int box_top, int box_bottom, String node_type
			, String join_type, String operation, String strategy, String command) {
		URL objURL = null;
		BufferedImage objImage = null;
		String iconFile = planIconFile(node_type, join_type, operation, strategy, command);
		objURL = this.getClass().getResource("/images/" + iconFile);
		try {
			objImage = ImageIO.read(objURL);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (objImage == null) {
			return;
		}
		// Match SVG icons: fill the operator box (Server.png is 16x16; without scaling it looks tiny)
		int iconSize = withZoom(OPERATOR_BOX_WIDTH);
		g.drawImage(objImage, GRAPH_LEFT_MARGIN + box_left, GRAPH_TOP_MARGIN + box_top, iconSize, iconSize, null);
	}

	class BufferedImageTranscoder extends ImageTranscoder {
	  @Override
	  public BufferedImage createImage(int w, int h) {
	    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    return bi;
	  }

	  @Override
	  public void writeImage(BufferedImage img, TranscoderOutput output) {
	    this.img = img;
	  }

	  public BufferedImage getBufferedImage() {
	    return img;
	  }
	  
	  private BufferedImage img = null;
	}

	// show SVG icon
//	public void drawOperatorSVGIcon(Graphics2D g, int box_left, int box_right, int box_top, int box_bottom, String node_type, String join_type, boolean previousIconTypeIdentical) {
	public BufferedImage drawOperatorSVGIcon(Graphics2D g, int box_left, int box_right, int box_top, int box_bottom
			, String node_type, String join_type, String operation, String strategy, String command
			, boolean previousIconTypeIdentical, BufferedImage objImage) {
		boolean bolShowProperties = false;
		//URL objURL = null;
		//BufferedImage objImage = null;
		if (bolShowProperties == true) { System.out.println("node_type: " + node_type + ", join_type: " + join_type); }
		if (bolShowProperties == true) { System.out.println("previousIconTypeIdentical: " + previousIconTypeIdentical); }
		if (previousIconTypeIdentical == false) {
			String iconFile = planIconFile(node_type, join_type, operation, strategy, command);
			URL objURL = getClass().getResource("/images/" + iconFile);
			
		    BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();
			
		    imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) withZoom(OPERATOR_BOX_WIDTH));
		    imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) withZoom(OPERATOR_BOX_WIDTH));
		    
		    TranscoderInput input = new TranscoderInput(objURL.toString());
		    try {
				imageTranscoder.transcode(input, null);
			} catch (TranscoderException e) {
				e.printStackTrace();
			}
		
		    //BufferedImage objImage = imageTranscoder.getBufferedImage();
		    objImage = imageTranscoder.getBufferedImage();
		}
		g.drawImage(objImage, GRAPH_LEFT_MARGIN + box_left, GRAPH_TOP_MARGIN + box_top, null);
		return objImage;
	}
	
//	private void drawOperatorIcon(Graphics2D g, int box_left, int box_right, int box_top, int box_bottom, String node_type, String join_type, boolean previousIconTypeIdentical) {
	private BufferedImage drawOperatorIcon(Graphics2D g, int box_left, int box_right, int box_top, int box_bottom
			, String node_type, String join_type, String operation, String strategy, String command
			, boolean previousIconTypeIdentical, BufferedImage objImage) {
		String iconFile = planIconFile(node_type, join_type, operation, strategy, command);
		if (iconFile.endsWith(".png") ==  true) {
			drawOperatorPNGIcon(g, box_left, box_right, box_top, box_bottom, node_type, join_type, operation, strategy, command);
		} else if (iconFile.endsWith(".svg") ==  true) {
			objImage = drawOperatorSVGIcon(g, box_left, box_right, box_top, box_bottom, node_type, join_type, operation, strategy, command, previousIconTypeIdentical, objImage);
		}
		return objImage;
	}
	
	/** Same typeface as Plan Source field labels (UIManager Label.font). */
	private Font operatorLabelFont() {
		Font labelFont = javax.swing.UIManager.getFont("Label.font");
		if (labelFont == null) {
			labelFont = new Font(OPERATOR_FONT_NAME, OPERATOR_FONT_STYLE, OPERATOR_FONT_SIZE);
		}
		return labelFont.deriveFont(OPERATOR_FONT_STYLE, (float) withZoom(Math.max(labelFont.getSize(), OPERATOR_FONT_SIZE)));
	}

	/** North/south operator labels: blue in light themes, soft white in dark. */
	private Color operatorLabelColor() {
		return UiTheme.isDark() ? OPERATOR_FONT_FOREGROUND_COLOR_DARK : OPERATOR_FONT_FOREGROUND_COLOR;
	}

	private Color workerTitleColor() {
		return UiTheme.isDark() ? OPERATOR_FONT_FOREGROUND_COLOR_DARK : WORKER_TITLE_FONT_COLOR;
	}

	private Color workerTextColor() {
		return UiTheme.isDark() ? OPERATOR_FONT_FOREGROUND_COLOR_DARK : WORKER_TEXT_FONT_COLOR;
	}

	public void drawNorthText(Graphics2D g, int x_center, int y_baseline, String north_1_text
			, int north_1_fontmetrics_width, int north_1_fontmetrics_height, int outside_box_top_margin
			, boolean north_1_highlight_1, boolean north_1_highlight_2
			) {

		if (north_1_text == null) return;
		
		int highlightMargin = withZoom(5);
		
		// draw text — measure with the live Label.font so centering matches the pixels drawn
		g.setFont(operatorLabelFont());
		FontMetrics fm = g.getFontMetrics();
		int textWidth = Math.max(fm.stringWidth(north_1_text), north_1_fontmetrics_width);
		int textHeight = Math.max(fm.getAscent(), north_1_fontmetrics_height);
		int textX = GRAPH_LEFT_MARGIN + x_center - (textWidth / 2);
		Color textColor;
		if (north_1_highlight_1 == true) {
			g.setColor(HIGHLIGHT_1_FONT_BACKGROUND_COLOR);
			g.fillRect(textX - highlightMargin
					, GRAPH_TOP_MARGIN + y_baseline - textHeight
					, textWidth + highlightMargin * 2, textHeight);
			textColor = HIGHLIGHT_1_FONT_FOREGROUND_COLOR;
		} else if (north_1_highlight_2 == true) {
			g.setColor(HIGHLIGHT_2_FONT_BACKGROUND_COLOR);
			g.fillRect(textX - highlightMargin
					, GRAPH_TOP_MARGIN + y_baseline - textHeight
					, textWidth + highlightMargin * 2, textHeight);
			textColor = HIGHLIGHT_2_FONT_FOREGROUND_COLOR;
		} else {
			textColor = operatorLabelColor();
		}
		drawStringWithFindHighlights(g, north_1_text, textX,
				GRAPH_TOP_MARGIN + y_baseline - withZoom(outside_box_top_margin), textColor);
	}
	
	public void drawWorkerText(Graphics2D g, int id_explain_plan_property, int box_left, int box_top, int worker_row_number
			, int inside_box_left_margin, int inside_box_top_margin
			, String worker_title_label_string, int worker_title_label_fontmetrics_height, int worker_title_label_fontmetrics_width
			, String actual_data_size_label_string, int actual_data_size_label_fontmetrics_height, int actual_data_size_label_fontmetrics_width, boolean actual_data_size_highlight_1
			, String actual_rows_label_string, int actual_rows_label_fontmetrics_height, int actual_rows_label_fontmetrics_width, boolean actual_rows_highlight_1
			, String difference_time_label_string, int difference_time_label_fontmetrics_height, int difference_time_label_fontmetrics_width, boolean difference_time_highlight_1
			, String exclusive_time_label_string, int exclusive_time_label_fontmetrics_height, int exclusive_time_label_fontmetrics_width, boolean exclusive_time_highlight_1
		) {
		int y_delta = 0;
		int y_baseline = box_top + worker_title_label_fontmetrics_height;
		//int y_baseline = operator_box_top_y_rect;
		//int y_baseline = operator_box_top_y_rect + withZoom(WORKER_TITLE_FONT_SIZE);
		int with_margin_box_left = box_left + inside_box_left_margin;

		if (actual_data_size_label_string != null) y_delta += actual_data_size_label_fontmetrics_height;
		if (actual_rows_label_string != null) y_delta += actual_rows_label_fontmetrics_height;
		if (difference_time_label_string != null) y_delta += difference_time_label_fontmetrics_height;
		
		// I don't think exclusive time worker has any real meaning
		if (exclusive_time_label_string != null) y_delta += exclusive_time_label_fontmetrics_height;

		// bump it down more
		// using WORKER_TEXT_FONT_SIZE b/c we don't know which label above will be used
		if (worker_title_label_string != null) y_delta += worker_title_label_fontmetrics_height + withZoom(WORKER_TEXT_FONT_SIZE);
		
		y_delta *= worker_row_number;
		y_baseline += y_delta;
		
		// allow for the margin
		y_baseline += inside_box_top_margin;

		// draw worker text

		// title
		g.setFont(new Font(WORKER_TITLE_FONT_NAME, WORKER_TITLE_FONT_STYLE, withZoom(WORKER_TITLE_FONT_SIZE)));
		g.setColor(workerTitleColor());
		g.drawString(worker_title_label_string, GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline);
		y_baseline += actual_data_size_label_fontmetrics_height; // using the worker title height may throw off vertical spacing

		// text
		// actual_data_size_label_string
		g.setFont(new Font(WORKER_TEXT_FONT_NAME, WORKER_TEXT_FONT_STYLE, withZoom(WORKER_TEXT_FONT_SIZE)));
		if (actual_data_size_label_string != null) {
			if (actual_data_size_highlight_1 == true) {
				g.setColor(HIGHLIGHT_1_FONT_BACKGROUND_COLOR);
				g.fillRect(GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline - actual_data_size_label_fontmetrics_height, actual_data_size_label_fontmetrics_width, actual_data_size_label_fontmetrics_height);
				g.setColor(HIGHLIGHT_1_FONT_FOREGROUND_COLOR);
			}
			g.drawString(actual_data_size_label_string, GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline);
			g.setColor(workerTextColor());
			y_baseline += actual_data_size_label_fontmetrics_height;
		}
		
		// actual_rows_label_string
		g.setFont(new Font(WORKER_TEXT_FONT_NAME, WORKER_TEXT_FONT_STYLE, withZoom(WORKER_TEXT_FONT_SIZE)));
		if (actual_rows_label_string != null) {
			if (actual_rows_highlight_1 == true) {
				g.setColor(HIGHLIGHT_1_FONT_BACKGROUND_COLOR);
				g.fillRect(GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline - actual_rows_label_fontmetrics_height, actual_rows_label_fontmetrics_width, actual_rows_label_fontmetrics_height);
				g.setColor(HIGHLIGHT_1_FONT_FOREGROUND_COLOR);
			}
			g.drawString(actual_rows_label_string, GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline);
			g.setColor(workerTextColor());
			y_baseline += actual_rows_label_fontmetrics_height;
		}
		
		// difference_time_label_string
		if (difference_time_label_string != null) {
			if (difference_time_highlight_1 == true) {
				g.setColor(HIGHLIGHT_1_FONT_BACKGROUND_COLOR);
				g.fillRect(GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline - difference_time_label_fontmetrics_height, difference_time_label_fontmetrics_width, difference_time_label_fontmetrics_height);
				g.setColor(HIGHLIGHT_1_FONT_FOREGROUND_COLOR);
			}
			g.drawString(difference_time_label_string, GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline);
			g.setColor(workerTextColor());
			y_baseline += difference_time_label_fontmetrics_height;
		}

		// exclusive_time_label_string
		if (exclusive_time_label_string != null) {
			if (exclusive_time_highlight_1 == true) {
				g.setColor(HIGHLIGHT_1_FONT_BACKGROUND_COLOR);
				g.fillRect(GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline - exclusive_time_label_fontmetrics_height, exclusive_time_label_fontmetrics_width, exclusive_time_label_fontmetrics_height);
				g.setColor(HIGHLIGHT_1_FONT_FOREGROUND_COLOR);
			}
			g.drawString(exclusive_time_label_string, GRAPH_LEFT_MARGIN + with_margin_box_left, GRAPH_TOP_MARGIN + y_baseline);
			g.setColor(workerTextColor());
			y_baseline += exclusive_time_label_fontmetrics_height;
		}
		
	}
	
	// REFACTORED
	public void drawSouthText(Graphics2D g, int id_explain_plan_property, int x_center, int y_baseline
			, String south_1_text, int south_1_fontmetrics_width, int south_1_fontmetrics_height
			, String south_2_text, int south_2_fontmetrics_width, int south_2_fontmetrics_height
			, String south_3_text, int south_3_fontmetrics_width, int south_3_fontmetrics_height
			, String south_4_text, int south_4_fontmetrics_width, int south_4_fontmetrics_height
			, String south_5_text, int south_5_fontmetrics_width, int south_5_fontmetrics_height
		) {
	
		// draw text — center with live FontMetrics so outline/hit bounds stay aligned
		g.setFont(operatorLabelFont());
		Color labelColor = operatorLabelColor();
		FontMetrics fm = g.getFontMetrics();
		y_baseline += south_1_fontmetrics_height; // bump it down
		if (south_1_text != null) {
			int w = Math.max(fm.stringWidth(south_1_text), south_1_fontmetrics_width);
			drawStringWithFindHighlights(g, south_1_text,
					GRAPH_LEFT_MARGIN + x_center - (w / 2), GRAPH_TOP_MARGIN + y_baseline, labelColor);
		}
		y_baseline += south_1_fontmetrics_height;
		if (south_2_text != null) {
			int w = Math.max(fm.stringWidth(south_2_text), south_2_fontmetrics_width);
			drawStringWithFindHighlights(g, south_2_text,
					GRAPH_LEFT_MARGIN + x_center - (w / 2), GRAPH_TOP_MARGIN + y_baseline, labelColor);
		}
		y_baseline += south_2_fontmetrics_height;
		if (south_3_text != null) {
			int w = Math.max(fm.stringWidth(south_3_text), south_3_fontmetrics_width);
			drawStringWithFindHighlights(g, south_3_text,
					GRAPH_LEFT_MARGIN + x_center - (w / 2), GRAPH_TOP_MARGIN + y_baseline, labelColor);
		}
		y_baseline += south_3_fontmetrics_height;
		if (south_4_text != null) {
			int w = Math.max(fm.stringWidth(south_4_text), south_4_fontmetrics_width);
			drawStringWithFindHighlights(g, south_4_text,
					GRAPH_LEFT_MARGIN + x_center - (w / 2), GRAPH_TOP_MARGIN + y_baseline, labelColor);
		}
		y_baseline += south_4_fontmetrics_height;
		if (south_5_text != null) {
			int w = Math.max(fm.stringWidth(south_5_text), south_5_fontmetrics_width);
			drawStringWithFindHighlights(g, south_5_text,
					GRAPH_LEFT_MARGIN + x_center - (w / 2), GRAPH_TOP_MARGIN + y_baseline, labelColor);
		}
	}
	
	public void drawArrowAndText(Graphics2D g, int id_explain_plan_property, int arrow_line_thickness, int column_boundary_left
			, int column_boundary_right, int box_top, int box_bottom, int box_left
			, String arrow_text, int arrow_text_fontmetrics_width, int arrow_text_fontmetrics_height, int arrow_text_bottom_margin
			, boolean total_actual_rows_is_exact, boolean total_actual_mass_is_exact, boolean plan_width_is_zero
			, int previous_column_boundary_right, int previous_box_top, int previous_box_right
		) {

		boolean bolShowProperties = false;

		if (bolShowProperties == true) { System.out.print("arrow_text: " + arrow_text); }
		if (bolShowProperties == true) { System.out.print(" / box_top: " + box_top); }
		if (bolShowProperties == true) { System.out.print(" / box_bottom: " + box_bottom); }
		if (bolShowProperties == true) { System.out.println(" / box_bottom - box_top: " + (box_bottom - box_top)); }
		
		LineThicknesses enmLineThickness = getLineThicknessBy();
		Font arrowTextFont = null;
		Color arrowTextColor = null;

        // to keep from seeing more than one arrow, make sure the point is at a
        // 60 degree angle
        //double xDelta = 0.866025 * withZoom(OPERATOR_BOX_HEIGHT) / 2.5; // cos(30) * length; was "/ 2.5"
        //double yDelta = 0.5 * withZoom(OPERATOR_BOX_HEIGHT) / 2.5; // sin(30) * length; was "/ 2.5"
		double xDelta = 0.866025 * Math.max(withZoom(OPERATOR_BOX_HEIGHT) / 1.8, arrow_line_thickness * 1.9);
        double yDelta = 0.5 * Math.max(withZoom(OPERATOR_BOX_HEIGHT) / 2.0, arrow_line_thickness * 1.9);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// Flat shaft end flush against the child operator; tip against the parent.
		// Overlap shaft into the arrowhead so antialiasing does not leave a hairline gap
		int shaftIntoHead = Math.max(2, (int) (xDelta * 0.45));
		int[] xLines = {
			GRAPH_LEFT_MARGIN + box_left
			, GRAPH_LEFT_MARGIN + column_boundary_left
			, GRAPH_LEFT_MARGIN + previous_column_boundary_right
			, GRAPH_LEFT_MARGIN + previous_box_right + shaftIntoHead
		};
		int[] yLines = {
			GRAPH_TOP_MARGIN + box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2
			, GRAPH_TOP_MARGIN + box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2
			, GRAPH_TOP_MARGIN + previous_box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2
			, GRAPH_TOP_MARGIN + previous_box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2
		};
		Path2D shaft = new Path2D.Float();
		shaft.moveTo(xLines[0], yLines[0]);
		for (int i = 1; i < xLines.length; i++) {
			shaft.lineTo(xLines[i], yLines[i]);
		}
		// Light near the flat (operator) end; dark at the arrow tip
		Color arrowFlatEnd = UiTheme.isDark() ? new Color(0x93, 0xC5, 0xFD) : ARROW_LINE_COLOR_END;
		Color arrowTip = UiTheme.isDark() ? new Color(0x1D, 0x4E, 0xD8) : ARROW_LINE_COLOR;
		if (xLines[0] != xLines[xLines.length - 1] || yLines[0] != yLines[yLines.length - 1]) {
			g.setPaint(new LinearGradientPaint(
					(float) xLines[0], (float) yLines[0],
					(float) xLines[xLines.length - 1], (float) yLines[yLines.length - 1],
					new float[] { 0f, 1f },
					new Color[] { arrowFlatEnd, arrowTip }));
		} else {
			g.setColor(arrowTip);
		}
		g.setStroke(new BasicStroke(arrow_line_thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		g.draw(shaft);

		int[] xTriangle = {
			GRAPH_LEFT_MARGIN + previous_box_right
			,GRAPH_LEFT_MARGIN + previous_box_right + (int) xDelta
			,GRAPH_LEFT_MARGIN + previous_box_right + (int) xDelta
		};
		int[] yTriangle = {
			GRAPH_TOP_MARGIN + previous_box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2
			,GRAPH_TOP_MARGIN + previous_box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2 - (int) yDelta
			,GRAPH_TOP_MARGIN + previous_box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2 + (int) yDelta
		};
		g.setColor(arrowTip);
		g.setStroke(new BasicStroke(1));
		g.fillPolygon(xTriangle, yTriangle, xTriangle.length);

		if (arrow_text != null) {
			
			if (enmLineThickness == LineThicknesses.ACTUAL_DATA_MASS && total_actual_mass_is_exact == true) {
				arrowTextFont = new Font(ACTUAL_MASS_ARROW_FONT_NAME, ACTUAL_MASS_ARROW_FONT_STYLE, withZoom(ACTUAL_MASS_ARROW_FONT_SIZE));
				arrowTextColor = ACTUAL_MASS_ARROW_FONT_COLOR;
			} else if (enmLineThickness == LineThicknesses.ACTUAL_DATA_MASS && total_actual_mass_is_exact == false) {
				arrowTextFont = new Font(ESTIMATED_MASS_ARROW_FONT_NAME, ESTIMATED_MASS_ARROW_FONT_STYLE, withZoom(ESTIMATED_MASS_ARROW_FONT_SIZE));
				arrowTextColor = ESTIMATED_MASS_ARROW_FONT_COLOR;
			} else if (enmLineThickness == LineThicknesses.ACTUAL_ROWS && total_actual_rows_is_exact == true) {
				arrowTextFont = new Font(ACTUAL_ROWS_ARROW_FONT_NAME, ACTUAL_ROWS_ARROW_FONT_STYLE, withZoom(ACTUAL_ROWS_ARROW_FONT_SIZE));
				arrowTextColor = ACTUAL_ROWS_ARROW_FONT_COLOR;
			} else if (enmLineThickness == LineThicknesses.ACTUAL_ROWS && total_actual_rows_is_exact == false) {
				arrowTextFont = new Font(ESTIMATED_ROWS_ARROW_FONT_NAME, ESTIMATED_ROWS_ARROW_FONT_STYLE, withZoom(ESTIMATED_ROWS_ARROW_FONT_SIZE));
				arrowTextColor = ESTIMATED_ROWS_ARROW_FONT_COLOR;
			} else if (enmLineThickness == LineThicknesses.ESTIMATED_DATA_MASS) {
				arrowTextFont = new Font(ESTIMATED_MASS_ARROW_FONT_NAME, ESTIMATED_MASS_ARROW_FONT_STYLE, withZoom(ESTIMATED_MASS_ARROW_FONT_SIZE));
				arrowTextColor = ESTIMATED_MASS_ARROW_FONT_COLOR;
			} else if (enmLineThickness == LineThicknesses.ESTIMATED_ROWS) {
				arrowTextFont = new Font(ESTIMATED_ROWS_ARROW_FONT_NAME, ESTIMATED_ROWS_ARROW_FONT_STYLE, withZoom(ESTIMATED_ROWS_ARROW_FONT_SIZE));
				arrowTextColor = ESTIMATED_ROWS_ARROW_FONT_COLOR;
			}
			if (plan_width_is_zero == true)
				arrowTextColor = PLAN_WIDTH_0_ARROW_FONT_COLOR;

			g.setFont(arrowTextFont);
			int arrowWidth = arrowTextFont != null
					? Math.max(g.getFontMetrics().stringWidth(arrow_text), arrow_text_fontmetrics_width)
					: arrow_text_fontmetrics_width;
			drawStringWithFindHighlights(g, arrow_text,
					GRAPH_LEFT_MARGIN + box_left - arrowWidth,
					GRAPH_TOP_MARGIN + box_top + withZoom(OPERATOR_BOX_HEIGHT) / 2 - arrow_line_thickness / 2 - arrow_text_bottom_margin,
					arrowTextColor);
		}
	}

	/**
	 * Active Find query for substring highlights on north/south/arrow labels.
	 * Pass null or blank to clear. Triggers a repaint.
	 */
	public void setFindHighlightQuery(String query) {
		String next = query == null ? null : query.trim();
		if (next != null && next.isEmpty()) {
			next = null;
		}
		boolean unchanged = (findHighlightQuery == null && next == null)
				|| (findHighlightQuery != null && findHighlightQuery.equals(next));
		findHighlightQuery = next;
		// Always repaint when a query is active so Linux/X11 does not keep a stale frame
		// after Find updates selection without changing the search string.
		if (!unchanged || next != null) {
			repaint();
		}
	}

	/** Clear Paint/Composite left by SVG or gradients so label fills work on Linux. */
	private void resetDiagramTextGraphicsState(Graphics2D g) {
		g.setComposite(AlphaComposite.SrcOver);
		g.setPaint(operatorLabelColor());
		g.setStroke(new BasicStroke(1f));
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	/**
	 * Draw {@code text} at ({@code textX}, {@code baseline}), painting a yellow
	 * background behind each case-insensitive occurrence of the Find query.
	 * Uses {@link TextAttribute#BACKGROUND} (more reliable than fillRect on Linux/X11
	 * after Batik SVG painting) and also fills the match bounds explicitly.
	 */
	private void drawStringWithFindHighlights(Graphics2D g, String text, int textX, int baseline, Color defaultFg) {
		if (text == null || text.isEmpty()) {
			return;
		}
		String query = findHighlightQuery;
		if (query == null || query.isEmpty()) {
			g.setPaint(defaultFg);
			g.drawString(text, textX, baseline);
			return;
		}
		Paint priorPaint = g.getPaint();
		Composite priorComposite = g.getComposite();
		try {
			g.setComposite(AlphaComposite.SrcOver);
			FontMetrics fm = g.getFontMetrics();
			String lowerText = text.toLowerCase(java.util.Locale.ROOT);
			String lowerQuery = query.toLowerCase(java.util.Locale.ROOT);
			int queryLen = lowerQuery.length();
			if (queryLen == 0) {
				g.setPaint(defaultFg);
				g.drawString(text, textX, baseline);
				return;
			}

			AttributedString attributed = new AttributedString(text);
			attributed.addAttribute(TextAttribute.FONT, g.getFont());
			attributed.addAttribute(TextAttribute.FOREGROUND, defaultFg);
			boolean anyMatch = false;
			int searchFrom = 0;
			while (searchFrom < text.length()) {
				int match = lowerText.indexOf(lowerQuery, searchFrom);
				if (match < 0) {
					break;
				}
				int end = Math.min(text.length(), match + queryLen);
				if (end > match) {
					attributed.addAttribute(TextAttribute.BACKGROUND, FIND_MATCH_BACKGROUND, match, end);
					attributed.addAttribute(TextAttribute.FOREGROUND, FIND_MATCH_FOREGROUND, match, end);
					anyMatch = true;
				}
				searchFrom = Math.max(end, match + 1);
			}

			// Explicit fill behind matches — some Linux pipelines skip TextAttribute.BACKGROUND.
			if (anyMatch) {
				int ascent = fm.getAscent();
				int descent = Math.max(1, fm.getDescent());
				searchFrom = 0;
				while (searchFrom < text.length()) {
					int match = lowerText.indexOf(lowerQuery, searchFrom);
					if (match < 0) {
						break;
					}
					int end = Math.min(text.length(), match + queryLen);
					int prefixW = match == 0 ? 0 : fm.stringWidth(text.substring(0, match));
					int hitW = fm.stringWidth(text.substring(match, end));
					g.setPaint(FIND_MATCH_BACKGROUND);
					g.fillRect(textX + prefixW, baseline - ascent, Math.max(1, hitW), ascent + descent);
					searchFrom = Math.max(end, match + 1);
				}
			}

			g.setPaint(defaultFg);
			g.drawString(attributed.getIterator(), textX, baseline);
		} finally {
			g.setComposite(priorComposite);
			g.setPaint(priorPaint);
		}
	}

	public boolean equalsIgnoreCaseOrNull(String strValue1, String strValue2) {
		boolean bolReturn = false;
		if (strValue1 == null && strValue2 == null) bolReturn = true;
		else if (strValue1 == null && strValue2 != null) bolReturn = false;
		else if (strValue1 != null && strValue2 == null) bolReturn = false;
		else if (strValue1.equalsIgnoreCase(strValue2)) bolReturn = true;
		return bolReturn;
	}
	
	// REFACTORED
	public void drawOperations(Graphics gSimple, int intIDExplainPlan) {
		boolean bolShowSQL = false;
		boolean bolShowProperties = false;
		String strSQL;
		JsonArray jsnRows;
		JsonObject jsnRow;
		Graphics2D g = (Graphics2D) gSimple;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		neverExecutedOperatorIds = loadNeverExecutedOperatorIds(intIDExplainPlan);
		
		// set dimensions
		strSQL = "select\n";
		strSQL += "max(row_boundary_bottom) as row_boundary_bottom\n";
		strSQL += ", max(column_boundary_right) as column_boundary_right\n";
		strSQL += "from explain_plan_operator_annotations\n";
		strSQL += "where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				int row_boundary_bottom = jsnRow.getInteger("row_boundary_bottom");
				int column_boundary_right = jsnRow.getInteger("column_boundary_right");
				setXDimension(GRAPH_LEFT_MARGIN + column_boundary_right + GRAPH_RIGHT_MARGIN);
				setYDimension(GRAPH_TOP_MARGIN + row_boundary_bottom + GRAPH_BOTTOM_MARGIN);
			}
		}

		// draw and fill boxes w/worker info
		strSQL = "select\n" + 
				"EPOA.id_explain_plan_property\n" + 
				", EPOA.box_left\n" + 
				", EPOA.box_right\n" + 
				", EPOA.box_top\n" + 
				", EPOA.box_bottom\n" + 
				//", EPOA.parallel_leader_actual_startup_time_ms as start_ms\n" +
				", EPOA.min_actual_startup_time_ms as start_ms\n" + // use earliest start between PL and workers
				//", EPOA.parallel_leader_actual_total_time_ms - EPOA.parallel_leader_actual_startup_time_ms as duration_ms\n" +
				", EPOA.max_actual_total_time_ms - EPOA.min_actual_startup_time_ms as duration_ms\n" + // use latest end between PL and workers
				", EPOA.startup_cost * 1.0 as startup_cost\n" +
				", EPOA.total_cost * 1.0 - EPOA.startup_cost * 1.0 as cost_differential\n" +
				", case when count(EPOW.id_explain_plan_property) = 0 then 0 else 1 end as has_workers\n" + 
				", case when count(EPOSW.id_explain_plan_property) = 0 then 0 else 1 end as workers_expanded\n" + 
				"from explain_plan_operator_annotations as EPOA\n" + 
				"left outer join explain_plan_operator_workers as EPOW on EPOA.id_explain_plan = EPOW.id_explain_plan\n" + 
				"  and EPOA.id_explain_plan_property = EPOW.id_explain_plan_property\n" + 
				"left outer join explain_plan_operator_show_workers as EPOSW on EPOA.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"  and EPOA.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"where EPOA.id_explain_plan = " + idExplainPlan + "\n" + 
				"group by EPOA.id_explain_plan_property\n" + 
				";";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				int id_explain_plan_property = jsnRow.getInteger("id_explain_plan_property");
				int box_left = jsnRow.getInteger("box_left");
				int box_right = jsnRow.getInteger("box_right");
				int box_top = jsnRow.getInteger("box_top");
				int box_bottom = jsnRow.getInteger("box_bottom");
				//Double startMs = jsnRow.getDouble("actual_startup_time_ms");
				Double startMs = (Double) jsnRow.get("start_ms");
				//Double durationMs = jsnRow.getDouble("exclusive_time_ms");
				Double durationMs = (Double) jsnRow.get("duration_ms");
				//boolean has_workers = (jsnRow.get("has_workers") != null);
				Double startupCost = (Double) jsnRow.get("startup_cost");
				Double costDifferential = (Double) jsnRow.get("cost_differential");
				boolean has_workers = (jsnRow.getInteger("has_workers") == 1);
				boolean workers_expanded = (jsnRow.getInteger("workers_expanded") == 1);
				Composite priorMute = beginNeverExecutedMute(g, isNeverExecutedOperator(id_explain_plan_property));
				try {
					drawOperatorBox(g, id_explain_plan_property, box_left, box_right, box_top, box_bottom, has_workers, workers_expanded, getSliderType()
						, getPlanTimeSliderValueMs(), startMs, durationMs, getPlanCostSlider(), startupCost, costDifferential);
				} finally {
					endNeverExecutedMute(g, priorMute);
				}
			}
		}
		
		// draw icons
		// the equalsIgnoreCaseOrNull and order by clause weirdness is to help speed up drawing svg operator icons
		// generating the svg images takes a while, so use each one as many times as possible before generating the next one
		strSQL = "select\n";
		strSQL += "EPOA.id_explain_plan_property\n";
		strSQL += ", EPOA.box_left\n";
		strSQL += ", EPOA.box_right\n";
		strSQL += ", EPOA.box_top\n";
		strSQL += ", EPOA.box_bottom\n";
		strSQL += ", EPOA.node_type\n";
		strSQL += ", EPOA.join_type\n";
		strSQL += ", EPOA.operation\n";
		strSQL += ", EPOA.strategy\n";
		strSQL += ", EPOA.command\n";
		strSQL += "from explain_plan_operator_annotations as EPOA\n";
		strSQL += "where EPOA.id_explain_plan = " + idExplainPlan + "\n";
		strSQL += "order by EPOA.node_type, EPOA.join_type, EPOA.operation, EPOA.strategy, EPOA.command\n";

		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			boolean previousIconTypeIdentical;
			String previous_node_type = null;
			String previous_join_type = null;
			String previous_operation = null;
			String previous_strategy = null;
			String previous_command = null;
			BufferedImage objImage = null;
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				int id_explain_plan_property = jsnRow.getInteger("id_explain_plan_property");
				int box_left = jsnRow.getInteger("box_left");
				int box_right = jsnRow.getInteger("box_right");
				int box_top = jsnRow.getInteger("box_top");
				int box_bottom = jsnRow.getInteger("box_bottom");
				String node_type = jsnRow.getString("node_type");
				String join_type = jsnRow.getString("join_type");
				String operation = jsnRow.getString("operation");
				String strategy = jsnRow.getString("strategy");
				String command = jsnRow.getString("command");
				if (equalsIgnoreCaseOrNull(node_type, previous_node_type) == true
						&& equalsIgnoreCaseOrNull(join_type, previous_join_type) == true
						&& equalsIgnoreCaseOrNull(operation, previous_operation) == true
						&& equalsIgnoreCaseOrNull(strategy, previous_strategy) == true
						&& equalsIgnoreCaseOrNull(command, previous_command) == true
						) {
					previousIconTypeIdentical = true;
				} else {
					previousIconTypeIdentical = false;
				}
				Composite priorMute = beginNeverExecutedMute(g, isNeverExecutedOperator(id_explain_plan_property));
				try {
					objImage = drawOperatorIcon(g, box_left, box_right, box_top, box_bottom, node_type, join_type
							, operation, strategy, command, previousIconTypeIdentical, objImage);
				} finally {
					endNeverExecutedMute(g, priorMute);
				}
				previous_node_type = node_type; 
				previous_join_type = join_type;
				previous_operation = operation;
				previous_strategy = strategy;
				previous_command = command;
			}
		}

		// worker toggles after icons so the caret control is not obscured
		strSQL = "select\n" +
				"EPOA.id_explain_plan_property\n" +
				", EPOA.box_right\n" +
				", EPOA.box_top\n" +
				", case when count(EPOSW.id_explain_plan_property) = 0 then 0 else 1 end as workers_expanded\n" +
				"from explain_plan_operator_annotations as EPOA\n" +
				"inner join explain_plan_operator_workers as EPOW on EPOA.id_explain_plan = EPOW.id_explain_plan\n" +
				"  and EPOA.id_explain_plan_property = EPOW.id_explain_plan_property\n" +
				"left outer join explain_plan_operator_show_workers as EPOSW on EPOA.id_explain_plan = EPOSW.id_explain_plan\n" +
				"  and EPOA.id_explain_plan_property = EPOSW.id_explain_plan_property\n" +
				"where EPOA.id_explain_plan = " + idExplainPlan + "\n" +
				"group by EPOA.id_explain_plan_property\n" +
				";";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				drawWorkerToggleControl(g, jsnRow.getInteger("id_explain_plan_property"), jsnRow.getInteger("box_right"), jsnRow.getInteger("box_top"), jsnRow.getInteger("workers_expanded") == 1);
			}
		}

		// draw north and south text
		// Batik SVG / prior drawing can leave a non-Color Paint or odd Composite on
		// Linux; reset so Find substring backgrounds actually show.
		resetDiagramTextGraphicsState(g);
		strSQL = "select\n";
		strSQL += "id_explain_plan_property\n";
		strSQL += ", box_left + (box_right - box_left) / 2 as x_center\n";
		strSQL += ", box_top as north_y_baseline\n";
		strSQL += ", north_1_text\n";
		strSQL += ", north_1_fontmetrics_width\n";
		strSQL += ", north_1_fontmetrics_height\n";
		strSQL += ", north_1_highlight_1\n";
		strSQL += ", north_1_highlight_2\n";
		strSQL += ", (select outside_box_top_margin from config) as outside_box_top_margin\n";
		strSQL += ", box_bottom as south_y_baseline\n";
		strSQL += ", south_1_text\n";
		strSQL += ", south_1_fontmetrics_width\n";
		strSQL += ", south_1_fontmetrics_height\n";
		strSQL += ", south_2_text\n";
		strSQL += ", south_2_fontmetrics_width\n";
		strSQL += ", south_2_fontmetrics_height\n";
		strSQL += ", south_3_text\n";
		strSQL += ", south_3_fontmetrics_width\n";
		strSQL += ", south_3_fontmetrics_height\n";
		strSQL += ", south_4_text\n";
		strSQL += ", south_4_fontmetrics_width\n";
		strSQL += ", south_4_fontmetrics_height\n";
		strSQL += ", south_5_text\n";
		strSQL += ", south_5_fontmetrics_width\n";
		strSQL += ", south_5_fontmetrics_height\n";
		strSQL += "from explain_plan_operator_annotations\n";
		strSQL += "where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				int id_explain_plan_property = jsnRow.getInteger("id_explain_plan_property");
				int x_center = jsnRow.getInteger("x_center");
				int north_y_baseline = jsnRow.getInteger("north_y_baseline");
				String north_1_text = jsnRow.getString("north_1_text");
				int north_1_fontmetrics_width = jsnRow.getInteger("north_1_fontmetrics_width");
				int north_1_fontmetrics_height = jsnRow.getInteger("north_1_fontmetrics_height");
				boolean north_1_highlight_1 = (jsnRow.getInteger("north_1_highlight_1") == 1);
				boolean north_1_highlight_2 = (jsnRow.getInteger("north_1_highlight_2") == 1);
				int outside_box_top_margin = jsnRow.getInteger("outside_box_top_margin");
				int south_y_baseline = jsnRow.getInteger("south_y_baseline");
				String south_1_text = jsnRow.getString("south_1_text");
				int south_1_fontmetrics_width = jsnRow.getInteger("south_1_fontmetrics_width");
				int south_1_fontmetrics_height = jsnRow.getInteger("south_1_fontmetrics_height");
				String south_2_text = jsnRow.getString("south_2_text");
				int south_2_fontmetrics_width = jsnRow.getInteger("south_2_fontmetrics_width");
				int south_2_fontmetrics_height = jsnRow.getInteger("south_2_fontmetrics_height");
				String south_3_text = jsnRow.getString("south_3_text");
				int south_3_fontmetrics_width = jsnRow.getInteger("south_3_fontmetrics_width");
				int south_3_fontmetrics_height = jsnRow.getInteger("south_3_fontmetrics_height");
				String south_4_text = jsnRow.getString("south_4_text");
				int south_4_fontmetrics_width = jsnRow.getInteger("south_4_fontmetrics_width");
				int south_4_fontmetrics_height = jsnRow.getInteger("south_4_fontmetrics_height");
				String south_5_text = jsnRow.getString("south_5_text");
				int south_5_fontmetrics_width = jsnRow.getInteger("south_5_fontmetrics_width");
				int south_5_fontmetrics_height = jsnRow.getInteger("south_5_fontmetrics_height");
				boolean neverExecuted = isNeverExecutedOperator(id_explain_plan_property);
				Composite priorMute = beginNeverExecutedMute(g, neverExecuted);
				try {
					drawNorthText(g, x_center, north_y_baseline, north_1_text, north_1_fontmetrics_width
							, north_1_fontmetrics_height, outside_box_top_margin
							, neverExecuted ? false : north_1_highlight_1
							, neverExecuted ? false : north_1_highlight_2
							);
					drawSouthText(g, id_explain_plan_property, x_center, south_y_baseline
							, south_1_text, south_1_fontmetrics_width, south_1_fontmetrics_height
							, south_2_text, south_2_fontmetrics_width, south_2_fontmetrics_height
							, south_3_text, south_3_fontmetrics_width, south_3_fontmetrics_height
							, south_4_text, south_4_fontmetrics_width, south_4_fontmetrics_height
							, south_5_text, south_5_fontmetrics_width, south_5_fontmetrics_height
					);
				} finally {
					endNeverExecutedMute(g, priorMute);
				}
			}
		}

		// retrieve worker text
		// retrieve parallel leader text
		// and worker text
		strSQL = "\n" + 
				"with cteEPWorkerInfo as (\n" + 
				"  select \n" + 
				"  id_explain_plan\n" + 
				"  , show_worker_info\n" + 
				"  , show_worker_actual_data_size\n" + 
				"  , show_worker_actual_rows\n" + 
				"  , show_worker_difference_time\n" +
				"  , show_worker_disk\n" + 
				"  , show_worker_disk_plus_ram\n" +
				"  , show_worker_exclusive_time\n" +
				"  , show_worker_ram\n" + 
				"  from explain_plan_generated_values\n" + 
				")\n" + 
				", cteParallelLeaders as (\n" + 
				"  select\n" + 
				"    EPOA.id_explain_plan\n" + 
				"    , EPOA.id_explain_plan_property\n" + 
				"    , CTEW.show_worker_info\n" +
				"    , EPOA.box_top\n" + 
				"    , EPOA.box_left\n" + 
				"    , case when CTEW.show_worker_info = 1 then EPOA.parallel_leader_title_label_string end as parallel_leader_title_label_string\n" + 
				"    , case when CTEW.show_worker_actual_data_size = 1 then EPOA.parallel_leader_actual_data_size_label_string end as parallel_leader_actual_data_size_label_string\n" + 
				"    , case when CTEW.show_worker_actual_rows = 1 then EPOA.parallel_leader_actual_rows_label_string end as parallel_leader_actual_rows_label_string\n" + 
				"    , case when CTEW.show_worker_difference_time = 1 then EPOA.parallel_leader_difference_time_label_string end as parallel_leader_difference_time_label_string\n" + 
				//"    , case when CTEW.show_worker_disk = 1 then EPOA.parallel_leader_disk_label_string end as parallel_leader_disk_label_string\n" + 
				//"    , case when CTEW.show_worker_disk_plus_ram = 1 then EPOA.parallel_leader_disk_plus_ram_label_string end as parallel_leader_disk_plus_ram_label_string\n" + 
				"    , case when CTEW.show_worker_exclusive_time = 1 then EPOA.parallel_leader_exclusive_time_label_string end as parallel_leader_exclusive_time_label_string\n" + 
				//"    , case when CTEW.show_worker_ram = 1 then EPOA.parallel_leader_ram_label_string end as parallel_leader_ram_label_string\n" + 
				"    , EPOA.parallel_leader_actual_data_size_label_fontmetrics_height\n" + 
				"    , EPOA.parallel_leader_actual_data_size_label_fontmetrics_width\n" + 
				"    , EPOA.parallel_leader_actual_rows_label_fontmetrics_height\n" + 
				"    , EPOA.parallel_leader_actual_rows_label_fontmetrics_width\n" + 
				"    , EPOA.parallel_leader_difference_time_label_fontmetrics_height\n" + 
				"    , EPOA.parallel_leader_difference_time_label_fontmetrics_width\n" + 
				//"    , EPOA.parallel_leader_disk_label_fontmetrics_height\n" + 
				//"    , EPOA.parallel_leader_disk_label_fontmetrics_width\n" + 
				//"    , EPOA.parallel_leader_disk_plus_ram_label_fontmetrics_height\n" + 
				//"    , EPOA.parallel_leader_disk_plus_ram_label_fontmetrics_width\n" + 
				"    , EPOA.parallel_leader_exclusive_time_label_fontmetrics_height\n" + 
				"    , EPOA.parallel_leader_exclusive_time_label_fontmetrics_width\n" + 
				//"    , EPOA.parallel_leader_ram_label_fontmetrics_height\n" + 
				//"    , EPOA.parallel_leader_ram_label_fontmetrics_width\n" + 
				"    , EPOA.parallel_leader_title_label_fontmetrics_height\n" + 
				"    , EPOA.parallel_leader_title_label_fontmetrics_width\n" + 
				"    , EPOA.parallel_leader_difference_time_highlight\n" + 
				"    , EPOA.parallel_leader_actual_data_size_highlight\n" + 
				"    , EPOA.parallel_leader_actual_rows_highlight\n" + 
				//"    , EPOA.parallel_leader_disk_highlight\n" + 
				//"    , EPOA.parallel_leader_disk_plus_ram_highlight\n" + 
				"    , EPOA.parallel_leader_exclusive_time_highlight\n" + 
				//"    , EPOA.parallel_leader_ram_highlight\n" + 
				// retrieve margins
				"    , (select inside_box_left_margin from config) as inside_box_left_margin\n" +
				"    , (select inside_box_top_margin from config) as inside_box_top_margin\n" +
				"  from explain_plan_operator_annotations as EPOA\n" + 
				"  inner join cteEPWorkerInfo as CTEW on EPOA.id_explain_plan = CTEW.id_explain_plan\n" + 
				"  inner join explain_plan_operator_show_workers as EPOSW on EPOA.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"    and EPOA.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				")\n" + 
				", cteWorkers as (\n" + 
				"  select\n" + 
				"    EPOA.id_explain_plan\n" + 
				"    , EPOA.id_explain_plan_property\n" + 
				"    --, EPOW.worker_number\n" + 
				"    , 0 as row_number\n" + 
				"    , CTEW.show_worker_info\n" +
				"    , EPOA.box_top\n" + 
				"    , EPOA.box_left\n" + 
				"    , case when CTEW.show_worker_info = 1 then EPOW.worker_title_label_string end as worker_title_label_string\n" + 
				"    , case when CTEW.show_worker_actual_data_size = 1 then EPOW.actual_data_size_label_string end as actual_data_size_label_string\n" + 
				"    , case when CTEW.show_worker_actual_rows = 1 then EPOW.actual_rows_label_string end as actual_rows_label_string\n" + 
				"    , case when CTEW.show_worker_difference_time = 1 then EPOW.difference_time_label_string end as difference_time_label_string\n" + 
				//"    , case when CTEW.show_worker_disk = 1 then EPOW.disk_label_string end as disk_label_string\n" + 
				//"    , case when CTEW.show_worker_disk_plus_ram = 1 then EPOW.disk_plus_ram_label_string end as disk_plus_ram_label_string\n" + 
				"    , case when CTEW.show_worker_exclusive_time = 1 then EPOW.exclusive_time_label_string end as exclusive_time_label_string\n" + 
				//"    , case when CTEW.show_worker_ram = 1 then EPOW.ram_label_string end as ram_label_string\n" + 
				"    , EPOW.actual_data_size_label_fontmetrics_height\n" + 
				"    , EPOW.actual_data_size_label_fontmetrics_width\n" + 
				"    , EPOW.actual_rows_label_fontmetrics_height\n" + 
				"    , EPOW.actual_rows_label_fontmetrics_width\n" + 
				"    , EPOW.difference_time_label_fontmetrics_height\n" + 
				"    , EPOW.difference_time_label_fontmetrics_width\n" + 
				//"    , EPOW.disk_label_fontmetrics_height\n" + 
				//"    , EPOW.disk_label_fontmetrics_width\n" + 
				//"    , EPOW.disk_plus_ram_label_fontmetrics_height\n" + 
				//"    , EPOW.disk_plus_ram_label_fontmetrics_width\n" + 
				"    , EPOW.exclusive_time_label_fontmetrics_height\n" + 
				"    , EPOW.exclusive_time_label_fontmetrics_width\n" + 
				//"    , EPOW.ram_label_fontmetrics_height\n" + 
				//"    , EPOW.ram_label_fontmetrics_width\n" + 
				"    , EPOW.worker_title_label_fontmetrics_height\n" + 
				"    , EPOW.worker_title_label_fontmetrics_width\n" + 
				"    , EPOW.actual_data_size_highlight_1\n" + 
				"    , EPOW.actual_rows_highlight_1\n" + 
				"    , EPOW.difference_time_highlight_1\n" + 
				//"    , EPOW.disk_highlight_1\n" + 
				//"    , EPOW.disk_plus_ram_highlight_1\n" + 
				"    , EPOW.exclusive_time_highlight_1\n" + 
				//"    , EPOW.ram_highlight_1\n" + 
				// retrieve margins
				"    , (select inside_box_left_margin from config) as inside_box_left_margin\n" +
				"    , (select inside_box_top_margin from config) as inside_box_top_margin\n" +
				"  from explain_plan_operator_workers as EPOW\n" + 
				"  inner join explain_plan_operator_annotations as EPOA on EPOW.id_explain_plan = EPOA.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOA.id_explain_plan_property\n" + 
				"  inner join explain_plan_operator_show_workers as EPOSW on EPOW.id_explain_plan = EPOSW.id_explain_plan\n" + 
				"    and EPOW.id_explain_plan_property = EPOSW.id_explain_plan_property\n" + 
				"  inner join cteEPWorkerInfo as CTEW on EPOW.id_explain_plan = CTEW.id_explain_plan\n" + 
				"    and CTEW.show_worker_info = 1\n" + 
				")\n" + 
				"select\n" + 
				"id_explain_plan\n" + 
				", id_explain_plan_property\n" + 
				"--, worker_number + 1 as worker_number\n" + 
				", row_number() over (partition by id_explain_plan_property) as worker_row_number\n" + 
				", show_worker_info\n" +
				", box_top\n" + 
				", box_left\n" + 
				", worker_title_label_string\n" + 
				", actual_data_size_label_string\n" + 
				", actual_rows_label_string\n" + 
				", difference_time_label_string\n" + 
				//", disk_label_string\n" + 
				//", disk_plus_ram_label_string\n" + 
				", exclusive_time_label_string\n" + 
				//", ram_label_string\n" + 
				", actual_data_size_label_fontmetrics_height\n" + 
				", actual_data_size_label_fontmetrics_width\n" + 
				", actual_rows_label_fontmetrics_height\n" + 
				", actual_rows_label_fontmetrics_width\n" + 
				", difference_time_label_fontmetrics_height\n" + 
				", difference_time_label_fontmetrics_width\n" + 
				//", disk_label_fontmetrics_height\n" + 
				//", disk_label_fontmetrics_width\n" + 
				//", disk_plus_ram_label_fontmetrics_height\n" + 
				//", disk_plus_ram_label_fontmetrics_width\n" + 
				", exclusive_time_label_fontmetrics_height\n" + 
				", exclusive_time_label_fontmetrics_width\n" + 
				//", ram_label_fontmetrics_height\n" + 
				//", ram_label_fontmetrics_width\n" + 
				", worker_title_label_fontmetrics_height\n" + 
				", worker_title_label_fontmetrics_width\n" + 
				", actual_data_size_highlight_1\n" + 
				", actual_rows_highlight_1\n" + 
				", difference_time_highlight_1\n" + 
				//", disk_highlight_1\n" + 
				//", disk_plus_ram_highlight_1\n" + 
				", exclusive_time_highlight_1\n" + 
				//", ram_highlight_1\n" + 
				", inside_box_left_margin\n" + 
				", inside_box_top_margin\n" + 
				"from cteWorkers\n" + 
				"where id_explain_plan = " + idExplainPlan + " -- id_explain_plan\n" + 
				"and show_worker_info > 0\n" +
				"UNION\n" + 
				"select\n" + 
				"id_explain_plan\n" + 
				", id_explain_plan_property\n" + 
				"--, 0 as worker_number\n" + 
				", 0\n" + 
				", show_worker_info\n" +
				", box_top\n" + 
				", box_left\n" + 
				", parallel_leader_title_label_string\n" + 
				", parallel_leader_actual_data_size_label_string\n" + 
				", parallel_leader_actual_rows_label_string\n" + 
				", parallel_leader_difference_time_label_string\n" +
				//", parallel_leader_disk_label_string\n" + 
				//", parallel_leader_disk_plus_ram_label_string\n" + 
				", parallel_leader_exclusive_time_label_string\n" + 
				//", parallel_leader_ram_label_string\n" + 
				", parallel_leader_actual_data_size_label_fontmetrics_height\n" + 
				", parallel_leader_actual_data_size_label_fontmetrics_width\n" + 
				", parallel_leader_actual_rows_label_fontmetrics_height\n" + 
				", parallel_leader_actual_rows_label_fontmetrics_width\n" + 
				", parallel_leader_difference_time_label_fontmetrics_height\n" + 
				", parallel_leader_difference_time_label_fontmetrics_width\n" + 
				//", parallel_leader_disk_label_fontmetrics_height\n" + 
				//", parallel_leader_disk_label_fontmetrics_width\n" + 
				//", parallel_leader_disk_plus_ram_label_fontmetrics_height\n" + 
				//", parallel_leader_disk_plus_ram_label_fontmetrics_width\n" + 
				", parallel_leader_exclusive_time_label_fontmetrics_height\n" + 
				", parallel_leader_exclusive_time_label_fontmetrics_width\n" + 
				//", parallel_leader_ram_label_fontmetrics_height\n" + 
				//", parallel_leader_ram_label_fontmetrics_width\n" + 
				", parallel_leader_title_label_fontmetrics_height\n" + 
				", parallel_leader_title_label_fontmetrics_width\n" + 
				", parallel_leader_actual_data_size_highlight\n" + 
				", parallel_leader_actual_rows_highlight\n" + 
				", parallel_leader_difference_time_highlight\n" + 
				//", parallel_leader_disk_highlight\n" + 
				//", parallel_leader_disk_plus_ram_highlight\n" + 
				", parallel_leader_exclusive_time_highlight\n" + 
				//", parallel_leader_ram_highlight\n" + 
				", inside_box_left_margin\n" + 
				", inside_box_top_margin\n" + 
				"from cteParallelLeaders\n" + 
				"where id_explain_plan = " + idExplainPlan + " -- id_explain_plan\n" +
				"and show_worker_info > 0\n" +
				"order by 1, 2, 3\n" + 
				";\n" + 
				"";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		//System.out.println(strSQL); // show no matter what
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				int id_explain_plan_property = jsnRow.getInteger("id_explain_plan_property");
				if (bolShowProperties == true) { System.out.println("id_explain_plan_property: " + id_explain_plan_property); }
				int box_left = jsnRow.getInteger("box_left");
				int box_top = jsnRow.getInteger("box_top");
				//int worker_number = jsnRow.getInteger("worker_number");
				int worker_row_number = jsnRow.getInteger("worker_row_number");
				//int worker_number = intRow;
				String actual_data_size_label_string = jsnRow.getString("actual_data_size_label_string");
				String actual_rows_label_string = jsnRow.getString("actual_rows_label_string");
				String difference_time_label_string = jsnRow.getString("difference_time_label_string");
				//String exclusive_time_label_string = jsnRow.getString("exclusive_time_label_string");
				String exclusive_time_label_string = null;

				String worker_title_label_string = jsnRow.getString("worker_title_label_string");
				int actual_data_size_label_fontmetrics_height = jsnRow.getInteger("actual_data_size_label_fontmetrics_height");
				int actual_data_size_label_fontmetrics_width = jsnRow.getInteger("actual_data_size_label_fontmetrics_width");
				int actual_rows_label_fontmetrics_height = jsnRow.getInteger("actual_rows_label_fontmetrics_height");
				int actual_rows_label_fontmetrics_width = jsnRow.getInteger("actual_rows_label_fontmetrics_width");
				int difference_time_label_fontmetrics_height = jsnRow.getInteger("difference_time_label_fontmetrics_height");
				int difference_time_label_fontmetrics_width = jsnRow.getInteger("difference_time_label_fontmetrics_width");
				int exclusive_time_label_fontmetrics_height = jsnRow.getInteger("exclusive_time_label_fontmetrics_height");
				int exclusive_time_label_fontmetrics_width = jsnRow.getInteger("exclusive_time_label_fontmetrics_width");

				int worker_title_label_fontmetrics_height = jsnRow.getInteger("worker_title_label_fontmetrics_height");
				int worker_title_label_fontmetrics_width = jsnRow.getInteger("worker_title_label_fontmetrics_width");
				boolean actual_data_size_highlight_1 = (jsnRow.getInteger("actual_data_size_highlight_1") == 1);
				boolean actual_rows_highlight_1 = (jsnRow.getInteger("actual_rows_highlight_1") == 1);
				boolean difference_time_highlight_1 = (jsnRow.getInteger("difference_time_highlight_1") == 1);
				boolean exclusive_time_highlight_1 = (jsnRow.getInteger("exclusive_time_highlight_1") == 1);

				int inside_box_left_margin = jsnRow.getInteger("inside_box_left_margin");
				int inside_box_top_margin = jsnRow.getInteger("inside_box_top_margin");
				drawWorkerText(g, id_explain_plan_property, box_left, box_top, worker_row_number
						, inside_box_left_margin, inside_box_top_margin
						, worker_title_label_string, worker_title_label_fontmetrics_height, worker_title_label_fontmetrics_width
						, actual_data_size_label_string, actual_data_size_label_fontmetrics_height, actual_data_size_label_fontmetrics_width, actual_data_size_highlight_1
						, actual_rows_label_string, actual_rows_label_fontmetrics_height, actual_rows_label_fontmetrics_width, actual_rows_highlight_1
						, difference_time_label_string, difference_time_label_fontmetrics_height, difference_time_label_fontmetrics_width, difference_time_highlight_1
						, exclusive_time_label_string, exclusive_time_label_fontmetrics_height, exclusive_time_label_fontmetrics_width, exclusive_time_highlight_1
				);
			}
		}
		
		// draw arrow line and text
		// " + withZoom(OPERATOR_BOX_HEIGHT) + "
		strSQL = "select\n" + 
				"EPOA.id_explain_plan_property\n" + 
				", EPOA.box_left\n" + 
				", EPOA.box_top\n" + 
				", EPOA.box_bottom\n" + 
				", EPOA.arrow_line_thickness\n" + 
				", EPOA.arrow_text\n" + 
				", EPOA.arrow_text_fontmetrics_width\n" + 
				", EPOA.arrow_text_fontmetrics_height\n" +
				", EPOA.total_actual_rows_is_exact\n" +
				", EPOA.total_actual_mass_is_exact\n" +
				", EPOA.column_boundary_left\n" + 
				", EPOA.column_boundary_right\n" +
				", EPOA.plan_width\n" +
				", PREVIOUS_EPOA.box_right as previous_box_right\n" + 
				", PREVIOUS_EPOA.box_top as previous_box_top\n" + 
				", PREVIOUS_EPOA.column_boundary_left as previous_column_boundary_left\n" + 
				", PREVIOUS_EPOA.column_boundary_right as previous_column_boundary_right\n" + 
				", (select arrow_text_bottom_margin from config) as arrow_text_bottom_margin\n" + 
				"from explain_plan_operator_annotations as EPOA\n" + 
				"inner join explain_plan_operator_annotations as PREVIOUS_EPOA on EPOA.id_explain_plan = PREVIOUS_EPOA.id_explain_plan\n" +
				"and EPOA.parent_id_explain_plan_property = PREVIOUS_EPOA.id_explain_plan_property\n" + 
				"where EPOA.id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				int id_explain_plan_property = jsnRow.getInteger("id_explain_plan_property");
				int box_left = jsnRow.getInteger("box_left");
				int box_top = jsnRow.getInteger("box_top");
				int box_bottom = jsnRow.getInteger("box_bottom");
				int arrow_line_thickness = jsnRow.getInteger("arrow_line_thickness");
				String arrow_text = jsnRow.getString("arrow_text");
				int arrow_text_fontmetrics_width = jsnRow.getInteger("arrow_text_fontmetrics_width");
				boolean total_actual_rows_is_exact;
				boolean plan_width_is_zero;
				if (jsnRow.get("total_actual_rows_is_exact") == null)
					total_actual_rows_is_exact = false;
				else
					total_actual_rows_is_exact = (jsnRow.getInteger("total_actual_rows_is_exact") != 0); // null -> true, 1 -> true, 0 -> false
				boolean total_actual_mass_is_exact;
				if (jsnRow.get("total_actual_mass_is_exact") == null)
					total_actual_mass_is_exact = false;
				else
					total_actual_mass_is_exact = (jsnRow.getInteger("total_actual_mass_is_exact") != 0); // null -> true, 1 -> true, 0 -> false
				if (jsnRow.get("plan_width") == null)
					plan_width_is_zero = false;
				else
					plan_width_is_zero = (jsnRow.getInteger("plan_width") == 0); // null -> true, 0 -> true, 1 -> false
				int arrow_text_fontmetrics_height = jsnRow.getInteger("arrow_text_fontmetrics_height");
				int arrow_text_bottom_margin = jsnRow.getInteger("arrow_text_bottom_margin");
				int column_boundary_left = jsnRow.getInteger("column_boundary_left");
				int column_boundary_right = jsnRow.getInteger("column_boundary_right");
				int previous_column_boundary_right = jsnRow.getInteger("previous_column_boundary_right");
				int previous_box_right = jsnRow.getInteger("previous_box_right");
				int previous_box_top = jsnRow.getInteger("previous_box_top");

				boolean neverExecuted = isNeverExecutedOperator(id_explain_plan_property);
				if (neverExecuted) {
					arrow_line_thickness = Math.min(arrow_line_thickness, Math.max(1, withZoom(2)));
					arrow_text = "Never executed";
				}
				// Gradients leave a Paint that can suppress later Color fills on Linux.
				resetDiagramTextGraphicsState(g);
				Composite priorMute = beginNeverExecutedMute(g, neverExecuted);
				try {
					drawArrowAndText(g, id_explain_plan_property, arrow_line_thickness, column_boundary_left
							, column_boundary_right, box_top, box_bottom, box_left
							, arrow_text, arrow_text_fontmetrics_width, arrow_text_fontmetrics_height, arrow_text_bottom_margin, total_actual_rows_is_exact, total_actual_mass_is_exact, plan_width_is_zero
							, previous_column_boundary_right, previous_box_top, previous_box_right
					);
				} finally {
					endNeverExecutedMute(g, priorMute);
				}

			}
		}

		registerOperatorHitRegions();
		drawSelectedOperatorOutline(g);
	}

	/** Replace icon-only hit targets with full north/south/arrow content regions. */
	private void registerOperatorHitRegions() {
		if (idExplainPlan == null) {
			return;
		}
		propertyVsShapesMap = new HashMap<>();
		String strSQL = operatorContentBoundsSql(null);
		JsonArray rows = Database.rows(strSQL);
		if (rows == null) {
			return;
		}
		FontMetrics labelFm = getFontMetrics(operatorLabelFont());
		FontMetrics arrowFm = arrowTextFontMetrics();
		float arc = withZoom(8);
		int pad = selectionOutlinePad();
		for (int i = 0; i < rows.size(); i++) {
			JsonObject row = (JsonObject) rows.get(i);
			Integer id = row.getInteger("id_explain_plan_property");
			Rectangle content = computeOperatorContentBounds(row, labelFm, arrowFm);
			if (id == null || content == null) {
				continue;
			}
			Rectangle hit = new Rectangle(content.x - pad, content.y - pad,
					content.width + 2 * pad, content.height + 2 * pad);
			updateShapesMap(id, new RoundRectangle2D.Float(hit.x, hit.y, hit.width, hit.height, arc, arc));
		}
	}

	/** Outline north / operator / south / arrow (rows·mass) text for the selected operator. */
	private void drawSelectedOperatorOutline(Graphics2D g) {
		Rectangle bounds = computeSelectedOperatorContentBounds();
		if (bounds == null) {
			return;
		}
		int pad = selectionOutlinePad();
		float stroke = Math.max(2f, withZoom(2));
		// Keep stroke outside the content so labels are not covered by the border.
		float inset = stroke / 2f;
		float x = bounds.x - pad + inset;
		float y = bounds.y - pad + inset;
		float w = bounds.width + 2 * pad - stroke;
		float h = bounds.height + 2 * pad - stroke;
		float arc = withZoom(8);
		Stroke priorStroke = g.getStroke();
		Color priorColor = g.getColor();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(0x25, 0x63, 0xEB));
		g.draw(new RoundRectangle2D.Float(x, y, Math.max(1f, w), Math.max(1f, h), arc, arc));
		g.setStroke(priorStroke);
		g.setColor(priorColor);
	}

	private int selectionOutlinePad() {
		return Math.max(6, withZoom(6));
	}

	private FontMetrics arrowTextFontMetrics() {
		LineThicknesses thickness = getLineThicknessBy();
		if (thickness == LineThicknesses.ACTUAL_DATA_MASS || thickness == LineThicknesses.ESTIMATED_DATA_MASS) {
			boolean actual = thickness == LineThicknesses.ACTUAL_DATA_MASS;
			return getFontMetrics(new Font(
					actual ? ACTUAL_MASS_ARROW_FONT_NAME : ESTIMATED_MASS_ARROW_FONT_NAME,
					actual ? ACTUAL_MASS_ARROW_FONT_STYLE : ESTIMATED_MASS_ARROW_FONT_STYLE,
					withZoom(actual ? ACTUAL_MASS_ARROW_FONT_SIZE : ESTIMATED_MASS_ARROW_FONT_SIZE)));
		}
		boolean actual = thickness == LineThicknesses.ACTUAL_ROWS;
		return getFontMetrics(new Font(
				actual ? ACTUAL_ROWS_ARROW_FONT_NAME : ESTIMATED_ROWS_ARROW_FONT_NAME,
				actual ? ACTUAL_ROWS_ARROW_FONT_STYLE : ESTIMATED_ROWS_ARROW_FONT_STYLE,
				withZoom(actual ? ACTUAL_ROWS_ARROW_FONT_SIZE : ESTIMATED_ROWS_ARROW_FONT_SIZE)));
	}

	private String operatorContentBoundsSql(Integer operatorId) {
		String strSQL = "select id_explain_plan_property, box_left, box_right, box_top, box_bottom\n"
				+ ", north_1_text\n"
				+ ", south_1_text, south_2_text, south_3_text, south_4_text, south_5_text\n"
				+ ", arrow_text\n"
				+ ", coalesce(arrow_text_fontmetrics_width, 0) as arrow_text_fontmetrics_width\n"
				+ ", coalesce(arrow_text_fontmetrics_height, 0) as arrow_text_fontmetrics_height\n"
				+ ", coalesce((select arrow_text_bottom_margin from config), 0) as arrow_text_bottom_margin\n"
				+ ", coalesce((select outside_box_top_margin from config), 0) as outside_box_top_margin\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + idExplainPlan + "\n";
		if (operatorId != null) {
			strSQL += "and id_explain_plan_property = " + operatorId.intValue() + "\n";
		}
		return strSQL;
	}
	
	//LineThicknesses enmLineThicknessBy = LineThicknesses.ACTUAL_ROWS;
	public LineThicknesses getLineThicknessBy() {
		return enmLineThicknessBy;
	}
	
	public void setLineThicknessBy(LineThicknesses enmLineThicknessBy) {
		this.enmLineThicknessBy = enmLineThicknessBy;

		// plan graph needs to be repainted
		PlanSourcePanel objComponent = (PlanSourcePanel) objCurrentPlanTabs.getComponentAt(0);
		objComponent.setLineThicknessByIsDirty(true);
	}
	
	public Dimension getPreferredSize() {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) {
			System.out.println("Dimension(" + intXDimension + ", " + intYDimension + ")");
		}

		return new Dimension(getXDimension(), getYDimension());
	}
	
	public int getXDimension() {
		return intXDimension;
	}
	
	public void setXDimension(int intXDimension) {
		this.intXDimension = intXDimension;
	}
	
	public int getYDimension() {
		return intYDimension;
	}
	
	public void setYDimension(int intYDimension) {
		this.intYDimension = intYDimension;
	}
	
	public int withZoom(int intValue) {
		//double fltValueZoomed = (double) intValue * (double) (intZoomPercentage / 100.0);
		//return (int) fltValueZoomed;
		return (int) ((double) intValue * (double) (getZoomPercentage() / 100.0));
	}
	
	public int withZoomMinimumOne(int intValue) {
		double fltReturnValue = ((double) intValue * (double) (getZoomPercentage() / 100.0)); 
		return (fltReturnValue < 1) ? 1 : (int) fltReturnValue;
	}
	
	public int withZoomMinimumValue(Integer intValue, int intMinimumValue) {
		if (intValue == null) {
			intValue = 0;
		}
		if (intValue < intMinimumValue) {
			intValue = intMinimumValue;
		}
		double fltReturnValue = ((double) intValue * (double) (getZoomPercentage() / 100.0)); 
		return (fltReturnValue < 1) ? 1 : (int) fltReturnValue;
	}
	
	public Integer withZoom(Integer intObjectValue) {
		//double fltValueZoomed = (double) intValue * (double) (intZoomPercentage / 100.0);
		//return (int) fltValueZoomed;
		int intValue;
		if (intObjectValue == null) {
			return null;
		} else {
			intValue = intObjectValue;
			return (int) ((double) intValue * (double) (getZoomPercentage() / 100.0));
		}
	}

/*
	public int [] withZoom(int [] aryValues) {
		int [] aryReturnValues = new int[aryValues.length];
		for (int intIndex = 0; intIndex < aryValues.length; intIndex++) {
			aryReturnValues[intIndex] = (int) ((double) aryValues[intIndex] * (double) (getZoomPercentage() / 100.0));
		}
		return aryReturnValues;
	}

	public int [] withOffsetNoZoom(int addOffset, int [] aryValues) {
		int [] aryReturnValues = new int[aryValues.length];
		for (int intIndex = 0; intIndex < aryValues.length; intIndex++) {
			aryReturnValues[intIndex] = addOffset + aryValues[intIndex];
		}
		return aryReturnValues;
	}
*/
	// I like debugging, but this method is called often, so commenting out unused code
	// will hopefully speed it up
	private String operator_value_where_operator_name_is(JsonArray jsnPropertyRows, String strOperatorName) {
		//boolean bolShowProperties = false;
		//boolean bolShowInitPlanProperties = false;
		String strCurrentOperatorName, strCurrentOperatorValue;
		//String strFoundOperatorValue = null;
		//if (bolShowInitPlanProperties == true && strOperatorName.equalsIgnoreCase("Sort Space Type") == true) {
		//	System.out.println("Looking for: " + strOperatorName);
		//}
		for (int intProperty = 0; intProperty < jsnPropertyRows.size(); intProperty++) {
			strCurrentOperatorName = (String) (((JsonObject) jsnPropertyRows.get(intProperty)).get("operator_name"));
			strCurrentOperatorValue = (String) (((JsonObject) jsnPropertyRows.get(intProperty)).get("operator_value"));
			//strCurrentOperatorValue = strCurrentOperatorValue.replace("'", "''");
			//if (bolShowProperties == true) { System.out.println("strCurrentOperatorName: " + strCurrentOperatorName); }
			//if (bolShowInitPlanProperties == true && strCurrentOperatorName.equalsIgnoreCase("Sort Space Type") == true) { System.out.println("Found: " + strCurrentOperatorName); }
			if (strCurrentOperatorName != null) {
				if (strCurrentOperatorName.equalsIgnoreCase(strOperatorName) == true) {
					//if (bolShowInitPlanProperties == true && strCurrentOperatorName.equalsIgnoreCase("Sort Space Type") == true) {
					//	System.out.println("strCurrentOperatorName: " + strCurrentOperatorName);
					//}
					//if (bolShowInitPlanProperties == true && strCurrentOperatorName.equalsIgnoreCase("Sort Space Type") == true) {
					//	System.out.println(" / strCurrentOperatorValue: " + strCurrentOperatorValue);
					//}
					return strCurrentOperatorValue;
					//strFoundOperatorValue = strCurrentOperatorValue;
				}
			}
		}
		return null;
		//return strFoundOperatorValue;
	}
	
	public int getZoomPercentage() {
		return intZoomPercentage;
	}

	/** Visible size of the JScrollPane that contains this diagram, if any. */
	private Dimension findEnclosingViewportSize() {
		Container parent = getParent();
		while (parent != null) {
			if (parent instanceof JViewport) {
				return ((JViewport) parent).getExtentSize();
			}
			if (parent instanceof JScrollPane) {
				return ((JScrollPane) parent).getViewport().getExtentSize();
			}
			parent = parent.getParent();
		}
		return null;
	}
	
	/**
	 * Zoom percentage that fits the whole plan in the enclosing viewport
	 * (same result as the Fit button / {@code setZoomPercentage(-1)}).
	 */
	public int computeFitZoomPercentage() {
		if (idExplainPlan == null) {
			return 100;
		}
		String strSQL = "select\n"
				+ "max(row_boundary_bottom) as row_boundary_bottom\n"
				+ ", max(column_boundary_right) as column_boundary_right\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + idExplainPlan;
		JsonArray jsnRows = Database.rows(strSQL);
		int row_boundary_bottom = 0;
		int column_boundary_right = 0;
		if (jsnRows != null) {
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				JsonObject jsnRow = (JsonObject) jsnRows.get(intRow);
				if (jsnRow.get("row_boundary_bottom") != null) {
					row_boundary_bottom = jsnRow.getInteger("row_boundary_bottom");
				}
				if (jsnRow.get("column_boundary_right") != null) {
					column_boundary_right = jsnRow.getInteger("column_boundary_right");
				}
			}
		}

		int previousZoomPercentage = Math.max(this.intZoomPercentage, 1);
		double currentZoomFactor = previousZoomPercentage / 100.0;
		double planWidth100 = (column_boundary_right / currentZoomFactor)
				+ GRAPH_LEFT_MARGIN + GRAPH_RIGHT_MARGIN;
		double planHeight100 = (row_boundary_bottom / currentZoomFactor)
				+ GRAPH_TOP_MARGIN + GRAPH_BOTTOM_MARGIN;

		Dimension viewportSize = findEnclosingViewportSize();
		if (viewportSize == null || viewportSize.width <= 0 || viewportSize.height <= 0
				|| planWidth100 <= 0 || planHeight100 <= 0) {
			return 100;
		}
		double horizontalScreenToPlanRatio = viewportSize.width / planWidth100;
		double verticalScreenToPlanRatio = viewportSize.height / planHeight100;
		double screenToPlanRatio = Math.min(horizontalScreenToPlanRatio, verticalScreenToPlanRatio);
		int fit = (int) Math.floor(screenToPlanRatio * 100.0);
		if (fit < 10) {
			fit = 10;
		}
		if (fit > 400) {
			fit = 400;
		}
		return fit;
	}

	public void setZoomPercentage(int intZoomPercentage) {
		boolean bolShowProperties = false;
		int previousZoomPercentage = Math.max(this.intZoomPercentage, 1);
		
		// -1 fits the entire plan into the enclosing scroll pane viewport
		if (intZoomPercentage < 0) {
			intZoomPercentage = computeFitZoomPercentage();
		} else {
			if (intZoomPercentage < 10) {
				intZoomPercentage = 10;
			}
			if (intZoomPercentage > 400) {
				intZoomPercentage = 400;
			}
		}

		// Preferred size drives scrollbar thumbs; scale immediately so revalidate sees the new size
		// before paint recalculates exact extents from SQL.
		if (previousZoomPercentage > 0 && (intXDimension > 0 || intYDimension > 0)) {
			double scale = intZoomPercentage / (double) previousZoomPercentage;
			if (intXDimension > 0) {
				intXDimension = Math.max(1, (int) Math.round(intXDimension * scale));
			}
			if (intYDimension > 0) {
				intYDimension = Math.max(1, (int) Math.round(intYDimension * scale));
			}
		}

		this.intZoomPercentage = intZoomPercentage;

		// plan graph needs to be repainted
		PlanSourcePanel objComponent = (PlanSourcePanel) objCurrentPlanTabs.getComponentAt(0);
		if (bolShowProperties == true) { System.out.println("setZoomPercentage / objComponent.setPlanTextIsDirty(true)"); }
		//objComponent.setOperatorFontMetricsWidthAndHeightProcessed(false);
		//objComponent.setWorkerFontMetricsWidthAndHeightProcessed(false);
		//objComponent.setWorkerColumnRowWidthsHeightsProcessed(false);
		objComponent.setZoomPercentageIsDirty(true);
		//objComponent.setPlanTextIsDirty(true);
	}

	/** When true, scroll the selected operator into view after the next paint (post-zoom). */
	private boolean scrollSelectedIntoViewAfterPaint = false;

	/**
	 * After zoom changes, refresh preferred size / scrollbars. Exact extents are refined on paint;
	 * this ensures H/V thumbs move even before that.
	 */
	public void refreshScrollableSizeAfterZoom() {
		refreshScrollableSizeAfterZoom(false);
	}

	/**
	 * @param keepSelectedVisible if true (zoom + / 100%), keep the selected operator and its labels in view
	 */
	public void refreshScrollableSizeAfterZoom(boolean keepSelectedVisible) {
		scrollSelectedIntoViewAfterPaint = keepSelectedVisible && selectedOperatorId != null;
		revalidateEnclosingScrollPane();
		repaint();
	}

	private void revalidateEnclosingScrollPane() {
		revalidate();
		Container parent = getParent();
		while (parent != null) {
			parent.revalidate();
			parent.repaint();
			if (parent instanceof JScrollPane) {
				break;
			}
			parent = parent.getParent();
		}
	}

	/** Scroll so the selected operator box and north/south/arrow text stay in the viewport. */
	public void ensureSelectedOperatorVisible() {
		Rectangle bounds = computeSelectedOperatorVisibleBounds();
		if (bounds != null) {
			scrollRectToVisible(bounds);
		}
	}

	private Rectangle computeSelectedOperatorVisibleBounds() {
		Rectangle bounds = computeSelectedOperatorContentBounds();
		if (bounds == null) {
			return null;
		}
		int pad = Math.max(selectionOutlinePad(), withZoom(8));
		return new Rectangle(bounds.x - pad, bounds.y - pad, bounds.width + 2 * pad, bounds.height + 2 * pad);
	}

	/** Tight bounds covering operator box, north, south, and arrow (rows/mass) text. */
	private Rectangle computeSelectedOperatorContentBounds() {
		if (selectedOperatorId == null || idExplainPlan == null) {
			return null;
		}
		JsonObject row = Database.row(operatorContentBoundsSql(selectedOperatorId));
		if (row == null || row.get("box_left") == null) {
			return null;
		}
		return computeOperatorContentBounds(row, getFontMetrics(operatorLabelFont()), arrowTextFontMetrics());
	}

	/**
	 * Content bounds for one operator using live FontMetrics so the outline matches drawn text
	 * (stored DB metrics can lag Label.font / zoom).
	 */
	private Rectangle computeOperatorContentBounds(JsonObject row, FontMetrics labelFm, FontMetrics arrowFm) {
		if (row == null || row.get("box_left") == null) {
			return null;
		}
		int boxLeft = row.getInteger("box_left");
		int boxRight = row.getInteger("box_right");
		int boxTop = row.getInteger("box_top");
		int boxBottom = row.getInteger("box_bottom");
		int outsideTop = row.getInteger("outside_box_top_margin");
		int highlightPad = withZoom(5);

		int left = GRAPH_LEFT_MARGIN + boxLeft;
		int right = GRAPH_LEFT_MARGIN + boxRight;
		int top = GRAPH_TOP_MARGIN + boxTop;
		int bottom = GRAPH_TOP_MARGIN + boxBottom;
		int xCenter = GRAPH_LEFT_MARGIN + boxLeft + (boxRight - boxLeft) / 2;

		String northText = row.getString("north_1_text");
		if (northText != null && !northText.isEmpty()) {
			int northW = Math.max(labelFm.stringWidth(northText), 1);
			int northTop = GRAPH_TOP_MARGIN + boxTop - withZoom(outsideTop) - labelFm.getAscent();
			int northBottom = GRAPH_TOP_MARGIN + boxTop - withZoom(outsideTop) + labelFm.getDescent();
			left = Math.min(left, xCenter - northW / 2 - highlightPad);
			right = Math.max(right, xCenter + (northW + 1) / 2 + highlightPad);
			top = Math.min(top, northTop);
			bottom = Math.max(bottom, northBottom);
		}

		// Mirror drawSouthText: south_1 is pre-bumped; each present line advances by its ascent.
		String[] southTexts = {
				row.getString("south_1_text"),
				row.getString("south_2_text"),
				row.getString("south_3_text"),
				row.getString("south_4_text"),
				row.getString("south_5_text")
		};
		int yBaseline = boxBottom;
		for (int i = 0; i < southTexts.length; i++) {
			String text = southTexts[i];
			boolean present = text != null && !text.isEmpty();
			int lineH = present ? labelFm.getAscent() : 0;
			if (i == 0) {
				yBaseline += lineH; // drawSouthText bumps south_1 before drawing
			}
			if (present) {
				int southW = Math.max(labelFm.stringWidth(text), 1);
				int baselineY = GRAPH_TOP_MARGIN + yBaseline;
				left = Math.min(left, xCenter - southW / 2);
				right = Math.max(right, xCenter + (southW + 1) / 2);
				bottom = Math.max(bottom, baselineY + labelFm.getDescent());
			}
			yBaseline += lineH;
		}

		String arrowText = row.getString("arrow_text");
		if (arrowText != null && !arrowText.isEmpty()) {
			int arrowW = Math.max(arrowFm.stringWidth(arrowText),
					row.getInteger("arrow_text_fontmetrics_width"));
			int arrowBottomMargin = row.getInteger("arrow_text_bottom_margin");
			int arrowBaseline = GRAPH_TOP_MARGIN + boxTop + withZoom(OPERATOR_BOX_HEIGHT) / 2
					- arrowBottomMargin;
			left = Math.min(left, GRAPH_LEFT_MARGIN + boxLeft - arrowW);
			top = Math.min(top, arrowBaseline - arrowFm.getAscent());
			bottom = Math.max(bottom, arrowBaseline + arrowFm.getDescent());
		}

		return new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
	}
	
	public PercentageBy getPercentageBy() {
		return percentageBy;
	}
	
	public void setPercentageBy(PercentageBy percentageBy) {
		boolean bolShowProperties = false;
		this.percentageBy = percentageBy;

		// plan graph needs to be repainted
		PlanSourcePanel objComponent = (PlanSourcePanel) objCurrentPlanTabs.getComponentAt(0);
		if (bolShowProperties == true) { System.out.println("setPercentageBy / objComponent.setPlanTextIsDirty(true)"); }
		//objComponent.setDistributeToNorthSouthFieldsProcessed(false);
		//objComponent.setOperatorFontMetricsWidthAndHeightProcessed(false);
		objComponent.setPercentageByIsDirty(true);
		//objComponent.setPlanTextIsDirty(true);
	}

	/** Diagram label for a Node Type; keeps matching keys as Postgres emits them. */
	private String displayNodeTypeName(String nodeType) {
		if (nodeType == null || nodeType.isEmpty()) {
			return "";
		}
		if (nodeType.equals("Tid Range Scan")) {
			return "TID Range Scan";
		}
		if (nodeType.equals("Tid Scan")) {
			return "TID Scan";
		}
		return nodeType;
	}

	/** True when Node Type already looks like classic text EXPLAIN wording. */
	private static boolean isTextNativeNodeTypeLabel(String nodeType) {
		if (nodeType == null || nodeType.isBlank()) {
			return false;
		}
		String t = nodeType.trim();
		return t.matches("(?i)(Parallel\\s+)?Nested Loop\\s+(Inner|Left|Right|Full|Semi|Anti)\\s+Join")
				|| t.matches("(?i)(Parallel\\s+)?(Hash|Merge)\\s+(Inner|Left|Right|Full|Semi|Anti)\\s+Join")
				|| t.matches("(?i)Parallel\\s+.+")
				|| t.matches("(?i)(Hash|Group|Mixed)Aggregate")
				|| t.matches("(?i)(Partial|Finalize)\\s+(Hash|Group|Mixed)?\\s*Aggregate")
				|| t.equalsIgnoreCase("Insert")
				|| t.equalsIgnoreCase("Update")
				|| t.equalsIgnoreCase("Delete")
				|| t.equalsIgnoreCase("Merge");
	}

	/**
	 * Strip PostgreSQL cast suffixes from EXPLAIN expressions, e.g.
	 * {@code '11'::oid} → {@code '11'}, {@code 'MGI:'::text} → {@code 'MGI:'},
	 * including multi-word types like {@code ::character varying}.
	 */
	private static final Pattern PG_TYPE_CAST_PATTERN = Pattern.compile(
			"::(?:\"[^\"]+\"|[A-Za-z_][\\w]*(?:\\s+[A-Za-z_][\\w]*)*)(?:\\[\\])?");

	private String removeDatatypes(String strText) {
		if (strText == null) {
			return strText;
		}
		String previous;
		String current = strText;
		// Repeat for stacked casts: NULL::anyarray::text, ('x'::name)::text, …
		do {
			previous = current;
			current = PG_TYPE_CAST_PATTERN.matcher(current).replaceAll("");
		} while (!current.equals(previous));
		return current;
	}

	// if "and" and "or" both appear, and "=" does not appear, return string as-is
	// otherwise, remove all of them
	private String removeParenthesis(String strText) {
		if (strText == null)
			return strText;
		if (strText.toLowerCase().contains(" and ") == true && strText.toLowerCase().contains(" or ") == true)
			return strText;
		if (strText.contains("=") == false)
			return strText;
		strText = strText.replace("(", "");
		strText = strText.replace(")", "");
		
		return strText;
	}

	private String prefaceAndTruncate(String strPreface, String strAppend, int intMaxCharacters) {
		String strText;
		if (strAppend != null && strAppend.length() > intMaxCharacters) {
			strAppend = strAppend.substring(0, intMaxCharacters) + " ..."; // show up to max characters
		}
		if (strAppend == null)
			strText = "";
		else
			strText = strPreface + strAppend; 
		
		return strText;
	}

	public double getPlanTimeSliderValueMs() {
		return dblPlanTimeSliderMs;
	}

	public double getPlanCostSlider() {
		return dblPlanCostSlider;
	}

	public void setPlanTimeSliderMs(double dblPlanTimeSliderMs) {
		
		this.dblPlanTimeSliderMs = dblPlanTimeSliderMs;
	}

	public void setPlanCostSlider(double dblPlanCostSliderValue) {
		this.dblPlanCostSlider = dblPlanCostSliderValue;
	}
/*
	public boolean getShowPlanSliderActualTime() {
		return bolShowPlanSliderActualTime;
	}
	
	public void setShowPlanSliderActualTime(boolean bolShowPlanSliderActualTime) {
		this.bolShowPlanSliderActualTime = bolShowPlanSliderActualTime;
	}
*/
/*
	public boolean getShowPlanCostSlider() {
		return bolShowPlanCostSlider;
	}
	
	public void setShowCostPlanSlider(boolean bolShowPlanCostSlider) {
		this.bolShowPlanCostSlider = bolShowPlanCostSlider;
	}
*/

	// returns 1: time slider -or- 2: cost slider

	public int getSliderType() {
		return this.sliderType;
	}

	public void setSliderType(int sliderType) {
		this.sliderType = sliderType;
	}

	/** EXPLAIN JSON fields that are string arrays of expressions (shown as expandable lists). */
	public static final String[] EXPRESSION_LIST_PROPERTIES = {
			"Output", "Group Key", "Sort Key", "Presorted Key"
	};

	/**
	 * Incremental Sort nested group objects (Workers-style expandable in the properties grid).
	 * Children are flattened on expand (Sort Space Memory avg/peak hoisted one level).
	 */
	public static final String[] SORT_GROUP_PROPERTIES = {
			"Full-sort Groups", "Pre-sorted Groups"
	};

	public static boolean isExpressionListProperty(String propertyName) {
		if (propertyName == null) {
			return false;
		}
		for (String name : EXPRESSION_LIST_PROPERTIES) {
			if (name.equals(propertyName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSortGroupProperty(String propertyName) {
		if (propertyName == null) {
			return false;
		}
		for (String name : SORT_GROUP_PROPERTIES) {
			if (name.equals(propertyName)) {
				return true;
			}
		}
		return false;
	}

	private static String expressionListUnit(String propertyName, boolean plural) {
		if ("Output".equals(propertyName)) {
			return plural ? "columns" : "column";
		}
		return plural ? "keys" : "key";
	}

	private static String sqlLiteral(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	/**
	 * SQL fragment: Workers-style count summary for a Full-sort / Pre-sorted Groups parent
	 * ({@code 1 group} / {@code 4 groups}), using alias {@code EPP} for the group shell.
	 */
	private static String sortGroupSummarySql() {
		return "coalesce((select GC.operator_value from explain_plan_properties as GC\n"
				+ "  where GC.id_explain_plan = EPP.id_explain_plan\n"
				+ "    and GC.parent_id_explain_plan_property = EPP.id_explain_plan_property\n"
				+ "    and GC.operator_name = 'Group Count'), '0')\n"
				+ "|| case when coalesce((select GC.operator_value from explain_plan_properties as GC\n"
				+ "  where GC.id_explain_plan = EPP.id_explain_plan\n"
				+ "    and GC.parent_id_explain_plan_property = EPP.id_explain_plan_property\n"
				+ "    and GC.operator_name = 'Group Count'), '0') = '1' then ' group' else ' groups' end";
	}

	/**
	 * Notes for the properties panel: explain scaled diagram totals such as
	 * "~591 rows removed by filter" versus the raw EXPLAIN property value.
	 */
	public String getOperatorNotes(int intIDExplainPlanProperty) {
		if (idExplainPlan == null || intIDExplainPlanProperty < 0) {
			return "";
		}
		String strSQL = "select\n"
				+ "  id_explain_plan_property\n"
				+ ", parent_id_explain_plan_property\n"
				+ ", node_type\n"
				+ ", actual_loops\n"
				+ ", workers_planned\n"
				+ ", workers_launched\n"
				+ ", ancestor_workers_planned\n"
				+ ", ancestor_workers_launched\n"
				+ ", exact_actual_worker_count\n"
				+ ", rows_removed_by_filter\n"
				+ ", total_rows_removed_by_filter\n"
				+ ", total_rows_removed_by_filter_is_exact\n"
				+ ", rows_removed_by_join_filter\n"
				+ ", total_rows_removed_by_join_filter\n"
				+ ", total_rows_removed_by_join_filter_is_exact\n"
				+ ", rows_removed_by_index_recheck\n"
				+ ", total_rows_removed_by_index_recheck\n"
				+ ", total_rows_removed_by_index_recheck_is_exact\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + idExplainPlan + "\n"
				+ "and id_explain_plan_property = " + intIDExplainPlanProperty + "\n";
		JsonObject row = Database.row(strSQL);
		if (row == null) {
			return "";
		}

		StringBuilder notes = new StringBuilder();
		appendRowsRemovedNote(notes, row, intIDExplainPlanProperty,
				"removed by filter", "Rows Removed by Filter",
				"rows_removed_by_filter", "total_rows_removed_by_filter",
				"total_rows_removed_by_filter_is_exact");
		appendRowsRemovedNote(notes, row, intIDExplainPlanProperty,
				"removed by join filter", "Rows Removed by Join Filter",
				"rows_removed_by_join_filter", "total_rows_removed_by_join_filter",
				"total_rows_removed_by_join_filter_is_exact");
		appendRowsRemovedNote(notes, row, intIDExplainPlanProperty,
				"removed by index recheck", "Rows Removed by Index Recheck",
				"rows_removed_by_index_recheck", "total_rows_removed_by_index_recheck",
				"total_rows_removed_by_index_recheck_is_exact");
		return notes.toString().trim();
	}

	private void appendRowsRemovedNote(StringBuilder notes, JsonObject row, int operatorId,
			String diagramPhrase, String explainPropertyName,
			String rawKey, String totalKey, String exactKey) {
		Integer raw = parseAnnotationInt(row.get(rawKey));
		Integer total = parseAnnotationInt(row.get(totalKey));
		if (raw == null || total == null || total <= 0) {
			return;
		}
		boolean exact = parseAnnotationInt(row.get(exactKey)) != null
				&& parseAnnotationInt(row.get(exactKey)).intValue() == 1;
		if (exact && raw.intValue() == total.intValue()) {
			return;
		}

		Integer actualLoops = parseAnnotationInt(row.get("actual_loops"));
		Integer ancestorLaunched = parseAnnotationInt(row.get("ancestor_workers_launched"));
		Integer ancestorPlanned = parseAnnotationInt(row.get("ancestor_workers_planned"));
		Integer exactWorkers = parseAnnotationInt(row.get("exact_actual_worker_count"));

		int multiplier;
		String multiplierExplain;
		JsonObject gather = findGatherAncestor(operatorId);
		Integer gatherId = gather == null ? null : parseAnnotationInt(gather.get("id_explain_plan_property"));
		Integer gatherLaunched = gather == null ? null : parseAnnotationInt(gather.get("workers_launched"));
		Integer gatherPlanned = gather == null ? null : parseAnnotationInt(gather.get("workers_planned"));
		String gatherType = gather == null ? "Gather" : nullToEmpty(gather.getString("node_type"));

		if (actualLoops != null && actualLoops.intValue() > 1
				&& raw.intValue() * actualLoops.intValue() == total.intValue()) {
			multiplier = actualLoops.intValue();
			int workers = Math.max(0, multiplier - 1);
			if (gatherId != null) {
				multiplierExplain = multiplier + " (Actual Loops = leader + " + workers
						+ " worker" + (workers == 1 ? "" : "s")
						+ " from " + gatherType + " operator " + gatherId + ")";
			} else {
				multiplierExplain = multiplier + " (Actual Loops)";
			}
		} else if (exactWorkers != null && exactWorkers.intValue() > 0
				&& raw.intValue() * (1 + exactWorkers.intValue()) == total.intValue()) {
			multiplier = 1 + exactWorkers.intValue();
			multiplierExplain = multiplier + " (1 leader + " + exactWorkers
					+ " worker" + (exactWorkers.intValue() == 1 ? "" : "s") + ")";
		} else if (ancestorLaunched != null
				&& raw.intValue() * (1 + ancestorLaunched.intValue()) == total.intValue()) {
			multiplier = 1 + ancestorLaunched.intValue();
			if (gatherId != null) {
				multiplierExplain = multiplier + " (1 leader + " + ancestorLaunched
						+ " Workers Launched on " + gatherType + " operator " + gatherId + ")";
			} else {
				multiplierExplain = multiplier + " (1 leader + " + ancestorLaunched
						+ " ancestor Workers Launched)";
			}
		} else if (ancestorPlanned != null
				&& raw.intValue() * (1 + ancestorPlanned.intValue()) == total.intValue()) {
			multiplier = 1 + ancestorPlanned.intValue();
			if (gatherId != null) {
				multiplierExplain = multiplier + " (1 leader + " + ancestorPlanned
						+ " Workers Planned on " + gatherType + " operator " + gatherId + ")";
			} else {
				multiplierExplain = multiplier + " (1 leader + " + ancestorPlanned
						+ " ancestor Workers Planned)";
			}
		} else {
			// Fallback: describe whatever total we show even if factors don't divide cleanly
			multiplier = (raw.intValue() == 0) ? 0 : Math.max(1, (int) Math.round(total.doubleValue() / raw.doubleValue()));
			StringBuilder fb = new StringBuilder();
			fb.append(multiplier).append(" (scaled for parallel workers");
			if (actualLoops != null) {
				fb.append("; Actual Loops=").append(actualLoops);
			}
			if (gatherId != null && gatherLaunched != null) {
				fb.append("; Workers Launched on ").append(gatherType).append(" operator ")
						.append(gatherId).append("=").append(gatherLaunched);
			} else if (gatherId != null && gatherPlanned != null) {
				fb.append("; Workers Planned on ").append(gatherType).append(" operator ")
						.append(gatherId).append("=").append(gatherPlanned);
			} else if (ancestorLaunched != null) {
				fb.append("; ancestor Workers Launched=").append(ancestorLaunched);
			}
			fb.append(")");
			multiplierExplain = fb.toString();
		}

		if (notes.length() > 0) {
			notes.append("\n\n");
		}
		if (!exact) {
			notes.append("~");
		}
		// Utils.units already includes a " rows" / "k rows" suffix
		notes.append(Utils.units(total.doubleValue()));
		notes.append(" ").append(diagramPhrase);
		notes.append(" ≈ ").append(raw);
		notes.append(" ").append(explainPropertyName);
		notes.append(" × ").append(multiplierExplain);
	}

	private JsonObject findGatherAncestor(int operatorId) {
		Integer currentId = operatorId;
		for (int depth = 0; depth < 64 && currentId != null && currentId.intValue() >= 0; depth++) {
			String strSQL = "select id_explain_plan_property, parent_id_explain_plan_property, node_type,\n"
					+ "workers_planned, workers_launched\n"
					+ "from explain_plan_operator_annotations\n"
					+ "where id_explain_plan = " + idExplainPlan + "\n"
					+ "and id_explain_plan_property = " + currentId + "\n";
			JsonObject node = Database.row(strSQL);
			if (node == null) {
				return null;
			}
			String nodeType = node.getString("node_type");
			if (nodeType != null
					&& (nodeType.equalsIgnoreCase("Gather") || nodeType.equalsIgnoreCase("Gather Merge"))) {
				return node;
			}
			currentId = parseAnnotationInt(node.get("parent_id_explain_plan_property"));
			if (currentId != null && currentId.intValue() == operatorId) {
				return null; // safety
			}
		}
		return null;
	}

	private static Integer parseAnnotationInt(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		String text = value.toString().trim();
		if (text.isEmpty() || text.equalsIgnoreCase("null")) {
			return null;
		}
		try {
			if (text.contains(".")) {
				return (int) Math.round(Double.parseDouble(text));
			}
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/** True when ANALYZE reported Actual Loops = 0 (operator never ran). */
	private static boolean isNeverExecutedLoops(Object actualLoopsValue) {
		Integer loops = parseAnnotationInt(actualLoopsValue);
		return loops != null && loops.intValue() == 0;
	}

	private Set<Integer> loadNeverExecutedOperatorIds(int planId) {
		Set<Integer> ids = new HashSet<>();
		JsonArray rows = Database.rows(
				"select id_explain_plan_property, actual_loops\n"
						+ "from explain_plan_operator_annotations\n"
						+ "where id_explain_plan = " + planId + ";");
		if (rows == null) {
			return ids;
		}
		for (int i = 0; i < rows.size(); i++) {
			JsonObject row = (JsonObject) rows.get(i);
			if (!isNeverExecutedLoops(row.get("actual_loops"))) {
				continue;
			}
			Integer id = row.getInteger("id_explain_plan_property");
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	private boolean isNeverExecutedOperator(int idExplainPlanProperty) {
		return neverExecutedOperatorIds != null && neverExecutedOperatorIds.contains(Integer.valueOf(idExplainPlanProperty));
	}

	private Composite beginNeverExecutedMute(Graphics2D g, boolean neverExecuted) {
		if (!neverExecuted) {
			return null;
		}
		Composite prior = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, NEVER_EXECUTED_ALPHA));
		return prior;
	}

	private void endNeverExecutedMute(Graphics2D g, Composite prior) {
		if (prior != null) {
			g.setComposite(prior);
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	/** Treat Java null and the accidental label "null" as blank diagram text. */
	private static String blankIfNullish(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null")) {
			return "";
		}
		return value;
	}

	public Object[][] getOperatorProperties(int intIDExplainPlanProperty, java.util.Set<String> expandedLists, boolean expandWorkers) {
		
		String strSQL;
		JsonArray jsnRows;
		JsonObject jsnRow;
		Object[][] aryReturn = null;
		boolean bolShowSQL = false;
		boolean bolShowProperties = false;
		int intNewRow;
		boolean isRoot = (intIDExplainPlanProperty == -1);
		String workersCaret = expandWorkers ? "▲ " : "▼ ";
		if (expandedLists == null) {
			expandedLists = java.util.Collections.emptySet();
		}
		// Root summaries (2-6) sit between Operator (1) and remaining EXPLAIN keys (10)
		int propertySortKey = isRoot ? 10 : 2;
		String operatorValueSql = isRoot ? "'User or Application'" : String.valueOf(intIDExplainPlanProperty);

		strSQL = "select 0, 1, 'Operator' as operator_name, " + operatorValueSql + " as operator_value\n";
		if (isRoot) {
			// Mirror the five south_* lines already drawn on the root canvas box
			strSQL += "UNION ALL\n" +
					"select 0, 2, 'Actual rows',\n" +
					"  case when instr(south_1_text, ': ') > 0 then substr(south_1_text, instr(south_1_text, ': ') + 2) else south_1_text end\n" +
					"from explain_plan_operator_annotations\n" +
					"where id_explain_plan = " + idExplainPlan + " and id_explain_plan_property = -1 and south_1_text is not null\n" +
					"UNION ALL\n" +
					"select 0, 3, 'Estimated rows',\n" +
					"  case when instr(south_2_text, ': ') > 0 then substr(south_2_text, instr(south_2_text, ': ') + 2) else south_2_text end\n" +
					"from explain_plan_operator_annotations\n" +
					"where id_explain_plan = " + idExplainPlan + " and id_explain_plan_property = -1 and south_2_text is not null\n" +
					"UNION ALL\n" +
					"select 0, 4, 'Actual data mass',\n" +
					"  case when instr(south_3_text, ': ') > 0 then substr(south_3_text, instr(south_3_text, ': ') + 2) else south_3_text end\n" +
					"from explain_plan_operator_annotations\n" +
					"where id_explain_plan = " + idExplainPlan + " and id_explain_plan_property = -1 and south_3_text is not null\n" +
					"UNION ALL\n" +
					"select 0, 5, 'Estimated data mass',\n" +
					"  case when instr(south_4_text, ': ') > 0 then substr(south_4_text, instr(south_4_text, ': ') + 2) else south_4_text end\n" +
					"from explain_plan_operator_annotations\n" +
					"where id_explain_plan = " + idExplainPlan + " and id_explain_plan_property = -1 and south_4_text is not null\n" +
					"UNION ALL\n" +
					"select 0, 6, 'Total execution time',\n" +
					"  case when instr(south_5_text, ': ') > 0 then substr(south_5_text, instr(south_5_text, ': ') + 2) else south_5_text end\n" +
					"from explain_plan_operator_annotations\n" +
					"where id_explain_plan = " + idExplainPlan + " and id_explain_plan_property = -1 and south_5_text is not null\n";
		}
		strSQL += "UNION ALL\n" + 
				"select 0, " + propertySortKey + ", EPP.operator_name\n" + 
				", case\n" + 
				"	when EPP.operator_name = 'Workers' and WNUM.num_workers = 1 then '" + workersCaret + "(' || cast(WNUM.num_workers as varchar(10)) || ' worker)'\n" + 
				"	when EPP.operator_name = 'Workers' then '" + workersCaret + "(' || cast(WNUM.num_workers as varchar(10)) || ' workers)'\n";
		for (String listName : EXPRESSION_LIST_PROPERTIES) {
			String caret = expandedLists.contains(listName) ? "▲ " : "▼ ";
			String singular = expressionListUnit(listName, false);
			String plural = expressionListUnit(listName, true);
			strSQL += "	when EPP.operator_name = " + sqlLiteral(listName) + " and LISTNUM.num_items = 1 then '" + caret + "(1 " + singular + ")'\n" +
					"	when EPP.operator_name = " + sqlLiteral(listName) + " and LISTNUM.num_items is not null then '" + caret + "(' || cast(LISTNUM.num_items as varchar(10)) || ' " + plural + ")'\n";
		}
		for (String groupName : SORT_GROUP_PROPERTIES) {
			String caret = expandedLists.contains(groupName) ? "▲ " : "▼ ";
			strSQL += "	when EPP.operator_name = " + sqlLiteral(groupName) + " then '" + caret + "(' || ("
					+ sortGroupSummarySql() + ") || ')'\n";
		}
		strSQL += "	else EPP.operator_value\n" + 
				"end as operator_value\n" + 
				"from explain_plan_properties as EPP\n" + 
				"left outer join (\n" + 
				"	select EPOW.id_explain_plan, EPOW.id_explain_plan_property, count(*) as num_workers\n" + 
				"	from explain_plan_properties as SUB_EPP\n" + 
				"	inner join explain_plan_operator_workers as EPOW on SUB_EPP.id_explain_plan = EPOW.id_explain_plan\n" + 
				"		and SUB_EPP.parent_id_explain_plan_property = EPOW.id_explain_plan_property\n" + 
				"	where SUB_EPP.operator_name = 'Workers'\n" + 
				"	group by EPOW.id_explain_plan, EPOW.id_explain_plan_property\n" + 
				") as WNUM\n" + 
				"on EPP.id_explain_plan = WNUM.id_explain_plan\n" + 
				"	and EPP.parent_id_explain_plan_property = WNUM.id_explain_plan_property\n" + 
				"left outer join (\n" + 
				"	select LIST_PROP.id_explain_plan, LIST_PROP.id_explain_plan_property, count(*) as num_items\n" + 
				"	from explain_plan_properties as LIST_PROP\n" + 
				"	inner join explain_plan_properties as ITEM on LIST_PROP.id_explain_plan = ITEM.id_explain_plan\n" + 
				"		and LIST_PROP.id_explain_plan_property = ITEM.parent_id_explain_plan_property\n" + 
				"	where LIST_PROP.operator_name in ('Output', 'Group Key', 'Sort Key', 'Presorted Key')\n" + 
				"	group by LIST_PROP.id_explain_plan, LIST_PROP.id_explain_plan_property\n" + 
				") as LISTNUM\n" + 
				"on EPP.id_explain_plan = LISTNUM.id_explain_plan\n" + 
				"	and EPP.id_explain_plan_property = LISTNUM.id_explain_plan_property\n" + 
				"where EPP.id_explain_plan = " + idExplainPlan + " -- id_explain_plan\n" + 
				"and (\n" + 
				"  EPP.parent_id_explain_plan_property = " + intIDExplainPlanProperty + "\n" + 
				"  or (EPP.parent_id_explain_plan_property = 0 and -1 = " + intIDExplainPlanProperty + ")\n" + 
				")\n" + 
				"and EPP.operator_name not in ('Plans', 'P_CONTAINER')\n";
		if (isRoot) {
			// Hide empty structural shells; Environment/Settings children are unioned below
			strSQL += "and EPP.operator_name not in ('Plan', 'Triggers', 'PostgreSQL Environment', 'Settings')\n" +
					"and EPP.operator_value is not null\n";
			// Flatten pg_settings / EXPLAIN Settings into the root property grid
			strSQL += "UNION ALL\n" +
					"select 0, " + propertySortKey + ", ITEM.operator_name, ITEM.operator_value\n" +
					"from explain_plan_properties as ENV\n" +
					"inner join explain_plan_properties as ITEM on ENV.id_explain_plan = ITEM.id_explain_plan\n" +
					"  and ENV.id_explain_plan_property = ITEM.parent_id_explain_plan_property\n" +
					"where ENV.id_explain_plan = " + idExplainPlan + "\n" +
					"and ENV.parent_id_explain_plan_property = 0\n" +
					"and ENV.operator_name in ('PostgreSQL Environment', 'Settings')\n" +
					"and ITEM.operator_value is not null\n";
		}
		for (String listName : EXPRESSION_LIST_PROPERTIES) {
			if (!expandedLists.contains(listName)) {
				continue;
			}
			// Children live under the list property in explain_plan_properties (same for Output / Group Key / …)
			strSQL += "UNION ALL\n" +
					"select\n" +
					"0\n" +
					", " + propertySortKey + "\n" +
					", " + sqlLiteral(listName) + " || x'01' || printf('%010d', ITEM.id_explain_plan_property)\n" +
					", ITEM.operator_name\n" +
					"from explain_plan_properties as LIST_PROP\n" +
					"inner join explain_plan_properties as ITEM on LIST_PROP.id_explain_plan = ITEM.id_explain_plan\n" +
					"  and LIST_PROP.id_explain_plan_property = ITEM.parent_id_explain_plan_property\n" +
					"where LIST_PROP.id_explain_plan = " + idExplainPlan + "\n" +
					"and LIST_PROP.parent_id_explain_plan_property = " + intIDExplainPlanProperty + "\n" +
					"and LIST_PROP.operator_name = " + sqlLiteral(listName) + "\n";
		}
		for (String groupName : SORT_GROUP_PROPERTIES) {
			if (!expandedLists.contains(groupName)) {
				continue;
			}
			// Group Count (scalar child)
			strSQL += "UNION ALL\n" +
					"select 0, " + propertySortKey + ",\n" +
					"  GRP.operator_name || x'01' || printf('%010d', GRP.id_explain_plan_property)"
					+ " || x'01' || '0001' || 'Group Count',\n" +
					"  CHILD.operator_value\n" +
					"from explain_plan_properties as GRP\n" +
					"inner join explain_plan_properties as CHILD on GRP.id_explain_plan = CHILD.id_explain_plan\n" +
					"  and GRP.id_explain_plan_property = CHILD.parent_id_explain_plan_property\n" +
					"where GRP.id_explain_plan = " + idExplainPlan + "\n" +
					"and GRP.parent_id_explain_plan_property = " + intIDExplainPlanProperty + "\n" +
					"and GRP.operator_name = " + sqlLiteral(groupName) + "\n" +
					"and CHILD.operator_name = 'Group Count'\n";
			// Sort Methods Used → one row, methods comma-joined (Workers-style name/value)
			strSQL += "UNION ALL\n" +
					"select 0, " + propertySortKey + ",\n" +
					"  GRP.operator_name || x'01' || printf('%010d', GRP.id_explain_plan_property)"
					+ " || x'01' || '0002' || 'Sort Methods Used',\n" +
					"  group_concat(METHOD.operator_name, ', ')\n" +
					"from explain_plan_properties as GRP\n" +
					"inner join explain_plan_properties as METHODS on GRP.id_explain_plan = METHODS.id_explain_plan\n" +
					"  and GRP.id_explain_plan_property = METHODS.parent_id_explain_plan_property\n" +
					"inner join explain_plan_properties as METHOD on METHODS.id_explain_plan = METHOD.id_explain_plan\n" +
					"  and METHODS.id_explain_plan_property = METHOD.parent_id_explain_plan_property\n" +
					"where GRP.id_explain_plan = " + idExplainPlan + "\n" +
					"and GRP.parent_id_explain_plan_property = " + intIDExplainPlanProperty + "\n" +
					"and GRP.operator_name = " + sqlLiteral(groupName) + "\n" +
					"and METHODS.operator_name = 'Sort Methods Used'\n" +
					"group by GRP.operator_name, GRP.id_explain_plan_property\n";
			// Flatten Sort Space Memory: Average / Peak
			strSQL += "UNION ALL\n" +
					"select 0, " + propertySortKey + ",\n" +
					"  GRP.operator_name || x'01' || printf('%010d', GRP.id_explain_plan_property)"
					+ " || x'01' || case when MEMCHILD.operator_name = 'Average Sort Space Used' then '0003' else '0004' end"
					+ " || MEMCHILD.operator_name,\n" +
					"  MEMCHILD.operator_value\n" +
					"from explain_plan_properties as GRP\n" +
					"inner join explain_plan_properties as MEM on GRP.id_explain_plan = MEM.id_explain_plan\n" +
					"  and GRP.id_explain_plan_property = MEM.parent_id_explain_plan_property\n" +
					"inner join explain_plan_properties as MEMCHILD on MEM.id_explain_plan = MEMCHILD.id_explain_plan\n" +
					"  and MEM.id_explain_plan_property = MEMCHILD.parent_id_explain_plan_property\n" +
					"where GRP.id_explain_plan = " + idExplainPlan + "\n" +
					"and GRP.parent_id_explain_plan_property = " + intIDExplainPlanProperty + "\n" +
					"and GRP.operator_name = " + sqlLiteral(groupName) + "\n" +
					"and MEM.operator_name = 'Sort Space Memory'\n" +
					"and MEMCHILD.operator_name in ('Average Sort Space Used', 'Peak Sort Space Used')\n";
		}
		if (expandWorkers) {
			// Sort under Workers, then by each worker parent (PAR.id), Worker Number first within worker
			strSQL += "UNION ALL\n" + 
				"select\n" + 
				"0\n" + 
				", 2\n" + 
				", 'Workers' || x'01' || printf('%010d', PAR.id_explain_plan_property)" +
				" || x'01' || printf('%04d', case when EPP.operator_name = 'Worker Number' then 0 else 1 end)" +
				" || EPP.operator_name\n" + 
				", EPP.operator_value\n" + 
				"from explain_plan_properties as EPP\n" + 
				"left outer join explain_plan_properties as PAR on EPP.parent_id_explain_plan_property = PAR.id_explain_plan_property\n" + 
				"  and EPP.id_explain_plan = PAR.id_explain_plan\n" + 
				"left outer join explain_plan_properties as PAR_PAR on PAR.parent_id_explain_plan_property = PAR_PAR.id_explain_plan_property\n" + 
				"  and PAR.id_explain_plan = PAR_PAR.id_explain_plan\n" + 
				"left outer join explain_plan_properties as PAR_PAR_PAR on PAR_PAR.parent_id_explain_plan_property = PAR_PAR_PAR.id_explain_plan_property \n" + 
				"  and PAR_PAR.id_explain_plan = PAR_PAR_PAR.id_explain_plan \n" + 
				"where EPP.id_explain_plan = " + idExplainPlan + " -- id_explain_plan\n" + 
				"  and PAR_PAR.parent_id_explain_plan_property = " + intIDExplainPlanProperty + " -- parent_id_explain_plan_property\n" + 
				"  and PAR_PAR.operator_name = 'Workers'\n";
		}
		strSQL += "order by 1, 2, 3\n" + 
				";";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRows = Database.rows(strSQL);
		if (jsnRows != null) {
			intNewRow = -1;
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				String operatorName = jsnRow.getString("operator_name");
				// ugly way to create a gap before each worker block
				intNewRow++;
				if (operatorName != null && operatorName.startsWith("Workers\u0001")
						&& operatorName.endsWith("Worker Number")) {
					intNewRow++;
				}
			}
			aryReturn = new String[intNewRow + 1][2];
			if (bolShowProperties == true) { System.out.println(jsnRows.size() + " rows"); } 
			intNewRow = -1;
			for (int intRow = 0; intRow < jsnRows.size(); intRow++) {
				jsnRow = (JsonObject) jsnRows.get(intRow);
				String operatorName = jsnRow.getString("operator_name");
				String operatorValue = jsnRow.getString("operator_value");
				intNewRow++;
				if (operatorName != null && operatorName.startsWith("Workers\u0001")
						&& operatorName.endsWith("Worker Number")) {
					intNewRow++;
				}
				if (operatorName != null && operatorName.indexOf('\u0001') > 0
						&& isExpressionListProperty(operatorName.substring(0, operatorName.indexOf('\u0001')))) {
					// Expanded Output / Group Key / Sort Key / Presorted Key child → Value column
					aryReturn[intNewRow][0] = null;
					aryReturn[intNewRow][1] = operatorValue;
				} else if (operatorName != null && operatorName.indexOf('\u0001') > 0
						&& isSortGroupProperty(operatorName.substring(0, operatorName.indexOf('\u0001')))) {
					// Strip: GroupName + SOH + 10-digit id + SOH + 4-digit order + field name
					int secondSoh = operatorName.indexOf('\u0001', operatorName.indexOf('\u0001') + 1);
					aryReturn[intNewRow][0] = "  " + operatorName.substring(secondSoh + 5);
					aryReturn[intNewRow][1] = operatorValue;
				} else if (operatorName != null && operatorName.startsWith("Workers\u0001")) {
					// Strip sort prefix: Workers + SOH + 10-digit parent id + SOH + 4-digit order
					int secondSoh = operatorName.indexOf('\u0001', operatorName.indexOf('\u0001') + 1);
					aryReturn[intNewRow][0] = "  " + operatorName.substring(secondSoh + 5);
					aryReturn[intNewRow][1] = operatorValue;
				} else {
					aryReturn[intNewRow][0] = operatorName;
					aryReturn[intNewRow][1] = operatorValue;
				}
			}
			//return aryReturn;
		}
		
		//if (bolShowProperties == true) { System.out.println(strReturnString); }
		return aryReturn;
		//return null;
	}

	public void setIDExplainPlan(String planName) {
		String strSQL;
		boolean bolShowSQL = false;
		strSQL = "select id_explain_plan from explain_plans where plan_name = '" + planName.replace("'", "''") + "'";
		if (bolShowSQL == true) { System.out.println(strSQL); }
		JsonObject row = Database.row(strSQL);
		idExplainPlan = row == null ? null : (Integer) row.get("id_explain_plan");
	}

	public void setIDExplainPlan(Integer planId) {
		idExplainPlan = planId;
	}

	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		// get the current position
		int currentPosition = 0;
		if (orientation == SwingConstants.HORIZONTAL) {
			currentPosition = visibleRect.x;
		} else {
			currentPosition = visibleRect.y;
		}
		
		// return the number of pixels between currentPosition
		// and the nearest tick mark in the indicated direction
		if (direction < 0) {
			int newPosition = currentPosition - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
			return (newPosition == 0) ? maxUnitIncrement : newPosition;
		} else {
			return ((currentPosition / maxUnitIncrement) + 1) * maxUnitIncrement - currentPosition;
		}
	}
	
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.HORIZONTAL) {
			return visibleRect.width - maxUnitIncrement;
		} else {
			return visibleRect.height - maxUnitIncrement;
		}
	}
	
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}
	
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	private void enableLineThicknessByAndPercentageByButtons(int idExplainPlan) {
		String strSQL;
		JsonObject jsnRow;
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;

		strSQL = "select actual_rows_enabled, estimated_rows_enabled, actual_mass_enabled, estimated_mass_enabled\n"
				+ ", actual_vs_estimated_rows_ratio_enabled, actual_vs_estimated_mass_ratio_enabled\n"
				+ ", cost_enabled, exclusive_time_enabled, time_difference_enabled\n"
				+ "from explain_plan_generated_values where id_explain_plan = " + idExplainPlan; 
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRow = Database.row(strSQL);

		boolean bolEnableActualRowsButton = Utils.objectToBoolean(jsnRow.get("actual_rows_enabled"));
		boolean bolEnableEstimatedRowsButton = Utils.objectToBoolean(jsnRow.get("estimated_rows_enabled"));
		boolean bolEnableActualMassButton = Utils.objectToBoolean(jsnRow.get("actual_mass_enabled"));
		boolean bolEnableEstimatedMassButton = Utils.objectToBoolean(jsnRow.get("estimated_mass_enabled"));
		boolean bolEnableAVsERowsButton = Utils.objectToBoolean(jsnRow.get("actual_vs_estimated_rows_ratio_enabled"));
		boolean bolEnableAVsEMassButton = Utils.objectToBoolean(jsnRow.get("actual_vs_estimated_mass_ratio_enabled"));

		boolean bolEnableCostButton = Utils.objectToBoolean(jsnRow.get("cost_enabled"));
		//boolean bolEnableDiskButton = Utils.objectToBoolean(jsnRow.get("disk_enabled"));
		//boolean bolEnableDiskPlusRamButton = Utils.objectToBoolean(jsnRow.get("disk_plus_ram_enabled"));
		//boolean bolEnableRAMButton = Utils.objectToBoolean(jsnRow.get("ram_enabled"));
		boolean bolEnableExclusiveTimeButton = Utils.objectToBoolean(jsnRow.get("exclusive_time_enabled"));
		boolean bolEnableTimeDifferenceButton = Utils.objectToBoolean(jsnRow.get("time_difference_enabled"));

		Window window = SwingUtilities.getWindowAncestor(objCurrentPlanTabs);
		Container searchRoot = window != null ? window : objCurrentPlanTabs;
		PlanDiagramToolbar planDiagramToolbar = findComponentOfType(searchRoot, PlanDiagramToolbar.class);
		if (planDiagramToolbar != null) {
			planDiagramToolbar.setCombosEnabled(true);
			planDiagramToolbar.setLineThicknessOptionEnabled(LineThicknesses.ACTUAL_DATA_MASS, bolEnableActualMassButton);
			planDiagramToolbar.setLineThicknessOptionEnabled(LineThicknesses.ACTUAL_ROWS, bolEnableActualRowsButton);
			planDiagramToolbar.setLineThicknessOptionEnabled(LineThicknesses.ACTUAL_VS_ESTIMATED_ROWS_RATIO, bolEnableAVsERowsButton);
			planDiagramToolbar.setLineThicknessOptionEnabled(LineThicknesses.ACTUAL_VS_ESTIMATED_MASS_RATIO, bolEnableAVsEMassButton);
			planDiagramToolbar.setLineThicknessOptionEnabled(LineThicknesses.ESTIMATED_DATA_MASS, bolEnableEstimatedMassButton);
			planDiagramToolbar.setLineThicknessOptionEnabled(LineThicknesses.ESTIMATED_ROWS, bolEnableEstimatedRowsButton);

			planDiagramToolbar.setPercentageByOptionEnabled(PercentageBy.COST, bolEnableCostButton);
			planDiagramToolbar.setPercentageByOptionEnabled(PercentageBy.EXCLUSIVE_TIME, bolEnableExclusiveTimeButton);
			planDiagramToolbar.setPercentageByOptionEnabled(PercentageBy.TIME_DIFFERENCE, bolEnableTimeDifferenceButton);
			planDiagramToolbar.refreshPercentageByAvailability();

			PlanSourcePanel objComponent = (PlanSourcePanel) objCurrentPlanTabs.getComponentAt(0);

			// On first diagram appear for this plan, select the first enabled Line Thickness (combo order)
			LineThicknesses firstEnabledThickness = null;
			for (LineThicknesses value : LineThicknesses.values()) {
				boolean enabled;
				switch (value) {
				case ACTUAL_DATA_MASS:
					enabled = bolEnableActualMassButton;
					break;
				case ACTUAL_ROWS:
					enabled = bolEnableActualRowsButton;
					break;
				case ACTUAL_VS_ESTIMATED_ROWS_RATIO:
					enabled = bolEnableAVsERowsButton;
					break;
				case ACTUAL_VS_ESTIMATED_MASS_RATIO:
					enabled = bolEnableAVsEMassButton;
					break;
				case ESTIMATED_DATA_MASS:
					enabled = bolEnableEstimatedMassButton;
					break;
				case ESTIMATED_ROWS:
					enabled = bolEnableEstimatedRowsButton;
					break;
				default:
					enabled = false;
					break;
				}
				if (enabled) {
					firstEnabledThickness = value;
					break;
				}
			}

			boolean bolLineThicknessByIsDirty = false;
			if (firstEnabledThickness != null) {
				if (getLineThicknessBy() != firstEnabledThickness) {
					setLineThicknessBy(firstEnabledThickness);
					planDiagramToolbar.selectAndFireLineThickness(firstEnabledThickness);
					bolLineThicknessByIsDirty = true;
				} else {
					planDiagramToolbar.setSelectedLineThickness(firstEnabledThickness);
				}
			}

			objComponent.setLineThicknessByIsDirty(bolLineThicknessByIsDirty);

			boolean bolPercentageByIsDirty = false;
			boolean anyPercentageByEnabled = bolEnableCostButton || bolEnableExclusiveTimeButton
					|| bolEnableTimeDifferenceButton;
			if (!anyPercentageByEnabled) {
				// costs off + timing off: Percentage By is "(none available)"; north stays id-only
				bolPercentageByIsDirty = true;
			} else {
				PercentageBy preferred = getPercentageBy();
				PercentageBy chosen = null;
				if (preferred == PercentageBy.COST && bolEnableCostButton) {
					chosen = PercentageBy.COST;
				} else if (preferred == PercentageBy.EXCLUSIVE_TIME && bolEnableExclusiveTimeButton) {
					chosen = PercentageBy.EXCLUSIVE_TIME;
				} else if (preferred == PercentageBy.TIME_DIFFERENCE && bolEnableTimeDifferenceButton) {
					chosen = PercentageBy.TIME_DIFFERENCE;
				} else if (bolEnableCostButton) {
					chosen = PercentageBy.COST;
				} else if (bolEnableExclusiveTimeButton) {
					chosen = PercentageBy.EXCLUSIVE_TIME;
				} else if (bolEnableTimeDifferenceButton) {
					chosen = PercentageBy.TIME_DIFFERENCE;
				}

				if (chosen != null && chosen != preferred) {
					setPercentageBy(chosen);
					planDiagramToolbar.selectAndFirePercentageBy(chosen);
					bolPercentageByIsDirty = true;
				} else {
					planDiagramToolbar.setSelectedPercentageBy(preferred);
				}
			}

			objComponent.setPercentageByIsDirty(bolPercentageByIsDirty);
		}

		// since buttons changed, update cost and time sliders
		SliderPanel sliderPanel = findSliderPanelInSelectedTab();
		if (sliderPanel != null) {
			sliderPanel.refreshComponents();
		}
	}
	
	private void updateShapesMap(Integer intIDExplainPlanProperty, Shape shape) {
		boolean bolShowFunctionName = false;
		if (bolShowFunctionName == true) { System.out.println("called updateShapesMap()"); }
		if (intIDExplainPlanProperty != null) {
			List<Shape> shapesList = propertyVsShapesMap.get(intIDExplainPlanProperty.intValue());
			if (shapesList == null)
				shapesList = new ArrayList<Shape>();
			// see if shape already exists
			for (Integer intCheckForIDExplainPlanProperty : propertyVsShapesMap.keySet()) {
				List<Shape> checkForShapeList = propertyVsShapesMap.get(intCheckForIDExplainPlanProperty);
				if (checkForShapeList == null)
					continue;
				for (Shape checkForShape : checkForShapeList) {
					if (checkForShape.equals(shape)) {
						// it already exists; exit or else we'll create a duplicate shape
						return;
					}
				}
			}
			shapesList.add(shape);
			propertyVsShapesMap.put(intIDExplainPlanProperty.intValue(), shapesList);
		}
	}
	
	private void updateWorkerToggleShapesMap(Integer intIDExplainPlanProperty, Shape shape) {
		boolean bolShowFunctionName = false;
		boolean bolShowProperties = false;
		if (bolShowFunctionName == true) { System.out.println("called updateWorkerToggleShapesMap()"); }
		if (intIDExplainPlanProperty != null) {
			List<Shape> shapesList = showWorkerToggleVsShapesMap.get(intIDExplainPlanProperty.intValue());
			if (shapesList == null)
				shapesList = new ArrayList<Shape>();
			// see if shape already exists
			for (Integer intCheckForIDExplainPlanProperty : showWorkerToggleVsShapesMap.keySet()) {
				List<Shape> checkForShapeList = showWorkerToggleVsShapesMap.get(intCheckForIDExplainPlanProperty);
				if (checkForShapeList == null)
					continue;
				for (Shape checkForShape : checkForShapeList) {
					if (checkForShape.equals(shape)) {
						// it already exists; exit or else we'll create a duplicate shape
						return;
					}
				}
			}
			shapesList.add(shape);
			if (bolShowProperties == true) { System.out.println("shape added"); }
			showWorkerToggleVsShapesMap.put(intIDExplainPlanProperty.intValue(), shapesList);
		}
	}
	
	@Override
	//public void mouseClicked(MouseEvent e) {
	public void mouseReleased(MouseEvent e) {
		boolean bolShowProperties = false;
		boolean bolShowFunctionName = false;
		boolean bolShowSQL = false;
		String strSQL;
		if (bolShowFunctionName == true) { System.out.println("called mouseReleased(MouseEvent e)"); }
		
		if (bolShowProperties == true) { System.out.println("mouse released: " + e.getSource()); }
		
        JViewport ancestorViewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (ancestorViewPort != null) {
        	ancestorViewPort.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        //viewPort.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

		if (showWorkerToggleVsShapesMap != null) {
			for (Integer intIDExplainPlanProperty : showWorkerToggleVsShapesMap.keySet()) {
				List<Shape> shapeList = showWorkerToggleVsShapesMap.get(intIDExplainPlanProperty);
				if (shapeList == null)
					continue;
				for (Shape shape : shapeList) {
					if (shape.contains(e.getX(), e.getY())) {
						if (bolShowProperties == true) { System.out.println("mouse clicked in showWorkerToggleVsShapesMap: " + intIDExplainPlanProperty.intValue()); }
						
						if (bolShowProperties == true) { System.out.println("toggle this worker"); }

						strSQL = "select count(*) as found from explain_plan_operator_show_workers where id_explain_plan = " + idExplainPlan + "\n" +
								"and id_explain_plan_property = " + intIDExplainPlanProperty + "\n";
								
						if (bolShowSQL == true) { System.out.println(strSQL); }
						boolean bolFound = (Database.row(strSQL).getInteger("found") == 1);
						if (bolFound == true) {
							strSQL = "delete from explain_plan_operator_show_workers where id_explain_plan = " + idExplainPlan + "\n" +
								"and id_explain_plan_property = " + intIDExplainPlanProperty + "\n";
						} else {
							strSQL = "insert into explain_plan_operator_show_workers (id_explain_plan, id_explain_plan_property)\n"
									+ "select " + idExplainPlan + ", " + intIDExplainPlanProperty + " where (" + idExplainPlan + ", " + intIDExplainPlanProperty + ")\n"
									+ "not in (select id_explain_plan, id_explain_plan_property from explain_plan_operator_show_workers)";
						}

						if (bolShowSQL == true) { System.out.println(strSQL); }
						Database.dml(strSQL);
						
						// repaint
						PlanSourcePanel objComponent = (PlanSourcePanel) objCurrentPlanTabs.getComponentAt(0);
						//objComponent.setPlanGraphIsSynced(false); // leads to error, probably b/c there is no planText. commenting out prevents error msg.
						//objComponent.setWorkerColumnRowWidthsHeightsProcessed(false);
						//objComponent.setPlanTextIsDirty(true); // works; hopefully setWorkerColumnRowWidthsHeightsProcessed(false) works, so commented out
						objComponent.setWorkerIsDirty(true);
						objComponent.repaint();
						
						if (bolShowProperties == true) { System.out.println("repainted"); }

						repaint();
						return;
					}
				}
			}
		}

		Integer clickedOperatorId = findOperatorIdAt(e.getX(), e.getY());
		if (clickedOperatorId != null) {
			if (bolShowProperties == true) { System.out.println("mouse clicked in propertyVsShapesMap: " + clickedOperatorId.intValue()); }
			selectedOperatorId = clickedOperatorId;
			refreshSelectedOperatorProperties();
			SliderPanel sliderPanel = findSliderPanelInSelectedTab();
			if (sliderPanel != null) {
				sliderPanel.showSelectedOperator(selectedOperatorId);
			}
			repaint();
		}

	}

	/** Reload Properties for the currently selected operator from SQLite (JSON/text plan data). */
	private void refreshSelectedOperatorProperties() {
		OperatorDetailsPanel detailsPanel = findOperatorDetailsPanel(this);
		if (selectedOperatorId == null) {
			if (detailsPanel != null) {
				detailsPanel.clearOperatorProperties();
			} else {
				OperationPropertiesPanel propertiesPanel = findOperationPropertiesPanel(this);
				if (propertiesPanel != null) {
					propertiesPanel.clearOperatorProperties();
				}
			}
			return;
		}
		if (detailsPanel != null) {
			detailsPanel.showOperatorProperties(selectedOperatorId.intValue());
			return;
		}
		OperationPropertiesPanel propertiesPanel = findOperationPropertiesPanel(this);
		if (propertiesPanel != null) {
			propertiesPanel.showOperatorProperties(selectedOperatorId.intValue());
		}
	}

	private OperatorDetailsPanel findOperatorDetailsPanel(Component start) {
		Container root = start.getParent();
		while (root != null && !(root instanceof JTabbedPane) && root.getParent() != null) {
			root = root.getParent();
		}
		if (root == null) {
			return null;
		}
		return findComponentOfType(root, OperatorDetailsPanel.class);
	}

	private OperationPropertiesPanel findOperationPropertiesPanel(Component start) {
		Container root = start.getParent();
		while (root != null && !(root instanceof JTabbedPane) && root.getParent() != null) {
			root = root.getParent();
		}
		if (root == null) {
			return null;
		}
		return findComponentOfType(root, OperationPropertiesPanel.class);
	}

	private static <T> T findComponentOfType(Container root, Class<T> type) {
		if (type.isInstance(root)) {
			return type.cast(root);
		}
		for (Component child : root.getComponents()) {
			if (type.isInstance(child)) {
				return type.cast(child);
			}
			if (child instanceof Container) {
				T found = findComponentOfType((Container) child, type);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private Point origin;
	@Override
	public void mousePressed(MouseEvent e) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("mouse pressed"); }
		origin = new Point(e.getPoint());
	}
/*
	@Override
	public void mouseReleased(MouseEvent e) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("mouse released"); }
	}
*/

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		if (origin != null) {
            JViewport ancestorViewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
            if (ancestorViewPort != null) {
                int deltaX = origin.x - e.getX();
                int deltaY = origin.y - e.getY();

                Rectangle view = ancestorViewPort.getViewRect();
                view.x += deltaX;
                view.y += deltaY;
                
                ancestorViewPort.setCursor(new Cursor(Cursor.HAND_CURSOR));
                //ancestorViewPort.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                this.scrollRectToVisible(view);
            }
        }
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Integer under = findOperatorIdAt(e.getX(), e.getY());
		if ((hoveredOperatorId == null && under == null)
				|| (hoveredOperatorId != null && hoveredOperatorId.equals(under))) {
			return;
		}
		hoveredOperatorId = under;
		repaint();
	}

	/** Prefer the smallest containing hit region when expanded label areas overlap. */
	private Integer findOperatorIdAt(int x, int y) {
		if (propertyVsShapesMap == null) {
			return null;
		}
		Integer bestId = null;
		long bestArea = Long.MAX_VALUE;
		for (Integer id : propertyVsShapesMap.keySet()) {
			List<Shape> shapeList = propertyVsShapesMap.get(id);
			if (shapeList == null) {
				continue;
			}
			for (Shape shape : shapeList) {
				if (!shape.contains(x, y)) {
					continue;
				}
				Rectangle r = shape.getBounds();
				long area = Math.max(1L, (long) r.width * (long) r.height);
				if (area < bestArea) {
					bestArea = area;
					bestId = id;
				}
			}
		}
		return bestId;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("mouse released"); }
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("mouse entered"); }
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (hoveredOperatorId != null) {
			hoveredOperatorId = null;
			repaint();
		}
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("mouse exited"); }
	}
	
	public Integer getIdExplainPlan() {
        return idExplainPlan;
    }

	public Integer getSelectedOperatorId() {
		return selectedOperatorId;
	}

	/**
	 * Select an operator as if clicked: Properties/Sliders, blue outline, scroll into view.
	 */
	public void selectOperator(Integer id) {
		selectedOperatorId = id;
		if (id == null) {
			repaint();
			return;
		}
		refreshSelectedOperatorProperties();
		SliderPanel sliderPanel = findSliderPanelInSelectedTab();
		if (sliderPanel != null) {
			sliderPanel.showSelectedOperator(selectedOperatorId);
		}
		repaint();
		SwingUtilities.invokeLater(this::ensureSelectedOperatorVisible);
	}

	/**
	 * Operators whose painted/label text contains {@code query} (case-insensitive substring).
	 * Empty query → empty list. Stable order: x_cart, y_cart, id.
	 */
	public List<Integer> findOperatorIdsMatching(String query) {
		List<Integer> ids = new ArrayList<>();
		if (idExplainPlan == null || query == null) {
			return ids;
		}
		String trimmed = query.trim();
		if (trimmed.isEmpty()) {
			return ids;
		}
		String likeLiteral = ("%" + escapeLikePattern(trimmed) + "%").replace("'", "''");
		String sql = "select id_explain_plan_property\n"
				+ "from explain_plan_operator_annotations\n"
				+ "where id_explain_plan = " + idExplainPlan + "\n"
				+ "and (\n"
				+ "  lower(coalesce(north_1_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(south_1_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(south_2_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(south_3_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(south_4_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(south_5_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(arrow_text, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(node_type, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ "  or lower(coalesce(relation_name, '')) like lower('" + likeLiteral + "') escape '\\'\n"
				+ ")\n"
				+ "order by x_cart, y_cart, id_explain_plan_property;\n";
		JsonArray rows = Database.rows(sql);
		if (rows == null) {
			return ids;
		}
		for (int i = 0; i < rows.size(); i++) {
			JsonObject row = (JsonObject) rows.get(i);
			Integer id = row.getInteger("id_explain_plan_property");
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	/** Escape {@code %}, {@code _}, and {@code \} for SQLite LIKE … ESCAPE '\'. */
	private static String escapeLikePattern(String raw) {
		StringBuilder sb = new StringBuilder(raw.length() + 8);
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '\\' || c == '%' || c == '_') {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}
}