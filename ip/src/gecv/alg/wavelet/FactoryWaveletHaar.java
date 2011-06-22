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

package gecv.alg.wavelet;

import gecv.core.image.border.BorderIndex1D_Extend;
import gecv.struct.wavelet.WaveletCoefficient_F32;
import gecv.struct.wavelet.WaveletCoefficient_I32;


/**
 * Coefficients for Haar wavelet.
 *
 * @author Peter Abeles
 */
public class FactoryWaveletHaar {

	public static WaveletCoefficient_F32 generate_F32() {
		WaveletCoefficient_F32 ret = new WaveletCoefficient_F32();

		ret.border = new BorderIndex1D_Extend();
		ret.scaling = new float[]{(float)(1.0/Math.sqrt(2)),(float)(1.0/Math.sqrt(2))};
		ret.wavelet = new float[]{ret.scaling[0],-ret.scaling[0]};

		return ret;
	}

	public static WaveletCoefficient_I32 generate_I32() {
		WaveletCoefficient_I32 ret = new WaveletCoefficient_I32();

		ret.scaling = new int[]{1,1};
		ret.wavelet = new int[]{ret.scaling[0],-ret.scaling[0]};
		ret.denominatorScaling = 1;
		ret.denominatorWavelet = 1;

		return ret;
	}

	/**
	 * Create a description for the inverse transform.  Note that this will NOT produce
	 * an exact copy of the original due to rounding error.
	 *
	 * @return Wavelet inverse coefficient description.
	 */
	public static WaveletCoefficient_I32 generateInv_I32() {
		WaveletCoefficient_I32 ret = new WaveletCoefficient_I32();

		ret.scaling = new int[]{1,1};
		ret.wavelet = new int[]{ret.scaling[0],-ret.scaling[0]};
		ret.denominatorScaling = 2;
		ret.denominatorWavelet = 2;

		return ret;
	}
}
