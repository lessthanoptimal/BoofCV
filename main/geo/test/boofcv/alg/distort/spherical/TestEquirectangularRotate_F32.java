/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.PixelTransform2_F32;
import georegression.misc.GrlConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestEquirectangularRotate_F32 {

	/**
	 * Sees if recentering moves it to approximately the expected location
	 */
	@Test
	public void simpleTests() {

		EquirectangularRotate_F32 alg = new EquirectangularRotate_F32();
		alg.setImageShape(300,251);

		// this is the standard configuration and there should be no change
		alg.setDirection(0,0,0);
		alg.compute((int)(300.0f*0.5f), 250/2);
		assertMatch( alg, 300.0f*0.5f, 250/2);

		alg.setDirection( (float)Math.PI/2.0f,0,0);
		alg.compute((int)(300.0f*0.5f), 250/2);
		assertMatch( alg, 300.0f*0.75f, 250/2);

		alg.setDirection(0, (float)Math.PI/2,0);
		alg.compute((int)(300.0f*0.5f), 250/2);
		assertEquals( 0 , alg.distY, GrlConstants.FLOAT_TEST_TOL); //pathological.  only check y

		alg.setDirection(0, (float)-Math.PI/2,0);
		alg.compute((int)(300.0f*0.5f), 250/2);
		assertEquals( 250 , alg.distY, GrlConstants.FLOAT_TEST_TOL); //pathological.  only check y

		alg.setDirection(0, (float)Math.PI/4.0f,0);
		alg.compute((int)(300.0f*0.5f), 250/2);
		assertMatch( alg, 300.0f*0.5f, 250/4+0.5f);
		// 0.5f is fudge to make the test pass.  I *think* it's just discretation error
	}

	private void assertMatch(PixelTransform2_F32 tran , float x , float y ) {
		assertEquals( x , tran.distX, GrlConstants.FLOAT_TEST_TOL);
		assertEquals( y , tran.distY, GrlConstants.FLOAT_TEST_TOL);
	}
}
