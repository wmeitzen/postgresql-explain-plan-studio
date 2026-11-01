package com.williammeitzen.peps.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Toolbars extends JPanel implements ActionListener {
	
	//private JLabel zoomLabel;
	//private JButton zoomMinusButton;
	//private JButton zoomPlusButton;

	//zoomLabel = new JLabel("Zoom: ");

	//zoomMinusButton = new JButton("-");
	//zoomMinusButton.addActionListener(this);

	//zoomPlusButton = new JButton("+");
	//zoomPlusButton.addActionListener(this);

	//add(zoomLabel);
	//add(zoomPlusButton);
	//add(zoomMinusButton);
	
	//private PlanGraphPanel graphPanel;

	public Toolbars() {
		JPanel toolbarsPane = new JPanel(new GridLayout(0, 1));
		toolbarsPane.add(new ZoomToolbar());
		toolbarsPane.add(new PercentageByRadioButtonsToolbar());
		toolbarsPane.add(new LineThicknessByRadioButtonsToolbar());
		//toolbarsPane.add(new SliderPanel(graphPanel));
		add(toolbarsPane);
	}

	public void actionPerformed(ActionEvent e) {
	}

}
