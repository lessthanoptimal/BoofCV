/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.shape;

import boofcv.alg.shapes.polygon.RefinePolygonLineToImage;
import boofcv.struct.Configuration;

/**
 * Configuration parameters for {@link RefinePolygonLineToImage}
 *
 * @author Peter Abeles
 */
public class ConfigRefinePolygonLineToImage implements Configuration {

	/**
	 * How far away in pixels it will start sampling the line from a corner.  Corners can become highly aliased
	 * with ambiguous borders.
	 */
	public double cornerOffset = 1;
	/**
	 * Number of times along the line it will be sampled
	 */
	public int lineSamples = 30;
	/**
	 * Number of points tangent to the line in each direction it samples.  In total the number of
	 * samples along a line will be lineSamples*(2*sampleRadius+1)
	 */
	public int sampleRadius = 1;
	/**
	 * Maximum number of iterations
	 */
	public int maxIterations = 10;
	/**
	 * Convergence tolerance in pixels
	 */
	public double convergeTolPixels = 0.2;

	/**
	 * Maximum number of pixels a corner can be adjusted in a single iteration.  This becomes important with very
	 * small shapes and can prevent divergence.
	 */
	public double maxCornerChangePixel = 2.0;

	@Override
	public void checkValidity() {

	}

	@Override
	public String toString() {
		return "ConfigRefinePolygonLineToImage{" +
				"cornerOffset=" + cornerOffset +
				", lineSamples=" + lineSamples +
				", sampleRadius=" + sampleRadius +
				", maxIterations=" + maxIterations +
				", convergeTolPixels=" + convergeTolPixels +
				", maxCornerChangePixel=" + maxCornerChangePixel +
				'}';
	}
}
