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

	/** Default Rows chip: Actual vs Estimated factor ≥ this (Settings-overridable). */
	public static final double ROWS_WARNING_FACTOR = 10.0;

	/** Default Filter chip: removed share ≥ this percent (Settings-overridable). */
	public static final double FILTER_WARNING_PERCENT = 50.0;

	/** Default shared gate: indicators require Actual calculated rows ≥ this (Settings-overridable). */
	public static final double WARNING_MIN_ACTUAL_ROWS = 5000.0;

	/** @deprecated use {@link #WARNING_MIN_ACTUAL_ROWS} */
	public static final double FILTER_WARNING_MIN_ACTUAL_ROWS = WARNING_MIN_ACTUAL_ROWS;

	public static final String METRIC_ROWS = "rows";
	public static final String METRIC_MASS = "mass";
	public static final String MASS_UNIT_KB = "kB";
	public static final String MASS_UNIT_MB = "MB";
	public static final String MASS_UNIT_GB = "GB";

	/** Default Cost chip: among the top N by Cost % (Settings-overridable). */
	public static final int COST_WARNING_TOP_N = 2;

	/** Default Time chip: among the top N by Opr time % (Settings-overridable). */
	public static final int TIME_WARNING_TOP_N = 2;

	/** Default Heap chip: fetch share ≥ this % (0 ⇒ any positive share; Settings-overridable). */
	public static final double HEAP_WARNING_PERCENT = 0.0;

	private OperatorWarnings() {
	}

	public static boolean warnTime(boolean highlight1, boolean highlight2) {
		return highlight1 || highlight2;
	}

	/** Legacy PEPS top-1/top-2 highlight flags (tests / fallback). */
	public static boolean warnCost(boolean highlight1, boolean highlight2) {
		return highlight1 || highlight2;
	}

	/** Shared Settings gate for every warning indicator (rows metric). */
	public static boolean meetsMinActualRows(Double totalActualRows, double minActualRows) {
		return totalActualRows != null && totalActualRows.doubleValue() >= minActualRows;
	}

	/** Bytes for one Settings mass unit (1024-based, matching diagram mass display). */
	public static double massUnitToBytes(String massUnit) {
		if (MASS_UNIT_GB.equalsIgnoreCase(massUnit)) {
			return 1024.0 * 1024.0 * 1024.0;
		}
		if (MASS_UNIT_MB.equalsIgnoreCase(massUnit)) {
			return 1024.0 * 1024.0;
		}
		return 1024.0; // kB
	}

	/**
	 * Shared Settings gate: Actual calculated rows, or Actual calculated mass
	 * (bytes ≥ amount × unit).
	 */
	public static boolean meetsMinCalculated(String metric, Double totalActualRows,
			Double actualDataSizeBytes, double minAmount, String massUnit) {
		if (METRIC_MASS.equalsIgnoreCase(metric)) {
			if (actualDataSizeBytes == null) {
				return false;
			}
			return actualDataSizeBytes.doubleValue() >= minAmount * massUnitToBytes(massUnit);
		}
		return meetsMinActualRows(totalActualRows, minAmount);
	}

	/** Actual vs Estimated ratio for rows or mass (same factor math). */
	public static boolean warnEstimateRatio(Double actual, Double estimated, double factorOrMore) {
		Double factor = Pev2Metrics.plannerEstimateFactor(actual, estimated);
		return factor != null && factor.doubleValue() >= factorOrMore;
	}

	/**
	 * True when {@code percent} is among the top {@code topN} distinct % ranks
	 * (ties share a rank; value ≥ the Nth-highest unique value).
	 */
	public static boolean warnAmongTopPercents(Double percent, java.util.Collection<Double> allPercents,
			int topN) {
		if (percent == null || allPercents == null || topN <= 0) {
			return false;
		}
		java.util.TreeSet<Double> uniqueDesc = new java.util.TreeSet<>(java.util.Comparator.reverseOrder());
		for (Double pct : allPercents) {
			if (pct != null) {
				uniqueDesc.add(pct);
			}
		}
		if (uniqueDesc.isEmpty()) {
			return false;
		}
		int rank = 0;
		Double cutoff = null;
		for (Double pct : uniqueDesc) {
			rank++;
			cutoff = pct;
			if (rank >= topN) {
				break;
			}
		}
		return cutoff != null && percent.doubleValue() >= cutoff.doubleValue();
	}

	/** Cost indicator: Cost % among the top {@code topN} distinct ranks. */
	public static boolean warnCostAmongTop(Double costPercent, java.util.Collection<Double> allCostPercents,
			int topN) {
		return warnAmongTopPercents(costPercent, allCostPercents, topN);
	}

	/** Time indicator: Opr time % among the top {@code topN} distinct ranks. */
	public static boolean warnTimeAmongTop(Double timePercent, java.util.Collection<Double> allTimePercents,
			int topN) {
		return warnAmongTopPercents(timePercent, allTimePercents, topN);
	}

	/** Uses default Settings threshold ({@link #ROWS_WARNING_FACTOR}). */
	public static boolean warnRows(Double actualRowsPerLoop, Double planRowsPerLoop) {
		return warnRows(actualRowsPerLoop, planRowsPerLoop, ROWS_WARNING_FACTOR);
	}

	/** Rows indicator when Actual vs Estimated factor is {@code factorOrMore}× or higher. */
	public static boolean warnRows(Double actualRowsPerLoop, Double planRowsPerLoop, double factorOrMore) {
		Double factor = Pev2Metrics.plannerEstimateFactor(actualRowsPerLoop, planRowsPerLoop);
		return factor != null && factor.doubleValue() >= factorOrMore;
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

	/** Uses default Filter % and shared min Actual rows. */
	public static boolean warnFilter(Double removedRevised, Double actualRevised) {
		return warnFilter(removedRevised, actualRevised, FILTER_WARNING_PERCENT, WARNING_MIN_ACTUAL_ROWS);
	}

	/**
	 * Filter indicator when removed share is {@code percentOrMore}% or higher and
	 * Actual rows meets {@code minActualRows}.
	 */
	public static boolean warnFilter(Double removedRevised, Double actualRevised,
			double percentOrMore, double minActualRows) {
		if (!meetsMinActualRows(actualRevised, minActualRows)) {
			return false;
		}
		Double pct = filterRemovedPercent(removedRevised, actualRevised);
		return pct != null && pct.doubleValue() >= percentOrMore;
	}

	/** Filter % only (caller applies {@link #meetsMinActualRows}). */
	public static boolean warnFilterPercent(Double removedRevised, Double actualRevised, double percentOrMore) {
		Double pct = filterRemovedPercent(removedRevised, actualRevised);
		return pct != null && pct.doubleValue() >= percentOrMore;
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

	/** Uses default Settings threshold ({@link #HEAP_WARNING_PERCENT}). */
	public static boolean warnHeap(Double heapFetches, Double actualRowsPerLoop,
			Double rowsRemovedByFilterPerLoop, Double rowsRemovedByJoinFilterPerLoop) {
		return warnHeap(heapFetches, actualRowsPerLoop, rowsRemovedByFilterPerLoop,
				rowsRemovedByJoinFilterPerLoop, HEAP_WARNING_PERCENT);
	}

	/**
	 * Heap indicator when fetch share is {@code percentOrMore}% or higher.
	 * Threshold {@code 0} keeps PEV2 behavior (any positive share).
	 */
	public static boolean warnHeap(Double heapFetches, Double actualRowsPerLoop,
			Double rowsRemovedByFilterPerLoop, Double rowsRemovedByJoinFilterPerLoop,
			double percentOrMore) {
		Double pct = heapFetchPercent(heapFetches, actualRowsPerLoop, rowsRemovedByFilterPerLoop,
				rowsRemovedByJoinFilterPerLoop);
		if (pct == null) {
			return false;
		}
		if (percentOrMore <= 0.0) {
			return pct.doubleValue() > 0.0;
		}
		return pct.doubleValue() >= percentOrMore;
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

	/** Hover tooltip for a diagram warning chip. Returns {@code null} for unknown tags. */
	public static String tooltipFor(String tag) {
		if (tag == null || tag.isEmpty()) {
			return null;
		}
		if (TAG_TIME.equals(tag)) {
			return "Opr time % is among highest";
		}
		if (TAG_COST.equals(tag)) {
			return "Cost % is among highest";
		}
		if (TAG_ROWS.equals(tag)) {
			return "Actual vs. Estimate ratio is excessive";
		}
		if (TAG_FILTER.equals(tag)) {
			return "Filter removes high % of rows";
		}
		if (TAG_HEAP.equals(tag)) {
			return "Heap fetch % is high, consider VACUUM";
		}
		return null;
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
