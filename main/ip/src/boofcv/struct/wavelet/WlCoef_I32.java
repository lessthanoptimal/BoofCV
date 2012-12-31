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
 * Description of an integer wavelet.
 *
 * @author Peter Abeles
 */
public class WlCoef_I32 extends WlCoef {

	// scaling numbers
	public int scaling[];
	// wavelet numbers
	public int wavelet[];

	// denominator for scaling coefficients
	public int denominatorScaling;
	// denominator for wavelet coefficients
	public int denominatorWavelet;

	@Override
	public Class<?> getType() {
		return int.class;
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
