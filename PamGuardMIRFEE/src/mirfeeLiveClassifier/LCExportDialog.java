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

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

/**
 * Dialog for exporting results.
 * @author Holly LeBlond
 */
public class LCExportDialog extends PamDialog {
	
	protected LCControl lcControl;
	protected JComboBox<String> optionsBox;
	protected String dbName;
	protected String bsName;
	
	protected PrintWriter pw;
	protected StringBuilder sb;
	
	public LCExportDialog(LCControl lcControl, Window parentFrame) {
		super(parentFrame, "MIRFEE Live Classifier", false);
		this.lcControl = lcControl;
		
		dbName = "";
		bsName = "";
		for (int i = 0; i < lcControl.getPamController().getNumControlledUnits(); i++) {
			PamControlledUnit pcu = lcControl.getPamController().getControlledUnit(i);
			if (pcu.getUnitType().equals("Pamguard Database")) {
				DBControl dbc = (DBControl) pcu;
				dbName = dbc.getDatabaseName();
			}
		}
		BinaryStore bs = BinaryStore.findBinaryStoreControl();
		if (bs != null) {
			bsName = bs.getBinaryStoreSettings().getStoreLocation();
			if (bsName == null) {
				bsName = "";
			}
		}
		
		this.getOkButton().setText("Export");
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		b.anchor = b.NORTHWEST;
		b.fill = b.NONE;
		mainPanel.add(new JLabel("Export what?"), b);
		b.gridy++;
		optionsBox = new JComboBox<String>(new String[] {"Cluster results (.csv)",
														 "Individual contour results (.csv)",
														 "Accuracy matrix (.csv)"});
		if (lcControl.isViewer()) {
			optionsBox.addItem("Confusion matrix (.csv)");
			optionsBox.addItem("Full results with summary (.txt)");
		}
		mainPanel.add(optionsBox, b);
		
		this.setDialogComponent(mainPanel);
	}
	
	/**
	 * Opens a file chooser.
	 * @param selectedIndex - The index of the selected option in optionsBox
	 * @return The selected file
	 */
	protected File selectFile(int selectedIndex) {
		File outp = null;
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setMultiSelectionEnabled(false);
		String tsName = lcControl.getParams().getTrainPath().replace(".mirrfts", "");
		tsName.replace(".mtsf", "");
		if (selectedIndex == 0) {
			fc.setSelectedFile(new File(tsName+", cluster results.csv"));
		} else if (selectedIndex == 1) {
			fc.setSelectedFile(new File(tsName+", individual contour results.csv"));
		} else if (selectedIndex == 2) {
			fc.setSelectedFile(new File(tsName+", accuracy matrix.csv"));
		} else if (selectedIndex == 3) {
			fc.setSelectedFile(new File(tsName+", confusion matrix.csv"));
		} else if (selectedIndex == 4) {
			fc.setSelectedFile(new File(tsName+", full results.txt"));
		}
		if (selectedIndex != 4) {
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
		} else {
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
		}
		int returnVal = fc.showSaveDialog(lcControl.getGuiFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			outp = getSelectedFileWithExtension(fc);
			outp.setWritable(true);
			if (outp.exists()) {
				int res = JOptionPane.showConfirmDialog(lcControl.getGuiFrame(),
						"Overwrite selected file?",
						lcControl.getUnitName(),
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.YES_OPTION) {
					outp.setExecutable(true);
				} else {
					return null;
				}
			} else {
				try {
					outp.createNewFile();
				} catch (Exception e2) {
					e2.printStackTrace();
					lcControl.simpleErrorDialog("Could not create new file.\nSee console for details.", 300);
					return null;
				}
			}
		}
		return outp;
	}
	
	/**
	 * Returns the selected file from a JFileChooser, including the extension from
	 * the file filter.
	 * 
	 * Copied from here: https://stackoverflow.com/questions/16846078/jfilechoosershowsavedialog-cant-get-the-value-of-the-extension-file-chosen
	 * Author page: https://stackoverflow.com/users/964243/boann
	 */
	public static File getSelectedFileWithExtension(JFileChooser c) {
	    File file = c.getSelectedFile();
	    if (c.getFileFilter() instanceof FileNameExtensionFilter) {
	        String[] exts = ((FileNameExtensionFilter)c.getFileFilter()).getExtensions();
	        String nameLower = file.getName().toLowerCase();
	        for (String ext : exts) { // check if it already has a valid extension
	            if (nameLower.endsWith('.' + ext.toLowerCase())) {
	                return file; // if yes, return as-is
	            }
	        }
	        // if not, append the first extension from the selected filter
	        file = new File(file.toString() + '.' + exts[0]);
	    }
	    return file;
	}
	
