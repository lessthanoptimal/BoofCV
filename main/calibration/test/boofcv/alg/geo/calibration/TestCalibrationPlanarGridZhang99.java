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

package boofcv.alg.geo.calibration;

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
public class TestCalibrationPlanarGridZhang99 {

	Random rand = new Random(234);

	/**
	 * Create a set of observations from a known grid, give it the observations and see if it can
	 * reconstruct the known parameters.
	 */
	@Test
	public void fullTest() {
		PlanarCalibrationTarget config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.points;
		Zhang99Parameters expected = GenericCalibrationGrid.createStandardParam(true,2,3,rand);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(expected,grid);

		CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(config,expected.assumeZeroSkew,2);

		assertTrue(alg.process(observations));

		Zhang99Parameters found = alg.getOptimized();

		checkIntrinsicOnly(expected, found,0.01,0.1);
	}

	/**
	 * See how well it computes an initial guess at the parameters given perfect inputs
	 */
	@Test
	public void initialParam() {
		PlanarCalibrationTarget config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.points;
		Zhang99Parameters initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		Helper alg = new Helper(config,true,2);

		Zhang99Parameters found = alg.initialParam(observations);

		checkIntrinsicOnly(initial, found,0.01,0.1);
	}

	/**
	 * Test nonlinear optimization with perfect inputs
	 */
	@Test
	public void optimizedParam_perfect() {

		PlanarCalibrationTarget config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.points;
		Zhang99Parameters initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);
		Zhang99Parameters found = new Zhang99Parameters(true,2,3);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(config,true,2);
		assertTrue(alg.optimizedParam(observations, grid, initial, found,null));

		checkEquals(initial, found,initial);
	}

	/**
	 * Test nonlinear optimization with a bit of noise
	 */
	@Test
	public void optimizedParam_noisy() {

		PlanarCalibrationTarget config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.points;
		Zhang99Parameters initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);
		Zhang99Parameters expected = initial.copy();
		Zhang99Parameters found = new Zhang99Parameters(true,2,3);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		// add a tinny bit of noise
		initial.a += rand.nextDouble()*0.01*Math.abs(initial.a);
		initial.b += rand.nextDouble()*0.01*Math.abs(initial.b);
		initial.c += rand.nextDouble()*0.01*Math.abs(initial.c);
		initial.x0 += rand.nextDouble()*0.01*Math.abs(initial.x0);
		initial.y0 += rand.nextDouble()*0.01*Math.abs(initial.y0);

		for( int i = 0; i < expected.distortion.length; i++ ) {
			initial.distortion[i] = rand.nextGaussian()*expected.distortion[i]*0.1;
		}

		CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(config,true,2);
		assertTrue(alg.optimizedParam(observations, grid, initial, found,null));

		checkEquals(expected, found, initial);
	}

	private void checkIntrinsicOnly(Zhang99Parameters initial,
									Zhang99Parameters found , double tolK , double tolD ) {
		assertEquals(initial.a,found.a,Math.abs(initial.a)*tolK);
		assertEquals(initial.b,found.b,Math.abs(initial.b)*tolK);
		assertEquals(initial.c,found.c,Math.abs(initial.c)*tolK);
		assertEquals(initial.x0,found.x0,Math.abs(initial.x0)*tolK);
		assertEquals(initial.y0, found.y0, Math.abs(initial.y0) * tolK);

		for( int i = 0; i < initial.distortion.length; i++ ) {
			assertEquals(initial.distortion[i],found.distortion[i],tolD);
		}
	}

	public static void checkEquals( Zhang99Parameters expected ,
									Zhang99Parameters found ,
									Zhang99Parameters initial )
	{
		double pixelTol=0.5;
		double paramTol=0.3;

		// see if it improved the estimate
		assertTrue(Math.abs(expected.a-initial.a)*paramTol >= Math.abs(expected.a-found.a));
		assertTrue(Math.abs(expected.b-initial.b)*paramTol >= Math.abs(expected.b-found.b));
		assertEquals(expected.c, found.c, 1e-5);
		assertTrue(Math.abs(expected.x0-initial.x0)*paramTol >= Math.abs(expected.x0-found.x0));
		assertTrue(Math.abs(expected.y0-initial.y0)*paramTol >= Math.abs(expected.y0-found.y0));

		for( int i = 0; i < expected.distortion.length; i++ ) {
			double e = expected.distortion[i];
			double f = found.distortion[i];
			double init = initial.distortion[i];
			assertTrue(Math.abs(init - f) * 0.5 >= Math.abs(f - e));
		}

		for( int i = 0; i < 2; i++ ) {
			Zhang99Parameters.View pp = expected.views[i];
			Zhang99Parameters.View ff = found.views[i];

			GeometryUnitTest.assertEquals(pp.T, ff.T, pixelTol);
			GeometryUnitTest.assertEquals(pp.rotation.unitAxisRotation,ff.rotation.unitAxisRotation,pixelTol);
			assertEquals(pp.rotation.theta,ff.rotation.theta,pixelTol);
		}
	}

	private static class Helper extends CalibrationPlanarGridZhang99
	{

		public Helper(PlanarCalibrationTarget config, boolean assumeZeroSkew, int numSkewParam) {
			super(config, assumeZeroSkew, numSkewParam);
		}

		public Zhang99Parameters initialParam( List<List<Point2D_F64>> observations ) {
			return super.initialParam(observations);
		}
	}
}
