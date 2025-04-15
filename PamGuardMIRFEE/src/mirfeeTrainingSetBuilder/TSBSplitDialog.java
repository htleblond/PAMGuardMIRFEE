package mirfeeTrainingSetBuilder;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimeZone;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;

/**
 * Dialog for splitting subsets in two by date.
 * @author Holly LeBlond
 */
public class TSBSplitDialog extends PamDialog {
	
	public TSBControl tsbControl;
	private Window parentFrame;
	
	protected PamPanel mainPanel;

	protected JComboBox<String> startOrEndBox;
	protected JTextField yearField;
	protected JTextField monthField;
	protected JTextField dayField;
	protected JTextField hourField;
	protected JTextField minuteField;
	protected JTextField secondField;
	protected JTextField msField;
	protected JButton updateButton;
	
	protected JTextField oldIDDigit1Field;
	protected JTextField oldIDDigit2Field;
	protected JTextField oldLocationField;
	protected JTextField oldStartField;
	protected JTextField oldEndField;
	protected JTextField oldTotalField;
	protected JComboBox<String> newIDDigit1Box;
	protected JComboBox<String> newIDDigit2Box;
	protected JTextField newLocationField;
	protected JTextField newStartField;
	protected JTextField newEndField;
	protected JTextField newTotalField;
	
	protected TSBSubset currSubset;
	protected ArrayList<TSBDetection> currDetectionList;
	
