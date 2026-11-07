package com.williammeitzen.peps.model;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

/**
 * Compact fingerprint of the operator tree fields that drive PEPS diagram icons/labels.
 * Extra JSON-only properties are ignored so text and json EXPLAIN can be compared for shape.
 */
public final class PlanShapeFingerprint {

	private PlanShapeFingerprint() {
	}

	public static String fromPlanField(String planFieldText) throws Exception {
		String resolved = ExplainTextPlanConverter.resolveToJsonArrayString(planFieldText);
		if (resolved == null || resolved.isBlank()) {
			throw new IllegalArgumentException("Plan field is empty or unrecognized.");
		}
		return fromResolvedJsonArrayString(resolved);
	}

	public static String fromResolvedJsonArrayString(String jsonArrayString) throws Exception {
		Object parsed = Jsoner.deserialize(jsonArrayString);
		JsonObject wrapper;
		if (parsed instanceof JsonArray) {
			JsonArray array = (JsonArray) parsed;
			if (array.isEmpty() || !(array.get(0) instanceof JsonObject)) {
				throw new IllegalArgumentException("JSON plan array is empty.");
			}
			wrapper = (JsonObject) array.get(0);
		} else if (parsed instanceof JsonObject) {
			wrapper = (JsonObject) parsed;
		} else {
			throw new IllegalArgumentException("Unexpected JSON plan root type.");
		}
		Object plan = wrapper.get("Plan");
		if (!(plan instanceof JsonObject)) {
			throw new IllegalArgumentException("JSON plan missing Plan object.");
		}
		StringBuilder sb = new StringBuilder();
		appendNode(sb, (JsonObject) plan, 0);
		return sb.toString();
	}

	private static void appendNode(StringBuilder sb, JsonObject node, int depth) {
		for (int i = 0; i < depth; i++) {
			sb.append('.');
		}
		// Fingerprint icon/diagram shape (JSON base names), not text-native display labels.
		String nodeType = str(node.get("Node Type"));
		String joinType = str(node.get("Join Type"));
		String operation = str(node.get("Operation"));
		String strategy = str(node.get("Strategy"));
		sb.append(ExplainTextPlanConverter.resolveIconNodeType(nodeType, joinType, operation, strategy));
		appendField(sb, "JT", joinType.isEmpty() ? null : joinType);
		// JSON always emits Parallel Aware false; text omits it — only fingerprint when true
		// or when the text Node Type already starts with Parallel.
		boolean parallel = "true".equals(bool(node.get("Parallel Aware")))
				|| nodeType.toLowerCase(java.util.Locale.ROOT).startsWith("parallel ");
		if (parallel) {
			appendField(sb, "PA", "true");
		}
		appendField(sb, "ST", strategy.isEmpty() ? null : strategy);
		appendField(sb, "PM", node.get("Partial Mode"));
		appendField(sb, "OP", operation.isEmpty() ? null : operation);
		String rel = str(node.get("Relation Name"));
		// Synthetic VALUES alias appears in text but not typically in JSON.
		if (!rel.isEmpty() && !rel.equals("*VALUES*") && !(rel.startsWith("*") && rel.endsWith("*"))) {
			appendField(sb, "REL", rel);
		}
		appendField(sb, "IDX", node.get("Index Name"));
		sb.append('\n');
		Object plans = node.get("Plans");
		if (plans instanceof JsonArray) {
			JsonArray children = (JsonArray) plans;
			for (Object child : children) {
				if (child instanceof JsonObject) {
					appendNode(sb, (JsonObject) child, depth + 1);
				}
			}
		}
	}

	private static void appendField(StringBuilder sb, String key, Object value) {
		if (value == null) {
			return;
		}
		String s = str(value);
		if (s.isEmpty()) {
			return;
		}
		sb.append('|').append(key).append('=').append(s);
	}

	private static String bool(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Boolean) {
			return ((Boolean) value) ? "true" : "false";
		}
		String s = String.valueOf(value).trim();
		if (s.equalsIgnoreCase("true") || s.equals("1")) {
			return "true";
		}
		if (s.equalsIgnoreCase("false") || s.equals("0")) {
			return "false";
		}
		return s;
	}

	private static String str(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
