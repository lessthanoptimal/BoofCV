/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.intensity.impl;

import gecv.alg.detect.intensity.HarrisCornerIntensity;
import gecv.struct.image.ImageSInt16;

/**
 * <p>
 * Implementation of {@link gecv.alg.detect.intensity.HarrisCornerIntensity} based off of {@link SsdCorner_S16}.
 * </p>
 *
 * @author Peter Abeles
 */
public class HarrisCorner_S16 extends SsdCorner_S16 implements HarrisCornerIntensity<ImageSInt16> {

	// tuning parameter
	float kappa;

	public HarrisCorner_S16(int windowRadius, float kappa) {
		super(windowRadius);
		this.kappa = kappa;
	}

	@Override
	public void setKappa(float kappa) {
		this.kappa = kappa;
	}

	@Override
	protected float computeIntensity() {
		// det(A) + kappa*trace(A)^2
		float trace = totalXX + totalYY;
		return (totalXX * totalYY - totalXY * totalXY) + kappa * trace*trace;
	}

	@Override
	public float getKappa() {
		return kappa;
	}
}
