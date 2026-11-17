package com.williammeitzen.peps.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.williammeitzen.peps.gui.DocumentsEmptyPanel;

/**
 * Locks Get Started landing copy so regressions like “large sample EXPLAIN” cannot
 * ship unnoticed when flipping the repo public.
 */
class PublicReadinessCopyTest {

	private DocumentsEmptyPanel panel;

	@BeforeEach
	void setUp() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "Swing copy checks need a display");
		SwingEdt.run(() -> panel = new DocumentsEmptyPanel(null));
	}

	@AfterEach
	void tearDown() throws Exception {
		if (panel != null) {
			SwingEdt.run(() -> {
				panel.removeAll();
				panel = null;
			});
		}
	}

	@Test
	void getStartedTitleAndSubtitleMatchExpectations() throws Exception {
		List<String> labels = SwingEdt.call(() -> collectLabelTexts(panel));
		assertTrue(labels.contains(UiCopyExpectations.GET_STARTED_TITLE),
				() -> "missing title; labels=" + labels);
		assertTrue(labels.contains(UiCopyExpectations.GET_STARTED_SUBTITLE),
				() -> "missing subtitle; labels=" + labels);
	}

	@Test
	void getStartedActionsUsePublicWording() throws Exception {
		List<String> headings = SwingEdt.call(() -> collectButtonHeadings(panel));
		assertTrue(headings.contains(UiCopyExpectations.NEW_BLANK_PLAN), () -> headings.toString());
		assertTrue(headings.contains(UiCopyExpectations.SAMPLE_JSON_PLAN), () -> headings.toString());
		assertTrue(headings.contains(UiCopyExpectations.SAMPLE_TEXT_PLAN), () -> headings.toString());
		assertEquals(3, headings.size(), () -> "expected three landing actions; got " + headings);
	}

	@Test
	void landingCopyHasNoForbiddenPhrases() throws Exception {
		String blob = SwingEdt.call(() -> {
			StringBuilder sb = new StringBuilder();
			for (String s : collectLabelTexts(panel)) {
				sb.append(s).append('\n');
			}
			for (String s : collectButtonHeadings(panel)) {
				sb.append(s).append('\n');
			}
			return sb.toString();
		});
		for (String forbidden : UiCopyExpectations.FORBIDDEN_UI_PHRASES) {
			assertFalse(blob.toLowerCase().contains(forbidden.toLowerCase()),
					"forbidden phrase still present: " + forbidden);
		}
	}

	private static List<String> collectLabelTexts(Container root) {
		List<String> out = new ArrayList<>();
		walk(root, c -> {
			if (c instanceof JLabel) {
				String t = ((JLabel) c).getText();
				if (t != null && !t.isBlank()) {
					out.add(t.replaceAll("<[^>]+>", "").trim());
				}
			}
		});
		return out;
	}

	private static List<String> collectButtonHeadings(Container root) {
		Set<String> out = new LinkedHashSet<>();
		walk(root, c -> {
			if (c instanceof JButton) {
				for (Component child : ((JButton) c).getComponents()) {
					if (child instanceof Container) {
						walk((Container) child, inner -> {
							if (inner instanceof JLabel) {
								JLabel lab = (JLabel) inner;
								if (lab.getIcon() == null) {
									String t = lab.getText();
									if (t != null && !t.startsWith("<html") && lab.getFont() != null
											&& lab.getFont().isBold()) {
										out.add(t.trim());
									}
								}
							}
						});
					}
				}
			}
		});
		return new ArrayList<>(out);
	}

	private interface Visitor {
		void visit(Component c);
	}

	private static void walk(Container root, Visitor v) {
		v.visit(root);
		for (Component child : root.getComponents()) {
			if (child instanceof Container) {
				walk((Container) child, v);
			} else {
				v.visit(child);
			}
		}
	}
}
