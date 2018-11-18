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
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.robust.DistanceTrifocalTransferSq;
import boofcv.alg.geo.robust.GenerateTrifocalTensor;
import boofcv.alg.geo.robust.ManagerTrifocalTensor;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
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
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

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

		DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);

		MultiViewOps.extractCameraMatrices(model,P2,P3);
		SelfCalibrationLinearDualQuadratic selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
		selfcalib.addCameraMatrix(P1);
		selfcalib.addCameraMatrix(P2);
		selfcalib.addCameraMatrix(P3);

		GeometricResult result = selfcalib.solve();
		if(GeometricResult.SOLVE_FAILED == result)
			throw new RuntimeException("Egads "+result);

		List<Intrinsic> calibration = selfcalib.getSolutions();
		for( Intrinsic c : calibration ) {
			System.out.println("fx="+c.fx+" fy="+c.fy+" skew="+c.skew);
		}

		// change it from projective to metric

		// TODO estimate point 3D locations

		// TODO set up Bundle Adjustment

		// Estimate camera parameters with the assumption that the camera has a 60 degree FOV
		CameraPinhole intrisic = PerspectiveOps.createIntrinsic(image01.width,image01.height,60);

		SceneStructureMetric structure = new SceneStructureMetric(false);
		structure.initialize(1,3,inliers.size());


		// TODO estimate calibration

		// TODO Bundle adjustment

		// TODO Stereo
	}
}
