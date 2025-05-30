package mirfeeLiveClassifier;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import mirfee.MIRFEEControlledUnit;

/**
 * The controller class for the MIRFEE Live Classifier.
 * @author Holly LeBlond
 */
public class LCControl extends MIRFEEControlledUnit implements PamSettings {
	
	public static final String UNITTYPE = "MIRFEELC";
	
	protected LCParameters parameters = new LCParameters();
	protected LCTabPanel tabPanel;
	protected LCPythonThreadManager threadManager;
	protected LCProcess process;
	protected LCUpdateProcess updateProcess;
	protected LCMarkControl markControl;
	
	protected volatile boolean trainingSetLoaded;
	protected volatile boolean modelFittingFinished;
	
	public ArrayList<String> idWaitList = new ArrayList<String>(); // DELETE THIS LATER
	
	public LCControl(String unitName) {
		super(UNITTYPE, unitName);
		init();
	}
	
	protected void init() {
		PamSettingManager.getInstance().registerSettings(this);
		
		trainingSetLoaded = false;
		modelFittingFinished = true;
		
		tabPanel = new LCTabPanel(this);
		
		if (!this.isViewer) {
			runTempFolderDialogLoop("MIRFEE Live Classifier", "Live Classifier", parameters);
			threadManager = new LCPythonThreadManager(this);
		}
		
		process = new LCProcess(this);
		addPamProcess(process);
		
		updateProcess = null;
		if (this.isViewer()) {
			updateProcess = new LCUpdateProcess(this);
			markControl = new LCMarkControl(this, this.getTabPanel().getPanel());
		}
		
		if (this.getParams().timeZone == null) {
			showTimeZoneDialog();
		}
		
		if (!this.isViewer && this.getParams().getSubsetCounts().size() > 0) {
			LCSettingsDialog dialog = new LCSettingsDialog(null, this);
			dialog.validateTrainingSet();
		}
	}
	
	/**
	 * Brings up a dialog for selecting the time zone that matches the binary files (which should be local).
	 */
	public void showTimeZoneDialog() {
		String[] tz_list = TimeZone.getAvailableIDs();
		String tz = (String) JOptionPane.showInputDialog(this.getGuiFrame(),
				"Select a time zone.\n"
				+ "(NOTE: Dates and times are written into the binary files in\n"
				+ "local time for whatever reason. For both the table of results\n"
				+ "and annotation purposes, dates and times will be converted\n"
				+ "FROM the selected time zone to UTC.)", 
                this.getUnitName(), JOptionPane.QUESTION_MESSAGE, null, tz_list, "UTC");
		if (tz == null) {
			tz = "UTC";
		}
		getParams().timeZone = tz;
	}
	
	/**
	 * Converts a long to a string representing a date/time with the following format: yyyy-MM-dd HH:mm:ss+SSS
	 */
/*	@Deprecated
	public String convertLocalLongToUTC(long inp) {
		Date date = new Date(inp);
		String date_format = "yyyy-MM-dd HH:mm:ss+SSS";
		SimpleDateFormat currdateformat = new SimpleDateFormat(date_format);
		String currdate = currdateformat.format(date);
		LocalDateTime ldt = LocalDateTime.parse(currdate, DateTimeFormatter.ofPattern(date_format));
		ZoneId localZoneId = ZoneId.of(this.getParams().timeZone);
		ZoneId utcZoneId = ZoneId.of("UTC");
		ZonedDateTime localZonedDateTime = ldt.atZone(localZoneId);
		ZonedDateTime utcDateTime = localZonedDateTime.withZoneSameInstant(utcZoneId);
		DateTimeFormatter dtformat = DateTimeFormatter.ofPattern(date_format);
		return dtformat.format(utcDateTime);
	} */
		
	@Override
	public LCTabPanel getTabPanel() {
		return tabPanel;
	}
	
	/**
	 * @return The Live Classifier's Python thread manager.
	 */
	public LCPythonThreadManager getThreadManager() {
		return threadManager;
	}
	
	/**
	 * @return The Live Classifier's process.
	 */
	public LCProcess getProcess() {
		return process;
	}
	
	/**
	 * @return The Live Classifier's process for updating table values in conjunction with
	 * an instance of the Whistle and Moan Annotation Tool (WMAT).
	 */
	public LCUpdateProcess getUpdateProcess() {
		return updateProcess;
	}
	
	/**
	 * @return Whether or not the classifier is ready for processing.
	 */
	public boolean isTrainingSetLoaded() {
		return trainingSetLoaded;
	}
	
	/**
	 * Sets whether or not the classifier is ready for processing.
	 */
	public void setTrainingSetStatus(boolean inp) {
		trainingSetLoaded = inp;
	}
	
	/**
	 * @return Whether or not the classifier's machine learning models are done being fitted.
	 */
	public boolean isModelFittingFinished() {
		return modelFittingFinished;
	}
	
	/**
	 * Sets whether or not the classifier's machine learning models are done being fitted.
	 */
	public void setModelFittingStatus(boolean inp) {
		modelFittingFinished = inp;
	}
	
	public LCParameters getParams() {
		return parameters;
	}
	
	public void setParams(LCParameters inp) {
		parameters = inp;
	}

	@Override
	public Serializable getSettingsReference() {
		return parameters;
	}

	@Override
	public long getSettingsVersion() {
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		parameters = ((LCParameters) pamControlledUnitSettings.getSettings()).clone();
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
		LCSettingsDialog settingsDialog = new LCSettingsDialog(this.getPamView().getGuiFrame(), this);
	/*	if (this.isViewer()) {
			LCColourDialog colourDialog = new LCColourDialog(this.getPamView().getGuiFrame(), this, settingsDialog, true);
			colourDialog.setVisible(true);
		} else {
			settingsDialog.setVisible(true);
		} */
		settingsDialog.setVisible(true);
	}
}