package com.williammeitzen.peps.gui;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
import java.util.Vector;

import javax.swing.JComponent;
//import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.williammeitzen.peps.model.Database;

@SuppressWarnings("serial")
public class PlanStatisticsPanel extends JTable {
	//private Object[] columnNames = { "Plan", "Execution Time", "% Plan Cost"/*, "RAM Used"*/};
	//private Object[][] data;
	
	//private Object[] objColumnNames;
	//private Object[][] objData;
	
	static private PlanStatisticsPanel _instance = null;
	static private String planNamesDelimited = null;
	static private boolean readStatisticsDataFlag = false;
	
	private static String strPlanNamesTemplate = "select EP.plan_name, EPGV.execution_time_ms\n"
			+ ", EPGV.execution_time_ms / sum(EPGV.execution_time_ms) over () as total_execution_time_percentage\n"
			+ ", sum(EPGV.max_total_cost) as sum_max_total_cost\n"
			+ ", EPGV.max_total_cost / sum(EPGV.max_total_cost) over () as total_max_total_cost_percentage\n"
			+ ", EPGV.id_explain_plan as epgv_id_explain_plan\n"
			+ "from explain_plans as EP\n"
			+ "left outer join explain_plan_generated_values as EPGV on EP.id_explain_plan = EPGV.id_explain_plan\n"
			+ "where EP.plan_name in ({ALL_PLANS_DELIMITED})\n"
			+ "group by EP.plan_name\n"
			+ "order by lower(EP.plan_name);\n";
	
	private static String strOperatorQueryTemplate = "select\n" +
			"sum(coalesce(EPOA.total_cost, 0)) as sum_costs\n" +
			", count(*) as cnt\n" +
			", LOJ_EPOA.plan_name\n" +
			", EPOA.node_type\n" +
			", LOJ_EPOA.total_cost as lov_epoa_total_cost\n" +
			", case when sum(coalesce(EPOA.total_cost, 0)) > 0 and LOJ_EPOA.total_cost is not null then coalesce(LOJ_EPOA.total_cost, 0) / sum(coalesce(EPOA.total_cost, 0)) else null end as cost_percentage\n" +
			"from explain_plan_operator_annotations as EPOA\n" +
			"inner join explain_plans as EP on EPOA.id_explain_plan = EP.id_explain_plan\n" +
			"left outer join (\n" +
			"  select SUB_EP.plan_name, SUB_EPOA.node_type, SUB_EPOA.id_explain_plan\n" +
			"  , sum(SUB_EPOA.total_cost) as total_cost\n" +
			"  from explain_plan_operator_annotations as SUB_EPOA\n" +
			"  inner join explain_plans as SUB_EP on SUB_EPOA.id_explain_plan = SUB_EP.id_explain_plan\n" +
			"  where SUB_EP.plan_name = '{CURRENT_PLAN}'\n" +
			"  and SUB_EPOA.id_explain_plan_property <> -1\n" +
			"  group by SUB_EPOA.node_type\n" +
			") as LOJ_EPOA on\n" +
			"  EPOA.node_type = LOJ_EPOA.node_type\n" +
			"where EP.plan_name in ({ALL_PLANS_DELIMITED})\n" +
			"and EPOA.node_type is not null\n" +
			"group by EPOA.node_type\n" +
			"having sum(EPOA.exclusive_time_percentage_string) is not null\n" +
			"order by 1 desc, 2 desc\n" +
			"";

	
	public PlanStatisticsPanel() {
		super();
		createComponents();
		_instance = this;
	}
	
	static public PlanStatisticsPanel getInstance() {
		if (_instance == null)
			_instance = new PlanStatisticsPanel();
		return _instance;
	}
	
	public void createComponents() {
		boolean bolShowFunctionName = false;
		if (bolShowFunctionName == true) { System.out.println("PlanStatisticsPanel.createComponents_OLD()"); }
		if (getReadStatisticsDataFlag() == false)
			return;
		
		//test_WORKS_2();
		retrieveEnhancedStatisticsData(getPlanNamesDelimited());

	}
	
