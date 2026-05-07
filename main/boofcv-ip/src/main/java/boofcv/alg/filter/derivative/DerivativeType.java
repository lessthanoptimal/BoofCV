/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

/// List of standard kernels used to compute the gradient of an image.
///
/// The divisor for integer kernel is derived from the smoothing and differentiating which define is. For
/// Sobel its smoothing kernel is [1 2 1] and differentiated using [-1 0 1]. The smoothing has a sum of 4 and
/// differentiating has a distance of 2, so a divisor of 8. Three [1 0 1] has a smoothing kernel of [1] and
/// it is the differentiating kernel, so 1*2 = 2.
public enum DerivativeType {
	/// [GradientPrewitt]
	PREWITT,
	/// [GradientSobel]
	SOBEL,
	/// [GradientScharr]
	SCHARR,
	/// [GradientThree]
	THREE,
	/// [GradientTwo0]
	TWO_0,
	/// [GradientTwo1]
	TWO_1
}
