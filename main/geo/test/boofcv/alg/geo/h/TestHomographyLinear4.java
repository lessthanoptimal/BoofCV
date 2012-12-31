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

package boofcv.alg.geo.h;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.ops.NormOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestHomographyLinear4 extends CommonHomographyChecks{

	@Test
	public void perfectCalibrated() {
		// test the minimum number of points
		checkHomography(4, false, new HomographyLinear4(false));
		// test with extra points
		checkHomography(10, false, new HomographyLinear4(false));
	}

	@Test
	public void perfectPixels() {
		checkHomography(4, true, new HomographyLinear4(true));
		checkHomography(10, true, new HomographyLinear4(true));
	}

	/**
	 * Create a set of points perfectly on a plane and provide perfect observations of them
	 *
	 * @param N Number of observed points.
	 * @param isPixels Pixel or calibrated coordinates
	 * @param alg Algorithm being evaluated
	 */
	private void checkHomography(int N, boolean isPixels, HomographyLinear4 alg) {
		createScene(N,isPixels);

		// compute essential
		assertTrue(alg.process(pairs,solution));

		// validate by testing essential properties

		// sanity check, F is not zero
		assertTrue(NormOps.normF(solution) > 0.001 );

		// see if it follows the epipolar constraint
		for( AssociatedPair p : pairs ) {
			Point2D_F64 a = GeometryMath_F64.mult(solution,p.p1,new Point2D_F64());

			double diff = a.distance(p.p2);
			assertEquals(0,diff,1e-8);
		}
	}
}
