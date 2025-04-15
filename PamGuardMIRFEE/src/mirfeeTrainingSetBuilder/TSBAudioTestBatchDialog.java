package mirfeeTrainingSetBuilder;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.TimeZone;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.codehaus.plexus.util.FileUtils;

import Acquisition.filedate.StandardFileDate;
import Acquisition.filedate.StandardFileDateSettings;
import PamUtils.PamFileChooser;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import mirfee.MIRFEEControlledUnit;

/**
 * Dialog for generating smaller sets of audio (where detections occur) for faster testing.
 * @author Holly LeBlond
 */
public class TSBAudioTestBatchDialog extends PamDialog {
	
	protected TSBControl tsbControl;
	protected Window parentFrame;
	public boolean useLoaded;
	
	protected JTextField dataFileField;
	protected JButton dataFieldButton;
	protected JTextField audioFolderField;
	protected JButton audioFolderButton;
	protected JTextField outputFolderField;
	protected JButton outputFolderButton;
	//protected JComboBox<String> tzBox;
	//protected JCheckBox dstCheck;
	protected JComboBox<String> subsetBox;
	protected JComboBox<String> priorityBox;
	protected JTextField minimumField;
	protected JCheckBox maximumCheck;
	protected JTextField maximumField;
	//protected JCheckBox minimumClassCheck;
	//protected JTextField minimumClassField;
	protected JCheckBox maximumClassCheck;
	protected JTextField maximumClassField;
	protected JCheckBox ignoreBlankCheck;
	protected JCheckBox ignore2SGCheck;
	protected JCheckBox ignoreFPCheck;
	protected JCheckBox ignoreUnkCheck;
	
	protected ArrayList<File> dataFileList;
	protected ArrayList<File> audioFolderList;
	//protected ArrayList<WavObject> wavList;
	
	protected volatile boolean interrupted;
	
	public TSBAudioTestBatchDialog(Window parentFrame, TSBControl tsbControl, boolean useLoaded) {
		super(parentFrame, tsbControl.getUnitName(), false);
		this.tsbControl = tsbControl;
		this.parentFrame = parentFrame;
		this.useLoaded = useLoaded;
		this.interrupted = false;
		
		this.getOkButton().setText("Generate");
		this.getOkButton().removeActionListener(this.getOkButton().getActionListeners()[0]);
		this.getOkButton().addActionListener(new GenerateButtonListener());
		this.dataFileList = new ArrayList<File>();
		this.audioFolderList = new ArrayList<File>();

		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("Audio Test Batch Generator"));
		GridBagConstraints b = new PamGridBagContraints();
		b.anchor = b.NORTHWEST;
		b.fill = b.HORIZONTAL;
		
		JPanel p1 = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.anchor = c.WEST;
		c.fill = c.HORIZONTAL;
		if (!useLoaded) {
			p1.add(new JLabel("Data file(s):"), c);
			c.gridy++;
			c.gridx = 0;
			dataFileField = new JTextField(30);
			dataFileField.setEnabled(false);
			dataFileField.setText("No files have been selected.");
			p1.add(dataFileField, c);
			c.gridx += c.gridwidth;
			dataFieldButton = new JButton("Select");
			dataFieldButton.addActionListener(new DataFileListener());
			p1.add(dataFieldButton, c);
			c.gridy++;
			c.gridx = 0;
		}
		p1.add(new JLabel("Audio folder(s):"), c);
		c.gridy++;
		c.gridx = 0;
		audioFolderField = new JTextField(30);
		audioFolderField.setEnabled(false);
		audioFolderField.setText("No folders have been selected.");
		p1.add(audioFolderField, c);
		c.gridx += c.gridwidth;
		audioFolderButton = new JButton("Select");
		audioFolderButton.addActionListener(new AudioFolderListener());
		p1.add(audioFolderButton, c);
		c.gridy++;
		c.gridx = 0;
		p1.add(new JLabel("Output folder:"), c);
		c.gridy++;
		c.gridx = 0;
		outputFolderField = new JTextField(30);
		outputFolderField.setEnabled(false);
		p1.add(outputFolderField, c);
		c.gridx += c.gridwidth;
		outputFolderButton = new JButton("Select");
		outputFolderButton.addActionListener(new OutputFolderListener());
		p1.add(outputFolderButton, c);
		mainPanel.add(p1, b);
		
