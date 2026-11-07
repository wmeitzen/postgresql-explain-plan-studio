package com.williammeitzen.peps.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

/**
 * Detects plan-field format and converts classic Postgres {@code EXPLAIN} text output
 * into a JSON array shaped like {@code FORMAT JSON} so PEPS diagram generation can reuse
 * the existing JSON pipeline.
 */
public final class ExplainTextPlanConverter {

	private static final Pattern COST_ONLY = Pattern.compile(
			"\\(cost=([\\d.]+)\\.\\.([\\d.]+)(?:\\s+rows=([\\d.]+))?(?:\\s+width=(\\d+))?\\)");
	/** ANALYZE may emit fractional rows (e.g. {@code rows=962031.00}). */
	private static final Pattern ACTUAL = Pattern.compile(
			"\\(actual time=([\\d.]+)\\.\\.([\\d.]+)\\s+rows=([\\d.]+)\\s+loops=(\\d+)\\)");
	private static final Pattern ACTUAL_NEVER = Pattern.compile(
			"\\(actual rows=([\\d.]+)\\s+loops=(\\d+)\\)");
	private static final Pattern BUFFERS = Pattern.compile(
			"^\\s*Buffers:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern WORKERS_PLANNED = Pattern.compile(
			"^\\s*Workers Planned:\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern WORKERS_LAUNCHED = Pattern.compile(
			"^\\s*Workers Launched:\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern OUTPUT = Pattern.compile(
			"^\\s*Output:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern HASH_COND = Pattern.compile(
			"^\\s*Hash Cond:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern JOIN_FILTER = Pattern.compile(
			"^\\s*Join Filter:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern FILTER = Pattern.compile(
			"^\\s*Filter:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern INDEX_COND = Pattern.compile(
			"^\\s*Index Cond:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern RECHECK_COND = Pattern.compile(
			"^\\s*Recheck Cond:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SORT_KEY = Pattern.compile(
			"^\\s*Sort Key:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SORT_METHOD = Pattern.compile(
			"^\\s*Sort Method:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern GROUP_KEY = Pattern.compile(
			"^\\s*Group Key:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern INIT_PLAN = Pattern.compile(
			"^\\s*InitPlan\\s+(\\d+)\\s*\\((.+)\\)\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern SUB_PLAN = Pattern.compile(
			"^\\s*SubPlan\\s+(\\d+)\\s*(?:\\((.+)\\))?\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern CTE_SCAN_NAME = Pattern.compile(
			"^\\s*CTE Name:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern INNER_UNIQUE = Pattern.compile(
			"^\\s*Inner Unique:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern OUTER_UNIQUE = Pattern.compile(
			"^\\s*Outer Unique:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern CACHE_KEY = Pattern.compile(
			"^\\s*Cache Key:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern CACHE_MODE = Pattern.compile(
			"^\\s*Cache Mode:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	/** Memoize: Hits: 1  Misses: 2  Evictions: 0  Overflows: 0  Memory Usage: 10kB */
	private static final Pattern MEMOIZE_STATS = Pattern.compile(
			"(?i)^\\s*Hits:\\s*(\\d+)\\s+Misses:\\s*(\\d+)\\s+Evictions:\\s*(\\d+)\\s+Overflows:\\s*(\\d+)"
					+ "(?:\\s+Memory Usage:\\s*([\\d.]+)\\s*kB)?\\s*$");
	private static final Pattern MEMORY_USAGE_KB = Pattern.compile(
			"^\\s*Memory Usage:\\s*([\\d.]+)\\s*kB\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern PEAK_MEMORY_USAGE = Pattern.compile(
			"^\\s*Peak Memory Usage:\\s*([\\d.]+)\\s*kB\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern RELATION = Pattern.compile(
			"^(?<type>.+?)\\s+on\\s+(?:(?:ONLY)\\s+)?(?<rel>(?:\"[^\"]+\"|[\\w.]+))(?:\\s+(?<alias>\"[^\"]+\"|[\\w]+))?\\s*$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern USING_INDEX = Pattern.compile(
			"^(?<type>.+?)\\s+using\\s+(?<index>\"[^\"]+\"|[\\w.]+)\\s+on\\s+(?<rel>(?:\"[^\"]+\"|[\\w.]+))(?:\\s+(?<alias>\"[^\"]+\"|[\\w]+))?\\s*$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern NODE_ARROW = Pattern.compile("^(\\s*)(?:->\\s*)?(.*\\S)\\s*$");
	private static final Pattern QUERY_PLAN_HEADER = Pattern.compile("(?i)^\\s*QUERY PLAN\\s*$");
	private static final Pattern DASH_RULE = Pattern.compile("^\\s*-{5,}\\s*$");
	private static final Pattern ROW_COUNT_FOOTER = Pattern.compile("^\\s*\\(\\d+\\s+rows?\\)\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern PLANNING_TIME = Pattern.compile(
			"^\\s*Planning(?:\\s+Time)?:\\s*([\\d.]+)\\s*ms\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern EXECUTION_TIME = Pattern.compile(
			"^\\s*Execution(?:\\s+Time)?:\\s*([\\d.]+)\\s*ms\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TEXT_HINT = Pattern.compile(
			"(?i)(->\\s+|\\(cost=|Seq Scan|Index Scan|Index Only Scan|Bitmap|Nested Loop|Hash Join|Merge Join|Sort|Limit|Append|Gather|Materialize|CTE Scan|Subquery Scan|Result|Function Scan|Values Scan|ModifyTable|Insert|Update|Delete|ProjectSet|WindowAgg|Aggregate|HashAggregate|GroupAggregate|Unique|SetOp|LockRows|WorkTable Scan|Recursive Union)");

	private ExplainTextPlanConverter() {
	}

	public static PlanFormat sniff(String planText) {
		if (planText == null) {
			return PlanFormat.EMPTY;
		}
		String trimmed = planText.trim();
		if (trimmed.isEmpty()) {
			return PlanFormat.EMPTY;
		}
		if (looksLikeJson(trimmed)) {
			return PlanFormat.JSON;
		}
		if (looksLikeTextPlan(trimmed)) {
			return PlanFormat.TEXT;
		}
		return PlanFormat.UNKNOWN;
	}

	public static boolean looksLikeJson(String trimmed) {
		if (trimmed == null || trimmed.isEmpty()) {
			return false;
		}
		char c = trimmed.charAt(0);
		if (c != '{' && c != '[') {
			return false;
		}
		try {
			Jsoner.deserialize(trimmed);
			return true;
		} catch (Exception ignored) {
			// May still be JSON with psql wrappers — normalize then retry.
		}
		String normalized = normalizePsqlWrappers(trimmed);
		if (normalized.isBlank()) {
			return false;
		}
		char n = normalized.trim().charAt(0);
		if (n != '{' && n != '[') {
			return false;
		}
		try {
			Jsoner.deserialize(normalized.trim());
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	public static boolean looksLikeTextPlan(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		if (looksLikeJson(text.trim())) {
			return false;
		}
		return TEXT_HINT.matcher(text).find();
	}

	/**
	 * Resolve plan-field content to a JSON array string suitable for PEPS diagram generation.
	 * JSON content is normalized; text content is converted. Returns null if unusable.
	 */
	public static String resolveToJsonArrayString(String planText) throws ExplainTextPlanConvertException {
		PlanFormat format = sniff(planText);
		if (format == PlanFormat.EMPTY || format == PlanFormat.UNKNOWN) {
			return null;
		}
		if (format == PlanFormat.JSON) {
			String normalized = normalizePsqlWrappers(planText).trim();
			Object parsed;
			try {
				parsed = Jsoner.deserialize(normalized);
			} catch (Exception e) {
				throw new ExplainTextPlanConvertException("Could not parse JSON plan.", e);
			}
			if (parsed instanceof JsonArray) {
				return Jsoner.serialize(parsed);
			}
			if (parsed instanceof JsonObject) {
				JsonArray wrap = new JsonArray();
				wrap.add(parsed);
				return Jsoner.serialize(wrap);
			}
			throw new ExplainTextPlanConvertException("JSON plan did not deserialize to an object or array.");
		}
		return convertTextToJsonArrayString(planText);
	}

	public static String convertTextToJsonArrayString(String planText) throws ExplainTextPlanConvertException {
		JsonObject rootPlan = convertTextToPlanObject(planText);
		JsonObject wrapper = new JsonObject();
		Object planningTime = rootPlan.remove("_PlanningTime");
		Object executionTime = rootPlan.remove("_ExecutionTime");
		if (planningTime != null) {
			wrapper.put("Planning Time", planningTime);
		}
		if (executionTime != null) {
			wrapper.put("Execution Time", executionTime);
		}
		wrapper.put("Plan", rootPlan);
		JsonArray array = new JsonArray();
		array.add(wrapper);
		return Jsoner.serialize(array);
	}

	public static JsonObject convertTextToPlanObject(String planText) throws ExplainTextPlanConvertException {
		List<String> lines = prepareLines(planText);
		if (lines.isEmpty()) {
			throw new ExplainTextPlanConvertException("Text plan is empty after removing headers.");
		}

		Double planningTime = null;
		Double executionTime = null;
		List<NodeLine> nodes = new ArrayList<>();

		for (String raw : lines) {
			Matcher planTime = PLANNING_TIME.matcher(raw);
			if (planTime.matches()) {
				planningTime = Double.valueOf(planTime.group(1));
				continue;
			}
			Matcher execTime = EXECUTION_TIME.matcher(raw);
			if (execTime.matches()) {
				executionTime = Double.valueOf(execTime.group(1));
				continue;
			}

			Matcher arrow = NODE_ARROW.matcher(raw);
			if (!arrow.matches()) {
				continue;
			}
			String indentWs = arrow.group(1);
			String body = arrow.group(2);
			boolean isChildMarker = raw.stripLeading().startsWith("->");
			int indent = indentWs.length();
			// Child marker lines: "  -> Seq Scan" — depth from spaces before ->
			if (isChildMarker) {
				indent = raw.indexOf("->");
			}

			if (isNodeHeader(body)) {
				JsonObject node = parseNodeHeader(body);
				nodes.add(new NodeLine(indent, node, true));
			} else if (!nodes.isEmpty()) {
				applyDetailLine(nodes.get(nodes.size() - 1).json, body);
			}
		}

		if (nodes.isEmpty()) {
			throw new ExplainTextPlanConvertException("Could not find any operators in text plan.");
		}

		JsonObject root = buildTree(nodes);
		if (planningTime != null || executionTime != null) {
			// Times belong on the wrapper in JSON format; callers may attach later.
			root.put("_PlanningTime", planningTime);
			root.put("_ExecutionTime", executionTime);
		}
		return root;
	}

	private static List<String> prepareLines(String planText) {
		String normalized = normalizePsqlWrappers(planText);
		String[] split = normalized.replace("\r\n", "\n").replace('\r', '\n').split("\n");
		List<String> lines = new ArrayList<>();
		for (String line : split) {
			if (line == null) {
				continue;
			}
			if (QUERY_PLAN_HEADER.matcher(line).matches()) {
				continue;
			}
			if (DASH_RULE.matcher(line).matches()) {
				continue;
			}
			if (ROW_COUNT_FOOTER.matcher(line).matches()) {
				continue;
			}
			if (line.isBlank()) {
				continue;
			}
			lines.add(line);
		}
		return lines;
	}

	public static String normalizePsqlWrappers(String planText) {
		if (planText == null) {
			return "";
		}
		String normalized = planText;
		// Only collapse long runs inside JSON payloads. Text EXPLAIN nesting is
		// space-indent based — collapsing spaces would flatten the tree.
		String trimmed = normalized.trim();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			normalized = normalized.replaceAll(" {5,}", " ");
		}
		normalized = normalized.replaceAll("(?m)^ *" + Pattern.quote("QUERY PLAN") + "\\s*$", "");
		normalized = normalized.replaceAll("(?m)^\\s*-{10,}\\s*$", "");
		normalized = normalized.replaceAll(Pattern.quote("+") + " *[\r\n]", "\n");
		normalized = normalized.replaceAll("(?m)^\\s*\\(\\d+\\s+rows?\\)\\s*$", "");
		return normalized;
	}

	/** Ignore non-operator detail lines that can look vaguely like node names. */
	private static boolean isIgnorableDetail(String body) {
		if (body == null) {
			return true;
		}
		String t = body.trim();
		if (t.isEmpty()) {
			return true;
		}
		// VERBOSE / ANALYZE decoration lines — never operators.
		if (t.startsWith("Buckets:") || t.startsWith("Batches:") || t.startsWith("Memory Usage:")
				|| t.startsWith("Heap Blocks:") || t.startsWith("Heap Fetches:")
				|| t.startsWith("Worker") && t.contains(":")
				|| t.startsWith("Inner Unique:") || t.startsWith("Outer Unique:")
				|| t.startsWith("Cache Key:") || t.startsWith("Cache Mode:")
				|| t.startsWith("Hits:")
				|| t.startsWith("Functions:") || t.startsWith("Function Call:")
				|| t.startsWith("Presorted Key:") || t.startsWith("Full-sort Groups:")
				|| t.startsWith("Pre-sorted Groups:") || t.startsWith("Tuples:")
				|| t.startsWith("Rows Removed by") || t.startsWith("Peak Memory Usage:")
				|| t.startsWith("Disk:") || t.startsWith("Conflict")
				|| t.startsWith("WAL:") || t.startsWith("JIT:")
				|| t.startsWith("Subplans Removed:") || t.startsWith("Target Tables:")
				|| t.matches("(?i)^Buckets:\\s*.*") || t.matches("(?i)^Memory:\\s*.*")) {
			return true;
		}
		// Detail keys are "Something: value" without cost=/actual — exclude unless Init/SubPlan.
		if (t.contains(":") && !t.contains("cost=") && !t.contains("actual ")
				&& !INIT_PLAN.matcher(t).matches() && !SUB_PLAN.matcher(t).matches()) {
			// Keep lines that are clearly node headers with a trailing colon-free type — already handled.
			// Colon detail lines (Output:, Sort Key:, …) are not nodes.
			return true;
		}
		return false;
	}

	private static boolean isNodeHeader(String body) {
		if (body == null || body.isBlank()) {
			return false;
		}
		if (isIgnorableDetail(body)) {
			return false;
		}
		if (body.startsWith("Buffers:") || body.startsWith("Output:") || body.startsWith("Workers ")
				|| body.startsWith("Hash Cond:") || body.startsWith("Join Filter:")
				|| body.startsWith("Filter:") || body.startsWith("Index Cond:")
				|| body.startsWith("Recheck Cond:") || body.startsWith("Sort Key:")
				|| body.startsWith("Sort Method:") || body.startsWith("Group Key:")
				|| body.startsWith("CTE Name:") || body.startsWith("Inner Unique:")
				|| body.startsWith("Outer Unique:") || body.startsWith("Planning")
				|| body.startsWith("Execution")) {
			return false;
		}
		if (INIT_PLAN.matcher(body).matches() || SUB_PLAN.matcher(body).matches()) {
			return true;
		}
		return COST_ONLY.matcher(body).find() || body.matches("(?i).*(Scan|Join|Loop|Sort|Limit|Append|Gather|Materialize|Aggregate|Result|Hash|Unique|SetOp|LockRows|ModifyTable|Insert|Update|Delete|WindowAgg|ProjectSet|Values|Function|WorkTable|Recursive Union|Bitmap|Nested Loop|Merge Attach|Incremental Sort|Memoize).*");
	}

	private static JsonObject parseNodeHeader(String body) throws ExplainTextPlanConvertException {
		JsonObject node = new JsonObject();

		Matcher init = INIT_PLAN.matcher(body);
		if (init.matches()) {
			node.put("Node Type", "Result");
			node.put("Parent Relationship", "InitPlan");
			node.put("Subplan Name", "InitPlan " + init.group(1) + " (" + init.group(2) + ")");
			return node;
		}
		Matcher sub = SUB_PLAN.matcher(body);
		if (sub.matches()) {
			node.put("Node Type", "Result");
			node.put("Parent Relationship", "SubPlan");
			String name = sub.group(2) == null ? ("SubPlan " + sub.group(1)) : ("SubPlan " + sub.group(1) + " (" + sub.group(2) + ")");
			node.put("Subplan Name", name);
			return node;
		}

		String withoutCosts = body;
		Matcher cost = COST_ONLY.matcher(body);
		if (cost.find()) {
			node.put("Startup Cost", Double.valueOf(cost.group(1)));
			node.put("Total Cost", Double.valueOf(cost.group(2)));
			if (cost.group(3) != null) {
				node.put("Plan Rows", parseRowCount(cost.group(3)));
			}
			if (cost.group(4) != null) {
				node.put("Plan Width", Long.valueOf(cost.group(4)));
			}
			withoutCosts = body.substring(0, cost.start()).trim();
		}

		Matcher actual = ACTUAL.matcher(body);
		if (actual.find()) {
			node.put("Actual Startup Time", Double.valueOf(actual.group(1)));
			node.put("Actual Total Time", Double.valueOf(actual.group(2)));
			node.put("Actual Rows", parseRowCount(actual.group(3)));
			node.put("Actual Loops", Long.valueOf(actual.group(4)));
		} else {
			Matcher never = ACTUAL_NEVER.matcher(body);
			if (never.find()) {
				node.put("Actual Rows", parseRowCount(never.group(1)));
				node.put("Actual Loops", Long.valueOf(never.group(2)));
			}
		}

		Matcher using = USING_INDEX.matcher(withoutCosts);
		if (using.matches()) {
			applyNormalizedNodeType(node, using.group("type"));
			node.put("Index Name", stripQuotes(using.group("index")));
			node.put("Relation Name", stripQuotes(using.group("rel")));
			if (using.group("alias") != null) {
				node.put("Alias", stripQuotes(using.group("alias")));
			}
			return node;
		}

		Matcher rel = RELATION.matcher(withoutCosts);
		if (rel.matches()) {
			applyNormalizedNodeType(node, rel.group("type"));
			node.put("Relation Name", stripQuotes(rel.group("rel")));
			if (rel.group("alias") != null) {
				node.put("Alias", stripQuotes(rel.group("alias")));
			}
			return node;
		}

		// Plain node type (Hash Join, Limit, Sort, Nested Loop Left Join, …)
		String type = withoutCosts.replaceAll("\\s+", " ").trim();
		if (type.isEmpty()) {
			throw new ExplainTextPlanConvertException("Empty node header: " + body);
		}
		applyNormalizedNodeType(node, type);
		return node;
	}

	/**
	 * Resolve a possibly text-native Node Type to the JSON base name used for icons
	 * (e.g. {@code Nested Loop Left Join} → {@code Nested Loop}, {@code HashAggregate} → {@code Aggregate}).
	 */
	public static String resolveIconNodeType(String nodeType, String joinType, String operation, String strategy) {
		if (nodeType == null || nodeType.isBlank()) {
			return nodeType == null ? "" : nodeType;
		}
		String t = nodeType.trim().replaceAll("\\s+", " ");
		Matcher parallel = Pattern.compile("(?i)^Parallel\\s+(.+)$").matcher(t);
		if (parallel.matches()) {
			t = parallel.group(1).trim();
		}
		Matcher dml = Pattern.compile("(?i)^(Insert|Update|Delete|Merge)$").matcher(t);
		if (dml.matches() || (operation != null && !operation.isBlank()
				&& t.equalsIgnoreCase("ModifyTable"))) {
			if (dml.matches()) {
				return "ModifyTable";
			}
			return "ModifyTable";
		}
		Matcher agg = Pattern.compile("(?i)^(Hash|Group|Mixed)Aggregate$").matcher(t);
		if (agg.matches()) {
			return "Aggregate";
		}
		Matcher partialAgg = Pattern.compile("(?i)^(Partial|Finalize)\\s+(Hash|Group|Mixed)?\\s*Aggregate$").matcher(t);
		if (partialAgg.matches()) {
			return "Aggregate";
		}
		Matcher join = Pattern.compile(
				"(?i)^(Nested Loop|Hash Join|Merge Join)(?:\\s+(Inner|Left|Right|Full|Semi|Anti))?\\s*(?:Join)?$")
				.matcher(t);
		if (join.matches()) {
			return normalizeJoinBase(join.group(1));
		}
		Matcher joinAlt = Pattern.compile(
				"(?i)^(Hash|Merge)\\s+(Inner|Left|Right|Full|Semi|Anti)\\s+Join$")
				.matcher(t);
		if (joinAlt.matches()) {
			return capitalizeWords(joinAlt.group(1) + " Join");
		}
		if (joinType != null && !joinType.isBlank()) {
			if (t.toLowerCase(Locale.ROOT).startsWith("nested loop")) {
				return "Nested Loop";
			}
			if (t.toLowerCase(Locale.ROOT).startsWith("hash")) {
				return "Hash Join";
			}
			if (t.toLowerCase(Locale.ROOT).startsWith("merge") && !t.equalsIgnoreCase("Merge Append")) {
				return "Merge Join";
			}
		}
		if (strategy != null && !strategy.isBlank() && t.toLowerCase(Locale.ROOT).contains("aggregate")) {
			return "Aggregate";
		}
		return t;
	}

	/**
	 * Keep the text-native Node Type for properties, and also emit JSON sibling fields
	 * (Join Type / Parallel Aware / Strategy / Operation) so icons still resolve.
	 */
	static void applyNormalizedNodeType(JsonObject node, String rawType) {
		String original = rawType == null ? "" : rawType.trim().replaceAll("\\s+", " ");
		if (original.isEmpty()) {
			node.put("Node Type", original);
			return;
		}

		String t = original;

		// Parallel Seq Scan / Parallel Index Scan / …
		Matcher parallel = Pattern.compile("(?i)^Parallel\\s+(.+)$").matcher(t);
		if (parallel.matches()) {
			node.put("Parallel Aware", Boolean.TRUE);
			// Keep full text name (Parallel Seq Scan) as Node Type.
			node.put("Node Type", capitalizeWords(original));
			return;
		}

		// Insert/Update/Delete/Merge — text Node Type stays Insert/…; Operation for icons.
		Matcher dml = Pattern.compile("(?i)^(Insert|Update|Delete|Merge)$").matcher(t);
		if (dml.matches()) {
			String op = capitalizeWord(dml.group(1));
			node.put("Node Type", op);
			node.put("Operation", op);
			return;
		}

		// HashAggregate / GroupAggregate / MixedAggregate — keep text name; Strategy for icons.
		Matcher agg = Pattern.compile("(?i)^(Hash|Group|Mixed)Aggregate$").matcher(t);
		if (agg.matches()) {
			// Preserve Postgres text casing (HashAggregate), not Title-Case mangling.
			node.put("Node Type", aggregateTextLabel(agg.group(1)));
			node.put("Strategy", aggregateStrategyFromText(agg.group(1)));
			node.put("Partial Mode", "Simple");
			return;
		}
		Matcher partialAgg = Pattern.compile("(?i)^(Partial|Finalize)\\s+(Hash|Group|Mixed)?\\s*Aggregate$").matcher(t);
		if (partialAgg.matches()) {
			String mode = capitalizeWord(partialAgg.group(1));
			String label = mode;
			if (partialAgg.group(2) != null) {
				label += " " + aggregateTextLabel(partialAgg.group(2));
			} else {
				label += " Aggregate";
			}
			node.put("Node Type", label);
			node.put("Partial Mode", mode);
			if (partialAgg.group(2) != null) {
				node.put("Strategy", aggregateStrategyFromText(partialAgg.group(2)));
			} else {
				node.put("Strategy", "Plain");
			}
			return;
		}
		if (t.equalsIgnoreCase("Aggregate")) {
			node.put("Node Type", "Aggregate");
			if (!node.containsKey("Strategy")) {
				node.put("Strategy", "Plain");
			}
			if (!node.containsKey("Partial Mode")) {
				node.put("Partial Mode", "Simple");
			}
			return;
		}

		// Nested Loop Left Join / Hash Left Join / … — keep text name; Join Type for icons.
		Matcher join = Pattern.compile(
				"(?i)^(Nested Loop|Hash Join|Merge Join)(?:\\s+(Inner|Left|Right|Full|Semi|Anti))?\\s*(?:Join)?$")
				.matcher(t);
		if (join.matches()) {
			String joinType = join.group(2);
			node.put("Node Type", capitalizeWords(original));
			node.put("Join Type", joinType == null ? "Inner" : capitalizeWord(joinType));
			return;
		}
		Matcher joinAlt = Pattern.compile(
				"(?i)^(Hash|Merge)\\s+(Inner|Left|Right|Full|Semi|Anti)\\s+Join$")
				.matcher(t);
		if (joinAlt.matches()) {
			node.put("Node Type", capitalizeWords(original));
			node.put("Join Type", capitalizeWord(joinAlt.group(2)));
			return;
		}
		if (t.equalsIgnoreCase("Nested Loop") || t.equalsIgnoreCase("Hash Join") || t.equalsIgnoreCase("Merge Join")) {
			node.put("Node Type", capitalizeWords(t));
			node.put("Join Type", "Inner");
			return;
		}

		node.put("Node Type", capitalizeWords(t));
	}

	/** Postgres may print {@code rows=10} or {@code rows=10.00}. */
	private static Long parseRowCount(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return Long.valueOf(Math.round(Double.parseDouble(raw.trim())));
	}

	private static String aggregateTextLabel(String token) {
		if (token == null) {
			return "Aggregate";
		}
		if (token.equalsIgnoreCase("Hash")) {
			return "HashAggregate";
		}
		if (token.equalsIgnoreCase("Group")) {
			return "GroupAggregate";
		}
		if (token.equalsIgnoreCase("Mixed")) {
			return "MixedAggregate";
		}
		return "Aggregate";
	}

	/** Text HashAggregate → JSON Strategy Hashed; Group → Sorted; Mixed → Mixed. */
	private static String aggregateStrategyFromText(String token) {
		if (token == null) {
			return "Plain";
		}
		if (token.equalsIgnoreCase("Hash")) {
			return "Hashed";
		}
		if (token.equalsIgnoreCase("Group")) {
			return "Sorted";
		}
		if (token.equalsIgnoreCase("Mixed")) {
			return "Mixed";
		}
		return capitalizeWord(token);
	}

	private static String normalizeJoinBase(String base) {
		String b = base.trim().replaceAll("\\s+", " ");
		if (b.equalsIgnoreCase("Nested Loop")) {
			return "Nested Loop";
		}
		if (b.equalsIgnoreCase("Hash Join")) {
			return "Hash Join";
		}
		if (b.equalsIgnoreCase("Merge Join")) {
			return "Merge Join";
		}
		return capitalizeWords(b);
	}

	private static String capitalizeWord(String word) {
		if (word == null || word.isEmpty()) {
			return word;
		}
		String w = word.trim();
		return Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase(Locale.ROOT);
	}

	private static String capitalizeWords(String text) {
		String[] parts = text.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			// Keep well-known acronyms / camel tokens
			if (part.equalsIgnoreCase("CTE")) {
				sb.append("CTE");
			} else if (part.equalsIgnoreCase("Tid")) {
				sb.append("Tid");
			} else if (part.equalsIgnoreCase("SetOp")) {
				sb.append("SetOp");
			} else if (part.equalsIgnoreCase("LockRows")) {
				sb.append("LockRows");
			} else if (part.equalsIgnoreCase("ModifyTable")) {
				sb.append("ModifyTable");
			} else if (part.equalsIgnoreCase("WindowAgg")) {
				sb.append("WindowAgg");
			} else if (part.equalsIgnoreCase("ProjectSet")) {
				sb.append("ProjectSet");
			} else if (part.equalsIgnoreCase("BitmapAnd")) {
				sb.append("BitmapAnd");
			} else if (part.equalsIgnoreCase("BitmapOr")) {
				sb.append("BitmapOr");
			} else {
				sb.append(capitalizeWord(part));
			}
		}
		return sb.toString();
	}

	private static String stripQuotes(String name) {
		if (name == null) {
			return null;
		}
		String n = name.trim();
		if (n.contains(".")) {
			// schema.table — take table part for Relation Name; keep full in Alias if needed
			String[] parts = n.split("\\.");
			n = parts[parts.length - 1];
		}
		if (n.startsWith("\"") && n.endsWith("\"") && n.length() >= 2) {
			return n.substring(1, n.length() - 1);
		}
		return n;
	}

	private static void applyDetailLine(JsonObject node, String body) {
		Matcher buffers = BUFFERS.matcher(body);
		if (buffers.matches()) {
			parseBuffers(node, buffers.group(1));
			return;
		}
		Matcher workersPlanned = WORKERS_PLANNED.matcher(body);
		if (workersPlanned.matches()) {
			node.put("Workers Planned", Long.valueOf(workersPlanned.group(1)));
			return;
		}
		Matcher workersLaunched = WORKERS_LAUNCHED.matcher(body);
		if (workersLaunched.matches()) {
			node.put("Workers Launched", Long.valueOf(workersLaunched.group(1)));
			return;
		}
		Matcher output = OUTPUT.matcher(body);
		if (output.matches()) {
			JsonArray outs = new JsonArray();
			for (String part : splitCommaArgs(output.group(1))) {
				outs.add(part);
			}
			node.put("Output", outs);
			return;
		}
		putIfMatch(node, body, HASH_COND, "Hash Cond");
		putIfMatch(node, body, JOIN_FILTER, "Join Filter");
		putIfMatch(node, body, FILTER, "Filter");
		putIfMatch(node, body, INDEX_COND, "Index Cond");
		putIfMatch(node, body, RECHECK_COND, "Recheck Cond");
		putIfMatch(node, body, SORT_METHOD, "Sort Method");
		putIfMatch(node, body, CTE_SCAN_NAME, "CTE Name");
		putBooleanIfMatch(node, body, INNER_UNIQUE, "Inner Unique");
		putBooleanIfMatch(node, body, OUTER_UNIQUE, "Outer Unique");
		putIfMatch(node, body, CACHE_KEY, "Cache Key");
		putIfMatch(node, body, CACHE_MODE, "Cache Mode");

		Matcher memoStats = MEMOIZE_STATS.matcher(body);
		if (memoStats.matches()) {
			node.put("Cache Hits", Long.valueOf(memoStats.group(1)));
			node.put("Cache Misses", Long.valueOf(memoStats.group(2)));
			node.put("Cache Evictions", Long.valueOf(memoStats.group(3)));
			node.put("Cache Overflows", Long.valueOf(memoStats.group(4)));
			if (memoStats.group(5) != null) {
				node.put("Peak Memory Usage", Double.valueOf(memoStats.group(5)));
			}
			return;
		}
		Matcher memKb = MEMORY_USAGE_KB.matcher(body);
		if (memKb.matches()) {
			node.put("Peak Memory Usage", Double.valueOf(memKb.group(1)));
			return;
		}
		Matcher peakMem = PEAK_MEMORY_USAGE.matcher(body);
		if (peakMem.matches()) {
			node.put("Peak Memory Usage", Double.valueOf(peakMem.group(1)));
			return;
		}

		Matcher sortKey = SORT_KEY.matcher(body);
		if (sortKey.matches()) {
			JsonArray keys = new JsonArray();
			for (String part : splitCommaArgs(sortKey.group(1))) {
				keys.add(part);
			}
			node.put("Sort Key", keys);
			return;
		}
		Matcher groupKey = GROUP_KEY.matcher(body);
		if (groupKey.matches()) {
			JsonArray keys = new JsonArray();
			for (String part : splitCommaArgs(groupKey.group(1))) {
				keys.add(part);
			}
			node.put("Group Key", keys);
		}
	}

	private static void putIfMatch(JsonObject node, String body, Pattern pattern, String jsonKey) {
		Matcher m = pattern.matcher(body);
		if (m.matches()) {
			node.put(jsonKey, m.group(1).trim());
		}
	}

	private static void putBooleanIfMatch(JsonObject node, String body, Pattern pattern, String jsonKey) {
		Matcher m = pattern.matcher(body);
		if (m.matches()) {
			String v = m.group(1).trim();
			node.put(jsonKey, "true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v));
		}
	}

	private static void parseBuffers(JsonObject node, String buffersText) {
		// e.g. shared hit=3 read=1, temp read=2 written=1
		Matcher m = Pattern.compile("(?i)(shared|local|temp)\\s+((?:hit|read|dirtied|written)=\\d+(?:\\s+(?:hit|read|dirtied|written)=\\d+)*)")
				.matcher(buffersText);
		while (m.find()) {
			String scope = m.group(1).toLowerCase(Locale.ROOT);
			String rest = m.group(2);
			Matcher kv = Pattern.compile("(?i)(hit|read|dirtied|written)=(\\d+)").matcher(rest);
			while (kv.find()) {
				String kind = kv.group(1).toLowerCase(Locale.ROOT);
				long value = Long.parseLong(kv.group(2));
				String key = bufferJsonKey(scope, kind);
				if (key != null) {
					node.put(key, value);
				}
			}
		}
	}

	private static String bufferJsonKey(String scope, String kind) {
		String scopeLabel;
		if ("shared".equals(scope)) {
			scopeLabel = "Shared";
		} else if ("local".equals(scope)) {
			scopeLabel = "Local";
		} else if ("temp".equals(scope)) {
			scopeLabel = "Temp";
		} else {
			return null;
		}
		String kindLabel;
		if ("hit".equals(kind)) {
			kindLabel = "Hit Blocks";
		} else if ("read".equals(kind)) {
			kindLabel = "Read Blocks";
		} else if ("dirtied".equals(kind)) {
			kindLabel = "Dirtied Blocks";
		} else if ("written".equals(kind)) {
			kindLabel = "Written Blocks";
		} else {
			return null;
		}
		return scopeLabel + " " + kindLabel;
	}

	private static List<String> splitCommaArgs(String text) {
		List<String> parts = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		int depth = 0;
		boolean inQuotes = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
				inQuotes = !inQuotes;
			}
			if (!inQuotes) {
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth = Math.max(0, depth - 1);
				} else if (c == ',' && depth == 0) {
					parts.add(cur.toString().trim());
					cur.setLength(0);
					continue;
				}
			}
			cur.append(c);
		}
		if (cur.length() > 0) {
			parts.add(cur.toString().trim());
		}
		return parts;
	}

