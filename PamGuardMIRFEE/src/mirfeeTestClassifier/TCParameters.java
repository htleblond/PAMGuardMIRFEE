package mirfeeTestClassifier;

import PamModel.parametermanager.PamParameterSet;
import mirfeeLiveClassifier.LCParameters;
import mirfeeLiveClassifier.LCTrainingSetInfo;

/**
 * The parameters object for the Test Classifier.
 * Subclass of the Live Classifier's parameters.
 * @author Holly LeBlond
 */
public class TCParameters extends LCParameters {
	
	public static final int LEAVEONEOUTBOTHDIGITS = 0;
	public static final int LEAVEONEOUTFIRSTDIGIT = 1;
	public static final int KFOLD = 2;
	public static final int TESTSUBSET = 3;
	public static final int LABELLED = 4;
	public static final int UNLABELLED = 5;
	
	public LCTrainingSetInfo loadedTestingSetInfo;
	
	public int validation;
	//public int kNum; (already in LCParameters)
	public String testSubset;
	
	public TCParameters() {
		super();
		
		loadedTestingSetInfo = new LCTrainingSetInfo("");
		
		validation = LEAVEONEOUTBOTHDIGITS;
		testSubset = "";
	}
	
	public LCTrainingSetInfo getTestingSetInfo() {
		return loadedTestingSetInfo;
	}
	
	public void setTestingSetInfo(LCTrainingSetInfo inp) {
		loadedTestingSetInfo = inp;
	}
	
	public String getTestPath() {
		return loadedTestingSetInfo.pathName;
	}
	
	@Override
	public PamParameterSet getParameterSet() {
		return PamParameterSet.autoGenerate(this);
	}
	
	@Override
	public TCParameters clone() {
		try {
			return (TCParameters) super.clone();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}