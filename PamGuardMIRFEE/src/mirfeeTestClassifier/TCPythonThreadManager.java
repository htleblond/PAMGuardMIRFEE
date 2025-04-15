package mirfeeTestClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import mirfeeLiveClassifier.LCBestFeaturesDialog;
import mirfeeLiveClassifier.LCPythonThreadManager;
import mirfeeLiveClassifier.LCTrainingSetInfo;

/**
 * Creates an instance of a Python interpreter, sends clips to the
 * Python script for processing, and manages communication between
 * Java and Python.
 * Subclass of the Live Classifier's thread manager.
 * @author Holly LeBlond
 */
public class TCPythonThreadManager extends LCPythonThreadManager {
	
	protected volatile boolean running;
	protected volatile boolean waitingOnModel;
	protected volatile boolean lastInitFailed;
	
	protected volatile int totalClusters;
	protected volatile int totalProcessed;
	
	public TCPythonThreadManager(TCControl tcControl) {
		super(tcControl);
		this.running = false;
		this.waitingOnModel = false;
		this.lastInitFailed = false;
		this.totalClusters = -1;
		this.totalProcessed = -1;
	}
	
	/**
	 * Attempts to fit the training sets in Python.
	 * @return False if an error occurred. Otherwise, true.
	 */
	public boolean initializeTrainingSets() {
		if (running) {
			getControl().simpleErrorDialog("This action cannot be performed while another process is active.", 250);
			getLoadingBar().setString("Idle");
			return false;
		}
		running = true;
		TCParameters params = getControl().getParams();
		if (params.getTrainingSetInfo() == null ||
			(params.validation >= params.LABELLED && params.getTestingSetInfo() == null)) {
			// TODO Error message
			return false;
		}
		getStartButton().setText("Stop");
		getStartButton().setEnabled(true);
		String pyParams = params.outputPythonParamsToText();
	/*	if (params.getFeatureList().size() > 0) {
        	pyParams += "\""+params.getFeatureList().get(0)+"\"";
			for (int i = 1; i < params.getFeatureList().size(); i++) {
				pyParams += ",\""+params.getFeatureList().get(i)+"\"";
			}
		}
        pyParams += "]";
        pyParams += "]"; */
        getLoadingBar().setValue(0);
        addCommand("modelManager.clearModelList()");
		if (params.validation == params.LEAVEONEOUTBOTHDIGITS || params.validation == params.LEAVEONEOUTFIRSTDIGIT) {
			ArrayList<String> idList = new ArrayList<String>();
			Iterator<String> it = params.getTrainingSetInfo().subsetCounts.keySet().iterator();
			while (it.hasNext()) {
				if (params.validation == params.LEAVEONEOUTBOTHDIGITS) idList.add(it.next());
				else {
					String next = it.next().substring(0, 1);
					if (!idList.contains(next)) idList.add(next);
				}
			}
			idList.sort(Comparator.naturalOrder());
			getLoadingBar().setString("Fitting classifier models 0/"+String.valueOf(idList.size())+" (0.0%)");
			for (int i = 0; i < idList.size(); i++) {
				//String initCommand = "tcm"+idList.get(i)+" = LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","
				//		+pyParams+",[\""+idList.get(i)+"\"],[],False)";
				String initCommand = "modelManager.addModel(LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","
						+pyParams+",[\""+idList.get(i)+"\"],[],False))";
				initializeModel(initCommand);
				if (lastInitFailed || !running) {
					endProcess("Stopped");
					break;
				}
				double loadInt = 100 * (double) (i+1) / idList.size();
				getLoadingBar().setValue((int) Math.floor(loadInt));
				getLoadingBar().setString("Fitting classifier models "+String.valueOf(i+1)+"/"+String.valueOf(idList.size())+
						" ("+String.format("%.1f", (float) loadInt)+"%)");
			}
		} else if (params.validation == params.KFOLD) {
			getLoadingBar().setString("Fitting classifier models 0/"+String.valueOf(params.kNum)+" (0.0%)");
			for (int i = 0; i < params.kNum; i++) {
				//String initCommand = "tcm"+String.valueOf(i)+" = LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","
				//		+pyParams+",[],["+String.valueOf(i)+"],False)";
				String initCommand = "modelManager.addModel(LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","
								+pyParams+",[],["+String.valueOf(i)+"],False))";
				initializeModel(initCommand);
				if (lastInitFailed || !running) {
					endProcess("Stopped");
					break;
				}
				double loadInt = 100 * (double) (i+1) / params.kNum;
				getLoadingBar().setValue((int) Math.floor(loadInt));
				getLoadingBar().setString("Fitting classifier models "+String.valueOf(i+1)+"/"+String.valueOf(params.kNum)+
						" ("+String.format("%.1f", (float) loadInt)+"%)");
			}
		} else {
			getLoadingBar().setString("Fitting classifier models 0/1 (0.0%)");
			//String initCommand = "tcm = LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","+pyParams+",[";
			String initCommand = "modelManager.addModel(LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","+pyParams+",[";
			if (params.validation == params.TESTSUBSET) {
				//System.out.println(params.testSubset);
				if (params.testSubset.length() == 1) {
					ArrayList<String> subsetList = params.getTrainingSetInfo().getSortedSubsetList();
					boolean firstAdded = false;
					for (int i = 0; i < subsetList.size(); i++) {
						if (subsetList.get(i).substring(0, 1).equals(params.testSubset)) {
							if (firstAdded) initCommand += ",";
							initCommand += "\""+subsetList.get(i)+"\"";
							firstAdded = true;
						}
					}
				} else initCommand += "\""+params.testSubset+"\"";
			}
			//initCommand += "],[],False)";
			initCommand += "],[],False))";
			initializeModel(initCommand);
			if (!lastInitFailed && running) {
				getLoadingBar().setValue(100);
				getLoadingBar().setString("Fitting classifier models 1/1 (100.0%)");
			}
		}
		if (lastInitFailed || !running) {
	        addCommand("modelManager.clearModelList()");
			if (lastInitFailed) getControl().simpleErrorDialog("Error occured while attempting to fit classifier models in Python. "
					+ "See console for details.", 250);
			//getLoadingBar().setString("Idle");
			//getStartButton().setText("Start");
			//getStartButton().setEnabled(true);
			lastInitFailed = false;
			running = false;
			endProcess("Stopped");
			return false;
		}
		return true;
	}
	
	/**
	 * Sends the command for fitting a training model to the Python script and waits for a response.
	 * @param initCommand - The initialization command.
	 */
	protected void initializeModel(String initCommand) {
		waitingOnModel = true;
		addCommand(initCommand);
		while (waitingOnModel && running) {
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Attempts the "printBestFeatureOrder" command in the Python script and waits for a response.
	 * @return False if an error occurred. Otherwise, true.
	 */
	public boolean initializeBestFeaturesSet() {
		if (running) {
			getControl().simpleErrorDialog("This action cannot be performed while another process is active.", 250);
			return false;
		}
		TCParameters params = getControl().getParams();
		String pyParams = params.outputPythonParamsToText();
    /*    if (params.getFeatureList().size() > 0) {
        	pyParams += "\""+params.getFeatureList().get(0)+"\"";
			for (int i = 1; i < params.getFeatureList().size(); i++) {
				pyParams += ",\""+params.getFeatureList().get(i)+"\"";
			}
		}
        pyParams += "]";
        pyParams += "]"; */
        running = true;
        String initCommand = "tcmBest = LCPythonScript.LCModel(r\""+params.getTrainPath()+"\","+pyParams+",[],[],True)";
		initializeModel(initCommand);
		if (lastInitFailed || !running) {
			if (lastInitFailed) getControl().simpleErrorDialog("Error occured while attempting to fit classifier models in Python. "
					+ "See console for details.", 250);
			lastInitFailed = false;
			running = false;
			return false;
		}
		running = false;
		return true;
	}
	
	/**
	 * Starts sending testing data to the training models after they've been initialized.
	 * @param maxMapSize
	 */
	public void startPredictions(int maxMapSize) {
		TCParameters params = getControl().getParams();
		LCTrainingSetInfo setInfo = params.getTrainingSetInfo();
		if (params.validation >= params.LABELLED) setInfo = params.getTestingSetInfo();
		if (setInfo.compare(new LCTrainingSetInfo("")) || setInfo.compare(new LCTrainingSetInfo(null))) {
			getControl().simpleErrorDialog("Set containing values to be tested is invalid.", 250);
			endProcess("Stopped (error)");
			return;
		}
		File f = new File(setInfo.pathName);
		if (!f.exists()) {
			getControl().simpleErrorDialog("Set containing values to be tested no longer exists.", 250);
			endProcess("Stopped (error)");
			return;
		}
		ArrayList<String> clusterList = new ArrayList<String>();
		Scanner sc = null;
		String nextLine;
		String[] nextSplit = null;
		try {
			sc = new Scanner(f);
			//if (sc.hasNextLine()) nextSplit = sc.nextLine().split(",");
			if (sc.hasNextLine()) nextLine = sc.nextLine();
			else {
				getControl().simpleErrorDialog("Set containing values to be tested is apparently now empty.", 250);
				sc.close();
				endProcess("Stopped (error)");
				return;
			}
			if (nextLine.equals("EXTRACTOR PARAMS START")) {
				while (sc.hasNextLine() && !sc.nextLine().equals("EXTRACTOR PARAMS END"));
				if (sc.hasNextLine()) sc.nextLine();
			}
			while (sc.hasNextLine()) {
				nextSplit = sc.nextLine().split(",");
				if (params.validation == params.TESTSUBSET) {
					if (params.testSubset.length() == 1 && !nextSplit[0].substring(0, 1).equals(params.testSubset)) continue;
					if (params.testSubset.length() != 1 && !nextSplit[0].substring(0, 2).equals(params.testSubset)) continue;
				}
				if (!clusterList.contains(nextSplit[0])) clusterList.add(nextSplit[0]);
			}
			//clusterList.sort(Comparator.naturalOrder());
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
			if (sc != null) sc.close();
			getControl().simpleErrorDialog("Error scanning set for testing.", 300);
			endProcess("Stopped (error)");
			return;
		}
		totalClusters = clusterList.size();
		totalProcessed = 0;
		getLoadingBar().setValue(0);
		getLoadingBar().setString("0/"+String.valueOf(totalClusters)+" (0.0%)");
		for (int i = 0; i*maxMapSize < totalClusters; i++) {
			HashMap<String, ArrayList<TCDetection>> clusterMap = new HashMap<String, ArrayList<TCDetection>>();
			for (int j = i*maxMapSize; j < i*maxMapSize+maxMapSize && j < totalClusters; j++)
				clusterMap.put(clusterList.get(j), new ArrayList<TCDetection>());
			try {
				sc = new Scanner(f);
				nextLine = sc.nextLine();
				if (nextLine.equals("EXTRACTOR PARAMS START")) {
					while (sc.hasNextLine() && !sc.nextLine().equals("EXTRACTOR PARAMS END"));
					if (sc.hasNextLine()) sc.nextLine();
				}
				while (sc.hasNextLine()) {
					nextSplit = sc.nextLine().split(",");
					if (nextSplit.length < params.getTrainingSetInfo().featureList.size() + 8 ||
							!clusterMap.containsKey(nextSplit[0])) continue;
					try {
						TCDetection newDetection = new TCDetection(nextSplit);
						clusterMap.get(nextSplit[0]).add(newDetection);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// TODO
			}
			for (int j = i*maxMapSize; j < i*maxMapSize+maxMapSize && j < totalClusters; j++) {
				if (!running) {
					endProcess("Stopped");
					return;
				}
				ArrayList<TCDetection> currList = clusterMap.get(clusterList.get(j));
			/*	String outp = "tcm";
				if (params.validation == params.LEAVEONEOUT) outp += clusterList.get(j).substring(0,2);
				else if (params.validation == params.KFOLD)
					outp += String.valueOf((int) Math.floor(params.kNum * (double) j / totalClusters));
				outp += ".predictCluster(["; */
				String outp = "modelManager.predictCluster([";
				for (int k = 0; k < currList.size(); k++) {
					TCDetection curr = currList.get(k);
					outp += "[\""+curr.clusterID+"\",";
					outp += String.valueOf(curr.uid)+",";
					//outp += String.valueOf(getControl().convertDateStringToLong(curr.datetime))+",";
					outp += "\""+curr.location+"\",";
					outp += "\""+String.valueOf(curr.datetime)+"\",";
					outp += String.valueOf(curr.duration)+",";
					outp += String.valueOf(curr.lf)+",";
					outp += String.valueOf(curr.hf)+",";
					outp += "\""+curr.species+"\",";
					outp += "[";
					for (int l = 0; l < curr.featureVector.length; l++) {
						outp += String.valueOf(curr.featureVector[l]);
						if (l < curr.featureVector.length-1) outp += ",";
					}
					if (k < currList.size()-1) outp += "]],";
					else outp += "]]])";
				}
				if (!running) {
					endProcess("Stopped");
					return;
				}
				getControl().idWaitList.add(currList.get(0).clusterID); // TODO DELETE THIS
				if (currList.size() > 0) addCommand(outp);
				else {
					addOneToLoadingBar();
					getSidePanelPanel().addOneToErrorCounter();
				}
			}
			//addCommand("RUNLAST");
		}
		//endProcess();
	}
	
	// TODO DELETE THIS LATER - FOR BUG TESTING
/*	@Override
	public void pythonCommand(String command, boolean toPrint) {
		if (toPrint) super.pythonCommand(command, running || !command.startsWith("tcm.predictCluster"));
		else super.pythonCommand(command, false);
	} */
	
	@Override
	protected void parseIPTOutput(String outpstr, boolean print) {
		if (!running) return;
		if (print) System.out.println("TC IBR: "+outpstr);
		if (outpstr.equals("Initialization succeeded")) {
			//lcControl.setTrainingSetStatus(true);
			//lcControl.setModelFittingStatus(true);
			waitingOnModel = false;
		} else if (outpstr.equals("Initialization failed")) {
			//lcControl.setTrainingSetStatus(false);
			//lcControl.setModelFittingStatus(true);
			lastInitFailed = true;
			waitingOnModel = false;
		}
		if (outpstr.equals("ERROR - ModelManager is empty.")) {
			getControl().simpleErrorDialog("Whoops - no training models have been initialized yet.", 350);
			endProcess("Stopped"); // TODO Make sure this doesn't cause any problems.
		}
		if (outpstr.startsWith("ERROR - Input test entry found in all training sets: ")) {
			String[] info = outpstr.replace("ERROR - Input test entry found in all training sets: ", "").split(", ");
			getControl().getTabPanel().getPanel().addErrorToTable(info[0], info[1], Integer.valueOf(info[2]));
		}
		if (outpstr.startsWith("RESULT")) {
			getControl().getProcess().addResultsData(outpstr.substring(10));
			//System.out.println("Units: "+String.valueOf(getControl().getProcess().getOutputDataBlock(0).getUnitsCount()));
			addOneToLoadingBar();
		}
		if (outpstr.contains("Error encountered while attempting to process cluster")) {
			addOneToLoadingBar();
			getSidePanelPanel().addOneToErrorCounter();
		}
		if (outpstr.contains("Cluster ignored due to settings")) {
			addOneToLoadingBar();
			getSidePanelPanel().addOneToIgnoreCounter();
		}
	/*	if (outpstr.equals("RUNLAST")) {
			finished = true;
		} */
		if (outpstr.contains("BESTFEATUREORDER")) {
			if (getControl().getTabPanel().getPanel().getWaitingDialogThread() != null)
				getControl().getTabPanel().getPanel().getWaitingDialogThread().halt();
			LCBestFeaturesDialog dialog = new LCBestFeaturesDialog(lcControl.getGuiFrame(), lcControl, outpstr.substring(18));
			dialog.setVisible(true);
		}
	}
	
	@Override
	protected void parseEPTOutput(String outpstr) {
		System.out.println("TC EBR: "+outpstr);
	}
	
	/**
	 * Adds 1 to the loading bar in TCSidePanelPanel.
	 */
	protected void addOneToLoadingBar() {
		totalProcessed++;
		if (totalClusters <= 0) return;
		double value = 100 * (double) totalProcessed / totalClusters;
		getLoadingBar().setValue((int) Math.floor(value));
		getLoadingBar().setString(String.valueOf(totalProcessed)+"/"+String.valueOf(totalClusters)+" ("+
				String.format("%.1f", (float) value)+"%)");
		if (totalProcessed == totalClusters) endProcess("Done!"); // TODO FIND A BETTER WAY TO DO THIS !!!
	}
	
	/**
	 * Stops the process.
	 * @param loadingBarMessage - The resulting message in the loading bar.
	 */
	protected void endProcess(String loadingBarMessage) {
		running = false;
		waitingOnModel = false;
		lastInitFailed = false;
		getLoadingBar().setString(loadingBarMessage);
		//getLoadingBar().setValue(0);
		getStartButton().setText("Start");
		getStartButton().setEnabled(true);
	}
	
	/**
	 * @return The actual GUI side panel.
	 */
	protected TCSidePanelPanel getSidePanelPanel() {
		return getControl().getSidePanel().getTCSidePanelPanel();
	}
	
	/**
	 * @return The start button in TCSidePanelPanel.
	 */
	protected JButton getStartButton() {
		return getSidePanelPanel().startButton;
	}
	
	/**
	 * @return The loading bar in TCSidePanelPanel.
	 */
	protected JProgressBar getLoadingBar() {
		return getSidePanelPanel().loadingBar;
	}
	
	@Override
	protected TCControl getControl() {
		return (TCControl) lcControl;
	}
	
	/**
	 * Attempts to stop the thread manager's current actions.
	 */
	public void stop() {
		running = false;
		commandList.clear();
		try { // TODO FIND A BETTER WAY TO DO THIS !!!
			TimeUnit.MILLISECONDS.sleep(500);
			endProcess("Stopped");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return Whether or not the process is currently running.
	 */
	public boolean isRunning() {
		return running;
	}
	
}