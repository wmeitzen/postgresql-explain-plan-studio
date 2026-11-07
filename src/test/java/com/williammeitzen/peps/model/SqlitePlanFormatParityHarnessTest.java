package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Duplicates each connected SQLite plan into text/json variants, re-runs EXPLAIN with
 * analyze/timing/buffers/verbose, stores raw plan_text, and asserts shape fingerprints match.
 * Skips when Postgres is unreachable. Leaves duplicates in SQLite for UI comparison.
 */
class SqlitePlanFormatParityHarnessTest {

	private static final String TEXT_OPTS =
			"explain (format text, analyze, timing on, buffers on, verbose on)";
	private static final String JSON_OPTS =
			"explain (format json, analyze, timing on, buffers on, verbose on)";
	private static final Pattern EXPLAIN_PREFIX = Pattern.compile(
			"(?is)^\\s*explain\\s*(?:\\([^)]*\\))?\\s*");

	private static Connection sqlite;
	private static boolean anyConnected;

	@BeforeAll
	static void openSqlite() throws Exception {
		Database.init();
		sqlite = DriverManager.getConnection("jdbc:sqlite:postgresql-explain-plan-studio.sqlite");
		try (Statement st = sqlite.createStatement();
				ResultSet rs = st.executeQuery(""
						+ "select count(*) from explain_plans "
						+ "where coalesce(trim(server),'') <> '' and coalesce(trim(database),'') <> ''")) {
			anyConnected = rs.next() && rs.getInt(1) > 0;
		}
		assumeTrue(anyConnected, "No connected plans in SQLite.");
	}

	@Test
	void duplicateConnectedPlansAndAssertShapeParity() throws Exception {
		List<PlanRow> originals = loadConnectedPlans();
		assumeTrue(!originals.isEmpty(), "No connected plans to duplicate.");

		List<String> skipped = new ArrayList<>();
		List<String> mismatched = new ArrayList<>();
		int compared = 0;

		for (PlanRow original : originals) {
			List<Object> init = Database.pg_init(original.server, original.port, original.database,
					original.username, original.password == null ? "" : original.password,
					original.preCommand == null ? "" : original.preCommand);
			if ((Boolean) init.get(0)) {
				skipped.add(original.planName + " (connect failed: " + init.get(1) + ")");
				continue;
			}

			String body = stripExplainPrefix(original.explainSql);
			if (body.isBlank()) {
				skipped.add(original.planName + " (empty SQL body)");
				continue;
			}

			String textSql = TEXT_OPTS + "\n" + body;
			String jsonSql = JSON_OPTS + "\n" + body;

			String textPlan;
			String jsonPlan;
			try {
				textPlan = runExplain(textSql);
				jsonPlan = runExplain(jsonSql);
			} catch (Exception ex) {
				skipped.add(original.planName + " (explain failed: " + ex.getMessage() + ")");
				continue;
			}

			String textName = uniquePlanName(original.planName + " (text)");
			String jsonName = uniquePlanName(original.planName + " (json)");
			insertDuplicate(original, textName, textSql, textPlan);
			insertDuplicate(original, jsonName, jsonSql, jsonPlan);

			String textFp = PlanShapeFingerprint.fromPlanField(textPlan);
			String jsonFp = PlanShapeFingerprint.fromPlanField(jsonPlan);
			compared++;
			if (!textFp.equals(jsonFp)) {
				mismatched.add(original.planName
						+ "\n--- TEXT ---\n" + textFp
						+ "--- JSON ---\n" + jsonFp);
			}

			// Post command from original if present
			if (original.postCommand != null && !original.postCommand.isBlank()) {
				Database.pg_dml(original.postCommand, ";");
			}
		}

		System.out.println("SqlitePlanFormatParityHarness compared=" + compared
				+ " skipped=" + skipped.size()
				+ " mismatched=" + mismatched.size());
		for (String s : skipped) {
			System.out.println("SKIP: " + s);
		}
		assumeTrue(compared > 0, "No plans could be executed against Postgres. Skipped: " + skipped);
		assertTrue(mismatched.isEmpty(),
				"Shape mismatches:\n" + String.join("\n\n", mismatched));
		assertEquals(0, mismatched.size());
	}

