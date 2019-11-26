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

/**
 * Specifies number of layers in the pyramid. This can be done explicity or based on the minimum allowed width
 * and height.
 *
 * @author Peter Abeles
 */
public class ConfigPyramid2 {
	/** if not -1 then it specifies the number of levels in the pyramid */
	public int numLevelsRequested =-1;
	/** if not -1 then it specifies the minimum width/height of the highest level in the pyramid */
	public int minWidth=-1,minHeight=-1;

	public ConfigPyramid2() {
	}

	public ConfigPyramid2(int numLevelsRequested, int minWidth, int minHeight) {
		this.numLevelsRequested = numLevelsRequested;
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}

	/**
	 * Compute the number of layers in a pyramid given the input image's shape.
	 * @param width Input image's width
	 * @param height Input image's height
	 * @return Number of layers
	 */
	public int computeLayers( int width , int height ) {
		if( numLevelsRequested > 0 ) {
			return numLevelsRequested;
		} else if( minWidth > 0 ) {
			return computeNumLevels(width,minWidth);
		} else if( minHeight > 0 ) {
			return computeNumLevels(height,minHeight);
		} else {
			throw new IllegalArgumentException("Need to specify numLevels or minWidth or minHeight");
		}
	}

	int computeNumLevels( int length , int minLength ) {
		double scale = length/(double)minLength;
		double levels = Math.log(scale)/Math.log(2);
		return (int)Math.floor(levels)+1;
	}

	public void set( ConfigPyramid2 config ) {
		this.numLevelsRequested = config.numLevelsRequested;
		this.minWidth = config.minWidth;
		this.minHeight = config.minHeight;
	}
}
