package wmat;

import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.*;
import javax.swing.table.*; //
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import PamController.PamControlledUnit;

import javax.swing.filechooser.*;
import java.util.*; //
import java.text.*; //
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.swing.border.TitledBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import PamView.PamColors;
import PamView.PamTable;
import PamView.PamColors.PamColor;
import PamView.dialog.PamButton; //
import PamView.dialog.PamTextField; //

import binaryFileStorage.*;
import fftManager.FFTDataBlock;
import mirfee.MIRFEEControlledUnit;
import mirfeeLiveClassifier.LCCallCluster;
import mirfeeLiveClassifier.LCControl;
import mirfeeLiveClassifier.LCDataBlock;
import mirfeeLiveClassifier.LCDataUnit;
import mirfeeLiveClassifier.LCParameters;
import mirfeeLiveClassifier.LCPanel.CustomTableRenderer;
import PamguardMVC.DataUnitBaseData;
import PamguardMVC.PamDataBlock;
import pamScrollSystem.*;
import whistleClassifier.BasicFragmentStore;
import whistleClassifier.OverlappingFragmenter;
import whistleClassifier.WhistleFragment;
import whistleClassifier.WhistleFragmenter;
import whistlesAndMoans.ConnectedRegionDataBlock;
import whistlesAndMoans.ConnectedRegionDataUnit;
import whistlesAndMoans.WhistleToneConnectProcess;
import PamUtils.PamFileChooser;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import pamMaths.Regressions;
import pamMaths.STD;

/**
 * The panel where the GUI components of the WMAT are written.
 * Contains the JTable and is connected to most functions of the module.
 * @author Holly LeBlond
 */
public class WMATPanel {

	protected WMATControl wmatControl;
	
	protected PamPanel mainPanel;
	
	protected PamFileChooser fc;
	
	protected PamTextField fileField;
	protected PamButton fileButton;
	protected PamButton importButton;
	protected PamButton exportButton;
	protected PamButton connectButton;
	protected PamButton checkButton;
	protected PamButton updateButton;
	protected PamButton countButton;
	public PamTable ttable;
	public JScrollPane tableScroller;
	protected DefaultTableModel dtmodel;
	public DefaultComboBoxModel<String> speciesModel;
	public JComboBox<String> speciesBox;
	public DefaultComboBoxModel<String> calltypeModel;
	public JComboBox<String> calltypeBox;
	protected PamTextField commentField;
	protected PamButton speciesButton;
	protected PamButton calltypeButton;
	protected PamButton commentButton;
	protected PamButton allButton;
	//protected PamButton selectAllButton;
	protected PamButton selectNextUnlabelledButton;
	//protected PamButton clearSelectionButton;
	protected PamButton selectWithinViewButton;
	protected PamButton selectStartButton;
	protected PamButton searchButton;
	protected PamButton scrollButton;
	protected PamButton undoButton;
	
	protected BackupCellEditor speciesCellEditor;
	protected BackupCellEditor callTypeCellEditor;
	protected BackupCellEditor commentCellEditor;
	
	public Object[][] originalTable;
	public boolean[] tableChangeLog;
	
	protected HashMap<String, ConnectedRegionDataUnit> crduMap;
	
	protected WMATSearchDialog searchDialog;
	
	protected int currformat;
	
	static protected final int textLength = 35;
	
	protected WMATBinaryReader reader;
	protected WMATMarkControl marker;
	protected ArrayList<String> binaryFileDates;
	
	protected String defaultloc;
	//protected File[] files;
	
	protected WMATSQLLogging testLogger;
	
	protected int[] backupIndexes;
	protected Object[][] backupValues;
	
	protected WMATBinaryLoadingBarWindow binaryLoadingBarWindow;
	protected BinaryLoadingBarThread binaryLoadingBarThread;
	
	protected boolean warningForMultipleScrollersTriggeredThisSession = false;
	
	public WMATPanel(WMATControl wmatControl) {
		this.wmatControl = wmatControl;
		currformat = 6;
		this.crduMap = new HashMap<String, ConnectedRegionDataUnit>();
		testLogger = new WMATSQLLogging(wmatControl);
		backupIndexes = null;
		backupValues = null;
		
		mainPanel = new PamPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		PamPanel memPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		memPanel.setBorder(new TitledBorder("Whistle and Moan Annotation Tool"));
		
		fileField = new PamTextField(textLength);
		fileField.setEditable(false);
		c.gridwidth = 3;
		memPanel.add(fileField, c);
		
		c.gridx = 3;
		c.gridwidth = 2;
		fileButton = new PamButton("Load binary data");
		OpenListener openListener = new OpenListener();
		fileButton.addActionListener(openListener);
		memPanel.add(fileButton, c);
		
		PamPanel dataPanel = new PamPanel();
		dataPanel.setLayout(new GridLayout(0, 3, 5, 5));
		connectButton = new PamButton("Connect to database");
		ConnectListener connectListener = new ConnectListener();
		connectButton.addActionListener(connectListener);
		dataPanel.add(connectButton);
		checkButton = new PamButton("Check database for alignment");
		CheckListener checkListener = new CheckListener();
		checkButton.addActionListener(checkListener);
		checkButton.setEnabled(false);
		dataPanel.add(checkButton);
		updateButton = new PamButton("Execute database updates");
		UpdateListener updateListener = new UpdateListener();
		updateButton.addActionListener(updateListener);
		updateButton.setEnabled(false);
		dataPanel.add(updateButton);
		importButton = new PamButton("Import table");
		ImportListener importListener = new ImportListener();
		importButton.addActionListener(importListener);
		dataPanel.add(importButton);
		exportButton = new PamButton("Export table");
		ExportListener exportListener = new ExportListener();
		exportButton.addActionListener(exportListener);
		dataPanel.add(exportButton);
		countButton = new PamButton("Label counts");
		CountListener countListener = new CountListener();
		countButton.addActionListener(countListener);
		dataPanel.add(countButton);
		
		c.gridy++;
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 5;
		memPanel.add(dataPanel, c);
		
		
		String[] columnNames = {"UID", "Date/Time (UTC)", "LF", "HF", "Dur", "Amp", "Species", "Call type", "Comment", "Prediction", "Score"};
		
		//Kudos to this: https://stackoverflow.com/questions/9090974/problems-with-jtable-sorting-of-integer-values/9091438
		dtmodel = new DefaultTableModel(columnNames,0) {
			Class[] types = {Long.class, String.class, Integer.class, Integer.class, Integer.class, Integer.class,
					String.class, String.class, String.class, String.class, String.class};
			boolean[] canEdit = {false, false, false, false, false, false, true, true, true, false, false};
			
			@Override
			public Class getColumnClass(int index) {
				return this.types[index];
			}
			
			@Override
			public boolean isCellEditable(int row, int column) {
				return this.canEdit[column];
			}
			
		};
		ttable = new PamTable(dtmodel);
		ttable.setDefaultRenderer(String.class, new CustomTableRenderer(wmatControl));
		ttable.getTableHeader().setReorderingAllowed(false);
		ttable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		ttable.setAutoCreateRowSorter(true);
		ttable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		ttable.getColumnModel().getColumn(0).setPreferredWidth(70);
		ttable.getColumnModel().getColumn(1).setPreferredWidth(140);
		ttable.getColumnModel().getColumn(2).setPreferredWidth(35);
		ttable.getColumnModel().getColumn(3).setPreferredWidth(35);
		ttable.getColumnModel().getColumn(4).setPreferredWidth(35);
		ttable.getColumnModel().getColumn(5).setPreferredWidth(35);
		ttable.getColumnModel().getColumn(6).setPreferredWidth(60);
		ttable.getColumnModel().getColumn(7).setPreferredWidth(60);
		ttable.getColumnModel().getColumn(7).setPreferredWidth(60);
		ttable.getColumnModel().getColumn(8).setPreferredWidth(60);
		ttable.getColumnModel().getColumn(9).setPreferredWidth(60);
		ttable.getColumnModel().getColumn(10).setPreferredWidth(40);
		
		JTextField fSpecies = new JTextField();
		fSpecies.setDocument(new CommaRemovingDocument(WMATSQLLogging.SPECIES_CHAR_LENGTH));
		ttable.getColumnModel().getColumn(6).setCellEditor(speciesCellEditor = new BackupCellEditor(fSpecies));
		JTextField fCallType = new JTextField();
		fCallType.setDocument(new CommaRemovingDocument(WMATSQLLogging.CALLTYPE_CHAR_LENGTH));
		ttable.getColumnModel().getColumn(7).setCellEditor(callTypeCellEditor = new BackupCellEditor(fCallType));
		JTextField fComment = new JTextField();
		fComment.setDocument(new CommaRemovingDocument(WMATSQLLogging.COMMENT_NVARCHAR_LENGTH));
		ttable.getColumnModel().getColumn(8).setCellEditor(commentCellEditor = new BackupCellEditor(fComment));
		
		//ttable.getColumnModel().getColumn(6).set
		//System.out.println("Class name: "+ttable.getColumnModel().getColumn(6).getCellEditor().getClass().toString());
		ttable.getColumnModel().getColumn(6).getCellEditor().addCellEditorListener(new CellEditorLoggingListener());
		ttable.getColumnModel().getColumn(7).getCellEditor().addCellEditorListener(new CellEditorLoggingListener());
		ttable.getColumnModel().getColumn(8).getCellEditor().addCellEditorListener(new CellEditorLoggingListener());
		

		tableScroller = new JScrollPane(ttable);
		int prefWidth = 0;
		for (int i = 0; i < columnNames.length; i++)
			prefWidth += ttable.getColumnModel().getColumn(i).getPreferredWidth();
		tableScroller.setPreferredSize(new Dimension(prefWidth, wmatControl.getParams().tableHeight));
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 5;
		memPanel.add(tableScroller, c);
		
		PamPanel buttonPanel = new PamPanel();
		buttonPanel.setLayout(new GridLayout(0, 3, 5, 5));
		selectStartButton = new PamButton("Select within start interval");
		SelectStartListener selectStartListener = new SelectStartListener();
		selectStartButton.addActionListener(selectStartListener);
		buttonPanel.add(selectStartButton);
		searchButton = new PamButton("Select by search");
		SearchListener searchListener = new SearchListener();
		searchButton.addActionListener(searchListener);
		buttonPanel.add(searchButton);
		scrollButton = new PamButton("Scroll to selection on spectrogram");
		ScrollListener scrollListener = new ScrollListener();
		scrollButton.addActionListener(scrollListener);
		buttonPanel.add(scrollButton);
	/*	selectAllButton = new PamButton("Select all");
		SelectAllListener selectAllListener = new SelectAllListener();
		selectAllButton.addActionListener(selectAllListener);
		buttonPanel.add(selectAllButton); */
		selectNextUnlabelledButton = new PamButton("Select next unlabelled detection");
		SelectNextUnlabelledListener selectFirstUnlabelledListener = new SelectNextUnlabelledListener();
		selectNextUnlabelledButton.addActionListener(selectFirstUnlabelledListener);
		buttonPanel.add(selectNextUnlabelledButton);
	/*	clearSelectionButton = new PamButton("Clear selection");
		ClearSelectionListener clearSelectionListener = new ClearSelectionListener();
		clearSelectionButton.addActionListener(clearSelectionListener);
		buttonPanel.add(clearSelectionButton); */
		selectWithinViewButton = new PamButton("Select all in spectrogram view");
		SelectWithinViewListener selectWithinViewListener = new SelectWithinViewListener();
		selectWithinViewButton.addActionListener(selectWithinViewListener);
		buttonPanel.add(selectWithinViewButton);
		undoButton = new PamButton("Undo");
		UndoListener undoListener = new UndoListener();
		undoButton.addActionListener(undoListener);
		undoButton.setEnabled(false);
		buttonPanel.add(undoButton);
		
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		memPanel.add(buttonPanel, c);
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		speciesModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i < wmatControl.getParams().speciesList.size(); i++)
			speciesModel.addElement(wmatControl.getParams().speciesList.get(i));
		speciesBox = new JComboBox<String>(speciesModel);
		speciesBox.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaaaaaaa");
		memPanel.add(speciesBox, c);
		
