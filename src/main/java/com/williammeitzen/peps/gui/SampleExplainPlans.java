package com.williammeitzen.peps.gui;

/**
 * Built-in sample EXPLAIN content for the Documents empty-state landing screen.
 * SQL matches the demo queries; plan text is a large offline stand-in so new users
 * can open a diagram without a live Postgres connection.
 */
public final class SampleExplainPlans {

	public static final String JSON_EXPLAIN_SQL =
			"explain (format json, analyze, timing on, buffers on, verbose on)\n"
					+ "select * from all_cellline_view";

	public static final String TEXT_EXPLAIN_SQL =
			"explain (format text, analyze, timing on, buffers on, verbose on)\n"
					+ "select * from all_cellline_view";

	private static final int PARTITION_COUNT = 24;

	private SampleExplainPlans() {
	}

	/** Large JSON EXPLAIN ANALYZE output (array-wrapped PostgreSQL shape). */
	public static String jsonPlanText() {
		StringBuilder sb = new StringBuilder(16_384);
		sb.append("[\n  {\n");
		sb.append("    \"Plan\": ");
		appendJsonNode(sb, "Nested Loop", null, 0, true);
		sb.append(",\n");
		sb.append("    \"Planning Time\": 12.480,\n");
		sb.append("    \"Triggers\": [\n    ],\n");
		sb.append("    \"Execution Time\": 1842.337\n");
		sb.append("  }\n]\n");
		return sb.toString();
	}

	/** Large text EXPLAIN ANALYZE output matching the JSON sample shape. */
	public static String textPlanText() {
		StringBuilder sb = new StringBuilder(12_288);
		sb.append("Nested Loop  (cost=1250.00..98540.00 rows=250000 width=420)");
		sb.append(" (actual time=2.150..1788.420 rows=248812 loops=1)\n");
		sb.append("  Output: v.cellline_id, v.cellline_name, v.tissue, v.source, v.attributes\n");
		sb.append("  Buffers: shared hit=48210 read=9104\n");
		sb.append("  ->  Hash Join  (cost=850.00..42100.00 rows=180000 width=280)");
		sb.append(" (actual time=1.820..920.110 rows=180000 loops=1)\n");
		sb.append("        Output: a.cellline_id, a.cellline_name, a.tissue, b.source\n");
		sb.append("        Hash Cond: (a.cellline_id = b.cellline_id)\n");
		sb.append("        Buffers: shared hit=22100 read=4100\n");
		sb.append("        ->  Append  (cost=0.00..31200.00 rows=180000 width=200)");
		sb.append(" (actual time=0.040..510.200 rows=180000 loops=1)\n");
		sb.append("              Buffers: shared hit=18000 read=3200\n");
		for (int i = 1; i <= PARTITION_COUNT; i++) {
			String rel = "all_cellline_part_" + i;
			sb.append("              ->  Seq Scan on ").append(rel);
			sb.append(" a  (cost=0.00..").append(400 + i * 12).append(".00 rows=");
			sb.append(7000 + i * 50).append(" width=200)");
			sb.append(" (actual time=0.010..").append(String.format("%.3f", 8.0 + i * 0.7));
			sb.append(" rows=").append(7000 + i * 50).append(" loops=1)\n");
			sb.append("                    Output: a.cellline_id, a.cellline_name, a.tissue\n");
			sb.append("                    Filter: (a.is_active = true)\n");
			sb.append("                    Rows Removed by Filter: ").append(120 + i).append("\n");
			sb.append("                    Buffers: shared hit=").append(200 + i * 3);
			sb.append(" read=").append(40 + i).append("\n");
		}
		sb.append("        ->  Hash  (cost=620.00..620.00 rows=15000 width=80)");
		sb.append(" (actual time=1.400..1.410 rows=14980 loops=1)\n");
		sb.append("              Output: b.cellline_id, b.source\n");
		sb.append("              Buckets: 16384  Batches: 1  Memory Usage: 920kB\n");
		sb.append("              Buffers: shared hit=4100 read=900\n");
		sb.append("              ->  Seq Scan on cellline_source b");
		sb.append("  (cost=0.00..620.00 rows=15000 width=80)");
		sb.append(" (actual time=0.020..0.980 rows=14980 loops=1)\n");
		sb.append("                    Output: b.cellline_id, b.source\n");
		sb.append("                    Buffers: shared hit=4100 read=900\n");
		sb.append("  ->  Index Scan using cellline_attr_pkey on cellline_attributes v");
		sb.append("  (cost=0.42..0.55 rows=1 width=140)");
		sb.append(" (actual time=0.004..0.004 rows=1 loops=180000)\n");
		sb.append("        Output: v.cellline_id, v.attributes\n");
		sb.append("        Index Cond: (v.cellline_id = a.cellline_id)\n");
		sb.append("        Buffers: shared hit=26110 read=5004\n");
		sb.append("Planning Time: 12.480 ms\n");
		sb.append("Execution Time: 1842.337 ms\n");
		return sb.toString();
	}

