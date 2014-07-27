/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;

/**
 * <p>
 * Show how to rectify a pair of stereo images with known intrinsic parameters and stereo baseline.
 * The example code does the following:<br>
 * 1) Load stereo parameters from XML file with a pair of images.<br>
 * 2) Undistort and rectify images..  This provides one rectification matrix
 * for each image along with a new camera calibration matrix.<br>
 * 3) The original rectification does not try to maximize view area, however it can be adjusted.
 * 4)After rectification is finished the results are displayed.<br>
 * </p>
 *
 * <p>
 * Note that the y-axis in left and right images align after rectification.  The curved image edge
 * is an artifact of lens distortion being removed.
 * </p>
 *
 * @author Peter Abeles
 */
public class ExampleRectifyCalibratedStereo {

	public static void main( String args[] ) {
		String dir = "../data/applet/calibration/stereo/Bumblebee2_Chess/";

		StereoParameters param = UtilIO.loadXML(dir + "stereo.xml");

		// load images
		BufferedImage origLeft = UtilImageIO.loadImage(dir+"left05.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(dir+"right05.jpg");

		// distorted images
		MultiSpectral<ImageFloat32> distLeft =
				ConvertBufferedImage.convertFromMulti(origLeft, null,true, ImageFloat32.class);
		MultiSpectral<ImageFloat32> distRight =
				ConvertBufferedImage.convertFromMulti(origRight, null,true, ImageFloat32.class);

		// storage for undistorted + rectified images
		MultiSpectral<ImageFloat32> rectLeft = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distLeft.getWidth(),distLeft.getHeight(),distLeft.getNumBands());
		MultiSpectral<ImageFloat32> rectRight = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distRight.getWidth(),distRight.getHeight(),distRight.getNumBands());

		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(param.getLeft(), null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(param.getRight(), null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();
		// New calibration matrix,
		// Both cameras have the same one after rectification.
		DenseMatrix64F rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(param.left, rect1, rect2, rectK);
//		RectifyImageOps.allInsideLeft(param.left, leftHanded, rect1, rect2, rectK);

		// undistorted and rectify images
		ImageDistort<ImageFloat32,ImageFloat32> imageDistortLeft =
				RectifyImageOps.rectifyImage(param.getLeft(), rect1, ImageFloat32.class);
		ImageDistort<ImageFloat32,ImageFloat32> imageDistortRight =
				RectifyImageOps.rectifyImage(param.getRight(), rect2, ImageFloat32.class);

		DistortImageOps.distortMS(distLeft, rectLeft, imageDistortLeft);
		DistortImageOps.distortMS(distRight, rectRight, imageDistortRight);

		// convert for output
		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft,null,true);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null,true);

		// show results and draw a horizontal line where the user clicks to see rectification easier
		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addItem(new RectifiedPairPanel(true, origLeft, origRight), "Original");
		panel.addItem(new RectifiedPairPanel(true, outLeft, outRight), "Rectified");

		ShowImages.showWindow(panel,"Stereo Rectification Calibrated");
	}
}
