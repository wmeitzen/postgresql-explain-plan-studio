package com.williammeitzen.peps.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Built-in sample EXPLAIN plan text for the Documents empty-state landing screen.
 * Offline stand-ins so new users can open a diagram without a live Postgres connection.
 * The EXPLAIN SQL field is left empty (no database to run against).
 */
public final class SampleExplainPlans {

	private static final String JSON_SAMPLE_RESOURCE = "/samples/gxd_genotype_summary_view-explain.json";
	private static final String TEXT_SAMPLE_RESOURCE = "/samples/gxd_genotype_summary_view-explain.txt";

	private static volatile String cachedJsonPlan;
	private static volatile String cachedTextPlan;

	private SampleExplainPlans() {
	}

	/** Real JSON EXPLAIN ANALYZE output for {@code gxd_genotype_summary_view}. */
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

	/** Real text EXPLAIN ANALYZE output for {@code gxd_genotype_summary_view}. */
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
