package wmat;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamTable;
import mirfee.MIRFEEControlledUnit;
import mirfeeLiveClassifier.LCCallCluster;
import mirfeeLiveClassifier.LCControl;
import mirfeeLiveClassifier.LCDataBlock;
import mirfeeLiveClassifier.LCDataUnit;
import mirfeeLiveClassifier.LCParameters;
import whistlesAndMoans.*;

/**
 * The controller class for the Whistle and Moan Annotation Tool.
 * @author Holly LeBlond
 */
public class WMATControl extends PamControlledUnit implements PamSettings {
	
	protected WMATParameters parameters = new WMATParameters();
	
	protected WMATSidePanel wmatSidePanel;
	
	protected WMATProcess wmatProcess;
	
	public WMATControl(String unitName) {
		super("WMAT", unitName);
		PamSettingManager.getInstance().registerSettings(this);
		
		wmatSidePanel = new WMATSidePanel(this);
		setSidePanel(wmatSidePanel);
		
		this.wmatProcess = new WMATProcess(this, null);
		this.addPamProcess(wmatProcess);
		
		this.settingsDialog(null, true);
	}
	
	public WMATProcess getProcess() {
		return wmatProcess;
	}
	
	/**
	 * Streamlined error dialog.
	 */
	public void simpleErrorDialog() {
		JOptionPane.showMessageDialog(this.getGuiFrame(),
			"An error has occured.\nSee console for details.",
			"Whistle and Moan Annotation Tool",
			JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Streamlined error dialog with an editable message.
	 */
	public void simpleErrorDialog(String inptext) {
		JOptionPane.showMessageDialog(this.getGuiFrame(),
			inptext,
			"Whistle and Moan Annotation Tool",
			JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Converts a date/time string formatted like yyyy-MM-dd HH:mm:ss(+SSS) to a new time zone.
	 * @param tz1name - The original time zone's name
	 * @param tz2name - The new time zone's name
	 * @param originalDate - The original date/time string
	 * @param includeMilliseconds - Whether or not milliseconds should be appended to the string
	 */
	public String convertBetweenTimeZones(String tz1name, String tz2name, String originalDate, boolean includeMilliseconds) {
		try {
			String date_format = "yyyy-MM-dd HH:mm:ss";
			if (includeMilliseconds) date_format += "+SSS";
			LocalDateTime ldt = LocalDateTime.parse(originalDate, DateTimeFormatter.ofPattern(date_format));
			ZoneId tz1 = ZoneId.of(tz1name);
			ZoneId tz2 = ZoneId.of(tz2name);
			ZonedDateTime originalZDT = ldt.atZone(tz1);
			ZonedDateTime newZDT = originalZDT.withZoneSameInstant(tz2);
			DateTimeFormatter dtformat = DateTimeFormatter.ofPattern(date_format);
			return dtformat.format(newZDT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Deprecated
	public int importLCPredictions(LCDataBlock lcDB, boolean matchIndividualContours, boolean markComments, boolean overwrite, double minCertainty) {
		return this.getSidePanel().getWMATPanel().importLCPredictions(lcDB, matchIndividualContours, markComments, overwrite, minCertainty);
	}
	
	/**
	 * @return WMATSidePanel
	 */
	@Override
	public WMATSidePanel getSidePanel() {
		return wmatSidePanel;
	}
	
	protected WMATParameters getParams() {
		return parameters;
	}
	
	protected void setParams(WMATParameters inp) {
		parameters = inp;
	}
	
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem("Whistle and Moan Annotation Tool");
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
			settingsDialog(parentFrame, false);	
		}

	}
	
	/**
	 * Opens the settings dialog.
	 * @param parentFrame
	 */
	protected void settingsDialog(Frame parentFrame, boolean startup) {
		WMATSettingsDialog settingsDialog = new WMATSettingsDialog(parentFrame, this, startup);
		settingsDialog.setVisible(true);
	}

	@Override
	public Serializable getSettingsReference() {
		return parameters;
	}

	@Override
	public long getSettingsVersion() {
		// TODO
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		WMATParameters newParams = (WMATParameters) pamControlledUnitSettings.getSettings();
		parameters = newParams;
		//parameters = newParams.clone(); (not sure why this doesn't work - Cloneable should be imported)
		return true;
	}
}