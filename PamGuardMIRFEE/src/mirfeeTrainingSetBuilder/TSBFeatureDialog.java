package mirfeeTrainingSetBuilder;

import java.awt.*;

import javax.swing.*;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import PamView.dialog.PamDialog;

/**
 * Dialog that occurs before output for selecting which features will occur in the new training set.
 * @author Holly LeBlond
 */
public class TSBFeatureDialog extends PamDialog {
	
	TSBControl tsbControl;
	private Window parentFrame;
	protected PamPanel mainPanel;
	protected DefaultListModel dlModel;
	protected JList featureSelectionList;
	//protected int[] featureIndices;
	
	public TSBFeatureDialog(Window parentFrame, TSBControl tsbControl) {
		super(parentFrame, "MIRFEE Training Set Builder", true);
		
		this.tsbControl = tsbControl;
		this.parentFrame = parentFrame;
		//this.featureIndices = new int[0];
		
		mainPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		
		b.anchor = b.WEST;
		mainPanel.add(new JLabel("Select which features to include in the training set:"), b);
		dlModel = new DefaultListModel();
		for (int i = 0; i < tsbControl.getFeatureList().size(); i++) {
			dlModel.addElement(tsbControl.getFeatureList().get(i));
		}
		featureSelectionList = new JList<String>(dlModel);
		featureSelectionList.setSelectionModel(new DefaultListSelectionModel() {
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
		featureSelectionList.setLayoutOrientation(JList.VERTICAL);
		int[] selectAll = new int[featureSelectionList.getModel().getSize()];
		for (int i = 0; i < selectAll.length; i++) {
			selectAll[i] = i;
		}
		featureSelectionList.setSelectedIndices(selectAll);
		JScrollPane sp = new JScrollPane(featureSelectionList);
		b.gridy++;
		mainPanel.add(sp, b);
		
		setDialogComponent(mainPanel);
	}

	@Override
	public boolean getParams() {
		if (featureSelectionList.getSelectedIndices().length < 2) {
			tsbControl.simpleErrorDialog("At least two features must be selected.", 150);
			return false;
		}
		tsbControl.getTabPanel().getPanel().setOutputFeatureIndices(featureSelectionList.getSelectedIndices());
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		tsbControl.getTabPanel().getPanel().setOutputFeatureIndices(new int[0]);
	}

	@Override
	public void restoreDefaultSettings() {
		int[] selectAll = new int[featureSelectionList.getModel().getSize()];
		for (int i = 0; i < selectAll.length; i++) {
			selectAll[i] = i;
		}
		featureSelectionList.setSelectedIndices(selectAll);
	}
}