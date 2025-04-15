package mirfeeTestClassifier;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamBorderPanel;
import PamView.panel.PamPanel;

/**
 * The actual GUI components of the side panel.
 * @author Holly LeBlond
 */
public class TCSidePanelPanel extends PamBorderPanel {
	
	protected TCControl tcControl;
	
	protected PamPanel mainPanel;
	protected PamPanel counterPanel;
	
	protected JProgressBar loadingBar;
	protected JButton startButton;
	
	protected JTextField errorField;
	protected JTextField ignoreField;
	
	public TCSidePanelPanel(TCControl tcControl) {
		this.tcControl = tcControl;
		
		mainPanel = new PamPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("MIRFEE Test Classifier"));
		GridBagConstraints b = new PamGridBagContraints();
		//b.fill = b.HORIZONTAL;
		loadingBar = new JProgressBar();
		loadingBar.setPreferredSize(new Dimension(200, 20));
		loadingBar.setValue(0);
		loadingBar.setStringPainted(true);
		loadingBar.setString("Idle");
		mainPanel.add(loadingBar, b);
		b.gridx++;
		startButton = new JButton("Start");
		//startButton.setEnabled(false);
		startButton.addActionListener(new StartButtonListener());
		mainPanel.add(startButton, b);
		
		counterPanel = new PamPanel(new GridLayout(1, 4, 10, 10));
		//GridBagConstraints c = new PamGridBagContraints();
		//c.fill = c.HORIZONTAL;
		//c.anchor = c.WEST;
		errorField = new JTextField(4);
		errorField.setEnabled(false);
		errorField.setText("0");
		counterPanel.add(errorField);
		//c.gridx++;
		counterPanel.add(new JLabel("Errors"));
		//c.gridy++;
		//c.gridx = 0;
		ignoreField = new JTextField(4);
		ignoreField.setEnabled(false);
		ignoreField.setText("0");
		counterPanel.add(ignoreField);
		//c.gridx++;
		counterPanel.add(new JLabel("Ignores"));
		b.fill = b.NONE;
		b.anchor = b.WEST;
		b.gridy++;
		b.gridx = 0;
		b.gridwidth = 2;
		mainPanel.add(counterPanel, b);
		
		this.add(mainPanel);
	}
	
	public class StartButtonListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (startButton.getText().equals("Start")) {
				start();
			} else {
				stop();
			}
		}
		
	}
	
	/**
	 * When the start button is pressed.
	 */
	protected void start() {
		if (getControl().getTabPanel().getPanel().getTableModel().getRowCount() > 0) {
			int result = JOptionPane.showConfirmDialog(this, 
					getControl().makeHTML("Restarting will clear the table and data block. Proceed?", 250),
					getControl().getUnitName(),
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.NO_OPTION) return;
		}
		errorField.setText("0");
		ignoreField.setText("0");
		startButton.setEnabled(false);
		getControl().getTabPanel().getPanel().createMatrices(getControl().getParams().labelOrder, false);
		getControl().getTabPanel().getPanel().clearTable();
		getControl().getProcess().clearOutputDataBlock();
		loadingBar.setValue(0);
		loadingBar.setString("Verifying settings...");
		TCParameters params = getControl().getParams();
		if (params.loadedTestingSetInfo == null ||
				(params.validation >= params.LABELLED && params.loadedTestingSetInfo == null)) {
			loadingBar.setString("Idle");
			getControl().simpleErrorDialog("Training/testing sets have not been configured.", 250);
			return;
		}
		StartThread startThread = new StartThread();
		startThread.start();
	}
	
	protected class StartThread extends Thread {
		protected StartThread() {}
		@Override
		public void run() {
			TCParameters params = getControl().getParams();
			
			// Not meant to be visible - I'm only using some functions from it.
			TCSettingsDialog dialogInstance = new TCSettingsDialog(getControl().getGuiFrame(), getControl());
			
			loadingBar.setString("Verifying training set...");
			if (!dialogInstance.checkIfTrainingSetIsValid(params.getTrainingSetInfo(), true, false)) {
				loadingBar.setString("Idle");
				startButton.setEnabled(true);
				return;
			}
			if (params.validation >= params.LABELLED) {
				loadingBar.setString("Verifying testing set...");
				if (!dialogInstance.checkIfTestingSetIsValid(params.getTestingSetInfo(), true, false)) {
					loadingBar.setString("Idle");
					startButton.setEnabled(true);
					return;
				}
			}
			
			if (!getControl().getThreadManager().initializeTrainingSets()) {
				startButton.setEnabled(true);
				return;
			}
			getControl().getThreadManager().startPredictions(10000);
			//startButton.setText("Start");
			//startButton.setEnabled(true);
		}
	}
	
	/**
	 * When the stop button is pressed.
	 */
	protected void stop() {
		getControl().getThreadManager().stop();
	}
	
	public void addOneToErrorCounter() {
		errorField.setText(String.valueOf(Integer.valueOf(errorField.getText()).intValue() + 1));
	}
	
	public void addOneToIgnoreCounter() {
		ignoreField.setText(String.valueOf(Integer.valueOf(ignoreField.getText()).intValue() + 1));
	}
	
	/**
	 * Resets the error and ignore counters to 0.
	 */
	public void resetCounters() {
		errorField.setText("0");
		ignoreField.setText("0");
	}
	
	protected TCControl getControl() {
		return tcControl;
	}
	
	/**
	 * @return mainPanel
	 */
	public JComponent getComponent() {
		return mainPanel;
	}
}