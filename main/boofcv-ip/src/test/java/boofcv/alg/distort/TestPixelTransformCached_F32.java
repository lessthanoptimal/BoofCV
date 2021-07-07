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

package boofcv.alg.distort;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPixelTransformCached_F32 extends BoofStandardJUnit {

	int width = 20;
	int height = 25;

	@Test void compareToOrig() {
		PixelTransformAffine_F32 orig = new PixelTransformAffine_F32();
		orig.setTo(new Affine2D_F32(1f, 0.1f, 0.05f, 2f, 5f, 6f));

		PixelTransformCached_F32 alg = new PixelTransformCached_F32(width, height, orig);

		Point2D_F32 expected = new Point2D_F32();
		Point2D_F32 found = new Point2D_F32();

		// it goes outside the border by one since some times the outside bound is used
		for (int y = 0; y < height + 1; y++) {
			for (int x = 0; x < width + 1; x++) {
				alg.compute(x, y, found);
				orig.compute(x, y, expected);
				assertEquals(expected.x, found.x, 1e-8);
				assertEquals(expected.y, found.y, 1e-8);
			}
		}
	}
}