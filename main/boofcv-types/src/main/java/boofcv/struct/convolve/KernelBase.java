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

package boofcv.struct.convolve;

import lombok.Getter;

/**
 * Base class for all convolution kernels.
 *
 * @author Peter Abeles
 */
public abstract class KernelBase {
	/** The kernel's width. */
	public @Getter int width;

	/** Offset from array index to spatial index 0, For symmetric kernels with an odd width it is width/2 */
	public @Getter int offset;

	protected KernelBase( int width ) {
		if (width < 0)
			throw new IllegalArgumentException("Kernel width must be greater than zero not " + width);
		this.width = width;
		this.offset = width/2;
	}

	protected KernelBase( int width, int offset ) {
		if (width <= 0)
			throw new IllegalArgumentException("Kernel width must be greater than zero not " + width);
		if (offset < 0 || offset >= width)
			throw new IllegalArgumentException("The offset must be inside the kernel's bounds. o=" + offset + " w=" + width);
		this.width = width;
		this.offset = offset;
	}

	protected KernelBase() {}

	/**
	 * The radius is defined as the width divided by two.
	 *
	 * @return The kernel's radius.
	 */
	public int getRadius() {return width/2;}

	/**
	 * Returns the dimension of this kernel, 1D or 2D.
	 *
	 * @return Kernel's dimension
	 */
	public abstract int getDimension();

	public abstract boolean isInteger();

	public abstract <T extends KernelBase> T copy();
}
