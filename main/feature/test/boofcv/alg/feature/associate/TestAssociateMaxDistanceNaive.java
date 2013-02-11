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

package boofcv.alg.feature.associate;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestAssociateMaxDistanceNaive {

	Random rand = new Random(234);

	/**
	 * Change the tolerance and see which features it associates
	 */
	@Test
	public void checkDistanceCalculation() {

		double theta = Math.PI*rand.nextDouble()*2;
		double c = Math.cos(theta);
		double s = Math.sin(theta);
		double r = 3;


		Point2D_F64 a1 = new Point2D_F64(10,20);
		Point2D_F64 b1 = new Point2D_F64(a1.x + c*r,a1.y + s*r);

		AssociateMaxDistanceNaive alg = new AssociateMaxDistanceNaive(null,false,Double.MAX_VALUE);

		alg.setActiveSource(a1);
		assertEquals(3*3,alg.computeDistanceToSource(b1),1e-8);
	}

	/**
	 * Make sure the internal max distance is correctly mangled.
	 */
	@Test
	public void checkMaxDistance() {
		AssociateMaxDistanceNaive alg = new AssociateMaxDistanceNaive(null,false,Double.MAX_VALUE);

		alg.setMaxDistance(2);
		assertEquals(2,alg.getMaxDistance(),1e-8);
		assertEquals(4,alg.maxDistance,1e-8);
	}
}
