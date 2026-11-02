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

public class LineThicknessByRadioButtonsToolbar extends JToolBar implements ActionListener {
	
	private JLabel lineThicknessByLabel;
	private JRadioButton lineThicknessByActualMassButton;
	private JRadioButton lineThicknessByEstimatedMassButton;
	private JRadioButton lineThicknessByActualRowsButton;
	private JRadioButton lineThicknessByEstimatedRowsButton;
	private JRadioButton lineThicknessByActualVsEstimatedRowsRatioButton;
	private JRadioButton lineThicknessByActualVsEstimatedMassRatioButton;
	private ButtonGroup lineThicknessByButtonGroup;
	
	private StringListener textListener;
	private LineThicknessByRadioButtonsToolbarListener lineThicknessByRadioButtonsToolbarListener;
	
	public LineThicknessByRadioButtonsToolbar() {
		// setBorder is optional, but disables the "tear-away" feature 
		//setBorder(BorderFactory.createEtchedBorder());
		
		// "setFloatable(false)" also disables the "tear-away" feature
		//setFloatable(false);

		lineThicknessByLabel = new JLabel("Line Thickness By: ");
		
		lineThicknessByButtonGroup = new ButtonGroup();

		lineThicknessByActualMassButton = new JRadioButton("Actual Mass");
		lineThicknessByActualMassButton.addActionListener(this);

		lineThicknessByEstimatedMassButton = new JRadioButton("Estimated Mass");
		lineThicknessByEstimatedMassButton.addActionListener(this);

		lineThicknessByActualRowsButton = new JRadioButton("Actual Rows");
		lineThicknessByActualRowsButton.addActionListener(this);

		lineThicknessByEstimatedRowsButton = new JRadioButton("Estimated Rows");
		lineThicknessByEstimatedRowsButton.addActionListener(this);

		lineThicknessByActualVsEstimatedRowsRatioButton = new JRadioButton("Actual vs Estimated Rows Ratio");
		lineThicknessByActualVsEstimatedRowsRatioButton.addActionListener(this);

		lineThicknessByActualVsEstimatedMassRatioButton = new JRadioButton("Actual vs Estimated Mass Ratio");
		lineThicknessByActualVsEstimatedMassRatioButton.addActionListener(this);

		lineThicknessByButtonGroup.add(lineThicknessByActualMassButton);
		lineThicknessByButtonGroup.add(lineThicknessByEstimatedMassButton);
		lineThicknessByButtonGroup.add(lineThicknessByActualRowsButton);
		lineThicknessByButtonGroup.add(lineThicknessByEstimatedRowsButton);
		lineThicknessByButtonGroup.add(lineThicknessByActualVsEstimatedRowsRatioButton);
		lineThicknessByButtonGroup.add(lineThicknessByActualVsEstimatedMassRatioButton);
		
		add(lineThicknessByLabel);
		add(lineThicknessByActualMassButton);
		add(lineThicknessByEstimatedMassButton);
		add(lineThicknessByActualRowsButton);
		add(lineThicknessByEstimatedRowsButton);
		add(lineThicknessByActualVsEstimatedRowsRatioButton);
		add(lineThicknessByActualVsEstimatedMassRatioButton);

		// Match PlanGraphPanel / View menu default
		lineThicknessByActualRowsButton.setSelected(true);
	}

	public void setStringListener(StringListener listener) {
		this.textListener = listener;
	}
	
	public void setToolbarListener(LineThicknessByRadioButtonsToolbarListener listener) {
		this.lineThicknessByRadioButtonsToolbarListener = listener;
	}
	
	public void performEstimatedRowsButton() {
		performEvent(lineThicknessByEstimatedRowsButton);
	}
	
	public void performEstimatedMassButton() {
		performEvent(lineThicknessByEstimatedMassButton);
	}
	
	public void actionPerformed(ActionEvent e) {
		//System.out.println("Clicked");
		JRadioButton clicked = (JRadioButton) e.getSource();
		performEvent(clicked);		
	}
	
	public void performEvent(JRadioButton click) {
		if (click == lineThicknessByActualMassButton) {
			if (lineThicknessByRadioButtonsToolbarListener != null) {
				lineThicknessByRadioButtonsToolbarListener.lineThicknessByActualMassEventOccurred();
			}
		} else if (click == lineThicknessByEstimatedMassButton) {
			if (lineThicknessByRadioButtonsToolbarListener != null) {
				lineThicknessByRadioButtonsToolbarListener.lineThicknessByEstimatedMassEventOccurred();
			}
		} else if (click == lineThicknessByActualRowsButton) {
			if (lineThicknessByRadioButtonsToolbarListener != null) {
				lineThicknessByRadioButtonsToolbarListener.lineThicknessByActualRowsEventOccurred();
			}
		} else if (click == lineThicknessByEstimatedRowsButton) {
			if (lineThicknessByRadioButtonsToolbarListener != null) {
				lineThicknessByRadioButtonsToolbarListener.lineThicknessByEstimatedRowsEventOccurred();
			}
		} else if (click == lineThicknessByActualVsEstimatedRowsRatioButton) {
			if (lineThicknessByRadioButtonsToolbarListener != null) {
				lineThicknessByRadioButtonsToolbarListener.lineThicknessByActualVsEstimatedRowsRatioEventOccurred();
			}
		} else if (click == lineThicknessByActualVsEstimatedMassRatioButton) {
			if (lineThicknessByRadioButtonsToolbarListener != null) {
				lineThicknessByRadioButtonsToolbarListener.lineThicknessByActualVsEstimatedMassRatioEventOccurred();
			}
		}
		
	}
	
	
	
