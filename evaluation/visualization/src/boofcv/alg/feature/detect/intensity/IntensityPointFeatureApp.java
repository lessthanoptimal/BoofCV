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
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ImageListManager;
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
		extends SelectAlgorithmImagePanel implements ProcessInput
{
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
	// if it has processed an image or not
	boolean processImage = false;

	public IntensityPointFeatureApp( Class<T> imageType , Class<D> derivType ) {
		super(1);
		this.imageType = imageType;

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(imageType);
		ImageGenerator<D> derivGen = FactoryImageGenerator.create(derivType);
		deriv = new AnyImageDerivative<T,D>(GradientThree.getKernelX(isInteger),imageType,derivGen);

		addAlgorithm(0, "Laplacian", new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.TRACE,derivType));
		addAlgorithm(0, "Hessian Det", new WrapperLaplacianBlobIntensity<T,D>(HessianBlobIntensity.Type.DETERMINANT,derivType));
		addAlgorithm(0, "Harris",new WrapperGradientCornerIntensity<T,D>(FactoryPointIntensityAlg.createHarris(derivType,2,0.4f)));
		addAlgorithm(0, "KLT",new WrapperGradientCornerIntensity<T,D>( FactoryPointIntensityAlg.createKlt(derivType,2)));
		addAlgorithm(0, "FAST 12",new WrapperFastCornerIntensity<T,D>(FactoryPointIntensityAlg.createFast12(imageType,5,11)));
		addAlgorithm(0, "KitRos",new WrapperKitRosCornerIntensity<T,D>(derivType));
		addAlgorithm(0, "Median",new WrapperMedianCornerIntensity<T,D>(FactoryBlurFilter.median(imageType,2),imageType));

		gui = new ImagePanel();
		setMainGUI(gui);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( workImage == null )
			return;

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
		gui.requestFocusInWindow();
	}

	public void process( final BufferedImage input ) {
		setInputImage(input);
		this.input = input;
		workImage = ConvertBufferedImage.convertFrom(input,null,imageType);
		temp = new BufferedImage(workImage.width,workImage.height,BufferedImage.TYPE_INT_BGR);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
				processImage = true;
			}});
		doRefreshAll();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processImage;
	}

	public static void main( String args[] ) {
		IntensityPointFeatureApp<ImageFloat32,ImageFloat32> app =
				new IntensityPointFeatureApp<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

//		IntensityPointFeatureApp<ImageUInt8, ImageSInt16> app =
//				new IntensityPointFeatureApp<ImageUInt8,ImageSInt16>(ImageUInt8.class,ImageSInt16.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Feature Intensity");
	}
}
