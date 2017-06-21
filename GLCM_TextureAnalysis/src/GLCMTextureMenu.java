import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.plugin.DragAndDrop;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;

import java.awt.EventQueue;

import javax.swing.JCheckBox;
import javax.swing.JFrame;

import java.awt.Button;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.Label;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Panel;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.io.File;

import javax.swing.Box;
import javax.swing.SwingConstants;
import javax.swing.JSeparator;


public class GLCMTextureMenu extends JFrame implements WindowListener, Runnable {


	protected ImagePlus imp;
	public boolean done = false;
	protected Thread thread;
	
	private int xcoordi;
	private int ycoordi;
	
	GLCM_TextureAnalysis glcm_texture;
	
	CheckboxGroup windowSizeCheck;
	private Checkbox windowSqure_3;
	private Checkbox windowSqure_5;
	private Checkbox windowSqure_7;
	private Checkbox windowSqure_9;
	private Checkbox windowSqure_etc;
	TextField windowSizetextField;
	
	CheckboxGroup histogramBinsCheck;
	private Checkbox histoBins_8;
	private Checkbox histoBins_16;
	private Checkbox histoBins_32;
	private Checkbox histoBins_64;
	private Checkbox histoBins_128;
	private Checkbox histoBins_etc;
	TextField histoBinstextField;
	
	CheckboxGroup histogramTypeCheck;
	private Checkbox histoType_auto;
	private Checkbox histoType_autoInRoi;
	private Checkbox histoType_meanstd;
	private Checkbox histoType_manual;
	TextField stdtextField;
	TextField centerSItextField;
	TextField intervalSItextField;
	
	public Checkbox direction_0;
	public Checkbox direction_45;
	public Checkbox direction_90;
	public Checkbox direction_135;
	public Checkbox direction_average;
	public Checkbox direction_all;
	
	public Checkbox glcm_ASM;
	public Checkbox glcm_CON;
	public Checkbox glcm_ENT;
	public Checkbox glcm_MEAN;
	public Checkbox glcm_VAR;
	public Checkbox glcm_ACO;
	public Checkbox glcm_CLP;
	public Checkbox glcm_CLS;
	public Checkbox glcm_CLT;
	public Checkbox glcm_DIS;
	public Checkbox glcm_HOM;
	public Checkbox glcm_IDM;
	public Checkbox glcm_IDMN;
	public Checkbox glcm_IDN;
	public Checkbox glcm_CORR;
	public Checkbox glcm_IVAR;
	public Checkbox glcm_MAXP;
	public Checkbox glcm_SUMA;
	public Checkbox glcm_SUMV;
	public Checkbox glcm_SUME;
	public Checkbox glcm_DIFV;
	public Checkbox glcm_DIFE;
	public Checkbox glcm_IMC1;
	public Checkbox glcm_IMC2;
	public Checkbox glcm_all;
	public Checkbox roilonlycheck;
	public Checkbox create_map;
	public Checkbox create_value;
	
	public Button processbtn;
	
	
	
	
	

	/**
	 * Launch the application.
	 */

	/**
	 * Create the application.
	 */
	public GLCMTextureMenu() {
		super("Texture Option");
		initialize();
	}
	//	public void setBaserangenum2(String _input){
	//		baserangenum2.setText(""+_input);
	//	}
	public GLCMTextureMenu(ImagePlus imp, GLCM_TextureAnalysis _glcm_texture){
		super("Texture Option");
		this.glcm_texture=_glcm_texture;
		this.imp = imp;
		initialize();
		System.out.println(imp.getWindow().getLocation().x+" "+imp.getWindow().getWidth());
		thread = new Thread( this, "MeasureStack");
		thread.start();

	}


	public void run() {
		try {
			this.setVisible(true);	
			this.setResizable(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.setLocation(imp.getWindow().getLocation().x+imp.getWindow().getWidth(), imp.getWindow().getLocation().y);

	}


	/**
	 * Initialize the contents of the frame
	 */
	private void initialize() {
		this.setBounds(100, 100, 268, 680);
		//		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.getContentPane().setLayout(null);

		Label label = new Label("Window Size");
		label.setAlignment(Label.CENTER);
		label.setBounds(10, 5, 231, 23);
		getContentPane().add(label);
		
		windowSizeCheck = new CheckboxGroup();
		windowSqure_3 = new Checkbox("3 x 3",windowSizeCheck,false);
		windowSqure_3.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
//					glcm_texture.windowSize = 3;
					windowSizetextField.setEnabled(false);
				}
			}
		});
		windowSqure_3.setBounds(10, 32, 47, 23);
		getContentPane().add(windowSqure_3);
		
		windowSqure_5 = new Checkbox("5 x 5",windowSizeCheck,true);
		windowSqure_5.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
