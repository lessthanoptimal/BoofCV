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
 * A uniform distribution from 0 to maxWeight.
 *
 * @author Peter Abeles
 */
public class WeightDistanceUniform_F32 implements WeightDistance_F32  {

	float maxWeight;
	float weight;

	public WeightDistanceUniform_F32(float maxWeight) {
		this.maxWeight = maxWeight;
		this.weight = 1.0f/maxWeight;
	}

	@Override
	public float weight(float distance) {
		if( distance <= maxWeight )
			return weight;
		return 0;
	}
}
