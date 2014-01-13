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

/**
 * A uniform distribution from 0 to maxDistance, inclusive. If value is  greater than maxDistance it returns 0.
 * Does not check to see if the distance is less than zero.
 *
 * @author Peter Abeles
 */
public class WeightDistanceUniform_F32 implements WeightDistance_F32  {

	float maxDistance;
	float weight;

	public WeightDistanceUniform_F32(float maxDistance) {
		this.maxDistance = maxDistance;
		this.weight = 1.0f/ maxDistance;
	}

	@Override
	public float weight(float distance) {
		if( distance <= maxDistance)
			return weight;
		return 0;
	}
}
