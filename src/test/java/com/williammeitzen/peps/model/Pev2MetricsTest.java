package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Fixtures mirror dalibo/pev2 {@code calculateExclusives} behavior, including the
 * Materialize {@code rows=1 loops=321195} case that previously showed 1 in PEPS.
 */
class Pev2MetricsTest {

	@Test
	void revisedRows_multipliesByLoops_materializeCase() {
		assertEquals(321195.0, Pev2Metrics.revisedRows(1.0, 321195.0));
	}

	@Test
	void revisedRows_loopsOne_unchanged() {
		assertEquals(50.0, Pev2Metrics.revisedRows(50.0, 1.0));
	}

	@Test
	void revisedRows_nullLoops_defaultsToOne() {
		assertEquals(50.0, Pev2Metrics.revisedRows(50.0, null));
	}

	@Test
	void revisedRows_neverExecuted_zeroLoops() {
		assertEquals(0.0, Pev2Metrics.revisedRows(100.0, 0.0));
	}

	@Test
	void revisedRows_nullValue_staysNull() {
		assertNull(Pev2Metrics.revisedRows(null, 5.0));
	}

	@Test
	void revisedRows_parallelChild_usesLoopsNotExtraWorkers() {
		// Parallel Seq Scan: rows=62519 loops=5 (workers_launched=4 → loops already 5)
		assertEquals(312595.0, Pev2Metrics.revisedRows(62519.0, 5.0));
		// Must NOT also × (1+4) on top of loops
		assertEquals(312595.0, Pev2Metrics.revisedRows(62519.0, 5.0));
	}

	@Test
	void revisedTotalTime_dividesByWorkersPlusOne() {
		// time=10, loops=5, workers_launched_by_gather=4 → 10*5/5 = 10
		assertEquals(10.0, Pev2Metrics.revisedTotalTimeMs(10.0, 5.0, 4.0));
	}

	@Test
	void revisedTotalTime_nestedLoop_multipliesLoops_noWorkers() {
		assertEquals(50.0, Pev2Metrics.revisedTotalTimeMs(10.0, 5.0, null));
	}

	@Test
	void exclusiveDuration_subtractsChildren() {
		assertEquals(3.0, Pev2Metrics.exclusiveDurationMs(10.0, 7.0));
		assertEquals(0.0, Pev2Metrics.exclusiveDurationMs(10.0, 12.0));
	}

	@Test
	void exclusiveCost_subtractsChildren() {
		assertEquals(5.0, Pev2Metrics.exclusiveCost(20.0, 15.0));
		assertEquals(0.0, Pev2Metrics.exclusiveCost(10.0, 15.0));
	}

	@Test
	void isExcessiveEstimateMismatch_thresholdIsOver100x() {
		assertFalse(Pev2Metrics.isExcessiveEstimateMismatch(100.0, 100.0));
		assertFalse(Pev2Metrics.isExcessiveEstimateMismatch(1000.0, 10.0)); // 100× not >
		assertTrue(Pev2Metrics.isExcessiveEstimateMismatch(1001.0, 10.0)); // >100×
		assertTrue(Pev2Metrics.isExcessiveEstimateMismatch(10.0, 1001.0));
		assertTrue(Pev2Metrics.isExcessiveEstimateMismatch(50.0, 0.0)); // ∞
		assertFalse(Pev2Metrics.isExcessiveEstimateMismatch(null, 10.0));
	}

	@Test
	void costPercentOfPlan_matchesNorthLabelFormula() {
		// exclusive 1081. something / max total 67414 ≈ 1.60% (diagram Cost: 1.6%)
		assertEquals(1.60, Pev2Metrics.costPercentOfPlan(1081.0, 67414.0, 67414.0), 0.01);
		// Pass-through node: exclusive ~0 → Properties Cost % must be 0, not inclusive share
		assertEquals(0.0, Pev2Metrics.costPercentOfPlan(0.0, 50000.0, 67414.0), 1e-9);
		// Missing exclusive falls back to total (same as north coalesce)
		assertEquals(100.0, Pev2Metrics.costPercentOfPlan(null, 67414.0, 67414.0), 1e-9);
		assertNull(Pev2Metrics.costPercentOfPlan(10.0, 10.0, 0.0));
		assertNull(Pev2Metrics.costPercentOfPlan(null, null, 100.0));
	}

