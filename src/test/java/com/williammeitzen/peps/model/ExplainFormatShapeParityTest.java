package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Progressive live Postgres checks: FORMAT TEXT (converted) vs FORMAT JSON must share
 * the same diagram shape fingerprint. Skips when no reachable connected plan exists.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExplainFormatShapeParityTest {

	private static final String EXPLAIN_OPTS_TEXT =
			"explain (format text, analyze, timing on, buffers on, verbose on)\n";
	private static final String EXPLAIN_OPTS_JSON =
			"explain (format json, analyze, timing on, buffers on, verbose on)\n";

	private static ConnInfo conn;
	private static boolean pgReachable;

	@BeforeAll
	static void connect() throws Exception {
		Database.init();
		conn = loadPreferredConnection();
		assumeTrue(conn != null, "No connected explain_plans row with server/database.");
		List<Object> init = Database.pg_init(conn.server, conn.port, conn.database, conn.user, conn.password,
				conn.preCommand == null ? "" : conn.preCommand);
		pgReachable = !(Boolean) init.get(0);
		if (!pgReachable) {
			System.out.println("Postgres unreachable for shape parity: " + init.get(1));
		}
		assumeTrue(pgReachable, "Postgres not reachable from SQLite connection fields.");
	}

	@Test
	@Order(1)
	void simpleSeqScanAndLimit() throws Exception {
		assertShapeParity("select 1", "select c.relname from pg_class c limit 5");
	}

	@Test
	@Order(2)
	void valuesAndResult() throws Exception {
		assertShapeParity("values (1), (2), (3)");
	}

	@Test
	@Order(3)
	void joinsWhereAndGroupBy() throws Exception {
		String setup = ""
				+ "drop table if exists peps_shape_a;\n"
				+ "drop table if exists peps_shape_b;\n"
				+ "create temporary table peps_shape_a (id int primary key, g int, flag boolean);\n"
				+ "create temporary table peps_shape_b (id int primary key, g int);\n"
				+ "insert into peps_shape_a select g, g % 3, (g % 2 = 0) from generate_series(1, 40) g;\n"
				+ "insert into peps_shape_b select g, g % 5 from generate_series(1, 40) g;\n"
				+ "analyze peps_shape_a;\n"
				+ "analyze peps_shape_b;\n";
		List<Object> dml = Database.pg_dml(setup, ";");
		assumeTrue(!(Boolean) dml.get(0), "Setup DDL failed: " + dml.get(1));

		assertShapeParity(
				"select a.id from peps_shape_a a inner join peps_shape_b b on a.id = b.id",
				"select a.id from peps_shape_a a left join peps_shape_b b on a.id = b.id",
				"select a.id from peps_shape_a a right join peps_shape_b b on a.id = b.id",
				"select a.id from peps_shape_a a full join peps_shape_b b on a.id = b.id where a.flag is not false",
				"select a.g, count(*) from peps_shape_a a where a.flag = true group by a.g",
				"select a.g, count(*) from peps_shape_a a inner join peps_shape_b b on a.id = b.id "
						+ "where b.g > 0 group by a.g having count(*) > 0");
	}

	@Test
	@Order(4)
	void indexScanWhenPossible() throws Exception {
		String setup = ""
				+ "drop table if exists peps_shape_idx;\n"
				+ "create temporary table peps_shape_idx (id int primary key, note text);\n"
				+ "insert into peps_shape_idx select g, 'n' || g from generate_series(1, 100) g;\n"
				+ "analyze peps_shape_idx;\n";
		List<Object> dml = Database.pg_dml(setup, ";");
		assumeTrue(!(Boolean) dml.get(0), "Index setup failed: " + dml.get(1));
		assertShapeParity("select * from peps_shape_idx where id = 42");
	}

	private static void assertShapeParity(String... queries) throws Exception {
		for (String query : queries) {
			String textPlan = fetchExplain(EXPLAIN_OPTS_TEXT + query);
			String jsonPlan = fetchExplain(EXPLAIN_OPTS_JSON + query);
			String textFp = PlanShapeFingerprint.fromPlanField(textPlan);
			String jsonFp = PlanShapeFingerprint.fromPlanField(jsonPlan);
			assertEquals(jsonFp, textFp,
					() -> "Shape mismatch for:\n" + query
							+ "\n--- TEXT shape ---\n" + textFp
							+ "--- JSON shape ---\n" + jsonFp);
		}
	}

	private static String fetchExplain(String sql) throws Exception {
		List<Object> result = Database.pg_getPlan(sql);
		boolean failed = (Boolean) result.get(0);
		if (failed) {
			throw new IllegalStateException("EXPLAIN failed: " + result.get(1) + "\nSQL:\n" + sql);
		}
		String plan = (String) result.get(2);
		if (plan == null || plan.isBlank()) {
			throw new IllegalStateException("Empty EXPLAIN result for:\n" + sql);
		}
		return plan;
	}

	private static ConnInfo loadPreferredConnection() throws Exception {
		try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:postgresql-explain-plan-studio.sqlite");
				Statement st = sqlite.createStatement();
				ResultSet rs = st.executeQuery(""
						+ "select server, port, database, username, password, pre_explain_command "
						+ "from explain_plans "
						+ "where coalesce(trim(server), '') <> '' and coalesce(trim(database), '') <> '' "
						+ "order by case when server like '192.168.%' then 0 else 1 end, id_explain_plan "
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
			info.preCommand = rs.getString(6);
			return info;
		}
	}

	private static final class ConnInfo {
		String server;
		String port;
		String database;
		String user;
		String password;
		String preCommand;
	}
}
