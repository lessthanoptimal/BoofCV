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
 * The distribution is a cropped Gaussian distribution with mean at 0.  Note
 * that this matches the shape of the distribution but is not correctly normalized.  Input is assumed to be
 * the distance squared.  Does not check to see if distance is less than zero
 *
 * @author Peter Abeles
 */
public class WeightDistanceSqGaussian_F32 implements WeightDistance_F32  {

	float sigma;

	public WeightDistanceSqGaussian_F32(float sigma) {
		this.sigma = sigma;
	}

	@Override
	public float weight(float distanceSq) {
		return (float)Math.exp( - distanceSq/(2.0f*sigma*sigma) );
	}
}
