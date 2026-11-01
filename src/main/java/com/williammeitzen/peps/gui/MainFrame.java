package com.williammeitzen.peps.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;

import com.williammeitzen.peps.model.Database;
import com.williammeitzen.peps.gui.FormPanel;
import com.williammeitzen.peps.gui.PlanGraphPanel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
//import java.awt.FlowLayout;
//import java.awt.Font;
import java.awt.GridLayout;
//import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
//import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
//import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
//import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
//import javax.swing.JTextArea;
//import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.json.simple.JsonObject;

//import com.github.cliftonlabs.json_simple.JsonException;
//import com.github.cliftonlabs.json_simple.JsonArray;
//import com.github.cliftonlabs.json_simple.JsonObject;
//import com.github.cliftonlabs.json_simple.JsonKey;

import com.williammeitzen.peps.gui.Utils.LineThicknesses;
import com.williammeitzen.peps.gui.Utils.PercentageBy;

public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private static final String APPLICATION_VERSION = "0.0.2";
	private ZoomToolbar zoomToolbar;
	//private LineThicknessByToolbar lineThicknessByToolbar;
	private LineThicknessByRadioButtonsToolbar lineThicknessByRadioButtonsToolbar;
	//private PercentageByToolbar percentageByToolbar;
	private PercentageByRadioButtonsToolbar percentageByRadioButtonsToolbar;
	private FormPanel formPanel;
	//private JFileChooser fileChooser;
	//private PrefsDialog prefsDialog;
	//private Preferences prefs;
	private JSplitPane splitPane;
	private JTabbedPane tabPane;
	
	public MainFrame() {
		super("PostgreSQL Explain Plan Studio");

		boolean bolShowProperties = false;

		if (bolShowProperties == true) { System.out.println("MainFrame(): Database.init()"); }
		Database.init();

		setLayout(new BorderLayout());
		
		//toolbar = new Toolbar();
		zoomToolbar = new ZoomToolbar();
		//lineThicknessByToolbar = new LineThicknessByToolbar();
		lineThicknessByRadioButtonsToolbar = new LineThicknessByRadioButtonsToolbar();
		//percentageByToolbar = new PercentageByToolbar();
		percentageByRadioButtonsToolbar = new PercentageByRadioButtonsToolbar();
		//prefsDialog = new PrefsDialog(this);
		tabPane = new JTabbedPane();
		//tabPane.setFont(new Font("Verdana", Font.PLAIN, 10));
		formPanel = new FormPanel(tabPane);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, tabPane);
		splitPane.setOneTouchExpandable(true); // adds arrows to split bar
		
		setJMenuBar(createMenuBar()); // can't call set JMenuBar for historical / compatibility reasons
		
		addWindowListener(new WindowAdapter() {
 			public void windowClosing(WindowEvent e) {
 				//System.out.println("Window is closing");
  				//controller.disconnect(); // disconnect from db
 				// Suggested by Purcell to prevent some errors during the exit process
 				// I've never seen an error during the exit process; including it anyway
 				// Write application dimensions to config
 				Dimension dimApplicationSize = getSize();
 				Integer intWidth = dimApplicationSize.width;
 				Integer intHeight = dimApplicationSize.height;
 				Database.dml("update config set application_width = " + intWidth + ", application_height = " + intHeight);
 				dispose();
 				System.gc();
			}
		});

		zoomToolbar.setToolbarListener(new ZoomToolbarListener() {

			public void refreshEventOccurred() {
			}

			public void zoomPlusEventOccurred() {
				repaintOnZoomPlusOrMinus(10); // increase by 10%
			}
			
			public void zoomMinusEventOccurred() {
				repaintOnZoomPlusOrMinus(-10); // decrease by 10%
			}
		});
		
		lineThicknessByRadioButtonsToolbar.setToolbarListener(new LineThicknessByRadioButtonsToolbarListener() {
			public void lineThicknessByActualMassEventOccurred() {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_DATA_MASS);
			}
			
			public void lineThicknessByEstimatedMassEventOccurred() {
				repaintOnLineThicknessByChange(LineThicknesses.ESTIMATED_DATA_MASS);
			}
			
			public void lineThicknessByActualRowsEventOccurred() {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_ROWS);
			}
			
			public void lineThicknessByEstimatedRowsEventOccurred() {
				repaintOnLineThicknessByChange(LineThicknesses.ESTIMATED_ROWS);
			}
			
			public void lineThicknessByActualVsEstimatedRowsRatioEventOccurred() {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_VS_ESTIMATED_ROWS_RATIO);
			}
			
			public void lineThicknessByActualVsEstimatedMassRatioEventOccurred() {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_VS_ESTIMATED_MASS_RATIO);
			}

		});

		percentageByRadioButtonsToolbar.setToolbarListener(new PercentageByRadioButtonsToolbarListener() {

			public void percentageByCostEventOccurred() {
				repaintOnPercentageByChange(PercentageBy.COST);
			}
			
			public void percentageByExclusiveTimeEventOccurred() {
				repaintOnPercentageByChange(PercentageBy.EXCLUSIVE_TIME);
			}
			
			public void percentageByTimeDifferenceEventOccurred() {
				repaintOnPercentageByChange(PercentageBy.TIME_DIFFERENCE);
			}
		});
		

		JPanel toolbarsPane = new JPanel(new GridLayout(0, 1)); // works
		//JPanel toolbarsPane = new JPanel();
		//GridBagConstraints gc = new GridBagConstraints();
		//gc.gridx = 0;
		//gc.gridy = 0;
		//GridBagConstraints toolbarsPane = new GridBagConstraints(); // testing
		toolbarsPane.add(zoomToolbar);
		toolbarsPane.add(lineThicknessByRadioButtonsToolbar);
		toolbarsPane.add(percentageByRadioButtonsToolbar);
		//add(toolbarsPane, gc);
		//GridBagConstraints gc = new GridBagConstraints(); // 2
		//gc.gridx = 0; // 2
		//gc.gridy = 0; // 2
		//add(toolbarsPane, gc); // 2
		
		//add(toolbar, BorderLayout.PAGE_START); // original, works
		add(toolbarsPane, BorderLayout.PAGE_START); // 1
		add(splitPane, BorderLayout.CENTER);
		
		setMinimumSize(new Dimension(100, 100));

		// get application dimensions from config table
		JsonObject jsnRow;
		String strSQL;
		strSQL = "select application_width, application_height from config;";
		jsnRow = Database.row(strSQL);
		setSize(jsnRow.getInteger("application_width"), jsnRow.getInteger("application_height")); // UNCOMMENT THIS

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		//JMenuItem exportDataItem = new JMenuItem("Export Data ...");
		//JMenuItem importDataItem = new JMenuItem("Import Data ...");
		JMenuItem exitItem = new JMenuItem("Exit");
		
		//fileMenu.add(exportDataItem);
		//fileMenu.add(importDataItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		
		JMenu viewMenu = new JMenu("View");
		JMenu lineThicknessByItem = new JMenu("Line Thickness By");
		viewMenu.add(lineThicknessByItem);
		ButtonGroup lineThicknessByButtonGroup = new ButtonGroup();
		JRadioButton actualDataSizeRB = new JRadioButton("Actual Mass");
		JRadioButton estimatedDataSizeRB = new JRadioButton("Estimated Mass");
		JRadioButton actualRowsRB = new JRadioButton("Actual Rows");
		JRadioButton estimatedRowsRB = new JRadioButton("Estimated Rows");
		//JRadioButton actualVsEstimatedRB = new JRadioButton("Actual vs. Est. Ratio");
		JRadioButton actualVsEstimatedRowsRB = new JRadioButton("Actual vs. Est. Rows Ratio");
		JRadioButton actualVsEstimatedMassRB = new JRadioButton("Actual vs. Est. Mass Ratio");
		lineThicknessByButtonGroup.add(actualDataSizeRB);
		lineThicknessByButtonGroup.add(estimatedDataSizeRB);
		lineThicknessByButtonGroup.add(actualRowsRB);
		lineThicknessByButtonGroup.add(estimatedRowsRB);
		//lineThicknessByButtonGroup.add(actualVsEstimatedRB);
		lineThicknessByButtonGroup.add(actualVsEstimatedRowsRB);
		lineThicknessByButtonGroup.add(actualVsEstimatedMassRB);
		lineThicknessByItem.add(actualDataSizeRB);
		lineThicknessByItem.add(estimatedDataSizeRB);
		lineThicknessByItem.add(actualRowsRB);
		lineThicknessByItem.add(estimatedRowsRB);
		//lineThicknessByItem.add(actualVsEstimatedRB);
		lineThicknessByItem.add(actualVsEstimatedRowsRB);
		lineThicknessByItem.add(actualVsEstimatedMassRB);
		actualRowsRB.setSelected(true); // update PlanGraphPanel.java.LineThicknesses enmLineThicknessBy
		
		JMenu helpMenu = new JMenu("Help");
		JMenuItem helpMenuAboutItem = new JMenuItem("About ...");
		helpMenu.add(helpMenuAboutItem);


		actualDataSizeRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_DATA_MASS);
			}
		});
		
		estimatedDataSizeRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnLineThicknessByChange(LineThicknesses.ESTIMATED_DATA_MASS);
			}
		});
		
		actualRowsRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_ROWS);
			}
		});
		
		estimatedRowsRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnLineThicknessByChange(LineThicknesses.ESTIMATED_ROWS);
			}
		});
		
		actualVsEstimatedRowsRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_VS_ESTIMATED_ROWS_RATIO);
			}
		});
		
		actualVsEstimatedMassRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnLineThicknessByChange(LineThicknesses.ACTUAL_VS_ESTIMATED_MASS_RATIO);
			}
		});
		
		JMenu percentageByItem = new JMenu("Percentage By");
		viewMenu.add(percentageByItem);
		ButtonGroup percentageByButtonGroup = new ButtonGroup();
		JRadioButton costRB = new JRadioButton("Cost");
		//JRadioButton diskPlusRAMRB = new JRadioButton("Disk + RAM");
		//JRadioButton diskRB = new JRadioButton("Disk");
		//JRadioButton RAMRB = new JRadioButton("RAM");
		JRadioButton exclusiveTimeRB = new JRadioButton("Exclusive Time");
		JRadioButton timeDifferenceRB = new JRadioButton("Time Difference");
		percentageByButtonGroup.add(costRB);
		//percentageByButtonGroup.add(diskPlusRAMRB);
		//percentageByButtonGroup.add(diskRB);
		//percentageByButtonGroup.add(RAMRB);
		percentageByButtonGroup.add(exclusiveTimeRB);
		percentageByButtonGroup.add(timeDifferenceRB);
		percentageByItem.add(costRB);
		//percentageByItem.add(diskPlusRAMRB);
		//percentageByItem.add(diskRB);
		//percentageByItem.add(RAMRB);
		percentageByItem.add(exclusiveTimeRB);
		percentageByItem.add(timeDifferenceRB);
		costRB.setSelected(true); // update PlanGraphPanel.java.PercentageBy percentageBy

		costRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnPercentageByChange(PercentageBy.COST);
			}
		});

		exclusiveTimeRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnPercentageByChange(PercentageBy.EXCLUSIVE_TIME);
			}
		});
		
		timeDifferenceRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnPercentageByChange(PercentageBy.TIME_DIFFERENCE);
			}
		});
		
		JMenu zoomItem = new JMenu("Zoom");
		viewMenu.add(zoomItem);
		ButtonGroup zoomButtonGroup = new ButtonGroup();
		JRadioButton twentyfivePercentRB = new JRadioButton("25%");
		JRadioButton fiftyPercentRB = new JRadioButton("50%");
		JRadioButton seventyfivePercentRB = new JRadioButton("75%");
		JRadioButton oneHundredPercentRB = new JRadioButton("100%");
		JRadioButton oneHundredFiftyPercentRB = new JRadioButton("150%");
		JRadioButton threeHundredPercentRB = new JRadioButton("300%");
		JRadioButton fitToWindowRB = new JRadioButton("Fit to Window");
		zoomButtonGroup.add(twentyfivePercentRB);
		zoomButtonGroup.add(fiftyPercentRB);
		zoomButtonGroup.add(seventyfivePercentRB);
		zoomButtonGroup.add(oneHundredPercentRB);
		zoomButtonGroup.add(oneHundredFiftyPercentRB);
		zoomButtonGroup.add(threeHundredPercentRB);
		zoomButtonGroup.add(fitToWindowRB);
		zoomItem.add(twentyfivePercentRB);
		zoomItem.add(fiftyPercentRB);
		zoomItem.add(seventyfivePercentRB);
		zoomItem.add(oneHundredPercentRB);
		zoomItem.add(oneHundredFiftyPercentRB);
		zoomItem.add(threeHundredPercentRB);
		//zoomItem.add(fitToWindowRB); // not working yet
		oneHundredPercentRB.setSelected(true);

		twentyfivePercentRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(25);
			}
		});
		
		fiftyPercentRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(50);
			}
		});
		
		seventyfivePercentRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(75);
			}
		});
		
		oneHundredPercentRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(100);
			}
		});
		
		oneHundredFiftyPercentRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(150);
			}
		});
		
		threeHundredPercentRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(300);
			}
		});
		
		fitToWindowRB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaintOnZoomChange(-1);
			}
		});
		
		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);

		fileMenu.setMnemonic(KeyEvent.VK_F); // option-F underlines, but does not pull down, File menu. Why?
		exitItem.setMnemonic(KeyEvent.VK_X); // option-X underlines, but does not select, Exit item. Why?
		
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
					WindowListener[] listeners = getWindowListeners();
					
					for (WindowListener listener: listeners) {
						listener.windowClosing(new WindowEvent(MainFrame.this, 0));
					}
				}
