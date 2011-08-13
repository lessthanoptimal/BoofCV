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

package gecv.alg.filter.kernel;


import gecv.struct.convolve.Kernel2D_F32;


/**
 * <p>
 * Computes a 2D kernel for an arbitrary angle using steerable filters. Steerable functions can be computed from
 * a linear combination of rotated basis kernels.
 * </p>
 *
 * <p>
 * William T. Freeman and Edward H. Adelson, "The Design and Use of Steerable Filters", IEEE Trans. Patt. Anal.
 * and Machine Intell., Vol. 13, No. 9, Sept. 1991
 * </p>
 *
 * @author Peter Abeles
 */
// todo generalize by kernel type
public class SteerableKernel {

	// stores the output kernel
	private Kernel2D_F32 output;

	// definition of steerable function
	private SteerableCoefficients coef;
	private Kernel2D_F32 basis[];

	/**
	 * Compute the steerable filter.
	 *
	 * @param coef Coefficients for each basis.
	 * @param basis Kernels which form the basis for the steerable filter.
	 */
	public void setBasis( SteerableCoefficients coef ,
						  Kernel2D_F32...basis )
	{
		this.coef = coef;
		this.basis = basis;

		int width = basis[0].width;
		output = new Kernel2D_F32(width);
	}

	/**
	 * Computes the kernel at the specified angle.
	 *
	 * @param angle Angle the kernel should be pointed at.
	 * @return The computed kernel.  Data is recycled each time compute is called.
	 */
	public Kernel2D_F32 compute( double angle ) {
		// set the output to zero
		KernelMath.fill(output,0);

		int N = output.width*output.width;

		for( int i = 0; i < basis.length; i++ ) {
			double c = coef.compute(angle,i);

			Kernel2D_F32 k = basis[i];

			for( int j = 0; j < N; j++ ) {
				output.data[j] += k.data[j]*c;
			}
		}

		return output;
	}

	public int getBasisSize() {
		return basis.length;
	}

	public Kernel2D_F32 getBasis( int index ) {
		return basis[index];
	}
}
