package mirfeeFeaturePlotterOld;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.jogamp.newt.Window;

import PamView.PamSidePanel;
import PamView.PamTabPanel;
import PamguardMVC.PamDataBlock;
import whistlesAndMoans.*;
//import whistlesAndMoans.WhistleMoanControl.DetectionSettings;
import Layout.DisplayPanelProvider;
import Layout.DisplayProviderList;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import userDisplay.*;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;

/**
 * Haven't released this one yet.
 * This module should eventually allow you to create scatter plots for comparing features
 * extracted by the Feature Extractor, but it doesn't work super great yet.
 * I'll get back to this one eventually.
 * @author Holly LeBlond
 */
public class FPControl extends PamControlledUnit implements PamSettings {
	
	public static final String UNITTYPE = "MIRFEEFP";
	
	protected FPTabPanel fpTabPanel;

	public FPControl(String unitName) {
		super(UNITTYPE, "MIRFEE Feature Plotter");
		
		fpTabPanel = new FPTabPanel(this);
	}
	
	/**
	 * Streamlined error dialog with an editable message.
	 */
	public void simpleErrorDialog(String inptext, int width) {
		JOptionPane.showMessageDialog(this.getGuiFrame(),
				makeHTML(inptext, width),
			"MIRFEE Feature Plotter",
			JOptionPane.ERROR_MESSAGE);
	}
	
/*	// Kudos to this: https://stackoverflow.com/questions/1842223/java-linebreaks-in-jlabels
	public String makeHTML(String inp, int width) {
		//int width = 150;
		String outp = "<html><p style=\"width:"+Integer.toString(width)+"px\">"+inp+"</p></html>";
		return outp;
	} */
	
	public String makeHTML(String inp, int width) {
		return String.format("<html><body style='width: %1spx'>%1s", width, inp);
	}
		
	@Override
	public FPTabPanel getTabPanel() {
		return fpTabPanel;
	}

	@Override
	public Serializable getSettingsReference() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSettingsVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		// TODO Auto-generated method stub
		return false;
	}
	
}