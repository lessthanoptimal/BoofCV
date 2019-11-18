/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

/**
 * Discrete image pyramid where each level is always a factor of two and sub-sampled using nearest-neighbor
 * interpolation
 *
 * @author Peter Abeles
 */
public class PyramidDiscreteNN2<T extends ImageBase<T>> {

	ImageType<T> imageType;

	FDistort distort;

	// if not -1 then it specifies the number of levels in the pyramid
	int numLevels=-1;
	// if not -1 then it specifies the minimum width/height of the highest level in the pyramid
	int minWidth=-1,minHeight=-1;


	// Levels in the pyramid
	T[] levels;

	public PyramidDiscreteNN2( ImageType<T> imageType ) {
		this.imageType = imageType;
		levels = imageType.createArray(0);

		distort = new FDistort(imageType);
		distort.interpNN();
	}

	public void process(T input) {
		if( numLevels > 0 ) {
			if( levels.length != numLevels ) {
				declareArray(numLevels);
			}
		} else if( minWidth > 0 ) {
			int numLevels = Math.max(1,input.width/minWidth);
			if( levels.length != numLevels )
				declareArray(numLevels);
		} else if( minHeight > 0 ) {
			int numLevels = Math.max(1,input.height/minHeight);
			if( levels.length != numLevels )
				declareArray(numLevels);
		} else {
			throw new IllegalArgumentException("Need to specify numLevels or minWidth or minHeight");
		}

		// level 0 is always the input image
		levels[0] = input;
		// scale down each image by a factor of two relative to the previous level
		int scale = 2;
		for (int level = levels.length-2; level >= 0; level--) {
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

	public ImageType<T> getImageType() {
		return imageType;
	}

	public int getNumLevels() {
		return numLevels;
	}

	public void setNumLevels(int numLevels) {
		this.numLevels = numLevels;
	}

	public int getMinWidth() {
		return minWidth;
	}

	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
	}
}
