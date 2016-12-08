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

package boofcv.factory.shape;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.shapes.ellipse.BinaryEllipseDetector} for use in {@link FactoryShapeDetector}
 *
 * @author Peter Abeles
 */
public class ConfigEllipseDetector implements Configuration {
	/**
	 * Detector: maximum distance from the ellipse in pixels
	 */
	public double maxDistanceFromEllipse = 3.0;

	/**
	 * Detector: minimum number of pixels in the contour
	 */
	public int minimumContour = 10;

	/**
	 * Detector: maximum number of pixels in the contour. 0 == no limit
	 */
	public int maximumContour = 0;

	/**
	 * Detector: If true it will consider internal contours and not just external
	 */
	public boolean processInternal = false;

	/**
	 * Refinement: maximum number of refinement iterations it will performance.  Set to zero to disable
 	 */
	public int maxIterations = 5;

	/**
	 * Refinement: when the difference between two ellipses is less than this amount stop iterating
	 */
	public double convergenceTol = 0.01;

	/**
	 * Refinement: how many points along the contour it will sample.  Set to &le; 0 to disable refinement
	 */
	public int numSampleContour = 20;

	/**
	 * Refinement:<br>
	 * Determines the number of points sampled radially outwards from the line
	 * Total intensity values sampled at each point along the line is radius*2+2,
	 * and points added to line fitting is radius*2+1.
	 */
	public int refineRadialSamples = 1;

	/**
	 * Check:<br>
	 * Threshold for minimum edge intensity.  This should be a value which is 0 to (max-min pixel value)
	 * Set to &le; 0 to disable check.
	 */
	public double minimumEdgeIntensity = 20;

	/**
	 * Check:<br>
	 * Tangential distance away from contour the image is sampled when performing edge intensity check.
	 */
	public double checkRadialDistance = 1.5;

	/**
	 * The maximum ratio between the major to minor ratio
	 */
	public double maxMajorToMinorRatio = 20.0;

	@Override
	public void checkValidity() {

	}
}
