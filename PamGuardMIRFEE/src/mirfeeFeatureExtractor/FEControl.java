package mirfeeFeatureExtractor;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TimeZone;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import mirfee.MIRFEEControlledUnit;

/**
 * The controller class for the MIRFEE Feature Extractor.
 * @author Holly LeBlond
 */
public class FEControl extends MIRFEEControlledUnit implements PamSettings {
	
	protected FEParameters feParameters = new FEParameters();
	protected FESidePanel feSidePanel;
	protected FESettingsDialog feSettingsDialog;
	protected FEProcess feProcess;
	protected FEPythonThreadManager threadManager;
	
	private ArrayList<FEInputDataObject> inputWMATDataEntries;
	
	public static final String UNITTYPE = "MIRFEEFE";
	
	public FEControl(String unitName) {
		super(UNITTYPE, "MIRFEE Feature Extractor");
		
		PamSettingManager.getInstance().registerSettings(this);
		
		feSidePanel = new FESidePanel(this);
		setSidePanel(feSidePanel);
		
		feSettingsDialog = null;
		
		runTempFolderDialogLoop("MIRFEE Feature Extractor", "Feature Extractor", feParameters);
		
		if (!this.isViewer()) {
			this.threadManager = new FEPythonThreadManager(this);
		}
		
		this.inputWMATDataEntries = new ArrayList<FEInputDataObject>();
		loadInputWMATDataEntries(feParameters, null);
		
		this.feProcess = new FEProcess(this);
		addPamProcess(feProcess);
		
	}
	
