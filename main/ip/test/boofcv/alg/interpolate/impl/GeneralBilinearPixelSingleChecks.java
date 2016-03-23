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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;


/**
 * Generic class for testing bilinear interpolation
 *
 * @author Peter Abeles
 */
public abstract class GeneralBilinearPixelSingleChecks<T extends ImageGray> extends GeneralChecksInterpolationPixelS<T> {

	@Override
	protected InterpolatePixelS<T> wrap(T image, int minValue, int maxValue) {
		return FactoryInterpolation.bilinearPixelS(image,null);
	}

	@Override
	protected float compute(T _img, float x, float y) {
		ImageBorder<?> imgB = FactoryImageBorder.single(_img, BorderType.EXTENDED);
		GImageGray img = FactoryGImageGray.wrap(imgB);

		int gX = (int) x;
		int gY = (int) y;

		float v0 = img.get(gX, gY).floatValue();
		float v1 = img.get(gX + 1, gY).floatValue();
		float v2 = img.get(gX, gY + 1).floatValue();
		float v3 = img.get(gX + 1, gY + 1).floatValue();

		x %= 1f;
		y %= 1f;

		float a = 1f - x;
		float b = 1f - y;

		return a * b * v0 + x * b * v1 + a * y * v2 + x * y * v3;
	}
}
