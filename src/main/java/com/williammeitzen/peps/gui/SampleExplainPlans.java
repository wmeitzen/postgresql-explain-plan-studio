package com.williammeitzen.peps.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Built-in sample EXPLAIN plan text for the Documents empty-state landing screen.
 * Offline stand-ins so new users can open a diagram without a live Postgres connection.
 * Sample JSON and text plans ship the EXPLAIN SQL that produced each capture.
 */
public final class SampleExplainPlans {

	private static final String JSON_SAMPLE_RESOURCE = "/samples/sample-json-explain.json";
	private static final String JSON_EXPLAIN_SQL_RESOURCE = "/samples/sample-json-explain.sql";
	private static final String TEXT_SAMPLE_RESOURCE = "/samples/sample-text-explain.txt";
	private static final String TEXT_EXPLAIN_SQL_RESOURCE = "/samples/sample-text-explain.sql";

	private static volatile String cachedJsonPlan;
	private static volatile String cachedJsonExplainSql;
	private static volatile String cachedTextPlan;
	private static volatile String cachedTextExplainSql;

	private SampleExplainPlans() {
	}

	/** Real JSON EXPLAIN ANALYZE output for the {@code all_cellline} view query. */
	public static String jsonPlanText() {
		String cached = cachedJsonPlan;
		if (cached != null) {
			return cached;
		}
		synchronized (SampleExplainPlans.class) {
			if (cachedJsonPlan == null) {
				cachedJsonPlan = loadClasspathUtf8(JSON_SAMPLE_RESOURCE);
			}
			return cachedJsonPlan;
		}
	}

	/** EXPLAIN SQL shown with the Sample JSON plan ({@code all_cellline} view query). */
	public static String jsonExplainSql() {
		String cached = cachedJsonExplainSql;
		if (cached != null) {
			return cached;
		}
		synchronized (SampleExplainPlans.class) {
			if (cachedJsonExplainSql == null) {
				cachedJsonExplainSql = loadClasspathUtf8(JSON_EXPLAIN_SQL_RESOURCE);
			}
			return cachedJsonExplainSql;
		}
	}

	/** Real text EXPLAIN ANALYZE output for the {@code all_cellline} view query. */
	public static String textPlanText() {
		String cached = cachedTextPlan;
		if (cached != null) {
			return cached;
		}
		synchronized (SampleExplainPlans.class) {
			if (cachedTextPlan == null) {
				cachedTextPlan = loadClasspathUtf8(TEXT_SAMPLE_RESOURCE);
			}
			return cachedTextPlan;
		}
	}

	/** EXPLAIN SQL shown with the Sample text plan ({@code all_cellline} view query). */
	public static String textExplainSql() {
		String cached = cachedTextExplainSql;
		if (cached != null) {
			return cached;
		}
		synchronized (SampleExplainPlans.class) {
			if (cachedTextExplainSql == null) {
				cachedTextExplainSql = loadClasspathUtf8(TEXT_EXPLAIN_SQL_RESOURCE);
			}
			return cachedTextExplainSql;
		}
	}

	private static String loadClasspathUtf8(String resourcePath) {
		try (InputStream in = SampleExplainPlans.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new IllegalStateException("Missing sample resource: " + resourcePath);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load sample resource: " + resourcePath, e);
		}
	}
}
