package mirfeeLiveClassifier;

import PamguardMVC.PamDataUnit;
import dataGram.DatagramProvider;
import dataGram.DatagramScaleInformation;

/**
 * The Live Classifier's DatagramProvider.
 * @author Holly LeBlond
 */
public class LCDatagramProvider implements DatagramProvider {
	
	protected LCControl lcControl;
	protected LCDataBlock lcDataBlock;
	protected LCProcess lcProcess;
	
	/**
	 * @param lcDataBlock
	 */
	public LCDatagramProvider(LCControl lcControl, LCDataBlock lcDataBlock) {
		super();
		this.lcControl = lcControl;
		this.lcDataBlock = lcDataBlock;
		this.lcProcess = (LCProcess) lcDataBlock.getParentProcess();
	}

	@Override
	public int getNumDataGramPoints() {
		if (lcControl.getParams().labelOrder.length > 0) {
			return lcControl.getParams().labelOrder.length;
		}
		return 1;
	}

	@Override
	public int addDatagramData(PamDataUnit dataUnit, float[] dataGramLine) {
		//if (lcControl.getParams().printJava)
		//	System.out.println("Reached addDatagramData");
		LCDataUnit du = (LCDataUnit) dataUnit;
		LCCallCluster cc = du.getCluster();
		int totalPoints = 0;
		try {
			dataGramLine[cc.labelList.indexOf(cc.getPredictedSpeciesString())]++;
			totalPoints++;
		} catch (Exception e) {
			//
		}
		//System.out.println(dataGramLine[cc.labelList.indexOf(cc.getPredictedSpeciesString())]);
		return totalPoints;
	}

	/* (non-Javadoc)
	 * @see dataGram.DatagramProvider#getScaleInformation()
	 */
	@Override
	public DatagramScaleInformation getScaleInformation() {
		return new DatagramScaleInformation(0, getNumDataGramPoints()-1, "Label #");
	}

}