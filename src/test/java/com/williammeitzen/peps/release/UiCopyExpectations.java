package com.williammeitzen.peps.release;

/**
 * Canonical user-facing copy for public-readiness checks. Keep in sync with the
 * Get Started / Plan Source UI; tests fail when product strings drift.
 */
public final class UiCopyExpectations {

	private UiCopyExpectations() {
	}

	public static final String GET_STARTED_TITLE = "Get Started";

	public static final String GET_STARTED_SUBTITLE =
			"Create a blank plan, or open a sample EXPLAIN to explore the diagram.";

	public static final String NEW_BLANK_PLAN = "New blank plan";
	public static final String SAMPLE_JSON_PLAN = "Sample JSON plan";
	public static final String SAMPLE_TEXT_PLAN = "Sample text plan";

	public static final String SHOW_PLAN_TEXT_ONLY = "Plan text only";
	public static final String SHOW_CONNECTION_AND_PLAN = "Connection info and plan";

	public static final String ANONYMIZE = "Anonymize";
	public static final String ANONYMIZE_ALIASES = "Anonymize aliases";
	public static final String ANONYMIZE_BADGE_TIP = "Showing anonymized text";

	public static final String PLAN_FIELD_JSON_LABEL = "JSON plan:";
	public static final String PLAN_FIELD_TEXT_LABEL = "Text plan:";
	public static final String PLAN_FIELD_GENERIC_LABEL = "Text plan or JSON plan:";

	public static final String SQL_FIELD_LABEL = "EXPLAIN (with options) and SQL command:";

	/** Phrases that should not appear in shipping UI copy. */
	public static final String[] FORBIDDEN_UI_PHRASES = {
			"open a large sample EXPLAIN",
			"TODO",
			"FIXME",
			"XXX",
	};
}
