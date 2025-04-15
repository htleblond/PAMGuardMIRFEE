package mirfeeTrainingSetBuilder;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettings;

/**
 * The controller class for the MIRFEE Training Set Builder.
 * Is a subclass of PamControlledUnit.
 * @author Holly LeBlond
 */
public class TSBControl extends PamControlledUnit implements PamSettings {
	
	public static final String UNITTYPE = "MIRFEETSB";
	
	protected TSBParameters tsbParameters = new TSBParameters();
	
	protected TSBTabPanel tsbTabPanel;
	protected ArrayList<String> fullClassList;
	protected ArrayList<String> umbrellaClassList;
	protected HashMap<String, String> classMap;
	protected ArrayList<TSBSubset> subsetList;
	protected ArrayList<String> featureList;
	protected HashMap<String, String> feParamsMap;

	public TSBControl(String unitName) {
		super(UNITTYPE, "MIRFEE Training Set Builder");
		
		tsbTabPanel = new TSBTabPanel(this);
		fullClassList = new ArrayList<String>();
		umbrellaClassList = new ArrayList<String>();
		classMap = new HashMap<String, String>();
		
		subsetList = new ArrayList<TSBSubset>();
		featureList = new ArrayList<String>();
		feParamsMap = new HashMap<String, String>();
		//includeCallType = false;
	}
	
	/**
	 * Streamlined error dialog with an editable message.
	 */
	public void simpleErrorDialog(String inptext, int width) {
		JOptionPane.showMessageDialog(this.getGuiFrame(),
				makeHTML(inptext, width),
			"MIRFEE Training Set Builder",
			JOptionPane.ERROR_MESSAGE);
	}
	
	
	// Moved to MIRFEEControlledUnit as static function.
	/**
	 * Converts date/time strings formatted as yyyy-MM-dd HH:mm:ss+SSS back to longs.
	 */
/*	public long convertDateStringToLong(String inp) {
		// Kudos: https://stackoverflow.com/questions/12473550/how-to-convert-a-string-date-to-long-millseconds
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
		try {
		    Date d = f.parse(inp);
		    return d.getTime();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return -1;
	} */
	
	/**
	 * Converts a long to a string representing a date/time with the following format: yyyy-MM-dd HH:mm:ss+SSS
	 */
	public String convertLongToUTC(long inp) {
		Date date = new Date(inp);
		String date_format = "yyyy-MM-dd HH:mm:ss+SSS";
		SimpleDateFormat currdateformat = new SimpleDateFormat(date_format);
		return currdateformat.format(date);
	}
	
	/**
	 * Streamlined means of converting text to HTML for dialogs.
	 * @param inp - The text
	 * @param width - How wide the dialog should be
	 */
	public String makeHTML(String inp, int width) {
		return String.format("<html><body style='width: %1spx'>%1s", width, inp);
	}
	
	/**
	 * Sets the list of classes found in the input subsets.
	 */
	public void setFullClassList(ArrayList<String> inp) {
		fullClassList = new ArrayList<String>(inp);
	}
		
	/**
	 * @return The list of classes found in the input subsets.
	 */
	public ArrayList<String> getFullClassList() {
		return fullClassList;
	}
	
	/**
	 * Sets the list of "umbrella classes" (merging multiple classes to be output as a single class).
	 */
	public void setUmbrellaClassList(ArrayList<String> inp) {
		umbrellaClassList = new ArrayList<String>(inp);
	}
	
	/**
	 * @return The list of "umbrella classes" (merging multiple classes to be output as a single class).
	 */
	public ArrayList<String> getUmbrellaClassList() {
		return umbrellaClassList;
	}
	
	/**
	 * @return List of what class names are actually going to be output.
	 */
	public ArrayList<String> getOutputClassLabels() {
		ArrayList<String> outp = new ArrayList<String>();
		Iterator<String> it = getClassMap().values().iterator();
		while (it.hasNext()) {
			String next = it.next();
			if (!outp.contains(next)) outp.add(next);
		}
		return outp;
	}
	
	/**
	 * Sets a hash map matching class names from the loaded subsets to umbrella classes.
	 */
	public void setClassMap(HashMap<String, String> inp) {
		classMap = new HashMap<String, String>(inp);
	}
	
	/**
	 * @return A hash map matching class names from the loaded subsets to umbrella classes.
	 */
	public HashMap<String, String> getClassMap() {
		return classMap;
	}
	
	/**
	 * Sets the list of loaded TSBSubsets.
	 */
	public void setSubsetList(ArrayList<TSBSubset> inp) {
		subsetList = new ArrayList<TSBSubset>(inp);
	}
	
	/**
	 * @return The list of loaded TSBSubsets.
	 */
	public ArrayList<String> getFeatureList() {
		return featureList;
	}
	
	/**
	 * Sets the list of features found in the first loaded subset.
	 * All other subsets must have the same features.
	 */
	public void setFeatureList(ArrayList<String> inp) {
		featureList = new ArrayList<String>(inp);
	}
	
	/**
	 * @return The list of features found in the first loaded subset.
	 * All other subsets must have the same features.
	 */
	public ArrayList<TSBSubset> getSubsetList() {
		return subsetList;
	}
	
	/**
	 * Clears subsetList.
	 */
	public void resetSubsetList() {
		subsetList = new ArrayList<TSBSubset>();
	}
	
	/**
	 * @return The hash map of Feature Extractor parameters found in the first loaded subset.
	 */
	public HashMap<String, String> getFEParamsMap() {
		return feParamsMap;
	}
	
	/**
	 * Sets the hash map of Feature Extractor parameters found in the first loaded subset.
	 */
	public void setFEParamsMap(HashMap<String, String> inp) {
		feParamsMap = inp;
	}
		
	@Override
	public TSBTabPanel getTabPanel() {
		return tsbTabPanel;
	}
	
	public TSBParameters getParams() {
		return tsbParameters;
	}
	
	public void setParams(TSBParameters inp) {
		tsbParameters = inp;
	}

	@Override
	public Serializable getSettingsReference() {
		return tsbParameters;
	}

	@Override
	public long getSettingsVersion() {
		// TODO
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		tsbParameters = ((TSBParameters) pamControlledUnitSettings.getSettings()).clone();
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
		TSBSettingsDialog settingsDialog = new TSBSettingsDialog(parentFrame, this);
		settingsDialog.setVisible(true);
	}
	
}