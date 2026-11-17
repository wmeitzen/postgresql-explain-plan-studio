package com.williammeitzen.peps.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Guards the anonymize silhouette overlay: invisible badges must still reserve
 * a real preferred size so the first Anonymize-on layout is not 0×0.
 */
class AnonymizeBadgeLayoutTest {

	@Test
	void badgeCornerKeepsSizeWhileInvisible() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless());
		SwingUtilities.invokeAndWait(() -> {
			JLabel badge = PlanSourcePanel.createAnonymizeBadge();
			assertTrue(!badge.isVisible(), "badge starts hidden");
			JTextArea area = new JTextArea("select 1");
			JScrollPane scroll = new JScrollPane(area);
			scroll.setPreferredSize(new Dimension(400, 120));
			JComponent host = PlanSourcePanel.wrapWithAnonymizeBadge(scroll, badge);
			host.setSize(400, 120);
			PlanSourcePanel.relayoutAnonymizeBadgeHost(host);

			Object cornerObj = ((javax.swing.JLayeredPane) host).getClientProperty("anonymizeBadgeCorner");
			assertTrue(cornerObj instanceof JComponent);
			Dimension pref = ((JComponent) cornerObj).getPreferredSize();
			assertTrue(pref.width >= 20, "corner width too small: " + pref);
			assertTrue(pref.height >= 20, "corner height too small: " + pref);

			badge.setVisible(true);
			PlanSourcePanel.relayoutAnonymizeBadgeHost(host);
			Dimension after = ((JComponent) cornerObj).getPreferredSize();
			assertTrue(after.width >= 20);
			assertTrue(after.height >= 20);
			assertTrue(((JComponent) cornerObj).getWidth() >= 20
					|| host.getWidth() == 0 /* not realized */,
					"visible badge corner should layout with width when host has size");
		});
	}

	@Test
	void silhouetteIconLoadsAndScales() {
		assumeFalse(GraphicsEnvironment.isHeadless());
		javax.swing.ImageIcon icon = PlanSourcePanel.loadAnonymizeSilhouetteIcon();
		assertTrue(icon != null, "silhouette resource missing");
		assertTrue(icon.getIconWidth() > 0);
		assertTrue(icon.getIconHeight() > 0);
	}
}
