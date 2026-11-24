package com.williammeitzen.peps.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.williammeitzen.peps.model.ExplainTextPlanConverter;
import com.williammeitzen.peps.model.PlanFormat;

public class SampleExplainPlansTest {

	@Test
	public void sampleJsonIsRecognizedAndResolves() throws Exception {
		String json = SampleExplainPlans.jsonPlanText();
		assertEquals(PlanFormat.JSON, ExplainTextPlanConverter.sniff(json));
		String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(json);
		assertTrue(resolved != null && resolved.contains("\"Node Type\""));
		assertTrue(json.contains("all_cellline"));
		assertTrue(json.contains("\"Execution Time\": 1239.933"));
		assertTrue(json.length() > 4000);
	}

	@Test
	public void sampleJsonExplainSqlIsAllCellline() {
		String sql = SampleExplainPlans.jsonExplainSql();
		assertTrue(sql.contains("format json"));
		assertTrue(sql.contains("wal on"));
		assertTrue(sql.contains("mgd.all_cellline"));
	}

	@Test
	public void sampleTextIsRecognizedAndResolves() throws Exception {
		String text = SampleExplainPlans.textPlanText();
		assertEquals(PlanFormat.TEXT, ExplainTextPlanConverter.sniff(text));
		String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(text);
		assertTrue(resolved != null && resolved.contains("Nested Loop"));
		assertTrue(text.contains("all_cellline"));
		assertTrue(text.contains("Execution Time: 1196.539 ms"));
		assertTrue(text.length() > 2000);
	}

	@Test
	public void sampleTextExplainSqlIsAllCellline() {
		String sql = SampleExplainPlans.textExplainSql();
		assertTrue(sql.contains("format text"));
		assertTrue(sql.contains("wal on"));
		assertTrue(sql.contains("mgd.all_cellline"));
	}
}
