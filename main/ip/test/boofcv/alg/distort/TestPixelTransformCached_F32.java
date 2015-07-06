/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPixelTransformCached_F32 {

	int width = 20;
	int height = 25;

	@Test
	public void compareToOrig() {
		PixelTransformAffine_F32 orig = new PixelTransformAffine_F32();
		orig.set(new Affine2D_F32(1f,0.1f,0.05f,2f,5f,6f));

		PixelTransformCached_F32 alg = new PixelTransformCached_F32(width,height,orig);

		// it goes outside the border by one since some times the outside bound is used
		for (int y = 0; y < height + 1; y++) {
			for (int x = 0; x < width + 1; x++) {
				alg.compute(x,y);
				orig.compute(x,y);
				assertEquals(orig.distX,alg.distX,1e-8);
				assertEquals(orig.distY,alg.distY,1e-8);
			}
		}
	}
}