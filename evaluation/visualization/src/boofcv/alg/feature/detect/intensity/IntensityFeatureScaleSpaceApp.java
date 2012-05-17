/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.feature.detect.intensity;

import boofcv.abst.feature.detect.intensity.*;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the intensity of detected features across scalespace
 *
 * @author Peter Abeles
 */
public class IntensityFeatureScaleSpaceApp<T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel
{

	ListDisplayPanel gui = new ListDisplayPanel();

	GaussianScaleSpace<T,D> ss;

	BufferedImage input;
	T workImage;
	Class<T> imageType;
	boolean processedImage = false;

	public IntensityFeatureScaleSpaceApp( Class<T> imageType , Class<D> derivType ) {
		super(1);
		this.imageType = imageType;

		addAlgorithm(0, "Hessian Det", new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType));
		addAlgorithm(0, "Laplacian", new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.TRACE,derivType));
		addAlgorithm(0, "Harris",new WrapperGradientCornerIntensity<T,D>(FactoryIntensityPointAlg.harris(2, 0.4f, false, derivType)));
		addAlgorithm(0, "Shi Tomasi",new WrapperGradientCornerIntensity<T,D>( FactoryIntensityPointAlg.shiTomasi(2, false, derivType)));
		addAlgorithm(0, "FAST 12",new WrapperFastCornerIntensity<T,D>(FactoryIntensityPointAlg.fast12(5, 11, imageType)));
		addAlgorithm(0, "KitRos",new WrapperKitRosCornerIntensity<T,D>(derivType));
		addAlgorithm(0, "Median",new WrapperMedianCornerIntensity<T,D>(FactoryBlurFilter.median(imageType,2),imageType));

		setMainGUI(gui);

		ss = FactoryGaussianScaleSpace.nocache(imageType);

		double scales[] = new double[25];
		for( int i = 0; i < scales.length ; i++ ) {
			scales[i] =  Math.exp(i*0.15);
		}
		ss.setScales(scales);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( input == null ) {
			return;
		}
		GeneralFeatureIntensity<T,D> intensity = (GeneralFeatureIntensity<T,D>)cookie;

		gui.reset();
		BufferedImage b = VisualizeImageData.grayMagnitude(workImage,null, 255);
		gui.addImage(b,"Gray Image");

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Computing Scale Space Response",
				"", 0, ss.getTotalScales());

		for( int i = 0; i < ss.getTotalScales() && !progressMonitor.isCanceled(); i++ ) {
			ss.setActiveScale(i);
			double scale = ss.getCurrentScale();
			T scaledImage = ss.getScaledImage();

			D derivX = ss.getDerivative(true);
			D derivY = ss.getDerivative(false);
			D derivXX = ss.getDerivative(true,true);
			D derivYY = ss.getDerivative(false,false);
			D derivXY = ss.getDerivative(true,false);

			intensity.process(scaledImage,derivX,derivY,derivXX,derivYY,derivXY);
			ImageFloat32 featureImg = intensity.getIntensity();

			b = VisualizeImageData.colorizeSign(featureImg,null, PixelMath.maxAbs(featureImg));
			gui.addImage(b,String.format("Scale %6.2f",scale));

			final int progressStatus = i+1;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressMonitor.setProgress(progressStatus);
				}
			});
		}
		gui.requestFocusInWindow();
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		this.input = input;
		workImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);
		ss.setImage(workImage);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(input.getWidth()+50,input.getHeight()));
				processedImage = true;
			}
		});
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath() );
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String args[] ) {

		IntensityFeatureScaleSpaceApp<ImageFloat32,ImageFloat32> app =
				new IntensityFeatureScaleSpaceApp<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
//		IntensityFeatureScaleSpaceApp app =
//				new IntensityFeatureScaleSpaceApp(ImageUInt8.class, ImageSInt16.class);

		String prefix = "../data/evaluation/";

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("shapes",prefix+"shapes01.png"));
		inputs.add(new PathLabel("sunflowers",prefix+"sunflowers.png"));
		inputs.add(new PathLabel("beach",prefix+"scale/beach02.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Feature Scale Space Intensity");

	}
}
