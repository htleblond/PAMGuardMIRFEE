package wmat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import PamUtils.PamFileChooser;
import PamView.dialog.PamButton;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.SourcePanel;
import mirfee.MIRFEEControlledUnit;
import mirfeeLiveClassifier.LCDataUnit;
import whistlesAndMoans.ConnectedRegionDataUnit;
import wmat.WMATPanel.SpeciesListener;

/**
 * The settings dialog for the WMAT.
 * @author Holly LeBlond
 */
public class WMATSettingsDialog extends PamDialog {
	
	protected JTextField speciesField;
	protected DefaultComboBoxModel<String> speciesModel;
	protected JComboBox<String> speciesBox;
	protected JTextField callTypeField;
	protected DefaultComboBoxModel<String> callTypeModel;
	protected JComboBox<String> callTypeBox;
	protected JSpinner intervalSpinner;
	protected JSpinner scrollBufferSpinner;
	protected JSpinner tableHeightSpinner;
	//protected WMATTimeZonePanel tzPanel;
	protected SourcePanel predictionSourcePanel;
	protected JComboBox<String> predictionContextBox;
	protected SourcePanel slicedataSourcePanel;
	protected JCheckBox binaryTZCheck;
	protected JCheckBox databaseTZCheck;
	protected JTextField sqlTableField;
	//protected JCheckBox requireCtrlCheck;
	protected JCheckBox hotkeyQCheck;
	protected JCheckBox hotkeyWCheck;
	protected JCheckBox hotkeyECheck;
	protected JCheckBox hotkeyZCheck;
	protected JCheckBox[] hotkeyNumChecks;
	protected JComboBox<String>[] hotkeyNumSpeciesBoxes;
	protected JComboBox<String>[] hotkeyNumCallTypeBoxes;
	
	protected WMATControl wmatControl;
	protected Window parentFrame;
	protected boolean startup;
	
