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
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.sfm.robust.DistanceSe3SymmetricSq;
import boofcv.alg.sfm.robust.Se3FromEssentialGenerator;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.gui.d3.PointCloudTiltPanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.DoNothingTransform_F64;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.data.DenseMatrix64F;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Example demonstrating how to use to images taken from a single calibrated camera to create a stereo disparity image,
 * from which a dense 3D point cloud of the scene can be computed.  For this technique to work the camera's motion
 * needs to be approximately tangential to the direction the camera is pointing.  The code below assumes that the first
 * image is to the left of the second image.
 *
 * @author Peter Abeles
 */
public class ExampleStereoTwoViewsOneCamera {

	// Disparity calculation parameters
	private static final int minDisparity = 15;
	private static final int maxDisparity = 100;

	public static void main(String args[]) {
		// specify location of images and calibration
		String calibDir = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";
		String imageDir = "../data/applet/stereo/";

		// Camera parameters
		IntrinsicParameters intrinsic = UtilIO.loadXML(calibDir + "intrinsic.xml");

		// Input images from the camera moving left to right
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir + "mono_wall_01.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir+"mono_wall_02.jpg");

		// Input images with lens distortion
		ImageUInt8 distortedLeft = ConvertBufferedImage.convertFrom(origLeft, (ImageUInt8) null);
		ImageUInt8 distortedRight = ConvertBufferedImage.convertFrom(origRight, (ImageUInt8) null);

		// matched features between the two images
		List<AssociatedPair> matchedFeatures = ExampleFundamentalMatrix.computeMatches(origLeft, origRight);

		// convert from pixel coordinates into normalized image coordinates
		List<AssociatedPair> matchedCalibrated = convertToNormalizedCoordinates(matchedFeatures, intrinsic);

		// Robustly estimate camera motion
		List<AssociatedPair> inliers = new ArrayList<AssociatedPair>();
		Se3_F64 leftToRight = estimateCameraMotion(intrinsic, matchedCalibrated, inliers);

		drawInliers(origLeft, origRight, intrinsic, inliers);

		// Rectify and remove lens distortion for stereo processing
		DenseMatrix64F rectifiedK = new DenseMatrix64F(3, 3);
		ImageUInt8 rectifiedLeft = new ImageUInt8(distortedLeft.width, distortedLeft.height);
		ImageUInt8 rectifiedRight = new ImageUInt8(distortedLeft.width, distortedLeft.height);

		rectifyImages(distortedLeft, distortedRight, leftToRight, intrinsic, rectifiedLeft, rectifiedRight, rectifiedK);

		// compute disparity
		StereoDisparity<ImageSInt16, ImageFloat32> disparityAlg =
				FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity, 5, 5, 20, 1, 0.1, ImageSInt16.class);

		// Apply the Laplacian across the image to add extra resistance to changes in lighting or camera gain
		ImageSInt16 derivLeft = new ImageSInt16(rectifiedLeft.width,rectifiedLeft.height);
		ImageSInt16 derivRight = new ImageSInt16(rectifiedLeft.width,rectifiedLeft.height);
		LaplacianEdge.process(rectifiedLeft, derivLeft);
		LaplacianEdge.process(rectifiedRight,derivRight);

