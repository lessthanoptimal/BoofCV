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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Shows how to rectify a pair of stereo images with known intrinsic parameters and stereo baseline. When you
 * rectify a stereo pair you are applying a transform that removes lens distortion and "rotates" the views
 * such that they are parallel to each other, facilitating stereo processing.
 *
 * The example code does the following:
 * <ol>
 *     <li>Load stereo extrinsic and intrinsic parameters from a file along with a pair of images.</li>
 *     <li>Undistort and rectify images. This provides one rectification matrix for each image along with a new
 *         camera calibration matrix.</li>
 *     <li>The original rectification does not try to maximize view area, however it can be adjusted.</li>
 *     <li>After rectification is finished the results are displayed.</li>
 * </ol>
 *
 * Note that the y-axis in left and right images align after rectification. You can click in the images to draw a line
 * that makes this easy to see. The curved image birder is an artifact of lens distortion being removed.
 *
 * @author Peter Abeles
 */
public class ExampleRectifyCalibratedStereo {
	public static void main( String[] args ) {
		String dir = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/");

		StereoParameters param = CalibrationIO.load(new File(dir, "stereo.yaml"));

		// load images
		BufferedImage origLeft = UtilImageIO.loadImage(dir, "left05.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(dir, "right05.jpg");

		// distorted images
		Planar<GrayF32> distLeft =
				ConvertBufferedImage.convertFromPlanar(origLeft, null, true, GrayF32.class);
		Planar<GrayF32> distRight =
				ConvertBufferedImage.convertFromPlanar(origRight, null, true, GrayF32.class);

		// storage for undistorted + rectified images
		Planar<GrayF32> rectLeft = distLeft.createSameShape();
		Planar<GrayF32> rectRight = distRight.createSameShape();

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
		// Both cameras have the same one after rectification.
		DMatrixRMaj rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(param.left, rect1, rect2, rectK, null);
//		RectifyImageOps.allInsideLeft(param.left, rect1, rect2, rectK, null);

		// undistorted and rectify images
		var rect1_F32 = new FMatrixRMaj(3, 3); // TODO simplify code some how
		var rect2_F32 = new FMatrixRMaj(3, 3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<Planar<GrayF32>, Planar<GrayF32>> rectifyImageLeft =
				RectifyDistortImageOps.rectifyImage(param.getLeft(), rect1_F32, BorderType.SKIP, distLeft.getImageType());
		ImageDistort<Planar<GrayF32>, Planar<GrayF32>> rectifyImageRight =
				RectifyDistortImageOps.rectifyImage(param.getRight(), rect2_F32, BorderType.SKIP, distRight.getImageType());

		rectifyImageLeft.apply(distLeft, rectLeft);
		rectifyImageRight.apply(distRight, rectRight);

		// convert for output
		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft, null, true);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null, true);

		// show results and draw a horizontal line where the user clicks to see rectification easier
		var panel = new ListDisplayPanel();
		panel.addItem(new RectifiedPairPanel(true, origLeft, origRight), "Original");
		panel.addItem(new RectifiedPairPanel(true, outLeft, outRight), "Rectified");

		ShowImages.showWindow(panel, "Stereo Rectification Calibrated", true);
	}
}
