package mirfeeFeatureExtractor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;

/**
 * The dialog for choosing which features are in the vector.
 * @author Holly LeBlond
 */
//@SuppressWarnings("serial")
public class FEFeatureDialog extends PamDialog {
	
	protected FEControl feControl;
	protected FESettingsDialog settingsDialog;
	protected JTable table;
	protected DefaultTableModel dtmodel;
	
	protected JPanel p_left;
	protected DefaultListModel<String> dlmodel;
	protected JList<String> flist;
	
	protected JPanel p_right;
	protected JPanel p_cards;
	protected JPanel p_blank;
	protected JPanel p_amp;
	protected JPanel p_duration;
	protected JPanel p_freq_hd;
	protected JPanel p_freq_sd;
	protected JPanel p_frange;
	protected JPanel p_fslope_hd;
	protected JPanel p_formants;
	protected JPanel p_mfcc;
	protected JPanel p_poly;
	protected JPanel p_praat;
	protected JPanel p_harmonics;
	protected JPanel p_rms;
	protected JPanel p_bandwidth;
	protected JPanel p_centroid;
	protected JPanel p_contrast;
	protected JPanel p_flatness;
	protected JPanel p_flux;
	protected JPanel p_specmag;
	protected JPanel p_rolloff;
	protected JPanel p_wc;
	//protected JPanel p_yin;
	protected JPanel p_zcr;
	
	protected JPanel p_bottom;
	protected JButton insertButton;
	protected JButton appendButton;
	
	protected JComboBox<String> freq_hd_box;
	
	protected JComboBox<String> freq_sd_options_box;
	protected JLabel freq_sd_text;
	protected JLabel freq_sd_output_text;
	protected JComboBox<String> freq_sd_box;
	
	protected JComboBox<String> formants_options_box;
	protected JLabel formants_text;
	protected JComboBox<String> formants_output_box;
	protected JLabel formants_num_text;
	protected JTextField formants_num_field;
	protected JTextField formants_max_exp_freq_field;
	protected JTextField formants_desired_order_field;
	protected JTextField formants_min_freq_field;
	protected JTextField formants_max_bandwidth_field;
	
	protected JComboBox<String> mfcc_box;
	protected JTextField mfcc_n_field;
	protected JComboBox<String> mfcc_selector_box;
	
	protected JComboBox<String> poly_box;
	protected JTextField poly_order_field;
	protected JComboBox<String> poly_selector_box;
	
	protected JComboBox<String> praat_box;
	protected JTextField praat_min_field;
	protected JTextField praat_max_field;
	
	protected JComboBox<String> harmonics_box_1;
	protected JLabel harmonics_text;
	protected JComboBox<String> harmonics_box_2;
	protected JLabel harmonics_frame_output_text;
	protected JLabel harmonics_n_text;
	protected JComboBox<String> harmonics_box_3; 
	protected JTextField harmonics_n_field;
	protected JTextField harmonics_buffer_field;
	protected JTextField harmonics_min_field;
	protected JTextField harmonics_max_field;
	//protected JCheckBox harmonics_normalize_check;
	protected JLabel harmonics_ratio_max_text;
	protected JTextField harmonics_ratio_max_field;
	
	protected JComboBox<String> rms_box;
	
	protected JComboBox<String> bandwidth_box;
	protected JCheckBox bandwidth_normalize_check;
	protected JTextField bandwidth_power_field;
	
	protected JComboBox<String> centroid_box;
	
	protected JComboBox<String> contrast_box;
	protected JSpinner contrast_freq_spinner;
	protected JSpinner contrast_nbands_spinner;
	protected JComboBox<String> contrast_band_box;
	protected JRadioButton contrast_linear_rb;
	protected JRadioButton contrast_log_rb;
	protected ButtonGroup contrast_rbg;
	
	protected JComboBox<String> flatness_box;
	protected JTextField flatness_power_field;
	
	protected JComboBox<String> flux_box;
	
	protected JComboBox<String> specmag_box;
	protected JTextField specmag_min_field;
	protected JTextField specmag_max_field;
	protected JCheckBox specmag_normalize_check;
	
	protected JComboBox<String> rolloff_box;
	protected JTextField rolloff_threshold_field;
	
	//protected JComboBox<String> wc_options_box;
	//protected JComboBox<String> wc_output_box;
	//protected JSpinner wc_fragment_size_spinner;
	
	protected JComboBox<String> zcr_box;
	
	protected JScrollPane scrollPane;
	
	public FEFeatureDialog(Window parentFrame, FEControl feControl, FESettingsDialog settingsDialog, JTable table) {
		super(parentFrame, feControl.getUnitName(), true);
		this.feControl = feControl;
		this.settingsDialog = settingsDialog;
		this.table = table;
		this.dtmodel = (DefaultTableModel) table.getModel();
		
		this.getOkButton().setVisible(false);
		this.getCancelButton().setText("Done");
		this.getDefaultButton().setVisible(false);
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder(""));
		GridBagConstraints b = new PamGridBagContraints();
		b.gridy = 0;
		b.gridx = 0;
		b.fill = GridBagConstraints.BOTH;
		b.anchor = GridBagConstraints.CENTER;
		JPanel subPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		
		p_left = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
		String[] featureNames = new String[] {"Amplitude","Duration","Frequency (header data)","Frequency range (header data)",
				"Frequency slope (header data)","Frequency (slice data)","Linear predictive coding formants","Mel-frequency cepstral coefficients",
				"Polynomial features","Praat fundamental frequency","Praat-tracked harmonics","Root mean square",
				"Spectral bandwidth","Spectral centroid","Spectral contrast","Spectral flatness","Spectral flux (onset strength)",
				"Spectral magnitude","Spectral rolloff","Zero-crossing rate"};
		dlmodel = new DefaultListModel<String>();
		for (int i = 0; i < featureNames.length; i++) {
			dlmodel.addElement(featureNames[i]);
		}
		flist = new JList<String>(dlmodel);
		flist.setSize(200, 400);
		flist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		flist.setLayoutOrientation(JList.VERTICAL);
		scrollPane = new JScrollPane(flist);
		scrollPane.setPreferredSize(flist.getSize());
		p_left.add(scrollPane, c);
		subPanel.add(p_left);
		
		p_right = new JPanel(new BorderLayout());
		p_cards = new JPanel(new CardLayout());
		
		CardLayout cl = (CardLayout) p_cards.getLayout();
		
