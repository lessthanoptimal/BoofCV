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

package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.HarrisCornerDetector;
import gecv.alg.detect.corner.impl.SsdCorner_I16;
import gecv.struct.image.ImageInt16;

/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.HarrisCornerDetector} based off of {@link SsdCorner_I16}.
 * </p>
 *
 * @author Peter Abeles
 */
public class HarrisCornerDetector_I16 extends SsdCorner_I16 implements HarrisCornerDetector<ImageInt16> {

	float kappa = 0.04f;

	public HarrisCornerDetector_I16(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	@Override
	protected float computeIntensity() {
		// det(A) + kappa*trace(A)
		return totalXX * totalYY - totalXY * totalXY + kappa * (totalXX + totalYY);
	}

	@Override
	public float getKappa() {
		return kappa;
	}
}
