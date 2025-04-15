package mirfeeTrainingSetBuilder;

import PamModel.PamDependency;
import PamModel.PamPluginInterface;
import mirfee.MIRFEEInfo;

/**
 * Music Information Retrieval Feature-Extracting Ensemble (MIRFEE) Classifier for PamGuard - Training Set Builder
 * @author Holly LeBlond (Fisheries and Oceans Canada)
 */
public class TSBPlugin implements PamPluginInterface {
	
	String jarFile;
	
	@Override
	public String getDescription() {
		return getDefaultName();
	}

	@Override
	public void setJarFile(String jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	public String getJarFile() {
		return jarFile;
	}

	@Override
	public String getDeveloperName() {
		return MIRFEEInfo.getDeveloperName();
	}

	@Override
	public String getContactEmail() {
		return MIRFEEInfo.getContactEmail();
	}

	@Override
	public String getVersion() {
		return MIRFEEInfo.getVersion();
	}

	@Override
	public String getPamVerDevelopedOn() {
		return MIRFEEInfo.getPamVerDevelopedOn();
	}

	@Override
	public String getPamVerTestedOn() {
		return MIRFEEInfo.getPamVerTestedOn();
	}

	@Override
	public String getClassName() {
		return "mirfeeTrainingSetBuilder.TSBControl";
	}

	@Override
	public String getDefaultName() {
		return "MIRFEE Training Set Builder";
	}

	@Override
	public String getMenuGroup() {
		return "Classifiers";
	}

	@Override
	public String getToolTip() {
		return "Combines Feature Extractor data with Whistle and Moan Annotation Tool "
				+ "annotations to create training sets for the Live and Test Classifiers.";
	}

	@Override
	public PamDependency getDependency() {
		return null;
	}

	@Override
	public int getMinNumber() {
		return 0;
	}

	@Override
	public int getMaxNumber() {
		return 0;
	}

	@Override
	public int getNInstances() {
		return 1;
	}

	@Override
	public boolean isItHidden() {
		return false;
	}

	@Override
	public String getHelpSetName() {
		return null;
	}

	@Override
	public String getAboutText() {
		return "A tool for combining feature vectors output by the Feature Extractor with annotations "
				+ "exported from the Whistle and Moan Annotation Tool to make training sets for the "
				+ "Live and Test Classifiers. Also has a feature for making smaller batches of "
				+ "detection-containing audio for faster test runs.";
	}

	/* (non-Javadoc)
	 * @see PamModel.PamPluginInterface#allowedModes()
	 */
	@Override
	public int allowedModes() {
		return PamPluginInterface.ALLMODES;
	}

}
