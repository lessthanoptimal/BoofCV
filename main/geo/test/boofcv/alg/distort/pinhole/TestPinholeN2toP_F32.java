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

package boofcv.alg.distort.pinhole;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPinholeN2toP_F32 {

	@Test
	public void basicTest() {
		CameraPinholeRadial p = new CameraPinholeRadial().fsetK(1, 2, 3, 4, 5, 200, 300);

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(p, null);

		Point2D_F32 pixel = new Point2D_F32(150, 200);
		Point2D_F32 expected = new Point2D_F32();
		Point2D_F32 found = new Point2D_F32();

		GeometryMath_F32.mult(K, pixel, expected);

		PinholeNtoP_F32 alg = new PinholeNtoP_F32();
		alg.set(p.fx, p.fy, p.skew, p.cx, p.cy);

		alg.compute(pixel.x, pixel.y, found);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

}
