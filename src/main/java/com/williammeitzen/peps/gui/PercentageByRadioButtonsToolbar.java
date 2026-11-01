package com.williammeitzen.peps.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;

public class PercentageByRadioButtonsToolbar extends JToolBar implements ActionListener {
	
	private JLabel percentageByLabel;
	private JRadioButton percentageByCostRadioButton;
	private JRadioButton percentageByExclusiveTimeRadioButton;
	private JRadioButton percentageByTimeDifferenceRadioButton;
	private ButtonGroup percentageByButtonGroup;
	
	private StringListener textListener;
	private PercentageByRadioButtonsToolbarListener percentageByRadioButtonsToolbarListener;
	
	public PercentageByRadioButtonsToolbar() {

		percentageByLabel = new JLabel("Percentage By: ");
		
		//JRadioButton estimatedRowsRB = new JRadioButton("Estimated Rows");
		//JRadioButton actualVsEstimatedRB = new JRadioButton("Actual vs. Est. Ratio");
		//JRadioButton actualVsEstimatedRowsRB = new JRadioButton("Actual vs. Est. Rows Ratio");
		//JRadioButton actualVsEstimatedMassRB = new JRadioButton("Actual vs. Est. Mass Ratio");

		percentageByButtonGroup = new ButtonGroup();
		
		percentageByCostRadioButton = new JRadioButton("Cost");
		percentageByCostRadioButton.addActionListener(this);

		percentageByExclusiveTimeRadioButton = new JRadioButton("Exclusive Time");
		percentageByExclusiveTimeRadioButton.addActionListener(this);

		percentageByTimeDifferenceRadioButton = new JRadioButton("Time Difference");
		percentageByTimeDifferenceRadioButton.addActionListener(this);

		add(percentageByLabel);
		//add(percentageByCostRadioButton);
		//add(percentageByExclusiveTimeRadioButton);
		//add(percentageByTimeDifferenceRadioButton);
		
		percentageByButtonGroup.add(percentageByCostRadioButton);
		percentageByButtonGroup.add(percentageByExclusiveTimeRadioButton);
		percentageByButtonGroup.add(percentageByTimeDifferenceRadioButton);

		add(percentageByCostRadioButton);
		add(percentageByExclusiveTimeRadioButton);
		add(percentageByTimeDifferenceRadioButton);

	}

/*
	public void setTextPanel(TextPanel textPanel) {
		this.textPanel = textPanel;
	}
*/
	
	public void setStringListener(StringListener listener) {
		this.textListener = listener;
	}
	
	public void setToolbarListener(PercentageByRadioButtonsToolbarListener listener) {
		this.percentageByRadioButtonsToolbarListener = listener;
	}
	
	public void performCostButton() {
		performEvent(percentageByCostRadioButton);
	}

	public void performExclusiveTimeButton() {
		performEvent(percentageByExclusiveTimeRadioButton);
	}

	public void performTimeDifferenceButton() {
		performEvent(percentageByTimeDifferenceRadioButton);
	}

	
	public void performEvent(JRadioButton click) {
		if (click == percentageByCostRadioButton) {
			if (percentageByRadioButtonsToolbarListener != null) {
				percentageByRadioButtonsToolbarListener.percentageByCostEventOccurred() ;
			}

		} else if (click == percentageByExclusiveTimeRadioButton) {
			if (percentageByRadioButtonsToolbarListener != null) {
				percentageByRadioButtonsToolbarListener.percentageByExclusiveTimeEventOccurred() ;
			}
		} else if (click == percentageByTimeDifferenceRadioButton) {
			if (percentageByRadioButtonsToolbarListener != null) {
				percentageByRadioButtonsToolbarListener.percentageByTimeDifferenceEventOccurred() ;
			}

		}
	}
	
	public void actionPerformed(ActionEvent e) {
		//System.out.println("Clicked");
		JRadioButton clicked = (JRadioButton) e.getSource();
		performEvent(clicked);		
	}

	// button selected methods
	// don't need all of them b/c we'll call these methods only if the one selected isn't available any more
	public boolean getPercentageByCostButtonSelected() {
		return percentageByCostRadioButton.isSelected();
	}
	
	public void setPercentageByCostButtonSelect() {
		percentageByCostRadioButton.setSelected(true);
	}
	
	public boolean getPercentageByExclusiveTimeButtonSelected() {
		return percentageByExclusiveTimeRadioButton.isSelected();
	}
	
	public void setPercentageByExclusiveTimeButtonSelect() {
		percentageByExclusiveTimeRadioButton.setSelected(true);
	}
	
	public boolean getPercentageByTimeDifferenceButtonSelected() {
		return percentageByTimeDifferenceRadioButton.isSelected();
	}
	
	public void setPercentageByTimeDifferenceButtonSelect() {
		percentageByTimeDifferenceRadioButton.setSelected(true);
	}
	
	
	

	public void setPercentageByCostButtonEnabled(boolean enabled) {
		percentageByCostRadioButton.setEnabled(enabled);
	}

	public void setPercentageByExclusiveTimeButtonEnabled(boolean enabled) {
		percentageByExclusiveTimeRadioButton.setEnabled(enabled);
	}

	public void setPercentageByTimeDifferenceButtonEnabled(boolean enabled) {
		percentageByTimeDifferenceRadioButton.setEnabled(enabled);
	}

}
