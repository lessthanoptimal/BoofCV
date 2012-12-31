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

package boofcv.alg.filter.kernel;


import boofcv.struct.convolve.Kernel2D;


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
public interface SteerableKernel< K extends Kernel2D> {


	/**
	 * Compute the steerable filter.
	 *
	 * @param coef Coefficients for each basis.
	 * @param basis Kernels which form the basis for the steerable filter.
	 */
	public void setBasis( SteerableCoefficients coef ,  Kernel2D...basis );

	/**
	 * Computes the kernel at the specified angle.
	 *
	 * @param angle Angle the kernel should be pointed at.
	 * @return The computed kernel.  Data is recycled each time compute is called.
	 */
	public K compute( double angle );

	public int getBasisSize() ;

	public K getBasis( int index ) ;
}
