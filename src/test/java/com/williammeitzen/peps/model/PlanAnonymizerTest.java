package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Map;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.jupiter.api.Test;

class PlanAnonymizerTest {

	private static final String SAMPLE_SQL = ""
			+ "explain (analyze, buffers, format json, verbose)\n"
			+ "select g.genotype_id\n"
			+ "from mgd.gxd_genotype g\n"
			+ "where g.strain = 'C57BL/6J';\n";

	private static final String SAMPLE_PLAN = "["
			+ "{\"Plan\":{"
			+ "\"Node Type\":\"Seq Scan\","
			+ "\"Parallel Aware\":false,"
			+ "\"Relation Name\":\"gxd_genotype\","
			+ "\"Schema\":\"mgd\","
			+ "\"Alias\":\"g\","
			+ "\"Startup Cost\":0.00,"
			+ "\"Total Cost\":12.34,"
			+ "\"Plan Rows\":100,"
			+ "\"Plan Width\":8,"
			+ "\"Actual Startup Time\":0.012,"
			+ "\"Actual Total Time\":1.234,"
			+ "\"Actual Rows\":50,"
			+ "\"Actual Loops\":1,"
			+ "\"Filter\":\"((g.strain)::text = 'C57BL/6J'::text)\","
			+ "\"Output\":[\"g.genotype_id\"]"
			+ "}}]";

	@Test
	void anonymizedJsonPlanRemainsParseable() throws Exception {
		PlanAnonymizer.Result result = PlanAnonymizer.anonymize(
				"set search_path to mgd",
				"",
				SAMPLE_SQL,
				SAMPLE_PLAN,
				true);

		assertDoesNotThrow(() -> Jsoner.deserialize(result.plan));
		Object parsed = Jsoner.deserialize(result.plan);
		assertTrue(parsed instanceof JsonArray);
		JsonObject plan = (JsonObject) ((JsonObject) ((JsonArray) parsed).get(0)).get("Plan");
		assertEquals("Seq Scan", plan.get("Node Type"));
		assertEquals(12.34, ((Number) plan.get("Total Cost")).doubleValue(), 0.0001);
		assertEquals(1.234, ((Number) plan.get("Actual Total Time")).doubleValue(), 0.0001);
	}

	@Test
	void anonymizedPlanStillResolvesForDiagramPipeline() throws Exception {
		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", SAMPLE_SQL, SAMPLE_PLAN, true);
		String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(result.plan);
		assertTrue(resolved != null && !resolved.isBlank());
		Object parsed = Jsoner.deserialize(resolved);
		assertTrue(parsed instanceof JsonArray);
	}

	@Test
	void scrubsSchemaRelationAndLiteralButKeepsPublicAndPgCatalog() {
		String plan = "["
				+ "{\"Plan\":{"
				+ "\"Node Type\":\"Seq Scan\","
				+ "\"Relation Name\":\"customers\","
				+ "\"Schema\":\"mgd\","
				+ "\"Alias\":\"c\","
				+ "\"Filter\":\"(c.email = 'secret@example.com')\","
				+ "\"Total Cost\":9.9"
				+ "}}]";
		PlanAnonymizer.Result result = PlanAnonymizer.anonymize(
				"", "", "select * from mgd.customers c where c.email = 'secret@example.com'", plan, true);

		assertFalse(result.plan.contains("mgd"));
		assertFalse(result.plan.contains("customers"));
		assertFalse(result.plan.contains("secret@example.com"));
		assertFalse(result.sql.contains("mgd"));
		assertTrue(result.plan.contains("schema_"));
		assertTrue(result.plan.contains("table_"));

		String keepPlan = "[{\"Plan\":{\"Node Type\":\"Seq Scan\",\"Relation Name\":\"pg_class\",\"Schema\":\"pg_catalog\"}}]";
		PlanAnonymizer.Result keep = PlanAnonymizer.anonymize("", "", "select * from pg_catalog.pg_class", keepPlan, true);
		assertTrue(keep.plan.contains("pg_class"));
		assertTrue(keep.plan.contains("pg_catalog"));
	}

	@Test
	void aliasCheckboxControlsAliasScrubbing() {
		PlanAnonymizer.Result withAliases = PlanAnonymizer.anonymize("", "", SAMPLE_SQL, SAMPLE_PLAN, true);
		PlanAnonymizer.Result withoutAliases = PlanAnonymizer.anonymize("", "", SAMPLE_SQL, SAMPLE_PLAN, false);

		assertFalse(withAliases.plan.contains("\"Alias\":\"g\""));
		assertTrue(withAliases.plan.contains("alias_"));
		assertTrue(withoutAliases.plan.contains("\"Alias\":\"g\""));
		assertFalse(withoutAliases.plan.contains("alias_"));
	}

