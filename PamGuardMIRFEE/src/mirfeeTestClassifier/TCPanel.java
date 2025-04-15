package mirfeeTestClassifier;

import java.awt.GridLayout;

import javax.swing.JPanel;

import mirfeeLiveClassifier.LCPanel;
import mirfeeLiveClassifier.LCWaitingDialogThread;

/**
 * The panel where the GUI components are written.
 * Subclass of the Live Classifier's GUI panel.
 * @author Holly LeBlond
 */
public class TCPanel extends LCPanel {
	
	public TCPanel(TCControl control) {
		super(control, true);
		
		bestFeaturesButton.removeActionListener(null);
		//wmntButton.setEnabled(false);
	}
	
	@Override
	protected void bestFeaturesButtonAction() {
		TCParameters params = getControl().getParams();
		if (params.getTrainingSetInfo() == null || params.getTrainPath().equals("")) {
			getControl().simpleErrorDialog("Training and/or testing sets have not been configured yet.", 250);
			return;
		}
		wdThread = new LCWaitingDialogThread(getControl().getGuiFrame(), getControl(), "Waiting for response from Python script...");
		wdThread.start();
		if (getControl().getThreadManager().initializeBestFeaturesSet()) {
			//getControl().getThreadManager().pythonCommand("tcmBest.printBestFeatureOrder()", getControl().getParams().printInput);
		}
		else wdThread.halt();
	}
	
	@Override
	protected void exportButtonListenerAction() {
		TCExportDialog exportDialog = new TCExportDialog(getControl(), getControl().getPamView().getGuiFrame());
		exportDialog.setVisible(true);
	}
	
	// Only difference is that this one cuts off the bottom two rows of the confusion matrix.
	@Override
	protected void showMatrices() {
		JPanel matricesPanel = new JPanel(new GridLayout(2, 1, 10, 50));
		JPanel cmGridPanel = new JPanel(new GridLayout(jLabelConfMatrix.length-2, jLabelConfMatrix[0].length, 10, 10));
		for (int i = 0; i < jLabelConfMatrix.length-2; i++) {
			for (int j = 0; j < jLabelConfMatrix[i].length; j++) {
				cmGridPanel.add(jLabelConfMatrix[i][j]);
			}
		}
		matricesPanel.add(cmGridPanel);
		JPanel amGridPanel = new JPanel(new GridLayout(jLabelAccuracyMatrix.length, jLabelAccuracyMatrix[0].length, 10, 10));
		for (int i = 0; i < jLabelAccuracyMatrix.length; i++) {
			for (int j = 0; j < jLabelAccuracyMatrix[i].length; j++) {
				amGridPanel.add(jLabelAccuracyMatrix[i][j]);
			}
		}
		matricesPanel.add(amGridPanel);
		cmCardsPanel.add(matricesPanel, "matricesPanel");
		cl.show(cmCardsPanel, "matricesPanel");
	}
	
	protected TCControl getControl() {
		return (TCControl) control;
	}
}