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
 * Precomputed border coefficients up to the specified depth.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class WlBorderCoefFixed<T extends WlCoef> implements WlBorderCoef<T> {

	// coefficients for the lower border
	public T[] lowerCoef;
	// coefficients for the upper border
	// index 0 corresponds to the upper border and high indexes move away
	public T[] upperCoef;
	T innerCoef;

	public WlBorderCoefFixed( int numLower , int numUpper ) {
		lowerCoef = (T[])new WlCoef[ numLower ];
		upperCoef = (T[])new WlCoef[ numUpper ];
	}

	public void setInnerCoef(T innerCoef) {
		this.innerCoef = innerCoef;
	}

	public T getLower( int index ) {
		return lowerCoef[index];
	}

	public T getUpper( int index ) {
		return upperCoef[index];
	}

	public void setLower( int index , T coef ) {
		lowerCoef[index] = coef;
	}

	public void setUpper( int index , T coef ) {
		upperCoef[index] = coef;
	}

	@Override
	public int getLowerLength() {
		return lowerCoef.length;
	}

	@Override
	public int getUpperLength() {
		return upperCoef.length;
	}

	@Override
	public T getBorderCoefficients(int index) {
		if( index >= 0 ) {
			int i = index/2;
			if( i < lowerCoef.length)
				return lowerCoef[i];
			else
				return innerCoef;
		} else {
			index = (-index)/2-1;
			if( index < upperCoef.length )
				return upperCoef[index];
			else
				return innerCoef;
		}
	}

	@Override
	public T getInnerCoefficients() {
		return innerCoef;
	}
}