	private static JsonObject buildTree(List<NodeLine> nodes) throws ExplainTextPlanConvertException {
		Deque<NodeLine> stack = new ArrayDeque<>();
		NodeLine root = nodes.get(0);
		stack.push(root);

		for (int i = 1; i < nodes.size(); i++) {
			NodeLine current = nodes.get(i);
			while (stack.size() > 1 && stack.peek().indent >= current.indent) {
				stack.pop();
			}
			if (stack.isEmpty()) {
				throw new ExplainTextPlanConvertException("Invalid text plan nesting near: "
						+ String.valueOf(current.json.get("Node Type")));
			}
			NodeLine parent = stack.peek();
			JsonArray plans = (JsonArray) parent.json.get("Plans");
			if (plans == null) {
				plans = new JsonArray();
				parent.json.put("Plans", plans);
			}
			if (!current.json.containsKey("Parent Relationship")) {
				current.json.put("Parent Relationship", plans.isEmpty() ? "Outer" : "Inner");
			}
			plans.add(current.json);
			stack.push(current);
		}
		return root.json;
	}

	private static final class NodeLine {
		final int indent;
		final JsonObject json;

		NodeLine(int indent, JsonObject json, boolean unused) {
			this.indent = indent;
			this.json = json;
		}
	}

	public static final class ExplainTextPlanConvertException extends Exception {
		private static final long serialVersionUID = 1L;

		public ExplainTextPlanConvertException(String message) {
			super(message);
		}

		public ExplainTextPlanConvertException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
