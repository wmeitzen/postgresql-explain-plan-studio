package com.williammeitzen.peps.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Hashtable;
import java.util.function.IntUnaryOperator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.json.simple.JsonObject;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatSliderUI;
import com.williammeitzen.peps.model.Database;

public class SliderPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	private static final int SLIDER_PREFERRED_WIDTH = 600;
	private static final int SLIDER_PREFERRED_HEIGHT = 72;
	/** Room for tick labels + track/thumb + current-value column in portrait. */
	private static final int SLIDER_VERTICAL_PREFERRED_WIDTH = 168;
	/** Do not let the side dock shrink past the vertical thumb/labels. */
	public static final int PANEL_MINIMUM_WIDTH = 168;
	private static final int PANEL_COMPACT_HEIGHT = 140;
	private static final int THUMB_VALUE_TRACK_HEIGHT = 18;
	private static final int THUMB_VALUE_TRACK_WIDTH = 52;
	private static final String TIME_MODE_LABEL_FULL = "Time Difference Slider";
	private static final String COST_MODE_LABEL_FULL = "Cost Slider";
	/** Longest → shortest radio labels; pick the longest pair that fits the row. */
	private static final String[][] MODE_LABEL_TIERS = {
			{ "Time Difference Slider", "Cost Slider" },
			{ "Time Difference", "Cost Slider" },
			{ "Time Diff", "Cost" },
			{ "Time", "Cost" },
	};
	
	private JLabel timeSliderLabel;
	private JLabel costSliderLabel;
	private JLabel formattedCurrentTime;
	private JLabel formattedCurrentCost;
	/** Value shown directly above the slider thumb (3-digit style). */
	private JLabel thumbValueLabel;
	private JPanel thumbValueTrack;
	private JPanel sliderHost;
	private JPanel radioRow;
	private JLabel propertiesDetails;
	private JRadioButton timeSliderRB;
	private JRadioButton costSliderRB;
	/** Selected operator caption beside the radio buttons, e.g. "(35) Hash Join w/Filter". */
	private JLabel selectedOperatorLabel;
	private boolean modeLabelListenerInstalled;
	private boolean updatingSliderChrome;
	private int lastSliderOrientation = -1;
	private JViewport watchedViewport;
	private final ComponentAdapter viewportResizeListener = new ComponentAdapter() {
		@Override
		public void componentResized(ComponentEvent e) {
			updateSliderChrome();
		}
	};
	private Integer selectedOperatorId;
	/** Absolute start/end on the active slider scale (ms or cost units). */
	private Double selectedSpanStart;
	private Double selectedSpanEnd;
	/** Active slider that shows the selected-operator span bar. */
	private JSlider spanSlider;

//	private Double fltPlanTotalTimeMs;
	private JSlider timeSlider;
	private JSlider costSlider;
	private PlanGraphPanel planGraphPanel = null;
	private Hashtable<Integer, JLabel> position;
	private int sliderType; // 1 = time slider, 2 = cost slider
	public static int SLIDER_TYPE_TIME = 1;
	public static int SLIDER_TYPE_COST = 2;
	private static final Color SPAN_BAR_TIME = new Color(0, 190, 0, 150);
	private static final Color SPAN_BAR_COST = new Color(100, 116, 139, 170);
	private static final Color SPAN_BAR_TIME_DARK = new Color(74, 222, 128, 180);
	private static final Color SPAN_BAR_COST_DARK = new Color(148, 163, 184, 180);
	
	//private GridBagConstraints gc;
	
	public SliderPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH, PANEL_COMPACT_HEIGHT));
		setPreferredSize(new Dimension(SLIDER_PREFERRED_WIDTH, PANEL_COMPACT_HEIGHT));

		//createComponents_OLD();
		createComponents();
		
		//layoutComponents_OLD();
		layoutComponents();
		installModeLabelResizeListener();
		SwingUtilities.invokeLater(this::updateSliderChrome);
	}

	public void setPlanGraphPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		selectedOperatorId = null;
		selectedSpanStart = null;
		selectedSpanEnd = null;
		spanSlider = null;
		if (selectedOperatorLabel != null) {
			selectedOperatorLabel.setText("");
		}
	}
	
	private static int dblRoundedToInt(Double dblValue) {
		if (dblValue == null)
			return 0;
		return (int) Math.round(dblValue);
	}

	public void paint() {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("SliderPanel().paint()"); }
		//createComponents();
		//layoutComponents();
	}
	
/*
	private String dblRoundedToString(Double dblValue) {
		return Integer.toString(dblRoundedToInt(dblValue));
	}
*/
	