		p_blank = constructFeaturePanel(makeHTML("Select a feature to add to the vector."),
				new Object[0][0],
				false);
		p_cards.add(p_blank,"");
		cl.show(p_cards,"");
		
		int nameIndex = 0;
		
		p_amp = constructFeaturePanel(makeHTML("Amplitude value (in dB re SPSL) taken directly from contour header data."),
				new Object[0][0],
				false);
		p_cards.add(p_amp,featureNames[nameIndex++]);
		
		p_duration = constructFeaturePanel(makeHTML("Duration value (in milliseconds) taken directly from contour header data."),
				new Object[0][0],
				false);
		p_cards.add(p_duration,featureNames[nameIndex++]);
		
		freq_hd_box = makeOutputBox(new String[] {"Minimum","Maximum"});
		p_freq_hd = constructFeaturePanel(makeHTML("Minimum or maximum frequency value (in Hz) taken directly from contour header data."),
				new Object[][] {{"Output:", freq_hd_box}},
				false);
		p_cards.add(p_freq_hd,featureNames[nameIndex++]);
		
		p_frange = constructFeaturePanel(makeHTML("Difference between maximum and minimum frequency values (in Hz) from contour header data."),
				new Object[0][0],
				false);
		p_cards.add(p_frange,featureNames[nameIndex++]);
		
		p_fslope_hd = constructFeaturePanel(makeHTML("Maximum frequency value minus minimum frequency value, divided by duration value, "
				+ "all from contour header data (in Hz per second)."),
				new Object[0][0],
				false);
		p_cards.add(p_fslope_hd,featureNames[nameIndex++]);
		
		freq_sd_options_box = makeOutputBox(new String[] {"Frequencies","1st derivative","2nd derivative","Elbow angle","Start-to-end slope"});
		freq_sd_options_box.addActionListener(new FreqSDOptionsBoxListener());
		freq_sd_text = new JLabel(getFreqSDText());
		freq_sd_output_text = new JLabel("Output:");
		freq_sd_output_text.setVisible(freq_sd_options_box.getSelectedIndex() < 3);
		freq_sd_box = makeOutputBox(new String[] {"Mean","Median","Standard deviation","Minimum","Maximum","Range","Skewness"});
		freq_sd_box.setVisible(freq_sd_options_box.getSelectedIndex() < 3);
		p_freq_sd = constructFeaturePanel(makeHTML("Calculations performed on contour slice data frequencies."),
				new Object[][] {{freq_sd_options_box},{freq_sd_text},{freq_sd_output_text, freq_sd_box}},
				false);
		p_cards.add(p_freq_sd,featureNames[nameIndex++]);
		
