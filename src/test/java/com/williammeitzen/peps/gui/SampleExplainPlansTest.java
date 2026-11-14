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
		assertTrue(json.length() > 4000);
	}

	@Test
	public void sampleTextIsRecognizedAndResolves() throws Exception {
		String text = SampleExplainPlans.textPlanText();
		assertEquals(PlanFormat.TEXT, ExplainTextPlanConverter.sniff(text));
		String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(text);
		assertTrue(resolved != null && resolved.contains("Nested Loop"));
		assertTrue(text.length() > 2000);
	}
}
