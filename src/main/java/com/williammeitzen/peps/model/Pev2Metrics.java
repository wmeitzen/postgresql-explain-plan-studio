package com.williammeitzen.peps.model;

/**
 * Pure PEV2-aligned metric formulas (dalibo/pev2 {@code PlanService.calculateExclusives}
 * and related helpers). Used so PEPS operator totals match explain.dalibo.com /
 * pev2 node-for-node, and so the math can be unit-tested without Swing/SQLite.
 *
 * <p>Row totals multiply by {@code Actual Loops} only — not by
 * {@code (1 + workers)}. In parallel plans Postgres already folds workers into
 * {@code Actual Loops} (typically {@code workers_launched + 1}).
 *
 * <p>Timing divides by {@code workers_launched_by_gather + 1} after × loops
 * because wall-clock is overlapped across workers.
 */
public final class Pev2Metrics {

	private Pev2Metrics() {
	}

	/** Effective loop multiplier: missing → 1; never-executed 0 stays 0. */
	public static double loopsOrOne(Double actualLoops) {
		if (actualLoops == null) {
			return 1.0;
		}
		return actualLoops.doubleValue();
	}

	/**
	 * PEV2 {@code *_REVISED} for row-like metrics:
	 * {@code value * (Actual Loops || 1)}.
	 */
	public static Double revisedRows(Double perLoopValue, Double actualLoops) {
		if (perLoopValue == null) {
			return null;
		}
		return perLoopValue.doubleValue() * loopsOrOne(actualLoops);
	}

	/**
	 * Workers denominator for revised timing: {@code (Workers Launched By Gather || 0) + 1}.
	 */
	public static double workersDenominator(Double workersLaunchedByGather) {
		double workers = workersLaunchedByGather == null ? 0.0 : workersLaunchedByGather.doubleValue();
		if (workers < 0) {
			workers = 0;
		}
		return workers + 1.0;
	}

	/**
	 * PEV2 revised inclusive time:
	 * {@code Actual Total Time * Actual Loops / (workers_launched_by_gather + 1)}.
	 */
	public static Double revisedTotalTimeMs(Double actualTotalTimeMs, Double actualLoops,
			Double workersLaunchedByGather) {
		if (actualTotalTimeMs == null) {
			return null;
		}
		return (actualTotalTimeMs.doubleValue() * loopsOrOne(actualLoops))
				/ workersDenominator(workersLaunchedByGather);
	}

	/**
	 * PEV2 revised startup time (same scaling as total time).
	 */
	public static Double revisedStartupTimeMs(Double actualStartupTimeMs, Double actualLoops,
			Double workersLaunchedByGather) {
		if (actualStartupTimeMs == null) {
			return null;
		}
		return (actualStartupTimeMs.doubleValue() * loopsOrOne(actualLoops))
				/ workersDenominator(workersLaunchedByGather);
	}

	/**
	 * PEV2 exclusive duration: {@code max(0, revisedTotal - sum(childrenRevisedTotals))}.
	 * Callers must exclude InitPlan children (except Result←InitPlan) the same way pev2 does.
	 */
	public static Double exclusiveDurationMs(Double revisedTotalTimeMs, double childrenRevisedTotalSum) {
		if (revisedTotalTimeMs == null) {
			return null;
		}
		double exclusive = revisedTotalTimeMs.doubleValue() - childrenRevisedTotalSum;
		return exclusive > 0 ? exclusive : 0.0;
	}

	/**
	 * PEV2 exclusive cost: {@code max(0, totalCost - sum(child totalCost))}.
	 */
	public static Double exclusiveCost(Double totalCost, double childrenTotalCostSum) {
		if (totalCost == null) {
			return null;
		}
		double exclusive = totalCost.doubleValue() - childrenTotalCostSum;
		return exclusive > 0 ? exclusive : 0.0;
	}

