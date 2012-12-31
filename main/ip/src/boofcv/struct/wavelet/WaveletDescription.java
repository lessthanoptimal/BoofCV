/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.wavelet;

import boofcv.core.image.border.BorderIndex1D;


/**
 * Contains wavelet coefficients needed to transform an image in the forwards an inverse direction.
 *
 * @author Peter Abeles
 */
public class WaveletDescription<T extends WlCoef> {

		// how image boundaries are handled
	public BorderIndex1D border;

	// coefficients for the forward transform
	public T forward;
	// coefficients for the inverse transform
	public WlBorderCoef<T> inverse;

	public WaveletDescription( BorderIndex1D border , T forward, WlBorderCoef<T> inverse) {
		this.border = border;
		this.forward = forward;
		this.inverse = inverse;
	}

	/**
	 * Describes how border conditions along the image are handled
	 */
	public BorderIndex1D getBorder() {
		return border;
	}

	/**
	 * Returns coefficients for the forward transform.
	 */
	public T getForward() {
		return forward;
	}

	/**
	 * Coefficients for inverse transform
	 */
	public WlBorderCoef<T> getInverse() {
		return inverse;
	}
}
