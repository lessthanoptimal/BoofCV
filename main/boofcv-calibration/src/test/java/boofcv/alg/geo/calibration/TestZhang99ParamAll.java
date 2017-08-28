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
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestZhang99ParamAll {

	Random rand = new Random(234);

	/**
	 * Test to see if the conversion to and from a parameter array works well.
	 */
	@Test
	public void toAndFromParametersArray() {

		Dummy dummy = new Dummy();

		Zhang99AllParam p = new Zhang99AllParam(dummy,2);

		dummy.a = 3;
		for( int i = 0; i < 2; i++ ) {
			Zhang99AllParam.View v = p.views[i];
			v.T.set(rand.nextDouble(),rand.nextDouble(),rand.nextDouble());
			v.rotation.theta = rand.nextDouble();
			v.rotation.unitAxisRotation.set(rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian());
			v.rotation.unitAxisRotation.normalize();
		}

		// convert it into array format
		double array[] = new double[ p.numParameters() ];
		p.convertToParam(array);

		// create a new set of parameters and assign its value from the array
		Zhang99AllParam found = new Zhang99AllParam(new Dummy(),2);
		found.setFromParam(array);

		// compare the two sets of parameters
		checkEquals(p,found);
	}

	private void checkEquals(Zhang99AllParam expected ,
							 Zhang99AllParam found ) {
		double tol = 1e-6;

		Dummy dummyE = (Dummy)expected.getIntrinsic();
		Dummy dummyF = (Dummy)expected.getIntrinsic();

		assertEquals(dummyE.a,dummyF.a,tol);

		for( int i = 0; i < 2; i++ ) {
			Zhang99AllParam.View pp = expected.views[i];
			Zhang99AllParam.View ff = found.views[i];

			GeometryUnitTest.assertEquals(pp.T, ff.T, tol);
			GeometryUnitTest.assertEquals(pp.rotation.unitAxisRotation,ff.rotation.unitAxisRotation,tol);
			assertEquals(pp.rotation.theta,ff.rotation.theta,tol);
		}
	}

	private static class Dummy extends Zhang99IntrinsicParam {

		public double a;

		@Override
		public int getNumberOfRadial() {
			return 0;
		}

		@Override
		public void initialize(DMatrixRMaj K, double[] radial) {

		}

		@Override
		public int numParameters() {
			return 1;
		}

		@Override
		public int setFromParam(double[] param) {
			a = param[0];
			return 1;
		}

		@Override
		public int convertToParam(double[] param) {
			param[0] = a;
			return 1;
		}

		@Override
		public <T extends CameraModel> T getCameraModel() {
			return null;
		}

		@Override
		public Zhang99IntrinsicParam createLike() {
			return null;
		}

		@Override
		public Zhang99OptimizationJacobian createJacobian(List<CalibrationObservation> observations, List<Point2D_F64> grid) {
			return null;
		}

		@Override
		public void setTo(Zhang99IntrinsicParam orig) {

		}

		@Override
		public void forceProjectionUpdate() {

		}

		@Override
		public void project(Point3D_F64 cameraPt, Point2D_F64 pixel) {

		}
	}
}
