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

package boofcv.alg.filter.kernel.impl;


import boofcv.alg.filter.kernel.KernelMath;
import boofcv.alg.filter.kernel.SteerableCoefficients;
import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_I32;


/**
 * <p>
 * Implementation of {@link boofcv.alg.filter.kernel.SteerableKernel} for integer point kernels.
 * <p>
 *
 * @author Peter Abeles
 */
public class SteerableKernel_I32 implements SteerableKernel<Kernel2D_I32> {

	// stores the output kernel
	private Kernel2D_I32 output;

	// definition of steerable function
	private SteerableCoefficients coef;
	private Kernel2D basis[];

	@Override
	public void setBasis( SteerableCoefficients coef ,
						  Kernel2D...basis )
	{
		this.coef = coef;
		this.basis = basis;

		int width = basis[0].width;
		output = new Kernel2D_I32(width);
	}

	@Override
	public Kernel2D_I32 compute( double angle ) {
		// set the output to zero
		KernelMath.fill(output,0);

		int N = output.width*output.width;

		for( int i = 0; i < basis.length; i++ ) {
			double c = coef.compute(angle,i);

			Kernel2D_I32 k = (Kernel2D_I32)basis[i];

			for( int j = 0; j < N; j++ ) {
				output.data[j] += k.data[j]*c;
			}
		}

		return output;
	}

	@Override
	public int getBasisSize() {
		return basis.length;
	}

	@Override
	public Kernel2D_I32 getBasis( int index ) {
		return (Kernel2D_I32)basis[index];
	}
}
