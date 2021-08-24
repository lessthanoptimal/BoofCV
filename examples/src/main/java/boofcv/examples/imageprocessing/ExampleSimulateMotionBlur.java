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

package boofcv.examples.imageprocessing;

import boofcv.alg.distort.motion.MotionBlurOps;
import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;

import java.awt.image.BufferedImage;

/**
 * Shows you how to simulate motion blur in an image and what the results look like.
 *
 * @author Peter Abeles
 */
public class ExampleSimulateMotionBlur {
	public static void main( String[] args ) {
		// Load a chessboard image since it's easy to see blur with it
		GrayF32 image = UtilImageIO.loadImage(UtilIO.fileExample("calibration/mono/Sony_DSC-HX5V_Chess/frame03.jpg"), true, ImageType.SB_F32);
		GrayF32 blurred = image.createSameShape();

		var panel = new ListDisplayPanel();

		for (int degrees : new int[]{0, 25, -75}) {
			double radians = UtilAngle.degreeToRadian(degrees);
			for (double lengthOfMotion : new double[]{5, 15, 30}) {
				// Create the PSF (i.e. Kernel) that models motion blur with constant velocity
				// radians is the motion blur's direction
				Kernel2D_F32 kernel = MotionBlurOps.linearMotionPsf(lengthOfMotion, radians);
				GConvolveImageOps.convolve(kernel, image, blurred, BorderType.EXTENDED);

				// Visualize results
				BufferedImage visualized = ConvertBufferedImage.convertTo(blurred, null);
				panel.addImage(visualized, String.format("linear: angle=%d motion=%.0f", degrees, lengthOfMotion));
			}
		}

		ShowImages.showWindow(panel, "Simulated Motion Blur", true);
	}
}
