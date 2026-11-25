package com.williammeitzen.peps.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Layout vs paint geometry for warning chips across zoom extremes.
 * Mirrors {@code PlanGraphPanel} box_top reservation and drawWarningTags placement.
 */
class WarningChipZoomLayoutTest {

	/**
	 * Chip top Y relative to {@code row_boundary_top} (content coords, before
	 * {@code GRAPH_TOP_MARGIN}). Zero means chips sit flush with the row top.
	 */
	static int chipTopRelativeToRowTop(
			int reservedChipHeight,
			int reservedGap,
			int reservedNorthHeight,
			int reservedOutsideTop,
			int liveChipSide,
			int liveGap,
			int liveNorthAscent,
			int liveOutsideTop) {
		int boxTop = reservedChipHeight + reservedGap + reservedNorthHeight + reservedOutsideTop;
		int northBaseline = boxTop - liveOutsideTop;
		int northTop = northBaseline - liveNorthAscent;
		int chipBottom = northTop - liveGap;
		return chipBottom - liveChipSide;
	}

	@Test
	void matchedReservationKeepsChipsAtRowTop_acrossZoomExtremes() {
		int[] zooms = { 10, 100, 150, 400 };
		int baseChip = 16;
		int baseGap = 3;
		int baseNorth = 12;
		int baseOutside = 3;

		for (int zoom : zooms) {
			double z = zoom / 100.0;
			int chip = Math.max(1, (int) Math.round(baseChip * z));
			int gap = Math.max(3, (int) Math.round(baseGap * z));
			int north = Math.max(1, (int) Math.round(baseNorth * z));
			int outside = Math.max(0, (int) Math.round(baseOutside * z));

			int top = chipTopRelativeToRowTop(
					chip, gap, north, outside,
					chip, gap, north, outside);
			assertEquals(0, top, "zoom " + zoom + "%: matched reserve/paint must pin chips at row top");
		}
	}

	@Test
	void staleHighZoomMetricsLeaveGapWhenPaintedAt100() {
		// Simulates zooming 400% → 100% without refreshing warning_tags_fontmetrics_*.
		int reservedChip = 64;
		int reservedGap = 12;
		int reservedNorth = 48;
		int reservedOutside = 12;
		int liveChip = 16;
		int liveGap = 3;
		int liveNorth = 12;
		int liveOutside = 3;

		int top = chipTopRelativeToRowTop(
				reservedChip, reservedGap, reservedNorth, reservedOutside,
				liveChip, liveGap, liveNorth, liveOutside);
		assertTrue(top > 20,
				"stale 400% reservation at 100% paint must leave a large positive gap (got " + top + ")");
	}

	@Test
	void staleLowZoomMetricsPushChipsAboveRowTopWhenPaintedAt400() {
		// Simulates zooming 100% → 400% without refreshing metrics (original clip).
		int reservedChip = 16;
		int reservedGap = 3;
		int reservedNorth = 12;
		int reservedOutside = 3;
		int liveChip = 64;
		int liveGap = 12;
		int liveNorth = 48;
		int liveOutside = 12;

		int top = chipTopRelativeToRowTop(
				reservedChip, reservedGap, reservedNorth, reservedOutside,
				liveChip, liveGap, liveNorth, liveOutside);
		assertTrue(top < 0,
				"stale 100% reservation at 400% paint must push chips above row top (got " + top + ")");
	}
}
