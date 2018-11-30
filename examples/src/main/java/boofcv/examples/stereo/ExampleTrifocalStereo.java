/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.abst.geo.TriangulateNViewsCalibrated;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
import boofcv.alg.sfm.structure.PruneStructureFromSceneMetric;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.*;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.*;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.examples.stereo.ExampleStereoTwoViewsOneCamera.rectifyImages;
import static boofcv.examples.stereo.ExampleStereoTwoViewsOneCamera.showPointCloud;

// TODO Example images with significant lens distortion. BRIO?

/**
 * @author Peter Abeles
 */
public class ExampleTrifocalStereo {

	public static void computeStereoCloud( GrayU8 distortedLeft, GrayU8 distortedRight ,
										   Planar<GrayU8> colorLeft, Planar<GrayU8> colorRight,
										   CameraPinholeRadial intrinsicLeft ,
										   CameraPinholeRadial intrinsicRight ,
										   Se3_F64 leftToRight ,
										   int minDisparity , int maxDisparity) {

//		drawInliers(origLeft, origRight, intrinsic, inliers);
		int width = distortedLeft.width;
		int height = distortedRight.height;

		// Rectify and remove lens distortion for stereo processing
		DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
		DMatrixRMaj rectifiedR = new DMatrixRMaj(3, 3);

		// rectify a colored image
		Planar<GrayU8> rectColorLeft = colorLeft.createSameShape();
		Planar<GrayU8> rectColorRight = colorLeft.createSameShape();
		rectifyImages(colorLeft, colorRight, leftToRight, intrinsicLeft,intrinsicRight,
				rectColorLeft, rectColorRight, rectifiedK,rectifiedR);

		GrayU8 rectifiedLeft = distortedLeft.createSameShape();
		GrayU8 rectifiedRight = distortedRight.createSameShape();
		ConvertImage.average(rectColorLeft,rectifiedLeft);
		ConvertImage.average(rectColorRight,rectifiedRight);

		// compute disparity
		StereoDisparity<GrayS16, GrayF32> disparityAlg =
				FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
						minDisparity, maxDisparity, 6, 6, 30, 3, 0.05, GrayS16.class);

		// Apply the Laplacian across the image to add extra resistance to changes in lighting or camera gain
		GrayS16 derivLeft = new GrayS16(width,height);
		GrayS16 derivRight = new GrayS16(width,height);
		LaplacianEdge.process(rectifiedLeft, derivLeft);
		LaplacianEdge.process(rectifiedRight,derivRight);

		// process and return the results
		disparityAlg.process(derivLeft, derivRight);
		GrayF32 disparity = disparityAlg.getDisparity();

