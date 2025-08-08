package mirfeeLiveClassifier;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

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
	protected JButton loadButton;
	protected JButton saveButton;
	
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
		buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
		addButton = new JButton("Add");
		addButton.addActionListener(new AddListener());
		buttonPanel.add(addButton);
		removeButton = new JButton("Remove");
		removeButton.addActionListener(new RemoveListener(this));
		buttonPanel.add(removeButton);
		loadButton = new JButton("Load config");
		loadButton.addActionListener(new LoadListener(this));
		buttonPanel.add(loadButton);
		saveButton = new JButton("Save config");
		saveButton.addActionListener(new SaveListener(this));
		buttonPanel.add(saveButton);
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
	
	class LoadListener implements ActionListener {
		
		private LCAliasDialog dialogPane;
		
		public LoadListener(LCAliasDialog dialogPane) {
			this.dialogPane = dialogPane;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
			int returnVal = fc.showOpenDialog(dialogPane);
			if (returnVal != fc.APPROVE_OPTION) return;
			currentAliasesList.clear();
			currentAliasesMap.clear();
			ArrayList<String> labelList = new ArrayList<String>();
			for (int i = 0; i < loadedClasses.length; i++)
				labelList.add(loadedClasses[i]);
			File f = fc.getSelectedFile();
			Scanner sc;
			try {
				sc = new Scanner(f);
				while (sc.hasNextLine()) {
					String[] split = sc.nextLine().split(": ");
					if (split.length < 2 || currentAliasesList.contains(split[0])) continue;
					currentAliasesList.add(split[0]);
					currentAliasesMap.put(split[0], split[1]);
				}
				updateSelectionPanel();
				sc.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
	}
	
	class SaveListener implements ActionListener {
		
		private LCAliasDialog dialogPane;
		
		public SaveListener(LCAliasDialog dialogPane) {
			this.dialogPane = dialogPane;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (currentAliasesList.size() == 0) {
				lcControl.simpleErrorDialog("No aliases have been created yet.");
				return;
			}
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.setAcceptAllFileFilterUsed(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
			int returnVal = fc.showSaveDialog(dialogPane);
			if (returnVal != fc.APPROVE_OPTION) return;
			File f = fc.getSelectedFile();
			if (!f.getAbsolutePath().endsWith(".txt"))
				f = new File(f.getAbsolutePath()+".txt");
			if (f.exists()) {
				int res = JOptionPane.showConfirmDialog(dialogPane,
						"Overwrite selected file?",
						lcControl.getUnitName(),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (res != JOptionPane.YES_OPTION) return;
			}
			try {
				PrintWriter pw = new PrintWriter(f);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < currentAliasesList.size(); i++) {
					String key = currentAliasesList.get(i);
					sb.append(key+": "+currentAliasesMap.get(key)+"\n");
				}
				pw.write(sb.toString());
				pw.flush();
				pw.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
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