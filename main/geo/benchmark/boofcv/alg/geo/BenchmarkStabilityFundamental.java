/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.f.EstimateNto1ofEpipolar;
import boofcv.alg.geo.f.DistanceEpipolarConstraint;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.GeoModelEstimatorN;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkStabilityFundamental {

	Random rand = new Random(265);

	double sigmaPixels = 0;

	int totalPoints = 100;
	double sceneCenterZ = 3;
	double sceneRadius = 2;

	List<Point3D_F64> scene;
	List<AssociatedPair> observations;

	// create a reasonable calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,-200,0,80,-150,0,0,1);
	DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
	// relationship between both camera
	protected Se3_F64 motion;

	// are observations in pixels or normalized image coordinates
	public static boolean isPixels = false;

	List<Double> scores;

	public BenchmarkStabilityFundamental() {
		CommonOps.invert(K, K_inv);
	}

	public void createSceneCube() {
		scene = new ArrayList<>();

		for( int i = 0; i < totalPoints; i++ ) {
			Point3D_F64 p = new Point3D_F64();

			p.x = (rand.nextDouble()-0.5)*2*sceneRadius;
			p.y = (rand.nextDouble()-0.5)*2*sceneRadius;
			p.z = (rand.nextDouble()-0.5)*2*sceneRadius+sceneCenterZ;

			scene.add(p);
		}
	}

	public void createScenePlane() {
		scene = new ArrayList<>();

		for( int i = 0; i < totalPoints; i++ ) {
			Point3D_F64 p = new Point3D_F64();

			p.x = (rand.nextDouble()-0.5)*2*sceneRadius;
			p.y = (rand.nextDouble()-0.5)*2*sceneRadius;
			p.z = sceneCenterZ;

			scene.add(p);
		}
	}

	public void motionTranslate() {
		motion = new Se3_F64();
		motion.getT().set(0.2,0,0);
	}

	public void motionTransRot() {
		motion = new Se3_F64();
		motion.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.01,-0.02,0.05,null));
		motion.getT().set(0.2,0,0);
	}

	public void createObservations() {
		observations = new ArrayList<>();

		for( Point3D_F64 p1 : scene ) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			if( p1.z < 0 || p2.z < 0 )
				continue;

			AssociatedPair pair = new AssociatedPair();
			pair.p1.set(p1.x/p1.z,p1.y/p1.z);
			pair.p2.set(p2.x/p2.z,p2.y/p2.z);
			observations.add(pair);

			// convert to pixels
			GeometryMath_F64.mult(K, pair.p1, pair.p1);
			GeometryMath_F64.mult(K, pair.p2,pair.p2);

			// add noise
			pair.p1.x += rand.nextGaussian()*sigmaPixels;
			pair.p1.y += rand.nextGaussian()*sigmaPixels;
			pair.p2.x += rand.nextGaussian()*sigmaPixels;
			pair.p2.y += rand.nextGaussian()*sigmaPixels;

			// if needed, convert back into normalized image coordinates
			if( !isPixels ) {
				GeometryMath_F64.mult(K_inv, pair.p1, pair.p1);
				GeometryMath_F64.mult(K_inv, pair.p2,pair.p2);
			}
		}
	}

	public void evaluateMinimal( GeoModelEstimatorN<DenseMatrix64F,AssociatedPair> estimatorN ) {

		DistanceEpipolarConstraint distance = new DistanceEpipolarConstraint();

		Estimate1ofEpipolar estimator =
				new EstimateNto1ofEpipolar(estimatorN,distance,1);

		scores = new ArrayList<>();
		int failed = 0;

		int numSamples = estimator.getMinimumPoints();

		Random rand = new Random(234);

		DenseMatrix64F F = new DenseMatrix64F(3,3);

		for( int i = 0; i < 50; i++ ) {
			List<AssociatedPair> pairs = new ArrayList<>();

			// create a unique set of pairs
			while( pairs.size() < numSamples) {
				AssociatedPair p = observations.get( rand.nextInt(observations.size()) );

				if( !pairs.contains(p) ) {
					pairs.add(p);
				}
			}

			if( !estimator.process(pairs,F) ) {
				failed++;
				continue;
			}

			// normalize the scale of F
			CommonOps.scale(1.0/CommonOps.elementMaxAbs(F),F);

			double totalScore = 0;
			// score against all observations
			for( AssociatedPair p : observations ) {
				double score = Math.abs(GeometryMath_F64.innerProd(p.p2, F, p.p1));
				if( Double.isNaN(score))
					System.out.println("Score is NaN");
				scores.add(score);
				totalScore += score;
			}
//			System.out.println("  score["+i+"] = "+totalScore);
		}

		Collections.sort(scores);

		System.out.printf(" Failures %3d  Score:  50%% = %6.3e  95%% = %6.3e\n", failed, scores.get(scores.size() / 2), scores.get((int) (scores.size() * 0.95)));
	}

	public void evaluateAll( GeoModelEstimator1<DenseMatrix64F,AssociatedPair> estimator ) {
		scores = new ArrayList<>();
		int failed = 0;

		DenseMatrix64F F = new DenseMatrix64F(3,3);

		for( int i = 0; i < 50; i++ ) {

			if( !estimator.process(observations,F) ) {
				failed++;
				continue;
			}

			// normalize the scale of F
			CommonOps.scale(1.0/CommonOps.elementMaxAbs(F),F);

			// score against all observations
			for( AssociatedPair p : observations ) {
				double score = Math.abs(GeometryMath_F64.innerProd(p.p2, F, p.p1));
				if( Double.isNaN(score))
					System.out.println("Score is NaN");
				scores.add(score);
			}
		}

		Collections.sort(scores);

		System.out.printf(" Failures %3d  Score:  50%% = %6.3e  95%% = %6.3e\n", failed, scores.get(scores.size() / 2), scores.get((int) (scores.size() * 0.95)));
	}

	public static void main( String args[] ) {
		BenchmarkStabilityFundamental app = new BenchmarkStabilityFundamental();

		app.createSceneCube();
//		app.createScenePlane();
		app.motionTranslate();
		app.createObservations();
		if( isPixels ){
			app.evaluateMinimal(FactoryMultiView.computeFundamental_N(EnumEpipolar.FUNDAMENTAL_8_LINEAR));
			app.evaluateMinimal(FactoryMultiView.computeFundamental_N(EnumEpipolar.FUNDAMENTAL_7_LINEAR));
		} else {
			app.evaluateMinimal(FactoryMultiView.computeFundamental_N(EnumEpipolar.ESSENTIAL_8_LINEAR));
			app.evaluateMinimal(FactoryMultiView.computeFundamental_N(EnumEpipolar.ESSENTIAL_7_LINEAR));
			app.evaluateMinimal(FactoryMultiView.computeFundamental_N(EnumEpipolar.ESSENTIAL_5_NISTER));
		}

//		app.evaluateMinimal(FactoryMultiView.computeFundamental(8));


//		app.evaluateAll(FactoryMultiView.computeEssential(8));
//		app.evaluateAll(FactoryMultiView.computeEssential(5));
	}
}
