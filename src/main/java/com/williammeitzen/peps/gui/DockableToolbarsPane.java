package com.williammeitzen.peps.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/**
 * Stack of toolbar rows. Drag a toolbar by its grip/labels to:
 * <ul>
 *   <li>left or right of another toolbar on a row (vertical marker)</li>
 *   <li>above or below a row to place it on a different toolbar line (horizontal marker)</li>
 * </ul>
 */
public class DockableToolbarsPane extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int DRAG_THRESHOLD = 6;
	private static final int INDICATOR_THICKNESS = 3;
	private static final int ROW_EDGE_ZONE = 10;
	private static final Color INDICATOR_COLOR = new Color(0x1E, 0x88, 0xE5);

	private JToolBar draggedToolbar;
	private DropTarget dropTarget;
	private Rectangle indicatorBounds;

	private enum DropKind {
		BEFORE_IN_ROW,
		AFTER_IN_ROW,
		NEW_ROW_BEFORE,
		NEW_ROW_AFTER
	}

	private static final class DropTarget {
		final ToolbarRow row;
		final int indexInRow;
		final DropKind kind;

		DropTarget(ToolbarRow row, int indexInRow, DropKind kind) {
			this.row = row;
			this.indexInRow = indexInRow;
			this.kind = kind;
		}
	}

	private static final class ToolbarRow extends JPanel {
		private static final long serialVersionUID = 1L;

		ToolbarRow() {
			super(new FlowLayout(FlowLayout.LEFT, 4, 2));
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
		}

		List<JToolBar> toolbars() {
			List<JToolBar> list = new ArrayList<>();
			for (Component c : getComponents()) {
				if (c instanceof JToolBar) {
					list.add((JToolBar) c);
				}
			}
			return list;
		}

		List<JToolBar> toolbarsExcluding(JToolBar exclude) {
			List<JToolBar> list = new ArrayList<>();
			for (Component c : getComponents()) {
				if (c instanceof JToolBar && c != exclude) {
					list.add((JToolBar) c);
				}
			}
			return list;
		}
	}

	public DockableToolbarsPane(JToolBar... toolbars) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(true);
		// Start each toolbar on its own line (like the old stacked layout)
		for (JToolBar toolbar : toolbars) {
			prepareToolbar(toolbar);
			ToolbarRow row = new ToolbarRow();
			row.add(toolbar);
			add(row);
			installDragHandlers(toolbar);
		}
	}

	private void prepareToolbar(JToolBar toolbar) {
		toolbar.setFloatable(false);
		toolbar.setRollover(true);
		JLabel grip = new JLabel("\u2551"); // ║
		grip.setToolTipText("Drag to move toolbar between lines or beside another toolbar");
		grip.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		grip.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 6));
		toolbar.add(grip, 0);
	}

	private void installDragHandlers(JToolBar toolbar) {
		MouseAdapter dragHandler = new MouseAdapter() {
			private Point pressOnPane;
			private boolean dragging;

			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e) || e.getComponent() instanceof AbstractButton) {
					pressOnPane = null;
					dragging = false;
					return;
				}
				pressOnPane = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), DockableToolbarsPane.this);
				dragging = false;
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (pressOnPane == null) {
					return;
				}
				Point now = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), DockableToolbarsPane.this);
				if (!dragging && now.distance(pressOnPane) >= DRAG_THRESHOLD) {
					dragging = true;
					draggedToolbar = toolbar;
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
				if (dragging) {
					updateDropTarget(now);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (dragging && draggedToolbar != null && dropTarget != null) {
					commitDrop(draggedToolbar, dropTarget);
				}
				clearDragState();
				pressOnPane = null;
				dragging = false;
			}
		};

		toolbar.addMouseListener(dragHandler);
		toolbar.addMouseMotionListener(dragHandler);
		for (Component child : toolbar.getComponents()) {
			if (child instanceof AbstractButton) {
				continue;
			}
			child.addMouseListener(dragHandler);
			child.addMouseMotionListener(dragHandler);
			if (child instanceof JComponent) {
				((JComponent) child).setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
		}
	}

	private List<ToolbarRow> rows() {
		List<ToolbarRow> list = new ArrayList<>();
		for (Component c : getComponents()) {
			if (c instanceof ToolbarRow) {
				list.add((ToolbarRow) c);
			}
		}
		return list;
	}

	private void updateDropTarget(Point panePoint) {
		dropTarget = null;
		indicatorBounds = null;

		List<ToolbarRow> rowList = rows();
		if (rowList.isEmpty()) {
			repaint();
			return;
		}

		// Prefer left/right attach when over another toolbar's body
		for (ToolbarRow row : rowList) {
			for (JToolBar bar : row.toolbars()) {
				if (bar == draggedToolbar) {
					continue;
				}
				Rectangle barBounds = SwingUtilities.convertRectangle(row, bar.getBounds(), this);
				if (!barBounds.contains(panePoint)) {
					continue;
				}
				List<JToolBar> bars = row.toolbarsExcluding(draggedToolbar);
				int barIndex = bars.indexOf(bar);
				int midX = barBounds.x + barBounds.width / 2;
				if (panePoint.x < midX) {
					dropTarget = new DropTarget(row, barIndex, DropKind.BEFORE_IN_ROW);
					indicatorBounds = new Rectangle(
							barBounds.x - INDICATOR_THICKNESS / 2,
							barBounds.y,
							INDICATOR_THICKNESS,
							barBounds.height);
				} else {
					dropTarget = new DropTarget(row, barIndex + 1, DropKind.AFTER_IN_ROW);
					indicatorBounds = new Rectangle(
							barBounds.x + barBounds.width - INDICATOR_THICKNESS / 2,
							barBounds.y,
							INDICATOR_THICKNESS,
							barBounds.height);
				}
				repaint();
				return;
			}
		}

		// Row edge / between-row: new toolbar line (horizontal marker)
		for (int r = 0; r < rowList.size(); r++) {
			ToolbarRow row = rowList.get(r);
			Rectangle rowBounds = SwingUtilities.convertRectangle(row.getParent(), row.getBounds(), this);
			int topZone = rowBounds.y + ROW_EDGE_ZONE;
			int bottomZone = rowBounds.y + rowBounds.height - ROW_EDGE_ZONE;

			if (panePoint.y < topZone && panePoint.y >= rowBounds.y - ROW_EDGE_ZONE) {
				dropTarget = new DropTarget(row, 0, DropKind.NEW_ROW_BEFORE);
				indicatorBounds = new Rectangle(
						getInsets().left,
						rowBounds.y - INDICATOR_THICKNESS / 2,
						Math.max(getWidth() - getInsets().left - getInsets().right, 40),
						INDICATOR_THICKNESS);
				repaint();
				return;
			}
			if (panePoint.y > bottomZone && panePoint.y <= rowBounds.y + rowBounds.height + ROW_EDGE_ZONE) {
				dropTarget = new DropTarget(row, 0, DropKind.NEW_ROW_AFTER);
				indicatorBounds = new Rectangle(
						getInsets().left,
						rowBounds.y + rowBounds.height - INDICATOR_THICKNESS / 2,
						Math.max(getWidth() - getInsets().left - getInsets().right, 40),
						INDICATOR_THICKNESS);
				repaint();
				return;
			}

			// Empty space on this row (right of toolbars): append to row
			if (rowBounds.contains(panePoint)) {
				List<JToolBar> bars = row.toolbarsExcluding(draggedToolbar);
				dropTarget = new DropTarget(row, bars.size(), DropKind.AFTER_IN_ROW);
				if (!bars.isEmpty()) {
					JToolBar last = bars.get(bars.size() - 1);
					Rectangle lastBounds = SwingUtilities.convertRectangle(row, last.getBounds(), this);
					indicatorBounds = new Rectangle(
							lastBounds.x + lastBounds.width - INDICATOR_THICKNESS / 2,
							lastBounds.y,
							INDICATOR_THICKNESS,
							lastBounds.height);
				} else {
					indicatorBounds = new Rectangle(
							rowBounds.x + 4,
							rowBounds.y + 2,
							INDICATOR_THICKNESS,
							Math.max(rowBounds.height - 4, 20));
				}
				repaint();
				return;
			}
		}

		// Below all rows → new line after last
		ToolbarRow lastRow = rowList.get(rowList.size() - 1);
		Rectangle lastBounds = SwingUtilities.convertRectangle(lastRow.getParent(), lastRow.getBounds(), this);
		dropTarget = new DropTarget(lastRow, 0, DropKind.NEW_ROW_AFTER);
		indicatorBounds = new Rectangle(
				getInsets().left,
				lastBounds.y + lastBounds.height - INDICATOR_THICKNESS / 2,
				Math.max(getWidth() - getInsets().left - getInsets().right, 40),
				INDICATOR_THICKNESS);
		repaint();
	}

	private void commitDrop(JToolBar toolbar, DropTarget target) {
		ToolbarRow sourceRow = findRowContaining(toolbar);
		if (sourceRow == null) {
			return;
		}

		if (target.kind == DropKind.NEW_ROW_BEFORE || target.kind == DropKind.NEW_ROW_AFTER) {
			sourceRow.remove(toolbar);
			ToolbarRow newRow = new ToolbarRow();
			newRow.add(toolbar);
			int targetRowIndex = rows().indexOf(target.row);
			if (target.kind == DropKind.NEW_ROW_BEFORE) {
				add(newRow, targetRowIndex);
			} else {
				add(newRow, targetRowIndex + 1);
			}
		} else {
			sourceRow.remove(toolbar);
			List<JToolBar> remaining = target.row.toolbars();
			int index = Math.max(0, Math.min(target.indexInRow, remaining.size()));
			target.row.add(toolbar, index);
		}

		pruneEmptyRows();
		revalidate();
		repaint();
	}

	private ToolbarRow findRowContaining(JToolBar toolbar) {
		for (ToolbarRow row : rows()) {
			if (row.toolbars().contains(toolbar)) {
				return row;
			}
		}
		return null;
	}

	private void pruneEmptyRows() {
		for (ToolbarRow row : new ArrayList<>(rows())) {
			if (row.toolbars().isEmpty()) {
				remove(row);
			}
		}
		if (rows().isEmpty()) {
			add(new ToolbarRow());
		}
	}

	private void clearDragState() {
		draggedToolbar = null;
		dropTarget = null;
		indicatorBounds = null;
		setCursor(Cursor.getDefaultCursor());
		repaint();
	}

	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
		if (indicatorBounds != null && draggedToolbar != null) {
			Graphics g2 = g.create();
			try {
				g2.setColor(INDICATOR_COLOR);
				g2.fillRect(indicatorBounds.x, indicatorBounds.y, indicatorBounds.width, indicatorBounds.height);
			} finally {
				g2.dispose();
			}
		}
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension pref = getPreferredSize();
		return new Dimension(100, pref.height);
	}
}
