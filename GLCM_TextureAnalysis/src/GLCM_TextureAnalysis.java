import ij.plugin.PlugIn;
import ij.plugin.WandToolOptions;
import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.util.DicomTools;
import ij.gui.*;
import ij.measure.*;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;



public class GLCM_TextureAnalysis implements PlugIn , Measurements {
	private ImagePlus imp;
	ImagePlus glcm_ASM, glcm_CON, glcm_ENT, glcm_MEAN, glcm_VAR, glcm_ACO, glcm_CLP, glcm_CLS, glcm_CLT, glcm_DIS, glcm_HOM, glcm_IDM, glcm_IDMN, glcm_IDN, glcm_CORR;
	ImagePlus glcm_IVAR, glcm_MAXP, glcm_SUMA, glcm_SUMV, glcm_SUME, glcm_DIFA, glcm_DIFV, glcm_DIFE, glcm_IMC1, glcm_IMC2, glcm_all;
	
	int _W;
	int _H;
	int _Slice;
	int currentSlice;
	int currentFrame;

	boolean isHyper;
	
	GLCMTextureMenu texturemenu;
	int windowSize;
	int histogramBins;
	int histogramType; // 0=auto,1=mean-std,2=manual
	double SI_center;
	double SI_interval;	
	double SI_std;
	double histoMaxValue;
	double histoMinValue;
	double SI_mean, SI_sum, SI_sum2;
	ResultsTable texture_Summary;
	
	float[][] img2DArray; //img2DArray=new float[_Slice][_W][_H];
	
	public void run(String arg) {
		this.imp = IJ.getImage();
		
		this.imp = checkThatImageIsGray32(imp); //skc32bit

		if (imp.isHyperStack()){
			isHyper=true;
		}else{
			isHyper=false;
		}

		_Slice=imp.getNSlices();
		_W=imp.getWidth();
		_H=imp.getHeight();
		histoMaxValue = Double.MIN_VALUE;
		histoMinValue = Double.MAX_VALUE;
		
		SI_mean = 0;
		SI_sum = 0;
		SI_sum2 = 0;

		texturemenu = new GLCMTextureMenu(imp,this);

	}

	private ImagePlus checkThatImageIsGray32(ImagePlus i) {
		if (i.getType() != ImagePlus.GRAY32)
		{
			int nSlices = imp.getStackSize();
			if(nSlices < 2){
				ImageConverter ic = new ImageConverter(i);
				ic.convertToGray32();
			}else{
				StackConverter ic = new StackConverter(i);
				ic.convertToGray32();
			}

		}
		return i;
	}

	public void processResult(){
		boolean valueBoxCheck = texturemenu.create_value.getState();
		boolean mapBoxCheck = texturemenu.create_map.getState();
		boolean selectionBoxCheck = texturemenu.roilonlycheck.getState();
		
		Checkbox windowSizeCheckBox = texturemenu.windowSizeCheck.getSelectedCheckbox();
		Checkbox binsCheckBox = texturemenu.histogramBinsCheck.getSelectedCheckbox();
		Checkbox binsTypeBox = texturemenu.histogramTypeCheck.getSelectedCheckbox();
		String winSize = windowSizeCheckBox.getLabel();
		String binsSize = binsCheckBox.getLabel();
		String binsType = binsTypeBox.getLabel();
		
		if(winSize.equals("etc..")){
			windowSize = Integer.parseInt(texturemenu.windowSizetextField.getText());
		}else{
			windowSize = Integer.parseInt(winSize.substring(0, 1));
		}
		
		if(binsSize.equals("etc..")){
			histogramBins = Integer.parseInt(texturemenu.histoBinstextField.getText());
		}else{
			histogramBins = Integer.parseInt(binsSize);
		}
		
		if(binsType.equals("Auto")){
			histogramType = 1;
		}else if(binsType.equals("Auto In ROI")){
			histogramType = 2;
		}else if(binsType.equals("Mean-Std")){
			histogramType = 3;
			SI_std = Double.parseDouble(texturemenu.stdtextField.getText());
		}else if(binsType.equals("Mamual")){
			histogramType = 4;
			SI_center=Double.parseDouble(texturemenu.centerSItextField.getText());	
			SI_interval=Double.parseDouble(texturemenu.intervalSItextField.getText());	
		}

		
		boolean glcmCheck = false;
		if(texturemenu.direction_0.getState()==false && texturemenu.direction_45.getState()==false && texturemenu.direction_90.getState()==false &&
				texturemenu.direction_135.getState()==false && texturemenu.direction_average.getState()==false){
			IJ.showMessage("At least one degree must be selected...");
			return;
		}

		if(texturemenu.glcm_ASM.getState()==false && texturemenu.glcm_CON.getState()==false && texturemenu.glcm_ENT.getState()==false && texturemenu.glcm_MEAN.getState()==false
				&& texturemenu.glcm_VAR.getState()==false && texturemenu.glcm_CLP.getState()==false && texturemenu.glcm_CLS.getState()==false && texturemenu.glcm_CLT.getState()==false
				&& texturemenu.glcm_DIS.getState()==false && texturemenu.glcm_HOM.getState()==false && texturemenu.glcm_IDM.getState()==false && texturemenu.glcm_IDMN.getState()==false
				&& texturemenu.glcm_IDN.getState()==false && texturemenu.glcm_CORR.getState()==false && texturemenu.glcm_IVAR.getState()==false && texturemenu.glcm_MAXP.getState()==false
				&& texturemenu.glcm_SUMA.getState()==false && texturemenu.glcm_SUMV.getState()==false && texturemenu.glcm_SUME.getState()==false 
				&& texturemenu.glcm_DIFV.getState()==false && texturemenu.glcm_DIFE.getState()==false && texturemenu.glcm_IMC1.getState()==false && texturemenu.glcm_IMC2.getState()==false){
			IJ.showMessage("At least one parameter must be selected...");
			return;
		}

		if(valueBoxCheck==false && mapBoxCheck==false ){
			IJ.showMessage("You must select at least one of Map or Value..."); 
			return;
		}

		if(texturemenu.glcm_ASM.getState()==true || texturemenu.glcm_CON.getState()==true || texturemenu.glcm_ENT.getState()==true || texturemenu.glcm_MEAN.getState()==true
				|| texturemenu.glcm_VAR.getState()==true || texturemenu.glcm_CLP.getState()==true || texturemenu.glcm_CLS.getState()==true || texturemenu.glcm_CLT.getState()==true
				|| texturemenu.glcm_DIS.getState()==true || texturemenu.glcm_HOM.getState()==true || texturemenu.glcm_IDM.getState()==true || texturemenu.glcm_IDMN.getState()==true
				|| texturemenu.glcm_IDN.getState()==true || texturemenu.glcm_CORR.getState()==true || texturemenu.glcm_IVAR.getState()==true || texturemenu.glcm_MAXP.getState()==true
				|| texturemenu.glcm_SUMA.getState()==true || texturemenu.glcm_SUMV.getState()==true || texturemenu.glcm_SUME.getState()==true 
				|| texturemenu.glcm_DIFV.getState()==true || texturemenu.glcm_DIFE.getState()==true || texturemenu.glcm_IMC1.getState()==true || texturemenu.glcm_IMC2.getState()==true){
			glcmCheck = true;
		}


		if (isHyper){ //if hyperstack

			ImageStack stack1 = imp.getStack(); 
			currentSlice=imp.getZ();
			currentFrame=imp.getT();
			img2DArray=new float[_W][_H];

			float[] tmpPixels = (float[]) stack1.getPixels((currentFrame-1)*_Slice+(currentSlice));
			for(int indexH=0;indexH<_H;indexH++){
				for(int indexW=0;indexW<_W;indexW++){
					img2DArray[indexW][indexH]=tmpPixels[indexH*_W+indexW];
					if(histoMaxValue<tmpPixels[indexH*_W+indexW])histoMaxValue=tmpPixels[indexH*_W+indexW];
					if(histoMinValue>tmpPixels[indexH*_W+indexW])histoMinValue=tmpPixels[indexH*_W+indexW];
				}
			}

		}else{

			ImageStack stack1 = imp.getStack();
			currentSlice=imp.getZ();
			img2DArray=new float[_W][_H];

			float[] tmpPixels = (float[]) stack1.getPixels(currentSlice);
			for(int indexH=0;indexH<_H;indexH++){
				for(int indexW=0;indexW<_W;indexW++){
					img2DArray[indexW][indexH]=tmpPixels[indexH*_W+indexW];
					if(histoMaxValue<tmpPixels[indexH*_W+indexW])histoMaxValue=tmpPixels[indexH*_W+indexW];
					if(histoMinValue>tmpPixels[indexH*_W+indexW])histoMinValue=tmpPixels[indexH*_W+indexW];
				}
			}
		}
		
		if(histogramType==2 && selectionBoxCheck == true){//auto In ROI. get Min/Max
			IJ.showStatus("ROI Calculation...");

			Roi roi = imp.getRoi();
			if(roi!=null && roi.isArea()){
				histoMaxValue = imp.getStatistics().max;
				histoMinValue = imp.getStatistics().min;
			}
		}	
			
		int totalPixelRoi = 1;
		if(histogramType==3){//Mean-Std
			IJ.showStatus("ROI Calculation...");
			int currentRoiPixelCount = 0;
			if(selectionBoxCheck){
				Roi roi = imp.getRoi();
				if (roi!=null && !roi.isArea()) return;
				ImageProcessor ip = imp.getProcessor();
				ImageProcessor mask = roi!=null?roi.getMask():null;
				Rectangle r = roi!=null?roi.getBounds():new Rectangle(0,0,ip.getWidth(),ip.getHeight());
				int totalCurrentRoiPixelCount = imp.getStatistics().pixelCount;
				for(int indexH=0;indexH<_H;indexH++){
					for(int indexW=0;indexW<_W;indexW++){
						int x=indexW;
						int y=indexH;
						if (x>=r.x && y>=r.y && x<=r.x+r.width && y<=r.y+r.height&& (mask==null||mask.getPixel(x-r.x,y-r.y)!=0) ) {
							if(roi==null)continue;
							if(mask==null && x == r.x+r.width || y == r.y+r.height)continue; // rectangle ROI
							currentRoiPixelCount++;
							SI_sum += img2DArray[indexW][indexH];
							SI_sum2 += Math.pow(img2DArray[indexW][indexH], 2);
						}
					}
				}
			}else{
				int totalCurrentRoiPixelCount = imp.getStatistics().pixelCount;
				for(int indexH=0;indexH<_H;indexH++){
					for(int indexW=0;indexW<_W;indexW++){
						int x=indexW;
						int y=indexH;
						currentRoiPixelCount++;
						SI_sum += img2DArray[indexW][indexH];
						SI_sum2 += Math.pow(img2DArray[indexW][indexH], 2);
					}
				}
			}
			SI_mean = SI_sum/currentRoiPixelCount;
			totalPixelRoi = currentRoiPixelCount;

		}

		
		double SI_step=1;
		double histomax= 0;
		switch(histogramType){
		case 1: //auto
			SI_step= (histoMaxValue - histoMinValue) / histogramBins;
			for(int indexH=0;indexH<_H;indexH++){
				for(int indexW=0;indexW<_W;indexW++){
					double histo = Math.ceil( (img2DArray[indexW][indexH] - histoMinValue) / SI_step );
					if(histomax<histo)histomax=histo;
					if(histo>0){histo=histo-1;}
					if(histo<0){histo=0;}
					if(histo>histogramBins-1){histo=histogramBins-1;}
					img2DArray[indexW][indexH]=(float) histo;
				}
			}

			break;
		case 2: //autoInRoi
			SI_step= (histoMaxValue - histoMinValue) / histogramBins;
			for(int indexH=0;indexH<_H;indexH++){
				for(int indexW=0;indexW<_W;indexW++){
					double histo = Math.ceil( (img2DArray[indexW][indexH] - histoMinValue) / SI_step );
					if(histomax<histo)histomax=histo;
					if(histo>0){
						histo=histo-1;}
					if(histo<0){
						histo=0;}
					if(histo>histogramBins-1){
						histo=histogramBins-1;}
					if(histo==histogramBins){
						histo=histogramBins;}
					if(histo==0){
						histo=0;}
					img2DArray[indexW][indexH]=(float) histo;
				}
			}

			break;	
		case 3: //Mean-Std
			double SI_std_tmp = (SI_sum2 - Math.pow(SI_sum, 2)/totalPixelRoi)/(totalPixelRoi-1);
			SI_std_tmp = Math.sqrt(SI_std_tmp);


			double SI_low = SI_mean - SI_std*SI_std_tmp;
			double SI_high = SI_mean + SI_std*SI_std_tmp;
			histoMinValue = SI_low;
			histoMaxValue = SI_high;
			SI_step= (SI_high - SI_low) / histogramBins;

			for(int indexH=0;indexH<_H;indexH++){
				for(int indexW=0;indexW<_W;indexW++){
					double histo = Math.ceil( (img2DArray[indexW][indexH] - histoMinValue) / SI_step );
					if(histomax<histo)histomax=histo;
					if(histo>0){histo=histo-1;}
					if(histo<0){histo=0;}
					if(histo>histogramBins-1){histo=histogramBins-1;}
					img2DArray[indexW][indexH]=(float) histo;
				}
			}

			break;
		case 4: //Manual
			SI_step= SI_interval;
			histoMinValue = SI_center - SI_step*histogramBins/2;
			histoMaxValue = SI_center + SI_step*histogramBins/2;
			for(int indexH=0;indexH<_H;indexH++){
				for(int indexW=0;indexW<_W;indexW++){
					double tmp_num = Math.floor((img2DArray[indexW][indexH] - SI_center)/SI_step + histogramBins/2);
					img2DArray[indexW][indexH] = (float) Math.min(Math.max(0, tmp_num), histogramBins-1);
				}
			}

			break;
		}

		if (glcmCheck){
			double GLCM_ASM_0 = 0.0, GLCM_CON_0 = 0.0, GLCM_ENT_0 = 0.0, GLCM_MEAN_0 = 0.0, GLCM_VAR_0 = 0.0;
			double GLCM_ACO_0= 0.0, GLCM_CLP_0 = 0.0, GLCM_CLS_0 = 0.0, GLCM_CLT_0 = 0.0, GLCM_DIS_0 = 0.0;
			double GLCM_HOM_0 = 0.0, GLCM_IDM_0 = 0.0, GLCM_IDMN_0 = 0.0, GLCM_IDN_0 = 0.0;
			double GLCM_CORR_0 = 0.0, GLCM_IVAR_0 = 0.0, GLCM_MAXP_0= 0.0;						
			double GLCM_SUMA_0 = 0.0, GLCM_SUMV_0= 0.0, GLCM_SUME_0 = 0.0;
			double GLCM_DIFA_0 = 0.0, GLCM_DIFV_0 = 0.0, GLCM_DIFE_0 = 0.0;						
			double GLCM_IMC1_0 = 0.0, GLCM_IMC2_0 = 0.0;

			double GLCM_ASM_45 = 0.0, GLCM_CON_45 = 0.0, GLCM_ENT_45 = 0.0, GLCM_MEAN_45 = 0.0, GLCM_VAR_45 = 0.0;
			double GLCM_ACO_45 = 0.0, GLCM_CLP_45 = 0.0, GLCM_CLS_45 = 0.0, GLCM_CLT_45 = 0.0, GLCM_DIS_45 = 0.0;
			double GLCM_HOM_45= 0.0, GLCM_IDM_45 = 0.0, GLCM_IDMN_45 = 0.0, GLCM_IDN_45 = 0.0;
			double GLCM_CORR_45 = 0.0, GLCM_IVAR_45 = 0.0, GLCM_MAXP_45 = 0.0;						
			double GLCM_SUMA_45 = 0.0, GLCM_SUMV_45 = 0.0, GLCM_SUME_45 = 0.0;
			double GLCM_DIFA_45 = 0.0, GLCM_DIFV_45 = 0.0, GLCM_DIFE_45 = 0.0;						
			double GLCM_IMC1_45 = 0.0, GLCM_IMC2_45 = 0.0;

			double GLCM_ASM_90 = 0.0, GLCM_CON_90 = 0.0, GLCM_ENT_90 = 0.0, GLCM_MEAN_90 = 0.0, GLCM_VAR_90 = 0.0;
			double GLCM_ACO_90 = 0.0, GLCM_CLP_90 = 0.0, GLCM_CLS_90 = 0.0, GLCM_CLT_90 = 0.0, GLCM_DIS_90 = 0.0;
			double GLCM_HOM_90 = 0.0, GLCM_IDM_90 = 0.0, GLCM_IDMN_90 = 0.0, GLCM_IDN_90 = 0.0;
			double GLCM_CORR_90 = 0.0, GLCM_IVAR_90 = 0.0, GLCM_MAXP_90 = 0.0;						
			double GLCM_SUMA_90 = 0.0, GLCM_SUMV_90 = 0.0, GLCM_SUME_90 = 0.0;
			double GLCM_DIFA_90 = 0.0, GLCM_DIFV_90 = 0.0, GLCM_DIFE_90 = 0.0;						
			double GLCM_IMC1_90 = 0.0, GLCM_IMC2_90 = 0.0;

			double GLCM_ASM_135 = 0.0, GLCM_CON_135 = 0.0, GLCM_ENT_135 = 0.0, GLCM_MEAN_135 = 0.0, GLCM_VAR_135 = 0.0;
			double GLCM_ACO_135 = 0.0, GLCM_CLP_135 = 0.0, GLCM_CLS_135 = 0.0, GLCM_CLT_135 = 0.0, GLCM_DIS_135 = 0.0;
			double GLCM_HOM_135 = 0.0, GLCM_IDM_135 = 0.0, GLCM_IDMN_135 = 0.0, GLCM_IDN_135 = 0.0;
			double GLCM_CORR_135 = 0.0, GLCM_IVAR_135 = 0.0, GLCM_MAXP_135 = 0.0;						
			double GLCM_SUMA_135 = 0.0, GLCM_SUMV_135 = 0.0, GLCM_SUME_135 = 0.0;
			double GLCM_DIFA_135 = 0.0, GLCM_DIFV_135 = 0.0, GLCM_DIFE_135 = 0.0;						
			double GLCM_IMC1_135 = 0.0, GLCM_IMC2_135 = 0.0;

			double GLCM_ASM_ave = 0.0, GLCM_CON_ave = 0.0, GLCM_ENT_ave = 0.0, GLCM_MEAN_ave = 0.0, GLCM_VAR_ave = 0.0;
			double GLCM_ACO_ave = 0.0, GLCM_CLP_ave = 0.0, GLCM_CLS_ave = 0.0, GLCM_CLT_ave = 0.0, GLCM_DIS_ave = 0.0;
			double GLCM_HOM_ave = 0.0, GLCM_IDM_ave = 0.0, GLCM_IDMN_ave = 0.0, GLCM_IDN_ave = 0.0;
			double GLCM_CORR_ave = 0.0, GLCM_IVAR_ave = 0.0, GLCM_MAXP_ave = 0.0;						
			double GLCM_SUMA_ave = 0.0, GLCM_SUMV_ave = 0.0, GLCM_SUME_ave = 0.0;
			double GLCM_DIFA_ave = 0.0, GLCM_DIFV_ave = 0.0, GLCM_DIFE_ave = 0.0;						
			double GLCM_IMC1_ave = 0.0, GLCM_IMC2_ave = 0.0;
			
			float GLCM_ASM_0_Array[] = null, GLCM_CON_0_Array[] = null, GLCM_ENT_0_Array[] = null, GLCM_MEAN_0_Array[] = null, GLCM_VAR_0_Array[] = null;
			float GLCM_ACO_0_Array[] = null, GLCM_CLP_0_Array[] = null, GLCM_CLS_0_Array[] = null, GLCM_CLT_0_Array[] = null, GLCM_DIS_0_Array[] = null;
			float GLCM_HOM_0_Array[] = null, GLCM_IDM_0_Array[] = null, GLCM_IDMN_0_Array[] = null, GLCM_IDN_0_Array[] = null;
			float GLCM_CORR_0_Array[] = null, GLCM_IVAR_0_Array[] = null, GLCM_MAXP_0_Array[] = null;						
			float GLCM_SUMA_0_Array[] = null, GLCM_SUMV_0_Array[] = null, GLCM_SUME_0_Array[] = null;
			float GLCM_DIFA_0_Array[] = null, GLCM_DIFV_0_Array[] = null, GLCM_DIFE_0_Array[] = null;						
			float GLCM_IMC1_0_Array[] = null, GLCM_IMC2_0_Array[] = null;

			float GLCM_ASM_45_Array[] = null, GLCM_CON_45_Array[] = null, GLCM_ENT_45_Array[] = null, GLCM_MEAN_45_Array[] = null, GLCM_VAR_45_Array[] = null;
			float GLCM_ACO_45_Array[] = null, GLCM_CLP_45_Array[] = null, GLCM_CLS_45_Array[] = null, GLCM_CLT_45_Array[] = null, GLCM_DIS_45_Array[] = null;
			float GLCM_HOM_45_Array[] = null, GLCM_IDM_45_Array[] = null, GLCM_IDMN_45_Array[] = null, GLCM_IDN_45_Array[] = null;
			float GLCM_CORR_45_Array[] = null, GLCM_IVAR_45_Array[] = null, GLCM_MAXP_45_Array[] = null;						
			float GLCM_SUMA_45_Array[] = null, GLCM_SUMV_45_Array[] = null, GLCM_SUME_45_Array[] = null;
			float GLCM_DIFA_45_Array[] = null, GLCM_DIFV_45_Array[] = null, GLCM_DIFE_45_Array[] = null;						
			float GLCM_IMC1_45_Array[] = null, GLCM_IMC2_45_Array[] = null;

			float GLCM_ASM_90_Array[] = null, GLCM_CON_90_Array[] = null, GLCM_ENT_90_Array[] = null, GLCM_MEAN_90_Array[] = null, GLCM_VAR_90_Array[] = null;
			float GLCM_ACO_90_Array[] = null, GLCM_CLP_90_Array[] = null, GLCM_CLS_90_Array[] = null, GLCM_CLT_90_Array[] = null, GLCM_DIS_90_Array[] = null;
			float GLCM_HOM_90_Array[] = null, GLCM_IDM_90_Array[] = null, GLCM_IDMN_90_Array[] = null, GLCM_IDN_90_Array[] = null;
			float GLCM_CORR_90_Array[] = null, GLCM_IVAR_90_Array[] = null, GLCM_MAXP_90_Array[] = null;						
			float GLCM_SUMA_90_Array[] = null, GLCM_SUMV_90_Array[] = null, GLCM_SUME_90_Array[] = null;
			float GLCM_DIFA_90_Array[] = null, GLCM_DIFV_90_Array[] = null, GLCM_DIFE_90_Array[] = null;						
			float GLCM_IMC1_90_Array[] = null, GLCM_IMC2_90_Array[] = null;

			float GLCM_ASM_135_Array[] = null, GLCM_CON_135_Array[] = null, GLCM_ENT_135_Array[] = null, GLCM_MEAN_135_Array[] = null, GLCM_VAR_135_Array[] = null;
			float GLCM_ACO_135_Array[] = null, GLCM_CLP_135_Array[] = null, GLCM_CLS_135_Array[] = null, GLCM_CLT_135_Array[] = null, GLCM_DIS_135_Array[] = null;
			float GLCM_HOM_135_Array[] = null, GLCM_IDM_135_Array[] = null, GLCM_IDMN_135_Array[] = null, GLCM_IDN_135_Array[] = null;
			float GLCM_CORR_135_Array[] = null, GLCM_IVAR_135_Array[] = null, GLCM_MAXP_135_Array[] = null;						
			float GLCM_SUMA_135_Array[] = null, GLCM_SUMV_135_Array[] = null, GLCM_SUME_135_Array[] = null;
			float GLCM_DIFA_135_Array[] = null, GLCM_DIFV_135_Array[] = null, GLCM_DIFE_135_Array[] = null;						
			float GLCM_IMC1_135_Array[] = null, GLCM_IMC2_135_Array[] = null;

			float GLCM_ASM_ave_Array[] = null, GLCM_CON_ave_Array[] = null, GLCM_ENT_ave_Array[] = null, GLCM_MEAN_ave_Array[] = null, GLCM_VAR_ave_Array[] = null;
			float GLCM_ACO_ave_Array[] = null, GLCM_CLP_ave_Array[] = null, GLCM_CLS_ave_Array[] = null, GLCM_CLT_ave_Array[] = null, GLCM_DIS_ave_Array[] = null;
			float GLCM_HOM_ave_Array[] = null, GLCM_IDM_ave_Array[] = null, GLCM_IDMN_ave_Array[] = null, GLCM_IDN_ave_Array[] = null;
			float GLCM_CORR_ave_Array[] = null, GLCM_IVAR_ave_Array[] = null, GLCM_MAXP_ave_Array[] = null;						
			float GLCM_SUMA_ave_Array[] = null, GLCM_SUMV_ave_Array[] = null, GLCM_SUME_ave_Array[] = null;
			float GLCM_DIFA_ave_Array[] = null, GLCM_DIFV_ave_Array[] = null, GLCM_DIFE_ave_Array[] = null;						
			float GLCM_IMC1_ave_Array[] = null, GLCM_IMC2_ave_Array[] = null;
			
			if(texturemenu.create_map.getState()==true){
				if(texturemenu.direction_0.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						GLCM_ASM_0_Array = new float[_W*_H]; 
					}
					if(texturemenu.glcm_CON.getState()==true){
						GLCM_CON_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ENT.getState()==true){
						GLCM_ENT_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						GLCM_MEAN_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_VAR.getState()==true){
						GLCM_VAR_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ACO.getState()==true){
						GLCM_ACO_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLP.getState()==true){
						GLCM_CLP_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLS.getState()==true){
						GLCM_CLS_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLT.getState()==true){
						GLCM_CLT_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIS.getState()==true){
						GLCM_DIS_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_HOM.getState()==true){
						GLCM_HOM_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDM.getState()==true){
						GLCM_IDM_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						GLCM_IDMN_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDN.getState()==true){
						GLCM_IDN_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CORR.getState()==true){
						GLCM_CORR_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						GLCM_IVAR_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						GLCM_MAXP_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						GLCM_SUMA_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						GLCM_SUMV_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUME.getState()==true){
						GLCM_SUME_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						GLCM_DIFV_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						GLCM_DIFE_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						GLCM_IMC1_0_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						GLCM_IMC2_0_Array = new float[_W*_H];  
					}
				}
				
				if(texturemenu.direction_45.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						GLCM_ASM_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CON.getState()==true){
						GLCM_CON_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ENT.getState()==true){
						GLCM_ENT_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						GLCM_MEAN_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_VAR.getState()==true){
						GLCM_VAR_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ACO.getState()==true){
						GLCM_ACO_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLP.getState()==true){
						GLCM_CLP_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLS.getState()==true){
						GLCM_CLS_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLT.getState()==true){
						GLCM_CLT_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIS.getState()==true){
						GLCM_DIS_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_HOM.getState()==true){
						GLCM_HOM_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDM.getState()==true){
						GLCM_IDM_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						GLCM_IDMN_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDN.getState()==true){
						GLCM_IDN_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CORR.getState()==true){
						GLCM_CORR_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						GLCM_IVAR_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						GLCM_MAXP_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						GLCM_SUMA_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						GLCM_SUMV_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUME.getState()==true){
						GLCM_SUME_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						GLCM_DIFV_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						GLCM_DIFE_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						GLCM_IMC1_45_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						GLCM_IMC2_45_Array = new float[_W*_H];  
					}
				}
				
				if(texturemenu.direction_90.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						GLCM_ASM_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CON.getState()==true){
						GLCM_CON_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ENT.getState()==true){
						GLCM_ENT_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						GLCM_MEAN_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_VAR.getState()==true){
						GLCM_VAR_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ACO.getState()==true){
						GLCM_ACO_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLP.getState()==true){
						GLCM_CLP_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLS.getState()==true){
						GLCM_CLS_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLT.getState()==true){
						GLCM_CLT_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIS.getState()==true){
						GLCM_DIS_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_HOM.getState()==true){
						GLCM_HOM_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDM.getState()==true){
						GLCM_IDM_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						GLCM_IDMN_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDN.getState()==true){
						GLCM_IDN_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CORR.getState()==true){
						GLCM_CORR_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						GLCM_IVAR_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						GLCM_MAXP_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						GLCM_SUMA_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						GLCM_SUMV_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUME.getState()==true){
						GLCM_SUME_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						GLCM_DIFV_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						GLCM_DIFE_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						GLCM_IMC1_90_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						GLCM_IMC2_90_Array = new float[_W*_H];  
					}
				}
				
