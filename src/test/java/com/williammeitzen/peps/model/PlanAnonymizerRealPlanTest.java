package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Regression against the saved "gxd_genotype_summary_view (json)" plan from the
 * PEPS SQLite database (id_explain_plan = 25).
 */
class PlanAnonymizerRealPlanTest {

	@Test
	void scrubsSavedGxdGenotypeSummaryViewJsonPlan() throws Exception {
		String sql = readResource("gxd_genotype_summary_view_json_sql.txt");
		String plan = readResource("gxd_genotype_summary_view_json_plan.txt");

		assertTrue(sql.contains("gxd_genotype_summary_view"));
		assertTrue(plan.contains("s_2"));
		assertTrue(plan.contains("prb_strain") || plan.contains("_strain_key"));

		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", sql, plan, true);

		assertFalse(result.sql.contains("gxd_genotype_summary_view"),
				() -> "SQL leak: " + result.sql);
		assertFalse(result.plan.contains("gxd_genotype_summary_view"),
				"Plan still has view name (Query / elsewhere)");
		assertFalse(result.plan.contains("\"s_2\""), "Alias field s_2 still present");
		assertFalse(result.plan.contains("s_2."), "Output still has s_2. prefix");
		assertFalse(result.plan.contains("_strain_key"), "Column _strain_key still present");
		assertFalse(result.plan.contains("prb_strain"), "Relation prb_strain still present");
		assertFalse(result.plan.contains("acc_accession"), "Relation acc_accession still present");
		assertTrue(result.sql.contains("table_"), () -> "SQL: " + result.sql);
		assertTrue(result.plan.contains("alias_"));
		assertTrue(result.plan.contains("column_"));
		assertTrue(result.plan.contains("table_"));
	}

	@Test
	void aliasesOffKeepsJoinAliasesOnSavedGenotypePlan() throws Exception {
		String sql = readResource("gxd_genotype_summary_view_json_sql.txt");
		String plan = readResource("gxd_genotype_summary_view_json_plan.txt");

		PlanAnonymizer.Result result = PlanAnonymizer.anonymize("", "", sql, plan, false);

		assertTrue(result.plan.contains("g_1"), "g_1 should remain when aliases off");
		assertTrue(result.plan.contains("a_1"), "a_1 should remain when aliases off");
		assertTrue(result.plan.contains("(g_1.") || result.plan.contains("(g_1 "),
				() -> "Merge Cond should keep alias prefix: " + snippet(result.plan, "Merge Cond"));
		assertFalse(result.plan.contains("_genotype_key"));
		assertFalse(result.plan.contains("_object_key"));
		assertFalse(result.plan.contains("alias_"));
		assertTrue(result.plan.contains("column_"));
	}

	private static String snippet(String plan, String marker) {
		int i = plan.indexOf(marker);
		if (i < 0) {
			return "(missing " + marker + ")";
		}
		return plan.substring(i, Math.min(i + 120, plan.length()));
	}

	private static String readResource(String name) throws Exception {
		try (InputStream in = PlanAnonymizerRealPlanTest.class.getClassLoader().getResourceAsStream(name)) {
			assertNotNull(in, "missing test resource: " + name);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
