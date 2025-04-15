package mirfeeFeatureExtractor;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.swing.JOptionPane;

import Acquisition.AcquisitionControl;
import Acquisition.filedate.StandardFileDate;
import warnings.PamWarning;
import clipgenerator.localisation.ClipDelays;
import fftManager.FFTDataBlock;
import mirfeeLiveClassifier.LCControl;
import wavFiles.Wav16AudioFormat;
import wavFiles.WavFileWriter;
import whistlesAndMoans.ConnectedRegionDataBlock;
import whistlesAndMoans.ConnectedRegionDataUnit;
import whistlesAndMoans.SliceData;
import PamController.PamControlledUnit;
import PamController.PamController;
import PamController.StorageOptions;
import PamController.StorageParameters;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamObserverAdapter;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import annotation.handler.ManualAnnotationHandler;
import binaryFileStorage.BinaryStore;

import java.lang.Math;

/**
 * A modified version of clipGenerator.ClipProcess that creates sound clips
 * when Whistle and Moan Detector contours occur and sends them to the
 * thread manager for extracting feature data via a Python script.
 * @author Holly LeBlond (original code by Doug Gillespie)
 */
public class FEProcess extends PamProcess {
	
	protected ClipBlockProcess[] clipBlockProcesses;
	protected List<ClipRequest> clipRequestQueue;
	protected Object clipRequestSynch = new Object();
	protected PamRawDataBlock rawDataBlock;
	protected long specMouseDowntime;
	protected boolean specMouseDown;
	protected long masterClockTime;
	protected ClipDelays clipDelays;
	protected ManualAnnotationHandler manualAnnotaionHandler;
	protected static PamWarning warningMessage = new PamWarning("Clip Generator", "", 2);
	
	protected FEControl feControl;
	protected double[][] nrData;
	protected PamDataUnit prevDU;
	protected int clusterCountID;
	protected RawDataBlockCheckerThread rdbct;
	protected ArrayList<ClipRequest> csvClipList;
	
	protected FEDataBlock vectorDataBlock;

