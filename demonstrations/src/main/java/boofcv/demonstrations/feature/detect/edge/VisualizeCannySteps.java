/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect.edge;

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GradientToEdgeFeatures;
import boofcv.alg.feature.detect.edge.HysteresisEdgeTraceMark;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.edge.VisualizeEdgeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Visualizes steps in canny edge detector.
 *
 * @author Peter Abeles
 */
// todo abstract image type. Put in integer images
public class VisualizeCannySteps {

	//	static String fileName = UtilIO.pathExample("outdoors01.jpg");
//	static String fileName = UtilIO.pathExample("sunflowers.jpg");
//	static String fileName = UtilIO.pathExample("particles01.jpg");
	static String fileName = UtilIO.pathExample("scale/beach02.jpg");
//	static String fileName = UtilIO.pathExample("indoors01.jpg");
//	static String fileName = UtilIO.pathExample("shapes01.png)";

	public static void main( String[] args ) {

		BufferedImage input = Objects.requireNonNull(UtilImageIO.loadImage(fileName));
		GrayF32 inputF32 = ConvertBufferedImage.convertFrom(input, (GrayF32)null);

		GrayF32 blurred = new GrayF32(inputF32.width, inputF32.height);
		GrayF32 derivX = new GrayF32(inputF32.width, inputF32.height);
		GrayF32 derivY = new GrayF32(inputF32.width, inputF32.height);
		GrayF32 intensity = new GrayF32(inputF32.width, inputF32.height);
		GrayF32 orientation = new GrayF32(inputF32.width, inputF32.height);
		GrayF32 suppressed = new GrayF32(inputF32.width, inputF32.height);
		GrayS8 direction = new GrayS8(inputF32.width, inputF32.height);
		GrayU8 output = new GrayU8(inputF32.width, inputF32.height);

		BlurStorageFilter<GrayF32> blur = FactoryBlurFilter.gaussian(GrayF32.class, -1, 2);
		ImageGradient<GrayF32, GrayF32> gradient = FactoryDerivative.sobel(GrayF32.class, null);

		blur.process(inputF32, blurred);
		gradient.process(blurred, derivX, derivY);

		float threshLow = 5;
		float threshHigh = 40;

		GradientToEdgeFeatures.intensityE(derivX, derivY, intensity);
		GradientToEdgeFeatures.direction(derivX, derivY, orientation);
		GradientToEdgeFeatures.discretizeDirection4(orientation, direction);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity, direction, suppressed);

		BufferedImage renderedOrientation = VisualizeEdgeFeatures.renderOrientation4(direction, suppressed, threshLow, null);

		HysteresisEdgeTraceMark hysteresis = new HysteresisEdgeTraceMark();
		hysteresis.process(suppressed, direction, threshLow, threshHigh, output);

		BufferedImage renderedLabel = VisualizeBinaryData.renderBinary(output, false, null);

		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(suppressed, "Suppressed Intensity");
		gui.addImage(intensity, "Raw Intensity");
		gui.addImage(renderedOrientation, "Orientation");
		gui.addImage(renderedLabel, "Labeled Contours");

		ShowImages.showWindow(gui, "Visualized Canny Steps", true);
	}
}