	/**
	 * PEV2 cost share for diagram north labels and Calculated Properties
	 * {@code Cost %}: {@code exclusiveCost / max(Total Cost)} as a percent
	 * (0–100). Falls back to {@code totalCost} when exclusive is missing so
	 * labels never go blank after a partial annotation update.
	 *
	 * <p>Must not use inclusive {@code totalCost / sum(all totalCosts)} — that
	 * double-counts ancestors and diverges from diagram {@code Cost: N%} text.
	 */
	public static Double costPercentOfPlan(Double exclusiveCost, Double totalCostFallback,
			Double maxTotalCost) {
		if (maxTotalCost == null || maxTotalCost.doubleValue() == 0.0) {
			return null;
		}
		Double numerator = exclusiveCost != null ? exclusiveCost : totalCostFallback;
		if (numerator == null) {
			return null;
		}
		return numerator.doubleValue() / maxTotalCost.doubleValue() * 100.0;
	}

	/**
	 * PEV2 exclusive buffer/IO: {@code inclusive - sum(non-subplan children inclusive)}.
	 */
	public static Double exclusiveBlocks(Double inclusive, double childrenInclusiveSum) {
		if (inclusive == null) {
			return null;
		}
		return inclusive.doubleValue() - childrenInclusiveSum;
	}

	/**
	 * PEV2 planner estimate factor uses raw (per-loop) Actual Rows / Plan Rows,
	 * not revised totals. Direction: under if actual &gt; plan, over if actual &lt; plan.
	 *
	 * @return factor always ≥ 1 when both sides are present and non-zero; null if either missing
	 */
	public static Double plannerEstimateFactor(Double actualRowsPerLoop, Double planRowsPerLoop) {
		if (actualRowsPerLoop == null || planRowsPerLoop == null) {
			return null;
		}
		double actual = actualRowsPerLoop.doubleValue();
		double planned = planRowsPerLoop.doubleValue();
		if (planned == 0.0) {
			return actual == 0.0 ? 1.0 : Double.POSITIVE_INFINITY;
		}
		if (actual == planned) {
			return 1.0;
		}
		if (actual > planned) {
			return actual / planned;
		}
		return planned / actual;
	}

	/**
	 * PEV2 CTE-scan exclusive adjustment: subtract a proportional share of the CTE's
	 * revised duration from each CTE Scan's exclusive duration.
	 *
	 * {@code newExclusive = max(0, scanExclusive - cteDuration * scanRevisedTotal / sumScanExclusives)}
	 */
	public static double cteScanExclusiveAfterFix(double scanExclusive, double cteRevisedDuration,
			double scanRevisedTotal, double sumScanExclusives) {
		if (sumScanExclusives <= 0) {
			return Math.max(0.0, scanExclusive);
		}
		return Math.max(0.0,
				scanExclusive - (cteRevisedDuration * scanRevisedTotal / sumScanExclusives));
	}

	/**
	 * PEV2 marks multi-loop row counts approximative when Actual Rows is an average
	 * (non-fractional integer rows with loops &gt; 1).
	 */
	public static boolean rowsTotalIsExact(Double actualLoops) {
		if (actualLoops == null) {
			return true;
		}
		return actualLoops.doubleValue() <= 1.0;
	}

	/**
	 * Token used by pev2 {@code fixInitPlanUsageDuration} to find consumers of an
	 * InitPlan: prefer {@code $N} from {@code InitPlan k (returns $N)}, else the
	 * {@code InitPlan k} label.
	 */
	public static String initPlanRefToken(String subplanName) {
		if (subplanName == null) {
			return null;
		}
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile("\\(returns\\s+(\\$[0-9]+)\\)", java.util.regex.Pattern.CASE_INSENSITIVE)
				.matcher(subplanName);
		if (m.find()) {
			return m.group(1);
		}
		m = java.util.regex.Pattern.compile("(InitPlan\\s+[1-9][0-9]*)", java.util.regex.Pattern.CASE_INSENSITIVE)
				.matcher(subplanName);
		if (m.find()) {
			return m.group(1);
		}
		return subplanName.trim();
	}
}
