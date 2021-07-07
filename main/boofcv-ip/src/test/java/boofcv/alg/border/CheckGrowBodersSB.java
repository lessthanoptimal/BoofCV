/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.border;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class CheckGrowBodersSB<T extends ImageGray<T>> extends BoofStandardJUnit {
	int width = 30, height = 20;

	double minPixel, maxPixel;

	int borderLower = 2;
	int borderUpper = 3;

	T image;

	ImageBorder<T> border;

	protected CheckGrowBodersSB( double minPixel, double maxPixel, ImageType<T> imageType ) {
		this.minPixel = minPixel;
		this.maxPixel = maxPixel;
		image = imageType.createImage(width, height);
		GImageMiscOps.fillUniform(image, rand, minPixel, maxPixel);

		border = FactoryImageBorder.generic(BorderType.REFLECT, imageType);
	}

	protected abstract GrowBorderSB<T, ?> createAlg();

	@Test void rows() {
		GrowBorderSB alg = createAlg();
		int length = width + borderLower + borderUpper;
		Object data = image.getDataType().newArray(length);

		boolean signed = image.getDataType().isSigned();

		alg.setBorder(border);
		alg.setImage(image);
		for (int y : new int[]{-2, 0, 2, 3, height - 1, height + 1}) {
			alg.growRow(y, borderLower, borderUpper, data, 0);

			for (int x = 0; x < length; x++) {
				double expected = GeneralizedImageOps.get(border, x - borderLower, y);
				double found = GeneralizedImageOps.arrayElement(data, x, signed);

				assertEquals(expected, found, 1e-8, "x = " + x);
			}
		}
	}

	@Test void columns() {
		GrowBorderSB alg = createAlg();
		int length = height + borderLower + borderUpper;
		Object data = image.getDataType().newArray(length);

		boolean signed = image.getDataType().isSigned();

		alg.setBorder(border);
		alg.setImage(image);
		for (int x : new int[]{-2, 0, 2, 3, width - 1, width + 1}) {
			alg.growCol(x, borderLower, borderUpper, data, 0);

			for (int y = 0; y < length; y++) {
				double expected = GeneralizedImageOps.get(border, x, y - borderLower);
				double found = GeneralizedImageOps.arrayElement(data, y, signed);
				assertEquals(expected, found, 1e-8, x + " y = " + y);
			}
		}
	}
}
