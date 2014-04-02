/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import georegression.fitting.se.ModelManagerSe3_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.alg.dense.linsol.svd.SolvePseudoInverseSvd;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.robust.DistanceSe3SymmetricSq;
import boofcv.alg.sfm.robust.Se3FromEssentialGenerator;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.gui.d3.PointCloudTiltPanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.DoNothingTransform_F64;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;

/**
 * Example demonstrating how to use to images taken from a single calibrated camera to create a stereo disparity image,
 * from which a dense 3D point cloud of the scene can be computed.  For this technique to work the camera's motion
 * needs to be approximately tangential to the direction the camera is pointing.  The code below assumes that the first
 * image is to the left of the second image.
 *
 * @author Peter Abeles
 */
public class ExampleSfm {

	public static void main(String args[]) {
		// specify location of images and calibration
		String calibDir = "data/applet/sfm/";
		PointCloud pc = new PointCloud();

		// Camera parameters
		IntrinsicParameters intrinsic = BoofMiscOps.loadXML(calibDir + "intrinsic.xml");

		// Input images from the camera moving left to right
		BufferedImage first = UtilImageIO.loadImage(calibDir + "img_2150.jpg");
		BufferedImage second = UtilImageIO.loadImage(calibDir + "img_2151.jpg");
		BufferedImage third = UtilImageIO.loadImage(calibDir + "img_2152.jpg");

		// matched features between the two images
		List<AssociatedPair> matched12 = ExampleFundamentalMatrix.computeMatches(first, second);
		List<AssociatedPair> matched13 = ExampleFundamentalMatrix.computeMatches(first, third);
		List<AssociatedPair> matched23 = ExampleFundamentalMatrix.computeMatches(second, third);
		
		System.out.println(matched12.size() + " matches found between 1-2");
		System.out.println(matched23.size() + " matches found between 2-3");
		System.out.println(matched13.size() + " matches found between 1-3");
		
		// convert from pixel coordinates into normalized image coordinates
		List<AssociatedPair> matchedCal12 = convertToNormalizedCoordinates(matched12, intrinsic);
		List<AssociatedPair> matchedCal13 = convertToNormalizedCoordinates(matched13, intrinsic);
		List<AssociatedPair> matchedCal23 = convertToNormalizedCoordinates(matched23, intrinsic);

		// Robustly estimate camera motion
		List<AssociatedPair> inliers12 = new ArrayList<AssociatedPair>();
		Se3_F64 P1 = estimateCameraMotion(intrinsic, matchedCal12, inliers12);

		addFirstPoints( pc, matchedCal12, P1, intrinsic );

		drawInliers(first, second, intrinsic, inliers12);

		System.out.println("Total Inliers " + inliers12.size());
		System.out.println( pc.points );
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
		Estimate1ofEpipolar essentialAlg = FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_7_LINEAR, 2);
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
						8000, ransacTOL);

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
	
	private static void addFirstPoints( PointCloud pc, List<AssociatedPair> normCoords, Se3_F64 P1, IntrinsicParameters intrinsics ) 
	{
		List<Point2D_F64> pointsImg1 = new ArrayList<Point2D_F64>();
		List<Point2D_F64> pointsImg2 = new ArrayList<Point2D_F64>();
	
		for ( int i = 0; i < normCoords.size(); i++ ) {
			AssociatedPair cp = normCoords.get( i );
			pointsImg1.add( new Point2D_F64( cp.p1.x, cp.p1.y ) );
			pointsImg2.add( new Point2D_F64( cp.p2.x, cp.p2.y ) );
		}

		List<Point3D_F64> pointCloud = new ArrayList<Point3D_F64>();

		Se3_F64 P = new Se3_F64();
		P.R.set(0,0,1.0);
		P.R.set(1,1,1.0);
		P.R.set(2,2,1.0);
		
		DenseMatrix64F K = new DenseMatrix64F(3,3);
		PerspectiveOps.calibrationMatrix( intrinsics, K );
		double error = triangulatePoints( pointsImg1, pointsImg2, P, P1, K, pointCloud, intrinsics );
		
		System.out.println("calculated error: " + error);
		System.out.println(P);
		System.out.println(P1);
	}

	private static DenseMatrix64F linearLSTriangulation( Point3D_F64 u, Se3_F64 P, Point3D_F64 u1, Se3_F64 P1 )
	{
		int i = 0;
		DenseMatrix64F A = new DenseMatrix64F( 4, 3 );
		DenseMatrix64F B = new DenseMatrix64F( 4, 1 );
		DenseMatrix64F X = new DenseMatrix64F( 3, 1 );

		A.set(i++, u.x*P.R.get( 2, 0 ) - P.R.get(0,0) );
		A.set(i++, u.x*P.R.get( 2, 1 ) - P.R.get(0,1) );
		A.set(i++, u.x*P.R.get( 2, 2 ) - P.R.get(0,2) );
		A.set(i++, u.y*P.R.get( 2, 0 ) - P.R.get(1,0) );

		A.set(i++, u.y*P.R.get( 2, 1 ) - P.R.get(1,1) );
		A.set(i++, u.y*P.R.get( 2, 2 ) - P.R.get(1,2) );
		A.set(i++, u1.x*P1.R.get( 2, 0 ) - P1.R.get(0,0) );
		A.set(i++, u1.x*P1.R.get( 2, 1 ) - P1.R.get(0,1) );

		A.set(i++, u1.x*P1.R.get( 2, 2 ) - P1.R.get(0,2) );
		A.set(i++, u1.y*P1.R.get( 2, 0 ) - P1.R.get(1,0) );
		A.set(i++, u1.y*P1.R.get( 2, 1 ) - P1.R.get(1,1) );
		A.set(i++, u1.y*P1.R.get( 2, 2 ) - P1.R.get(1,2) );

		i = 0;
		B.set(i++,0, -(u.x*P.T.z - P.T.x) );
		B.set(i++,0, -(u.y*P.T.z - P.T.y) );
		B.set(i++,0, -(u1.x*P1.T.z - P1.T.x) );
		B.set(i++,0, -(u1.y*P1.T.z - P1.T.y) );
		
		//solve for X
		SolvePseudoInverseSvd svd = new SolvePseudoInverseSvd(3, 4);
		svd.setA( A );
		svd.solve( B, X );
		
		return X;
	}
	
	private static double triangulatePoints( List<Point2D_F64> pt_set1, List<Point2D_F64> pt_set2,
			Se3_F64 P, Se3_F64 P1, DenseMatrix64F K, List<Point3D_F64> pointCloud, IntrinsicParameters intrinsics )
	{
		List<Double> reproj_error = new ArrayList<Double>();
		DenseMatrix64F xPt_img = new DenseMatrix64F(3,1);
		DenseMatrix64F im = new DenseMatrix64F(3,4);
		PointTransform_F64 tran2 = LensDistortionOps.transformNormToRadial_F64( intrinsics );
		Point2D_F64 orig = new Point2D_F64();
		
		for ( int i = 0; i < pt_set1.size(); i++) {
			//convert to normalized homogeneous coordinates
			Point3D_F64 u = new Point3D_F64( pt_set1.get( i ).x, pt_set1.get( i ).y, 1.0 );
			Point3D_F64 u1 = new Point3D_F64( pt_set2.get( i ).x, pt_set2.get( i ).y, 1.0 );
			
			// triangulate
			DenseMatrix64F X = linearLSTriangulation(u,P,u1,P1);
			X.reshape( 4, 1, true );
			X.set( 3, 1.0 );
	
			//calculate reprojection error
			im = PerspectiveOps.createCameraMatrix(P1.R, P1.T, K, null);
			CommonOps.mult( im, X, xPt_img );
	
			tran2.compute( u1.x,  u1.y, orig );
			
			Point2D_F64 xPt_img_ = new Point2D_F64( xPt_img.get(0)/xPt_img.get(2), xPt_img.get(1)/xPt_img.get(2) );
	
			System.out.println( norm( xPt_img_, orig ) );
			
			reproj_error.add( norm( xPt_img_, orig ) );
			
			//store 3D point
			pointCloud.add(new Point3D_F64(X.get(0,0),X.get(1,0),X.get(2,0)));
		}
		
		double me = 0.0f;
		for (int i = 0; i < reproj_error.size(); i++ ) {
			me += reproj_error.get( i ).doubleValue();
		}
		me /= reproj_error.size();
		return me;
	}

	private static double norm( Point2D_F64 p1, Point2D_F64 p2 ) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt( dx*dx + dy*dy );
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