	/**
	 * Basically exports the table into a .csv file.
	 */
	protected void exportClusterResults() {
		sb.append("Cluster ID,First UID,Last UID,Date/Time (UTC),n,Actual species,Predicted species,"
				+ "Prediction counter,Prediction probabilities,Certainty,Certainty descriptor\n");
		pw.write(sb.toString());
		pw.flush();
		LCDataBlock db = (LCDataBlock) lcControl.getProcess().getOutputDataBlock(0);
		HashMap<String, ArrayList<Long>> uidMap = db.retrieveAllUIDsByIDandDate();
		PamTable table = lcControl.getTabPanel().getPanel().getTable();
		for (int i = 0; i < table.getRowCount(); i++) {
			sb = new StringBuilder();
			sb.append(table.getValueAt(i, 0)+",");
			ArrayList<Long> uidList = uidMap.get(table.getValueAt(i, 0)+", "+table.getValueAt(i, 1));
			sb.append(String.valueOf(uidList.get(0))+",");
			sb.append(String.valueOf(uidList.get(uidList.size()-1))+",");
			sb.append(table.getValueAt(i, 1)+",");
			sb.append(String.valueOf(table.getValueAt(i, 2))+",");
			if (lcControl.isViewer()) {
				sb.append(table.getValueAt(i, 7)+",");
			} else {
				sb.append("Unlabelled,");
			}
			sb.append(table.getValueAt(i, 3)+",");
			sb.append(table.getValueAt(i, 4)+",");
			sb.append(table.getValueAt(i, 5)+",");
			String certainty = (String) table.getValueAt(i, 6);
			sb.append(certainty.substring(0, 4)+",");
			sb.append(certainty.substring(6, certainty.length()-1));
			if (i < table.getRowCount()-1) {
				sb.append("\n");
			}
			pw.write(sb.toString());
			pw.flush();
		}
		pw.close();
		JOptionPane.showMessageDialog(lcControl.getGuiFrame(),
				"Table successfully exported to .csv file.",
				lcControl.getUnitName(),
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Exports all individual contour results into a .csv file.
	 */
	protected void exportIndividualContourResults() {
		sb.append("Cluster ID,UID,Location,Date/Time (UTC),Duration (ms),Lowest frequency,Highest Frequency,Actual Species,Predicted Species,");
		for (int i = 0; i < lcControl.getParams().labelOrder.length; i++) {
			sb.append(lcControl.getParams().labelOrder[i]+" probability score,");
		}
		sb.append("Certainty\n");
		pw.write(sb.toString());
		pw.flush();
		LCDataBlock db = (LCDataBlock) lcControl.getProcess().getOutputDataBlock(0);
		for (int i = 0; i < db.getUnitsCount(); i++) {
			LCDataUnit du = db.getDataUnit(i, db.REFERENCE_CURRENT);
			LCCallCluster cc = du.getCluster();
			for (int j = 0; j < cc.getSize(); j++) {
				sb = new StringBuilder();
				sb.append(cc.clusterID+",");
				sb.append(String.valueOf(cc.uids[j])+",");
				if (cc.location.length() > 0)
					sb.append(cc.location+",");
				else
					sb.append("n/a,");
				sb.append(lcControl.convertDateLongToString(lcControl.convertFromLocalToUTC(cc.datetimes[j]))+",");
				sb.append(String.valueOf(cc.durations[j])+",");
				sb.append(String.valueOf(cc.lfs[j])+",");
				sb.append(String.valueOf(cc.hfs[j])+",");
				if (lcControl.isViewer()) {
					sb.append(cc.getActualSpeciesString()+",");
				} else {
					sb.append("Unlabelled,");
				}
				sb.append(cc.getPredictedSpeciesString()+",");
				for (int k = 0; k < cc.probaList[j].length; k++) {
					sb.append(String.format("%.2f", (float) cc.probaList[j][k])+",");
				}
				sb.append(String.format("%.2f", (float) cc.getIndividualCertainty(j)));
				if (i < db.getUnitsCount()-1 || j < cc.getSize()-1) {
					sb.append("\n");
				}
				pw.write(sb.toString());
				pw.flush();
			}
		}
		pw.close();
		JOptionPane.showMessageDialog(lcControl.getGuiFrame(),
				"Individual contour data successfully exported to .csv file.",
				lcControl.getUnitName(),
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Exports either the accuracy matrix or the confusion matrix into a .csv file.
	 * @param doConfusion - If true, exports confusion matrix. If false, exports accuracy matrix.
	 */
	protected void exportMatrix(boolean doConfusion) {
		LCPanel panel = (LCPanel) lcControl.getTabPanel().getPanel();
		JLabel[][] matrix;
		if (doConfusion) {
			matrix = panel.getConfusionMatrixLabels();
		} else {
			matrix = panel.getAccuracyMatrixLabels();
		}
		for (int i = 0; i < matrix.length; i++) {
			sb = new StringBuilder();
			for (int j = 0; j < matrix[i].length; j++) {
				//System.out.println(matrix[i][j].getText());
				if (matrix[i][j].getText().equals("Precision")) sb.append("Prcsn.");
				else sb.append(matrix[i][j].getText());
				if (j < matrix[i].length-1) {
					sb.append(",");
				}
			}
			if (i < matrix.length-1) {
				sb.append("\n");
			}
			pw.write(sb.toString());
			pw.flush();
		}
		pw.close();
		String msg = "Confusion matrix successfully exported to .csv file.";
		if (!doConfusion) {
			msg = "Accuracy matrix successfully exported to .csv file.";
		}
		JOptionPane.showMessageDialog(lcControl.getGuiFrame(),
				msg,
				lcControl.getUnitName(),
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Exports detailed classification results into a .txt file.
	 */
	protected void exportFullResults() {
		sb = produceHeaderInfo(sb);
		sb = produceFeatureExtractorInfo(sb);
		sb = produceClassifierInfo(sb);
		sb = produceMatrixInfo(sb);
		sb = produceIndividualContourInfo(sb);
		
		pw.close();
		JOptionPane.showMessageDialog(lcControl.getGuiFrame(),
				"Full results successfully exported to .txt file.",
				lcControl.getUnitName(),
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Produces header info for "full results".
	 */
	protected StringBuilder produceHeaderInfo(StringBuilder sb) {
		sb.append(lcControl.getUnitName().toUpperCase()+", FULL RESULTS\n\n");
		sb.append("NOTE - Some of the parameters listed here may be incorrect if the binary files were changed, "
				+ "or if any of the modules were replaced.\n\n");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
		long currtime = lcControl.convertBetweenTimeZones(System.currentTimeMillis(), TimeZone.getDefault().getID(), "UTC");
		sb.append("Export time (UTC): "+sdf.format(new Date(currtime))+"\n");
		sb.append("Plugin version: "+MIRFEEInfo.getVersion()+"\n");
		sb.append("Database: "+dbName+"\n");
		sb.append("Binary folder: "+bsName+"\n\n");
		pw.write(sb.toString());
		pw.flush();
		return sb;
	}
	
	/**
	 * Produces Feature Extractor header info for "full results".
	 */
	protected StringBuilder produceFeatureExtractorInfo(StringBuilder sb) {
		//FEParameters feParams = null;
		LCParameters lcParams = lcControl.getParams();
	/*	for (int i = 0; i < lcControl.getPamController().getDataBlocks().size(); i++) {
			PamDataBlock db = lcControl.getPamController().getDataBlocks().get(i);
			if (db.getDataName().equals(lcControl.getParams().inputProcessName)) {
				FEDataBlock fedb = (FEDataBlock) db;
				FEControl fec = (FEControl) fedb.getParentProcess().getPamControlledUnit();
				feParams = fec.getParams();
				break;
			}
		} */
		FEParameters feParams = null;
		FEDataBlock fedb = lcParams.getInputFEDataBlock(lcControl);
		if (fedb != null) feParams = fedb.getParamsClone();
		sb = new StringBuilder();
		boolean printFEParamsFoundInTrainingSet = false;
		if (feParams != null && feParams.findUnmatchedParameters(lcParams.getTrainingSetInfo().feParamsMap, true).size() > 0) {
			printFEParamsFoundInTrainingSet = true;
			sb.append("NOTE - Different Feature Extractor parameters were found in the training set file.\n\n");
		}
		sb.append("FEATURE EXTRACTOR PARAMETERS\n\n");
		if (feParams == null) {
			sb.append("(Feature Extractor module not found.)\n\n");
			if (lcParams.getTrainingSetInfo() != null) {
				sb.append("Features (from training set): "+String.valueOf(lcParams.getFeatureList().size()));
				for (int i = 0; i < lcParams.getFeatureList().size(); i++) {
					sb.append("\t"+lcParams.getFeatureList().get(i)+"\n");
				}
				sb.append("\n");
			}
		} else {
			sb = printParamsFromFEParametersObject(sb, feParams);
		}
		if (printFEParamsFoundInTrainingSet) {
			sb = printFEParamsFromTrainingSetFile(sb, lcParams.getTrainingSetInfo().feParamsMap, feParams,
					"CONTRADICTORY FEATURE EXTRACTOR PARAMETERS FOUND IN TRAINING SET FILE");
		}
		pw.write(sb.toString());
		pw.flush();
		return sb;
	}
	
	/**
	 * Produces Feature Extractor settings info from a loaded Feature Extractor module for "full results".
	 */
	protected StringBuilder printParamsFromFEParametersObject(StringBuilder sb, FEParameters feParams) {
		if (feParams.inputFromWMATorMTSF) {
			sb.append("Input source: CSV file\n");
			sb.append("Input source name: ");
			for (int i = 0; i < feParams.inputWMATFileNames.size(); i++) {
				sb.append(new File(feParams.inputWMATFileNames.get(i)));
				if (i < feParams.inputWMATFileNames.size()-1) sb.append(", ");
			}
			sb.append("\n");
		} else {
			sb.append("Input source: Live data from Whistle and Moan Detector\n");
			sb.append("Input source name: "+feParams.inputProcessName+"\n");
		}
		sb.append("Audio data source: "+feParams.audioSourceProcessName+"\n");
		sb.append("Audio sampling rate: "+String.valueOf(feParams.sr)+" Hz\n");
		if (feParams.audioAutoClipLength) {
			sb.append("Audio clip length: Full length of contour\n");
		} else {
			sb.append("Audio clip length: "+String.valueOf(feParams.audioClipLength)+" ms\n");
		}
		sb.append("STFT length: "+String.valueOf(feParams.audioSTFTLength)+" bins\n");
		sb.append("STFT hop size: "+String.valueOf(feParams.audioHopSize)+" samples\n");
		sb.append("STFT window function: "+feParams.audioWindowFunction+"\n");
		sb.append("STFT normalized: "+String.valueOf(feParams.audioNormalizeChecked)+"\n");
		if (feParams.audioHPFChecked) {
			sb.append("High-pass filter: On\n");
			sb.append("High-pass filter threshold: "+String.valueOf(feParams.audioHPFThreshold)+" Hz\n");
			sb.append("High-pass filter magnitude: "+String.valueOf(feParams.audioHPFMagnitude)+"\n");
		} else {
			sb.append("High-pass filter: Off\n");
		}
		if (feParams.audioLPFChecked) {
			sb.append("Low-pass filter: On\n");
			sb.append("Low-pass filter threshold: "+String.valueOf(feParams.audioLPFThreshold)+" Hz\n");
			sb.append("Low-pass filter magnitude: "+String.valueOf(feParams.audioLPFMagnitude)+"\n");
		} else {
			sb.append("Low-pass filter: Off\n");
		}
		if (feParams.audioNRChecked) {
			sb.append("Noise reduction: On\n");
			sb.append("Noise reduction clip start time: "+String.valueOf(feParams.audioNRStart)+" ms\n");
			sb.append("Noise reduction clip length: "+String.valueOf(feParams.audioNRLength)+" ms\n");
			sb.append("Noise reduction scalar: "+String.valueOf(feParams.audioNRScalar)+"\n");
		} else {
			sb.append("Noise reduction: Off\n");
		}
		sb.append("Features: "+String.valueOf(feParams.featureList.length)+"\n");
		for (int i = 0; i < feParams.featureList.length; i++) {
			sb.append("\t"+feParams.featureList[i][1]+"\n");
		}
		sb.append("Contours sorted into clusters: "+String.valueOf(feParams.miscClusterChecked)+"\n");
		if (feParams.miscClusterChecked) {
			sb.append("Maximum cluster join distance: "+String.valueOf(feParams.miscJoinDistance)+" ms\n");
		}
		if (feParams.miscIgnoreFileStartChecked) {
			sb.append("All processed contours occur after: "+String.valueOf(feParams.miscIgnoreFileStartLength)+" ms\n");
		}
		if (feParams.miscIgnoreLowFreqChecked) {
			sb.append("All processed contours are higher than: "+String.valueOf(feParams.miscIgnoreLowFreq)+" Hz\n");
		}
		if (feParams.miscIgnoreHighFreqChecked) {
			sb.append("All processed contours are lower than: "+String.valueOf(feParams.miscIgnoreHighFreq)+" Hz\n");
		}
		if (feParams.miscIgnoreShortDurChecked) {
			sb.append("All processed contours are longer than: "+String.valueOf(feParams.miscIgnoreShortDur)+" ms\n");
		}
		if (feParams.miscIgnoreLongDurChecked) {
			sb.append("All processed contours are shorter than: "+String.valueOf(feParams.miscIgnoreLongDur)+" ms\n");
		}
		if (feParams.miscIgnoreQuietAmpChecked) {
			sb.append("All processed contours are louder than: "+String.valueOf(feParams.miscIgnoreQuietAmp)+" dB re SPSL\n");
		}
		if (feParams.miscIgnoreLoudAmpChecked) {
			sb.append("All processed contours are quieter than: "+String.valueOf(feParams.miscIgnoreLoudAmp)+" dB re SPSL\n");
		}
		sb.append("\n");
		return sb;
	}
	
	/**
	 * Produces unmatched Feature Extractor settings info from the loaded training set for "full results".
	 */
	protected StringBuilder printFEParamsFromTrainingSetFile(StringBuilder sb, HashMap<String, String> map, 
			FEParameters feParams, String message) {
		ArrayList<String> unmatched = new ArrayList<String>();
		if (feParams != null) {
			unmatched = feParams.findUnmatchedParameters(map, true);
		} else {
			Iterator<String> it = map.keySet().iterator();
			while (it.hasNext()) unmatched.add(it.next());
		}
		//for (int i = 0; i < unmatched.size(); i++) System.out.println(unmatched.get(i)); // TODO REMOVE
		if (unmatched.size() > 0) {
			sb.append(message+"\n\n");
			if (unmatched.contains("sr")) sb.append("Audio sampling rate: "+map.get("sr")+" Hz\n");
			if (unmatched.contains("audioAutoClipLength")) {
				if (Boolean.valueOf(map.get("audioAutoClipLength"))) sb.append("Audio clip length: Full length of contour\n");
				else {
					if (unmatched.contains("audioClipLength"))
						sb.append("Audio clip length: "+map.get("audioClipLength")+" ms\n");
					else sb.append("Audio clip length: ?\n");
				}
			} else if (unmatched.contains("audioClipLength")) sb.append("Audio clip length: "+map.get("audioClipLength")+" ms\n");
			if (unmatched.contains("audioSTFTLength")) sb.append("STFT length: "+map.get("audioSTFTLength")+" bins\n");
			if (unmatched.contains("audioHopSize")) sb.append("STFT hop size: "+map.get("audioHopSize")+" samples\n");
			if (unmatched.contains("audioWindowFunction")) sb.append("STFT window function: "+map.get("audioWindowFunction")+"\n");
			if (unmatched.contains("audioNormalizeChecked")) sb.append("STFT normalized: "+map.get("audioNormalizeChecked")+"\n");
			if (unmatched.contains("audioHPFChecked")) {
				if (Boolean.valueOf(map.get("audioHPFChecked"))) sb.append("High-pass filter: On\n");
				else sb.append("High-pass filter: Off\n");
			}
			if (unmatched.contains("audioHPFThreshold")) sb.append("High-pass filter threshold: "+map.get("audioHPFThreshold")+"\n");
			if (unmatched.contains("audioHPFMagnitude")) sb.append("High-pass filter magnitude: "+map.get("audioHPFMagnitude")+"\n");
			if (unmatched.contains("audioLPFChecked")) {
				if (Boolean.valueOf(map.get("audioLPFChecked"))) sb.append("Low-pass filter: On\n");
				else sb.append("Low-pass filter: Off\n");
			}
			if (unmatched.contains("audioLPFThreshold")) sb.append("Low-pass filter threshold: "+map.get("audioLPFThreshold")+"\n");
			if (unmatched.contains("audioLPFMagnitude")) sb.append("Low-pass filter magnitude: "+map.get("audioLPFMagnitude")+"\n");
			if (unmatched.contains("audioNRChecked")) {
				if (Boolean.valueOf(map.get("audioNRChecked"))) sb.append("Noise reduction: On\n");
				else sb.append("Noise reduction: Off\n");
			}
			if (unmatched.contains("audioNRStart")) sb.append("Noise reduction clip start time: "+map.get("audioNRStart")+" ms\n");
			if (unmatched.contains("audioNRLength")) sb.append("Noise reduction clip length: "+map.get("audioNRLength")+" ms\n");
			if (unmatched.contains("audioNRScalar")) sb.append("Noise reduction scalar: "+map.get("audioNRScalar")+"\n");
			LCTrainingSetInfo tsi = lcControl.getParams().getTrainingSetInfo();
			sb.append("Features: "+String.valueOf(tsi.featureList.size())+"\n");
			for (int i = 0; i < tsi.featureList.size(); i++)
				sb.append("\t"+tsi.featureList.get(i)+"\n");
			sb.append("\n");
		}
		pw.write(sb.toString());
		pw.flush();
		return sb;
	}
	
	/**
	 * Produces classifier settings info for "full results".
	 */
	protected StringBuilder produceClassifierInfo(StringBuilder sb) {
		sb = new StringBuilder();
		sb.append("LIVE CLASSIFIER PARAMETERS\n\n");
		LCParameters params = lcControl.getParams();
		sb.append("Feature vector data source: "+params.getInputFEDataBlock(lcControl).getDataName()+"\n");
		sb.append("Training set: "+lcControl.getParams().getTrainPath()+"\n");
		if (params.selectKBest) {
			sb.append("k-Best feature selection: On (k = "+String.valueOf(params.kBest)+")\n");
		} else {
			sb.append("k-Best feature selection: Off\n");
		}
		if (params.samplingLimits.equals("none")) {
			sb.append("Sampling limit: None\n");
		} else if (params.samplingLimits.equals("automax")) {
			sb.append("Sampling limit: Automatically set to size of least-populated class\n");
		} else if (params.samplingLimits.equals("setmax")) {
			sb.append("Sampling limit: Manually-set maximum (n = "+String.valueOf(params.maxSamples)+")\n");
		} else if (params.samplingLimits.equals("duplicate")) {
			sb.append("Sampling limit: Entries in smaller classes duplicated to match largest class\n");
		}
		if (params.limitClusterSize) {
			sb.append("Cluster size limit for training data: None\n");
		} else {
			sb.append("Cluster size limit for training data: "+String.valueOf(params.maxClusterSize)+"\n");
		}
		sb.append("Time zone: "+params.timeZone+"\n");
		sb.append("Class labels: "+String.valueOf(params.labelOrder.length)+"\n");
		for (int i = 0; i < params.labelOrder.length; i++) {
			sb.append("\t"+params.labelOrder[i]+"\n");
		}
		sb.append("Classifier model: "+params.classifierName+"\n");
		if (!params.classifierName.equals("HistGradientBoostingClassifier")) {
			sb.append("Number of estimators: "+String.valueOf(params.nEstimators)+"\n");
			sb.append("Criterion: "+params.criterion+"\n");
			if (params.hasMaxDepth) {
				sb.append("Max. tree depth: "+String.valueOf(params.maxDepth)+"\n");
			} else {
				sb.append("Max. tree depth: None\n");
			}
			if (!params.maxFeaturesMode.equals("Custom")) {
				sb.append("Max. features per tree: "+params.maxFeaturesMode+"\n");
			} else {
				sb.append("Max. features per tree: "+String.valueOf(params.maxFeatures)+"\n");
			}
			sb.append("Bootstrap: "+String.valueOf(params.bootstrap)+"\n");
			if (!params.classWeightMode.equals("Custom")) {
				sb.append("Class weights: "+params.classWeightMode+"\n");
			} else {
				sb.append("Class weights:");
				for (int i = 0; i < params.classWeights.length; i++) {
					sb.append(" "+String.valueOf(params.classWeights[i]));
				}
				sb.append("\n");
			}
		} else {
			sb.append("Learning rate: "+String.valueOf(params.learningRate)+"\n");
			sb.append("Max. number of iterations: "+String.valueOf(params.maxIterations)+"\n");
			if (params.hasMaxDepth) {
				sb.append("Max. tree depth: "+String.valueOf(params.maxDepth)+"\n");
			} else {
				sb.append("Max. tree depth: None\n");
			}
		}
		sb.append("Min. cluster size: "+String.valueOf(params.minClusterSize)+"\n");
		sb.append("Descriptors:\n");
		sb.append("\tVery low  <  "+String.valueOf(params.veryLow)+"\n");
		sb.append("\tLow       <  "+String.valueOf(params.low)+"\n");
		sb.append("\tAverage   <  "+String.valueOf(params.average)+"\n");
		sb.append("\tHigh      <  "+String.valueOf(params.high)+"\n");
		sb.append("\tVery high <= 1.00\n");
		sb.append("Worst acceptable certainty: "+params.worstCertainty+"\n\n");
		pw.write(sb.toString());
		pw.flush();
		return sb;
	}
	
	/**
	 * Produces matrix results for "full results".
	 */
	protected StringBuilder produceMatrixInfo(StringBuilder sb) {
		sb = new StringBuilder();
		sb.append("ACCURACY MATRIX\n\n");
		LCPanel panel = (LCPanel) lcControl.getTabPanel().getPanel();
		JLabel[][] accMatrix = panel.getAccuracyMatrixLabels();
		for (int i = 0; i < accMatrix.length; i++) {
			for (int j = 0; j < accMatrix[i].length; j++) {
				sb.append(accMatrix[i][j].getText());
				if (j < accMatrix[i].length-1) {
					sb.append("\t");
				}
			}
			sb.append("\n");
		}
		sb.append("\n");
		pw.write(sb.toString());
		pw.flush();
		
		sb = new StringBuilder();
		sb.append("CONFUSION MATRIX\n\n");
		JLabel[][] confMatrix = panel.getConfusionMatrixLabels();
		for (int i = 0; i < confMatrix.length; i++) {
			for (int j = 0; j < confMatrix[i].length; j++) {
				sb.append(confMatrix[i][j].getText());
				if (j < confMatrix[i].length-1) {
					sb.append("\t");
				}
			}
			sb.append("\n");
		}
		sb.append("\n");
		pw.write(sb.toString());
		pw.flush();
		return sb;
	}
	
	/**
	 * Produces individual contour results for "full results".
	 */
	protected StringBuilder produceIndividualContourInfo(StringBuilder sb) {
		sb = new StringBuilder();
		sb.append("INDIVIDUAL CLUSTER RESULTS");
		pw.write(sb.toString());
		pw.flush();
		
		LCDataBlock db = (LCDataBlock) lcControl.getProcess().getOutputDataBlock(0);
		PamTable table = lcControl.getTabPanel().getPanel().getTable();
		ArrayList<String> fullMapInput = new ArrayList<String>();
		for (int i = 0; i < table.getRowCount(); i++) {
			fullMapInput.add(table.getValueAt(i, 0)+", "+table.getValueAt(i, 1));
		}
		final int maxInputSize = 10000;
		for (int i = 0; (double) i < ((double) fullMapInput.size())/maxInputSize; i++) {
			ArrayList<String> mapInput = new ArrayList<String>();
			for (int j = 0; j < maxInputSize && i*maxInputSize+j < fullMapInput.size(); j++) {
				mapInput.add(fullMapInput.get(i*maxInputSize+j));
			}
			HashMap<String, LCDataUnit> unitMap = db.retrieveDataUnitsByIDandDate(mapInput);
			Iterator<String> it = unitMap.keySet().iterator();
			ArrayList<String> idDateList = new ArrayList<String>();
			while (it.hasNext()) idDateList.add(it.next());
			idDateList.sort(Comparator.naturalOrder());
			ArrayList<String> toWrite = new ArrayList<String>();
			//while (it.hasNext()) {
			while (idDateList.size() > 0) {
				LCDataUnit du = unitMap.get(idDateList.remove(0));
				if (du == null) {
					continue;
				}
				LCCallCluster cc = du.getCluster();
				sb = new StringBuilder();
				sb.append("\n\nCLUSTER "+cc.clusterID+"\n");
				sb.append("Location: ");
				if (cc.location.length() > 0)
					sb.append(cc.location+"\n");
				else
					sb.append("n/a\n");
				sb.append("Date/Time (UTC): "+lcControl.convertDateLongToString(cc.getStartAndEnd(false)[0])+"\n");
				sb.append("Duration: "+String.valueOf(cc.getStartAndEnd(false)[1]-cc.getStartAndEnd(false)[0])+" ms\n");
				sb.append("Size: "+String.valueOf(cc.getSize())+" contour(s)\n");
				ArrayList<String> contourInfo = new ArrayList<String>();
				for (int j = 0; j < cc.getSize(); j++) {
					String contour = "\t"+String.valueOf(cc.uids[j])+"  ";
					contour += lcControl.convertDateLongToString(lcControl.convertFromLocalToUTC(cc.datetimes[j]))+"  ";
					contour += "[";
					for (int k = 0; k < cc.probaList[j].length; k++) {
						contour += String.format("%.2f", (float) cc.probaList[j][k]);
						if (k < cc.probaList[j].length-1) {
							contour += " ";
						}
					}
					contour += "]  ";
					contour += String.format("%.2f", (float) cc.getIndividualCertainty(j))+"  ";
					contour += cc.getIndividualActualSpeciesString(j)+" -> "+cc.getIndividualPredictedSpeciesString(j)+"\n";
					contourInfo.add(contour);
				}
				contourInfo.sort(Comparator.naturalOrder());
				for (int j = 0; j < contourInfo.size(); j++) {
					sb.append(contourInfo.get(j));
				}
				sb.append("Actual species: "+cc.getActualSpeciesString()+"\n");
				sb.append("Predicted species: "+cc.getPredictedSpeciesString()+"\n");
				sb.append("Overall probabilities: "+cc.getAverageProbaAsString()+"\n");
				sb.append("Overall certainty: "+String.format("%.2f", (float) cc.getCertainty())+
						" ("+lcControl.getParams().getCertaintyDescriptor(cc.getCertainty())+")");
				toWrite.add(sb.toString());
			}
			toWrite.sort(Comparator.naturalOrder());
			for (int j = 0; j < toWrite.size(); j++) {
				pw.write(toWrite.get(j));
				pw.flush();
			}
		}
		return sb;
	}

	@Override
	public boolean getParams() {
		File f = selectFile(optionsBox.getSelectedIndex());
		if (f == null) {
			return false;
		}
		try {
			pw = new PrintWriter(f);
		} catch (Exception e) {
			e.printStackTrace();
			lcControl.simpleErrorDialog("Error: Could not create PrintWriter with selected file.", 300);
			return false;
		}
		sb = new StringBuilder();
		if (optionsBox.getSelectedIndex() == 0) {
			exportClusterResults();
		} else if (optionsBox.getSelectedIndex() == 1) {
			exportIndividualContourResults();
		} else if (optionsBox.getSelectedIndex() == 2) {
			exportMatrix(false);
		} else if (optionsBox.getSelectedIndex() == 3) {
			exportMatrix(true);
		} else if (optionsBox.getSelectedIndex() == 4) {
			exportFullResults();
		}
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {
		// (Disabled)
	}
	
}