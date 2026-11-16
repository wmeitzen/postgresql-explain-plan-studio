package com.williammeitzen.peps.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

/**
 * Just-in-time anonymization of EXPLAIN SQL/plan text for safe sharing.
 * Builds one deterministic map used for both scrubbing and reverse translation.
 */
public final class PlanAnonymizer {

	private static final Pattern IDENT = Pattern.compile(
			"(?<![\\w$])([A-Za-z_][\\w$]*|\"[^\"]+\")(?![\\w$])");
	private static final Pattern STRING_LITERAL = Pattern.compile(
			"'(?:[^']|'')*'");
	private static final Pattern NUMBER_LITERAL = Pattern.compile(
			"(?<![\\w.])(\\d+(?:\\.\\d+)?)(?![\\w.])");
	private static final Pattern PARTITION_SUFFIX = Pattern.compile(
			"^(.+?)_(\\d+)$");
	/** FROM/JOIN/UPDATE/INTO relations in SQL (not alias.column). */
	private static final Pattern SQL_RELATION = Pattern.compile(
			"(?i)\\b(?:from|join|update|into)\\s+(?:only\\s+)?"
					+ "(?:((?:\"[^\"]+\"|[A-Za-z_][\\w$]*))\\s*\\.\\s*)?"
					+ "((?:\"[^\"]+\"|[A-Za-z_][\\w$]*))"
					+ "(?:\\s+(?:as\\s+)?((?:\"[^\"]+\"|[A-Za-z_][\\w$]*)))?");

	private static final Set<String> KEEP_SCHEMAS = Set.of(
			"public", "pg_catalog", "pg_toast", "information_schema");

	private static final Set<String> KEEP_IDENTIFIERS;
	static {
		Set<String> keep = new HashSet<>();
		String[] words = {
				"select", "from", "where", "and", "or", "not", "null", "true", "false",
				"join", "left", "right", "inner", "outer", "full", "cross", "on", "using",
				"as", "in", "is", "like", "ilike", "between", "exists", "case", "when",
				"then", "else", "end", "group", "by", "order", "limit", "offset", "having",
				"union", "all", "except", "intersect", "with", "recursive", "insert",
				"update", "delete", "into", "values", "set", "returning", "distinct",
				"count", "sum", "avg", "min", "max", "coalesce", "nullif", "cast",
				"explain", "analyze", "buffers", "verbose", "format", "json", "text",
				"xml", "yaml", "costs", "timing", "summary", "settings", "wal",
				"only", "tablesample", "lateral", "window", "over", "partition",
				"rows", "range", "groups", "filter", "within", "array", "row",
				"ctid", "tableoid", "xmin", "xmax", "cmin", "cmax",
				"pl", "worker", "workers", "gather", "parallel",
				"integer", "bigint", "smallint", "numeric", "real", "double", "precision",
				"boolean", "text", "varchar", "char", "bytea", "jsonb", "uuid", "date",
				"timestamp", "timestamptz", "time", "interval", "oid", "name", "regclass",
				"any", "anyelement", "anyarray", "record", "void",
				"asc", "desc", "nulls", "first", "last", "collate", "default"
		};
		for (String w : words) {
			keep.add(w);
		}
		KEEP_IDENTIFIERS = Collections.unmodifiableSet(keep);
	}

	private static final String[] PLAN_NAME_KEYS = {
			"Schema", "Relation Name", "Index Name", "CTE Name", "Subplan Name",
			"Function Name", "Table Function Name", "Trigger Name", "Constraint Name"
	};

	private PlanAnonymizer() {
	}

	public static final class Result {
		public final String preCommand;
		public final String postCommand;
		public final String sql;
		public final String plan;
		/** anonymized token → original */
		public final Map<String, String> reverseMap;
		/** original → anonymized token */
		public final Map<String, String> forwardMap;

