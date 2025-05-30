package mirfeeTestClassifier;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import mirfeeLiveClassifier.LCControl;
import mirfeeLiveClassifier.LCMarkControl;

/**
 * The Test Classifier's PamControlledUnit, which is an extension of the Live Classifier's control unit.
 * @author Holly LeBlond
 */
public class TCControl extends LCControl {
	
	//protected TCParameters parameters = new TCParameters();
	//protected LCTrainingSetInfo loadedTestingSetInfo;
	protected TCSidePanel sidePanel;
	
	public TCControl(String unitName) {
		super(unitName);
		
		// TODO MAKE SURE TIME ZONE CARRIES OVER FROM SUPERCLASS !!!!!
	}
	
	@Override
	protected void init() {
		if (parameters == null || !(parameters instanceof TCParameters)) parameters = new TCParameters();
		PamSettingManager.getInstance().registerSettings(this);
		
		//loadedTrainingSetInfo = new LCTrainingSetInfo("");
		//loadedTestingSetInfo = new LCTrainingSetInfo("");
		tabPanel = new TCTabPanel(this);
		sidePanel = new TCSidePanel(this);
		
		// Specifically for the Test Classifier, this is done for both normal mode and viewer mode, as the Python script can be used in either.
		runTempFolderDialogLoop("MIRFEE Test Classifier", "Test Classifier", parameters);
	/*	if (parameters.tempFolder.length() == 0) {
			LCTempFolderDialog tfDialog = new LCTempFolderDialog(this.getGuiFrame(), this, "MIRFEE Test Classifier", "Test Classifier");
			tfDialog.setVisible(true);
		} else {
			File testFile = new File(parameters.tempFolder);
			if (!testFile.exists()) {
				LCTempFolderDialog tfDialog = new LCTempFolderDialog(this.getGuiFrame(), this, "MIRFEE Test Classifier", "Test Classifier");
				tfDialog.setVisible(true);
			}
		} */
		threadManager = new TCPythonThreadManager(this);
		this.removePamProcess(process);
		process = new TCProcess(this);
		this.addPamProcess(process);
		
		updateProcess = null; // No actual need for updateProcess since everything's already labelled.
		markControl = new LCMarkControl(this, this.getTabPanel().getPanel());
		
		if (this.getParams().timeZone == null) {
			showTimeZoneDialog();
		}
	}
	
	@Override
	public TCTabPanel getTabPanel() {
		return (TCTabPanel) tabPanel;
	}
	
	@Override
	public TCSidePanel getSidePanel() {
		return sidePanel;
	}
	
	@Override
	public TCPythonThreadManager getThreadManager() {
		return (TCPythonThreadManager) threadManager;
	}
	
	@Override
	public TCProcess getProcess() {
		return (TCProcess) process;
	}
	
/*	public boolean isTrainingSetLoaded() {
		return trainingSetLoaded;
	}
	
	public void setTrainingSetStatus(boolean inp) {
		trainingSetLoaded = inp;
	}
	
	public boolean isModelFittingFinished() {
		return modelFittingFinished;
	}
	
	public void setModelFittingStatus(boolean inp) {
		modelFittingFinished = inp;
	} */
	
	@Override
	public TCParameters getParams() {
		return (TCParameters) parameters;
	}
	
	public void setParams(TCParameters inp) {
		parameters = inp;
	}

	@Override
	public Serializable getSettingsReference() {
		return parameters;
	}

	@Override
	public long getSettingsVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		parameters = ((TCParameters) pamControlledUnitSettings.getSettings()).clone();
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
		TCSettingsDialog settingsDialog = new TCSettingsDialog(this.getPamView().getGuiFrame(), this);
		settingsDialog.setVisible(true);
	}
}