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

import boofcv.alg.geo.calibration.pinhole.CalibParamPinholeRadial;
import boofcv.struct.calib.CameraPinholeRadial;
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
		fullTest(false);
		fullTest(true);
	}

	public void fullTest( boolean partial ) {
		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();
		Zhang99AllParam expected = GenericCalibrationGrid.createStandardParam(true,2,true,3,rand);

		List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(expected,grid);

		if( partial ) {
			for (int i = 0; i < observations.size(); i++) {
				CalibrationObservation o = observations.get(i);
				for (int j = 0; j < 5; j++) {
					o.points.remove(i*2+j);
				}
			}
		}
		CalibrationPlanarGridZhang99 alg =
				new CalibrationPlanarGridZhang99(grid,true,2,true);

		assertTrue(alg.process(observations));

		Zhang99AllParam found = alg.getOptimized();

		checkIntrinsicOnly(
				(CameraPinholeRadial)expected.getIntrinsic().getCameraModel(),
				(CameraPinholeRadial)found.getIntrinsic().getCameraModel(),0.01,0.1,0.1);
	}

	/**
	 * See how well it computes an initial guess at the parameters given perfect inputs
	 */
	@Test
	public void initialParam() {
		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();
		Zhang99AllParam initial = GenericCalibrationGrid.createEasierParam(true, 2, false, 3, rand);
		// tangential can't be linearly estimated

		List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(initial,grid);

		Helper alg = new Helper(grid,true,2,false);

		Zhang99AllParam found = initial.createLike();
		alg.initialParam(observations,found);

		checkIntrinsicOnly(
				(CameraPinholeRadial)initial.getIntrinsic().getCameraModel(),
				(CameraPinholeRadial)found.getIntrinsic().getCameraModel(), 0.01, 0.1, 0.1);
	}

	/**
	 * Test nonlinear optimization with perfect inputs
	 */
	@Test
	public void optimizedParam_perfect() {

		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();
		Zhang99AllParam initial = GenericCalibrationGrid.createStandardParam(true,2,true,3,rand);
		Zhang99AllParam found = new Zhang99AllParam(new CalibParamPinholeRadial(true,2,true),3);

		List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(initial,grid);

		CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(grid,true,2,true);
		assertTrue(alg.optimizedParam(observations, grid, initial, found,null));

		checkEquals(initial, found, initial);
	}

	/**
	 * Test nonlinear optimization with a bit of noise
	 */
	@Test
	public void optimizedParam_noisy() {

		List<Point2D_F64> grid = GenericCalibrationGrid.standardLayout();
		Zhang99AllParam initial = GenericCalibrationGrid.createStandardParam(true,2,true,3,rand);
		Zhang99AllParam expected = initial.copy();
		Zhang99AllParam found = new Zhang99AllParam(new CalibParamPinholeRadial(true,2,true),3);

		List<CalibrationObservation> observations = GenericCalibrationGrid.createObservations(initial,grid);

		// add a tinny bit of noise
		CameraPinholeRadial intrinsic = initial.getIntrinsic().getCameraModel();
		intrinsic.fx += rand.nextDouble()*0.01*Math.abs(intrinsic.fx);
		intrinsic.fy += rand.nextDouble()*0.01*Math.abs(intrinsic.fy);
		intrinsic.skew += rand.nextDouble()*0.01*Math.abs(intrinsic.skew);
		intrinsic.cx += rand.nextDouble()*0.01*Math.abs(intrinsic.cx);
		intrinsic.cy += rand.nextDouble()*0.01*Math.abs(intrinsic.cy);

		for( int i = 0; i < intrinsic.radial.length; i++ ) {
			intrinsic.radial[i] = rand.nextGaussian()*intrinsic.radial[i]*0.1;
		}

		CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(grid,true,2,true);
		assertTrue(alg.optimizedParam(observations, grid, initial, found,null));

		checkEquals(expected, found, initial);
	}

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

	private void checkIntrinsicOnly(CameraPinholeRadial initial,
									CameraPinholeRadial found , double tolK , double tolD , double tolT ) {
		assertEquals(initial.fx,found.fx,Math.abs(initial.fx)*tolK);
		assertEquals(initial.fy,found.fy,Math.abs(initial.fy)*tolK);
		assertEquals(initial.skew,found.skew,Math.abs(initial.skew)*tolK);
		assertEquals(initial.cx,found.cx,Math.abs(initial.cx)*tolK);
		assertEquals(initial.cy, found.cy, Math.abs(initial.cy) * tolK);

		for( int i = 0; i < initial.radial.length; i++ ) {
			assertEquals(initial.radial[i],found.radial[i],tolD);
		}
		assertEquals(initial.t1,found.t1,tolT);
		assertEquals(initial.t2,found.t2,tolT);
	}

	public static void checkEquals( Zhang99AllParam expected ,
									Zhang99AllParam found ,
									Zhang99AllParam initial )
	{
		checkEquals((CameraPinholeRadial)expected.getIntrinsic().getCameraModel(),
				(CameraPinholeRadial)found.getIntrinsic().getCameraModel(),
				(CameraPinholeRadial)initial.getIntrinsic().getCameraModel());

		double pixelTol=0.5;

		for( int i = 0; i < 2; i++ ) {
			Zhang99AllParam.View pp = expected.views[i];
			Zhang99AllParam.View ff = found.views[i];

			GeometryUnitTest.assertEquals(pp.T, ff.T, pixelTol);
			GeometryUnitTest.assertEquals(pp.rotation.unitAxisRotation,ff.rotation.unitAxisRotation,pixelTol);
			assertEquals(pp.rotation.theta,ff.rotation.theta,pixelTol);
		}
	}

	private static void checkEquals( CameraPinholeRadial expected ,
									 CameraPinholeRadial found ,
									 CameraPinholeRadial initial )
	{
		double paramTol=0.3;

		// see if it improved the estimate
		assertTrue(Math.abs(expected.fx-initial.fx)*paramTol >= Math.abs(expected.fx-found.fx));
		assertTrue(Math.abs(expected.fy-initial.fy)*paramTol >= Math.abs(expected.fy-found.fy));
		assertEquals(expected.skew, found.skew, 1e-5);
		assertTrue(Math.abs(expected.cx-initial.cx)*paramTol >= Math.abs(expected.cx-found.cx));
		assertTrue(Math.abs(expected.cy-initial.cy)*paramTol >= Math.abs(expected.cy-found.cy));

		for( int i = 0; i < expected.radial.length; i++ ) {
			double e = expected.radial[i];
			double f = found.radial[i];
			double init = initial.radial[i];
			assertTrue(Math.abs(init - f) * 0.5 >= Math.abs(f - e));
		}

		assertTrue(Math.abs(expected.t1 - found.t1) <= Math.abs(initial.t1 - found.t1));
		assertTrue(Math.abs(expected.t2 - found.t2) <= Math.abs(initial.t2 - found.t2));
	}

	private static class Helper extends CalibrationPlanarGridZhang99
	{

		public Helper(List<Point2D_F64> layout, boolean assumeZeroSkew,
					  int numRadial, boolean includeTangential) {
			super(layout, assumeZeroSkew, numRadial,includeTangential);
		}

		public boolean initialParam(List<CalibrationObservation> observations , Zhang99AllParam param  ) {
			return super.initialParam(observations,param);
		}
	}
}
