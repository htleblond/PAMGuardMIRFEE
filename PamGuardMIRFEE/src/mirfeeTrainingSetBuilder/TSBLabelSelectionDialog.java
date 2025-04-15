package mirfeeTrainingSetBuilder;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import PamView.dialog.PamDialog;

/**
 * Dialog that occurs before output for selecting which labels will occur in the new training set.
 * @author Holly LeBlond
 */
public class TSBLabelSelectionDialog extends PamDialog {
	
	TSBControl tsbControl;
	private Window parentFrame;
	protected PamPanel mainPanel;
	protected DefaultListModel dlModel;
	protected JList labelSelectionList;
	
	public TSBLabelSelectionDialog(Window parentFrame, TSBControl tsbControl) {
		super(parentFrame, tsbControl.getUnitName(), true);
		
		this.tsbControl = tsbControl;
		this.parentFrame = parentFrame;
		
		mainPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		
		b.anchor = b.WEST;
		mainPanel.add(new JLabel("Select which labels to include in the training set:"), b);
		dlModel = new DefaultListModel();
		
		for (int i = 0; i < tsbControl.getOutputClassLabels().size(); i++) {
			dlModel.addElement(tsbControl.getOutputClassLabels().get(i));
		}
		labelSelectionList = new JList<String>(dlModel);
		labelSelectionList.setSelectionModel(new DefaultListSelectionModel() {
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
		labelSelectionList.setLayoutOrientation(JList.VERTICAL);
		int[] selectAll = new int[labelSelectionList.getModel().getSize()];
		for (int i = 0; i < selectAll.length; i++) {
			selectAll[i] = i;
		}
		labelSelectionList.setSelectedIndices(selectAll);
		JScrollPane sp = new JScrollPane(labelSelectionList);
		b.gridy++;
		mainPanel.add(sp, b);
		
		setDialogComponent(mainPanel);
	}

	@Override
	public boolean getParams() {
		int[] selectedIndices = labelSelectionList.getSelectedIndices();
	/*	if (selectedIndices.length < 2) { // Could be testing set with only one class.
			tsbControl.simpleErrorDialog("At least two labels must be selected.", 150);
			return false;
		} */
		ArrayList<String> outp = new ArrayList<String>();
		for (int i = 0; i < selectedIndices.length; i++)
			outp.add(tsbControl.getOutputClassLabels().get(selectedIndices[i]));
		tsbControl.getTabPanel().getPanel().setOutputLabelList(outp);
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		tsbControl.getTabPanel().getPanel().setOutputLabelList(new ArrayList<String>());
	}

	@Override
	public void restoreDefaultSettings() {
		int[] selectAll = new int[labelSelectionList.getModel().getSize()];
		for (int i = 0; i < selectAll.length; i++) {
			selectAll[i] = i;
		}
		labelSelectionList.setSelectedIndices(selectAll);
	}
}