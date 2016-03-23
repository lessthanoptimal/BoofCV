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

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;


/**
 * Generic class for testing bilinear interpolation
 *
 * @author Peter Abeles
 */
public abstract class GeneralBilinearPixelMultiChecks<T extends ImageInterleaved> extends GeneralChecksInterpolationPixelMB<T> {

	@Override
	protected InterpolatePixelMB<T> wrap(T image, int minValue, int maxValue) {
		return FactoryInterpolation.bilinearPixelMB(image, null);
	}

	@Override
	protected <SB extends ImageGray> InterpolatePixelS<SB> wrapSingle(SB image, int minValue, int maxValue) {
		return FactoryInterpolation.bilinearPixelS(image, null);
	}

	@Override
	protected void compute(T _img, float x, float y, float pixel[] ) {
		ImageBorder<T> imgB = FactoryImageBorder.interleaved(_img, BorderType.EXTENDED);
		GImageMultiBand img = FactoryGImageMultiBand.wrap(imgB);

		float []X0Y0 = new float[ _img.getNumBands() ];
		float []X1Y0 = new float[ _img.getNumBands() ];
		float []X1Y1 = new float[ _img.getNumBands() ];
		float []X0Y1 = new float[ _img.getNumBands() ];

		int gX = (int) x;
		int gY = (int) y;

		img.get(gX  , gY  , X0Y0);
		img.get(gX+1, gY  , X1Y0);
		img.get(gX  , gY+1, X0Y1);
		img.get(gX+1, gY+1, X1Y1);

		for (int i = 0; i < _img.getNumBands(); i++) {
			float v0 = X0Y0[i];
			float v1 = X1Y0[i];
			float v2 = X0Y1[i];
			float v3 = X1Y1[i];

			x %= 1f;
			y %= 1f;

			float a = 1f - x;
			float b = 1f - y;

			pixel[i] = a * b * v0 + x * b * v1 + a * y * v2 + x * y * v3;
		}
	}
}
