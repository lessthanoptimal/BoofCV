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

package boofcv.struct.convolve;

/**
 * This is a kernel in a 1D convolution.  The kernel's width is the number of elements in it
 * and must be an odd number.  A kernel's radius is defined as the width divided by two.
 *
 * @author Peter Abeles
 */
public abstract class Kernel1D extends KernelBase {

	public Kernel1D(int width) {
		super(width);
	}

	public Kernel1D(int width, int offset) {
		super(width, offset);
	}

	public Kernel1D() {
	}

	@Override
	public int getDimension() {
		return 1;
	}

	public abstract double getDouble( int index );

	public abstract void setD( int index , double value );
}
