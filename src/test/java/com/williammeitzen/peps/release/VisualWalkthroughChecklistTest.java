package com.williammeitzen.peps.release;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VisualWalkthroughChecklistTest {

	@Test
	void checklistCoversCorePublicSurfaces() {
		assertTrue(VisualWalkthroughChecklist.STEPS.size() >= 8);
		boolean hasGetStarted = false;
		boolean hasDiagram = false;
		boolean hasAnonymize = false;
		for (VisualWalkthroughChecklist.Step step : VisualWalkthroughChecklist.STEPS) {
			assertFalse(step.description().isBlank());
			if (step.area() == ReleaseIssue.Area.GET_STARTED) {
				hasGetStarted = true;
			}
			if (step.area() == ReleaseIssue.Area.DIAGRAM) {
				hasDiagram = true;
			}
			if (step.area() == ReleaseIssue.Area.ANONYMIZE) {
				hasAnonymize = true;
			}
		}
		assertTrue(hasGetStarted && hasDiagram && hasAnonymize);
	}
}
