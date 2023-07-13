package mirrfFeatureExtractor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;

import javax.swing.border.TitledBorder;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;

/**
 * The actual side panel.
 * @author Holly LeBlond
 */
public class FEPanel {
	
	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;
	public static final int IGNORE = 2;
	public static final int PENDING = 3;

	protected FEControl feControl;
	protected FESettingsDialog settingsDialog;
	
	protected PamPanel mainPanel;
	
	protected JTextField successField;
	protected JTextField failureField;
	protected JTextField ignoreField;
	protected JTextField pendingField;
	protected JButton resetButton;
	protected JButton reloadCSVButton; // This is useless now.
	
	public FEPanel(FEControl feControl) {
		this.feControl = feControl;
		
		mainPanel = new PamPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		PamPanel memPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		memPanel.setBorder(new TitledBorder("MIRRF Feature Extractor"));
		
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 1;
		memPanel.add(new JLabel("Successes: "), c);
		c.gridx++;
		successField = new JTextField(3);
		memPanel.add(successField, c);
		c.gridx++;
		memPanel.add(new JLabel("Failures: "), c);
		c.gridx++;
		failureField = new JTextField(3);
		memPanel.add(failureField, c);
		c.gridx++;
		memPanel.add(new JLabel("Ignores: "), c);
		c.gridx++;
		ignoreField = new JTextField(3);
		memPanel.add(ignoreField, c);
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		memPanel.add(new JLabel("Pending: "), c);
		c.gridx++;
		pendingField = new JTextField(3);
		memPanel.add(pendingField, c);
		c.gridx++;
		c.gridwidth = 2;
		resetButton = new JButton("Reset counters");
		resetButton.addActionListener(new ResetListener());
		memPanel.add(resetButton, c);
		c.gridx += 2;
		reloadCSVButton = new JButton("Reload .csv");
		reloadCSVButton.addActionListener(new ReloadCSVListener());
		reloadCSVButton.setEnabled(false);
		//memPanel.add(reloadCSVButton, c); // This is useless now.
		
		mainPanel.add(memPanel);
		
		successField.setEnabled(false);
		failureField.setEnabled(false);
		ignoreField.setEnabled(false);
		pendingField.setEnabled(false);
		
		pendingField.setText("0");
		resetCounters();
	}
	
	/**
	 * Resets all counters except "pending" to 0.
	 */
	public void resetCounters() {
		successField.setText("0");
		failureField.setText("0");
		ignoreField.setText("0");
	}
	
	/**
	 * Adds 1 to the respective counter.
	 * @param i - Number determining the chosen counter:
	 * <br> 0 = Success
	 * <br> 1 = Failure
	 * <br> 2 = Ignore
	 * <br> 3 = Pending
	 * @param uid - The contour's UID, for printing purposes.
	 */
	public void addOneToCounter(int i, String uid) {
		try {
			if (i == SUCCESS) {
				successField.setText(String.valueOf(Integer.valueOf(successField.getText())+1));
				System.out.println("Successfully processed contour "+uid+".");
			} else if (i == FAILURE) {
				failureField.setText(String.valueOf(Integer.valueOf(failureField.getText())+1));
				System.out.println("Could not process contour "+uid+".");
			} else if (i == IGNORE) {
				ignoreField.setText(String.valueOf(Integer.valueOf(ignoreField.getText())+1));
				System.out.println("Ignored contour "+uid+" due to settings.");
			} else if (i == PENDING) {
				pendingField.setText(String.valueOf(Integer.valueOf(pendingField.getText())+1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Subtracts 1 from the pending counter.
	 * @return True if the counter is above 0. False otherwise.
	 */
	public boolean subtractOneFromPendingCounter() {
		try {
			if (Integer.valueOf(pendingField.getText()) > 0) {
				pendingField.setText(String.valueOf(Integer.valueOf(pendingField.getText())-1));
			} else {
				return false;
			}
		} catch (Exception e) {
			System.out.println("ERROR: subtractOneFromPendingCounter failed.");
			return false;
		}
		return true;
	}
	
	/**
	 * @return The number in the success counter as an integer.
	 */
	public int getSuccessCount() {
		int outp = -1;
		try {
			outp = Integer.valueOf(successField.getText());
		} catch (Exception e) {
			System.out.println("ERROR: getSuccessCount failed.");
		}
		return outp;
	}
	
	protected class ReloadCSVListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			ArrayList<Integer> intList = new ArrayList<Integer>();
			int csvSize = feControl.getParams().inputCSVEntries.size();
			for (int i = 0; i < csvSize; i++) {
				intList.add(i);
			}
			feControl.getParams().inputCSVIndexes = new ArrayList<Integer>(intList);
		}
	}
	
	protected class ResetListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			resetCounters();
		}
	}
	
	@Deprecated
	protected JButton getReloadCSVButton() {
		return reloadCSVButton;
	}
	
	/**
	 * Streamlined error dialog.
	 */
	public void SimpleErrorDialog() {
		JOptionPane.showMessageDialog(null,
			"An error has occured.\nSee console for details.",
			"",
			JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Streamlined error dialog with an editable message.
	 */
	public void SimpleErrorDialog(String inptext) {
		JOptionPane.showMessageDialog(null,
			inptext,
			"",
			JOptionPane.ERROR_MESSAGE);
	}
	
	public JComponent getComponent() {
		return mainPanel;
	}

}