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
 * Inverse wavelet description which simply returns the same set of coefficients at all time.
 *
 * @author Peter Abeles
 */
public class WlBorderCoefStandard<T extends WlCoef> implements WlBorderCoef<T> {

	T coef;

	public WlBorderCoefStandard(T coef ) {
		this.coef = coef;
	}

	@Override
	public T getBorderCoefficients(int index) {
		return coef;
	}

	@Override
	public int getLowerLength() {
		return 0;
	}

	@Override
	public int getUpperLength() {
		return 0;
	}

	@Override
	public T getInnerCoefficients() {
		return coef;
	}
}
