package com.williammeitzen.peps.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.jupiter.api.Test;

class ExplainTextPlanConverterTest {

	@Test
	void sniffDetectsJsonAndText() {
		assertEquals(PlanFormat.JSON, ExplainTextPlanConverter.sniff("[{\"Plan\":{\"Node Type\":\"Result\"}}]"));
		assertEquals(PlanFormat.TEXT, ExplainTextPlanConverter.sniff(
				"Seq Scan on pg_class (cost=0.00..14.56 rows=356 width=273)"));
		assertEquals(PlanFormat.EMPTY, ExplainTextPlanConverter.sniff("   "));
	}

	@Test
	void convertsSimpleSeqScan() throws Exception {
		JsonObject plan = planFromFixture("explain-text/01-seq-scan.txt");
		assertEquals("Seq Scan", plan.get("Node Type"));
		assertEquals("pg_class", plan.get("Relation Name"));
		assertEquals(0.0d, ((Number) plan.get("Startup Cost")).doubleValue(), 0.0001);
		assertEquals(14.56d, ((Number) plan.get("Total Cost")).doubleValue(), 0.0001);
		assertEquals(356L, ((Number) plan.get("Plan Rows")).longValue());
	}

	@Test
	void convertsParentChildLimit() throws Exception {
		JsonObject plan = planFromFixture("explain-text/02-limit-seq-scan.txt");
		assertEquals("Limit", plan.get("Node Type"));
		JsonArray children = (JsonArray) plan.get("Plans");
		assertNotNull(children);
		assertEquals(1, children.size());
		JsonObject child = (JsonObject) children.get(0);
		assertEquals("Seq Scan", child.get("Node Type"));
		assertEquals("Outer", child.get("Parent Relationship"));
	}

	@Test
	void convertsNestedLoopWithIndexCond() throws Exception {
		JsonObject plan = planFromFixture("explain-text/03-nested-loop-join.txt");
		assertEquals("Nested Loop", plan.get("Node Type"));
		assertEquals("Inner", plan.get("Join Type"));
		JsonArray children = (JsonArray) plan.get("Plans");
		assertEquals(2, children.size());
		JsonObject outer = (JsonObject) children.get(0);
		JsonObject inner = (JsonObject) children.get(1);
		assertEquals("Seq Scan", outer.get("Node Type"));
		assertEquals("Outer", outer.get("Parent Relationship"));
		assertEquals("Index Scan", inner.get("Node Type"));
		assertEquals("Inner", inner.get("Parent Relationship"));
		assertEquals("t2_pkey", inner.get("Index Name"));
		assertEquals("(id = t1.id)", inner.get("Index Cond"));
	}

	@Test
	void convertsHashJoinAnalyzeAndTiming() throws Exception {
		String json = ExplainTextPlanConverter.convertTextToJsonArrayString(loadFixture("explain-text/04-hash-join-analyze.txt"));
		JsonArray array = (JsonArray) Jsoner.deserialize(json);
		JsonObject wrapper = (JsonObject) array.get(0);
		assertEquals(0.150d, ((Number) wrapper.get("Planning Time")).doubleValue(), 0.0001);
		assertEquals(0.090d, ((Number) wrapper.get("Execution Time")).doubleValue(), 0.0001);
		JsonObject plan = (JsonObject) wrapper.get("Plan");
		assertEquals("Hash Join", plan.get("Node Type"));
		assertEquals("Inner", plan.get("Join Type"));
		assertEquals(0.050d, ((Number) plan.get("Actual Startup Time")).doubleValue(), 0.0001);
		assertEquals("(t1.id = t2.id)", plan.get("Hash Cond"));
		JsonArray children = (JsonArray) plan.get("Plans");
		assertEquals(2, children.size());
		JsonObject hash = (JsonObject) children.get(1);
		assertEquals("Hash", hash.get("Node Type"));
		JsonArray hashChildren = (JsonArray) hash.get("Plans");
		assertEquals(1, hashChildren.size());
		assertEquals("Seq Scan", ((JsonObject) hashChildren.get(0)).get("Node Type"));
	}

	@Test
	void convertsBuffersAndWorkers() throws Exception {
		JsonObject plan = planFromFixture("explain-text/05-buffers-workers.txt");
		assertEquals("Gather", plan.get("Node Type"));
		assertEquals(2L, ((Number) plan.get("Workers Planned")).longValue());
		assertEquals(2L, ((Number) plan.get("Workers Launched")).longValue());
		JsonObject child = (JsonObject) ((JsonArray) plan.get("Plans")).get(0);
		assertEquals("Parallel Seq Scan", child.get("Node Type"));
		assertEquals(Boolean.TRUE, child.get("Parallel Aware"));
		assertEquals("Seq Scan",
				ExplainTextPlanConverter.resolveIconNodeType(
						(String) child.get("Node Type"), null, null, null));
		assertEquals("(flag = true)", child.get("Filter"));
		assertEquals(12L, ((Number) child.get("Shared Hit Blocks")).longValue());
		assertEquals(3L, ((Number) child.get("Shared Read Blocks")).longValue());
	}