		Result(String preCommand, String postCommand, String sql, String plan,
				Map<String, String> forwardMap, Map<String, String> reverseMap) {
			this.preCommand = preCommand == null ? "" : preCommand;
			this.postCommand = postCommand == null ? "" : postCommand;
			this.sql = sql == null ? "" : sql;
			this.plan = plan == null ? "" : plan;
			this.forwardMap = Collections.unmodifiableMap(new LinkedHashMap<>(forwardMap));
			this.reverseMap = Collections.unmodifiableMap(new LinkedHashMap<>(reverseMap));
		}
	}

	public static Result anonymize(String preCommand, String postCommand, String sql, String plan,
			boolean anonymizeAliases) {
		Collector collector = new Collector(anonymizeAliases);
		collector.scanText(preCommand);
		collector.scanText(postCommand);
		collector.scanText(sql);
		collector.scanPlan(plan);

		Map<String, String> forward = collector.buildForwardMap();
		Map<String, String> reverse = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : forward.entrySet()) {
			reverse.put(e.getValue(), e.getKey());
		}

		return new Result(
				applyForward(preCommand, forward),
				applyForward(postCommand, forward),
				applyForward(sql, forward),
				applyForward(plan, forward),
				forward,
				reverse);
	}

	/** Apply reverse map (anonymized → original). Longest tokens first. */
	public static String applyReverse(String text, Map<String, String> reverseMap) {
		if (text == null || text.isEmpty() || reverseMap == null || reverseMap.isEmpty()) {
			return text == null ? "" : text;
		}
		return applyMap(text, reverseMap);
	}

	/**
	 * Tokens in {@code text} that look like our anonymized placeholders but are not in the map.
	 */
	public static List<String> unmatchedAnonymizedTokens(String text, Map<String, String> reverseMap) {
		List<String> unmatched = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return unmatched;
		}
		Set<String> known = reverseMap == null ? Set.of() : reverseMap.keySet();
		Pattern placeholder = Pattern.compile(
				"(?<![\\w$])((?:schema|table|index|cte|alias|column|function|constraint|trigger|subplan|value)_[a-z]+(?:_\\d+)?)(?![\\w$])",
				Pattern.CASE_INSENSITIVE);
		Matcher m = placeholder.matcher(text);
		Set<String> seen = new HashSet<>();
		while (m.find()) {
			String token = m.group(1);
			String key = token.toLowerCase(Locale.ROOT);
			if (!known.contains(token) && !knownContainsIgnoreCase(known, token) && seen.add(key)) {
				unmatched.add(token);
			}
		}
		return unmatched;
	}

	private static boolean knownContainsIgnoreCase(Set<String> known, String token) {
		for (String k : known) {
			if (k.equalsIgnoreCase(token)) {
				return true;
			}
		}
		return false;
	}

	private static String applyForward(String text, Map<String, String> forward) {
		if (text == null || text.isEmpty() || forward.isEmpty()) {
			return text == null ? "" : text;
		}
		return applyMap(text, forward);
	}

	private static String applyMap(String text, Map<String, String> map) {
		List<String> keys = new ArrayList<>(map.keySet());
		keys.sort(Comparator.comparingInt(String::length).reversed().thenComparing(s -> s));
		String result = text;
		for (String key : keys) {
			String replacement = map.get(key);
			if (replacement == null || key.isEmpty() || key.equals(replacement)) {
				continue;
			}
			// SQL/plan string literals are mapped as "'foo'" → "'value_a'".
			if (key.length() >= 2 && key.charAt(0) == '\'' && key.charAt(key.length() - 1) == '\'') {
				result = replaceLiteral(result, key, replacement);
			} else if (key.matches("\\d+(?:\\.\\d+)?")) {
				result = replaceNumberLiteral(result, key, replacement);
			} else {
				result = replaceWholeIdentifier(result, key, replacement);
			}
		}
		return result;
	}

	private static String replaceLiteral(String text, String from, String to) {
		StringBuilder out = new StringBuilder();
		Matcher m = STRING_LITERAL.matcher(text);
		int last = 0;
		while (m.find()) {
			out.append(text, last, m.start());
			out.append(m.group().equals(from) ? to : m.group());
			last = m.end();
		}
		out.append(text.substring(last));
		return out.toString();
	}

	private static String replaceNumberLiteral(String text, String from, String to) {
		StringBuilder out = new StringBuilder();
		Matcher m = NUMBER_LITERAL.matcher(text);
		int last = 0;
		while (m.find()) {
			out.append(text, last, m.start());
			out.append(m.group(1).equals(from) ? to : m.group(1));
			last = m.end();
		}
		out.append(text.substring(last));
		return out.toString();
	}

	private static String replaceWholeIdentifier(String text, String from, String to) {
		String unquotedFrom = stripQuotes(from);
		String unquotedTo = stripQuotes(to);
		boolean fromWasQuotedForm = from.length() >= 2 && from.startsWith("\"") && from.endsWith("\"");
		StringBuilder out = new StringBuilder();
		Matcher m = IDENT.matcher(text);
		int last = 0;
		while (m.find()) {
			String token = m.group(1);
			boolean tokenQuoted = token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"");
			String tokenBare = stripQuotes(token);
			out.append(text, last, m.start());
			if (tokenQuoted) {
				// JSON string values are one IDENT token ("s_2._strain_key"). Replace the
				// whole value when it matches, otherwise replace idents inside it.
				boolean exact = fromWasQuotedForm
						? token.equals(from) || tokenBare.equals(unquotedFrom)
						: tokenBare.equalsIgnoreCase(unquotedFrom);
				if (exact) {
					out.append("\"").append(unquotedTo).append("\"");
				} else {
					out.append("\"")
							.append(replaceUnquotedIdent(tokenBare, unquotedFrom, unquotedTo))
							.append("\"");
				}
			} else {
				boolean match = fromWasQuotedForm
						? tokenBare.equals(unquotedFrom)
						: tokenBare.equalsIgnoreCase(unquotedFrom) || token.equals(from);
				out.append(match ? unquotedTo : token);
			}
			last = m.end();
		}
		out.append(text.substring(last));
		return out.toString();
	}

	/** Replace a bare identifier inside an already-unquoted string (e.g. Output exprs). */
	private static String replaceUnquotedIdent(String text, String from, String to) {
		if (text == null || from == null || from.isEmpty()) {
			return text;
		}
		Matcher m = Pattern.compile("(?i)(?<![\\w$])" + Pattern.quote(from) + "(?![\\w$])").matcher(text);
		return m.replaceAll(Matcher.quoteReplacement(to));
	}

	private static String stripQuotes(String ident) {
		if (ident != null && ident.length() >= 2 && ident.startsWith("\"") && ident.endsWith("\"")) {
			return ident.substring(1, ident.length() - 1);
		}
		return ident == null ? "" : ident;
	}

	private static final class Collector {
		private final boolean anonymizeAliases;
		private final Set<String> schemas = new HashSet<>();
		private final Set<String> relations = new HashSet<>();
		private final Set<String> indexes = new HashSet<>();
		private final Set<String> ctes = new HashSet<>();
		private final Set<String> subplans = new HashSet<>();
		private final Set<String> aliases = new HashSet<>();
		private final Set<String> functions = new HashSet<>();
		private final Set<String> columns = new HashSet<>();
		private final Set<String> stringValues = new HashSet<>();
		private final Set<String> numberValues = new HashSet<>();

		Collector(boolean anonymizeAliases) {
			this.anonymizeAliases = anonymizeAliases;
		}

		void scanPlan(String plan) {
			if (plan == null || plan.isBlank()) {
				return;
			}
			Object parsed = tryParseJson(plan);
			if (parsed != null) {
				walkJson(parsed);
				return;
			}
			// Text plan: collect names/literals from known property lines only — never
			// scrub costs/timings/rows that appear as free numbers in the plan body.
			scanTextPlanLines(plan);
		}

		void scanText(String text) {
			if (text == null || text.isEmpty()) {
				return;
			}
			// Relations from FROM/JOIN/… — do not treat alias.column as schema.table.
			Matcher rel = SQL_RELATION.matcher(text);
			while (rel.find()) {
				String schema = rel.group(1);
				String relation = rel.group(2);
				String alias = rel.group(3);
				if (schema != null) {
					registerSchema(stripQuotes(schema));
				}
				if (relation != null) {
					registerRelation(stripQuotes(relation));
				}
				if (anonymizeAliases && alias != null
						&& !isSqlKeyword(stripQuotes(alias))) {
					registerAlias(stripQuotes(alias));
				}
			}
			Matcher lit = STRING_LITERAL.matcher(text);
			while (lit.find()) {
				registerStringLiteral(lit.group());
			}
		}

		void scanTextPlanLines(String plan) {
			for (String line : plan.split("\\R")) {
				String trimmed = line.trim();
				int colon = trimmed.indexOf(':');
				if (colon <= 0) {
					continue;
				}
				String key = trimmed.substring(0, colon).trim();
				String value = trimmed.substring(colon + 1).trim();
				handleJsonStringField(key, value);
			}
		}

		private void walkJson(Object node) {
			if (node instanceof JsonObject) {
				JsonObject obj = (JsonObject) node;
				for (Object keyObj : obj.keySet()) {
					String key = String.valueOf(keyObj);
					Object value = obj.get(keyObj);
					if (value instanceof CharSequence) {
						handleJsonStringField(key, value.toString());
					} else if (value instanceof JsonArray) {
						JsonArray arr = (JsonArray) value;
						if ("Output".equalsIgnoreCase(key) || "Hash Key".equalsIgnoreCase(key)
								|| "Group Key".equalsIgnoreCase(key) || "Sort Key".equalsIgnoreCase(key)) {
							for (int i = 0; i < arr.size(); i++) {
								Object el = arr.get(i);
								if (el != null) {
									scanExpression(String.valueOf(el));
								}
							}
						}
						walkJson(value);
					} else if (value != null && !(value instanceof Number) && !(value instanceof Boolean)) {
						walkJson(value);
					} else {
						walkJson(value);
					}
				}
			} else if (node instanceof JsonArray) {
				JsonArray arr = (JsonArray) node;
				for (int i = 0; i < arr.size(); i++) {
					walkJson(arr.get(i));
				}
			}
		}

		private void handleJsonStringField(String key, String value) {
			if (value == null || value.isEmpty()) {
				return;
			}
			if ("Schema".equalsIgnoreCase(key)) {
				registerSchema(value);
				return;
			}
			if ("Relation Name".equalsIgnoreCase(key)) {
				registerRelation(value);
				return;
			}
			if ("Index Name".equalsIgnoreCase(key)) {
				registerIndex(value);
				return;
			}
			if ("CTE Name".equalsIgnoreCase(key)) {
				registerCte(value);
				return;
			}
			if ("Subplan Name".equalsIgnoreCase(key)) {
				registerSubplan(value);
				return;
			}
			if ("Alias".equalsIgnoreCase(key)) {
				if (anonymizeAliases) {
					registerAlias(value);
				}
				return;
			}
			if ("Function Name".equalsIgnoreCase(key) || "Table Function Name".equalsIgnoreCase(key)) {
				registerFunction(value);
				return;
			}
			for (String planKey : PLAN_NAME_KEYS) {
				if (planKey.equalsIgnoreCase(key)) {
					registerRelation(value);
					return;
				}
			}
			if (key.toLowerCase(Locale.ROOT).contains("filter")
					|| key.toLowerCase(Locale.ROOT).contains("cond")
					|| "Output".equalsIgnoreCase(key)) {
				scanExpression(value);
			}
		}

		private void scanExpression(String expr) {
			if (expr == null || expr.isEmpty()) {
				return;
			}
			Matcher lit = STRING_LITERAL.matcher(expr);
			while (lit.find()) {
				registerStringLiteral(lit.group());
			}
			Matcher num = NUMBER_LITERAL.matcher(expr);
			while (num.find()) {
				registerNumberLiteral(num.group(1));
			}
			Matcher id = IDENT.matcher(expr);
			while (id.find()) {
				String raw = id.group(1);
				String name = stripQuotes(raw);
				if (shouldKeepIdentifier(name)) {
					continue;
				}
				if (schemas.contains(name) || relations.contains(name) || indexes.contains(name)
						|| ctes.contains(name) || aliases.contains(name) || functions.contains(name)) {
					continue;
				}
				// Heuristic: identifiers in expressions that aren't known objects → columns
				if (name.length() > 1 || raw.startsWith("\"")) {
					columns.add(name);
				}
			}
		}

		private void registerSchema(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			String n = stripQuotes(name.trim());
			if (KEEP_SCHEMAS.contains(n.toLowerCase(Locale.ROOT))) {
				return;
			}
			schemas.add(n);
		}

		private void registerRelation(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			String n = stripQuotes(name.trim());
			if (shouldKeepIdentifier(n) || n.startsWith("pg_")) {
				return;
			}
			relations.add(n);
		}

		private void registerIndex(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			String n = stripQuotes(name.trim());
			if (shouldKeepIdentifier(n) || n.startsWith("pg_")) {
				return;
			}
			indexes.add(n);
		}

		private void registerCte(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			ctes.add(stripQuotes(name.trim()));
		}

		private void registerSubplan(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			// Keep structural labels like "InitPlan 1 (returns $0)"; only map simple identifiers.
			String n = stripQuotes(name.trim());
			if (!n.matches("[A-Za-z_][\\w$]*")) {
				return;
			}
			subplans.add(n);
		}

		private void registerAlias(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			String n = stripQuotes(name.trim());
			if (n.startsWith("*SELECT*") || n.startsWith("*VALUES*")) {
				return;
			}
			aliases.add(n);
		}

		private void registerFunction(String name) {
			if (name == null || name.isBlank()) {
				return;
			}
			String n = stripQuotes(name.trim());
			if (shouldKeepIdentifier(n) || n.startsWith("pg_")) {
				return;
			}
			functions.add(n);
		}

		private void registerStringLiteral(String literal) {
			if (literal == null || literal.length() < 2) {
				return;
			}
			stringValues.add(literal);
		}

		private void registerNumberLiteral(String number) {
			if (number == null || number.isBlank()) {
				return;
			}
			// Skip tiny structural numbers that appear everywhere in plans (costs often stay;
			// we still anonymize predicate numbers). Prefer values that look intentional.
			if ("0".equals(number) || "1".equals(number)) {
				return;
			}
			numberValues.add(number);
		}

		Map<String, String> buildForwardMap() {
			Map<String, String> forward = new LinkedHashMap<>();
			assignPartitionAware(forward, schemas, "schema");
			assignPartitionAware(forward, relations, "table");
			assignPartitionAware(forward, indexes, "index");
			assignPartitionAware(forward, ctes, "cte");
			assignSimple(forward, subplans, "subplan");
			if (anonymizeAliases) {
				assignSimple(forward, aliases, "alias");
			}
			assignSimple(forward, functions, "function");
			assignSimple(forward, columns, "column");
			assignValues(forward, stringValues, "value", true);
			assignValues(forward, numberValues, "value", false);
			return forward;
		}

		private void assignPartitionAware(Map<String, String> forward, Set<String> names, String prefix) {
			Map<String, List<String>> families = new TreeMap<>();
			List<String> singles = new ArrayList<>();
			for (String name : names) {
				Matcher m = PARTITION_SUFFIX.matcher(name);
				if (m.matches()) {
					families.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(name);
				} else {
					singles.add(name);
				}
			}
			int letter = 0;
			Collections.sort(singles);
			for (String name : singles) {
				putUnique(forward, name, prefix + "_" + toLetters(letter++));
			}
			List<String> stems = new ArrayList<>(families.keySet());
			Collections.sort(stems);
			for (String stem : stems) {
				List<String> members = families.get(stem);
				Collections.sort(members);
				String base = prefix + "_" + toLetters(letter++);
				for (String member : members) {
					Matcher m = PARTITION_SUFFIX.matcher(member);
					String suffix = m.matches() ? m.group(2) : "1";
					putUnique(forward, member, base + "_" + suffix);
				}
			}
		}

		private void assignSimple(Map<String, String> forward, Set<String> names, String prefix) {
			List<String> sorted = new ArrayList<>(names);
			Collections.sort(sorted);
			int letter = 0;
			for (String name : sorted) {
				putUnique(forward, name, prefix + "_" + toLetters(letter++));
			}
		}

		private void assignValues(Map<String, String> forward, Set<String> values, String prefix,
				boolean stringLiteral) {
			List<String> sorted = new ArrayList<>(values);
			Collections.sort(sorted);
			int letter = 0;
			for (String value : sorted) {
				String token = prefix + "_" + toLetters(letter++);
				if (stringLiteral) {
					boolean keepPctStart = value.length() >= 2 && value.charAt(1) == '%';
					boolean keepPctEnd = value.length() >= 3 && value.charAt(value.length() - 2) == '%';
					String inner = token;
					if (keepPctStart) {
						inner = "%" + inner;
					}
					if (keepPctEnd) {
						inner = inner + "%";
					}
					putUnique(forward, value, "'" + inner + "'");
				} else {
					// Preserve integer vs decimal shape loosely
					if (value.contains(".")) {
						putUnique(forward, value, String.valueOf(1000 + letter));
					} else {
						putUnique(forward, value, String.valueOf(1000 + letter));
					}
				}
			}
		}

		private static void putUnique(Map<String, String> forward, String original, String anon) {
			if (original == null || original.isEmpty() || forward.containsKey(original)) {
				return;
			}
			forward.put(original, anon);
		}

		private static String toLetters(int index) {
			// 0→a, 25→z, 26→aa …
			StringBuilder sb = new StringBuilder();
			int n = index;
			do {
				sb.insert(0, (char) ('a' + (n % 26)));
				n = n / 26 - 1;
			} while (n >= 0);
			return sb.toString();
		}

		private static boolean shouldKeepIdentifier(String name) {
			if (name == null || name.isEmpty()) {
				return true;
			}
			String lower = name.toLowerCase(Locale.ROOT);
			if (KEEP_IDENTIFIERS.contains(lower) || KEEP_SCHEMAS.contains(lower)) {
				return true;
			}
			if (lower.startsWith("pg_")) {
				return true;
			}
			if (lower.matches("\\$\\d+")) {
				return true;
			}
			return false;
		}

		private static Object tryParseJson(String plan) {
			try {
				String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(plan);
				if (resolved != null && !resolved.isBlank()) {
					return Jsoner.deserialize(resolved);
				}
			} catch (Exception ignored) {
				// fall through
			}
			String normalized = ExplainTextPlanConverter.normalizePsqlWrappers(plan).trim();
			try {
				if (normalized.startsWith("[") || normalized.startsWith("{")) {
					return Jsoner.deserialize(normalized);
				}
			} catch (Exception ignored) {
				// text plan — fall through to line scan
			}
			return null;
		}

		private static boolean isSqlKeyword(String name) {
			return shouldKeepIdentifier(name);
		}
	}
}