//			}
		});

		class AboutDialog extends JDialog {
		  //public AboutDialog(JFrame parent) {
			public AboutDialog() {
				//super(modalBlocker, "hello", true);
				setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
				String strDownloadURL = "https://github.com/wmeitzen/postgresql-explain-plan-studio/";
				String strSupportURL = "https://github.com/wmeitzen/postgresql-explain-plan-studio/issues/";
		
				Box b = Box.createVerticalBox();
			    b.add(Box.createGlue());
			    b.add(new JLabel("Java source code and product"));
			    b.add(new JLabel("By William Meitzen"));
			    JLabel downloadLink = new JLabel("At " + strDownloadURL);
			    downloadLink.setForeground(Color.BLUE.darker());
			    downloadLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			    b.add(downloadLink);
			    JLabel supportLink = new JLabel("Support: " + strSupportURL);
			    supportLink.setForeground(Color.BLUE.darker());
			    supportLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			    b.add(supportLink);
			    b.add(new JLabel("Application version: " + applicationVersion()));
			    b.add(new JLabel("SQLite database schema version: " + Database.dbSchemaVersionNumber()));
			    b.add(Box.createGlue());
			    getContentPane().add(b, "Center");

			    downloadLink.addMouseListener(new MouseAdapter() {
			        @Override
			        public void mouseClicked(MouseEvent e) {
			        	try {
			                Desktop.getDesktop().browse(new URI(strDownloadURL));
			            } catch (IOException | URISyntaxException e1) {
			                e1.printStackTrace();
			            }
			        }
			    });
			    
			    supportLink.addMouseListener(new MouseAdapter() {
			        @Override
			        public void mouseClicked(MouseEvent e) {
			        	try {
			                Desktop.getDesktop().browse(new URI(strSupportURL));
			            } catch (IOException | URISyntaxException e1) {
			                e1.printStackTrace();
			            }
			        }
			    });
			    
			    JPanel p2 = new JPanel();
			    JButton ok = new JButton("Ok");
			    p2.add(ok);
			    getContentPane().add(p2, "South");
				super.setLocationRelativeTo(this);
				super.setTitle("PostgreSQL Explain Plan Studio " + applicationVersion());

			    ok.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent evt) {
			        setVisible(false);
			        dispose();
			      }
			    });
			    setSize(500, 200);

		  }
		}
		
		helpMenuAboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
		        AboutDialog aboutDialog = new AboutDialog();
		        aboutDialog.setVisible(true);
		      }
		});

		return menuBar;
	}
	
	private void repaintOnZoomChange(int zoomPct) {
		boolean bolShowFunctionName = false;
		JScrollPane scrollPane = null;
		
		if (bolShowFunctionName == true) { System.out.println("repaintOnZoomChange()"); }
		
		if (tabPane.getSelectedComponent() != null) {
			if (tabPane.getSelectedComponent() instanceof JScrollPane) {
				scrollPane = (JScrollPane) tabPane.getSelectedComponent();
			} else if (tabPane.getSelectedComponent() instanceof JSplitPane) {
				JSplitPane splitPane = (JSplitPane) tabPane.getSelectedComponent();
				if (splitPane.getLeftComponent() instanceof JScrollPane) {
					scrollPane = (JScrollPane) splitPane.getLeftComponent();
				} else if (splitPane.getLeftComponent() instanceof JSplitPane) {
					JSplitPane leftSplitPane = (JSplitPane) splitPane.getLeftComponent();
					if (leftSplitPane.getLeftComponent() instanceof JScrollPane) {
						scrollPane = (JScrollPane) leftSplitPane.getLeftComponent();
					}
				}
			}
			
			if (scrollPane != null) {
				PlanGraphPanel planGraphPanel = (PlanGraphPanel) scrollPane.getViewport().getComponent(0);
				planGraphPanel.setZoomPercentage(zoomPct);
				planGraphPanel.repaint();
			}
		}
	}
	
	private void repaintOnZoomPlusOrMinus(int zoomPct) {
		boolean bolShowFunctionName = false;
		JScrollPane scrollPane = null;
		int currentZoomPercentage;
		
		if (bolShowFunctionName == true) { System.out.println("repaintOnZoomPlusOrMinus(int " + zoomPct + ")"); }
		
		if (tabPane.getSelectedComponent() != null) {
			if (tabPane.getSelectedComponent() instanceof JScrollPane) {
				scrollPane = (JScrollPane) tabPane.getSelectedComponent();
			} else if (tabPane.getSelectedComponent() instanceof JSplitPane) {
				JSplitPane splitPane = (JSplitPane) tabPane.getSelectedComponent();
				if (splitPane.getLeftComponent() instanceof JScrollPane) {
					scrollPane = (JScrollPane) splitPane.getLeftComponent();
				} else if (splitPane.getLeftComponent() instanceof JSplitPane) {
					JSplitPane leftSplitPane = (JSplitPane) splitPane.getLeftComponent();
					if (leftSplitPane.getLeftComponent() instanceof JScrollPane) {
						scrollPane = (JScrollPane) leftSplitPane.getLeftComponent();
					}
				}
			}
			
			if (scrollPane != null) {
				if (scrollPane.getViewport().getComponent(0) instanceof PlanGraphPanel) {
					PlanGraphPanel planGraphPanel = (PlanGraphPanel) scrollPane.getViewport().getComponent(0);
					currentZoomPercentage = planGraphPanel.getZoomPercentage();
					planGraphPanel.setZoomPercentage(currentZoomPercentage + zoomPct);
					planGraphPanel.repaint();
				}
			}
		}
	}

	private void repaintOnPercentageByChange(PercentageBy percentageBy) {
		boolean bolShowFunctionName = false;
		JScrollPane scrollPane = null;
		
		if (bolShowFunctionName == true) { System.out.println("repaintOnPercentageByChange()"); }
		
		if (tabPane.getSelectedComponent() != null) {
			if (tabPane.getSelectedComponent() instanceof JScrollPane) {
				scrollPane = (JScrollPane) tabPane.getSelectedComponent();
			} else if (tabPane.getSelectedComponent() instanceof JSplitPane) {
				JSplitPane splitPane = (JSplitPane) tabPane.getSelectedComponent();
				if (splitPane.getLeftComponent() instanceof JScrollPane) {
					scrollPane = (JScrollPane) splitPane.getLeftComponent();
				} else if (splitPane.getLeftComponent() instanceof JSplitPane) {
					JSplitPane leftSplitPane = (JSplitPane) splitPane.getLeftComponent();
					if (leftSplitPane.getLeftComponent() instanceof JScrollPane) {
						scrollPane = (JScrollPane) leftSplitPane.getLeftComponent();
					}
				}
			}
			
			if (scrollPane != null) {
				if (scrollPane.getViewport().getComponent(0) instanceof PlanGraphPanel) {
					PlanGraphPanel planGraphPanel = (PlanGraphPanel) scrollPane.getViewport().getComponent(0);
					planGraphPanel.setPercentageBy(percentageBy);
					planGraphPanel.repaint();
				}
			}
		}
	}
	
	public String applicationVersion() {
		return APPLICATION_VERSION;
	}
	
	private void repaintOnLineThicknessByChange(LineThicknesses lineThickness) {
		boolean bolShowFunctionName = false;
		JScrollPane scrollPane = null;
		
		if (bolShowFunctionName == true) { System.out.println("repaintOnLineThicknessByChange()"); }
		
		if (tabPane.getSelectedComponent() != null) {
			if (tabPane.getSelectedComponent() instanceof JScrollPane) {
				scrollPane = (JScrollPane) tabPane.getSelectedComponent();
			} else if (tabPane.getSelectedComponent() instanceof JSplitPane) {
				JSplitPane splitPane = (JSplitPane) tabPane.getSelectedComponent();
				if (splitPane.getLeftComponent() instanceof JScrollPane) {
					scrollPane = (JScrollPane) splitPane.getLeftComponent();
				} else if (splitPane.getLeftComponent() instanceof JSplitPane) {
					JSplitPane leftSplitPane = (JSplitPane) splitPane.getLeftComponent();
					if (leftSplitPane.getLeftComponent() instanceof JScrollPane) {
						scrollPane = (JScrollPane) leftSplitPane.getLeftComponent();
					}
				}
			}
			
			if (scrollPane != null) {
				if (scrollPane.getViewport().getComponent(0) instanceof PlanGraphPanel) {
					PlanGraphPanel planGraphPanel = (PlanGraphPanel) scrollPane.getViewport().getComponent(0);
					planGraphPanel.setLineThicknessBy(lineThickness);
					planGraphPanel.repaint();
				}
			}
		}
	}
	
}
