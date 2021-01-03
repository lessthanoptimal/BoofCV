/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Adds tests specific to integer images
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"PointlessBooleanExpression"})
public abstract class StandardImageIntegerTests<T extends GrayI<T>> extends StandardSingleBandTests<T> {
	boolean expectedSign;

	protected StandardImageIntegerTests( boolean expectedSign ) {
		this.expectedSign = expectedSign;
	}

	@Test void checkSign() {
		GrayI<?> img = (GrayI<?>)createImage(10, 10);

		assertEquals(expectedSign, img.getDataType().isSigned());
	}

	@Test void forEachPixel() {
		T image = createImage(8, 12);
		setRandom(image);

		image.forEachPixel(( x, y, v ) -> {
			assertEquals(image.get(x, y), v);
			image.set(x, y, y*image.width + x);
		});

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				assertEquals(y*image.width + x, image.get(x, y));
			}
		}
	}
}
