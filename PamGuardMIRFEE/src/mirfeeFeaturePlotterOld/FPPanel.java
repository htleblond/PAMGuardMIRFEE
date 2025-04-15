package mirfeeFeaturePlotterOld;

import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.*;
import javax.swing.table.*; //
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.renjin.primitives.subset.ArraySubsettable;

import Layout.PamFramePlots;
import Layout.PamInternalFrame;
import PamDetection.RawDataUnit;

import javax.swing.filechooser.*;
import javax.swing.event.*;
import java.util.*; //
import java.text.*; //
import java.io.PrintWriter;

import javax.swing.border.TitledBorder;

import fftManager.Complex;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import mirfeeFeatureExtractor.FEFeatureDialog;
import mirfeeTrainingSetBuilder.TSBSettingsDialog;
import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayTabPanel;
import userDisplay.UserDisplayTabPanelControl;
import userDisplay.UserFramePlots;
import whistlesAndMoans.AbstractWhistleDataUnit;
import wmat.WMATSearchDialog;
import userDisplay.UserDisplayFrame;
import PamUtils.PamCalendar;
import PamUtils.SelectFolder;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.PamLabel;
import PamView.dialog.PamTextDisplay;
import PamView.panel.PamBorderPanel;
import PamView.panel.PamPanel;
import PamView.PamList; //
import PamView.PamTable;
import PamView.dialog.PamButton; //
import PamView.dialog.PamTextField; //
import PamView.dialog.SourcePanel;
import binaryFileStorage.*;
import PamguardMVC.DataUnitBaseData;
import Spectrogram.SpectrogramDisplay;
import PamUtils.SelectFolder;
//import PamController.PamFolders;
import pamScrollSystem.*;
import PamUtils.PamFileChooser;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The panel where the GUI components are written.
 * @author Holly LeBlond
 */
public class FPPanel extends PamBorderPanel {
	
	FPControl fpControl;
	protected PamPanel mainPanel;
	
	protected JTextField fileField;
	protected JButton fileButton;
	protected JButton settingsButton;
	protected JComboBox<String> xBox;
	protected JComboBox<String> yBox;
	protected JButton graphButton;
	protected JButton saveButton;
	
	protected JFXPanel jf;
	protected ScatterChart<String, Number> scatterChart;
	
	protected ArrayList<String> featureList;
	protected ArrayList<String[]> entryList;
	protected ArrayList<String> labelList;
	
