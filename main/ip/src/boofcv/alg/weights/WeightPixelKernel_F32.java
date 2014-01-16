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

package boofcv.alg.weights;

import boofcv.struct.convolve.Kernel2D_F32;

/**
 * Weight which uses the values contained in a {@link Kernel2D_F32}. For performance reasons no checks are
 * done to see if a request has been made outside the kernel's radius.  Those values should be zero.
 *
 * @author Peter Abeles
 */
public abstract class WeightPixelKernel_F32 implements WeightPixel_F32 {
	protected Kernel2D_F32 kernel;

	@Override
	public float weightIndex(int index) {
		return kernel.data[index];
	}

	@Override
	public float weight(int x, int y) {
		x += kernel.getRadius();
		y += kernel.getRadius();

		return kernel.data[ y*kernel.width + x ];
	}

	@Override
	public int getRadiusX() {
		return kernel.getRadius();
	}

	@Override
	public int getRadiusY() {
		return kernel.getRadius();
	}
}
