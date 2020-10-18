/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.distort;

/**
 * Computes a transform in pixel coordinates
 *
 * @author Peter Abeles
 */
public interface PixelTransform<T> {
	/**
	 * applies a transform to a pixel coordinate
	 *
	 * @param x Pixel x-coordinate
	 * @param y Pixel y-coordinate
	 * @param output The transformed pixel
	 */
	void compute( int x, int y, T output );

	/**
	 * Creates a copy of this transform for use in concurrent application. What that means is that any variable
	 * which might be modified by a concurrent call to {@link #compute} is not passed to the 'copied' output.
	 * Expensive to compute models might be passed in as a reference.
	 */
	PixelTransform<T> copyConcurrent();
}