	private static void appendJsonNode(StringBuilder sb, String nodeType, String relation,
			int depth, boolean root) {
		indent(sb, depth);
		sb.append("{\n");
		indent(sb, depth + 1);
		sb.append("\"Node Type\": \"").append(nodeType).append("\",\n");
		if (relation != null) {
			indent(sb, depth + 1);
			sb.append("\"Relation Name\": \"").append(relation).append("\",\n");
			indent(sb, depth + 1);
			sb.append("\"Alias\": \"a\",\n");
		}
		if ("Nested Loop".equals(nodeType)) {
			indent(sb, depth + 1);
			sb.append("\"Parallel Aware\": false,\n");
			indent(sb, depth + 1);
			sb.append("\"Startup Cost\": 1250.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Total Cost\": 98540.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Rows\": 250000,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Width\": 420,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Startup Time\": 2.150,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Total Time\": 1788.420,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Rows\": 248812,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Loops\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Output\": [\"v.cellline_id\", \"v.cellline_name\", \"v.tissue\", \"v.source\", \"v.attributes\"],\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Hit Blocks\": 48210,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Read Blocks\": 9104,\n");
			indent(sb, depth + 1);
			sb.append("\"Plans\": [\n");
			appendJsonNode(sb, "Hash Join", null, depth + 2, false);
			sb.append(",\n");
			appendJsonNode(sb, "Index Scan", null, depth + 2, false);
			sb.append('\n');
			indent(sb, depth + 1);
			sb.append("]\n");
		} else if ("Hash Join".equals(nodeType)) {
			indent(sb, depth + 1);
			sb.append("\"Parent Relationship\": \"Outer\",\n");
			indent(sb, depth + 1);
			sb.append("\"Startup Cost\": 850.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Total Cost\": 42100.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Rows\": 180000,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Width\": 280,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Startup Time\": 1.820,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Total Time\": 920.110,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Rows\": 180000,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Loops\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Output\": [\"a.cellline_id\", \"a.cellline_name\", \"a.tissue\", \"b.source\"],\n");
			indent(sb, depth + 1);
			sb.append("\"Hash Cond\": \"(a.cellline_id = b.cellline_id)\",\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Hit Blocks\": 22100,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Read Blocks\": 4100,\n");
			indent(sb, depth + 1);
			sb.append("\"Plans\": [\n");
			appendJsonNode(sb, "Append", null, depth + 2, false);
			sb.append(",\n");
			appendJsonNode(sb, "Hash", null, depth + 2, false);
			sb.append('\n');
			indent(sb, depth + 1);
			sb.append("]\n");
		} else if ("Append".equals(nodeType)) {
			indent(sb, depth + 1);
			sb.append("\"Parent Relationship\": \"Outer\",\n");
			indent(sb, depth + 1);
			sb.append("\"Startup Cost\": 0.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Total Cost\": 31200.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Rows\": 180000,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Width\": 200,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Startup Time\": 0.040,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Total Time\": 510.200,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Rows\": 180000,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Loops\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Hit Blocks\": 18000,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Read Blocks\": 3200,\n");
			indent(sb, depth + 1);
			sb.append("\"Plans\": [\n");
			for (int i = 1; i <= PARTITION_COUNT; i++) {
				if (i > 1) {
					sb.append(",\n");
				}
				appendJsonNode(sb, "Seq Scan", "all_cellline_part_" + i, depth + 2, false);
			}
			sb.append('\n');
			indent(sb, depth + 1);
			sb.append("]\n");
		} else if ("Hash".equals(nodeType)) {
			indent(sb, depth + 1);
			sb.append("\"Parent Relationship\": \"Inner\",\n");
			indent(sb, depth + 1);
			sb.append("\"Startup Cost\": 620.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Total Cost\": 620.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Rows\": 15000,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Width\": 80,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Startup Time\": 1.400,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Total Time\": 1.410,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Rows\": 14980,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Loops\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Output\": [\"b.cellline_id\", \"b.source\"],\n");
			indent(sb, depth + 1);
			sb.append("\"Hash Buckets\": 16384,\n");
			indent(sb, depth + 1);
			sb.append("\"Hash Batches\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Peak Memory Usage\": 920,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Hit Blocks\": 4100,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Read Blocks\": 900,\n");
			indent(sb, depth + 1);
			sb.append("\"Plans\": [\n");
			appendJsonNode(sb, "Seq Scan", "cellline_source", depth + 2, false);
			sb.append('\n');
			indent(sb, depth + 1);
			sb.append("]\n");
		} else if ("Index Scan".equals(nodeType)) {
			indent(sb, depth + 1);
			sb.append("\"Parent Relationship\": \"Inner\",\n");
			indent(sb, depth + 1);
			sb.append("\"Scan Direction\": \"Forward\",\n");
			indent(sb, depth + 1);
			sb.append("\"Index Name\": \"cellline_attr_pkey\",\n");
			indent(sb, depth + 1);
			sb.append("\"Relation Name\": \"cellline_attributes\",\n");
			indent(sb, depth + 1);
			sb.append("\"Alias\": \"v\",\n");
			indent(sb, depth + 1);
			sb.append("\"Startup Cost\": 0.42,\n");
			indent(sb, depth + 1);
			sb.append("\"Total Cost\": 0.55,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Rows\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Width\": 140,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Startup Time\": 0.004,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Total Time\": 0.004,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Rows\": 1,\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Loops\": 180000,\n");
			indent(sb, depth + 1);
			sb.append("\"Output\": [\"v.cellline_id\", \"v.attributes\"],\n");
			indent(sb, depth + 1);
			sb.append("\"Index Cond\": \"(v.cellline_id = a.cellline_id)\",\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Hit Blocks\": 26110,\n");
			indent(sb, depth + 1);
			sb.append("\"Shared Read Blocks\": 5004\n");
		} else if ("Seq Scan".equals(nodeType)) {
			boolean partition = relation != null && relation.startsWith("all_cellline_part_");
			int part = 1;
			if (partition) {
				try {
					part = Integer.parseInt(relation.substring("all_cellline_part_".length()));
				} catch (NumberFormatException ignored) {
					part = 1;
				}
			}
			indent(sb, depth + 1);
			sb.append("\"Parent Relationship\": \"Member\",\n");
			indent(sb, depth + 1);
			sb.append("\"Parallel Aware\": false,\n");
			indent(sb, depth + 1);
			sb.append("\"Relation Name\": \"").append(relation).append("\",\n");
			indent(sb, depth + 1);
			sb.append("\"Alias\": \"").append(partition ? "a" : "b").append("\",\n");
			indent(sb, depth + 1);
			sb.append("\"Startup Cost\": 0.00,\n");
			indent(sb, depth + 1);
			sb.append("\"Total Cost\": ").append(partition ? (400 + part * 12) : 620).append(".00,\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Rows\": ").append(partition ? (7000 + part * 50) : 15000).append(",\n");
			indent(sb, depth + 1);
			sb.append("\"Plan Width\": ").append(partition ? 200 : 80).append(",\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Startup Time\": ").append(partition ? "0.010" : "0.020").append(",\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Total Time\": ").append(partition
					? String.format("%.3f", 8.0 + part * 0.7) : "0.980").append(",\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Rows\": ").append(partition ? (7000 + part * 50) : 14980).append(",\n");
			indent(sb, depth + 1);
			sb.append("\"Actual Loops\": 1,\n");
			if (partition) {
				indent(sb, depth + 1);
				sb.append("\"Output\": [\"a.cellline_id\", \"a.cellline_name\", \"a.tissue\"],\n");
				indent(sb, depth + 1);
				sb.append("\"Filter\": \"(a.is_active = true)\",\n");
				indent(sb, depth + 1);
				sb.append("\"Rows Removed by Filter\": ").append(120 + part).append(",\n");
				indent(sb, depth + 1);
				sb.append("\"Shared Hit Blocks\": ").append(200 + part * 3).append(",\n");
				indent(sb, depth + 1);
				sb.append("\"Shared Read Blocks\": ").append(40 + part).append('\n');
			} else {
				indent(sb, depth + 1);
				sb.append("\"Output\": [\"b.cellline_id\", \"b.source\"],\n");
				indent(sb, depth + 1);
				sb.append("\"Shared Hit Blocks\": 4100,\n");
				indent(sb, depth + 1);
				sb.append("\"Shared Read Blocks\": 900\n");
			}
		}
		indent(sb, depth);
		sb.append('}');
		if (root) {
			// no-op; caller adds trailing fields at the plan root wrapper
		}
	}

	private static void indent(StringBuilder sb, int depth) {
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
	}
}
