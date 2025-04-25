package mirfeeLiveClassifier;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.table.*;

import PamController.PamControlledUnit;

import javax.swing.filechooser.*;
import java.util.*;
import java.text.*;

import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamBorderPanel;
import PamguardMVC.PamDataBlock;
import pamScrollSystem.AbstractPamScroller;
import pamScrollSystem.AbstractScrollManager;
import pamScrollSystem.ViewerScrollerManager;
import wmat.WMATAnnotationInfo;
import wmat.WMATControl;
import wmat.WMATDataUnit;
import PamView.PamColors;
import PamView.PamTable;

/**
 * The panel where the GUI components are written.
 * @author Holly LeBlond
 */
public class LCPanel extends PamBorderPanel {
	
	protected LCControl control;
	protected PamBorderPanel mainPanel;
	protected PamBorderPanel memPanel;
	protected volatile PamBorderPanel topLeftPanel;
	protected PamBorderPanel topRightPanel;
	protected CardLayout cl;
	protected JPanel cmCardsPanel;
	protected PamBorderPanel bottomLeftPanel;
	protected PamBorderPanel bottomRightPanel;
	
	protected DefaultTableModel dtm;
	protected PamTable resultsTable;
	protected JTextArea resultsTA;
	protected HashSet<String> tableHashSet;
	
	protected volatile boolean running;
	protected volatile boolean waiting;
	protected volatile boolean initWorked;
	protected volatile int numberOfSubsets;
	
	protected volatile int[][] accuracyMatrix;
	protected volatile JLabel[][] jLabelAccuracyMatrix;
	
	protected final Object confMatrixKey = 0;
	protected volatile int[][] confMatrix;
	protected volatile JLabel[][] jLabelConfMatrix;
	
	protected volatile String currSuperClusterID;
	protected volatile ArrayList<LCCallCluster> ccList;
	
	protected volatile HashMap<String, String> uidToClusterMap;
	
	protected JButton scrollButton;
	protected JButton bestFeaturesButton;
	protected JButton exportButton;
	//protected JButton wmntButton;
	
	protected volatile LCLoadingBarWindow loadingBarWindow;
	protected volatile LoadingBarThread loadingBarThread;
	
	protected volatile LCWaitingDialogThread wdThread;
	