		// show results
		BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);

		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectColorLeft, new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB),true);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectColorRight, new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB),true);

		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification",true);
		ShowImages.showWindow(visualized, "Disparity",true);

		showPointCloud(disparity, outLeft, leftToRight, rectifiedK,rectifiedR, minDisparity, maxDisparity);
	}

	public static void main(String[] args) {
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rock_leaves_01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rock_leaves_02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rock_leaves_03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/chicken01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/chicken02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/chicken03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/books01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/books02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/books03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pumpkintop01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pumpkintop02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pumpkintop03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/triflowers01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/triflowers02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/triflowers03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pebbles01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pebbles02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pebbles03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/bobcats01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/bobcats02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/bobcats03.png"));

		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/deer01.png"));
		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/deer02.png"));
		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/deer03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/seal01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/seal02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/seal03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/puddle01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/puddle02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/puddle03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/barrel01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/barrel02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/barrel03.png"));

		// TODO bad focal length
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/power_01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/power_02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/power_03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/turkey01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/turkey02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/turkey03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/bowl_01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/bowl_02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/bowl_03.png"));

		// The bad rectification appears to be because of bad pose estimation

		// TODO Rectified shifted wrong direction
		//      transposing R fixes this one, plus giving a better estimate for f
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rockview01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rockview02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rockview03.png"));

//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/waterdrip01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/waterdrip02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/waterdrip03.png"));

		// TODO Rectified shifted wrong direction
		//      again transposing R
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/skull01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/skull02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/skull03.png"));

		// TODO Rectified shifted wrong direction
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/library01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/library02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/library03.png"));

		// TODO Rectified shifted wrong direction
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pelican01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pelican02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/pelican03.png"));

		// TODO Distorted Reconstruction
//		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/eggs01.png"));
//		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/eggs02.png"));
//		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/eggs03.png"));

		Planar<GrayU8> color01 = ConvertBufferedImage.convertFrom(buff01,true,ImageType.pl(3,GrayU8.class));
		Planar<GrayU8> color02 = ConvertBufferedImage.convertFrom(buff02,true,ImageType.pl(3,GrayU8.class));
		Planar<GrayU8> color03 = ConvertBufferedImage.convertFrom(buff03,true,ImageType.pl(3,GrayU8.class));

		GrayU8 image01 = ConvertImage.average(color01,null);
		GrayU8 image02 = ConvertImage.average(color02,null);
		GrayU8 image03 = ConvertImage.average(color03,null);

		// TODO don't use scale invariant and see how it goes
		DetectDescribePoint<GrayU8,BrightFeature> detDesc = FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(0, 4, 1000, 1, 9, 4, 2), null,null, GrayU8.class);

		FastQueue<Point2D_F64> locations01 = new FastQueue<>(Point2D_F64.class,true);
		FastQueue<Point2D_F64> locations02 = new FastQueue<>(Point2D_F64.class,true);
		FastQueue<Point2D_F64> locations03 = new FastQueue<>(Point2D_F64.class,true);

		FastQueue<BrightFeature> features01 = UtilFeature.createQueue(detDesc,100);
		FastQueue<BrightFeature> features02 = UtilFeature.createQueue(detDesc,100);
		FastQueue<BrightFeature> features03 = UtilFeature.createQueue(detDesc,100);

		detDesc.detect(image01);

		int width = image01.width, height = image01.height;
		System.out.println("Image Shape "+width+" x "+height);
		double cx = width/2;
		double cy = height/2;
//		double scale = Math.max(cx,cy);

		// COMMENT ON center point zero
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
//			locations01.grow().set((pixel.x-cx)/scale,(pixel.y-cy)/scale);
			locations01.grow().set(pixel.x-cx,pixel.y-cy);
			features01.grow().setTo(detDesc.getDescription(i));
		}
		detDesc.detect(image02);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
//			locations02.grow().set((pixel.x-cx)/scale,(pixel.y-cy)/scale);
			locations02.grow().set(pixel.x-cx,pixel.y-cy);
			features02.grow().setTo(detDesc.getDescription(i));
		}
		detDesc.detect(image03);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
//			locations03.grow().set((pixel.x-cx)/scale,(pixel.y-cy)/scale);
			locations03.grow().set(pixel.x-cx,pixel.y-cy);
			features03.grow().setTo(detDesc.getDescription(i));
		}

		System.out.println("features01.size = "+features01.size);
		System.out.println("features02.size = "+features02.size);
		System.out.println("features03.size = "+features03.size);

		ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
		AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 0.1, true);

		AssociateThreeByPairs<BrightFeature> associateThree = new AssociateThreeByPairs<>(associate,BrightFeature.class);

		associateThree.setFeaturesA(features01);
		associateThree.setFeaturesB(features02);
		associateThree.setFeaturesC(features03);

		associateThree.associate();

		System.out.println("Total Matched Triples = "+associateThree.getMatches().size);

		ConfigRansac configRansac = new ConfigRansac();
		configRansac.maxIterations = 500;
		configRansac.inlierThreshold = 2;

		ConfigTrifocal configTri = new ConfigTrifocal();
		configTri.error = ConfigTrifocal.ErrorModel.POINT_TRANSFER;

		Ransac<TrifocalTensor,AssociatedTriple> ransac = FactoryMultiViewRobust.trifocalRansac(configTri,configRansac);

		FastQueue<AssociatedTripleIndex> assoiatedIdx = associateThree.getMatches();
		FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple.class,true);
		for (int i = 0; i < assoiatedIdx.size; i++) {
			AssociatedTripleIndex p = assoiatedIdx.get(i);
			associated.grow().set(locations01.get(p.a),locations02.get(p.b),locations03.get(p.c));
		}
		ransac.process(associated.toList());

		List<AssociatedTriple> inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();
		System.out.println("Remaining after RANSAC "+inliers.size());
		model.print();

		// estimate using all the inliers
		// No need to re-scale the input because the estimator automatically adjusts the input on its own
		Estimate1ofTrifocalTensor trifocalEstimator = FactoryMultiView.trifocal_1(EnumTrifocal.LINEAR_7,-1);
		if( !trifocalEstimator.process(inliers,model) )
			throw new RuntimeException("Estimator failed");

		DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);
		MultiViewOps.extractCameraMatrices(model,P2,P3);

		// TODO things seem to go well until converted into P or F
		//  Notes: Trifocal transfer has a very small error, but conversion into camera matrix or fundamental seems
		//         to introduce large errors. Both P and F converge to same solutions
		//         If RANSAC is run with triangulation hardly any matches occur due to massive errors
		//  Recovering after finding P and F seems to be difficult because solutions are stuck in local minima
		//
		//  Noticed that when scaling pixels trifocal tensor produces very similar results, but P changed resulting
		//         in very different calibration homography
		SelfCalibrationLinearDualQuadratic selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
		selfcalib.addCameraMatrix(P1);
		selfcalib.addCameraMatrix(P2);
		selfcalib.addCameraMatrix(P3);

		GeometricResult result = selfcalib.solve();
		if(GeometricResult.SOLVE_FAILED == result)
			throw new RuntimeException("Egads "+result);

		List<CameraPinhole> listPinhole = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			Intrinsic c = selfcalib.getSolutions().get(i);
