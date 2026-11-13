/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image.border;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"rawtypes", "unchecked"})
class TestFactoryImageBorder extends BoofStandardJUnit {
	Class[] grayTypes = {GrayU8.class, GrayS8.class, GrayU16.class, GrayS16.class, GrayS32.class, GrayS64.class,
			GrayF32.class, GrayF64.class};

	/// Every gray image type can have a border with a constant value
	@Test void singleValue_everyGrayType() {
		for (Class type : grayTypes) {
			ImageBorder border = FactoryImageBorder.singleValue(7, type);
			border.setImage(GeneralizedImageOps.createSingleBand(type, 5, 4));
			assertEquals(7.0, getGeneral(border, -1, 2), type.getSimpleName());
			assertEquals(7.0, getGeneral(border, 5, 4), type.getSimpleName());
		}
	}

	/// Every gray image type can have every border which reads pixels outside the image
	@Test void single_everyGrayType() {
		BorderType[] borders = {BorderType.EXTENDED, BorderType.REFLECT, BorderType.WRAP, BorderType.ZERO};
		for (Class type : grayTypes) {
			ImageGray image = GeneralizedImageOps.createSingleBand(type, 5, 4);
			GeneralizedImageOps.set(image, 0, 2, 3);
			for (BorderType borderType : borders) {
				ImageBorder border = FactoryImageBorder.single(borderType, type);
				border.setImage(image);
				// one pixel to the left of (0,2) is (0,2) for extended, (1,2) for reflect, (4,2) for wrap
				double expected = borderType == BorderType.EXTENDED ? 3 : 0;
				assertEquals(expected, getGeneral(border, -1, 2), type.getSimpleName() + " " + borderType);
			}
		}
	}

	private static double getGeneral( ImageBorder border, int x, int y ) {
		double[] pixel = new double[1];
		border.getGeneral(x, y, pixel);
		return pixel[0];
	}
}
