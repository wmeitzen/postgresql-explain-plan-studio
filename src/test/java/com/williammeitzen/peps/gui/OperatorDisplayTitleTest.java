package com.williammeitzen.peps.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OperatorDisplayTitleTest {

	@Test
	void formatOperatorDisplayTitle_includesJoinTypeWhenPresent() {
		assertEquals("Materialize", PlanGraphPanel.formatOperatorDisplayTitle("Materialize", null));
		assertEquals("Materialize", PlanGraphPanel.formatOperatorDisplayTitle("Materialize", ""));
		assertEquals("Nested Loop (Inner Join)",
				PlanGraphPanel.formatOperatorDisplayTitle("Nested Loop", "Inner Join"));
		assertEquals("", PlanGraphPanel.formatOperatorDisplayTitle(null, "Inner Join"));
	}

	@Test
	void propertyColumnHeader_prefixesNodeType() {
		assertEquals("Property", PlanGraphPanel.propertyColumnHeaderForOperator(""));
		assertEquals("Property", PlanGraphPanel.propertyColumnHeaderForOperator(null));
		assertEquals("Materialize Property",
				PlanGraphPanel.propertyColumnHeaderForOperator("Materialize"));
		assertEquals("Nested Loop (Inner Join) Property",
				PlanGraphPanel.propertyColumnHeaderForOperator("Nested Loop (Inner Join)"));
	}
}
