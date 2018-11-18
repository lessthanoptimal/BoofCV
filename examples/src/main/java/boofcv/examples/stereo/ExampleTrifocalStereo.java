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
import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.abst.geo.TriangulateNViewsCalibrated;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.robust.DistanceTrifocalTransferSq;
import boofcv.alg.geo.robust.GenerateTrifocalTensor;
import boofcv.alg.geo.robust.ManagerTrifocalTensor;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.EnumTrifocal;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ExampleTrifocalStereo {
	public static void main(String[] args) {
		GrayU8 image01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rock_leaves_01.png"),GrayU8.class);
		GrayU8 image02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rock_leaves_02.png"),GrayU8.class);
		GrayU8 image03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/rock_leaves_03.png"),GrayU8.class);

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

		System.out.println("Image Shape "+image01.width+" x "+image01.height);
		double cx = image01.width/2;
		double cy = image01.height/2;

		// COMMENT ON center point zero
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
			locations01.grow().set(pixel.x-cx,pixel.y-cy);
			features01.grow().setTo(detDesc.getDescription(i));
		}
		detDesc.detect(image02);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
			locations02.grow().set(pixel.x-cx,pixel.y-cy);
			features02.grow().setTo(detDesc.getDescription(i));
		}
		detDesc.detect(image03);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
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

		double fitTol = 10;
		int totalIterations = 2_000;

		Estimate1ofTrifocalTensor trifocal = FactoryMultiView.trifocal_1(EnumTrifocal.LINEAR_7,0);
		ModelManager<TrifocalTensor> manager = new ManagerTrifocalTensor();
		ModelGenerator<TrifocalTensor,AssociatedTriple> generator = new GenerateTrifocalTensor(trifocal);
		DistanceFromModel<TrifocalTensor,AssociatedTriple> distance = new DistanceTrifocalTransferSq();
		Ransac<TrifocalTensor,AssociatedTriple> ransac = new Ransac<>(123123,manager,generator,distance,totalIterations,2*fitTol*fitTol);

		FastQueue<AssociatedTripleIndex> assoiatedIdx = associateThree.getMatches();
		FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple.class,true);
		for (int i = 0; i < assoiatedIdx.size; i++) {
			AssociatedTripleIndex p = assoiatedIdx.get(i);

			associated.grow().set(locations01.get(p.a),locations02.get(p.b),locations03.get(p.c));
		}
		ransac.setSampleSize(12);
		ransac.process(associated.toList());

		List<AssociatedTriple> inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();
		System.out.println("Remaining after RANSAC "+inliers.size());

		// estimate using all the inliers
		generator.generate(inliers,model);

		System.out.println("Estimating calib");

		DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);

		MultiViewOps.extractCameraMatrices(model,P2,P3);
		SelfCalibrationLinearDualQuadratic selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
		selfcalib.addCameraMatrix(P1);
		selfcalib.addCameraMatrix(P2);
		selfcalib.addCameraMatrix(P3);

		System.out.println("Refining auto calib");
		GeometricResult result = selfcalib.solve();
		if(GeometricResult.SOLVE_FAILED == result)
			throw new RuntimeException("Egads "+result);

		List<CameraPinhole> listPinhole = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			Intrinsic c = selfcalib.getSolutions().get(i);
			CameraPinhole p = new CameraPinhole(c.fx,c.fy,0,0,0,image01.width,image01.height);
			listPinhole.add(p);
		}

		// TODO figure out what's wrong with refine
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

		// convert camera matrix from projective to metric
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		PerspectiveOps.decomposeAbsDualQuadratic(selfcalib.getQ(),H);

		DMatrixRMaj K = new DMatrixRMaj(3,3);
		List<Se3_F64> worldToView = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			worldToView.add( new Se3_F64());
		}
		// ignore K since we already have that
		MultiViewOps.projectiveToMetric(P1,H,worldToView.get(0),K);
		MultiViewOps.projectiveToMetric(P2,H,worldToView.get(1),K);
		MultiViewOps.projectiveToMetric(P3,H,worldToView.get(2),K);

		// scale is arbitrary. Set max translation to 1
		double maxT = 0;
		for( Se3_F64 p : worldToView ) {
			maxT = Math.max(maxT,p.T.norm());
		}
		for( Se3_F64 p : worldToView ) {
			p.T.scale(1.0/maxT);
//			p.print();
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
			structure.setView(i,false,worldToView.get(i));
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

		System.out.println("\nCamera");
		for (int i = 0; i < structure.cameras.length; i++) {
			System.out.println(structure.cameras[i].getModel().toString());
		}
		System.out.println("\n\nworldToView");
		for (int i = 0; i < structure.cameras.length; i++) {
			System.out.println(structure.views[i].worldToView.toString());
		}


	}
}
