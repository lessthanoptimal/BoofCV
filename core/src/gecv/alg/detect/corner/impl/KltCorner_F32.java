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

import gecv.alg.detect.corner.KltCornerDetector;
import gecv.alg.detect.corner.impl.SsdCorner_F32;
import gecv.struct.image.ImageFloat32;


/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.KltCornerDetector} based off of {@link SsdCorner_F32}.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class KltCorner_F32 extends SsdCorner_F32 implements KltCornerDetector<ImageFloat32> {


	public KltCorner_F32(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	@Override
	protected float computeIntensity() {
		// compute the smallest eigenvalue
		float left = (totalXX + totalYY) * 0.5f;
		float b = (totalXX - totalYY) * 0.5f;
		double right = Math.sqrt(b * b + totalXY * totalXY);

		// the smallest eigenvalue will be minus the right side
		return (float)(left - right);
	}
}