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

package boofcv.alg.weights;

/**
 * Weights from a uniform distribution within a symmetric square region. All samples within the region return
 * a constant value, which is 1.0/w^2, where w = radius*2+1. For performance reasons, no checks are done to see if the
 * sample point is outside the radius and should be zero.
 *
 * @author Peter Abeles
 */
public class WeightPixelUniform_F32 implements WeightPixel_F32 {
	int radiusX,radiusY;
	float weight;
	boolean odd;

	public WeightPixelUniform_F32() {
	}

	public WeightPixelUniform_F32(int radiusX, int radiusY, boolean odd ) {
		setRadius(radiusX,radiusY,odd);
	}

	@Override
	public float weightIndex(int index) {
		return weight;
	}

	@Override
	public float weight(int x, int y) {
		return weight;
	}

	@Override
	public void setRadius(int radiusX, int radiusY, boolean odd ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.odd = odd;

		int w = radiusX*2;
		int h = radiusY*2;
		if( odd ) {
			w += 1;
			h += 1;
		}
		weight = 1.0f/(w*h);
	}

	@Override
	public int getRadiusX() {
		return radiusX;
	}

	@Override
	public int getRadiusY() {
		return radiusY;
	}

	@Override
	public boolean isOdd() {
		return odd;
	}
}
