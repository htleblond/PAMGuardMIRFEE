package mirfeeTestClassifier;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.dialog.PamGridBagContraints;
import PamView.dialog.SourcePanel;
import mirfeeFeatureExtractor.FEDataUnit;
import mirfeeLiveClassifier.LCSettingsDialog;
import mirfeeLiveClassifier.LCTrainingSetInfo;

/**
 * The settings dialog for the Test Classifier.
 * Subclass of the Live Classifier's settings dialog.
 * @author Holly LeBlond
 */
public class TCSettingsDialog extends LCSettingsDialog {
	
	protected JTextField testSetField;
	protected JButton testSetButton;
	protected ButtonGroup validationRBG;
	protected JRadioButton leaveOneOutRB;
	protected JCheckBox firstDigitCheck;
	protected JRadioButton kFoldRB;
	protected JRadioButton testSubsetRB;
	protected JRadioButton labelledRB;
	protected JRadioButton unlabelledRB;
	protected JTextField kFoldField;
	protected JComboBox<String> testSubsetBox;
	
	protected LCTrainingSetInfo loadedTestingSet;
	
	public TCSettingsDialog(Window parentFrame, TCControl tcControl) {
		super(parentFrame, tcControl);
		if (manageAliasesButton != null)
			manageAliasesButton.setVisible(false);
	}
	
	@Override
	protected TCControl getControl() {
		return (TCControl) lcControl;
	}
	
	@Override
	protected void init() {
		super.init();
		loadedTestingSet = getControl().getParams().getTestingSetInfo();
	}
	
