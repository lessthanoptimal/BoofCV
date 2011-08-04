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

import gecv.core.image.ImageGenerator;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

/**
 * @author Peter Abeles
 */
public class CompanionPyramid<T extends ImageBase> {

	ImageGenerator<T> inputGen;

	T images[];

	public CompanionPyramid(ImageGenerator<T> inputGen) {
		this.inputGen = inputGen;
	}

	public CompanionPyramid( Class<T> imageType ) {
		this.inputGen = FactoryImageGenerator.create(imageType);
	}

	public T getLayer( int layer ) {
		return images[layer];
	}

	public void attach( ImagePyramid<?> pyramid ) {
		if( images == null ) {
			declare(pyramid);
		} else {
			if( checkPyramidChange(pyramid) ) {
				declare(pyramid);
			}
		}
	}

	/**
	 * Checks to see if the pyramids structure has changed.
	 */
	private boolean checkPyramidChange(ImagePyramid<?> pyramid) {

		if( pyramid.getNumLayers() != images.length ) {
			return true;
		} else {
			for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
				int w = pyramid.getWidth(i);
				int h = pyramid.getHeight(i);

				T a = images[i];

				if( a.width != w || a.height != h ) {
					return true;
				}

				images[i] = inputGen.createInstance(w,h);
			}
		}
		return false;
	}

	private void declare(ImagePyramid<?> pyramid) {
		images = inputGen.createArray(pyramid.getNumLayers());
		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			int w = pyramid.getWidth(i);
			int h = pyramid.getHeight(i);

			images[i] = inputGen.createInstance(w,h);
		}
	}
}
