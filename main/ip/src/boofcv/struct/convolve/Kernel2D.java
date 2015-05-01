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
 * Base type for 2D convolution kernels
 *
 * @author Peter Abeles
 */
public abstract class Kernel2D extends KernelBase {


	protected Kernel2D(int width, int offset) {
		super(width, offset);
	}

	protected Kernel2D(int width) {
		super(width);
	}

	protected Kernel2D() {
	}

	@Override
	public int getDimension() {
		return 2;
	}

	public abstract double getDouble( int x , int y );
}
