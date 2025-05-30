package mirfeeFeatureExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import PamModel.parametermanager.PamParameterSet;
import mirfee.MIRFEEParameters;

import org.apache.commons.text.WordUtils;

//@SuppressWarnings("serial")
public class FEParameters extends MIRFEEParameters {
	
	public static final int OUTPUT_NONE = 0;
	public static final int OUTPUT_MFE = 1;
	public static final int OUTPUT_MTSF = 2;
	
	public int sr;
	
	public boolean inputFromWMATorMTSF;
	public String inputProcessName;
	public ArrayList<String> inputWMATFileNames;
	//public ArrayList<File> inputDataFiles;
	//public ArrayList<FEInputDataObject> inputDataEntries;
	//public ArrayList<Integer> inputDataIndexes;
	public int inputDataExpectedFileSize;
	public boolean inputIgnoreBlanks;
	public boolean inputIgnore2SecondGlitch;
	public boolean inputIgnoreFalsePositives;
	public boolean inputIgnoreUnk;
	
	//public boolean outputDataChecked;
	public int outputDataOption;
	public String outputDataName;
	public boolean outputDecimalLimit;
	
	public String audioSourceProcessName;
	public boolean audioAutoClipLength;
	public int audioClipLength;
	public int audioSTFTLength;
	public int audioHopSize;
	public String audioWindowFunction;
	public boolean audioNormalizeChecked;
	public boolean audioHPFChecked;
	public int audioHPFThreshold;
	public int audioHPFMagnitude;
	public boolean audioLPFChecked;
	public int audioLPFThreshold;
	public int audioLPFMagnitude;
	public boolean audioNRChecked;
	public int audioNRStart;
	public int audioNRLength;
	public double audioNRScalar;
	
	public String[][] featureList;
	
	public boolean miscClusterChecked;
	public int miscJoinDistance;
	public boolean miscIgnoreFileStartChecked;
	public int miscIgnoreFileStartLength;
	public boolean miscIgnoreLowFreqChecked;
	public int miscIgnoreLowFreq;
	public boolean miscIgnoreHighFreqChecked;
	public int miscIgnoreHighFreq;
	public boolean miscIgnoreShortDurChecked;
	public int miscIgnoreShortDur;
	public boolean miscIgnoreLongDurChecked;
	public int miscIgnoreLongDur;
	public boolean miscIgnoreQuietAmpChecked;
	public int miscIgnoreQuietAmp;
	public boolean miscIgnoreLoudAmpChecked;
	public int miscIgnoreLoudAmp;
	
	public boolean miscPrintJavaChecked;
	public boolean miscPrintInputChecked;
	public boolean miscPrintOutputChecked;
	
	public int expMaxThreads;
	public int expMaxClipsAtOnce;
	public int expBlockPushTriggerBuffer;
	
	public FEParameters() {
		
		this.sr = 0;
		
		this.inputFromWMATorMTSF = false;
		this.inputProcessName = "";
		this.inputWMATFileNames = new ArrayList<String>();
		//this.inputDataFiles = new ArrayList<File>();
		//this.inputDataEntries = new ArrayList<FEInputDataObject>();
		//this.inputDataIndexes = new ArrayList<Integer>();
		this.inputDataExpectedFileSize = 10;
		this.inputIgnoreBlanks = true;
		this.inputIgnore2SecondGlitch = true;
		this.inputIgnoreFalsePositives = true;
		this.inputIgnoreUnk = true;
		
		//this.outputDataChecked = false;
		this.outputDataOption = OUTPUT_NONE;
		this.outputDataName = "";
		this.outputDecimalLimit = true;
		
		this.audioSourceProcessName = "";
		this.audioAutoClipLength = true;
		this.audioClipLength = 350;
		this.audioSTFTLength = 2048;
		this.audioHopSize = 1024;
		this.audioWindowFunction = "Hann";
		this.audioNormalizeChecked = false;
		this.audioHPFChecked = false;
		this.audioHPFThreshold = 2000;
		this.audioHPFMagnitude = 1;
		this.audioLPFChecked = false;
		this.audioLPFThreshold = 20000;
		this.audioLPFMagnitude = 1;
		this.audioNRChecked = false;
		this.audioNRStart = 500;
		this.audioNRLength = 350;
		this.audioNRScalar = 1.0;
		
		this.featureList = new String[0][0];
		
		this.miscClusterChecked = true;
		this.miscJoinDistance = 2000;
		this.miscIgnoreFileStartChecked = true;
		this.miscIgnoreFileStartLength = 2000;
		this.miscIgnoreLowFreqChecked = false;
		this.miscIgnoreLowFreq = 0;
		this.miscIgnoreHighFreqChecked = false;
		this.miscIgnoreHighFreq = 0;
		this.miscIgnoreShortDurChecked = false;
		this.miscIgnoreShortDur = 0;
		this.miscIgnoreLongDurChecked = false;
		this.miscIgnoreLongDur = 0;
		this.miscIgnoreQuietAmpChecked = false;
		this.miscIgnoreQuietAmp = 0;
		this.miscIgnoreLoudAmpChecked = false;
		this.miscIgnoreLoudAmp = 0;
		
		this.miscPrintJavaChecked = false;
		this.miscPrintInputChecked = false;
		this.miscPrintOutputChecked = false;
		
		this.expMaxThreads = 10;
		this.expMaxClipsAtOnce = 50;
		this.expBlockPushTriggerBuffer = 1000;
		
	}
	
