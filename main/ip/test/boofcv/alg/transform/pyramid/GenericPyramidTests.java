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

package boofcv.alg.transform.pyramid;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * Basic high level tests common to all PyramidUpdater
 *
 * @author Peter Abeles
 */
public abstract class GenericPyramidTests<T extends ImageGray> {

	Random rand = new Random(234);
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
	public void checkModifiesLayersOnUpdate() {
		T input = GeneralizedImageOps.createSingleBand(imageType, width, height);
		ImagePyramid<T> pyramid = createPyramid(1,2,4);
		GImageMiscOps.fillUniform(input, rand, 0, 100);

		pyramid.process(input);

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			T image = pyramid.getLayer(i);
			assertTrue( GImageStatistics.sum(image) > 0 );
		}
	}

	protected abstract ImagePyramid<T> createPyramid( int... scales);
}
