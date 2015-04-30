/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import org.junit.Test;

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
		checkToAndFromParam(true,false);
		checkToAndFromParam(false,false);
		checkToAndFromParam(true,true);
		checkToAndFromParam(false,true);
	}

	public void checkToAndFromParam( boolean assumeZeroSkew , boolean includeTangential )
	{
		Zhang99ParamAll p = new Zhang99ParamAll(assumeZeroSkew,3,includeTangential,2);

		p.a = 2;p.b=3;p.c=4;p.x0=5;p.y0=6;
		p.radial = new double[]{1,2,3};
		p.t1 = 7; p.t2 = 8;
		for( int i = 0; i < 2; i++ ) {
			Zhang99ParamAll.View v = p.views[i];
			v.T.set(rand.nextDouble(),rand.nextDouble(),rand.nextDouble());
			v.rotation.theta = rand.nextDouble();
			v.rotation.unitAxisRotation.set(rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian());
			v.rotation.unitAxisRotation.normalize();
		}

		// convert it into array format
		double array[] = new double[ p.numParameters() ];
		p.convertToParam(array);

		// create a new set of parameters and assign its value from the array
		Zhang99ParamAll found = new Zhang99ParamAll(assumeZeroSkew,3,includeTangential,2);
		found.setFromParam(array);

		// compare the two sets of parameters
		checkEquals(p,found,assumeZeroSkew,includeTangential);
	}

	private void checkEquals(Zhang99ParamAll expected ,
							 Zhang99ParamAll found ,
							 boolean assumeZeroSkew , boolean includeTangential) {
		double tol = 1e-6;

		assertEquals(expected.a,found.a,tol);
		assertEquals(expected.b,found.b,tol);
		if( !assumeZeroSkew )
			assertEquals(expected.c,found.c,tol);
		assertEquals(expected.x0,found.x0,tol);
		assertEquals(expected.y0,found.y0,tol);

		for( int i = 0; i < expected.radial.length; i++ ) {
			assertEquals(expected.radial[i],found.radial[i],tol);
		}

		if( includeTangential ) {
			assertEquals(expected.t1,found.t1,tol);
			assertEquals(expected.t2,found.t2,tol);
		}

		for( int i = 0; i < 2; i++ ) {
			Zhang99ParamAll.View pp = expected.views[i];
			Zhang99ParamAll.View ff = found.views[i];

			GeometryUnitTest.assertEquals(pp.T, ff.T, tol);
			GeometryUnitTest.assertEquals(pp.rotation.unitAxisRotation,ff.rotation.unitAxisRotation,tol);
			assertEquals(pp.rotation.theta,ff.rotation.theta,tol);
		}
	}
}
