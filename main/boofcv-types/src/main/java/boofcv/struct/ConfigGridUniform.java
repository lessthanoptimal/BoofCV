/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
 * Configuration for uniformly sampling points inside an image using a grid.
 *
 * @author Peter Abeles
 */
public class ConfigGridUniform implements Configuration {
	/** Scales the size of a region up by this amount */
	public double regionScaleFactor = 2.0;

	/** The smallest allowed cell size in pixels */
	public int minCellLength = 5;

	public ConfigGridUniform( double regionScaleFactor, int minCellLength ) {
		this.regionScaleFactor = regionScaleFactor;
		this.minCellLength = minCellLength;
	}

	public ConfigGridUniform() {}

	/**
	 * Selects the desired length of a cell based on the input image size and maximum number of points returned
	 *
	 * @param maxSample The maximum number of points/features which can be returned.
	 */
	public int selectTargetCellSize( int maxSample, int imageWidth, int imageHeight ) {
		if (maxSample <= 0)
			throw new IllegalArgumentException("maxSample must be a positive number");
		int targetLength = (int)Math.ceil(regionScaleFactor*Math.sqrt(imageWidth*imageHeight)/
				Math.sqrt(maxSample));
		targetLength = Math.max(minCellLength, targetLength);
		return targetLength;
	}

	@Override
	public void checkValidity() {
		if (regionScaleFactor <= 0)
			throw new IllegalArgumentException("Must be greater than 0");
		if (minCellLength <= 0)
			throw new IllegalArgumentException("Must be greater than 0");
		if (regionScaleFactor < 1.0)
			throw new IllegalArgumentException("Scale factor must be greater than zero");
	}

	public ConfigGridUniform setTo( ConfigGridUniform src ) {
		this.regionScaleFactor = src.regionScaleFactor;
		this.minCellLength = src.minCellLength;
		return this;
	}

	public ConfigGridUniform copy() {
		return new ConfigGridUniform().setTo(this);
	}
}
