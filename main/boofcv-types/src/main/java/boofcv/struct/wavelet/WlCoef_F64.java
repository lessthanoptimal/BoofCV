/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
 * Description of a 64-bit floating point wavelet.
 *
 * @author Peter Abeles
 */
public class WlCoef_F64 extends WlCoef {

	// scaling numbers
	public double[] scaling;
	// wavelet numbers
	public double[] wavelet;

	public WlCoef_F64() {
		scaling = new double[0];
		wavelet = new double[0];
	}

	public WlCoef_F64( double[] scaling, int offsetScaling, double[] wavelet, int offsetWavelet ) {
		this.scaling = scaling;
		this.wavelet = wavelet;
		this.offsetScaling = offsetScaling;
		this.offsetWavelet = offsetWavelet;
	}

	@Override
	public Class<?> getType() {
		return double.class;
	}

	@Override
	public int getScalingLength() {
		return scaling.length;
	}

	@Override
	public int getWaveletLength() {
		return wavelet.length;
	}
}