//			c.fx = c.fy = 720;
			CameraPinhole p = new CameraPinhole(c.fx,c.fy,0,0,0,width,height);
			listPinhole.add(p);
		}

		// refine doesn't do very much
//		System.out.println("Refining auto calib");
//		SelfCalibrationRefineDualQuadratic refineDual = new SelfCalibrationRefineDualQuadratic();
//		refineDual.setZeroPrinciplePoint(true);
//		refineDual.setFixedAspectRatio(true);
//		refineDual.setZeroSkew(true);
//		refineDual.addCameraMatrix(P1);
//		refineDual.addCameraMatrix(P2);
//		refineDual.addCameraMatrix(P3);
//
//		if( !refineDual.refine(listPinhole,selfcalib.getQ()) )
//			throw new RuntimeException("Refine failed!");

		List<Intrinsic> calibration = selfcalib.getSolutions();
		for (int i = 0; i < 3; i++) {
			Intrinsic c = calibration.get(i);
			System.out.println("init   fx="+c.fx+" fy="+c.fy+" skew="+c.skew);
			CameraPinhole r = listPinhole.get(i);
			System.out.println("refine fx="+r.fx+" fy="+r.fy+" skew="+r.skew);
		}

		System.out.println("Projective to metric");
		// convert camera matrix from projective to metric
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		if( !MultiViewOps.computeRectifyingHomography(selfcalib.getQ(),H) )
			throw new RuntimeException("Projective to metric failed");

		DMatrixRMaj K = new DMatrixRMaj(3,3);
		List<Se3_F64> worldToView = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			worldToView.add( new Se3_F64());
		}

		// ignore K since we already have that
		MultiViewOps.projectiveToMetric(P1,H,worldToView.get(0),K);
		MultiViewOps.projectiveToMetric(P2,H,worldToView.get(1),K);
		MultiViewOps.projectiveToMetric(P3,H,worldToView.get(2),K);

