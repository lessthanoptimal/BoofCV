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

package boofcv.alg.distort.spherical;

import boofcv.struct.distort.PixelTransform;
import boofcv.testing.BoofStandardJUnit;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
abstract class TestEquirectangularDistortBase_F64 extends BoofStandardJUnit {

	void copy( EquirectangularDistortBase_F64 original , int equiWidth, int equiHeight) {

		original.setEquirectangularShape(equiWidth,equiHeight);

		PixelTransform<Point2D_F64> copy = original.copyConcurrent();

		Point2D_F64 origP = new Point2D_F64();
		Point2D_F64 copyC = new Point2D_F64();

		for (int i = 0; i < equiWidth; i++) {
			original.compute(i,12,origP);
			copy.compute(i,12,copyC);

			assertEquals( origP.x, copyC.x, GrlConstants.TEST_F64);
			assertEquals( origP.y, copyC.y, GrlConstants.TEST_F64);
		}
	}
}
