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

package gecv.struct.wavelet;

import gecv.core.image.border.BorderIndex1D;


/**
 * Base class that defines a set of wavelet coefficients.
 *
 * @author Peter Abeles
 */
public abstract class WaveletCoefficient {

	// offset of wavelet numbers from start of signal array
	public int offsetScaling;

	// offset of wavelet numbers from start of signal array
	public int offsetWavelet;

	// how image boundaries are handled
	public BorderIndex1D border;

	/**
	 * Returns the primitive type of the coefficients.
	 *
	 * @return Coefficient data type.
	 */
	public abstract Class<?> getType();

	public abstract int getScalingLength();

	public abstract int getWaveletLength();

	public BorderIndex1D getBorder() {
		return border;
	}
}