	@Test
	void costPercentOfPlan_mustNotUseSumOfTotalsDenominator() {
		// Bug that caused Properties ≠ north: total/sum(all totals) vs exclusive/max
		double exclusive = 0.0;
		double inclusiveTotal = 63952.59;
		double maxTotal = 67414.01;
		double sumAllTotals = 500000.0; // inflated by ancestors
		assertEquals(0.0, Pev2Metrics.costPercentOfPlan(exclusive, inclusiveTotal, maxTotal), 1e-9);
		double wrongPropertiesStyle = inclusiveTotal / sumAllTotals * 100.0;
		assertTrue(wrongPropertiesStyle > 1.0,
				"sanity: old formula looked non-zero while exclusive share is 0");
	}

	@Test
	void exclusiveBlocks_canBeNegative_likePev2() {
		// pev2 does not clamp buffers to zero
		assertEquals(-2.0, Pev2Metrics.exclusiveBlocks(3.0, 5.0));
		assertEquals(2.0, Pev2Metrics.exclusiveBlocks(5.0, 3.0));
	}

	@Test
	void plannerEstimateFactor_usesPerLoopNotRevised() {
		// Display totals may be ×loops; estimate factor stays per-loop
		assertEquals(2.0, Pev2Metrics.plannerEstimateFactor(200.0, 100.0));
		assertEquals(5.0, Pev2Metrics.plannerEstimateFactor(1.0, 5.0));
		assertEquals(1.0, Pev2Metrics.plannerEstimateFactor(10.0, 10.0));
	}

	@Test
	void cteScanExclusiveAfterFix_proportional() {
		// cte=100, two scans exclusive 40 and 60, scan1 revised total 50, sum excl=100
		// scan1 new = 40 - 100*(50/100) = -10 → 0
		assertEquals(0.0, Pev2Metrics.cteScanExclusiveAfterFix(40.0, 100.0, 50.0, 100.0));
		// scan2: 60 - 100*(50/100) = 10  (if same revised — illustrative)
		assertEquals(10.0, Pev2Metrics.cteScanExclusiveAfterFix(60.0, 100.0, 50.0, 100.0));
	}

	@Test
	void rowsTotalIsExact_falseWhenLoopsGreaterThanOne() {
		assertTrue(Pev2Metrics.rowsTotalIsExact(1.0));
		assertTrue(Pev2Metrics.rowsTotalIsExact(null));
		assertFalse(Pev2Metrics.rowsTotalIsExact(321195.0));
	}

	@Test
	void initPlanRefToken_prefersDollarParam() {
		assertEquals("$1", Pev2Metrics.initPlanRefToken("InitPlan 2 (returns $1)"));
		assertEquals("InitPlan 2", Pev2Metrics.initPlanRefToken("InitPlan 2"));
	}

	@Test
	void materializeAndParallelParity_endToEndFormulas() {
		// Materialize #446-style: rows=1 loops=321195 → pev2 shows 321195
		assertEquals(321195.0, Pev2Metrics.revisedRows(1.0, 321195.0));
		assertFalse(Pev2Metrics.rowsTotalIsExact(321195.0));

		// Nested Loop inner index: time=0.9 loops=5000 → revised 4500; exclusive after child 0
		assertEquals(4500.0, Pev2Metrics.revisedTotalTimeMs(0.9, 5000.0, null));
		assertEquals(4500.0, Pev2Metrics.exclusiveDurationMs(4500.0, 0.0));

		// Parallel child under Gather (4 workers launched): loops=5, time=10 → revised 10
		assertEquals(10.0, Pev2Metrics.revisedTotalTimeMs(10.0, 5.0, 4.0));
		assertEquals(312595.0, Pev2Metrics.revisedRows(62519.0, 5.0));

		// Cost exclusive
		assertEquals(2.0, Pev2Metrics.exclusiveCost(12.34, 10.34));
	}
}
