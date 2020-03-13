/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.distort.FDistort;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Discrete image pyramid where each level is always a factor of two and sub-sampled using nearest-neighbor
 * interpolation
 *
 * @author Peter Abeles
 */
public class PyramidDiscreteNN2<T extends ImageBase<T>> {

	ImageType<T> imageType;

	FDistort distort;

	private ConfigDiscreteLevels configLayers = new ConfigDiscreteLevels();

	// Levels in the pyramid
	T[] levels;

	public PyramidDiscreteNN2( ImageType<T> imageType ) {
		this.imageType = imageType;
		levels = imageType.createArray(0);

		distort = new FDistort(imageType);
		distort.interpNN();
	}

	public void process(T input) {
		int requestedLayers = configLayers.computeLayers(input.width,input.height);
		if( levels.length != requestedLayers ) {
			declareArray(requestedLayers);
		}

		// level 0 is always the input image
		levels[0] = input;
		// scale down each image by a factor of two relative to the previous level
		int scale = 2;
		for (int level = 1; level < levels.length; level++) {
			int width = input.width/scale;
			int height = input.height/scale;

			levels[level].reshape(width,height);

			distort.input(levels[level-1]);
			distort.output(levels[level]);
			distort.scaleExt();
			distort.apply();

			scale *= 2;
		}
	}

	private void declareArray( int numLevels ) {
		levels = imageType.createArray(numLevels);
		for (int i = 1; i < levels.length; i++) {
			levels[i] = imageType.createImage(1,1);
		}
	}

	public T get( int i ) {
		return levels[i];
	}

	public T getLayer( int i ) {
		return levels[i];
	}

	public ImageType<T> getImageType() {
		return imageType;
	}


	public int getLevelsCount() {
		return levels.length;
	}

	public ConfigDiscreteLevels getConfigLayers() {
		return configLayers;
	}
}
