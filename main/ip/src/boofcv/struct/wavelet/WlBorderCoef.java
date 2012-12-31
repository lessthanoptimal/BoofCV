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


/**
 * Provides a different set of coefficients along the image's border and the inner portion.
 * This is required in conditions where simply remapping the indexes does not support the change.
 *
 * @author Peter Abeles
 */
public interface WlBorderCoef<T extends WlCoef> {

	// positive is distance from lower border
	// negative is distance from upper border
	T getBorderCoefficients( int index );


	public int getLowerLength();

	public int getUpperLength();

	/**
	 * Set of coefficients used inside the image where the scaling and wavelet signals do
	 * not go outside the image borders.
	 *
	 * @return Wavelet coefficients for inside the image.
	 */
	T getInnerCoefficients();
}
