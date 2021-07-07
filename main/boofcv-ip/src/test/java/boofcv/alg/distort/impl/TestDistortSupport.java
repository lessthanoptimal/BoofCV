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

package boofcv.alg.distort.impl;

import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestDistortSupport extends BoofStandardJUnit {
	@Test void distortScale() {
		GrayF32 a = new GrayF32(25,30);
		GrayF32 b = new GrayF32(15,25);
		Point2D_F32 distorted = new Point2D_F32();

		PixelTransform<Point2D_F32> tran = DistortSupport.transformScale(a, b, null);

		// check edge cases at the image border
		tran.compute(0,0, distorted);
		assertEquals(0,distorted.x,1e-8);
		assertEquals(0,distorted.y,1e-8);

		tran.compute(24,29, distorted);
		assertEquals(24*15.0/25.0,distorted.x,1e-4);
		assertEquals(29*25.0/30.0,distorted.y,1e-4);

		// some point inside now
		tran.compute(5,6, distorted);

		assertEquals(5.0*15.0/25.0,distorted.x,1e-4);
		assertEquals(6.0*25.0/30.0,distorted.y,1e-4);
	}

	@Test void distortRotate() {
		Point2D_F32 distorted = new Point2D_F32();
		PixelTransform<Point2D_F32> tran = DistortSupport.transformRotate(13f,15.0f,13f,15f,(float)(-Math.PI/2.0));

		// trivial case
		tran.compute(13,15, distorted);
		assertEquals(13,distorted.x,1e-4);
		assertEquals(15,distorted.y,1e-4);
		// see how it handles the rotation
		tran.compute(15,20, distorted);
		assertEquals(8,distorted.x,1e-4);
		assertEquals(17,distorted.y,1e-4);
	}
}