	public String getFeatureAbbrsAsString() {
		String outp = "";
		for (int i = 0; i < featureList.length; i++) {
			outp += featureList[i][1];
			if (i < featureList.length-1) outp += ",";
		}
		return outp;
	}
	
	public boolean inputFilesAreMTSF() {
		return this.inputFromWMATorMTSF && this.inputWMATFileNames.size() > 0 && 
				(this.inputWMATFileNames.get(0).endsWith(".mirrfts") || this.inputWMATFileNames.get(0).endsWith(".mtsf"));
	}
	
	public HashMap<String, String> outputParamsToHashMap() {
		HashMap<String, String> outp = new HashMap<String, String>();
		outp.put("sr", String.valueOf(sr)); // TODO THIS WILL PROBABLY CAUSE ISSUES
		outp.put("audioAutoClipLength", String.valueOf(audioAutoClipLength));
		if (!audioAutoClipLength) {
			outp.put("audioClipLength", String.valueOf(audioClipLength));
		}
		outp.put("audioSTFTLength", String.valueOf(audioSTFTLength));
		outp.put("audioHopSize", String.valueOf(audioHopSize));
		outp.put("audioWindowFunction", String.valueOf(audioWindowFunction));
		outp.put("audioNormalizeChecked", String.valueOf(audioNormalizeChecked));
		outp.put("audioHPFChecked", String.valueOf(audioHPFChecked));
		if (audioHPFChecked) {
			outp.put("audioHPFThreshold", String.valueOf(audioHPFThreshold));
			outp.put("audioHPFMagnitude", String.valueOf(audioHPFMagnitude));
		}
		outp.put("audioLPFChecked", String.valueOf(audioLPFChecked));
		if (audioLPFChecked) {
			outp.put("audioLPFThreshold", String.valueOf(audioLPFThreshold));
			outp.put("audioLPFMagnitude", String.valueOf(audioLPFMagnitude));
		}
		outp.put("audioNRChecked", String.valueOf(audioNRChecked));
		if (audioNRChecked) {
			outp.put("audioNRStart", String.valueOf(audioNRStart));
			outp.put("audioNRLength", String.valueOf(audioNRLength));
			outp.put("audioNRScalar", String.valueOf(audioNRScalar));
		}
		//outp.put("featureList", getFeatureAbbrsAsString());
		
		// TODO Re-consider the following:
	/*	outp.put("miscClusterChecked", String.valueOf(miscClusterChecked));
		if (miscClusterChecked) outp.put("miscJoinDistance", String.valueOf(miscJoinDistance));
		outp.put("miscIgnoreFileStartChecked", String.valueOf(miscIgnoreFileStartChecked));
		if (miscIgnoreFileStartChecked) outp.put("miscIgnoreFileStartLength", String.valueOf(miscIgnoreFileStartLength));
		outp.put("miscIgnoreLowFreqChecked", String.valueOf(miscIgnoreLowFreqChecked));
		if (miscIgnoreLowFreqChecked) outp.put("miscIgnoreLowFreq", String.valueOf(miscIgnoreLowFreq));
		outp.put("miscIgnoreHighFreqChecked", String.valueOf(miscIgnoreHighFreqChecked));
		if (miscIgnoreHighFreqChecked) outp.put("miscIgnoreHighFreq", String.valueOf(miscIgnoreHighFreq));
		outp.put("miscIgnoreShortDurChecked", String.valueOf(miscIgnoreShortDurChecked));
		if (miscIgnoreShortDurChecked) outp.put("miscIgnoreShortDur", String.valueOf(miscIgnoreShortDur));
		outp.put("miscIgnoreLongDurChecked", String.valueOf(miscIgnoreLongDurChecked));
		if (miscIgnoreLongDurChecked) outp.put("miscIgnoreLongDur", String.valueOf(miscIgnoreLongDur));
		outp.put("miscIgnoreQuietAmpChecked", String.valueOf(miscIgnoreQuietAmpChecked));
		if (miscIgnoreQuietAmpChecked) outp.put("miscIgnoreQuietAmp", String.valueOf(miscIgnoreQuietAmp));
		outp.put("miscIgnoreLoudAmpChecked", String.valueOf(miscIgnoreLoudAmpChecked));
		if (miscIgnoreLoudAmpChecked) outp.put("miscIgnoreLoudAmp", String.valueOf(miscIgnoreLoudAmp)); */
		
		return outp;
	}
	