	private void retrieveEnhancedStatisticsData(String strPlanNames) {
		
	    //Vector vctRow;
		String strSQL;
		JsonArray jsnPlanNames;
		JsonArray jsnOperatorTypes;
		JsonObject jsnPlanName;
		JsonObject jsnOperatorType;
		boolean bolShowProperties = false;
		boolean bolShowFunctionName = false;
	    Vector vctColumns = new Vector();
		Vector vctRows = new Vector();
		String strColumnName;
		String strValue;

		if (bolShowFunctionName == true) { System.out.println("retrieveEnhancedStatisticsData(String strPlanNames)"); }
		if (bolShowProperties == true) { System.out.println("strPlanNames: " + strPlanNames); }

		vctColumns.addElement("Plan name");
		vctColumns.addElement("Execution (time / %)");
		vctColumns.addElement("Cost");

		// add plan names, execution times, cost
		strSQL = strPlanNamesTemplate;
		strSQL = strSQL.replace("{ALL_PLANS_DELIMITED}", strPlanNames);
		jsnPlanNames = Database.rows(strSQL);

		if (jsnPlanNames != null) {
			//for (int intPlanNameRow = 0; intPlanNameRow < jsnPlanNames.size(); intPlanNameRow++) {
			for (int intPlanNameRow = 0; jsnPlanNames.size() > 0 && intPlanNameRow == 0; intPlanNameRow++) {
				if (bolShowProperties == true) { System.out.println("jsnPlanNames.size(): " + jsnPlanNames.size()); }
				if (bolShowProperties == true) { System.out.println("intPlanNameRow: " + intPlanNameRow); }
				jsnPlanName = (JsonObject) jsnPlanNames.get(intPlanNameRow);
				
				strSQL = strOperatorQueryTemplate;
				strSQL = strSQL.replace("{ALL_PLANS_DELIMITED}", strPlanNames);
				strSQL = strSQL.replace("{CURRENT_PLAN}", jsnPlanName.getString("plan_name")); // probably don't need this replacement, since we're only getting column names
				jsnOperatorTypes = Database.rows(strSQL);
				//jsnOperatorType = Database.row(strSQL);
				
				// add operator type column names
				for (int intOperatorType = 0; intOperatorType < jsnOperatorTypes.size(); intOperatorType++) {
					jsnOperatorType = (JsonObject) jsnOperatorTypes.get(intOperatorType);
				    vctColumns.addElement(jsnOperatorType.getString("node_type"));
				}
			}

			// now go back and add values
			for (int intPlanNameRow = 0; intPlanNameRow < jsnPlanNames.size(); intPlanNameRow++) {
				jsnPlanName = (JsonObject) jsnPlanNames.get(intPlanNameRow);
			    Vector vctRow = new Vector();
				vctRow.addElement(jsnPlanName.getString("plan_name"));
				if (jsnPlanName.get("epgv_id_explain_plan") == null) { // see if we joined with epgv at all
					vctRow.addElement("(plan graph not generated yet)");
				} else if (jsnPlanName.get("execution_time_ms") == null || jsnPlanName.get("total_execution_time_percentage") == null) {
					vctRow.addElement("");
				} else {
					vctRow.addElement(Utils.elapsed_time_ms_one_decimal(jsnPlanName.getDouble("execution_time_ms"))
						+ " / " + Utils.roundValueRequirePrecision(jsnPlanName.getDouble("total_execution_time_percentage") * 100, 0) + "%");
				}
				if (jsnPlanName.get("total_max_total_cost_percentage") == null || jsnPlanName.get("sum_max_total_cost") == null) {
					vctRow.addElement("");
				} else {
					//vctRow.addElement(Utils.roundValueRequirePrecision(jsnPlanName.getDouble("total_max_total_cost_percentage") * 100, 0) + "%");
					vctRow.addElement(
						Utils.roundValueRequirePrecision(jsnPlanName.getDouble("total_max_total_cost_percentage") * 100, 0) + "%"
						+ " (" + Utils.formattedCost(jsnPlanName.getDouble("sum_max_total_cost")) + ")"
					);
					
				}
					
				strSQL = strOperatorQueryTemplate;
				strSQL = strSQL.replace("{ALL_PLANS_DELIMITED}", strPlanNames);
				strSQL = strSQL.replace("{CURRENT_PLAN}", jsnPlanName.getString("plan_name"));
				jsnOperatorTypes = Database.rows(strSQL);

				// jsnOperatorTypes != null: see if the user deleted plan data 
				for (int intOperatorType = 0; jsnOperatorTypes != null && intOperatorType < jsnOperatorTypes.size(); intOperatorType++) {
					jsnOperatorType = (JsonObject) jsnOperatorTypes.get(intOperatorType);
	
					// add fields for this row
					if (jsnOperatorType.getString("plan_name") == null || jsnOperatorType.get("cost_percentage") == null) {
						vctRow.addElement("");
					} else {
						//vctRow.addElement(jsnOperatorType.getFloat("cost_percentage"));
						vctRow.addElement(
							Utils.roundValueRequirePrecision(jsnOperatorType.getDouble("cost_percentage") * 100, 0) + "%"
							+ " (" + Utils.formattedCost(jsnOperatorType.getDouble("lov_epoa_total_cost")) + ")"
						);
					}
				}
				vctRows.addElement(vctRow);
			}
		}

		DefaultTableModel model = (DefaultTableModel) this.getModel();
		model.setDataVector(vctRows, vctColumns);


		this.setModel(model);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		//for (int column = 0; column < columnNames.length; column++) {
		for (int column = 0; column <= 1; column++) {
			this.getColumnModel().getColumn(column).setPreferredWidth(200);
		}
		for (int column = 2; column < vctColumns.size(); column++) {
			this.getColumnModel().getColumn(column).setPreferredWidth(100);
		}

		JTableHeader th = this.getTableHeader();
		th.setReorderingAllowed(false);
		int count = th.getMouseListeners().length;
		MouseListener mouseListeners[] = th.getMouseListeners();
		for (int i = 0; i < count; i++)
			th.removeMouseListener(mouseListeners[i]);

		th.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent evt) {
				JTable table = ((JTableHeader) evt.getSource()).getTable();
				for (int column = 0; column < vctColumns.size(); column++) {
					table.getColumnModel().getColumn(column).setResizable(true);
				}
			}