/*
	public void paintComponent(Graphics g) {
		layoutComponents();
	}
*/
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("SliderPanel().paintComponent()"); }
		//this.removeAll();
		//createComponents(); // causes recursion if paint() also has createComponents()
		//layoutComponents();
		//repaint(); // causes recursion by calling paint() which I think calls paintComponent()
	}
	
	public void refreshComponents() {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("SliderPanel().refreshComponents()"); }
		this.removeAll();
		createComponents();
		layoutComponents();
		lastSliderOrientation = -1;
		updateSliderChrome();
		repaint();
	}


	
	public void createComponents() {
		
		Double planTotalTimeMs;
		Double planTotalCost;
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		String strSQL;
		JsonObject jsnRow;

		int idExplainPlan = (planGraphPanel != null && planGraphPanel.getIdExplainPlan() != null)
				? planGraphPanel.getIdExplainPlan().intValue() : -1;

		ButtonGroup sliderLabelButtonGroup = new ButtonGroup();
		timeSliderRB = new JRadioButton(TIME_MODE_LABEL_FULL);
		costSliderRB = new JRadioButton(COST_MODE_LABEL_FULL);
		timeSliderRB.setToolTipText(TIME_MODE_LABEL_FULL);
		costSliderRB.setToolTipText(COST_MODE_LABEL_FULL);

		boolean bolEnableCost = false;
		boolean bolEnableExclusiveTime = false;
		boolean bolEnableTimeDifference = false;
		if (idExplainPlan >= 0) {
			strSQL = "select cost_enabled, exclusive_time_enabled, time_difference_enabled\n"
					+ "from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
			if (bolShowSQL == true) { System.out.println(strSQL); }
			jsnRow = Database.row(strSQL);
			if (jsnRow != null) {
				bolEnableCost = Utils.objectToBoolean(jsnRow.get("cost_enabled"));
				bolEnableExclusiveTime = Utils.objectToBoolean(jsnRow.get("exclusive_time_enabled"));
				bolEnableTimeDifference = Utils.objectToBoolean(jsnRow.get("time_difference_enabled"));
			}
		}
/*
		if (bolEnableCost == false)
			costSliderRB.setEnabled(false);
		if (bolEnableExclusiveTime == false && bolEnableTimeDifference == false)
			timeSliderRB.setEnabled(false);
*/
		if (bolShowProperties == true) { System.out.println("bolEnableCost: " + bolEnableCost); }
		costSliderRB.setEnabled(bolEnableCost);
		if (bolShowProperties == true) { System.out.println("bolEnableExclusiveTime: " + bolEnableExclusiveTime); }
		if (bolShowProperties == true) { System.out.println("bolEnableTimeDifference: " + bolEnableTimeDifference); }
		timeSliderRB.setEnabled(bolEnableExclusiveTime || bolEnableTimeDifference);

		int sliderType = planGraphPanel != null ? planGraphPanel.getSliderType() : SliderPanel.SLIDER_TYPE_COST;
		if (sliderType == SliderPanel.SLIDER_TYPE_TIME && timeSliderRB.isEnabled() == true) {
			timeSliderRB.setSelected(true);
		} else if (sliderType == SliderPanel.SLIDER_TYPE_COST && costSliderRB.isEnabled() == true) {
			costSliderRB.setSelected(true);
		} else if (sliderType != SliderPanel.SLIDER_TYPE_TIME && sliderType != SliderPanel.SLIDER_TYPE_COST) {
			System.out.println("Error in SliderPanel.createComponents(): Unknown slider type");
		}
		sliderLabelButtonGroup.add(timeSliderRB);
		sliderLabelButtonGroup.add(costSliderRB);
		
		timeSliderLabel = new JLabel(TIME_MODE_LABEL_FULL);
		costSliderLabel = new JLabel(COST_MODE_LABEL_FULL);

		if (costSliderRB.isEnabled() == false) {
			costSliderLabel.setEnabled(false);
		}
		
		if (timeSliderRB.isEnabled() == false) {
			timeSliderLabel.setEnabled(false);
		}
		
		// make sure we have valid current times and costs
		if (dblRoundedToInt(getCurrentPlanCost()) >= dblRoundedToInt(getPlanTotalCost()))
			setCurrentPlanCost(0.0);

		if (dblRoundedToInt(getCurrentPlanTimeMs()) >= dblRoundedToInt(getPlanTotalTimeMs()))
			setCurrentPlanTimeMs(0.0);

		costSlider = new JSlider(JSlider.HORIZONTAL, 0, dblRoundedToInt(getPlanTotalCost()), dblRoundedToInt(getCurrentPlanCost()));
		timeSlider = new JSlider(JSlider.HORIZONTAL, 0, dblRoundedToInt(getPlanTotalTimeMs()), dblRoundedToInt(getCurrentPlanTimeMs()));

		ChangeListener timeSliderChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				//System.out.println("slider value: " + ((JSlider) e.getSource()).getValue());
				int value = ((JSlider) e.getSource()).getValue();
				Double getPlanTotalTimeMs = getPlanTotalTimeMs();
				if (bolShowProperties == true) { System.out.println("value: " + value); }
				if (getPlanTotalTimeMs != null) {
					double msValue = getPlanTotalTimeMs * value / timeSlider.getMaximum();
					if (bolShowProperties == true) { System.out.println("msValue: " + msValue); }
					//setCurrentPlanTimeMs(value);
					//setCurrentPlanTimeLabel(value);
					setCurrentPlanTimeMs(msValue);
					setCurrentPlanTimeLabel(msValue);
					if (planGraphPanel != null) {
						planGraphPanel.repaint();
					}
				}
			}
		};

		ChangeListener costSliderChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int value = ((JSlider) e.getSource()).getValue();
				Double getPlanTotalCost = getPlanTotalCost(); // runs a sql command
				if (bolShowProperties == true) { System.out.println("value: " + value); }
				if (getPlanTotalCost != null) {
					double msValue = getPlanTotalCost * value / costSlider.getMaximum();
					if (bolShowProperties == true) { System.out.println("msValue: " + msValue); }
					//setCurrentPlanTimeMs(value);
					//setCurrentPlanTimeLabel(value);
					setCurrentPlanCost(msValue);
					setCurrentPlanCostLabel(msValue);
					//setCurrentPlanCostLabel(Utils.formattedCost(msValue));
					if (planGraphPanel != null) {
						planGraphPanel.repaint();
					}
				}
			}
		};

		timeSliderRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//repaintOnPercentageByChange(PercentageBy.COST);
				//planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME
				if (planGraphPanel != null) {
					planGraphPanel.setSliderType(SliderPanel.SLIDER_TYPE_TIME);
				}
				//repaint();
				refreshComponents();
			}
		});
		
		costSliderRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//repaintOnPercentageByChange(PercentageBy.COST);
				if (planGraphPanel != null) {
					planGraphPanel.setSliderType(SliderPanel.SLIDER_TYPE_COST);
				}
				//repaint();
				refreshComponents();
			}
		});
		
		timeSlider.addChangeListener(timeSliderChangeListener);
		costSlider.addChangeListener(costSliderChangeListener);
		
		position = new Hashtable();
		planTotalTimeMs = getPlanTotalTimeMs(); 
		planTotalCost = getPlanTotalCost(); 
		if (planTotalTimeMs == null) planTotalTimeMs = 0d;
		if (planTotalCost == null) planTotalCost = 0d;

		if (sliderType == SliderPanel.SLIDER_TYPE_TIME) {
			position.put(0, new JLabel("0 sec"));
		} else if (sliderType == SliderPanel.SLIDER_TYPE_COST) {
			position.put(0, new JLabel("0"));
		} else {
			System.out.println("Error in SliderPanel.createComponents(): Unknown slider type");
		}

		thumbValueLabel = new JLabel("0.00", SwingConstants.CENTER);
		thumbValueLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		thumbValueTrack = new JPanel(null);
		thumbValueTrack.setOpaque(false);
		thumbValueTrack.setPreferredSize(new Dimension(SLIDER_PREFERRED_WIDTH, THUMB_VALUE_TRACK_HEIGHT));
		thumbValueTrack.add(thumbValueLabel);

		selectedOperatorLabel = new JLabel(" ");
		Font labelFont = javax.swing.UIManager.getFont("Label.font");
		if (labelFont != null) {
			selectedOperatorLabel.setFont(labelFont);
		}
		selectedOperatorLabel.setToolTipText("Selected operator span on this slider");

		if (sliderType == SliderPanel.SLIDER_TYPE_TIME) {
			// Set the label to be drawn
			timeSlider.setLabelTable(position);
			formattedCurrentTime = new JLabel("0.00 sec");
			thumbValueLabel.setText(formatThreeDigitTimeMs(0));
		} else if (sliderType == SliderPanel.SLIDER_TYPE_COST) {
			// Set the label to be drawn
			costSlider.setLabelTable(position);
			formattedCurrentCost = new JLabel("0.00");
			thumbValueLabel.setText(formatThreeDigitCost(0));
		} else {
			System.out.println("Error in SliderPanel.createComponents(): Unknown slider type");
		}
		
		//GridBagLayout g = new GridBagLayout();
		//GridBagConstraints gc = new GridBagConstraints();
		

	}

