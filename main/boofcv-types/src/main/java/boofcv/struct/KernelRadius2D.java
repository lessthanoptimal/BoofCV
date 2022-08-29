/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.struct;

/**
 * Specifies a size of a 2D kernel with a radius along each axis.
 *
 * @author Peter Abeles
 */
public class KernelRadius2D {
	public int radiusX;
	public int radiusY;

	public KernelRadius2D() {
	}

	public KernelRadius2D( int radiusX, int radiusY ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
	}

	public int getLargestAxis() {
		return Math.max(radiusX, radiusY);
	}

	public void reset() {
		radiusX = 0;
		radiusY = 0;
	}

	public KernelRadius2D setTo( int radiusX, int radiusY ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		return this;
	}

	public KernelRadius2D setTo( KernelRadius2D src ) {
		this.radiusX = src.radiusX;
		this.radiusY = src.radiusY;
		return this;
	}
}
