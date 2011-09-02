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

package boofcv.alg.feature.detect.intensity;

import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.filter.blur.FactoryBlurFilter;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.alg.transform.gss.UtilScaleSpace;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.intensity.FactoryPointIntensityAlg;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays the intensity in each layer of a {@link ScaleSpacePyramid}.
 *
 * @author Peter Abeles
 */
public class IntensityFeatureScaleSpacePyramidApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmPanel {

//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/scale/mountain_7p1mm.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	ListDisplayPanel gui = new ListDisplayPanel();

	ScaleSpacePyramid<T> pyramid;

	BufferedImage input;
	T workImage;
	ImageFloat32 scaledIntensity;
	Class<T> imageType;
	AnyImageDerivative<T,D> anyDerivative;

	public IntensityFeatureScaleSpacePyramidApp( Class<T> imageType , Class<D> derivType ) {
		this.imageType = imageType;

		addAlgorithm("Hessian Det", new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType));
		addAlgorithm("Laplacian", new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.TRACE,derivType));
		addAlgorithm("Harris",new WrapperGradientCornerIntensity<T,D>(FactoryPointIntensityAlg.createHarris(derivType,2,0.4f)));
		addAlgorithm("KLT",new WrapperGradientCornerIntensity<T,D>( FactoryPointIntensityAlg.createKlt(derivType,2)));
		addAlgorithm("FAST 12",new WrapperFastCornerIntensity<T,D>(FactoryPointIntensityAlg.createFast12(imageType,5,11)));
		addAlgorithm("KitRos",new WrapperKitRosCornerIntensity<T,D>(derivType));
		addAlgorithm("Median",new WrapperMedianCornerIntensity<T,D>(FactoryBlurFilter.median(imageType,2),imageType));

		add(gui, BorderLayout.CENTER);

		double scales[] = new double[31];
		for( int i = 0; i < scales.length ; i++ ) {
			scales[i] =  Math.exp(i*0.15);
		}
		pyramid = new ScaleSpacePyramid<T>(imageType,scales);

		anyDerivative = UtilScaleSpace.createDerivatives(imageType, FactoryImageGenerator.create(derivType));
	}

	@Override
	public synchronized void setActiveAlgorithm(String name, Object cookie ) {
		if( input == null ) {
			return;
		}
		GeneralFeatureIntensity<T,D> intensity = (GeneralFeatureIntensity<T,D>)cookie;

		gui.reset();
		gui.addImage(input,"Original Image");
		BufferedImage b = VisualizeImageData.grayMagnitude(workImage,null, 255);
		gui.addImage(b,"Gray Image");

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Computing Scale Space Pyramid Response",
				"", 0, pyramid.getNumLayers());



		for( int i = 0; i < pyramid.getNumLayers() && !progressMonitor.isCanceled(); i++ ) {
			double scale = pyramid.getScale(i);
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
			b = VisualizeImageData.colorizeSign(scaledIntensity,null, PixelMath.maxAbs(scaledIntensity));
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
		workImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		scaledIntensity = new ImageFloat32(workImage.width,workImage.height);
		pyramid.update(workImage);
		setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
		refreshAlgorithm();

		ShowImages.showWindow(this,"Feature Scale Space Pyramid Intensity: "+imageType.getSimpleName());
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);

		IntensityFeatureScaleSpacePyramidApp<ImageFloat32,ImageFloat32> app =
				new IntensityFeatureScaleSpacePyramidApp<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
		app.setImage(input);

//		IntensityFeatureScaleSpacePyramidApp<ImageUInt8, ImageSInt16> app2 =
//				new IntensityFeatureScaleSpacePyramidApp<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);
//		app2.setImage(input);
	}
}
