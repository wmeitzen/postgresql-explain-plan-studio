package com.williammeitzen.peps.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import com.williammeitzen.peps.model.Database;

/**
 * One-shot seeder: add SQLite demo plans for every statement kind PostgreSQL EXPLAIN
 * accepts (DML + DDL), matching the style of existing demos (Merge, Sample Scan, …).
 *
 * Run: {@code mvnw -q -DskipTests exec:java -Dexec.classpathScope=test
 * -Dexec.mainClass=com.williammeitzen.peps.tools.SeedExplainableDmlDdlPlans}
 */
public final class SeedExplainableDmlDdlPlans {

	private static final String EXPLAIN =
			"explain (analyze, buffers, format json, verbose)\n\n";

	private static final String DEMO_TABLE_PRE = ""
			+ "DROP TABLE IF EXISTS peps_demo_dml CASCADE; "
			+ "CREATE TABLE peps_demo_dml ("
			+ "  id int PRIMARY KEY,"
			+ "  symbol text NOT NULL,"
			+ "  hits int NOT NULL DEFAULT 0"
			+ "); "
			+ "INSERT INTO peps_demo_dml VALUES"
			+ "  (1, 'Kit', 3),"
			+ "  (2, 'Trp53', 10),"
			+ "  (3, 'Sox2', 1); "
			+ "ANALYZE peps_demo_dml;";

	private static final String DEMO_TABLE_POST =
			"DROP TABLE IF EXISTS peps_demo_dml CASCADE;";

	private SeedExplainableDmlDdlPlans() {
	}

	public static void main(String[] args) throws Exception {
		Database.init();
		ConnInfo conn = loadTemplateConnection();
		if (conn == null) {
			throw new IllegalStateException(
					"No template explain_plans row with server/database (expected Merge or similar).");
		}

		List<Object> init = Database.pg_init(conn.server, conn.port, conn.database, conn.user, conn.password, "");
		if ((Boolean) init.get(0)) {
			throw new IllegalStateException("Postgres unreachable: " + init.get(1));
		}

		seed(conn, "Insert", DEMO_TABLE_PRE,
				EXPLAIN + "insert into peps_demo_dml (id, symbol, hits) values (4, 'Pax6', 0)",
				DEMO_TABLE_POST);

		seed(conn, "Update", DEMO_TABLE_PRE,
				EXPLAIN + "update peps_demo_dml set hits = hits + 1 where id = 1",
				DEMO_TABLE_POST);

		seed(conn, "Delete", DEMO_TABLE_PRE,
				EXPLAIN + "delete from peps_demo_dml where id = 3",
				DEMO_TABLE_POST);

		// Merge already exists as a named demo; refresh plan_text if present, else insert.
		seed(conn, "Merge",
				""
						+ "DROP TABLE IF EXISTS peps_demo_merge; "
						+ "CREATE TABLE peps_demo_merge ("
						+ "  id int PRIMARY KEY,"
						+ "  symbol text NOT NULL,"
						+ "  status text NOT NULL DEFAULT 'new',"
						+ "  hits int NOT NULL DEFAULT 0"
						+ "); "
						+ "INSERT INTO peps_demo_merge VALUES"
						+ "  (1, 'Kit', 'active', 3),"
						+ "  (2, 'Trp53', 'active', 10),"
						+ "  (5, 'Pax6', 'retired', 1); "
						+ "ANALYZE peps_demo_merge;",
				EXPLAIN + ""
						+ "merge into peps_demo_merge as t\n"
						+ "using (values\n"
						+ "  (1, 'Kit', 'active', 1),\n"
						+ "  (2, 'Trp53', 'hold', 0),\n"
						+ "  (3, 'Sox2', 'active', 2),\n"
						+ "  (5, 'Pax6', 'retired', 0)\n"
						+ ") as s(id, symbol, status, hits)\n"
						+ "on t.id = s.id\n"
						+ "when matched and s.status = 'hold' then\n"
						+ "  update set status = s.status, hits = t.hits + s.hits\n"
						+ "when matched and s.hits = 0 then\n"
						+ "  delete\n"
						+ "when matched then\n"
						+ "  update set symbol = s.symbol, status = s.status, hits = t.hits + s.hits\n"
						+ "when not matched then\n"
						+ "  insert (id, symbol, status, hits) values (s.id, s.symbol, s.status, s.hits)",
				"DROP TABLE IF EXISTS peps_demo_merge;");

		seed(conn, "Values", "",
				EXPLAIN + "values (1, 'Kit'), (2, 'Trp53'), (3, 'Sox2')",
				"");

		seed(conn, "TABLE", DEMO_TABLE_PRE,
				EXPLAIN + "table peps_demo_dml",
				DEMO_TABLE_POST);

		seed(conn, "CREATE TABLE AS", DEMO_TABLE_PRE,
				EXPLAIN + "create table peps_demo_ctas as select id, symbol, hits from peps_demo_dml",
				DEMO_TABLE_POST + " DROP TABLE IF EXISTS peps_demo_ctas;");

		seed(conn, "CREATE MATERIALIZED VIEW", DEMO_TABLE_PRE,
				EXPLAIN + "create materialized view peps_demo_mv as select id, symbol, hits from peps_demo_dml",
				DEMO_TABLE_POST + " DROP MATERIALIZED VIEW IF EXISTS peps_demo_mv;");

		seed(conn, "Select Into", DEMO_TABLE_PRE,
				EXPLAIN + "select id, symbol, hits into peps_demo_selinto from peps_demo_dml",
				DEMO_TABLE_POST + " DROP TABLE IF EXISTS peps_demo_selinto;");

		seed(conn, "EXECUTE",
				DEMO_TABLE_PRE + " PREPARE peps_demo_prep(int) AS "
						+ "select id, symbol, hits from peps_demo_dml where id = $1;",
				EXPLAIN + "execute peps_demo_prep(2)",
				"DEALLOCATE peps_demo_prep; " + DEMO_TABLE_POST);

		seed(conn, "DECLARE", DEMO_TABLE_PRE,
				EXPLAIN + "declare peps_demo_cursor cursor for "
						+ "select id, symbol, hits from peps_demo_dml order by id",
				DEMO_TABLE_POST);

		System.out.println("Done seeding EXPLAIN-able DML/DDL demo plans.");
	}