/*
	private void costSliderChangeListenerStateChanged(double msValue) {
		
	}
*/
	
	public void layoutComponents() {

		setLayout(new GridBagLayout());
		
		GridBagConstraints gc = new GridBagConstraints();
		
		gc.gridy = 0;
		gc.gridx = 0;
		gc.weightx = 1;
		gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(4, 8, 0, 8);
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

		radioRow = new JPanel();
		radioRow.setOpaque(false);
		rebuildRadioRowLayout(isPortraitLayout());
		add(radioRow, gc);
		
		clearSliderComponent(costSlider);
		clearSliderComponent(costSliderLabel);
		clearSliderComponent(timeSlider);
		clearSliderComponent(timeSliderLabel);
		clearSliderComponent(formattedCurrentCost);
		clearSliderComponent(formattedCurrentTime);
		clearSliderComponent(sliderHost);

		int sliderType = planGraphPanel != null ? planGraphPanel.getSliderType() : SliderPanel.SLIDER_TYPE_COST;
		JSlider activeSlider = null;
		if (sliderType == SliderPanel.SLIDER_TYPE_TIME) {
			activeSlider = timeSlider;
		} else if (sliderType == SliderPanel.SLIDER_TYPE_COST) {
			activeSlider = costSlider;
		} else {
			System.out.println("Error in SliderPanel.layoutComponents(): Unknown slider type");
		}

		if (activeSlider != null) {
			spanSlider = activeSlider;
			installSpanAwareSliderUI(spanSlider);
			rebuildSliderHost(isPortraitLayout());

			gc.gridy++;
			gc.insets = new Insets(2, 8, 6, 8);
			if (isPortraitLayout()) {
				gc.fill = GridBagConstraints.BOTH;
				gc.weightx = 1;
				gc.weighty = 1;
			} else {
				gc.fill = GridBagConstraints.HORIZONTAL;
				gc.weightx = 1;
				gc.weighty = 0; // keep content packed; dock height stays short
			}
			add(sliderHost, gc);

			spanSlider.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					repositionThumbValueLabel(spanSlider);
					spanSlider.repaint();
				}
			});
		} else {
			spanSlider = null;
		}
		
		if (planGraphPanel != null && planGraphPanel.getIdExplainPlan() != null) {
			if (sliderType == SliderPanel.SLIDER_TYPE_TIME) {
				setPlanTotalTimeMs(planGraphPanel.getIdExplainPlan());
				setCurrentPlanTimeLabel(getCurrentPlanTimeMs() != null ? getCurrentPlanTimeMs() : 0);
			} else if (sliderType == SliderPanel.SLIDER_TYPE_COST) {
				setPlanTotalCost(planGraphPanel.getIdExplainPlan());
				setCurrentPlanCostLabel(getCurrentPlanCost() != null ? getCurrentPlanCost() : 0);
			} else {
				System.out.println("Error in SliderPanel.layoutComponents(): Unknown slider type");
			}
		}

		Integer operatorToShow = selectedOperatorId;
		if (operatorToShow == null && planGraphPanel != null) {
			operatorToShow = planGraphPanel.getSelectedOperatorId();
		}
		showSelectedOperator(operatorToShow);
		updateSliderModeLabels();
		if (spanSlider != null) {
			repositionThumbValueLabel(spanSlider);
		}
	}

	private void installModeLabelResizeListener() {
		if (modeLabelListenerInstalled) {
			return;
		}
		modeLabelListenerInstalled = true;
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateSliderChrome();
			}

			@Override
			public void componentShown(ComponentEvent e) {
				attachViewportListener();
				updateSliderChrome();
			}
		});
		addHierarchyListener(new HierarchyListener() {
			@Override
			public void hierarchyChanged(HierarchyEvent e) {
				if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
					attachViewportListener();
					updateSliderChrome();
				}
			}
		});
		attachViewportListener();
	}

	private void attachViewportListener() {
		Container parent = getParent();
		JViewport viewport = parent instanceof JViewport ? (JViewport) parent : null;
		if (viewport == watchedViewport) {
			return;
		}
		if (watchedViewport != null) {
			watchedViewport.removeComponentListener(viewportResizeListener);
		}
		watchedViewport = viewport;
		if (watchedViewport != null) {
			watchedViewport.addComponentListener(viewportResizeListener);
		}
	}

	/**
	 * Use the enclosing viewport/dock size — not this panel's preferred height.
	 * Otherwise a short landscape preferred size never flips to portrait when
	 * side-docked (panel stays ~140px tall inside a tall viewport).
	 */
	private Dimension layoutHostSize() {
		attachViewportListener();
		if (watchedViewport != null) {
			Dimension d = watchedViewport.getExtentSize();
			if (d.width > 0 && d.height > 0) {
				return d;
			}
			d = watchedViewport.getSize();
			if (d.width > 0 && d.height > 0) {
				return d;
			}
		}
		Container parent = getParent();
		if (parent != null) {
			Dimension d = parent.getSize();
			if (d.width > 0 && d.height > 0) {
				return d;
			}
		}
		int width = getWidth();
		int height = getHeight();
		if (width > 0 && height > 0) {
			return new Dimension(width, height);
		}
		return getPreferredSize();
	}

	/** Portrait (taller than wide) → vertical slider like Zoom; landscape → horizontal. */
	private boolean isPortraitLayout() {
		Dimension host = layoutHostSize();
		return host.height > host.width;
	}

	private void updateSliderChrome() {
		if (updatingSliderChrome || timeSliderRB == null || costSliderRB == null) {
			return;
		}
		boolean portrait = isPortraitLayout();
		int orientation = portrait ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL;
		if (orientation != lastSliderOrientation) {
			updatingSliderChrome = true;
			try {
				lastSliderOrientation = orientation;
				applySliderOrientation(timeSlider, orientation);
				applySliderOrientation(costSlider, orientation);
				removeAll();
				layoutComponents();
			} finally {
				updatingSliderChrome = false;
			}
			revalidate();
			repaint();
			return;
		}
		updateSliderModeLabels();
		if (spanSlider != null) {
			repositionThumbValueLabel(spanSlider);
		}
	}

	private void applySliderOrientation(JSlider slider, int orientation) {
		if (slider == null) {
			return;
		}
		if (slider.getOrientation() != orientation) {
			int value = slider.getValue();
			slider.setOrientation(orientation);
			slider.setValue(value);
		}
		if (slider == spanSlider) {
			installSpanAwareSliderUI(slider);
		}
	}

	private void rebuildRadioRowLayout(boolean portrait) {
		if (radioRow == null) {
			return;
		}
		radioRow.removeAll();
		if (portrait) {
			radioRow.setLayout(new BoxLayout(radioRow, BoxLayout.Y_AXIS));
			radioRow.add(timeSliderRB);
			radioRow.add(Box.createVerticalStrut(2));
			radioRow.add(costSliderRB);
			if (selectedOperatorLabel != null) {
				radioRow.add(Box.createVerticalStrut(4));
				selectedOperatorLabel.setAlignmentX(LEFT_ALIGNMENT);
				radioRow.add(selectedOperatorLabel);
			}
			timeSliderRB.setAlignmentX(LEFT_ALIGNMENT);
			costSliderRB.setAlignmentX(LEFT_ALIGNMENT);
		} else {
			radioRow.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 0));
			radioRow.add(timeSliderRB);
			radioRow.add(costSliderRB);
			if (selectedOperatorLabel != null) {
				radioRow.add(selectedOperatorLabel);
			}
		}
	}

	private void rebuildSliderHost(boolean portrait) {
		if (spanSlider == null || thumbValueTrack == null) {
			return;
		}
		if (sliderHost == null) {
			sliderHost = new JPanel(new BorderLayout());
			sliderHost.setOpaque(false);
		}
		sliderHost.removeAll();
		if (portrait) {
			spanSlider.setPreferredSize(new Dimension(SLIDER_VERTICAL_PREFERRED_WIDTH, SLIDER_PREFERRED_WIDTH));
			spanSlider.setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH - THUMB_VALUE_TRACK_WIDTH, 120));
			thumbValueTrack.setPreferredSize(new Dimension(THUMB_VALUE_TRACK_WIDTH, 10));
			sliderHost.add(thumbValueTrack, BorderLayout.EAST);
			sliderHost.add(spanSlider, BorderLayout.CENTER);
			setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH, PANEL_COMPACT_HEIGHT));
			setPreferredSize(new Dimension(SLIDER_VERTICAL_PREFERRED_WIDTH, SLIDER_PREFERRED_WIDTH));
		} else {
			spanSlider.setPreferredSize(new Dimension(SLIDER_PREFERRED_WIDTH, SLIDER_PREFERRED_HEIGHT));
			spanSlider.setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH, SLIDER_PREFERRED_HEIGHT));
			thumbValueTrack.setPreferredSize(new Dimension(SLIDER_PREFERRED_WIDTH, THUMB_VALUE_TRACK_HEIGHT));
			sliderHost.add(thumbValueTrack, BorderLayout.NORTH);
			sliderHost.add(spanSlider, BorderLayout.CENTER);
			setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH, PANEL_COMPACT_HEIGHT));
			setPreferredSize(new Dimension(SLIDER_PREFERRED_WIDTH, PANEL_COMPACT_HEIGHT));
		}
		sliderHost.revalidate();
	}

	private void updateSliderModeLabels() {
		if (timeSliderRB == null || costSliderRB == null) {
			return;
		}
		timeSliderRB.setToolTipText(TIME_MODE_LABEL_FULL);
		costSliderRB.setToolTipText(COST_MODE_LABEL_FULL);

		int available = getWidth();
		if (available <= 0) {
			available = getPreferredSize().width;
		}
		Insets insets = getInsets();
		int inner = available - (insets != null ? insets.left + insets.right : 0) - 16;
		if (selectedOperatorLabel != null) {
			String op = selectedOperatorLabel.getText();
			if (op != null && !op.isBlank() && !" ".equals(op) && !isPortraitLayout()) {
				inner -= selectedOperatorLabel.getPreferredSize().width + 16;
			}
		}

		String timeText = MODE_LABEL_TIERS[MODE_LABEL_TIERS.length - 1][0];
		String costText = MODE_LABEL_TIERS[MODE_LABEL_TIERS.length - 1][1];
		Font font = timeSliderRB.getFont();
		for (String[] tier : MODE_LABEL_TIERS) {
			int need = measureRadioWidth(tier[0], font) + measureRadioWidth(tier[1], font) + 16;
			if (isPortraitLayout()) {
				// Stacked radios: each label only needs to fit the strip width.
				need = Math.max(measureRadioWidth(tier[0], font), measureRadioWidth(tier[1], font));
			}
			if (inner <= 0 || need <= inner) {
				timeText = tier[0];
				costText = tier[1];
				break;
			}
		}
		if (!timeText.equals(timeSliderRB.getText())) {
			timeSliderRB.setText(timeText);
		}
		if (!costText.equals(costSliderRB.getText())) {
			costSliderRB.setText(costText);
		}
		if (timeSliderLabel != null) {
			timeSliderLabel.setText(timeText);
		}
		if (costSliderLabel != null) {
			costSliderLabel.setText(costText);
		}
	}

	private static int measureRadioWidth(String text, Font font) {
		JRadioButton probe = new JRadioButton(text);
		if (font != null) {
			probe.setFont(font);
		}
		return probe.getPreferredSize().width;
	}

	private void repositionThumbValueLabel(JSlider slider) {
		if (thumbValueLabel == null || thumbValueTrack == null || slider == null) {
			return;
		}
		thumbValueLabel.setSize(thumbValueLabel.getPreferredSize());
		boolean vertical = slider.getOrientation() == SwingConstants.VERTICAL;
		if (vertical) {
			int thumbCenterY = yPositionForSliderValue(slider, slider.getValue());
			Point trackPoint = SwingUtilities.convertPoint(slider, new Point(0, thumbCenterY), thumbValueTrack);
			int y = trackPoint.y - thumbValueLabel.getHeight() / 2;
			int maxY = Math.max(0, thumbValueTrack.getHeight() - thumbValueLabel.getHeight());
			y = Math.max(0, Math.min(y, maxY));
			int x = Math.max(0, (thumbValueTrack.getWidth() - thumbValueLabel.getWidth()) / 2);
			thumbValueLabel.setLocation(x, y);
		} else {
			int thumbCenterX = xPositionForSliderValue(slider, slider.getValue());
			Point trackPoint = SwingUtilities.convertPoint(slider, new Point(thumbCenterX, 0), thumbValueTrack);
			int x = trackPoint.x - thumbValueLabel.getWidth() / 2;
			int maxX = Math.max(0, thumbValueTrack.getWidth() - thumbValueLabel.getWidth());
			x = Math.max(0, Math.min(x, maxX));
			thumbValueLabel.setLocation(x, Math.max(0, (THUMB_VALUE_TRACK_HEIGHT - thumbValueLabel.getHeight()) / 2));
		}
		thumbValueTrack.repaint();
	}

	private static int xPositionForSliderValue(JSlider slider, int value) {
		if (slider.getUI() instanceof BasicSliderUI) {
			BasicSliderUI ui = (BasicSliderUI) slider.getUI();
			int low = 0;
			int high = Math.max(slider.getWidth() - 1, 0);
			while (low < high) {
				int mid = (low + high) / 2;
				if (ui.valueForXPosition(mid) < value) {
					low = mid + 1;
				} else {
					high = mid;
				}
			}
			return low;
		}
		int range = slider.getMaximum() - slider.getMinimum();
		if (range <= 0) {
			return slider.getWidth() / 2;
		}
		double fraction = (value - slider.getMinimum()) / (double) range;
		Insets insets = slider.getInsets();
		int left = insets.left + 8;
		int right = slider.getWidth() - insets.right - 8;
		return left + (int) Math.round(fraction * Math.max(right - left, 0));
	}

	private static int yPositionForSliderValue(JSlider slider, int value) {
		if (slider.getUI() instanceof BasicSliderUI) {
			BasicSliderUI ui = (BasicSliderUI) slider.getUI();
			// Vertical track: larger Y → smaller value (max at top).
			int low = 0;
			int high = Math.max(slider.getHeight() - 1, 0);
			while (low < high) {
				int mid = (low + high) / 2;
				if (ui.valueForYPosition(mid) > value) {
					low = mid + 1;
				} else {
					high = mid;
				}
			}
			return low;
		}
		int range = slider.getMaximum() - slider.getMinimum();
		if (range <= 0) {
			return slider.getHeight() / 2;
		}
		double fraction = (slider.getMaximum() - value) / (double) range;
		Insets insets = slider.getInsets();
		int top = insets.top + 8;
		int bottom = slider.getHeight() - insets.bottom - 8;
		return top + (int) Math.round(fraction * Math.max(bottom - top, 0));
	}

	/** Numeric part uses 3 significant digits (e.g. 1.50 sec, 399 ms). */
	private static String formatThreeDigitTimeMs(double milliseconds) {
		double seconds = milliseconds / 1000.0;
		double abs = Math.abs(seconds);
		String sign = seconds < 0 ? "-" : "";
		if (abs == 0) {
			return "0.00 sec";
		}
		if (abs < 1e-6) {
			return sign + threeDigitNumber(abs * 1e9) + " ns";
		}
		if (abs < 1e-3) {
			return sign + threeDigitNumber(abs * 1e6) + " \u03bcs";
		}
		if (abs < 1) {
			return sign + threeDigitNumber(abs * 1e3) + " ms";
		}
		if (abs < 60) {
			return sign + threeDigitNumber(abs) + " sec";
		}
		if (abs < 3600) {
			return sign + threeDigitNumber(abs / 60.0) + " min";
		}
		return sign + threeDigitNumber(abs / 3600.0) + " hr";
	}

	private static String formatThreeDigitCost(double cost) {
		double abs = Math.abs(cost);
		String sign = cost < 0 ? "-" : "";
		String[] units = { "", "k", "M", "B", "Tr" };
		int index = 0;
		while (abs >= 1000 && index < units.length - 1) {
			abs /= 1000;
			index++;
		}
		return sign + threeDigitNumber(abs) + units[index];
	}

	private static String threeDigitNumber(double magnitude) {
		double abs = Math.abs(magnitude);
		if (abs == 0) {
			return "0.00";
		}
		int digits;
		if (abs >= 100) {
			digits = 0;
		} else if (abs >= 10) {
			digits = 1;
		} else {
			digits = 2;
		}
		return String.format("%." + digits + "f", magnitude);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return new Dimension(SLIDER_PREFERRED_WIDTH, PANEL_COMPACT_HEIGHT);
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		// Match Property/Value pane wheel speed
		return 48;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return orientation == SwingConstants.VERTICAL ? Math.max(visibleRect.height - 16, 16) : Math.max(visibleRect.width - 16, 16);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		// Stay as wide as the viewport so a short height (vertical scrollbar) does not
		// collapse the slider to the old fixed ~200px preferred width.
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		// Portrait side dock: fill the tall viewport so the vertical slider can stretch.
		// Must key off viewport size (not this panel) or we never leave the short preferred height.
		return isPortraitLayout();
	}
	
	private void clearSliderComponent(Component component)
	{
		if (component != null)
			remove(component);
	}

	/**
	 * Show the selected operator's label and under-track span on the active slider.
	 * Pass null to clear.
	 */
	public void showSelectedOperator(Integer operatorPropertyId) {
		selectedOperatorId = operatorPropertyId;
		selectedSpanStart = null;
		selectedSpanEnd = null;
		if (selectedOperatorLabel != null) {
			selectedOperatorLabel.setText(" ");
			selectedOperatorLabel.setToolTipText("Selected operator span on this slider");
		}

		if (operatorPropertyId == null || planGraphPanel == null || planGraphPanel.getIdExplainPlan() == null) {
			if (spanSlider != null) {
				spanSlider.repaint();
			}
			return;
		}

		int idExplainPlan = planGraphPanel.getIdExplainPlan().intValue();
		// Span bar uses aligned annotation extents; label start/end use Property/Value grid strings.
		String strSQL = "select EPOA.id_explain_plan_property, EPOA.south_1_text, EPOA.node_type\n"
				+ ", coalesce(EPOA.operator_display_number, EPOA.id_explain_plan_property) as operator_display_number\n"
				+ ", EPOA.min_actual_startup_time_ms as start_ms\n"
				+ ", EPOA.max_actual_total_time_ms as end_ms\n"
				+ ", EPOA.difference_time_ms\n"
				+ ", EPOA.actual_startup_time_ms\n"
				+ ", EPOA.actual_total_time_ms\n"
				+ ", EPOA.parallel_leader_actual_startup_time_ms\n"
				+ ", EPOA.parallel_leader_actual_total_time_ms\n"
				+ ", EPOA.startup_cost * 1.0 as startup_cost\n"
				+ ", EPOA.total_cost * 1.0 as total_cost\n"
				+ ", (select EPP.operator_value from explain_plan_properties as EPP\n"
				+ "    where EPP.id_explain_plan = EPOA.id_explain_plan\n"
				+ "      and EPP.parent_id_explain_plan_property = EPOA.id_explain_plan_property\n"
				+ "      and EPP.operator_name = 'Actual Startup Time') as grid_actual_startup_time\n"
				+ ", (select EPP.operator_value from explain_plan_properties as EPP\n"
				+ "    where EPP.id_explain_plan = EPOA.id_explain_plan\n"
				+ "      and EPP.parent_id_explain_plan_property = EPOA.id_explain_plan_property\n"
				+ "      and EPP.operator_name = 'Actual Total Time') as grid_actual_total_time\n"
				+ ", (select EPP.operator_value from explain_plan_properties as EPP\n"
				+ "    where EPP.id_explain_plan = EPOA.id_explain_plan\n"
				+ "      and EPP.parent_id_explain_plan_property = EPOA.id_explain_plan_property\n"
				+ "      and EPP.operator_name = 'Startup Cost') as grid_startup_cost\n"
				+ ", (select EPP.operator_value from explain_plan_properties as EPP\n"
				+ "    where EPP.id_explain_plan = EPOA.id_explain_plan\n"
				+ "      and EPP.parent_id_explain_plan_property = EPOA.id_explain_plan_property\n"
				+ "      and EPP.operator_name = 'Total Cost') as grid_total_cost\n"
				+ "from explain_plan_operator_annotations as EPOA\n"
				+ "where EPOA.id_explain_plan = " + idExplainPlan + "\n"
				+ "and EPOA.id_explain_plan_property = " + operatorPropertyId.intValue();
		JsonObject row = Database.row(strSQL);
		if (row == null || row.get("id_explain_plan_property") == null) {
			if (spanSlider != null) {
				spanSlider.repaint();
			}
			return;
		}

		String south1 = row.getString("south_1_text");
		if (south1 == null || south1.isBlank()) {
			south1 = row.getString("node_type");
		}
		if (south1 == null) {
			south1 = "";
		}

		boolean timeMode = planGraphPanel.getSliderType() == SLIDER_TYPE_TIME;
		boolean calculated = false;
		String gridStartText;
		String gridEndText;
		if (timeMode) {
			selectedSpanStart = toDouble(row.get("start_ms"));
			selectedSpanEnd = toDouble(row.get("end_ms"));
			Double rawStart = firstNonNull(
					toDouble(row.get("actual_startup_time_ms")),
					toDouble(row.get("parallel_leader_actual_startup_time_ms")));
			Double rawEnd = firstNonNull(
					toDouble(row.get("actual_total_time_ms")),
					toDouble(row.get("parallel_leader_actual_total_time_ms")));
			calculated = isAlignedAwayFromRaw(selectedSpanStart, rawStart)
					|| isAlignedAwayFromRaw(selectedSpanEnd, rawEnd);
			gridStartText = firstNonBlank(row.getString("grid_actual_startup_time"),
					rawStart != null ? stripTrailingZeros(rawStart) : null);
			gridEndText = firstNonBlank(row.getString("grid_actual_total_time"),
					rawEnd != null ? stripTrailingZeros(rawEnd) : null);
		} else {
			selectedSpanStart = toDouble(row.get("startup_cost"));
			selectedSpanEnd = toDouble(row.get("total_cost"));
			gridStartText = firstNonBlank(row.getString("grid_startup_cost"),
					selectedSpanStart != null ? stripTrailingZeros(selectedSpanStart) : null);
			gridEndText = firstNonBlank(row.getString("grid_total_cost"),
					selectedSpanEnd != null ? stripTrailingZeros(selectedSpanEnd) : null);
			calculated = false;
		}

		if (selectedSpanStart != null && selectedSpanEnd != null && selectedSpanEnd < selectedSpanStart) {
			double tmp = selectedSpanStart;
			selectedSpanStart = selectedSpanEnd;
			selectedSpanEnd = tmp;
			calculated = true;
		}

		Double gridStart = toDouble(gridStartText);
		Double gridEnd = toDouble(gridEndText);
		Double spanDiff = null;
		if (gridStart != null && gridEnd != null) {
			spanDiff = gridEnd - gridStart;
		} else if (timeMode) {
			spanDiff = toDouble(row.get("difference_time_ms"));
			if (spanDiff == null && selectedSpanStart != null && selectedSpanEnd != null) {
				spanDiff = selectedSpanEnd - selectedSpanStart;
			}
		} else if (selectedSpanStart != null && selectedSpanEnd != null) {
			spanDiff = selectedSpanEnd - selectedSpanStart;
		}

		String startLabel = gridStartText;
		String endLabel = gridEndText;
		String diffLabel = null;
		if (timeMode) {
			// Property/Value stores raw ms numbers; restore sec/ms/… units for the slider caption.
			if (gridStart != null) {
				startLabel = formatThreeDigitTimeMs(gridStart);
			}
			if (gridEnd != null) {
				endLabel = formatThreeDigitTimeMs(gridEnd);
			}
			if (spanDiff != null) {
				diffLabel = formatThreeDigitTimeMs(spanDiff);
			}
		} else if (spanDiff != null) {
			int scale = Math.max(decimalPlaces(gridStartText), decimalPlaces(gridEndText));
			diffLabel = formatRoundedNumber(spanDiff, scale);
		}

		int displayNumber = operatorPropertyId.intValue();
		if (row.get("operator_display_number") instanceof Number) {
			displayNumber = ((Number) row.get("operator_display_number")).intValue();
		} else if (row.get("operator_display_number") != null) {
			try {
				displayNumber = Integer.parseInt(String.valueOf(row.get("operator_display_number")).trim());
			} catch (NumberFormatException ignored) {
				// keep sparse id
			}
		}
		String label = buildSelectedOperatorLabel(displayNumber, south1,
				startLabel, endLabel, diffLabel, calculated);
		if (selectedOperatorLabel != null) {
			selectedOperatorLabel.setText(label);
			selectedOperatorLabel.setToolTipText(label);
		}
		updateSliderModeLabels();

		if (spanSlider != null) {
			spanSlider.repaint();
		}
	}

	private static String buildSelectedOperatorLabel(int operatorId, String south1,
			String startText, String endText, String diffText, boolean calculated) {
		StringBuilder label = new StringBuilder();
		label.append('(').append(operatorId).append(") ").append(south1);
		if (startText == null || endText == null || startText.isBlank() || endText.isBlank()) {
			return label.toString();
		}
		label.append("  ").append(startText.trim())
				.append(" \u2192 ").append(endText.trim());
		if (diffText != null && !diffText.isBlank()) {
			label.append("  (diff ").append(diffText.trim()).append(')');
		}
		if (calculated) {
			label.append("  Calculated value");
		}
		return label.toString();
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a;
		}
		return b;
	}

	private static int decimalPlaces(String numberText) {
		if (numberText == null) {
			return 0;
		}
		String text = numberText.trim();
		int e = Math.max(text.indexOf('e'), text.indexOf('E'));
		if (e >= 0) {
			text = text.substring(0, e);
		}
		int dot = text.indexOf('.');
		if (dot < 0) {
			return 0;
		}
		return text.length() - dot - 1;
	}

	/** Round to the same decimal scale as Property/Value start/end text (avoids 2781.8849999999998). */
	private static String formatRoundedNumber(double value, int scale) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return Double.toString(value);
		}
		int safeScale = Math.max(0, Math.min(scale, 12));
		java.math.BigDecimal rounded = java.math.BigDecimal.valueOf(value)
				.setScale(safeScale, java.math.RoundingMode.HALF_UP)
				.stripTrailingZeros();
		return rounded.toPlainString();
	}

	/** Match Property/Value grid style: plain number text without forced unit reformatting. */
	private static String stripTrailingZeros(double value) {
		return formatRoundedNumber(value, 12);
	}

	private static Double firstNonNull(Double a, Double b) {
		return a != null ? a : b;
	}

	/** True when the slider span value differs from the raw EXPLAIN number (aligned for workers/PL). */
	private static boolean isAlignedAwayFromRaw(Double aligned, Double raw) {
		if (aligned == null || raw == null) {
			return false;
		}
		return Math.abs(aligned.doubleValue() - raw.doubleValue()) > 1e-9;
	}

	/** Re-install span-aware slider UI after Look-and-Feel changes. */
	public void reinstallSpanAwareSliderUI() {
		if (spanSlider != null) {
			installSpanAwareSliderUI(spanSlider);
			spanSlider.revalidate();
			spanSlider.repaint();
		}
	}

	private void installSpanAwareSliderUI(JSlider slider) {
		if (slider == null) {
			return;
		}
		if (UIManager.getLookAndFeel() instanceof FlatLaf) {
			slider.setUI(new OperatorSpanFlatSliderUI(this));
		} else {
			slider.setUI(new OperatorSpanBasicSliderUI(slider, this));
		}
	}

	private static Double toDouble(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(value.toString());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private Double[] selectedSpanAbsoluteRange() {
		if (selectedSpanStart == null || selectedSpanEnd == null) {
			return null;
		}
		return new Double[] { selectedSpanStart, selectedSpanEnd };
	}

	private Color selectedSpanBarColor() {
		boolean dark = UiTheme.isDark();
		if (planGraphPanel != null && planGraphPanel.getSliderType() == SLIDER_TYPE_COST) {
			return dark ? SPAN_BAR_COST_DARK : SPAN_BAR_COST;
		}
		return dark ? SPAN_BAR_TIME_DARK : SPAN_BAR_TIME;
	}

	private Double selectedSpanTotal() {
		if (planGraphPanel == null) {
			return null;
		}
		return planGraphPanel.getSliderType() == SLIDER_TYPE_TIME
				? getPlanTotalTimeMs()
				: getPlanTotalCost();
	}

	private static int absoluteToSliderValue(double absolute, double total, JSlider slider) {
		int value = (int) Math.round(absolute / total * slider.getMaximum());
		return Math.max(slider.getMinimum(), Math.min(slider.getMaximum(), value));
	}

	/**
	 * Paint span using the UI's own position-for-value + trackRect so the bar lands on the track.
	 */
	private static void paintSpanOnTrack(Graphics g, JSlider slider, Rectangle trackRect,
			IntUnaryOperator positionForValue, Double startAbs, Double endAbs, Double total, Color fill) {
		if (g == null || slider == null || trackRect == null || positionForValue == null
				|| startAbs == null || endAbs == null || total == null || total <= 0
				|| slider.getMaximum() <= 0 || fill == null) {
			return;
		}
		try {
			int v1 = absoluteToSliderValue(startAbs, total, slider);
			int v2 = absoluteToSliderValue(endAbs, total, slider);
			int p1 = positionForValue.applyAsInt(v1);
			int p2 = positionForValue.applyAsInt(v2);
			boolean vertical = slider.getOrientation() == SwingConstants.VERTICAL;

			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(fill);
				if (vertical) {
					int top = Math.max(trackRect.y, Math.min(p1, p2));
					int bottom = Math.min(trackRect.y + trackRect.height, Math.max(p1, p2));
					int height = Math.max(bottom - top, Math.min(8, trackRect.height));
					if (top + height > trackRect.y + trackRect.height) {
						top = Math.max(trackRect.y, trackRect.y + trackRect.height - height);
					}
					int barWidth = 5;
					int x = trackRect.x + (trackRect.width - barWidth) / 2;
					g2.fillRoundRect(x, top, barWidth, height, 2, 2);
				} else {
					int left = Math.max(trackRect.x, Math.min(p1, p2));
					int right = Math.min(trackRect.x + trackRect.width, Math.max(p1, p2));
					int width = Math.max(right - left, Math.min(8, trackRect.width));
					if (left + width > trackRect.x + trackRect.width) {
						left = Math.max(trackRect.x, trackRect.x + trackRect.width - width);
					}
					int barHeight = 5;
					int y = trackRect.y + (trackRect.height - barHeight) / 2;
					g2.fillRoundRect(left, y, width, barHeight, 2, 2);
				}
			} finally {
				g2.dispose();
			}
		} catch (RuntimeException ignored) {
			// Never let span painting break the EDT / theme redraw
		}
	}

	/** FlatLaf slider UI that paints the operator span on the real track. */
	private static final class OperatorSpanFlatSliderUI extends FlatSliderUI {
		private final SliderPanel panel;

		OperatorSpanFlatSliderUI(SliderPanel panel) {
			this.panel = panel;
		}

		@Override
		public void paintTrack(Graphics g) {
			super.paintTrack(g);
			Double[] range = panel.selectedSpanAbsoluteRange();
			if (slider == null || trackRect == null || range == null) {
				return;
			}
			IntUnaryOperator position = slider.getOrientation() == SwingConstants.VERTICAL
					? this::yPositionForValue
					: this::xPositionForValue;
			paintSpanOnTrack(g, slider, trackRect, position, range[0], range[1],
					panel.selectedSpanTotal(), panel.selectedSpanBarColor());
		}
	}

	/** System LAF slider UI that paints the operator span on the real track. */
	private static final class OperatorSpanBasicSliderUI extends BasicSliderUI {
		private final SliderPanel panel;

		OperatorSpanBasicSliderUI(JSlider slider, SliderPanel panel) {
			super(slider);
			this.panel = panel;
		}

		@Override
		public void paintTrack(Graphics g) {
			super.paintTrack(g);
			Double[] range = panel.selectedSpanAbsoluteRange();
			if (slider == null || trackRect == null || range == null) {
				return;
			}
			IntUnaryOperator position = slider.getOrientation() == SwingConstants.VERTICAL
					? this::yPositionForValue
					: this::xPositionForValue;
			paintSpanOnTrack(g, slider, trackRect, position, range[0], range[1],
					panel.selectedSpanTotal(), panel.selectedSpanBarColor());
		}
	}

	public JLabel getPropertiesDetails() {
		return propertiesDetails;
	}

	public void setPropertiesDetails(String propertiesDetails) {
		this.propertiesDetails.setText(propertiesDetails);
	}
	
	// set / get TOTAL time query takes to run
	public void setPlanTotalTimeMs(int idExplainPlan) {
		boolean bolShowFunctionName = false;
		boolean bolShowSQL = false;
		boolean bolShowProperties = false;
		Double fltPlanTotalTimeMs;
		double dblPercent;
		String strSQL;
		String strElapsedTime;
		//int idExplainPlan = 4;
		if (bolShowFunctionName == true) { System.out.println("SliderPanel.setPlanTotalTimeMs()"); }
		strSQL = "select execution_time_ms from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		JsonObject timeRow = Database.row(strSQL);
		fltPlanTotalTimeMs = timeRow != null ? (Double) timeRow.get("execution_time_ms") : null;

		if (fltPlanTotalTimeMs != null) {
			timeSlider.setMaximum(5000);
			timeSlider.setMajorTickSpacing(100);
			//timeSlider.setMinorTickSpacing(50);
			//position.put(dblRoundedToInt(timeSlider.getMaximum() * 0.25), new JLabel(dblRoundedToString(timeSlider.getMaximum() * 0.25)));
			//position.put(dblRoundedToInt(timeSlider.getMaximum() * 0.50), new JLabel(dblRoundedToString(timeSlider.getMaximum() * 0.50)));
			dblPercent = 0.25;
			position.put(dblRoundedToInt(timeSlider.getMaximum() * dblPercent), new JLabel(Utils.elapsed_time_ms(fltPlanTotalTimeMs * dblPercent)));
			dblPercent = 0.50;
			position.put(dblRoundedToInt(timeSlider.getMaximum() * dblPercent), new JLabel(Utils.elapsed_time_ms(fltPlanTotalTimeMs * dblPercent)));
			dblPercent = 0.75;
			position.put(dblRoundedToInt(timeSlider.getMaximum() * dblPercent), new JLabel(Utils.elapsed_time_ms(fltPlanTotalTimeMs * dblPercent)));
			//position.put(dblRoundedToInt(timeSlider.getMaximum() * 0.75), new JLabel(dblRoundedToString(timeSlider.getMaximum() * 0.75)));
			if (bolShowProperties == true) { System.out.println("timeSlider.getMajorTickSpacing(): " + timeSlider.getMajorTickSpacing()); }
			timeSlider.setPaintTicks(true);
			timeSlider.setPaintLabels(true);
			//this.fltPlanTotalTimeMs = fltPlanTotalTimeMs;
		}
		
		if (fltPlanTotalTimeMs != null) {
			strElapsedTime = Utils.elapsed_time_ms_one_decimal(fltPlanTotalTimeMs);
			if (bolShowProperties == true) { System.out.println("strElapsedTime: " + strElapsedTime); }
			//position.put(dblRoundedToInt(fltPlanTotalTimeMs), new JLabel(strElapsedTime)); // works, but tiny elapsed times are put in the wrong spot
			//position.put(dblRoundedToInt(fltPlanTotalTimeMs), new JLabel("goober")); // testing
			position.put(timeSlider.getMaximum(), new JLabel(strElapsedTime));
			if (bolShowProperties == true) { System.out.println("dblRoundedToInt(" + fltPlanTotalTimeMs + "): " + dblRoundedToInt(fltPlanTotalTimeMs)); }
			timeSlider.setLabelTable(position);
		}
		//setPlanTotalTimeMs(fltPlanTotalTimeMs);
	}
	
	
	public void setPlanTotalCost(int idExplainPlan) {
		boolean bolShowFunctionName = false;
		boolean bolShowSQL = false;
		boolean bolShowProperties = false;
		Double fltPlanTotalCost;
		double dblPercent;
		String strSQL;
		String strElapsedCost;
		//int idExplainPlan = 4;
		if (bolShowFunctionName == true) { System.out.println("SliderPanel.setPlanTotalCost()"); }
		strSQL = "select max_total_cost from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		JsonObject costRow = Database.row(strSQL);
		fltPlanTotalCost = costRow != null ? (Double) costRow.get("max_total_cost") : null;

		if (fltPlanTotalCost != null) {
			costSlider.setMaximum(5000);
			costSlider.setMajorTickSpacing(100);

			dblPercent = 0.25;
			position.put(dblRoundedToInt(costSlider.getMaximum() * dblPercent), new JLabel(Utils.formattedCost(fltPlanTotalCost * dblPercent)));
			dblPercent = 0.50;
			position.put(dblRoundedToInt(costSlider.getMaximum() * dblPercent), new JLabel(Utils.formattedCost(fltPlanTotalCost * dblPercent)));
			dblPercent = 0.75;
			position.put(dblRoundedToInt(costSlider.getMaximum() * dblPercent), new JLabel(Utils.formattedCost(fltPlanTotalCost * dblPercent)));

			if (bolShowProperties == true) { System.out.println("costSlider.getMajorTickSpacing(): " + costSlider.getMajorTickSpacing()); }
			costSlider.setPaintTicks(true);
			costSlider.setPaintLabels(true);
		}

		if (fltPlanTotalCost != null) {
			strElapsedCost = Double.toString(fltPlanTotalCost);
			if (bolShowProperties == true) { System.out.println("strElapsedCost: " + strElapsedCost); }
			//position.put(costSlider.getMaximum(), new JLabel(strElapsedCost));
			position.put(costSlider.getMaximum(), new JLabel(Utils.formattedCost(fltPlanTotalCost)));
			if (bolShowProperties == true) { System.out.println("dblRoundedToInt(" + fltPlanTotalCost + "): " + dblRoundedToInt(fltPlanTotalCost)); }
			costSlider.setLabelTable(position);
		}
	}

