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
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.net.URL;
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

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
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
	
	private static final String APPLICATION_VERSION = "1.0.0";
	private PlanDiagramToolbar planDiagramToolbar;
	private FormPanel formPanel;
	//private JFileChooser fileChooser;
	//private PrefsDialog prefsDialog;
	//private Preferences prefs;
	private DockingLayout dockingLayout;
	private JTabbedPane tabPane;
	private JMenu fileMenuForAlt;
	private JMenu viewMenuForAlt;
	private JMenu helpMenuForAlt;
	
	public MainFrame() {
		super("PostgreSQL Explain Plan Studio");

		boolean bolShowProperties = false;

		if (bolShowProperties == true) { System.out.println("MainFrame(): Database.init()"); }
		Database.init();
		PlanGraphPanel.loadPreferredZoomFromConfig();
		UiTheme.apply(UiTheme.loadSavedScheme());
		installApplicationIcons();

		setLayout(new BorderLayout());
		
		planDiagramToolbar = new PlanDiagramToolbar();
		tabPane = new JTabbedPane();
		// Prefer full tab titles (no "...") — FlatLaf uses preferred width of each tab
		tabPane.putClientProperty("JTabbedPane.tabWidthMode", "preferred");
		// Dark scheme also enables card tabs + separators via UiTheme UIManager keys
		tabPane.putClientProperty("JTabbedPane.showTabSeparators", Boolean.TRUE);
		tabPane.putClientProperty("JTabbedPane.tabSeparatorsFullHeight", Boolean.TRUE);
		tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		javax.swing.JPanel documentsHost = new javax.swing.JPanel(new java.awt.CardLayout());
		formPanel = new FormPanel(tabPane, planDiagramToolbar, documentsHost);
		DocumentsEmptyPanel emptyDocuments = new DocumentsEmptyPanel(formPanel);
		documentsHost.add(tabPane, FormPanel.DOCUMENTS_CARD_TABS);
		documentsHost.add(emptyDocuments, FormPanel.DOCUMENTS_CARD_EMPTY);
		formPanel.setDocumentsEmptyPanel(emptyDocuments);
		formPanel.refreshDocumentsEmptyState();
		dockingLayout = new DockingLayout(this, formPanel, documentsHost);
		
		setJMenuBar(createMenuBar()); // can't call set JMenuBar for historical / compatibility reasons
		UiTheme.styleMenuBar(getJMenuBar());
		installMenuAltShortcuts();
		
		addWindowListener(new WindowAdapter() {
 			public void windowClosing(WindowEvent e) {
 				// Write application dimensions to config, then exit the JVM.
 				// dispose() alone often leaves AWT / Docking Frames threads alive on macOS
 				// (Dock icon sticks; shell never returns to the prompt after java -jar).
 				Dimension dimApplicationSize = getSize();
 				Integer intWidth = dimApplicationSize.width;
 				Integer intHeight = dimApplicationSize.height;
 				Database.dml("update config set application_width = " + intWidth + ", application_height = " + intHeight);
 				if (formPanel != null) {
 					formPanel.saveSelectedPlanForExit();
 					formPanel.saveDiagramDockingLayoutForExit();
 				}
 				if (dockingLayout != null) {
 					dockingLayout.saveLayout();
 				}
 				HollowGuideArrowOverlay.dismiss();
 				dispose();
 				System.exit(0);
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

			public void zoomPercentEventOccurred(int zoomPercentage) {
				// Slider-driven: apply zoom only; do not rebuild slider max/Fit (that caused jerking).
				repaintOnZoomChangeFromSlider(zoomPercentage);
			}
		});
		planDiagramToolbar.setZoomSliderSyncHook(() -> {
			PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
			if (planGraphPanel != null) {
				planDiagramToolbar.refreshZoomSliderFromGraph(planGraphPanel);
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
		add(dockingLayout.getContentArea(), BorderLayout.CENTER);
		
		// Leave room for Plans + Documents shell mins + docking divider travel.
		setMinimumSize(new Dimension(640, 400));

		// get application dimensions from config table
		JsonObject jsnRow;
		String strSQL;
		strSQL = "select application_width, application_height from config;";
		jsnRow = Database.row(strSQL);
		setSize(jsnRow.getInteger("application_width"), jsnRow.getInteger("application_height")); // UNCOMMENT THIS

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}

	/** Open a top-level menu with Alt+mnemonic even when LAF only underlines the letter. */
	private void installMenuAltShortcuts() {
		bindAltMenuOpen(fileMenuForAlt, KeyEvent.VK_F);
		bindAltMenuOpen(viewMenuForAlt, KeyEvent.VK_V);
		bindAltMenuOpen(helpMenuForAlt, KeyEvent.VK_H);
	}

	private void bindAltMenuOpen(JMenu menu, int keyCode) {
		JRootPane root = getRootPane();
		if (root == null || menu == null) {
			return;
		}
		String actionKey = "peps.openMenu." + keyCode;
		KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, KeyEvent.ALT_DOWN_MASK);
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, actionKey);
		root.getActionMap().put(actionKey, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				menu.doClick();
			}
		});
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
		ButtonGroup colorSchemeGroup = new ButtonGroup();
		JRadioButtonMenuItem defaultSchemeItem = new JRadioButtonMenuItem(UiTheme.Scheme.DEFAULT.label());
		JRadioButtonMenuItem lightSchemeItem = new JRadioButtonMenuItem(UiTheme.Scheme.LIGHT.label());
		JRadioButtonMenuItem darkSchemeItem = new JRadioButtonMenuItem(UiTheme.Scheme.DARK.label());
		colorSchemeGroup.add(defaultSchemeItem);
		colorSchemeGroup.add(lightSchemeItem);
		colorSchemeGroup.add(darkSchemeItem);
		viewMenu.add(defaultSchemeItem);
		viewMenu.add(lightSchemeItem);
		viewMenu.add(darkSchemeItem);
		if (dockingLayout != null) {
			dockingLayout.installViewMenuItems(viewMenu);
			if (formPanel != null) {
				dockingLayout.setLayoutChangedHook(formPanel::repairDiagramDockingAfterThemeChange);
			}
		}
		if (formPanel != null) {
			formPanel.installDiagramPanelViewMenuItems(viewMenu);
		}
		viewMenu.addSeparator();
		JMenuItem resetPanelsItem = new JMenuItem("Reset Panels");
		resetPanelsItem.setToolTipText("Show all panels and restore the default layout");
		resetPanelsItem.addActionListener(e -> {
			if (dockingLayout != null) {
				dockingLayout.resetToDefaultLayout();
			}
			if (formPanel != null) {
				formPanel.resetPanelsToDefault();
			}
		});
		viewMenu.add(resetPanelsItem);

		UiTheme.Scheme savedScheme = UiTheme.loadSavedScheme();
		switch (savedScheme) {
		case LIGHT:
			lightSchemeItem.setSelected(true);
			break;
		case DARK:
			darkSchemeItem.setSelected(true);
			break;
		case DEFAULT:
		default:
			defaultSchemeItem.setSelected(true);
			break;
		}

		defaultSchemeItem.addActionListener(e -> applyColorScheme(UiTheme.Scheme.DEFAULT));
		lightSchemeItem.addActionListener(e -> applyColorScheme(UiTheme.Scheme.LIGHT));
		darkSchemeItem.addActionListener(e -> applyColorScheme(UiTheme.Scheme.DARK));

		JMenu helpMenu = new JMenu("Help");
		JMenuItem helpMenuAboutItem = new JMenuItem("About ...");
		helpMenu.add(helpMenuAboutItem);

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);

		fileMenu.setMnemonic(KeyEvent.VK_F);
		viewMenu.setMnemonic(KeyEvent.VK_V);
		helpMenu.setMnemonic(KeyEvent.VK_H);
		exitItem.setMnemonic(KeyEvent.VK_X);

		// Bound after setJMenuBar() so the root pane InputMap is ready (see installMenuAltShortcuts).
		this.fileMenuForAlt = fileMenu;
		this.viewMenuForAlt = viewMenu;
		this.helpMenuForAlt = helpMenu;
		
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

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
			    // Soft sky on Dark; stronger blue on Light/Default for contrast.
			    Color linkColor = UiTheme.isDark()
			    		? new Color(0x93, 0xC5, 0xFD)
			    		: new Color(0x25, 0x63, 0xEB);
			    JLabel downloadLink = new JLabel("At " + strDownloadURL);
			    downloadLink.setForeground(linkColor);
			    downloadLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			    b.add(downloadLink);
			    JLabel supportLink = new JLabel("Support: " + strSupportURL);
			    supportLink.setForeground(linkColor);
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
	
	private void applyColorScheme(UiTheme.Scheme scheme) {
		UiTheme.applyAndRefresh(scheme, MainFrame.this);
		// Linux/VMware: updateComponentTreeUI can desync Docking Frames visibility vs View menu.
		if (formPanel != null) {
			formPanel.repairDiagramDockingAfterThemeChange();
			formPanel.refreshDocumentsEmptyState();
		}
		UiTheme.styleMenuBar(getJMenuBar());
	}

	/** Window / taskbar icon — plan-diagram mark from {@code /images/app-icon.png}. */
	private void installApplicationIcons() {
		URL url = getClass().getResource("/images/app-icon.png");
		if (url == null) {
			return;
		}
		try {
			Image base = ImageIO.read(url);
			if (base == null) {
				return;
			}
			List<Image> icons = new ArrayList<>();
			for (int size : new int[] { 16, 32, 48, 64, 128, 256 }) {
				icons.add(base.getScaledInstance(size, size, Image.SCALE_SMOOTH));
			}
			setIconImages(icons);
		} catch (IOException ignored) {
			// keep default window icon
		}
	}

	private void repaintOnZoomChange(int zoomPct, boolean keepSelectedVisible) {
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel != null) {
			planGraphPanel.setZoomPercentage(zoomPct);
			planGraphPanel.refreshScrollableSizeAfterZoom(keepSelectedVisible);
			if (planDiagramToolbar != null) {
				planDiagramToolbar.syncZoomPercentage(planGraphPanel.getZoomPercentage());
			}
		}
	}

	/** Slider path: update zoom without re-entering the slider change listener. */
	private void repaintOnZoomChangeFromSlider(int zoomPct) {
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel == null) {
			return;
		}
		if (planGraphPanel.getZoomPercentage() == zoomPct) {
			return;
		}
		planGraphPanel.setZoomPercentage(zoomPct);
		planGraphPanel.refreshScrollableSizeAfterZoom(true);
	}
	
	private void repaintOnZoomPlusOrMinus(int zoomPct, boolean keepSelectedVisible) {
		PlanGraphPanel planGraphPanel = findSelectedPlanGraphPanel();
		if (planGraphPanel != null) {
			planGraphPanel.setZoomPercentage(planGraphPanel.getZoomPercentage() + zoomPct);
			planGraphPanel.refreshScrollableSizeAfterZoom(keepSelectedVisible);
			if (planDiagramToolbar != null) {
				planDiagramToolbar.syncZoomPercentage(planGraphPanel.getZoomPercentage());
			}
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