	private static String runExplain(String sql) {
		List<Object> result = Database.pg_getPlan(sql);
		if ((Boolean) result.get(0)) {
			throw new IllegalStateException(String.valueOf(result.get(1)));
		}
		String plan = (String) result.get(2);
		if (plan == null || plan.isBlank()) {
			throw new IllegalStateException("empty plan");
		}
		return plan;
	}

	private static String stripExplainPrefix(String sql) {
		if (sql == null) {
			return "";
		}
		Matcher m = EXPLAIN_PREFIX.matcher(sql);
		if (m.find()) {
			return sql.substring(m.end()).trim();
		}
		return sql.trim();
	}

	private static String uniquePlanName(String preferred) throws Exception {
		String candidate = preferred;
		int n = 2;
		while (planNameExists(candidate)) {
			candidate = preferred + " " + n;
			n++;
		}
		return candidate;
	}

	private static boolean planNameExists(String name) throws Exception {
		try (PreparedStatement ps = sqlite.prepareStatement(
				"select 1 from explain_plans where plan_name = ?")) {
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	private static void insertDuplicate(PlanRow original, String newName, String explainSql, String planText)
			throws Exception {
		int newId;
		try (Statement st = sqlite.createStatement();
				ResultSet rs = st.executeQuery(
						"select coalesce(max(id_explain_plan) + 1, 1) from explain_plans")) {
			rs.next();
			newId = rs.getInt(1);
		}
		try (PreparedStatement ps = sqlite.prepareStatement(""
				+ "insert into explain_plans ("
				+ "id_explain_plan, plan_name, server, port, database, username, password, "
				+ "remember_password, auth_type, pre_explain_command, explain_sql_command, "
				+ "post_explain_command, plan_text"
				+ ") values (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
			ps.setInt(1, newId);
			ps.setString(2, newName);
			ps.setString(3, original.server);
			ps.setString(4, original.port);
			ps.setString(5, original.database);
			ps.setString(6, original.username);
			ps.setString(7, original.password);
			ps.setInt(8, original.rememberPassword ? 1 : 0);
			ps.setString(9, original.authType == null ? "password" : original.authType);
			ps.setString(10, original.preCommand);
			ps.setString(11, explainSql);
			ps.setString(12, original.postCommand);
			ps.setString(13, planText);
			ps.executeUpdate();
		}
	}

	private static List<PlanRow> loadConnectedPlans() throws Exception {
		List<PlanRow> rows = new ArrayList<>();
		try (Statement st = sqlite.createStatement();
				ResultSet rs = st.executeQuery(""
						+ "select id_explain_plan, plan_name, server, port, database, username, password, "
						+ "remember_password, auth_type, pre_explain_command, explain_sql_command, "
						+ "post_explain_command "
						+ "from explain_plans "
						+ "where coalesce(trim(server),'') <> '' and coalesce(trim(database),'') <> '' "
						+ "and plan_name not like '% (text)' and plan_name not like '% (json)' "
						+ "order by id_explain_plan")) {
			while (rs.next()) {
				PlanRow row = new PlanRow();
				row.id = rs.getInt(1);
				row.planName = rs.getString(2);
				row.server = rs.getString(3);
				row.port = rs.getString(4);
				row.database = rs.getString(5);
				row.username = rs.getString(6);
				row.password = rs.getString(7);
				row.rememberPassword = rs.getInt(8) == 1;
				row.authType = rs.getString(9);
				row.preCommand = rs.getString(10);
				row.explainSql = rs.getString(11);
				row.postCommand = rs.getString(12);
				rows.add(row);
			}
		}
		return rows;
	}

	private static final class PlanRow {
		int id;
		String planName;
		String server;
		String port;
		String database;
		String username;
		String password;
		boolean rememberPassword;
		String authType;
		String preCommand;
		String explainSql;
		String postCommand;
	}
}
