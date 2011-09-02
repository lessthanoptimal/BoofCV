/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.alg.transform.gss;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GradientThree;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageGenerator;
import boofcv.struct.image.ImageBase;

/**
 * Utility functions related to scale space processing.
 *
 * @author Peter Abeles
 */
public class UtilScaleSpace {

	/**
	 * <p>
	 * Creates an {@link AnyImageDerivative} for use when processing scale space images.
	 * </p>
	 *
	 * <p>
	 * The derivative is calculating using a kernel which does not involve any additional blurring.
	 * Using a Gaussian kernel is equivalent to blurring the image an additional time then computing the derivative
	 * Other derivatives such as Sobel and Prewitt also blur the image.   Image bluing has already been done
	 * once before the derivative is computed.
	 * </p>
	 *
	 * @param <I> Image type.
	 * @param <D> Image derivative type.
	 * @return AnyImageDerivative
	 */
	public static <I extends ImageBase, D extends ImageBase>
	AnyImageDerivative<I,D> createDerivatives( Class<I> inputType , ImageGenerator<D> derivGen ) {

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(inputType);

		return new AnyImageDerivative<I,D>(GradientThree.getKernelX(isInteger),inputType,derivGen);
	}
}
