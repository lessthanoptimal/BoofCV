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

package boofcv.abst.geo.bundle;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.Tuple2;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
abstract class GenericBundleAdjustmentProjectiveChecks extends BoofStandardJUnit {
	abstract BundleAdjustment<SceneStructureProjective> createAlg();

	@Test
	void horizontalPerfect() {
		BundleAdjustment<SceneStructureProjective> alg = createAlg();

		Tuple2<SceneStructureProjective, SceneObservations> a = createHorizontalMotion( 123);

		alg.setParameters(a.d0,a.d1);
		alg.optimize(a.d0); // don't assertTrue() since it can fail

		Tuple2<SceneStructureProjective, SceneObservations> b = createHorizontalMotion( 123);
		assertEquals(a.d0,b.d0,1e-6);
	}

	/**
	 * Same solution when called multiple times in a row. Checks to see if it is correctly reset
	 */
	@Test
	void multipleCalls() {
		BundleAdjustment<SceneStructureProjective> alg = createAlg();

		Tuple2<SceneStructureProjective, SceneObservations> a = createHorizontalMotion( 123);
		Tuple2<SceneStructureProjective, SceneObservations> c = createHorizontalMotion( 234);
		addNoiseToPoint3D(c);
		alg.setParameters(a.d0,a.d1);
		alg.optimize(c.d0);
		alg.setParameters(a.d0,a.d1);
		alg.optimize(a.d0);

		Tuple2<SceneStructureProjective, SceneObservations> b = createHorizontalMotion( 123);
		assertEquals(a.d0,b.d0,1e-6);
	}

	@Test
	void horizontalNoisyObs() {
		BundleAdjustment<SceneStructureProjective> alg = createAlg();

		Tuple2<SceneStructureProjective, SceneObservations> a = createHorizontalMotion( 123);

		// Add noise to every observation
		SceneObservations observations = a.d1;
		for (int i = 0; i < observations.views.size; i++) {
			SceneObservations.View v = observations.views.data[i];
			for (int j = 0; j < v.point.size; j++) {
				v.observations.data[j*2+0] += (float) (rand.nextGaussian()*0.1);
				v.observations.data[j*2+1] += (float) (rand.nextGaussian()*0.1);
			}
		}

		alg.setParameters(a.d0,a.d1);
		assertTrue(alg.optimize(a.d0));

		Tuple2<SceneStructureProjective, SceneObservations> b = createHorizontalMotion( 123);
		assertEquals(a.d0,b.d0,0.02);
	}

	@Test
	void horizontalNoisyFeatures() {
		BundleAdjustment<SceneStructureProjective> alg = createAlg();

		Tuple2<SceneStructureProjective, SceneObservations> a = createHorizontalMotion( 123);

		// Add noise to every 3D point
		addNoiseToPoint3D(a);

		alg.setParameters(a.d0,a.d1);
		assertTrue(alg.optimize(a.d0));

		// Since reprojection errors are perfect it should do a very good job reducing the residuals
		checkReprojectionError(a.d0,a.d1,1e-4);

		// even though observations are perfect it might have only converged towards a locally optimal solution thats
		// close to the optimal one
		Tuple2<SceneStructureProjective, SceneObservations> b = createHorizontalMotion( 123);
		assertEquals(a.d0,b.d0,0.1);
	}

