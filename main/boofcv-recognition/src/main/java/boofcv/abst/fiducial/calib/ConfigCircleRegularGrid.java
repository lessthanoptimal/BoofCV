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

package boofcv.abst.fiducial.calib;

import boofcv.alg.fiducial.calib.circle.DetectCircleHexagonalGrid;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Calibration parameters for a regular grid of circle calibration target.
 *
 * @author Peter Abeles
 * @see DetectCircleHexagonalGrid
 */
public class ConfigCircleRegularGrid implements Configuration {

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = ConfigThreshold.local(ThresholdType.BLOCK_MEAN, ConfigLength.relative(0.02, 5));

	/**
	 * Configuration for the ellipse detector
	 */
	public ConfigEllipseDetector ellipse = new ConfigEllipseDetector();

	/**
	 * How similar two ellipses must be to be connected. 0 to 1.0. 1.0 = perfect match and 0.0 = infinite
	 * difference in size
	 */
	public double ellipseSizeSimilarity = 0.25;

	/**
	 * How similar edge intensity between two ellipses need to be. 0 to 1.0. 1.0 = perfect match
	 */
	public double edgeIntensitySimilarityTolerance = 0.75;

	{
		// this is being used as a way to smooth out the binary image. Speeds things up quite a bit
		thresholding.scale = 0.85;
	}

	public ConfigCircleRegularGrid setTo( ConfigCircleRegularGrid src ) {
		this.thresholding.setTo(src.thresholding);
		this.ellipse.setTo(src.ellipse);
		this.ellipseSizeSimilarity = src.ellipseSizeSimilarity;
		this.edgeIntensitySimilarityTolerance = src.edgeIntensitySimilarityTolerance;
		return this;
	}

	@Override public void checkValidity() {}
}