		c.gridx++;
		speciesButton = new PamButton("Species");
		SpeciesListener speciesListener = new SpeciesListener();
		speciesButton.addActionListener(speciesListener);
		memPanel.add(speciesButton, c);
		
		c.gridx++;
		calltypeModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i < wmatControl.getParams().callTypeList.size(); i++)
			calltypeModel.addElement(wmatControl.getParams().callTypeList.get(i));
		calltypeBox = new JComboBox<String>(calltypeModel);
		calltypeBox.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaaaaaaa");
		memPanel.add(calltypeBox, c);
		
		c.gridx++;
		calltypeButton = new PamButton("Call type");
		CalltypeListener calltypeListener = new CalltypeListener();
		calltypeButton.addActionListener(calltypeListener);
		memPanel.add(calltypeButton, c);
		
		CommentListener commentListener = new CommentListener();
		
		c.gridx++;
		c.gridheight = 2;
		c.fill = GridBagConstraints.BOTH;
		allButton = new PamButton("Enter\n all");
		AllListener allListener = new AllListener();
		allButton.addActionListener(allListener);
		memPanel.add(allButton, c);
		
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridheight = 1;
		c.gridwidth = 3;
		commentField = new PamTextField(textLength);
		commentField.setDocument(new JTextFieldLimit(WMATSQLLogging.COMMENT_NVARCHAR_LENGTH).getDocument());
		commentField.addActionListener(commentListener);
		memPanel.add(commentField, c);
		
		c.gridx = 3;
		c.gridwidth = 1;
		commentButton = new PamButton("Comment");
		commentButton.addActionListener(commentListener);
		memPanel.add(commentButton, c);
		
		mainPanel.add(memPanel);
		
		this.marker = new WMATMarkControl("WMAT Contour Selector", this);
		
		updateHotkeys();
	}
	
	public void updateHotkeys() {
		//Kudos: https://stackoverflow.com/questions/15313469/java-keyboard-keycodes-list
		mainPanel.getInputMap(mainPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(81, java.awt.event.InputEvent.ALT_DOWN_MASK), "altQ");
		mainPanel.getActionMap().put("altQ", new SelectNextUnlabelledHotkey());
		mainPanel.getInputMap(mainPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(87, java.awt.event.InputEvent.ALT_DOWN_MASK), "altW");
		mainPanel.getActionMap().put("altW", new ScrollHotkey());
		mainPanel.getInputMap(mainPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(69, java.awt.event.InputEvent.ALT_DOWN_MASK), "altE");
		mainPanel.getActionMap().put("altE", new SelectWithinViewHotkey());
		mainPanel.getInputMap(mainPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(90, java.awt.event.InputEvent.CTRL_DOWN_MASK), "ctrlZ");
		mainPanel.getActionMap().put("ctrlZ", new UndoHotkey());
		for (int i = 0; i < 10; i++) {
			mainPanel.getInputMap(mainPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(48+i, java.awt.event.InputEvent.ALT_DOWN_MASK), "alt"+String.valueOf(i));
			mainPanel.getActionMap().put("alt"+String.valueOf(i), new NumHotkey(i));
		}
	}
	
	interface HotkeyAction extends Action {
		@Override
		public default Object getValue(String key) {return null;}
		@Override
		public default void putValue(String key, Object value) {}
		@Override
		public default void setEnabled(boolean b) {}
		@Override
		public default void addPropertyChangeListener(PropertyChangeListener listener) {}
		@Override
		public default void removePropertyChangeListener(PropertyChangeListener listener) {}
	}
	
	public void stopCellEditing() {
		speciesCellEditor.publicFireEditingStopped();
		callTypeCellEditor.publicFireEditingStopped();
		commentCellEditor.publicFireEditingStopped();
	}
	
	class SelectNextUnlabelledHotkey implements HotkeyAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
			selectNextUnlabelledButton.doClick();
		}
		@Override
		public boolean isEnabled() {
			return wmatControl.getParams().hotkeyQEnabled;
		}
	}
	
	class ScrollHotkey implements HotkeyAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
			scrollButton.doClick();
		}
		@Override
		public boolean isEnabled() {
			return wmatControl.getParams().hotkeyWEnabled;
		}
	}
	
	class SelectWithinViewHotkey implements HotkeyAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
			selectWithinViewButton.doClick();
		}
		@Override
		public boolean isEnabled() {
			return wmatControl.getParams().hotkeyEEnabled;
		}
	}
	
	class UndoHotkey implements HotkeyAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
			undoButton.doClick();
		}
		@Override
		public boolean isEnabled() {
			return wmatControl.getParams().hotkeyZEnabled;
		}
	}
	
	class NumHotkey implements HotkeyAction {
		
		int num;
		
		public NumHotkey(int num) {
			this.num = num;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
			String[] vals = wmatControl.getParams().hotkeyNumLabels[num];
			if (vals[0].equals("<clear>")) {
				speciesBox.setSelectedItem("");
			} else if (!vals[0].equals("<skip>")) {
				speciesBox.setSelectedItem(vals[0]);
			}
			if (vals[1].equals("<clear>")) {
				calltypeBox.setSelectedItem("");
			} else if (!vals[1].equals("<skip>")) {
				calltypeBox.setSelectedItem(vals[1]);
			}
			if ((vals[0].equals((String) speciesBox.getSelectedItem()) || vals[0].equals("<skip>") || vals[0].equals("<clear>")) &&
					(vals[1].equals((String) calltypeBox.getSelectedItem()) || vals[1].equals("<skip>") || vals[1].equals("<clear>"))) {
				if (!vals[0].equals("<skip>")) speciesButton.doClick();
				if (!vals[1].equals("<skip>")) calltypeButton.doClick();
				return;
			}
			wmatControl.simpleErrorDialog("Label(s) assigned to hotkey are no longer in list(s).");
		}
		@Override
		public boolean isEnabled() {
			return wmatControl.getParams().hotkeyNumEnabled[num];
		}
	}
	
	/**
	 * Removes all elements from the JTable.
	 */
	public void clearTable() {
		dtmodel.setRowCount(0);
	}
	
	/**
	 * The listener for the 'Select all' button.
	 */
