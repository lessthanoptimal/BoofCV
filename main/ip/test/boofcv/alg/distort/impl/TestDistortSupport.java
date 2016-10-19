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

package boofcv.alg.distort.impl;

import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestDistortSupport {
	@Test
	public void distortScale() {
		GrayF32 a = new GrayF32(25,30);
		GrayF32 b = new GrayF32(15,25);

		PixelTransform2_F32 tran = DistortSupport.transformScale(a, b, null);

		// check edge cases at the image border
		tran.compute(0,0);
		assertEquals(0,tran.distX,1e-8);
		assertEquals(0,tran.distY,1e-8);

		tran.compute(24,29);
		assertEquals(24*15.0/25.0,tran.distX,1e-4);
		assertEquals(29*25.0/30.0,tran.distY,1e-4);

		// some point inside now
		tran.compute(5,6);

		assertEquals(5.0*15.0/25.0,tran.distX,1e-4);
		assertEquals(6.0*25.0/30.0,tran.distY,1e-4);
	}

	@Test
	public void distortRotate() {

		PixelTransform2_F32 tran = DistortSupport.transformRotate(13f,15.0f,13f,15f,(float)(-Math.PI/2.0));

		// trivial case
		tran.compute(13,15);
		assertEquals(13,tran.distX,1e-4);
		assertEquals(15,tran.distY,1e-4);
		// see how it handles the rotation
		tran.compute(15,20);
		assertEquals(8,tran.distX,1e-4);
		assertEquals(17,tran.distY,1e-4);
	}
}
