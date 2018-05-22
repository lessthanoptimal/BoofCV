/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.struct.calib.CameraModel;
import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class GenericCalibrationZhang99<CM extends CameraModel>
{
	protected Random rand = new Random(234);

	/**
	 * Create a set of observations from a known grid, give it the observations and see if it can
	 * reconstruct the known parameters.
	 */
	@Test
	public void fullTest() {
		fullTest(false);
		fullTest(true);
	}

	public void fullTest( boolean partial ) {
		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();
		for( Zhang99IntrinsicParam intrinsic : createParameters(rand) ) {
//			System.out.println("Full Test : partial = "+partial);

			Zhang99AllParam expected = GenericCalibrationGrid.createStandardParam(intrinsic, 15, rand);
			List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(expected, grid);

			if (partial) {
				for (int i = 0; i < observations.size(); i++) {
					CalibrationObservation o = observations.get(i);
					for (int j = 0; j < 5; j++) {
						o.points.remove(rand.nextInt(o.points.size()));
					}
				}
			}

			CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(grid, intrinsic.createLike());

			assertTrue(alg.process(observations));

			Zhang99AllParam found = alg.getOptimized();

			double after = GenericCalibrationGrid.computeErrors(expected,grid,found);

			assertTrue(after < 0.001 );

//			checkIntrinsicOnly(
//					(CM) expected.getIntrinsic().getCameraModel(),
//					(CM) found.getIntrinsic().getCameraModel(), 0.02, 0.1, 0.1);
		}
	}

	/**
	 * See how well it computes an initial guess at the parameters given perfect inputs
	 */
	@Test
	public void linearEstimate() {

		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();
		for( Zhang99IntrinsicParam intrinsic : createParametersForLinearTest(rand) ) {
			Zhang99AllParam expected = GenericCalibrationGrid.createStandardParam(intrinsic,3,rand);
			List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(expected, grid);

			CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(grid, intrinsic.createLike());

			Zhang99AllParam found = expected.createLike();
			alg.linearEstimate(observations, found);

			checkIntrinsicOnly(
					(CM) expected.getIntrinsic().getCameraModel(),
					(CM) found.getIntrinsic().getCameraModel(), 0.01, 0.1, 0.1);
		}
	}

	/**
	 * Test nonlinear optimization with perfect inputs
	 */
	@Test
	public void optimizedParam_perfect() {

		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();

		for( Zhang99IntrinsicParam intrinsic : createParameters(rand) ) {
			Zhang99AllParam initial = GenericCalibrationGrid.createStandardParam(intrinsic,8,rand);
			Zhang99AllParam found = initial.createLike();

			List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(initial,grid);

			CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(grid, intrinsic.createLike());
			assertTrue(alg.optimizedParam(observations, grid, initial.copy(), found,null));

			double after = GenericCalibrationGrid.computeErrors(initial,grid,found);
			assertTrue(after < 1e-12 );

//			checkEquals(initial, found, initial);
		}
	}

	/**
	 * Standard testing parameters. Should be solvable with non-linear refinement.
	 */
	public abstract List<Zhang99IntrinsicParam> createParameters(Random rand);

	/**
	 * These parameters are intended to be easy for the linear estimator to estimate.
	 */
	public abstract List<Zhang99IntrinsicParam> createParametersForLinearTest(Random rand);

	/**
	 * Test nonlinear optimization with a bit of noise
	 */
	@Test
	public void optimizedParam_noisy() {

		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();

		for( Zhang99IntrinsicParam intrinsic : createParameters(rand) ) {
//			System.out.println("***** noisy");
			Zhang99AllParam initial = GenericCalibrationGrid.createStandardParam(intrinsic,6, rand);
			Zhang99AllParam expected = initial.copy();
			Zhang99AllParam found = initial.createLike();

			List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(initial, grid);

			// add a tinny bit of noise
			addNoise((CM) initial.getIntrinsic().getCameraModel(), 0.01);
			initial.getIntrinsic().forceProjectionUpdate();

			double before = GenericCalibrationGrid.computeErrors(expected,grid,initial);

			CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(grid, intrinsic.createLike());
			assertTrue(alg.optimizedParam(observations, grid, initial, found, null));

			double after = GenericCalibrationGrid.computeErrors(expected,grid,found);
			assertTrue(after*0.0001 < before);
		}
	}

	public abstract void addNoise( CM param , double magnitude );

	@Test
	public void applyDistortion() {
		Point2D_F64 n = new Point2D_F64(0.05,-0.1);
		double radial[] = new double[]{0.1};
		double t1 = 0.034,t2 = 0.34;

		Point2D_F64 distorted = new Point2D_F64();

		double r2 = n.x*n.x + n.y*n.y;
		distorted.x = n.x + radial[0]*r2*n.x + 2*t1*n.x*n.y + t2*(r2 + 2*n.x*n.x);
		distorted.y = n.y + radial[0]*r2*n.y + t1*(r2 + 2*n.y*n.y) + 2*t2*n.x*n.y;

		CalibrationPlanarGridZhang99.applyDistortion(n,radial,t1,t2);

		assertEquals(distorted.x, n.x, 1e-8);
		assertEquals(distorted.y, n.y, 1e-8);
	}

	protected abstract void checkIntrinsicOnly(CM expected,
											   CM found ,
											   double tolK , double tolD , double tolT );

	public void checkEquals( Zhang99AllParam expected ,
							 Zhang99AllParam found ,
							 Zhang99AllParam initial )
	{
		checkEquals(
				(CM)expected.getIntrinsic().getCameraModel(),
				(CM)found.getIntrinsic().getCameraModel(),
				(CM)initial.getIntrinsic().getCameraModel(),0.3);

		double pixelTol=0.5;

		for( int i = 0; i < 2; i++ ) {
			Zhang99AllParam.View pp = expected.views[i];
			Zhang99AllParam.View ff = found.views[i];

			GeometryUnitTest.assertEquals(pp.T, ff.T, pixelTol);
			GeometryUnitTest.assertEquals(pp.rotation.unitAxisRotation,ff.rotation.unitAxisRotation,pixelTol);
			assertEquals(pp.rotation.theta,ff.rotation.theta,pixelTol);
		}
	}

	public abstract void checkEquals( CM expected ,  CM found , CM initial , double tol );
}
