package com.williammeitzen.peps.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

import com.williammeitzen.peps.model.Database;
import com.williammeitzen.peps.model.ExplainTextPlanConverter;

/**
 * Builds an HTML comparison: per-operator plan source (TEXT or JSON) vs PEPS-style properties.
 * Args: optional plan name (default {@code Update}). Always writes {@code C:\docs\md\plan-mapping.html}.
 */
public final class GeneratePlanMappingMarkdown {

	private static final Path OUT = Path.of("C:\\docs\\md\\plan-mapping.html");
	private static final String EXPLAIN_TEXT =
			"explain (analyze, buffers, format text, verbose, timing)\n\n";
	private static final String EXPLAIN_JSON =
			"explain (analyze, buffers, format json, verbose)\n\n";
	private static final Pattern TEXT_NODE = Pattern.compile(
			"^(\\s*)(?:->\\s*)?(.+?)\\s+\\(cost=.*$");
	private static final Pattern EXPLAIN_PREFIX = Pattern.compile(
			"(?is)^\\s*explain\\s*(?:\\([^)]*\\))?\\s*");

	private GeneratePlanMappingMarkdown() {
	}

	public static void main(String[] args) throws Exception {
		String planName = args.length > 0 ? args[0] : "Update";

		Database.init();
		DemoPlan demo = loadDemo(planName);
		if (demo == null) {
			throw new IllegalStateException("Plan not found in SQLite: " + planName);
		}

		List<Object> init = Database.pg_init(demo.server, demo.port, demo.database, demo.user, demo.password, "");
		if ((Boolean) init.get(0)) {
			throw new IllegalStateException("Postgres unreachable: " + init.get(1));
		}

		String statement = extractStatement(demo.explainSql);

		runDml(demo.pre);
		String textPlan = fetchExplain(EXPLAIN_TEXT + statement);
		runDml(demo.post);

		runDml(demo.pre);
		String jsonPlan = fetchExplain(EXPLAIN_JSON + statement);
		runDml(demo.post);

		JsonObject textRoot = ExplainTextPlanConverter.convertTextToPlanObject(textPlan);
		JsonObject jsonRoot = extractPlanRoot(jsonPlan);

		List<String> textSnippets = splitTextOperators(textPlan);
		List<OpRow> textProps = flattenProps(textRoot);
		List<String> jsonSnippets = new ArrayList<>();
		List<OpRow> jsonProps = flattenJsonWithSnippets(jsonRoot, jsonSnippets);

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
		html.append("<meta charset=\"utf-8\">\n");
		html.append("<title>").append(escape(planName)).append(" — plan mapping</title>\n");
		html.append("<style>\n");
		html.append("body{font:15px/1.45 system-ui,Segoe UI,sans-serif;margin:24px;color:#111;background:#fafafa;}\n");
		html.append("h1{font-size:1.4rem;margin:0 0 .4rem;}\n");
		html.append("h2{font-size:1.15rem;margin:1.6rem 0 .6rem;}\n");
		html.append(".sql{display:block;background:#f0f0f0;padding:10px 12px;border-radius:6px;");
		html.append("overflow-wrap:anywhere;word-break:break-word;}\n");
		html.append("table{border-collapse:collapse;width:100%;table-layout:fixed;margin:0 0 1.5rem;background:#fff;}\n");
		html.append("th,td{border:1px solid #ccc;vertical-align:top;padding:10px;width:33%;");
		html.append("overflow-wrap:anywhere;word-break:break-word;}\n");
		html.append("th{background:#eee;text-align:left;}\n");
		html.append(".cell{margin:0;overflow-wrap:anywhere;word-break:break-word;}\n");
		html.append(".cell div{margin:0 0 .35em;}\n");
		html.append(".cell div:last-child{margin-bottom:0;}\n");
		html.append(".note{color:#555;margin:.4rem 0 1rem;}\n");
		html.append("</style>\n</head>\n<body>\n");

		html.append("<h1>").append(escape(planName)).append("</h1>\n");
		html.append("<div class=\"sql\">").append(nlToDivs(statement.trim())).append("</div>\n");
		html.append("<p class=\"note\">Each row is one operator: plan source, properties panel, ")
				.append("and diagram south text.</p>\n");

		html.append("<h2>TEXT</h2>\n");
		appendSideBySide(html, textSnippets, textProps);

		html.append("<h2>JSON</h2>\n");
		appendSideBySide(html, jsonSnippets, jsonProps);

		html.append("</body>\n</html>\n");

		Files.createDirectories(OUT.getParent());
		Files.writeString(OUT, html.toString(), StandardCharsets.UTF_8);
		System.out.println("Wrote " + OUT.toAbsolutePath());
	}

