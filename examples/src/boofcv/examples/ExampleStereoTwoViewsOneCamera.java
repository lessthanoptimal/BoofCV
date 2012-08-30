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
import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.UtilIntrinsic;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.sfm.robust.DistanceSe3SymmetricSq;
import boofcv.alg.sfm.robust.Se3FromEssentialGenerator;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Example demonstrating how to create a point cloud from a calibrated camera when two views of the same object
 * have been taken.  For best results the two views should be take from about the same orientation with translation
 * along the camera's x-axis.
 *
 * @author Peter Abeles
 */
public class ExampleStereoTwoViewsOneCamera {


	public static Se3_F64 estimateCameraMotion( List<AssociatedPair> matchedCalibrated ) {
		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamentalOne(7, false, 1);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
		ModelGenerator<Se3_F64,AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg,triangulate);

		DistanceFromModel<Se3_F64,AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate);

		int N = generateEpipolarMotion.getMinimumPoints();

		ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion =
				new SimpleInlierRansac<Se3_F64,AssociatedPair>(2323,generateEpipolarMotion,distanceSe3,
						500,N,N,100000,2);

		if( !epipolarMotion.process(matchedCalibrated) )
			throw new RuntimeException("Motion estimation failed");

		return epipolarMotion.getModel();
	}

	public static List<AssociatedPair> convertToCalibrated(List<AssociatedPair> matchedFeatures , IntrinsicParameters intrinsic ) {
		DenseMatrix64F K_inv = UtilIntrinsic.calibrationMatrix(intrinsic, null);
		if( !CommonOps.invert(K_inv))
			throw new RuntimeException("Matrix invert failed");

		List<AssociatedPair> calibratedFeatures = new ArrayList<AssociatedPair>();

		for( AssociatedPair p : matchedFeatures ) {
			AssociatedPair c = new AssociatedPair();
			GeometryMath_F64.mult(K_inv, p.keyLoc, c.keyLoc);
			GeometryMath_F64.mult(K_inv,p.currLoc,c.currLoc);

			calibratedFeatures.add(p);
		}

		return calibratedFeatures;
	}

	public static void rectifyImages( ImageUInt8 distortedLeft ,
									  ImageUInt8 distortedRight ,
									  Se3_F64 leftToRight ,
									  IntrinsicParameters intrinsic ,
									  ImageUInt8 rectifiedLeft ,
									  ImageUInt8 rectifiedRight ,
									  DenseMatrix64F rectifiedK )
	{
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

		// original camera calibration matrices
		DenseMatrix64F K = UtilIntrinsic.calibrationMatrix(intrinsic,null);

		rectifyAlg.process(K,new Se3_F64(),K,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();

		// New calibration matrix,
		rectifiedK.set(rectifyAlg.getCalibrationMatrix());

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.allInsideLeft(intrinsic, rect1, rect2, rectifiedK);

		// undistorted and rectify images
		ImageDistort<ImageUInt8> distortLeft =
				RectifyImageOps.rectifyImage(intrinsic,rect1, ImageUInt8.class);
		ImageDistort<ImageUInt8> distortRight =
				RectifyImageOps.rectifyImage(intrinsic,rect2, ImageUInt8.class);

		distortLeft.apply(distortedLeft, rectifiedLeft);
		distortRight.apply(distortedRight, rectifiedRight);
	}

	public static void main( String args[] ) {
		String calibDir = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";
		String imageDir = "/home/pja/temp/images/";

		// Camera parameters
		IntrinsicParameters intrinsic = BoofMiscOps.loadXML(calibDir+"intrinsic.xml");

		// original image
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir+"image000001.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir+"image000080.jpg");

		// distorted input images
		ImageUInt8 distortedLeft = ConvertBufferedImage.convertFrom(origLeft,(ImageUInt8)null);
		ImageUInt8 distortedRight = ConvertBufferedImage.convertFrom(origRight,(ImageUInt8)null);

		// matched features between the two images
		List<AssociatedPair> matchedFeatures = ExampleFundamentalMatrix.computeMatches(origLeft,origRight);

		// convert from pixel coordinates into calibrated coordinates
		List<AssociatedPair> matchedCalibrated = convertToCalibrated(matchedFeatures,intrinsic);

		// Robustly estimate camera motion
		Se3_F64 leftToRight = estimateCameraMotion(matchedCalibrated);

		// Rectify and remove lens distortion for stereo processing
		DenseMatrix64F rectifiedK = new DenseMatrix64F(3,3);
		ImageUInt8 rectifiedLeft = new ImageUInt8(distortedLeft.width,distortedLeft.height);
		ImageUInt8 rectifiedRight = new ImageUInt8(distortedLeft.width,distortedLeft.height);

		rectifyImages(distortedLeft,distortedRight,leftToRight,intrinsic,rectifiedLeft,rectifiedRight,rectifiedK);

		// compute disparity
		int minDisparity = 1;
		int maxDisparity = 250;
		StereoDisparity<ImageUInt8,ImageFloat32> disparityAlg =
				FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity, 5, 5, 40, 6, 0.05, ImageUInt8.class);

		// process and return the results
		disparityAlg.process(rectifiedLeft,rectifiedRight) ;
		ImageFloat32 disparity = disparityAlg.getDisparity();

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);

		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectifiedLeft,null);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectifiedRight,null);

		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight),"Rectification");
		ShowImages.showWindow(visualized,"Disparity");
	}

}
