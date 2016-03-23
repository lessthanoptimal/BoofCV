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

package boofcv.alg.interpolate;

import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestInterpolatePixel_S_to_MB {
	@Test
	public void checkFunctions() {
		Helper helper = new Helper();

		InterpolatePixelMB alg = new InterpolatePixel_S_to_MB(helper);

		float pixel[] = new float[1];

		assertEquals(1, alg.getImageType().getNumBands());
		alg.get(2, 3, pixel);
		assertEquals(2, pixel[0], 1e-8);
		alg.get_fast(2, 3, pixel);
		assertEquals(3, pixel[0], 1e-8);
		assertTrue(alg.isInFastBounds(20, 4));
		assertFalse(alg.isInFastBounds(0, 4));
		assertEquals(10, alg.getFastBorderX());
		assertEquals(11, alg.getFastBorderY());

		ImageBorder border = FactoryImageBorder.genericValue(0,alg.getImageType());
		alg.setBorder(border);
		assertTrue(border == alg.getBorder());
	}

	class Helper implements InterpolatePixelS {

		ImageBorder border;
		ImageBase image;

		@Override
		public float get(float x, float y) {
			return 2;
		}

		@Override
		public float get_fast(float x, float y) {
			return 3;
		}

		@Override
		public void setBorder(ImageBorder border) {
			this.border = border;
		}

		@Override
		public ImageBorder getBorder() {
			return border;
		}

		@Override
		public void setImage(ImageBase image) {
			this.image = image;
		}

		@Override
		public ImageBase getImage() {
			return image;
		}

		@Override
		public boolean isInFastBounds(float x, float y) {
			return x >= 10 && x <= 100;
		}

		@Override
		public int getFastBorderX() {
			return 10;
		}

		@Override
		public int getFastBorderY() {
			return 11;
		}

		@Override
		public ImageType getImageType() {
			return ImageType.single(GrayU8.class);
		}
	}
}