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

package boofcv.alg.filter.derivative;

import boofcv.alg.filter.derivative.impl.HessianThreeDeterminant_Border;
import boofcv.alg.filter.derivative.impl.HessianThreeDeterminant_Inner;
import boofcv.alg.filter.derivative.impl.HessianThreeDeterminant_Inner_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Computes the determinant of a Hessian computed by differentiating using [-1 0 1] kernel.<br>
 * f(x,y) = Lxx*Lyy - Lxy<sup>2</sup><br>
 * The Lxx and Lyy have a kernel of [1 0 -2 0 1] and Lxy is:</p>
 * <pre>
 *       [  1   0  -1 ]
 * Lxy = [  0   0   0 ]
 *       [ -1   0   1 ]
 * </pre>
 *
 * @author Peter Abeles
 */
public class HessianThreeDeterminant {
	/**
	 * Computes the Laplacian of input image.
	 *
	 * @param orig Input image. Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process( GrayU8 orig, GrayS16 deriv, @Nullable ImageBorder_S32<GrayU8> border ) {
		deriv.reshape(orig.width, orig.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			HessianThreeDeterminant_Inner_MT.process(orig, deriv);
		} else {
			HessianThreeDeterminant_Inner.process(orig, deriv);
		}

		if (border != null) {
			HessianThreeDeterminant_Border.process(orig, deriv, border);
		}
	}

	/**
	 * Computes Laplacian on an U8 image but outputs derivative in a F32 image. Removes a step when processing
	 * images for feature detection
	 */
	public static void process( GrayU8 orig, GrayF32 deriv, @Nullable ImageBorder_S32<GrayU8> border ) {
		deriv.reshape(orig.width, orig.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			HessianThreeDeterminant_Inner_MT.process(orig, deriv);
		} else {
			HessianThreeDeterminant_Inner.process(orig, deriv);
		}

		if (border != null) {
			HessianThreeDeterminant_Border.process(orig, deriv, border);
		}
	}

	/**
	 * Computes the Laplacian of 'orig'.
	 *
	 * @param orig Input image. Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process( GrayF32 orig, GrayF32 deriv, @Nullable ImageBorder_F32 border ) {
		deriv.reshape(orig.width, orig.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			HessianThreeDeterminant_Inner_MT.process(orig, deriv);
		} else {
			HessianThreeDeterminant_Inner.process(orig, deriv);
		}

		if (border != null) {
			HessianThreeDeterminant_Border.process(orig, deriv, border);
		}
	}
}
