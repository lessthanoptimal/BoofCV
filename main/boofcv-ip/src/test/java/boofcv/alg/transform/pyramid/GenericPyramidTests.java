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

package boofcv.alg.transform.pyramid;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Basic high level tests common to all PyramidUpdater
 *
 * @author Peter Abeles
 */
public abstract class GenericPyramidTests<T extends ImageGray<T>> extends BoofStandardJUnit {
	int width = 80;
	int height = 120;

	Class<T> imageType;

	protected GenericPyramidTests(Class<T> imageType) {
		this.imageType = imageType;
	}

	/**
	 * Checks to see if every layer in the pyramid has been modified on a call to update
	 */
	@Test
	void checkModifiesLayersOnUpdate() {
		T input = GeneralizedImageOps.createSingleBand(imageType, width, height);
		ImagePyramid<T> pyramid = createPyramid(3);
		GImageMiscOps.fillUniform(input, rand, 0, 100);

		pyramid.process(input);

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			T image = pyramid.getLayer(i);
			assertTrue( GImageStatistics.sum(image) > 0 );
		}
	}

	protected abstract ImagePyramid<T> createPyramid( int numLevels );

	@Test
	void copyStructure() {
		T input = GeneralizedImageOps.createSingleBand(imageType, width, height);
		ImagePyramid<T> src = createPyramid(3);
		GImageMiscOps.fillUniform(input, rand, 0, 100);

		src.process(input);

		ImagePyramid<T> cpy = src.copyStructure();

		cpy.process(input);

		// see if the structure is the same
		assertEquals(src.getNumLayers(),cpy.getNumLayers());
		for (int i = 0; i < src.getNumLayers(); i++) {
			assertEquals(src.getScale(i),cpy.getScale(i));
			assertEquals(src.getWidth(i),cpy.getWidth(i));
			assertEquals(src.getHeight(i),cpy.getHeight(i));
			assertEquals(src.getSampleOffset(i),cpy.getSampleOffset(i));
		}

		// process an image and the data should now be the same
		cpy.process(input);
		for (int i = 0; i < src.getNumLayers(); i++) {
			BoofTesting.assertEquals(src.getLayer(i),cpy.getLayer(i),1e-4);
		}
	}
}
