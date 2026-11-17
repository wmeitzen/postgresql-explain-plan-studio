package com.williammeitzen.peps.release;

import java.util.List;

/**
 * Ordered computer-use / manual smoke steps for public readiness. Keep this
 * checklist short and appearance-focused; automated tests cover copy and layout.
 */
public final class VisualWalkthroughChecklist {

	private VisualWalkthroughChecklist() {
	}

	public static final List<Step> STEPS = List.of(
			new Step(ReleaseIssue.Area.GET_STARTED,
					"Documents empty state shows Get Started title and three action cards"),
			new Step(ReleaseIssue.Area.GET_STARTED,
					"Subtitle says “sample EXPLAIN” (not “large sample”)"),
			new Step(ReleaseIssue.Area.PLAN_SOURCE,
					"Sample JSON plan opens Plan Source with a usable plan text area (full height)"),
			new Step(ReleaseIssue.Area.PLAN_SOURCE,
					"Plan text only is selected; SQL/connection chrome is hidden"),
			new Step(ReleaseIssue.Area.PLAN_SOURCE,
					"Show Plan Diagram control is visible and clickable"),
			new Step(ReleaseIssue.Area.DIAGRAM,
					"Diagram renders operators for the sample plan without empty canvas"),
			new Step(ReleaseIssue.Area.METRICS,
					"Cost / time north labels show operator type and percentages where expected"),
			new Step(ReleaseIssue.Area.ANONYMIZE,
					"Enabling Anonymize shows silhouette badges on plan fields (non-zero size)"),
			new Step(ReleaseIssue.Area.ANONYMIZE,
					"Translate tab appears when Anonymize is on"),
			new Step(ReleaseIssue.Area.THEME,
					"Dark/Light/Default themes keep readable contrast on landing + plan source"));

	public record Step(ReleaseIssue.Area area, String description) {
	}
}
