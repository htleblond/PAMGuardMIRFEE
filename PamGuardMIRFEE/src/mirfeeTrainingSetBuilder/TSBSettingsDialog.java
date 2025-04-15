package mirfeeTrainingSetBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import java.util.*; //

import javax.swing.border.TitledBorder;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import PamView.dialog.PamDialog;

/**
 * The Training Set Builder's settings dialog.
 * @author Holly LeBlond
 */
public class TSBSettingsDialog extends PamDialog {
	
	TSBControl tsbControl;
	private Window parentFrame;
	protected PamPanel mainPanel;
	
	protected ButtonGroup yesOrNoCallType;
	protected JRadioButton noCallType;
	protected JRadioButton yesCallType;
	
	protected JCheckBox belowFreqCheck;
	protected JTextField belowFreqField;
	protected JCheckBox aboveFreqCheck;
	protected JTextField aboveFreqField;
	protected JCheckBox belowDurCheck;
	protected JTextField belowDurField;
	protected JCheckBox aboveDurCheck;
	protected JTextField aboveDurField;
	
	protected JComboBox<String> overlapOptionsBox;
	protected JComboBox<String> multilabelOptionsBox;
	
	protected JButton umbrellaButton;
	protected ArrayList<String> classList;
	protected ArrayList<String> umbrellaList;
	protected HashMap<String, String> classMap;
	protected ArrayList<JLabel> jLabelList;
	protected ArrayList<JComboBox<String>> jComboBoxList;
	
