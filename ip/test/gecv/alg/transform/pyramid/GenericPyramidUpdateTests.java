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

package gecv.alg.transform.pyramid;

import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.PyramidUpdater;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Basic high level tests common to all PyramidUpdater
 *
 * @author Peter Abeles
 */
public abstract class GenericPyramidUpdateTests<T extends ImageBase> {

	Random rand = new Random(234);
	int width = 80;
	int height = 120;

	Class<T> imageType;

	protected GenericPyramidUpdateTests(Class<T> imageType) {
		this.imageType = imageType;
	}

	/**
	 * Make sure uninitialized pyramids are initialized upon a call to update
	 */
	public void checkInitialized() {

		ImageBase input = GeneralizedImageOps.createImage(imageType,width,height);
		ImagePyramid pyramid = createPyramid(1,2,4);

		assertFalse(pyramid.isInitialized());

		PyramidUpdater updater = createUpdater();
		updater.update(input,pyramid);

		assertTrue(pyramid.isInitialized());
	}

	/**
	 * Checks to see if every layer in the pyramid has been modified on a call to update
	 */
	public void checkModifiesLayersOnUpdate() {
		ImageBase input = GeneralizedImageOps.createImage(imageType,width,height);
		ImagePyramid pyramid = createPyramid(1,2,4);
		GeneralizedImageOps.randomize(input,rand,0,100);

		PyramidUpdater updater = createUpdater();
		updater.update(input,pyramid);

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			ImageBase image = pyramid.getLayer(i);

			assertTrue( GeneralizedImageOps.sum(image) > 0 );
		}
	}

	protected abstract PyramidUpdater createUpdater();

	protected abstract ImagePyramid<T> createPyramid( int... scales);
}