/*
	public void setPlanTotalTimeMs(Double fltPlanTotalTimeMs) {
		boolean bolShowFunctionName = false;
		if (bolShowFunctionName == true) { System.out.println("SliderPanel.setPlanTotalTimeMs(Double " + fltPlanTotalTimeMs + ")"); }
		//timeSlider.setMaximum(dblRoundedToInt(fltPlanTotalTimeMs));
		//position.put(dblRoundedToInt(fltPlanTotalTimeMs), new JLabel(Utils.elapsed_time_ms(fltPlanTotalTimeMs)));
		this.fltPlanTotalTimeMs = fltPlanTotalTimeMs;
	}
*/
	
	//public void setPlanTotalTimeMs(int intPlanTotalTimeMs) {
	//	setPlanTotalTimeMs(Double.valueOf(intPlanTotalTimeMs));
	//}
	
	public Double getPlanTotalTimeMs() {
		boolean bolShowFunctionName = false;
		boolean bolShowSQL = false;
		Double fltPlanTotalTimeMs;
		String strSQL;
		if (bolShowFunctionName == true) { System.out.println("SliderPanel.setPlanTotalTimeMs()"); }
		if (planGraphPanel == null || planGraphPanel.getIdExplainPlan() == null) {
			return 0d;
		}
		//int idExplainPlan = 4; // how do I get the current id_explain_plan number?
		int idExplainPlan = planGraphPanel.getIdExplainPlan().intValue();
		strSQL = "select execution_time_ms from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		JsonObject row = Database.row(strSQL);
		if (row == null) {
			return 0d;
		}
		fltPlanTotalTimeMs = (Double) row.get("execution_time_ms");
		//fltPlanTotalTimeMs = 54318.152; // actual total execution time for idExplainPlan = 4, a.k.a. "Plan 5", is 54 sec
		//if (fltPlanTotalTimeMs == null)
		//	fltPlanTotalTimeMs = (5 * 60 + 10) * 1000d; // 5 min 10 sec
		return fltPlanTotalTimeMs != null ? fltPlanTotalTimeMs : 0d;
	}

	public Double getPlanTotalCost() {
		boolean bolShowFunctionName = false;
		boolean bolShowSQL = false;
		Double fltPlanTotalCost;
		String strSQL;
		if (bolShowFunctionName == true) { System.out.println("SliderPanel.getPlanTotalCost()"); }
		if (planGraphPanel == null || planGraphPanel.getIdExplainPlan() == null) {
			return 0d;
		}
		//int idExplainPlan = 4; // how do I get the current id_explain_plan number?
		int idExplainPlan = planGraphPanel.getIdExplainPlan().intValue();
		strSQL = "select max_total_cost from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		JsonObject row = Database.row(strSQL);
		if (row == null) {
			return 0d;
		}
		fltPlanTotalCost = (Double) row.get("max_total_cost");
		return fltPlanTotalCost != null ? fltPlanTotalCost : 0d;
	}

	// set / get where the slider bar is while the plan is running
	public void setCurrentPlanTimeMs(Double fltCurrentPlanTimeMs) {
		if (planGraphPanel != null) {
			planGraphPanel.setPlanTimeSliderMs(fltCurrentPlanTimeMs);
		}
	}
	
	public void setCurrentPlanTimeMs(int intCurrentPlanTimeMs) {
		setCurrentPlanTimeMs(Double.valueOf(intCurrentPlanTimeMs));
	}
	
	public void setCurrentPlanTimeLabel(int intCurrentPlanTimeMs) {
		setCurrentPlanTimeLabel((double) intCurrentPlanTimeMs);
	}
	
	public void setCurrentPlanTimeLabel(double dblCurrentPlanTimeMs) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("dblCurrentPlanTimeMs: " + dblCurrentPlanTimeMs); }
		String text = formatThreeDigitTimeMs(dblCurrentPlanTimeMs);
		if (formattedCurrentTime != null) {
			formattedCurrentTime.setText(text);
		}
		updateThumbValueLabel(text, timeSlider);
	}
	
	public void setCurrentPlanCostLabel(double dblCurrentPlanCost) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("dblCurrentPlanCost: " + dblCurrentPlanCost); }
		String text = formatThreeDigitCost(dblCurrentPlanCost);
		if (formattedCurrentCost != null) {
			formattedCurrentCost.setText(text);
		}
		updateThumbValueLabel(text, costSlider);
	}

	private void updateThumbValueLabel(String text, JSlider slider) {
		if (thumbValueLabel == null) {
			return;
		}
		thumbValueLabel.setText(text);
		if (slider != null) {
			repositionThumbValueLabel(slider);
		}
	}
	
	public Double getCurrentPlanTimeMs() {
		return planGraphPanel != null ? planGraphPanel.getPlanTimeSliderValueMs() : 0d;
	}
	
	public void setCurrentPlanCost(Double fltCurrentPlanCost) {
		if (planGraphPanel != null) {
			planGraphPanel.setPlanCostSlider(fltCurrentPlanCost);
		}
	}

	public Double getCurrentPlanCost() {
		return planGraphPanel != null ? planGraphPanel.getPlanCostSlider() : 0d;
	}
	
/*
	public int getSliderType() {
		return this.sliderType; 
	}
	
	public void setSliderType(int sliderType) {
		this.sliderType = sliderType; 
	}
*/

}

