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
import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.geo.TriangulateTwoViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.radtan.RemoveRadialPtoN_F64;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
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

		if(rectifiedK.get(0,0) < 0)
			throw new RuntimeException("Egads");

		System.out.println("Rectified K");
		rectifiedK.print();

		System.out.println("Rectified R");
		rectifiedR.print();

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
//		String name = "rock_leaves_";
//		String name = "chicken";
//		String name = "books";
//		String name = "triflowers";
//		String name = "pebbles";
		String name = "bobcats";
//		String name = "deer";
//		String name = "seal"; // TODO really confusing perspective
//		String name = "puddle";
//		String name = "barrel";
//		String name = "rockview";
//		String name = "waterdrip";
//		String name = "skull";
//		String name = "library";
//		String name = "power_";
		// TODO bad focal length
//		String name = "pumpkintop";
//		String name = "turkey";
//		String name = "bowl_";
//		String name = "eggs";
//		String name = "pelican"; // TODO really confusing perspective

		BufferedImage buff01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/"+name+"01.png"));
		BufferedImage buff02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/"+name+"02.png"));
		BufferedImage buff03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/"+name+"03.png"));

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
		configRansac.inlierThreshold = 1;

		ConfigTrifocal configTri = new ConfigTrifocal();
		ConfigTrifocalError configError = new ConfigTrifocalError();
		configError.model = ConfigTrifocalError.Model.REPROJECTION;

		Ransac<TrifocalTensor,AssociatedTriple> ransac =
				FactoryMultiViewRobust.trifocalRansac(configTri,configError,configRansac);

		FastQueue<AssociatedTripleIndex> associatedIdx = associateThree.getMatches();
		FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple.class,true);
		for (int i = 0; i < associatedIdx.size; i++) {
			AssociatedTripleIndex p = associatedIdx.get(i);
			associated.grow().set(locations01.get(p.a),locations02.get(p.b),locations03.get(p.c));
		}
		ransac.process(associated.toList());

		List<AssociatedTriple> inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();
		System.out.println("Remaining after RANSAC "+inliers.size());

		// estimate using all the inliers
		// No need to re-scale the input because the estimator automatically adjusts the input on its own
		configTri.which = EnumTrifocal.ALGEBRAIC_7;
		configTri.converge.maxIterations = 100;
		Estimate1ofTrifocalTensor trifocalEstimator = FactoryMultiView.trifocal_1(configTri);
		if( !trifocalEstimator.process(inliers,model) )
			throw new RuntimeException("Estimator failed");
		model.print();

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

			observations.getView(0).add(i,(float)t.p1.x,(float)t.p1.y);
			observations.getView(1).add(i,(float)t.p2.x,(float)t.p2.y);
			observations.getView(2).add(i,(float)t.p3.x,(float)t.p3.y);

			structure.connectPointToView(i,0);
			structure.connectPointToView(i,1);
			structure.connectPointToView(i,2);
		}
		// Initial estimate for point 3D locations
		triangulatePoints(structure,observations);

		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = false;
		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		// Create and configure the bundle adjustment solver
		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleAdjustmentMetric(configSBA);
		// prints out useful debugging information that lets you know how well it's converging
