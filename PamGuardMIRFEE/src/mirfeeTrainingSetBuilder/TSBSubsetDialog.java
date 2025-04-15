package mirfeeTrainingSetBuilder;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Comparator;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import PamUtils.PamFileChooser;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import mirfeeFeatureExtractor.FEParameters;

/**
 * Dialog for creating and adding new subsets to the training set.
 * @author Holly LeBlond
 */
public class TSBSubsetDialog extends PamDialog {
	
	protected TSBControl tsbControl;
	protected Window parentFrame;
	protected boolean editing;
	protected String oldID = "";
	
	protected PamPanel mainPanel;
	
	protected JTextField featuresCSVField;
	protected JButton featuresCSVButton;
	protected JTextField wmatCSVField;
	protected JButton wmatCSVButton;
	protected JButton reloadButton;
	protected JComboBox<String> idDigit1Box;
	protected JComboBox<String> idDigit2Box;
	protected JTextField locationField;
	protected JTextField startField;
	protected JTextField endField;
	protected JTextField totalField;
	protected DefaultListModel dlmodel;
	//protected JList<JCheckBox> checkList;
	protected JList<String> checkList;
	
	protected ArrayList<String[]> featuresEntriesList;
	protected ArrayList<String[]> wmatEntriesList;
	protected ArrayList<String> classList;
	protected ArrayList<ArrayList<TSBDetection>> validEntriesList;
	protected ArrayList<String> featureList;
	protected HashMap<String, String> feSettingsMap;
	