//		K.print();
//		// use the fact that K is already known
//		PerspectiveOps.pinholeToMatrix(listPinhole.get(0),K);
//		projectiveToMetricKnownK(K,P1,H,worldToView.get(0));
//		PerspectiveOps.pinholeToMatrix(listPinhole.get(1),K);
//		projectiveToMetricKnownK(K,P2,H,worldToView.get(1));
//		PerspectiveOps.pinholeToMatrix(listPinhole.get(2),K);
//		projectiveToMetricKnownK(K,P3,H,worldToView.get(2));

		// scale is arbitrary. Set max translation to 1
		double maxT = 0;
		for( Se3_F64 p : worldToView ) {
			maxT = Math.max(maxT,p.T.norm());
		}
		for( Se3_F64 p : worldToView ) {
			p.T.scale(1.0/maxT);
			p.print();
		}

		// TODO is there a bug and its choosing this solution or are there really two possible?
		// TODO change it so that the bottom on the photo is +y
		// there are two possible solution. force it to be the one I want it to be
		if( worldToView.get(1).T.x > 0 ) {
			for (int i = 1; i < 3; i++) {
				worldToView.get(i).T.scale(-1);
			}
		}

		// Triangulate points in 3D in metric space
		List<Point3D_F64> points3D = new ArrayList<>();
		TriangulateNViewsCalibrated triangulation = FactoryMultiView.triangulateCalibratedNViewDLT();

		List<Point2D_F64> normObs = new ArrayList<>();
		for (int i = 0; i < worldToView.size(); i++) {
			normObs.add( new Point2D_F64());
		}
		for (int i = 0; i < inliers.size(); i++) {
			AssociatedTriple t = inliers.get(i);
			PerspectiveOps.convertPixelToNorm(listPinhole.get(0),t.p1,normObs.get(0));
			PerspectiveOps.convertPixelToNorm(listPinhole.get(1),t.p2,normObs.get(1));
			PerspectiveOps.convertPixelToNorm(listPinhole.get(2),t.p3,normObs.get(2));

			Point3D_F64 X = new Point3D_F64();
			triangulation.triangulate(normObs,worldToView,X);
			points3D.add(X);
		}

		// Construct bundle adjustment data structure
		SceneStructureMetric structure = new SceneStructureMetric(false);
		SceneObservations observations = new SceneObservations(3);

		structure.initialize(3,3,inliers.size());
		for (int i = 0; i < listPinhole.size(); i++) {
			CameraPinhole cp = listPinhole.get(i);
			BundlePinholeSimplified bp = new BundlePinholeSimplified();

			bp.f = cp.fx;

			structure.setCamera(i,false,bp);
			structure.setView(i,i==0,worldToView.get(i));
			structure.connectViewToCamera(i,i);
		}
		for (int i = 0; i < inliers.size(); i++) {
			AssociatedTriple t = inliers.get(i);
			Point3D_F64 X = points3D.get(i);

			observations.getView(0).add(i,(float)t.p1.x,(float)t.p1.y);
			observations.getView(1).add(i,(float)t.p2.x,(float)t.p2.y);
			observations.getView(2).add(i,(float)t.p3.x,(float)t.p3.y);

			structure.setPoint(i,X.x,X.y,X.z);
			structure.connectPointToView(i,0);
			structure.connectPointToView(i,1);
			structure.connectPointToView(i,2);
		}

		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = false;
		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		// Create and configure the bundle adjustment solver
		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleAdjustmentMetric(configSBA);
		// prints out useful debugging information that lets you know how well it's converging
		bundleAdjustment.setVerbose(System.out,0);
		// Specifies convergence criteria
		bundleAdjustment.configure(1e-6, 1e-6, 100);

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure,observations);
		pruner.pruneObservationsByErrorRank(0.7);
		pruner.pruneViews(10);
		pruner.prunePoints(1);

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		System.out.println("\nCamera");
		for (int i = 0; i < structure.cameras.length; i++) {
			System.out.println(structure.cameras[i].getModel().toString());
		}
		System.out.println("\n\nworldToView");
		for (int i = 0; i < structure.views.length; i++) {
			System.out.println(structure.views[i].worldToView.toString());
		}

		// TODO take initial structure estimate and reconsider all points. See if it can be improved

		System.out.println("\n\nComputing Stereo Disparity");
		BundlePinholeSimplified cp = structure.getCameras()[0].getModel();
		CameraPinholeRadial intrinsic01 = new CameraPinholeRadial();
		intrinsic01.fsetK(cp.f,cp.f,0,cx,cy,width,height);
		intrinsic01.fsetRadial(cp.k1,cp.k2);

		cp = structure.getCameras()[1].getModel();
		CameraPinholeRadial intrinsic02 = new CameraPinholeRadial();
		intrinsic02.fsetK(cp.f,cp.f,0,cx,cy,width,height);
		intrinsic02.fsetRadial(cp.k1,cp.k2);

		Se3_F64 leftToRight = structure.views[1].worldToView;

