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

import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.Tuple2;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("ALL") public abstract class GenericBundleAdjustmentMetricChecks extends BoofStandardJUnit {
	public abstract BundleAdjustment<SceneStructureMetric> createAlg();

	@Test void horizontalPerfect() {
		BundleAdjustment<SceneStructureMetric> alg = createAlg();

		Tuple2<SceneStructureMetric, SceneObservations> a = createHorizontalMotion( 123,true);

		alg.setParameters(a.d0,a.d1);
		alg.optimize(a.d0); // don't assertTrue() since it can fail

		Tuple2<SceneStructureMetric, SceneObservations> b = createHorizontalMotion( 123,true);
		assertEquals(a.d0,b.d0,1e-6,1e-6,1e-6);
	}

	/**
	 * Same solution when called multiple times in a row. Checks to see if it is correctly reset
	 */
	@Test void multipleCalls() {
		BundleAdjustment<SceneStructureMetric> alg = createAlg();

		Tuple2<SceneStructureMetric, SceneObservations> a = createHorizontalMotion( 123,true);
		Tuple2<SceneStructureMetric, SceneObservations> c = createHorizontalMotion( 234,true);
		addNoiseToPoint3D(c);
		alg.setParameters(a.d0,a.d1);
		alg.optimize(c.d0);
		alg.setParameters(a.d0,a.d1);
		alg.optimize(a.d0);

		Tuple2<SceneStructureMetric, SceneObservations> b = createHorizontalMotion( 123,true);
		assertEquals(a.d0,b.d0,1e-6,1e-6,1e-6);
	}

	@Test void horizontalNoisyObs() {
		BundleAdjustment<SceneStructureMetric> alg = createAlg();

		Tuple2<SceneStructureMetric, SceneObservations> a = createHorizontalMotion( 123,true);

		// Add noise to every observation
		SceneObservations observations = a.d1;
		for (int i = 0; i < observations.views.size; i++) {
			SceneObservations.View v = observations.views.data[i];
			for (int j = 0; j < v.point.size; j++) {
				v.observations.data[j*2+0] += (float) rand.nextGaussian()*0.1f;
				v.observations.data[j*2+1] += (float) rand.nextGaussian()*0.1f;
			}
		}

		alg.setParameters(a.d0,a.d1);
		assertTrue(alg.optimize(a.d0));

		Tuple2<SceneStructureMetric, SceneObservations> b = createHorizontalMotion( 123,true);
		assertEquals(a.d0,b.d0,1e-6,0.01,0.01);
	}

	@Test void horizontalNoisyFeatures() {
		BundleAdjustment<SceneStructureMetric> alg = createAlg();

		Tuple2<SceneStructureMetric, SceneObservations> a = createHorizontalMotion( 123,true);

		// Add noise to every 3D point
		addNoiseToPoint3D(a);

		alg.setParameters(a.d0,a.d1);
		assertTrue(alg.optimize(a.d0));

		// Since reprojection errors are perfect it should do a very good job reducing the residuals
		checkReprojectionError(a.d0,a.d1,1e-4);

		// even though observations are perfect it might have only converged towards a locally optimal solution thats
		// close to the optimal one
		Tuple2<SceneStructureMetric, SceneObservations> b = createHorizontalMotion( 123,true);
		assertEquals(a.d0,b.d0,0,0.1,1e-3);
	}

	private void addNoiseToPoint3D(Tuple2<SceneStructureMetric, SceneObservations> a) {
		SceneStructureMetric structure = a.d0;
		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point p = structure.points.data[i];
			p.coordinate[0] += rand.nextGaussian()*0.1;
			p.coordinate[1] += rand.nextGaussian()*0.1;
			p.coordinate[2] += rand.nextGaussian()*0.1;
		}
	}

	@Test void horizontalNoisyPose() {
		BundleAdjustment<SceneStructureMetric> alg = createAlg();

		Tuple2<SceneStructureMetric, SceneObservations> a = createHorizontalMotion( 123,true);

		// Add noise to every view pose estimate. Except 0 since that's the world coordinates and it's easier to check
		// errors if that's unmolested
		SceneStructureMetric structure = a.d0;
		for (int i = 1; i < structure.views.size; i++) {
			Se3_F64 parent_to_view = structure.getParentToView(i);
			parent_to_view.T.x += rand.nextGaussian()*0.1;
			parent_to_view.T.y += rand.nextGaussian()*0.1;
			parent_to_view.T.z += rand.nextGaussian()*0.1;
		}

		alg.setParameters(a.d0,a.d1);
		assertTrue(alg.optimize(a.d0));

		// Since reprojection errors are perfect it should do a very good job reducing the residuals
		checkReprojectionError(a.d0,a.d1,1e-4);

		// even though observations are perfect it might have only converged towards a locally optimal solution thats
		// close to the optimal one
		Tuple2<SceneStructureMetric, SceneObservations> b = createHorizontalMotion( 123,true);
		assertEquals(a.d0,b.d0,1e-6,0.1,1e-3);
	}

	public static void checkReprojectionError(SceneStructureMetric structure , SceneObservations observations , double tol ) {

		BundlePinhole c = (BundlePinhole)structure.cameras.get(0).model;

		// making a bunch of assumptions here...
		CameraPinhole intrinsic = new CameraPinhole(c.fx,c.fy,c.skew,c.cx,c.cy,600,600);

		WorldToCameraToPixel wcp = new WorldToCameraToPixel();

		PointIndex2D_F64 o = new PointIndex2D_F64();
		Point2D_F64 predicted = new Point2D_F64();

		if( structure.homogenous ) {
			Point4D_F64 p4 = new Point4D_F64();
			Point3D_F64 p3 = new Point3D_F64();
			for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
				SceneObservations.View v = observations.views.data[viewIndex];
				Se3_F64 parent_to_view = structure.getParentToView(viewIndex);

				wcp.configure(intrinsic, parent_to_view);
				for (int j = 0; j < v.point.size; j++) {
					v.getPixel(j, o);
					structure.points.data[o.index].get(p4);
					p3.x = p4.x/p4.w;
					p3.y = p4.y/p4.w;
					p3.z = p4.z/p4.w;
					wcp.transform(p3, predicted);
					double residual = o.p.distance(predicted);
					if (Math.abs(residual) > tol)
						fail("Error is too large. " + residual);
				}
			}
		} else {
			Point3D_F64 p3 = new Point3D_F64();
			for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
				SceneObservations.View v = observations.views.data[viewIndex];
				Se3_F64 parent_to_view = structure.getParentToView(viewIndex);

				wcp.configure(intrinsic, parent_to_view);
				for (int j = 0; j < v.point.size; j++) {
					v.getPixel(j, o);
					structure.points.data[o.index].get(p3);
					wcp.transform(p3, predicted);
					double residual = o.p.distance(predicted);
					if (Math.abs(residual) > tol)
						fail("Error is too large. " + residual);
				}
			}
		}
	}

	public static void assertEquals(SceneStructureMetric a , SceneStructureMetric b ,
									double tolCamera , double tolDistance , double tolRotation  ) {

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

		for (int i = 0; i < a.motions.size; i++) {
			double error = a.motions.data[i].motion.T.distance(b.motions.data[i].motion.T);
			assertTrue( error < tolDistance );
			assertTrue(MatrixFeatures_DDRM.isIdentical(a.motions.data[i].motion.R,
					b.motions.data[i].motion.R,tolRotation));
		}

	}

	public Tuple2<SceneStructureMetric, SceneObservations>
	createHorizontalMotion( long seed , boolean cameraFixed )
	{
		Random rand = new Random(seed);

		int width = 600;
		CameraPinhole intrinsic = new CameraPinhole(400,400,0,300,300,width,width);

		int numViews = 5;
		int numFeatures = 250;

		SceneStructureMetric structure = new SceneStructureMetric(false);
		structure.initialize(1,numViews,numFeatures);

		SceneObservations observations = new SceneObservations();
		observations.initialize(numViews);

		structure.setCamera(0,cameraFixed,intrinsic);

		double minX = 0, maxX = 5;
		for (int i = 0; i < numViews; i++) {
			Se3_F64 worldToView = new Se3_F64();
			worldToView.T.x = -((maxX-minX)*i/(numViews-1) + minX);
			structure.setView(i, 0, i==0,worldToView);
		}

		WorldToCameraToPixel wcp = new WorldToCameraToPixel();
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
					Se3_F64 parent_to_view = structure.getParentToView(viewIndex);
					wcp.configure(intrinsic, parent_to_view);
					wcp.transform(P, pixel);
					if (pixel.x >= 0 && pixel.x < width && pixel.y >= 0 && pixel.y < width) {
						count++;
					}
				}
				if (count >= 2) {
					for (int viewIndex = 0; viewIndex < numViews; viewIndex++) {
						Se3_F64 parent_to_view = structure.getParentToView(viewIndex);
						wcp.configure(intrinsic, parent_to_view);
						wcp.transform(P, pixel);
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