				if(texturemenu.direction_135.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						GLCM_ASM_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CON.getState()==true){
						GLCM_CON_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ENT.getState()==true){
						GLCM_ENT_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						GLCM_MEAN_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_VAR.getState()==true){
						GLCM_VAR_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ACO.getState()==true){
						GLCM_ACO_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLP.getState()==true){
						GLCM_CLP_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLS.getState()==true){
						GLCM_CLS_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLT.getState()==true){
						GLCM_CLT_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIS.getState()==true){
						GLCM_DIS_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_HOM.getState()==true){
						GLCM_HOM_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDM.getState()==true){
						GLCM_IDM_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						GLCM_IDMN_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDN.getState()==true){
						GLCM_IDN_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CORR.getState()==true){
						GLCM_CORR_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						GLCM_IVAR_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						GLCM_MAXP_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						GLCM_SUMA_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						GLCM_SUMV_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUME.getState()==true){
						GLCM_SUME_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						GLCM_DIFV_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						GLCM_DIFE_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						GLCM_IMC1_135_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						GLCM_IMC2_135_Array = new float[_W*_H];  
					}
				}
				
				if(texturemenu.direction_average.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						GLCM_ASM_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CON.getState()==true){
						GLCM_CON_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ENT.getState()==true){
						GLCM_ENT_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						GLCM_MEAN_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_VAR.getState()==true){
						GLCM_VAR_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_ACO.getState()==true){
						GLCM_ACO_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLP.getState()==true){
						GLCM_CLP_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLS.getState()==true){
						GLCM_CLS_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CLT.getState()==true){
						GLCM_CLT_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIS.getState()==true){
						GLCM_DIS_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_HOM.getState()==true){
						GLCM_HOM_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDM.getState()==true){
						GLCM_IDM_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						GLCM_IDMN_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IDN.getState()==true){
						GLCM_IDN_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_CORR.getState()==true){
						GLCM_CORR_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						GLCM_IVAR_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						GLCM_MAXP_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						GLCM_SUMA_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						GLCM_SUMV_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_SUME.getState()==true){
						GLCM_SUME_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						GLCM_DIFV_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						GLCM_DIFE_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						GLCM_IMC1_ave_Array = new float[_W*_H];  
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						GLCM_IMC2_ave_Array = new float[_W*_H];  
					}
				}

			}

			int roiPixelCount = 0;

			double[][] GLCM_mat;
			int limitSize = (windowSize-1)/2;

			int currentSlice=imp.getZ(); 
			
			Roi roi = null;
			if(selectionBoxCheck){
				roi = imp.getRoi();
			}
			if (roi!=null && !roi.isArea()) roi = null;
			ImageProcessor ip = imp.getProcessor();
			ImageProcessor mask = roi!=null?roi.getMask():null;
			Rectangle r = roi!=null?roi.getBounds():new Rectangle(0,0,ip.getWidth(),ip.getHeight());
			int totalCurrentRoiPixelCount = imp.getStatistics().pixelCount;
			int currentRoiPixelCount = 0;
			for(int indexH=limitSize;indexH<_H-limitSize;indexH++){
				IJ.showStatus("Slice: "+currentSlice+"/"+_Slice+" ... "+String.format("%.2f", ((double)(currentRoiPixelCount)/(totalCurrentRoiPixelCount)*100))+"%");
				for(int indexW=limitSize;indexW<_W-limitSize;indexW++){
					int x=indexW;
					int y=indexH;
					double GLCM_ASM_ave_pixel = 0, GLCM_CON_ave_pixel = 0, GLCM_ENT_ave_pixel = 0, GLCM_MEAN_ave_pixel = 0, GLCM_VAR_ave_pixel = 0;
					double GLCM_ACO_ave_pixel = 0, GLCM_CLP_ave_pixel = 0, GLCM_CLS_ave_pixel = 0, GLCM_CLT_ave_pixel = 0, GLCM_DIS_ave_pixel = 0;
					double GLCM_HOM_ave_pixel = 0, GLCM_IDM_ave_pixel = 0, GLCM_IDMN_ave_pixel = 0, GLCM_IDN_ave_pixel = 0;
					double GLCM_CORR_ave_pixel = 0, GLCM_IVAR_ave_pixel = 0, GLCM_MAXP_ave_pixel = 0;						
					double GLCM_SUMA_ave_pixel = 0, GLCM_SUMV_ave_pixel = 0, GLCM_SUME_ave_pixel = 0;
					double GLCM_DIFA_ave_pixel = 0, GLCM_DIFV_ave_pixel = 0, GLCM_DIFE_ave_pixel = 0;						
					double GLCM_IMC1_ave_pixel = 0, GLCM_IMC2_ave_pixel = 0;
					
					
					if (x>=r.x && y>=r.y && x<=r.x+r.width && y<=r.y+r.height&& (mask==null||mask.getPixel(x-r.x,y-r.y)!=0) ) {
//						if(x == r.x+r.width || y == r.y+r.height)continue; // rectangle ROI
						roiPixelCount++;
						currentRoiPixelCount++;
						GLCM_mat = new double[histogramBins][histogramBins];
						//0°
						if(texturemenu.direction_0.getState()==true || texturemenu.direction_average.getState()==true){
							for(int kernel_h=-limitSize;kernel_h<=limitSize;kernel_h++){
								for(int kernel_w=-limitSize;kernel_w<limitSize;kernel_w++){
									int referecePixel = (int)img2DArray[indexW+kernel_w][indexH+kernel_h];
									int neighborPixel = (int)img2DArray[indexW+kernel_w+1][indexH+kernel_h];
									GLCM_mat[referecePixel][neighborPixel] = GLCM_mat[referecePixel][neighborPixel]+1; 
									GLCM_mat[neighborPixel][referecePixel] = GLCM_mat[neighborPixel][referecePixel]+1; //symetry mtx
								}
							}
							for(int h=0;h<histogramBins;h++){ // normalization
								for(int w=0;w<histogramBins;w++){
									GLCM_mat[w][h] = GLCM_mat[w][h]/ (windowSize*(windowSize-1)*2);
								}
							}

							double P_ij = 0;
							double GLCM_ASM = 0.0, GLCM_CON = 0.0, GLCM_ENT = 0.0, GLCM_MEAN = 0.0, GLCM_VAR = 0.0;
							double GLCM_ACO = 0.0, GLCM_CLP = 0.0, GLCM_CLS = 0.0, GLCM_CLT = 0.0, GLCM_DIS = 0.0;
							double GLCM_HOM = 0.0, GLCM_IDM = 0.0, GLCM_IDMN = 0.0, GLCM_IDN = 0.0;
							double GLCM_CORR = 0.0, GLCM_IVAR = 0.0, GLCM_MAXP = 0.0;						
							double GLCM_SUMA = 0.0, GLCM_SUMV = 0.0, GLCM_SUME = 0.0;
							double GLCM_DIFA = 0.0, GLCM_DIFV = 0.0, GLCM_DIFE = 0.0;						
							double GLCM_IMC1 = 0.0, GLCM_IMC2 = 0.0;

							// 3) calculate marginal, diff, sum matrix
							// 3-1) for marginal, GLCM is symetry mtx. Px is same with Py. Ux=Uy=U(GLCM_MEAN).						
							double[] Px = new double[histogramBins];
							Arrays.fill(Px, 0.0);
							double[] diff_i = new double[histogramBins];
							Arrays.fill(diff_i, 0.0);
							double[] sum_i = new double[2*histogramBins-1];
							Arrays.fill(sum_i, 0.0);
							int diff, sum;
							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									diff = Math.abs(i-j);
									sum = i+j;

									diff_i[diff] +=P_ij;
									sum_i[sum] +=P_ij;
									Px[i] += P_ij;

									GLCM_MEAN += i * P_ij;
								}
							}

							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									// 1) Angular secon moment or Energy
									GLCM_ASM += Math.pow(P_ij, 2);

									// 2) Contrast
									GLCM_CON += P_ij * Math.pow(i-j, 2);

									// 3) Entropy 
									if (P_ij != 0){
										GLCM_ENT += P_ij * (-1 * Math.log(P_ij)/Math.log(2));
									}

									// 4) Mean is already calculated. 
									// 5) Variance 
									GLCM_VAR += P_ij * Math.pow(i - GLCM_MEAN, 2);

									// 6) Autocorrelation1
									GLCM_ACO += P_ij * i * j;

									// 7) Cluster Prominence
									GLCM_CLP += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 4); 

									// 8) Cluster Shade
									GLCM_CLS += P_ij * Math.pow(i + j - GLCM_MEAN -GLCM_MEAN, 3);

									// 9) Cluster Tendency
									GLCM_CLT += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 2);

									// 10) Dissimilarity
									GLCM_DIS += P_ij * Math.abs(i-j);

									// 11) Homogeneity 1
									GLCM_HOM += P_ij / (1 + Math.abs(i-j));

									// 12) Homogeneity 2 or Inverse difference moment(IDM)
									GLCM_IDM += P_ij / (1 + Math.pow(i-j, 2));

									// 13) Inverse difference moment normalized (IDMN)
									GLCM_IDMN += P_ij / (1 + (Math.pow(i-j, 2) / Math.pow(histogramBins,2) ) );

									// 14) Inverse difference normalized (IDN)
									GLCM_IDN += P_ij / (1 + (Math.pow(i-j, 2) / ((double)histogramBins) ) );

									// 15 Correlation
									GLCM_CORR += P_ij *(i-GLCM_MEAN) * (j-GLCM_MEAN);

									// 16) Inverse Variance
									if (i != j){
										GLCM_IVAR += P_ij / Math.pow(i-j, 2);
									}

									// 17) Maximum Probability
									if (GLCM_MAXP <= P_ij){
										GLCM_MAXP = P_ij;
									}
								}
							}
							if(GLCM_VAR!=0)
								GLCM_CORR = GLCM_CORR / GLCM_VAR;

							// 18 ~ 20)  Sum Average (SUMA), Variance (SUMV), Entropy (SUME)
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMA += sum_i[i] * i;
								if(sum_i[i] != 0){
									GLCM_SUME -= sum_i[i] * Math.log(sum_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMV += Math.pow(i-GLCM_SUMA, 2) * sum_i[i];
							}

							// 21 ~ 23)  Difference Average (DIFA), Variance (DIFV), Entropy (DIFE)
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFA += diff_i[i] * i;
								if(diff_i[i] != 0){
									GLCM_DIFE -= diff_i[i] * Math.log(diff_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFV += Math.pow(i-GLCM_DIFA, 2) * diff_i[i];
							}

							// looping for HXY matrix..
							double HX = 0.0, HXY = 0.0, HXY1 = 0.0, HXY2 = 0.0; 
							for (int i = 0; i < histogramBins; i ++){
								if(Px[i]!=0){
									HX -= Px[i] * Math.log(Px[i]);								
								} 
								for (int j = 0; j < histogramBins; j++){
									P_ij = P_ij = GLCM_mat[i][j];

									if(P_ij != 0){
										HXY -= P_ij * Math.log(P_ij);
									}
									if(Px[i]!=0 && Px[j] !=0){
										HXY1 -= P_ij * Math.log(Px[i]*Px[j]);
										HXY2 -= Px[i]*Px[j] * Math.log(Px[i]*Px[j]);
									}				
								}
							}
							// 24) Informational measure of correlation 1 (IMC1)
							if(HX!=0)
								GLCM_IMC1 = (HXY - HXY1) / HX;

							// 25) Informational measure of correlation 1 (IMC1)
							if(HXY2-HXY>0)
								GLCM_IMC2 = Math.sqrt(1-Math.exp(-2*(HXY2-HXY)));

							if(texturemenu.create_value.getState()==true){
								if(texturemenu.direction_0.getState()==true){
									GLCM_ASM_0 += GLCM_ASM; GLCM_CON_0 += GLCM_CON; GLCM_ENT_0 += GLCM_ENT; GLCM_MEAN_0 += GLCM_MEAN; GLCM_VAR_0 += GLCM_VAR;
									GLCM_ACO_0+= GLCM_ACO; GLCM_CLP_0 += GLCM_CLP; GLCM_CLS_0 += GLCM_CLS; GLCM_CLT_0 += GLCM_CLT; GLCM_DIS_0 += GLCM_DIS;
									GLCM_HOM_0 += GLCM_HOM; GLCM_IDM_0 += GLCM_IDM; GLCM_IDMN_0 += GLCM_IDMN; GLCM_IDN_0 += GLCM_IDN;
									GLCM_CORR_0 += GLCM_CORR; GLCM_IVAR_0 += GLCM_IVAR; GLCM_MAXP_0 += GLCM_MAXP;						
									GLCM_SUMA_0 += GLCM_SUMA; GLCM_SUMV_0 += GLCM_SUMV; GLCM_SUME_0 += GLCM_SUME;
									GLCM_DIFA_0 += GLCM_DIFA; GLCM_DIFV_0 += GLCM_DIFV; GLCM_DIFE_0 += GLCM_DIFE;						
									GLCM_IMC1_0 += GLCM_IMC1; GLCM_IMC2_0 += GLCM_IMC2;
								}
								if(texturemenu.direction_average.getState()==true){
									GLCM_ASM_ave += GLCM_ASM; GLCM_CON_ave += GLCM_CON; GLCM_ENT_ave += GLCM_ENT; GLCM_MEAN_ave += GLCM_MEAN; GLCM_VAR_ave += GLCM_VAR;
									GLCM_ACO_ave+= GLCM_ACO; GLCM_CLP_ave += GLCM_CLP; GLCM_CLS_ave += GLCM_CLS; GLCM_CLT_ave += GLCM_CLT; GLCM_DIS_ave += GLCM_DIS;
									GLCM_HOM_ave += GLCM_HOM; GLCM_IDM_ave += GLCM_IDM; GLCM_IDMN_ave += GLCM_IDMN; GLCM_IDN_ave += GLCM_IDN;
									GLCM_CORR_ave += GLCM_CORR; GLCM_IVAR_ave += GLCM_IVAR; GLCM_MAXP_ave += GLCM_MAXP;						
									GLCM_SUMA_ave += GLCM_SUMA; GLCM_SUMV_ave += GLCM_SUMV; GLCM_SUME_ave += GLCM_SUME;
									GLCM_DIFA_ave += GLCM_DIFA; GLCM_DIFV_ave += GLCM_DIFV; GLCM_DIFE_ave += GLCM_DIFE;						
									GLCM_IMC1_ave += GLCM_IMC1; GLCM_IMC2_ave += GLCM_IMC2;
									GLCM_ASM_ave_pixel += GLCM_ASM; GLCM_CON_ave_pixel += GLCM_CON; GLCM_ENT_ave_pixel += GLCM_ENT; GLCM_MEAN_ave_pixel += GLCM_MEAN; GLCM_VAR_ave_pixel += GLCM_VAR;
									GLCM_ACO_ave_pixel+= GLCM_ACO; GLCM_CLP_ave_pixel += GLCM_CLP; GLCM_CLS_ave_pixel += GLCM_CLS; GLCM_CLT_ave_pixel += GLCM_CLT; GLCM_DIS_ave_pixel += GLCM_DIS;
									GLCM_HOM_ave_pixel += GLCM_HOM; GLCM_IDM_ave_pixel += GLCM_IDM; GLCM_IDMN_ave_pixel += GLCM_IDMN; GLCM_IDN_ave_pixel += GLCM_IDN;
									GLCM_CORR_ave_pixel += GLCM_CORR; GLCM_IVAR_ave_pixel += GLCM_IVAR; GLCM_MAXP_ave_pixel += GLCM_MAXP;						
									GLCM_SUMA_ave_pixel += GLCM_SUMA; GLCM_SUMV_ave_pixel += GLCM_SUMV; GLCM_SUME_ave_pixel += GLCM_SUME;
									GLCM_DIFA_ave_pixel += GLCM_DIFA; GLCM_DIFV_ave_pixel += GLCM_DIFV; GLCM_DIFE_ave_pixel += GLCM_DIFE;						
									GLCM_IMC1_ave_pixel += GLCM_IMC1; GLCM_IMC2_ave_pixel += GLCM_IMC2;
								}
							}
							if(texturemenu.create_map.getState()==true && texturemenu.direction_0.getState()==true){
								if(texturemenu.glcm_ASM.getState()==true){
									GLCM_ASM_0_Array[indexH*_W+indexW] = (float) GLCM_ASM; 
								}
								if(texturemenu.glcm_CON.getState()==true){
									GLCM_CON_0_Array[indexH*_W+indexW] = (float) GLCM_CON; 
								}
								if(texturemenu.glcm_ENT.getState()==true){
									GLCM_ENT_0_Array[indexH*_W+indexW] = (float) GLCM_ENT; 
								}
								if(texturemenu.glcm_MEAN.getState()==true){
									GLCM_MEAN_0_Array[indexH*_W+indexW] = (float) GLCM_MEAN; 
								}
								if(texturemenu.glcm_VAR.getState()==true){
									GLCM_VAR_0_Array[indexH*_W+indexW] = (float) GLCM_VAR; 
								}
								if(texturemenu.glcm_ACO.getState()==true){
									GLCM_ACO_0_Array[indexH*_W+indexW] = (float) GLCM_ACO; 
								}
								if(texturemenu.glcm_CLP.getState()==true){
									GLCM_CLP_0_Array[indexH*_W+indexW] = (float) GLCM_CLP; 
								}
								if(texturemenu.glcm_CLS.getState()==true){
									GLCM_CLS_0_Array[indexH*_W+indexW] = (float) GLCM_CLS; 
								}
								if(texturemenu.glcm_CLT.getState()==true){
									GLCM_CLT_0_Array[indexH*_W+indexW] = (float) GLCM_CLT; 
								}
								if(texturemenu.glcm_DIS.getState()==true){
									GLCM_DIS_0_Array[indexH*_W+indexW] = (float) GLCM_DIS; 
								}
								if(texturemenu.glcm_HOM.getState()==true){
									GLCM_HOM_0_Array[indexH*_W+indexW] = (float) GLCM_HOM; 
								}
								if(texturemenu.glcm_IDM.getState()==true){
									GLCM_IDM_0_Array[indexH*_W+indexW] = (float) GLCM_IDM; 
								}
								if(texturemenu.glcm_IDMN.getState()==true){
									GLCM_IDMN_0_Array[indexH*_W+indexW] = (float) GLCM_IDMN; 
								}
								if(texturemenu.glcm_IDN.getState()==true){
									GLCM_IDN_0_Array[indexH*_W+indexW] = (float) GLCM_IDN; 
								}
								if(texturemenu.glcm_CORR.getState()==true){
									GLCM_CORR_0_Array[indexH*_W+indexW] = (float) GLCM_CORR; 
								}
								if(texturemenu.glcm_IVAR.getState()==true){
									GLCM_IVAR_0_Array[indexH*_W+indexW] = (float) GLCM_IVAR; 
								}
								if(texturemenu.glcm_MAXP.getState()==true){
									GLCM_MAXP_0_Array[indexH*_W+indexW] = (float) GLCM_MAXP; 
								}
								if(texturemenu.glcm_SUMA.getState()==true){
									GLCM_SUMA_0_Array[indexH*_W+indexW] = (float) GLCM_SUMA; 
								}
								if(texturemenu.glcm_SUMV.getState()==true){
									GLCM_SUMV_0_Array[indexH*_W+indexW] = (float) GLCM_SUMV; 
								}
								if(texturemenu.glcm_SUME.getState()==true){
									GLCM_SUME_0_Array[indexH*_W+indexW] = (float) GLCM_SUME; 
								}
								if(texturemenu.glcm_DIFV.getState()==true){
									GLCM_DIFV_0_Array[indexH*_W+indexW] = (float) GLCM_DIFV; 
								}
								if(texturemenu.glcm_DIFE.getState()==true){
									GLCM_DIFE_0_Array[indexH*_W+indexW] = (float) GLCM_DIFE; 
								}
								if(texturemenu.glcm_IMC1.getState()==true){
									GLCM_IMC1_0_Array[indexH*_W+indexW] = (float) GLCM_IMC1; 
								}
								if(texturemenu.glcm_IMC2.getState()==true){
									GLCM_IMC2_0_Array[indexH*_W+indexW] = (float) GLCM_IMC2; 
								}
							}
						}

						//45°
						GLCM_mat = new double[histogramBins][histogramBins];
						if(texturemenu.direction_45.getState()==true || texturemenu.direction_average.getState()==true){
							for(int kernel_h=-limitSize+1;kernel_h<=limitSize;kernel_h++){
								for(int kernel_w=-limitSize;kernel_w<limitSize;kernel_w++){
									int referecePixel = (int)img2DArray[indexW+kernel_w][indexH+kernel_h];
									int neighborPixel = (int)img2DArray[indexW+kernel_w+1][indexH+kernel_h-1];
									GLCM_mat[referecePixel][neighborPixel] = GLCM_mat[referecePixel][neighborPixel]+1; 
									GLCM_mat[neighborPixel][referecePixel] = GLCM_mat[neighborPixel][referecePixel]+1; //symetry mtx
								}
							}
							for(int h=0;h<histogramBins;h++){ // normalization
								for(int w=0;w<histogramBins;w++){                                           
									GLCM_mat[w][h] = GLCM_mat[w][h]/ ((windowSize-1)*(windowSize-1)*2);
								}
							}

							double P_ij = 0;
							double GLCM_ASM = 0.0, GLCM_CON = 0.0, GLCM_ENT = 0.0, GLCM_MEAN = 0.0, GLCM_VAR = 0.0;
							double GLCM_ACO = 0.0, GLCM_CLP = 0.0, GLCM_CLS = 0.0, GLCM_CLT = 0.0, GLCM_DIS = 0.0;
							double GLCM_HOM = 0.0, GLCM_IDM = 0.0, GLCM_IDMN = 0.0, GLCM_IDN = 0.0;
							double GLCM_CORR = 0.0, GLCM_IVAR = 0.0, GLCM_MAXP = 0.0;						
							double GLCM_SUMA = 0.0, GLCM_SUMV = 0.0, GLCM_SUME = 0.0;
							double GLCM_DIFA = 0.0, GLCM_DIFV = 0.0, GLCM_DIFE = 0.0;						
							double GLCM_IMC1 = 0.0, GLCM_IMC2 = 0.0;

							// 3) calculate marginal, diff, sum matrix
							// 3-1) for marginal, GLCM is symetry mtx. Px is same with Py. Ux=Uy=U(GLCM_MEAN).						
							double[] Px = new double[histogramBins];
							Arrays.fill(Px, 0.0);
							double[] diff_i = new double[histogramBins];
							Arrays.fill(diff_i, 0.0);
							double[] sum_i = new double[2*histogramBins-1];
							Arrays.fill(sum_i, 0.0);
							int diff, sum;
							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									diff = Math.abs(i-j);
									sum = i+j;

									diff_i[diff] +=P_ij;
									sum_i[sum] +=P_ij;
									Px[i] += P_ij;

									GLCM_MEAN += i * P_ij;
								}
							}

							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									// 1) Angular secon moment or Energy
									GLCM_ASM += Math.pow(P_ij, 2);

									// 2) Contrast
									GLCM_CON += P_ij * Math.pow(i-j, 2);

									// 3) Entropy 
									if (P_ij != 0){
										GLCM_ENT += P_ij * (-1 * Math.log(P_ij)/Math.log(2));
									}

									// 4) Mean is already calculated. 
									// 5) Variance 
									GLCM_VAR += P_ij * Math.pow(i - GLCM_MEAN, 2);

									// 6) Autocorrelation1
									GLCM_ACO += P_ij * i * j;

									// 7) Cluster Prominence
									GLCM_CLP += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 4); 

									// 8) Cluster Shade
									GLCM_CLS += P_ij * Math.pow(i + j - GLCM_MEAN -GLCM_MEAN, 3);

									// 9) Cluster Tendency
									GLCM_CLT += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 2);

									// 10) Dissimilarity
									GLCM_DIS += P_ij * Math.abs(i-j);

									// 11) Homogeneity 1
									GLCM_HOM += P_ij / (1 + Math.abs(i-j));

									// 12) Homogeneity 2 or Inverse difference moment(IDM)
									GLCM_IDM += P_ij / (1 + Math.pow(i-j, 2));

									// 13) Inverse difference moment normalized (IDMN)
									GLCM_IDMN += P_ij / (1 + (Math.pow(i-j, 2) / Math.pow(histogramBins,2) ) );

									// 14) Inverse difference normalized (IDN)
									GLCM_IDN += P_ij / (1 + (Math.pow(i-j, 2) / ((double)histogramBins) ) );

									// 15 Correlation
									GLCM_CORR += P_ij *(i-GLCM_MEAN) * (j-GLCM_MEAN);

									// 16) Inverse Variance
									if (i != j){
										GLCM_IVAR += P_ij / Math.pow(i-j, 2);
									}

									// 17) Maximum Probability
									if (GLCM_MAXP <= P_ij){
										GLCM_MAXP = P_ij;
									}
								}
							}

							if(GLCM_VAR!=0)
								GLCM_CORR = GLCM_CORR / GLCM_VAR;

							// 18 ~ 20)  Sum Average (SUMA), Variance (SUMV), Entropy (SUME)
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMA += sum_i[i] * i;
								if(sum_i[i] != 0){
									GLCM_SUME -= sum_i[i] * Math.log(sum_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMV += Math.pow(i-GLCM_SUMA, 2) * sum_i[i];
							}

							// 21 ~ 23)  Difference Average (DIFA), Variance (DIFV), Entropy (DIFE)
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFA += diff_i[i] * i;
								if(diff_i[i] != 0){
									GLCM_DIFE -= diff_i[i] * Math.log(diff_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFV += Math.pow(i-GLCM_DIFA, 2) * diff_i[i];
							}

							// looping for HXY matrix..
							double HX = 0.0, HXY = 0.0, HXY1 = 0.0, HXY2 = 0.0; 
							for (int i = 0; i < histogramBins; i ++){
								if(Px[i]!=0){
									HX -= Px[i] * Math.log(Px[i]);								
								} 
								for (int j = 0; j < histogramBins; j++){
									P_ij = P_ij = GLCM_mat[i][j];

									if(P_ij != 0){
										HXY -= P_ij * Math.log(P_ij);
									}
									if(Px[i]!=0 && Px[j] !=0){
										HXY1 -= P_ij * Math.log(Px[i]*Px[j]);
										HXY2 -= Px[i]*Px[j] * Math.log(Px[i]*Px[j]);
									}				
								}
							}
							// 24) Informational measure of correlation 1 (IMC1)
							if(HX!=0)
								GLCM_IMC1 = (HXY - HXY1) / HX;

							// 25) Informational measure of correlation 1 (IMC1)
							if(HXY2-HXY>0)
								GLCM_IMC2 = Math.sqrt(1-Math.exp(-2*(HXY2-HXY)));

							if(texturemenu.create_value.getState()==true){
								if(texturemenu.direction_45.getState()==true){
									GLCM_ASM_45 += GLCM_ASM; GLCM_CON_45 += GLCM_CON; GLCM_ENT_45 += GLCM_ENT; GLCM_MEAN_45 += GLCM_MEAN; GLCM_VAR_45 += GLCM_VAR;
									GLCM_ACO_45+= GLCM_ACO; GLCM_CLP_45 += GLCM_CLP; GLCM_CLS_45 += GLCM_CLS; GLCM_CLT_45 += GLCM_CLT; GLCM_DIS_45 += GLCM_DIS;
									GLCM_HOM_45 += GLCM_HOM; GLCM_IDM_45 += GLCM_IDM; GLCM_IDMN_45 += GLCM_IDMN; GLCM_IDN_45 += GLCM_IDN;
									GLCM_CORR_45 += GLCM_CORR; GLCM_IVAR_45 += GLCM_IVAR; GLCM_MAXP_45 += GLCM_MAXP;						
									GLCM_SUMA_45 += GLCM_SUMA; GLCM_SUMV_45 += GLCM_SUMV; GLCM_SUME_45 += GLCM_SUME;
									GLCM_DIFA_45 += GLCM_DIFA; GLCM_DIFV_45 += GLCM_DIFV; GLCM_DIFE_45 += GLCM_DIFE;						
									GLCM_IMC1_45 += GLCM_IMC1; GLCM_IMC2_45 += GLCM_IMC2;
								}
								
								if(texturemenu.direction_average.getState()==true){
									GLCM_ASM_ave += GLCM_ASM; GLCM_CON_ave += GLCM_CON; GLCM_ENT_ave += GLCM_ENT; GLCM_MEAN_ave += GLCM_MEAN; GLCM_VAR_ave += GLCM_VAR;
									GLCM_ACO_ave+= GLCM_ACO; GLCM_CLP_ave += GLCM_CLP; GLCM_CLS_ave += GLCM_CLS; GLCM_CLT_ave += GLCM_CLT; GLCM_DIS_ave += GLCM_DIS;
									GLCM_HOM_ave += GLCM_HOM; GLCM_IDM_ave += GLCM_IDM; GLCM_IDMN_ave += GLCM_IDMN; GLCM_IDN_ave += GLCM_IDN;
									GLCM_CORR_ave += GLCM_CORR; GLCM_IVAR_ave += GLCM_IVAR; GLCM_MAXP_ave += GLCM_MAXP;						
									GLCM_SUMA_ave += GLCM_SUMA; GLCM_SUMV_ave += GLCM_SUMV; GLCM_SUME_ave += GLCM_SUME;
									GLCM_DIFA_ave += GLCM_DIFA; GLCM_DIFV_ave += GLCM_DIFV; GLCM_DIFE_ave += GLCM_DIFE;						
									GLCM_IMC1_ave += GLCM_IMC1; GLCM_IMC2_ave += GLCM_IMC2;
									GLCM_ASM_ave_pixel += GLCM_ASM; GLCM_CON_ave_pixel += GLCM_CON; GLCM_ENT_ave_pixel += GLCM_ENT; GLCM_MEAN_ave_pixel += GLCM_MEAN; GLCM_VAR_ave_pixel += GLCM_VAR;
									GLCM_ACO_ave_pixel+= GLCM_ACO; GLCM_CLP_ave_pixel += GLCM_CLP; GLCM_CLS_ave_pixel += GLCM_CLS; GLCM_CLT_ave_pixel += GLCM_CLT; GLCM_DIS_ave_pixel += GLCM_DIS;
									GLCM_HOM_ave_pixel += GLCM_HOM; GLCM_IDM_ave_pixel += GLCM_IDM; GLCM_IDMN_ave_pixel += GLCM_IDMN; GLCM_IDN_ave_pixel += GLCM_IDN;
									GLCM_CORR_ave_pixel += GLCM_CORR; GLCM_IVAR_ave_pixel += GLCM_IVAR; GLCM_MAXP_ave_pixel += GLCM_MAXP;						
									GLCM_SUMA_ave_pixel += GLCM_SUMA; GLCM_SUMV_ave_pixel += GLCM_SUMV; GLCM_SUME_ave_pixel += GLCM_SUME;
									GLCM_DIFA_ave_pixel += GLCM_DIFA; GLCM_DIFV_ave_pixel += GLCM_DIFV; GLCM_DIFE_ave_pixel += GLCM_DIFE;						
									GLCM_IMC1_ave_pixel += GLCM_IMC1; GLCM_IMC2_ave_pixel += GLCM_IMC2;
								}
							}

							if(texturemenu.create_map.getState()==true && texturemenu.direction_45.getState()==true){
								if(texturemenu.glcm_ASM.getState()==true){
									GLCM_ASM_45_Array[indexH*_W+indexW] = (float) GLCM_ASM; 
								}
								if(texturemenu.glcm_CON.getState()==true){
									GLCM_CON_45_Array[indexH*_W+indexW] = (float) GLCM_CON; 
								}
								if(texturemenu.glcm_ENT.getState()==true){
									GLCM_ENT_45_Array[indexH*_W+indexW] = (float) GLCM_ENT; 
								}
								if(texturemenu.glcm_MEAN.getState()==true){
									GLCM_MEAN_45_Array[indexH*_W+indexW] = (float) GLCM_MEAN; 
								}
								if(texturemenu.glcm_VAR.getState()==true){
									GLCM_VAR_45_Array[indexH*_W+indexW] = (float) GLCM_VAR; 
								}
								if(texturemenu.glcm_ACO.getState()==true){
									GLCM_ACO_45_Array[indexH*_W+indexW] = (float) GLCM_ACO; 
								}
								if(texturemenu.glcm_CLP.getState()==true){
									GLCM_CLP_45_Array[indexH*_W+indexW] = (float) GLCM_CLP; 
								}
								if(texturemenu.glcm_CLS.getState()==true){
									GLCM_CLS_45_Array[indexH*_W+indexW] = (float) GLCM_CLS; 
								}
								if(texturemenu.glcm_CLT.getState()==true){
									GLCM_CLT_45_Array[indexH*_W+indexW] = (float) GLCM_CLT; 
								}
								if(texturemenu.glcm_DIS.getState()==true){
									GLCM_DIS_45_Array[indexH*_W+indexW] = (float) GLCM_DIS; 
								}
								if(texturemenu.glcm_HOM.getState()==true){
									GLCM_HOM_45_Array[indexH*_W+indexW] = (float) GLCM_HOM; 
								}
								if(texturemenu.glcm_IDM.getState()==true){
									GLCM_IDM_45_Array[indexH*_W+indexW] = (float) GLCM_IDM; 
								}
								if(texturemenu.glcm_IDMN.getState()==true){
									GLCM_IDMN_45_Array[indexH*_W+indexW] = (float) GLCM_IDMN; 
								}
								if(texturemenu.glcm_IDN.getState()==true){
									GLCM_IDN_45_Array[indexH*_W+indexW] = (float) GLCM_IDN; 
								}
								if(texturemenu.glcm_CORR.getState()==true){
									GLCM_CORR_45_Array[indexH*_W+indexW] = (float) GLCM_CORR; 
								}
								if(texturemenu.glcm_IVAR.getState()==true){
									GLCM_IVAR_45_Array[indexH*_W+indexW] = (float) GLCM_IVAR; 
								}
								if(texturemenu.glcm_MAXP.getState()==true){
									GLCM_MAXP_45_Array[indexH*_W+indexW] = (float) GLCM_MAXP; 
								}
								if(texturemenu.glcm_SUMA.getState()==true){
									GLCM_SUMA_45_Array[indexH*_W+indexW] = (float) GLCM_SUMA; 
								}
								if(texturemenu.glcm_SUMV.getState()==true){
									GLCM_SUMV_45_Array[indexH*_W+indexW] = (float) GLCM_SUMV; 
								}
								if(texturemenu.glcm_SUME.getState()==true){
									GLCM_SUME_45_Array[indexH*_W+indexW] = (float) GLCM_SUME; 
								}
								if(texturemenu.glcm_DIFV.getState()==true){
									GLCM_DIFV_45_Array[indexH*_W+indexW] = (float) GLCM_DIFV; 
								}
								if(texturemenu.glcm_DIFE.getState()==true){
									GLCM_DIFE_45_Array[indexH*_W+indexW] = (float) GLCM_DIFE; 
								}
								if(texturemenu.glcm_IMC1.getState()==true){
									GLCM_IMC1_45_Array[indexH*_W+indexW] = (float) GLCM_IMC1; 
								}
								if(texturemenu.glcm_IMC2.getState()==true){
									GLCM_IMC2_45_Array[indexH*_W+indexW] = (float) GLCM_IMC2; 
								}
							}
						}

						//90°
						GLCM_mat = new double[histogramBins][histogramBins];
						if(texturemenu.direction_90.getState()==true || texturemenu.direction_average.getState()==true){
							int windowCount = 0;
							for(int kernel_h=-limitSize;kernel_h<=limitSize;kernel_h++){
								for(int kernel_w=-limitSize+1;kernel_w<=limitSize;kernel_w++){
									windowCount++;
									int referecePixel = (int)img2DArray[indexW+kernel_w][indexH+kernel_h];
									int neighborPixel = (int)img2DArray[indexW+kernel_w-1][indexH+kernel_h];
									GLCM_mat[referecePixel][neighborPixel] = GLCM_mat[referecePixel][neighborPixel]+1; 
									GLCM_mat[neighborPixel][referecePixel] = GLCM_mat[neighborPixel][referecePixel]+1; //symetry mtx
								}
							}
							for(int h=0;h<histogramBins;h++){ // normalization
								for(int w=0;w<histogramBins;w++){
									GLCM_mat[w][h] = GLCM_mat[w][h]/ (windowSize*(windowSize-1)*2);
								}
							}

							double P_ij = 0;
							double GLCM_ASM = 0.0, GLCM_CON = 0.0, GLCM_ENT = 0.0, GLCM_MEAN = 0.0, GLCM_VAR = 0.0;
							double GLCM_ACO = 0.0, GLCM_CLP = 0.0, GLCM_CLS = 0.0, GLCM_CLT = 0.0, GLCM_DIS = 0.0;
							double GLCM_HOM = 0.0, GLCM_IDM = 0.0, GLCM_IDMN = 0.0, GLCM_IDN = 0.0;
							double GLCM_CORR = 0.0, GLCM_IVAR = 0.0, GLCM_MAXP = 0.0;						
							double GLCM_SUMA = 0.0, GLCM_SUMV = 0.0, GLCM_SUME = 0.0;
							double GLCM_DIFA = 0.0, GLCM_DIFV = 0.0, GLCM_DIFE = 0.0;						
							double GLCM_IMC1 = 0.0, GLCM_IMC2 = 0.0;

							// 3) calculate marginal, diff, sum matrix
							// 3-1) for marginal, GLCM is symetry mtx. Px is same with Py. Ux=Uy=U(GLCM_MEAN).						
							double[] Px = new double[histogramBins];
							Arrays.fill(Px, 0.0);
							double[] diff_i = new double[histogramBins];
							Arrays.fill(diff_i, 0.0);
							double[] sum_i = new double[2*histogramBins-1];
							Arrays.fill(sum_i, 0.0);
							int diff, sum;
							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									diff = Math.abs(i-j);
									sum = i+j;

									diff_i[diff] +=P_ij;
									sum_i[sum] +=P_ij;
									Px[i] += P_ij;

									GLCM_MEAN += i * P_ij;
								}
							}

							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									// 1) Angular secon moment or Energy
									GLCM_ASM += Math.pow(P_ij, 2);

									// 2) Contrast
									GLCM_CON += P_ij * Math.pow(i-j, 2);

									// 3) Entropy 
									if (P_ij != 0){
										GLCM_ENT += P_ij * (-1 * Math.log(P_ij)/Math.log(2));
									}

									// 4) Mean is already calculated. 
									// 5) Variance 
									GLCM_VAR += P_ij * Math.pow(i - GLCM_MEAN, 2);

									// 6) Autocorrelation1
									GLCM_ACO += P_ij * i * j;

									// 7) Cluster Prominence
									GLCM_CLP += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 4); 

									// 8) Cluster Shade
									GLCM_CLS += P_ij * Math.pow(i + j - GLCM_MEAN -GLCM_MEAN, 3);

									// 9) Cluster Tendency
									GLCM_CLT += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 2);

									// 10) Dissimilarity
									GLCM_DIS += P_ij * Math.abs(i-j);

									// 11) Homogeneity 1
									GLCM_HOM += P_ij / (1 + Math.abs(i-j));

									// 12) Homogeneity 2 or Inverse difference moment(IDM)
									GLCM_IDM += P_ij / (1 + Math.pow(i-j, 2));

									// 13) Inverse difference moment normalized (IDMN)
									GLCM_IDMN += P_ij / (1 + (Math.pow(i-j, 2) / Math.pow(histogramBins,2) ) );

									// 14) Inverse difference normalized (IDN)
									GLCM_IDN += P_ij / (1 + (Math.pow(i-j, 2) / ((double)histogramBins) ) );

									// 15 Correlation
									GLCM_CORR += P_ij *(i-GLCM_MEAN) * (j-GLCM_MEAN);

									// 16) Inverse Variance
									if (i != j){
										GLCM_IVAR += P_ij / Math.pow(i-j, 2);
									}

									// 17) Maximum Probability
									if (GLCM_MAXP <= P_ij){
										GLCM_MAXP = P_ij;
									}
								}
							}
							if(GLCM_VAR!=0)
								GLCM_CORR = GLCM_CORR / GLCM_VAR;

							// 18 ~ 20)  Sum Average (SUMA), Variance (SUMV), Entropy (SUME)
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMA += sum_i[i] * i;
								if(sum_i[i] != 0){
									GLCM_SUME -= sum_i[i] * Math.log(sum_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMV += Math.pow(i-GLCM_SUMA, 2) * sum_i[i];
							}

							// 21 ~ 23)  Difference Average (DIFA), Variance (DIFV), Entropy (DIFE)
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFA += diff_i[i] * i;
								if(diff_i[i] != 0){
									GLCM_DIFE -= diff_i[i] * Math.log(diff_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFV += Math.pow(i-GLCM_DIFA, 2) * diff_i[i];
							}

							// looping for HXY matrix..
							double HX = 0.0, HXY = 0.0, HXY1 = 0.0, HXY2 = 0.0; 
							for (int i = 0; i < histogramBins; i ++){
								if(Px[i]!=0){
									HX -= Px[i] * Math.log(Px[i]);								
								} 
								for (int j = 0; j < histogramBins; j++){
									P_ij = P_ij = GLCM_mat[i][j];

									if(P_ij != 0){
										HXY -= P_ij * Math.log(P_ij);
									}
									if(Px[i]!=0 && Px[j] !=0){
										HXY1 -= P_ij * Math.log(Px[i]*Px[j]);
										HXY2 -= Px[i]*Px[j] * Math.log(Px[i]*Px[j]);
									}				
								}
							}
							// 24) Informational measure of correlation 1 (IMC1)
							if(HX!=0)
								GLCM_IMC1 = (HXY - HXY1) / HX;

							// 25) Informational measure of correlation 1 (IMC1)
							if(HXY2-HXY>0)
								GLCM_IMC2 = Math.sqrt(1-Math.exp(-2*(HXY2-HXY)));

							if(texturemenu.create_value.getState()==true){
								if(texturemenu.direction_90.getState()==true){
									GLCM_ASM_90 += GLCM_ASM; GLCM_CON_90 += GLCM_CON; GLCM_ENT_90 += GLCM_ENT; GLCM_MEAN_90 += GLCM_MEAN; GLCM_VAR_90 += GLCM_VAR;
									GLCM_ACO_90+= GLCM_ACO; GLCM_CLP_90 += GLCM_CLP; GLCM_CLS_90 += GLCM_CLS; GLCM_CLT_90 += GLCM_CLT; GLCM_DIS_90 += GLCM_DIS;
									GLCM_HOM_90 += GLCM_HOM; GLCM_IDM_90 += GLCM_IDM; GLCM_IDMN_90 += GLCM_IDMN; GLCM_IDN_90 += GLCM_IDN;
									GLCM_CORR_90 += GLCM_CORR; GLCM_IVAR_90 += GLCM_IVAR; GLCM_MAXP_90 += GLCM_MAXP;						
									GLCM_SUMA_90 += GLCM_SUMA; GLCM_SUMV_90 += GLCM_SUMV; GLCM_SUME_90 += GLCM_SUME;
									GLCM_DIFA_90 += GLCM_DIFA; GLCM_DIFV_90 += GLCM_DIFV; GLCM_DIFE_90 += GLCM_DIFE;						
									GLCM_IMC1_90 += GLCM_IMC1; GLCM_IMC2_90 += GLCM_IMC2;
								}
								if(texturemenu.direction_average.getState()==true){
									GLCM_ASM_ave += GLCM_ASM; GLCM_CON_ave += GLCM_CON; GLCM_ENT_ave += GLCM_ENT; GLCM_MEAN_ave += GLCM_MEAN; GLCM_VAR_ave += GLCM_VAR;
									GLCM_ACO_ave+= GLCM_ACO; GLCM_CLP_ave += GLCM_CLP; GLCM_CLS_ave += GLCM_CLS; GLCM_CLT_ave += GLCM_CLT; GLCM_DIS_ave += GLCM_DIS;
									GLCM_HOM_ave += GLCM_HOM; GLCM_IDM_ave += GLCM_IDM; GLCM_IDMN_ave += GLCM_IDMN; GLCM_IDN_ave += GLCM_IDN;
									GLCM_CORR_ave += GLCM_CORR; GLCM_IVAR_ave += GLCM_IVAR; GLCM_MAXP_ave += GLCM_MAXP;						
									GLCM_SUMA_ave += GLCM_SUMA; GLCM_SUMV_ave += GLCM_SUMV; GLCM_SUME_ave += GLCM_SUME;
									GLCM_DIFA_ave += GLCM_DIFA; GLCM_DIFV_ave += GLCM_DIFV; GLCM_DIFE_ave += GLCM_DIFE;						
									GLCM_IMC1_ave += GLCM_IMC1; GLCM_IMC2_ave += GLCM_IMC2;
									GLCM_ASM_ave_pixel += GLCM_ASM; GLCM_CON_ave_pixel += GLCM_CON; GLCM_ENT_ave_pixel += GLCM_ENT; GLCM_MEAN_ave_pixel += GLCM_MEAN; GLCM_VAR_ave_pixel += GLCM_VAR;
									GLCM_ACO_ave_pixel+= GLCM_ACO; GLCM_CLP_ave_pixel += GLCM_CLP; GLCM_CLS_ave_pixel += GLCM_CLS; GLCM_CLT_ave_pixel += GLCM_CLT; GLCM_DIS_ave_pixel += GLCM_DIS;
									GLCM_HOM_ave_pixel += GLCM_HOM; GLCM_IDM_ave_pixel += GLCM_IDM; GLCM_IDMN_ave_pixel += GLCM_IDMN; GLCM_IDN_ave_pixel += GLCM_IDN;
									GLCM_CORR_ave_pixel += GLCM_CORR; GLCM_IVAR_ave_pixel += GLCM_IVAR; GLCM_MAXP_ave_pixel += GLCM_MAXP;						
									GLCM_SUMA_ave_pixel += GLCM_SUMA; GLCM_SUMV_ave_pixel += GLCM_SUMV; GLCM_SUME_ave_pixel += GLCM_SUME;
									GLCM_DIFA_ave_pixel += GLCM_DIFA; GLCM_DIFV_ave_pixel += GLCM_DIFV; GLCM_DIFE_ave_pixel += GLCM_DIFE;						
									GLCM_IMC1_ave_pixel += GLCM_IMC1; GLCM_IMC2_ave_pixel += GLCM_IMC2;
								}
							}
							
							if(texturemenu.create_map.getState()==true && texturemenu.direction_90.getState()==true){
								if(texturemenu.glcm_ASM.getState()==true){
									GLCM_ASM_90_Array[indexH*_W+indexW] = (float) GLCM_ASM; 
								}
								if(texturemenu.glcm_CON.getState()==true){
									GLCM_CON_90_Array[indexH*_W+indexW] = (float) GLCM_CON; 
								}
								if(texturemenu.glcm_ENT.getState()==true){
									GLCM_ENT_90_Array[indexH*_W+indexW] = (float) GLCM_ENT; 
								}
								if(texturemenu.glcm_MEAN.getState()==true){
									GLCM_MEAN_90_Array[indexH*_W+indexW] = (float) GLCM_MEAN; 
								}
								if(texturemenu.glcm_VAR.getState()==true){
									GLCM_VAR_90_Array[indexH*_W+indexW] = (float) GLCM_VAR; 
								}
								if(texturemenu.glcm_ACO.getState()==true){
									GLCM_ACO_90_Array[indexH*_W+indexW] = (float) GLCM_ACO; 
								}
								if(texturemenu.glcm_CLP.getState()==true){
									GLCM_CLP_90_Array[indexH*_W+indexW] = (float) GLCM_CLP; 
								}
								if(texturemenu.glcm_CLS.getState()==true){
									GLCM_CLS_90_Array[indexH*_W+indexW] = (float) GLCM_CLS; 
								}
								if(texturemenu.glcm_CLT.getState()==true){
									GLCM_CLT_90_Array[indexH*_W+indexW] = (float) GLCM_CLT; 
								}
								if(texturemenu.glcm_DIS.getState()==true){
									GLCM_DIS_90_Array[indexH*_W+indexW] = (float) GLCM_DIS; 
								}
								if(texturemenu.glcm_HOM.getState()==true){
									GLCM_HOM_90_Array[indexH*_W+indexW] = (float) GLCM_HOM; 
								}
								if(texturemenu.glcm_IDM.getState()==true){
									GLCM_IDM_90_Array[indexH*_W+indexW] = (float) GLCM_IDM; 
								}
								if(texturemenu.glcm_IDMN.getState()==true){
									GLCM_IDMN_90_Array[indexH*_W+indexW] = (float) GLCM_IDMN; 
								}
								if(texturemenu.glcm_IDN.getState()==true){
									GLCM_IDN_90_Array[indexH*_W+indexW] = (float) GLCM_IDN; 
								}
								if(texturemenu.glcm_CORR.getState()==true){
									GLCM_CORR_90_Array[indexH*_W+indexW] = (float) GLCM_CORR; 
								}
								if(texturemenu.glcm_IVAR.getState()==true){
									GLCM_IVAR_90_Array[indexH*_W+indexW] = (float) GLCM_IVAR; 
								}
								if(texturemenu.glcm_MAXP.getState()==true){
									GLCM_MAXP_90_Array[indexH*_W+indexW] = (float) GLCM_MAXP; 
								}
								if(texturemenu.glcm_SUMA.getState()==true){
									GLCM_SUMA_90_Array[indexH*_W+indexW] = (float) GLCM_SUMA; 
								}
								if(texturemenu.glcm_SUMV.getState()==true){
									GLCM_SUMV_90_Array[indexH*_W+indexW] = (float) GLCM_SUMV; 
								}
								if(texturemenu.glcm_SUME.getState()==true){
									GLCM_SUME_90_Array[indexH*_W+indexW] = (float) GLCM_SUME; 
								}
								if(texturemenu.glcm_DIFV.getState()==true){
									GLCM_DIFV_90_Array[indexH*_W+indexW] = (float) GLCM_DIFV; 
								}
								if(texturemenu.glcm_DIFE.getState()==true){
									GLCM_DIFE_90_Array[indexH*_W+indexW] = (float) GLCM_DIFE; 
								}
								if(texturemenu.glcm_IMC1.getState()==true){
									GLCM_IMC1_90_Array[indexH*_W+indexW] = (float) GLCM_IMC1; 
								}
								if(texturemenu.glcm_IMC2.getState()==true){
									GLCM_IMC2_90_Array[indexH*_W+indexW] = (float) GLCM_IMC2; 
								}
							}
						}

						//135°
						GLCM_mat = new double[histogramBins][histogramBins];
						if(texturemenu.direction_135.getState()==true || texturemenu.direction_average.getState()==true){
							int windowCount = 0;
							for(int kernel_h=-limitSize+1;kernel_h<=limitSize;kernel_h++){
								for(int kernel_w=-limitSize+1;kernel_w<=limitSize;kernel_w++){
									windowCount++;
									int referecePixel = (int)img2DArray[indexW+kernel_w][indexH+kernel_h];
									int neighborPixel = (int)img2DArray[indexW+kernel_w-1][indexH+kernel_h-1];
									GLCM_mat[referecePixel][neighborPixel] = GLCM_mat[referecePixel][neighborPixel]+1; 
									GLCM_mat[neighborPixel][referecePixel] = GLCM_mat[neighborPixel][referecePixel]+1; //symetry mtx
								}
							}
							for(int h=0;h<histogramBins;h++){ // normalization
								for(int w=0;w<histogramBins;w++){
									GLCM_mat[w][h] = GLCM_mat[w][h]/ ((windowSize-1)*(windowSize-1)*2);
								}
							}

							double P_ij = 0;
							double GLCM_ASM = 0.0, GLCM_CON = 0.0, GLCM_ENT = 0.0, GLCM_MEAN = 0.0, GLCM_VAR = 0.0;
							double GLCM_ACO = 0.0, GLCM_CLP = 0.0, GLCM_CLS = 0.0, GLCM_CLT = 0.0, GLCM_DIS = 0.0;
							double GLCM_HOM = 0.0, GLCM_IDM = 0.0, GLCM_IDMN = 0.0, GLCM_IDN = 0.0;
							double GLCM_CORR = 0.0, GLCM_IVAR = 0.0, GLCM_MAXP = 0.0;						
							double GLCM_SUMA = 0.0, GLCM_SUMV = 0.0, GLCM_SUME = 0.0;
							double GLCM_DIFA = 0.0, GLCM_DIFV = 0.0, GLCM_DIFE = 0.0;						
							double GLCM_IMC1 = 0.0, GLCM_IMC2 = 0.0;

							// 3) calculate marginal, diff, sum matrix
							// 3-1) for marginal, GLCM is symetry mtx. Px is same with Py. Ux=Uy=U(GLCM_MEAN).						
							double[] Px = new double[histogramBins];
							Arrays.fill(Px, 0.0);
							double[] diff_i = new double[histogramBins];
							Arrays.fill(diff_i, 0.0);
							double[] sum_i = new double[2*histogramBins-1];
							Arrays.fill(sum_i, 0.0);
							int diff, sum;
							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									diff = Math.abs(i-j);
									sum = i+j;

									diff_i[diff] +=P_ij;
									sum_i[sum] +=P_ij;
									Px[i] += P_ij;

									GLCM_MEAN += i * P_ij;
								}
							}

							for (int i = 0; i < histogramBins; i ++){
								for (int j = 0; j < histogramBins; j++){
									P_ij = GLCM_mat[i][j];
									if(P_ij==0)continue;
									// 1) Angular secon moment or Energy
									GLCM_ASM += Math.pow(P_ij, 2);

									// 2) Contrast
									GLCM_CON += P_ij * Math.pow(i-j, 2);

									// 3) Entropy 
									if (P_ij != 0){
										GLCM_ENT += P_ij * (-1 * Math.log(P_ij)/Math.log(2));
									}

									// 4) Mean is already calculated. 
									// 5) Variance 
									GLCM_VAR += P_ij * Math.pow(i - GLCM_MEAN, 2);

									// 6) Autocorrelation1
									GLCM_ACO += P_ij * i * j;

									// 7) Cluster Prominence
									GLCM_CLP += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 4); 

									// 8) Cluster Shade
									GLCM_CLS += P_ij * Math.pow(i + j - GLCM_MEAN -GLCM_MEAN, 3);

									// 9) Cluster Tendency
									GLCM_CLT += P_ij * Math.pow(i + j - GLCM_MEAN - GLCM_MEAN, 2);

									// 10) Dissimilarity
									GLCM_DIS += P_ij * Math.abs(i-j);

									// 11) Homogeneity 1
									GLCM_HOM += P_ij / (1 + Math.abs(i-j));

									// 12) Homogeneity 2 or Inverse difference moment(IDM)
									GLCM_IDM += P_ij / (1 + Math.pow(i-j, 2));

									// 13) Inverse difference moment normalized (IDMN)
									GLCM_IDMN += P_ij / (1 + (Math.pow(i-j, 2) / Math.pow(histogramBins,2) ) );

									// 14) Inverse difference normalized (IDN)
									GLCM_IDN += P_ij / (1 + (Math.pow(i-j, 2) / ((double)histogramBins) ) );

									// 15 Correlation
									GLCM_CORR += P_ij *(i-GLCM_MEAN) * (j-GLCM_MEAN);

									// 16) Inverse Variance
									if (i != j){
										GLCM_IVAR += P_ij / Math.pow(i-j, 2);
									}

									// 17) Maximum Probability
									if (GLCM_MAXP <= P_ij){
										GLCM_MAXP = P_ij;
									}
								}
							}
							if(GLCM_VAR!=0)
								GLCM_CORR = GLCM_CORR / GLCM_VAR;

							// 18 ~ 20)  Sum Average (SUMA), Variance (SUMV), Entropy (SUME)
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMA += sum_i[i] * i;
								if(sum_i[i] != 0){
									GLCM_SUME -= sum_i[i] * Math.log(sum_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < sum_i.length; i ++){
								GLCM_SUMV += Math.pow(i-GLCM_SUMA, 2) * sum_i[i];
							}

							// 21 ~ 23)  Difference Average (DIFA), Variance (DIFV), Entropy (DIFE)
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFA += diff_i[i] * i;
								if(diff_i[i] != 0){
									GLCM_DIFE -= diff_i[i] * Math.log(diff_i[i])/Math.log(2);
								}
							}
							for (int i = 0; i < diff_i.length; i ++){
								GLCM_DIFV += Math.pow(i-GLCM_DIFA, 2) * diff_i[i];
							}

							// looping for HXY matrix..
							double HX = 0.0, HXY = 0.0, HXY1 = 0.0, HXY2 = 0.0; 
							for (int i = 0; i < histogramBins; i ++){
								if(Px[i]!=0){
									HX -= Px[i] * Math.log(Px[i]);								
								} 
								for (int j = 0; j < histogramBins; j++){
									P_ij = P_ij = GLCM_mat[i][j];

									if(P_ij != 0){
										HXY -= P_ij * Math.log(P_ij);
									}
									if(Px[i]!=0 && Px[j] !=0){
										HXY1 -= P_ij * Math.log(Px[i]*Px[j]);
										HXY2 -= Px[i]*Px[j] * Math.log(Px[i]*Px[j]);
									}				
								}
							}

							// 24) Informational measure of correlation 1 (IMC1)
							if(HX!=0)
								GLCM_IMC1 = (HXY - HXY1) / HX;

							// 25) Informational measure of correlation 1 (IMC1)
							if(HXY2-HXY>0)
								GLCM_IMC2 = Math.sqrt(1-Math.exp(-2*(HXY2-HXY)));                                  

							if(texturemenu.create_value.getState()==true){
								if(texturemenu.direction_135.getState()==true){
									GLCM_ASM_135 += GLCM_ASM; GLCM_CON_135 += GLCM_CON; GLCM_ENT_135 += GLCM_ENT; GLCM_MEAN_135 += GLCM_MEAN; GLCM_VAR_135 += GLCM_VAR;
									GLCM_ACO_135+= GLCM_ACO; GLCM_CLP_135 += GLCM_CLP; GLCM_CLS_135 += GLCM_CLS; GLCM_CLT_135 += GLCM_CLT; GLCM_DIS_135 += GLCM_DIS;
									GLCM_HOM_135 += GLCM_HOM; GLCM_IDM_135 += GLCM_IDM; GLCM_IDMN_135 += GLCM_IDMN; GLCM_IDN_135 += GLCM_IDN;
									GLCM_CORR_135 += GLCM_CORR; GLCM_IVAR_135 += GLCM_IVAR; GLCM_MAXP_135 += GLCM_MAXP;						
									GLCM_SUMA_135 += GLCM_SUMA; GLCM_SUMV_135 += GLCM_SUMV; GLCM_SUME_135 += GLCM_SUME;
									GLCM_DIFA_135 += GLCM_DIFA; GLCM_DIFV_135 += GLCM_DIFV; GLCM_DIFE_135 += GLCM_DIFE;						
									GLCM_IMC1_135 += GLCM_IMC1; GLCM_IMC2_135 += GLCM_IMC2;
								}
								if(texturemenu.direction_average.getState()==true){
									GLCM_ASM_ave += GLCM_ASM; GLCM_CON_ave += GLCM_CON; GLCM_ENT_ave += GLCM_ENT; GLCM_MEAN_ave += GLCM_MEAN; GLCM_VAR_ave += GLCM_VAR;
									GLCM_ACO_ave+= GLCM_ACO; GLCM_CLP_ave += GLCM_CLP; GLCM_CLS_ave += GLCM_CLS; GLCM_CLT_ave += GLCM_CLT; GLCM_DIS_ave += GLCM_DIS;
									GLCM_HOM_ave += GLCM_HOM; GLCM_IDM_ave += GLCM_IDM; GLCM_IDMN_ave += GLCM_IDMN; GLCM_IDN_ave += GLCM_IDN;
									GLCM_CORR_ave += GLCM_CORR; GLCM_IVAR_ave += GLCM_IVAR; GLCM_MAXP_ave += GLCM_MAXP;						
									GLCM_SUMA_ave += GLCM_SUMA; GLCM_SUMV_ave += GLCM_SUMV; GLCM_SUME_ave += GLCM_SUME;
									GLCM_DIFA_ave += GLCM_DIFA; GLCM_DIFV_ave += GLCM_DIFV; GLCM_DIFE_ave += GLCM_DIFE;						
									GLCM_IMC1_ave += GLCM_IMC1; GLCM_IMC2_ave += GLCM_IMC2;
									GLCM_ASM_ave_pixel += GLCM_ASM; GLCM_CON_ave_pixel += GLCM_CON; GLCM_ENT_ave_pixel += GLCM_ENT; GLCM_MEAN_ave_pixel += GLCM_MEAN; GLCM_VAR_ave_pixel += GLCM_VAR;
									GLCM_ACO_ave_pixel+= GLCM_ACO; GLCM_CLP_ave_pixel += GLCM_CLP; GLCM_CLS_ave_pixel += GLCM_CLS; GLCM_CLT_ave_pixel += GLCM_CLT; GLCM_DIS_ave_pixel += GLCM_DIS;
									GLCM_HOM_ave_pixel += GLCM_HOM; GLCM_IDM_ave_pixel += GLCM_IDM; GLCM_IDMN_ave_pixel += GLCM_IDMN; GLCM_IDN_ave_pixel += GLCM_IDN;
									GLCM_CORR_ave_pixel += GLCM_CORR; GLCM_IVAR_ave_pixel += GLCM_IVAR; GLCM_MAXP_ave_pixel += GLCM_MAXP;						
									GLCM_SUMA_ave_pixel += GLCM_SUMA; GLCM_SUMV_ave_pixel += GLCM_SUMV; GLCM_SUME_ave_pixel += GLCM_SUME;
									GLCM_DIFA_ave_pixel += GLCM_DIFA; GLCM_DIFV_ave_pixel += GLCM_DIFV; GLCM_DIFE_ave_pixel += GLCM_DIFE;						
									GLCM_IMC1_ave_pixel += GLCM_IMC1; GLCM_IMC2_ave_pixel += GLCM_IMC2;
								}
							}
							
							if(texturemenu.create_map.getState()==true && texturemenu.direction_135.getState()==true){
								if(texturemenu.glcm_ASM.getState()==true){
									GLCM_ASM_135_Array[indexH*_W+indexW] = (float) GLCM_ASM; 
								}
								if(texturemenu.glcm_CON.getState()==true){
									GLCM_CON_135_Array[indexH*_W+indexW] = (float) GLCM_CON; 
								}
								if(texturemenu.glcm_ENT.getState()==true){
									GLCM_ENT_135_Array[indexH*_W+indexW] = (float) GLCM_ENT; 
								}
								if(texturemenu.glcm_MEAN.getState()==true){
									GLCM_MEAN_135_Array[indexH*_W+indexW] = (float) GLCM_MEAN; 
								}
								if(texturemenu.glcm_VAR.getState()==true){
									GLCM_VAR_135_Array[indexH*_W+indexW] = (float) GLCM_VAR; 
								}
								if(texturemenu.glcm_ACO.getState()==true){
									GLCM_ACO_135_Array[indexH*_W+indexW] = (float) GLCM_ACO; 
								}
								if(texturemenu.glcm_CLP.getState()==true){
									GLCM_CLP_135_Array[indexH*_W+indexW] = (float) GLCM_CLP; 
								}
								if(texturemenu.glcm_CLS.getState()==true){
									GLCM_CLS_135_Array[indexH*_W+indexW] = (float) GLCM_CLS; 
								}
								if(texturemenu.glcm_CLT.getState()==true){
									GLCM_CLT_135_Array[indexH*_W+indexW] = (float) GLCM_CLT; 
								}
								if(texturemenu.glcm_DIS.getState()==true){
									GLCM_DIS_135_Array[indexH*_W+indexW] = (float) GLCM_DIS; 
								}
								if(texturemenu.glcm_HOM.getState()==true){
									GLCM_HOM_135_Array[indexH*_W+indexW] = (float) GLCM_HOM; 
								}
								if(texturemenu.glcm_IDM.getState()==true){
									GLCM_IDM_135_Array[indexH*_W+indexW] = (float) GLCM_IDM; 
								}
								if(texturemenu.glcm_IDMN.getState()==true){
									GLCM_IDMN_135_Array[indexH*_W+indexW] = (float) GLCM_IDMN; 
								}
								if(texturemenu.glcm_IDN.getState()==true){
									GLCM_IDN_135_Array[indexH*_W+indexW] = (float) GLCM_IDN; 
								}
								if(texturemenu.glcm_CORR.getState()==true){
									GLCM_CORR_135_Array[indexH*_W+indexW] = (float) GLCM_CORR; 
								}
								if(texturemenu.glcm_IVAR.getState()==true){
									GLCM_IVAR_135_Array[indexH*_W+indexW] = (float) GLCM_IVAR; 
								}
								if(texturemenu.glcm_MAXP.getState()==true){
									GLCM_MAXP_135_Array[indexH*_W+indexW] = (float) GLCM_MAXP; 
								}
								if(texturemenu.glcm_SUMA.getState()==true){
									GLCM_SUMA_135_Array[indexH*_W+indexW] = (float) GLCM_SUMA; 
								}
								if(texturemenu.glcm_SUMV.getState()==true){
									GLCM_SUMV_135_Array[indexH*_W+indexW] = (float) GLCM_SUMV; 
								}
								if(texturemenu.glcm_SUME.getState()==true){
									GLCM_SUME_135_Array[indexH*_W+indexW] = (float) GLCM_SUME; 
								}
								if(texturemenu.glcm_DIFV.getState()==true){
									GLCM_DIFV_135_Array[indexH*_W+indexW] = (float) GLCM_DIFV; 
								}
								if(texturemenu.glcm_DIFE.getState()==true){
									GLCM_DIFE_135_Array[indexH*_W+indexW] = (float) GLCM_DIFE; 
								}
								if(texturemenu.glcm_IMC1.getState()==true){
									GLCM_IMC1_135_Array[indexH*_W+indexW] = (float) GLCM_IMC1; 
								}
								if(texturemenu.glcm_IMC2.getState()==true){
									GLCM_IMC2_135_Array[indexH*_W+indexW] = (float) GLCM_IMC2; 
								}
							}
							
							if(texturemenu.create_map.getState()==true && texturemenu.direction_average.getState()==true){
								if(texturemenu.glcm_ASM.getState()==true){
									GLCM_ASM_ave_Array[indexH*_W+indexW] = (float) GLCM_ASM_ave_pixel/4; 
								}
								if(texturemenu.glcm_CON.getState()==true){
									GLCM_CON_ave_Array[indexH*_W+indexW] = (float) GLCM_CON_ave_pixel/4; 
								}
								if(texturemenu.glcm_ENT.getState()==true){
									GLCM_ENT_ave_Array[indexH*_W+indexW] = (float) GLCM_ENT_ave_pixel/4; 
								}
								if(texturemenu.glcm_MEAN.getState()==true){
									GLCM_MEAN_ave_Array[indexH*_W+indexW] = (float) GLCM_MEAN_ave_pixel/4; 
								}
								if(texturemenu.glcm_VAR.getState()==true){
									GLCM_VAR_ave_Array[indexH*_W+indexW] = (float) GLCM_VAR_ave_pixel/4; 
								}
								if(texturemenu.glcm_ACO.getState()==true){
									GLCM_ACO_ave_Array[indexH*_W+indexW] = (float) GLCM_ACO_ave_pixel/4; 
								}
								if(texturemenu.glcm_CLP.getState()==true){
									GLCM_CLP_ave_Array[indexH*_W+indexW] = (float) GLCM_CLP_ave_pixel/4; 
								}
								if(texturemenu.glcm_CLS.getState()==true){
									GLCM_CLS_ave_Array[indexH*_W+indexW] = (float) GLCM_CLS_ave_pixel/4; 
								}
								if(texturemenu.glcm_CLT.getState()==true){
									GLCM_CLT_ave_Array[indexH*_W+indexW] = (float) GLCM_CLT_ave_pixel/4; 
								}
								if(texturemenu.glcm_DIS.getState()==true){
									GLCM_DIS_ave_Array[indexH*_W+indexW] = (float) GLCM_DIS_ave_pixel/4; 
								}
								if(texturemenu.glcm_HOM.getState()==true){
									GLCM_HOM_ave_Array[indexH*_W+indexW] = (float) GLCM_HOM_ave_pixel/4; 
								}
								if(texturemenu.glcm_IDM.getState()==true){
									GLCM_IDM_ave_Array[indexH*_W+indexW] = (float) GLCM_IDM_ave_pixel/4; 
								}
								if(texturemenu.glcm_IDMN.getState()==true){
									GLCM_IDMN_ave_Array[indexH*_W+indexW] = (float) GLCM_IDMN_ave_pixel/4; 
								}
								if(texturemenu.glcm_IDN.getState()==true){
									GLCM_IDN_ave_Array[indexH*_W+indexW] = (float) GLCM_IDN_ave_pixel/4; 
								}
								if(texturemenu.glcm_CORR.getState()==true){
									GLCM_CORR_ave_Array[indexH*_W+indexW] = (float) GLCM_CORR_ave_pixel/4; 
								}
								if(texturemenu.glcm_IVAR.getState()==true){
									GLCM_IVAR_ave_Array[indexH*_W+indexW] = (float) GLCM_IVAR_ave_pixel/4; 
								}
								if(texturemenu.glcm_MAXP.getState()==true){
									GLCM_MAXP_ave_Array[indexH*_W+indexW] = (float) GLCM_MAXP_ave_pixel/4; 
								}
								if(texturemenu.glcm_SUMA.getState()==true){
									GLCM_SUMA_ave_Array[indexH*_W+indexW] = (float) GLCM_SUMA_ave_pixel/4; 
								}
								if(texturemenu.glcm_SUMV.getState()==true){
									GLCM_SUMV_ave_Array[indexH*_W+indexW] = (float) GLCM_SUMV_ave_pixel/4; 
								}
								if(texturemenu.glcm_SUME.getState()==true){
									GLCM_SUME_ave_Array[indexH*_W+indexW] = (float) GLCM_SUME_ave_pixel/4; 
								}
								if(texturemenu.glcm_DIFV.getState()==true){
									GLCM_DIFV_ave_Array[indexH*_W+indexW] = (float) GLCM_DIFV_ave_pixel/4; 
								}
								if(texturemenu.glcm_DIFE.getState()==true){
									GLCM_DIFE_ave_Array[indexH*_W+indexW] = (float) GLCM_DIFE_ave_pixel/4; 
								}
								if(texturemenu.glcm_IMC1.getState()==true){
									GLCM_IMC1_ave_Array[indexH*_W+indexW] = (float) GLCM_IMC1_ave_pixel/4; 
								}
								if(texturemenu.glcm_IMC2.getState()==true){
									GLCM_IMC2_ave_Array[indexH*_W+indexW] = (float) GLCM_IMC2_ave_pixel/4; 
								}
							}
						}
					}
					else{

					}

				}
			}


			if(texturemenu.create_value.getState()==true){
				if(texture_Summary==null)texture_Summary = new ResultsTable();

				if(texturemenu.direction_0.getState()==true){
					GLCM_ASM_0 = GLCM_ASM_0/roiPixelCount; GLCM_CON_0 = GLCM_CON_0/roiPixelCount; GLCM_ENT_0 = GLCM_ENT_0/roiPixelCount; GLCM_MEAN_0 = GLCM_MEAN_0/roiPixelCount;
					GLCM_VAR_0 = GLCM_VAR_0/roiPixelCount; GLCM_ACO_0= GLCM_ACO_0/roiPixelCount; GLCM_CLP_0 = GLCM_CLP_0/roiPixelCount; GLCM_CLS_0 = GLCM_CLS_0/roiPixelCount; 
					GLCM_CLT_0 = GLCM_CLT_0/roiPixelCount; GLCM_DIS_0 = GLCM_DIS_0/roiPixelCount;GLCM_HOM_0 = GLCM_HOM_0/roiPixelCount; GLCM_IDM_0 = GLCM_IDM_0/roiPixelCount; 
					GLCM_IDMN_0 = GLCM_IDMN_0/roiPixelCount; GLCM_IDN_0 = GLCM_IDN_0/roiPixelCount;GLCM_CORR_0 = GLCM_CORR_0/roiPixelCount; GLCM_IVAR_0 = GLCM_IVAR_0/roiPixelCount; 
					GLCM_MAXP_0 = GLCM_MAXP_0/roiPixelCount; GLCM_SUMA_0 = GLCM_SUMA_0/roiPixelCount; GLCM_SUMV_0 = GLCM_SUMV_0/roiPixelCount; 
					GLCM_SUME_0 = GLCM_SUME_0/roiPixelCount; GLCM_DIFA_0 = GLCM_DIFA_0/roiPixelCount; GLCM_DIFV_0 = GLCM_DIFV_0/roiPixelCount; 
					GLCM_DIFE_0 = GLCM_DIFE_0/roiPixelCount; GLCM_IMC1_0 = GLCM_IMC1_0/roiPixelCount; GLCM_IMC2_0 = GLCM_IMC2_0/roiPixelCount;

					texture_Summary.incrementCounter();
					texture_Summary.addValue("Size", windowSize+" X "+windowSize);
					texture_Summary.addValue("Bins", ""+histogramBins);
					texture_Summary.addValue("Min/Max", histoMinValue+" / "+histoMaxValue);
					texture_Summary.addValue("Diretion", "0\u00B0");
					texture_Summary.addValue("Count", roiPixelCount);
					texture_Summary.addLabel("GLCM_0\u00B0");

					if(texturemenu.glcm_ASM.getState()==true)
						texture_Summary.addValue("ASM", GLCM_ASM_0);
					if(texturemenu.glcm_CON.getState()==true)
						texture_Summary.addValue("CON", GLCM_CON_0);
					if(texturemenu.glcm_ENT.getState()==true)
						texture_Summary.addValue("ENT", GLCM_ENT_0);
					if(texturemenu.glcm_MEAN.getState()==true)
						texture_Summary.addValue("MEAN", GLCM_MEAN_0);
					if(texturemenu.glcm_VAR.getState()==true)
						texture_Summary.addValue("VAR", GLCM_VAR_0);
					if(texturemenu.glcm_ACO.getState()==true)
						texture_Summary.addValue("ACO", GLCM_ACO_0);
					if(texturemenu.glcm_CLP.getState()==true)
						texture_Summary.addValue("CLP", GLCM_CLP_0);
					if(texturemenu.glcm_CLS.getState()==true)
						texture_Summary.addValue("CLS", GLCM_CLS_0);
					if(texturemenu.glcm_CLT.getState()==true)
						texture_Summary.addValue("CLT", GLCM_CLT_0);
					if(texturemenu.glcm_DIS.getState()==true)
						texture_Summary.addValue("DIS", GLCM_DIS_0);
					if(texturemenu.glcm_HOM.getState()==true)
						texture_Summary.addValue("HOM", GLCM_HOM_0);
					if(texturemenu.glcm_IDM.getState()==true)
						texture_Summary.addValue("IDM", GLCM_IDM_0);
					if(texturemenu.glcm_IDMN.getState()==true)
						texture_Summary.addValue("IDMN", GLCM_IDMN_0);
					if(texturemenu.glcm_IDN.getState()==true)
						texture_Summary.addValue("IDN", GLCM_IDN_0);
					if(texturemenu.glcm_CORR.getState()==true)
						texture_Summary.addValue("CORR", GLCM_CORR_0);
					if(texturemenu.glcm_IVAR.getState()==true)
						texture_Summary.addValue("IVAR", GLCM_IVAR_0);
					if(texturemenu.glcm_MAXP.getState()==true)
						texture_Summary.addValue("MAXP", GLCM_MAXP_0);
					if(texturemenu.glcm_SUMA.getState()==true)
						texture_Summary.addValue("SUMA", GLCM_SUMA_0);
					if(texturemenu.glcm_SUMV.getState()==true)
						texture_Summary.addValue("SUMV", GLCM_SUMV_0);
					if(texturemenu.glcm_SUME.getState()==true)
						texture_Summary.addValue("SUME", GLCM_SUME_0);
					if(texturemenu.glcm_DIFV.getState()==true)
						texture_Summary.addValue("DIFV", GLCM_DIFV_0);
					if(texturemenu.glcm_DIFE.getState()==true)
						texture_Summary.addValue("DIFE", GLCM_DIFE_0);
					if(texturemenu.glcm_IMC1.getState()==true)
						texture_Summary.addValue("IMC1", GLCM_IMC1_0);
					if(texturemenu.glcm_IMC2.getState()==true)
						texture_Summary.addValue("IMC2", GLCM_IMC2_0);
				}
				if(texturemenu.direction_45.getState()==true){
					GLCM_ASM_45 = GLCM_ASM_45/roiPixelCount; GLCM_CON_45 = GLCM_CON_45/roiPixelCount; GLCM_ENT_45 = GLCM_ENT_45/roiPixelCount; 
					GLCM_MEAN_45 = GLCM_MEAN_45/roiPixelCount; GLCM_VAR_45 = GLCM_VAR_45/roiPixelCount;	GLCM_ACO_45= GLCM_ACO_45/roiPixelCount;
					GLCM_CLP_45 = GLCM_CLP_45/roiPixelCount; GLCM_CLS_45 = GLCM_CLS_45/roiPixelCount; GLCM_CLT_45 = GLCM_CLT_45/roiPixelCount; 
					GLCM_DIS_45 = GLCM_DIS_45/roiPixelCount; GLCM_HOM_45 = GLCM_HOM_45/roiPixelCount; GLCM_IDM_45 = GLCM_IDM_45/roiPixelCount; 
					GLCM_IDMN_45 = GLCM_IDMN_45/roiPixelCount; GLCM_IDN_45 = GLCM_IDN_45/roiPixelCount; GLCM_CORR_45 = GLCM_CORR_45/roiPixelCount; 
					GLCM_IVAR_45 = GLCM_IVAR_45/roiPixelCount; GLCM_MAXP_45 = GLCM_MAXP_45/roiPixelCount; GLCM_SUMA_45 = GLCM_SUMA_45/roiPixelCount; 
					GLCM_SUMV_45 = GLCM_SUMV_45/roiPixelCount; GLCM_SUME_45 = GLCM_SUME_45/roiPixelCount; GLCM_DIFA_45 = GLCM_DIFA_45/roiPixelCount; 
					GLCM_DIFV_45 = GLCM_DIFV_45/roiPixelCount; GLCM_DIFE_45 = GLCM_DIFE_45/roiPixelCount; GLCM_IMC1_45 = GLCM_IMC1_45/roiPixelCount; 
					GLCM_IMC2_45 = GLCM_IMC2_45/roiPixelCount;

					texture_Summary.incrementCounter();
					texture_Summary.addValue("Size", windowSize+" X "+windowSize);
					texture_Summary.addValue("Bins", ""+histogramBins);
					texture_Summary.addValue("Min/Max", histoMinValue+" / "+histoMaxValue);
					texture_Summary.addValue("Diretion", "45\u00B0");
					texture_Summary.addValue("Count", roiPixelCount);
					texture_Summary.addLabel("GLCM_45\u00B0");

					if(texturemenu.glcm_ASM.getState()==true)
						texture_Summary.addValue("ASM", GLCM_ASM_45);
					if(texturemenu.glcm_CON.getState()==true)
						texture_Summary.addValue("CON", GLCM_CON_45);
					if(texturemenu.glcm_ENT.getState()==true)
						texture_Summary.addValue("ENT", GLCM_ENT_45);
					if(texturemenu.glcm_MEAN.getState()==true)
						texture_Summary.addValue("MEAN", GLCM_MEAN_45);
					if(texturemenu.glcm_VAR.getState()==true)
						texture_Summary.addValue("VAR", GLCM_VAR_45);
					if(texturemenu.glcm_ACO.getState()==true)
						texture_Summary.addValue("ACO", GLCM_ACO_45);
					if(texturemenu.glcm_CLP.getState()==true)
						texture_Summary.addValue("CLP", GLCM_CLP_45);
					if(texturemenu.glcm_CLS.getState()==true)
						texture_Summary.addValue("CLS", GLCM_CLS_45);
					if(texturemenu.glcm_CLT.getState()==true)
						texture_Summary.addValue("CLT", GLCM_CLT_45);
					if(texturemenu.glcm_DIS.getState()==true)
						texture_Summary.addValue("DIS", GLCM_DIS_45);
					if(texturemenu.glcm_HOM.getState()==true)
						texture_Summary.addValue("HOM", GLCM_HOM_45);
					if(texturemenu.glcm_IDM.getState()==true)
						texture_Summary.addValue("IDM", GLCM_IDM_45);
					if(texturemenu.glcm_IDMN.getState()==true)
						texture_Summary.addValue("IDMN", GLCM_IDMN_45);
					if(texturemenu.glcm_IDN.getState()==true)
						texture_Summary.addValue("IDN", GLCM_IDN_45);
					if(texturemenu.glcm_CORR.getState()==true)
						texture_Summary.addValue("CORR", GLCM_CORR_45);
					if(texturemenu.glcm_IVAR.getState()==true)
						texture_Summary.addValue("IVAR", GLCM_IVAR_45);
					if(texturemenu.glcm_MAXP.getState()==true)
						texture_Summary.addValue("MAXP", GLCM_MAXP_45);
					if(texturemenu.glcm_SUMA.getState()==true)
						texture_Summary.addValue("SUMA", GLCM_SUMA_45);
					if(texturemenu.glcm_SUMV.getState()==true)
						texture_Summary.addValue("SUMV", GLCM_SUMV_45);
					if(texturemenu.glcm_SUME.getState()==true)
						texture_Summary.addValue("SUME", GLCM_SUME_45);
					if(texturemenu.glcm_DIFV.getState()==true)
						texture_Summary.addValue("DIFV", GLCM_DIFV_45);
					if(texturemenu.glcm_DIFE.getState()==true)
						texture_Summary.addValue("DIFE", GLCM_DIFE_45);
					if(texturemenu.glcm_IMC1.getState()==true)
						texture_Summary.addValue("IMC1", GLCM_IMC1_45);
					if(texturemenu.glcm_IMC2.getState()==true)
						texture_Summary.addValue("IMC2", GLCM_IMC2_45);
				}
				if(texturemenu.direction_90.getState()==true){
					GLCM_ASM_90 = GLCM_ASM_90/roiPixelCount; GLCM_CON_90 = GLCM_CON_90/roiPixelCount; GLCM_ENT_90 = GLCM_ENT_90/roiPixelCount; GLCM_MEAN_90 = GLCM_MEAN_90/roiPixelCount; GLCM_VAR_90 = GLCM_VAR_90/roiPixelCount;
					GLCM_ACO_90= GLCM_ACO_90/roiPixelCount; GLCM_CLP_90 = GLCM_CLP_90/roiPixelCount; GLCM_CLS_90 = GLCM_CLS_90/roiPixelCount; GLCM_CLT_90 = GLCM_CLT_90/roiPixelCount; GLCM_DIS_90 = GLCM_DIS_90/roiPixelCount;
					GLCM_HOM_90 = GLCM_HOM_90/roiPixelCount; GLCM_IDM_90 = GLCM_IDM_90/roiPixelCount; GLCM_IDMN_90 = GLCM_IDMN_90/roiPixelCount; GLCM_IDN_90 = GLCM_IDN_90/roiPixelCount;
					GLCM_CORR_90 = GLCM_CORR_90/roiPixelCount; GLCM_IVAR_90 = GLCM_IVAR_90/roiPixelCount; GLCM_MAXP_90 = GLCM_MAXP_90/roiPixelCount;						
					GLCM_SUMA_90 = GLCM_SUMA_90/roiPixelCount; GLCM_SUMV_90 = GLCM_SUMV_90/roiPixelCount; GLCM_SUME_90 = GLCM_SUME_90/roiPixelCount;
					GLCM_DIFA_90 = GLCM_DIFA_90/roiPixelCount; GLCM_DIFV_90 = GLCM_DIFV_90/roiPixelCount; GLCM_DIFE_90 = GLCM_DIFE_90/roiPixelCount;						
					GLCM_IMC1_90 = GLCM_IMC1_90/roiPixelCount; GLCM_IMC2_90 = GLCM_IMC2_90/roiPixelCount;

					texture_Summary.incrementCounter();
					texture_Summary.addValue("Size", windowSize+" X "+windowSize);
					texture_Summary.addValue("Bins", ""+histogramBins);
					texture_Summary.addValue("Min/Max", histoMinValue+" / "+histoMaxValue);
					texture_Summary.addValue("Diretion", "90\u00B0");
					texture_Summary.addValue("Count", roiPixelCount);
					texture_Summary.addLabel("GLCM_90\u00B0");

					if(texturemenu.glcm_ASM.getState()==true)
						texture_Summary.addValue("ASM", GLCM_ASM_90);
					if(texturemenu.glcm_CON.getState()==true)
						texture_Summary.addValue("CON", GLCM_CON_90);
					if(texturemenu.glcm_ENT.getState()==true)
						texture_Summary.addValue("ENT", GLCM_ENT_90);
					if(texturemenu.glcm_MEAN.getState()==true)
						texture_Summary.addValue("MEAN", GLCM_MEAN_90);
					if(texturemenu.glcm_VAR.getState()==true)
						texture_Summary.addValue("VAR", GLCM_VAR_90);
					if(texturemenu.glcm_ACO.getState()==true)
						texture_Summary.addValue("ACO", GLCM_ACO_90);
					if(texturemenu.glcm_CLP.getState()==true)
						texture_Summary.addValue("CLP", GLCM_CLP_90);
					if(texturemenu.glcm_CLS.getState()==true)
						texture_Summary.addValue("CLS", GLCM_CLS_90);
					if(texturemenu.glcm_CLT.getState()==true)
						texture_Summary.addValue("CLT", GLCM_CLT_90);
					if(texturemenu.glcm_DIS.getState()==true)
						texture_Summary.addValue("DIS", GLCM_DIS_90);
					if(texturemenu.glcm_HOM.getState()==true)
						texture_Summary.addValue("HOM", GLCM_HOM_90);
					if(texturemenu.glcm_IDM.getState()==true)
						texture_Summary.addValue("IDM", GLCM_IDM_90);
					if(texturemenu.glcm_IDMN.getState()==true)
						texture_Summary.addValue("IDMN", GLCM_IDMN_90);
					if(texturemenu.glcm_IDN.getState()==true)
						texture_Summary.addValue("IDN", GLCM_IDN_90);
					if(texturemenu.glcm_CORR.getState()==true)
						texture_Summary.addValue("CORR", GLCM_CORR_90);
					if(texturemenu.glcm_IVAR.getState()==true)
						texture_Summary.addValue("IVAR", GLCM_IVAR_90);
					if(texturemenu.glcm_MAXP.getState()==true)
						texture_Summary.addValue("MAXP", GLCM_MAXP_90);
					if(texturemenu.glcm_SUMA.getState()==true)
						texture_Summary.addValue("SUMA", GLCM_SUMA_90);
					if(texturemenu.glcm_SUMV.getState()==true)
						texture_Summary.addValue("SUMV", GLCM_SUMV_90);
					if(texturemenu.glcm_SUME.getState()==true)
						texture_Summary.addValue("SUME", GLCM_SUME_90);
					if(texturemenu.glcm_DIFV.getState()==true)
						texture_Summary.addValue("DIFV", GLCM_DIFV_90);
					if(texturemenu.glcm_DIFE.getState()==true)
						texture_Summary.addValue("DIFE", GLCM_DIFE_90);
					if(texturemenu.glcm_IMC1.getState()==true)
						texture_Summary.addValue("IMC1", GLCM_IMC1_90);
					if(texturemenu.glcm_IMC2.getState()==true)
						texture_Summary.addValue("IMC2", GLCM_IMC2_90);
				}
				if(texturemenu.direction_135.getState()==true){
					GLCM_ASM_135 = GLCM_ASM_135/roiPixelCount; GLCM_CON_135 = GLCM_CON_135/roiPixelCount; GLCM_ENT_135 = GLCM_ENT_135/roiPixelCount; GLCM_MEAN_135 = GLCM_MEAN_135/roiPixelCount; GLCM_VAR_135 = GLCM_VAR_135/roiPixelCount;
					GLCM_ACO_135= GLCM_ACO_135/roiPixelCount; GLCM_CLP_135 = GLCM_CLP_135/roiPixelCount; GLCM_CLS_135 = GLCM_CLS_135/roiPixelCount; GLCM_CLT_135 = GLCM_CLT_135/roiPixelCount; GLCM_DIS_135 = GLCM_DIS_135/roiPixelCount;
					GLCM_HOM_135 = GLCM_HOM_135/roiPixelCount; GLCM_IDM_135 = GLCM_IDM_135/roiPixelCount; GLCM_IDMN_135 = GLCM_IDMN_135/roiPixelCount; GLCM_IDN_135 = GLCM_IDN_135/roiPixelCount;
					GLCM_CORR_135 = GLCM_CORR_135/roiPixelCount; GLCM_IVAR_135 = GLCM_IVAR_135/roiPixelCount; GLCM_MAXP_135 = GLCM_MAXP_135/roiPixelCount;						
					GLCM_SUMA_135 = GLCM_SUMA_135/roiPixelCount; GLCM_SUMV_135 = GLCM_SUMV_135/roiPixelCount; GLCM_SUME_135 = GLCM_SUME_135/roiPixelCount;
					GLCM_DIFA_135 = GLCM_DIFA_135/roiPixelCount; GLCM_DIFV_135 = GLCM_DIFV_135/roiPixelCount; GLCM_DIFE_135 = GLCM_DIFE_135/roiPixelCount;						
					GLCM_IMC1_135 = GLCM_IMC1_135/roiPixelCount; GLCM_IMC2_135 = GLCM_IMC2_135/roiPixelCount;

					texture_Summary.incrementCounter();
					texture_Summary.addValue("Size", windowSize+" X "+windowSize);
					texture_Summary.addValue("Bins", ""+histogramBins);
					texture_Summary.addValue("Min/Max", histoMinValue+" / "+histoMaxValue);
					texture_Summary.addValue("Diretion", "135\u00B0");
					texture_Summary.addValue("Count", roiPixelCount);
					texture_Summary.addLabel("GLCM_135\u00B0");

					if(texturemenu.glcm_ASM.getState()==true)
						texture_Summary.addValue("ASM", GLCM_ASM_135);
					if(texturemenu.glcm_CON.getState()==true)
						texture_Summary.addValue("CON", GLCM_CON_135);
					if(texturemenu.glcm_ENT.getState()==true)
						texture_Summary.addValue("ENT", GLCM_ENT_135);
					if(texturemenu.glcm_MEAN.getState()==true)
						texture_Summary.addValue("MEAN", GLCM_MEAN_135);
					if(texturemenu.glcm_VAR.getState()==true)
						texture_Summary.addValue("VAR", GLCM_VAR_135);
					if(texturemenu.glcm_ACO.getState()==true)
						texture_Summary.addValue("ACO", GLCM_ACO_135);
					if(texturemenu.glcm_CLP.getState()==true)
						texture_Summary.addValue("CLP", GLCM_CLP_135);
					if(texturemenu.glcm_CLS.getState()==true)
						texture_Summary.addValue("CLS", GLCM_CLS_135);
					if(texturemenu.glcm_CLT.getState()==true)
						texture_Summary.addValue("CLT", GLCM_CLT_135);
					if(texturemenu.glcm_DIS.getState()==true)
						texture_Summary.addValue("DIS", GLCM_DIS_135);
					if(texturemenu.glcm_HOM.getState()==true)
						texture_Summary.addValue("HOM", GLCM_HOM_135);
					if(texturemenu.glcm_IDM.getState()==true)
						texture_Summary.addValue("IDM", GLCM_IDM_135);
					if(texturemenu.glcm_IDMN.getState()==true)
						texture_Summary.addValue("IDMN", GLCM_IDMN_135);
					if(texturemenu.glcm_IDN.getState()==true)
						texture_Summary.addValue("IDN", GLCM_IDN_135);
					if(texturemenu.glcm_CORR.getState()==true)
						texture_Summary.addValue("CORR", GLCM_CORR_135);
					if(texturemenu.glcm_IVAR.getState()==true)
						texture_Summary.addValue("IVAR", GLCM_IVAR_135);
					if(texturemenu.glcm_MAXP.getState()==true)
						texture_Summary.addValue("MAXP", GLCM_MAXP_135);
					if(texturemenu.glcm_SUMA.getState()==true)
						texture_Summary.addValue("SUMA", GLCM_SUMA_135);
					if(texturemenu.glcm_SUMV.getState()==true)
						texture_Summary.addValue("SUMV", GLCM_SUMV_135);
					if(texturemenu.glcm_SUME.getState()==true)
						texture_Summary.addValue("SUME", GLCM_SUME_135);
					if(texturemenu.glcm_DIFV.getState()==true)
						texture_Summary.addValue("DIFV", GLCM_DIFV_135);
					if(texturemenu.glcm_DIFE.getState()==true)
						texture_Summary.addValue("DIFE", GLCM_DIFE_135);
					if(texturemenu.glcm_IMC1.getState()==true)
						texture_Summary.addValue("IMC1", GLCM_IMC1_135);
					if(texturemenu.glcm_IMC2.getState()==true)
						texture_Summary.addValue("IMC2", GLCM_IMC2_135);
				}
				if(texturemenu.direction_average.getState()==true){
					GLCM_ASM_ave = GLCM_ASM_ave/(roiPixelCount*4); GLCM_CON_ave = GLCM_CON_ave/(roiPixelCount*4); GLCM_ENT_ave = GLCM_ENT_ave/(roiPixelCount*4); 
					GLCM_MEAN_ave = GLCM_MEAN_ave/(roiPixelCount*4); GLCM_VAR_ave = GLCM_VAR_ave/(roiPixelCount*4);	GLCM_ACO_ave= GLCM_ACO_ave/(roiPixelCount*4);
					GLCM_CLP_ave = GLCM_CLP_ave/(roiPixelCount*4); GLCM_CLS_ave = GLCM_CLS_ave/(roiPixelCount*4); GLCM_CLT_ave = GLCM_CLT_ave/(roiPixelCount*4); 
					GLCM_DIS_ave = GLCM_DIS_ave/(roiPixelCount*4); GLCM_HOM_ave = GLCM_HOM_ave/(roiPixelCount*4); GLCM_IDM_ave = GLCM_IDM_ave/(roiPixelCount*4); 
					GLCM_IDMN_ave = GLCM_IDMN_ave/(roiPixelCount*4); GLCM_IDN_ave = GLCM_IDN_ave/(roiPixelCount*4); GLCM_CORR_ave = GLCM_CORR_ave/(roiPixelCount*4); 
					GLCM_IVAR_ave = GLCM_IVAR_ave/(roiPixelCount*4); GLCM_MAXP_ave = GLCM_MAXP_ave/(roiPixelCount*4); GLCM_SUMA_ave = GLCM_SUMA_ave/(roiPixelCount*4); 
					GLCM_SUMV_ave = GLCM_SUMV_ave/(roiPixelCount*4); GLCM_SUME_ave = GLCM_SUME_ave/(roiPixelCount*4); GLCM_DIFA_ave = GLCM_DIFA_ave/(roiPixelCount*4); 
					GLCM_DIFV_ave = GLCM_DIFV_ave/(roiPixelCount*4); GLCM_DIFE_ave = GLCM_DIFE_ave/(roiPixelCount*4); GLCM_IMC1_ave = GLCM_IMC1_ave/(roiPixelCount*4); 
					GLCM_IMC2_ave = GLCM_IMC2_ave/(roiPixelCount*4);

					texture_Summary.incrementCounter();
					texture_Summary.addValue("Size", windowSize+" X "+windowSize);
					texture_Summary.addValue("Bins", ""+histogramBins);
					texture_Summary.addValue("Min/Max", histoMinValue+" / "+histoMaxValue);
					texture_Summary.addValue("Diretion", "Average");
					texture_Summary.addValue("Count", roiPixelCount);
					texture_Summary.addLabel("GLCM_ave");

					if(texturemenu.glcm_ASM.getState()==true)
						texture_Summary.addValue("ASM", GLCM_ASM_ave);
					if(texturemenu.glcm_CON.getState()==true)
						texture_Summary.addValue("CON", GLCM_CON_ave);
					if(texturemenu.glcm_ENT.getState()==true)
						texture_Summary.addValue("ENT", GLCM_ENT_ave);
					if(texturemenu.glcm_MEAN.getState()==true)
						texture_Summary.addValue("MEAN", GLCM_MEAN_ave);
					if(texturemenu.glcm_VAR.getState()==true)
						texture_Summary.addValue("VAR", GLCM_VAR_ave);
					if(texturemenu.glcm_ACO.getState()==true)
						texture_Summary.addValue("ACO", GLCM_ACO_ave);
					if(texturemenu.glcm_CLP.getState()==true)
						texture_Summary.addValue("CLP", GLCM_CLP_ave);
					if(texturemenu.glcm_CLS.getState()==true)
						texture_Summary.addValue("CLS", GLCM_CLS_ave);
					if(texturemenu.glcm_CLT.getState()==true)
						texture_Summary.addValue("CLT", GLCM_CLT_ave);
					if(texturemenu.glcm_DIS.getState()==true)
						texture_Summary.addValue("DIS", GLCM_DIS_ave);
					if(texturemenu.glcm_HOM.getState()==true)
						texture_Summary.addValue("HOM", GLCM_HOM_ave);
					if(texturemenu.glcm_IDM.getState()==true)
						texture_Summary.addValue("IDM", GLCM_IDM_ave);
					if(texturemenu.glcm_IDMN.getState()==true)
						texture_Summary.addValue("IDMN", GLCM_IDMN_ave);
					if(texturemenu.glcm_IDN.getState()==true)
						texture_Summary.addValue("IDN", GLCM_IDN_ave);
					if(texturemenu.glcm_CORR.getState()==true)
						texture_Summary.addValue("CORR", GLCM_CORR_ave);
					if(texturemenu.glcm_IVAR.getState()==true)
						texture_Summary.addValue("IVAR", GLCM_IVAR_ave);
					if(texturemenu.glcm_MAXP.getState()==true)
						texture_Summary.addValue("MAXP", GLCM_MAXP_ave);
					if(texturemenu.glcm_SUMA.getState()==true)
						texture_Summary.addValue("SUMA", GLCM_SUMA_ave);
					if(texturemenu.glcm_SUMV.getState()==true)
						texture_Summary.addValue("SUMV", GLCM_SUMV_ave);
					if(texturemenu.glcm_SUME.getState()==true)
						texture_Summary.addValue("SUME", GLCM_SUME_ave);
					if(texturemenu.glcm_DIFV.getState()==true)
						texture_Summary.addValue("DIFV", GLCM_DIFV_ave);
					if(texturemenu.glcm_DIFE.getState()==true)
						texture_Summary.addValue("DIFE", GLCM_DIFE_ave);
					if(texturemenu.glcm_IMC1.getState()==true)
						texture_Summary.addValue("IMC1", GLCM_IMC1_ave);
					if(texturemenu.glcm_IMC2.getState()==true)
						texture_Summary.addValue("IMC2", GLCM_IMC2_ave);
				}

				texture_Summary.show("Texture Analysis");
			}
			
			if(texturemenu.create_map.getState()==true){
				
				if(texturemenu.direction_0.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("ASM_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_ASM_0_Array);
						direction_0_map.show();
					}
					if(texturemenu.glcm_CON.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("CON_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_CON_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_ENT.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("ENT_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_ENT_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("MEAN_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_MEAN_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_VAR.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("VAR_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_VAR_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_ACO.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("ACO_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_ACO_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_CLP.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("CLP_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_CLP_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_CLS.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("CLS_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_CLS_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_CLT.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("CLT_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_CLT_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_DIS.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("DIS_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_DIS_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_HOM.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("HOM_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_HOM_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_IDM.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("IDM_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_IDM_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("IDMN_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_IDMN_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_IDN.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("IDN_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_IDN_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_CORR.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("CORR_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_CORR_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("IVAR_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_IVAR_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("MAXP_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_MAXP_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("SUMA_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_SUMA_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("SUMV_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_SUMV_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_SUME.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("SUME_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_SUME_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("DIFV_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_DIFV_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("DIFE_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_DIFE_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("IMC1_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_IMC1_0_Array);
						direction_0_map.show(); 
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						ImagePlus direction_0_map = NewImage.createImage("IMC2_0", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_0_map.getProcessor().setPixels(GLCM_IMC2_0_Array);
						direction_0_map.show(); 
					}
				}
				
				if(texturemenu.direction_45.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("ASM_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_ASM_45_Array);
						direction_45_map.show();
					}
					if(texturemenu.glcm_CON.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("CON_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_CON_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_ENT.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("ENT_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_ENT_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("MEAN_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_MEAN_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_VAR.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("VAR_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_VAR_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_ACO.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("ACO_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_ACO_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_CLP.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("CLP_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_CLP_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_CLS.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("CLS_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_CLS_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_CLT.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("CLT_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_CLT_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_DIS.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("DIS_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_DIS_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_HOM.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("HOM_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_HOM_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_IDM.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("IDM_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_IDM_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("IDMN_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_IDMN_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_IDN.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("IDN_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_IDN_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_CORR.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("CORR_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_CORR_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("IVAR_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_IVAR_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("MAXP_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_MAXP_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("SUMA_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_SUMA_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("SUMV_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_SUMV_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_SUME.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("SUME_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_SUME_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("DIFV_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_DIFV_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("DIFE_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_DIFE_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("IMC1_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_IMC1_45_Array);
						direction_45_map.show(); 
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						ImagePlus direction_45_map = NewImage.createImage("IMC2_45", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_45_map.getProcessor().setPixels(GLCM_IMC2_45_Array);
						direction_45_map.show(); 
					}
				}
				
				if(texturemenu.direction_90.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("ASM_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_ASM_90_Array);
						direction_90_map.show();
					}
					if(texturemenu.glcm_CON.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("CON_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_CON_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_ENT.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("ENT_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_ENT_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("MEAN_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_MEAN_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_VAR.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("VAR_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_VAR_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_ACO.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("ACO_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_ACO_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_CLP.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("CLP_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_CLP_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_CLS.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("CLS_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_CLS_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_CLT.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("CLT_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_CLT_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_DIS.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("DIS_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_DIS_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_HOM.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("HOM_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_HOM_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_IDM.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("IDM_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_IDM_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("IDMN_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_IDMN_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_IDN.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("IDN_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_IDN_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_CORR.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("CORR_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_CORR_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("IVAR_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_IVAR_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("MAXP_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_MAXP_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("SUMA_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_SUMA_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("SUMV_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_SUMV_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_SUME.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("SUME_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_SUME_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("DIFV_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_DIFV_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("DIFE_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_DIFE_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("IMC1_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_IMC1_90_Array);
						direction_90_map.show(); 
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						ImagePlus direction_90_map = NewImage.createImage("IMC2_90", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_90_map.getProcessor().setPixels(GLCM_IMC2_90_Array);
						direction_90_map.show(); 
					}
				}
				
				if(texturemenu.direction_135.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("ASM_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_ASM_135_Array);
						direction_135_map.show();
					}
					if(texturemenu.glcm_CON.getState()==true){
						ImagePlus direction_135_map= NewImage.createImage("CON_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_CON_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_ENT.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("ENT_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_ENT_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("MEAN_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_MEAN_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_VAR.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("VAR_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_VAR_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_ACO.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("ACO_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_ACO_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_CLP.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("CLP_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_CLP_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_CLS.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("CLS_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_CLS_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_CLT.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("CLT_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_CLT_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_DIS.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("DIS_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_DIS_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_HOM.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("HOM_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_HOM_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_IDM.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("IDM_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_IDM_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("IDMN_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_IDMN_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_IDN.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("IDN_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_IDN_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_CORR.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("CORR_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_CORR_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("IVAR_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_IVAR_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("MAXP_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_MAXP_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("SUMA_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_SUMA_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("SUMV_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_SUMV_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_SUME.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("SUME_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_SUME_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("DIFV_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_DIFV_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("DIFE_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_DIFE_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("IMC1_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_IMC1_135_Array);
						direction_135_map.show(); 
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						ImagePlus direction_135_map = NewImage.createImage("IMC2_135", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_135_map.getProcessor().setPixels(GLCM_IMC2_135_Array);
						direction_135_map.show(); 
					}
				}
				
				if(texturemenu.direction_average.getState()==true){
					if(texturemenu.glcm_ASM.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("ASM_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_ASM_ave_Array);
						direction_ave_map.show();
					}
					if(texturemenu.glcm_CON.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("CON_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_CON_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_ENT.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("ENT_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_ENT_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_MEAN.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("MEAN_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_MEAN_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_VAR.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("VAR_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_VAR_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_ACO.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("ACO_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_ACO_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_CLP.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("CLP_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_CLP_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_CLS.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("CLS_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_CLS_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_CLT.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("CLT_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_CLT_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_DIS.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("DIS_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_DIS_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_HOM.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("HOM_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_HOM_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_IDM.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("IDM_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_IDM_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_IDMN.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("IDMN_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_IDMN_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_IDN.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("IDN_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_IDN_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_CORR.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("CORR_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_CORR_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_IVAR.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("IVAR_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_IVAR_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_MAXP.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("MAXP_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_MAXP_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_SUMA.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("SUMA_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_SUMA_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_SUMV.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("SUMV_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_SUMV_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_SUME.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("SUME_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_SUME_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_DIFV.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("DIFV_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_DIFV_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_DIFE.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("DIFE_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_DIFE_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_IMC1.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("IMC1_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_IMC1_ave_Array);
						direction_ave_map.show(); 
					}
					if(texturemenu.glcm_IMC2.getState()==true){
						ImagePlus direction_ave_map = NewImage.createImage("IMC2_ave", _W, _H, 1, 32, NewImage.FILL_BLACK);
						direction_ave_map.getProcessor().setPixels(GLCM_IMC2_ave_Array);
						direction_ave_map.show(); 
					}
				}
			}
		}

				
		IJ.showStatus("Texture Analysis Complete");
			
//		}
	}


	float[] arraySum(float[] a,float[] b){
		float[] tmparr = new float[_W*_H];
		for(int i=0;i<a.length;i++){
			tmparr[i]=(a[i]+b[i]); //2로 나누는 이유는 dynamic이 1개라서 ㅍ 
		}

		return tmparr;
	}

	float[] arraySumForRoi(float[] a,float[] b,Roi roi){
		float[] tmparr = new float[_W*_H];
		for(int i=0;i<a.length;i++){
			if (roi!=null && !roi.isArea()) roi = null;
			ImageProcessor ip = imp.getProcessor();
			ImageProcessor mask = roi!=null?roi.getMask():null;
			Rectangle r = roi!=null?roi.getBounds():new Rectangle(0,0,ip.getWidth(),ip.getHeight());

			int x=(int)i%_W;
			int y=(int)i/_W;
			if(roi!=null){
				if (x>=r.x && y>=r.y && x<=r.x+r.width && y<=r.y+r.height&& (mask==null||mask.getPixel(x-r.x,y-r.y)!=0) ) {
					tmparr[i]=a[i]+b[i];
				}else{
					tmparr[i]=(float) 0.0;
				}
			}else{
				tmparr[i]=(float) 0.0;
			}

		}

		return tmparr;
	}

	float[] arrayDevide(float[] a,float[] b){
		float[] tmparr = new float[_W*_H];
		for(int i=0;i<a.length;i++){
			if (b[i]==0){
				tmparr[i]=0;
			}else{
				tmparr[i]=a[i]/b[i];
			}

		}
		return tmparr;
	}


	float[] arraySubtract(float[] a,float[] b){
		float[] tmparr = new float[_W*_H];
		for(int i=0;i<a.length;i++){
			tmparr[i]=a[i]-b[i];

		}
		return tmparr;
	}


	public void setPropertyInfo(ImagePlus Parent,ImagePlus Child){

		Calibration oriCal = Parent.getCalibration(); 
		Calibration newCal = Child.getCalibration(); 
		newCal.pixelWidth=oriCal.pixelWidth; 
		newCal.pixelHeight=oriCal.pixelHeight; 
		newCal.pixelDepth=oriCal.pixelDepth; 
		newCal.setXUnit(oriCal.getXUnit()); 
		newCal.setYUnit(oriCal.getYUnit()); 
		newCal.setZUnit(oriCal.getZUnit()); 
		newCal.frameInterval=oriCal.frameInterval;
		newCal.setTimeUnit(oriCal.getTimeUnit());
	}

	public String filePath(){
		URL url = getClass().getResource("/ij/IJ.class");
		String cPath = url == null ? null : url.toString().replaceAll("%20", " ");
		int firstIdx = cPath.indexOf("file:/");
		int lastIdx = cPath.indexOf("ij.jar!");
		String lut_path;
		if (lastIdx == -1 || lastIdx >= cPath.length()) {
			lastIdx = cPath.indexOf("/bin/ij/IJ.class");
			lut_path = ""+cPath.substring(firstIdx+6,lastIdx)+File.separator+"16_Colors.lut";
		} else {
			lut_path = ""+cPath.substring(firstIdx+6,lastIdx-1)+File.separator+"luts"+File.separator+"16_Colors.lut";
		}
		if(System.getProperty("os.name").equals("Linux")){
			lut_path = "/"+lut_path;
		}
		return lut_path;
	}
}