/*	class SelectAllListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			ttable.selectAll();
		}
	} */
	
	class SelectNextUnlabelledListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (ttable.getRowCount() == 0) {
				wmatControl.simpleErrorDialog("No data has been loaded into the table.");
				return;
			}
			int startIndex = 0;
			if (ttable.getSelectedRow() > -1)
				startIndex = (ttable.getSelectedRows()[ttable.getSelectedRows().length-1]+1) % ttable.getRowCount();
			// startIndex is the first row after the last currently-selected row.
			// If nothing has been selected, it defaults to the first row (0).
			// If the last row in the table has been selected, the modulo makes it wrap around to the beginning.
			int i = startIndex;
			do {
				String val = (String) ttable.getValueAt(i, 6);
				if (val.length() == 0) {
					ttable.clearSelection();
					ttable.addRowSelectionInterval(i, i);
					ttable.scrollRectToVisible(ttable.getCellRect(i, 0, true));
					return;
				}
				i = (i+1) % ttable.getRowCount(); // Wraps around to the top of the list.
			} while (i != startIndex); // Stops when every row has been read after wrapping around.
			JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
				    "All detections in the table have been labelled by species.",
				    wmatControl.getUnitName(),
				    JOptionPane.WARNING_MESSAGE);
		}
	}
	
	/**
	 * The listener for the 'Clear all' button.
	 */
