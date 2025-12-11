package wmat;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.sqlite.SQLiteConnection;

import PamController.PamController;
import PamView.PamTable;
import PamView.dialog.PamGridBagContraints;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import generalDatabase.DBProcess;
import generalDatabase.DBSystem;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableItem;
import mirfee.MIRFEEControlledUnit;

/**
 * Used to read from and write to the 'whistle_and_moan_detector' (or otherwise re-named) table in the database.
 * @author Holly LeBlond
 */
public class WMATSQLLogging {
	
	public static final int SPECIES_CHAR_LENGTH = 20;
	public static final int CALLTYPE_CHAR_LENGTH = 20;
	public static final int COMMENT_NVARCHAR_LENGTH = 400;
	
	protected WMATControl wmatControl;
	
	protected Set<String> tableSet;
	protected Set<String> dbSet;
	protected List<String> tableList;
	protected List<String> dbList;
	
	protected Object[][] originalTable;
	protected boolean[] tableChangeLog;
	
	protected volatile WMATDatabaseLoadingBarWindow databaseLoadingBarWindow;
	protected volatile DatabaseLoadingBarThread databaseLoadingBarThread;
	
	protected volatile WMATCommitLoadingBarWindow commitLoadingBarWindow;
	protected volatile CommitLoadingBarThread commitLoadingBarThread;
	
	public WMATSQLLogging(WMATControl wmatControl) {
		this.wmatControl = wmatControl;
	}
	
