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

package gecv.alg.detect.interest;

import gecv.abst.detect.corner.GeneralFeatureIntensity;
import gecv.abst.detect.corner.WrapperLaplacianBlobIntensity;
import gecv.alg.detect.corner.HessianBlobIntensity;
import gecv.alg.misc.PixelMath;
import gecv.alg.transform.gss.FactoryGaussianScaleSpace;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.image.ListDisplayPanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.UtilImageIO;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays the intensity of detected features across scalespace
 *
 * @author Peter Abeles
 */
public class FeatureIntensityScaleSpaceApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
//	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
	static String fileName = "evaluation/data/shapes01.png";

	public static void main( String args[] ) {
		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32();

		double scales[] = new double[31];
		for( int i = 0; i < scales.length ; i++ ) {
			scales[i] =  Math.exp(i*0.15);
		}
		ss.setScales(scales);


		GeneralFeatureIntensity<ImageFloat32, ImageFloat32> intensity;
//		intensity = new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.DETERMINANT,ImageFloat32.class);
		intensity = new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.TRACE,ImageFloat32.class);
//		intensity = new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
//				FactoryCornerIntensity.createHarris(ImageFloat32.class,2,0.4f));
//		intensity = new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
//				FactoryCornerIntensity.createKlt(ImageFloat32.class,2));
//		intensity = new WrapperFastCornerIntensity<ImageFloat32,ImageFloat32>(FactoryCornerIntensity.createFast12(ImageFloat32.class,5,11));
//		intensity = new WrapperKitRosCornerIntensity<ImageFloat32,ImageFloat32>(FactoryCornerIntensity.createKitRos(ImageFloat32.class));
//		intensity = new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(FactoryCornerIntensity.createHarris(ImageFloat32.class,2,0.04f));
//		intensity = new WrapperMedianCornerIntensity<ImageFloat32,ImageFloat32>(FactoryCornerIntensity.createMedian(ImageFloat32.class),
//				FactoryBlurFilter.median(ImageFloat32.class,2));

		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		ss.setImage(inputF32);

		ListDisplayPanel gui = new ListDisplayPanel();
		ListDisplayPanel guiIntensity = new ListDisplayPanel();

		gui.addImage(input,"Original Image");

		for( int i = 0; i < ss.getTotalScales(); i++ ) {
			ss.setActiveScale(i);
			double scale = ss.getCurrentScale();
			ImageFloat32 scaledImage = ss.getScaledImage();
			BufferedImage b = ConvertBufferedImage.convertTo(scaledImage,null);
			gui.addImage(b,String.format("Scale %6.2f",scale));

			ImageFloat32 derivX = ss.getDerivative(true);
			ImageFloat32 derivY = ss.getDerivative(false);
			ImageFloat32 derivXX = ss.getDerivative(true,true);
			ImageFloat32 derivYY = ss.getDerivative(false,false);
			ImageFloat32 derivXY = ss.getDerivative(true,false);

			intensity.process(scaledImage,derivX,derivY,derivXX,derivYY,derivXY);

			ImageFloat32 featureImg = intensity.getIntensity();

			b = VisualizeImageData.grayMagnitude(featureImg,null, PixelMath.maxAbs(featureImg));
			guiIntensity.addImage(b,String.format("Scale %6.2f",scale));
		}

		ShowImages.showWindow(gui,"Scale Space");
		ShowImages.showWindow(guiIntensity,"Feature Intensity");
	}
}
