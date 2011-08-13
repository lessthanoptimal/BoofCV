/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.intensity;

import gecv.abst.detect.intensity.*;
import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.alg.misc.PixelMath;
import gecv.alg.transform.gss.FactoryGaussianScaleSpace;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.ListDisplayPanel;
import gecv.gui.SelectAlgorithmPanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.UtilImageIO;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays the intensity of detected features across scalespace
 *
 * @author Peter Abeles
 */
public class IntensityFeatureScaleSpaceApp extends SelectAlgorithmPanel {

//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	ListDisplayPanel gui = new ListDisplayPanel();

	GaussianScaleSpace<ImageFloat32,ImageFloat32> ss;

	BufferedImage input;
	ImageFloat32 inputF32;

	public IntensityFeatureScaleSpaceApp() {
		addAlgorithm("Hessian Det", new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.DETERMINANT,ImageFloat32.class));
		addAlgorithm("Laplacian", new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.TRACE,ImageFloat32.class));
		addAlgorithm("Harris",new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(FactoryCornerIntensity.createHarris(ImageFloat32.class,2,0.4f)));
		addAlgorithm("KLT",new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>( FactoryCornerIntensity.createKlt(ImageFloat32.class,2)));
		addAlgorithm("FAST 12",new WrapperFastCornerIntensity<ImageFloat32,ImageFloat32>(FactoryCornerIntensity.createFast12(ImageFloat32.class,5,11)));
		addAlgorithm("KitRos",new WrapperKitRosCornerIntensity<ImageFloat32,ImageFloat32>(ImageFloat32.class));
		addAlgorithm("Median",new WrapperMedianCornerIntensity<ImageFloat32,ImageFloat32>(FactoryBlurFilter.median(ImageFloat32.class,2),ImageFloat32.class));

		add(gui, BorderLayout.CENTER);

		ss = FactoryGaussianScaleSpace.nocache_F32();

		double scales[] = new double[31];
		for( int i = 0; i < scales.length ; i++ ) {
			scales[i] =  Math.exp(i*0.15);
		}
		ss.setScales(scales);
	}

	@Override
	public synchronized void setActiveAlgorithm(String name, Object cookie ) {
		if( input == null ) {
			return;
		}
		GeneralFeatureIntensity<ImageFloat32,ImageFloat32> intensity =
				(GeneralFeatureIntensity<ImageFloat32,ImageFloat32>)cookie;

		gui.reset();
		gui.addImage(input,"Original Image");

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Computing Scale Space Response",
				"", 0, ss.getTotalScales());


		for( int i = 0; i < ss.getTotalScales() && !progressMonitor.isCanceled(); i++ ) {
			ss.setActiveScale(i);
			double scale = ss.getCurrentScale();
			ImageFloat32 scaledImage = ss.getScaledImage();

			ImageFloat32 derivX = ss.getDerivative(true);
			ImageFloat32 derivY = ss.getDerivative(false);
			ImageFloat32 derivXX = ss.getDerivative(true,true);
			ImageFloat32 derivYY = ss.getDerivative(false,false);
			ImageFloat32 derivXY = ss.getDerivative(true,false);

			intensity.process(scaledImage,derivX,derivY,derivXX,derivYY,derivXY);

			ImageFloat32 featureImg = intensity.getIntensity();
			BufferedImage b = VisualizeImageData.colorizeSign(featureImg,null, PixelMath.maxAbs(featureImg));
			gui.addImage(b,String.format("Scale %6.2f",scale));

			final int progressStatus = i+1;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressMonitor.setProgress(progressStatus);
				}
			});

		}
	}

	public synchronized void setImage( BufferedImage input ) {
		setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
		this.input = input;
		inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);
		ss.setImage(inputF32);
		refreshAlgorithm();
	}

	public static void main( String args[] ) {
		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32();

		double scales[] = new double[31];
		for( int i = 0; i < scales.length ; i++ ) {
			scales[i] =  Math.exp(i*0.15);
		}
		ss.setScales(scales);

		BufferedImage input = UtilImageIO.loadImage(fileName);

		IntensityFeatureScaleSpaceApp app = new IntensityFeatureScaleSpaceApp();
		app.setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));

		ShowImages.showWindow(app,"Feature Scale Space Intensity");
		app.setImage(input);
	}
}
