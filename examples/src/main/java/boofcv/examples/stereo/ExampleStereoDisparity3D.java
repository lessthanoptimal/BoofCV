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

package boofcv.examples.stereo;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.mvs.DisparityParameters;
import boofcv.alg.mvs.MultiViewStereoOps;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import org.ejml.data.DMatrixRMaj;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Expanding upon ExampleStereoDisparity, this example demonstrates how to rescale an image for stereo processing and
 * then compute its 3D point cloud. Images are often rescaled to improve speed and some times quality. Creating
 * 3D point clouds from disparity images is easy and well documented in the literature, but there are some nuances
 * to it.
 *
 * @author Peter Abeles
 */
public class ExampleStereoDisparity3D {

	// Specifies the disparity values which will be considered
	private static final int disparityMin = 15;
	private static final int disparityRange = 60;

	/**
	 * Given already computed rectified images and known stereo parameters, create a 3D cloud and visualize it
	 */
	public static JComponent computeAndShowCloud( StereoParameters param,
												  GrayU8 rectLeft,
												  RectifyCalibrated rectAlg, GrayF32 disparity ) {
		// The point cloud will be in the left cameras reference frame
		DMatrixRMaj rectK = rectAlg.getCalibrationMatrix();
		DMatrixRMaj rectR = rectAlg.getRectifiedRotation();

		// Put all the disparity parameters into one data structure
		var disparityParameters = new DisparityParameters();
		disparityParameters.baseline = param.getBaseline();
		disparityParameters.disparityMin = disparityMin;
		disparityParameters.disparityRange = disparityRange;
		disparityParameters.rotateToRectified.setTo(rectR);
		PerspectiveOps.matrixToPinhole(rectK, rectLeft.width, rectLeft.height, disparityParameters.pinhole);

		// Iterate through each pixel in disparity image and compute its 3D coordinate
		PointCloudViewer pcv = VisualizeData.createPointCloudViewer();
		pcv.setTranslationStep(param.getBaseline()*0.1);

		// Next create the 3D point cloud. The function below will handle conversion from disparity into
		// XYZ, then transform from rectified into normal camera coordinate system. Feel free to glance at the
		// source code to understand exactly what it's doing
		MultiViewStereoOps.disparityToCloud(disparity, disparityParameters, null,
				( pixX, pixY, x, y, z ) -> {
					// look up the gray value. Then convert it into RGB
					int v = rectLeft.unsafe_get(pixX, pixY);
					pcv.addPoint(x, y, z, v << 16 | v << 8 | v);
				});

		// Configure the display
//		pcv.setFog(true);
//		pcv.setClipDistance(baseline*45);
//		PeriodicColorizer colorizer = new TwoAxisRgbPlane.Z_XY(4.0);
//		colorizer.setPeriod(baseline*5);
//		pcv.setColorizer(colorizer); // sometimes pseudo color can be easier to view
		pcv.setDotSize(1);
		pcv.setCameraHFov(PerspectiveOps.computeHFov(param.left));
//		pcv.setCameraToWorld(cameraToWorld);
		JComponent viewer = pcv.getComponent();
		viewer.setPreferredSize(new Dimension(600, 600*param.left.height/param.left.width));
		return viewer;
	}

	public static void main( String[] args ) {
		// ------------- Compute Stereo Correspondence

		// Load camera images and stereo camera parameters
		String calibDir = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/");
		String imageDir = UtilIO.pathExample("stereo/");

		StereoParameters param = CalibrationIO.load(new File(calibDir, "stereo.yaml"));

		// load and convert images into a BoofCV format
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir, "chair01_left.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir, "chair01_right.jpg");

		GrayU8 distLeft = ConvertBufferedImage.convertFrom(origLeft, (GrayU8)null);
		GrayU8 distRight = ConvertBufferedImage.convertFrom(origRight, (GrayU8)null);

		// rectify images and compute disparity
		GrayU8 rectLeft = distLeft.createSameShape();
		GrayU8 rectRight = distRight.createSameShape();

		RectifyCalibrated rectAlg = ExampleStereoDisparity.rectify(distLeft, distRight, param, rectLeft, rectRight);

//		GrayU8 disparity = ExampleStereoDisparity.denseDisparity(rectLeft, rectRight, 3,disparityMin, disparityRange);
		GrayF32 disparity = ExampleStereoDisparity.denseDisparitySubpixel(
				rectLeft, rectRight, 5, disparityMin, disparityRange);

		// ------------- Convert disparity image into a 3D point cloud

		JComponent viewer = computeAndShowCloud(param, rectLeft, rectAlg, disparity);

		// display the results. Click and drag to change point cloud camera
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, disparityRange, 0);
		ShowImages.showWindow(visualized, "Disparity", true);
		ShowImages.showWindow(viewer, "Point Cloud", true);
	}
}
