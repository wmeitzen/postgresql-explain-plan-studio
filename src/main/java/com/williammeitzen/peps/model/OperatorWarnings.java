package com.williammeitzen.peps.model;

/**
 * Dashboard warning tags painted above diagram north labels and used by Find.
 * Time/Cost follow PEPS top-1/top-2 percentage highlights; Rows/Filter/Heap follow
 * PEV2 badge-presence thresholds from {@code node.ts}.
 */
public final class OperatorWarnings {

	/** Font Awesome {@code fa-clock} — operator time warning. */
	public static final String TAG_TIME = "\uF017";
	/** Dollar sign — cost warning. */
	public static final String TAG_COST = "$";
	/** Font Awesome {@code fa-thumbs-down} — actual vs estimated rows. */
	public static final String TAG_ROWS = "\uF165";
	/**
	 * Font Awesome Free Solid {@code fa-filter} (U+F0B0) — rows-removed warning.
	 * Requires {@code /fonts/fa-solid-900.ttf} when painting.
	 */
	public static final String TAG_FILTER = "\uF0B0";
	/** Font Awesome {@code fa-recycle} — heap fetches. */
	public static final String TAG_HEAP = "\uF1B8";

	/** PEV2 {@code estimationClass} starts at factor &gt; 10. */
	public static final double ROWS_WARNING_FACTOR = 10.0;

	/** PEV2 {@code rowsRemovedClass} starts at &gt; 50%. */
	public static final double FILTER_WARNING_PERCENT = 50.0;

	/** PEV2 {@code heapFetchesClass} starts at share &gt; 0%. */
	public static final double HEAP_WARNING_PERCENT = 0.0;

	private OperatorWarnings() {
	}

	public static boolean warnTime(boolean highlight1, boolean highlight2) {
		return highlight1 || highlight2;
	}

	public static boolean warnCost(boolean highlight1, boolean highlight2) {
		return highlight1 || highlight2;
	}

	public static boolean warnRows(Double actualRowsPerLoop, Double planRowsPerLoop) {
		Double factor = Pev2Metrics.plannerEstimateFactor(actualRowsPerLoop, planRowsPerLoop);
		return factor != null && factor.doubleValue() > ROWS_WARNING_FACTOR;
	}

	/**
	 * PEV2 filter share: {@code floor(removed / (removed + actual) * 100)} using
	 * revised totals when available.
	 */
	public static Double filterRemovedPercent(Double removedRevised, Double actualRevised) {
		if (removedRevised == null) {
			return null;
		}
		double removed = Math.max(0.0, removedRevised.doubleValue());
		double actual = actualRevised == null ? 0.0 : Math.max(0.0, actualRevised.doubleValue());
		double denom = removed + actual;
		if (denom <= 0.0) {
			return null;
		}
		return Math.floor(removed / denom * 100.0);
	}

	public static boolean warnFilter(Double removedRevised, Double actualRevised) {
		Double pct = filterRemovedPercent(removedRevised, actualRevised);
		return pct != null && pct.doubleValue() > FILTER_WARNING_PERCENT;
	}

	/**
	 * Prefer the largest revised removed count among filter / join filter / index recheck.
	 */
	public static Double primaryRemovedRevised(Double filter, Double joinFilter, Double indexRecheck) {
		Double best = null;
		best = maxNullable(best, filter);
		best = maxNullable(best, joinFilter);
		best = maxNullable(best, indexRecheck);
		return best;
	}

	/**
	 * PEV2 heap-fetch share uses per-loop Actual Rows and per-loop filter removals
	 * (not join-filter alone in the documented formula; join filter is included).
	 */
	public static Double heapFetchPercent(Double heapFetches, Double actualRowsPerLoop,
			Double rowsRemovedByFilterPerLoop, Double rowsRemovedByJoinFilterPerLoop) {
		if (heapFetches == null) {
			return null;
		}
		double heap = Math.max(0.0, heapFetches.doubleValue());
		double actual = actualRowsPerLoop == null ? 0.0 : Math.max(0.0, actualRowsPerLoop.doubleValue());
		double filter = rowsRemovedByFilterPerLoop == null ? 0.0
				: Math.max(0.0, rowsRemovedByFilterPerLoop.doubleValue());
		double join = rowsRemovedByJoinFilterPerLoop == null ? 0.0
				: Math.max(0.0, rowsRemovedByJoinFilterPerLoop.doubleValue());
		double denom = actual + filter + join;
		if (denom <= 0.0) {
			return heap > 0.0 ? Double.POSITIVE_INFINITY : 0.0;
		}
		return heap / denom * 100.0;
	}

	public static boolean warnHeap(Double heapFetches, Double actualRowsPerLoop,
			Double rowsRemovedByFilterPerLoop, Double rowsRemovedByJoinFilterPerLoop) {
		Double pct = heapFetchPercent(heapFetches, actualRowsPerLoop, rowsRemovedByFilterPerLoop,
				rowsRemovedByJoinFilterPerLoop);
		return pct != null && pct.doubleValue() > HEAP_WARNING_PERCENT;
	}

	/** Concatenate active diagram glyphs in Find-checkbox order, space-separated. */
	public static String formatTags(boolean time, boolean cost, boolean rows, boolean filter, boolean heap) {
		StringBuilder sb = new StringBuilder();
		appendToken(sb, time, TAG_TIME);
		appendToken(sb, cost, TAG_COST);
		appendToken(sb, rows, TAG_ROWS);
		appendToken(sb, filter, TAG_FILTER);
		appendToken(sb, heap, TAG_HEAP);
		return sb.toString();
	}

	/**
	 * Same selection as {@link #formatTags} but with single words for Calculated
	 * Properties ({@code Time Cost Rows Filter Heap}).
	 */
	public static String formatWarningWords(boolean time, boolean cost, boolean rows, boolean filter,
			boolean heap) {
		StringBuilder sb = new StringBuilder();
		appendToken(sb, time, "Time");
		appendToken(sb, cost, "Cost");
		appendToken(sb, rows, "Rows");
		appendToken(sb, filter, "Filter");
		appendToken(sb, heap, "Heap");
		return sb.toString();
	}

	private static void appendToken(StringBuilder sb, boolean on, String token) {
		if (!on) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(' ');
		}
		sb.append(token);
	}

	public static boolean tagsContain(String warningTags, String tag) {
		if (warningTags == null || warningTags.isEmpty() || tag == null || tag.isEmpty()) {
			return false;
		}
		return warningTags.contains(tag);
	}

	private static Double maxNullable(Double a, Double b) {
		if (b == null) {
			return a;
		}
		if (a == null) {
			return b;
		}
		return Math.max(a.doubleValue(), b.doubleValue());
	}
}