	@Override
	protected JPanel createInputPanel() {
		// inputSourcePanel is never used here, I'm only initializing it to prevent a nullPointerException in actuallyGetParams().
		inputSourcePanel = new SourcePanel(this, "Data source", FEDataUnit.class, false, true);
		
		JPanel outp = new JPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		b.fill = GridBagConstraints.HORIZONTAL;
		b.anchor = GridBagConstraints.NORTH;
		
		JPanel trainingSetPanel = new JPanel(new GridBagLayout());
		trainingSetPanel.setBorder(new TitledBorder("Training set"));
		trainSetField = new JTextField(20);
		trainSetField.setMinimumSize(new Dimension(trainSetField.getPreferredSize().width, trainSetField.getHeight()));
		trainSetField.setEnabled(false);
		trainSetField.setText(getControl().getParams().getTrainPath());
		GridBagConstraints c = new PamGridBagContraints();
		trainingSetPanel.add(trainSetField, c);
		c.gridx++;
		trainSetButton = new JButton("Select file");
		trainSetButton.addActionListener(new TrainSetListener(false));
		trainingSetPanel.add(trainSetButton, c);
		outp.add(trainingSetPanel, b);
		
		JPanel testingSetPanel = new JPanel(new GridBagLayout());
		testingSetPanel.setBorder(new TitledBorder("Testing set"));
		testSetField = new JTextField(20);
		testSetField.setMinimumSize(new Dimension(testSetField.getPreferredSize().width, testSetField.getHeight()));
		testSetField.setEnabled(false);
		testSetField.setText(getControl().getParams().getTestPath());
		c = new PamGridBagContraints();
		testingSetPanel.add(testSetField, c);
		c.gridx++;
		testSetButton = new JButton("Select file");
		testSetButton.addActionListener(new TrainSetListener(true));
		testingSetPanel.add(testSetButton, c);
		b.gridy++;
		outp.add(testingSetPanel, b);
		
		JPanel validationPanel = new JPanel(new GridBagLayout());
		validationPanel.setBorder(new TitledBorder("Validation"));
		c = new PamGridBagContraints();
		leaveOneOutRB = new JRadioButton();
		leaveOneOutRB.setText("Leave-one-subset-out cross-validation on training set");
		leaveOneOutRB.addActionListener(new ValidationRBListener(ValidationRBListener.LEAVEONEOUT));
		c.gridwidth = 4;
		c.anchor = c.WEST;
		c.fill = c.NONE;
		validationPanel.add(leaveOneOutRB, c);
		c.gridy++;
		c.gridx = 1;
		c.gridwidth = 3;
		firstDigitCheck = new JCheckBox("Split by first subset ID digit only");
		validationPanel.add(firstDigitCheck, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		kFoldRB = new JRadioButton();
		kFoldRB.addActionListener(new ValidationRBListener(ValidationRBListener.KFOLD));
		validationPanel.add(kFoldRB, c);
		c.gridx++;
		kFoldField = new JTextField(4);
		kFoldField.setDocument(JIntFilter());
		validationPanel.add(kFoldField, c);
		c.gridx++;
		c.gridwidth = 2;
		validationPanel.add(new JLabel("-fold cross-validation on training set"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		testSubsetRB = new JRadioButton();
		testSubsetRB.setText("Test subset within training set:");
		testSubsetRB.addActionListener(new ValidationRBListener(ValidationRBListener.TESTSUBSET));
		validationPanel.add(testSubsetRB, c);
		c.gridx += 3;
		c.gridwidth = 1;
		testSubsetBox = new JComboBox<String>();
		fillTestSubsetBox();
		validationPanel.add(testSubsetBox, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		labelledRB = new JRadioButton();
		labelledRB.setText("Use labelled .mtsf file as testing set");
		labelledRB.addActionListener(new ValidationRBListener(ValidationRBListener.TESTSET));
		validationPanel.add(labelledRB, c);
		c.gridy++;
		unlabelledRB = new JRadioButton();
		unlabelledRB.setText("Use unlabelled .mfe file as testing set (TBA)");
		unlabelledRB.addActionListener(new ValidationRBListener(ValidationRBListener.TESTSET));
		unlabelledRB.setEnabled(false);
		validationPanel.add(unlabelledRB, c);
		validationRBG = new ButtonGroup();
		validationRBG.add(leaveOneOutRB);
		validationRBG.add(kFoldRB);
		validationRBG.add(testSubsetRB);
		validationRBG.add(labelledRB);
		validationRBG.add(unlabelledRB);
		b.gridy++;
		outp.add(validationPanel, b);
		
		return outp;
	}
	
	@Override
	protected boolean compareFEFeatures(ArrayList<String> inp, boolean testSet) {
		if (!testSet) return true;
		if (loadedTrainingSet.featureList.size() != inp.size()) return false;
		for (int i = 0; i < inp.size(); i++) {
			if (!loadedTrainingSet.featureList.get(i).equals(inp.get(i))) {
				lcControl.simpleErrorDialog("Features found in testing set do not match those "
						+ "found in training set.", 250);
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected boolean compareFEParams(HashMap<String, String> inp, boolean testSet) {
		if (!testSet) return true;
		HashMap<String, String> currMap = loadedTrainingSet.feParamsMap;
		Iterator<String> it = currMap.keySet().iterator();
		while (it.hasNext()) {
			String nextKey = it.next();
			if (!inp.containsKey(nextKey) || !currMap.get(nextKey).equals(inp.get(nextKey))) {
				int res = JOptionPane.showConfirmDialog(this,
						getControl().makeHTML("The selected set contains Feature Extractor settings information that "
								+ "does not match those found in the training set. "
								+ "It can still be used (as long as the features match), but it will likely "
								+ "not produce optimal results. Proceed?", 350),
						lcControl.getUnitName(),
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null);
				return res == JOptionPane.OK_OPTION;
			}
		}
		return true;
	}
	
	/**
	 * Fills testSubsetBox with options.
	 */
	protected void fillTestSubsetBox() {
		testSubsetBox.removeAllItems();
		if (loadedTrainingSet == null) return;
		ArrayList<String> subsetList = loadedTrainingSet.getSortedSubsetList();
		ArrayList<String> outp = new ArrayList<String>();
		for (int i = 0; i < subsetList.size(); i++) {
			String firstDigit = subsetList.get(i).substring(0, 1);
			if (!outp.contains("All from "+firstDigit)) outp.add("All from "+firstDigit);
		}
		for (int i = 0; i < subsetList.size(); i++) {
			if (!outp.contains(subsetList.get(i))) outp.add(subsetList.get(i));
		}
		for (int i = 0; i < outp.size(); i++) testSubsetBox.addItem(outp.get(i));
		String currID = getControl().getParams().testSubset;
		if (currID.length() == 1) testSubsetBox.setSelectedItem("All from "+currID);
		else if  (currID.length() == 2) testSubsetBox.setSelectedItem(currID);
	}
	
	/**
	 * The listener for the validation radio buttons.
	 */
	protected class ValidationRBListener implements ActionListener {
		public static final int LEAVEONEOUT = 0;
		public static final int KFOLD = 1;
		public static final int TESTSUBSET = 2;
		public static final int TESTSET = 3;
		private int selection;
		
		protected ValidationRBListener(int selection) {
			this.selection = selection;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			firstDigitCheck.setEnabled(selection == LEAVEONEOUT);
			testSetButton.setEnabled(selection == TESTSET);
			kFoldField.setEnabled(selection == KFOLD);
			testSubsetBox.setEnabled(selection == TESTSUBSET);
		}
	}
	
	protected class LabelNotFoundException extends Exception {}
	
	/**
	 * When using either leave-one-out or k-fold cross-validation, checks if all instances of a class are contained in a single subset.
	 * When testing a single subset, checks if all instances of a class are contained in the selected subset.
	 * If either of these are the case, classification cannot proceed, as all training set instances must contains all classes.
	 * @param curr - The currently loaded training set.
	 * @param params - The new set of parameters.
	 * @return False if there's an instance of a training set missing a class, or if another error occurs. Otherwise, true.
	 */
	public boolean checkClassSpread(LCTrainingSetInfo curr, TCParameters params) {
		//if (kFold) testSubset = false; // kFold overrides testSubset.
		if (params.validation == params.TESTSUBSET && testSubsetBox.getItemCount() == 0) {
			getControl().simpleErrorDialog("Invalid testing subset selected.", 250);
			return false;
		}
		//TCParameters params = generateParameters();
		HashMap<String, boolean[]> containMap = new HashMap<String, boolean[]>();
		File f = new File(curr.pathName);
		if (!f.exists()) {
			getControl().simpleErrorDialog("Selected training set apparently no longer exists.", 250);
			return false;
		}
		Scanner sc = null;
		try {
			sc = new Scanner(f);
			int numLines = -1;
			ArrayList<String> clusterList = new ArrayList<String>();
			if (!sc.hasNextLine()) {
				getControl().simpleErrorDialog("Selected training set is apparently now empty.", 250);
				sc.close();
				return false;
			}
			String nextLine = sc.nextLine();
			if (nextLine.equals("EXTRACTOR PARAMS START")) {
				while (sc.hasNextLine() && !sc.nextLine().equals("EXTRACTOR PARAMS END"));
				if (sc.hasNextLine()) sc.nextLine();
			}
			while (sc.hasNextLine()) {
				String[] nextSplit = sc.nextLine().split(",");
				if (nextSplit.length > 0 && !clusterList.contains(nextSplit[0]) && numLines > -1) clusterList.add(nextSplit[0]);
				numLines++;
			}
			sc.close();
			if (numLines <= 0) {
				getControl().simpleErrorDialog("Selected training set is apparently now empty.", 250);
				return false;
			}
			int kNum = params.kNum;
			if (params.validation == params.KFOLD && clusterList.size() < kNum) {
				getControl().simpleErrorDialog("k-fold number must be greater than the number of call clusters in the table.", 250);
				return false;
			}
			clusterList.sort(Comparator.naturalOrder());
			sc = new Scanner(f);
			nextLine = sc.nextLine();
			if (nextLine.equals("EXTRACTOR PARAMS START")) {
				while (sc.hasNextLine() && !sc.nextLine().equals("EXTRACTOR PARAMS END"));
				if (sc.hasNextLine()) sc.nextLine();
			}
			int lineNum = 0;
			ArrayList<String> labelList = params.getLabelOrderAsList();
			while (sc.hasNextLine()) {
				String[] nextSplit = sc.nextLine().split(",");
				if (nextSplit.length < 8+curr.featureList.size() || nextSplit[0].length() < 2) {
					continue;
				}
				String key = nextSplit[0].substring(0,2); //LEAVEONEOUTBOTHDIGITS
				if (params.validation == params.LEAVEONEOUTFIRSTDIGIT) {
					key = key.substring(0, 1);
				} else if (params.validation == params.KFOLD) {
					key = String.valueOf((int) Math.floor(kNum * (double) clusterList.indexOf(nextSplit[0]) / clusterList.size()));
				} else if (params.validation == params.TESTSUBSET) {
					String currID = (String) testSubsetBox.getSelectedItem();
					if (currID.length() == 1 && key.substring(0, 1).equals(currID)) continue;
					if (currID.equals(key)) continue;
				}
				if (!containMap.containsKey(key)) {
					boolean[] boolArr = new boolean[labelList.size()];
					for (int i = 0; i < labelList.size(); i++) boolArr[i] = false;
					containMap.put(key, boolArr);
				}
				containMap.get(key)[labelList.indexOf(nextSplit[7])] = true;
				lineNum++;
			}
			sc.close();
			if (params.validation == params.TESTSUBSET && containMap.size() == 0) {
				getControl().simpleErrorDialog("Training set must contain at least one other subset for training in order to "
						+ "test individual subsets.", 250);
				return false;
			}
			for (int i = 0; i < labelList.size(); i++) {
				int occursIn = 0;
				Iterator<String> it = containMap.keySet().iterator();
				while (it.hasNext()) {
					String next = it.next();
					if (containMap.get(next)[i]) occursIn++;
				}
				if (occursIn == 0) {
					if (params.validation == params.TESTSUBSET) {
						getControl().simpleErrorDialog("\""+labelList.get(i)+"\" only occurs in the selected "
								+ "testing subset. It must occur somewhere else in the set as well.", 250);
						return false;
					}
					else throw new LabelNotFoundException();
				}
				if (occursIn == 1 && params.validation <= 3) {
					if (params.validation == params.KFOLD) getControl().simpleErrorDialog("\""+labelList.get(i)+"\" only occurs in one fold. "
								+ "Try again with a new k-value or consider spreading the label "
								+ "out through the set more.", 250);
					else getControl().simpleErrorDialog("\""+labelList.get(i)+"\" only occurs in one subset. "
								+ "It must occur in multiple subsets in order for leave-one-out "
								+ "cross-validation to work.", 250);
					return false;
				}
			}
		} catch (FileNotFoundException e) {
			getControl().simpleErrorDialog("Selected training set apparently no longer exists.", 250);
			return false;
		} catch (LabelNotFoundException e2) {
			e2.printStackTrace();
			getControl().simpleErrorDialog("Label was not found in the set during the second scan. "
					+ "This should not happen and is entirely the developer's fault.", 250);
			return false;
		} catch (Exception e3) {
			sc.close();
			e3.printStackTrace();
			getControl().simpleErrorDialog("Could not read selected training set. It may not be formatted properly.", 250);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks if the training set is actually usable with the selected settings.
	 * @param inp - The loaded training set.
	 * @param readThroughFile - Whether or not the .mtsf file the training set came from should be read through again for changes.
	 * @param showLoadingDialogs - Whether or not loading dialogs should appear.
	 * @return True if the training set is valid. Otherwise, false.
	 */
	public boolean checkIfTrainingSetIsValid(LCTrainingSetInfo inp, boolean readThroughFile, boolean showLoadingDialogs) {
		TCParameters params = generateParameters();
		LCTrainingSetInfo curr = inp;
		if (readThroughFile) {
			curr = readTrainingSet(false, showLoadingDialogs, new File(loadedTrainingSet.pathName));
			if (curr == null) return false;
			if (!loadedTrainingSet.compare(curr)) {
				getControl().simpleErrorDialog("Training set appears to have changed. Re-select it and try again.", 250);
				return false;
			}
		}
		if (curr == null) {
			getControl().simpleErrorDialog("No training set has been selected.", 250);
			return false;
		}
		if (curr.labelCounts.keySet().size() <= 1) {
			getControl().simpleErrorDialog("Training set must contain at least two different class labels.", 250);
			return false;
		}
		if (curr.featureList.size() <= 1) {
			getControl().simpleErrorDialog("Training set must contain at least two features.", 250);
			return false;
		}
	/*	if (params.validation == params.LEAVEONEOUT) {
			if (!checkClassSpread(curr, false, false)) return false;
		} else if (params.validation == params.KFOLD) {
			if (!checkClassSpread(curr, true, false)) return false;
		} else if (params.validation == params.TESTSUBSET) {
			if (!checkClassSpread(curr, false, true)) return false;
		} */
		if (params.validation != params.LEAVEONEOUTBOTHDIGITS || params.validation != params.LEAVEONEOUTFIRSTDIGIT ||
				params.validation != params.KFOLD || params.validation != params.TESTSUBSET) {
			if (!checkClassSpread(curr, params)) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the testing set is actually usable with the selected settings and training set.
	 * @param inp - The loaded testing set.
	 * @param readThroughFile - Whether or not the .mtsf file the testing set came from should be read through again for changes.
	 * @param showLoadingDialogs - Whether or not loading dialogs should appear.
	 * @return True if the testing set is valid. Otherwise, false.
	 */
	public boolean checkIfTestingSetIsValid(LCTrainingSetInfo inp, boolean readThroughFile, boolean showLoadingDialogs) {
		TCParameters params = generateParameters();
		if (inp == null || inp.pathName.length() == 0) {
			getControl().simpleErrorDialog("No testing set has been selected.", 250);
			return false;
		}
		if (loadedTrainingSet == null || loadedTrainingSet.pathName.length() == 0) {
			getControl().simpleErrorDialog("Training set needs to be selected first.", 250);
			return false;
		}
		LCTrainingSetInfo curr = inp;
		if (readThroughFile) curr = readTrainingSet(true, showLoadingDialogs, new File(loadedTestingSet.pathName));
		if (curr == null) return false;
		if (curr.pathName.equals(loadedTrainingSet.pathName)) {
			getControl().simpleErrorDialog("Training and testing sets cannot be the same file.", 250);
			return false;
		}
		if (curr.featureList.size() != loadedTrainingSet.featureList.size()) {
			getControl().simpleErrorDialog("Features between training and testing sets are not the same.", 250);
			return false;
		}
		for (int i = 0; i < curr.featureList.size(); i++) {
			if (!curr.featureList.get(i).equals(loadedTrainingSet.featureList.get(i))) {
				getControl().simpleErrorDialog("Features between training and testing sets are not the same.", 250);
				return false;
			}
		}
		int matchingClasses = 0;
		int mismatchedClasses = 0;
		Iterator<String> it = curr.labelCounts.keySet().iterator();
		while (it.hasNext()) {
			String currLabel = it.next();
			if (loadedTrainingSet.labelCounts.keySet().contains(currLabel)) {
				matchingClasses++;
			} else {
				mismatchedClasses++;
			}
		}
		if (matchingClasses == 0) {
			getControl().simpleErrorDialog("Testing set shares no class labels with training set.", 250);
			return false;
		}
		if (mismatchedClasses > 0) {
			int result = JOptionPane.showConfirmDialog(this, 
					getControl().makeHTML("Testing set contains class labels that aren't present in the "
							+ "training set. These will be ignored. Proceed?", 300),
					getControl().getUnitName(),
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.NO_OPTION) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void trainSetButtonThreadAction(boolean testSet) {
		LCTrainingSetInfo tsi = readTrainingSet(testSet, true);
		if (tsi == null) {
			if (wdThread != null) wdThread.halt();
			return;
		}
		if (testSet) {
			if (checkIfTestingSetIsValid(tsi, false, true)) {
				loadedTestingSet = tsi;
				testSetField.setText(tsi.pathName);
			}
		} else {
			loadedTrainingSet = tsi;
			trainSetField.setText(tsi.pathName);
			fillTestSubsetBox();
			loadedTestingSet = getControl().getParams().getTestingSetInfo();
			testSetField.setText(getControl().getParams().getTestPath());
			updateLabelList(tsi);
		}
		if (wdThread != null) wdThread.halt();
	}
	
	@Override
	public void actuallyGetParams() {
		super.actuallyGetParams();
		TCParameters params = getControl().getParams();
		if (params.validation == params.LEAVEONEOUTBOTHDIGITS || params.validation == params.LEAVEONEOUTFIRSTDIGIT) {
			leaveOneOutRB.doClick();
			firstDigitCheck.setSelected(params.validation == params.LEAVEONEOUTFIRSTDIGIT);
		}
		else if (params.validation == params.KFOLD) kFoldRB.doClick();
		else if (params.validation == params.TESTSUBSET) testSubsetRB.doClick();
		else if (params.validation == params.LABELLED) labelledRB.doClick();
		else if (params.validation == params.UNLABELLED) unlabelledRB.doClick();
		kFoldField.setText(String.valueOf(params.kNum));
	}
	
	@Override
	protected boolean checkIfSettingsAreValid() {
		if (!super.checkIfSettingsAreValid()) return false;
		if (kFoldRB.isSelected() && (kFoldField.getText().length() == 0 || Integer.valueOf(kFoldField.getText()) < 2)) return false;
		if ((labelledRB.isSelected() || unlabelledRB.isSelected()) && !checkIfTestingSetIsValid(loadedTestingSet, true, true)) return false;
		if (testSubsetRB.isSelected() && testSubsetBox.getItemCount() == 0) return false;
		return true;
	}
	
	@Override
	public TCParameters generateParameters() {
		TCParameters params = (TCParameters) super.generateParameters();
		if (leaveOneOutRB.isSelected()) {
			if (firstDigitCheck.isSelected()) params.validation = params.LEAVEONEOUTFIRSTDIGIT;
			else params.validation = params.LEAVEONEOUTBOTHDIGITS;
		} else if (kFoldRB.isSelected()) {
			params.validation = params.KFOLD;
			params.kNum = Integer.valueOf(kFoldField.getText());
		} else if (testSubsetRB.isSelected()) {
			params.validation = params.TESTSUBSET;
			String testSubset = (String) testSubsetBox.getSelectedItem();
			if (testSubset.contains("All from ")) params.testSubset = testSubset.substring(9);
			else params.testSubset = testSubset;
		} else if (labelledRB.isSelected()) {
			params.validation = params.LABELLED;
		} else if (unlabelledRB.isSelected()) {
			params.validation = params.UNLABELLED;
		}
		return params;
	}
	
	@Override
	protected boolean validateTrainingSet() {
		if (!checkIfSettingsAreValid()) {
			if (wdThread != null) wdThread.halt();
			return false;
		}
		getControl().setTrainingSetStatus(false);
		TCParameters params = generateParameters();
        params.setTrainingSetInfo(loadedTrainingSet);
        if (labelledRB.isSelected() || unlabelledRB.isSelected()) {
			params.setTestingSetInfo(loadedTestingSet);
		}
        
		// TODO Set GUI signal ?????
        getControl().setParams(params);
        getControl().getTabPanel().getPanel().createMatrices(params.labelOrder, false);
        String pyParams = getControl().getParams().outputPythonParamsToText();
        if (pyParams.length() > 0) {
            getControl().getThreadManager().addCommand("txtParams = "+pyParams);
        } else {
        	getControl().getThreadManager().addCommand("txtParams = []");
        }
    	if (wdThread != null) wdThread.halt();
    	return true;
	}
	
	/**
	 * Note that the OK button starts a thread a runs validateTrainingSet() before reaching this.
	 */
	@Override
	public boolean getParams() {
		return true;
	}
	
	@Override
	public void restoreDefaultSettings() {
		super.restoreDefaultSettings();
		leaveOneOutRB.doClick();
		kFoldField.setText("10");
	}
}