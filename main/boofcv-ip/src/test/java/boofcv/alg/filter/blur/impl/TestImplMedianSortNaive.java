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

package boofcv.alg.filter.blur.impl;

import boofcv.BoofTesting;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestImplMedianSortNaive extends BoofStandardJUnit {

	@Test void trivial() {

		GrayU8 templateImage = new GrayU8(4, 4);
		for (int i = 0; i < templateImage.width; i++) {
			for (int j = 0; j < templateImage.height; j++) {
				templateImage.set(j, i, i*templateImage.width + j);
			}
		}

		int numFound = 0;
		Method[] methods = ImplMedianSortNaive.class.getMethods();

		for (Method m : methods) {
			if (!m.getName().equals("process"))
				continue;
			if (m.getParameterTypes().length != 5)
				continue;
			Class[] params = m.getParameterTypes();
			if (params[0] != GrayI.class && params[0] != GrayF32.class)
				continue;

			ImageGray input = GeneralizedImageOps.createSingleBand(params[0], 5, 4);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[1], 5, 4);

			GConvertImage.convert(templateImage, input);

			BoofTesting.checkSubImage(this, "trivialTest", true, m, input, found);
			numFound++;
		}

		assertEquals(2, numFound);
	}

	public void trivialTest( Method m, ImageGray _image, ImageGray _found ) {

		try {
			m.invoke(null, _image, _found, 1, 1, null);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		GImageGray found = FactoryGImageGray.wrap(_found);

		assertEquals(5, found.get(1, 1).intValue());
		assertEquals(6, found.get(2, 1).intValue());
		assertEquals(9, found.get(1, 2).intValue());
		assertEquals(10, found.get(2, 2).intValue());

		// check the edges
		assertEquals(4, found.get(0, 0).intValue());
		assertEquals(5, found.get(2, 0).intValue());
		assertEquals(13, found.get(2, 3).intValue());
	}
}