//		bundleAdjustment.setVerbose(System.out,0);
		// Specifies convergence criteria
		bundleAdjustment.configure(1e-6, 1e-6, 100);

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		checkBehindCamera(structure, observations, bundleAdjustment);

		double bestScore = bundleAdjustment.getFitScore();
		List<Se3_F64> best = new ArrayList<>();
		for (int i = 0; i < structure.views.length; i++) {
			best.add(structure.views[i].worldToView.copy());
		}

		for (int i = 0; i < structure.cameras.length; i++) {
			BundlePinholeSimplified c = structure.cameras[i].getModel();
			c.f = selfcalib.getSolutions().get(i).fx;
			c.k1 = c.k2 = 0;
		}
		// flip rotation assuming that it was done wrong
		for (int i = 1; i < structure.views.length; i++) {
			CommonOps_DDRM.transpose(structure.views[i].worldToView.R);
		}
		triangulatePoints(structure,observations);

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		checkBehindCamera(structure, observations, bundleAdjustment);

		// revert to old settings
		System.out.println(" ORIGINAL / NEW = " + bestScore+" / "+bundleAdjustment.getFitScore());
		if( bundleAdjustment.getFitScore() > bestScore ) {
			for (int i = 0; i < structure.cameras.length; i++) {
				BundlePinholeSimplified c = structure.cameras[i].getModel();
				c.f = selfcalib.getSolutions().get(i).fx;
				c.k1 = c.k2 = 0;
			}
			for (int i = 0; i < structure.views.length; i++) {
				structure.views[i].worldToView.set(best.get(i));
			}
			triangulatePoints(structure,observations);
			bundleAdjustment.setParameters(structure,observations);
			bundleAdjustment.optimize(structure);
		}

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

	// TODO Do this correction without running bundle adjustment again
	private static Point3D_F64 checkBehindCamera(SceneStructureMetric structure, SceneObservations observations, BundleAdjustment<SceneStructureMetric> bundleAdjustment) {

		int totalBehind = 0;
		Point3D_F64 X = new Point3D_F64();
		for (int i = 0; i < structure.points.length; i++) {
			structure.points[i].get(X);
			if( X.z < 0 )
				totalBehind++;
		}
		structure.views[1].worldToView.T.print();
		if( totalBehind > structure.points.length/2 ) {
			System.out.println("Flipping because it's reversed. score = "+bundleAdjustment.getFitScore());
			for (int i = 1; i < structure.views.length; i++) {
				Se3_F64 w2v = structure.views[i].worldToView;
				w2v.set(w2v.invert(null));
			}
			triangulatePoints(structure,observations);

			bundleAdjustment.setParameters(structure,observations);
			bundleAdjustment.optimize(structure);
			System.out.println("  after = "+bundleAdjustment.getFitScore());
		} else {
			System.out.println("Points not behind camera. "+totalBehind+" / "+structure.points.length);
		}
		return X;
	}

	public static void triangulatePoints( SceneStructureMetric structure , SceneObservations observations )
	{
		TriangulateNViewsMetric triangulation = FactoryMultiView.triangulateNViewCalibrated(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));

		List<RemoveRadialPtoN_F64> list_p_to_n = new ArrayList<>();
		for (int i = 0; i < structure.cameras.length; i++) {
			BundlePinholeSimplified cam = (BundlePinholeSimplified)structure.cameras[i].model;
			RemoveRadialPtoN_F64 p2n = new RemoveRadialPtoN_F64();
			p2n.setK(cam.f,cam.f,0,0,0).setDistortion(new double[]{cam.k1,cam.k2},0,0);
			list_p_to_n.add(p2n);

		}

		FastQueue<Point2D_F64> normObs = new FastQueue<>(Point2D_F64.class,true);
		normObs.resize(3);

		Point3D_F64 X = new Point3D_F64();

		List<Se3_F64> worldToViews = new ArrayList<>();
		for (int i = 0; i < structure.points.length; i++) {
			normObs.reset();
			worldToViews.clear();
			SceneStructureMetric.Point sp = structure.points[i];
			for (int j = 0; j < sp.views.size; j++) {
				int viewIdx = sp.views.get(j);
				SceneStructureMetric.View v = structure.views[viewIdx];
				worldToViews.add(v.worldToView);

				// get the observation in pixels
				Point2D_F64 n = normObs.grow();
				int pointidx = observations.views[viewIdx].point.indexOf(i);
				observations.views[viewIdx].get(pointidx,n);
				// convert to normalized image coordinates
				list_p_to_n.get(v.camera).compute(n.x,n.y,n);
			}

			triangulation.triangulate(normObs.toList(),worldToViews,X);
			sp.set(X.x,X.y,X.z);
		}
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

		TriangulateTwoViewsMetric triangulator = FactoryMultiView.triangulateTwoViewMetric(
				new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));

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
