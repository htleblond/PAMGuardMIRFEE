package mirfeeTrainingSetBuilder;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.table.*;

import javax.swing.filechooser.*;
import javax.swing.event.*;
import java.util.*; //
import java.io.PrintWriter;

import javax.swing.border.TitledBorder;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamBorderPanel;
import PamView.panel.PamPanel;
import PamView.PamTable;
import PamUtils.PamFileChooser;

/**
 * The panel where the GUI components are written.
 * @author Holly LeBlond
 */
public class TSBPanel extends PamBorderPanel {
	
	TSBControl tsbControl;
	protected PamPanel mainPanel;
	
	protected JButton addButton;
	protected JButton deleteButton;
	protected JButton clearButton;
	//protected JButton moveUpButton;
	//protected JButton moveDownButton;
	protected JButton editButton;
	protected JButton splitButton;
	
	protected JButton settingsButton;
	
	protected DefaultTableModel subsetTableModel;
	protected PamTable subsetTable;
	
	protected JButton audioBatchButton;
	
	protected JButton loadButton;
	protected JButton saveButton;
	
	protected int[] outputFeatureIndices;
	protected ArrayList<String> outputLabelList;
	
	public TSBPanel(TSBControl tsbControl) {
		this.tsbControl = tsbControl;
		
		this.setLayout(new BorderLayout());
		
		mainPanel = new PamPanel();
		//mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setLayout(new BorderLayout());
		//GridBagConstraints a = new PamGridBagContraints();
		
		PamPanel memPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		memPanel.setBorder(new TitledBorder("MIRFEE Training Set Builder"));
		
		//JPanel topButtonsPanel = new JPanel(new GridLayout(1, 7, 5, 5));
		JPanel topButtonsPanel = new JPanel(new GridLayout(1, 5, 5, 5));
		addButton = new JButton("Add subset");
		addButton.addActionListener(new AddButtonListener());
		topButtonsPanel.add(addButton);
		editButton = new JButton("Edit subset");
		editButton.addActionListener(new EditButtonListener());
		editButton.setEnabled(false);
		topButtonsPanel.add(editButton);
		splitButton = new JButton("Split subset");
		splitButton.addActionListener(new SplitButtonListener());
		splitButton.setEnabled(false);
		topButtonsPanel.add(splitButton);
		//moveUpButton = new JButton("Move up");
		//moveUpButton.addActionListener(new MoveButtonListener(true));
		//moveUpButton.setEnabled(false);
		//topButtonsPanel.add(moveUpButton);
		//moveDownButton = new JButton("Move down");
		//moveDownButton.addActionListener(new MoveButtonListener(false));
		//moveDownButton.setEnabled(false);
		//topButtonsPanel.add(moveDownButton);
		deleteButton = new JButton("Delete subset");
		deleteButton.addActionListener(new DeleteButtonListener());
		deleteButton.setEnabled(false);
		topButtonsPanel.add(deleteButton);
		clearButton = new JButton("Clear table");
		clearButton.addActionListener(new ClearButtonListener());
		clearButton.setEnabled(false);
		topButtonsPanel.add(clearButton);
		b.gridy = 0;
		b.gridx = 0;
		b.fill = b.NONE;
		b.anchor = b.NORTHWEST;
		memPanel.add(topButtonsPanel, b);
		
		settingsButton = new JButton("Settings");
		settingsButton.addActionListener(new SettingsButtonListener());
		settingsButton.setEnabled(true);
		b.gridx++;
		b.anchor = b.NORTHEAST;
		memPanel.add(settingsButton, b);
		
		String[] columnNames = {"ID", "Location", "Start", "End", "Total"};
		subsetTableModel = new DefaultTableModel(columnNames,0) {
			Class[] types = {String.class, String.class, String.class, String.class, Integer.class};
			boolean[] canEdit = {false, false, false, false, false};
			
			@Override
			public Class getColumnClass(int index) {
				return this.types[index];
			}
			
			@Override
			public boolean isCellEditable(int row, int column) {
				return this.canEdit[column];
			}
		};
		subsetTable = new PamTable(subsetTableModel);
		subsetTable.getTableHeader().setReorderingAllowed(false);
		subsetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		subsetTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
            	if (!e.getValueIsAdjusting()) {
	            	int selected = subsetTable.getSelectedRow();
	                if (selected > -1) {
	                	editButton.setEnabled(true);
	                	splitButton.setEnabled(true);
	                	//moveUpButton.setEnabled(selected > 0);
	                	//moveDownButton.setEnabled(selected < subsetTable.getRowCount()-1);
	                	deleteButton.setEnabled(true);
	                } else {
	                	editButton.setEnabled(false);
	                	splitButton.setEnabled(false);
	                	//moveUpButton.setEnabled(false);
	                	//moveDownButton.setEnabled(false);
	                	deleteButton.setEnabled(false);
	                }
            	}
            }
        });
		subsetTable.setAutoCreateRowSorter(true);
		subsetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		subsetTable.getColumnModel().getColumn(0).setPreferredWidth(30);
		subsetTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		subsetTable.getColumnModel().getColumn(2).setPreferredWidth(150);
		subsetTable.getColumnModel().getColumn(3).setPreferredWidth(150);
		subsetTable.getColumnModel().getColumn(4).setPreferredWidth(50);
		//subsetTable.setSize(550, 450);
		JScrollPane sp = new JScrollPane(subsetTable);
		sp.setPreferredSize(new Dimension(1000, 500));
		b.gridy++;
		b.gridx = 0;
		b.gridwidth = 2;
		b.fill = b.BOTH;
		b.anchor = b.WEST;
		memPanel.add(sp, b);
		
		audioBatchButton = new JButton("Create audio test batch");
		audioBatchButton.addActionListener(new AudioBatchButtonListener());
		b.gridy++;
		b.gridx = 0;
		b.gridwidth = 1;
		b.fill = b.NONE;
		b.anchor = b.SOUTHWEST;
		memPanel.add(audioBatchButton, b);
		JPanel bottomRightButtonsPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		loadButton = new JButton("Load set");
		loadButton.addActionListener(new LoadButtonListener());
		bottomRightButtonsPanel.add(loadButton);
		saveButton = new JButton("Save set");
		saveButton.addActionListener(new SaveButtonListener());
		saveButton.setEnabled(false);
		bottomRightButtonsPanel.add(saveButton);
		//b.gridy++;
		b.gridx = 1;
		b.gridwidth = 1;
		b.fill = b.NONE;
		b.anchor = b.SOUTHEAST;
		memPanel.add(bottomRightButtonsPanel, b);
		
		//a.fill = a.BOTH;
		mainPanel.add(memPanel);
		this.add(mainPanel);
	}
	
	protected class AddButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			TSBSubsetDialog subsetDialog = new TSBSubsetDialog(tsbControl.getPamView().getGuiFrame(), tsbControl, false);
			subsetDialog.setVisible(true);
		}
	}
	
	protected class EditButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			TSBSubsetDialog subsetDialog = new TSBSubsetDialog(tsbControl.getPamView().getGuiFrame(), tsbControl, true);
			subsetDialog.setVisible(true);
		}
	}
	
	protected class SplitButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			TSBSplitDialog splitDialog = new TSBSplitDialog(tsbControl.getPamView().getGuiFrame(), tsbControl);
			splitDialog.setVisible(true);
		}
	}
	