//					glcm_texture.windowSize = 5;
					windowSizetextField.setEnabled(false);
				}
			}
		});
		windowSqure_5.setBounds(71, 32, 47, 23);
		getContentPane().add(windowSqure_5);
		
		windowSqure_7 = new Checkbox("7 x 7",windowSizeCheck,false);
		windowSqure_7.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
//					glcm_texture.windowSize = 7;
					windowSizetextField.setEnabled(false);
				}
			}
		});
		windowSqure_7.setBounds(132, 32, 47, 23);
		getContentPane().add(windowSqure_7);
		
		windowSqure_9 = new Checkbox("9 x 9",windowSizeCheck,false);
		windowSqure_9.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
//					glcm_texture.windowSize = 9;
					windowSizetextField.setEnabled(false);
				}
			}
		});
		windowSqure_9.setBounds(194, 32, 47, 23);
		getContentPane().add(windowSqure_9);
		
		windowSqure_etc = new Checkbox("etc..",windowSizeCheck,false);
		windowSqure_etc.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED){
//					glcm_texture.windowSize = 11;
					windowSizetextField.setEnabled(true);
				}
			}
		});
		windowSqure_etc.setBounds(10, 61, 47, 23);
		getContentPane().add(windowSqure_etc);
		
		windowSizetextField = new TextField();
		windowSizetextField.setEnabled(false);
		windowSizetextField.setText(""+11);
		windowSizetextField.addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				if ((int)e.getKeyChar()==48||(int)e.getKeyChar()==49||(int)e.getKeyChar()==50||(int)e.getKeyChar()==51||(int)e.getKeyChar()==52||(int)e.getKeyChar()==53||(int)e.getKeyChar()==54||(int)e.getKeyChar()==55||(int)e.getKeyChar()==56||(int)e.getKeyChar()==57){
				}else{
					e.consume();
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
				try{
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					int windowSizeTF = Integer.parseInt(windowSizetextField.getText());	
					if(windowSizeTF%2==0){windowSizeTF=windowSizeTF+1; windowSizetextField.setText(""+(windowSizeTF));}
					if(windowSizeTF>=imp.getProcessor().getWidth()){windowSizeTF=imp.getProcessor().getWidth()-1; windowSizetextField.setText(""+(windowSizeTF));}
					
//					glcm_texture.windowSize=Integer.parseInt(windowSizetextField.getText());	
//					if(glcm_texture.windowSize%2==0){glcm_texture.windowSize=glcm_texture.windowSize+1; windowSizetextField.setText(""+(glcm_texture.windowSize));}
//					if(glcm_texture.windowSize>=imp.getProcessor().getWidth()){glcm_texture.windowSize=imp.getProcessor().getWidth()-1; windowSizetextField.setText(""+(glcm_texture.windowSize));}
					
					System.out.println(glcm_texture.windowSize);
				}catch (NumberFormatException e1){};
			}
		});
		windowSizetextField.setBounds(62, 62, 24, 20);
		getContentPane().add(windowSizetextField);

		JSeparator separator = new JSeparator();
		separator.setBounds(9, 90, 232, 6);
		getContentPane().add(separator);
		
		
		Label label_1 = new Label("Histogram bins");
		label_1.setAlignment(Label.CENTER);
		label_1.setBounds(10, 92, 231, 23);
		getContentPane().add(label_1);
		histogramBinsCheck=new CheckboxGroup();
		histoBins_8 = new Checkbox("8", histogramBinsCheck, false);
		histoBins_8.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramBins = 8;
				histoBinstextField.setEnabled(false);
			}
		});
		histoBins_8.setBounds(10, 118, 33, 23);
		getContentPane().add(histoBins_8);
		
		histoBins_16 = new Checkbox("16", histogramBinsCheck, false);
		histoBins_16.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramBins = 16;
				histoBinstextField.setEnabled(false);
			}
		});
		histoBins_16.setBounds(58, 118, 33, 23);
		getContentPane().add(histoBins_16);
		
		histoBins_32 = new Checkbox("32", histogramBinsCheck, false);
		histoBins_32.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramBins = 32;
				histoBinstextField.setEnabled(false);
			}
		});
		histoBins_32.setBounds(106, 118, 33, 23);
		getContentPane().add(histoBins_32);
		
		histoBins_64 = new Checkbox("64", histogramBinsCheck, true);
		histoBins_64.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramBins = 64;
				histoBinstextField.setEnabled(false);
			}
		});
		histoBins_64.setBounds(154, 118, 33, 23);
		getContentPane().add(histoBins_64);
		
		histoBins_128 = new Checkbox("128", histogramBinsCheck, false);
		histoBins_128.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramBins = 128;
				histoBinstextField.setEnabled(false);
			}
		});
		histoBins_128.setBounds(201, 118, 40, 23);
		getContentPane().add(histoBins_128);

		histoBins_etc = new Checkbox("etc..", histogramBinsCheck, false);
		histoBins_etc.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramBins = 256;
				histoBinstextField.setEnabled(true);
			}
		});
		histoBins_etc.setBounds(10, 149, 47, 23);
		getContentPane().add(histoBins_etc);
		
		histoBinstextField = new TextField();
		histoBinstextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try{
//					glcm_texture.histogramBins=Integer.parseInt(histoBinstextField.getText());	
				}catch (NumberFormatException e1){};
			}
			@Override
			public void keyTyped(KeyEvent e) {
				if ((int)e.getKeyChar()==48||(int)e.getKeyChar()==49||(int)e.getKeyChar()==50||(int)e.getKeyChar()==51||(int)e.getKeyChar()==52||(int)e.getKeyChar()==53||(int)e.getKeyChar()==54||(int)e.getKeyChar()==55||(int)e.getKeyChar()==56||(int)e.getKeyChar()==57){
				}else{
					e.consume();
				}
			}
		});
		histoBinstextField.setText("256");
		histoBinstextField.setEnabled(false);
		histoBinstextField.setBounds(62, 148, 33, 20);
		getContentPane().add(histoBinstextField);

		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(10, 178, 232, 6);
		getContentPane().add(separator_1);
		
		
		Label label_2 = new Label("Histogram Type");
		label_2.setAlignment(Label.CENTER);
		label_2.setBounds(10, 180, 231, 23);
		getContentPane().add(label_2);
		histogramTypeCheck = new CheckboxGroup();
		histoType_auto = new Checkbox("Auto", histogramTypeCheck, true);
		histoType_auto.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramType = 1;
				stdtextField.setEnabled(false);
				centerSItextField.setEnabled(false);
				intervalSItextField.setEnabled(false);
			}
		});
		histoType_auto.setBounds(11, 209, 47, 23);
		getContentPane().add(histoType_auto);
		
		histoType_autoInRoi = new Checkbox("Auto In ROI", histogramTypeCheck, false);
		histoType_autoInRoi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramType = 2;
				stdtextField.setEnabled(false);
				centerSItextField.setEnabled(false);
				intervalSItextField.setEnabled(false);
			}
		});
		histoType_autoInRoi.setBounds(145, 209, 87, 23);
		getContentPane().add(histoType_autoInRoi);
		
		histoType_meanstd = new Checkbox("Mean-Std", histogramTypeCheck, false);
		histoType_meanstd.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramType = 3;
				stdtextField.setEnabled(true);
				centerSItextField.setEnabled(false);
				intervalSItextField.setEnabled(false);
			}
		});
		histoType_meanstd.setBounds(10, 238, 76, 23);
		getContentPane().add(histoType_meanstd);
		
		stdtextField = new TextField();
		stdtextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try{
//					glcm_texture.SI_std=Double.parseDouble(stdtextField.getText());	
				}catch (NumberFormatException e1){};
			}
			@Override
			public void keyTyped(KeyEvent e) {
				if ((int)e.getKeyChar()==48||(int)e.getKeyChar()==49||(int)e.getKeyChar()==50||(int)e.getKeyChar()==51||(int)e.getKeyChar()==52||(int)e.getKeyChar()==53||(int)e.getKeyChar()==54||(int)e.getKeyChar()==55||(int)e.getKeyChar()==56||(int)e.getKeyChar()==57){
				}else{
					e.consume();
				}
			}
		});
		stdtextField.setEnabled(false);
		stdtextField.setText("1.96");
		stdtextField.setBounds(91, 239, 44, 20);
		getContentPane().add(stdtextField);
		
		histoType_manual = new Checkbox("Mamual", histogramTypeCheck, false);
		histoType_manual.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