	public TSBSettingsDialog(Window parentFrame, TSBControl tsbControl) {
		super(parentFrame, "MIRFEE Training Set Builder", false);
		
		this.tsbControl = tsbControl;
		this.parentFrame = parentFrame;
		
		classList = tsbControl.getFullClassList();
		umbrellaList = tsbControl.getUmbrellaClassList();
		classMap = tsbControl.getClassMap();
		
		mainPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		
		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		topPanel.setBorder(new TitledBorder("Class label settings"));
		c.anchor = c.WEST;
		noCallType = new JRadioButton();
		noCallType.setText("Only use species as label");
		topPanel.add(noCallType, c);
		c.gridy++;
		yesCallType = new JRadioButton();
		yesCallType.setText("Include call type in label with species (TBA)");
		yesCallType.setEnabled(false);
		topPanel.add(yesCallType, c);
		yesOrNoCallType = new ButtonGroup();
		yesOrNoCallType.add(noCallType);
		yesOrNoCallType.add(yesCallType);
		b.fill = b.HORIZONTAL;
		mainPanel.add(topPanel, b);
		
		JPanel skipPanel = new JPanel(new GridBagLayout());
		skipPanel.setBorder(new TitledBorder("Skips"));
		skipPanel.setAlignmentX(LEFT_ALIGNMENT);
		c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		skipPanel.add(new JLabel("Skip all contours that...", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridwidth = 1;
		belowFreqCheck = new JCheckBox();
		belowFreqCheck.addActionListener(new CheckBoxListener(belowFreqCheck));
		skipPanel.add(belowFreqCheck, c);
		c.gridx++;
		skipPanel.add(new JLabel("...are of a frequency lower than: ", SwingConstants.LEFT), c);
		c.gridx++;
		belowFreqField = new JTextField(5);
		belowFreqField.setDocument(JIntFilter());
		skipPanel.add(belowFreqField, c);
		c.gridx++;
		skipPanel.add(new JLabel("Hz", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx = 0;
		aboveFreqCheck = new JCheckBox();
		aboveFreqCheck.addActionListener(new CheckBoxListener(aboveFreqCheck));
		skipPanel.add(aboveFreqCheck, c);
		c.gridx++;
		skipPanel.add(new JLabel("...are of a frequency higher than: ", SwingConstants.LEFT), c);
		c.gridx++;
		aboveFreqField = new JTextField(5);
		aboveFreqField.setDocument(JIntFilter());
		skipPanel.add(aboveFreqField, c);
		c.gridx++;
		skipPanel.add(new JLabel("Hz", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx = 0;
		belowDurCheck = new JCheckBox();
		belowDurCheck.addActionListener(new CheckBoxListener(belowDurCheck));
		skipPanel.add(belowDurCheck, c);
		c.gridx++;
		skipPanel.add(new JLabel("...are shorter than: ", SwingConstants.LEFT), c);
		c.gridx++;
		belowDurField = new JTextField(5);
		belowDurField.setDocument(JIntFilter());
		skipPanel.add(belowDurField, c);
		c.gridx++;
		skipPanel.add(new JLabel("ms", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx = 0;
		aboveDurCheck = new JCheckBox();
		aboveDurCheck.addActionListener(new CheckBoxListener(aboveDurCheck));
		skipPanel.add(aboveDurCheck, c);
		c.gridx++;
		skipPanel.add(new JLabel("...are longer than: ", SwingConstants.LEFT), c);
		c.gridx++;
		aboveDurField = new JTextField(5);
		aboveDurField.setDocument(JIntFilter());
		skipPanel.add(aboveDurField, c);
		c.gridx++;
		skipPanel.add(new JLabel("ms", SwingConstants.LEFT), c);
		b.gridy++;
		mainPanel.add(skipPanel, b);
		
		JPanel middlePanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		middlePanel.setBorder(new TitledBorder("Overlaps"));
		c.anchor = c.WEST;
		c.gridwidth = 1;
		middlePanel.add(new JLabel("Instances when contours with different species overlap:"), c);
		c.gridy++;
		overlapOptionsBox = new JComboBox<String>(new String[] {"Skip both", "Keep both"});
		middlePanel.add(overlapOptionsBox, c);
		c.gridy++;
		middlePanel.add(new JLabel("Instances when multiple species occur in the same cluster:"), c);
		c.gridy++;
		multilabelOptionsBox = new JComboBox(new String[] {"Only keep most-occuring species", "Keep everything", "Skip entire cluster"});
		middlePanel.add(multilabelOptionsBox, c);
		b.gridy++;
		mainPanel.add(middlePanel, b);
		
		
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		bottomPanel.setBorder(new TitledBorder("Umbrella classes"));
		c.anchor = c.CENTER;
		c.gridwidth = 2;
		bottomPanel.add(new JLabel("* - Class labels are added by adding new subsets to the main table."), c);
		c.gridy++;
		umbrellaButton = new JButton("Add new umbrella class");
		umbrellaButton.addActionListener(new UmbrellaListener());
		bottomPanel.add(umbrellaButton, c);
		c.gridwidth = 1;
		jLabelList = new ArrayList<JLabel>();
		jComboBoxList = new ArrayList<JComboBox<String>>();
		for (int i = 0; i < classList.size(); i++) {
			c.gridy++;
			c.gridx = 0;
			jLabelList.add(new JLabel(classList.get(i)));
			bottomPanel.add(jLabelList.get(i), c);
			c.gridx++;
			JComboBox<String> newBox = new JComboBox<String>();
			newBox.addItem(classList.get(i));
			for (int j = 0; j < umbrellaList.size(); j++)
				newBox.addItem(umbrellaList.get(j));
			newBox.setSelectedItem(classMap.get(classList.get(i)));
			jComboBoxList.add(newBox);
			bottomPanel.add(jComboBoxList.get(i), c);
		}
		b.gridy++;
		mainPanel.add(bottomPanel, b);
		
		actuallyGetParams();
		
		setDialogComponent(mainPanel);
	}
	
	/*	// Kudos to this: https://stackoverflow.com/questions/1842223/java-linebreaks-in-jlabels
	public String makeHTML(String inp, int width) {
		String outp = "<html><p style=\"width:"+Integer.toString(width)+"px\">"+inp+"</p></html>";
		return outp;
	} */
	
	/**
	 * ActionListener for checkboxes.
	 */
	protected class CheckBoxListener implements ActionListener {
		private JCheckBox box;
		public CheckBoxListener(JCheckBox box) {
			this.box = box;
		}
		public void actionPerformed(ActionEvent e) {
			switchOn(box, box.isSelected());
		}
	}
	
	/**
	 * Enables or disables certain features depending on the input component.
	 */
	protected void switchOn(Object box, boolean boo) {
		if (box.equals(belowFreqCheck)) {
			belowFreqField.setEnabled(boo);
		} else if (box.equals(aboveFreqCheck)) {
			aboveFreqField.setEnabled(boo);
		} else if (box.equals(belowDurCheck)) {
			belowDurField.setEnabled(boo);
		} else if (box.equals(aboveDurCheck)) {
			aboveDurField.setEnabled(boo);
		}
	}
	
	/**
	 * Limits entry in text field to numbers only.
	 */
	public PlainDocument JIntFilter() {
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
	 * Brings up dialog for adding "umbrella classes" to the list.
	 */
	protected class UmbrellaListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String newClass = JOptionPane.showInputDialog("Enter new umbrella class:");
			if (newClass == null) return;
			if (umbrellaList.contains(newClass)) {
				tsbControl.simpleErrorDialog("Input umbrella class already exists", 250);
				return;
			}
			umbrellaList.add(newClass);
			for (int i = 0; i < jComboBoxList.size(); i++)
				jComboBoxList.get(i).addItem(newClass);
		}
	}
	
	/**
	 * Fills components with values from TSBParameters.
	 */
	public void actuallyGetParams() {
		TSBParameters params = tsbControl.getParams();
		if (params.includeCallType)
			yesOrNoCallType.setSelected(yesCallType.getModel(), true);
		else
			yesOrNoCallType.setSelected(noCallType.getModel(), true);
		overlapOptionsBox.setSelectedIndex(params.overlapOption);
		multilabelOptionsBox.setSelectedIndex(params.multilabelOption);
		belowFreqCheck.setSelected(params.skipLowFreqChecked);
		belowFreqField.setEnabled(params.skipLowFreqChecked);
		belowFreqField.setText(String.valueOf(params.skipLowFreq));
		aboveFreqCheck.setSelected(params.skipHighFreqChecked);
		aboveFreqField.setEnabled(params.skipHighFreqChecked);
		aboveFreqField.setText(String.valueOf(params.skipHighFreq));
		belowDurCheck.setSelected(params.skipShortDurChecked);
		belowDurField.setEnabled(params.skipShortDurChecked);
		belowDurField.setText(String.valueOf(params.skipShortDur));
		aboveDurCheck.setSelected(params.skipLongDurChecked);
		aboveDurField.setEnabled(params.skipLongDurChecked);
		aboveDurField.setText(String.valueOf(params.skipLongDur));
	}

	@Override
	public boolean getParams() {
		TSBParameters newParams = tsbControl.getParams().clone();
		try {
			if (newParams.skipLowFreqChecked = belowFreqCheck.isSelected())
				newParams.skipLowFreq = Integer.valueOf(belowFreqField.getText());
			if (newParams.skipHighFreqChecked = aboveFreqCheck.isSelected())
				newParams.skipHighFreq = Integer.valueOf(aboveFreqField.getText());
			if (newParams.skipShortDurChecked = belowDurCheck.isSelected())
				newParams.skipShortDur = Integer.valueOf(belowDurField.getText());
			if (newParams.skipLongDurChecked = aboveDurCheck.isSelected())
				newParams.skipLongDur = Integer.valueOf(aboveDurField.getText());
			newParams.includeCallType = yesCallType.isSelected();
			newParams.overlapOption = overlapOptionsBox.getSelectedIndex();
			newParams.multilabelOption = multilabelOptionsBox.getSelectedIndex();
		} catch (Exception e) {
			e.printStackTrace();
			tsbControl.simpleErrorDialog("An error occurred - see console for details.", 200);
			return false;
		}
		tsbControl.setParams(newParams);
		tsbControl.setUmbrellaClassList(umbrellaList);
		HashMap<String, String> outpMap = new HashMap<String, String>();
		for (int i = 0; i < jLabelList.size(); i++)
			outpMap.put(jLabelList.get(i).getText(), (String) jComboBoxList.get(i).getSelectedItem());
		tsbControl.setClassMap(outpMap);
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {}
}


