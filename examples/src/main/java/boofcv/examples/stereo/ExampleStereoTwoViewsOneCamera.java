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

import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.cloud.DisparityToColorPointCloud;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.robust.ModelMatcherMultiview;
import boofcv.examples.sfm.ExampleComputeFundamentalMatrix;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.geo.ConfigEssential;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.gui.d3.UtilDisparitySwing;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Example demonstrating how to use to images taken from a single calibrated camera to create a stereo disparity image,
 * from which a dense 3D point cloud of the scene can be computed. For this technique to work the camera's motion
 * needs to be approximately tangential to the direction the camera is pointing. The code below assumes that the first
 * image is to the left of the second image.
 *
 * @author Peter Abeles
 */
public class ExampleStereoTwoViewsOneCamera {

	// Specifies the disparity values which will be considered
	private static final int disparityMin = 15;
	private static final int disparityRange = 85;

	public static void main( String[] args ) {
		// specify location of images and calibration
		String calibDir = UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/");
		String imageDir = UtilIO.pathExample("stereo/");

		// Camera parameters
		CameraPinholeBrown intrinsic = CalibrationIO.load(new File(calibDir, "intrinsic.yaml"));

		// Input images from the camera moving left to right
		BufferedImage origLeft = UtilImageIO.loadImage(imageDir, "mono_wall_01.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(imageDir, "mono_wall_02.jpg");

		// Input images with lens distortion
		GrayU8 distortedLeft = ConvertBufferedImage.convertFrom(origLeft, (GrayU8)null);
		GrayU8 distortedRight = ConvertBufferedImage.convertFrom(origRight, (GrayU8)null);

		// matched features between the two images
		List<AssociatedPair> matchedFeatures = ExampleComputeFundamentalMatrix.computeMatches(origLeft, origRight);

		// convert from pixel coordinates into normalized image coordinates
		List<AssociatedPair> matchedCalibrated = convertToNormalizedCoordinates(matchedFeatures, intrinsic);

		// Robustly estimate camera motion
		List<AssociatedPair> inliers = new ArrayList<>();
		Se3_F64 leftToRight = estimateCameraMotion(intrinsic, matchedCalibrated, inliers);

		drawInliers(origLeft, origRight, intrinsic, inliers);

		// Rectify and remove lens distortion for stereo processing
		DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
		DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);
		GrayU8 rectifiedLeft = distortedLeft.createSameShape();
		GrayU8 rectifiedRight = distortedRight.createSameShape();
		GrayU8 rectifiedMask = distortedLeft.createSameShape();

		rectifyImages(distortedLeft, distortedRight, leftToRight, intrinsic, intrinsic,
				rectifiedLeft, rectifiedRight, rectifiedMask, rectifiedK, rectifiedR);

		// compute disparity
		ConfigDisparityBMBest5 config = new ConfigDisparityBMBest5();
		config.errorType = DisparityError.CENSUS;
		config.disparityMin = disparityMin;
		config.disparityRange = disparityRange;
		config.subpixel = true;
		config.regionRadiusX = config.regionRadiusY = 5;
		config.maxPerPixelError = 20;
		config.validateRtoL = 1;
		config.texture = 0.1;
		StereoDisparity<GrayU8, GrayF32> disparityAlg =
				FactoryStereoDisparity.blockMatchBest5(config, GrayU8.class, GrayF32.class);

		// process and return the results
		disparityAlg.process(rectifiedLeft, rectifiedRight);
		GrayF32 disparity = disparityAlg.getDisparity();
		RectifyImageOps.applyMask(disparity, rectifiedMask, 0);

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, disparityRange, 0);

		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectifiedLeft, null);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectifiedRight, null);

		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification", true);
		ShowImages.showWindow(visualized, "Disparity", true);

		showPointCloud(disparity, outLeft, leftToRight, rectifiedK, rectifiedR, disparityMin, disparityRange);

		System.out.println("Total found " + matchedCalibrated.size());
		System.out.println("Total Inliers " + inliers.size());
	}

	/**
	 * Estimates the camera motion robustly using RANSAC and a set of associated points.
	 *
	 * @param intrinsic Intrinsic camera parameters
	 * @param matchedNorm set of matched point features in normalized image coordinates
	 * @param inliers OUTPUT: Set of inlier features from RANSAC
	 * @return Found camera motion. Note translation has an arbitrary scale
	 */
	public static Se3_F64 estimateCameraMotion( CameraPinholeBrown intrinsic,
												List<AssociatedPair> matchedNorm, List<AssociatedPair> inliers ) {
		ModelMatcherMultiview<Se3_F64, AssociatedPair> epipolarMotion =
				FactoryMultiViewRobust.baselineRansac(new ConfigEssential(), new ConfigRansac(200, 0.5));
		epipolarMotion.setIntrinsic(0, intrinsic);
		epipolarMotion.setIntrinsic(1, intrinsic);

		if (!epipolarMotion.process(matchedNorm))
			throw new RuntimeException("Motion estimation failed");

		// save inlier set for debugging purposes
		inliers.addAll(epipolarMotion.getMatchSet());

		return epipolarMotion.getModelParameters();
	}

	/**
	 * Convert a set of associated point features from pixel coordinates into normalized image coordinates.
	 */
	public static List<AssociatedPair> convertToNormalizedCoordinates( List<AssociatedPair> matchedFeatures, CameraPinholeBrown intrinsic ) {

		Point2Transform2_F64 p_to_n = LensDistortionFactory.narrow(intrinsic).undistort_F64(true, false);

		List<AssociatedPair> calibratedFeatures = new ArrayList<>();

		for (AssociatedPair p : matchedFeatures) {
			AssociatedPair c = new AssociatedPair();

			p_to_n.compute(p.p1.x, p.p1.y, c.p1);
			p_to_n.compute(p.p2.x, p.p2.y, c.p2);

			calibratedFeatures.add(c);
		}

		return calibratedFeatures;
	}

	/**
	 * Remove lens distortion and rectify stereo images
	 *
	 * @param distortedLeft Input distorted image from left camera.
	 * @param distortedRight Input distorted image from right camera.
	 * @param leftToRight Camera motion from left to right
	 * @param intrinsicLeft Intrinsic camera parameters
	 * @param rectifiedLeft Output rectified image for left camera.
	 * @param rectifiedRight Output rectified image for right camera.
	 * @param rectifiedMask Mask that indicates invalid pixels in rectified image. 1 = valid, 0 = invalid
	 * @param rectifiedK Output camera calibration matrix for rectified camera
	 */
	public static <T extends ImageBase<T>>
	void rectifyImages( T distortedLeft,
						T distortedRight,
						Se3_F64 leftToRight,
						CameraPinholeBrown intrinsicLeft,
						CameraPinholeBrown intrinsicRight,
						T rectifiedLeft,
						T rectifiedRight,
						GrayU8 rectifiedMask,
						DMatrixRMaj rectifiedK,
						DMatrixRMaj rectifiedR ) {
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

		// original camera calibration matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsicLeft, (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsicRight, (DMatrixRMaj)null);

		rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

		// rectification matrix for each image
		DMatrixRMaj rect1 = rectifyAlg.getUndistToRectPixels1();
		DMatrixRMaj rect2 = rectifyAlg.getUndistToRectPixels2();
		rectifiedR.setTo(rectifyAlg.getRectifiedRotation());

		// New calibration matrix,
		rectifiedK.setTo(rectifyAlg.getCalibrationMatrix());

		// Adjust the rectification to make the view area more useful
		ImageDimension rectShape = new ImageDimension();
		RectifyImageOps.fullViewLeft(intrinsicLeft, rect1, rect2, rectifiedK, rectShape);
//		RectifyImageOps.allInsideLeft(intrinsicLeft, rect1, rect2, rectifiedK, rectShape);
		// Taking in account the relative rotation between the image axis and the baseline is important in
		// this scenario since a person can easily hold the camera at an odd angle. If you don't adjust
		// the rectified image size you might end up with a lot of wasted pixels and a low resolution model!
		rectifiedLeft.reshape(rectShape.width, rectShape.height);
		rectifiedRight.reshape(rectShape.width, rectShape.height);

		// undistorted and rectify images
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3);
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		// Extending the image prevents a harsh edge reducing false matches at the image border
		// SKIP is another option, possibly a tinny bit faster, but has a harsh edge which will need to be filtered
		ImageDistort<T, T> distortLeft =
				RectifyDistortImageOps.rectifyImage(intrinsicLeft, rect1_F32, BorderType.EXTENDED, distortedLeft.getImageType());
		ImageDistort<T, T> distortRight =
				RectifyDistortImageOps.rectifyImage(intrinsicRight, rect2_F32, BorderType.EXTENDED, distortedRight.getImageType());

		distortLeft.apply(distortedLeft, rectifiedLeft, rectifiedMask);
		distortRight.apply(distortedRight, rectifiedRight);
	}

	/**
	 * Draw inliers for debugging purposes. Need to convert from normalized to pixel coordinates.
	 */
	public static void drawInliers( BufferedImage left, BufferedImage right, CameraPinholeBrown intrinsic,
									List<AssociatedPair> normalized ) {
		Point2Transform2_F64 n_to_p = LensDistortionFactory.narrow(intrinsic).distort_F64(false, true);

		List<AssociatedPair> pixels = new ArrayList<>();

		for (AssociatedPair n : normalized) {
			AssociatedPair p = new AssociatedPair();

			n_to_p.compute(n.p1.x, n.p1.y, p.p1);
			n_to_p.compute(n.p2.x, n.p2.y, p.p2);

			pixels.add(p);
		}

		// display the results
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(pixels);
		panel.setImages(left, right);

		ShowImages.showWindow(panel, "Inlier Features", true);
	}

	/**
	 * Show results as a point cloud
	 */
	public static void showPointCloud( ImageGray disparity, BufferedImage left,
									   Se3_F64 motion, DMatrixRMaj rectifiedK, DMatrixRMaj rectifiedR,
									   int disparityMin, int disparityRange ) {
		DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
		PointCloudWriter.CloudArraysF32 cloud = new PointCloudWriter.CloudArraysF32();

		double baseline = motion.getT().norm();
		d2c.configure(baseline, rectifiedK, rectifiedR, new DoNothing2Transform2_F64(), disparityMin, disparityRange);
		d2c.process(disparity, UtilDisparitySwing.wrap(left), cloud);

		CameraPinhole rectifiedPinhole = PerspectiveOps.matrixToPinhole(rectifiedK, disparity.width, disparity.height, null);

		// skew the view to make the structure easier to see
		Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(-baseline*5, 0, 0, 0, 0.2, 0, null);

		PointCloudViewer pcv = VisualizeData.createPointCloudViewer();
		pcv.setCameraHFov(PerspectiveOps.computeHFov(rectifiedPinhole));
		pcv.setCameraToWorld(cameraToWorld);
		pcv.setTranslationStep(baseline/3);
		pcv.addCloud(cloud.cloudXyz, cloud.cloudRgb);
		pcv.setDotSize(1);
		pcv.setTranslationStep(baseline/10);

		pcv.getComponent().setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));
		ShowImages.showWindow(pcv.getComponent(), "Point Cloud", true);
	}
}