//				glcm_texture.histogramType = 4;
				stdtextField.setEnabled(false);
				centerSItextField.setEnabled(true);
				intervalSItextField.setEnabled(true);
			}
		});
		histoType_manual.setBounds(145, 238, 76, 23);
		getContentPane().add(histoType_manual);
		
		Label label_4 = new Label("Center SI");
		label_4.setAlignment(Label.CENTER);
		label_4.setBounds(10, 267, 57, 23);
		getContentPane().add(label_4);
		
		centerSItextField = new TextField();
		centerSItextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try{
//					glcm_texture.SI_center=Double.parseDouble(centerSItextField.getText());	
				}catch (NumberFormatException e1){};
			}
			@Override
			public void keyTyped(KeyEvent e) {
				if ((int)e.getKeyChar()==48||(int)e.getKeyChar()==49||(int)e.getKeyChar()==50||(int)e.getKeyChar()==51||(int)e.getKeyChar()==52||(int)e.getKeyChar()==53||(int)e.getKeyChar()==54||(int)e.getKeyChar()==55||(int)e.getKeyChar()==56||(int)e.getKeyChar()==57){
				}else{
					e.consume();
				}
			}
		});
		centerSItextField.setText("320");
		centerSItextField.setEnabled(false);
		centerSItextField.setBounds(70, 268, 44, 20);
		getContentPane().add(centerSItextField);
		
		Label label_5 = new Label("Interval SI");
		label_5.setAlignment(Label.CENTER);
		label_5.setBounds(134, 267, 57, 23);
		getContentPane().add(label_5);
		
		intervalSItextField = new TextField();
		intervalSItextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try{
//					glcm_texture.SI_interval=Double.parseDouble(intervalSItextField.getText());	
				}catch (NumberFormatException e1){};
			}
			@Override
			public void keyTyped(KeyEvent e) {
				if ((int)e.getKeyChar()==48||(int)e.getKeyChar()==49||(int)e.getKeyChar()==50||(int)e.getKeyChar()==51||(int)e.getKeyChar()==52||(int)e.getKeyChar()==53||(int)e.getKeyChar()==54||(int)e.getKeyChar()==55||(int)e.getKeyChar()==56||(int)e.getKeyChar()==57){
				}else{
					e.consume();
				}
			}
		});
		intervalSItextField.setText("10");
		intervalSItextField.setEnabled(false);
		intervalSItextField.setBounds(197, 268, 44, 20);
		getContentPane().add(intervalSItextField);
		
		JSeparator separator_2 = new JSeparator();
		separator_2.setBounds(10, 296, 232, 6);
		getContentPane().add(separator_2);
		
		Label label_6 = new Label("Direction");
		label_6.setAlignment(Label.CENTER);
		label_6.setBounds(51, 301, 150, 23);
		getContentPane().add(label_6);
		
		direction_0 = new Checkbox("0\u00B0");
		direction_0.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					direction_all.setState(false);
				}
			}
		});
		direction_0.setBounds(11, 324, 41, 23);
		getContentPane().add(direction_0);
		
		direction_45 = new Checkbox("45\u00B0");
		direction_45.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					direction_all.setState(false);
				}
			}
		});
		direction_45.setBounds(61, 324, 41, 23);
		getContentPane().add(direction_45);
		
		direction_90 = new Checkbox("90\u00B0");
		direction_90.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					direction_all.setState(false);
				}
			}
		});
		direction_90.setBounds(111, 324, 41, 23);
		getContentPane().add(direction_90);
		
		direction_135 = new Checkbox("135\u00B0");
		direction_135.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					direction_all.setState(false);
				}
			}
		});
		direction_135.setBounds(161, 324, 41, 23);
		getContentPane().add(direction_135);
		
		direction_average = new Checkbox("ave.");
		direction_average.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					direction_all.setState(false);
				}
			}
		});
		direction_average.setBounds(211, 324, 41, 23);
		getContentPane().add(direction_average);
		
		direction_all = new Checkbox("All");
		direction_all.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.SELECTED){
					direction_0.setState(true);
					direction_45.setState(true);
					direction_90.setState(true);
					direction_135.setState(true);
					direction_average.setState(true);
				}else{
					direction_0.setState(false);
					direction_45.setState(false);
					direction_90.setState(false);
					direction_135.setState(false);
					direction_average.setState(false);
				}
			}
		});
		direction_all.setBounds(11, 301, 43, 23);
		getContentPane().add(direction_all);

		JSeparator separator_3 = new JSeparator();
		separator_3.setBounds(9, 353, 232, 6);
		getContentPane().add(separator_3);
		

		Label label_3 = new Label("GLCM Parameter");
		label_3.setAlignment(Label.CENTER);
		label_3.setBounds(50, 365, 151, 23);
		this.getContentPane().add(label_3);

		glcm_ASM = new Checkbox("ASM");
		glcm_ASM.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_ASM.setBounds(4, 394, 43, 23);
		getContentPane().add(glcm_ASM);
		
		glcm_CON = new Checkbox("CON");
		glcm_CON.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_CON.setBounds(53, 394, 43, 23);
		getContentPane().add(glcm_CON);
		
		glcm_ENT = new Checkbox("ENT");
		glcm_ENT.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_ENT.setBounds(104, 394, 43, 23);
		getContentPane().add(glcm_ENT);
		
		glcm_MEAN = new Checkbox("MEAN");
		glcm_MEAN.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_MEAN.setBounds(153, 394, 50, 23);
		getContentPane().add(glcm_MEAN);
		
		glcm_VAR = new Checkbox("VAR");
		glcm_VAR.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_VAR.setBounds(208, 394, 43, 23);
		getContentPane().add(glcm_VAR);
		
		glcm_ACO = new Checkbox("ACO");
		glcm_ACO.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_ACO.setBounds(4, 423, 43, 23);
		getContentPane().add(glcm_ACO);
		
		glcm_CLP = new Checkbox("CLP");
		glcm_CLP.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_CLP.setBounds(53, 423, 43, 23);
		getContentPane().add(glcm_CLP);
		
		glcm_CLS = new Checkbox("CLS");
		glcm_CLS.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_CLS.setBounds(104, 423, 43, 23);
		getContentPane().add(glcm_CLS);
		
		glcm_CLT = new Checkbox("CLT");
		glcm_CLT.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_CLT.setBounds(153, 423, 50, 23);
		getContentPane().add(glcm_CLT);
		
		glcm_DIS = new Checkbox("DIS");
		glcm_DIS.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_DIS.setBounds(208, 423, 43, 23);
		getContentPane().add(glcm_DIS);
		
		glcm_HOM = new Checkbox("HOM");
		glcm_HOM.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_HOM.setBounds(4, 452, 43, 23);
		getContentPane().add(glcm_HOM);
		
		glcm_IDM = new Checkbox("IDM");
		glcm_IDM.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_IDM.setBounds(67, 452, 43, 23);
		getContentPane().add(glcm_IDM);
		
		glcm_IDMN = new Checkbox("IDMN");
		glcm_IDMN.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_IDMN.setBounds(130, 452, 45, 23);
		getContentPane().add(glcm_IDMN);
		
		glcm_IDN = new Checkbox("IDN");
		glcm_IDN.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_IDN.setBounds(194, 452, 52, 23);
		getContentPane().add(glcm_IDN);
		
		glcm_CORR = new Checkbox("CORR");
		glcm_CORR.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_CORR.setBounds(4, 481, 52, 23);
		getContentPane().add(glcm_CORR);
		
		glcm_IVAR = new Checkbox("IVAR");
		glcm_IVAR.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_IVAR.setBounds(67, 481, 52, 23);
		getContentPane().add(glcm_IVAR);
		
		glcm_MAXP = new Checkbox("MAXP");
		glcm_MAXP.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_MAXP.setBounds(130, 481, 52, 23);
		getContentPane().add(glcm_MAXP);
		
		glcm_SUMA = new Checkbox("SUMA");
		glcm_SUMA.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_SUMA.setBounds(194, 481, 52, 23);
		getContentPane().add(glcm_SUMA);
		
		glcm_SUMV = new Checkbox("SUMV");
		glcm_SUMV.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_SUMV.setBounds(4, 510, 52, 23);
		getContentPane().add(glcm_SUMV);
		
		glcm_SUME = new Checkbox("SUME");
		glcm_SUME.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_SUME.setBounds(67, 510, 52, 23);
		getContentPane().add(glcm_SUME);
		
		glcm_DIFV = new Checkbox("DIFV");
		glcm_DIFV.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_DIFV.setBounds(130, 510, 52, 23);
		getContentPane().add(glcm_DIFV);
		
		glcm_DIFE = new Checkbox("DIFE");
		glcm_DIFE.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_DIFE.setBounds(194, 510, 52, 23);
		getContentPane().add(glcm_DIFE);
		
		glcm_IMC1 = new Checkbox("IMC1");
		glcm_IMC1.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_IMC1.setBounds(4, 539, 52, 23);
		getContentPane().add(glcm_IMC1);
		
		glcm_IMC2 = new Checkbox("IMC2");
		glcm_IMC2.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.DESELECTED){
					glcm_all.setState(false);
				}
			}
		});
		glcm_IMC2.setBounds(67, 539, 52, 23);
		getContentPane().add(glcm_IMC2);
		
		glcm_all = new Checkbox("All");
		glcm_all.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.SELECTED){
					glcm_ASM.setState(true);
					glcm_CON.setState(true);
					glcm_ENT.setState(true);
					glcm_MEAN.setState(true);
					glcm_VAR.setState(true);
					glcm_ACO.setState(true);
					glcm_CLP.setState(true);
					glcm_CLS.setState(true);
					glcm_CLT.setState(true);
					glcm_DIS.setState(true);
					glcm_HOM.setState(true);
					glcm_IDM.setState(true);
					glcm_IDMN.setState(true);
					glcm_IDN.setState(true);
					glcm_CORR.setState(true);
					glcm_IVAR.setState(true);
					glcm_MAXP.setState(true);
					glcm_SUMA.setState(true);
					glcm_SUMV.setState(true);
					glcm_SUME.setState(true);
					glcm_DIFV.setState(true);
					glcm_DIFE.setState(true);
					glcm_IMC1.setState(true);
					glcm_IMC2.setState(true);
				}else{
					glcm_ASM.setState(false);
					glcm_CON.setState(false);
					glcm_ENT.setState(false);
					glcm_MEAN.setState(false);
					glcm_VAR.setState(false);
					glcm_ACO.setState(false);
					glcm_CLP.setState(false);
					glcm_CLS.setState(false);
					glcm_CLT.setState(false);
					glcm_DIS.setState(false);
					glcm_HOM.setState(false);
					glcm_IDM.setState(false);
					glcm_IDMN.setState(false);
					glcm_IDN.setState(false);
					glcm_CORR.setState(false);
					glcm_IVAR.setState(false);
					glcm_MAXP.setState(false);
					glcm_SUMA.setState(false);
					glcm_SUMV.setState(false);
					glcm_SUME.setState(false);
					glcm_DIFV.setState(false);
					glcm_DIFE.setState(false);
					glcm_IMC1.setState(false);
					glcm_IMC2.setState(false);
				}
			}
		});
		glcm_all.setBounds(10, 365, 43, 23);
		getContentPane().add(glcm_all);
		

		JSeparator separator_5 = new JSeparator();
		separator_5.setBounds(10, 568, 232, 6);
		getContentPane().add(separator_5);

		roilonlycheck = new Checkbox("Selection only");
		roilonlycheck.setEnabled(true);
		roilonlycheck.setBounds(10, 580, 101, 23);
		this.getContentPane().add(roilonlycheck);
		
		create_map = new Checkbox("Map");
		create_map.setEnabled(true);
		create_map.setBounds(130, 580, 52, 23);
		getContentPane().add(create_map);
		
		create_value = new Checkbox("Value");
		create_value.setBounds(190, 580, 52, 23);
		getContentPane().add(create_value);
		
		processbtn = new Button("Process");
		processbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					glcm_texture.processResult();
			}
		});
		processbtn.setBounds(135, 609, 106, 23);
		this.getContentPane().add(processbtn);
		
		
		
 	
	}




	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		shutDown();
	}
	public void shutDown(){
		done = true;
		setVisible(false);
		dispose();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}
}
