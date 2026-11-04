package com.williammeitzen.peps.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
	
	private static final String APPLICATION_VERSION = "0.0.5";
	private PlanDiagramToolbar planDiagramToolbar;
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
		UiTheme.apply(UiTheme.loadSavedScheme());

		setLayout(new BorderLayout());
		
		planDiagramToolbar = new PlanDiagramToolbar();
		tabPane = new JTabbedPane();
		// Prefer full tab titles (no "...") — FlatLaf uses preferred width of each tab
		tabPane.putClientProperty("JTabbedPane.tabWidthMode", "preferred");
		// Dark scheme also enables card tabs + separators via UiTheme UIManager keys
		tabPane.putClientProperty("JTabbedPane.showTabSeparators", Boolean.TRUE);
		tabPane.putClientProperty("JTabbedPane.tabSeparatorsFullHeight", Boolean.TRUE);
		tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		formPanel = new FormPanel(tabPane, planDiagramToolbar);

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

		planDiagramToolbar.setZoomToolbarListener(new ZoomToolbarListener() {
			public void zoomPlusEventOccurred() {
				repaintOnZoomPlusOrMinus(10, true);
			}
			
			public void zoomMinusEventOccurred() {
				repaintOnZoomPlusOrMinus(-10, false);
			}

			public void zoom100EventOccurred() {
				repaintOnZoomChange(100, true);
			}

			public void zoomFitEventOccurred() {
				repaintOnZoomChange(-1, false);
			}
		});
		
		planDiagramToolbar.setLineThicknessListener(new LineThicknessByRadioButtonsToolbarListener() {
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

		planDiagramToolbar.setPercentageByListener(new PercentageByRadioButtonsToolbarListener() {
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

		// Toolbar lives inside the Plan Diagram tab content (see FormPanel.createPlanDiagramTab)
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
		JMenu colorSchemeMenu = new JMenu("Color scheme");
		viewMenu.add(colorSchemeMenu);
		ButtonGroup colorSchemeGroup = new ButtonGroup();
		JRadioButton defaultSchemeRB = new JRadioButton(UiTheme.Scheme.DEFAULT.label());
		JRadioButton lightSchemeRB = new JRadioButton(UiTheme.Scheme.LIGHT.label());
		JRadioButton darkSchemeRB = new JRadioButton(UiTheme.Scheme.DARK.label());
		colorSchemeGroup.add(defaultSchemeRB);
		colorSchemeGroup.add(lightSchemeRB);
		colorSchemeGroup.add(darkSchemeRB);
		colorSchemeMenu.add(defaultSchemeRB);
		colorSchemeMenu.add(lightSchemeRB);
		colorSchemeMenu.add(darkSchemeRB);

		UiTheme.Scheme savedScheme = UiTheme.loadSavedScheme();
		switch (savedScheme) {
		case LIGHT:
			lightSchemeRB.setSelected(true);
			break;
		case DARK:
			darkSchemeRB.setSelected(true);
			break;
		case DEFAULT:
		default:
			defaultSchemeRB.setSelected(true);
			break;
		}

		defaultSchemeRB.addActionListener(e -> UiTheme.applyAndRefresh(UiTheme.Scheme.DEFAULT, MainFrame.this));
		lightSchemeRB.addActionListener(e -> UiTheme.applyAndRefresh(UiTheme.Scheme.LIGHT, MainFrame.this));
		darkSchemeRB.addActionListener(e -> UiTheme.applyAndRefresh(UiTheme.Scheme.DARK, MainFrame.this));

		JMenu helpMenu = new JMenu("Help");
		JMenuItem helpMenuAboutItem = new JMenuItem("About ...");
		helpMenu.add(helpMenuAboutItem);

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
	
	private void repaintOnZoomChange(int zoomPct, boolean keepSelectedVisible) {
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel != null) {
			planGraphPanel.setZoomPercentage(zoomPct);
			planGraphPanel.refreshScrollableSizeAfterZoom(keepSelectedVisible);
		}
	}
	
	private void repaintOnZoomPlusOrMinus(int zoomPct, boolean keepSelectedVisible) {
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel != null) {
			planGraphPanel.setZoomPercentage(planGraphPanel.getZoomPercentage() + zoomPct);
			planGraphPanel.refreshScrollableSizeAfterZoom(keepSelectedVisible);
		}
	}

	private void repaintOnPercentageByChange(PercentageBy percentageBy) {
		if (planDiagramToolbar != null) {
			planDiagramToolbar.setSelectedPercentageBy(percentageBy);
		}
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel != null) {
			planGraphPanel.setPercentageBy(percentageBy);
			planGraphPanel.repaint();
		}
	}
	
	public String applicationVersion() {
		return APPLICATION_VERSION;
	}
	
	private void repaintOnLineThicknessByChange(LineThicknesses lineThickness) {
		if (planDiagramToolbar != null) {
			planDiagramToolbar.setSelectedLineThickness(lineThickness);
		}
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel != null) {
			planGraphPanel.setLineThicknessBy(lineThickness);
			planGraphPanel.repaint();
		}
	}

	/** Locate the Plan Diagram graph under the selected tab (unwraps toolbar wrapper panel). */
	private PlanGraphPanel findSelectedPlanGraphPanel() {
		Component selected = tabPane.getSelectedComponent();
		if (selected == null) {
			return null;
		}
		return findPlanGraphPanel(selected);
	}

	private static PlanGraphPanel findPlanGraphPanel(Component root) {
		if (root instanceof PlanGraphPanel) {
			return (PlanGraphPanel) root;
		}
		if (root instanceof JScrollPane) {
			Component view = ((JScrollPane) root).getViewport().getView();
			if (view instanceof PlanGraphPanel) {
				return (PlanGraphPanel) view;
			}
			return findPlanGraphPanel(view);
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				PlanGraphPanel found = findPlanGraphPanel(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
	
}
