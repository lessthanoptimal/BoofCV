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

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;

/**
 * The disparity between two stereo images is used to estimate the range of objects inside
 * the camera's view.  Disparity is the difference in position between the viewed location
 * of a point in the left and right stereo images.  Because input images are rectified,
 * corresponding points can be found by only searching along image rows.
 *
 * Values in the disparity image specify how different the two images are.  A value of X indicates
 * that the corresponding point in the right image from the left is at "x' = x - X - minDisparity",
 * where x' and x are the locations in the right and left images respectively.  An invalid value
 * with no correspondence is set to a value more than (max - min) disparity.
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
	 * @param regionSize Radius of region being matched
	 * @param minDisparity Minimum disparity that is considered
	 * @param maxDisparity Maximum disparity that is considered
	 * @return Disparity image
	 */
	public static ImageUInt8 denseDisparity( ImageUInt8 rectLeft , ImageUInt8 rectRight ,
											 int regionSize,
											 int minDisparity , int maxDisparity )
	{
		// A slower but more accuracy algorithm is selected
		// All of these parameters should be turned
		StereoDisparity<ImageUInt8,ImageUInt8> disparityAlg =
				FactoryStereoDisparity.regionWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity, regionSize, regionSize, 25, 1, 0.2, ImageUInt8.class);

		// process and return the results
		disparityAlg.process(rectLeft,rectRight);

		return disparityAlg.getDisparity();
	}

	/**
	 * Same as above, but compute disparity to within sub-pixel accuracy. The difference between the
	 * two is more apparent when a 3D point cloud is computed.
	 */
	public static ImageFloat32 denseDisparitySubpixel( ImageUInt8 rectLeft , ImageUInt8 rectRight ,
													   int regionSize ,
													   int minDisparity , int maxDisparity )
	{
		// A slower but more accuracy algorithm is selected
		// All of these parameters should be turned
		StereoDisparity<ImageUInt8,ImageFloat32> disparityAlg =
				FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity, regionSize, regionSize, 25, 1, 0.2, ImageUInt8.class);

		// process and return the results
		disparityAlg.process(rectLeft,rectRight);

		return disparityAlg.getDisparity();
	}

	/**
	 * Rectified the input images using known calibration.
	 */
	public static RectifyCalibrated rectify( ImageUInt8 origLeft , ImageUInt8 origRight ,
											 StereoParameters param ,
											 ImageUInt8 rectLeft , ImageUInt8 rectRight )
	{
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
		DenseMatrix64F rectK = rectifyAlg.getCalibrationMatrix();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.allInsideLeft(param.left, rect1, rect2, rectK);

		// undistorted and rectify images
		ImageDistort<ImageUInt8,ImageUInt8> imageDistortLeft =
				RectifyImageOps.rectifyImage(param.getLeft(), rect1, ImageUInt8.class);
		ImageDistort<ImageUInt8,ImageUInt8> imageDistortRight =
				RectifyImageOps.rectifyImage(param.getRight(), rect2, ImageUInt8.class);

		imageDistortLeft.apply(origLeft, rectLeft);
		imageDistortRight.apply(origRight, rectRight);

		return rectifyAlg;
	}

	public static void main( String args[] ) {
		String calibDir = "../data/applet/calibration/stereo/Bumblebee2_Chess/";
		String imageDir = "../data/applet/stereo/";

		StereoParameters param = UtilIO.loadXML(calibDir + "stereo.xml");

		// load and convert images into a BoofCV format
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir + "chair01_left.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir + "chair01_right.jpg");

		ImageUInt8 distLeft = ConvertBufferedImage.convertFrom(origLeft,(ImageUInt8)null);
		ImageUInt8 distRight = ConvertBufferedImage.convertFrom(origRight,(ImageUInt8)null);

		// rectify images
		ImageUInt8 rectLeft = new ImageUInt8(distLeft.width,distLeft.height);
		ImageUInt8 rectRight = new ImageUInt8(distRight.width,distRight.height);

		rectify(distLeft,distRight,param,rectLeft,rectRight);

		// compute disparity
		ImageUInt8 disparity = denseDisparity(rectLeft,rectRight,5,10,60);
//		ImageFloat32 disparity = denseDisparitySubpixel(rectLeft,rectRight,5,10,60);

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null,10,60,0);

		ShowImages.showWindow(rectLeft,"Rectified");
		ShowImages.showWindow(visualized,"Disparity");
	}
}
