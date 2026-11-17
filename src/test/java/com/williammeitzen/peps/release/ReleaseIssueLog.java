package com.williammeitzen.peps.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe list of readiness findings. Tests and manual/computer-use notes
 * can append here; {@link #writeMarkdown(Path)} dumps a checklist for triage.
 */
public final class ReleaseIssueLog {

	private static final ReleaseIssueLog INSTANCE = new ReleaseIssueLog();

	private final List<ReleaseIssue> issues = new CopyOnWriteArrayList<>();

	public static ReleaseIssueLog get() {
		return INSTANCE;
	}

	public void clear() {
		issues.clear();
	}

	public void add(ReleaseIssue issue) {
		issues.add(Objects.requireNonNull(issue));
	}

	public void add(ReleaseIssue.Severity severity, ReleaseIssue.Area area, String title, String detail) {
		add(new ReleaseIssue(severity, area, title, detail));
	}

	public List<ReleaseIssue> snapshot() {
		return Collections.unmodifiableList(new ArrayList<>(issues));
	}

	public int size() {
		return issues.size();
	}

	public long count(ReleaseIssue.Severity severity) {
		return issues.stream().filter(i -> i.severity() == severity).count();
	}

	public String toMarkdown() {
		StringBuilder sb = new StringBuilder();
		sb.append("# PEPS public-readiness issues\n\n");
		sb.append("Generated for triage before switching the repo from private to public.\n\n");
		if (issues.isEmpty()) {
			sb.append("_No issues recorded._\n");
			return sb.toString();
		}
		sb.append("| Severity | Area | Title | Detail |\n");
		sb.append("|---|---|---|---|\n");
		for (ReleaseIssue i : issues) {
			sb.append("| ").append(i.severity())
					.append(" | ").append(i.area())
					.append(" | ").append(escape(i.title()))
					.append(" | ").append(escape(i.detail()))
					.append(" |\n");
		}
		sb.append("\n## Checklist\n\n");
		for (ReleaseIssue i : issues) {
			sb.append("- [ ] **").append(i.severity()).append("** ")
					.append(i.title()).append("\n");
		}
		return sb.toString();
	}

	public Path writeMarkdown(Path path) throws IOException {
		Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());
		Files.writeString(path, toMarkdown(), StandardCharsets.UTF_8);
		return path;
	}

	private static String escape(String s) {
		return s.replace("|", "\\|").replace("\n", " ");
	}
}
