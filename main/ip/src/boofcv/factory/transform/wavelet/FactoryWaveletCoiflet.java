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

package boofcv.factory.transform.wavelet;

import boofcv.core.image.border.BorderIndex1D_Wrap;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlBorderCoefStandard;
import boofcv.struct.wavelet.WlCoef_F32;


/**
 * <p>
 * Coiflet wavelets are designed to maintain a close match between the trend and the original
 * signal.
 * </p>
 *
 * <p>
 * CoifI wavelets have the following properties:<br>
 * <ul>
 * <li>Designed so that the trend stays close to the original signal's value</li>
 * <li>Conserve the signal's energy</li>
 * <li>If the signal is approximately polynomial of degree I/2-1 or less within the support then the trend is approximately zero.</li>
 * <li>If the signal is approximately polynomial of degree I/2-2 or less within the support then the fluctuation is approximately zero.</li>
 * <li>The sum of the scaling numbers is sqrt(2)</li>
 * <li>The sum of the wavelet numbers is 0</li>
 * </ul>
 *
 * <p>
 * Citations:<br>
 * James S. Walker, "A Primer on WAVELETS and Their Scientific Applications," 2nd Ed. 2008
 * </p>
 *
 * @author Peter Abeles
 */
public class FactoryWaveletCoiflet {

	/**
	 * Creates a description of a Coiflet of order I wavelet.
	 * @param I order of the wavelet.
	 * @return Wavelet description.
	 */
	public static WaveletDescription<WlCoef_F32> generate_F32( int I ) {
		if( I != 6 ) {
			throw new IllegalArgumentException("Only 6 is currently supported");
		}

		WlCoef_F32 coef = new WlCoef_F32();

		coef.offsetScaling = -2;
		coef.offsetWavelet = -2;

		coef.scaling = new float[6];
		coef.wavelet = new float[6];

		double sqrt7 = Math.sqrt(7);
		double div = 16.0*Math.sqrt(2);
		coef.scaling[0] = (float)((1.0-sqrt7)/div);
		coef.scaling[1] = (float)((5.0+sqrt7)/div);
		coef.scaling[2] = (float)((14.0+2.0*sqrt7)/div);
		coef.scaling[3] = (float)((14.0-2.0*sqrt7)/div);
		coef.scaling[4] = (float)((1.0-sqrt7)/div);
		coef.scaling[5] = (float)((-3.0+sqrt7)/div);

		coef.wavelet[0] = coef.scaling[5];
		coef.wavelet[1] = -coef.scaling[4];
		coef.wavelet[2] = coef.scaling[3];
		coef.wavelet[3] = -coef.scaling[2];
		coef.wavelet[4] = coef.scaling[1];
		coef.wavelet[5] = -coef.scaling[0];

		WlBorderCoefStandard<WlCoef_F32> inverse = new WlBorderCoefStandard<>(coef);

		return new WaveletDescription<>(new BorderIndex1D_Wrap(), coef, inverse);
	}
}