	public WMATSettingsDialog(Window parentFrame, WMATControl wmatControl, boolean startup) {
		super(parentFrame, wmatControl.getUnitName(), true);
		this.wmatControl = wmatControl;
		this.parentFrame = parentFrame;
		this.startup = startup;
		
		JTabbedPane tabbedPane = new JTabbedPane();
		
		//JPanel p = new JPanel(new BorderLayout());
		
		JPanel interfaceTab = new JPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		b.fill = GridBagConstraints.HORIZONTAL;
		b.anchor = GridBagConstraints.WEST;
		
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		speciesPanel.setBorder(new TitledBorder("'Species' list"));
		speciesPanel.setAlignmentX(LEFT_ALIGNMENT);
		GridBagConstraints c = new PamGridBagContraints();
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		speciesField = new JTextField();
		speciesField.setDocument(new JTextFieldLimit(WMATSQLLogging.SPECIES_CHAR_LENGTH).getDocument());
		SpeciesAddListener speciesAddListener = new SpeciesAddListener();
		speciesField.addActionListener(speciesAddListener);
		speciesPanel.add(speciesField, c);
		c.gridx++;
		PamButton speciesAddButton = new PamButton("Add");
		speciesAddButton.addActionListener(speciesAddListener);
		speciesPanel.add(speciesAddButton, c);
		c.gridx++;
		PamButton speciesImportButton = new PamButton("Import");
		// Listener added to button after speciesBox is initialized.
		speciesPanel.add(speciesImportButton, c);
		c.gridy++;
		c.gridx = 0;
		speciesModel = new DefaultComboBoxModel<String>();
		for (int i = 1; i < wmatControl.getParams().speciesList.size(); i++) {
			speciesModel.addElement(wmatControl.getParams().speciesList.get(i));
		}
		speciesBox = new JComboBox<String>(speciesModel);
		speciesBox.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaaaaaaa");
		speciesImportButton.addActionListener(new ImportListener(speciesBox, WMATSQLLogging.SPECIES_CHAR_LENGTH));
		speciesPanel.add(speciesBox, c);
		c.gridx++;
		PamButton speciesRemoveButton = new PamButton("Remove");
		SpeciesRemoveListener speciesRemoveListener = new SpeciesRemoveListener();
		speciesRemoveButton.addActionListener(speciesRemoveListener);
		speciesPanel.add(speciesRemoveButton, c);
		c.gridx++;
		PamButton speciesExportButton = new PamButton("Export");
		speciesExportButton.addActionListener(new ExportListener(speciesBox));
		speciesPanel.add(speciesExportButton, c);
		interfaceTab.add(speciesPanel, b);
		
		b.gridy++;
		JPanel callTypePanel = new JPanel(new GridBagLayout());
		callTypePanel.setBorder(new TitledBorder("'Call type' list"));
		c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		callTypeField = new JTextField();
		callTypeField.setDocument(new JTextFieldLimit(WMATSQLLogging.CALLTYPE_CHAR_LENGTH).getDocument());
		CallTypeAddListener callTypeAddListener = new CallTypeAddListener();
		callTypeField.addActionListener(callTypeAddListener);
		callTypePanel.add(callTypeField, c);
		c.gridx++;
		PamButton callTypeAddButton = new PamButton("Add");
		callTypeAddButton.addActionListener(callTypeAddListener);
		callTypePanel.add(callTypeAddButton, c);
		c.gridx++;
		PamButton callTypeImportButton = new PamButton("Import");
		// Listener added to button after callTypeBox is initialized.
		callTypePanel.add(callTypeImportButton, c);
		c.gridy++;
		c.gridx = 0;
		callTypeModel = new DefaultComboBoxModel<String>();
		for (int i = 1; i < wmatControl.getParams().callTypeList.size(); i++) {
			callTypeModel.addElement(wmatControl.getParams().callTypeList.get(i));
		}
		callTypeBox = new JComboBox<String>(callTypeModel);
		callTypeBox.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaaaaaaa");
		callTypeImportButton.addActionListener(new ImportListener(callTypeBox, WMATSQLLogging.CALLTYPE_CHAR_LENGTH));
		callTypePanel.add(callTypeBox, c);
		c.gridx++;
		PamButton callTypeRemoveButton = new PamButton("Remove");
		CallTypeRemoveListener callTypeRemoveListener = new CallTypeRemoveListener();
		callTypeRemoveButton.addActionListener(callTypeRemoveListener);
		callTypePanel.add(callTypeRemoveButton, c);
		c.gridx++;
		PamButton callTypeExportButton = new PamButton("Export");
		callTypeExportButton.addActionListener(new ExportListener(callTypeBox));
		callTypePanel.add(callTypeExportButton, c);
		interfaceTab.add(callTypePanel, b);
		
		b.gridy++;
		JPanel intervalPanel = new JPanel(new GridBagLayout());
		intervalPanel.setBorder(new TitledBorder("Button parameters"));
		c = new PamGridBagContraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		String intervalText = "Everything within the specified time interval after the start of each file is selected when "
				+ "'Select within start interval' is pressed.";
		String html = "<html><body style='width: %1spx'>%1s";
		intervalPanel.add(new JLabel(String.format(html, 250, intervalText)), c);
		c.gridy++;
		c.gridwidth = 1;
		intervalPanel.add(new JLabel("Interval (in milliseconds): "), c);
		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		intervalSpinner = new JSpinner(new SpinnerNumberModel(wmatControl.getParams().startBuffer,0,null,500));
		// Kudos to Brad: https://stackoverflow.com/questions/8440754/remove-comma-from-jspinner
		intervalSpinner.setEditor(new JSpinner.NumberEditor(intervalSpinner, "#"));
		// Kudos to King Midas: https://stackoverflow.com/questions/7374659/jspinner-increase-length-of-editor-box
		((JSpinner.DefaultEditor) intervalSpinner.getEditor()).getTextField().setColumns(5);
		intervalPanel.add(intervalSpinner, c);
		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		String scrollBufferText = "When 'Scroll to selection on spectrogram' is pressed, the spectrogram scrolls to the time "
				+ "stamp of the selected contour minus the specified buffer length.";
		intervalPanel.add(new JLabel(String.format(html, 250, scrollBufferText)), c);
		c.gridy++;
		c.gridwidth = 1;
		intervalPanel.add(new JLabel("Buffer (in milliseconds): "), c);
		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		scrollBufferSpinner = new JSpinner(new SpinnerNumberModel(wmatControl.getParams().scrollBuffer,0,null,500));
		scrollBufferSpinner.setEditor(new JSpinner.NumberEditor(scrollBufferSpinner, "#"));
		((JSpinner.DefaultEditor) scrollBufferSpinner.getEditor()).getTextField().setColumns(5);
		intervalPanel.add(scrollBufferSpinner, c);
		interfaceTab.add(intervalPanel, b);
		
		b.gridy++;
		JPanel guiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		guiPanel.setBorder(new TitledBorder("GUI parameters"));
		JPanel guiSubpanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.NONE;
		guiSubpanel.add(new JLabel("Table height:"), c);
		c.gridx++;
		tableHeightSpinner = new JSpinner(new SpinnerNumberModel(wmatControl.getParams().tableHeight,1,2000,50));
		tableHeightSpinner.setEditor(new JSpinner.NumberEditor(tableHeightSpinner, "#"));
		((JSpinner.DefaultEditor) tableHeightSpinner.getEditor()).getTextField().setColumns(5);
		guiSubpanel.add(tableHeightSpinner, c);
		guiPanel.add(guiSubpanel);
		interfaceTab.add(guiPanel, b);
		
		tabbedPane.add("Interface", interfaceTab);
		
		JPanel dataTab = new JPanel(new GridBagLayout());
		b = new PamGridBagContraints();
		b.fill = GridBagConstraints.HORIZONTAL;
		b.anchor = GridBagConstraints.WEST;
		
		predictionSourcePanel = new SourcePanel(this, "Live Classifier prediction data source", LCDataUnit.class, false, true);
		JPanel predictionSubpanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.anchor = c.WEST;
		c.fill = c.NONE;
		predictionSubpanel.add(new JLabel("Prediction context:"), c);
		c.gridx++;
		predictionContextBox = new JComboBox<String>(new String[] {"Overall cluster predictions","Individual contour predictions"});
		predictionContextBox.setSelectedIndex(wmatControl.getParams().predictionContext);
		predictionSubpanel.add(predictionContextBox, c);
		predictionSourcePanel.getPanel().add(predictionSubpanel);
		dataTab.add(predictionSourcePanel.getPanel(), b);
		
		b.gridy++;
		slicedataSourcePanel = new SourcePanel(this, "Contour data source (for slice data)", ConnectedRegionDataUnit.class, false, true);
		dataTab.add(slicedataSourcePanel.getPanel(), b);
		//b.gridy++;
		
		b.gridy++;
	/*	tzPanel = new WMATTimeZonePanel(true, true, true, true);
		tzPanel.setAudioTimeZone(wmatControl.getParams().audioTZ);
		tzPanel.setBinaryTimeZone(wmatControl.getParams().binaryTZ);
		tzPanel.setDatabaseTimeZone(wmatControl.getParams().databaseTZ); */
		JPanel tzPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.anchor = c.WEST;
		c.fill = c.HORIZONTAL;
		tzPanel.setBorder(new TitledBorder("Time zone conversion"));
		tzPanel.add(new JLabel("Convert from system time zone to UTC:"), c);
		c.gridy++;
		tzPanel.add(binaryTZCheck = new JCheckBox("Binary file time stamps"), c);
		binaryTZCheck.setSelected(wmatControl.getParams().binaryIsInLocalTime);
		c.gridy++;
		tzPanel.add(databaseTZCheck = new JCheckBox("Database UTC column"), c);
		databaseTZCheck.setSelected(wmatControl.getParams().databaseUTCColumnIsInLocalTime);
		dataTab.add(tzPanel, b);
		
		b.gridy++;
		JPanel sqlPanel = new JPanel(new GridBagLayout());
		sqlPanel.setBorder(new TitledBorder("SQL table name"));
		c = new PamGridBagContraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		String sqlText = "Name of the database table being read from and written into. Table must include all columns from a "
				+ "typical Whistle and Moan Detector database table.\n";
		html = "<html><body style='width: %1spx'>%1s";
		sqlPanel.add(new JLabel(String.format(html, 250, sqlText)), c);
		c.gridy++;
		sqlTableField = new JTextField();
		sqlTableField.setDocument(JSQLFilter(sqlTableField));
		sqlTableField.setText(wmatControl.getParams().sqlTableName);
		sqlPanel.add(sqlTableField, c);
		dataTab.add(sqlPanel, b);
		
		//p.add(BorderLayout.NORTH, mainPanel);
		
		tabbedPane.add("Data", dataTab);
		
		JPanel hotkeysTab = new JPanel(new GridBagLayout());
		b = new PamGridBagContraints();
		b.fill = GridBagConstraints.HORIZONTAL;
		b.anchor = GridBagConstraints.NORTH;
		
		JPanel hotkeysPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.anchor = c.WEST;
		c.ipadx = 5;
		hotkeyQCheck = new JCheckBox("Alt+Q:");
		hotkeyQCheck.setSelected(wmatControl.getParams().hotkeyQEnabled);
		hotkeysPanel.add(hotkeyQCheck, c);
		c.gridx++;
		c.gridwidth = 2;
		hotkeysPanel.add(new JLabel("Select next unlabelled detection"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		hotkeyWCheck = new JCheckBox("Alt+W:");
		hotkeyWCheck.setSelected(wmatControl.getParams().hotkeyWEnabled);
		hotkeysPanel.add(hotkeyWCheck, c);
		c.gridx++;
		c.gridwidth = 2;
		hotkeysPanel.add(new JLabel("Scroll to selection on spectrogram"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		hotkeyECheck = new JCheckBox("Alt+E:");
		hotkeyECheck.setSelected(wmatControl.getParams().hotkeyEEnabled);
		hotkeysPanel.add(hotkeyECheck, c);
		c.gridx++;
		c.gridwidth = 2;
		hotkeysPanel.add(new JLabel("Select all in spectrogram view"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		hotkeyZCheck = new JCheckBox("Ctrl+Z:");
		hotkeyZCheck.setSelected(wmatControl.getParams().hotkeyZEnabled);
		hotkeysPanel.add(hotkeyZCheck, c);
		c.gridx++;
		c.gridwidth = 2;
		hotkeysPanel.add(new JLabel("Undo"), c);
		c.gridy++;
		c.gridx = 1;
		c.gridwidth = 1;
		hotkeysPanel.add(new JLabel("Species"), c);
		c.gridx++;
		hotkeysPanel.add(new JLabel("Call type"), c);
		hotkeyNumChecks = new JCheckBox[10];
		hotkeyNumSpeciesBoxes = new JComboBox[10];
		hotkeyNumCallTypeBoxes = new JComboBox[10];
		for (int i = 1; i <= 10; i++) {
			c.gridy++;
			c.gridx = 0;
			int val = i % 10;
			hotkeyNumChecks[val] = new JCheckBox("Alt+"+String.valueOf(val)+":");
			hotkeyNumChecks[val].setSelected(wmatControl.getParams().hotkeyNumEnabled[val]);
			hotkeysPanel.add(hotkeyNumChecks[val], c);
			c.gridx++;
			hotkeyNumSpeciesBoxes[val] = new JComboBox<String>();
			hotkeysPanel.add(hotkeyNumSpeciesBoxes[val], c);
			c.gridx++;
			hotkeyNumCallTypeBoxes[val] = new JComboBox<String>();
			hotkeysPanel.add(hotkeyNumCallTypeBoxes[val], c);
		}
		updateHotkeySpeciesBoxes(false);
		updateHotkeyCallTypeBoxes(false);
		for (int i = 0; i < hotkeyNumSpeciesBoxes.length; i++) {
			hotkeyNumSpeciesBoxes[i].setSelectedItem(wmatControl.getParams().hotkeyNumLabels[i][0]);
			hotkeyNumCallTypeBoxes[i].setSelectedItem(wmatControl.getParams().hotkeyNumLabels[i][1]);
		}
		hotkeysTab.add(hotkeysPanel, b);
		
		tabbedPane.add("Hotkeys", hotkeysTab);
		
		setDialogComponent(tabbedPane);
		
	}
	
	protected void updateHotkeySpeciesBoxes(boolean keepItem) {
		for (int i = 0; i < hotkeyNumSpeciesBoxes.length; i++) {
			String item = (String) hotkeyNumSpeciesBoxes[i].getSelectedItem();
			hotkeyNumSpeciesBoxes[i].removeAllItems();
			hotkeyNumSpeciesBoxes[i].addItem("<skip>");
			hotkeyNumSpeciesBoxes[i].addItem("<clear>");
			for (int j = 0; j < speciesBox.getItemCount(); j++)
				hotkeyNumSpeciesBoxes[i].addItem(speciesBox.getItemAt(j));
			if (keepItem)
				hotkeyNumSpeciesBoxes[i].setSelectedItem(item);
			if (!keepItem || !hotkeyNumSpeciesBoxes[i].getSelectedItem().equals(item))
				hotkeyNumSpeciesBoxes[i].setSelectedIndex(0);
		}
	}
	
	protected void updateHotkeyCallTypeBoxes(boolean keepItem) {
		for (int i = 0; i < hotkeyNumCallTypeBoxes.length; i++) {
			String item = (String) hotkeyNumCallTypeBoxes[i].getSelectedItem();
			hotkeyNumCallTypeBoxes[i].removeAllItems();
			hotkeyNumCallTypeBoxes[i].addItem("<skip>");
			hotkeyNumCallTypeBoxes[i].addItem("<clear>");
			for (int j = 0; j < callTypeBox.getItemCount(); j++)
				hotkeyNumCallTypeBoxes[i].addItem(callTypeBox.getItemAt(j));
			if (keepItem)
				hotkeyNumCallTypeBoxes[i].setSelectedItem(item);
			if (!keepItem || !hotkeyNumCallTypeBoxes[i].getSelectedItem().equals(item))
				hotkeyNumCallTypeBoxes[i].setSelectedIndex(0);
		}
	}
	
	class SpeciesAddListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (speciesModel.getIndexOf(speciesField.getText()) == -1 && !(speciesField.getText().equals(""))) {
				speciesModel.addElement(speciesField.getText());
			}
			speciesField.setText("");
			updateHotkeySpeciesBoxes(true);
		}
	}
	
	class SpeciesRemoveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (speciesModel.getSize() > 0) {
				speciesModel.removeElement(speciesModel.getSelectedItem());
			}
			updateHotkeySpeciesBoxes(true);
		}
	}
	
	class CallTypeAddListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (callTypeModel.getIndexOf(callTypeField.getText()) == -1 && !(callTypeField.getText().equals(""))) {
				callTypeModel.addElement(callTypeField.getText());
			}
			callTypeField.setText("");
			updateHotkeyCallTypeBoxes(true);
		}
	}
	
