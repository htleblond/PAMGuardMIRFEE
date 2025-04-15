package mirfeeFeaturePlotterOld;

import PamModel.PamDependency;
import PamModel.PamPluginInterface;
import mirfee.MIRFEEInfo;

/**
 * Music Information Retrieval Random Forest (MIRFEE) Classifier for PamGuard - Feature Plotter
 * @author Holly LeBlond (Fisheries and Oceans Canada)
 */
public class FPPlugin implements PamPluginInterface {
	
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
		return "mirfeeFeaturePlotter.FPControl";
	}

	@Override
	public String getDefaultName() {
		return "MIRFEE Feature Plotter";
	}

	@Override
	public String getMenuGroup() {
		return "Classifiers";
	}

	@Override
	public String getToolTip() {
		return "TBA";
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
		String desc = "TBA";
		return desc;
	}

	/* (non-Javadoc)
	 * @see PamModel.PamPluginInterface#allowedModes()
	 */
	@Override
	public int allowedModes() {
		return PamPluginInterface.ALLMODES;
	}

}
