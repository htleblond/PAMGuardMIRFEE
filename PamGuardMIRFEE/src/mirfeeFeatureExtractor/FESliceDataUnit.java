package mirfeeFeatureExtractor;

import java.util.ArrayList;
import java.util.Comparator;

import PamguardMVC.PamDataUnit;

/**
 * A data unit for storing slice data from .wmat files for
 * when the ClipBlockProcess needs it.
 * @author Holly LeBlond
 */
//@SuppressWarnings("rawtypes")
public class FESliceDataUnit extends PamDataUnit {
	
	public long[] sliceStartSamples;
	public double[] sliceFreqs;
	public String label;
	
	public String clusterID; // initially null
	
	public FESliceDataUnit(FEInputDataObject inp, long startSample, long sampleDuration) {
		super(0, 0, -1, -1);
		
		this.setUID(inp.uid);
		long longtime = FEControl.convertDateStringToLong(inp.datetime);
		this.setTimeMilliseconds(longtime);
		this.setFrequency(new double[] {Double.valueOf(inp.lf), Double.valueOf(inp.hf)});
		this.setDurationInMilliseconds(Double.valueOf(inp.duration));
		this.setMeasuredAmplitude(inp.amplitude);
		this.setStartSample(startSample);
		this.setSampleDuration(sampleDuration);
		
		ArrayList<double[]> splitList = new ArrayList<double[]>();
		for (int i = 0; i < inp.slicedata.length; i++)
			splitList.add(inp.slicedata[i]);
		splitList.sort(Comparator.comparingLong(a -> (long) a[0]));
		int arrSize = splitList.size();
		sliceStartSamples = new long[arrSize];
		sliceFreqs = new double[arrSize];
		for (int i = 0; i < arrSize; i++) {
			sliceStartSamples[i] = (long) splitList.get(i)[0];
			sliceFreqs[i] = splitList.get(i)[1];
		}
		this.label = inp.label;
	}
	
	public void setClusterID(String clusterID) {
		this.clusterID = clusterID;
	}
	
}