	class CallTypeRemoveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (callTypeModel.getSize() > 0) {
				callTypeModel.removeElement(callTypeModel.getSelectedItem());
			}
			updateHotkeyCallTypeBoxes(true);
		}
	}
	
	class ImportListener implements ActionListener {
		
		JComboBox<String> inputBox;
		int maxStringLength;
		
		public ImportListener(JComboBox<String> inputBox, int maxStringLength) {
			this.inputBox = inputBox;
			this.maxStringLength = maxStringLength;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
			int returnVal = fc.showOpenDialog(parentFrame);
			if (returnVal != JFileChooser.APPROVE_OPTION)
				return;
			File f = fc.getSelectedFile();
			ArrayList<String> outp = new ArrayList<String>();
			ArrayList<String> unusables = new ArrayList<String>();
			//outp.add("");
			Scanner sc;
			try {
				sc = new Scanner(f);
				while (sc.hasNextLine()) {
					String next = sc.nextLine();
					if (outp.contains(next) || next.replace(" ", "").length() == 0)
						continue;
					if ((next.length() > maxStringLength || next.contains(",")) && !unusables.contains(next))
						unusables.add(next);
					else outp.add(next);
				}
				sc.close();
			} catch (Exception e2) {
				e2.printStackTrace();
				wmatControl.simpleErrorDialog("Error occurred while reading selected file - see console for details.");
				return;
			}
			if (unusables.size() > 0) {
				String message = "The following labels cannot be added to the list, due to either containing a comma or being over "
						+ String.valueOf(maxStringLength) + " characters long:\n\n";
				for (int i = 0; i < unusables.size(); i++)
					message += unusables.get(i)+"\n";
				message += "\nWould you still like to use everything else that was found in the file?";
				returnVal = JOptionPane.showConfirmDialog(parentFrame,
						MIRFEEControlledUnit.makeHTML(message, 300),
						wmatControl.getUnitName(),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (returnVal != JOptionPane.YES_OPTION)
					return;
			}
			inputBox.removeAllItems();;
			for (int i = 0; i < outp.size(); i++)
				inputBox.addItem(outp.get(i));
			
			updateHotkeySpeciesBoxes(true);
			updateHotkeyCallTypeBoxes(true);
		}
	}
	
	class ExportListener implements ActionListener {
		
		JComboBox<String> inputBox;
		
		public ExportListener(JComboBox<String> inputBox) {
			this.inputBox = inputBox;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
			int returnVal = fc.showSaveDialog(parentFrame);
			if (returnVal == JFileChooser.CANCEL_OPTION)
				return;
			File f = WMATPanel.getSelectedFileWithExtension(fc);
			f.setWritable(true, false);
			if (f.exists()) {
				int res = JOptionPane.showConfirmDialog(parentFrame,
						"Overwrite selected file?",
						wmatControl.getUnitName(),
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.YES_OPTION)
					f.setExecutable(true);
				else return;
			} else {
				try {
					f.createNewFile();
				} catch (Exception e2) {
					e2.printStackTrace();
					wmatControl.simpleErrorDialog("Could not create new file.\nSee console for details.");
					return;
				}
			}
			try {
				PrintWriter pw = new PrintWriter(f);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < inputBox.getItemCount(); i++) {
					sb.append(inputBox.getItemAt(i));
					if (i < inputBox.getItemCount() - 1)
						sb.append("\n");
				}
				pw.write(sb.toString());
				pw.flush();
				pw.close();
				JOptionPane.showMessageDialog(parentFrame,
						"List successfully written to file.",
						wmatControl.getUnitName(),
						JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e2) {
				System.out.println(e2);
				wmatControl.simpleErrorDialog();
			}
		}
	}
	
	/**
	 * Sets a limit to the number of characters allowed in a JTextField.
	 * Used with JTextField.setDocument(new JTextFieldLimit(int limit).getDocument());
	 * Copied from https://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
	 * Author page: https://stackoverflow.com/users/1866109/francisco-j-g%c3%bcemes-sevilla
	 * (Also modified to filter out commas.)
	 */
	public class JTextFieldLimit extends JTextField {
	    private int limit;

	    public JTextFieldLimit(int limit) {
	        super();
	        this.limit = limit;
	    }
	    @Override
	    protected Document createDefaultModel() {
	        return new LimitDocument();
	    }
	    private class LimitDocument extends PlainDocument {

	        @Override
	        public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
	            if (str == null) return;
	            if (str.length() == 1 && str.charAt(0) == ',') return;
	            if ((getLength() + str.length()) <= limit) super.insertString(offset, str, attr);
	        }       
	    }
	}
	
	/**
	 * Limits entry in text field to numbers only.
	 * @return PlainDocument
	 */
	public PlainDocument JNumFilter() {
		PlainDocument d = new PlainDocument() {
			@Override
	        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
	            char c = str.charAt(0);
	            if (c >= '0' && c <= '9') {
	            	super.insertString(offs, str, a);
		        }
	        }
		};
		return d;
	}
	
	/**
	 * Limits entry in text field to miniscule letters, digits and underscores.
	 * Capital letters are automatically un-capitalized, and digits are blocked from being the first character.
	 * @return PlainDocument
	 */
	public PlainDocument JSQLFilter(JTextField field) {
		PlainDocument d = new PlainDocument() {
			@Override
	        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				if (str.length() > 1) {
					super.insertString(offs, str, a);
					return;
				}
	            char c = str.charAt(0);
	            if (c >= 'A' && c <= 'Z') {
	            	c += 32;
	            } else if (!((c >= '0' && c <= '9' && field.getText().length() > 0)
	            		    || (c >= 'a' && c <= 'z')
	            		    || c == '_')) {
	            	return;
		        }
	            super.insertString(offs, String.valueOf(c), a);
	        }
		};
		return d;
	}
	
	@Override
	public boolean getParams() {
		WMATParameters newParams = new WMATParameters();
		if (sqlTableField.getText().length() == 0) {
			wmatControl.simpleErrorDialog("SQL table must have a name.");
			return false;
		}
		
		if (startup || !sqlTableField.getText().equals(wmatControl.getParams().sqlTableName)) {
			if (!startup) {
				int result = JOptionPane.showConfirmDialog(parentFrame,
						"The SQL table name has been changed, therefore the database should be manually re-connected to."
						+ "\n\nProceed with settings changes?",
						wmatControl.getUnitName(),
						JOptionPane.OK_CANCEL_OPTION);
				if (result != JOptionPane.OK_OPTION) return false;
			}
			newParams.sqlTableName = sqlTableField.getText();
		}
		
		if (binaryTZCheck.isSelected() != wmatControl.getParams().binaryIsInLocalTime ||
			databaseTZCheck.isSelected() != wmatControl.getParams().databaseUTCColumnIsInLocalTime) {
			if (!startup) {
				int result = JOptionPane.showConfirmDialog(parentFrame,
						"Time zones have been changed, therefore the binary data should be manually re-loaded."
						+ "\n\nProceed with settings changes?",
						wmatControl.getUnitName(),
						JOptionPane.OK_CANCEL_OPTION);
				if (result != JOptionPane.OK_OPTION) return false;
			}
			newParams.binaryIsInLocalTime = binaryTZCheck.isSelected();
			newParams.databaseUTCColumnIsInLocalTime = databaseTZCheck.isSelected();
		}
		
		newParams.speciesList = new ArrayList<String>();
		newParams.speciesList.add("");
		wmatControl.getSidePanel().getWMATPanel().speciesModel = new DefaultComboBoxModel<String>();
		wmatControl.getSidePanel().getWMATPanel().speciesModel.addElement("");
		for (int i = 0; i < speciesModel.getSize(); i++) {
			newParams.speciesList.add(speciesModel.getElementAt(i));
			wmatControl.getSidePanel().getWMATPanel().speciesModel.addElement(speciesModel.getElementAt(i));
		}
		wmatControl.getSidePanel().getWMATPanel().speciesBox.setModel(wmatControl.getSidePanel().getWMATPanel().speciesModel);
		
		newParams.callTypeList = new ArrayList<String>();
		newParams.callTypeList.add("");
		wmatControl.getSidePanel().getWMATPanel().calltypeModel = new DefaultComboBoxModel<String>();
		wmatControl.getSidePanel().getWMATPanel().calltypeModel.addElement("");
		for (int i = 0; i < callTypeModel.getSize(); i++) {
			newParams.callTypeList.add(callTypeModel.getElementAt(i));
			wmatControl.getSidePanel().getWMATPanel().calltypeModel.addElement(callTypeModel.getElementAt(i));
		}
		wmatControl.getSidePanel().getWMATPanel().calltypeBox.setModel(wmatControl.getSidePanel().getWMATPanel().calltypeModel);
		
		//if (intervalField.getText().length() > 0)
		//	newParams.startBuffer = Integer.valueOf(intervalField.getText());
		//if (scrollBufferField.getText().length() > 0)
		//	newParams.scrollBuffer = Integer.valueOf(scrollBufferField.getText());
		newParams.startBuffer = (int) intervalSpinner.getValue();
		newParams.scrollBuffer = (int) scrollBufferSpinner.getValue();
		newParams.tableHeight = (int) tableHeightSpinner.getValue();
		
		newParams.predictionSourceName = predictionSourcePanel.getSourceName();
		newParams.predictionContext = predictionContextBox.getSelectedIndex();
		newParams.slicedataSourceName = slicedataSourcePanel.getSourceName();
		newParams.hotkeyQEnabled = hotkeyQCheck.isSelected();
		newParams.hotkeyWEnabled = hotkeyWCheck.isSelected();
		newParams.hotkeyEEnabled = hotkeyECheck.isSelected();
		newParams.hotkeyZEnabled = hotkeyZCheck.isSelected();
		for (int i = 0; i < hotkeyNumChecks.length; i++) {
			newParams.hotkeyNumEnabled[i] = hotkeyNumChecks[i].isSelected();
			newParams.hotkeyNumLabels[i][0] = (String) hotkeyNumSpeciesBoxes[i].getSelectedItem();
			newParams.hotkeyNumLabels[i][1] = (String) hotkeyNumCallTypeBoxes[i].getSelectedItem();
		}
		
		wmatControl.setParams(newParams);
		int prefWidth = wmatControl.getSidePanel().getWMATPanel().tableScroller.getPreferredSize().width;
		wmatControl.getSidePanel().getWMATPanel().tableScroller.setPreferredSize(new Dimension(prefWidth, newParams.tableHeight));
		wmatControl.getSidePanel().getWMATPanel().updateLCPredictions();
		wmatControl.getSidePanel().getWMATPanel().updateHotkeys();
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {
		// (Confirm dialog apparently comes built-in.)
		// NOT meant to immediately replace the settings, just setting the values in the boxes to default.
		WMATParameters newParams = new WMATParameters();
		speciesModel.removeAllElements();
		for (int i = 1; i < newParams.speciesList.size(); i++) speciesModel.addElement(newParams.speciesList.get(i));
		callTypeModel.removeAllElements();
		for (int i = 1; i < newParams.callTypeList.size(); i++) callTypeModel.addElement(newParams.callTypeList.get(i));
		intervalSpinner.setValue(newParams.startBuffer);
		scrollBufferSpinner.setValue(newParams.scrollBuffer);
		tableHeightSpinner.setValue(newParams.tableHeight);
		predictionContextBox.setSelectedIndex(0);
		binaryTZCheck.setSelected(newParams.binaryIsInLocalTime);
		databaseTZCheck.setSelected(newParams.databaseUTCColumnIsInLocalTime);
		sqlTableField.setText(newParams.sqlTableName);
		//requireCtrlCheck.setSelected(newParams.hotkeyCtrlRequired);
		hotkeyQCheck.setSelected(newParams.hotkeyQEnabled);
		hotkeyWCheck.setSelected(newParams.hotkeyWEnabled);
		hotkeyECheck.setSelected(newParams.hotkeyEEnabled);
		hotkeyZCheck.setSelected(newParams.hotkeyZEnabled);
		for (int i = 0; i < hotkeyNumChecks.length; i++) {
			hotkeyNumChecks[i].setSelected(newParams.hotkeyNumEnabled[i]);
		}
		updateHotkeySpeciesBoxes(false);
		updateHotkeySpeciesBoxes(false);
	}
}