	public LCPanel(LCControl control, boolean isViewer) {
		this.control = control;
		this.running = false;
		this.waiting = false;
		this.initWorked = false;
		
		this.currSuperClusterID = "0A-0000"; // ??????
		this.ccList = new ArrayList<LCCallCluster>();
		
		this.uidToClusterMap = new HashMap<String, String>();
		
		this.tableHashSet = new HashSet<String>();
		
		this.setLayout(new BorderLayout());
		
		mainPanel = new PamBorderPanel();
		mainPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		memPanel = new PamBorderPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		
		topLeftPanel = new PamBorderPanel(new GridBagLayout());
		topLeftPanel.setBorder(new TitledBorder("Results"));
		GridBagConstraints c = new PamGridBagContraints();
		c.anchor = c.NORTH;
		c.fill = c.NONE;
		if (!isViewer) {
			String[] columnNames = {"Cluster", "Date/Time (UTC)", "n", "Predicted species", "Pr. counter", "Pr. proba.", "Certainty"};
			dtm = new DefaultTableModel(columnNames,0) {
				Class[] types = {String.class, String.class, Integer.class, String.class, String.class, String.class, String.class};
				boolean[] canEdit = {false, false, false, false, false, false, false};
				
				@Override
				public Class getColumnClass(int index) {
					return this.types[index];
				}
				
				@Override
				public boolean isCellEditable(int row, int column) {
					return this.canEdit[column];
				}
			};
			resultsTable = new PamTable(dtm);
			resultsTable.setDefaultRenderer(String.class, new CustomTableRenderer(control));
			resultsTable.getTableHeader().setReorderingAllowed(false);
			resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			resultsTable.setAutoCreateRowSorter(true);
			resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
			resultsTable.getColumnModel().getColumn(0).setPreferredWidth(75);
			resultsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
			resultsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
			resultsTable.getColumnModel().getColumn(3).setPreferredWidth(125);
			resultsTable.getColumnModel().getColumn(4).setPreferredWidth(125);
			resultsTable.getColumnModel().getColumn(5).setPreferredWidth(125);
			resultsTable.getColumnModel().getColumn(6).setPreferredWidth(100);
		} else {
			String[] columnNames = {"Cluster", "Date/Time (UTC)", "n", "Pr. species", "Pr. counter", "Pr. proba.", "Certainty", "Ac. species"};
			dtm = new DefaultTableModel(columnNames,0) {
				Class[] types = {String.class, String.class, Integer.class, String.class, String.class, String.class, String.class, String.class};
				boolean[] canEdit = {false, false, false, false, false, false, false, false};
				
				@Override
				public Class getColumnClass(int index) {
					return this.types[index];
				}
				
				@Override
				public boolean isCellEditable(int row, int column) {
					return this.canEdit[column];
				}
			};
			resultsTable = new PamTable(dtm);
			resultsTable.setDefaultRenderer(String.class, new CustomTableRenderer(control));
			resultsTable.getTableHeader().setReorderingAllowed(false);
			resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			resultsTable.setAutoCreateRowSorter(true);
			resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
			resultsTable.getColumnModel().getColumn(0).setPreferredWidth(75);
			resultsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
			resultsTable.getColumnModel().getColumn(2).setPreferredWidth(35);
			resultsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
			resultsTable.getColumnModel().getColumn(4).setPreferredWidth(125);
			resultsTable.getColumnModel().getColumn(5).setPreferredWidth(125);
			resultsTable.getColumnModel().getColumn(6).setPreferredWidth(100);
			resultsTable.getColumnModel().getColumn(7).setPreferredWidth(70);
		}
		TableRowSorter<TableModel> trs = new TableRowSorter<TableModel>(resultsTable.getModel());
		trs.setSortable(4, false);
		if (isViewer)
			trs.toggleSortOrder(0);
		resultsTable.setRowSorter(trs);
		resultsTable.getSelectionModel().addListSelectionListener(new ResultsSelectionListener());
		//subsetTable.setSize(550, 450);
		JScrollPane sp = new JScrollPane(resultsTable);
		//sp.setPreferredSize(new Dimension(950, 650));
		sp.setPreferredSize(new Dimension(820, 400));
		topLeftPanel.add(sp);
		b.anchor = b.NORTHWEST;
		b.fill = b.NONE;
		memPanel.add(topLeftPanel, b);
		
		topRightPanel = new PamBorderPanel(new GridBagLayout());
		topRightPanel.setBorder(new TitledBorder("Matrices"));
		cmCardsPanel = new JPanel(new CardLayout());
		cl = (CardLayout) cmCardsPanel.getLayout();
		JPanel nothingPanel = new JPanel();
		// TODO add something to fill space
		cmCardsPanel.add(nothingPanel, "Nothing");
		topRightPanel.add(cmCardsPanel);
		cl.show(cmCardsPanel, "Nothing");
		b.gridy = 0;
		b.gridx = 1;
		b.anchor = b.NORTHEAST;
		b.fill = b.BOTH;
		memPanel.add(topRightPanel, b);
		
		bottomLeftPanel = new PamBorderPanel(new GridBagLayout());
		bottomLeftPanel.setBorder(new TitledBorder("Individual results"));
		resultsTA = new JTextArea();
		resultsTA.setEditable(false);
		// TODO font, etc.
		JScrollPane sp2 = new JScrollPane(resultsTA);
		sp2.setPreferredSize(new Dimension(800, 200));
		bottomLeftPanel.add(sp2);
		b.gridy = 1;
		b.gridx = 0;
		//b.gridwidth = 2;
		b.fill = b.HORIZONTAL;
		b.anchor = b.NORTH;
		memPanel.add(bottomLeftPanel, b);
		
		bottomRightPanel = new PamBorderPanel(new GridBagLayout());
		bottomRightPanel.setBorder(new TitledBorder("Actions"));
		c = new PamGridBagContraints();
		c.fill = c.HORIZONTAL;
		c.anchor = c.NORTH;
		scrollButton = new JButton("Scroll to selection on spectrogram");
		scrollButton.addActionListener(new ScrollButtonListener());
		bottomRightPanel.add(scrollButton, c);
		c.gridy++;
		bestFeaturesButton = new JButton("Feature usefulness analysis");
		bestFeaturesButton.addActionListener(new BestFeaturesButtonListener());
		bottomRightPanel.add(bestFeaturesButton, c);
		c.gridy++;
		exportButton = new JButton("Export results");
		exportButton.addActionListener(new ExportButtonListener());
		bottomRightPanel.add(exportButton, c);
		//c.gridy++;
		//wmntButton = new JButton("Export predictions to WMNT");
		//wmntButton.addActionListener(new WMNTButtonListener());
		//bottomRightPanel.add(wmntButton, c);
		b.gridx++;
		b.fill = b.BOTH;
		memPanel.add(bottomRightPanel, b);
		
		mainPanel.add(memPanel);
		this.add(mainPanel);
	}
	