	/**
	 * Loads the data from the files in newInputFileList into the inputWMATDataEntries object when using .wmat or .mtsf files as the input data.
	 * This was originally done in FESettingsDialog (which now uses this function here) and the data was stored in FEParameters, but that caused severe space complexity issues,
	 * so it is now done here to save space. While the data from those files is no longer stored in the configuration, it gets loaded automatically when re-opening the configuration,
	 * as the file names are still stored in the parameters.
	 * @param params - The FEParameters object, which is supposed to be either the currently-set parameters object, or a new parameters object about to replace it.
	 * In the latter case, all other settings that aren't input related should be set before running this function.
	 * @param newInputFileList - The new list of input files. If null, the files will instead be loaded from the list of file names already in the parameters object, if there are any.
	 * @return True if everything worked properly, or the parameters have it set to not use WMAT/MTSF files as input. In the latter case, inputWMATDataEntries is cleared and nothing else happens.
	 * False if an error occurred when reading a file, or if none of the files contained any valid data.
	 */
	public boolean loadInputWMATDataEntries(FEParameters params, ArrayList<File> newInputFileList) {
		ArrayList<FEInputDataObject> newWMATDataEntries = new ArrayList<FEInputDataObject>();
		if (!params.inputFromWMATorMTSF) {
			this.inputWMATDataEntries = new ArrayList<FEInputDataObject>();
			return true;
		}
		if (newInputFileList == null) { // If null, the files are loaded in via the file names already in params, if there are any.
			newInputFileList = new ArrayList<File>();
			for (int i = 0; i < params.inputWMATFileNames.size(); i++)
				newInputFileList.add(new File(params.inputWMATFileNames.get(i)));
		}
		if (newInputFileList.size() == 0) {
			simpleErrorDialog("No input files have been selected.", 250);
			return false;
		}
		ArrayList<String[]> startsAndEnds = new ArrayList<String[]>();
		for (int k = 0; k < newInputFileList.size(); k++) {
			try {
				File f = newInputFileList.get(k);
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				if (!f.exists()) {
					simpleErrorDialog(f.getName()+" does not exist.");
					return false;
				}
				Scanner sc;
				if (f.getAbsolutePath().endsWith(".wmat") || f.getAbsolutePath().endsWith(".wmnt")) {
					try {
						sc = new Scanner(f);
						if (!sc.hasNextLine()) {
							sc.close();
							simpleErrorDialog(f.getName()+" is blank.", 250);
							return false;
						}
						String firstLine = sc.nextLine();
						if (!firstLine.startsWith("uid,datetime,lf,hf,duration,amplitude,species,calltype,comment,slicedata")) {
							sc.close();
							simpleErrorDialog(f.getName()+" is not correctly formatted.", 250);
							return false;
						}
						String start = "";
						String end = "";
						while (sc.hasNextLine()) {
							String[] nextSplit = sc.nextLine().split(",");
							try {
								if ((params.inputIgnoreBlanks && nextSplit[6].length() == 0)
										|| (params.inputIgnore2SecondGlitch && nextSplit[6].equals("2-second glitch"))
										|| (params.inputIgnoreFalsePositives && (nextSplit[6].equals("False positive") || nextSplit[6].equals("False Detection")))
										|| (params.inputIgnoreUnk && (nextSplit[6].equals("Unk") || nextSplit[6].equals("Unknown")))) {
									continue;
								}
								String datetime = nextSplit[1];
								if (start.length() == 0 || start.compareTo(datetime) > 0) start = datetime;
								if (end.length() == 0 || end.compareTo(datetime) < 0) end = datetime;
								newWMATDataEntries.add(new FEInputDataObject(nextSplit, false, null, null));
							} catch (Exception e) {
								e.printStackTrace(); // TODO Remove if it becomes a problem.
								continue;
							}
						}
						startsAndEnds.add(new String[] {start, end});
						sc.close();
					} catch (Exception e) {
						e.printStackTrace();
						simpleErrorDialog("Error occured when attempting to read "+f.getName()+".", 350);
						return false;
					}
				} else { // input file is .mtsf (current) or .mirrfts (old)
					try {
						sc = new Scanner(f);
						if (!sc.hasNextLine()) {
							sc.close();
							simpleErrorDialog(f.getName()+" is blank.", 250);
							return false;
						}
						ArrayList<String> foundFeatures = FEControl.findFeaturesInFile(f);
						if (foundFeatures == null || foundFeatures.size() == 0) {
							sc.close();
							simpleErrorDialog(f.getName()+" is not correctly formatted.", 250);
							return false;
						}
						boolean foundHeader = false;
						String header = "cluster,uid,location,date,duration,lf,hf,label";
						for (int i = 0; i < foundFeatures.size(); i++)
							header += ","+foundFeatures.get(i);
						String start = "";
						String end = "";
					/*	while (sc.hasNextLine()) {
							String next = sc.nextLine();
							if (!foundHeader) {
								if (next.equals(header)) foundHeader = true;
								continue;
							}
							String[] nextSplit = next.split(",");
							try {
								if ((inputIgnoreBlanksCheck.isSelected() && nextSplit[7].length() == 0)
										|| (inputIgnore2SecondGlitchCheck.isSelected() && nextSplit[7].equals("2-second glitch"))
										|| (inputIgnoreFalsePositivesCheck.isSelected() && nextSplit[7].equals("False positive"))
										|| (inputIgnoreUnkCheck.isSelected() && 
												(nextSplit[7].equals("Unk") || nextSplit[7].equals("Unknown")))) {
									continue;
								}
								ArrayList<String> problematicFeatures = feControl.findProblematicFeaturesInFile(f);
								ArrayList<String> cantRetrieve = new ArrayList<String>();
								for (int j = 0; j < featureTable.getRowCount(); j++) {
									String value = (String) featureTable.getValueAt(j, 1);
									String[] tokens = value.split("_");
									if (tokens[0].equals("amplitude") || tokens[0].equals("freqsd") || tokens[0].equals("freqsdd1") ||
											tokens[0].equals("freqsdd2") || tokens[0].equals("freqsdslope") || tokens[0].equals("freqsdelbow") ||
											tokens[0].equals("wcfreqs") || tokens[0].equals("wcslopes") || tokens[0].equals("wccurves"))
										if (!problematicFeatures.contains(value)) cantRetrieve.add(value);
								}
								if (cantRetrieve.size() > 0) {
									String txt = "Since you're taking input from a .mirrfts file, the following header features can't be retrieved, "
											+ "as amplitude and slice data are not normally stored in .mirrfts files and the selected features weren't "
											+ "found in the input file to copy over:\n";
									for (int j = 0; j < cantRetrieve.size(); j++)
										txt += "\n"+cantRetrieve.get(j);
									feControl.simpleErrorDialog(txt, 350);
									return false;
								}
								String datetime = nextSplit[3];
								if (start.length() == 0 || start.compareTo(datetime) > 0) start = datetime;
								if (end.length() == 0 || end.compareTo(datetime) < 0) end = datetime;
								newParams.inputDataEntries.add(new FEInputDataObject(nextSplit, true, foundFeatures, problematicFeatures));
							} catch (Exception e) {
								e.printStackTrace(); // TODO Remove if it becomes a problem.
								continue;
							}
							//newParams.inputDataEntries.add(nextSplit);
						} */
						while (sc.hasNextLine()) {
							if (sc.nextLine().equals(header)) break;
						}
						// I'm not sure why the problematic features bit was originally in a while-loop as shown above.
						ArrayList<String> problematicFeatures = FEControl.findProblematicFeaturesInFile(f);
						ArrayList<String> cantRetrieve = new ArrayList<String>();
						for (int j = 0; j < params.featureList.length; j++) {
							String value = params.featureList[j][1];
							String[] tokens = value.split("_");
							if (tokens[0].equals("amplitude") || tokens[0].equals("freqsd") || tokens[0].equals("freqsdd1") ||
									tokens[0].equals("freqsdd2") || tokens[0].equals("freqsdslope") || tokens[0].equals("freqsdelbow") ||
									tokens[0].equals("wcfreqs") || tokens[0].equals("wcslopes") || tokens[0].equals("wccurves"))
								if (!problematicFeatures.contains(value)) cantRetrieve.add(value);
						}
						if (cantRetrieve.size() > 0) {
							String txt = "Since you're taking input from a .mtsf file, the following header features can't be retrieved, "
									+ "as amplitude and slice data are not normally stored in .mtsf files and the selected features weren't "
									+ "found in the input file to copy over:\n";
							for (int j = 0; j < cantRetrieve.size(); j++)
								txt += "\n"+cantRetrieve.get(j);
							simpleErrorDialog(txt, 350);
							return false;
						}
						//if (f.getAbsolutePath().endsWith(".mirrfts")) {
						while (sc.hasNextLine()) {
							String[] nextSplit =  sc.nextLine().split(",");
							try {
								if ((params.inputIgnoreBlanks && nextSplit[7].length() == 0)
										|| (params.inputIgnore2SecondGlitch && nextSplit[7].equals("2-second glitch"))
										|| (params.inputIgnoreFalsePositives && (nextSplit[7].equals("False positive") || nextSplit[6].equals("False Detection")))
										|| (params.inputIgnoreUnk && (nextSplit[7].equals("Unk") || nextSplit[7].equals("Unknown")))) {
									continue;
								}
								String datetime = nextSplit[3];
								if (start.length() == 0 || start.compareTo(datetime) > 0) start = datetime;
								if (end.length() == 0 || end.compareTo(datetime) < 0) end = datetime;
								newWMATDataEntries.add(new FEInputDataObject(nextSplit, true, foundFeatures, problematicFeatures));
							} catch (Exception e) {
								e.printStackTrace(); // TODO Remove if it becomes a problem.
								continue;
							}
						}
						// There was an attempt to re-format the training set files to binary, but I've abandoned that for now.
					/*	} else {
							// Kudos: https://stackoverflow.com/questions/3402735/what-is-simplest-way-to-read-a-file-into-string
							String remainder = sc.useDelimiter("\\Z").next();
							sc.close();
							byte[] bytearr = remainder.getBytes();
							ByteArrayInputStream bis = new ByteArrayInputStream(bytearr, 0, bytearr.length);
							DataInputStream dis = new DataInputStream(bis);
							while (true) {
								try {
									String[] inpdata = new String[8];
									inpdata[0] = dis.readUTF(); // cluster ID
									inpdata[1] = String.valueOf(dis.readLong()); // UID
									inpdata[2] = dis.readUTF(); // location
									inpdata[3] = String.valueOf(feControl.convertDateLongToString(dis.readLong())); // datetime
									inpdata[4] = String.valueOf(dis.readInt()); // duration
									inpdata[5] = String.valueOf(dis.readInt()); // lf
									inpdata[6] = String.valueOf(dis.readInt()); // hf
									inpdata[7] = dis.readUTF(); // label
									for (int j = 0; j < foundFeatures.size(); j++)
										dis.readDouble(); // features (not used here)
									if ((inputIgnoreBlanksCheck.isSelected() && inpdata[7].length() == 0)
											|| (inputIgnore2SecondGlitchCheck.isSelected() && inpdata[7].equals("2-second glitch"))
											|| (inputIgnoreFalsePositivesCheck.isSelected() && inpdata[7].equals("False positive"))
											|| (inputIgnoreUnkCheck.isSelected() && 
													(inpdata[7].equals("Unk") || inpdata[7].equals("Unknown")))) {
										continue;
									}
									if (start.length() == 0 || start.compareTo(inpdata[3]) > 0) start = inpdata[3];
									if (end.length() == 0 || end.compareTo(inpdata[3]) < 0) end = inpdata[3];
									newParams.inputDataEntries.add(new FEInputDataObject(inpdata, true, foundFeatures, problematicFeatures));
								} catch (Exception e) {
									break;
								}
							}
							dis.close();
						} */
						startsAndEnds.add(new String[] {start, end});
						sc.close();
					} catch (Exception e) {
						e.printStackTrace();
						simpleErrorDialog("Error occured when attempting to read "+f.getName()+".", 350);
						return false;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				simpleErrorDialog("Error occured when attempting to read input files.", 350);
				return false;
			}
		}
		// Kudos to Lukas Eder on https://stackoverflow.com/questions/4699807/sort-arraylist-of-array-in-java.
		startsAndEnds.sort(Comparator.comparing(a -> a[0]));
		for (int i = 0; i < startsAndEnds.size()-1; i++) {
			if (startsAndEnds.get(i)[1].compareTo(startsAndEnds.get(i+1)[0]) >= 0) {
				String message = "The selected input files overlap each other in terms of date/time. This may result in features being extracted "
						+ "from the wrong audio file and multiple instances of the same data entry appearing in the output file.\n\n"
						+ "Proceed? (It is highly advised that you don't!)";
				int res = JOptionPane.showConfirmDialog(null,
						FEControl.makeHTML(message, 350),
						this.getUnitName(),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (res != JOptionPane.YES_OPTION) return false;
				break;
			}
		}
		if (newWMATDataEntries.size() == 0) {
			simpleErrorDialog("Input data files contain no valid entries.", 300);
			return false;
		}
		newWMATDataEntries.sort(Comparator.comparing(a -> a.datetime));
		this.inputWMATDataEntries = newWMATDataEntries;
		return true;
	}
	
	/**
	 * @return Clone of inputWMATDataEntries.
	 */
	public ArrayList<FEInputDataObject> getWMATInputDataEntries(){
		return (ArrayList<FEInputDataObject>) this.inputWMATDataEntries.clone();
	}
	
	/**
	 * Calls the function in FEPanel that adds 1 to the respective counter.
	 * @param i - Number determining the chosen counter:
	 * <br> 0 = Success
	 * <br> 1 = Failure
	 * <br> 2 = Ignore
	 * <br> 3 = Pending
	 * @param uid - The contour's UID, for printing purposes.
	 */
	public void addOneToCounter(int i, String uid) {
		this.getSidePanel().getFEPanel().addOneToCounter(i, uid);
	}
	
	/**
	 * Calls the function in FEPanel that subtracts 1 from the pending counter in the FEPanel.
	 * @return True if the counter is above 0. False otherwise.
	 */
	public boolean subtractOneFromPendingCounter() {
		return this.getSidePanel().getFEPanel().subtractOneFromPendingCounter();
	}
	
	public static HashMap<String, String> findFESettingsInFile(File f) {
		HashMap<String, String> outp = new HashMap<String, String>();
		Scanner sc;
		try {
			sc = new Scanner(f);
			if (!sc.hasNextLine() || !(sc.nextLine().equals("EXTRACTOR PARAMS START") && sc.hasNextLine())) {
				sc.close();
				return null;
			}
			while (sc.hasNextLine()) {
				String next = sc.nextLine();
				if (next.equals("EXTRACTOR PARAMS END")) break;
				String[] tokens = next.split("=");
				if (tokens.length >= 2) outp.put(tokens[0], tokens[1]);
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return outp;
	}
	
	/**
	 * Searches for feature names within an input .mfe or .mtsf file.
	 * @return An ArrayList of found feature names. Otherwise, returns null if the file format is incorrect or no header line could be found.
	 */
	public static ArrayList<String> findFeaturesInFile(File f) {
		if (!(f.getPath().endsWith(".mirrffe") || f.getPath().endsWith(".mirrfts") ||
				f.getPath().endsWith(".mfe") || f.getPath().endsWith(".mtsf"))) return null;
		ArrayList<String> outp = new ArrayList<String>();
		Scanner sc;
		try {
			sc = new Scanner(f);
			while (sc.hasNextLine()) {
				String next = sc.nextLine();
				if ((f.getPath().endsWith("fe") && next.startsWith("cluster,uid,date,duration,lf,hf,")) ||
						(!f.getPath().endsWith("fe") && next.startsWith("cluster,uid,location,date,duration,lf,hf,label,"))) {
					String[] tokens = next.split(",");
					int startindex = 6;
					if (!f.getPath().endsWith("fe")) startindex = 8;
					for (int i = startindex; i < tokens.length; i++) outp.add(tokens[i]);
					sc.close();
					return outp;
				}
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * The same as findFeaturesInFile, but only includes header or slice data features that can't be calculated if they otherwise weren't present in the input .mtsf file:
	 * amplitude, freqsd, freqsdd1, freqsdd2, freqsdslope, freqsdelbow, wcfreqs, wcslopes, wccurves
	 */
	public static ArrayList<String> findProblematicFeaturesInFile(File f) {
		ArrayList<String> foundFeatures = findFeaturesInFile(f);
		if (foundFeatures == null) return null;
		for (int j = 0; j < foundFeatures.size(); j++) {
			String[] tokens = foundFeatures.get(j).split("_");
			if (tokens[0].equals("amplitude") || tokens[0].equals("freqsd") || tokens[0].equals("freqsdd1") ||
					tokens[0].equals("freqsdd2") || tokens[0].equals("freqsdslope") || tokens[0].equals("freqsdelbow") ||
					tokens[0].equals("wcfreqs") || tokens[0].equals("wcslopes") || tokens[0].equals("wccurves"))
				continue;
			foundFeatures.remove(j);
			j--;
		}
		return foundFeatures;
	}
	
	/**
	 * Checks if two files contain the exact same FE settings and features.
	 * Files must be either .mfe or .mtsf (or their old counterparts).
	 * @return True if settings and features match. Otherwise, false.
	 */
	public boolean fileSettingsMatch(File f1, File f2) {
		HashMap<String, String> f1settings = findFESettingsInFile(f1);
		HashMap<String, String> f2settings = findFESettingsInFile(f2);
		if (f1settings == null || f2settings == null || f1settings.size() != f2settings.size())
			return false;
		Iterator<String> it = f1settings.keySet().iterator();
		while (it.hasNext()) {
			String next = it.next();
			if (!f2settings.containsKey(next) || !f1settings.get(next).equals(f2settings.get(next)))
				return false;
		}
		ArrayList<String> f1features = findFeaturesInFile(f1);
		ArrayList<String> f2features = findFeaturesInFile(f2);
		if (f1features == null || f2features == null || f1features.size() == 0 || f2features.size() == 0 || f1features.size() != f2features.size())
			return false;
		for (int i = 0; i < f1features.size(); i++) {
			if (!f1features.get(i).equals(f2features.get(i))) return false;
		}
		return true;
	}
	
	@Override
	public FESidePanel getSidePanel() {
		return feSidePanel;
	}
	
	public FEParameters getParams() {
		return feParameters;
	}
	
	public void setParams(FEParameters inp) {
		feParameters = inp;
	}
	
	public FESettingsDialog getSettingsDialog() {
		return feSettingsDialog;
	}
	
	public void setSettingsDialog(FESettingsDialog settingsDialog) {
		feSettingsDialog = settingsDialog;
	}
	
	/**
	 * The object that handles the Python scripts that perform the feature extraction.
	 */
	public FEPythonThreadManager getThreadManager() {
		return threadManager;
	}

	@Override
	public Serializable getSettingsReference() {
		return feParameters;
	}

	@Override
	public long getSettingsVersion() {
		// TODO
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		feParameters = ((FEParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}	


	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName());
		menuItem.addActionListener(new DetectionSettings(parentFrame));
		return menuItem;
	}

	class DetectionSettings implements ActionListener {

		private Frame parentFrame;

		public DetectionSettings(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			settingsDialog(parentFrame);	
		}
	}

	/**
	 * Opens the settings dialog.
	 * @param parentFrame
	 */
	protected void settingsDialog(Frame parentFrame) {
		FESettingsDialog settingsDialog = new FESettingsDialog(this.getPamView().getGuiFrame(), this);
		settingsDialog.setVisible(true);
	}
}