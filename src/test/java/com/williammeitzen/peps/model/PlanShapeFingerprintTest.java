package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlanShapeFingerprintTest {

	@Test
	void textAndJsonShapesMatchForNormalizedJoin() throws Exception {
		String text = ""
				+ "Nested Loop Left Join  (cost=0.29..16.33 rows=10 width=8)\n"
				+ "  ->  Seq Scan on t1  (cost=0.00..1.10 rows=10 width=4)\n"
				+ "  ->  Index Scan using t2_pkey on t2  (cost=0.29..1.51 rows=1 width=4)\n";
		String json = "["
				+ "{\"Plan\":{"
				+ "\"Node Type\":\"Nested Loop\",\"Join Type\":\"Left\","
				+ "\"Plans\":["
				+ "{\"Node Type\":\"Seq Scan\",\"Relation Name\":\"t1\",\"Parent Relationship\":\"Outer\"},"
				+ "{\"Node Type\":\"Index Scan\",\"Relation Name\":\"t2\",\"Index Name\":\"t2_pkey\",\"Parent Relationship\":\"Inner\"}"
				+ "]}}]";
		// Icon/diagram shape matches even though text keeps "Nested Loop Left Join" as Node Type.
		assertEquals(
				PlanShapeFingerprint.fromPlanField(json),
				PlanShapeFingerprint.fromPlanField(text));
	}
}
