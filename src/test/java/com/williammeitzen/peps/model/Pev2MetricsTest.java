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
}
