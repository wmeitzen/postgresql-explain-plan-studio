package com.williammeitzen.peps.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OperatorDisplayTitleTest {

	@Test
	void formatOperatorDisplayTitle_isNodeTypeOnly() {
		assertEquals("Materialize", PlanGraphPanel.formatOperatorDisplayTitle("Materialize"));
		assertEquals("Nested Loop", PlanGraphPanel.formatOperatorDisplayTitle("Nested Loop"));
		assertEquals("Nested Loop", PlanGraphPanel.formatOperatorDisplayTitle("  Nested Loop  "));
		assertEquals("", PlanGraphPanel.formatOperatorDisplayTitle(null));
		assertEquals("", PlanGraphPanel.formatOperatorDisplayTitle(""));
	}

	@Test
	void propertyColumnHeader_prefixesNodeTypeOnly() {
		assertEquals("Property", PlanGraphPanel.propertyColumnHeaderForOperator(""));
		assertEquals("Property", PlanGraphPanel.propertyColumnHeaderForOperator(null));
		assertEquals("Materialize Property",
				PlanGraphPanel.propertyColumnHeaderForOperator("Materialize"));
		assertEquals("Nested Loop Property",
				PlanGraphPanel.propertyColumnHeaderForOperator("Nested Loop"));
	}
}
