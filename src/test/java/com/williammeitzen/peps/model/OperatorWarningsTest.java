package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OperatorWarningsTest {

	@Test
	void timeAndCost_followPepsTopTwoHighlights() {
		assertFalse(OperatorWarnings.warnTime(false, false));
		assertTrue(OperatorWarnings.warnTime(true, false));
		assertTrue(OperatorWarnings.warnTime(false, true));
		assertTrue(OperatorWarnings.warnCost(true, false));
		assertFalse(OperatorWarnings.warnCost(false, false));
	}

	@Test
	void rows_requiresFactorOrMore() {
		assertTrue(OperatorWarnings.warnRows(100.0, 10.0)); // 10× ≥ default 10
		assertFalse(OperatorWarnings.warnRows(99.0, 10.0)); // 9.9×
		assertTrue(OperatorWarnings.warnRows(10.0, 101.0));
		assertFalse(OperatorWarnings.warnRows(null, 10.0));
		assertFalse(OperatorWarnings.warnRows(100.0, 10.0, 11.0));
		assertTrue(OperatorWarnings.warnRows(100.0, 10.0, 10.0));
	}

	@Test
	void filter_requiresPercentOrMoreAndMinActualRows() {
		assertEquals(50.0, OperatorWarnings.filterRemovedPercent(50.0, 50.0));
		assertEquals(90.0, OperatorWarnings.filterRemovedPercent(900.0, 100.0));
		// Defaults: ≥50% and Actual rows ≥ 5000
		assertFalse(OperatorWarnings.warnFilter(900.0, 100.0)); // below min rows
		assertFalse(OperatorWarnings.warnFilter(4000.0, 6000.0)); // 40% < 50%
		assertTrue(OperatorWarnings.warnFilter(5000.0, 5000.0)); // 50% and 5000 rows
		assertTrue(OperatorWarnings.warnFilter(9000.0, 1000.0, 50.0, 500.0)); // custom thresholds
		assertFalse(OperatorWarnings.warnFilter(9000.0, 1000.0, 50.0, 5001.0));
		assertTrue(OperatorWarnings.warnFilterPercent(5000.0, 5000.0, 50.0));
		assertFalse(OperatorWarnings.meetsMinActualRows(4999.0, 5000.0));
		assertTrue(OperatorWarnings.meetsMinActualRows(5000.0, 5000.0));
		assertTrue(OperatorWarnings.meetsMinCalculated(OperatorWarnings.METRIC_ROWS,
				5000.0, null, 5000.0, OperatorWarnings.MASS_UNIT_MB));
		assertFalse(OperatorWarnings.meetsMinCalculated(OperatorWarnings.METRIC_MASS,
				5000.0, 1024.0 * 1024.0, 2.0, OperatorWarnings.MASS_UNIT_MB)); // 1MB < 2MB
		assertTrue(OperatorWarnings.meetsMinCalculated(OperatorWarnings.METRIC_MASS,
				1.0, 2.0 * 1024.0 * 1024.0, 2.0, OperatorWarnings.MASS_UNIT_MB));
		assertTrue(OperatorWarnings.warnEstimateRatio(100.0, 10.0, 10.0));
	}

	@Test
	void costAndTime_amongTopN_includesTiesAtCutoff() {
		java.util.List<Double> all = java.util.Arrays.asList(50.0, 40.0, 30.0, 40.0);
		assertTrue(OperatorWarnings.warnCostAmongTop(50.0, all, 2));
		assertTrue(OperatorWarnings.warnCostAmongTop(40.0, all, 2));
		assertFalse(OperatorWarnings.warnCostAmongTop(30.0, all, 2));
		assertTrue(OperatorWarnings.warnCostAmongTop(30.0, all, 3));
		assertFalse(OperatorWarnings.warnCostAmongTop(null, all, 2));
		assertTrue(OperatorWarnings.warnTimeAmongTop(40.0, all, 2));
		assertFalse(OperatorWarnings.warnTimeAmongTop(30.0, all, 2));
	}

	@Test
	void heap_matchesPev2HeapFetchesClassPresence() {
		assertFalse(OperatorWarnings.warnHeap(0.0, 100.0, 0.0, 0.0));
		assertTrue(OperatorWarnings.warnHeap(1.0, 100.0, 0.0, 0.0));
		assertEquals(50.0, OperatorWarnings.heapFetchPercent(50.0, 100.0, 0.0, 0.0));
		assertFalse(OperatorWarnings.warnHeap(5.0, 100.0, 0.0, 0.0, 10.0)); // 5% < 10%
		assertTrue(OperatorWarnings.warnHeap(10.0, 100.0, 0.0, 0.0, 10.0));
	}

	@Test
	void formatTags_orderMatchesFindCheckboxes() {
		assertEquals("", OperatorWarnings.formatTags(false, false, false, false, false));
		assertEquals(
				OperatorWarnings.TAG_TIME + " " + OperatorWarnings.TAG_COST + " "
						+ OperatorWarnings.TAG_ROWS + " " + OperatorWarnings.TAG_FILTER + " "
						+ OperatorWarnings.TAG_HEAP,
				OperatorWarnings.formatTags(true, true, true, true, true));
		assertEquals(OperatorWarnings.TAG_COST + " " + OperatorWarnings.TAG_FILTER,
				OperatorWarnings.formatTags(false, true, false, true, false));
	}

	@Test
	void formatWarningWords_usesPipeSeparatedWordsInChipOrder() {
		assertEquals("", OperatorWarnings.formatWarningWords(false, false, false, false, false));
		assertEquals("Time | Cost | Ratio | Filter | Heap",
				OperatorWarnings.formatWarningWords(true, true, true, true, true));
		assertEquals("Cost | Filter", OperatorWarnings.formatWarningWords(false, true, false, true, false));
	}

	@Test
	void formatWarningWordsFromTags_matchesDiagramChipOrder() {
		String tags = OperatorWarnings.formatTags(true, true, true, true, false);
		assertEquals("Time | Cost | Ratio | Filter",
				OperatorWarnings.formatWarningWordsFromTags(tags));
		assertEquals("Cost | Filter",
				OperatorWarnings.formatWarningWordsFromTags(
						OperatorWarnings.formatTags(false, true, false, true, false)));
		assertEquals("", OperatorWarnings.formatWarningWordsFromTags(null));
		assertEquals("", OperatorWarnings.formatWarningWordsFromTags(""));
	}

	@Test
	void tooltipFor_matchesDashboardHoverCopy() {
		assertEquals("Opr time % is among highest", OperatorWarnings.tooltipFor(OperatorWarnings.TAG_TIME));
		assertEquals("Cost % is among highest", OperatorWarnings.tooltipFor(OperatorWarnings.TAG_COST));
		assertEquals("Actual vs. Estimated ratio is excessive",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_ROWS));
		assertEquals("Filter removes high % of rows",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_FILTER));
		assertEquals("Heap fetch % is high, consider VACUUM",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_HEAP));
		assertEquals("Opr time 23%",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_TIME, "23", null, null, null, null, null));
		assertEquals("Cost 45%",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_COST, null, "45", null, null, null, null));
		assertEquals("Actual vs. Estimated ratio 156:1",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_ROWS, null, null, "156:1", null, null, null));
		assertEquals("Filter removes 90% of rows",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_FILTER, null, null, null, "90",
						OperatorWarnings.METRIC_ROWS, null));
		assertEquals("Filter removes 90% of mass",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_FILTER, null, null, null, "90",
						OperatorWarnings.METRIC_MASS, null));
		assertEquals("Heap fetch 12%, consider VACUUM",
				OperatorWarnings.tooltipFor(OperatorWarnings.TAG_HEAP, null, null, null, null, null, "12"));
		assertEquals(null, OperatorWarnings.tooltipFor(""));
		assertEquals(null, OperatorWarnings.tooltipFor(null));
	}
}
