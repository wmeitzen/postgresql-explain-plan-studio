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
	void rows_matchesPev2EstimationClassPresence() {
		assertFalse(OperatorWarnings.warnRows(100.0, 10.0)); // 10× not >
		assertTrue(OperatorWarnings.warnRows(101.0, 10.0));
		assertTrue(OperatorWarnings.warnRows(10.0, 101.0));
		assertFalse(OperatorWarnings.warnRows(null, 10.0));
	}

	@Test
	void filter_matchesPev2RowsRemovedClassPresence() {
		assertFalse(OperatorWarnings.warnFilter(50.0, 50.0)); // 50% not >
		assertTrue(OperatorWarnings.warnFilter(51.0, 49.0)); // floor 51%
		assertTrue(OperatorWarnings.warnFilter(900.0, 100.0));
		assertEquals(90.0, OperatorWarnings.filterRemovedPercent(900.0, 100.0));
	}

	@Test
	void heap_matchesPev2HeapFetchesClassPresence() {
		assertFalse(OperatorWarnings.warnHeap(0.0, 100.0, 0.0, 0.0));
		assertTrue(OperatorWarnings.warnHeap(1.0, 100.0, 0.0, 0.0));
		assertEquals(50.0, OperatorWarnings.heapFetchPercent(50.0, 100.0, 0.0, 0.0));
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
	void formatWarningWords_usesSingleWords() {
		assertEquals("", OperatorWarnings.formatWarningWords(false, false, false, false, false));
		assertEquals("Time Cost Rows Filter Heap",
				OperatorWarnings.formatWarningWords(true, true, true, true, true));
		assertEquals("Cost Filter", OperatorWarnings.formatWarningWords(false, true, false, true, false));
	}
}
