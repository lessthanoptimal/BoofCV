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

import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.struct.wavelet.*;


/**
 * Coefficients for Haar wavelet.
 *
 * @author Peter Abeles
 */
public class FactoryWaveletHaar {

	public static <C extends WlCoef> 
	WaveletDescription<C> generate( boolean isInteger , int imageBits )
	{
		if( isInteger ) {
			if( imageBits <= 32) {
				WlCoef_I32 forward = new WlCoef_I32();

				forward.scaling = new int[]{1,1};
				forward.wavelet = new int[]{forward.scaling[0],-forward.scaling[0]};
				forward.denominatorScaling = 1;
				forward.denominatorWavelet = 1;

				WlBorderCoef<WlCoef_I32> inverse = new WlBorderCoefStandard<>(generateInv_I32());

				return new WaveletDescription(new BorderIndex1D_Extend(),forward,inverse);
			}
		} else {
			if( imageBits == 32 ) {
				WlCoef_F32 forward = new WlCoef_F32();

				forward.scaling = new float[]{(float)(1.0/Math.sqrt(2)),(float)(1.0/Math.sqrt(2))};
				forward.wavelet = new float[]{forward.scaling[0],-forward.scaling[0]};

				WlBorderCoef<WlCoef_F32> inverse = new WlBorderCoefStandard<>(forward);

				return new WaveletDescription(new BorderIndex1D_Extend(),forward,inverse);
			}
		}
		return null;
	}

	/**
	 * Create a description for the inverse transform.  Note that this will NOT produce
	 * an exact copy of the original due to rounding error.
	 *
	 * @return Wavelet inverse coefficient description.
	 */
	private static WlCoef_I32 generateInv_I32() {
		WlCoef_I32 ret = new WlCoef_I32();

		ret.scaling = new int[]{1,1};
		ret.wavelet = new int[]{ret.scaling[0],-ret.scaling[0]};
		ret.denominatorScaling = 2;
		ret.denominatorWavelet = 2;

		return ret;
	}
}