			public void mousePressed(MouseEvent evt) {
				int count = evt.getClickCount();
				if (count > 1) {
					JTable table = ((JTableHeader) evt.getSource()).getTable();
					TableColumnModel colModel = table.getColumnModel();
					//Rectangle headerRects[] = new Rectangle[4];
					Rectangle headerRects[] = new Rectangle[vctColumns.size()];
					//for (int i = 0; i < 4; i++)
					for (int i = 0; i < vctColumns.size(); i++)
						headerRects[i] = table.getTableHeader().getHeaderRect(i);
					if (th.getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
						int index = -1;
						//for (int j = 0; j < 4; j++)
						for (int j = 0; j < vctColumns.size(); j++)
							if (evt.getX() > (headerRects[j].x + headerRects[j].width - 10) && evt.getX() < (headerRects[j].x + headerRects[j].width + 10)) {
								index = j;
								break;
							}
						if (index == -1)
							return;

						int width = 15; // Min width
						for (int row = 0; row < table.getRowCount(); row++) {
							TableCellRenderer renderer = table.getCellRenderer(row, index);
							JComponent comp = (JComponent) table.prepareRenderer(renderer, row, index);
							width = Math.max(comp.getPreferredSize().width + 5, width);
						}
						colModel.getColumn(index).setResizable(false);
						colModel.getColumn(index).setPreferredWidth(width);
					}
				}
			}
		});

		for (int i = 0; i < count; i++)
			th.addMouseListener(mouseListeners[i]);
	}

	public static void setPlanNamesDelimited(String planNamesDelimited) {
		boolean bolShowFunctionName = false;
		boolean bolShowProperties = false;
		if (bolShowFunctionName == true) { System.out.println("PlanStatisticsPanel setPlanNamesDelimited()"); }
		if (bolShowProperties == true) { System.out.println("PlanStatisticsPanel setPlanNamesDelimited() setting " + planNamesDelimited); }
		PlanStatisticsPanel.planNamesDelimited = planNamesDelimited;
	}

	public static String getPlanNamesDelimited() {
		boolean bolShowFunctionName = false;
		boolean bolShowProperties = false;
		if (bolShowFunctionName == true) { System.out.println("PlanStatisticsPanel getPlanNamesDelimited()"); }
		// hard-code until we know how to get the names from FormPanel
		//String strHardCodePlanNames = "'Plan 8', 'Plan 9'";
		//return planNamesDelimited;
		if (bolShowProperties == true) { System.out.println("PlanStatisticsPanel getPlanNamesDelimited() returning " + planNamesDelimited); }
		//return strHardCodePlanNames;
		return planNamesDelimited;
	}
	
	public /*static*/ void setReadStatisticsDataFlag(boolean enabled) {
		PlanStatisticsPanel.readStatisticsDataFlag = enabled;
	}
	
	public static boolean getReadStatisticsDataFlag() {
		return PlanStatisticsPanel.readStatisticsDataFlag;
	}
}
