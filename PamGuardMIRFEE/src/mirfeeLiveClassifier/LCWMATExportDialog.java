package mirfeeLiveClassifier;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.tensorflow.proto.example.FeatureList;

import PamController.PamControlledUnit;
import PamView.PamTable;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamguardMVC.PamDataBlock;
import binaryFileStorage.BinaryStore;
import generalDatabase.DBControl;
import mirfee.MIRFEEInfo;
import mirfeeFeatureExtractor.FEControl;
import mirfeeFeatureExtractor.FEDataBlock;
import mirfeeFeatureExtractor.FEParameters;
import wmat.WMATControl;

/**
 * Dialog for exporting predictions to WMAT.
 * @author Holly LeBlond
 */
@Deprecated
public class LCWMATExportDialog extends PamDialog {
	
	protected LCControl lcControl;
	protected WMATControl wmatControl;
	protected JComboBox<String> exportOptionBox;
	protected JComboBox<String> minCertaintyBox;
	protected JCheckBox overwriteCheck;
	
	public LCWMATExportDialog(LCControl lcControl, WMATControl wmatControl, Window parentFrame) {
		super(parentFrame, "MIRFEE Live Classifier", false);
		this.lcControl = lcControl;
		this.wmatControl = wmatControl;
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		mainPanel.setBorder(new TitledBorder("Export what?"));
		b.anchor = b.NORTHWEST;
		b.fill = b.HORIZONTAL;
		b.gridwidth = 2;
		//mainPanel.add(new JLabel("Export what?"), b);
		//b.gridy++;
		exportOptionBox = new JComboBox<String>(new String[] {"Overall cluster predictions", "Individual contour predictions"});
		mainPanel.add(exportOptionBox, b);
		b.gridy++;
		b.gridwidth = 1;
		mainPanel.add(new JLabel("Minimum certainty:"), b);
		b.gridx++;
		minCertaintyBox = new JComboBox<String>(new String[] {"Very low", "Low", "Average", "High", "Very high"});
		minCertaintyBox.setSelectedItem(lcControl.getParams().worstCertainty);
		mainPanel.add(minCertaintyBox, b);
		b.gridy++;
		b.gridx = 0;
		b.gridwidth = 2;
		overwriteCheck = new JCheckBox("Overwrite non-blank annotations");
		overwriteCheck.setSelected(false);
		mainPanel.add(overwriteCheck, b);
		this.setDialogComponent(mainPanel);
		
	}

	@Override
	public boolean getParams() {
		int res = JOptionPane.showOptionDialog(
				this,
				lcControl.makeHTML("Prediction data will be written into the WMAT species column. Optionally, you can mark in the comments column that the "
						+ "annotation was produced by the Live Classifier, which is strongly recommended. Proceed?", 300),
				"MIRFEE Live Classifier",
				JOptionPane.YES_NO_CANCEL_OPTION, 
				JOptionPane.WARNING_MESSAGE,
				null,
				new String[] {"Yes, mark in comments", "Yes, without comments", "Cancel"},
				"Yes, mark in comments");
		if (res == JOptionPane.CANCEL_OPTION)
			return false;
		if (overwriteCheck.isSelected()) {
			String message;
			if (res == JOptionPane.YES_OPTION)
				message = "Are you sure? Pre-existing annotations in the species and comments columns will be overwritten. This can be undone with the Undo button.";
			else
				message = "Are you sure? Pre-existing annotations in the species column will be overwritten. This can be undone with the Undo button.";
			int res2 = JOptionPane.showConfirmDialog(
					this,
					lcControl.makeHTML(message, 300),
					"MIRFEE Live Classifier",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (res2 == JOptionPane.NO_OPTION)
				return false;
		}
		double minCertainty = 0.0; // Very low
		if (minCertaintyBox.getSelectedIndex() == 1) // Low
			minCertainty = lcControl.getParams().veryLow;
		if (minCertaintyBox.getSelectedIndex() == 2) // Average
			minCertainty = lcControl.getParams().low;
		if (minCertaintyBox.getSelectedIndex() == 3) // High
			minCertainty = lcControl.getParams().average;
		if (minCertaintyBox.getSelectedIndex() == 4) // Very high
			minCertainty = lcControl.getParams().high;
		int updateCount = wmatControl.importLCPredictions(lcControl.getProcess().resultsDataBlock, exportOptionBox.getSelectedIndex() == 1,
															res == JOptionPane.YES_OPTION, overwriteCheck.isSelected(), minCertainty);
		if (updateCount == -1) {
			lcControl.simpleErrorDialog("No data has been loaded into the WMAT.");
		} else if (updateCount == 0) {
			JOptionPane.showMessageDialog(this, "No entries in the WMAT were updated, as no matches were found.", "MIRFEE Live Classifier", JOptionPane.WARNING_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this, String.valueOf(updateCount)+" entries in the WMAT were updated.");
		}
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		// (Closes dialog.)
	}

	@Override
	public void restoreDefaultSettings() {
		// (Button disabled.)
	}
	
}