	public FPPanel(FPControl fpControl) {
		this.fpControl = fpControl;
		
		this.setLayout(new BorderLayout());
		
		mainPanel = new PamPanel();
		//mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setLayout(new BorderLayout());
		//GridBagConstraints a = new PamGridBagContraints();
		
		PamPanel memPanel = new PamPanel(new GridBagLayout());
		GridBagConstraints b = new PamGridBagContraints();
		memPanel.setBorder(new TitledBorder(""));
		
		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 4;
		c.fill = c.HORIZONTAL;
		fileField = new JTextField(50);
		fileField.setEnabled(false);
		topPanel.add(fileField, c);
		c.gridx += 4;
		c.gridwidth = 1;
		c.anchor = c.WEST;
		c.fill = c.NONE;
		fileButton = new JButton("Select training set");
		fileButton.addActionListener(new LoadButtonListener());
		topPanel.add(fileButton, c);
		c.gridx++;
		c.anchor = c.EAST;
		c.fill = c.HORIZONTAL;
		settingsButton = new JButton("Settings");
		// ADD LISTENER
		topPanel.add(settingsButton, c);
		c.gridy++;
		c.gridx = 0;
		c.anchor = c.WEST;
		c.fill = c.NONE;
		topPanel.add(new JLabel("X-Axis"), c);
		c.gridx++;
		topPanel.add(new JLabel("Y-Axis"), c);
		c.gridy++;
		c.gridx = 0;
		xBox = new JComboBox<String>();
		xBox.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaa");
	/*	xBox.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		        updateGraph();
		    }
		}); */
		topPanel.add(xBox, c);
		c.gridx++;
		yBox = new JComboBox<String>();
		yBox.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaa");
	/*	yBox.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		        updateGraph();
		    }
		}); */
		topPanel.add(yBox, c);
		c.gridx++;
		graphButton = new JButton("Generate graph");
		graphButton.setEnabled(false);
		graphButton.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		        updateGraph();
		    }
		});
		topPanel.add(graphButton, c);
		c.gridx = 5;
		c.anchor = c.EAST;
		c.fill = c.HORIZONTAL;
		saveButton = new JButton("Save graphic");
		saveButton.setEnabled(false);
		// ADD LISTENER
		topPanel.add(saveButton, c);
		b.anchor = b.NORTHWEST;
		b.fill = b.HORIZONTAL;
		memPanel.add(topPanel, b);
		
		JPanel bottomPanel = new JPanel(new FlowLayout());
		bottomPanel.setBorder(new TitledBorder(""));
		jf = new JFXPanel();
		jf.setSize(800, 500);
		// Kudos: https://www.tutorialspoint.com/javafx/scatter_chart.htm
		//Stage stage = new Stage();
		NumberAxis xAxis = new NumberAxis(0, 100, 10);
		NumberAxis yAxis = new NumberAxis(0, 100, 10);
		scatterChart = new ScatterChart(xAxis, yAxis);
		scatterChart.setPrefSize(750, 500);
		XYChart.Series series = new XYChart.Series();
		//series.getData().add(new XYChart.Data(8, 12)); 
	    //series.getData().add(new XYChart.Data(4, 5.5));
		scatterChart.getData().addAll(series);
		Group root = new Group(scatterChart);
		Scene scene = new Scene(root, 800, 500);
		//stage.setScene(scene);
		//stage.show();
		jf.setScene(scene);
		//JScrollPane jsp = new JScrollPane(jf);
		//bottomPanel.add(jsp);
		bottomPanel.add(jf);
		b.gridy++;
		memPanel.add(bottomPanel, b);
		
		mainPanel.add(memPanel);
		this.add(mainPanel);
	}
	
	protected void updateGraph() {
		ArrayList<ArrayList<double[]>> doubleList = new ArrayList<ArrayList<double[]>>();
		double xMin = 0.0;
		double xMax = 0.0;
		double yMin = 0.0;
		double yMax = 0.0;
		boolean firstFound = false;
		for (int i = 0; i < labelList.size(); i++) {
			ArrayList<double[]> subList = new ArrayList<double[]>();
			for (int j = 0; j < entryList.size(); j++) {
				if (entryList.get(j)[4].equals(labelList.get(i))) {
					try {
						double xVal = Double.valueOf(entryList.get(j)[xBox.getSelectedIndex()+5]);
						double yVal = Double.valueOf(entryList.get(j)[yBox.getSelectedIndex()+5]);
						if (!firstFound) {
							xMin = xVal;
							xMax = xVal;
							yMin = yVal;
							yMax = yVal;
							firstFound = true;
						} else {
							if (xVal < xMin) {
								xMin = xVal;
							}
							if (xVal > xMax) {
								xMax = xVal;
							}
							if (yVal < yMin) {
								yMin = yVal;
							}
							if (yVal > yMax) {
								yMax = yVal;
							}
						}
						subList.add(new double[] {xVal,yVal});
					} catch (Exception e){
						// ?????
					}
				}
			}
			doubleList.add(subList);
		}
		NumberAxis xAxis = new NumberAxis(xMin, xMax, (xMax-xMin)/10);
		NumberAxis yAxis = new NumberAxis(yMin, yMax, (yMax-yMin)/10);
		xAxis.setLabel((String) xBox.getSelectedItem());
		yAxis.setLabel((String) yBox.getSelectedItem());
		scatterChart = new ScatterChart(xAxis, yAxis);
		scatterChart.setPrefSize(750, 500);
		//ArrayList<XYChart.Series> seriesList = new ArrayList<XYChart.Series>();
		for (int i = 0; i < doubleList.size(); i++) {
			XYChart.Series series = new XYChart.Series();
			for (int j = 0; j < doubleList.get(i).size(); j++) {
				series.getData().add(new XYChart.Data(doubleList.get(i).get(j)[0], doubleList.get(i).get(j)[1]));
			}
			//seriesList.add(series);
			scatterChart.getData().add(series);
		}
		//scatterChart.getData().addAll(series);
		Group root = new Group(scatterChart);
		Scene scene = new Scene(root, 800, 500);
		
		// Kudos: https://stackoverflow.com/questions/52762804/how-can-i-change-the-points-colors-of-a-scatterchart-using-javafx-inside-java-co
		Set<Node> nodes = root.lookupAll(".series" + 1);
		for (Node n : nodes) {
        //    n.setStyle("-fx-background-color: #860061, white;\n"
			n.setStyle("-fx-background-color: blue;\n"
                    //+ "    -fx-background-insets: 0, 2;\n"
                    + "    -fx-background-radius: 3px;");
                    //+ "    -fx-background-radius: 3px;\n"
                    //+ "    -fx-padding: 5px;");
        }
		
		jf.setScene(scene);
	}
	
	class LoadButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			PamFileChooser fc = new PamFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Comma-separated values file (*.csv)","csv"));
			int returnVal = fc.showOpenDialog(fpControl.getPamView().getGuiFrame());
			if (returnVal == fc.CANCEL_OPTION) {
				return;
			}
			File f = fc.getSelectedFile();
			if (!f.exists()) {
				fpControl.simpleErrorDialog("Selected file does not exist.", 250);
				return;
			}
			
			Scanner sc = null;
			try {
				sc = new Scanner(f);
				String[] firstLine = null;
				if (sc.hasNextLine()) {
					firstLine = sc.nextLine().split(",");
				}
				if (firstLine.length < 7) {
					fpControl.simpleErrorDialog("Selected file not valid MIRFEE training set.", 250);
					sc.close();
					return;
				}
				if (!(firstLine[0].equals("cluster") && firstLine[1].equals("uid") && firstLine[2].equals("location")
						 && firstLine[3].equals("date") && firstLine[4].equals("label"))) {
					fpControl.simpleErrorDialog("Selected file not valid MIRFEE training set.", 250);
					sc.close();
					return;
				}
				ArrayList<String> newFeatureList = new ArrayList<String>();
				for (int i = 5; i < firstLine.length; i++) {
					newFeatureList.add(firstLine[i]);
					//System.out.println(firstLine[i]);
				}
				if (newFeatureList.size() < 2) {
					fpControl.simpleErrorDialog("Selected file contains fewer than two features.", 250);
					sc.close();
					return;
				}
				ArrayList<String[]> newEntryList = new ArrayList<String[]>();
				ArrayList<String> newLabelList = new ArrayList<String>();
				while (sc.hasNextLine()) {
					String[] nextLine = sc.nextLine().split(",");
					newEntryList.add(nextLine);
					if (!newLabelList.contains(nextLine[4])) {
						newLabelList.add(nextLine[4]);
					}
				}
				if (newEntryList.size() == 0) {
					fpControl.simpleErrorDialog("Selected file contains no entries.", 250);
					sc.close();
					return;
				}
				featureList = new ArrayList<String>(newFeatureList);
				entryList = new ArrayList<String[]>(newEntryList);
				labelList = new ArrayList<String>(newLabelList);
				fileField.setText(f.getPath());
				sc.close();
			} catch (Exception e2){
				if (sc != null) {
					sc.close();
					e2.printStackTrace();
					fpControl.simpleErrorDialog("Error processing selected file.", 250);
				} else {
					fpControl.simpleErrorDialog("Could not create scanner.", 250);
				}
				return;
			}
			xBox.removeAllItems();
			yBox.removeAllItems();
			for (int i = 0; i < featureList.size(); i++) {
				xBox.addItem(featureList.get(i));
				yBox.addItem(featureList.get(i));
			}
			xBox.setSelectedIndex(0);
			yBox.setSelectedIndex(1);
			graphButton.setEnabled(true);
			saveButton.setEnabled(true);
			//updateGraph();
		}
	}
}
