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

import gecv.alg.detect.intensity.KltCornerIntensity;
import gecv.struct.image.ImageSInt16;


/**
 * <p>
 * Implementation of {@link gecv.alg.detect.intensity.KltCornerIntensity} based off of {@link SsdCornerNaive_S16}.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class KltCorner_S16 extends SsdCorner_S16 implements KltCornerIntensity<ImageSInt16> {
	public KltCorner_S16(int windowRadius) {
		super(windowRadius);
	}

	@Override
	protected float computeIntensity() {
		// compute the smallest eigenvalue
		double left = (totalXX + totalYY) * 0.5f;
		double b = (totalXX - totalYY) * 0.5f;
		double right = Math.sqrt(b * b + (double)totalXY * totalXY);

		// the smallest eigenvalue will be minus the right side
		return (float)(left - right);
	}
}