	protected class BestFeaturesButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			BestFeaturesButtonThread bfbThread = new BestFeaturesButtonThread();
			bfbThread.start();
		}
	}
	
	protected class BestFeaturesButtonThread extends Thread {
		protected BestFeaturesButtonThread() {}
		@Override
		public void run() {
			bestFeaturesButtonAction();
		}
	}
	
	/**
	 * Sends a command to the Python interpreter, which returns a sorted list of scores that attempt
	 * to quantify how "useful" each feature is, which are then shown in a dialog.
	 */
	protected void bestFeaturesButtonAction() {
		if (control.isViewer()) {
			control.simpleErrorDialog("This function is only available in regular processing mode.", 250);
			return;
		}
		if (!control.isTrainingSetLoaded()) {
			control.simpleErrorDialog("No training set has been loaded yet.", 250);
			return;
		}
		wdThread = new LCWaitingDialogThread(control.getGuiFrame(), control, "Waiting for response from Python script...");
		wdThread.start();
		control.getThreadManager().pythonCommand("modelManager.printBestFeatureOrder()", control.getParams().printInput);
	}
	
	/**
	 * The listener for the 'Scroll to selection on spectrogram' button.
	 */
	class ScrollButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!control.isViewer()) {
				control.simpleErrorDialog("This function in only available in viewer mode.", 250);
				return;
			}
			if (resultsTable.getRowCount() > 0) {
				if (resultsTable.getSelectedRow() > -1) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					String fromTable = resultsTable.getValueAt(resultsTable.getSelectedRow(), 1).toString();
					
					try {
						Date date = df.parse(fromTable);
						long outpTime = date.getTime();
						ViewerScrollerManager vsm = (ViewerScrollerManager) AbstractScrollManager.getScrollManager();
						for (int i = 0; i < vsm.getPamScrollers().size(); i++) {
							AbstractPamScroller scroller = vsm.getPamScrollers().get(i);
							long duration = scroller.getMaximumMillis() - scroller.getMinimumMillis();
							scroller.anotherScrollerMovedOuter(outpTime-1000, outpTime+duration-1000);
						}
						vsm.loadData(true);
					} catch (ParseException e1) {
						e1.printStackTrace();
						control.simpleErrorDialog("Error parsing date from table.", 250);
					}
					return;
				}
			}
			JOptionPane.showMessageDialog(control.getGuiFrame(),
					"No contour has been selected from the table.",
				    "MIRFEE Live Classifier",
				    JOptionPane.ERROR_MESSAGE);
		}
	}
	
	class ExportButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			exportButtonListenerAction();
		}
	}
	
	protected void exportButtonListenerAction() {
		LCExportDialog exportDialog = new LCExportDialog(control, control.getPamView().getGuiFrame());
		exportDialog.setVisible(true);
	}
	
	class WMATButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			wmatButtonAction();
		}
	}
	
	@Deprecated
	protected void wmatButtonAction() {
		// DELETE THIS LOOP AFTER TESTING
	/*	for (int i = 0; i < control.getPamController().getDataBlocks().size(); i++) {
			PamDataBlock db = control.getPamController().getDataBlocks().get(i);
			System.out.println(db.getLongDataName()+" -> "+String.valueOf(db.getNumOfflineDataMaps()));
		} */
		if (!control.isViewer()) {
			control.simpleErrorDialog("This feature is only available in viewer mode.", 250);
			return;
		}
		WMATControl wmatControl = null;
		for (int i = 0; i < control.getPamController().getNumControlledUnits(); i++) {
			PamControlledUnit pcu = control.getPamController().getControlledUnit(i);
			if (pcu.getUnitType().equals("WMAT")) {
				wmatControl = (WMATControl) pcu;
				break;
			}
		}
		if (wmatControl == null) {
			control.simpleErrorDialog("No instance of the Whistle and Moan Annotation Tool has been added to this configuration. To add one, go to "
					+ "File -> Add Modules -> Utilities.", 300);
			return;
		}
		LCWMATExportDialog dialog = new LCWMATExportDialog(control, wmatControl, control.getGuiFrame());
		dialog.setVisible(true);
	}
	
	/**
	 * Displays individual contour results in the space below the table when table rows are selected.
	 */
	protected class ResultsSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			resultsTA.setText("");
			int[] selectedRows = resultsTable.getSelectedRows();
	        if (selectedRows.length == 0) {
	        	// TODO add message
	        	return;
	        }
	        ArrayList<String> rowList = new ArrayList<String>();
	        for (int i = 0; i < selectedRows.length; i++) {
	        	rowList.add(((String) resultsTable.getValueAt(selectedRows[i], 0))+", "+((String) resultsTable.getValueAt(selectedRows[i], 1)));
	        }
	        LCDataBlock db = (LCDataBlock) control.getProcess().getOutputDataBlock(0);
	        HashMap<String, LCDataUnit> unitMap = db.retrieveDataUnitsByIDandDate(rowList);
	        String outp = "";
	        for (int i = 0; i < selectedRows.length; i++) {
	        	String key = ((String) resultsTable.getValueAt(selectedRows[i], 0))+", "+((String) resultsTable.getValueAt(selectedRows[i], 1));
	        	if (!unitMap.containsKey(key)) {
	        		continue;
	        	}
	        	LCDataUnit du = unitMap.get(key);
	        	LCCallCluster cc = du.getCluster();
	        	for (int j = 0; j < cc.getSize(); j++) {
	        		if (j == 0) {
	        			outp += cc.clusterID+"\t";
	        		} else {
	        			outp += "\t\t";
	        		}
	        		outp += String.valueOf(cc.uids[j])+"\t";
	        		if (cc.uids[j] < 10000000) {
	        			outp += "\t";
	        		}
	        		outp += control.convertDateLongToString(control.convertFromLocalToUTC(cc.datetimes[j]))+"\t\t";
	        		outp += "[";
	        		for (int k = 0; k < cc.labelList.size(); k++) {
	        			if (k != 0) {
	        				outp += ", ";
	        			}
	        			outp += String.format("%.2f", cc.probaList[j][k]);
	        		}
	        		outp += "]\t";
	        		outp += String.format("%.2f", cc.getIndividualCertainty(j))+"\t";
	        		if (cc.actualSpecies[j] == -1) {
	        			outp += "Unlabelled";
	        		} else if (cc.actualSpecies[j] == -2) {
	        			outp += "Other";
	        		} else {
	        			outp += cc.labelList.get(cc.actualSpecies[j]);
	        		}
	        		outp += " -> "+cc.labelList.get(cc.getPredictedSpeciesArray()[j]);
	        		outp += "\n";
	        	}
	        }
	        // TODO add more stuff ?????
	        resultsTA.setText(outp);
	    }
	}
	
	/**
	 * Creates the accuracy matrix and, in Viewer mode, the confusion matrix.
	 */
	public void createMatrices(String[] labelOrder, boolean resetConfusionMatrixOnly) {
		if (!resetConfusionMatrixOnly || accuracyMatrix == null) {
			accuracyMatrix = new int[labelOrder.length][5];
			jLabelAccuracyMatrix = new JLabel[labelOrder.length+2][7];
			jLabelAccuracyMatrix[0][0] = new JLabel("");
			jLabelAccuracyMatrix[0][1] = new JLabel("VL");
			jLabelAccuracyMatrix[0][1].setForeground(new Color(240,0,0));
			jLabelAccuracyMatrix[0][2] = new JLabel("L");
			jLabelAccuracyMatrix[0][2].setForeground(new Color(240,0,0));
			jLabelAccuracyMatrix[0][3] = new JLabel("A");
			jLabelAccuracyMatrix[0][4] = new JLabel("H");
			jLabelAccuracyMatrix[0][4].setForeground(new Color(0,205,0));
			jLabelAccuracyMatrix[0][5] = new JLabel("VH");
			jLabelAccuracyMatrix[0][5].setForeground(new Color(0,205,0));
			jLabelAccuracyMatrix[0][6] = new JLabel("");
			for (int i = 0; i < accuracyMatrix.length; i++) {
				jLabelAccuracyMatrix[i+1][0] = new JLabel(labelOrder[i]);
				jLabelAccuracyMatrix[i+1][6] = new JLabel("0.0%");
				for (int j = 0; j < accuracyMatrix[i].length; j++) {
					accuracyMatrix[i][j] = 0;
					jLabelAccuracyMatrix[i+1][j+1] = new JLabel("0");
					if (j < 2) {
						jLabelAccuracyMatrix[i+1][j+1].setForeground(new Color(240,0,0));
					} else if (j > 2) {
						jLabelAccuracyMatrix[i+1][j+1].setForeground(new Color(0,205,0));
					}
				}
			}
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][0] = new JLabel("");
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][1] = new JLabel("0.0%");
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][1].setForeground(new Color(240,0,0));
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][2] = new JLabel("0.0%");
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][2].setForeground(new Color(240,0,0));
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][3] = new JLabel("0.0%");
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][4] = new JLabel("0.0%");
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][4].setForeground(new Color(0,205,0));
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][5] = new JLabel("0.0%");
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][5].setForeground(new Color(0,205,0));
			jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][6] = new JLabel("0");
		}
		
		synchronized(confMatrixKey) {
			confMatrix = new int[labelOrder.length+2][labelOrder.length];
			jLabelConfMatrix = new JLabel[labelOrder.length+4][labelOrder.length+2];
			for (int i = 0; i < jLabelConfMatrix.length; i++) {
				for (int j = 0; j < jLabelConfMatrix[i].length; j++) {
					if (i == 0) {
						if (j == 0) {
							jLabelConfMatrix[i][j] = new JLabel("");
						} else if (j == labelOrder.length+1) {
							jLabelConfMatrix[i][j] = new JLabel("Recall");
						} else {
							jLabelConfMatrix[i][j] = new JLabel(labelOrder[j-1]);
						}
					} else if (i == labelOrder.length+1) {
						if (j == 0) {
							jLabelConfMatrix[i][j] = new JLabel("Prcsn.");
						} else {
							jLabelConfMatrix[i][j] = new JLabel("-%");
						}
					} else {
						if (i < labelOrder.length+1 && j == 0) {
							jLabelConfMatrix[i][j] = new JLabel(labelOrder[i-1]);
						} else if (i < labelOrder.length+1 && j == labelOrder.length+1) {
							jLabelConfMatrix[i][j] = new JLabel("-%");
						} else if (i == labelOrder.length+2 && j == 0) {
							jLabelConfMatrix[i][j] = new JLabel("Oth.");
						} else if (i == labelOrder.length+2 && j == labelOrder.length+1) {
							jLabelConfMatrix[i][j] = new JLabel("");
						} else if (i == labelOrder.length+3 && j == 0) {
							jLabelConfMatrix[i][j] = new JLabel("Unl.");
						} else if (i == labelOrder.length+3 && j == labelOrder.length+1) {
							jLabelConfMatrix[i][j] = new JLabel("");
						} else {
							jLabelConfMatrix[i][j] = new JLabel("0");
							if (i < labelOrder.length+1) {
								confMatrix[i-1][j-1] = 0;
							} else {
								confMatrix[i-2][j-1] = 0;
							}
							if (i-1 == j-1) {
								jLabelConfMatrix[i][j].setForeground(Color.GREEN);
							} else if (i < labelOrder.length+1) {
								jLabelConfMatrix[i][j].setForeground(Color.RED);
							}
						}
					}
				}
			}
			showMatrices();
		} // synchronized ends here
	}
	
	/**
	 * Displays the matrices in the upper right panel.
	 */
	protected void showMatrices() {
		JPanel matricesPanel = new JPanel(new GridLayout(2, 1, 10, 50));
		JPanel amGridPanel = new JPanel(new GridLayout(jLabelAccuracyMatrix.length, jLabelAccuracyMatrix[0].length, 10, 10));
		for (int i = 0; i < jLabelAccuracyMatrix.length; i++) {
			for (int j = 0; j < jLabelAccuracyMatrix[i].length; j++) {
				amGridPanel.add(jLabelAccuracyMatrix[i][j]);
			}
		}
		if (control.isViewer()) {
			JPanel cmGridPanel = new JPanel(new GridLayout(jLabelConfMatrix.length, jLabelConfMatrix[0].length, 10, 10));
			for (int i = 0; i < jLabelConfMatrix.length; i++) {
				for (int j = 0; j < jLabelConfMatrix[i].length; j++) {
					cmGridPanel.add(jLabelConfMatrix[i][j]);
				}
			}
			matricesPanel.add(cmGridPanel);
		}
		matricesPanel.add(amGridPanel);
		cmCardsPanel.add(matricesPanel, "matricesPanel");
		cl.show(cmCardsPanel, "matricesPanel");
	}
	
	/**
	 * Adds a row to the table displaying results found in the input data unit.
	 */
	public void addResultToTable(LCDataUnit du) {
		LCCallCluster cc = du.getCluster();
		
		// This is here because using the scroll button for the first time duplicates every entry in the table for some reason.
		String idString = cc.clusterID+", "+LCControl.convertDateLongToString(cc.getStartAndEnd(false)[0]);
		if (tableHashSet.contains(idString))
			return;
		tableHashSet.add(idString);
	/*	for (int i = 0; i < dtm.getRowCount(); i++) {
			if (cc.clusterID.equals((String) dtm.getValueAt(i, 0)) 
					&& LCControl.convertDateLongToString(cc.getStartAndEnd(false)[0]).equals((String) dtm.getValueAt(i, 1))) {
				return;
			}
		} */
		
		Object[] newRow = new Object[7];
		if (dtm.getColumnCount() >= 8) {
			newRow = new Object[8];
		}
		newRow[0] = cc.clusterID;
		// NOTE that the time zone is converted from local to UTC in the getStartAndEnd function with the false boolean.
		newRow[1] = control.convertDateLongToString(cc.getStartAndEnd(false)[0]);
		newRow[2] = cc.getSize();
		double[] probaAvgs = new double[cc.labelList.size()];
		for (int i = 0; i < probaAvgs.length; i++) {
			probaAvgs[i] = 0.0;
			for (int j = 0; j < cc.getSize(); j++) {
				probaAvgs[i] += cc.probaList[j][i];
			}
			probaAvgs[i] /= cc.getSize();
		}
		int predictionIndex = 0;
		for (int i = 1; i < probaAvgs.length; i++) {
			if (probaAvgs[i] > probaAvgs[predictionIndex]) {
				predictionIndex = i;
			}
		}
		newRow[3] = cc.labelList.get(predictionIndex);
		int[] speciesCount = new int[cc.labelList.size()];
		for (int i = 0; i < speciesCount.length; i++) {
			speciesCount[i] = 0;
		}
		for (int i = 0; i < cc.probaList.length; i++) {
			predictionIndex = 0;
			for (int j = 1; j < cc.probaList[i].length; j++) {
				if (cc.probaList[i][j] > cc.probaList[i][predictionIndex]) {
					predictionIndex = j;
				}
			}
			speciesCount[predictionIndex]++;
		}
		newRow[4] = cc.getColumn4String(dtm.getColumnCount() >= 8);
		newRow[5] = cc.getAverageProbaAsString();
	/*	double first = probaAvgs[0];
		double second = 0.0;
		for (int i = 1; i < probaAvgs.length; i++) {
			if (probaAvgs[i] > first) {
				second = first;
				first = probaAvgs[i];
			} else if (probaAvgs[i] > second) {
				second = probaAvgs[i];
			}
		} */
		double certainty = cc.getCertainty();
		int descIndex = 0;
		newRow[6] = String.format("%.2f", certainty)+" (";
		if (certainty < control.getParams().veryLow) {
			newRow[6] += "Very low)";
		} else if (certainty < control.getParams().low) {
			newRow[6] += "Low)";
			descIndex = 1;
		} else if (certainty < control.getParams().average) {
			newRow[6] += "Average)";
			descIndex = 2;
		} else if (certainty < control.getParams().high) {
			newRow[6] += "High)";
			descIndex = 3;
		} else {
			newRow[6] += "Very high)";
			descIndex = 4;
		}
		ArrayList<String> labelList = new ArrayList<String>();
		for (int i = 0; i < control.getParams().labelOrder.length; i++) {
			labelList.add(control.getParams().labelOrder[i]);
		}
		//if (descIndex >= descriptors.indexOf(control.getParams().worstCertainty) && cc.getSize() >= control.getParams().minClusterSize) {
		if (cc.getSize() >= control.getParams().minClusterSize) {
			if (accuracyMatrix == null) {
				createMatrices(control.getParams().labelOrder, false);
			}
			
			//int actualIndex = labelList.indexOf(cc.getActualSpecies(false));
			int predictedIndex = labelList.indexOf(cc.getPredictedSpeciesString());
			if (predictedIndex > -1) {
				accuracyMatrix[predictedIndex][descIndex]++;
				jLabelAccuracyMatrix[predictedIndex+1][descIndex+1].setText(String.valueOf(accuracyMatrix[predictedIndex][descIndex]));
				int matrixSum = 0;
				int[] sums1 = new int[accuracyMatrix.length];
				int[] sums2 = new int[accuracyMatrix[0].length];
				for (int i = 0; i < sums1.length; i++) {
					sums1[i] = 0;
				}
				for (int i = 0; i < sums2.length; i++) {
					sums2[i] = 0;
				}
				for (int i = 0; i < accuracyMatrix.length; i++) {
					for (int j = 0; j < accuracyMatrix[i].length; j++) {
						matrixSum += accuracyMatrix[i][j];
						sums1[i] += accuracyMatrix[i][j];
						sums2[j] += accuracyMatrix[i][j];
					}
				}
				for (int i = 0; i < sums1.length; i++) {
					jLabelAccuracyMatrix[i+1][6].setText(String.format("%.1f", 100*((double) sums1[i])/matrixSum)+"%");
				}
				for (int i = 0; i < sums2.length; i++) {
					jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][i+1].setText(String.format("%.1f", 100*((double) sums2[i])/matrixSum)+"%");
				}
				jLabelAccuracyMatrix[jLabelAccuracyMatrix.length-1][6].setText(String.valueOf(matrixSum));
			}
		}
		if (dtm.getColumnCount() >= 8) {
			newRow[7] = cc.getActualSpeciesString();
		}
		for (int i = 0; i < cc.getSize(); i++) {
			uidToClusterMap.put(String.valueOf(cc.uids[i])+", "+control.convertDateLongToString(control.convertFromLocalToUTC(cc.datetimes[i])),
					cc.clusterID+", "+control.convertDateLongToString(cc.getStartAndEnd(false)[0]));
		}
		if (!control.getParams().shouldIgnoreCluster(cc) || control.getParams().displayIgnored)
			dtm.addRow(newRow);
		if (!control.getParams().shouldIgnoreCluster(cc)) {
			if (dtm.getColumnCount() >= 8) {
				String actualSpecies = cc.getActualSpeciesString();
				if (actualSpecies.endsWith(" *")) {
					actualSpecies = actualSpecies.substring(0, actualSpecies.length()-2);
				}
				String predictedSpecies = cc.getPredictedSpeciesString();
				if (labelList.contains(predictedSpecies)) {
					if (labelList.indexOf(actualSpecies) != -1) {
						confMatrix[labelList.indexOf(actualSpecies)][labelList.indexOf(predictedSpecies)]++;
					} else if (actualSpecies.equals("Other")) {
						confMatrix[confMatrix.length-2][labelList.indexOf(predictedSpecies)]++;
					} else if (actualSpecies.equals("Unlabelled")) {
						confMatrix[confMatrix.length-1][labelList.indexOf(predictedSpecies)]++;
					}
					updateConfMatrixLabels();
				}
			}
		}
	}
	
	/**
	 * If desired, adds a row to a table if an error occurred while attempting to process a cluster.
	 * @param id - The cluster's ID.
	 * @param datetime - The cluster's date/time.
	 * @param n - Number of detections in the cluster.
	 */
	public void addErrorToTable(String id, String datetime, int n) {
		Object[] newRow = new Object[dtm.getColumnCount()];
		newRow[0] = id;
		newRow[1] = datetime.substring(1, datetime.length()-1);
		newRow[2] = n;
		newRow[3] = "ERROR";
		newRow[4] = "(cluster present in training set)";
		newRow[5] = "?";
		newRow[6] = "?";
		if (dtm.getColumnCount() > 7) newRow[7] = "?";
		dtm.addRow(newRow);
	}
	
	/**
	 * Updates the confusion matrix in the upper right panel for when results are added to the table.
	 */
	public void updateConfMatrixLabels() {
		synchronized(confMatrixKey) {
			// fills in digits and updates row percentages
			for (int i = 0; i < confMatrix.length; i++) {
				int sum = 0;
				for (int j = 0; j < confMatrix[i].length; j++) {
					if (confMatrix[i][j] < 0) {
						confMatrix[i][j] = 0;
					}
					//System.out.println(String.valueOf(i)+", "+String.valueOf(j));
					if (i < confMatrix[i].length) {
						jLabelConfMatrix[i+1][j+1].setText(String.valueOf(confMatrix[i][j]));
						sum += confMatrix[i][j];
					} else {
						jLabelConfMatrix[i+2][j+1].setText(String.valueOf(confMatrix[i][j]));
					}
				}
				if (i < confMatrix[i].length) {
					if (sum > 0) {
						jLabelConfMatrix[i+1][jLabelConfMatrix[i+1].length-1].setText(String.format("%.1f", 100*((double) confMatrix[i][i])/sum)+"%");
					} else {
						jLabelConfMatrix[i+1][jLabelConfMatrix[i+1].length-1].setText("-%");
					}
				}
			}
			// updates column percentages
			for (int j = 0; j < confMatrix[0].length; j++) {
				int sum = 0;
				for (int i = 0; i < confMatrix[0].length; i++) {
					sum += confMatrix[i][j];
				}
				if (sum > 0) {
					jLabelConfMatrix[confMatrix[0].length+1][j+1].setText(String.format("%.1f", 100*((double) confMatrix[j][j])/sum)+"%");
				} else {
					jLabelConfMatrix[confMatrix[0].length+1][j+1].setText("-%");
				}
			}
			// updates total percentage
			int totalSum = 0;
			int correctSum = 0;
			for (int i = 0; i < confMatrix[0].length; i++) {
				for (int j = 0; j < confMatrix[0].length; j++) {
					totalSum += confMatrix[i][j];
					if (i == j) {
						correctSum += confMatrix[i][j];
					}
				}
			}
			if (correctSum > 0) {
				jLabelConfMatrix[confMatrix[0].length+1][confMatrix[0].length+1].setText(String.format("%.1f", 100*((double) correctSum)/totalSum)+"%");
			} else {
				jLabelConfMatrix[confMatrix[0].length+1][confMatrix[0].length+1].setText("-%");
			}
		} // synchronized ends here
	}
	
	/**
	 * Re-loads all data unit info into the table.
	 */
	private void fullTableUpdate() {
		LCDataBlock db = (LCDataBlock) control.getProcess().getOutputDataBlock(0);
		for (int i = 0; i < confMatrix.length; i++) {
			for (int j = 0; j < confMatrix[i].length; j++) {
				confMatrix[i][j] = 0;
			}
		}
		// Update accuracy matrix?
		for (int i = 0; i < db.getUnitsCount(); i++) {
			LCDataUnit du = db.getDataUnit(i, db.REFERENCE_CURRENT);
			LCCallCluster cc = du.getCluster();
			String actualSpecies = cc.getActualSpeciesString();
			if (actualSpecies.endsWith(" *")) {
				actualSpecies = actualSpecies.substring(0, actualSpecies.length()-2);
			}
			String predictedSpecies = cc.getPredictedSpeciesString();
			if (actualSpecies.equals("Other")) {
				confMatrix[confMatrix.length-2][cc.labelList.indexOf(predictedSpecies)]++;
			} else if (actualSpecies.equals("Unlabelled")) {
				confMatrix[confMatrix.length-1][cc.labelList.indexOf(predictedSpecies)]++;
			} else {
				confMatrix[cc.labelList.indexOf(actualSpecies)][cc.labelList.indexOf(predictedSpecies)]++;
			}
		}
		updateConfMatrixLabels();
	}
	
	protected class LoadingBarThread extends Thread {
		protected LoadingBarThread() {}
		@Override
		public void run() {
			loadingBarWindow.setVisible(true);
		}
	}
	
	/**
	 * Updates the "actual species" column when the species column in the WMNT is modified.
	 */
	public void wmatUpdate(WMATDataUnit wmatdu) {
		if (wmatdu.startLoadingBar) {
			loadingBarWindow = new LCLoadingBarWindow(control.getGuiFrame(), wmatdu.totalRowsToUpdate);
			loadingBarThread = new LoadingBarThread();
			loadingBarThread.start();
		}
		LCDataBlock db = (LCDataBlock) control.getProcess().getOutputDataBlock(0);
		if (wmatdu.clearAllFirst) {
			fullTableUpdate();
		}
		HashMap<String, WMATAnnotationInfo> wmatMap = wmatdu.uidMap;
		Iterator<String> it = wmatMap.keySet().iterator();
		ArrayList<String> updatedRows = new ArrayList<String>();
		HashMap<String, String> speciesMap = new HashMap<String, String>();
		while (it.hasNext()) {
			String currKey = it.next();
			//System.out.println("currKey: "+currKey+" ("+String.valueOf(uidToClusterMap.containsKey(currKey))+")");
			if (!uidToClusterMap.containsKey(currKey)) {
				continue;
			}
			if (!updatedRows.contains(uidToClusterMap.get(currKey))) {
				updatedRows.add(uidToClusterMap.get(currKey));
			}
		}
		HashMap<String, LCDataUnit> unitMap = db.retrieveDataUnitsByIDandDate(updatedRows);
		it = wmatMap.keySet().iterator();
		while (it.hasNext()) {
			String uidKey = it.next();
			String currKey = uidToClusterMap.get(uidKey);
			LCDataUnit du = unitMap.get(currKey);
			if (du == null) {
				unitMap.remove(currKey);
				continue;
			}
			LCCallCluster cc = du.getCluster();
			// TODO Make this take callType into account.
			String actualSpecies = cc.getActualSpeciesString();
			if (actualSpecies.endsWith(" *")) {
				actualSpecies = actualSpecies.substring(0, actualSpecies.length()-2);
			}
			if (!speciesMap.containsKey(cc.clusterID)) {
				speciesMap.put(cc.clusterID, actualSpecies);
			}
			String foundSpecies = wmatMap.get(uidKey).species;
			if (control.getParams().labelAliases.containsKey(foundSpecies))
				du.getCluster().setIndividualActualSpecies(Long.valueOf(uidKey.split(", ")[0]), control.getParams().labelAliases.get(foundSpecies));
			else
				du.getCluster().setIndividualActualSpecies(Long.valueOf(uidKey.split(", ")[0]), wmatMap.get(uidKey).species);
		}
		
		for (int i = 0; i < resultsTable.getRowCount(); i++) {
			if (unitMap.size() == 0) {
				break;
			}
			String clusterID = (String) resultsTable.getValueAt(i, 0);
			String datetime = (String) resultsTable.getValueAt(i, 1);
			String key = clusterID+", "+datetime;
			if (!unitMap.containsKey(key)) {
				continue;
			}
			LCDataUnit du = unitMap.remove(key);
			if (du == null) {
				continue;
			}
			LCCallCluster cc = du.getCluster();
			resultsTable.setValueAt(cc.getColumn4String(true), i, 4);
			resultsTable.setValueAt(cc.getActualSpeciesString(), i, 7);
			
			ArrayList<String> labelList = new ArrayList<String>();
			for (int j = 0; j < control.getParams().labelOrder.length; j++) {
				labelList.add(control.getParams().labelOrder[j]);
			}
			if (labelList.contains(cc.getPredictedSpeciesString())) {
				String originalSpecies = speciesMap.get(cc.clusterID);
				String newSpecies = cc.getActualSpeciesString();
				if (newSpecies.endsWith(" *")) {
					newSpecies = newSpecies.substring(0, newSpecies.length()-2);
				}
				if (!originalSpecies.equals(newSpecies)) {
					if (labelList.contains(originalSpecies)) {
						confMatrix[labelList.indexOf(originalSpecies)][labelList.indexOf(cc.getPredictedSpeciesString())]--;
					} else if (originalSpecies.equals("Other")) {
						confMatrix[confMatrix.length-2][labelList.indexOf(cc.getPredictedSpeciesString())]--;
					} else if (originalSpecies.equals("Unlabelled")) {
						confMatrix[confMatrix.length-1][labelList.indexOf(cc.getPredictedSpeciesString())]--;
					}
					if (labelList.contains(newSpecies)) {
						confMatrix[labelList.indexOf(newSpecies)][labelList.indexOf(cc.getPredictedSpeciesString())]++;
					} else if (newSpecies.equals("Other")) {
						confMatrix[confMatrix.length-2][labelList.indexOf(cc.getPredictedSpeciesString())]++;
					} else if (newSpecies.equals("Unlabelled")) {
						confMatrix[confMatrix.length-1][labelList.indexOf(cc.getPredictedSpeciesString())]++;
					}
					updateConfMatrixLabels();
				}
			}
		}
		if (loadingBarWindow != null && loadingBarWindow.isVisible()) {
			loadingBarWindow.updateLoadingBar(wmatMap.size());
			if (wmatdu.endLoadingBar) {
				loadingBarWindow.setVisible(false);
			}
		}
	}
	
	// Kudos to jdiver at: https://stackoverflow.com/questions/23814282/change-the-font-color-in-a-specific-cell-of-a-jtable
	public static class CustomTableRenderer extends DefaultTableCellRenderer {
		
		protected LCControl control;
		public CustomTableRenderer(LCControl control) {
			this.control = control;
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (isSelected) {
				c.setBackground(new Color(0,140,255));
				c.setForeground(Color.WHITE);
			} else {
				c.setForeground(Color.BLACK);
				if (table.getValueAt(row, 3).equals("ERROR")) {
					c.setBackground(Color.WHITE);
					c.setForeground(Color.RED);
				} else if (column == 3) {
					String strVal = (String) value;
					Color curr = control.getParams().labelColours.get(value);
					c.setBackground(curr);
					if (curr.getRed() >= 200 || curr.getGreen() >= 200) {
						c.setForeground(Color.BLACK);
					} else {
						c.setForeground(Color.WHITE);
					}
				} else if (column == 6) {
					String strVal = (String) value;
					strVal = strVal.substring(6, strVal.length()-1);
					if (strVal.equals("Very low") || strVal.equals("Low")) {
						c.setBackground(new Color(250,150,150));
					} else if (strVal.equals("Average")) {
						c.setBackground(new Color(250,250,150));
					} else if (strVal.equals("High") || strVal.equals("Very high")) {
						c.setBackground(new Color(150,250,150));
					}
				} else if (column == 7 && !value.equals("Unlabelled")) {
					String strVal = (String) value;
					if (strVal.endsWith(" *")) {
						strVal = strVal.substring(0, strVal.length()-2);
					}
					if (strVal.equals((String) table.getValueAt(row, 3))) {
						c.setBackground(new Color(150,250,150));
					} else if (strVal.equals("Other")) {
						c.setBackground(Color.BLACK);
						c.setForeground(Color.WHITE);
					} else {
						c.setBackground(new Color(250,150,150));
					}
				} else {
					String strVal = (String) table.getValueAt(row, 6);
					strVal = strVal.substring(6, strVal.length()-1);
					String worst = control.getParams().worstCertainty;
					ArrayList<String> descriptors = new ArrayList<String>();
					descriptors.add("Very low");
					descriptors.add("Low");
					descriptors.add("Average");
					descriptors.add("High");
					descriptors.add("Very high");
					int total = (int) table.getValueAt(row, 2);
					if (descriptors.indexOf(strVal) < descriptors.indexOf(worst) || total < control.getParams().minClusterSize) {
						c.setBackground(Color.GRAY);
					} else {
						c.setBackground(PamColors.getInstance().getBorderColour());
					}
				}
			}
			return c;
		}
	}
	
	public PamTable getTable() {
		return resultsTable;
	}
	
	public final JLabel[][] getAccuracyMatrixLabels() {
		return jLabelAccuracyMatrix;
	}
	
	public final JLabel[][] getConfusionMatrixLabels() {
		return jLabelConfMatrix;
	}
	
	public DefaultTableModel getTableModel() {
		return dtm;
	}
	
	public void clearTable() {
		while (dtm.getRowCount() > 0) dtm.removeRow(0);
		tableHashSet.clear();
	}
	
	public LCWaitingDialogThread getWaitingDialogThread() {
		return wdThread;
	}
	
	/**
	 * Returns the selected file from a JFileChooser, including the extension from
	 * the file filter.
	 * 
	 * Copied from here: https://stackoverflow.com/questions/16846078/jfilechoosershowsavedialog-cant-get-the-value-of-the-extension-file-chosen
	 * Author page: https://stackoverflow.com/users/964243/boann
	 */
	public static File getSelectedFileWithExtension(JFileChooser c) {
	    File file = c.getSelectedFile();
	    if (c.getFileFilter() instanceof FileNameExtensionFilter) {
	        String[] exts = ((FileNameExtensionFilter)c.getFileFilter()).getExtensions();
	        String nameLower = file.getName().toLowerCase();
	        for (String ext : exts) { // check if it already has a valid extension
	            if (nameLower.endsWith('.' + ext.toLowerCase())) {
	                return file; // if yes, return as-is
	            }
	        }
	        // if not, append the first extension from the selected filter
	        file = new File(file.toString() + '.' + exts[0]);
	    }
	    return file;
	}
}
