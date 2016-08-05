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
	public int minimumContour = 20;

	/**
	 * Detector: maximum number of pixels in the contour. 0 == no limit
	 */
	public int maximumContour = 0;

	/**
	 * Detector: If true it will consider internal contours and not just external
	 */
	public boolean processInternal = false;

	/**
	 * Refinement: maximum number of iterations it will performance
 	 */
	protected int maxIterations = 10;

	/**
	 * Refinement: when the difference between two ellipses is less than this amount stop iterating
	 */
	protected double convergenceTol = 1e-6;

	/**
	 * Refinement: how many points along the contour it will sample.  Set to &le; 0 to disable refinement
	 */
	protected int numSampleContour = 20;

	/**
	 * Refinement:<br>
	 * Determines the number of points sampled radially outwards from the line
	 * Total intensity values sampled at each point along the line is radius*2+2,
	 * and points added to line fitting is radius*2+1.
	 */
	protected int refineRadialSamples = 1;

	/**
	 * Check:<br>
	 * Threshold for minimum edge intensity.  This should be a value which is 0 to (max-min pixel value)
	 * Set to &le; 0 to disable check.
	 */
	public double checkIntensityThreshold = 20;

	/**
	 * Check:<br>
	 * Tangential distance away from contour the image is sampled when performing edge
	 * intensity check.
	 */
	public double checkRadialDistance = 1.5;

	/**
	 * Check:<br>
	 * Number of points along the contour it samples when performing edge intensity check.
	 */
	public int checkSamplePoints = 20;

	@Override
	public void checkValidity() {

	}
}