	@Test
	void reverseMapRestoresAdviceSql() {
		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", SAMPLE_SQL, SAMPLE_PLAN, true);
		String anonTable = null;
		String anonSchema = null;
		String anonColumn = null;
		for (Map.Entry<String, String> e : result.forwardMap.entrySet()) {
			if ("gxd_genotype".equals(e.getKey())) {
				anonTable = e.getValue();
			}
			if ("mgd".equals(e.getKey())) {
				anonSchema = e.getValue();
			}
			if ("genotype_id".equals(e.getKey()) || "strain".equals(e.getKey())) {
				anonColumn = e.getValue();
			}
		}
		assertTrue(anonTable != null && anonSchema != null);

		String advice = "CREATE INDEX ON " + anonSchema + "." + anonTable
				+ (anonColumn == null ? "" : " (" + anonColumn + ")") + ";";
		String restored = PlanAnonymizer.applyReverse(advice, result.reverseMap);
		assertTrue(restored.contains("mgd"));
		assertTrue(restored.contains("gxd_genotype"));
		assertFalse(restored.contains("table_"));
		assertFalse(restored.contains("schema_"));
	}

	@Test
	void unmatchedAnonymizedTokensAreReported() {
		Map<String, String> reverse = Map.of("table_a", "customers", "schema_a", "mgd");
		List<String> unmatched = PlanAnonymizer.unmatchedAnonymizedTokens(
				"CREATE INDEX ON schema_a.table_a (column_z);", reverse);
		assertTrue(unmatched.stream().anyMatch(t -> t.equalsIgnoreCase("column_z")));
		assertTrue(PlanAnonymizer.unmatchedAnonymizedTokens(
				"CREATE INDEX ON schema_a.table_a;", reverse).isEmpty());
	}

	@Test
	void sqlAndPlanShareSameTokens() {
		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", SAMPLE_SQL, SAMPLE_PLAN, true);
		String tableToken = result.forwardMap.get("gxd_genotype");
		String schemaToken = result.forwardMap.get("mgd");
		assertTrue(tableToken != null && schemaToken != null);
		assertTrue(result.sql.contains(tableToken));
		assertTrue(result.sql.contains(schemaToken));
		assertTrue(result.plan.contains(tableToken));
		assertTrue(result.plan.contains("\"" + schemaToken + "\"")
				|| result.plan.contains(schemaToken));
	}

	@Test
	void quotedJsonValuesStayQuotedAfterScrub() throws Exception {
		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", SAMPLE_SQL, SAMPLE_PLAN, true);
		JsonObject plan = (JsonObject) ((JsonObject) ((JsonArray) Jsoner.deserialize(result.plan)).get(0)).get("Plan");
		Object relation = plan.get("Relation Name");
		Object schema = plan.get("Schema");
		assertTrue(relation instanceof String);
		assertTrue(schema instanceof String);
		assertFalse(((String) relation).isBlank());
		assertFalse(((String) schema).isBlank());
		assertFalse("gxd_genotype".equals(relation));
		assertFalse("mgd".equals(schema));
	}

	@Test
	void scrubsViewNameInSqlAndOutputAliasColumns() throws Exception {
		String sql = ""
				+ "explain (format json, analyze, timing on, buffers on, verbose on)\n"
				+ "select * from gxd_genotype_summary_view\n";
		String plan = "["
				+ "{\"Plan\":{"
				+ "\"Node Type\":\"Seq Scan\","
				+ "\"Relation Name\":\"gxd_genotype_summary_view\","
				+ "\"Alias\":\"s_2\","
				+ "\"Output\":[\"s_2._strain_key\",\"s_2._species_key\",\"s_2.strain\",\"s_2.standard\",\"s_2.private\"],"
				+ "\"Total Cost\":42.0,"
				+ "\"Actual Total Time\":1.5"
				+ "}}]";

		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", sql, plan, true);

		assertFalse(result.sql.contains("gxd_genotype_summary_view"),
				() -> "SQL still had view name: " + result.sql);
		assertFalse(result.plan.contains("gxd_genotype_summary_view"),
				() -> "Plan still had view name: " + result.plan);
		assertFalse(result.plan.contains("s_2"),
				() -> "Plan still had alias s_2: " + result.plan);
		assertFalse(result.plan.contains("_strain_key"),
				() -> "Plan still had column _strain_key: " + result.plan);
		assertTrue(result.plan.contains("table_") || result.sql.contains("table_"));
		assertTrue(result.plan.contains("alias_"));
		assertTrue(result.plan.contains("column_"));
		assertEquals(42.0,
				((Number) ((JsonObject) ((JsonObject) ((JsonArray) Jsoner.deserialize(result.plan)).get(0))
						.get("Plan")).get("Total Cost")).doubleValue(),
				0.0001);
	}
}