/*	protected class MoveButtonListener implements ActionListener{
		
		private boolean up;
		public MoveButtonListener(boolean up) {
			this.up = up;
		}
		
		public void actionPerformed(ActionEvent e) {
			int selection = subsetTable.getSelectedRow();
			if (selection > -1) {
				if (up) {
					if (selection > 0) {
						subsetTableModel.moveRow(selection, selection, selection-1);
						subsetTable.setRowSelectionInterval(selection-1, selection-1);
					}
				} else {
					if (selection < subsetTable.getRowCount()-1) {
						subsetTableModel.moveRow(selection, selection, selection+1);
						subsetTable.setRowSelectionInterval(selection+1, selection+1);
					}
				}
				selection = subsetTable.getSelectedRow();
				//System.out.println("getSelectedRow: "+String.valueOf(selection));
				moveUpButton.setEnabled(selection > 0);
				moveDownButton.setEnabled(selection < subsetTable.getRowCount()-1);
			} else {
				moveUpButton.setEnabled(false);
				moveDownButton.setEnabled(false);
			}
		}
	} */
	
	protected class DeleteButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int selection = subsetTable.getSelectedRow();
			if (selection > -1) {
				String rowID = (String) subsetTable.getValueAt(selection, 0);
				for (int i = 0; i < tsbControl.getSubsetList().size(); i++) {
					if (rowID.equals(tsbControl.getSubsetList().get(i).id)) {
						tsbControl.getSubsetList().remove(i);
						break;
					}
				}
				for (int i = 0 ; i < subsetTableModel.getRowCount(); i++) {
					if (rowID.equals(subsetTableModel.getValueAt(i, 0))) {
						subsetTableModel.removeRow(i);
						break;
					}
				}
				if (subsetTable.getRowCount() > 0) {
					if (selection == subsetTable.getRowCount()) {
						subsetTable.setRowSelectionInterval(selection-1, selection-1);
					} else {
						subsetTable.setRowSelectionInterval(selection, selection);
					}
				}
				if (subsetTable.getRowCount() == 0) {
					tsbControl.setFeatureList(new ArrayList<String>());
				}
				editButton.setEnabled(subsetTable.getRowCount() > 0);
				//moveUpButton.setEnabled(subsetTable.getRowCount() > 0 && selection > 0);
				//moveDownButton.setEnabled(subsetTable.getRowCount() > 0 && selection < subsetTable.getRowCount()-1);
				deleteButton.setEnabled(subsetTable.getRowCount() > 0);
				clearButton.setEnabled(subsetTable.getRowCount() > 0);
				saveButton.setEnabled(subsetTable.getRowCount() > 0);
			} else {
				deleteButton.setEnabled(false);
			}
		}
	}
	
	protected class ClearButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (subsetTable.getRowCount() > 0) {
				int res = JOptionPane.showConfirmDialog(tsbControl.getPamView().getGuiFrame(),
						tsbControl.makeHTML("Are you sure?", 150),
						"MIRFEE Training Set Builder",
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.NO_OPTION) {
					return;
				}
				tsbControl.resetSubsetList();
				while (subsetTable.getRowCount() > 0) {
					subsetTableModel.removeRow(0);
				}
			}
			tsbControl.setFeatureList(new ArrayList<String>());
			editButton.setEnabled(false);
			//moveUpButton.setEnabled(false);
			//moveDownButton.setEnabled(false);
			deleteButton.setEnabled(false);
			clearButton.setEnabled(false);
			saveButton.setEnabled(false);
		}
	}
	
	protected class SettingsButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			tsbControl.settingsDialog(tsbControl.getPamView().getGuiFrame());
		}
	}
	
	protected class AudioBatchButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JComboBox<String> box = new JComboBox<String>();
			box.addItem("Use data from loaded training set");
			box.addItem("Use data from external .wmat or .mtsf file");
			int res = JOptionPane.showConfirmDialog(tsbControl.getGuiFrame(),
					box,
					"Creating new audio batch",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (res != JOptionPane.OK_OPTION) return;
			if (box.getSelectedIndex() == 0) {
				if (tsbControl.getSubsetList().size() == 0) {
					tsbControl.simpleErrorDialog("Training data must be loaded into the table first.", 250);
					return;
				}
			}
			TSBAudioTestBatchDialog dialog = new TSBAudioTestBatchDialog(tsbControl.getPamView().getGuiFrame(), tsbControl, box.getSelectedIndex() == 0);
			dialog.setVisible(true);
		}
	}
	
	protected class LoadButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			loadButtonAction();
		}
	}
	
	/**
	 * Function performed by the load button.
	 * Opens a file chooser and attempts to load a pre-existing training set into the table for modifying.
	 */
	protected void loadButtonAction() {
		PamFileChooser fc = new PamFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("MIRFEE training set file (*.mtsf)","mirrfts","mtsf"));
		//fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
		int returnVal = fc.showOpenDialog(tsbControl.getPamView().getGuiFrame());
		if (returnVal == fc.CANCEL_OPTION) {
			return;
		}
		File f = fc.getSelectedFile();
		if (!f.exists()) {
			tsbControl.simpleErrorDialog("Selected file does not exist.", 250);
			return;
		}
		if (subsetTable.getRowCount() > 0) {
			int res = JOptionPane.showConfirmDialog(tsbControl.getPamView().getGuiFrame(),
					tsbControl.makeHTML("Loading in a pre-existing training set will clear all progress. Proceed?", 250),
					"MIRFEE Training Set Builder",
					JOptionPane.YES_NO_OPTION);
			if (res != JOptionPane.YES_OPTION) {
				return;
			}
			while (subsetTable.getRowCount() > 0) {
				subsetTableModel.removeRow(0);
			}
		}
		tsbControl.classMap.clear();
		tsbControl.fullClassList.clear();
		tsbControl.subsetList.clear();
		tsbControl.featureList.clear();
		tsbControl.feParamsMap.clear();
		String[] firstSplit = null;
		ArrayList<String[]> dataLines = new ArrayList<String[]>();
		Scanner sc = null;
		try {
			sc = new Scanner(f);
			if (!sc.hasNextLine()) {
				tsbControl.simpleErrorDialog("Selected file is not formatted like Training Set Builder output.", 250);
				sc.close();
				return;
			}
			String firstLine = sc.nextLine();
			if (firstLine.equals("EXTRACTOR PARAMS START")) {
				while (sc.hasNextLine()) {
					String nextLine = sc.nextLine();
					if (nextLine.equals("EXTRACTOR PARAMS END")) break;
					String[] split = nextLine.split("=");
					if (split.length >= 2)
						tsbControl.feParamsMap.put(split[0], split[1]);
				}
				if (sc.hasNextLine()) firstLine = sc.nextLine();
				else {
					tsbControl.simpleErrorDialog("Selected file does not contain any valid entries.", 250);
					sc.close();
					return;
				}
			}
			firstSplit = firstLine.split(",");
			if (firstSplit.length < 10) {
				tsbControl.simpleErrorDialog("Selected file is not formatted like Training Set Builder output.", 250);
				sc.close();
				return;
			}
			if (!(firstLine.startsWith("cluster,uid,location,date,duration,lf,hf,label,"))) {
				tsbControl.simpleErrorDialog("Selected file is not formatted like Training Set Builder output.", 250);
				sc.close();
				return;
			}
			while (sc.hasNextLine()) {
				String[] nextLine = sc.nextLine().split(",");
				String[] outpLine = new String[firstSplit.length];
				if (nextLine.length >= firstSplit.length) {
					try {
						assert (nextLine[0].length() > 3);
						assert (nextLine[1].length() > 0);
						assert (nextLine[3].length() == 23);
						for (int i = 4; i < firstSplit.length; i++) {
							assert (nextLine[i].length() > 0);
						}
						outpLine[0] = nextLine[0];
						outpLine[1] = String.valueOf(Long.valueOf(nextLine[1]));
						outpLine[2] = nextLine[2];
						outpLine[3] = nextLine[3];
						outpLine[4] = String.valueOf(Double.valueOf(nextLine[4]));
						outpLine[5] = String.valueOf(Double.valueOf(nextLine[5]));
						outpLine[6] = String.valueOf(Double.valueOf(nextLine[6]));
						outpLine[7] = nextLine[7];
						for (int i = 8; i < outpLine.length; i++) {
							outpLine[i] = String.valueOf(Double.valueOf(nextLine[i]));
						}
						dataLines.add(outpLine);
					} catch (AssertionError | Exception e2) {
						//e2.printStackTrace();
						// TODO
					}
				}
			}
			sc.close();
		} catch (Exception e2) {
			tsbControl.simpleErrorDialog("Exception thrown when parsing through feature data file.", 250);
			e2.printStackTrace();
			if (sc != null) {
				sc.close();
			}
			return;
		}
		if (dataLines.size() == 0) {
			tsbControl.simpleErrorDialog("Selected file contains no valid data entries", 250);
			return;
		}
		ArrayList<String> idList = new ArrayList<String>();
		for (int i = 0; i < dataLines.size(); i++) {
			String subs = dataLines.get(i)[0].substring(0, 2);
			if (!idList.contains(subs)) {
				idList.add(subs);
			}
		}
		ArrayList<TSBSubset> outpSubsetList = new ArrayList<TSBSubset>();
		ArrayList<String> outpClassList = new ArrayList<String>();
		ArrayList<String> outpFeatureList = new ArrayList<String>();
		HashMap<String, String> outpMap = new HashMap<String, String>();
		for (int i = 0; i < idList.size(); i++) {
			TSBSubset curr = new TSBSubset();
			curr.id = idList.get(i);
			int j = 0;
			while (j < dataLines.size()) {
				String[] currLine = dataLines.get(j);
				if (currLine[0].substring(0, 2).equals(curr.id)) {
					if (curr.location.length() == 0) {
						curr.location = currLine[2];
					}
					if (curr.start.length() == 0 || currLine[3].compareTo(curr.start) < 0) {
						curr.start = currLine[3];
					}
					if (curr.end.length() == 0 || currLine[3].compareTo(curr.end) > 0) {
						curr.end = currLine[3];
					}
					if (!curr.classList.contains(currLine[7])) {
						curr.classList.add(currLine[7]);
						curr.validEntriesList.add(new ArrayList<TSBDetection>());
					}
					try {
						currLine[0] = currLine[0].substring(3); //TODO MAKE SURE THIS IS OKAY
						TSBDetection outp = new TSBDetection(tsbControl, firstSplit.length-8, currLine);
						curr.validEntriesList.get(curr.classList.indexOf(currLine[7])).add(outp);
					} catch (AssertionError | Exception e2) {
						e2.printStackTrace();
					}
					dataLines.remove(j);
				} else {
					j++;
				}
			}
			curr.selectionArray = new int[curr.classList.size()];
			for (int k = 0; k < curr.selectionArray.length; k++) {
				curr.selectionArray[k] = k;
			}
			outpSubsetList.add(curr);
			for (int k = 0; k < curr.classList.size(); k++) {
				if (!outpClassList.contains(curr.classList.get(k))) {
					outpClassList.add(curr.classList.get(k));
					outpMap.put(curr.classList.get(k), curr.classList.get(k));
				}
			}
		}
		for (int i = 8; i < firstSplit.length; i++) {
			outpFeatureList.add(firstSplit[i]);
		}
		String[] columnNames = new String[5 + outpClassList.size()];
		columnNames[0] = "ID";
		columnNames[1] = "Location";
		columnNames[2] = "Start";
		columnNames[3] = "End";
		columnNames[4] = "Total";
		for (int i = 0; i < outpClassList.size(); i++) {
			columnNames[i+5] = outpClassList.get(i);
		}
		Class[] typesPre = new Class[columnNames.length];
		for (int i = 0; i < typesPre.length; i++) {
			if (i < 4) {
				typesPre[i] = String.class;
			} else {
				typesPre[i] = Integer.class;
			}
		}
		boolean[] canEditPre = new boolean[columnNames.length];
		for (int i = 0; i < canEditPre.length; i++) {
			canEditPre[i] = false;
		}
		DefaultTableModel newTableModel = new DefaultTableModel(columnNames,0) {
			Class[] types = typesPre;
			boolean[] canEdit = canEditPre;
			
			@Override
			public Class getColumnClass(int index) {
				return this.types[index];
			}
			
			@Override
			public boolean isCellEditable(int row, int column) {
				return this.canEdit[column];
			}
		};
		ArrayList<Object[]> rowList = new ArrayList<Object[]>();
		for (int i = 0; i < outpSubsetList.size(); i++) {
			TSBSubset curr = outpSubsetList.get(i);
			Object[] row = new Object[columnNames.length];
			row[0] = curr.id;
			row[1] = curr.location;
			row[2] = curr.start;
			row[3] = curr.end;
			int currTotal = 0;
			for (int j = 5; j < columnNames.length; j++) {
				int index = curr.classList.indexOf(columnNames[j]);
				if (index > -1) {
					row[j] = curr.validEntriesList.get(index).size();
					currTotal += curr.validEntriesList.get(index).size();
				} else {
					row[j] = 0;
				}
			}
			row[4] = currTotal;
			rowList.add(row);
		}
		setTableModel(newTableModel);
		subsetTable.getColumnModel().getColumn(0).setPreferredWidth(30);
		subsetTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		subsetTable.getColumnModel().getColumn(2).setPreferredWidth(150);
		subsetTable.getColumnModel().getColumn(3).setPreferredWidth(150);
		for (int i = 4; i < subsetTable.getColumnCount(); i++) {
			subsetTable.getColumnModel().getColumn(i).setPreferredWidth(50);
		}
		for (int i = 0; i < rowList.size(); i++) {
			subsetTableModel.addRow(rowList.get(i));
		}
		
		tsbControl.setSubsetList(outpSubsetList);
		tsbControl.setFeatureList(outpFeatureList);
		tsbControl.setFullClassList(outpClassList);
		tsbControl.setClassMap(outpMap);
		
		clearButton.setEnabled(subsetTable.getRowCount() > 0);
		saveButton.setEnabled(subsetTable.getRowCount() > 0);
	}
	
	protected class SaveButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			saveButtonAction();
		}
	}
	
	/**
	 * Function performed by the save button.
	 * Basically attempts to create a .mtsf file containing the new training set.
	 */
	protected void saveButtonAction() {
		outputFeatureIndices = new int[0];
		if (subsetTable.getRowCount() > 0) {
			TSBParameters params = tsbControl.getParams();
			TSBLabelSelectionDialog labelDialog = new TSBLabelSelectionDialog(tsbControl.getPamView().getGuiFrame(), tsbControl);
			labelDialog.setVisible(true);
			if (outputLabelList.size() == 0) { // Note that selecting OK in TSBLabelSelectionDialog changes the list, so this isn't dead code.
				return;
			}
			TSBFeatureDialog featureDialog = new TSBFeatureDialog(tsbControl.getPamView().getGuiFrame(), tsbControl);
			featureDialog.setVisible(true);
			if (outputFeatureIndices.length == 0) { // Note that selecting OK in TSBFeatureDialog changes outputFeatureIndices, so this isn't dead code.
				return;
			}
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("MIRFEE training set file (*.mtsf)","mirrfts","mtsf"));
			//fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
			int returnVal = fc.showSaveDialog(tsbControl.getPamView().getGuiFrame());
			if (returnVal == fc.APPROVE_OPTION) {
				File f = fc.getSelectedFile();
				if (f.getName().length() == 0) {
					tsbControl.simpleErrorDialog("Invalid file name.", 200);
					return;
				}
				if (f.getPath().length() >= 4) {
					if (!f.getPath().endsWith(".mirrfts") || !!f.getPath().endsWith(".mtsf")) {
						f = new File(f.getPath()+".mtsf");
					}
				} else {
					f = new File(f.getPath()+".mtsf");
				}
				if (f.exists()) {
					int res = JOptionPane.showConfirmDialog(tsbControl.getPamView().getGuiFrame(),
							tsbControl.makeHTML("Overwrite selected file?", 250),
							"MIRFEE Training Set Builder",
							JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.NO_OPTION) {
						return;
					}
					if (!f.delete()) {
						tsbControl.simpleErrorDialog("Could not delete selected file.", 300);
						return;
					}
				}
				try {
					f.createNewFile();
				} catch (IOException e1) {
					tsbControl.simpleErrorDialog("Could not create new blank file with selected file name.", 300);
					return;
				}
				ArrayList<String[]> outpList = new ArrayList<String[]>();
				String[] firstLine = new String[8+outputFeatureIndices.length];
				firstLine[0] = "cluster";
				firstLine[1] = "uid";
				firstLine[2] = "location";
				firstLine[3] = "date";
				firstLine[4] = "duration";
				firstLine[5] = "lf";
				firstLine[6] = "hf";
				firstLine[7] = "label";
				for (int i = 8; i < firstLine.length; i++) {
					firstLine[i] = tsbControl.getFeatureList().get(outputFeatureIndices[i-8]);
				}
				outpList.add(firstLine);
				for (int i = 0; i < tsbControl.getTabPanel().getPanel().getSubsetTable().getRowCount(); i++) {
					String currID = (String) tsbControl.getTabPanel().getPanel().getSubsetTable().getValueAt(i, 0);
					TSBSubset currSubset = null;
					for (int j = 0; j < tsbControl.getSubsetList().size(); j++) {
						if (currID.equals(tsbControl.getSubsetList().get(j).id)) {
							currSubset = tsbControl.getSubsetList().get(j);
							break;
						}
					}
					if (currSubset == null) continue;
					HashMap<String, TSBClusterDetectionList> clusterMap = currSubset.createTSBClusterHashMap();
					ArrayList<String> idList = new ArrayList<String>();
					Iterator<String> it = clusterMap.keySet().iterator();
					while (it.hasNext()) idList.add(it.next());
					Collections.sort(idList);
					for (int j = 0; j < idList.size(); j++) {
						//System.out.println(idList.get(j)+" -> "+String.valueOf(clusterMap.get(idList.get(j)).size()));
						TSBClusterDetectionList currCluster = clusterMap.get(idList.get(j));
						if (currCluster.size() == 0) continue;
						if (params.multilabelOption == params.MULTILABEL_SKIP_CLUSTER && currCluster.containsMultipleSpecies()) {
							System.out.println("Skipped cluster "+idList.get(j)+" due to multilabel settings.");
							continue;
						}
						if (params.overlapOption == params.OVERLAP_SKIP_BOTH)
							currCluster = currCluster.removeOverlaps(currID);
						ArrayList<String> outpClasses = new ArrayList<String>();
						for (int k = 0; k < currSubset.selectionArray.length; k++)
							outpClasses.add(currSubset.classList.get(currSubset.selectionArray[k]));
						for (int k = 0; k < currCluster.size(); k++) {
							if (!outpClasses.contains(currCluster.get(k).species)) {
								currCluster.remove(k);
								k--;
							}
						}
						if (params.overlapOption == params.MULTILABEL_KEEP_MOST)
							currCluster = currCluster.removeLessOccuringSpecies(currID);
						ArrayList<String[]> splitList = new ArrayList<String[]>();
						for (int k = 0; k < currCluster.size(); k++) {
							TSBDetection currDetection = currCluster.get(k);
							if (!tsbControl.getFullClassList().contains(currDetection.species)) continue;
							if (!outputLabelList.contains(tsbControl.getClassMap().get(currDetection.species))) continue;
							if ((params.skipLowFreqChecked && currDetection.lf < params.skipLowFreq) ||
									(params.skipHighFreqChecked && currDetection.hf > params.skipHighFreq)) {
								System.out.println("Skipped contour "+String.valueOf(currDetection.uid)+" in "+currID+"-"+currDetection.clusterID+" due to specified frequency limits.");
								continue;
							}
							if ((params.skipShortDurChecked && currDetection.duration < params.skipShortDur) ||
									(params.skipLongDurChecked && currDetection.duration > params.skipLongDur)) {
								System.out.println("Skipped contour "+String.valueOf(currDetection.uid)+" in "+currID+"-"+currDetection.clusterID+" due to specified duration limits.");
								continue;
							}
							String[] nextLine = new String[8+outputFeatureIndices.length];
							nextLine[0] = currID+"-"+currDetection.clusterID;
							nextLine[1] = String.valueOf(currDetection.uid);
							nextLine[2] = currSubset.location;
							nextLine[3] = currDetection.datetime;
							nextLine[4] = String.valueOf(currDetection.duration);
							nextLine[5] = String.valueOf(currDetection.lf);
							nextLine[6] = String.valueOf(currDetection.hf);
							nextLine[7] = tsbControl.getClassMap().get(currDetection.species);
							for (int l = 0; l < outputFeatureIndices.length; l++) {
								nextLine[l+8] = String.valueOf(currDetection.featureVector[outputFeatureIndices[l]]);
							}
							splitList.add(nextLine);
						}
						splitList.sort(Comparator.comparing(a -> Long.valueOf(a[1])));
						splitList.sort(Comparator.comparing(a -> a[0]));
						for (int k = 0; k < splitList.size(); k++) outpList.add(splitList.get(k));
					}
				}
				try {
					PrintWriter pw = new PrintWriter(f);
					StringBuilder sb = new StringBuilder();
					sb.append("EXTRACTOR PARAMS START\n");
					pw.write(sb.toString());
					pw.flush();
					HashMap<String, String> feParamsMap = tsbControl.getFEParamsMap();
					Iterator<String> it = feParamsMap.keySet().iterator();
					while (it.hasNext()) {
						sb = new StringBuilder();
						String nextKey = it.next();
						sb.append(nextKey+"="+feParamsMap.get(nextKey)+"\n");
						pw.write(sb.toString());
						pw.flush();
					}
					sb = new StringBuilder();
					sb.append("EXTRACTOR PARAMS END\n");
					pw.write(sb.toString());
					pw.flush();
					for (int i = 0; i < outpList.size(); i++) {
						sb = new StringBuilder();
						String[] outp = outpList.get(i);
						sb.append(outp[0]);
						for (int j = 1; j < outp.length; j++) {
							sb.append(","+outp[j]);
						}
						if (i < outpList.size()-1) {
							sb.append("\n");
						}
						pw.write(sb.toString());
						pw.flush();
					}
					pw.close();
					JOptionPane.showMessageDialog(tsbControl.getPamView().getGuiFrame(),
							"Training set successfully written to .mtsf file.",
							"MIRFEE Training Set Builder",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e2) {
					System.out.println(e2);
					tsbControl.simpleErrorDialog("Could not fully write to .mtsf file. See console for more details.", 300);
				}
			}
		} else {
			saveButton.setEnabled(false);
		}
		outputFeatureIndices = new int[0];
	}
	
	/**
	 * Sets which features will be output to the new .mtsf file.
	 * Supposed to be done via the TSBFeatureDialog.
	 */
	public void setOutputFeatureIndices(int[] inp) {
		outputFeatureIndices = inp;
	}
	
	/**
	 * Sets which labels will be output to the new .mtsf file.
	 * Supposed to be done via the TSBLabelSelectionDialog.
	 */
	public void setOutputLabelList(ArrayList<String> inp) {
		outputLabelList = inp;
	}
	
	/**
	 * @return The DefaultTableModel for the big table in the middle of the panel.
	 */
	public DefaultTableModel getSubsetTableModel() {
		return subsetTableModel;
	}
	
	/**
	 * @return The big table in the middle of the panel.
	 */
	public JTable getSubsetTable() {
		return subsetTable;
	}
	
	/**
	 * Sets the model for the table in the middle of the panel.
	 */
	public void setTableModel(DefaultTableModel stm) {
		subsetTableModel = stm;
		subsetTable.setModel(subsetTableModel);
	}
}