		b.gridy++;
		JPanel p2 = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.anchor = c.WEST;
		//c.fill = c.HORIZONTAL;
		c.fill = c.NONE;
	/*	p2.add(new JLabel("Time zone:"), c);
		c.gridx += c.gridwidth;
		c.gridwidth = 2;
		tzBox = new JComboBox<String>(TimeZone.getAvailableIDs());
		tzBox.setSelectedItem("UTC");
		p2.add(tzBox, c); */
	/*	c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		p2.add(new JLabel("(If audio file names and annotation data are in the same time zone, select UTC.)"), c); */
		if (useLoaded) {
			//c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
			p2.add(new JLabel("Subset:"), c);
			c.gridx += c.gridwidth;
			c.gridwidth = 2;
			subsetBox = new JComboBox<String>();
			fillSubsetBox();
			p2.add(subsetBox, c);
		}
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		p2.add(new JLabel("Priority:"), c);
		c.gridx += c.gridwidth;
		c.gridwidth = 2;
		priorityBox = new JComboBox<String>(new String[] {"None (random)",
				"Files containing the most detections","Files containing the fewest detections",
				"Earliest files", "Latest files"});
		priorityBox.setSelectedIndex(0);
		p2.add(priorityBox, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		p2.add(new JLabel("Detections per file:"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		p2.add(new JLabel("Minimum:"), c);
		c.gridx += c.gridwidth + 1;
		minimumField = new JTextField(6);
		minimumField.setDocument(JIntFilter());
		minimumField.setText("1");
		p2.add(minimumField, c);
		c.gridy++;
		c.gridx = 0;
		p2.add(new JLabel("Maximum:"), c);
		c.gridx += c.gridwidth;
		maximumCheck = new JCheckBox();
		maximumCheck.addActionListener(new CheckListener());
		p2.add(maximumCheck, c);
		c.gridx += c.gridwidth;
		maximumField = new JTextField(6);
		maximumField.setEnabled(false);
		maximumField.setDocument(JIntFilter());
		maximumField.setText("1000");
		p2.add(maximumField, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		p2.add(new JLabel("Detections per class over entire batch:"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		p2.add(new JLabel("Maximum:"), c);
		c.gridx += c.gridwidth;
		maximumClassCheck = new JCheckBox();
		maximumClassCheck.addActionListener(new CheckListener());
		p2.add(maximumClassCheck, c);
		c.gridx += c.gridwidth;
		maximumClassField = new JTextField(6);
		maximumClassField.setEnabled(false);
		maximumClassField.setDocument(JIntFilter());
		maximumClassField.setText("10000");
		p2.add(maximumClassField, c);
		if (!useLoaded) {
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 3;
			ignoreBlankCheck = new JCheckBox("Ignore entries with no species label");
			ignoreBlankCheck.setSelected(true);
			p2.add(ignoreBlankCheck, c);
			c.gridy++;
			ignore2SGCheck = new JCheckBox("Ignore entries with '2-second glitch' label");
			ignore2SGCheck.setSelected(true);
			p2.add(ignore2SGCheck, c);
			c.gridy++;
			ignoreFPCheck = new JCheckBox("Ignore entries with 'False Positive' label");
			ignoreFPCheck.setSelected(true);
			p2.add(ignoreFPCheck, c);
			c.gridy++;
			ignoreUnkCheck = new JCheckBox("Ignore entries with 'Unk' or 'Unknown' labels");
			ignoreUnkCheck.setSelected(true);
			p2.add(ignoreUnkCheck, c);
		}
		mainPanel.add(p2, b);
		
		this.setDialogComponent(mainPanel);
	}
	
	/**
	 * Fills subsetBox with options.
	 */
	protected void fillSubsetBox() {
		subsetBox.removeAllItems();
		subsetBox.addItem("All");
		ArrayList<TSBSubset> subsetList = tsbControl.getSubsetList();
		ArrayList<String> outp = new ArrayList<String>();
		for (int i = 0; i < subsetList.size(); i++) {
			String firstDigit = subsetList.get(i).id.substring(0, 1);
			if (!outp.contains("All from "+firstDigit)) outp.add("All from "+firstDigit);
		}
		Collections.sort(outp);
		for (int i = 0; i < outp.size(); i++) subsetBox.addItem(outp.get(i));
		outp = new ArrayList<String>();
		for (int i = 0; i < subsetList.size(); i++) {
			if (!outp.contains(subsetList.get(i).id)) outp.add(subsetList.get(i).id);
		}
		Collections.sort(outp);
		for (int i = 0; i < outp.size(); i++) subsetBox.addItem(outp.get(i));
		subsetBox.setSelectedIndex(0);
	}
	
	protected class DataFileListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("WMAT table export file (*.wmat)","wmat","wmnt"));
			//fc.addChoosableFileFilter(new FileNameExtensionFilter("MIRRF feature vector data file (*.mirrffe)","mirrffe"));
			fc.addChoosableFileFilter(new FileNameExtensionFilter("MIRFEE training set file (*.mtsf)","mirrfts","mtsf"));
			fc.setMultiSelectionEnabled(true);
			int returnVal = fc.showOpenDialog(parentFrame);
			if (returnVal == fc.CANCEL_OPTION) return;
			File[] files = fc.getSelectedFiles();
			dataFileList.clear();
			for (int i = 0; i < files.length; i++) dataFileList.add(files[i]);
			if (dataFileList.size() == 0) dataFileField.setText("No files have been selected.");
			else dataFileField.setText(String.valueOf(dataFileList.size())+" file(s) selected.");
		}
	}
	
	/**
	 * Opens a file chooser when audioFolderButton is pressed.
	 */
	protected class AudioFolderListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setMultiSelectionEnabled(true);
			int returnVal = fc.showOpenDialog(parentFrame);
			if (returnVal == fc.CANCEL_OPTION) return;
			//wavList.clear();
			File[] folders = fc.getSelectedFiles();
			audioFolderList.clear();
			for (int i = 0; i < folders.length; i++) audioFolderList.add(folders[i]);
			if (audioFolderList.size() == 0) audioFolderField.setText("No folders have been selected.");
			else audioFolderField.setText(String.valueOf(audioFolderList.size())+" folder(s) selected.");
		}
	}
	