	private static void seed(ConnInfo conn, String planName, String pre, String explainSql, String post)
			throws Exception {
		System.out.println("Seeding: " + planName);

		List<Object> preResult = Database.pg_dml(pre == null ? "" : pre, ";");
		if ((Boolean) preResult.get(0)) {
			throw new IllegalStateException(planName + " pre failed: " + preResult.get(1));
		}

		List<Object> planResult = Database.pg_getPlan(explainSql);
		if ((Boolean) planResult.get(0)) {
			Database.pg_dml(post == null ? "" : post, ";");
			throw new IllegalStateException(planName + " EXPLAIN failed: " + planResult.get(1));
		}
		String planText = (String) planResult.get(2);
		if (planText == null || planText.isBlank()) {
			Database.pg_dml(post == null ? "" : post, ";");
			throw new IllegalStateException(planName + " returned empty EXPLAIN output");
		}

		List<Object> postResult = Database.pg_dml(post == null ? "" : post, ";");
		if ((Boolean) postResult.get(0)) {
			throw new IllegalStateException(planName + " post failed: " + postResult.get(1));
		}

		upsertPlan(conn, planName, pre, explainSql, post, planText);
		System.out.println("  ok, plan_text length=" + planText.length());
	}

	private static void upsertPlan(ConnInfo conn, String planName, String pre, String explainSql,
			String post, String planText) throws Exception {
		try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:postgresql-explain-plan-studio.sqlite")) {
			Integer existingId = null;
			try (PreparedStatement find = sqlite.prepareStatement(
					"select id_explain_plan from explain_plans where plan_name = ?")) {
				find.setString(1, planName);
				try (ResultSet rs = find.executeQuery()) {
					if (rs.next()) {
						existingId = rs.getInt(1);
					}
				}
			}

			if (existingId != null) {
				try (PreparedStatement upd = sqlite.prepareStatement(""
						+ "update explain_plans set "
						+ "plan_text = ?, server = ?, port = ?, database = ?, username = ?, password = ?, "
						+ "pre_explain_command = ?, explain_sql_command = ?, post_explain_command = ?, "
						+ "remember_password = ?, auth_type = ?, "
						+ "write_version_environment_to_json = 1, write_query_to_json = 1, "
						+ "write_pg_environment_settings_to_json = 1 "
						+ "where id_explain_plan = ?")) {
					bindConnAndCommands(upd, conn, planText, pre, explainSql, post);
					upd.setInt(12, existingId);
					upd.executeUpdate();
				}
				return;
			}

			int nextId;
			try (Statement st = sqlite.createStatement();
					ResultSet rs = st.executeQuery("select coalesce(max(id_explain_plan), 0) + 1 from explain_plans")) {
				rs.next();
				nextId = rs.getInt(1);
			}

			try (PreparedStatement ins = sqlite.prepareStatement(""
					+ "insert into explain_plans ("
					+ "id_explain_plan, plan_name, plan_text, server, port, database, username, password, "
					+ "pre_explain_command, explain_sql_command, post_explain_command, "
					+ "remember_password, auth_type, "
					+ "write_version_environment_to_json, write_query_to_json, write_pg_environment_settings_to_json"
					+ ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 1)")) {
				ins.setInt(1, nextId);
				ins.setString(2, planName);
				ins.setString(3, planText);
				ins.setString(4, conn.server);
				ins.setString(5, conn.port);
				ins.setString(6, conn.database);
				ins.setString(7, conn.user);
				ins.setString(8, conn.password);
				ins.setString(9, pre);
				ins.setString(10, explainSql);
				ins.setString(11, post);
				ins.setInt(12, conn.rememberPassword);
				ins.setString(13, conn.authType);
				ins.executeUpdate();
			}

			try (PreparedStatement gv = sqlite.prepareStatement(
					"insert or ignore into explain_plan_generated_values (id_explain_plan, is_active) values (?, 0)")) {
				gv.setInt(1, nextId);
				gv.executeUpdate();
			}
		}
	}

	private static void bindConnAndCommands(PreparedStatement upd, ConnInfo conn, String planText,
			String pre, String explainSql, String post) throws Exception {
		upd.setString(1, planText);
		upd.setString(2, conn.server);
		upd.setString(3, conn.port);
		upd.setString(4, conn.database);
		upd.setString(5, conn.user);
		upd.setString(6, conn.password);
		upd.setString(7, pre);
		upd.setString(8, explainSql);
		upd.setString(9, post);
		upd.setInt(10, conn.rememberPassword);
		upd.setString(11, conn.authType);
	}

	private static ConnInfo loadTemplateConnection() throws Exception {
		try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:postgresql-explain-plan-studio.sqlite");
				Statement st = sqlite.createStatement();
				ResultSet rs = st.executeQuery(""
						+ "select server, port, database, username, password, auth_type, remember_password "
						+ "from explain_plans "
						+ "where coalesce(trim(server), '') <> '' and coalesce(trim(database), '') <> '' "
						+ "order by case when plan_name = 'Merge' then 0 else 1 end, "
						+ "case when server like '192.168.%' then 0 else 1 end, id_explain_plan "
						+ "limit 1")) {
			if (!rs.next()) {
				return null;
			}
			ConnInfo info = new ConnInfo();
			info.server = rs.getString(1);
			info.port = rs.getString(2);
			info.database = rs.getString(3);
			info.user = rs.getString(4);
			info.password = rs.getString(5) == null ? "" : rs.getString(5);
			info.authType = rs.getString(6) == null ? "password" : rs.getString(6);
			info.rememberPassword = rs.getObject(7) == null ? 1 : rs.getInt(7);
			return info;
		}
	}

	private static final class ConnInfo {
		String server;
		String port;
		String database;
		String user;
		String password;
		String authType;
		int rememberPassword;
	}
}
