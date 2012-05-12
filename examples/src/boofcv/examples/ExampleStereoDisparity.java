/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.UtilIntrinsic;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;

/**
 * The disparity between two stereo images is used to estimate the range of objects inside
 * the camera's view.  Disparity is the difference in position between the viewed location
 * of a point in the left and right stereo images.  The following example demonstrates
 * how to compute the disparity between two calibrated stereo camera images.
 *
 * The disparity image itself is encoded such that the values of each pixel indicate the
 * difference between the left and right image.  If no association was found for a particular
 * pixel then it is assigned an invalid value, which is any value more than (max - min disparity).
 *
 * @author Peter Abeles
 */
public class ExampleStereoDisparity {

	/**
	 * Computes the dense disparity between between two stereo images.  The input images
	 * must be rectified with lens distortion removed to work!  Floating point images
	 * are also supported.
	 *
	 * @param rectLeft Rectified left camera image
	 * @param rectRight Rectified right camera image
	 * @param minDisparity Minimum disparity that is considered
	 * @param maxDisparity Maximum disparity that is considered
	 * @return Disparity image
	 */
	public static ImageUInt8 denseDisparity( ImageUInt8 rectLeft , ImageUInt8 rectRight ,
											 int minDisparity , int maxDisparity )
	{
		// A slower but more accuracy algorithm is selected
		// All of these parameters should be turned
		StereoDisparity<ImageUInt8,ImageUInt8> disparityAlg =
				FactoryStereoDisparity.regionWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity, 3, 3, 20, 6, 0.2, ImageUInt8.class);

		// process and return the results
		disparityAlg.process(rectLeft,rectRight);

		return disparityAlg.getDisparity();
	}

	/**
	 * Rectified the input images using known calibration.
	 */
	public static void rectify( ImageUInt8 origLeft , ImageUInt8 origRight ,
								StereoParameters param ,
								ImageUInt8 rectLeft , ImageUInt8 rectRight )
	{
		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = UtilIntrinsic.calibrationMatrix(param.getLeft(), null);
		DenseMatrix64F K2 = UtilIntrinsic.calibrationMatrix(param.getRight(),null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();
		// New calibration matrix,
		DenseMatrix64F rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(param.left, rect1, rect2, rectK);

		// undistorted and rectify images
		ImageDistort<ImageUInt8> imageDistortLeft =
				RectifyImageOps.rectifyImage(param.getLeft(), rect1, ImageUInt8.class);
		ImageDistort<ImageUInt8> imageDistortRight =
				RectifyImageOps.rectifyImage(param.getRight(), rect2, ImageUInt8.class);

		imageDistortLeft.apply(origLeft, rectLeft);
		imageDistortRight.apply(origRight, rectRight);
	}

	public static void main( String args[] ) {
		String calibDir = "../data/applet/calibration/stereo/Bumblebee2_Chess/";
		String imageDir = "../data/applet/stereo/";

		StereoParameters param = BoofMiscOps.loadXML(calibDir + "stereo.xml");

		// load and convert images into a BoofCV format
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir + "thing01_left.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir+"thing01_right.jpg");

		ImageUInt8 distLeft = ConvertBufferedImage.convertFrom(origLeft,(ImageUInt8)null);
		ImageUInt8 distRight = ConvertBufferedImage.convertFrom(origRight,(ImageUInt8)null);

		// rectify images
		ImageUInt8 rectLeft = new ImageUInt8(distLeft.width,distLeft.height);
		ImageUInt8 rectRight = new ImageUInt8(distRight.width,distRight.height);

		rectify(distLeft,distRight,param,rectLeft,rectRight);

		// compute disparity
		ImageUInt8 disparity = denseDisparity(rectLeft,rectRight,10,150);

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null,10,150,0);

		ShowImages.showWindow(rectLeft,"Rectified");
		ShowImages.showWindow(visualized,"Disparity");
	}
}
