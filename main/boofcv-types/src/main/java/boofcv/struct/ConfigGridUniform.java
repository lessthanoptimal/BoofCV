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

package boofcv.struct;

/**
 * Configuration for uniformally sampling points inside an image using a grid.
 *
 * @author Peter Abeles
 */
public class ConfigGridUniform implements Configuration {
	/** Scales the size of a region up by the inverse of this number */
	public double inverseRegionScale = 0.25;
	/** The smallest allowed cell size in pixels */
	public int minCellLength = 5;

	/**
	 * Selects the desired length of a cell
	 */
	public int selectTargetCellSize( int maxSample, int imageWidth, int imageHeight) {
		int targetLength = (int)Math.ceil(Math.sqrt(imageWidth*imageHeight)/
				(0.1+Math.sqrt(maxSample*inverseRegionScale)));
		targetLength = Math.max(minCellLength,targetLength);
		return targetLength;
	}

	@Override
	public void checkValidity() {

	}
}
