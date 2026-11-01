package com.williammeitzen.peps.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
//import javax.swing.event.DocumentEvent;
//import javax.swing.event.DocumentListener;

//import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

//import gui.Utils.PercentageBy;
import com.williammeitzen.peps.model.Database;

public class SliderPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	private static final int SLIDER_PREFERRED_WIDTH = 600;
	private static final int SLIDER_PREFERRED_HEIGHT = 80;
	private static final int PANEL_MINIMUM_WIDTH = 320;
	
	private JLabel timeSliderLabel;
	private JLabel costSliderLabel;
	private JLabel formattedCurrentTime;
	private JLabel formattedCurrentCost;
	private JLabel propertiesDetails;
	private JRadioButton timeSliderRB;
	private JRadioButton costSliderRB;

//	private Double fltPlanTotalTimeMs;
	private JSlider timeSlider;
	private JSlider costSlider;
	private PlanGraphPanel planGraphPanel = null;
	private Hashtable<Integer, JLabel> position;
	private int sliderType; // 1 = time slider, 2 = cost slider
	public static int SLIDER_TYPE_TIME = 1;
	public static int SLIDER_TYPE_COST = 2;
	
	//private GridBagConstraints gc;
	
	public SliderPanel(PlanGraphPanel graphPanel) {
		planGraphPanel = graphPanel;
		setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH, 120));

		//createComponents_OLD();
		createComponents();
		
		//layoutComponents_OLD();
		layoutComponents();
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
		repaint();
	}


	
	public void createComponents() {
		
		Double planTotalTimeMs;
		Double planTotalCost;
		boolean bolShowProperties = false;
		boolean bolShowSQL = false;
		String strSQL;
		JsonObject jsnRow;

		int idExplainPlan = (planGraphPanel.getIdExplainPlan() != null) ? planGraphPanel.getIdExplainPlan().intValue() : -1;

		ButtonGroup sliderLabelButtonGroup = new ButtonGroup();
		//JRadioButton timeSliderRB = new JRadioButton("Time Slider");
		timeSliderRB = new JRadioButton("Time Slider");
		//JRadioButton costSliderRB = new JRadioButton("Cost Slider");
		costSliderRB = new JRadioButton("Cost Slider");
		
		strSQL = "select cost_enabled, exclusive_time_enabled, time_difference_enabled\n"
				+ "from explain_plan_generated_values where id_explain_plan = " + idExplainPlan; 
		if (bolShowSQL == true) { System.out.println(strSQL); }
		jsnRow = Database.row(strSQL);

		boolean bolEnableCost = Utils.objectToBoolean(jsnRow.get("cost_enabled"));
		boolean bolEnableExclusiveTime = Utils.objectToBoolean(jsnRow.get("exclusive_time_enabled"));
		boolean bolEnableTimeDifference = Utils.objectToBoolean(jsnRow.get("time_difference_enabled"));
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
		
		if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME && timeSliderRB.isEnabled() == true) {
			timeSliderRB.setSelected(true);
		} else if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_COST && costSliderRB.isEnabled() == true) {
			costSliderRB.setSelected(true);
		} else if (planGraphPanel.getSliderType() != SliderPanel.SLIDER_TYPE_TIME && planGraphPanel.getSliderType() != SliderPanel.SLIDER_TYPE_COST) {
			System.out.println("Error in SliderPanel.createComponents(): Unknown slider type");
		}
		sliderLabelButtonGroup.add(timeSliderRB);
		sliderLabelButtonGroup.add(costSliderRB);
		
		timeSliderLabel = new JLabel("Time Slider");
		costSliderLabel = new JLabel("Cost Slider");

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
					planGraphPanel.repaint();
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
					planGraphPanel.repaint();
				}
			}
		};

		timeSliderRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//repaintOnPercentageByChange(PercentageBy.COST);
				//planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME
				planGraphPanel.setSliderType(SliderPanel.SLIDER_TYPE_TIME);
				//repaint();
				refreshComponents();
			}
		});
		
		costSliderRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//repaintOnPercentageByChange(PercentageBy.COST);
				planGraphPanel.setSliderType(SliderPanel.SLIDER_TYPE_COST);
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

		if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME) {
			position.put(0, new JLabel("0 sec"));
		} else if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_COST) {
			position.put(0, new JLabel("0"));
		} else {
			System.out.println("Error in SliderPanel.createComponents(): Unknown slider type");
		}

		if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME) {
			// Set the label to be drawn
			timeSlider.setLabelTable(position);
			//timeSlider.addChangeListener(timeSliderChangeListener);
			formattedCurrentTime = new JLabel("0 sec");
		} else if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_COST) {
			// Set the label to be drawn
			costSlider.setLabelTable(position);
			//costSlider.addChangeListener(costSliderChangeListener);
			formattedCurrentCost = new JLabel("0");
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
		
		// Radio buttons — no vertical weight so a short pane doesn't compress the slider
		gc.gridy = 0;
		gc.gridx = 0;
		gc.weightx = 1;
		gc.weighty = 0;
		gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets(4, 8, 0, 8);
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

		add(timeSliderRB, gc);

		gc.gridy++;
		add(costSliderRB, gc);
		
		clearSliderComponent(costSlider);
		clearSliderComponent(costSliderLabel);
		clearSliderComponent(timeSlider);
		clearSliderComponent(timeSliderLabel);

		JLabel activeLabel = null;
		JSlider activeSlider = null;
		if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME) {
			activeLabel = timeSliderLabel;
			activeSlider = timeSlider;
		} else if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_COST) {
			activeLabel = costSliderLabel;
			activeSlider = costSlider;
		} else {
			System.out.println("Error in SliderPanel.layoutComponents(): Unknown slider type");
		}

		if (activeLabel != null && activeSlider != null) {
			gc.gridy++;
			gc.fill = GridBagConstraints.NONE;
			gc.weightx = 0;
			add(activeLabel, gc);

			gc.gridy++;
			gc.fill = GridBagConstraints.HORIZONTAL;
			gc.weightx = 1;
			gc.weighty = 0;
			gc.insets = new Insets(0, 8, 0, 8);
			activeSlider.setPreferredSize(new Dimension(SLIDER_PREFERRED_WIDTH, SLIDER_PREFERRED_HEIGHT));
			activeSlider.setMinimumSize(new Dimension(PANEL_MINIMUM_WIDTH, SLIDER_PREFERRED_HEIGHT));
			add(activeSlider, gc);
		}
		
		gc.gridy++;
		gc.weightx = 1;
		gc.weighty = 1; // leftover space below current-value label
		gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets(0, 8, 8, 8);
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		
		clearSliderComponent(formattedCurrentCost);
		clearSliderComponent(formattedCurrentTime);
		
		if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_TIME) {
			if (formattedCurrentTime == null) {
				formattedCurrentTime = new JLabel("0 sec");
			}
			add(formattedCurrentTime, gc);
			setPlanTotalTimeMs(planGraphPanel.getIdExplainPlan());
		} else if (planGraphPanel.getSliderType() == SliderPanel.SLIDER_TYPE_COST) {
			if (formattedCurrentCost == null) {
				formattedCurrentCost = new JLabel("0");
			}
			add(formattedCurrentCost, gc);
			setPlanTotalCost(planGraphPanel.getIdExplainPlan());
		} else {
			System.out.println("Error in SliderPanel.layoutComponents(): Unknown slider type");
		}
		
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return new Dimension(SLIDER_PREFERRED_WIDTH, 160);
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 16;
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
		return false;
	}
	
	private void clearSliderComponent(Component component)
	{
		if (component != null)
			remove(component);
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
		fltPlanTotalTimeMs = (Double) Database.row(strSQL).get("execution_time_ms");

		if (fltPlanTotalTimeMs != null) {
			timeSlider.setMaximum(5000);
			timeSlider.setMajorTickSpacing(100);
			//timeSlider.setMinorTickSpacing(50);
			//position.put(dblRoundedToInt(timeSlider.getMaximum() * 0.25), new JLabel(dblRoundedToString(timeSlider.getMaximum() * 0.25)));
			//position.put(dblRoundedToInt(timeSlider.getMaximum() * 0.50), new JLabel(dblRoundedToString(timeSlider.getMaximum() * 0.50)));
			dblPercent = 0.25;
			position.put(dblRoundedToInt(timeSlider.getMaximum() * dblPercent), new JLabel(Utils.elapsed_time_ms(getPlanTotalTimeMs() * dblPercent)));
			dblPercent = 0.50;
			position.put(dblRoundedToInt(timeSlider.getMaximum() * dblPercent), new JLabel(Utils.elapsed_time_ms(getPlanTotalTimeMs() * dblPercent)));
			dblPercent = 0.75;
			position.put(dblRoundedToInt(timeSlider.getMaximum() * dblPercent), new JLabel(Utils.elapsed_time_ms(getPlanTotalTimeMs() * dblPercent)));
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
		fltPlanTotalCost = (Double) Database.row(strSQL).get("max_total_cost");

		if (fltPlanTotalCost != null) {
			costSlider.setMaximum(5000);
			costSlider.setMajorTickSpacing(100);

			dblPercent = 0.25;
			position.put(dblRoundedToInt(costSlider.getMaximum() * dblPercent), new JLabel(Utils.formattedCost(getPlanTotalCost() * dblPercent)));
			dblPercent = 0.50;
			position.put(dblRoundedToInt(costSlider.getMaximum() * dblPercent), new JLabel(Utils.formattedCost(getPlanTotalCost() * dblPercent)));
			dblPercent = 0.75;
			position.put(dblRoundedToInt(costSlider.getMaximum() * dblPercent), new JLabel(Utils.formattedCost(getPlanTotalCost() * dblPercent)));

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
		//int idExplainPlan = 4; // how do I get the current id_explain_plan number?
		int idExplainPlan = (planGraphPanel.getIdExplainPlan() != null) ? planGraphPanel.getIdExplainPlan().intValue() : -1;
		strSQL = "select execution_time_ms from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		fltPlanTotalTimeMs = (Double) Database.row(strSQL).get("execution_time_ms");
		//fltPlanTotalTimeMs = 54318.152; // actual total execution time for idExplainPlan = 4, a.k.a. "Plan 5", is 54 sec
		//if (fltPlanTotalTimeMs == null)
		//	fltPlanTotalTimeMs = (5 * 60 + 10) * 1000d; // 5 min 10 sec
		return fltPlanTotalTimeMs;
	}

	public Double getPlanTotalCost() {
		boolean bolShowFunctionName = false;
		boolean bolShowSQL = false;
		Double fltPlanTotalCost;
		String strSQL;
		if (bolShowFunctionName == true) { System.out.println("SliderPanel.getPlanTotalCost()"); }
		//int idExplainPlan = 4; // how do I get the current id_explain_plan number?
		int idExplainPlan = (planGraphPanel.getIdExplainPlan() != null) ? planGraphPanel.getIdExplainPlan().intValue() : -1;
		strSQL = "select max_total_cost from explain_plan_generated_values where id_explain_plan = " + idExplainPlan;
		if (bolShowSQL == true) { System.out.println(strSQL); }
		fltPlanTotalCost = (Double) Database.row(strSQL).get("max_total_cost");
		return fltPlanTotalCost;
	}

	// set / get where the slider bar is while the plan is running
	public void setCurrentPlanTimeMs(Double fltCurrentPlanTimeMs) {
		planGraphPanel.setPlanTimeSliderMs(fltCurrentPlanTimeMs); 
	}
	
	public void setCurrentPlanTimeMs(int intCurrentPlanTimeMs) {
		setCurrentPlanTimeMs(Double.valueOf(intCurrentPlanTimeMs));
	}
	
	public void setCurrentPlanTimeLabel(int intCurrentPlanTimeMs) {
		formattedCurrentTime.setText(Utils.elapsed_time_ms_one_decimal(intCurrentPlanTimeMs));
	}
	
	public void setCurrentPlanTimeLabel(double dblCurrentPlanTimeMs) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("dblCurrentPlanTimeMs: " + dblCurrentPlanTimeMs); }
		formattedCurrentTime.setText(Utils.elapsed_time_ms_one_decimal(dblCurrentPlanTimeMs));
	}
	
	public void setCurrentPlanCostLabel(double dblCurrentPlanCost) {
		boolean bolShowProperties = false;
		if (bolShowProperties == true) { System.out.println("dblCurrentPlanCost: " + dblCurrentPlanCost); }
		//formattedCurrentCost.setText(Double.toString(dblCurrentPlanCost));
		formattedCurrentCost.setText(Utils.formattedCost(dblCurrentPlanCost));
	}
	
	public Double getCurrentPlanTimeMs() {
		return planGraphPanel.getPlanTimeSliderValueMs(); 
	}
	
	public void setCurrentPlanCost(Double fltCurrentPlanCost) {
		planGraphPanel.setPlanCostSlider(fltCurrentPlanCost); 
	}

	public Double getCurrentPlanCost() {
		return planGraphPanel.getPlanCostSlider(); 
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

