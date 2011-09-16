/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.filter.derivative.GradientThree;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.intensity.FactoryPointIntensityAlg;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.gui.ProcessImage;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays the intensity of detected features inside an image
 *
 * @author Peter Abeles
 */
public class IntensityPointFeatureApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmPanel implements ProcessImage
{

//	static String fileName = "data/outdoors01.jpg";
//	static String fileName = "data/sunflowers.png";
//	static String fileName = "data/particles01.jpg";
//	static String fileName = "data/scale/beach02.jpg";
//	static String fileName = "data/scale/mountain_7p1mm.jpg";
//	static String fileName = "data/indoors01.jpg";
	static String fileName = "data/shapes01.png";
//	static String fileName = "data/stitch/cave_01.jpg";

	// displays intensity image
	ImagePanel gui;

	// original input image
	BufferedImage input;
	// intensity image is rendered here
	BufferedImage temp;
	// converted input image
	T workImage;
	// type of image the input image is
	Class<T> imageType;
	// computes image derivative
	AnyImageDerivative<T,D> deriv;

	public IntensityPointFeatureApp( Class<T> imageType , Class<D> derivType ) {
		this.imageType = imageType;

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(imageType);
		ImageGenerator<D> derivGen = FactoryImageGenerator.create(derivType);
		deriv = new AnyImageDerivative<T,D>(GradientThree.getKernelX(isInteger),imageType,derivGen);

		addAlgorithm("Original",null);
		addAlgorithm("Hessian Det", new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType));
		addAlgorithm("Laplacian", new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.TRACE,derivType));
		addAlgorithm("Harris",new WrapperGradientCornerIntensity<T,D>(FactoryPointIntensityAlg.createHarris(derivType,2,0.4f)));
		addAlgorithm("KLT",new WrapperGradientCornerIntensity<T,D>( FactoryPointIntensityAlg.createKlt(derivType,2)));
		addAlgorithm("FAST 12",new WrapperFastCornerIntensity<T,D>(FactoryPointIntensityAlg.createFast12(imageType,5,11)));
		addAlgorithm("KitRos",new WrapperKitRosCornerIntensity<T,D>(derivType));
		addAlgorithm("Median",new WrapperMedianCornerIntensity<T,D>(FactoryBlurFilter.median(imageType,2),imageType));

		gui = new ImagePanel();
		add(gui, BorderLayout.CENTER);
	}

	@Override
	public synchronized void setActiveAlgorithm(String name, Object cookie ) {
		if( workImage == null )
			return;

		if( cookie == null ) {
			gui.setBufferedImage(input);
			gui.repaint();
			return;
		}
		
		GeneralFeatureIntensity<T,D> intensity = (GeneralFeatureIntensity<T,D>)cookie;

		deriv.setInput(workImage);

		D derivX = deriv.getDerivative(true);
		D derivY = deriv.getDerivative(false);
		D derivXX = deriv.getDerivative(true,true);
		D derivYY = deriv.getDerivative(false,false);
		D derivXY = deriv.getDerivative(true,false);

		intensity.process(workImage,derivX,derivY,derivXX,derivYY,derivXY);

		ImageFloat32 featureImg = intensity.getIntensity();
		VisualizeImageData.colorizeSign(featureImg,temp, PixelMath.maxAbs(featureImg));
		gui.setBufferedImage(temp);
		gui.repaint();
	}

	@Override
	public synchronized void process( final BufferedImage input ) {
		this.input = input;
		workImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		temp = new BufferedImage(workImage.width,workImage.height,BufferedImage.TYPE_INT_BGR);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
			}});
		refreshAlgorithm();
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);

		IntensityPointFeatureApp<ImageFloat32,ImageFloat32> app =
				new IntensityPointFeatureApp<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
		app.process(input);

//		IntensityFeatureScaleSpaceApp<ImageUInt8, ImageSInt16> app2 =
//				new IntensityFeatureScaleSpaceApp<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);
//		app2.setImage(input);

		ShowImages.showWindow(app,"Feature Intensity");
	}
}
