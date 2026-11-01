package com.williammeitzen.peps.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public class ZoomToolbar extends JToolBar implements ActionListener {
	
	private JButton goodbyeButton;
	private JButton saveTextButton;
	private JButton saveButton;
	private JLabel zoomLabel;
	private JButton zoomMinusButton;
	private JButton zoomPlusButton;
	
	private StringListener textListener;
	private ZoomToolbarListener zoomToolbarListener;
	
	public ZoomToolbar() {
		// setBorder is optional, but disables the "tear-away" feature 
		//setBorder(BorderFactory.createEtchedBorder());
		
		// "setFloatable(false)" also disables the "tear-away" feature
		//setFloatable(false);
		
		goodbyeButton = new JButton("Goodbye");
		goodbyeButton.addActionListener(this);
		
		saveButton = new JButton();
		//saveButton.setIcon(Utils.createIcon("/images/Save.png"));
		saveButton.setIcon(new ImageIcon(getClass().getResource("/images/Save.png")));
		saveButton.setToolTipText("Test 2");
		saveButton.addActionListener(this);
		
		saveTextButton = new JButton("Test 3");
		//saveTextButton.setIcon(Utils.createIcon("/images/Save.png"));
		saveTextButton.setIcon(new ImageIcon(getClass().getResource("/images/Save.png")));
		saveTextButton.setToolTipText("Test 3 tooltip"); // optional
		saveTextButton.addActionListener(this);
		
		zoomLabel = new JLabel("Zoom: ");

		zoomMinusButton = new JButton("-");
		zoomMinusButton.addActionListener(this);

		zoomPlusButton = new JButton("+");
		zoomPlusButton.addActionListener(this);


		//add(goodbyeButton);
		//add(saveButton);
		//addSeparator();
		//add(saveTextButton);
		//addSeparator();
		add(zoomLabel);
		add(zoomPlusButton);
		add(zoomMinusButton);
	}

/*
	public void setTextPanel(TextPanel textPanel) {
		this.textPanel = textPanel;
	}
*/
	
	public void setStringListener(StringListener listener) {
		this.textListener = listener;
	}
	
	public void setToolbarListener(ZoomToolbarListener listener) {
		this.zoomToolbarListener = listener;
	}
	
	public void actionPerformed(ActionEvent e) {
		//System.out.println("Clicked");
		JButton clicked = (JButton) e.getSource();
		
		if (clicked == goodbyeButton) {
			if (textListener != null) {
				textListener.textEmitted("Goodbye\n");
			}
/*
		} else if (clicked == saveButton || clicked == saveTextButton) {
			if (zoomToolbarListener != null) {
				zoomToolbarListener.saveEventOccurred();
			}
*/
		} else if (clicked == zoomPlusButton) {
			if (zoomToolbarListener != null) {
				zoomToolbarListener.zoomPlusEventOccurred() ;
			}
		} else if (clicked == zoomMinusButton) {
			if (zoomToolbarListener != null) {
				zoomToolbarListener.zoomMinusEventOccurred() ;
			}
		}
	}
}