		// process and return the results
		disparityAlg.process(derivLeft, derivRight);
		ImageFloat32 disparity = disparityAlg.getDisparity();

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);

		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectifiedLeft, null);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectifiedRight, null);

		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification");
		ShowImages.showWindow(visualized, "Disparity");

		showPointCloud(disparity, outLeft, leftToRight, rectifiedK, minDisparity, maxDisparity);

		System.out.println("Total found " + matchedCalibrated.size());
		System.out.println("Total Inliers " + inliers.size());
	}

	/**
	 * Estimates the camera motion robustly using RANSAC and a set of associated points.
	 *
	 * @param intrinsic   Intrinsic camera parameters
	 * @param matchedNorm set of matched point features in normalized image coordinates
	 * @param inliers     OUTPUT: Set of inlier features from RANSAC
	 * @return Found camera motion.  Note translation has an arbitrary scale
	 */
	public static Se3_F64 estimateCameraMotion(IntrinsicParameters intrinsic,
											   List<AssociatedPair> matchedNorm, List<AssociatedPair> inliers)
	{
		Estimate1ofEpipolar essentialAlg = FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_5_NISTER, 5);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
		ModelManager<Se3_F64> manager = new ModelManagerSe3_F64();
		ModelGenerator<Se3_F64, AssociatedPair> generateEpipolarMotion =
				new Se3FromEssentialGenerator(essentialAlg, triangulate);

		DistanceFromModel<Se3_F64, AssociatedPair> distanceSe3 =
				new DistanceSe3SymmetricSq(triangulate,
						intrinsic.fx, intrinsic.fy, intrinsic.skew,
						intrinsic.fx, intrinsic.fy, intrinsic.skew);

		// 1/2 a pixel tolerance for RANSAC inliers
		double ransacTOL = 0.5 * 0.5 * 2.0;

		ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
				new Ransac<Se3_F64, AssociatedPair>(2323, manager, generateEpipolarMotion, distanceSe3,
						200, ransacTOL);

		if (!epipolarMotion.process(matchedNorm))
			throw new RuntimeException("Motion estimation failed");

		// save inlier set for debugging purposes
		inliers.addAll(epipolarMotion.getMatchSet());

		return epipolarMotion.getModelParameters();
	}

	/**
	 * Convert a set of associated point features from pixel coordinates into normalized image coordinates.
	 */
	public static List<AssociatedPair> convertToNormalizedCoordinates(List<AssociatedPair> matchedFeatures, IntrinsicParameters intrinsic) {

		PointTransform_F64 tran = LensDistortionOps.transformRadialToNorm_F64(intrinsic);

		List<AssociatedPair> calibratedFeatures = new ArrayList<AssociatedPair>();

		for (AssociatedPair p : matchedFeatures) {
			AssociatedPair c = new AssociatedPair();

			tran.compute(p.p1.x, p.p1.y, c.p1);
			tran.compute(p.p2.x, p.p2.y, c.p2);

			calibratedFeatures.add(c);
		}

		return calibratedFeatures;
	}

	/**
	 * Remove lens distortion and rectify stereo images
	 *
	 * @param distortedLeft  Input distorted image from left camera.
	 * @param distortedRight Input distorted image from right camera.
	 * @param leftToRight    Camera motion from left to right
	 * @param intrinsic      Intrinsic camera parameters
	 * @param rectifiedLeft  Output rectified image for left camera.
	 * @param rectifiedRight Output rectified image for right camera.
	 * @param rectifiedK     Output camera calibration matrix for rectified camera
	 */
	public static void rectifyImages(ImageUInt8 distortedLeft,
									 ImageUInt8 distortedRight,
									 Se3_F64 leftToRight,
									 IntrinsicParameters intrinsic,
									 ImageUInt8 rectifiedLeft,
									 ImageUInt8 rectifiedRight,
									 DenseMatrix64F rectifiedK) {
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

		// original camera calibration matrices
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic, null);

		rectifyAlg.process(K, new Se3_F64(), K, leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();

		// New calibration matrix,
		rectifiedK.set(rectifyAlg.getCalibrationMatrix());

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.allInsideLeft(intrinsic, rect1, rect2, rectifiedK);

		// undistorted and rectify images
		ImageDistort<ImageUInt8,ImageUInt8> distortLeft =
				RectifyImageOps.rectifyImage(intrinsic, rect1, ImageUInt8.class);
		ImageDistort<ImageUInt8,ImageUInt8> distortRight =
				RectifyImageOps.rectifyImage(intrinsic, rect2, ImageUInt8.class);

		distortLeft.apply(distortedLeft, rectifiedLeft);
		distortRight.apply(distortedRight, rectifiedRight);
	}

	/**
	 * Draw inliers for debugging purposes.  Need to convert from normalized to pixel coordinates.
	 */
	public static void drawInliers(BufferedImage left, BufferedImage right, IntrinsicParameters intrinsic,
								   List<AssociatedPair> normalized) {
		PointTransform_F64 tran = LensDistortionOps.transformNormToRadial_F64(intrinsic);

		List<AssociatedPair> pixels = new ArrayList<AssociatedPair>();

		for (AssociatedPair n : normalized) {
			AssociatedPair p = new AssociatedPair();

			tran.compute(n.p1.x, n.p1.y, p.p1);
			tran.compute(n.p2.x, n.p2.y, p.p2);

			pixels.add(p);
		}

		// display the results
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(pixels);
		panel.setImages(left, right);

		ShowImages.showWindow(panel, "Inlier Features");
	}

	/**
	 * Show results as a point cloud
	 */
	public static void showPointCloud(ImageSingleBand disparity, BufferedImage left,
									  Se3_F64 motion, DenseMatrix64F rectifiedK ,
									  int minDisparity, int maxDisparity) {
		PointCloudTiltPanel gui = new PointCloudTiltPanel();

		double baseline = motion.getT().norm();

		gui.configure(baseline, rectifiedK, new DoNothingTransform_F64(), minDisparity, maxDisparity);
		gui.process(disparity, left);
		gui.setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));

		ShowImages.showWindow(gui, "Point Cloud");
	}
}
