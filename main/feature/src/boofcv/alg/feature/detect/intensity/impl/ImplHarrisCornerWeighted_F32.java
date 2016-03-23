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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.HarrisCornerIntensity;
import boofcv.struct.image.GrayF32;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.detect.intensity.HarrisCornerIntensity}
 * that samples pixels using a Gaussian distribution and is based off of {@link ImplSsdCornerWeighted_F32}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplHarrisCornerWeighted_F32 extends ImplSsdCornerWeighted_F32
		implements HarrisCornerIntensity<GrayF32>
{
	float kappa;

	public ImplHarrisCornerWeighted_F32(int windowRadius, float kappa) {
		super(windowRadius);
		this.kappa = kappa;
	}

	@Override
	protected float computeResponse() {
		// det(A) - kappa*trace(A)^2
		float trace = totalXX + totalYY;
		return (totalXX * totalYY - totalXY * totalXY) - kappa * trace*trace;
	}

	@Override
	public float getKappa() {
		return kappa;
	}

	@Override
	public void setKappa(float kappa) {
		this.kappa = kappa;
	}
}