	public TSBSubsetDialog(Window parentFrame, TSBControl tsbControl, boolean editing) {
		super(parentFrame, "MIRFEE Training Set Builder", false);
		this.tsbControl = tsbControl;
		this.parentFrame = parentFrame;
		this.editing = editing;
		
		this.featuresEntriesList = new ArrayList<String[]>();
		this.wmatEntriesList = new ArrayList<String[]>();
		this.classList = new ArrayList<String>();
		this.validEntriesList = new ArrayList<ArrayList<TSBDetection>>();
		this.featureList = new ArrayList<String>();
		
		mainPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		mainPanel.setBorder(new TitledBorder(""));
		
		JPanel topLeftPanel = new JPanel(new BorderLayout());
		//topLeftPanel.setBorder(new TitledBorder(""));
		JPanel topLeftSubPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = c.EAST;
		c.fill = c.NONE;
		topLeftSubPanel.add(new JLabel("Feature Extractor output:"), c);
		c.gridx++;
		featuresCSVField = new JTextField(30);
		featuresCSVField.setEnabled(false);
		topLeftSubPanel.add(featuresCSVField, c);
		c.gridx++;
		c.anchor = c.WEST;
		c.fill = c.HORIZONTAL;
		featuresCSVButton = new JButton("Select file");
		featuresCSVButton.addActionListener(new FileListener(FileListener.LOAD_FE));
		topLeftSubPanel.add(featuresCSVButton, c);
		c.gridy++;
		c.gridx = 0;
		c.anchor = c.EAST;
		c.fill = c.NONE;
		topLeftSubPanel.add(new JLabel("WMAT output:"), c);
		c.gridx++;
		wmatCSVField = new JTextField(30);
		wmatCSVField.setEnabled(false);
		topLeftSubPanel.add(wmatCSVField, c);
		c.gridx++;
		c.anchor = c.WEST;
		c.fill = c.HORIZONTAL;
		wmatCSVButton = new JButton("Select file");
		wmatCSVButton.addActionListener(new FileListener(FileListener.LOAD_WMAT));
		topLeftSubPanel.add(wmatCSVButton, c);
		topLeftPanel.add(topLeftSubPanel);
		c.gridy++;
		reloadButton = new JButton("Reload");
		reloadButton.addActionListener(new FileListener(FileListener.RELOAD));
		reloadButton.setEnabled(false);
		topLeftSubPanel.add(reloadButton, c);
		topLeftPanel.add(topLeftSubPanel);
		b.gridy = 0;
		b.gridx = 0;
		b.gridwidth = 1;
		b.anchor = b.SOUTH;
		b.fill = b.HORIZONTAL;
		mainPanel.add(topLeftPanel, b);
		
		JPanel bottomLeftPanel = new JPanel(new BorderLayout());
		//bottomLeftPanel.setBorder(new TitledBorder(""));
		JPanel bottomLeftSubPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		c.anchor = c.WEST;
		c.fill = c.NONE;
		bottomLeftSubPanel.add(new JLabel("ID"), c);
		c.gridx += 2;
		c.gridwidth = 1;
		bottomLeftSubPanel.add(new JLabel("Location"), c);
		c.gridx++;
		bottomLeftSubPanel.add(new JLabel("Start"), c);
		c.gridx++;
		bottomLeftSubPanel.add(new JLabel("End"), c);
		c.gridx++;
		bottomLeftSubPanel.add(new JLabel("Total"), c);
		c.gridy++;
		c.gridx = 0;
		String[] box1Digits = new String[] {"1","2","3","4","5","6","7","8","9","0",
				"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
		idDigit1Box = new JComboBox<String>(box1Digits);
		bottomLeftSubPanel.add(idDigit1Box, c);
		c.gridx++;
		String[] box2Digits = new String[] {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
				"1","2","3","4","5","6","7","8","9","0"};
		idDigit2Box = new JComboBox<String>(box2Digits);
		bottomLeftSubPanel.add(idDigit2Box, c);
		c.gridx++;
		locationField = new JTextField(14);
		bottomLeftSubPanel.add(locationField, c);
		c.gridx++;
		startField = new JTextField(14);
		startField.setEnabled(false);
		bottomLeftSubPanel.add(startField, c);
		c.gridx++;
		endField = new JTextField(14);
		endField.setEnabled(false);
		bottomLeftSubPanel.add(endField, c);
		c.gridx++;
		totalField = new JTextField(5);
		totalField.setEnabled(false);
		totalField.setText("0");
		bottomLeftSubPanel.add(totalField, c);
		bottomLeftPanel.add(bottomLeftSubPanel);
		b.gridy++;
		b.anchor = b.CENTER;
		mainPanel.add(bottomLeftPanel, b);
		
		JPanel rightPanel = new JPanel(new BorderLayout());
		JPanel rightSubPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = c.WEST;
		c.fill = c.NONE;
		rightSubPanel.add(new JLabel("Classes found:"), c);
		c.gridy++;
		c.fill = c.BOTH;
		dlmodel = new DefaultListModel();
		//checkList = new JList<JCheckBox>(dlmodel);
		//checkList.setCellRenderer(new CheckboxListCellRenderer());
		checkList = new JList<String>(dlmodel);
		// Copied from: https://stackoverflow.com/questions/2404546/select-multiple-items-in-jlist-without-using-the-ctrl-command-key
		checkList.setSelectionModel(new DefaultListSelectionModel() {
		    @Override
		    public void setSelectionInterval(int index0, int index1) {
		        if(super.isSelectedIndex(index0)) {
		            super.removeSelectionInterval(index0, index1);
		        }
		        else {
		            super.addSelectionInterval(index0, index1);
		        }
		    }
		});
		checkList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateFields();
			}
		});
		checkList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane sp = new JScrollPane(checkList);
		rightSubPanel.add(sp, c);
		rightPanel.add(rightSubPanel);
		b.gridy = 0;
		b.gridx = 1;
		b.gridheight = 2;
		b.fill = b.VERTICAL;
		mainPanel.add(rightPanel, b);
		
		setDialogComponent(mainPanel);
		
		if (editing) {
			featureList = tsbControl.getFeatureList();
			feSettingsMap = tsbControl.getFEParamsMap();
			int rowIndex = tsbControl.getTabPanel().getPanel().getSubsetTable().getSelectedRow();
			if (rowIndex > -1) {
				String currID = (String) tsbControl.getTabPanel().getPanel().getSubsetTable().getValueAt(rowIndex, 0);
				TSBSubset currSubset = null;
				for (int i = 0; i < tsbControl.subsetList.size(); i++) {
					if (currID.equals(tsbControl.subsetList.get(i).id)) {
						currSubset = tsbControl.subsetList.get(i);
						break;
					}
				}
				if (currSubset != null) {
					this.oldID = currID;
					if (currSubset.featurePath.length() > 0 && currSubset.wmatPath.length() > 0) {
						reloadButton.setEnabled(true);
					}
					featuresCSVField.setText(currSubset.featurePath);
					wmatCSVField.setText(currSubset.wmatPath);
					idDigit1Box.setSelectedItem(currSubset.id.substring(0, 1));
					idDigit2Box.setSelectedItem(currSubset.id.substring(1));
					locationField.setText(currSubset.location);
					startField.setText(currSubset.start);
					endField.setText(currSubset.end);
					classList = new ArrayList<String>(currSubset.classList);
					validEntriesList = new ArrayList<ArrayList<TSBDetection>>(currSubset.validEntriesList);
					DefaultListModel dlm = new DefaultListModel();
					for (int i = 0; i < classList.size(); i++) {
						dlm.addElement(classList.get(i));
					}
					checkList.setModel(dlm);
					checkList.setSelectedIndices(currSubset.selectionArray);
					updateFields();
				}
			}
		}
	}
	
	/**
	 * Action listener for loading files.
	 * @param action
	 * <br>0 == loading in a .mfe file
	 * <br>1 == loading in a .wmat file
	 * <br>2 == reloading both if both are already selected
	 */
	protected class FileListener implements ActionListener {
		public static final int LOAD_FE = 0;
		public static final int LOAD_WMAT = 1;
		public static final int RELOAD = 2;
		
		protected int action;
		
		protected FileListener(int action) {
			this.action = action;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (action == LOAD_FE || action == LOAD_WMAT) {
				PamFileChooser fc = new PamFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setMultiSelectionEnabled(false);
				if (action == LOAD_FE) {
					fc.addChoosableFileFilter(new FileNameExtensionFilter("MIRFEE feature vector data file (*.mfe)","mirrffe","mfe"));
					//fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
				} else {
					fc.addChoosableFileFilter(new FileNameExtensionFilter("WMAT table export file (*.wmat)","wmat","wmnt"));
					//fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
				}
				int returnVal = fc.showOpenDialog(parentFrame);
				if (returnVal != fc.APPROVE_OPTION) return;
				File f = getSelectedFileWithExtension(fc);
				if (!f.exists()) {
					tsbControl.simpleErrorDialog("Selected file does not exist.", 150);
					return;
				}
				boolean succeeded;
				if (action == LOAD_FE) succeeded = loadFE(f);
				else succeeded = loadWMAT(f);
				if (succeeded) mergeFEandWMAT();
			} else {
				if (featuresCSVField.getText().length() == 0 || wmatCSVField.getText().length() == 0) {
					//reloadButton.setEnabled(false);
					tsbControl.simpleErrorDialog("Both feature data and WMAT data files need to be selected.", 150);
					return;
				}
				if (!loadFE(new File(featuresCSVField.getText()))) return;
				if (!loadWMAT(new File (wmatCSVField.getText()))) return;
				mergeFEandWMAT();
			}
		}
	}
	
	/**
	 * Attempts to read a .mfe file and stores the data for merging with .wmat data.
	 * @param f - The input file
	 * @return True if the file was valid. Otherwise, false.
	 */
	protected boolean loadFE(File f) {
		HashMap<String, String> outpSettingsMap = new HashMap<String, String>();
		ArrayList<String> outpFeatures = new ArrayList<String>();
		ArrayList<String[]> outpFEList = new ArrayList<String[]>();
		Scanner sc;
		try {
			sc = new Scanner(f);
			if (!sc.hasNextLine()) {
				sc.close();
				tsbControl.simpleErrorDialog("Selected file is blank.", 150);
				return false;
			}
			String nextLine = sc.nextLine();
			if (!nextLine.equals("EXTRACTOR PARAMS START")) {
				sc.close();
				tsbControl.simpleErrorDialog("Selected file does not contain properly-formatted "
						+ "Feature Extractor settings info.", 150);
				return false;
			}
			boolean ended = false;
			while (sc.hasNextLine()) {
				nextLine = sc.nextLine();
				if (nextLine.equals("EXTRACTOR PARAMS END")) {
					if (!sc.hasNextLine()) {
						sc.close();
						tsbControl.simpleErrorDialog("Selected file does not contain properly-formatted "
								+ "Feature Extractor settings info.", 150);
						return false;
					}
					String featureTestLine = "cluster,uid,date,duration,lf,hf";
					nextLine = sc.nextLine();
					if (tsbControl.getSubsetList().size() > 0) {
						for (int i = 0; i < tsbControl.getFeatureList().size(); i++) 
							featureTestLine += ","+tsbControl.getFeatureList().get(i);
						//System.out.println(featureTestLine);
						//System.out.println(nextLine);
						if (!nextLine.equals(featureTestLine)) {
							sc.close();
							String message = "Selected file's features do not match those of subsets loaded into the table.\n";
							String[] nextSplit = nextLine.split(",");
							String[] fileFeatures = new String[nextSplit.length-6];
							for (int i = 0; i < fileFeatures.length; i++)
								fileFeatures[i] = nextSplit[i+6];
							int longer = fileFeatures.length;
							int shorter = tsbControl.getFeatureList().size();
							if (longer < shorter) {
								longer = tsbControl.getFeatureList().size();
								shorter = fileFeatures.length;
							}
							for (int i = 0; i < longer; i++) {
								if (i < shorter) {
									if (!fileFeatures[i].equals(tsbControl.getFeatureList().get(i)))
										message += "\n"+fileFeatures[i]+" -> "+tsbControl.getFeatureList().get(i);
								} else {
									if (longer == fileFeatures.length)
										message += "\n"+fileFeatures[i]+" -> (none)";
									else
										message += "\n(none) -> "+tsbControl.getFeatureList().get(i);
								}
							}
							tsbControl.simpleErrorDialog(message, 300);
							return false;
						}
						outpFeatures = tsbControl.getFeatureList();
					} else {
						if (!nextLine.startsWith(featureTestLine+",")) {
							sc.close();
							tsbControl.simpleErrorDialog("Selected file not formatted like Feature Extractor output.", 150);
							return false;
						}
						String[] nextSplit = nextLine.split(",");
						for (int i = 6; i < nextSplit.length; i++) outpFeatures.add(nextSplit[i]);
					}
					ended = true;
					break;
				}
				String[] nextSplit = nextLine.split("=");
				if (nextSplit.length < 2) continue;
				outpSettingsMap.put(nextSplit[0], nextSplit[1]);
			}
			if (!ended) {
				sc.close();
				tsbControl.simpleErrorDialog("Selected file not formatted like Feature Extractor output.", 150);
				return false;
			}
			if (tsbControl.getSubsetList().size() > 0) {
				FEParameters placeholderParams = new FEParameters();
				ArrayList<String> unmatched = placeholderParams.findUnmatchedParameters(tsbControl.getFEParamsMap(), outpSettingsMap, false);
				if (unmatched.size() > 0) {
					sc.close();
					tsbControl.simpleErrorDialog("Selected file's Feature Extractor settings do not match those of "
							+ "subsets loaded into the table.", 150);
					return false;
				}
			}
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
			while (sc.hasNextLine()) {
				String[] nextSplit = sc.nextLine().split(",");
				try {
					Long.valueOf(nextSplit[1]);
					df.parse(nextSplit[2]);
					for (int i = 3; i < 6+outpFeatures.size(); i++) {
						nextSplit[i] = String.valueOf(Double.valueOf(nextSplit[i]));
					}
					outpFEList.add(nextSplit);
				} catch (Exception e2){
					// TODO
				}
			}
			sc.close();
		} catch (Exception e2) {
			e2.printStackTrace();
			tsbControl.simpleErrorDialog("Error occured while attempting to parse selected file", 150);
			return false;
		}
		if (outpFEList.size() == 0) {
			tsbControl.simpleErrorDialog("Selected file contained no valid entries.", 150);
			return false;
		}
		featuresEntriesList = outpFEList;
		featuresCSVField.setText(f.getPath());
	/*	if (tsbControl.getFeatureList().size() > 0) {
			featureList = new ArrayList<String>(tsbControl.getFeatureList());
		} else {
			featureList = new ArrayList<String>(outpFeatures);
		} */
		featureList = new ArrayList<String>(outpFeatures);
		feSettingsMap = outpSettingsMap;
		return true;
	}
	
	/**
	 * Attempts to read a .wmat file and stores the data for merging with .mfe data.
	 * @param f - The input file
	 * @return True if the file was valid. Otherwise, false.
	 */
	protected boolean loadWMAT(File f) {
		Scanner sc;
		try {
			sc = new Scanner(f);
			if (!sc.hasNextLine()) {
				sc.close();
				tsbControl.simpleErrorDialog("Selected file is blank.", 150);
				return false;
			}
			ArrayList<String[]> outpWMATList = new ArrayList<String[]>();
			String firstLine = sc.nextLine();
			if (!firstLine.startsWith("uid,datetime,lf,hf,duration,amplitude,species,calltype,comment")) {
				sc.close();
				tsbControl.simpleErrorDialog("Selected file not formatted like WMAT output.", 150);
				return false;
			}
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			while (sc.hasNextLine()) {
				String[] nextSplit = sc.nextLine().split(",");
				try {
					Long.valueOf(nextSplit[0]);
					df.parse(nextSplit[1]);
					Integer.valueOf(nextSplit[2]);
					Integer.valueOf(nextSplit[3]);
					Integer.valueOf(nextSplit[4]);
					Integer.valueOf(nextSplit[5]);
					if (tsbControl.getParams().includeCallType && nextSplit.length > 7) {
						if (nextSplit[6].length() > 0 || nextSplit[7].length() > 0) {
							outpWMATList.add(nextSplit);
						}
					} else {
						if (nextSplit[6].length() > 0) {
							outpWMATList.add(nextSplit);
						}
					}
				} catch (Exception e2){
					// TODO
				}
			}
			sc.close();
			if (outpWMATList.size() > 0) {
				wmatEntriesList = outpWMATList;
				wmatCSVField.setText(f.getPath());
			} else {
				tsbControl.simpleErrorDialog("Selected file contained no valid entries.", 150);
				return false;
			}
		} catch (Exception e2) {
			e2.printStackTrace();
			tsbControl.simpleErrorDialog("Error occured while attempting to parse selected file", 150);
			return false;
		}
		return true;
	}
	
	/**
	 * Merges input .mfe and .wmat data to create labelled vector data for training and/or testing.
	 */
	protected void mergeFEandWMAT() {
		if (featuresEntriesList.size() == 0 || wmatEntriesList.size() == 0) return;
		classList = new ArrayList<String>();
		for (int i = 0; i < wmatEntriesList.size(); i++) {
			String[] curr = wmatEntriesList.get(i);
			String className = "";
			if (tsbControl.getParams().includeCallType) {
				if (curr.length > 7) {
					if (curr[6].length() > 0 && curr[7].length() > 0) {
						className = curr[6]+" ("+curr[7]+")";
					} else if (curr[6].length() > 0) {
						className = curr[6];
					} else if (curr[7].length() > 0) {
						className = curr[7];
					}
				}
			} else {
				className = curr[6];
			}
			if (className.length() > 0 && !classList.contains(className)) {
				classList.add(className);
			}
			wmatEntriesList.get(i)[6] = className;
		}
		Collections.sort(classList);
		
		HashMap<String, String[]> feMap = new HashMap<String, String[]>();
		for (int i = 0; i < featuresEntriesList.size(); i++) {
			String[] entry = featuresEntriesList.get(i);
			feMap.put(entry[1]+", "+entry[2], entry);
		}
		
		validEntriesList = new ArrayList<ArrayList<TSBDetection>>();
		//boolean printFirst = true;
		for (int i = 0; i < classList.size(); i++) {
			ArrayList<TSBDetection> currDetectionList = new ArrayList<TSBDetection>();
			for (int j = 0; j < wmatEntriesList.size(); j++) {
				String[] currWMAT = wmatEntriesList.get(j);
				String className = "";
				if (tsbControl.getParams().includeCallType) {
					if (currWMAT.length > 7) {
						if (currWMAT[6].length() > 0 && currWMAT[7].length() > 0) {
							className = currWMAT[6]+" ("+currWMAT[7]+")";
						} else if (currWMAT[6].length() > 0) {
							className = currWMAT[6];
						} else if (currWMAT[7].length() > 0) {
							className = currWMAT[7];
						}
					}
				} else {
					className = currWMAT[6];
				}
				if (className.equals(classList.get(i))) {
					// Rookie use of O(n^2) below - my bad!!!!
				/*	for (int k = 0; k < featuresEntriesList.size(); k++) {
						String[] currFE = featuresEntriesList.get(k);
						if (currWMNT[0].equals(currFE[1]) && currWMNT[1].equals(currFE[2])) {
							try {
								TSBDetection outp = new TSBDetection(tsbControl, featureList.size(),
										currFE[0], currWMNT, Arrays.copyOfRange(currFE, 6, currFE.length));
								currDetectionList.add(outp);
							} catch (AssertionError | Exception e2) {
								e2.printStackTrace();
							}
							break;
						}
					} */
					String[] currFE = feMap.get(currWMAT[0]+", "+currWMAT[1]);
					if (currFE == null) continue;
					try {
						TSBDetection outp = new TSBDetection(tsbControl, featureList.size(),
								currFE[0], currWMAT, Arrays.copyOfRange(currFE, 6, currFE.length));
						currDetectionList.add(outp);
					} catch (AssertionError | Exception e2) {
						e2.printStackTrace();
					}
				}
			}
			if (currDetectionList.size() > 0) {
				Collections.sort(currDetectionList, Comparator.comparingLong(TSBDetection::getDateTimeAsLong));
				validEntriesList.add(currDetectionList);
			} else {
				classList.remove(i);
				i--;
			}
		}
		checkList.removeAll();
		DefaultListModel dlm = new DefaultListModel();
		for (int i = 0; i < classList.size(); i++) {
			dlm.addElement(classList.get(i));
		}
		checkList.setModel(dlm);
		for (int i = 0; i < checkList.getModel().getSize(); i++) {
			if (tsbControl.getFullClassList().contains(checkList.getModel().getElementAt(i))) {
				checkList.setSelectedIndex(i);
			}
		}
		updateFields();
		if (validEntriesList.size() > 0) {
			reloadButton.setEnabled(true);
		} else {
			tsbControl.simpleErrorDialog("Selected files contain no matching valid data.", 150);
		}
	}
	
	/**
	 * Updates text fields for dates and detection counts when data is loaded in.
	 */
	public void updateFields() {
		String startDate = "";
		String endDate = "";
		int total = 0;
		for (int i = 0; i < checkList.getModel().getSize(); i++) {
			if (checkList.isSelectedIndex(i)) {
				ArrayList<TSBDetection> currList = validEntriesList.get(i);
				if (currList.size() > 0 && (startDate.length() == 0 || currList.get(0).datetime.compareTo(startDate) < 0)) {
					startDate = currList.get(0).datetime;
				}
				if (currList.size() > 0 && (endDate.length() == 0 || currList.get(currList.size()-1).datetime.compareTo(endDate) > 0)) {
					endDate = currList.get(currList.size()-1).datetime;
				}
				total += currList.size();
			}
		}
		startField.setText(startDate);
		endField.setText(endDate);
		totalField.setText(String.valueOf(total));
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

	@Override
	public boolean getParams() {
		if (checkList.getSelectedIndices().length == 0) {
			tsbControl.simpleErrorDialog("No classes have been selected.", 150);
			return false;
		}
		TSBSubset outp = new TSBSubset();
		outp.id = idDigit1Box.getItemAt(idDigit1Box.getSelectedIndex());
		outp.id += idDigit2Box.getItemAt(idDigit2Box.getSelectedIndex());
		ArrayList<TSBSubset> sl = tsbControl.getSubsetList();
		for (int i = 0; i < sl.size(); i++) {
			if (sl.get(i).id.equals(outp.id) && !outp.id.equals(oldID)) {
				tsbControl.simpleErrorDialog("Selected ID has already been taken.", 150);
				return false;
			}
		}
		outp.featurePath = featuresCSVField.getText();
		outp.wmatPath = wmatCSVField.getText();
		outp.location = locationField.getText();
		outp.start = startField.getText();
		outp.end = endField.getText();
		outp.classList = classList;
		outp.selectionArray = checkList.getSelectedIndices();
		outp.validEntriesList = validEntriesList;
		boolean classListChanged = false;
		if (editing) {
			for (int i = 0; i < tsbControl.getSubsetList().size(); i++) {
				if (oldID.equals(tsbControl.getSubsetList().get(i).id)) {
					tsbControl.getSubsetList().set(i, outp);
				}
			}
		} else {
			tsbControl.getSubsetList().add(outp);
		}
		ArrayList<String> outpClassList = tsbControl.getFullClassList();
		HashMap<String, String> outpMap = tsbControl.getClassMap();
		for (int i = 0; i < outp.selectionArray.length; i++) {
			if (!outpClassList.contains(classList.get(outp.selectionArray[i]))) {
				outpClassList.add(classList.get(outp.selectionArray[i]));
				tsbControl.getClassMap().put(classList.get(outp.selectionArray[i]), classList.get(outp.selectionArray[i]));
				classListChanged = true;
			}
		}
		if (classListChanged) {
			String[] columnNames = new String[5 + outpClassList.size()];
			columnNames[0] = "ID";
			columnNames[1] = "Location";
			columnNames[2] = "Start";
			columnNames[3] = "End";
			columnNames[4] = "Total";
			for(int i = 0; i < outpClassList.size(); i++) {
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
			DefaultTableModel subsetTableModel = new DefaultTableModel(columnNames,0) {
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
			int rowCount = tsbControl.getTabPanel().getPanel().subsetTable.getRowCount();
			if (!editing) {
				rowCount++;
			}
			for (int i = 0; i < rowCount; i++) {
				TSBSubset curr = null;
				if (editing || (!editing && i < rowCount-1)) {
					String currID = (String) tsbControl.getTabPanel().getPanel().subsetTable.getValueAt(i, 0);
					for (int j = 0; j < tsbControl.getSubsetList().size(); j++) {
						if (currID.equals(tsbControl.getSubsetList().get(j).id) || currID.equals(oldID)) {
							curr = tsbControl.getSubsetList().get(j);
							break;
						}
					}
				} else {
					curr = outp;
				}
				if (curr != null) {
					Object[] row = new Object[columnNames.length];
					row[0] = curr.id;
					row[1] = curr.location;
					row[2] = curr.start;
					row[3] = curr.end;
					int currTotal = 0;
					for (int j = 5; j < columnNames.length; j++) {
						int classTotal = 0;
						for (int k = 0; k < curr.selectionArray.length; k++) {
							if (columnNames[j].equals(curr.classList.get(curr.selectionArray[k]))) {
								classTotal = curr.validEntriesList.get(curr.selectionArray[k]).size();
								currTotal += classTotal;
								break;
							}
						}
						row[j] = classTotal;
					}
					row[4] = currTotal;
					rowList.add(row);
				}
			}
			tsbControl.getTabPanel().getPanel().setTableModel(subsetTableModel);
			tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnModel().getColumn(0).setPreferredWidth(30);
			tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnModel().getColumn(1).setPreferredWidth(100);
			tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnModel().getColumn(2).setPreferredWidth(150);
			tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnModel().getColumn(3).setPreferredWidth(150);
			for (int i = 4; i < tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnCount(); i++) {
				tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnModel().getColumn(i).setPreferredWidth(50);
			}
			for (int i = 0; i < rowList.size(); i++) {
				tsbControl.getTabPanel().getPanel().subsetTableModel.addRow(rowList.get(i));
			}
		} else {
			Object[] row = new Object[tsbControl.getTabPanel().getPanel().subsetTable.getColumnCount()];
			row[0] = outp.id;
			row[1] = outp.location;
			row[2] = outp.start;
			row[3] = outp.end;
			int currTotal = 0;
			for (int j = 5; j < row.length; j++) {
				int classTotal = 0;
				for (int k = 0; k < outp.selectionArray.length; k++) {
					if (tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnName(j).equals(outp.classList.get(outp.selectionArray[k]))) {
						classTotal = outp.validEntriesList.get(outp.selectionArray[k]).size();
						currTotal += classTotal;
						break;
					}
				}
				row[j] = classTotal;
			}
			row[4] = currTotal;
			if (editing) {
				for (int i = 0; i < tsbControl.getTabPanel().getPanel().subsetTable.getColumnCount(); i++) {
					tsbControl.getTabPanel().getPanel().subsetTable.setValueAt(row[i],
							tsbControl.getTabPanel().getPanel().subsetTable.getSelectedRow(), i);
				}
			} else {
				tsbControl.getTabPanel().getPanel().subsetTableModel.addRow(row);
			}
		}
		tsbControl.setFullClassList(outpClassList);
		tsbControl.setClassMap(outpMap);
		//if (tsbControl.getTabPanel().getPanel().subsetTable.getRowCount() < 1)
		tsbControl.setFeatureList(featureList);
		tsbControl.setFEParamsMap(feSettingsMap);
		tsbControl.getTabPanel().getPanel().clearButton.setEnabled(true);
		tsbControl.getTabPanel().getPanel().saveButton.setEnabled(true);
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {}
	
}