/*	class ClearSelectionListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			ttable.clearSelection();
		}
	} */
	
	class SelectWithinViewListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			ViewerScrollerManager vsm = (ViewerScrollerManager) AbstractScrollManager.getScrollManager();
			if (vsm.getPamScrollers().size() == 0) {
				JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
					    "No spectrogram display has been added.",
					    wmatControl.getUnitName(),
					    JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (vsm.getPamScrollers().size() > 1 && !warningForMultipleScrollersTriggeredThisSession) {
				JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
					    "Multiple scrollers found — only the first one found will be used. If the wrong values are selected, the "
					    + "other displays with scrollers may need to be removed. (This warning should only appear once per session.)",
					    wmatControl.getUnitName(),
					    JOptionPane.WARNING_MESSAGE);
				warningForMultipleScrollersTriggeredThisSession = true;
			}
			AbstractPamScroller scroller = vsm.getPamScrollers().get(0);
			String startTime = MIRFEEControlledUnit.convertDateLongToString(scroller.getMinimumMillis());
			startTime = wmatControl.convertBetweenTimeZones(MIRFEEControlledUnit.getLocalTimeZoneName(), "UTC", startTime, true);
			String endTime = MIRFEEControlledUnit.convertDateLongToString(scroller.getMaximumMillis());
			endTime = wmatControl.convertBetweenTimeZones(MIRFEEControlledUnit.getLocalTimeZoneName(), "UTC", endTime, true);
			//System.out.println("startTime: "+startTime);
			//System.out.println("endTime: "+endTime);
			
			ttable.clearSelection();
			int firstScroll = -1;
			int lastScroll = -1;
			for (int i = 0; i < ttable.getRowCount(); i++) {
				String val = ttable.getValueAt(i, 1).toString();
				if (startTime.compareTo(val) <= 0 && endTime.compareTo(val) > 0) {
					ttable.addRowSelectionInterval(i, i);
					if (firstScroll == -1) firstScroll = i;
					lastScroll = i;
				}
			}
			if (firstScroll != -1) {
				ttable.scrollRectToVisible(ttable.getCellRect(lastScroll, 0, true));
				ttable.scrollRectToVisible(ttable.getCellRect(firstScroll, 0, true));
			}
		}
	}
	
	/**
	 * The listener for the 'Select within start interval' button.
	 */
	class SelectStartListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			ttable.clearSelection();
			binaryFileDates.sort(null);
			for (int i = 0; i < ttable.getRowCount(); i++) {
				try {
					String fileDate = "1970-01-01 00:00:00+000";
					for (int j = 0; j < binaryFileDates.size(); j++) {
						if (binaryFileDates.get(j).compareTo(ttable.getValueAt(i, 1).toString()) > 0)
							break;
						fileDate = binaryFileDates.get(j);
					}
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
					Calendar cal = Calendar.getInstance();
					cal.setTime(df.parse(fileDate));
					cal.add(Calendar.MILLISECOND, wmatControl.getParams().startBuffer);
					String limdate = df.format(cal.getTime());
					if (ttable.getValueAt(i, 1).toString().compareTo(limdate) < 0) {
						ttable.addRowSelectionInterval(i, i);
					}
				} catch (ParseException e1) {
					e1.printStackTrace();
					//simpleErrorDialog();
				}
			}
		}
	}
	
	/**
	 * The listener for the 'Select by search' button.
	 */
	class SearchListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (ttable.getRowCount() <= 0) {
				JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
					    "No data loaded into table.",
					    "Selection by search",
					    JOptionPane.WARNING_MESSAGE);
			} else {
				searchDialog = new WMATSearchDialog(wmatControl.getGuiFrame(), wmatControl, ttable);
				searchDialog.setVisible(true);
			}
		}
	}
	
	/**
	 * The listener for the 'Species' button.
	 */
	class SpeciesListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			createBackup(true, false);
			String species = speciesBox.getSelectedItem().toString();
			int[] selrows = ttable.getSelectedRows();
			if (selrows.length == 0) {
				return;
			}
			for(int i = 0; i < selrows.length; i++) {
				ttable.setValueAt(species, selrows[i], 6);
				fixChangeLog(selrows[i], getOriginalIndex(selrows[i]));
			}
			updateFromSelectedRows();
		}
	}
	
	/**
	 * The listener for the 'Call type' button.
	 */
	class CalltypeListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			createBackup(true, false);
			String classification = calltypeBox.getSelectedItem().toString();
			int[] selrows = ttable.getSelectedRows();
			if (selrows.length == 0) {
				return;
			}
			for(int i = 0; i < selrows.length; i++) {
				ttable.setValueAt(classification, selrows[i], 7);
				fixChangeLog(selrows[i], getOriginalIndex(selrows[i]));
			}
			updateFromSelectedRows();
		}
	}
	
	/**
	 * The listener for the 'Comment' button and text field.
	 */
	class CommentListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			createBackup(true, false);
			String comment = commentField.getText();
			int[] selrows = ttable.getSelectedRows();
			if (selrows.length == 0) {
				return;
			}
			for(int i = 0; i < selrows.length; i++) {
				ttable.setValueAt(comment, selrows[i], 8);
				fixChangeLog(selrows[i], getOriginalIndex(selrows[i]));
			}
			updateFromSelectedRows();
		}
	}
	
	/**
	 * The listener for the 'Enter all' button.
	 */
	class AllListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			createBackup(true, false);
			String species = speciesBox.getSelectedItem().toString();
			String classification = calltypeBox.getSelectedItem().toString();
			String comment = commentField.getText();
			int[] selrows = ttable.getSelectedRows();
			if (selrows.length == 0) {
				return;
			}
			for(int i = 0; i < selrows.length; i++) {
				ttable.setValueAt(species, selrows[i], 6);
				ttable.setValueAt(classification, selrows[i], 7);
				ttable.setValueAt(comment, selrows[i], 8);
				fixChangeLog(selrows[i], getOriginalIndex(selrows[i]));
			}
			updateFromSelectedRows();
		}
	}
	
	@Deprecated
	public int importLCPredictions(LCDataBlock lcDB, boolean matchIndividualContours, boolean markComments, boolean overwrite, double minCertainty) {
		HashMap<String, Integer> tableMap = new HashMap<String, Integer>();
		Object[][] tableArray = getTableRowsAsArray();
		if (tableArray.length == 0)
			return -1;
		createBackup(false, false);
		for (int i = 0; i < tableArray.length; i++) {
			String key = String.valueOf((long) tableArray[i][0])+", "+String.valueOf(tableArray[i][1]);
			if (tableMap.containsKey(key))
				continue;
			tableMap.put(key, i);
		}
		int updateCount = 0;
		for (int i = 0; i < lcDB.getUnitsCount(); i++) {
			LCDataUnit du = lcDB.getDataUnit(i, LCDataBlock.REFERENCE_ABSOLUTE);
			LCCallCluster cc = du.getCluster();
			for (int j = 0; j < cc.getSize(); j++) {
				String key = String.valueOf(cc.uids[j])+", "+MIRFEEControlledUnit.convertDateLongToString(MIRFEEControlledUnit.convertFromLocalToUTC(cc.datetimes[j]));
				if (!tableMap.containsKey(key))
					continue;
				int tableIndex = tableMap.get(key).intValue();
				if (!overwrite) {
					if (!ttable.getModel().getValueAt(tableIndex, 6).equals("") ||
						!ttable.getModel().getValueAt(tableIndex, 7).equals("") ||
						!ttable.getModel().getValueAt(tableIndex, 8).equals(""))
						continue;
				}
				if (matchIndividualContours) {
					if (cc.getIndividualCertainty(j) < minCertainty)
						continue;
					ttable.getModel().setValueAt(cc.getIndividualPredictedSpeciesString(j), tableIndex, 6);
					ttable.getModel().setValueAt("", tableIndex, 7);
					if (markComments)
						ttable.getModel().setValueAt("LC individual prediction ("+String.format("%.2f", cc.getIndividualCertainty(j))+")", tableIndex, 8);
				} else {
					if (cc.getCertainty() < minCertainty)
						break;
					ttable.getModel().setValueAt(cc.getPredictedSpeciesString(), tableIndex, 6);
					ttable.getModel().setValueAt("", tableIndex, 7);
					if (markComments)
						ttable.getModel().setValueAt("LC cluster prediction ("+String.format("%.2f", cc.getCertainty())+")", tableIndex, 8);
				}
				
				fixChangeLog(tableIndex, getOriginalIndex(tableIndex));
				updateCount++;
			}
		}
		updateFromFullTable();
		return updateCount;
	}
	
	/**
	 * The listener for the 'Connect to database' button.
	 */
	class ConnectListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			ConnectThread connectThread = new ConnectThread();
			connectThread.start();
		}
	}
	
	protected class ConnectThread extends Thread {
		protected ConnectThread() {}
		@Override
		public void run() {
			int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
				    "This will turn on AutoCommit.\nProceed?",
				    "Connect to database",
				    JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
					    "Read data from database into table?\n(Data currently in the table will be overwritten.)\n",
					    "Connect to database",
					    JOptionPane.YES_NO_CANCEL_OPTION);
				if (res != JOptionPane.CANCEL_OPTION) {
					fileField.setText("Connecting to database...");
					checkButton.setEnabled(false);
					updateButton.setEnabled(false);
					backupIndexes = null;
					backupValues = null;
					undoButton.setEnabled(false);
					try {
						testLogger.createColumnsAndFillTable(res == JOptionPane.YES_OPTION);
						updateFromFullTable();
					} catch (Exception e2) {
						e2.printStackTrace();
						simpleErrorDialog();
					}
					fileField.setText(defaultloc);
				}
			}
		}
	}
	
	/**
	 * The listener for the 'Load data' button.
	 */
	class OpenListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			OpenerThread openerThread = new OpenerThread();
			openerThread.start();
		}
	}
	
	/**
	 * Thread used for running the loading bar window.
	 */
	protected class BinaryLoadingBarThread extends Thread {
		protected BinaryLoadingBarThread() {}
		@Override
		public void run() {
			binaryLoadingBarWindow.setVisible(true);
		}
	}
	
	/**
	 * Thread used for loading binary file data into the table, 
	 * and to allow the loading bar window to function properly.
	 */
	protected class OpenerThread extends Thread {
		protected OpenerThread() {}
		@Override
		public void run() {
			if (ttable.getRowCount() > 0) {
				int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
					    "Are you sure?\n(All new uncommitted entries will be lost.)",
					    "Opening data",
					    JOptionPane.YES_NO_OPTION);
				if (res != JOptionPane.YES_OPTION) {
					return;
				}
			}
			
			try {
				WhistleToneConnectProcess wmDetector = null;
				if (wmatControl.getParams().slicedataSourceName != null) {
					PamDataBlock db = wmatControl.getPamController().getDetectorDataBlock(wmatControl.getParams().slicedataSourceName);
					if (db != null) wmDetector = (WhistleToneConnectProcess) db.getParentProcess();
				} else {
					wmatControl.simpleErrorDialog("A Whistle and Moan Detector contour source should be selected in the settings first.");
					return;
				}
				if (wmDetector == null) {
					wmatControl.simpleErrorDialog("No actual Whistle and Moan Detector module appears to be in this configuration. "
							+ "You should add one and ensure the sampling rate, FFT length and FFT hop match those that the contours "
							+ "in the binary files were processed with.");
					return;
				}
				
				backupIndexes = null;
				backupValues = null;
				undoButton.setEnabled(false);
				defaultloc = findBinaryStorePath();
				fileField.setText("Loading data from binary files...");
				File defFolder = new File(defaultloc);
				ArrayList<File> fileList = new ArrayList<File>();
				ArrayList<File> dirQueue = new ArrayList<File>();
				dirQueue.add(defFolder);
				
				binaryLoadingBarWindow = new WMATBinaryLoadingBarWindow(wmatControl.getGuiFrame());
				binaryLoadingBarThread = new BinaryLoadingBarThread();
				binaryLoadingBarThread.start();
				
				while (dirQueue.size() > 0) {
					File currDir = dirQueue.remove(0);
					File[] files = currDir.listFiles();
					for (int i = 0; i < files.length; i++) {
						if (files[i].isDirectory()) {
							//if (checkBinaryStoreSubfolderOption()) dirQueue.add(files[i]);
							dirQueue.add(files[i]); // Disabling it isn't even an option in Viewer Mode for some reason.
						} else if (files[i].getPath().endsWith(".pgdf")) {
							fileList.add(files[i]);
							binaryLoadingBarWindow.addOneToTotalFileCount();
						}
					}
				}
				
				if (fileList.size() == 0) {
					binaryLoadingBarWindow.setVisible(false);
					wmatControl.simpleErrorDialog("No Whistle and Moan Detector binary files were found in the specified "
							+ "binary file folder or any of its subfolders.");
					return;
				}
				
				clearTable();
				
				binaryFileDates = new ArrayList<String>();
				
				for (int i = 0; i < fileList.size(); i++) {
					try {
						PopulateTable(fileList.get(i), wmDetector);
					} catch (Exception e2) {
						System.out.println("Error while parsing "+fileList.get(i).getName()+".");
						e2.printStackTrace();
					}
					binaryLoadingBarWindow.addOneToLoadingBar();
				}
				binaryLoadingBarWindow.setVisible(false);
				fileField.setText(defaultloc);
				
			} catch (Exception e2) {
				fileField.setText("Load error - see console.");
				e2.printStackTrace();
				System.out.println("Error - tried to open at: " + defaultloc);
			}
			//updateFromFullTable(); // Seems to make updating not work at all for some reason.
			checkButton.setEnabled(false);
			updateButton.setEnabled(false);
			
			if(!(fileField.getText().equals("Load error - see console."))) {
				int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
					    "Connect to database as well? (Recommended)\n(This will turn on AutoCommit.)",
					    "Opening data",
					    JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.YES_OPTION) {
					try {
						testLogger.createColumnsAndFillTable(true);
						updateFromFullTable();
					} catch (Exception e2) {
						e2.printStackTrace();
						simpleErrorDialog();
					}
				}
			}
			updateLCPredictions();
			originalTable = getTableRowsAsArray();
			tableChangeLog = resetChangeLog(originalTable.length);
		}
	}
	
	/**
	 * Reads from a .pgdf file that contains Whistle and Moan Detector contours and loads them into the JTable.
	 * @param inpfile - The File being read from.
	 */
	protected void PopulateTable(File inpfile, WhistleToneConnectProcess wmDetector) {
		try {
			reader = new WMATBinaryReader(wmatControl, inpfile.getPath(), wmDetector);
		} catch (Exception e2){
			//fileField.setText("Load error - see console.");
			System.out.println("Could not find file: " + inpfile.getPath());
			return;
		}
		try {
			if (reader.bh.getModuleType().equals("WhistlesMoans") && reader.bh.getStreamName().equals("Contours")) {
				currformat = reader.bh.getHeaderFormat();
				BinaryObjectData curr = reader.nextData(currformat);
				int num = 0;
				while(true){
					num++;
					curr = reader.nextData(currformat);
					DataUnitBaseData dubd = null;
					if (currformat >= 3) {
						dubd = curr.getDataUnitBaseData();
					}
					if (curr.getObjectType() != -4){
						long detectionTime = MIRFEEControlledUnit.convertFromLocalToUTC(curr.getTimeMilliseconds());
						long fileTime = MIRFEEControlledUnit.convertFromLocalToUTC(reader.bh.getDataDate());
						Date detectionDate = new Date(detectionTime);
						Date fileDate = new Date(fileTime);
						
						String date_format = "yyyy-MM-dd HH:mm:ss+SSS";
						SimpleDateFormat currdateformat = new SimpleDateFormat(date_format);
						String detectionDateString = currdateformat.format(detectionDate);
						String fileDateString = currdateformat.format(fileDate);
						
						if (detectionDateString == null || fileDateString == null) continue;
						if (!binaryFileDates.contains(fileDateString))
							binaryFileDates.add(fileDateString);
						if (!(dubd.getUID() <= 0 && detectionDateString.equals("1970-01-01 00:00:00+000"))) {
							if (currformat > 3) {
								double[] freqs = dubd.getFrequency();
								//double dur = dubd.getSampleDuration(); //Millisecond version doesn't work
								//double amp = dubd.getCalculatedAmlitudeDB(); //THIS DOESN'T ACTUALLY WORK
								// THE ABOVE TWO VARIABLES ARE LOADED IN THROUGH THE DATABASE INSTEAD
								dtmodel.addRow(new Object[]{dubd.getUID(), detectionDateString,
										(int)freqs[0], (int)freqs[1], -1, -1, "", "", ""});
							} else  if (currformat == 3){
								dtmodel.addRow(new Object[]{dubd.getUID(), detectionDateString,
										-1, -1, -1, -1, "", "", ""});
							} else {
								dtmodel.addRow(new Object[]{-1, detectionDateString,
										-1, -1, -1, -1, "", "", ""});
							}
							crduMap.put(String.valueOf(dubd.getUID())+", "+detectionDateString,
									reader.getDataAsDataUnit(curr, reader.bh, reader.bh.getHeaderFormat()));
						}
					} else {
						break;
					}
				}
				binaryFileDates.sort(null);
			}
		} catch (NullPointerException npe) {
			System.out.println("Binary header not found in file: " + inpfile.getPath());
		} catch (Exception e2) {
			System.out.println("Error reading file: " + inpfile.getPath());
			e2.printStackTrace();
		}
		reader.closeReader();
	}
	
	private void updateFromSelectedRows() {
		WMATDataBlock db = (WMATDataBlock) wmatControl.getProcess().getOutputDataBlock(0);
		db.updateLC(false);
	}
	
	private void updateFromFullTable() {
		WMATDataBlock db = (WMATDataBlock) wmatControl.getProcess().getOutputDataBlock(0);
		db.updateLC(true);
	}
	
	/**
	 * Returns the selected file from a JFileChooser, including the extension from
	 * the file filter.
	 * 
	 * Copied from here: https://stackoverflow.com/questions/16846078/jfilechoosershowsavedialog-cant-get-the-value-of-the-extension-file-chosen
	 * Author page: https://stackoverflow.com/users/964243/boann
	 */
	public static File getSelectedFileWithExtension(JFileChooser c) {
	    File file = c.getSelectedFile();
	    if (c.getFileFilter() instanceof FileNameExtensionFilter) {
	        String[] exts = ((FileNameExtensionFilter)c.getFileFilter()).getExtensions();
	        String nameLower = file.getName().toLowerCase();
	        for (String ext : exts) { // check if it already has a valid extension
	            if (nameLower.endsWith('.' + ext.toLowerCase())) {
	                return file; // if yes, return as-is
	            }
	        }
	        // if not, append the first extension from the selected filter
	        file = new File(file.toString() + '.' + exts[0]);
	    }
	    return file;
	}
	
	/**
	 * The listener for the 'Import table' button.
	 */
	class ImportListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (ttable.getRowCount() == 0) {
				simpleErrorDialog(String.format("<html><body style='width: %1spx'>%1s", 300,
						"Binary data must be loaded in first."
						+ "\n(The purpose of this feature is to provide a backup for any labelling work "
						+ "in case the database connection ends, not to be a replacement for binary files or the database.)"));
				return;
			}
			int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
					String.format("<html><body style='width: %1spx'>%1s", 300,
					"This will match labelling data from .wmat, .csv or .txt files that have been exported from the WMAT. "
					+ "Any entries found in the selected file will be matched to binary data entries currently loaded into the table "
					+ "and will overwrite species, call type and comment data in the matched table entries. "
					+ "It will have no effect on any entries in the table that aren't present in the selected file."
					+ "\nProceed?"),
					"Whistle and Moan Annotation Tool",
					JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.NO_OPTION) {
				return;
			}
			fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("WMAT table export file (*.wmat)","wmat","wmnt"));
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
			int returnVal = fc.showOpenDialog(wmatControl.getGuiFrame());
			if (returnVal == JFileChooser.CANCEL_OPTION) {
				return;
			}
			File f = getSelectedFileWithExtension(fc);
			if (!f.exists()) {
				simpleErrorDialog("Selected file does not exist.");
				return;
			}
			int matched = 0;
			int notInTable = 0;
			int invalids = 0;
			Scanner sc;
			try {
				sc = new Scanner(f);
				if (f.getName().endsWith(".wmat") || f.getName().endsWith(".wmnt")) {
					sc.nextLine();
				}
				while (sc.hasNextLine()) {
					boolean found = false;
					String[] tokens;
					if (f.getName().endsWith(".wmat") || f.getName().endsWith(".wmnt")) {
						tokens = sc.nextLine().split(",");
					} else {
						tokens = sc.nextLine().split("\t");
					}
					if (tokens.length >= 6) {
						for (int i = 0; i < ttable.getRowCount(); i++) {
							if (tokens[0].equals(String.valueOf(ttable.getValueAt(i, 0))) &&
									tokens[1].equals(String.valueOf(ttable.getValueAt(i, 1)))) {
								ttable.setValueAt("", i, 6);
								ttable.setValueAt("", i, 7);
								ttable.setValueAt("", i, 8);
								if (tokens.length >= 7) {
									ttable.setValueAt(tokens[6], i, 6);
									if (tokens.length >= 8) {
										ttable.setValueAt(tokens[7], i, 7);
										if (tokens.length >= 9) {
											ttable.setValueAt(tokens[8], i, 8);
										}
									}
								}
								fixChangeLog(i, getOriginalIndex(i));
								if (!found) {
									matched++;
								}
								found = true;
							}
						}
						if (!found) {
							notInTable++;
						}
					} else {
						invalids++;
					}
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				simpleErrorDialog("Parsing error encountered while reading file.");
			}
			updateFromFullTable();
			JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
					String.format("<html><body style='width: %1spx'>%1s", 300,
					"Matched entries: "+String.valueOf(matched)
					+"\nUnmatched entries in table: "+String.valueOf(ttable.getRowCount()-matched)
					+"\nUnmatched entries in file: "+String.valueOf(notInTable)
					+"\nInvalid entries in file: "+String.valueOf(invalids)),
					"Whistle and Moan Annotation Tool",
					JOptionPane.INFORMATION_MESSAGE);
		}	
	}
	
	/**
	 * The listener for the 'Export table' button.
	 */
	class ExportListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if (ttable.getRowCount() == 0) {
				simpleErrorDialog("Table is currently empty.");
			}	
			fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("WMAT table export file (*.wmat)","wmat","wmnt"));
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
			//fc.addChoosableFileFilter(new FileNameExtensionFilter("Text file (*.txt)","txt"));
			int returnVal = fc.showSaveDialog(wmatControl.getGuiFrame());
			if (returnVal != JFileChooser.APPROVE_OPTION) return;
			File f = getSelectedFileWithExtension(fc);
			f.setWritable(true, false);
			if (f.exists()) {
				int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
						"Overwrite selected file?",
						"Whistle and Moan Annotation Tool",
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.YES_OPTION) {
					f.setExecutable(true);
				} else {
					return;
				}
			} else {
				try {
					f.createNewFile();
				} catch (Exception e2) {
					e2.printStackTrace();
					simpleErrorDialog("Could not create new file.\nSee console for details.");
					return;
				}
			}
			String fn = f.getName();
			if (fn.endsWith(".wmat") || fn.endsWith(".wmnt")) {
				try {
					PrintWriter pw = new PrintWriter(f);
					StringBuilder sb = new StringBuilder();
					sb.append("uid,datetime,lf,hf,duration,amplitude,species,calltype,comment,slicedata\n");
					pw.write(sb.toString());
					for (int i = 0; i < ttable.getRowCount(); i++) {
						sb = new StringBuilder();
						for (int j = 0; j < 9; j++) {
							sb.append(ttable.getValueAt(i, j).toString());
							if (j < 8) {
								sb.append(",");
								continue;
							}
							ConnectedRegionDataUnit crdu = 
									crduMap.get(ttable.getValueAt(i, 0).toString()+", "
											+ttable.getValueAt(i, 1).toString());
							WhistleToneConnectProcess wmDetector = null;
							if (wmatControl.getParams().slicedataSourceName != null) {
								PamDataBlock db = 
										wmatControl.getPamController().getDetectorDataBlock(
												wmatControl.getParams().slicedataSourceName);
								if (db != null) {
									wmDetector = (WhistleToneConnectProcess) db.getParentProcess();
									FFTDataBlock fftDB = (FFTDataBlock) wmDetector.getParentDataBlock();
									WMATSliceDataCalculator calc =
											new WMATSliceDataCalculator(crdu.getConnectedRegion().getSliceData(),
											(int) wmDetector.getSampleRate(), fftDB.getFftLength());
									
									double[] timestamps = calc.getTimestampsInSeconds();
									double[] freqs = calc.getFreqs();
									// TODO testing stuff
								/*	BasicFragmentStore bfs = new BasicFragmentStore((int) wmDetector.getSampleRate());
									WhistleFragment contour = new WhistleFragment(timestamps, freqs);
									WhistleFragmenter fragmenter = new OverlappingFragmenter();
									fragmenter.setFragmentLength(3);
									WhistleFragment[] fragments = fragmenter.getFragments(contour);
									for (int k = 0; k < fragments.length; k++) {
										//System.out.print(String.valueOf(fragments[k].getTimesInSeconds().length)+" ");
										bfs.addFragemnt(fragments[k]);
									}
									double[] paramArr = bfs.getParameterArray();
									String printoutp = ttable.getValueAt(i, 0)+": ";
									if (paramArr != null) {
										for (int k = 0; k < paramArr.length; k++)
											printoutp += String.valueOf(paramArr[k])+" ";
									} else {
										printoutp += "null";
									}
									System.out.println(printoutp); */
									
									for (int k = 0; k < freqs.length; k++)
										sb.append(","+String.valueOf(calc.sliceDataArr[k].getStartSample())+
												">"+String.valueOf(freqs[k]));
									//System.out.println(ttable.getValueAt(i, 0).toString()+": "+
									//		String.valueOf(calc.calculateFreqElbowAngle()));
								}
							}
							sb.append("\n");
						}
						pw.write(sb.toString());
						pw.flush();
					}
					pw.close();
					JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
							"Table successfully written to .wmat file.",
							"Whistle and Moan Annotation Tool",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e2) {
					e2.printStackTrace();
					simpleErrorDialog();
					return;
				}
			} else if (fn.endsWith(".csv")) {
				try {
					PrintWriter pw = new PrintWriter(f);
					StringBuilder sb = new StringBuilder();
					sb.append("uid,datetime,lf,hf,duration,amplitude,species,calltype,comment,prediction,score\n");
					pw.write(sb.toString());
					for (int i = 0; i < ttable.getRowCount(); i++) {
						sb = new StringBuilder();
						for (int j = 0; j < ttable.getColumnCount(); j++) {
							if (ttable.getValueAt(i, j) != null)
								sb.append(ttable.getValueAt(i, j).toString());
							else
								sb.append("");
							if (j < ttable.getColumnCount()-1)
								sb.append(",");
							else
								sb.append("\n");
						}
						pw.write(sb.toString());
						pw.flush();
					}
					pw.close();
					JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
							"Table successfully written to .csv file.",
							"Whistle and Moan Annotation Tool",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e2) {
					e2.printStackTrace();
					simpleErrorDialog();
					return;
				}
			}
		}
	}
	
	/**
	 * The listener for the 'Check database for alignment' button.
	 */
	class CheckListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (testLogger != null) {
				testLogger.checkAlignment(ttable);
			}
		}
	}
	
	class CountListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			WMATCountDialog countDialog = new WMATCountDialog(wmatControl.getGuiFrame(), wmatControl, ttable);
			countDialog.setVisible(true);
		}
	}
	
	protected class UpdateThread extends Thread {
		protected UpdateThread() {}
		@Override
		public void run() {
			if (testLogger != null) {
				int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
					    MIRFEEControlledUnit.makeHTML("Would you like to commit all changes now?\n"
							    + "(This includes database changes made by other modules. If you press \"no\", the SQL update commands\n"
							    + "will be executed, but won't be committed until you press \"File > Save Data\". If AutoCommit was\n"
							    + "turned on at startup, the commit will happen automatically regardless.)", 350),
					    wmatControl.getUnitName(),
					    JOptionPane.YES_NO_CANCEL_OPTION);
				if (res == JOptionPane.CANCEL_OPTION) return;
				fileField.setText("Committing to database...");
				//testLogger.openConnection();
				testLogger.executeUpdates(ttable, originalTable, tableChangeLog, res == JOptionPane.YES_OPTION);
				originalTable = testLogger.getOriginalTable();
				tableChangeLog = testLogger.getChangeLog();
				//testLogger.closeConnection();
				fileField.setText(defaultloc);
			} else {
				wmatControl.simpleErrorDialog("Error: SQL logging function was not created.");
			}
		}
	}
	
	/**
	 * The listener for the 'Commit to database' button.
	 */
	class UpdateListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			UpdateThread updateThread = new UpdateThread();
			updateThread.start();
		}
	}
	
	/**
	 * The listener for the 'Scroll to selection on spectrogram' button.
	 */
	class ScrollListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (ttable.getRowCount() > 0) {
				if (ttable.getSelectedRow() > -1) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SSS");
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					String fromTable = ttable.getValueAt(ttable.getSelectedRow(), 1).toString();
					//fromTable = wmatControl.convertBetweenTimeZones("UTC", MIRFEEControlledUnit.getLocalTimeZoneName(), fromTable, true);
					try {
						Date date = df.parse(fromTable);
						long outpTime = date.getTime();
						ViewerScrollerManager vsm = (ViewerScrollerManager) AbstractScrollManager.getScrollManager();
						for (int i = 0; i < vsm.getPamScrollers().size(); i++) {
							AbstractPamScroller scroller = vsm.getPamScrollers().get(i);
							long duration = scroller.getMaximumMillis() - scroller.getMinimumMillis();
							int buffer = wmatControl.getParams().scrollBuffer;
							scroller.anotherScrollerMovedOuter(outpTime-buffer, outpTime+duration-buffer);
							//System.out.println(MIRFEEControlledUnit.convertDateLongToString(scroller.getMinimumMillis()));
						}
						vsm.loadData(true);
					} catch (ParseException e1) {
						e1.printStackTrace();
						simpleErrorDialog();
					}
					return;
				}
			}
			JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
					"No contour has been selected from the table.",
				    "Whistle and Moan Annotation Tool",
				    JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * The listener for the 'Undo' button.
	 */
	class UndoListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			undo();
		}
	}
	
	/**
	 * Saves the selected values in 'backupIndexes' and their index numbers in 'backupValues'. 
	 */
	private void createBackup(boolean selectedOnly, boolean fromUndoButton) {
		if (ttable.getRowCount() > 0 && !(selectedOnly && ttable.getSelectedRowCount() == 0)) {
			// NOTE: getSelectedRows() does NOT actually cause any issues if the rows are re-arranged.
			if (selectedOnly)
				backupIndexes = ttable.getSelectedRows();
			else {
				backupIndexes = new int[ttable.getRowCount()];
				for (int i = 0; i < ttable.getRowCount(); i++)
					backupIndexes[i] = i;
			}
			backupValues = new Object[backupIndexes.length][ttable.getModel().getColumnCount()];
			for (int i = 0; i < backupIndexes.length; i++) {
				for (int j = 0; j < ttable.getModel().getColumnCount(); j++) {
					backupValues[i][j] = ttable.getValueAt(backupIndexes[i], j);
				}
			}
			if (!fromUndoButton || undoButton.getText().equals("Redo"))
				undoButton.setText("Undo");
			else
				undoButton.setText("Redo");
			undoButton.setEnabled(true);
		}
	}
	
	/**
	 * Undoes the last change to the table. Currently only goes back once.
	 */
	private void undo() {
		if (backupIndexes != null && backupValues != null) {
			int[] oldIndexes = backupIndexes.clone();
			// NOTE: This still works even if you sort by a column.
			Object[][] oldValues = backupValues.clone();
			oldIndexes = new int[oldIndexes.length];
			for (int i = 0; i < oldIndexes.length; i++) {
				for (int j = 0; j < ttable.getModel().getRowCount(); j++) {
					if (ttable.getValueAt(j,0).equals(oldValues[i][0]) &&
					    ttable.getValueAt(j,1).equals(oldValues[i][1])) {
						oldIndexes[i] = j;
						break;
					}
				}
			}
			ttable.clearSelection();
			for (int i = 0; i < oldIndexes.length; i++)
				ttable.addRowSelectionInterval(oldIndexes[i], oldIndexes[i]);
			createBackup(true, true);
			for (int i = 0; i < oldIndexes.length; i++) {
				ttable.setValueAt(oldValues[i][6], oldIndexes[i], 6);
				ttable.setValueAt(oldValues[i][7], oldIndexes[i], 7);
				ttable.setValueAt(oldValues[i][8], oldIndexes[i], 8);
				ttable.addRowSelectionInterval(oldIndexes[i], oldIndexes[i]);
				fixChangeLog(oldIndexes[i], getOriginalIndex(oldIndexes[i]));
			}
			updateFromSelectedRows();
			return;
		}
		undoButton.setEnabled(false);
	}
	
	/**
	 * Streamlined error dialog.
	 */
	public void simpleErrorDialog() {
		JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
			"An error has occured.\nSee console for details.",
			"",
			JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Streamlined error dialog with an editable message.
	 */
	public void simpleErrorDialog(String inptext) {
		JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
			inptext,
			"",
			JOptionPane.ERROR_MESSAGE);
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
	 * Extension of the LimitedPlainDocument below that prevents commas from being input,
	 * in addition to keeping input strings below a certain length.
	 */
	protected class CommaRemovingDocument extends LimitedPlainDocument {
		public CommaRemovingDocument(int maxLen) {
			this.maxLen = maxLen; 
		}
		
		@Override
		public void insertString(int param, String str, javax.swing.text.AttributeSet attributeSet) 
				 throws javax.swing.text.BadLocationException {
			if (str == null) return;
			super.insertString(param, str.replaceAll(",", ""), attributeSet);
		}
	}
	
	protected class BackupCellEditor extends DefaultCellEditor {

		public BackupCellEditor(JTextField textField) {
			super(textField);
		}
		
		public void publicFireEditingStopped() {
			fireEditingStopped();
		}
		
		@Override
		public boolean stopCellEditing() {
			createBackup(true, false);
			return super.stopCellEditing();
		}
		
	}
	
	protected class CellEditorLoggingListener implements CellEditorListener {
		
		@Override
		public void editingStopped(ChangeEvent e) {
			// Backup done through BackupCellEditor
			int[] selrows = ttable.getSelectedRows();
			if (selrows.length == 0) return;
			for(int i = 0; i < selrows.length; i++) // Only because it's possible for multiple cells to be selected while you're only editing one
				fixChangeLog(selrows[i], getOriginalIndex(selrows[i]));
			updateFromSelectedRows();
		}

		@Override
		public void editingCanceled(ChangeEvent e) {}
		
	}
	
	/**
	 * Sets a limit to the number of characters allowed in a JTable cell.
	 * Copied from: https://stackoverflow.com/questions/28779236/how-to-make-jtable-cell-character-length
	 * Author page: https://stackoverflow.com/users/4617078/bigminimus
	 */
	private class LimitedPlainDocument extends javax.swing.text.PlainDocument {
		 protected int maxLen = -1;  
		 public LimitedPlainDocument() {}
		 public LimitedPlainDocument(int maxLen) { this.maxLen = maxLen; }
		 public void insertString(int param, String str, javax.swing.text.AttributeSet attributeSet) 
				 throws javax.swing.text.BadLocationException {
			 if (str != null && maxLen > 0 && this.getLength() + str.length() > maxLen) {
				 //java.awt.Toolkit.getDefaultToolkit().beep();
				 return;
			 }
			 super.insertString(param, str, attributeSet);
		 }
	}
	
	/**
	 * Finds the path to the binary store.
	 * Copied from PamController as it's private in the library for some reason.
	 * @return The path to the binary store (String).
	 * @author Doug Gillespie (in PamController)
	 */
	public String findBinaryStorePath() {
		BinaryStore binaryControl = BinaryStore.findBinaryStoreControl();
		if (binaryControl == null) {
			return null;
		}
		String storeLoc = binaryControl.getBinaryStoreSettings().getStoreLocation();
		if (storeLoc == null) {
			return "";
		}
		if (storeLoc.endsWith(File.separator) == false) {
			storeLoc += File.separator;
		}
		return storeLoc;
	}
	
	public boolean checkBinaryStoreSubfolderOption() {
		BinaryStore binaryControl = BinaryStore.findBinaryStoreControl();
		if (binaryControl == null) {
			return false;
		}
		return binaryControl.getBinaryStoreSettings().datedSubFolders;
	}
	
	/**
	 * Returns all rows in the table as a 2D Object array.
	 * @return The table as a 2D Object array (Object[][]).
	 */
	public Object[][] getTableRowsAsArray(){
		Object[][] outp = new Object[ttable.getModel().getRowCount()][ttable.getModel().getColumnCount()];
		for (int i = 0; i < ttable.getModel().getRowCount(); i++) {
			for (int j = 0; j < ttable.getModel().getColumnCount(); j++) {
				outp[i][j] = ttable.getModel().getValueAt(i, j);
			}
		}
		return outp;
	}
	
	/**
	 * Returns original index of row in table.
	 * @param rowIndex - The current index of the row in the table.
	 * @return The original index of the row in the initial table before any sorting (Integer).
	 */
	public int getOriginalIndex(int rowIndex) {
		Object[] row = new Object[2];
		row[0] = ttable.getValueAt(rowIndex, 0);
		row[1] = ttable.getValueAt(rowIndex, 1);
		for (int i = 0; i < originalTable.length; i++) {
			if (originalTable[i][0].equals(row[0]) && originalTable[i][1].equals(row[1])) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Returns an array of false booleans.
	 * @param len - Desired length of array.
	 * @return An array of false booleans (boolean[]).
	 */
	public boolean[] resetChangeLog(int len) {
		boolean[] outp = new boolean[len];
		for (int i = 0; i < len; i++) {
			outp[i] = false;
		}
		return outp;
	}
	
	/**
	 * Map of ConnectedRegionDataUnits that match table entries via
	 * "[uid], [timestamp]" as the key. Mainly meant for dealing with
	 * slice data.
	 */
	public HashMap<String, ConnectedRegionDataUnit> getCRDUMap(){
		return crduMap;
	}
	
	/**
	 * Compares the current values of a row in the table against the values of the same row in the database.
	 * If all values are identical, its respective boolean in tableChangeLog is set to false; otherwise it is set to true.
	 * @param currIndex - Index of row in current table (in case of sorting).
	 * @param origIndex - Index of row in initial table.
	 */
	public void fixChangeLog(int currIndex, int origIndex) {
		if (currIndex > -1 && origIndex > -1) {
			if (ttable.getValueAt(currIndex, 6).equals(originalTable[origIndex][6]) &&
				ttable.getValueAt(currIndex, 7).equals(originalTable[origIndex][7]) &&
				ttable.getValueAt(currIndex, 8).equals(originalTable[origIndex][8])) {
				tableChangeLog[origIndex] = false;
			} else {
				tableChangeLog[origIndex] = true;
			}
		} else {
			System.out.println("Indices out of bounds.");
		}
	}
	
	public void updateLCPredictions() {
		WMATParameters params = wmatControl.getParams();
		if (params.predictionSourceName == null || ttable.getRowCount() == 0)
			return;
		LCDataBlock db = null;
		for (int i = 0; i < wmatControl.getPamController().getDataBlocks().size(); i++) {
			PamDataBlock curr = wmatControl.getPamController().getDataBlocks().get(i);
			//System.out.println(curr.getLongDataName()+" -> "+params.predictionSourceName);
			if (curr.getLongDataName().equals(params.predictionSourceName)) {
				db = (LCDataBlock) curr;
				break;
			}
		}
		if (db == null) {
			System.out.println("ERROR: Selected prediction data source not found.");
			return;
		}
		HashMap<String, Integer> rowMap = new HashMap<String, Integer>();
		for (int i = 0; i < ttable.getRowCount(); i++)
			rowMap.put(String.valueOf(ttable.getValueAt(i, 0))+", "+((String) ttable.getValueAt(i, 1)), i);
		for (int i = 0; i < db.getUnitsCount(); i++) {
			LCDataUnit du = db.getDataUnit(i, db.REFERENCE_ABSOLUTE);
			LCCallCluster cc = du.getCluster();
			for (int j = 0; j < cc.getSize(); j++) {
				String key = String.valueOf(cc.uids[j])+", "
								+MIRFEEControlledUnit.convertDateLongToString(MIRFEEControlledUnit.convertFromLocalToUTC(cc.datetimes[j]));
				//System.out.println(key);
				Integer index = rowMap.get(key);
				if (index == null) continue;
				if (params.predictionContext == params.USE_CLUSTER_PREDICTIONS) {
					ttable.setValueAt(cc.getPredictedSpeciesString(), index.intValue(), 9);
					ttable.setValueAt(String.format("%.2f", cc.getCertainty()), index.intValue(), 10);
				} else { // USE_CONTOUR_PREDICTIONS
					ttable.setValueAt(cc.getIndividualPredictedSpeciesString(j), index.intValue(), 9);
					ttable.setValueAt(String.format("%.2f", cc.getIndividualCertainty(j)), index.intValue(), 10);
				}
			}
		}
	}
	
	// Kudos to jdiver at: https://stackoverflow.com/questions/23814282/change-the-font-color-in-a-specific-cell-of-a-jtable
	public static class CustomTableRenderer extends DefaultTableCellRenderer {
		
		protected WMATControl wmatControl;
		public CustomTableRenderer(WMATControl wmatControl) {
			this.wmatControl = wmatControl;
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (isSelected) {
				c.setBackground(new Color(0,140,255));
				c.setForeground(Color.WHITE);
			} else {
				WMATParameters wmatParams = wmatControl.getParams();
				c.setBackground(PamColors.getInstance().getBorderColour());
				c.setForeground(Color.BLACK);
				if (wmatParams.predictionSourceName != null) {
					LCDataBlock db = null;
					for (int i = 0; i < wmatControl.getPamController().getDataBlocks().size(); i++) {
						PamDataBlock curr = wmatControl.getPamController().getDataBlocks().get(i);
						//System.out.println(curr.getLongDataName()+" -> "+params.predictionSourceName);
						if (curr.getLongDataName().equals(wmatParams.predictionSourceName)) {
							db = (LCDataBlock) curr;
							break;
						}
					}
					if (db != null) {
						LCParameters lcParams = db.getParamsClone();
						if (column == 6 && value != null && ((String) value).length() > 0 && table.getValueAt(row, 9) != null) {
							// TODO Fix this when you add "umbrella classes"
							String speciesValue = (String) table.getValueAt(row, 9);
							String aliasValue = lcParams.labelAliases.get((String) value);
							if (((String) value).equals(speciesValue) || (aliasValue != null && aliasValue.equals(speciesValue)))
								c.setBackground(new Color(150,250,150));
							else
								c.setBackground(new Color(250,150,150));
						} else if (column == 9 && value != null) {
							Color color = lcParams.labelColours.get((String) value);
							if (color != null) {
								c.setBackground(color);
								if (color.getRed() >= 200 || color.getGreen() >= 200)
									c.setForeground(Color.BLACK);
								else
									c.setForeground(Color.WHITE);
							}
						} else if (column == 10 && value != null) {
							double score = Double.valueOf((String) value);
							if (score < lcParams.low)
								c.setBackground(new Color(250,150,150));
							else if (score < lcParams.average)
								c.setBackground(new Color(250,250,150));
							else
								c.setBackground(new Color(150,250,150));
						}
					}
				}
			}
			return c;
		}
	}
	
	/**
	 * Returns the current format of the last loaded binary file. Default is 6.
	 * @return int - The current format of the loaded binary files.
	 */
	public int getCurrFormat() {
		return currformat;
	}
	
	/**
	 * @return mainPanel
	 */
	public JComponent getComponent() {
		return mainPanel;
	}

}