	// button selected and select methods
	public boolean getLineThicknessByActualMassButtonSelected() {
		return lineThicknessByActualMassButton.isSelected();
	}

	public void setLineThicknessByActualMassButtonSelect() {
		lineThicknessByActualMassButton.setSelected(true);
	}

	public boolean getLineThicknessByActualVsEstimatedMassRatioButtonSelected() {
		return lineThicknessByActualVsEstimatedMassRatioButton.isSelected();
	}

	public void setLineThicknessByActualVsEstimatedMassRatioButtonSelect() {
		lineThicknessByActualVsEstimatedMassRatioButton.setSelected(true);
	}
	
	public boolean getLineThicknessByActualVsEstimatedRowsRatioButtonSelected() {
		return lineThicknessByActualVsEstimatedRowsRatioButton.isSelected();
	}

	public void setLineThicknessByActualVsEstimatedRowsRatioButtonSelect() {
		lineThicknessByActualVsEstimatedRowsRatioButton.setSelected(true);
	}
	
	public boolean getLineThicknessByEstimatedRowsButtonSelected() {
		return lineThicknessByEstimatedRowsButton.isSelected();
	}
	
	public void setLineThicknessByEstimatedRowsButtonSelect() {
		lineThicknessByEstimatedRowsButton.setSelected(true);
		//lineThicknessByEstimatedRowsButton.doClick(); // doesn't help
	}

	public boolean getLineThicknessByActualRowsButtonSelected() {
		return lineThicknessByActualRowsButton.isSelected();
	}

	public void setLineThicknessByActualRowsButtonSelect() {
		lineThicknessByActualRowsButton.setSelected(true);
	}

	public boolean getLineThicknessByEstimatedMassButtonSelected() {
		return lineThicknessByEstimatedMassButton.isSelected();
	}

	public void setLineThicknessByEstimatedMassButtonSelect() {
		lineThicknessByEstimatedMassButton.setSelected(true);
	}

	
	
	// button enabled methods
	public boolean getLineThicknessByActualRowsButtonEnabled() {
		return lineThicknessByActualRowsButton.isEnabled();
	}
	
	public void setLineThicknessByActualRowsButtonEnabled(boolean enabled) {
		lineThicknessByActualRowsButton.setEnabled(enabled);
	}
	
	public boolean getLineThicknessByEstimatedRowsButtonEnabled() {
		return lineThicknessByEstimatedRowsButton.isEnabled();
	}
	
	public void setLineThicknessByEstimatedRowsButtonEnabled(boolean enabled) {
		lineThicknessByEstimatedRowsButton.setEnabled(enabled);
	}
	
	public boolean getLineThicknessByActualMassButtonEnabled() {
		return lineThicknessByActualMassButton.isEnabled();
	}
	
	public void setLineThicknessByActualMassButtonEnabled(boolean enabled) {
		lineThicknessByActualMassButton.setEnabled(enabled);
	}
	
	public boolean getLineThicknessByEstimatedMassButtonEnabled() {
		return lineThicknessByEstimatedMassButton.isEnabled();
	}
	
	public void setLineThicknessByEstimatedMassButtonEnabled(boolean enabled) {
		lineThicknessByEstimatedMassButton.setEnabled(enabled);
	}
	
	public boolean getLineThicknessByActualVsEstimatedRowsRatioButtonEnabled() {
		return lineThicknessByActualVsEstimatedRowsRatioButton.isEnabled();
	}
	
	public void setLineThicknessByActualVsEstimatedRowsRatioButtonEnabled(boolean enabled) {
		lineThicknessByActualVsEstimatedRowsRatioButton.setEnabled(enabled);
	}
	
	public boolean getLineThicknessByActualVsEstimatedMassRatioButtonEnabled() {
		return lineThicknessByActualVsEstimatedMassRatioButton.isEnabled();
	}
	
	public void setLineThicknessByActualVsEstimatedMassRatioButtonEnabled(boolean enabled) {
		lineThicknessByActualVsEstimatedMassRatioButton.setEnabled(enabled);
	}
}
