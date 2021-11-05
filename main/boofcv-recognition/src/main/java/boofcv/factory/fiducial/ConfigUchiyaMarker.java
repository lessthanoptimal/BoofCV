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

package boofcv.factory.fiducial;

import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.shape.ConfigEllipseEdgeCheck;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import boofcv.struct.ConnectRule;

/**
 * Configuration for Uchiya Marker approach to detect random dot markers
 *
 * @author Peter Abeles
 * @see boofcv.alg.fiducial.dots.UchiyaMarkerImageTracker
 */
public class ConfigUchiyaMarker implements Configuration {
	/**
	 * Specifies the marker's width. This must match width used to generate the random dots.
	 */
	public double markerWidth = -1.0;
	/**
	 * Specifies the marker's height. This must match width used to generate the random dots.
	 */
	public double markerHeight = -1.0;

	/**
	 * Specifies how images are thresholded and converted into a binary format
	 */
	public ConfigThreshold threshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, ConfigLength.fixed(20));

	/**
	 * Defines the feature descriptor
	 */
	public ConfigLlah llah = new ConfigLlah();

	/**
	 * Configures RANSAC used to fit the homography. You might need to tune this.
	 */
	public ConfigRansac ransac = new ConfigRansac(200, 2.0);

	/**
	 * Pixel connectivity rule for blob/contour finder.
	 */
	public ConnectRule contourRule = ConnectRule.EIGHT;

	/**
	 * Minimum number of pixels in the contour to consider
	 */
	public int contourMinimumLength = 5;

	/**
	 * Detector: maximum distance from the ellipse in pixels
	 */
	public double maxDistanceFromEllipse = 3.0;

	/**
	 * Minimum number of pixels in the minor axis
	 */
	public double minimumMinorAxis = 0.5;

	/**
	 * The maximum ratio between the major to minor ratio
	 */
	public double maxMajorToMinorRatio = 5.0;

	/**
	 * Parameters for checking the intensity of the contour along an ellipse. When using adaptive thresholding
	 * there can be a lot of false positives
	 */
	public ConfigEllipseEdgeCheck checkEdge = new ConfigEllipseEdgeCheck();

	{
		// Default values taken from paper and source code
		llah.numberOfNeighborsN = 7;
		llah.sizeOfCombinationM = 5;
		llah.quantizationK = 32;
		llah.hashType = ConfigLlah.HashType.AFFINE;
		llah.hashTableSize = Integer.MAX_VALUE; // this is 31-bits and the code has 32-bits. signed vs unsigned

		// Can get more blurred images if this is reduced, BUT there's a massive speed hit in some complex scenes
		// checkEdge.minimumEdgeIntensity = 15;
	}

	public ConfigUchiyaMarker setTo( ConfigUchiyaMarker src ) {
		this.markerWidth = src.markerWidth;
		this.markerHeight = src.markerHeight;
		this.threshold.setTo(src.threshold);
		this.llah.setTo(src.llah);
		this.ransac.setTo(src.ransac);
		this.contourRule = src.contourRule;
		this.contourMinimumLength = src.contourMinimumLength;
		this.maxDistanceFromEllipse = src.maxDistanceFromEllipse;
		this.minimumMinorAxis = src.minimumMinorAxis;
		this.maxMajorToMinorRatio = src.maxMajorToMinorRatio;
		this.checkEdge.setTo(src.checkEdge);
		return this;
	}

	@Override
	public void checkValidity() {
		llah.checkValidity();
		if (markerWidth <= 0)
			throw new IllegalArgumentException("Marker's width must set!");
		if (markerHeight <= 0)
			throw new IllegalArgumentException("Marker's height must set!");
	}
}