	private static void appendSideBySide(StringBuilder html, List<String> snippets, List<OpRow> props) {
		html.append("<table>\n");
		html.append("<tr><th>Plan</th><th>Properties</th><th>South</th></tr>\n");
		int n = Math.max(snippets.size(), props.size());
		for (int i = 0; i < n; i++) {
			String src = i < snippets.size() ? snippets.get(i) : "";
			OpRow prop = i < props.size() ? props.get(i) : null;
			html.append("<tr>\n<td><div class=\"cell\">")
					.append(nlToDivs(src.trim()))
					.append("</div></td>\n<td><div class=\"cell\">")
					.append(prop == null ? "—" : formatPropertiesPanel(i + 1, prop.props))
					.append("</div></td>\n<td><div class=\"cell\">")
					.append(prop == null ? "—" : nlToDivs(formatSouth(prop.props)))
					.append("</div></td>\n</tr>\n");
		}
		html.append("</table>\n");
	}

	/** One visual line per div so long tokens wrap inside the column. */
	private static String nlToDivs(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		String[] lines = text.replace("\r\n", "\n").split("\n", -1);
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append("<div>").append(escape(line.isEmpty() ? "\u00A0" : line)).append("</div>");
		}
		return sb.toString();
	}

	/**
	 * Approximate PEPS diagram south labels (south_1+).
	 * Mirrors ModifyTable/Merge south layout (Operation+Relation, then {@code as alias}).
	 */
	private static String formatSouth(Map<String, String> props) {
		String nodeType = prop(props, "Node Type");
		String operation = prop(props, "Operation");
		String relation = prop(props, "Relation Name");
		String alias = prop(props, "Alias");
		String joinType = prop(props, "Join Type");
		String hashCond = prop(props, "Hash Cond");
		String filter = prop(props, "Filter");
		String joinFilter = prop(props, "Join Filter");
		String indexName = prop(props, "Index Name");
		String rowsRemovedFilter = prop(props, "Rows Removed by Filter");
		String rowsRemovedJoin = prop(props, "Rows Removed by Join Filter");

		if (alias != null && relation != null && alias.equalsIgnoreCase(relation)) {
			alias = null;
		}
		String asAlias = (alias == null || alias.isBlank()) ? null : "as " + alias;

		String south1 = nodeType == null ? "" : nodeType;
		if (filter != null || joinFilter != null) {
			south1 += " w/Filter";
		}

		List<String> south = new ArrayList<>();
		south.add(south1);

		if (nodeType != null && (nodeType.equalsIgnoreCase("ModifyTable")
				|| nodeType.equalsIgnoreCase("Insert")
				|| nodeType.equalsIgnoreCase("Update")
				|| nodeType.equalsIgnoreCase("Delete")
				|| nodeType.equalsIgnoreCase("Merge"))) {
			String op = (operation == null || operation.isBlank()) ? nodeType : operation;
			String line2 = op;
			if (op.equalsIgnoreCase("insert")) {
				line2 += " into";
			} else if (op.equalsIgnoreCase("delete")) {
				line2 += " from";
			}
			if (relation != null && !relation.isBlank()) {
				line2 += " " + relation;
			}
			south.add(line2);
			if (op.equalsIgnoreCase("merge")) {
				if (asAlias != null) {
					south.add(asAlias);
				}
			} else if (asAlias != null) {
				south.set(south.size() - 1, line2 + " " + asAlias);
			}
		} else if (nodeType != null && (nodeType.equalsIgnoreCase("Hash Join")
				|| nodeType.matches("(?i)(Parallel\\s+)?Hash(\\s+\\w+)?\\s+Join"))) {
			if (nodeType.equalsIgnoreCase("Hash Join") && joinType != null && !joinType.isBlank()) {
				south.add("(" + joinType + " Join)");
			}
			if (hashCond != null && !hashCond.isBlank()) {
				south.add("Hash cond: " + stripCastsLight(hashCond));
			}
			if (joinFilter != null && !joinFilter.isBlank()) {
				south.add("Filter: " + stripCastsLight(joinFilter));
			}
			appendRowsRemovedIfPositive(south, rowsRemovedJoin, "removed by join filter");
		} else if (nodeType != null && (nodeType.equalsIgnoreCase("Merge Join")
				|| nodeType.matches("(?i)(Parallel\\s+)?Merge(\\s+\\w+)?\\s+Join"))) {
			String mergeCond = prop(props, "Merge Cond");
			if (nodeType.equalsIgnoreCase("Merge Join") && joinType != null && !joinType.isBlank()) {
				south.add("(" + joinType + " Join)");
			}
			if (mergeCond != null && !mergeCond.isBlank()) {
				south.add("Merge cond: " + stripCastsLight(mergeCond));
			}
			if (joinFilter != null && !joinFilter.isBlank()) {
				south.add("Filter: " + stripCastsLight(joinFilter));
			}
			appendRowsRemovedIfPositive(south, rowsRemovedJoin, "removed by join filter");
		} else if (nodeType != null && (nodeType.equalsIgnoreCase("Nested Loop")
				|| nodeType.matches("(?i)(Parallel\\s+)?Nested Loop(\\s+\\w+\\s+Join)?"))) {
			if (nodeType.equalsIgnoreCase("Nested Loop") && joinType != null && !joinType.isBlank()) {
				south.add("(" + joinType + " Join)");
			}
			if (joinFilter != null && !joinFilter.isBlank()) {
				south.add("Filter: " + stripCastsLight(joinFilter));
			}
			appendRowsRemovedIfPositive(south, rowsRemovedJoin, "removed by join filter");
		} else if (nodeType != null && nodeType.equalsIgnoreCase("Values Scan")) {
			if (relation != null && !relation.isBlank()) {
				south.add(relation);
			}
			if (asAlias != null) {
				south.add(asAlias);
			}
		} else if (nodeType != null && nodeType.equalsIgnoreCase("Hash")) {
			// typically empty south body beyond south1
		} else if (nodeType != null && (nodeType.equalsIgnoreCase("Index Scan")
				|| nodeType.matches("(?i)Parallel\\s+Index Scan"))) {
			String indexCond = prop(props, "Index Cond");
			if (relation != null) {
				south.add(relation);
			}
			if (asAlias != null) {
				south.add(asAlias);
			}
			if (indexCond != null && !indexCond.isBlank()) {
				if (indexName != null) {
					south.add(indexName);
				} else {
					appendRowsRemovedIfPositive(south, rowsRemovedFilter, "removed by filter");
				}
				south.add("Index cond: " + stripCastsLight(indexCond));
			} else {
				appendRowsRemovedIfPositive(south, rowsRemovedFilter, "removed by filter");
				if (indexName != null) {
					south.add(indexName);
				}
			}
		} else if (nodeType != null && nodeType.equalsIgnoreCase("Memoize")) {
			String cacheKey = prop(props, "Cache Key");
			String peakMem = prop(props, "Peak Memory Usage");
			if (cacheKey != null && !cacheKey.isBlank()) {
				south.add(stripCastsLight(cacheKey));
			}
			if (peakMem != null && !peakMem.isBlank()) {
				try {
					south.add(com.williammeitzen.peps.gui.Utils.kilobytes(
							Double.parseDouble(peakMem.trim())) + " RAM used");
				} catch (NumberFormatException ex) {
					south.add(peakMem + " RAM used");
				}
			}
		} else if (nodeType != null && (nodeType.equalsIgnoreCase("Incremental Sort")
				|| nodeType.matches("(?i)Parallel\\s+Incremental Sort"))) {
			String presortedLine = presortedBySouthLine(props.get("Presorted Key"));
			if (presortedLine != null) {
				south.add(presortedLine);
			}
		} else if (nodeType != null && (nodeType.equalsIgnoreCase("Seq Scan")
				|| nodeType.equalsIgnoreCase("Sample Scan")
				|| nodeType.matches("(?i)Parallel\\s+Seq Scan"))) {
			if (relation != null) {
				south.add(relation);
			}
			if (asAlias != null) {
				south.add(asAlias);
			}
			if (filter != null && !filter.isBlank()) {
				south.add("Filter: " + stripCastsLight(filter));
			}
			appendRowsRemovedIfPositive(south, rowsRemovedFilter, "removed by filter");
		} else {
			if (relation != null) {
				south.add(relation);
			}
			if (asAlias != null) {
				south.add(asAlias);
			}
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String line : south) {
			if (line != null && !line.isBlank()) {
				if (!first) {
					sb.append('\n');
				}
				first = false;
				sb.append(stripCastsLight(line));
			}
		}
		return sb.toString();
	}

	private static String prop(Map<String, String> props, String key) {
		String v = props.get(key);
		if (v == null || v.isBlank() || "null".equalsIgnoreCase(v)) {
			return null;
		}
		return v;
	}

	/** Mirror PEPS Incremental Sort south: one key named, else {@code Presorted by N values}. */
	private static String presortedBySouthLine(String serializedPresortedKey) {
		if (serializedPresortedKey == null || serializedPresortedKey.isBlank()
				|| "null".equalsIgnoreCase(serializedPresortedKey)) {
			return null;
		}
		String raw = serializedPresortedKey.trim();
		List<String> keys = new ArrayList<>();
		if (raw.startsWith("[")) {
			try {
				Object parsed = Jsoner.deserialize(raw);
				if (parsed instanceof JsonArray) {
					for (Object item : (JsonArray) parsed) {
						if (item != null) {
							String s = String.valueOf(item).trim();
							if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
								keys.add(s);
							}
						}
					}
				}
			} catch (Exception ignored) {
				// fall through to single-value handling
			}
		}
		if (keys.isEmpty()) {
			keys.add(stripCastsLight(raw));
		}
		if (keys.size() == 1) {
			return "Presorted by " + stripCastsLight(keys.get(0));
		}
		return "Presorted by " + keys.size() + " values";
	}

	private static String stripCastsLight(String text) {
		if (text == null) {
			return null;
		}
		String previous;
		String current = text;
		do {
			previous = current;
			current = current.replaceAll(
					"::(?:\"[^\"]+\"|[A-Za-z_][\\w]*(?:\\s+[A-Za-z_][\\w]*)*)(?:\\[\\])?", "");
		} while (!current.equals(previous));
		return current.replaceAll("^\\((.*)\\)$", "$1");
	}

	private static void appendRowsRemovedIfPositive(List<String> south, String countText, String phrase) {
		if (countText == null || countText.isBlank()) {
			return;
		}
		try {
			double n = Double.parseDouble(countText.trim());
			if (n <= 0) {
				return;
			}
		} catch (NumberFormatException ignored) {
			// non-numeric — still show
		}
		south.add(countText + " " + phrase);
	}

	/** Properties panel style: Operator N, then name/value sorted A–Z. */
	private static String formatPropertiesPanel(int operatorNumber, Map<String, String> props) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div><strong>Operator</strong> ").append(operatorNumber).append("</div>");
		TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		sorted.putAll(props);
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			sb.append("<div><strong>").append(escape(e.getKey())).append("</strong> ")
					.append(escape(stripCastsLight(e.getValue()))).append("</div>");
		}
		return sb.toString();
	}

	private static List<String> splitTextOperators(String textPlan) {
		String[] lines = textPlan.replace("\r\n", "\n").split("\n");
		List<String> snippets = new ArrayList<>();
		StringBuilder current = null;
		for (String line : lines) {
			if (line.matches("(?i)^\\s*(Planning Time:|Execution Time:).*")) {
				continue;
			}
			Matcher m = TEXT_NODE.matcher(line);
			if (m.matches()) {
				if (current != null) {
					snippets.add(current.toString().stripTrailing());
				}
				current = new StringBuilder(line);
			} else if (current != null) {
				current.append('\n').append(line);
			}
		}
		if (current != null) {
			snippets.add(current.toString().stripTrailing());
		}
		return snippets;
	}

	private static List<OpRow> flattenProps(JsonObject node) {
		List<OpRow> rows = new ArrayList<>();
		flattenPropsInto(node, rows);
		return rows;
	}

	private static void flattenPropsInto(JsonObject node, List<OpRow> rows) {
		OpRow row = new OpRow();
		row.props = propsFromNode(node);
		rows.add(row);
		Object plans = node.get("Plans");
		if (plans instanceof JsonArray) {
			for (Object child : (JsonArray) plans) {
				if (child instanceof JsonObject) {
					flattenPropsInto((JsonObject) child, rows);
				}
			}
		}
	}

	private static List<OpRow> flattenJsonWithSnippets(JsonObject node, List<String> snippets) {
		List<OpRow> rows = new ArrayList<>();
		flattenJsonInto(node, rows, snippets);
		return rows;
	}

	private static void flattenJsonInto(JsonObject node, List<OpRow> rows, List<String> snippets) {
		JsonObject copy = shallowCopyWithoutPlans(node);
		snippets.add(pretty(copy, 0));
		OpRow row = new OpRow();
		row.props = propsFromNode(node);
		rows.add(row);
		Object plans = node.get("Plans");
		if (plans instanceof JsonArray) {
			for (Object child : (JsonArray) plans) {
				if (child instanceof JsonObject) {
					flattenJsonInto((JsonObject) child, rows, snippets);
				}
			}
		}
	}

	private static JsonObject shallowCopyWithoutPlans(JsonObject node) {
		JsonObject copy = new JsonObject();
		for (Object keyObj : node.keySet()) {
			String key = String.valueOf(keyObj);
			if ("Plans".equals(key)) {
				continue;
			}
			copy.put(key, node.get(key));
		}
		return copy;
	}

	private static Map<String, String> propsFromNode(JsonObject node) {
		Map<String, String> props = new LinkedHashMap<>();
		for (Object keyObj : node.keySet()) {
			String key = String.valueOf(keyObj);
			if ("Plans".equals(key) || "Plan".equals(key)) {
				continue;
			}
			Object val = node.get(key);
			if (val instanceof JsonObject || val instanceof JsonArray) {
				props.put(key, Jsoner.serialize(val));
			} else if (val instanceof Double || val instanceof Float) {
				props.put(key, formatNumber(((Number) val).doubleValue()));
			} else {
				props.put(key, String.valueOf(val));
			}
		}
		return props;
	}

	private static String formatNumber(double d) {
		if (d == Math.rint(d) && Math.abs(d) < 1e15) {
			if (d == (long) d) {
				return String.valueOf((long) d);
			}
		}
		String s = Double.toString(d);
		if (s.endsWith(".0")) {
			return s.substring(0, s.length() - 2) + ".00";
		}
		return s;
	}

	private static JsonObject extractPlanRoot(String jsonPlan) throws Exception {
		Object parsed = Jsoner.deserialize(jsonPlan);
		JsonObject wrapper;
		if (parsed instanceof JsonArray) {
			wrapper = (JsonObject) ((JsonArray) parsed).get(0);
		} else if (parsed instanceof JsonObject) {
			wrapper = (JsonObject) parsed;
		} else {
			throw new IllegalArgumentException("Unexpected JSON root");
		}
		Object plan = wrapper.get("Plan");
		if (!(plan instanceof JsonObject)) {
			throw new IllegalArgumentException("Missing Plan");
		}
		return (JsonObject) plan;
	}

	private static String pretty(Object value, int indent) {
		String pad = "  ".repeat(indent);
		String padIn = "  ".repeat(indent + 1);
		if (value instanceof JsonObject) {
			JsonObject obj = (JsonObject) value;
			if (obj.isEmpty()) {
				return "{}";
			}
			List<String> keys = orderedKeys(obj);
			StringBuilder sb = new StringBuilder("{\n");
			for (int i = 0; i < keys.size(); i++) {
				if (i > 0) {
					sb.append(",\n");
				}
				String key = keys.get(i);
				sb.append(padIn).append('"').append(key).append("\": ")
						.append(pretty(obj.get(key), indent + 1));
			}
			sb.append('\n').append(pad).append('}');
			return sb.toString();
		}
		if (value instanceof JsonArray) {
			JsonArray arr = (JsonArray) value;
			if (arr.isEmpty()) {
				return "[]";
			}
			boolean allScalar = true;
			for (Object el : arr) {
				if (el instanceof JsonObject || el instanceof JsonArray) {
					allScalar = false;
					break;
				}
			}
			if (allScalar && arr.size() <= 8) {
				return Jsoner.serialize(arr);
			}
			StringBuilder sb = new StringBuilder("[\n");
			for (int i = 0; i < arr.size(); i++) {
				if (i > 0) {
					sb.append(",\n");
				}
				sb.append(padIn).append(pretty(arr.get(i), indent + 1));
			}
			sb.append('\n').append(pad).append(']');
			return sb.toString();
		}
		if (value instanceof Double || value instanceof Float) {
			double d = ((Number) value).doubleValue();
			if (d == Math.rint(d) && Math.abs(d) < 1e12) {
				return String.format(Locale.ROOT, "%.2f", d);
			}
			return Jsoner.serialize(value);
		}
		return Jsoner.serialize(value);
	}

	/** A–Z key order so JSON plan text lines up with the properties panel. */
	private static List<String> orderedKeys(JsonObject obj) {
		TreeMap<String, Object> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Object keyObj : obj.keySet()) {
			sorted.put(String.valueOf(keyObj), obj.get(keyObj));
		}
		return new ArrayList<>(sorted.keySet());
	}

	private static String extractStatement(String explainSql) {
		String s = explainSql == null ? "" : explainSql.trim();
		Matcher m = EXPLAIN_PREFIX.matcher(s);
		if (m.find()) {
			return s.substring(m.end()).trim();
		}
		return s;
	}

	private static void runDml(String sql) {
		if (sql == null || sql.isBlank()) {
			return;
		}
		List<Object> r = Database.pg_dml(sql, ";");
		if ((Boolean) r.get(0)) {
			throw new IllegalStateException("DML failed: " + r.get(1));
		}
	}

	private static String fetchExplain(String sql) {
		List<Object> r = Database.pg_getPlan(sql);
		if ((Boolean) r.get(0)) {
			throw new IllegalStateException("EXPLAIN failed: " + r.get(1) + "\n" + sql);
		}
		String plan = (String) r.get(2);
		if (plan == null || plan.isBlank()) {
			throw new IllegalStateException("Empty EXPLAIN");
		}
		return plan;
	}

	private static DemoPlan loadDemo(String planName) throws Exception {
		try (Connection c = DriverManager.getConnection("jdbc:sqlite:postgresql-explain-plan-studio.sqlite");
				PreparedStatement ps = c.prepareStatement(""
						+ "select server, port, database, username, password, "
						+ "pre_explain_command, explain_sql_command, post_explain_command "
						+ "from explain_plans where plan_name = ?")) {
			ps.setString(1, planName);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				DemoPlan d = new DemoPlan();
				d.server = rs.getString(1);
				d.port = rs.getString(2);
				d.database = rs.getString(3);
				d.user = rs.getString(4);
				d.password = rs.getString(5) == null ? "" : rs.getString(5);
				d.pre = rs.getString(6);
				d.explainSql = rs.getString(7);
				d.post = rs.getString(8);
				return d;
			}
		}
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static final class DemoPlan {
		String server;
		String port;
		String database;
		String user;
		String password;
		String pre;
		String explainSql;
		String post;
	}

	private static final class OpRow {
		Map<String, String> props;
	}
}
