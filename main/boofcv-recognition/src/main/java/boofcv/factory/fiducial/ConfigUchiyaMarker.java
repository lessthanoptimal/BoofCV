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

package boofcv.factory.fiducial;

import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.geo.ConfigRansac;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import boofcv.struct.ConnectRule;

/**
 * Configuration for Uchiya Markers
 *
 * @see boofcv.alg.fiducial.dots.UchiyaMarkerImageTracker
 *
 * @author Peter Abeles
 */
public class ConfigUchiyaMarker implements Configuration {
	/**
	 * Specifies how images are thresholded and converted into a binary format
	 */
	public ConfigThreshold threshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, ConfigLength.relative(0.2,20));

	/**
	 * Defines the feature descriptor
	 */
	public ConfigLlah llah = new ConfigLlah();

	/**
	 * Configures RANSAC used to fit the homography. You might need to tune this.
	 */
	public ConfigRansac ransac = new ConfigRansac(200,2.0);

	/**
	 * Pixel connectivity rule for blob/contour finder.
	 */
	public ConnectRule contourRule = ConnectRule.FOUR;

	/**
	 * Detector: maximum distance from the ellipse in pixels
	 */
	public double maxDistanceFromEllipse = 3.0;

	/**
	 * Minimum number of pixels in the minor axis
	 */
	public double minimumMinorAxis = 1.5;

	/**
	 * The maximum ratio between the major to minor ratio
	 */
	public double maxMajorToMinorRatio = 20.0;

	{
		// Default values taken from paper and source code
		llah.numberOfNeighborsN = 7;
		llah.sizeOfCombinationM = 5;
		llah.quantizationK = 4;
		llah.hashType = ConfigLlah.HashType.AFFINE;
		llah.hashTableSize = Integer.MAX_VALUE; // this is 31-bits and the code has 32-bits. signed vs unsigned
	}

	@Override
	public void checkValidity() {
		llah.checkValidity();
	}
}
