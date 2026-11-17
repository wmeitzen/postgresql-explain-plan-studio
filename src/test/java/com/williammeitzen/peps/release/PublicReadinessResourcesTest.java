package com.williammeitzen.peps.release;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;

import com.williammeitzen.peps.gui.PlanSourcePanel;
import com.williammeitzen.peps.gui.SampleExplainPlans;
import com.williammeitzen.peps.model.ExplainTextPlanConverter;
import com.williammeitzen.peps.model.PlanFormat;

/**
 * Non-UI packaging/readiness checks: sample plans resolve, anonymize asset ships.
 */
class PublicReadinessResourcesTest {

	@Test
	void anonymizeSilhouettePngIsOnClasspath() {
		URL url = PlanSourcePanel.class.getResource("/images/anonymize-silhouette.png");
		assertNotNull(url, "anonymize-silhouette.png missing from resources — badges will be blank");
	}

	@Test
	void sampleJsonAndTextPlansResolve() throws Exception {
		assertTrue(SampleExplainPlans.jsonPlanText().length() > 1000);
		assertTrue(SampleExplainPlans.textPlanText().length() > 500);
		assertEqualsJson(SampleExplainPlans.jsonPlanText());
		assertEqualsText(SampleExplainPlans.textPlanText());
		String jsonResolved = ExplainTextPlanConverter.resolveToJsonArrayString(SampleExplainPlans.jsonPlanText());
		String textResolved = ExplainTextPlanConverter.resolveToJsonArrayString(SampleExplainPlans.textPlanText());
		assertNotNull(jsonResolved);
		assertNotNull(textResolved);
		assertTrue(jsonResolved.contains("\"Node Type\""));
		assertTrue(textResolved.contains("\"Node Type\"") || textResolved.contains("Nested Loop"));
	}

	@Test
	void releaseIssueLogRoundTripsMarkdown() throws Exception {
		ReleaseIssueLog log = ReleaseIssueLog.get();
		log.clear();
		log.add(ReleaseIssue.Severity.MAJOR, ReleaseIssue.Area.GET_STARTED,
				"Example finding", "detail for triage");
		String md = log.toMarkdown();
		assertTrue(md.contains("Example finding"));
		assertTrue(md.contains("MAJOR"));
		assertTrue(md.contains("- [ ]"));
		log.clear();
	}

	private static void assertEqualsJson(String plan) {
		assertTrue(ExplainTextPlanConverter.sniff(plan) == PlanFormat.JSON);
	}

	private static void assertEqualsText(String plan) {
		assertTrue(ExplainTextPlanConverter.sniff(plan) == PlanFormat.TEXT);
	}
}