	public ArrayList<String> findUnmatchedParameters(HashMap<String, String> newMap, boolean fromFile) {
		HashMap<String, String> currMap = outputParamsToHashMap();
		return findUnmatchedParameters(currMap, newMap, fromFile);
	}
	
	public ArrayList<String> findUnmatchedParameters(HashMap<String, String> currMap, HashMap<String, String> newMap, boolean fromFile) {
		ArrayList<String> outp = new ArrayList<String>();
		HashMap<String, String> map1 = currMap;
		HashMap<String, String> map2 = newMap;
		if (fromFile) {
			map1 = newMap;
			map2 = currMap;
		}
		Iterator<String> it = map1.keySet().iterator();
		while (it.hasNext()) {
			String nextKey = it.next();
			if (!map2.containsKey(nextKey)) outp.add(nextKey);
			else if (!map1.get(nextKey).equals(map2.get(nextKey))) outp.add(nextKey);
		}
		return outp;
	}
	
	@Override
	public String outputPythonParamsToText() {
		if (tempFolder.length() == 0) {
			return "";
		}
		String outp = "[";
		if (inputFromWMATorMTSF) {
			if (inputFilesAreMTSF())
				outp += "\"mtsf\",";
			else
				outp += "\"wmat\",";
		} else
			outp += "\"detector\",";
		outp += String.valueOf(sr)+",";
		outp += WordUtils.capitalize(String.valueOf(outputDecimalLimit))+",";
		outp += WordUtils.capitalize(String.valueOf(audioAutoClipLength))+",";
		outp += String.valueOf(audioClipLength)+",";
		outp += String.valueOf(audioSTFTLength)+",";
		outp += String.valueOf(audioHopSize)+",";
		outp += "\""+String.valueOf(audioWindowFunction)+"\",";
		outp += WordUtils.capitalize(String.valueOf(audioNormalizeChecked))+",";
		outp += WordUtils.capitalize(String.valueOf(audioHPFChecked))+",";
		outp += String.valueOf(audioHPFThreshold)+",";
		outp += String.valueOf(audioHPFMagnitude)+",";
		outp += WordUtils.capitalize(String.valueOf(audioLPFChecked))+",";
		outp += String.valueOf(audioLPFThreshold)+",";
		outp += String.valueOf(audioLPFMagnitude)+",";
		outp += WordUtils.capitalize(String.valueOf(audioNRChecked))+",";
		outp += String.valueOf(audioNRStart)+",";
		outp += String.valueOf(audioNRLength)+",";
		outp += String.valueOf(audioNRScalar)+",";
		outp += "[";
		for (int i = 0; i < featureList.length; i++) {
			outp += "\""+featureList[i][1]+"\"";
			if (i < featureList.length-1) {
				outp += ",";
			} else {
				outp += "]";
			}
		}
		outp += "]";
		return outp;
	}

	@Override
	public PamParameterSet getParameterSet() {
		return PamParameterSet.autoGenerate(this);
	}
	
	@Override
	public FEParameters clone() {
		return (FEParameters) super.clone();
	}
}