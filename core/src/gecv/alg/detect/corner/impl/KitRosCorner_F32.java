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

import gecv.alg.detect.corner.KitRosCornerIntensity;
import gecv.struct.image.ImageFloat32;

/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.KitRosCornerIntensity} based off of {@link SsdCorner_F32}.
 * </p>
 *
 * @author Peter Abeles
 */
public class KitRosCorner_F32 extends SsdCorner_F32 implements KitRosCornerIntensity<ImageFloat32> {

	public KitRosCorner_F32(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	protected float computeIntensity() {
		// accessing the derivative images in this fashion is likely causes a large performance hit
		// in benchmark tests it does perform significantly slower than Harris and this is the most likely
		// culprit.
		double dX = derivX.get(x, y);
		double dY = derivY.get(x, y);

		double xx = dX * dX;
		double yy = dY * dY;

		double bottom = xx + yy;

		if (bottom == 0.0)
			return 0;

		double top = totalXX * yy - 2 * totalXY * dX * dY + totalYY * xx;
		return (float)(top / bottom);
	}
}