	/**
	 * Adds annotation columns to database table if they aren't already present and fills the table in WMATPanel.
	 * @param overwrite - Whether or not to overwrite the values currently in the WMATPanel's table.
	 * @return True if columns were created and the table was filled. Otherwise, false.
	 */
	public boolean createColumnsAndFillTable(boolean overwrite) {
		databaseLoadingBarWindow = new WMATDatabaseLoadingBarWindow(wmatControl.getGuiFrame());
		databaseLoadingBarThread = new DatabaseLoadingBarThread();
		databaseLoadingBarThread.start();
		Statement stmt = null;
		ResultSet rsColumns = null;
		try {
			getDBControl().getDbParameters().setUseAutoCommit(true);
			if (isMySQL()) {
				stmt = getDBSystem().getConnection().getConnection().createStatement();
				rsColumns = stmt.executeQuery("SHOW COLUMNS FROM "+wmatControl.getParams().sqlTableName+";");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				if (rsColumns != null && !rsColumns.isClosed()) rsColumns.close();
				if (stmt != null && !stmt.isClosed()) stmt.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			wmatControl.getSidePanel().getWMATPanel().checkButton.setEnabled(false);
			wmatControl.getSidePanel().getWMATPanel().updateButton.setEnabled(false);
			databaseLoadingBarWindow.setVisible(false);
			JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
				"Could not connect to database - see console for details.\nIf this problem persists, try restarting PamGuard.",
			    "Database error",
			    JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		PamTable ttable = wmatControl.getSidePanel().getWMATPanel().ttable;
		tableSet = new HashSet<String>();
		tableList = new ArrayList<String>();
		for (int i = 0; i < ttable.getRowCount(); i++) {
			String entry = ttable.getValueAt(i, 0).toString() + " - "
					+ ttable.getValueAt(i, 1).toString().substring(0, 19);
			tableSet.add(entry);
			tableList.add(entry);
		}
		
		if (isMySQL()) {
			boolean createSpeciesColumn = false;
			boolean createCallTypeColumn = false;
			boolean createCommentColumn = false;
			try {
				while (rsColumns.next()) {
					if (rsColumns.getString(1).equals("species")) createSpeciesColumn = true;
					else if (rsColumns.getString(1).equals("callType")) createCallTypeColumn = true;
					else if (rsColumns.getString(1).equals("comment")) createCommentColumn = true;
				}
				if (!createSpeciesColumn)
					stmt.executeUpdate("ALTER TABLE "+wmatControl.getParams().sqlTableName+"\r\nADD COLUMN species CHAR("+String.valueOf(SPECIES_CHAR_LENGTH)+");");
				if (!createCallTypeColumn)
					stmt.executeUpdate("ALTER TABLE "+wmatControl.getParams().sqlTableName+"\r\nADD COLUMN callType CHAR("+String.valueOf(CALLTYPE_CHAR_LENGTH)+");");
				if (!createCommentColumn)
					stmt.executeUpdate("ALTER TABLE "+wmatControl.getParams().sqlTableName+"\r\nADD COLUMN comment NVARCHAR("+String.valueOf(COMMENT_NVARCHAR_LENGTH)+");");
				rsColumns.close();
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
				try {
					if (rsColumns != null && !rsColumns.isClosed()) rsColumns.close();
					if (stmt != null && !stmt.isClosed()) stmt.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				databaseLoadingBarWindow.setVisible(false);
				JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
						"Could not create new columns - see console for details.",
					    "Database error",
					    JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} else {
			int res = JOptionPane.showConfirmDialog(wmatControl.getGuiFrame(),
				    "Due to locking issues in SQLite, all pre-existing database changes from other modules need to be committed\n"
				    + "in order to create the annotation columns in the table.\n\n"
				    + "Proceed?",
				    "Creating columns",
				    JOptionPane.OK_CANCEL_OPTION);
			if (res == JOptionPane.CANCEL_OPTION) {
				databaseLoadingBarWindow.setVisible(false);
				return false;
			}
			EmptyTableDefinition edt = new EmptyTableDefinition(wmatControl.getParams().sqlTableName);
			if (!getDBProcess().checkColumn(edt, new PamTableItem("species", Types.CHAR, SPECIES_CHAR_LENGTH)) ||
					!getDBProcess().checkColumn(edt, new PamTableItem("callType", Types.CHAR, CALLTYPE_CHAR_LENGTH)) ||
					!getDBProcess().checkColumn(edt, new PamTableItem("comment", Types.VARCHAR, COMMENT_NVARCHAR_LENGTH))) {
				databaseLoadingBarWindow.setVisible(false);
				JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
						"Could not create new columns - see console for details.",
					    "Database error",
					    JOptionPane.ERROR_MESSAGE);
				return false;
			}
			getDBControl().commitChanges();
		}
		
		ResultSet rs = null;
		try {
			stmt = getDBSystem().getConnection().getConnection().createStatement();
			rs = stmt.executeQuery("SELECT * FROM "+wmatControl.getParams().sqlTableName+";");
		/*	rs.last(); // Doesn't work with SQLite.
			databaseLoadingBarWindow.startReadingCount(rs.getRow());
			rs.first(); */
			int rowCount = 0;
			while (rs.next()) {
				rowCount++;
			}
			databaseLoadingBarWindow.startReadingCount(rowCount);
			rs = stmt.executeQuery("SELECT * FROM "+wmatControl.getParams().sqlTableName+";");
			dbSet = new HashSet<String>();
			dbList = new ArrayList<String>();
			int rownum = -1;
			while (rs.next()) {
				try {
					String datetime = convertDate(String.valueOf(rs.getTimestamp("UTC")).substring(0, 19),
							rs.getShort("UTCMilliseconds"), false);
					String entry = String.valueOf(rs.getLong("UID")) + " - " + datetime;
					dbSet.add(entry);
					dbList.add(entry);
					rownum = findRow(String.valueOf(rs.getLong("UID")), datetime);
					WMATAnnotationInfo ai = new WMATAnnotationInfo();
					if (rownum != -1 && overwrite == true) {
						if (rs.getInt("startSample") > -1 && rs.getDouble("startSeconds") > -1 && rs.getInt("duration") > -1) {
							int sr = (int) (rs.getInt("startSample") / rs.getDouble("startSeconds"));
							int dur = (int) (1000 * (double) rs.getInt("duration") / sr);
							ttable.setValueAt(dur, rownum, 4);
						} else {
							ttable.setValueAt(-1, rownum, 4);
						}
						if (rs.getDouble("amplitude") > -1) {
							ttable.setValueAt((int) rs.getDouble("amplitude"), rownum, 5);
						} else {
							ttable.setValueAt(-1, rownum, 5);
						}
						if (rs.getString("species") != null) {
							ttable.setValueAt(rs.getString("species"), rownum, 6);
							ai.species = rs.getString("species");
							if (wmatControl.getSidePanel().getWMATPanel().speciesModel.getIndexOf(rs.getString("species")) == -1) {
								wmatControl.getSidePanel().getWMATPanel().speciesModel.addElement(rs.getString("species"));
							}
						} else {
							ttable.setValueAt("", rownum, 6);
						}
						if (rs.getString("callType") != null) {
							ttable.setValueAt(rs.getString("callType"), rownum, 7);
							ai.callType = rs.getString("callType");
							if (wmatControl.getSidePanel().getWMATPanel().calltypeModel.getIndexOf(rs.getString("callType")) == -1) {
								wmatControl.getSidePanel().getWMATPanel().calltypeModel.addElement(rs.getString("callType"));
							}
						} else {
							ttable.setValueAt("", rownum, 7);
						}
						if (rs.getString("comment") != null) {
							ttable.setValueAt(rs.getString("comment"), rownum, 8);
							ai.comment = rs.getString("comment");
						} else {
							ttable.setValueAt("", rownum, 8);
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				databaseLoadingBarWindow.addOneToLoadingBar();
			}
			rs.close();
			stmt.close();
			wmatControl.getSidePanel().getWMATPanel().checkButton.setEnabled(true);
			wmatControl.getSidePanel().getWMATPanel().updateButton.setEnabled(true);
		} catch(SQLException e) {
			e.printStackTrace();
			try {
				if (rs != null && !rs.isClosed()) rs.close();
				if (stmt != null && !stmt.isClosed()) stmt.close();
			} catch (HeadlessException | SQLException e1) {
				e1.printStackTrace();
			}
			databaseLoadingBarWindow.setVisible(false);
			JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
					"Error encountered while attempting to fill table - see console for details.",
				    "Database error",
				    JOptionPane.ERROR_MESSAGE);
			return false;
		}
		databaseLoadingBarWindow.setVisible(false);
		return true;
	}

	/**
	 * Finds the index of the row in the table with matching UID and date values.
	 * Returns -1 if such row is not present.
	 */
	protected int findRow(String uid, String date) {
		PamTable ttable = wmatControl.getSidePanel().getWMATPanel().ttable;
		for (int i = 0; i < ttable.getRowCount(); i++) {
			if (ttable.getValueAt(i, 0).toString().equals(uid) &&
					ttable.getValueAt(i, 1).toString().substring(0, 19).equals(date)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Fixes the rounding discrepancy for date/time between the binary files and database
	 * and converts time zones. (Note that this is only a problem in MySQL, NOT SQLite.)
	 * Unfortunately, the binary files appear to have the correct time, not the database.
	 * @param inpdate - Date being converted (String).
	 * @param mill - The milliseconds attached to the date (Short).
	 * @param writing - False removes a second and converts to UTC. True adds a second and converts from UTC.
	 * @return The converted date (String).
	 */
	protected String convertDate(String inpdate, short mill, boolean writing) {
		String conv;
		String tzName;
		if (wmatControl.getParams().databaseUTCColumnIsInLocalTime)
			tzName = MIRFEEControlledUnit.getLocalTimeZoneName();
		else tzName = "UTC";
		if (!writing) {
			conv = wmatControl.convertBetweenTimeZones(tzName, "UTC", inpdate.substring(0, 19), false);
		} else {
			conv = wmatControl.convertBetweenTimeZones("UTC", tzName, inpdate.substring(0, 19), false);
		}
		if (!isMySQL() || mill < 500) {
			return conv;
		}
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(df.parse(conv));
			if (!writing) { //DB to table
				cal.add(Calendar.SECOND, -1);
			} else { //Table to DB
				cal.add(Calendar.SECOND, 1);
			}
			return df.format(cal.getTime());
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	/**
	 * For when the 'Check database for alignment' button is pressed.
	 * @param outpTable
	 */
	public void checkAlignment(PamTable outpTable) {
		DefaultListModel<String> tableModel = new DefaultListModel<>();
		JList<String> tableList2 = new JList<String>(tableModel);
		DefaultListModel<String> dbModel = new DefaultListModel<>();
		JList<String> dbList2 = new JList<String>(dbModel);
		for (int i = 0; i < tableList.size(); i++) {
			if (!(dbSet.contains(tableList.get(i)))){
				String entry = tableList.get(i);
				tableModel.addElement(entry);
			}
		}
		for (int i = 0; i < dbList.size(); i++) {
			if (!(tableSet.contains(dbList.get(i)))){
				String entry = dbList.get(i);
				dbModel.addElement(entry);
			}
		}
		
		//Shoutout to this: https://stackoverflow.com/questions/7861724/is-there-a-word-wrap-property-for-jlabel/7861833#7861833
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		String introstr = "The following lists show detections present in either the binary files or the database that are not present in the other.";
		String html = "<html><body style='width: %1spx'>%1s";
		JLabel intro = new JLabel(String.format(html, 300, introstr));
		c.gridwidth = 2;
		mainPanel.add(intro, c);
		c.gridwidth = 1;
		c.gridy++;
		JPanel tablePanel = new JPanel(new GridBagLayout());
		tablePanel.setBorder(new TitledBorder("Present only in binary"));
		JScrollPane sp1 = new JScrollPane(tableList2);
		sp1.setPreferredSize(new Dimension(200,300));
		tablePanel.add(sp1);
		mainPanel.add(tablePanel, c);
		c.gridx++;
		JPanel dbPanel = new JPanel(new GridBagLayout());
		dbPanel.setBorder(new TitledBorder("Present only in database"));
		JScrollPane sp2 = new JScrollPane(dbList2);
		sp2.setPreferredSize(new Dimension(200,300));
		dbPanel.add(sp2);
		mainPanel.add(dbPanel, c);
		JOptionPane.showMessageDialog(wmatControl.getGuiFrame(),
				mainPanel,
				"Checking alignment",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	protected class DatabaseLoadingBarThread extends Thread {
		protected DatabaseLoadingBarThread() {}
		@Override
		public void run() {
			databaseLoadingBarWindow.setVisible(true);
		}
	}
	
	/**
	 * Thread used for running the loading bar window.
	 */
	protected class CommitLoadingBarThread extends Thread {
		protected CommitLoadingBarThread() {}
		@Override
		public void run() {
			commitLoadingBarWindow.setVisible(true);
		}
	}
	
	/**
	 * Executes update commands for any changes made in the WMATPanel's table.
	 * @param outpTable - The WMATPanel's PamTable
	 * @param origTable - The original contents of the PamTable
	 * @param changeLog - Which rows in the PamTable have been changed
	 * @param commitNow - If true, PAMGuard's database module commits the changes after the update commands have been executed
	 */
	public void executeUpdates(PamTable outpTable, Object[][] origTable, boolean[] changeLog, boolean commitNow) {
		int totalChanges = 0;
		for (int i = 0; i < changeLog.length; i++) {
			if (changeLog[i] == true) {
				totalChanges++;
			}
		}
		commitLoadingBarWindow = new WMATCommitLoadingBarWindow(wmatControl, wmatControl.getGuiFrame(), totalChanges);
		commitLoadingBarThread = new CommitLoadingBarThread();
		commitLoadingBarThread.start();
		int y = 0;
		int n = 0;
		DBControl dbControl = (DBControl) PamController.getInstance().findControlledUnit(DBControl.getDbUnitType());
		DBSystem mySystem = dbControl.getDatabaseSystem();
		System.out.println("\nUPDATING DATABASE: "+mySystem.getDatabaseName());
		for (int i = 0; i < outpTable.getRowCount(); i++) {
			if (changeLog[getOriginalIndex(i,outpTable,origTable)] == true) {
				String outpSql = "UPDATE "+wmatControl.getParams().sqlTableName+" SET species = ";
				if (outpTable.getValueAt(i, 6).toString().length() > 0) {
					outpSql = outpSql + "\"" + outpTable.getValueAt(i, 6).toString() + "\", callType = ";
				} else {
					outpSql = outpSql + "null, callType = ";
				}
				if (outpTable.getValueAt(i, 7).toString().length() > 0) {
					outpSql = outpSql + "\"" + outpTable.getValueAt(i, 7).toString() + "\", comment = ";
				} else {
					outpSql = outpSql + "null, comment = ";
				}
				if (outpTable.getValueAt(i, 8).toString().length() > 0) {
					outpSql = outpSql + "\"" + outpTable.getValueAt(i, 8).toString() + "\" WHERE UID = ";
				} else {
					outpSql = outpSql + "null WHERE UID = ";
				}
				outpSql = outpSql + outpTable.getValueAt(i, 0).toString() + " AND UTC = '";
				
				String datetime = convertDate(outpTable.getValueAt(i, 1).toString().substring(0, 19),
						Short.valueOf(outpTable.getValueAt(i, 1).toString().substring(20, 23)), true);
				if (!isMySQL()) datetime += "."+outpTable.getValueAt(i, 1).toString().substring(20, 23);
				outpSql = outpSql + datetime + "';";
				
				boolean success = executeIndividualUpdate(outpSql);
				if (success == true) {
					y++;
					System.out.println("Change "+(y+n)+" of "+totalChanges+": SUCCESS ("
							+ outpTable.getValueAt(i, 0).toString()+", "+outpTable.getValueAt(i, 1).toString()+", "+outpTable.getValueAt(i, 6).toString()
							+ ", "+outpTable.getValueAt(i, 7).toString()+", "+outpTable.getValueAt(i, 8).toString()+")");
					for (int j = 6; j < outpTable.getModel().getColumnCount(); j++) {
						origTable[getOriginalIndex(i,outpTable,origTable)][j] = outpTable.getValueAt(i, j);
					}
					changeLog[getOriginalIndex(i,outpTable,origTable)] = false;
				} else {
					n++;
					System.out.println("Change "+(y+n)+" of "+totalChanges+": FAILURE ("
							+ outpTable.getValueAt(i, 0).toString()+", "+outpTable.getValueAt(i, 1).toString()+", "+outpTable.getValueAt(i, 6).toString()
							+ ", "+outpTable.getValueAt(i, 7).toString()+", "+outpTable.getValueAt(i, 8).toString()+")");
				}
				commitLoadingBarWindow.updateLoadingBar(success, commitNow);
			}
		}
		originalTable = origTable;
		tableChangeLog = changeLog;
		if (commitNow) {
			System.out.println("COMMITTING CHANGES TO DATABASE: "+mySystem.getDatabaseName());
			if (getDBControl().commitChanges()) {
				System.out.println("COMMIT SUCCEEDED");
				commitLoadingBarWindow.finish(commitLoadingBarWindow.COMMIT_SUCCEEDED);
			} else {
				System.out.println("COMMIT FAILED");
				commitLoadingBarWindow.finish(commitLoadingBarWindow.COMMIT_FAILED);
			}
		}
	}
	
	/**
	 * Executes an individual UPDATE command.
	 * @param sqlEntry - The SQL update statement. (String)
	 * @return boolean - Whether or not the SQL succeeded or not.
	 */
	protected boolean executeIndividualUpdate(String sqlEntry) {
		Statement stmt = null;
		try {
			stmt = getDBControl().getConnection().getConnection().createStatement();
			stmt.execute(sqlEntry);
			if (stmt.getUpdateCount() < 1) {
				stmt.close();
				return false;
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				if (stmt != null && !stmt.isClosed()) stmt.close();
			} catch (HeadlessException | SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		}
		return true;
	}
	
	protected int getOriginalIndex(int rowIndex, PamTable ttable, Object[][] originalTable) {
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
	
	public Object[][] getOriginalTable(){
		return originalTable;
	}
	
	public boolean[] getChangeLog() {
		return tableChangeLog;
	}
	
	public boolean isMySQL() {
		return !(getDBSystem().getDatabaseName().contains(".sqlite") || getDBSystem().getDatabaseName().contains(".db"));
	}
	
	protected DBControl getDBControl() {
		return (DBControl) PamController.getInstance().findControlledUnit(DBControl.getDbUnitType());
	}
	
	protected DBControlUnit getDBControlUnit() {
		return DBControlUnit.findDatabaseControl();
	}
	
	protected DBProcess getDBProcess() {
		DBControlUnit controlUnit = getDBControlUnit();
		if (controlUnit == null) return null;
		return controlUnit.getDbProcess();
	}
	
	protected DBSystem getDBSystem() {
		DBControlUnit controlUnit = getDBControlUnit();
		if (controlUnit == null) return null;
		return controlUnit.getDatabaseSystem();
	}
}