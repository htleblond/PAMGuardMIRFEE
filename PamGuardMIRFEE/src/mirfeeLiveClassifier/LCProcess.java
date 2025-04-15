package mirfeeLiveClassifier;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import mirfeeFeatureExtractor.FECallCluster;
import mirfeeFeatureExtractor.FEDataBlock;
import mirfeeFeatureExtractor.FEDataUnit;

/**
 * The Live Classifier's PamProcess.
 * @author Holly LeBlond
 */
public class LCProcess extends PamProcess {
	
	protected LCControl lcControl;
	protected LCDataBlock resultsDataBlock;
	
	public static final String streamName = "Call Clusters";
	
	public LCProcess(LCControl lcControl) {
		super(lcControl, null);
		this.lcControl = lcControl;
		init();
	}
	
	protected void init() {
	/*	for (int i = 0; i < lcControl.getPamController().getDataBlocks().size(); i++) {
			if (lcControl.getParams().printJava)
				System.out.println(lcControl.getPamController().getDataBlocks().get(i).getDataName()+" : "+lcControl.getParams().inputProcessName);
			if (lcControl.getPamController().getDataBlocks().get(i).getDataName().equals(lcControl.getParams().inputProcessName)) {
				this.setParentDataBlock(lcControl.getPamController().getDataBlocks().get(i));
			}
		} */
		this.setParentDataBlock(lcControl.getParams().getInputFEDataBlock(lcControl));
		resultsDataBlock = new LCDataBlock(lcControl, streamName, this, 0);
		this.addOutputDataBlock(resultsDataBlock);
		resultsDataBlock.setOverlayDraw(new LCOverlayGraphics(lcControl, resultsDataBlock));
		resultsDataBlock.setBinaryDataSource(new LCBinaryDataSource(lcControl, resultsDataBlock, streamName));
		resultsDataBlock.setShouldBinary(true);
		resultsDataBlock.setDatagramProvider(new LCDatagramProvider(lcControl, resultsDataBlock));
	}
	
	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		if (!lcControl.isTrainingSetLoaded()) {
			return;
		}
		super.newData(o, arg);
		//if (lcControl.getParams().printJava)
		//	System.out.println("REACHED lcProcess.newData");
		FEDataUnit vectorDataUnit = (FEDataUnit) arg;
		FECallCluster cc = vectorDataUnit.getCluster();
		//String outp = "tcm.predictCluster([";
		String outp = "modelManager.predictCluster([";
		for (int i = 0; i < cc.getSize(); i++) {
			outp += "["+cc.clusterID+",";
			outp += String.valueOf(cc.uids[i])+",";
			//outp += String.valueOf(cc.datetimes[i])+",";
			outp += "\"\","; // Live Classifier doesn't record location.
			// Note that the data time here should be in UTC, but it has to be converted to local when added to the block or the graphics won't work.
			outp += "\""+lcControl.convertDateLongToString(cc.datetimes[i])+"\",";
			outp += String.valueOf(cc.durations[i])+",";
			outp += String.valueOf(cc.lfs[i])+",";
			outp += String.valueOf(cc.hfs[i])+",";
			outp += "\"\","; // Species obviously isn't known yet, as this is live.
			outp += "[";
			for (int j = 0; j < cc.featureVector[i].length; j++) {
				outp += String.valueOf(cc.featureVector[i][j]);
				if (j < cc.featureVector[i].length-1) {
					outp += ",";
				}
			}
			if (i < cc.getSize()-1) {
				outp += "]],";
			} else {
				outp += "]]])";
			}
		}
		//System.out.println("LCProcess newData: "+outp);
		lcControl.getThreadManager().addCommand(outp);
	}
	
	/**
	 * Converts prediction data made by the Python script into a data unit and adds it to the block.
	 * @return False if the input string isn't formatted correctly and is thus not added to the block.
	 * Otherwise, returns true.
	 */
	public boolean addResultsData(String inp) {
		try {
			String[] tokens = inp.split(Pattern.quote("]|["));
			String[] uids = tokens[1].split(Pattern.quote(", "));
			String[] locations = tokens[2].substring(1, tokens[2].length()-1).split(Pattern.quote("', '"), -1);
			String[] datetimes = tokens[3].substring(1, tokens[3].length()-1).split(Pattern.quote("', '"), -1);
			String[] durations = tokens[4].split(Pattern.quote(", "));
			String[] lfs = tokens[5].split(Pattern.quote(", "));
			String[] hfs = tokens[6].split(Pattern.quote(", "));
			String[] actualSpecies = tokens[7].substring(1, tokens[7].length()-1).split(Pattern.quote("', '"), -1);
			String[] probas = tokens[8].substring(1, tokens[8].length()-2).split(Pattern.quote("], ["));
			
			LCCallCluster cc = new LCCallCluster(lcControl.getParams().labelOrder, uids.length);
			cc.clusterID = tokens[0].substring(0, tokens[0].length()-1).split(Pattern.quote("', '"))[0];
			cc.location = locations[0]; // Only really used by Test Classifier.
			for (int i = 0; i < cc.getSize(); i++) {
				cc.uids[i] = Long.valueOf(uids[i]);
				// TZ needs to be local for the graphics to work - remember to change this back when necessary.
				cc.datetimes[i] = lcControl.convertFromUTCToLocal(lcControl.convertDateStringToLong(datetimes[i]));
				cc.durations[i] = (int) Double.valueOf(durations[i]).doubleValue();
				cc.lfs[i] = (int) Double.valueOf(lfs[i]).doubleValue();
				cc.hfs[i] = (int) Double.valueOf(hfs[i]).doubleValue();
				cc.setIndividualActualSpecies(Long.valueOf(uids[i]), actualSpecies[i]);
				String[] probaRow = probas[i].split(Pattern.quote(", "));
				for (int j = 0; j < probaRow.length; j++) {
					cc.probaList[i][j] = Double.valueOf(probaRow[j]);
				}
			}
			LCDataUnit du = new LCDataUnit(lcControl, cc);
			resultsDataBlock.addPamData(du);
			lcControl.idWaitList.remove(cc.clusterID); // DELETE THIS
			//System.out.println(String.valueOf(resultsDataBlock.getUnitsCount())+" : "+String.valueOf(lcControl.idWaitList.size())); // DELETE THIS TOO
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void prepareProcess() {
		if (!lcControl.isTrainingSetLoaded()) {
			return;
		}
		FEDataBlock fedb = (FEDataBlock) this.getParentDataBlock();
		if (fedb == null) {
			lcControl.setTrainingSetStatus(false);
			lcControl.simpleErrorDialog("Live Classifier Error: The selected Feature Extractor appears to no longer "
					+ "exist. The Live Classifier cannot function until this is fixed.", 250);
			return;
		}
		String[][] feFeatures = fedb.getFeatureList();
		if (feFeatures.length != lcControl.getParams().getFeatureList().size()) {
			lcControl.setTrainingSetStatus(false);
			lcControl.simpleErrorDialog("Live Classifier Error: The features of the selected Feature Extractor and the features "
					+ "in the loaded training set do not match. The Live Classifier cannot function until this is fixed.", 250);
			return;
		}
		for (int i = 0; i < feFeatures.length; i++) {
			if (!feFeatures[i][1].equals(lcControl.getParams().getFeatureList().get(i))) {
				lcControl.setTrainingSetStatus(false);
				lcControl.simpleErrorDialog("Live Classifier Error: The features of the selected Feature Extractor and the features "
						+ "in the loaded training set do not match. The Live Classifier cannot function until this is fixed.", 250);
				return;
			}
		}
	}
	
	@Override
	public void pamStart() {
		if (!lcControl.isTrainingSetLoaded()) {
			return;
		}
		lcControl.getThreadManager().setFinished(false);
		lcControl.getTabPanel().getPanel().exportButton.setEnabled(false);
	}

	@Override
	public void pamStop() {
		if (!lcControl.isTrainingSetLoaded()) {
			return;
		}
		while (true) {
			FEDataBlock parent = (FEDataBlock) this.parentDataBlock;
			if (parent.isFinished()) {
				break;
			} else {
				try {
					TimeUnit.MILLISECONDS.sleep(200);
				} catch (Exception e) {
					System.out.println("Sleep exception.");
					e.printStackTrace();
				}
			}
		}
		try {
			// TODO THIS NEEDS TO BE REPLACED AT SOME POINT
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (Exception e) {
			System.out.println("Sleep exception.");
			e.printStackTrace();
		}
		//lcControl.getThreadManager().pythonCommand("tcm.runLast()", lcControl.getParams().printInput);
		lcControl.getThreadManager().pythonCommand("modelManager.runLast()", lcControl.getParams().printInput);
		while (!lcControl.getThreadManager().getFinished()) {
			try {
				if (lcControl.getParams().printInput)
					System.out.println("Waiting for RUNLAST");
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (Exception e) {
				System.out.println("Sleep exception.");
				e.printStackTrace();
			}
		}
		try {
			// TODO THIS NEEDS TO BE REPLACED AT SOME POINT
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (Exception e) {
			System.out.println("Sleep exception.");
			e.printStackTrace();
		}
		lcControl.getTabPanel().getPanel().exportButton.setEnabled(true);
	}
	
	public void clearOutputDataBlock() {
		this.getOutputDataBlock(0).clearAll();
	}
}