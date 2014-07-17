/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;


import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F32;
import georegression.transform.homography.HomographyPointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPixelTransformHomography_F32 {
	@Test
	public void constructor_32() {
		Homography2D_F32 a = new Homography2D_F32(1,2,3,4,5,6,7,8,9);

		PixelTransformHomography_F32 alg = new PixelTransformHomography_F32();
		alg.set(a);

		alg.compute(2,3);
		Point2D_F32 p = new Point2D_F32(2,3);
		Point2D_F32 expected = new Point2D_F32();
		HomographyPointOps_F32.transform(a, p, expected);

		assertEquals(expected.x,alg.distX,1e-4);
		assertEquals(expected.y,alg.distY,1e-4);
	}

	@Test
	public void constructor_64() {
		Homography2D_F64 a = new Homography2D_F64(1,2,3,4,5,6,7,8,9);

		PixelTransformHomography_F32 alg = new PixelTransformHomography_F32();
		alg.set(a);

		alg.compute(2,3);
		Point2D_F64 p = new Point2D_F64(2,3);
		Point2D_F64 expected = new Point2D_F64();
		HomographyPointOps_F64.transform(a, p, expected);

		assertEquals(expected.x,alg.distX,1e-4);
		assertEquals(expected.y,alg.distY,1e-4);
	}
}
