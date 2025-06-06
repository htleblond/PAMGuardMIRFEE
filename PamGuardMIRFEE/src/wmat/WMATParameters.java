package wmat;

import java.io.Serializable;
import java.util.ArrayList;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;

/**
 * Parameters object for the WMAT.
 * @author Holly LeBlond
 */
public class WMATParameters implements Serializable, Cloneable, ManagedParameters {
	
	public static final int USE_CLUSTER_PREDICTIONS = 0;
	public static final int USE_CONTOUR_PREDICTIONS = 1;
	
	public ArrayList<String> speciesList;
	public ArrayList<String> callTypeList;
	
	public int startBuffer;
	public int scrollBuffer;
	public int tableHeight;
	
	public String predictionSourceName;
	public int predictionContext;
	public String slicedataSourceName;
	public boolean binaryIsInLocalTime;
	public boolean databaseUTCColumnIsInLocalTime;
	
	public String sqlTableName;
	
	//public boolean hotkeyCtrlRequired;
	public boolean hotkeyQEnabled;
	public boolean hotkeyWEnabled;
	public boolean hotkeyEEnabled;
	public boolean hotkeyZEnabled;
	public boolean[] hotkeyNumEnabled;
	public String[][] hotkeyNumLabels;
	
	public WMATParameters() {
		
		String[] speciesNames = new String[] {"", "2-second glitch", "False positive", "KW", "KWSR", "KWT", "KWNR", 
				"HW", "CSL", "PWSD", "KW/HW?", "KW/PWSD?", "Fish",
				"Vessel", "Mooring", "Unk", "Unk-Bio", "Unk-Anthro", "Unk-Odontocete", "Unk-Mysticete", "Unk-Cetacean", "Deployment", "Aliens"};
		speciesList = new ArrayList<String>();
		for (int i = 0; i < speciesNames.length; i++) speciesList.add(speciesNames[i]);
		
		String[] callTypeNames = {"","n/a","Whistle","Moan","N01i","N01ii","N01iii","N01iv","N01v","N02","N03",
				"N04","N05i","N05ii","N07i","N07ii","N07iii","N07iv","N08i","N08ii","N08iii","N09i","N09ii",
				"N09iii","N10","N11","N12","N13","N16i","N16ii","N16iii","N16iv","N17","N18","N20","N21",
				"N27","N47","Unnamed Aclan","Unnamed AAsubclan","Unnamed ABsubclan","N23i","N23ii","N24i","N24ii","N25","N26",
				"N28","N29","N30","N39","N40","N41","N44","N45","N46","N48","Unnamed Gclan","Unnamed GGsubclan","Unnamed GIsubclan",
				"N32i","N32ii","N33","N34","N42","N43","N50","N51","N52","Unnamed Rclan","S01","S02i","S02ii","S02iii","S03","S04",
				"S05","S06","S07","S08i","S08ii","S09","S10","S12","S13i","S13ii","S14","S16","S17","S18","S19","S22","S31","S33",
				"S36","S37i","S37ii","S40","S41","S42","S44","Unnamed Jclan"};
		callTypeList = new ArrayList<String>();
		for (int i = 0; i < callTypeNames.length; i++) callTypeList.add(callTypeNames[i]);
		
		startBuffer = 2000;
		scrollBuffer = 1000;
		tableHeight = 300;
		
		predictionSourceName = null;
		predictionContext = USE_CLUSTER_PREDICTIONS;
		slicedataSourceName = null;
		binaryIsInLocalTime = true;
		databaseUTCColumnIsInLocalTime = false;
		
		sqlTableName = "whistle_and_moan_detector";
		
		//hotkeyCtrlRequired = true;
		hotkeyQEnabled = true;
		hotkeyWEnabled = true;
		hotkeyEEnabled = true;
		hotkeyZEnabled = true;
		hotkeyNumEnabled = new boolean[10];
		for (int i = 0; i < hotkeyNumEnabled.length; i++)
			hotkeyNumEnabled[i] = false;
		hotkeyNumLabels = new String[10][2];
		for (int i = 0; i < hotkeyNumLabels.length; i++)
			hotkeyNumLabels[i] = new String[] {"<skip>", "<skip>"};
	}
	
	@Override
	public PamParameterSet getParameterSet() {
		return PamParameterSet.autoGenerate(this);
	}
}