	public FEProcess(FEControl feControl) {
		super(feControl, null);
		this.feControl = feControl;
		clipRequestQueue = new LinkedList<ClipRequest>();
		
		prevDU = null;
		clusterCountID = 0;
		
		vectorDataBlock = new FEDataBlock(feControl, "MIRFEE feature vector data", this, 0);
		addOutputDataBlock(vectorDataBlock);
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		super.newData(o, arg);
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL &&
				PamController.getInstance().getRunMode() != PamController.RUN_MIXEDMODE) {
			return;
		}
		processRequestList();
	}

	@Override
	public void masterClockUpdate(long timeMilliseconds, long sampleNumber) {
		super.masterClockUpdate(timeMilliseconds, sampleNumber);
		masterClockTime = timeMilliseconds;
	}

	/**
	 * Process the queue of clip request - these are passed straight back
	 * into the ClipBlockProcesses which started them since there is a 
	 * certain amount of bookkeeping which needs to be done at the
	 * individual block level.
	 * @author Doug Gillespie (modified by Holly LeBlond)
	 */
	protected void processRequestList() {
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL &&
				PamController.getInstance().getRunMode() != PamController.RUN_MIXEDMODE) {
			return;
		}
		if (clipRequestQueue.size() == 0) {
			return;
		}
		synchronized(clipRequestSynch) {
			ClipRequest clipRequest;
			ListIterator<ClipRequest> li = clipRequestQueue.listIterator();
			int clipErr;
			while (li.hasNext()) {
				clipRequest = li.next();
				clipErr = clipRequest.clipBlockProcess.processClipRequest(clipRequest);
				if (clipErr == RawDataUnavailableException.DATA_ALREADY_DISCARDED ||
					clipErr == RawDataUnavailableException.INVALID_CHANNEL_LIST) {
					feControl.addOneToCounter(FEPanel.FAILURE, String.valueOf(clipRequest.getUID()));
					li.remove();
				} else if (clipErr == RawDataUnavailableException.DATA_NOT_ARRIVED) {
					continue;
				} else if (clipErr == 4) {
					feControl.addOneToCounter(FEPanel.IGNORE, String.valueOf(clipRequest.getUID()));
					li.remove();
				} else if (clipErr == 0) {
					feControl.addOneToCounter(FEPanel.PENDING, String.valueOf(clipRequest.getUID()));
					li.remove();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#prepareProcess()
	 */
	@Override
	public void prepareProcess() {
		if (feControl.getParams().audioSourceProcessName.length() > 0)
			feControl.getParams().sr = 
				(int) PamController.getInstance().getRawDataBlock(feControl.getParams().audioSourceProcessName).getSampleRate();
		vectorDataBlock.setSampleRate((float) feControl.getParams().sr, false);
		
		subscribeDataBlocks();
	}

	@Override
	public void pamStart() {
		//if (feControl.getParams().miscPrintJavaChecked)
		//	System.out.println("REACHED pamStart().");
		clipRequestQueue.clear(); // just in case anything hanging around from previously. 
		// if there is it may crash since the ClipblockProcess will probably have been replaced anyway.
		
		vectorDataBlock.setFinished(false);
		
		prevDU = null;
		clusterCountID = 0;
		
		if (feControl.getParams().inputFromWMATorMTSF) {
			fillClipRequestQueueViaCSV();
		}
		
		rdbct = new RawDataBlockCheckerThread();
		rdbct.start();
		
		feControl.getThreadManager().checkThreads(); // TODO MAKE SURE THIS DOESN'T MESS THINGS UP !!!!!
		
		feControl.getThreadManager().signalPAMHasStarted();
	}
	
	@Override
	public void pamStop() {
		FEPythonThreadManager threadManager = feControl.getThreadManager();
		threadManager.signalPAMHasStopped();
		int waitNum = 0;
		int currWaitlistSize = threadManager.getWaitlistSize();
		int currClipsLeft = threadManager.clipsLeft();
		int currVectorsLeft = threadManager.vectorsLeft();
		while (vectorDataBlock.countObservers() > 0
				&& (threadManager.getWaitlistSize() > 0 || threadManager.clipsLeft() > 0 || threadManager.vectorsLeft() > 0)) {
			if (!(threadManager.getWaitlistSize() > 0 || threadManager.clipsLeft() > 0)) {
				threadManager.resetActiveThreads();
			}
			if (feControl.getParams().miscPrintJavaChecked) {
				System.out.println("Waited "+String.valueOf(waitNum)+"/60 seconds: "+
						"waitlistSize: "+String.valueOf(threadManager.getWaitlistSize())+
						", clipsLeft: "+String.valueOf(threadManager.clipsLeft())+
						", vectorsLeft: "+String.valueOf(threadManager.vectorsLeft()));
			}
			if (currWaitlistSize != threadManager.getWaitlistSize() ||
					currClipsLeft != threadManager.clipsLeft() ||
					currVectorsLeft != threadManager.vectorsLeft()) {
				currWaitlistSize = threadManager.getWaitlistSize();
				currClipsLeft = threadManager.clipsLeft();
				currVectorsLeft = threadManager.vectorsLeft();
				waitNum = 0;
			} else waitNum++;
			if (waitNum >= 60) { // Brings up warning dialog if caught in a loop for 60 seconds.
				int res = JOptionPane.showOptionDialog(feControl.getGuiFrame(),
						feControl.makeHTML("Python is not responding. What would you like to do?", 300),
						feControl.getUnitName(),
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null, 
						new Object[] {"Stop processing", "Clear queues", "Wait 60 more seconds"},
						"Wait 60 more seconds");
				if (res == JOptionPane.CANCEL_OPTION) {
					waitNum = 0;
				} else {
					threadManager.resetWaitlists();
					if (res == JOptionPane.YES_OPTION) {
						feControl.getPamController().pamStop(); // TODO MAKE SURE THIS ACTUALLY WORKS !!!!!
					}
					break;
				}
			}
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// This is now done via pushVectorDataToBlock() in FEPythonThread.
	/*	File tempFolder = new File(feControl.getParams().tempFolder);
		File[] filesToDelete = tempFolder.listFiles();
		for (int i = 0; i < filesToDelete.length; i++) {
			if (filesToDelete[i].getName().substring(filesToDelete[i].getName().length()-4).equals(".wav")) {
				filesToDelete[i].delete();
			}
		} */
		vectorDataBlock.setFinished(true);
	}
	
	/**
	 * Sets up a queue for processing clips when using the CSV input option.
	 * Adds entries to queue if they occur between the start time of the current
	 * audio file and the expected end of the file as specified in the settings.
	 * @author Holly LeBlond
	 */
	protected void fillClipRequestQueueViaCSV() {
		AcquisitionControl ac = null;
		for (int i = 0; i < feControl.getPamController().getNumControlledUnits(); i++) {
			if (feControl.getPamController().getControlledUnit(i).getUnitType().equals("Data Acquisition")) {
				ac = (AcquisitionControl) feControl.getPamController().getControlledUnit(i);
				break;
			}
		}
		if (ac == null) {
			System.out.println("WARNING: No Sound Acquisition process could be found. CSV data cannot be processed.");
			return;
		}
		
		csvClipList = new ArrayList<ClipRequest>();
		
		long start = ac.getDaqProcess().getDaqStatusDataBlock().getLastUnit().getTimeMilliseconds();
		long end = start + 1000*60*feControl.getParams().inputDataExpectedFileSize;
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		ArrayList<FEInputDataObject> entries = feControl.getWMATInputDataEntries();
		int startPoint; // This is for reducing time complexity.
		// Basically, it skips over entries at an interval of sqrt(entries.size()) and goes back an instance once it passes the first corresponding timestamp.
		// Worst-case time complexity per audio file is O(2n^0.5), which is substantially better than the previous O(n).
		int sqrtVal = (int) Math.ceil(Math.sqrt(entries.size()));
		for (startPoint = 0; startPoint < entries.size(); startPoint += sqrtVal) {
			FEInputDataObject curr = entries.get(startPoint);
			long currTime;
			try {
				currTime = df.parse(curr.datetime).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
				startPoint -= sqrtVal - 1; // This shouldn't happen, but we don't want to skip too much if it somehow does.
				continue;
			}
			if (start <= currTime) {
				if (startPoint == 0 && currTime < end)
					break;
				else if (startPoint == 0)
					return; // Audio ends before any of the detections in the whole list of entries occur.
				else {
					startPoint -= sqrtVal;
					break;
				}
			}
		}
		if (startPoint >= entries.size())
			startPoint -= sqrtVal;
		for (int i = startPoint; i < entries.size(); i++) {
			FEInputDataObject curr = entries.get(i);
			long currTime;
			try {
				currTime = df.parse(curr.datetime).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
				continue;
			}
			if (currTime >= end) break;
			if (!(start <= currTime && currTime < end)) continue;
			try {
				if (feControl.getParams().inputFilesAreMTSF()) {
					FETrainingDataUnit newDU = new FETrainingDataUnit(curr, (long) (feControl.getParams().sr*(currTime - start))/1000,
							(long) (feControl.getParams().sr*(curr.duration))/1000);
					csvClipList.add(new ClipRequest(clipBlockProcesses[0], newDU));
				} else { // input files are .wmat
					FESliceDataUnit newDU = new FESliceDataUnit(curr, (long) (feControl.getParams().sr*(currTime - start))/1000,
							(long) (feControl.getParams().sr*(curr.duration))/1000);
					csvClipList.add(new ClipRequest(clipBlockProcesses[0], newDU));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Collections.sort(csvClipList, Comparator.comparing(ClipRequest::getEndTimeInDataUnit));
		
		if (!feControl.getParams().inputFilesAreMTSF()) {
			int counter = 0;
			for (int i = 0; i < csvClipList.size(); i++) {
				ClipRequest cr = csvClipList.get(i);
				FESliceDataUnit sdu = (FESliceDataUnit) cr.dataUnit;
				if (i == 0 || cr.getTimeInDataUnit() > csvClipList.get(i-1).getEndTimeInDataUnit() + feControl.getParams().miscJoinDistance)
					counter++;
				sdu.setClusterID(String.format("%05d", (sdu.getUID() - (sdu.getUID() % 1000000))/1000000)+"-"+String.format("%05d", counter));
				csvClipList.get(i).dataUnit = sdu;
			}
		}
	}
	
	/**
	 * Called at end of setup of after settings dialog to subscribe data blocks. 
	 * @author Doug Gillespie (modified by Holly LeBlond)
	 */
	public synchronized void subscribeDataBlocks() {
		unSubscribeDataBlocks();
		rawDataBlock = PamController.getInstance().getRawDataBlock(feControl.getParams().audioSourceProcessName);
		setParentDataBlock(rawDataBlock, true);
		
		int nBlocks = 1;
		clipBlockProcesses = new ClipBlockProcess[nBlocks];
		PamDataBlock aDataBlock;
		if (feControl.getParams().inputFromWMATorMTSF) {
			aDataBlock = new PamDataBlock<FESliceDataUnit>(FESliceDataUnit.class, processName, this, 0);
			aDataBlock.setSampleRate(feControl.getParams().sr, false);
		} else {
			aDataBlock = PamController.getInstance().getDetectorDataBlock(feControl.getParams().inputProcessName);
		}
		clipBlockProcesses[0] = new ClipBlockProcess(this, aDataBlock);
	}
	
	/**
	 * Kill off the old ClipBlockProcesses before creating new ones.
	 * @author Doug Gillespie
	 */
	protected void unSubscribeDataBlocks() {
		if (clipBlockProcesses == null) {
			return;
		}
		for (int i = 0; i < clipBlockProcesses.length; i++) {
			if (clipBlockProcesses[i] == null) {
				continue;
			}
			clipBlockProcesses[i].disconnect();
		}
	}
	
	/**
	 * @author Doug Gillespie (modified by Holly LeBlond)
	 */
	public class ClipBlockProcess extends PamObserverAdapter {
		
		private PamDataBlock dataBlock;
		protected FEProcess clipProcess;
		private WavFileWriter wavFile;
		
		/**
		 * @param dataBlock
		 * @param clipGenSetting
		 */
		public ClipBlockProcess(FEProcess clipProcess, PamDataBlock dataBlock) {
			super();
			this.clipProcess = clipProcess;
			this.dataBlock = dataBlock;
			dataBlock.addObserver(this, true);
		}
		
		/**
		 * Process a clip request, i.e. make an actual clip from the raw data. This is called back 
		 * from the main thread receiving raw audio data and is called only after any decisions regarding
		 * whether or not a clip should be made have been taken - to get on and make the clip in the
		 * output folder. 
		 * @param clipRequest clip request information
		 * @return 0 if OK or the cause from RawDataUnavailableException if data are not available. 
		 * @author Doug Gillespie (modified by Holly LeBlond)
		 */
		protected int processClipRequest(ClipRequest clipRequest) { 
			PamDataUnit dataUnit = (PamDataUnit) clipRequest.dataUnit;
			long rawStart = dataUnit.getStartSample();
			long rawEnd = rawStart + dataUnit.getSampleDuration();
			int channelMap = decideChannelMap(dataUnit.getChannelBitmap());
			
			FEParameters params = feControl.getParams();
			
			if ((params.miscIgnoreFileStartChecked && 1000*dataUnit.getStartSample()/dataBlock.getSampleRate() < params.miscIgnoreFileStartLength) ||
				(params.miscIgnoreLowFreqChecked && dataUnit.getFrequency()[0] < params.miscIgnoreLowFreq) ||
				(params.miscIgnoreHighFreqChecked && dataUnit.getFrequency()[1] > params.miscIgnoreHighFreq) ||
				(params.miscIgnoreShortDurChecked && dataUnit.getDurationInMilliseconds() < params.miscIgnoreShortDur) ||
				(params.miscIgnoreLongDurChecked && dataUnit.getDurationInMilliseconds() > params.miscIgnoreLongDur) ||
				(!params.inputFilesAreMTSF() && params.miscIgnoreQuietAmpChecked && dataUnit.getAmplitudeDB() < params.miscIgnoreQuietAmp) ||
				(!params.inputFilesAreMTSF() && params.miscIgnoreLoudAmpChecked && dataUnit.getAmplitudeDB() > params.miscIgnoreLoudAmp) ||
				(params.sr < 2*dataUnit.getFrequency()[1])) {
					return 4;
			}
			
			boolean countUp = false;
			if (prevDU == null || !params.miscClusterChecked || (params.audioNRChecked && nrData == null)) countUp = true;
			else {
				long endTime = prevDU.getEndTimeInMilliseconds();
				if (!params.inputFromWMATorMTSF)
					endTime = feControl.convertFromLocalToUTC(endTime);
				if (endTime + params.miscJoinDistance 
					< feControl.convertFromLocalToUTC(dataUnit.getTimeMilliseconds())) countUp = true;
			}
			
			if (params.audioNRChecked && countUp) {
				try {
					nrData = rawDataBlock.getSamples(rawStart - convertMsToSamples(params.audioNRStart), convertMsToSamples(params.audioNRLength), channelMap);
				} catch (RawDataUnavailableException e) {
					// TODO Make the print statements a troubleshooting option at some point.
					//System.out.println("Start sample in block: "+String.valueOf(rawDataBlock.getLastUnit().getStartSample()));
					//System.out.println("Start sample of NR clip: "+String.valueOf(rawStart-convertMsToSamples(params.audioNRStart)));
					//e.printStackTrace(); // TODO Remove?
					return e.getDataCause();
				}
				
				if (nrData == null) {
					System.out.println("Null NR clip");
					return RawDataUnavailableException.DATA_ALREADY_DISCARDED;
				}
			}
			
			double[][] rawData = null;
			try {
				if (params.audioAutoClipLength) rawData = rawDataBlock.getSamples(rawStart, (int) (rawEnd-rawStart), channelMap);
				else rawData = rawDataBlock.getSamples(rawStart, convertMsToSamples(params.audioClipLength), channelMap);
			}
			catch (RawDataUnavailableException e) {
				// TODO Make the print statements a troubleshooting option at some point.
				//System.out.println("Start sample in block: "+String.valueOf(rawDataBlock.getLastUnit().getStartSample()));
				//System.out.println("Start sample of raw data: "+String.valueOf(rawStart));
				//e.printStackTrace(); // TODO Remove?
				return e.getDataCause();
			}
			if (rawData == null) {
				System.out.println("Null raw data");
				return RawDataUnavailableException.DATA_ALREADY_DISCARDED; // if rawDataBlock.getSamples returns null, assume that the data is already gone and return an error
			}
			
			String nrPath = "";
			if (params.audioNRChecked && countUp) {
				nrPath = createClipPath(dataUnit.getUID(), clusterCountID+1, true);
				AudioFormat af = new Wav16AudioFormat(getSampleRate(), nrData.length);
				wavFile = new WavFileWriter(nrPath, af);
				boolean nrWrote = wavFile.write(nrData);
				wavFile.close();
				if (!nrWrote) {
					System.out.println("Could not write file: "+nrPath);
					return RawDataUnavailableException.DATA_ALREADY_DISCARDED;
				}
			}
			
			String clipPath;
			if (countUp) clipPath = createClipPath(dataUnit.getUID(), clusterCountID+1, false);
			else clipPath = createClipPath(dataUnit.getUID(), clusterCountID, false);
			AudioFormat af = new Wav16AudioFormat(getSampleRate(), rawData.length);
			wavFile = new WavFileWriter(clipPath, af);
			boolean clipWrote = wavFile.write(rawData);
			wavFile.close();
			if (!clipWrote) {
				if (params.audioNRChecked && countUp) {
					File f = new File(nrPath);
					f.delete();
				}
				System.out.println("Could not write file: "+nrPath);
				return RawDataUnavailableException.DATA_ALREADY_DISCARDED;
			}
			
			prevDU = dataUnit;
			if (countUp) clusterCountID++;
			
			String clusterID = createClusterID(dataUnit.getUID(), clusterCountID, false);
			long[] sliceStartSamples;
			double[] sliceFreqs;
			if (params.inputFromWMATorMTSF && params.inputFilesAreMTSF()) {
				FETrainingDataUnit tdu = (FETrainingDataUnit) dataUnit;
				sliceStartSamples = new long[] {-1,-1};
				sliceFreqs = new double[] {-1,-1};
				clusterID = tdu.clusterID;
			} else if (params.inputFromWMATorMTSF && !params.inputFilesAreMTSF()) {
				FESliceDataUnit sdu = (FESliceDataUnit) dataUnit;
				sliceStartSamples = sdu.sliceStartSamples;
				sliceFreqs = sdu.sliceFreqs;
				if (sdu.clusterID != null)
					clusterID = sdu.clusterID;
				else
					// This shouldn't happen, but I'm not sure how to handle it if it did.
					clusterID = createClusterID(dataUnit.getUID(), 0, false);
			} else {
				ConnectedRegionDataUnit crdu = (ConnectedRegionDataUnit) dataUnit;
				long contourStartSample = crdu.getStartSample();
				List<SliceData> sdList = crdu.getConnectedRegion().getSliceData();
				Collections.sort(sdList, Comparator.comparingLong(SliceData::getStartSample));
				sliceStartSamples = new long[sdList.size()];
				sliceFreqs = new double[sdList.size()];
				for (int i = 0; i < sdList.size(); i++) {
					FFTDataBlock fftDB = (FFTDataBlock) crdu.getFFTDataUnits(0).get(0).getParentDataBlock();
					sliceStartSamples[i] = sdList.get(i).getStartSample();
					// For unknown reasons, the slice's start sample isn't always set.
					if (sliceStartSamples[i] == 0) sliceStartSamples[i] = i * fftDB.getFftHop() + contourStartSample;
					sliceFreqs[i] = fftDB.getSampleRate() * sdList.get(i).getPeakInfo()[0][2] / fftDB.getFftLength();
				}
			}
			String[] extras = new String[8];
			extras[0] = "uid="+String.valueOf(dataUnit.getUID());
			if (params.inputFromWMATorMTSF)
				extras[1] = "datelong="+String.valueOf(dataUnit.getTimeMilliseconds());
			else
				extras[1] = "datelong="+String.valueOf(feControl.convertFromLocalToUTC(dataUnit.getTimeMilliseconds()));
			extras[2] = "amplitude="+String.valueOf(dataUnit.getAmplitudeDB());
			extras[3] = "duration="+String.valueOf(dataUnit.getDurationInMilliseconds());
			extras[4] = "freqhd_min="+String.valueOf(dataUnit.getFrequency()[0]);
			extras[5] = "freqhd_max="+String.valueOf(dataUnit.getFrequency()[1]);
			extras[6] = "slice_data=[("+String.valueOf(sliceStartSamples[0])+","+String.valueOf(sliceFreqs[0]);
			for (int i = 1; i < sliceFreqs.length; i++)
				extras[6] += "),("+String.valueOf(sliceStartSamples[i])+","+String.valueOf(sliceFreqs[i]);
			extras[6] += ")]";
			extras[7] = "";
			if (params.inputFromWMATorMTSF && params.inputFilesAreMTSF()) {
				FETrainingDataUnit tdu = (FETrainingDataUnit) dataUnit;
				extras[7] += "pe_cluster_id=\""+clusterID+"\"";
				extras[7] += ",pe_location=\""+tdu.location+"\"";
				extras[7] += ",pe_label=\""+tdu.label+"\"";
				extras[7] += ",pe_header_features={";
				Iterator<String> it = tdu.problematicFeatures.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					extras[7] += "\""+key+"\": "+String.valueOf(tdu.problematicFeatures.get(key));
					if (it.hasNext()) extras[7] += ",";
				}
				extras[7] += "}";
			} else {
				extras[7] += "pe_cluster_id=\""+clusterID+"\"";
			}
			feControl.getThreadManager().sendContourClipToThread(clusterID, String.valueOf(dataUnit.getUID()), nrPath, clipPath, extras);
			
			return 0; // no error. 
		}

		/**
		 * @return 1
		 */
		protected int decideChannelMap(int channelBitmap) {
			return 1;
		}

		/**
		 * disconnect from it's data source.
		 * @author Doug Gillespie
		 */
		public void disconnect() {
			dataBlock.deleteObserver(this);
		}
		
		@Override
		public String getObserverName() {
			return clipProcess.getObserverName();
		}
		
		@Override
		public PamObserver getObserverObject() {
			return clipProcess.getObserverObject();
		}
		
		@Override
		public void addData(PamObservable o, PamDataUnit dataUnit) {
			/**
			 * This one should get updates from the triggering data block. 
			 */
			addClipRequest(new ClipRequest(this, dataUnit));
		}

		@Override
		public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {}
	}
	
	/**
	 * Data needed for a clip request. 
	 * @author Doug Gillespie (modified by Holly LeBlond)
	 */
	public class ClipRequest {
		
		protected ClipBlockProcess clipBlockProcess;
		protected PamDataUnit dataUnit;
		
		public ClipRequest(ClipBlockProcess clipBlockProcess,
				PamDataUnit dataUnit) {
			super();
			this.clipBlockProcess = clipBlockProcess;
			this.dataUnit = dataUnit;
		}
		
		/**
		 * @return The UID in the data unit
		 * @author Holly LeBlond
		 */
		public long getUID() {
			return dataUnit.getUID();
		}
		
		/**
		 * @return The start time in the data unit
		 * @author Holly LeBlond
		 */
		public long getTimeInDataUnit() {
			return dataUnit.getTimeMilliseconds();
		}
		
		/**
		 * @return The end time in the data unit
		 * @author Holly LeBlond
		 */
		public long getEndTimeInDataUnit() {
			return dataUnit.getEndTimeInMilliseconds();
		}
	}
	
	/**
	 * Used for signaling the thread manager that the audio has loaded past the current cluster plus the join distance.
	 * Also used for putting CSV-loaded input data into the clipRequestQueue when the audio has loaded far enough.
	 * @author Holly LeBlond
	 */
	public class RawDataBlockCheckerThread extends Thread {
		protected RawDataBlockCheckerThread() {}
		
		@Override
		public void run() {
			while (!vectorDataBlock.isFinished()) {
				try {
					TimeUnit.MILLISECONDS.sleep(20);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (rawDataBlock.getLastUnit() == null) continue;
				
				long currTime = rawDataBlock.getLastUnit().getEndTimeInMilliseconds(); // NOTE THAT THIS IS IN LOCAL TIME FOR SOME REASON !!!
				AcquisitionControl daq = (AcquisitionControl) feControl.getPamController().findControlledUnit("Data Acquisition");
				StandardFileDate sfd = (StandardFileDate) daq.getFileDate();
				ZonedDateTime zdt = ZonedDateTime.now();
				currTime = feControl.convertBetweenTimeZones(currTime, ZonedDateTime.now().getZone().getId(), sfd.getSettings().getTimeZoneName());
				
				if (feControl.getParams().inputFromWMATorMTSF) {
					while (csvClipList.size() > 0) {
						// Note that csvClipList should already be sorted by end time.
						ClipRequest cr = csvClipList.get(0);
						if (cr.getEndTimeInDataUnit() <= currTime) {
							addClipRequest(cr);
							csvClipList.remove(0);
						} else break;
					}
				}
				
				feControl.getThreadManager().setRDBCTTimestamp(currTime);
			}
		}
	}
	
	public int convertMsToSamples(int ms) {
		int samples = (int) (feControl.getParams().sr * ((double) ms/1000));
		return samples;
	}
	
	/**
	 * Get the output folder, based on time and sub folder options.
	 */
	public String getClipFileFolder() {
		return feControl.getParams().tempFolder;
	}
	
	/**
	 * Creates a cluster ID String based off of the input UID and cluster count number.
	 * @param uid
	 * @param clusterCount
	 * @param underscore - Whether or not it uses a dash or an underscore.
	 * @author Holly LeBlond
	 */
	public String createClusterID(long uid, int clusterCount, boolean underscore) {
		if (underscore) {
			return String.format("%05d", (uid - (uid % 1000000))/1000000)+"_"+String.format("%05d", clusterCount);
		}
		return String.format("%05d", (uid - (uid % 1000000))/1000000)+"-"+String.format("%05d", clusterCount);
	}
	
	/**
	 * Produces the path for a new sound clip with a cluster ID.
	 * @param uid
	 * @param clusterCount
	 * @param isNR - If the file is supposed to be used for noise removal (true) or if it's the actual contour clip (false).
	 * @author Holly LeBlond
	 */
	public String createClipPath(long uid, int clusterCount, boolean isNR) {
		String fp = getClipFileFolder();
		String cID = createClusterID(uid, clusterCount, true);
		if (isNR) return fp+"NR_"+cID+".wav";
		return fp+"FE_"+cID+"_"+String.valueOf(uid)+".wav";
	}

	/**
	 * @author Doug Gillespie
	 */
	@Override
	public boolean flushDataBlockBuffers(long maxWait) {
		boolean ans = super.flushDataBlockBuffers(maxWait);
		processRequestList(); // one last go at processing the clip request list before stopping.
		return ans;
	}
	
	/**
	 * @author Doug Gillespie
	 */
	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		long minH = 0;
		if (clipBlockProcesses == null || clipBlockProcesses.length == 0) {
			return 0;
		}
		for (int i = 0; i < clipBlockProcesses.length; i++) {
			if (clipBlockProcesses[i] == null) {
				continue;
			}
			minH = Math.max(minH, clipBlockProcesses[i].getRequiredDataHistory(o, arg));
		}
		minH += Math.max(3000, 192000/(long)getSampleRate());
		if (specMouseDown) {
			minH = Math.max(minH, masterClockTime-specMouseDowntime);
		}
		return minH;
	}
	
	/**
	 * @author Doug Gillespie
	 */
	protected void addClipRequest(ClipRequest clipRequest) {
		synchronized (clipRequestSynch) {
			clipRequestQueue.add(clipRequest);
		}
	}
	
	/**
	 * @return The output data block.
	 * @author Holly LeBlond
	 */
	public FEDataBlock getVectorDataBlock() {
		return vectorDataBlock;
	}
	
	/**
	 * Adds a data unit containing vector data to the output data block.
	 * @author Holly LeBlond
	 */
	public void addVectorData(FEDataUnit du) {
		vectorDataBlock.addPamData(du);
	}
}