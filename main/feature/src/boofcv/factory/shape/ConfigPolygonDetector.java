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

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector} for use in {@link FactoryShapeDetector}.
 *
 * @author Peter Abeles
 */
public class ConfigPolygonDetector implements Configuration {

	/**
	 * Number of sides in the polygon it's trying to detect
	 */
	public int numberOfSides = -1;

	/**
	 * Two lines are merged together if their angle is <= to this number.
	 */
	public double contour2Poly_mergeTolerance = 0.1;

	/**
	 * Number of split and merge iterations when converting contour into polygon
	 */
	public int contour2Poly_iterations = 20;

	/**
	 * Number of pixels, as a fraction of contour length, to split a new line in SplitMergeLineFitLoop.
	 */
	public double contour2Poly_splitDistanceFraction = 0.04;

	/**
	 * Subpixel refinement using lines.  This assumes that the lines are straight so it works best when lens
	 * distortion has been removed.
	 */
	public boolean refineWithLines = true;

	/**
	 * Subpixel refinement using corners.  Doesn't require straight lines so it is more tolerant of distortion.
	 * However not as much information is being used so the fit might not be as good under ideal situations.
	 */
	public boolean refineWithCorners = false;

	/**
	 * Size of minimum shape contour as a fraction of the input image's width.
	 */
	public double minContourImageWidthFraction = 0.23;

	/**
	 * Will the found polygons be in clockwise order?
	 */
	public boolean clockwise = true;

	/**
	 * Configuration for refining with lines.  Ignored if not used.
	 */
	public ConfigRefinePolygonLineToImage configRefineLines = new ConfigRefinePolygonLineToImage();

	/**
	 * Configuration for refining with corners.  Ignored if not used.
	 */
	public ConfigRefinePolygonCornersToImage configRefineCorners = new ConfigRefinePolygonCornersToImage();

	/**
	 * Specifies the number of sides in the polygon and uses default settings for everything else
	 */
	public ConfigPolygonDetector(int numberOfSides) {
		this.numberOfSides = numberOfSides;
	}

	public ConfigPolygonDetector(int numberOfSides, boolean clockwise) {
		this.numberOfSides = numberOfSides;
		this.clockwise = clockwise;
	}

	@Override
	public void checkValidity() {
		if( (refineWithCorners && refineWithLines) ) {
			throw new IllegalArgumentException("Can't refine with both corners and lines");
		}

		if( refineWithLines && !configRefineLines.insideBlack)
			throw new IllegalArgumentException("The detector only support black objects so refine lines fitBlack must be true");
		if( refineWithCorners && !configRefineCorners.insideBlack )
			throw new IllegalArgumentException("The detector only support black objects so refine corners fitBlack must be true");

	}
}
