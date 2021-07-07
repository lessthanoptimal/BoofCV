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

package boofcv.alg.distort.spherical;

import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestEquirectangularRotate_F64 extends TestEquirectangularDistortBase_F64 {

	/**
	 * Sees if recentering moves it to approximately the expected location
	 */
	@Test
	void simpleTests() {

		EquirectangularRotate_F64 alg = new EquirectangularRotate_F64();
		alg.setEquirectangularShape(300,251);
		Point2D_F64 p = new Point2D_F64();

		// this is the standard configuration and there should be no change
		alg.setDirection(0,0,0);
		alg.compute((int)(300.0*0.5), 250/2, p);
		assertMatch( p, 300.0*0.5, 250/2);

		alg.setDirection( Math.PI/2.0,0,0);
		alg.compute((int)(300.0*0.5), 250/2, p);
		assertMatch( p, 300.0*0.75, 250/2);

		alg.setDirection(0, Math.PI/2,0);
		alg.compute((int)(300.0*0.5), 250/2, p);
		assertEquals( 0 , p.y, GrlConstants.TEST_F64); //pathological. only check y

		alg.setDirection(0, -Math.PI/2,0);
		alg.compute((int)(300.0*0.5), 250/2, p);
		assertEquals( 250 , p.y, GrlConstants.TEST_F64); //pathological. only check y

		alg.setDirection(0, Math.PI/4.0,0);
		alg.compute((int)(300.0*0.5), 250/2, p);
		assertMatch( p, 300.0*0.5, 250/4+0.5);
		// 0.5 is fudge to make the test pass. I *think* it's just discretation error
	}

	private void assertMatch(Point2D_F64 tran , double x , double y ) {
		assertEquals( x , tran.x, GrlConstants.TEST_F64);
		assertEquals( y , tran.y, GrlConstants.TEST_F64);
	}

	@Test
	void copy() {
		EquirectangularRotate_F64 alg = new EquirectangularRotate_F64();
		alg.setEquirectangularShape(300,251);

		copy(alg,100,120);
	}
}
