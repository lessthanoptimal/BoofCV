/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.slic;

import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;

/**
 * Implementation of {@link SegmentSlic} for image of type {@link GrayF32}.
 *
 * @author Peter Abeles
 */
public class SegmentSlic_F32 extends SegmentSlic<GrayF32> {
	public SegmentSlic_F32(int numberOfRegions, float m, int totalIterations,
						   ConnectRule connectRule) {
		super(numberOfRegions, m , totalIterations, connectRule,ImageType.single(GrayF32.class));
	}

	@Override
	public void setColor(float[] color, int x, int y) {
		color[0] = input.unsafe_get(x,y);
	}

	@Override
	public void addColor(float[] color, int index, float weight) {
		color[0] += input.data[index]*weight;
	}

	@Override
	public float colorDistance(float[] color, int index) {
		float difference = color[0] - input.data[index];
		return difference*difference;
	}

	@Override
	public float getIntensity(int x, int y) {
		return input.get(x,y);
	}
}