	/**
	 * Opens a file chooser when outputFolderButton is pressed.
	 */
	protected class OutputFolderListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setMultiSelectionEnabled(false);
			int returnVal = fc.showOpenDialog(parentFrame);
			if (returnVal == fc.CANCEL_OPTION) return;
			File folder = fc.getSelectedFile();
			outputFolderField.setText(folder.getAbsolutePath());
		}
	}
	
	/**
	 * Simple object for keeping track of .wav files and when they start and end.
	 */
	public class WavObject {
		public File file;
		public long start;
		public long end;
		
		public WavObject(File file, long start, long end) {
			this.file = file;
			this.start = start;
			this.end = end;
		}
		
		public long getStart() {
			return start;
		}
		
		/**
		 * Cuts the start time to the end of the previous WavObject (if the latter occurs after the former).
		 */
		public void cutStartTimeToEndOfPrev(WavObject prev) {
			if (start < prev.end) start = prev.end;
		}
	}
	
	/**
	 * Object containing a WavObject and a list of TSBDetections associated with it.
	 */
	public class DetectionSetObject {
		private WavObject wavObject;
		private boolean useLoaded;
		private ArrayList<TSBDetection> detectionList;
		private int[] labelCounts;
		private ArrayList<String> outputClassLabels;
		
		public DetectionSetObject(WavObject wavObject, ArrayList<String> outputLabels, boolean useLoaded) {
			this.wavObject = wavObject;
			this.useLoaded = useLoaded;
			this.detectionList = new ArrayList<TSBDetection>();
			this.outputClassLabels = outputLabels;
			this.labelCounts = new int[outputClassLabels.size()];
			for (int i = 0; i < outputClassLabels.size(); i++) labelCounts[i] = 0;
		}
		
		public boolean addDetection(TSBDetection inp) {
			if (useLoaded && !tsbControl.getClassMap().containsKey(inp.species)) return false;
			int index = getSpeciesIndex(inp.species);
			if (index < 0) return false;
			labelCounts[index]++;
			detectionList.add(inp);
			return true;
		}
		
		public TSBDetection getDetection(int index) {
			return detectionList.get(index);
		}
		
		public int getSize() {
			return detectionList.size();
		}
		
		public int getSpeciesIndex(String species) {
			int index = 0;
			if (useLoaded) index = outputClassLabels.indexOf(tsbControl.getClassMap().get(species));
			else index = outputClassLabels.indexOf(species);
			return index;
		}
		
		public int getSpeciesCount(String species) {
			int index = getSpeciesIndex(species);
			if (index < 0) return 0;
			return labelCounts[index];
		}
		
		public WavObject getWavObject() {
			return wavObject;
		}
		
		public long getStartTime() {
			return wavObject.getStart();
		}
	}
	
	/**
	 * Replaces the original listener for the "OK" button.
	 */
	protected class GenerateButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			GenerateButtonThread gbThread = new GenerateButtonThread();
			gbThread.start();
		}
	}
	
	/**
	 * Thread activated by GenerateButtonListener.
	 * Performs generateBatch(), and if it returns true, effectively "presses OK" and closes the dialog.
	 */
	protected class GenerateButtonThread extends Thread {
		
		protected GenerateButtonThread() {}
		
		@Override
		public void run() {
			if (generateBatch()) {
				okButtonPressed();
			}
		}
	}
	
	/**
	 * The thread for the loading bar window.
	 */
	protected class LoadingBarWindowThread extends Thread {
		protected TSBAudioTestBatchLoadingBarWindow loadingBarWindow;
		
		protected LoadingBarWindowThread(TSBAudioTestBatchLoadingBarWindow loadingBarWindow) {
			this.loadingBarWindow = loadingBarWindow;
		}
		
		@Override
		public void run() {
			loadingBarWindow.setVisible(true);
		}
	}
	
	/**
	 * Sets the "interrupted" boolean to true, signaling the audio creation process to stop.
	 */
	public void interrupt() {
		interrupted = true;
	}
	
	/**
	 * Attempts to generate a new batch of audio based off of input settings.
	 */
	protected boolean generateBatch() {
		try {
			int minNum = Integer.valueOf(minimumField.getText());
			if (maximumCheck.isSelected()) {
				int maxNum = Integer.valueOf(maximumField.getText());
				if (maxNum < 1) {
					tsbControl.simpleErrorDialog("Maximum detections per file must be at least 1.", 250);
					return false;
				} else if (minNum > maxNum) {
					tsbControl.simpleErrorDialog("Maximum detections per file cannot be less than minimum detections per file.", 250);
					return false;
				}
			}
			if (maximumClassCheck.isSelected()) {
				int maxClassNum = Integer.valueOf(maximumClassField.getText());
				if (maxClassNum < 1) {
					tsbControl.simpleErrorDialog("Maximum detections per class must be at least (and preferably much greater than) 1.", 250);
					return false;
				} else if (minNum > maxClassNum) {
					tsbControl.simpleErrorDialog("Maximum detections per class cannot be less than minimum detections per file.", 250);
					return false;
				}
			}
		} catch (Exception e) {
			tsbControl.simpleErrorDialog("Invalid settings.", 250);
			return false;
		}
		if (!useLoaded && dataFileList.size() == 0) {
			tsbControl.simpleErrorDialog("No data files have been selected.", 250);
			return false;
		}
		if (audioFolderList.size() == 0) {
			tsbControl.simpleErrorDialog("No audio folders have been selected.", 250);
			return false;
		}
		if (outputFolderField.getText().length() > 0) {
			File test = new File(outputFolderField.getText());
			if (!test.exists()) {
				tsbControl.simpleErrorDialog("Selected output folder does not exist.", 250);
				return false;
			}
		} else {
			tsbControl.simpleErrorDialog("No output folder has been selected.", 250);
			return false;
		}
		
		TSBAudioTestBatchLoadingBarWindow loadingBarWindow = new TSBAudioTestBatchLoadingBarWindow(this, tsbControl);
		//loadingBarWindow.setVisible(true);
		LoadingBarWindowThread loadingBarThread = new LoadingBarWindowThread(loadingBarWindow);
		loadingBarThread.start();
		
		int totalWavFilesInFolders = 0;
		int initialIgnoreCount = 0;
		int initialErrorCount = 0;
		ArrayList<WavObject> wavList = new ArrayList<WavObject>();
		StandardFileDateSettings sfdSettings = new StandardFileDateSettings();
		sfdSettings.setTimeZoneName(ZonedDateTime.now().getZone().getId()); // NOTE: Yes, this needs to be set to local time in order to convert to UTC.
		sfdSettings.setAdjustDaylightSaving(true);
		StandardFileDate sfd = new StandardFileDate(null);
		sfd.setSettings(sfdSettings);
		loadingBarWindow.startFolderCheck(audioFolderList.size());
		for (int i = 0; i < audioFolderList.size(); i++) {
			if (interrupted) return true;
			//System.out.println("audioFolderList loop: "+String.valueOf(i));
			File[] files = audioFolderList.get(i).listFiles();
			for (int j = 0; j < files.length; j++) {
				if (interrupted) return true;
				System.out.println("audioFolderList loop: "+String.valueOf(i)+", "+String.valueOf(j));
				File f = files[j];
				if (!f.getName().endsWith(".wav")) continue;
				long start = sfd.getTimeFromFile(f);
				//long start = MIRFEEControlledUnit.convertBetweenTimeZones(sfd.getTimeFromFile(f), ZonedDateTime.now().getZone().getId(), (String) tzBox.getSelectedItem());
				//System.out.println(MIRFEEControlledUnit.convertDateLongToString(sfd.getTimeFromFile(f)));
				//System.out.println(MIRFEEControlledUnit.convertDateLongToString(start)+", "+ZonedDateTime.now().getZone().getId()+" -> "+(String) tzBox.getSelectedItem());
				try {
					// Kudos to this: https://stackoverflow.com/questions/3009908/how-do-i-get-a-sound-files-total-time-in-java
					AudioInputStream stream = AudioSystem.getAudioInputStream(f);
					long duration = (long) (1000*(stream.getFrameLength()+0.0) / stream.getFormat().getFrameRate());
					wavList.add(new WavObject(f, start, start+duration));
					stream.close();
				} catch (UnsupportedAudioFileException | IOException e1) {
					System.out.println("Error reading "+f.getName());
					e1.printStackTrace();
					initialErrorCount++;
				}
				totalWavFilesInFolders++;
			}
			loadingBarWindow.updateFolderLoad(i+1, audioFolderList.size());
		}
		if (interrupted) return true;
		Collections.sort(wavList, Comparator.comparingLong(WavObject::getStart));
		
		ArrayList<Integer> overlapList = new ArrayList<Integer>();
		for (int i = 0; i < wavList.size()-1; i++) {
			WavObject w1 = wavList.get(i);
			WavObject w2 = wavList.get(i+1);
			if (w2.start < w1.end) overlapList.add(i+1);
		}
		if (overlapList.size() > 0) {
			int res = JOptionPane.showOptionDialog(this,
					MIRFEEControlledUnit.makeHTML("Some files appear to overlap with others in time. Succeeding overlapping files "
							+ "can either be ignored or be interpreted as if their start times have been cut to when the preceding "
							+ "file ends. The latter option DOES NOT modify the resulting file in any way.", 350),
					tsbControl.getUnitName(),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null,
					new Object[] {"Ignore overlapping files", "Cut start times"},
					null);
			for (int i = overlapList.size()-1; i >= 0; i--) {
				int currIndex = overlapList.get(i).intValue();
				if (res != JOptionPane.YES_OPTION) { // != YES so the "X" button does the NO action
					WavObject currWav = wavList.get(currIndex);
					WavObject prevWav = wavList.get(currIndex-1);
					if (currWav.end < prevWav.end) {
						wavList.remove(currIndex);
						initialIgnoreCount++;
					}
					else wavList.get(currIndex).cutStartTimeToEndOfPrev(prevWav);
				} else {
					wavList.remove(currIndex);
					initialIgnoreCount++;
				}
			}
		}
		if (wavList.size() == 0) {
			loadingBarWindow.setVisible(false);
			tsbControl.simpleErrorDialog("No audio files were found in the selected folders.", 250);
			return false;
		}
		if (interrupted) return true;
		
		//loadingBarWindow.startFileCount(totalWavFilesInFolders, initialIgnoreCount, initialErrorCount);
		ArrayList<TSBSubset> subOutpList = new ArrayList<TSBSubset>();
		if (useLoaded) {
			if (subsetBox.getSelectedItem().equals("All")) {
				subOutpList = tsbControl.getSubsetList();
			} else {
				for (int i = 0; i < tsbControl.getSubsetList().size(); i++) {
					TSBSubset curr = tsbControl.getSubsetList().get(i);
					String selection = (String) subsetBox.getSelectedItem();
					if ((selection.contains("All from") && selection.substring(selection.length()-1).equals(curr.id.substring(0, 1))) ||
							selection.equals(curr.id)) {
						subOutpList.add(curr);
					}
				}
			}
		} else {
			for (int i = 0; i < dataFileList.size(); i++) {
				TSBSubset fileSubset = new TSBSubset();
				File f = dataFileList.get(i);
				Scanner sc = null;
				try {
					sc = new Scanner(f);
					if (sc.hasNextLine()) sc.nextLine();
					else {
						sc.close();
						continue;
					}
					while (sc.hasNextLine()) {
						String[] nextSplit = sc.nextLine().split(",");
						TSBDetection currRow = null;
						try {
							if (f.getName().endsWith(".wmat") || f.getName().endsWith(".wmnt")) 
								currRow = new TSBDetection(tsbControl, 0, "", nextSplit, new String[0]);
							else if (f.getName().endsWith(".mirrfts") || f.getName().endsWith(".mtsf"))
								currRow = new TSBDetection(tsbControl, nextSplit.length-8, nextSplit);
							else continue;
						} catch (AssertionError | Exception e2) {
							continue;
						}
						if ((ignoreBlankCheck.isSelected() && currRow.species.equals("")) ||
							(ignore2SGCheck.isSelected() && currRow.species.equals("2-second glitch")) ||
							(ignoreFPCheck.isSelected() && currRow.species.equals("False positive")) ||
							(ignoreUnkCheck.isSelected() && (currRow.species.equals("Unk") || currRow.species.equals("Unknown"))))
							continue;
						fileSubset.addEntry(currRow, true);
					}
					subOutpList.add(fileSubset);
					sc.close();
				} catch (Exception e) {
					if (sc != null) sc.close();
				}
			}
		}
		if (interrupted) return true;
		
		ArrayList<String> dsoOutputLabels = new ArrayList<String>();
		ArrayList<TSBDetection> detectionList = new ArrayList<TSBDetection>();
		for (int i = 0; i < subOutpList.size(); i++) {
			TSBSubset currSubset = subOutpList.get(i);
			for (int j = 0; j < currSubset.validEntriesList.size(); j++) {
				if (!dsoOutputLabels.contains(currSubset.classList.get(j))) dsoOutputLabels.add(currSubset.classList.get(j));
				ArrayList<TSBDetection> currList = currSubset.validEntriesList.get(j);
				for (int k = 0; k < currList.size(); k++) {
					detectionList.add(currList.get(k));
				}
			}
		}
		if (interrupted) return true;
		Collections.sort(detectionList, Comparator.comparingLong(TSBDetection::getDateTimeAsLong));
		
		//System.out.println("wavList.size(): "+String.valueOf(wavList.size()));
		//System.out.println("Initial initialIgnoreCount: "+String.valueOf(initialIgnoreCount));
		ArrayList<DetectionSetObject> dsoList = new ArrayList<DetectionSetObject>();
		DetectionSetObject currDSO = new DetectionSetObject(wavList.remove(0), dsoOutputLabels, useLoaded);
		while (detectionList.size() > 0) {
			if (interrupted) return true;
			WavObject currWav = currDSO.getWavObject();
			TSBDetection currDetection = detectionList.get(0);
			long dTime = currDetection.getDateTimeAsLong();
			System.out.println(currDetection.datetime+" -> ["+tsbControl.convertLongToUTC(currWav.start)+", "+tsbControl.convertLongToUTC(currWav.end)+"] "+
					String.valueOf(currWav.start <= dTime && dTime < currWav.end));
			if (currWav.start <= dTime && dTime < currWav.end) {
				currDSO.addDetection(currDetection);
				detectionList.remove(0);
			} else if (dTime < currWav.start){
				detectionList.remove(0);
			} else {
				if (currDSO.getSize() > 0) {
					dsoList.add(currDSO);
					currDSO = null;
				} else initialIgnoreCount++;
				if (wavList.size() == 0) break;
				currDSO = new DetectionSetObject(wavList.remove(0), dsoOutputLabels, useLoaded);
			}
		}
		if (currDSO != null && currDSO.getSize() > 0) dsoList.add(currDSO);
		else if (wavList.size() > 0) initialIgnoreCount++;
		initialIgnoreCount += wavList.size();
		//System.out.println("Subsequent initialIgnoreCount: "+String.valueOf(initialIgnoreCount));
		
		if (interrupted) return true;
		Collections.shuffle(dsoList);
		if (priorityBox.getSelectedIndex() == 1) {
			Collections.sort(dsoList, Comparator.comparingInt(DetectionSetObject::getSize));
			Collections.reverse(dsoList);
		} else if (priorityBox.getSelectedIndex() == 2) {
			Collections.sort(dsoList, Comparator.comparingInt(DetectionSetObject::getSize));
		} else if (priorityBox.getSelectedIndex() == 3) {
			Collections.sort(dsoList, Comparator.comparingLong(DetectionSetObject::getStartTime));
		} else if (priorityBox.getSelectedIndex() == 4) {
			Collections.sort(dsoList, Comparator.comparingLong(DetectionSetObject::getStartTime));
			Collections.reverse(dsoList);
		}
		
		//System.out.println("Initial ignored: "+String.valueOf(initialIgnoreCount + initialErrorCount));
		//System.out.println("dsoList.size(): "+String.valueOf(dsoList.size()));
		//System.out.println("totalWavFilesInFolders: "+String.valueOf(totalWavFilesInFolders));
		loadingBarWindow.startFileCount(totalWavFilesInFolders, initialIgnoreCount, initialErrorCount, dsoOutputLabels);
		//ArrayList<String> outpClasses = tsbControl.getOutputClassLabels();
		int[] classCount = new int[dsoOutputLabels.size()];
		for (int i = 0; i < classCount.length; i++) classCount[i] = 0;
		int minPerFile = Integer.valueOf(minimumField.getText());
		int maxPerFile = -1;
		int maxPerClass = -1;
		if (maximumCheck.isSelected()) maxPerFile = Integer.valueOf(maximumField.getText());
		if (maximumClassCheck.isSelected()) maxPerClass = Integer.valueOf(maximumClassField.getText());
		while (dsoList.size() > 0 && !interrupted) {
			//System.out.println(dsoList.size());
			currDSO = dsoList.remove(0);
			String toPrint = "classCount: [";
			for (int i = 0; i < classCount.length; i++) {
				if (i != 0) toPrint += ", ";
				toPrint += String.valueOf(classCount[i]);
			}
			toPrint += "] ";
			toPrint += currDSO.getWavObject().file.getName()+" ";
			toPrint += String.valueOf(currDSO.getDetection(0).uid);
			System.out.println(toPrint);
			if (currDSO.getSize() < minPerFile || (maximumCheck.isSelected() && currDSO.getSize() > maxPerFile)) {
				loadingBarWindow.addToCounter(loadingBarWindow.IGNORED, totalWavFilesInFolders, currDSO);
				continue;
			}
			if (maximumClassCheck.isSelected()) {
				boolean skip = false;
				for (int i = 0; i < dsoOutputLabels.size(); i++) {
					if (classCount[i] + currDSO.getSpeciesCount(dsoOutputLabels.get(i)) > maxPerClass) skip = true;
				}
				if (skip) {
					loadingBarWindow.addToCounter(loadingBarWindow.IGNORED, totalWavFilesInFolders, currDSO);
					continue;
				}
			}
			File currFile = currDSO.getWavObject().file;
			File outpFile = new File(outputFolderField.getText()+"\\"+currFile.getName());
			try {
				FileUtils.copyFile(currFile, outpFile);
			} catch (Exception e) {
				e.printStackTrace();
				loadingBarWindow.addToCounter(loadingBarWindow.ERROR, totalWavFilesInFolders, currDSO);
				continue;
			}
			for (int i = 0; i < dsoOutputLabels.size(); i++) classCount[i] += currDSO.getSpeciesCount(dsoOutputLabels.get(i));
			loadingBarWindow.addToCounter(loadingBarWindow.SAVED, totalWavFilesInFolders, currDSO);
		}
		loadingBarWindow.setToFinished();
		while (!interrupted) {};
		return true;
	}
	
	/**
	 * Enables/disables certain text fields if certain boxes are checked.
	 */
	protected class CheckListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			maximumField.setEnabled(maximumCheck.isSelected());
			//minimumClassField.setEnabled(minimumClassCheck.isSelected());
			maximumClassField.setEnabled(maximumClassCheck.isSelected());
		}
	}
	
	/**
	 * Limits entry in text field to numbers only.
	 * @return PlainDocument
	 */
	public PlainDocument JIntFilter() {
		PlainDocument d = new PlainDocument() {
			@Override
	        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
	            char c = str.charAt(0);
	            if (c >= '0' && c <= '9') {
	            	super.insertString(offs, str, a);
		        }
	        }
		};
		return d;
	}
	
	@Override
	public boolean getParams() {
		//boolean worked = generateBatch();
		// TODO ?????
		//return worked;
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {}
	
}