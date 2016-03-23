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
import boofcv.struct.image.GrayS16;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.detect.intensity.HarrisCornerIntensity} based off of {@link ImplSsdCorner_S16}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplHarrisCorner_S16 extends ImplSsdCorner_S16 implements HarrisCornerIntensity<GrayS16> {

	// tuning parameter
	float kappa;

	public ImplHarrisCorner_S16(int windowRadius, float kappa) {
		super(windowRadius);
		this.kappa = kappa;
	}

	@Override
	public void setKappa(float kappa) {
		this.kappa = kappa;
	}

	@Override
	protected float computeIntensity() {
		// det(A) -+ kappa*trace(A)^2
		float totalXX = this.totalXX, totalYY = this.totalYY, totalXY = this.totalXY;
		float trace = totalXX + totalYY;

		return (totalXX * totalYY - totalXY * totalXY) - kappa * trace*trace;
	}

	@Override
	public float getKappa() {
		return kappa;
	}
}
