/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.edge.VisualizeEdgeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt8;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;


/**
 * @author Peter Abeles
 */
// todo abstract image type.  Put in integer images
public class VisualizeCannySteps {

	//	static String fileName = UtilIO.pathExample("outdoors01.jpg");
//	static String fileName = UtilIO.pathExample("sunflowers.jpg");
//	static String fileName = UtilIO.pathExample("particles01.jpg");
	static String fileName = UtilIO.pathExample("scale/beach02.jpg");
//	static String fileName = UtilIO.pathExample("indoors01.jpg");
//	static String fileName = UtilIO.pathExample("shapes01.png)";

	public static void main( String args[] ){

		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		ImageFloat32 blurred = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 derivX = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 derivY = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 intensity = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 orientation = new ImageFloat32(inputF32.width,inputF32.height);
		ImageFloat32 suppressed = new ImageFloat32(inputF32.width,inputF32.height);
		ImageSInt8 direction = new ImageSInt8(inputF32.width,inputF32.height);
		ImageUInt8 output = new ImageUInt8(inputF32.width,inputF32.height);

		BlurStorageFilter<ImageFloat32> blur = FactoryBlurFilter.gaussian(ImageFloat32.class,-1,2);
		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel_F32();

		blur.process(inputF32,blurred);
		gradient.process(blurred,derivX,derivY);

		float threshLow = 5;
		float threshHigh = 40;

		GradientToEdgeFeatures.intensityE(derivX, derivY, intensity);
		GradientToEdgeFeatures.direction(derivX, derivY, orientation);
		GradientToEdgeFeatures.discretizeDirection4(orientation, direction);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity, direction, suppressed);

		BufferedImage renderedOrientation = VisualizeEdgeFeatures.renderOrientation4(direction,suppressed,threshLow,null);

		HysteresisEdgeTraceMark hysteresis = new HysteresisEdgeTraceMark();
		hysteresis.process(suppressed,direction,threshLow,threshHigh,output);

		BufferedImage renderedLabel = VisualizeBinaryData.renderBinary(output, false, null);

		ListDisplayPanel gui =  new ListDisplayPanel();
		gui.addImage(suppressed,"Suppressed Intensity");
		gui.addImage(intensity,"Raw Intensity");
		gui.addImage(renderedOrientation,"Orientation");
		gui.addImage(renderedLabel, "Labeled Contours");

		ShowImages.showWindow(gui,"Visualized Canny Steps", true);
	}
}