	private void addNoiseToPoint3D(Tuple2<SceneStructureProjective, SceneObservations> a) {
		SceneStructureCommon structure = a.d0;
		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point p = structure.points.data[i];
			p.coordinate[0] += rand.nextGaussian()*0.1;
			p.coordinate[1] += rand.nextGaussian()*0.1;
			p.coordinate[2] += rand.nextGaussian()*0.1;
		}
	}

	@Test
	void horizontalNoisyPose() {
		BundleAdjustment<SceneStructureProjective> alg = createAlg();

		Tuple2<SceneStructureProjective, SceneObservations> a = createHorizontalMotion( 123);

		// Add noise to every view pose estimate. Except 0 since that's the world coordinates and it's easier to check
		// errors if that's unmolested
		SceneStructureProjective structure = a.d0;
		for (int i = 1; i < structure.views.size; i++) {
			SceneStructureProjective.View v = structure.views.data[i];
			v.worldToView.data[3 ] += rand.nextGaussian()*0.1;
			v.worldToView.data[7 ] += rand.nextGaussian()*0.1;
			v.worldToView.data[11] += rand.nextGaussian()*0.1;
		}

		alg.setParameters(a.d0,a.d1);
		assertTrue(alg.optimize(a.d0));

		// Since reprojection errors are perfect it should do a very good job reducing the residuals
		checkReprojectionError(a.d0,a.d1,1e-4);

		// even though observations are perfect it might have only converged towards a locally optimal solution thats
		// close to the optimal one
		Tuple2<SceneStructureProjective, SceneObservations> b = createHorizontalMotion( 123);
		assertEquals(a.d0,b.d0,0.1);
	}

	static void checkReprojectionError(SceneStructureProjective structure , SceneObservations observations , double tol ) {


		PointIndex2D_F64 o = new PointIndex2D_F64();
		Point2D_F64 predicted = new Point2D_F64();

		if( structure.homogenous ) {
			Point4D_F64 p4 = new Point4D_F64();
			Point3D_F64 p3 = new Point3D_F64();
			for (int indexView = 0; indexView < observations.views.size; indexView++) {
				SceneObservations.View v = observations.views.data[indexView];

				for (int j = 0; j < v.point.size; j++) {
					v.getPixel(j, o);
					structure.points.data[o.index].get(p4);
					p3.x = p4.x/p4.w;
					p3.y = p4.y/p4.w;
					p3.z = p4.z/p4.w;
					PerspectiveOps.renderPixel(structure.views.data[indexView].worldToView,p3,predicted);
					double residual = o.p.distance(predicted);
					if (Math.abs(residual) > tol)
						fail("Error is too large. " + residual);
				}
			}
		} else {
			Point3D_F64 p3 = new Point3D_F64();
			for (int indexView = 0; indexView < observations.views.size; indexView++) {
				SceneObservations.View v = observations.views.data[indexView];

				for (int j = 0; j < v.point.size; j++) {
					v.getPixel(j, o);
					structure.points.data[o.index].get(p3);
					PerspectiveOps.renderPixel(structure.views.data[indexView].worldToView,p3,predicted);
					double residual = o.p.distance(predicted);
					if (Math.abs(residual) > tol)
						fail("Error is too large. " + residual);
				}
			}
		}
	}

	static void assertEquals(SceneStructureProjective a , SceneStructureProjective b ,
									double tolDistance ) {

		Assertions.assertEquals(a.homogenous, b.homogenous);

		if( a.homogenous ) {
//			Point4D_F64 pa = new Point4D_F64();
//			Point4D_F64 pb = new Point4D_F64();

			for (int i = 0; i < a.points.size; i++) {
				// need to normalize the points first otherwise they can't be computed
				a.points.data[i].normalizeH();
				b.points.data[i].normalizeH();
				double error = a.points.data[i].distance(b.points.data[i]);
				assertTrue( error < tolDistance);
			}
		} else {
			for (int i = 0; i < a.points.size; i++) {
				double error = a.points.data[i].distance(b.points.data[i]);
				assertTrue( error < tolDistance);
			}
		}

		// don't compare camera matrices because there are infinite equivalent solutions
	}

	Tuple2<SceneStructureProjective, SceneObservations> createHorizontalMotion( long seed )
	{
		Random rand = new Random(seed);

		int width = 600;
		CameraPinhole intrinsic = new CameraPinhole(400,400,0,300,300,width,width);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic,(DMatrixRMaj)null);
		int numViews = 5;
		int numFeatures = 200;

		SceneStructureProjective structure = new SceneStructureProjective(false);
		SceneObservations observations = new SceneObservations();

		observations.initialize(numViews);
		structure.initialize(numViews,numFeatures);

		double minX = 0, maxX = 5;
		for (int i = 0; i < numViews; i++) {
			Se3_F64 worldToView = new Se3_F64();
			worldToView.T.x = -((maxX-minX)*i/(numViews-1) + minX);

			DMatrixRMaj P = new DMatrixRMaj(3,4);
			PerspectiveOps.createCameraMatrix(worldToView.R,worldToView.T,K,P);

			structure.setView(i,i==0,P,width,width);
		}

		Point2D_F64 pixel = new Point2D_F64();

		for (int featureIndex = 0; featureIndex < numFeatures; featureIndex++) {
			// Run until it finds a location where it's visible in at least two views
			while( true ) {
				Point3D_F64 P = new Point3D_F64();
				P.x = (maxX - minX) * rand.nextDouble() + minX;
				P.y = rand.nextGaussian() / 3;
				P.z = rand.nextGaussian() / 10 + maxX / 3;

				structure.setPoint(featureIndex, P.x, P.y, P.z);

				// see which views it's visible in
				int count = 0;
				for (int viewIndex = 0; viewIndex < numViews; viewIndex++) {
					SceneStructureProjective.View v = structure.views.data[viewIndex];
					PerspectiveOps.renderPixel(v.worldToView,P,pixel);
					if (pixel.x >= 0 && pixel.x < width && pixel.y >= 0 && pixel.y < width) {
						count++;
					}
				}
				if (count >= 2) {
					for (int viewIndex = 0; viewIndex < numViews; viewIndex++) {
						SceneStructureProjective.View v = structure.views.data[viewIndex];
						PerspectiveOps.renderPixel(v.worldToView,P,pixel);
						if (pixel.x >= 0 && pixel.x < width && pixel.y >= 0 && pixel.y < width) {
							observations.getView(viewIndex).add(featureIndex, (float) pixel.x, (float) pixel.y);
							structure.connectPointToView(featureIndex, viewIndex);
						}
					}
					break;
				}
			}
		}

		return new Tuple2<>(structure,observations);
	}
}
