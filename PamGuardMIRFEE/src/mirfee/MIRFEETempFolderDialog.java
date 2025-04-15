package mirfee;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import PamController.PamControlledUnit;
import PamUtils.PamFileChooser;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;

/**
 * Dialog for selecting an external folder to store temporary sound files and Python code.
 * @author Holly LeBlond
 */
//@SuppressWarnings("serial")
public class MIRFEETempFolderDialog extends PamDialog {
	
	protected PamControlledUnit control;
	protected MIRFEEParameters params;
	protected String unitName;
	protected String subfolderName;
	private Window parentFrame;
	
	protected int key;
	
	protected JTextField fileField;
	protected JButton selectButton;
	//protected boolean hasMirrfName;
	protected JTextField keyField;
	protected JButton randomizeButton;
	
	public MIRFEETempFolderDialog(Window parentFrame, PamControlledUnit control, String unitName, String subfolderName,
			MIRFEEParameters params, boolean importPreExistingFolderName) {
		super(parentFrame, unitName, false);
		this.control = control;
		this.params = params;
		this.parentFrame = parentFrame;
		//this.hasMirrfName = false;
		this.unitName = unitName;
		this.subfolderName = subfolderName;
		
		JPanel mainPanel = new JPanel(new FlowLayout());
		JPanel subPanel = new JPanel(new GridBagLayout());
		subPanel.setBorder(new TitledBorder(""));
		GridBagConstraints b = new PamGridBagContraints();
		b.gridy = 0;
		b.gridx = 0;
		b.gridwidth = 3;
		b.fill = GridBagConstraints.HORIZONTAL;
		b.anchor = GridBagConstraints.CENTER;
		subPanel.add(new JLabel(makeHTML("MIRFEE requires a folder to temporarily keep sound clips and Python code during processing. "
				+ "Unfortunately, PamGuard's file security configuration doesn't allow the program to create new files or folders within PamGuard's own "
				+ "folders, so a different folder must be created.<br><br>"
				+ "A new folder named \"MIRFEE Temp\\"+subfolderName+"\\[key]\" will be created in the folder you select (unless the selected folder is named \"MIRFEE Temp\" itself). "
				+ "NOTE THAT anything placed inside the new folder will be deleted every time the "+subfolderName+" is run. Do NOT select the same folder containing any audio you plan on "
				+ "processing.", 300)), b);
		b.gridy++;
		b.gridwidth = 2;
		fileField = new JTextField(30);
		if (importPreExistingFolderName) fileField.setText(params.tempFolder);
		fileField.setEnabled(false);
		subPanel.add(fileField, b);
		b.gridx += b.gridwidth;
		b.gridwidth = 1;
		selectButton = new JButton("Select folder");
		selectButton.addActionListener(new FileListener());
		subPanel.add(selectButton, b);
		b.gridy++;
		b.gridx = 0;
		b.anchor = b.EAST;
		//b.fill = b.NONE;
		subPanel.add(new JLabel("Key:", SwingConstants.RIGHT), b);
		b.gridx++;
		b.fill = b.NONE;
		keyField = new JTextField(10);
		keyField.setEnabled(false);
		createRandomKey();
		subPanel.add(keyField, b);
		b.gridx++;
		b.fill = b.HORIZONTAL;
		randomizeButton = new JButton("Randomize");
		randomizeButton.addActionListener(new RandomizeListener());
		subPanel.add(randomizeButton, b);
		
		mainPanel.add(subPanel);
		setDialogComponent(mainPanel);
	}
	
/*	// Kudos to this: https://stackoverflow.com/questions/1842223/java-linebreaks-in-jlabels
	public String makeHTML(String inp) {
		int width = 300;
		String outp = "<html><p style=\"width:"+Integer.toString(width)+"px\">"+inp+"</p></html>";
		return outp;
	} */
	
	public String makeHTML(String inp, int width) {
		return String.format("<html><body style='width: %1spx'>%1s", width, inp);
	}
	
	/**
	 * The listener for the 'Export table' button.
	 */
	class FileListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setMultiSelectionEnabled(false);
			if (fileField.getText().length() > 0) {
				File f = new File(fileField.getText());
				if (f.exists()) fc.setCurrentDirectory(f);
			}
			int returnVal = fc.showSaveDialog(parentFrame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				fileField.setText(fc.getSelectedFile().getPath()+"\\");
			}
		}
	}
	
	protected class RandomizeListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			createRandomKey();
		}
	}
	
	public void createRandomKey() {
		Random rand = new Random();
		key = rand.nextInt(1000000000);
		keyField.setText(String.format("%09d", key));
	}
	
	public PamControlledUnit getControl() {
		return control;
	}
	
	/**
	 * Streamlined error dialog with an editable message.
	 */
	public void simpleErrorDialog(String inptext) {
		JOptionPane.showMessageDialog(null,
			inptext,
			unitName,
			JOptionPane.ERROR_MESSAGE);
	}
	
	@Override
	public boolean getParams() {
		String currPath = fileField.getText();
		File testFile = new File(currPath);
		if (!testFile.exists()) {
			simpleErrorDialog("Selected folder does not exist.");
			return false;
		}
		if (!testFile.isDirectory()) {
			simpleErrorDialog("Selected object is not a folder.");
			return false;
		}
		if (!currPath.endsWith("MIRFEE Temp\\")) {
			currPath += "MIRFEE Temp\\";
			testFile = new File(currPath);
		}
		if (!testFile.exists()) {
			if (!testFile.mkdir()) {
				simpleErrorDialog("Could not create MIRFEE Temp folder within selected folder.");
				return false;
			}
		}
		currPath += subfolderName+"\\";
		testFile = new File(currPath);
		if (!testFile.exists()) {
			testFile.mkdir();
		}
		currPath += keyField.getText()+"\\";
		//System.out.println(currPath);
		testFile = new File(currPath);
		if (!testFile.exists()) {
			testFile.mkdir();
		}
		testFile = new File(currPath+"test.txt");
		if (testFile.canRead()) {
			if (!testFile.delete()) {
				simpleErrorDialog("Folder does not allow file management.");
				return false;
			}
		} else {
			try {
				boolean boo = testFile.createNewFile();
				if (boo) {
					testFile.delete();
				} else {
					simpleErrorDialog("Folder does not allow file management.");
					return false;
				}
			} catch(Exception e) {
				simpleErrorDialog("Folder does not allow file management.");
				return false;
			}
		}
		params.tempFolder = currPath;
		params.tempKey = key;
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {}
}