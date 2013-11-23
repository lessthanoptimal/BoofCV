/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPointAlg;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidFloat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Displays the feature intensity for each layer in a scale-space.  The scale-space can be represented as a
 * pyramid of set of blurred images.
 *
 * @author Peter Abeles
 */
public class IntensityFeaturePyramidApp<T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel
{
	ListDisplayPanel gui = new ListDisplayPanel();

	PyramidFloat<T> pyramid;

	BufferedImage input;
	T workImage;
	ImageFloat32 scaledIntensity;
	Class<T> imageType;
	AnyImageDerivative<T,D> anyDerivative;
	boolean processedImage = false;

	GeneralFeatureIntensity<T,D> intensity;

	public IntensityFeaturePyramidApp(Class<T> imageType, Class<D> derivType ) {
		super(2);
		this.imageType = imageType;

		addAlgorithm(0, "Hessian Det", new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType));
		addAlgorithm(0, "Laplacian", new WrapperHessianBlobIntensity<T,D>(HessianBlobIntensity.Type.TRACE,derivType));
		addAlgorithm(0, "Harris",new WrapperGradientCornerIntensity<T,D>(FactoryIntensityPointAlg.harris(2, 0.4f, false, derivType)));
		addAlgorithm(0, "Shi Tomasi",new WrapperGradientCornerIntensity<T,D>( FactoryIntensityPointAlg.shiTomasi(2, false, derivType)));
		addAlgorithm(0, "FAST",new WrapperFastCornerIntensity<T,D>(FactoryIntensityPointAlg.fast(5, 11, imageType)));
		addAlgorithm(0, "KitRos",new WrapperKitRosCornerIntensity<T,D>(derivType));
		addAlgorithm(0, "Median",new WrapperMedianCornerIntensity<T,D>(FactoryBlurFilter.median(imageType,2),imageType));

		addAlgorithm(1 , "Pyramid", 0);
		addAlgorithm(1 , "Scale-Space", 1);

		setMainGUI(gui);

		anyDerivative = GImageDerivativeOps.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( input == null ) {
			return;
		}

		if( indexFamily == 0 ) {
			intensity = (GeneralFeatureIntensity<T,D>)cookie;
			if( pyramid == null )
				return;
		} else if( indexFamily == 1 ) {
			// setup the pyramid
			double scales[] = new double[25];
			for( int i = 0; i < scales.length ; i++ ) {
				scales[i] =  Math.exp(i*0.15);
			}

			if( ((Number)cookie).intValue() == 0 ) {
				pyramid = FactoryPyramid.scaleSpacePyramid(scales, imageType);
			} else {
				pyramid = FactoryPyramid.scaleSpace(scales, imageType);
			}
			if( workImage != null )
				pyramid.process(workImage);

			if( intensity == null )
				return;
		}

		// setup the feature intensity
		gui.reset();
		BufferedImage b = VisualizeImageData.grayMagnitude(workImage,null, 255);
		gui.addImage(b,"Gray Image");

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Computing Scale Space Pyramid Response",
				"", 0, pyramid.getNumLayers());

		for( int i = 0; i < pyramid.getNumLayers() && !progressMonitor.isCanceled(); i++ ) {
			double scale = pyramid.getSigma(i);
			T scaledImage = pyramid.getLayer(i);

			anyDerivative.setInput(scaledImage);
			D derivX = anyDerivative.getDerivative(true);
			D derivY = anyDerivative.getDerivative(false);
			D derivXX = anyDerivative.getDerivative(true,true);
			D derivYY = anyDerivative.getDerivative(false,false);
			D derivXY = anyDerivative.getDerivative(true,false);

			intensity.process(scaledImage,derivX,derivY,derivXX,derivYY,derivXY);

			ImageFloat32 featureImg = intensity.getIntensity();

			// scale it up to full resolution
			DistortImageOps.scale(featureImg,scaledIntensity, TypeInterpolate.NEAREST_NEIGHBOR);
			// visualize the rescaled intensity
			b = VisualizeImageData.colorizeSign(scaledIntensity,null, ImageStatistics.maxAbs(scaledIntensity));
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
		scaledIntensity = new ImageFloat32(workImage.width,workImage.height);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
				processedImage = true;
			}});
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		intensity = null;
		pyramid = null;

		setActiveAlgorithm(0,null,cookies[0]);
		setActiveAlgorithm(1,null,cookies[1]);
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	public static void main( String args[] ) {

		IntensityFeaturePyramidApp<ImageFloat32,ImageFloat32> app =
				new IntensityFeaturePyramidApp<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

//		IntensityFeaturePyramidApp<ImageUInt8, ImageSInt16> app =
//				new IntensityFeaturePyramidApp<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();

		inputs.add(new PathLabel("shapes","../data/evaluation/shapes01.png"));
		inputs.add(new PathLabel("amoeba","../data/evaluation/amoeba_shapes.jpg"));
		inputs.add(new PathLabel("sunflowers","../data/evaluation/sunflowers.png"));
		inputs.add(new PathLabel("beach","../data/evaluation/scale/beach02.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Feature Scale Space Pyramid Intensity");

	}
}
