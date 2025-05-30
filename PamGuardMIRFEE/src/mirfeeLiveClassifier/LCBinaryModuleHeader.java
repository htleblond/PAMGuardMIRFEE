package mirfeeLiveClassifier;

import java.io.Serializable;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;
import PamModel.parametermanager.PamParameterSet.ParameterSetType;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleHeader;

/**
 * The Live Classifier's module header for binary files.
 * @author Holly LeBlond
 */
public class LCBinaryModuleHeader extends ModuleHeader implements Serializable, ManagedParameters {
	
	private static final long serialVersionUID = 1L;
	
	public String[] species = new String[0];
	public String[] features = new String[0];

	public LCBinaryModuleHeader(int moduleVersion) {
		super(moduleVersion);
		//System.out.println("LCBinaryModuleHeader initialized");
	}

	@Override
	public boolean createHeader(BinaryObjectData binaryObjectData, BinaryHeader binaryHeader) {
		return false;
	}
	
	@Override
	public PamParameterSet getParameterSet() {
		//System.out.println("LCBinaryModuleHeader.getParameterSet() called");
		PamParameterSet ps = PamParameterSet.autoGenerate(this, ParameterSetType.DETECTOR);
		//PamParameterSet ps = PamParameterSet.autoGenerate(this);
		return ps;
	}
}