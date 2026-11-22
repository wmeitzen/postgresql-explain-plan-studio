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
	/** Font Awesome {@code fa-scale-balanced} / {@code fa-balance-scale} — actual vs estimated ratio. */
	public static final String TAG_ROWS = "\uF24E";
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

	/** Default shared gate: indicators require calculated rows ≥ this (Settings-overridable). */
	public static final double WARNING_MIN_ACTUAL_ROWS = 500.0;

	/** @deprecated use {@link #WARNING_MIN_ACTUAL_ROWS} */
	public static final double FILTER_WARNING_MIN_ACTUAL_ROWS = WARNING_MIN_ACTUAL_ROWS;

	public static final String METRIC_ROWS = "rows";
	public static final String METRIC_MASS = "mass";
	public static final String MASS_UNIT_KB = "kB";
	public static final String MASS_UNIT_MB = "MB";
	public static final String MASS_UNIT_GB = "GB";

	/** Gate uses Actual calculated rows/mass (default). */
	public static final String BASIS_ACTUAL = "actual";
	/** Gate uses Estimate calculated rows/mass. */
	public static final String BASIS_ESTIMATE = "estimate";

	public static final String BASIS_LABEL_ACTUAL = "Actual calculated";
	public static final String BASIS_LABEL_ESTIMATE = "Estimate calculated";

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
	 * Shared Settings gate: calculated rows, or calculated mass
	 * (bytes ≥ amount × unit). Caller supplies Actual or Estimate values.
	 */
	public static boolean meetsMinCalculated(String metric, Double totalRows,
			Double dataSizeBytes, double minAmount, String massUnit) {
		if (METRIC_MASS.equalsIgnoreCase(metric)) {
			if (dataSizeBytes == null) {
				return false;
			}
			return dataSizeBytes.doubleValue() >= minAmount * massUnitToBytes(massUnit);
		}
		return meetsMinActualRows(totalRows, minAmount);
	}

	/**
	 * Shared Settings gate choosing Actual or Estimate columns per
	 * {@code basis} ({@link #BASIS_ACTUAL} / {@link #BASIS_ESTIMATE}).
	 */
	public static boolean meetsMinCalculated(String basis, String metric,
			Double totalActualRows, Double actualDataSizeBytes,
			Double totalEstimatedRows, Double estimatedDataSizeBytes,
			double minAmount, String massUnit) {
		if (BASIS_ESTIMATE.equalsIgnoreCase(basis)) {
			return meetsMinCalculated(metric, totalEstimatedRows, estimatedDataSizeBytes,
					minAmount, massUnit);
		}
		return meetsMinCalculated(metric, totalActualRows, actualDataSizeBytes,
				minAmount, massUnit);
	}

	public static boolean isEstimateBasis(String basis) {
		return BASIS_ESTIMATE.equalsIgnoreCase(basis);
	}

	public static String normalizeBasis(String basis) {
		return isEstimateBasis(basis) ? BASIS_ESTIMATE : BASIS_ACTUAL;
	}

	public static String basisLabel(String basis) {
		return isEstimateBasis(basis) ? BASIS_LABEL_ESTIMATE : BASIS_LABEL_ACTUAL;
	}

	public static String basisFromLabel(String label) {
		return BASIS_LABEL_ESTIMATE.equals(label) ? BASIS_ESTIMATE : BASIS_ACTUAL;
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

	/**
	 * Filter removed share for Settings metric {@code rows} or {@code mass}.
	 * Mass estimates removed bytes from removed rows × (actual mass / actual rows).
	 */
	public static Double filterRemovedPercent(String metric, Double removedRevised, Double actualRevised,
			Double actualDataSizeBytes) {
		if (METRIC_MASS.equalsIgnoreCase(metric)) {
			if (removedRevised == null || actualDataSizeBytes == null) {
				return null;
			}
			double removedRows = Math.max(0.0, removedRevised.doubleValue());
			double actualRows = actualRevised == null ? 0.0 : Math.max(0.0, actualRevised.doubleValue());
			double actualMass = Math.max(0.0, actualDataSizeBytes.doubleValue());
			if (actualRows <= 0.0) {
				return removedRows > 0.0 ? 100.0 : filterRemovedPercent(removedRevised, actualRevised);
			}
			double removedMass = removedRows * (actualMass / actualRows);
			return filterRemovedPercent(removedMass, actualMass);
		}
		return filterRemovedPercent(removedRevised, actualRevised);
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
		return warnFilterPercent(METRIC_ROWS, removedRevised, actualRevised, null, percentOrMore);
	}

	public static boolean warnFilterPercent(String metric, Double removedRevised, Double actualRevised,
			Double actualDataSizeBytes, double percentOrMore) {
		Double pct = filterRemovedPercent(metric, removedRevised, actualRevised, actualDataSizeBytes);
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
	 * Same selection/order as {@link #formatTags} for Calculated Properties
	 * ({@code Time | Cost | Ratio | Filter | Heap}).
	 */
	public static String formatWarningWords(boolean time, boolean cost, boolean rows, boolean filter,
			boolean heap) {
		StringBuilder sb = new StringBuilder();
		appendWord(sb, time, "Time");
		appendWord(sb, cost, "Cost");
		appendWord(sb, rows, "Ratio");
		appendWord(sb, filter, "Filter");
		appendWord(sb, heap, "Heap");
		return sb.toString();
	}

	/**
	 * Map diagram {@code warning_tags} glyphs to the Warnings property value in the
	 * same left-to-right chip order ({@code Time | Cost | Ratio | Filter | Heap}).
	 */
	public static String formatWarningWordsFromTags(String warningTags) {
		if (warningTags == null || warningTags.isBlank()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		appendWord(sb, tagsContain(warningTags, TAG_TIME), "Time");
		appendWord(sb, tagsContain(warningTags, TAG_COST), "Cost");
		appendWord(sb, tagsContain(warningTags, TAG_ROWS), "Ratio");
		appendWord(sb, tagsContain(warningTags, TAG_FILTER), "Filter");
		appendWord(sb, tagsContain(warningTags, TAG_HEAP), "Heap");
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

	private static void appendWord(StringBuilder sb, boolean on, String word) {
		if (!on) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(" | ");
		}
		sb.append(word);
	}

	public static boolean tagsContain(String warningTags, String tag) {
		if (warningTags == null || warningTags.isEmpty() || tag == null || tag.isEmpty()) {
			return false;
		}
		return warningTags.contains(tag);
	}

	/** Hover tooltip for a diagram warning chip. Returns {@code null} for unknown tags. */
	public static String tooltipFor(String tag) {
		return tooltipFor(tag, null, null, null, null, METRIC_ROWS, null);
	}

	/**
	 * Hover tooltip including measured values when available.
	 * {@code estimateRatio} is display text such as {@code 156:1}; percent fields
	 * are numeric strings without a trailing {@code %}.
	 * {@code filterMetric} is {@link #METRIC_ROWS} or {@link #METRIC_MASS}.
	 */
	public static String tooltipFor(String tag, String timePercent, String costPercent,
			String estimateRatio, String filterPercent, String filterMetric, String heapPercent) {
		if (tag == null || tag.isEmpty()) {
			return null;
		}
		if (TAG_TIME.equals(tag)) {
			if (timePercent != null && !timePercent.isEmpty()) {
				return "Opr time " + timePercent + "%";
			}
			return "Opr time % is among highest";
		}
		if (TAG_COST.equals(tag)) {
			if (costPercent != null && !costPercent.isEmpty()) {
				return "Cost " + costPercent + "%";
			}
			return "Cost % is among highest";
		}
		if (TAG_ROWS.equals(tag)) {
			if (estimateRatio != null && !estimateRatio.isEmpty()) {
				return "Actual vs. Estimated ratio " + estimateRatio;
			}
			return "Actual vs. Estimated ratio is excessive";
		}
		if (TAG_FILTER.equals(tag)) {
			String unit = METRIC_MASS.equalsIgnoreCase(filterMetric) ? "mass" : "rows";
			if (filterPercent != null && !filterPercent.isEmpty()) {
				return "Filter removes " + filterPercent + "% of " + unit;
			}
			return "Filter removes high % of " + unit;
		}
		if (TAG_HEAP.equals(tag)) {
			if (heapPercent != null && !heapPercent.isEmpty()) {
				return "Heap fetch " + heapPercent + "%, consider VACUUM";
			}
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
