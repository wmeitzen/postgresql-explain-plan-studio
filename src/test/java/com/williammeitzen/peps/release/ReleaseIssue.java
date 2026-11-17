package com.williammeitzen.peps.release;

import java.time.Instant;
import java.util.Objects;

/**
 * One public-readiness finding (functional or visual). Collected by smoke tests
 * and computer-use walkthroughs so fixes can be prioritized before going public.
 */
public final class ReleaseIssue {

	public enum Severity {
		BLOCKER,
		MAJOR,
		MINOR,
		POLISH
	}

	public enum Area {
		GET_STARTED,
		PLAN_SOURCE,
		DIAGRAM,
		ANONYMIZE,
		THEME,
		METRICS,
		DOCS_PACKAGING,
		OTHER
	}

	private final Severity severity;
	private final Area area;
	private final String title;
	private final String detail;
	private final Instant foundAt;

	public ReleaseIssue(Severity severity, Area area, String title, String detail) {
		this.severity = Objects.requireNonNull(severity);
		this.area = Objects.requireNonNull(area);
		this.title = Objects.requireNonNull(title);
		this.detail = detail == null ? "" : detail;
		this.foundAt = Instant.now();
	}

	public Severity severity() {
		return severity;
	}

	public Area area() {
		return area;
	}

	public String title() {
		return title;
	}

	public String detail() {
		return detail;
	}

	public Instant foundAt() {
		return foundAt;
	}

	@Override
	public String toString() {
		return "[" + severity + "/" + area + "] " + title
				+ (detail.isEmpty() ? "" : " — " + detail);
	}
}