//		GrayU8 scaled01 = new GrayU8(width/2,height/2);
//		GrayU8 scaled02 = new GrayU8(width/2,height/2);
//		Planar<GrayU8> scolor01 = new Planar<>(GrayU8.class,scaled01.width,scaled01.height,3);
//		Planar<GrayU8> scolor02 = new Planar<>(GrayU8.class,scaled01.width,scaled01.height,3);
//
//		PerspectiveOps.scaleIntrinsic(intrinsic01,0.5);
//		PerspectiveOps.scaleIntrinsic(intrinsic02,0.5);
//		new FDistort(image01,scaled01).scale().apply();
//		new FDistort(image02,scaled02).scale().apply();
//		new FDistort(color01,scolor01).scale().apply();
//		new FDistort(color02,scolor02).scale().apply();
//		image01 = scaled01;image02 = scaled02;color01 = scolor01; color02 = scolor02;

		// TODO dynamic max disparity
		computeStereoCloud(image01,image02,color01,color02,intrinsic01,intrinsic02,leftToRight,0,250);
	}

	public static double computeReprojection( List<CameraPinhole> cameras,
											  List<Se3_F64> listWorldToView ,
											  List<AssociatedTriple> listObs )
	{
		double error = 0;
		TriangulateNViewsCalibrated triangulation = FactoryMultiView.triangulateCalibratedNViewDLT();
//		RefineTriangulationMetric refine = FactoryMultiView.triangulateRefine(1e-20,100);

		List<Point2D_F64> normObs = new ArrayList<>();
		for (int i = 0; i < listWorldToView.size(); i++) {
			normObs.add( new Point2D_F64());
		}

//		Point3D_F64 XX = new Point3D_F64();
		Point3D_F64 X = new Point3D_F64();
		Point3D_F64 Y = new Point3D_F64();
		Point2D_F64 pixel = new Point2D_F64();
		for (int i = listObs.size()-1; i >= 0; i--) {
			AssociatedTriple t = listObs.get(i);
			for (int j = 0; j < listWorldToView.size(); j++) {
				PerspectiveOps.convertPixelToNorm(cameras.get(j),t.get(j),normObs.get(j));
			}

			triangulation.triangulate(normObs,listWorldToView,X);
//			refine.process(normObs,listWorldToView,XX,X);


			for (int j = 0; j < listWorldToView.size(); j++) {
				listWorldToView.get(j).transform(X,Y);
				PerspectiveOps.convertNormToPixel(cameras.get(j),Y.x/Y.z,Y.y/Y.z,pixel);
				double e = t.get(j).distance(pixel);
				error += e;
			}
		}

		// make the units easier to understand
		return error/(listObs.size()*3);
	}

	public static List<Point3D_F64> triangulate( Se3_F64 a_to_b , DMatrixRMaj KA , DMatrixRMaj KB ,
												 List<Point2D_F64> pixelsA , List<Point2D_F64> pixelsB)
	{
		if( pixelsA.size() != pixelsB.size() )
			throw new IllegalArgumentException("Observation counts must match");

		int N = pixelsA.size();

		DMatrixRMaj KA_inv = new DMatrixRMaj(3,3);
		DMatrixRMaj KB_inv = new DMatrixRMaj(3,3);

		CommonOps_DDRM.invert(KA,KA_inv);
		CommonOps_DDRM.invert(KB,KB_inv);

		List<Point3D_F64> output = new ArrayList<>();

		Point2D_F64 na = new Point2D_F64();
		Point2D_F64 nb = new Point2D_F64();

		TriangulateTwoViewsCalibrated triangulator = FactoryMultiView.triangulateTwoGeometric();

		for (int i = 0; i < N; i++) {
			GeometryMath_F64.mult(KA_inv,pixelsA.get(i),na);
			GeometryMath_F64.mult(KB_inv,pixelsB.get(i),nb);

			Point3D_F64 X = new Point3D_F64();
			if( !triangulator.triangulate(na,nb,a_to_b,X) )
				throw new RuntimeException("Triangulation failed?!");

			output.add(X);
		}

		return output;
	}
}
