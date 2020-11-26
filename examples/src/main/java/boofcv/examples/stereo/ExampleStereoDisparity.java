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

import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The disparity between two stereo images is used to estimate the range of objects inside
 * the camera's view. Disparity is the difference in position between the viewed location
 * of a point in the left and right stereo images. Because input images are rectified,
 * corresponding points can be found by only searching along image rows.
 *
 * Values in the disparity image specify how different the two images are. A value of X indicates
 * that the corresponding point in the right image from the left is at "x' = x - X - minDisparity",
 * where x' and x are the locations in the right and left images respectively. An invalid value
 * with no correspondence is set to a value more than (max - min) disparity.
 *
 * @author Peter Abeles
 */
public class ExampleStereoDisparity {

	/**
	 * Computes the dense disparity between between two stereo images. The input images
	 * must be rectified with lens distortion removed to work!  Floating point images
	 * are also supported.
	 *
	 * @param rectLeft Rectified left camera image
	 * @param rectRight Rectified right camera image
	 * @param regionSize Radius of region being matched
	 * @param disparityMin Minimum disparity that is considered
	 * @param disparityRange Number of disparity values considered.
	 * @return Disparity image
	 */
	public static GrayU8 denseDisparity( GrayU8 rectLeft, GrayU8 rectRight,
										 int regionSize,
										 int disparityMin, int disparityRange ) {
		// A slower but more accuracy algorithm is selected
		// All of these parameters should be turned
		ConfigDisparityBMBest5 config = new ConfigDisparityBMBest5();
		config.errorType = DisparityError.CENSUS;
		config.disparityMin = disparityMin;
		config.disparityRange = disparityRange;
		config.subpixel = false;
		config.regionRadiusX = config.regionRadiusY = regionSize;
		config.maxPerPixelError = 35;
		config.validateRtoL = 1;
		config.texture = 0.2;
		StereoDisparity<GrayU8, GrayU8> disparityAlg =
				FactoryStereoDisparity.blockMatchBest5(config, GrayU8.class, GrayU8.class);

		// process and return the results
		disparityAlg.process(rectLeft, rectRight);

		return disparityAlg.getDisparity();
	}

	/**
	 * Same as above, but compute disparity to within sub-pixel accuracy. The difference between the
	 * two is more apparent when a 3D point cloud is computed.
	 */
	public static GrayF32 denseDisparitySubpixel( GrayU8 rectLeft, GrayU8 rectRight,
												  int regionSize,
												  int disparityMin, int disparityRange ) {
		// A slower but more accuracy algorithm is selected
		// All of these parameters should be turned
		ConfigDisparityBMBest5 config = new ConfigDisparityBMBest5();
		config.errorType = DisparityError.CENSUS;
		config.disparityMin = disparityMin;
		config.disparityRange = disparityRange;
		config.subpixel = true;
		config.regionRadiusX = config.regionRadiusY = regionSize;
		config.maxPerPixelError = 35;
		config.validateRtoL = 1;
		config.texture = 0.2;
		StereoDisparity<GrayU8, GrayF32> disparityAlg =
				FactoryStereoDisparity.blockMatchBest5(config, GrayU8.class, GrayF32.class);

		// process and return the results
		disparityAlg.process(rectLeft, rectRight);

		return disparityAlg.getDisparity();
	}

	/**
	 * Rectified the input images using known calibration.
	 */
	public static RectifyCalibrated rectify( GrayU8 origLeft, GrayU8 origRight,
											 StereoParameters param,
											 GrayU8 rectLeft, GrayU8 rectRight ) {
		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(param.getLeft(), (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(param.getRight(), (DMatrixRMaj)null);

		rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

		// rectification matrix for each image
		DMatrixRMaj rect1 = rectifyAlg.getUndistToRectPixels1();
		DMatrixRMaj rect2 = rectifyAlg.getUndistToRectPixels2();
		// New calibration matrix,
		DMatrixRMaj rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.allInsideLeft(param.left, rect1, rect2, rectK, null);

		// undistorted and rectify images
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3);
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<GrayU8, GrayU8> imageDistortLeft =
				RectifyDistortImageOps.rectifyImage(param.getLeft(), rect1_F32, BorderType.SKIP, origLeft.getImageType());
		ImageDistort<GrayU8, GrayU8> imageDistortRight =
				RectifyDistortImageOps.rectifyImage(param.getRight(), rect2_F32, BorderType.SKIP, origRight.getImageType());

		imageDistortLeft.apply(origLeft, rectLeft);
		imageDistortRight.apply(origRight, rectRight);

		return rectifyAlg;
	}

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

		rectify(distLeft, distRight, param, rectLeft, rectRight);

		// compute disparity
		int disparityRange = 60;
		GrayU8 disparity = denseDisparity(rectLeft, rectRight, 5, 10, disparityRange);
//		GrayF32 disparity = denseDisparitySubpixel(rectLeft, rectRight, 5, 10, disparityRange);

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, disparityRange, 0);

		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(rectLeft, "Rectified");
		gui.addImage(visualized, "Disparity");

		ShowImages.showWindow(gui, "Stereo Disparity", true);
	}
}
