package com.williammeitzen.peps.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

import javax.swing.JButton;
import com.williammeitzen.peps.gui.MainFrame;

public class PlanListener implements ActionListener {
	//public void planEventOccurred(PlanGraphPanel ev);

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton clicked = (JButton) e.getSource();
		System.out.println(clicked);
/*
		if (clicked == twentyfivePercentRB) {
			if (textListener != null) {
				textListener.textEmitted("Hello\n");
			}
*/
	}
}