	public TSBSplitDialog(Window parentFrame, TSBControl tsbControl) {
		super(parentFrame, "MIRFEE Training Set Builder", false);
		this.tsbControl = tsbControl;
		this.parentFrame = parentFrame;
		
		if (tsbControl.getTabPanel().getPanel().getSubsetTable().getSelectedRow() < 0) {
			assert false;
			return;
		}
		String currID = (String) tsbControl.getTabPanel().getPanel().getSubsetTable().getValueAt(
				tsbControl.getTabPanel().getPanel().getSubsetTable().getSelectedRow(), 0);
		currSubset = null;
		for (int i = 0; i < tsbControl.getSubsetList().size(); i++) {
			if (tsbControl.getSubsetList().get(i).id.equals(currID)) {
				currSubset = tsbControl.getSubsetList().get(i);
				break;
			}
		}
		if (currSubset == null) {
			assert false;
			return;
		}
		
		mainPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		mainPanel.setBorder(new TitledBorder(""));
		
		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		topPanel.setBorder(new TitledBorder(""));
		topPanel.add(new JLabel("New subset "), c);
		c.gridx++;
		startOrEndBox = new JComboBox<String>(new String[] {"starts", "ends"});
		// ADD LISTENER
		topPanel.add(startOrEndBox, c);
		c.gridx++;
		topPanel.add(new JLabel(" at "), c);
		c.gridx++;
		yearField = new JTextField(4);
		yearField.setHorizontalAlignment(SwingConstants.RIGHT);
		yearField.setDocument(new JTextFieldLimit(4).getDocument());
		// ADD LISTENER
		topPanel.add(yearField, c);
		c.gridx++;
		topPanel.add(new JLabel("-"), c);
		c.gridx++;
		monthField = new JTextField(2);
		monthField.setHorizontalAlignment(SwingConstants.RIGHT);
		monthField.setDocument(new JTextFieldLimit(2).getDocument());
		// ADD LISTENER
		topPanel.add(monthField, c);
		c.gridx++;
		topPanel.add(new JLabel("-"), c);
		c.gridx++;
		dayField = new JTextField(2);
		dayField.setHorizontalAlignment(SwingConstants.RIGHT);
		dayField.setDocument(new JTextFieldLimit(2).getDocument());
		// ADD LISTENER
		topPanel.add(dayField, c);
		c.gridx++;
		topPanel.add(new JLabel(" "), c);
		c.gridx++;
		hourField = new JTextField(2);
		hourField.setHorizontalAlignment(SwingConstants.RIGHT);
		hourField.setDocument(new JTextFieldLimit(2).getDocument());
		// ADD LISTENER
		topPanel.add(hourField, c);
		c.gridx++;
		topPanel.add(new JLabel(":"), c);
		c.gridx++;
		minuteField = new JTextField(2);
		minuteField.setHorizontalAlignment(SwingConstants.RIGHT);
		minuteField.setDocument(new JTextFieldLimit(2).getDocument());
		// ADD LISTENER
		topPanel.add(minuteField, c);
		c.gridx++;
		topPanel.add(new JLabel(":"), c);
		c.gridx++;
		secondField = new JTextField(2);
		secondField.setHorizontalAlignment(SwingConstants.RIGHT);
		secondField.setDocument(new JTextFieldLimit(2).getDocument());
		// ADD LISTENER
		topPanel.add(secondField, c);
		c.gridx++;
		topPanel.add(new JLabel("+"), c);
		c.gridx++;
		msField = new JTextField(3);
		msField.setHorizontalAlignment(SwingConstants.RIGHT);
		msField.setDocument(new JTextFieldLimit(3).getDocument());
		// ADD LISTENER
		topPanel.add(msField, c);
		c.gridx++;
		topPanel.add(new JLabel(" "), c);
		c.gridx++;
		updateButton = new JButton("Update");
		updateButton.addActionListener(new UpdateButtonListener());
		topPanel.add(updateButton, c);
		b.fill = b.HORIZONTAL;
		b.anchor = b.CENTER;
		mainPanel.add(topPanel, b);
		
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		bottomPanel.setBorder(new TitledBorder(""));
		c.gridx = 1;
		c.anchor = c.WEST;
		bottomPanel.add(new JLabel("ID"), c);
		c.gridx = 3;
		bottomPanel.add(new JLabel("Location"), c);
		c.gridx++;
		bottomPanel.add(new JLabel("Start"), c);
		c.gridx++;
		bottomPanel.add(new JLabel("End"), c);
		c.gridx++;
		bottomPanel.add(new JLabel("Total"), c);
		c.gridy++;
		c.gridx = 0;
		c.anchor = c.EAST;
		bottomPanel.add(new JLabel("Old:"), c);
		c.gridx++;
		c.anchor = c.CENTER;
		c.fill = c.HORIZONTAL;
		oldIDDigit1Field = new JTextField(2);
		oldIDDigit1Field.setEnabled(false);
		bottomPanel.add(oldIDDigit1Field, c);
		c.gridx++;
		oldIDDigit2Field = new JTextField(2);
		oldIDDigit2Field.setEnabled(false);
		bottomPanel.add(oldIDDigit2Field, c);
		c.gridx++;
		oldLocationField = new JTextField(14);
		oldLocationField.setEnabled(false);
		bottomPanel.add(oldLocationField, c);
		c.gridx++;
		oldStartField = new JTextField(14);
		oldStartField.setEnabled(false);
		bottomPanel.add(oldStartField, c);
		c.gridx++;
		oldEndField = new JTextField(14);
		oldEndField.setEnabled(false);
		bottomPanel.add(oldEndField, c);
		c.gridx++;
		oldTotalField = new JTextField(5);
		oldTotalField.setEnabled(false);
		bottomPanel.add(oldTotalField, c);
		c.gridy++;
		c.gridx = 0;
		c.anchor = c.EAST;
		c.fill = c.NONE;
		bottomPanel.add(new JLabel("New:"), c);
		c.gridx++;
		c.anchor = c.CENTER;
		c.fill = c.HORIZONTAL;
		String[] box1Digits = new String[] {"1","2","3","4","5","6","7","8","9","0",
				"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
		newIDDigit1Box = new JComboBox<String>(box1Digits);
		bottomPanel.add(newIDDigit1Box, c);
		c.gridx++;
		String[] box2Digits = new String[] {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
				"1","2","3","4","5","6","7","8","9","0"};
		newIDDigit2Box = new JComboBox<String>(box2Digits);
		bottomPanel.add(newIDDigit2Box, c);
		c.gridx++;
		newLocationField = new JTextField(14);
		bottomPanel.add(newLocationField, c);
		c.gridx++;
		newStartField = new JTextField(14);
		newStartField.setEnabled(false);
		bottomPanel.add(newStartField, c);
		c.gridx++;
		newEndField = new JTextField(14);
		newEndField.setEnabled(false);
		bottomPanel.add(newEndField, c);
		c.gridx++;
		newTotalField = new JTextField(5);
		newTotalField.setEnabled(false);
		bottomPanel.add(newTotalField, c);
		b.gridy++;
		mainPanel.add(bottomPanel, b);
		
		setDialogComponent(mainPanel);
		
		currDetectionList = new ArrayList<TSBDetection>();
		ArrayList<String> selectedLabels = new ArrayList<String>();
		for (int i = 0; i < currSubset.selectionArray.length; i++) {
			selectedLabels.add(currSubset.classList.get(currSubset.selectionArray[i]));
			//System.out.println(currSubset.classList.get(currSubset.selectionArray[i]));
		}
		for (int i = 0; i < currSubset.validEntriesList.size(); i++) {
			for (int j = 0; j < currSubset.validEntriesList.get(i).size(); j++) {
				TSBDetection currDetection = currSubset.validEntriesList.get(i).get(j);
				if (selectedLabels.contains(currDetection.species)) {
					currDetectionList.add(currDetection);
				}
			}
		}
		//currList.sort(Comparator.comparing(a -> a[2]));
		Collections.sort(currDetectionList, Comparator.comparingLong(TSBDetection::getDateTimeAsLong));
		
		oldIDDigit1Field.setText(currID.substring(0, 1));
		oldIDDigit2Field.setText(currID.substring(1));
		oldLocationField.setText(currSubset.location);
		newLocationField.setText(currSubset.location);
		oldStartField.setText(currSubset.start);
		String halfwayDate = "";
		for (int i = 0; i < currDetectionList.size(); i++) {
			//System.out.println(String.valueOf(i)+" -> "+String.valueOf(currList.size()/2));
			if (i >= currDetectionList.size()/2) {
				TSBDetection currDetection = currDetectionList.get(i);
				halfwayDate = currDetection.datetime.substring(0,19)+"+000";
				break;
			}
		}
		yearField.setText(halfwayDate.substring(0,4));
		monthField.setText(halfwayDate.substring(5,7));
		dayField.setText(halfwayDate.substring(8,10));
		hourField.setText(halfwayDate.substring(11,13));
		minuteField.setText(halfwayDate.substring(14,16));
		secondField.setText(halfwayDate.substring(17,19));
		msField.setText("000");
		int halfwayPoint = 0;
		for (int i = 0; i < currDetectionList.size(); i++) {
			//System.out.println(currList.get(i)[2]+" -> "+halfwayDate);
			if (currDetectionList.get(i).datetime.compareTo(halfwayDate) > 0) {
				halfwayPoint = i;
				break;
			}
		}
		oldEndField.setText(currDetectionList.get(halfwayPoint-1).datetime);
		if (halfwayPoint < currDetectionList.size()-1) {
			newStartField.setText(currDetectionList.get(halfwayPoint).datetime);
			newEndField.setText(currDetectionList.get(currDetectionList.size()-1).datetime);
		}
		oldTotalField.setText(String.valueOf(halfwayPoint));
		newTotalField.setText(String.valueOf(currDetectionList.size()-(halfwayPoint)));
		ArrayList<String> newIDList = new ArrayList<String>();
		String newID = currID.substring(0,1)+"A";
		boolean foundFreeID = true;
		for (int i = 0; i < box2Digits.length; i++) {
			if (box2Digits[i].equals(currID.substring(1))) {
				for (int j = i+1; j != i; j++) {
					String testID = currID.substring(0,1)+box2Digits[j];
					for (int k = 0; k < tsbControl.getSubsetList().size(); k++) {
						if (tsbControl.getSubsetList().get(k).id.equals(testID)) {
							foundFreeID = false;
							break;
						}
					}
					if (foundFreeID) {
						newID = testID;
						break;
					}
					if (j == box2Digits.length-1) {
						j = -1;
					}
				}
			}
		}
		newIDDigit1Box.setSelectedItem(newID.substring(0,1));
		newIDDigit2Box.setSelectedItem(newID.substring(1));
	}
	
	/**
	 * Updates the dates and detection count for both new subsets to match the new input date/time.
	 */
	protected class UpdateButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			String newDate = "";
			try {
				newDate = String.format("%04d", Integer.valueOf(yearField.getText()));
				newDate += "-"+String.format("%02d", Integer.valueOf(monthField.getText()));
				newDate += "-"+String.format("%02d", Integer.valueOf(dayField.getText()));
				newDate += " "+String.format("%02d", Integer.valueOf(hourField.getText()));
				newDate += ":"+String.format("%02d", Integer.valueOf(minuteField.getText()));
				newDate += ":"+String.format("%02d", Integer.valueOf(secondField.getText()));
				newDate += "+"+String.format("%03d", Integer.valueOf(msField.getText()));
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				df.parse(newDate);
			} catch (Exception e1) {
				tsbControl.simpleErrorDialog("Invalid date/time entered.", 250);
				return;
			}
			int halfwayIndex = currDetectionList.size();
			for (int i = 0; i < currDetectionList.size(); i++) {
				if (newDate.compareTo(currDetectionList.get(i).datetime) < 0) {
					halfwayIndex = i;
					break;
				}
			}
			if (startOrEndBox.getSelectedItem().equals("starts")) {
				if (halfwayIndex > 0) {
					oldStartField.setText(currDetectionList.get(0).datetime);
					oldEndField.setText(currDetectionList.get(halfwayIndex-1).datetime);
				} else {
					oldStartField.setText("");
					oldEndField.setText("");
				}
				oldTotalField.setText(String.valueOf(halfwayIndex));
				if (halfwayIndex < currDetectionList.size()) {
					newStartField.setText(currDetectionList.get(halfwayIndex).datetime);
					newEndField.setText(currDetectionList.get(currDetectionList.size()-1).datetime);
				} else {
					newStartField.setText("");
					newEndField.setText("");
				}
				newTotalField.setText(String.valueOf(currDetectionList.size()-halfwayIndex));
			} else {
				if (halfwayIndex > 0) {
					newStartField.setText(currDetectionList.get(0).datetime);
					newEndField.setText(currDetectionList.get(halfwayIndex).datetime);
				} else {
					newStartField.setText("");
					newEndField.setText("");
				}
				newTotalField.setText(String.valueOf(halfwayIndex+1));
				if (halfwayIndex < currDetectionList.size()) {
					oldStartField.setText(currDetectionList.get(halfwayIndex+1).datetime);
					oldEndField.setText(currDetectionList.get(currDetectionList.size()-1).datetime);
				} else {
					oldStartField.setText("");
					oldEndField.setText("");
				}
				oldTotalField.setText(String.valueOf(currDetectionList.size()-(halfwayIndex+1)));
			}
		}
	}
	
	/**
	 * Sets a limit to the number of characters allowed in a JTextField.
	 * Used with JTextField.setDocument(new JTextFieldLimit(int limit).getDocument());
	 * Copied from https://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
	 * Author page: https://stackoverflow.com/users/1866109/francisco-j-g%c3%bcemes-sevilla
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

	            if ((getLength() + str.length()) <= limit) {
	                super.insertString(offset, str, attr);
	            }
	        }       
	    }
	}

	@Override
	public boolean getParams() {
		if (oldTotalField.getText().equals("0") || newTotalField.getText().equals("0")) {
			tsbControl.simpleErrorDialog("New or old subset would have no entries in it if split at the currently specified data/time.", 250);
			return false;
		}
		String newID = (String) newIDDigit1Box.getSelectedItem() + (String) newIDDigit2Box.getSelectedItem();
		for (int i = 0; i < tsbControl.getSubsetList().size(); i++) {
			if (tsbControl.getSubsetList().get(i).id.equals(newID)) {
				tsbControl.simpleErrorDialog("Chosen ID for new subset already taken.", 250);
				return false;
			}
		}
		TSBSubset newSubset = new TSBSubset();
		newSubset.id = newID;
		newSubset.location = newLocationField.getText();
		newSubset.featurePath = currSubset.featurePath;
		newSubset.wmatPath = currSubset.wmatPath;
		newSubset.start = newStartField.getText();
		newSubset.end = newEndField.getText();
		newSubset.classList = new ArrayList<String>(currSubset.classList);
		newSubset.selectionArray = currSubset.selectionArray;
		for (int i = 0; i < currSubset.validEntriesList.size(); i++) {
			newSubset.validEntriesList.add(new ArrayList<TSBDetection>());
			int j = 0;
			while (j < currSubset.validEntriesList.get(i).size()) {
				TSBDetection currDetection = currSubset.validEntriesList.get(i).get(j);
				if (currDetection.datetime.compareTo(newStartField.getText()) >= 0 &&
						currDetection.datetime.compareTo(newEndField.getText()) <= 0) {
					newSubset.validEntriesList.get(i).add(currDetection);
					currSubset.validEntriesList.get(i).remove(j);
				} else {
					j++;
				}
			}
		}
		currSubset.start = oldStartField.getText();
		currSubset.end = oldEndField.getText();
		for (int i = 0; i < tsbControl.getSubsetList().size(); i++) {
			if (currSubset.id.equals(tsbControl.getSubsetList().get(i).id)) {
				tsbControl.getSubsetList().set(i, currSubset);
				break;
			}
		}
		tsbControl.getSubsetList().add(newSubset);
		ArrayList<Object[]> newRows = new ArrayList<Object[]>();
		for (int i = 0; i < tsbControl.getTabPanel().getPanel().getSubsetTable().getRowCount(); i++) {
			Object[] newRow = new Object[tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnCount()];
			if (tsbControl.getTabPanel().getPanel().getSubsetTable().getValueAt(i, 0).equals(currSubset.id)) {
				Object[] newRow2 = new Object[tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnCount()];
				for (int j = 0; j < 4; j++) {
					newRow[j] = tsbControl.getTabPanel().getPanel().getSubsetTable().getValueAt(i, j);
				}
				newRow2[0] = newSubset.id;
				newRow2[1] = newSubset.location;
				newRow2[2] = newSubset.start;
				newRow2[3] = newSubset.end;
				newRow[2] = currSubset.start;
				newRow[3] = currSubset.end;
				int totalCount = 0;
				int totalCount2 = 0;
				for (int j = 5; j < newRow.length; j++) {
					String columnName = tsbControl.getTabPanel().getPanel().getSubsetTable().getColumnName(j);
					for (int k = 0; k < newSubset.selectionArray.length; k++) {
						if (newSubset.classList.get(newSubset.selectionArray[k]).equals(columnName)) {
							totalCount += currSubset.validEntriesList.get(currSubset.selectionArray[k]).size();
							newRow[j] = String.valueOf(currSubset.validEntriesList.get(currSubset.selectionArray[k]).size());
							totalCount2 += newSubset.validEntriesList.get(newSubset.selectionArray[k]).size();
							newRow2[j] = String.valueOf(newSubset.validEntriesList.get(newSubset.selectionArray[k]).size());
							break;
						}
						if (k == newSubset.selectionArray.length-1) {
							newRow[j] = "0";
							newRow2[j] = "0";
						}
					}
				}
				newRow[4] = String.valueOf(totalCount);
				newRow2[4] = String.valueOf(totalCount2);
				if (startOrEndBox.getSelectedItem().equals("starts")) {
					newRows.add(newRow);
					newRows.add(newRow2);
				} else {
					newRows.add(newRow2);
					newRows.add(newRow);
				}
			} else {
				for (int j = 0; j < newRow.length; j++) {
					newRow[j] = tsbControl.getTabPanel().getPanel().getSubsetTable().getValueAt(i, j);
				}
				newRows.add(newRow);
			}
		}
		while (tsbControl.getTabPanel().getPanel().getSubsetTableModel().getRowCount() > 0) {
			tsbControl.getTabPanel().getPanel().getSubsetTableModel().removeRow(0);
		}
		for (int i = 0; i < newRows.size(); i++) {
			tsbControl.getTabPanel().getPanel().getSubsetTableModel().addRow(newRows.get(i));
		}
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {}
}