		formants_options_box = makeOutputBox(new String[] {"Formant frequency","Valid formant count",
				"Difference between two formants","Formant frequency ratio"});
		formants_options_box.addActionListener(new FormantsOptionsBoxListener());
		formants_text = new JLabel(getFormantsText());
		formants_num_text = new JLabel(getFormantsNumText());
		formants_num_text.setVisible(formants_options_box.getSelectedIndex() != 1);
		formants_output_box = makeOutputBox(new String[] {"Full clip","Mean","Median","Standard deviation","Minimum","Maximum","Range"});
		formants_num_field = new JTextField(5);
		formants_num_field.setDocument(JIntFilter());
		formants_num_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (formants_options_box.getSelectedIndex() == 3)
					textIntUpdate(formants_num_field, 2);
				else textIntUpdate(formants_num_field, 1);
			}
		});
		formants_num_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	if (formants_options_box.getSelectedIndex() == 3)
					textIntUpdate(formants_num_field, 2);
				else textIntUpdate(formants_num_field, 1);
		    }
		});
		formants_num_field.setText("1");
		if (formants_options_box.getSelectedIndex() == 3)
			formants_num_field.setText("2");
		formants_num_field.setVisible(formants_options_box.getSelectedIndex() != 1);
		formants_max_exp_freq_field = new JTextField(5);
		formants_max_exp_freq_field.setDocument(JIntFilter());
		formants_max_exp_freq_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(formants_max_exp_freq_field, 10);
				formants_desired_order_field.setText(getFormantsDesiredOrderText());
			}
		});
		formants_max_exp_freq_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(formants_max_exp_freq_field, 10);
				formants_desired_order_field.setText(getFormantsDesiredOrderText());
		    }
		});
		formants_max_exp_freq_field.setText("6000");
		formants_desired_order_field = new JTextField(5);
		formants_desired_order_field.setEnabled(false);
		formants_desired_order_field.setText(getFormantsDesiredOrderText());
		formants_min_freq_field = new JTextField(5);
		formants_min_freq_field.setDocument(JIntFilter());
		formants_min_freq_field.setText("90");
		formants_max_bandwidth_field = new JTextField(5);
		formants_max_bandwidth_field.setDocument(JIntFilter());
		formants_max_bandwidth_field.setText("400");
		p_formants = constructFeaturePanel(makeHTML("Formants calculated through linear predictive coding. Designed more for use on the human "
				+ "vocal tract, but may potentially be useful for cetacean calls with adapted settings."),
				new Object[][] {{formants_options_box},{formants_text},{"Output:"},{"",formants_output_box},{formants_num_text},{"",formants_num_field},
					{"Max. expected fundamental:"},{"",formants_max_exp_freq_field},{"Desired LPC order:"},{"",formants_desired_order_field},
					{"Min. formant frequency:"},{"",formants_min_freq_field},{"Max. formant bandwidth:"},{"",formants_max_bandwidth_field}},
				false);
		p_cards.add(p_formants,featureNames[nameIndex++]);
		
		mfcc_box = makeOutputBox();
		mfcc_n_field = new JTextField(5);
		mfcc_n_field.setDocument(fieldToBoxFilter(mfcc_n_field, "mfcc"));
		mfcc_n_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mfccBoxUpdate();
			}
		});
		// Kudos to this: https://stackoverflow.com/questions/6088571/can-i-catch-the-event-of-exiting-a-jtextfield
		mfcc_n_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	mfccBoxUpdate();
		    }
		});
		mfcc_selector_box = makeOutputBox(new String[] {""}) ;
		mfcc_n_field.setText("12");
		mfccBoxUpdate();
		p_mfcc = constructFeaturePanel(makeHTML("Calculates Mel-frequency cepstral coefficients (MFCCs) from the audio clip. Can produce output value "
				+ "for all coefficients or one specific coefficient."),
				new Object[][] {{"Output:", mfcc_box},{"Number of MFCCs:",mfcc_n_field},{"Selected coefficient:",mfcc_selector_box}},
				true);
		p_cards.add(p_mfcc,featureNames[nameIndex++]);
		
		poly_box = makeOutputBox();
		poly_order_field = new JTextField(5);
		poly_order_field.setDocument(fieldToBoxFilter(poly_order_field, "poly"));
		poly_order_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				polyBoxUpdate();
			}
		});
		poly_order_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	polyBoxUpdate();
		    }
		});
		poly_selector_box = new JComboBox<String>();
		poly_order_field.setText("0");
		polyBoxUpdate();
		p_poly = constructFeaturePanel(makeHTML("Get coefficients of fitting an nth-order polynomial to the columns of a spectrogram.\n\n"
				+ "(From Librosa documentation: https://librosa.org/doc/main/generated/librosa.feature.poly_features.html)"),
				new Object[][] {{"Output:",poly_box},{"Order:",poly_order_field},{"Selected coefficient:",poly_selector_box}},
				true);
		p_cards.add(p_poly,featureNames[nameIndex++]);
		
		praat_box = makeOutputBox();
		praat_box.setSelectedIndex(1);
		praat_min_field = new JTextField(5);
		praat_max_field = new JTextField(5);
		praat_min_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(praat_min_field, 0);
				if (Integer.valueOf(praat_min_field.getText()) >= Integer.valueOf(praat_max_field.getText())) {
					praat_min_field.setText(Integer.toString(Integer.valueOf(praat_max_field.getText())-1));
				}
			}
		});
		praat_min_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(praat_min_field, 0);
				if (Integer.valueOf(praat_min_field.getText()) >= Integer.valueOf(praat_max_field.getText())) {
					praat_min_field.setText(Integer.toString(Integer.valueOf(praat_max_field.getText())-1));
				}
		    }
		});
		praat_max_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(praat_max_field, 1);
				if (Integer.valueOf(praat_min_field.getText()) >= Integer.valueOf(praat_max_field.getText())) {
					praat_max_field.setText(Integer.toString(Integer.valueOf(praat_min_field.getText())+1));
				}
			}
		});
		praat_max_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(praat_max_field, 1);
				if (Integer.valueOf(praat_min_field.getText()) >= Integer.valueOf(praat_max_field.getText())) {
					praat_max_field.setText(Integer.toString(Integer.valueOf(praat_min_field.getText())+1));
				}
		    }
		});
		praat_min_field.setDocument(JIntFilter());
		praat_max_field.setDocument(JIntFilter());
		praat_min_field.setText("50");
		praat_max_field.setText("20000");
		p_praat = constructFeaturePanel(makeHTML("Estimates fundamental frequency of each frame using the Praat pitch detection algorithm via the Parselmouth library. "
				+ "Not always individually accurate, but potentially useful as a feature anyway."),
				new Object[][] {{"Output:", praat_box},{"Minimum frequency (Hz):"},{"", praat_min_field},{"Maximum frequency (Hz)"},
				{"", praat_max_field}},
				false);
		p_cards.add(p_praat,featureNames[nameIndex++]);
		
		harmonics_box_1 = makeOutputBox(new String[] {"Total harmonic distortion","Harmonics-to-background ratio","Harmonic centroid",
				"Bin-exclusive harmonic centroid","Harmonic-to-fundamental ratio"});
		harmonics_box_1.addActionListener(new HarmonicsOptionsBoxListener());
		harmonics_text = new JLabel(getHarmonicsText());
		harmonics_box_2 = makeOutputBox();
		harmonics_box_2.setSelectedIndex(1);
		harmonics_frame_output_text = new JLabel("Calculation on each frame:");
		harmonics_frame_output_text.setVisible(harmonics_box_1.getSelectedIndex() == 2);
		harmonics_n_text = new JLabel(getHarmonicNumberText());
		harmonics_box_3 = makeOutputBox(new String[] {"Mean","Median","Mode","Standard deviation"});
		harmonics_box_3.setVisible(harmonics_box_1.getSelectedIndex() == 2);
		harmonics_n_field = new JTextField(5);
		harmonics_n_field.setDocument(JIntFilter());
		harmonics_n_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(harmonics_n_field, 2);
			}
		});
		harmonics_n_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(harmonics_n_field, 2);
		    }
		});
		harmonics_n_field.setText("8");
		harmonics_min_field = new JTextField(5);
		harmonics_max_field = new JTextField(5);
		harmonics_min_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(harmonics_min_field, 0);
				if (Integer.valueOf(harmonics_min_field.getText()) >= Integer.valueOf(harmonics_max_field.getText())) {
					harmonics_min_field.setText(Integer.toString(Integer.valueOf(harmonics_max_field.getText())-1));
				}
			}
		});
		harmonics_min_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(harmonics_min_field, 0);
				if (Integer.valueOf(harmonics_min_field.getText()) >= Integer.valueOf(harmonics_max_field.getText())) {
					harmonics_min_field.setText(Integer.toString(Integer.valueOf(harmonics_max_field.getText())-1));
				}
		    }
		});
		harmonics_max_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(harmonics_max_field, 1);
				if (Integer.valueOf(harmonics_min_field.getText()) >= Integer.valueOf(harmonics_max_field.getText())) {
					harmonics_max_field.setText(Integer.toString(Integer.valueOf(harmonics_min_field.getText())+1));
				}
			}
		});
		harmonics_max_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(harmonics_max_field, 1);
				if (Integer.valueOf(harmonics_min_field.getText()) >= Integer.valueOf(harmonics_max_field.getText())) {
					harmonics_max_field.setText(Integer.toString(Integer.valueOf(harmonics_min_field.getText())+1));
				}
		    }
		});
		harmonics_min_field.setDocument(JIntFilter());
		harmonics_max_field.setDocument(JIntFilter());
		harmonics_min_field.setText("50");
		harmonics_max_field.setText("20000");
		harmonics_ratio_max_text = new JLabel(makeHTML("Maximum possible ratio (e.g. if fundamental is silent):"));
		harmonics_ratio_max_text.setVisible(harmonics_box_1.getSelectedIndex() == 3);
		harmonics_ratio_max_field = new JTextField(5);
		harmonics_ratio_max_field.setDocument(JDoubleFilter(harmonics_ratio_max_field));
		harmonics_ratio_max_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textDoubleUpdate(harmonics_ratio_max_field, 1.0);
			}
		});
		harmonics_ratio_max_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textDoubleUpdate(harmonics_ratio_max_field, 1.0);
		    }
		});
		harmonics_ratio_max_field.setVisible(harmonics_box_1.getSelectedIndex() == 3);
		harmonics_ratio_max_field.setText("100.0");
		//harmonics_normalize_check = new JCheckBox();
		//harmonics_normalize_check.setText("Normalize");
		p_harmonics = constructFeaturePanel(makeHTML("Calculates various harmonics-related features using the Praat pitch detection algorithm."),
				new Object[][] {{harmonics_box_1},{harmonics_text},{"Overall output:"},{"", harmonics_box_2},{harmonics_frame_output_text},
					{"",harmonics_box_3},{harmonics_n_text},{"",harmonics_n_field},{"Minimum frequency (Hz):"},{"",harmonics_min_field},
					{"Maximum frequency (Hz):"},{"",harmonics_max_field},{harmonics_ratio_max_text},{"",harmonics_ratio_max_field}},
				false);
		p_cards.add(p_harmonics,featureNames[nameIndex++]);
		
		rms_box = makeOutputBox();
		p_rms = constructFeaturePanel(makeHTML("Calculates root mean square (RMS) values directly from the audio of the clip."),
				new Object[][] {{"Output:", rms_box}},
				false);
		p_cards.add(p_rms,featureNames[nameIndex++]);
		
		bandwidth_box = makeOutputBox();
		bandwidth_power_field = new JTextField(5);
		bandwidth_power_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(bandwidth_power_field, 1);
			}
		});
		bandwidth_power_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(bandwidth_power_field, 1);
		    }
		});
		bandwidth_power_field.setDocument(JIntFilter());
		bandwidth_power_field.setText("2");
		bandwidth_normalize_check = new JCheckBox();
		bandwidth_normalize_check.setText("Normalize");
		p_bandwidth = constructFeaturePanel(makeHTML("Compute p\'th-order spectral bandwidth.\n\n"
				+ "(From Librosa documentation: https://librosa.org/doc/main/generated/librosa.feature.spectral_bandwidth.html)"),
				new Object[][] {{"Output:", bandwidth_box},{"Power:",bandwidth_power_field},{bandwidth_normalize_check}},
				false);
		p_cards.add(p_bandwidth,featureNames[nameIndex++]);
		
		centroid_box = makeOutputBox();
		p_centroid = constructFeaturePanel(makeHTML("Calculates the \"centre of mass\" of the spectrum of an audio clip."),
				new Object[][] {{"Output:", centroid_box}},
				false);
		p_cards.add(p_centroid,featureNames[nameIndex++]);
		
		contrast_box = makeOutputBox();
	/*	contrast_freq_field = new JTextField(5);
		contrast_freq_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(contrast_freq_field, 1);
			}
		});
		contrast_freq_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(contrast_freq_field, 1);
		    }
		});
		contrast_freq_field.setDocument(JIntFilter());
		contrast_freq_field.setText("200"); */
		contrast_freq_spinner = new JSpinner(new SpinnerNumberModel(200,1,null,50));
		((JSpinner.DefaultEditor) contrast_freq_spinner.getEditor()).getTextField().setColumns(5);
	/*	contrast_bands_field = new JTextField(5);
		contrast_bands_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(contrast_bands_field, 2);
			}
		});
		contrast_bands_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(contrast_bands_field, 2);
		    }
		});
		contrast_bands_field.setDocument(JIntFilter());
		contrast_bands_field.setText("6"); */
		contrast_nbands_spinner = new JSpinner(new SpinnerNumberModel(6,2,null,1));
		((JSpinner.DefaultEditor) contrast_nbands_spinner.getEditor()).getTextField().setColumns(5);
		contrast_nbands_spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				contrastBoxUpdate();
			}
		});
		contrast_band_box = new JComboBox<String>();
		contrastBoxUpdate();
		contrast_linear_rb = new JRadioButton();
		contrast_linear_rb.setText("Linear");
		contrast_log_rb = new JRadioButton();
		contrast_log_rb.setText("Logarithmic");
		contrast_rbg = new ButtonGroup();
		contrast_rbg.add(contrast_linear_rb);
		contrast_rbg.add(contrast_log_rb);
		contrast_rbg.setSelected(contrast_log_rb.getModel(), true);
		p_contrast = constructFeaturePanel(makeHTML("Calculates the level difference between peaks and valleys in an audio spectrum."),
				new Object[][] {{"Output:", contrast_box},{makeHTML("Frequency cutoff for first bin:")},{"",contrast_freq_spinner},
				{makeHTML("Number of frequency bands:")},{"",contrast_nbands_spinner},{makeHTML("Selected band:")},{"",contrast_band_box},
				{contrast_linear_rb},{contrast_log_rb}},
				false);
		p_cards.add(p_contrast,featureNames[nameIndex++]);
		
		flatness_box = makeOutputBox();
		flatness_power_field = new JTextField(5);
		flatness_power_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(flatness_power_field, 1);
			}
		});
		flatness_power_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(flatness_power_field, 1);
		    }
		});
		flatness_power_field.setDocument(JIntFilter());
		flatness_power_field.setText("2");
		p_flatness = constructFeaturePanel(makeHTML("Quantifies how \"noise-like\" or \"tone-like\" frames of an audio clip are."),
				new Object[][] {{"Output:", flatness_box},{"Power:", flatness_power_field}},
				false);
		p_cards.add(p_flatness,featureNames[nameIndex++]);
		
		flux_box = makeOutputBox();
		p_flux = constructFeaturePanel(makeHTML("Compute a spectral flux onset strength envelope.\n\n"
				+ "(From Librosa documentation: https://librosa.org/doc/main/generated/librosa.onset.onset_strength.html)"),
				new Object[][] {{"Output:", flux_box}},
				false);
		p_cards.add(p_flux,featureNames[nameIndex++]);
		
		specmag_box = makeOutputBox();
		specmag_min_field = new JTextField(5);
		specmag_max_field = new JTextField(5);
		specmag_min_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(specmag_min_field, 0);
				if (Integer.valueOf(specmag_min_field.getText()) >= Integer.valueOf(specmag_max_field.getText())) {
					specmag_min_field.setText(Integer.toString(Integer.valueOf(specmag_max_field.getText())-1));
				}
			}
		});
		specmag_min_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(specmag_min_field, 0);
				if (Integer.valueOf(specmag_min_field.getText()) >= Integer.valueOf(specmag_max_field.getText())) {
					specmag_min_field.setText(Integer.toString(Integer.valueOf(specmag_max_field.getText())-1));
				}
		    }
		});
		specmag_max_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textIntUpdate(specmag_max_field, 1);
				if (Integer.valueOf(specmag_min_field.getText()) >= Integer.valueOf(specmag_max_field.getText())) {
					specmag_max_field.setText(Integer.toString(Integer.valueOf(specmag_min_field.getText())+1));
				}
			}
		});
		specmag_max_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	textIntUpdate(specmag_max_field, 1);
				if (Integer.valueOf(specmag_min_field.getText()) >= Integer.valueOf(specmag_max_field.getText())) {
					specmag_max_field.setText(Integer.toString(Integer.valueOf(specmag_min_field.getText())+1));
				}
		    }
		});
		specmag_min_field.setDocument(JIntFilter());
		specmag_max_field.setDocument(JIntFilter());
		specmag_min_field.setText("0");
		specmag_max_field.setText("24000");
		specmag_normalize_check = new JCheckBox();
		specmag_normalize_check.setText("Normalize");
		p_specmag = constructFeaturePanel(makeHTML("Retrieves magnitude values from the STFT bins of the audio clip."),
				new Object[][] {{"Output:", specmag_box},{"Minimum frequency (Hz):"},{"", specmag_min_field},
				{"Maximum frequency (Hz):"},{"", specmag_max_field}},
				false);
		p_cards.add(p_specmag,featureNames[nameIndex++]);
		
		rolloff_box = makeOutputBox();
		rolloff_threshold_field = new JTextField(5);
		rolloff_threshold_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rolloff_threshold_field.getText().length() < 1) {
					rolloff_threshold_field.setText("1");
				} else if (Integer.valueOf(rolloff_threshold_field.getText()) < 1) {
					rolloff_threshold_field.setText("1");
				} else if (Integer.valueOf(rolloff_threshold_field.getText()) > 99) {
					rolloff_threshold_field.setText("99");
				}
			}
		});
		rolloff_threshold_field.addFocusListener(new FocusAdapter() {
		    public void focusLost(FocusEvent e) {
		    	if (rolloff_threshold_field.getText().length() < 1) {
					rolloff_threshold_field.setText("1");
				} else if (Integer.valueOf(rolloff_threshold_field.getText()) < 1) {
					rolloff_threshold_field.setText("1");
				} else if (Integer.valueOf(rolloff_threshold_field.getText()) > 99) {
					rolloff_threshold_field.setText("99");
				}
		    }
		});
		rolloff_threshold_field.setDocument(JIntFilter());
		rolloff_threshold_field.setText("85");
		p_rolloff = constructFeaturePanel(makeHTML("Calculates frequency (in Hz) in each frame where [threshold]% of the spectral energy lies below."),
				new Object[][] {{"Output:", rolloff_box},{"Threshold (%):", rolloff_threshold_field}},
				false);
		p_cards.add(p_rolloff,featureNames[nameIndex++]);
		
	/*	wc_options_box = makeOutputBox(new String[] {"Frequencies","Frequency slopes","Frequency curvatures"});
		wc_output_box = makeOutputBox(new String[] {"Mean","Standard deviation","Skewness"});
		wc_fragment_size_spinner = new JSpinner(new SpinnerNumberModel(8, 3, 100, 1));
		JLabel wc_fragment_note = new JLabel(makeHTML("Note that if the specified fragment size is too long, some contours will be too short to calculate certain features from."));
		p_wc = constructFeaturePanel(makeHTML("Features used by PAMGuard's Whistle Classifier that are extracted directly from the contours."),
				new Object[][] {{wc_options_box},{"Output:",wc_output_box},{"Fragment size:",wc_fragment_size_spinner},{wc_fragment_note}},
				false);
		p_cards.add(p_wc,featureNames[nameIndex++]); */
		
		zcr_box = makeOutputBox();
		p_zcr = constructFeaturePanel(makeHTML("Calculates how often the x-axis is crossed in an audio time series."),
				new Object[][] {{"Output:", zcr_box}},
				false);
		p_cards.add(p_zcr,featureNames[nameIndex++]);
		
		
		// Kudos to this: https://stackoverflow.com/questions/13800775/find-selected-item-of-a-jlist-and-display-it-in-real-time
		flist.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                if (!arg0.getValueIsAdjusting()) {
                	if (flist.getSelectedIndex() == -1) {
                		cl.show(p_cards,"");
                	} else {
                		cl.show(p_cards,flist.getSelectedValue());
                	}
                }
            }
        });
		
		p_right.add(p_cards, BorderLayout.CENTER);
		subPanel.add(p_right);
		mainPanel.add(subPanel, b);
		
		b.gridy++;
		b.anchor = GridBagConstraints.SOUTH;
		p_bottom = new JPanel(new GridLayout(1, 2, 5, 5));
		insertButton = new JButton("Insert into vector above selected row");
		insertButton.addActionListener(new AddButtonListener(true));
		p_bottom.add(insertButton);
		appendButton = new JButton("Append to vector");
		appendButton.addActionListener(new AddButtonListener(false));
		p_bottom.add(appendButton);
		mainPanel.add(p_bottom, b);
		setDialogComponent(mainPanel);
	}
	
	public String getFormantsText() {
		if (formants_options_box.getSelectedIndex() == 0)
			return makeHTML("The frequency of the specified formant.");
		else if (formants_options_box.getSelectedIndex() == 1)
			return makeHTML("The number of valid formants found.");
		else if (formants_options_box.getSelectedIndex() == 2)
			return makeHTML("The difference in frequency between two formants.");
		return makeHTML("The frequency ratio between the selected formant and the first formant.");
	}
	
	public String getFormantsNumText() {
		if (formants_options_box.getSelectedIndex() == 2)
			return makeHTML("Between formants n and n+1:");
		return makeHTML("Formant number:");
	}
	
	public String getFormantsDesiredOrderText() {
		if (feControl.getParams().sr > 0) {
			return String.valueOf((int) Math.ceil(feControl.getParams().sr/(Double.valueOf(formants_max_exp_freq_field.getText())*0.25)));
		}
		return "(No SR found.)";
	}
	
	protected class FormantsOptionsBoxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			formants_text.setText(getFormantsText());
			formants_num_text.setText(getFormantsNumText());
			formants_num_text.setVisible(formants_options_box.getSelectedIndex() != 1);
			formants_num_field.setVisible(formants_options_box.getSelectedIndex() != 1);
			if (formants_options_box.getSelectedIndex() == 3 && Integer.valueOf(formants_num_field.getText()) < 2)
				formants_num_field.setText("2");
		}
	}
	
	public String getFreqSDText() {
		switch (freq_sd_options_box.getSelectedIndex()) {
		case 1:
			return makeHTML("The \"rate of change\" between the peak frequencies of each slice, "
					+ "by frequency (in Hz) over time (in ms).");
		case 2:
			return makeHTML("The \"rate of change of the rate of change\" between the peak frequencies of each slice, "
					+ "by frequency (in Hz) over time (in ms).");
		case 3:
			return makeHTML("A measurement of the contour's \"curvature\", in degrees.");
		case 4:
			return makeHTML("\"As-the-crow-flies\" slope in terms of time and frequency between the first and last slices in the contour, "
					+ "by frequency (in Hz) over time (in ms).");
		}
		return makeHTML("The peak frequencies of each slice, in Hz.");
	}
	
	protected class FreqSDOptionsBoxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			freq_sd_text.setText(getFreqSDText());
			freq_sd_output_text.setVisible(freq_sd_options_box.getSelectedIndex() < 3);
			freq_sd_box.setVisible(freq_sd_options_box.getSelectedIndex() < 3);
		}
	}
	
	public String getHarmonicsText() {
		switch (harmonics_box_1.getSelectedIndex()) {
		case 0:
			return makeHTML("A measurement of harmonic distortion.");
		case 1:
			return makeHTML("The mean magnitude of the bins corresponding to found harmonics divided by the median magnitude of the whole FFT frame.");
		case 2:
			return makeHTML("The spectral centroid of a frame divided by the found Praat fundamental.");
		case 3:
			return makeHTML("The \"centroid\" in terms of harmonic strength, by harmonic number, calculated directly from FFT bins corresponding only to found harmonics.");
		}
		return makeHTML("The magnitude of a selected harmonic divided by the magnitude of the fundamental.");
	}
	
	public String getHarmonicNumberText() {
		if (harmonics_box_1.getSelectedIndex() == 3)
			return makeHTML("Harmonic number:");
		return makeHTML("Number of harmonics:");
	}
	
	protected class HarmonicsOptionsBoxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			harmonics_text.setText(getHarmonicsText());
			harmonics_n_text.setText(getHarmonicNumberText());
			harmonics_n_text.setVisible(harmonics_box_1.getSelectedIndex() != 2);
			harmonics_n_field.setVisible(harmonics_box_1.getSelectedIndex() != 2);
			harmonics_frame_output_text.setVisible(harmonics_box_1.getSelectedIndex() == 3);
			harmonics_box_3.setVisible(harmonics_box_1.getSelectedIndex() == 3);
			harmonics_ratio_max_text.setVisible(harmonics_box_1.getSelectedIndex() == 4);
			harmonics_ratio_max_field.setVisible(harmonics_box_1.getSelectedIndex() == 4);
		}
	}
	
	protected class AddButtonListener implements ActionListener {
		
		boolean inserting;
		
		protected AddButtonListener(boolean inserting) {
			this.inserting = inserting;
		}
		
		public void actionPerformed(ActionEvent e) {
			String outp = "";
			if (flist.getSelectedIndex() > -1) {
				String selection = flist.getSelectedValue();
				if (selection.equals("Amplitude")) {
					outp = "amplitude";
				} else if (selection.equals("Duration")) {
					outp = "duration";
				} else if (selection.equals("Frequency (header data)")) {
					outp = "freqhd_";
					outp += outpAbbr((String) freq_hd_box.getSelectedItem());
				} else if (selection.equals("Frequency range (header data)")) {
					outp = "frange";
				} else if (selection.equals("Frequency slope (header data)")) {
					outp = "fslopehd";
				} else if (selection.equals("Frequency (slice data)")) {
					outp = "freqsd";
					int num = freq_sd_options_box.getSelectedIndex();
					if (num == 1) {
						selection = "Frequency, 1st derivative (slice data)";
						outp += "d1";
					} else if (num == 2) {
						selection = "Frequency, 2nd derivative (slice data)";
						outp += "d2";
					} else if (num == 3) {
						selection = "Frequency, elbow angle (slice data)";
						outp += "elbow";
					} else if (num == 4) {
						selection = "Frequency, start-to-end slope (slice data)";
						outp += "slope";
					}
					if (num < 3) outp += "_"+outpAbbr((String) freq_sd_box.getSelectedItem());
				} else if (selection.equals("Linear predictive coding formants")) {
					outp = "_"+formants_max_exp_freq_field.getText()+"_";
					outp += formants_min_freq_field.getText()+"_";
					outp += formants_max_bandwidth_field.getText();
					selection = (String) formants_options_box.getSelectedItem();
					if (formants_options_box.getSelectedIndex() == 0) {
						outp = "formantfreq"+outp;
						outp += "_"+formants_num_field.getText();
					} else if (formants_options_box.getSelectedIndex() == 1) {
						outp = "formantcount"+outp;
					} else if (formants_options_box.getSelectedIndex() == 2) {
						outp = "formantdiff"+outp;
						outp += "_"+formants_num_field.getText();
					} else if (formants_options_box.getSelectedIndex() == 3) {
						outp = "formantratio"+outp;
						outp += "_"+formants_num_field.getText();
					}
					outp += "_"+outpAbbr((String) formants_output_box.getSelectedItem());
				} else if (selection.equals("Mel-frequency cepstral coefficients")) {
					outp = "mfcc_";
					outp += mfcc_n_field.getText() + "_";
					outp += (String) mfcc_selector_box.getSelectedItem() + "_";
					outp += outpAbbr((String) mfcc_box.getSelectedItem());
				} else if (selection.equals("Polynomial features")) {
					outp = "poly_";
					outp += poly_order_field.getText() + "_";
					outp += (String) poly_selector_box.getSelectedItem() + "_";
					outp += outpAbbr((String) poly_box.getSelectedItem());
				} else if (selection.equals("Praat fundamental frequency")) {
					outp = "praat_";
					outp += praat_min_field.getText() + "_";
					outp += praat_max_field.getText() + "_";
					outp += outpAbbr((String) praat_box.getSelectedItem());
				} else if (selection.equals("Praat-tracked harmonics")) {
					if (harmonics_box_1.getSelectedIndex() == 0) {
						outp = "thd_";
					} else if (harmonics_box_1.getSelectedIndex() == 1) {
						outp = "hbr_";
					} else if (harmonics_box_1.getSelectedIndex() == 2) {
						outp = "hcentroid_";
					} else if (harmonics_box_1.getSelectedIndex() == 3) {
						outp = "behc_";
					} else {
						outp = "hfr_";
					}
					selection = (String) harmonics_box_1.getSelectedItem();
					if (harmonics_box_1.getSelectedIndex() != 2)
						outp += harmonics_n_field.getText() + "_";
					outp += harmonics_min_field.getText() + "_";
					outp += harmonics_max_field.getText() + "_";
					if (harmonics_box_1.getSelectedIndex() == 3)
						outp += outpAbbr((String) harmonics_box_3.getSelectedItem())+"_";
					else if (harmonics_box_1.getSelectedIndex() == 4)
						outp += (String) harmonics_ratio_max_field.getText()+"_";
					outp += outpAbbr((String) harmonics_box_2.getSelectedItem());
				} else if (selection.equals("Root mean square")) {
					outp = "rms_";
					outp += outpAbbr((String) rms_box.getSelectedItem());
				} else if (selection.equals("Spectral bandwidth")) {
					outp = "bandwidth_";
					outp += bandwidth_power_field.getText() + "_";
					if (bandwidth_normalize_check.isSelected()) {
						outp += "ny_";
					} else {
						outp += "nn_";
					}
					outp += outpAbbr((String) bandwidth_box.getSelectedItem());
				} else if (selection.equals("Spectral centroid")) {
					outp = "centroid_";
					outp += outpAbbr((String) centroid_box.getSelectedItem());
				} else if (selection.equals("Spectral contrast")) {
					float sr = settingsDialog.getSamplingRateFromAudioSource();
					int freq = (int) contrast_freq_spinner.getValue();
					int n_bands = (int) contrast_nbands_spinner.getValue();
					if (freq * Math.pow(2, n_bands) >= sr/2) {
						feControl.simpleErrorDialog("Frequency cutoff * (2 ^ number of bands) must be less than "
								+ "half the sampling rate.", 300);
						return;
					}
					outp = "contrast_";
					outp += String.valueOf(freq) + "_";
					outp += String.valueOf(n_bands) + "_";
					if (contrast_band_box.getSelectedIndex() == 0)
						outp += "all_";
					else
						outp += ((String) contrast_band_box.getSelectedItem()) + "_";
					if (contrast_linear_rb.isSelected()) {
						outp += "lin_";
					} else {
						outp += "log_";
					}
					outp += outpAbbr((String) contrast_box.getSelectedItem());
				} else if (selection.equals("Spectral flatness")) {
					outp = "flatness_";
					outp += flatness_power_field.getText() + "_";
					outp += outpAbbr((String) flatness_box.getSelectedItem());
				} else if (selection.equals("Spectral flux (onset strength)")) {
					outp = "flux_";
					outp += outpAbbr((String) flux_box.getSelectedItem());
				} else if (selection.equals("Spectral magnitude")) {
					outp = "specmag_";
					outp += specmag_min_field.getText() + "_";
					outp += specmag_max_field.getText() + "_";
					outp += outpAbbr((String) specmag_box.getSelectedItem());
				} else if (selection.equals("Spectral rolloff")) {
					outp = "rolloff_";
					outp += rolloff_threshold_field.getText() + "_";
					outp += outpAbbr((String) rolloff_box.getSelectedItem());
			/*	} else if (selection.equals("Whistle Classifier features")) {
					if (wc_options_box.getSelectedIndex() == 0) {
						selection = "Whistle Classifier fragment frequencies";
						outp += "wcfreqs_";
					} else if (wc_options_box.getSelectedIndex() == 1) {
						selection = "Whistle Classifier fragment slopes";
						outp += "wcslopes_";
					} else {
						selection = "Whistle Classifier fragment curvatures";
						outp += "wccurves_";
					}
					if (wc_output_box.getSelectedIndex() == 0)
						outp += "mean_";
					else if (wc_output_box.getSelectedIndex() == 1)
						outp += "std_";
					else
						outp += "skew_";
					outp += String.valueOf((int) wc_fragment_size_spinner.getValue()); */
				} else if (selection.equals("Zero-crossing rate")) {
					outp = "zcr_";
					outp += outpAbbr((String) zcr_box.getSelectedItem());
				}
				if (outp.length() > 0) {
					if (inserting) {
						int tableSelection = table.getSelectedRow();
						if (tableSelection == -1) {
							feControl.simpleErrorDialog("No feature in the table has been selected.");
							return;
						}
						dtmodel.insertRow(tableSelection, new Object[] {selection, outp.toLowerCase()});
					} else {
						dtmodel.addRow(new Object[] {selection, outp.toLowerCase()});
						table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, false));
						//JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
						//scrollBar.setValue(scrollBar.getMaximum());
					}
				}
			}
		}
	}
	
	/**
	 * Converts full words found in the combo boxes to an abbreviation.
	 */
	protected String outpAbbr(String inp) {
		if (inp == "Mean") {
			return "mean";
		} else if (inp == "Median") {
			return "med";
		} else if (inp == "Standard deviation") {
			return "std";
		} else if (inp == "Minimum") {
			return "min";
		} else if (inp == "Maximum") {
			return "max";
		} else if (inp == "Range") {
			return "rng";
		} else if (inp == "Mode") { // Only an option for harmonic centroid.
			return "mode";
		} else if (inp == "Skewness" || inp == "Skew") {
			return "skew";
		} else if (inp == "Full clip") {
			return "fullclip";
		}
		return "";
	}
	
	// Kudos to this: https://stackoverflow.com/questions/1842223/java-linebreaks-in-jlabels
	public String makeHTML(String inp) {
		int width = 150;
		String outp = "<html><p style=\"width:"+Integer.toString(width)+"px\">"+inp+"</p></html>";
		return outp;
	}
	
	public String makeHTML(String inp, int width) {
		String outp = "<html><p style=\"width:"+Integer.toString(width)+"px\">"+inp+"</p></html>";
		return outp;
	}
	
	/**
	 * @return A JComboBox containing the values "Mean", "Median",
	 * "Standard deviation", "Minimum", "Maximum", and "Range".
	 */
	public JComboBox<String> makeOutputBox() {
		String[] choices = {"Mean","Median","Standard deviation","Minimum","Maximum","Range"};
		JComboBox<String> outp = new JComboBox<String>(choices);
		return outp;
	}
	
	/**
	 * @return A JComboBox containing the input choices.
	 */
	public JComboBox<String> makeOutputBox(String[] choices) {
		JComboBox<String> outp = new JComboBox<String>(choices);
		return outp;
	}
	
	/**
	 * Streamlined means of constructing panels detailing info and
	 * options for each feature.
	 * @param description - The text description.
	 * @param comps - Option components following the description.
	 * Object[]s of size 1 will be added as is with one call of gridy++.
	 * Object[]s of size 2 will also be added with one call of gridy++,
	 * with one call of gridx++ after the first object is added.
	 * @param useHorizontalFill - Whether or not c.fill = GridBagConstraints.HORIZONTAL
	 * for each Object[] in "comps" of size 2.
	 */
	protected JPanel constructFeaturePanel(String description, Object[][] comps, boolean useHorizontalFill) {
		JPanel outp = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		JLabel descLabel = new JLabel();
		descLabel.setText(description);
		outp.add(descLabel, c);
		for (int i = 0; i < comps.length; i++) {
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.NORTHWEST;
			if (comps[i].length >= 1) {
				if (comps[0].length > 0) {
					if (comps[i].length == 1) {
						c.gridwidth = 2;
					}
					if (comps[i][0] instanceof String) {
						outp.add(new JLabel((String)comps[i][0], SwingConstants.LEFT), c);
					} else if (comps[i][0] instanceof Component) {
						//System.out.println("Adding component: "+comps[i][0].toString());
						outp.add((Component)comps[i][0], c);
					}
				}
			}
			if (comps[i].length == 2) {
				c.gridx++;
				if (useHorizontalFill == true) {
					c.fill = GridBagConstraints.HORIZONTAL;
				}
				c.anchor = GridBagConstraints.NORTHEAST;
				if (comps[i][1] instanceof String) {
					outp.add(new JLabel((String)comps[i][1], SwingConstants.RIGHT), c);
				} else if (comps[i][1] instanceof Component) {
					outp.add((Component)comps[i][1], c);
				}
			}
		}
		return outp;
	}
	
	/**
	 * Used for ensuring a numerical text box has a minimum possible value.
	 */
	protected void textIntUpdate(JTextField field, int lowest) {
		try {
			if (field.getText().length() < 1 || Integer.valueOf(field.getText()) < lowest) {
				field.setText(Integer.toString(lowest));
			}
		} catch (Exception e) {
			field.setText(Integer.toString(lowest));
		}
	}
	
	/**
	 * Used for ensuring a numerical text box has a minimum possible value.
	 */
	protected void textDoubleUpdate(JTextField field, double lowest) {
		try {
			if (field.getText().length() < 1 || Double.valueOf(field.getText()) < lowest) {
				field.setText(Double.toString(lowest));
			}
		} catch (Exception e) {
			field.setText(Double.toString(lowest));
		}
	}
	
	/**
	 * Updates mfcc_selector_box when the number in mfcc_n_field changes.
	 */
	protected void mfccBoxUpdate() {
		if (mfcc_n_field.getText().length() < 1) {
        	mfcc_n_field.setText("1");
        } else if (Integer.valueOf(mfcc_n_field.getText()) < 1) {
        	mfcc_n_field.setText("1");
        }
		mfcc_selector_box.removeAllItems();
		mfcc_selector_box.addItem("All");
        for (int i = 1; i <= Integer.valueOf(mfcc_n_field.getText()); i++) {
        	mfcc_selector_box.addItem(Integer.toString(i));
        }
	}
	
	/**
	 * Updates poly_selector_box when the number in poly_order_field changes.
	 */
	protected void polyBoxUpdate() {
		if (poly_order_field.getText().length() < 1) {
			poly_order_field.setText("0");
        } else if (Integer.valueOf(poly_order_field.getText()) < 0) {
        	poly_order_field.setText("0");
        }
		poly_selector_box.removeAllItems();
		poly_selector_box.addItem("All");
        for (int i = 0; i <= Integer.valueOf(poly_order_field.getText()); i++) {
        	poly_selector_box.addItem(Integer.toString(i));
        }
	}
	
	protected void contrastBoxUpdate() {
		contrast_band_box.removeAllItems();
		contrast_band_box.addItem("All");
		for (int i = 1; i <= (int) contrast_nbands_spinner.getValue(); i++)
        	contrast_band_box.addItem(Integer.toString(i));
	}
	
	/**
	 * Limits entry in text field to numbers only.
	 */
	protected PlainDocument JIntFilter() {
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
	 * Limits entry in text field to numbers and a single decimal point only.
	 */
	protected PlainDocument JDoubleFilter(JTextField field) {
		PlainDocument d = new PlainDocument() {
			@Override
	        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
	            char c = str.charAt(0);
	            String fieldText = field.getText();
	            if ((!fieldText.contains(".")) && ((c >= '0' && c <= '9') || (c == '.'))) {
	            	super.insertString(offs, str, a);
		        } else if (fieldText.contains(".") && (c >= '0' && c <= '9')) {
		        	super.insertString(offs, str, a);
		        }
	        }
		};
		return d;
	}
	
	/**
	 * Ensures certain JComboBoxes update when their respective
	 * text fields change values.
	 */
	protected PlainDocument fieldToBoxFilter(JTextField field, String cardName) {
		PlainDocument d = new PlainDocument() {
			@Override
	        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
	            char c = str.charAt(0);
	            if (c >= '0' && c <= '9') {
	            	super.insertString(offs, str, a);
	            	if (field.getText().length() > 0) {
	            		if (Integer.valueOf(field.getText()) > 0) {
	            			if (cardName == "mfcc") {
	            				mfccBoxUpdate();
	            			} else if (cardName == "poly") {
	            				polyBoxUpdate();
	            			}
	            			
	            		}
	            	}
		        }
	        }
		};
		return d;
	}
	
	/**
	 * Returns false, as the feature table in the settings dialog
	 * is already updated by the add button.
	 */
	@Override
	public boolean getParams() {
		return false;
	}

	@Override
	public void cancelButtonPressed() {}

	@Override
	public void restoreDefaultSettings() {}
}