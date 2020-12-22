/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.stereo;

import boofcv.abst.disparity.ConfigSpeckleFilter;
import boofcv.abst.disparity.DisparitySmoother;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Stereo disparity images can be very noisy and often times post processing to reduce the
 * noise is desired/required. This example demonstrates ones of the most basic ways to remove
 * noise by removing speckle. In this case, speckle are small regions with incorrect disparities.
 * The filter works by removing all small regions which have a significantly different
 * disparity from their neighbors.
 *
 * @author Peter Abeles
 */
public class ExampleStereoDisparitySmoothing {
	static int disparityRange = 60;

	public static void main( String[] args ) {
		String calibDir = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/");
		String imageDir = UtilIO.pathExample("stereo/");

		StereoParameters param = CalibrationIO.load(new File(calibDir, "stereo.yaml"));

		// load and convert images into a BoofCV format
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir, "chair01_left.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir, "chair01_right.jpg");

		GrayU8 distLeft = ConvertBufferedImage.convertFrom(origLeft, (GrayU8)null);
		GrayU8 distRight = ConvertBufferedImage.convertFrom(origRight, (GrayU8)null);

		// rectify images
		GrayU8 rectLeft = distLeft.createSameShape();
		GrayU8 rectRight = distRight.createSameShape();

		// Using a previous example, rectify then compute the disparity image
		RectifyCalibrated rectifier = ExampleStereoDisparity.rectify(distLeft, distRight, param, rectLeft, rectRight);
		GrayF32 disparity = ExampleStereoDisparity.denseDisparitySubpixel(
				rectLeft, rectRight, 5, 10, disparityRange);

		// Let's show the results in a single window before speckle is "removed"
		var gui = new ListDisplayPanel();
		gui.addImage("Before", VisualizeImageData.disparity(disparity, null, disparityRange, 0));
		gui.addItem("Before 3D",
				ExampleStereoDisparity3D.computeAndShowCloud(param, rectLeft, rectifier, disparity));

		// Here's what we came here for. Time to remove the speckle
		var configSpeckle = new ConfigSpeckleFilter();
		configSpeckle.similarTol = 1.0f; // Two pixels are connected if their disparity is this similar
		configSpeckle.maximumArea.setFixed(200); // probably the most important parameter, speckle size
		DisparitySmoother<GrayU8, GrayF32> smoother =
				FactoryStereoDisparity.removeSpeckle(configSpeckle, GrayF32.class);

		smoother.process(rectLeft, disparity, disparityRange);
		gui.addImage("After", VisualizeImageData.disparity(disparity, null, disparityRange, 0));
		gui.addItem("After 3D",
				ExampleStereoDisparity3D.computeAndShowCloud(param, rectLeft, rectifier, disparity));

		// Notice how in the "After 3D" view the number of randomly floating points is much less?
		ShowImages.showWindow(gui, "Disparity Smoothing", true);
	}
}
