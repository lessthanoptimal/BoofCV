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

package boofcv.struct.convolve;

/**
 * <p>
 * Computes the instantaneous value of a continuous valued function.
 * </p>
 *
 * <p>
 * The kernel is assumed to be zero outside of its radius.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class KernelContinuous1D_F32 extends Kernel1D {

	protected KernelContinuous1D_F32(int width) {
		super(width);
	}

	/**
	 * Computes the value of the kernel at hte specified point.
	 *
	 * @param x Function's input.
	 * @return  Function's value at point 'x'
	 */
	abstract public float compute( float x );
}
