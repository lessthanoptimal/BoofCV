/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.kernel;

import boofcv.struct.convolve.KernelBase;
import boofcv.struct.convolve.KernelContinuous1D_F32;

/**
 * <p>
 * A kernel can be used to approximate bicubic interpolation.  Full bicubic interpolation is much more expensive.
 * The value of a=-0.5 is the best approximation.
 * </p>
 *
 * <p>
 * <ul>
 * <li>R. Keys, (1981). "Cubic convolution interpolation for digital image processing".
 * IEEE Transactions on Signal Processing, Acoustics, Speech, and Signal Processing 29: 1153 </li>
 * <li>http://en.wikipedia.org/wiki/Bicubic_interpolation for more information.  July 25, 2011</li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class BicubicKernel_F32 extends KernelContinuous1D_F32 {

	// parameter in its convolution function
	float a;

	/**
	 * Values of a =-0.5 and -0.75 are typical
	 * @param a A parameter
	 */
	public BicubicKernel_F32( float a ) {
		super(5);
		this.a = a;
	}

	@Override
	public boolean isInteger() {
		return false;
	}

	@Override
	public <T extends KernelBase> T copy() {
		return (T)new BicubicKernel_F32(a);
	}

	@Override
	public float compute(float x) {
		float absX = x < 0 ? -x : x;

		if( absX <= 1f ) {
			float x2 = absX*absX;

			return (a+2f)*x2*absX - (a+3f)*x2 + 1f;
		} else if( absX < 2 ) {
			float x2 = absX*absX;

			return a*x2*absX - 5f*a*x2 + 8f*a*absX -4f*a;
		} else {
			return 0f;
		}
	}

	@Override
	public double getDouble(int index) {
		throw new RuntimeException("Well this function really shouldn't be required to be implemented");
	}

	@Override
	public void setD(int index, double value) {
		throw new RuntimeException("Well this function really shouldn't be required to be implemented");
	}
}
