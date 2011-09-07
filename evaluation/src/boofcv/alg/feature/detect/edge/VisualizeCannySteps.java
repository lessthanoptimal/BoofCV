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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.binary.BinaryImageHighOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.edge.VisualizeEdgeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSInt8;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * @author Peter Abeles
 */
// todo abstract image type.  Put in integer images
public class VisualizeCannySteps {

	//	static String fileName = "evaluation/data/outdoors01.jpg";
//	static String fileName = "evaluation/data/sunflowers.png";
	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	public static void main( String args[] ){

		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);
		ImageSInt32 labeled = new ImageSInt32(inputF32.width,inputF32.height);

		ImageFloat32 blurred = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 derivX = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 derivY = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 intensity = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 orientation = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 suppressed = new ImageFloat32(inputF32.width,inputF32.height);
		ImageSInt8 direction = new ImageSInt8(inputF32.width,inputF32.height);
		ImageUInt8 work = new ImageUInt8(inputF32.width,inputF32.height);

		BlurStorageFilter<ImageFloat32> blur = FactoryBlurFilter.gaussian(ImageFloat32.class,-1,2);
		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel_F32();

		blur.process(inputF32,blurred);
		gradient.process(blurred,derivX,derivY);

		float threshLow = 5;
		float threshHigh = 40;

		GradientToEdgeFeatures.intensityE(derivX,derivY,intensity);
		GradientToEdgeFeatures.direction(derivX,derivY,orientation);
		GradientToEdgeFeatures.discretizeDirection4(orientation,direction);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction,suppressed);
		int numFound = BinaryImageHighOps.hysteresisLabel8(suppressed,labeled,threshLow,threshHigh,false,work);

		Random rand = new Random(234);
		int colors[] = new int[ numFound+1 ];
		for( int i = 1; i < colors.length; i++ ) {
			colors[i] = rand.nextInt(0xFFFFFF);
		}

		BufferedImage renderedOrientation = VisualizeEdgeFeatures.renderOrientation4(direction,suppressed,threshLow,null);
		BufferedImage renderedLabel = VisualizeBinaryData.renderLabeled(labeled,null,colors);
		ShowImages.showWindow(intensity,"Raw Intensity",true);
		ShowImages.showWindow(suppressed,"Suppressed Intensity",true);
		ShowImages.showWindow(renderedOrientation,"Orientation");
		ShowImages.showWindow(renderedLabel,"Labeled Contours");
	}
}
