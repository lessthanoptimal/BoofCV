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

package boofcv.struct.weights;

/**
 * Weights from a uniform distribution.
 *
 * @author Peter Abeles
 */
public class WeightPixelUniform_F32 implements WeightPixel_F32 {
	int radius;
	float weight;

	public WeightPixelUniform_F32() {
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
	public void setRadius(int radius) {
		this.radius = radius;
		int w = radius*2+1;
		weight = 1.0f/(w*w);
	}

	@Override
	public int getRadius() {
		return radius;
	}
}
