/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPinholeNtoP_F64 extends BoofStandardJUnit {

	@Test void basicTest() {
		CameraPinholeBrown p = new CameraPinholeBrown().fsetK(1, 2, 3, 4, 5, 200, 300);

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(p, (DMatrixRMaj)null);

		Point2D_F64 pixel = new Point2D_F64(150, 200);
		Point2D_F64 expected = new Point2D_F64();
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(K, pixel, expected);

		PinholeNtoP_F64 alg = new PinholeNtoP_F64();
		alg.setK(p.fx, p.fy, p.skew, p.cx, p.cy);

		alg.compute(pixel.x, pixel.y, found);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}
}