	@Test
	void parsesMemoizeCacheDetails() throws Exception {
		JsonObject plan = planFromFixture("explain-text/08-memoize-cache.txt");
		JsonObject memoize = (JsonObject) ((JsonArray) plan.get("Plans")).get(1);
		assertEquals("Memoize", memoize.get("Node Type"));
		assertEquals("p._strain_key", memoize.get("Cache Key"));
		assertEquals("logical", memoize.get("Cache Mode"));
		assertEquals(961954L, ((Number) memoize.get("Cache Hits")).longValue());
		assertEquals(77L, ((Number) memoize.get("Cache Misses")).longValue());
		assertEquals(0L, ((Number) memoize.get("Cache Evictions")).longValue());
		assertEquals(0L, ((Number) memoize.get("Cache Overflows")).longValue());
		assertEquals(10.0d, ((Number) memoize.get("Peak Memory Usage")).doubleValue(), 0.0001);
		assertEquals(304L, ((Number) memoize.get("Shared Hit Blocks")).longValue());
	}

	@Test
	void parsesFractionalActualRowsFromAnalyzeText() throws Exception {
		JsonObject plan = planFromFixture("explain-text/07-fractional-actual-rows.txt");
		assertEquals("Nested Loop Left Join", plan.get("Node Type"));
		assertEquals("Left", plan.get("Join Type"));
		assertEquals(15.676d, ((Number) plan.get("Actual Startup Time")).doubleValue(), 0.0001);
		assertEquals(1096.732d, ((Number) plan.get("Actual Total Time")).doubleValue(), 0.0001);
		assertEquals(962031L, ((Number) plan.get("Actual Rows")).longValue());
		assertEquals(1L, ((Number) plan.get("Actual Loops")).longValue());
	}

	@Test
	void preservesTextNativeNodeTypeAndEmitsIconSiblings() {
		JsonObject left = new JsonObject();
		ExplainTextPlanConverter.applyNormalizedNodeType(left, "Nested Loop Left Join");
		assertEquals("Nested Loop Left Join", left.get("Node Type"));
		assertEquals("Left", left.get("Join Type"));
		assertEquals("Nested Loop",
				ExplainTextPlanConverter.resolveIconNodeType(
						(String) left.get("Node Type"), (String) left.get("Join Type"), null, null));

		JsonObject hashAgg = new JsonObject();
		ExplainTextPlanConverter.applyNormalizedNodeType(hashAgg, "HashAggregate");
		assertEquals("HashAggregate", hashAgg.get("Node Type"));
		assertEquals("Hashed", hashAgg.get("Strategy"));
		assertEquals("Simple", hashAgg.get("Partial Mode"));

		JsonObject insert = new JsonObject();
		ExplainTextPlanConverter.applyNormalizedNodeType(insert, "Insert");
		assertEquals("Insert", insert.get("Node Type"));
		assertEquals("Insert", insert.get("Operation"));
		assertEquals("ModifyTable",
				ExplainTextPlanConverter.resolveIconNodeType("Insert", null, "Insert", null));
	}

	@Test
	void resolveLeavesJsonUsableAndConvertsText() throws Exception {
		String jsonIn = "[{\"Plan\":{\"Node Type\":\"Result\"}}]";
		String resolvedJson = ExplainTextPlanConverter.resolveToJsonArrayString(jsonIn);
		assertNotNull(resolvedJson);
		assertTrue(resolvedJson.contains("Result"));

		String textIn = loadFixture("explain-text/01-seq-scan.txt");
		String resolvedText = ExplainTextPlanConverter.resolveToJsonArrayString(textIn);
		assertNotNull(resolvedText);
		JsonArray array = (JsonArray) Jsoner.deserialize(resolvedText);
		JsonObject plan = (JsonObject) ((JsonObject) array.get(0)).get("Plan");
		assertEquals("Seq Scan", plan.get("Node Type"));
	}

	@Test
	void textAndSqliteShapedJsonShareNodeTreeShape() throws Exception {
		JsonObject fromText = planFromFixture("explain-text/02-limit-seq-scan.txt");
		String resolvedJson = ExplainTextPlanConverter.resolveToJsonArrayString(
				loadFixture("explain-text/06-sqlite-shaped-json.json"));
		JsonObject fromJson = (JsonObject) ((JsonObject) ((JsonArray) Jsoner.deserialize(resolvedJson)).get(0)).get("Plan");
		assertEquals(fromJson.get("Node Type"), fromText.get("Node Type"));
		JsonArray textChildren = (JsonArray) fromText.get("Plans");
		JsonArray jsonChildren = (JsonArray) fromJson.get("Plans");
		assertEquals(jsonChildren.size(), textChildren.size());
		assertEquals(
				((JsonObject) jsonChildren.get(0)).get("Node Type"),
				((JsonObject) textChildren.get(0)).get("Node Type"));
		assertEquals(
				((JsonObject) jsonChildren.get(0)).get("Relation Name"),
				((JsonObject) textChildren.get(0)).get("Relation Name"));
	}

	private static JsonObject planFromFixture(String resource) throws Exception {
		String json = ExplainTextPlanConverter.convertTextToJsonArrayString(loadFixture(resource));
		JsonArray array = (JsonArray) Jsoner.deserialize(json);
		return (JsonObject) ((JsonObject) array.get(0)).get("Plan");
	}

	private static String loadFixture(String resource) throws IOException {
		try (InputStream in = ExplainTextPlanConverterTest.class.getClassLoader().getResourceAsStream(resource)) {
			assertNotNull(in, "Missing fixture: " + resource);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
