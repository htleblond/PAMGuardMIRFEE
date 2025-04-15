package mirfeeLiveClassifier;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;

public class LCAliasDialog extends PamDialog {
	
	private LCControl lcControl;
	private LCSettingsDialog dialog;
	
	protected JPanel mainPanel;
	
	protected JPanel selectionSuperPanel;
	protected JPanel selectionPanel;
	protected JLabel defaultMessage;
	protected JLabel[] labelColumn;
	protected JComboBox<String>[] boxColumn;
	
	protected JPanel buttonPanel;
	protected JButton addButton;
	protected JButton removeButton;
	
	protected String[] loadedClasses;
	protected ArrayList<String> currentAliasesList;
	protected HashMap<String, String> currentAliasesMap;
	
	public LCAliasDialog(Window parentFrame, LCControl lcControl, LCSettingsDialog dialog) {
		super(parentFrame, lcControl.getUnitName(), false);
		this.lcControl = lcControl;
		this.dialog = dialog;
		
		loadedClasses = new String[dialog.labelList.getModel().getSize()];
		for (int i = 0; i < loadedClasses.length; i++) {
			loadedClasses[i] = dialog.labelList.getModel().getElementAt(i);
		}
		
		currentAliasesMap = new HashMap<String, String>(dialog.currentAliases);
		Iterator<String> it = currentAliasesMap.keySet().iterator();
		currentAliasesList = new ArrayList<String>();
		while (it.hasNext())
			currentAliasesList.add(it.next());
		currentAliasesList.sort(null);
		
		mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder(""));
		GridBagConstraints b = new PamGridBagContraints();
		b.anchor = b.NORTH;
		b.fill = b.HORIZONTAL;
		
		selectionSuperPanel = new JPanel(new FlowLayout());
		selectionPanel = new JPanel();
		selectionSuperPanel.add(selectionPanel); // since updateSelectionPanel removes it
		updateSelectionPanel();
		mainPanel.add(selectionSuperPanel, b);
		
		b.gridy++;
		buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		addButton = new JButton("Add");
		addButton.addActionListener(new AddListener());
		buttonPanel.add(addButton);
		removeButton = new JButton("Remove");
		removeButton.addActionListener(new RemoveListener(this));
		buttonPanel.add(removeButton);
		mainPanel.add(buttonPanel, b);
		
		this.setDialogComponent(mainPanel);
	}
	
	public void updateSelectionPanel() {
		selectionSuperPanel.remove(selectionPanel);
		if (currentAliasesList.size() == 0) {
			selectionPanel = new JPanel(new FlowLayout());
			defaultMessage = new JLabel(LCControl.makeHTML("In cases where a class label in the selected training set additionally encompasses other species labels used in the "
					+ "WMAT (e.g. SRKW, NRKW, etc. in a training set where all killer whale detections are labelled KW), then those labels can be aliased to the training set class "
					+ "label. This would allow a detection in the WMAT labelled \"SRKW\" to be counted as a \"KW\" in the Live Classifier, for example.", 150));
			selectionPanel.add(defaultMessage);
		} else {
			selectionPanel = new JPanel(new GridLayout(currentAliasesList.size(),2,20,5));
			labelColumn = new JLabel[currentAliasesList.size()];
			boxColumn = new JComboBox[currentAliasesList.size()];
			for (int i = 0; i < labelColumn.length; i++) {
				labelColumn[i] = new JLabel(currentAliasesList.get(i));
				selectionPanel.add(labelColumn[i]);
				boxColumn[i] = new JComboBox<String>(loadedClasses);
				boxColumn[i].setSelectedItem(currentAliasesMap.get(currentAliasesList.get(i)));
				boxColumn[i].addActionListener(new BoxListener(i));
				selectionPanel.add(boxColumn[i]);
			}
		}
		selectionSuperPanel.add(selectionPanel);
		this.setDialogComponent(mainPanel);
	}
	
	class AddListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String newAlias = JOptionPane.showInputDialog("Enter new alias:");
			if (newAlias == null) return;
			for (int i = 0; i < loadedClasses.length; i++) {
				if (loadedClasses[i].equals(newAlias)) {
					lcControl.simpleErrorDialog("Alias cannot be the same as a class in the training set.", 250);
					return;
				}
			}
			if (currentAliasesList.contains(newAlias)) {
				lcControl.simpleErrorDialog("Input alias already in list.", 250);
				return;
			}
			currentAliasesList.add(newAlias);
			currentAliasesList.sort(null);
			currentAliasesMap.put(newAlias, loadedClasses[0]);
			updateSelectionPanel();
		}
		
	}
	
	class RemoveListener implements ActionListener {
		
		private LCAliasDialog dialogPane;
		
		public RemoveListener(LCAliasDialog dialogPane) {
			this.dialogPane = dialogPane;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (currentAliasesList.size() == 0) {
				lcControl.simpleErrorDialog("No aliases have been created.");
				return;
			}
			JPanel removePanel = new JPanel(new GridBagLayout());
			GridBagConstraints b = new PamGridBagContraints();
			b.fill = b.HORIZONTAL;
			b.anchor = b.WEST;
			removePanel.add(new JLabel("Select alias to remove:"), b);
			b.gridy++;
			String[] options = new String[currentAliasesList.size()];
			for (int i = 0; i < options.length; i++)
				options[i] = currentAliasesList.get(i);
			JComboBox<String> removeBox = new JComboBox(options);
			removePanel.add(removeBox, b);
			int res = JOptionPane.showConfirmDialog(dialogPane, 
					removePanel, 
					lcControl.getUnitName(), 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.PLAIN_MESSAGE, 
					null);
			if (res != JOptionPane.OK_OPTION)
				return;
			String removedAlias = (String) removeBox.getSelectedItem();
			currentAliasesList.remove(removedAlias);
			currentAliasesMap.remove(removedAlias);
			updateSelectionPanel();
		}
		
	}
	
	class BoxListener implements ActionListener {
		
		int index;
		public BoxListener(int index) {
			this.index = index;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			updateAliasesMap(index);
		}
		
	}
	
	public void updateAliasesMap(int index) {
		currentAliasesMap.put(labelColumn[index].getText(), (String) boxColumn[index].getSelectedItem());
	}

	@Override
	public boolean getParams() {
		dialog.currentAliases = new HashMap<String, String>(currentAliasesMap);
		return true;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {} // button disabled
	
}