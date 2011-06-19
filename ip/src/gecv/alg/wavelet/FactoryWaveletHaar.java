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


/**
 * Coefficients for Haar wavelet.
 *
 * @author Peter Abeles
 */
public class FactoryWaveletHaar {

	public static WaveletDesc_F32 generate_F32() {
		WaveletDesc_F32 ret = new WaveletDesc_F32();

		ret.scaling = new float[]{(float)(1.0/Math.sqrt(2)),(float)(1.0/Math.sqrt(2))};
		ret.wavelet = new float[]{ret.scaling[0],-ret.scaling[0]};

		return ret;
	}

	public static WaveletDesc_I32 generate_I32() {
		WaveletDesc_I32 ret = new WaveletDesc_I32();

		ret.scaling = new int[]{1,1};
		ret.wavelet = new int[]{ret.scaling[0],-ret.scaling[0]};
		ret.denominatorScaling = 1;
		ret.denominatorWavelet = 1;

		return ret;
	}
}
