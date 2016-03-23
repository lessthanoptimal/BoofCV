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

package boofcv.alg.filter.derivative.impl;

import boofcv.struct.image.GrayF32;


/**
 * <p>
 * This is an attempt to improve the performance by minimizing the number of times arrays are accessed
 * and partially unrolling loops.
 * </p>
 * <p>
 * While faster than the standard algorithm, the standard appears to be fast enough.
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientThree_Share {


	/**
	 * Can only be used with images that are NOT sub-images.
	 */
	public static void derivX_F32(GrayF32 orig,
								  GrayF32 derivX) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight();

		for (int y = 0; y < height; y++) {
			int index = width * y + 1;
			int endX = index + width - 2;
			int endXAlt = endX - (width - 2) % 3;

			float x0 = data[index - 1];
			float x1 = data[index];

			for (; index < endXAlt;) {
				float x2 = data[index + 1];
				imgX[index++] = (x2 - x0) * 0.5f;
				x0 = data[index + 1];
				imgX[index++] = (x0 - x1) * 0.5f;
				x1 = data[index + 1];
				imgX[index++] = (x1 - x2) * 0.5f;
			}

			for (; index < endX; index++) {
				imgX[index] = (data[index + 1] - data[index - 1]) * 0.5f;
			}
		}
	}
}