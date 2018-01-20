/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
 * @see DetectCircleHexagonalGrid
 *
 * @author Peter Abeles
 */
public class ConfigCircleRegularGrid implements Configuration {

	/**
	 * Number of black circles tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Number of black circles wide the grid is. Target dependent.
	 */
	public int numCols = -1;

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = ConfigThreshold.local(ThresholdType.BLOCK_MEAN,ConfigLength.relative(0.02,5));

	/**
	 * Configuration for the ellipse detector
	 */
	public ConfigEllipseDetector ellipse = new ConfigEllipseDetector();

	/**
	 * Distance between each center's center along the x and y axis.  Another way to look at this is that
	 * it is twice the distance of the center of each grid cell.
	 */
	public double centerDistance;

	/**
	 * Diameter of each circle.
	 */
	public double circleDiameter;

	/**
	 * How similar two ellipses must be to be connected.  0 to 1.0.  1.0 = perfect match and 0.0 = infinite
	 * difference in size
	 */
	public double ellipseSizeSimilarity = 0.25;

	/**
	 * How similar edge intensity between two ellipses need to be.  0 to 1.0.  1.0 = perfect match
	 */
	public double edgeIntensitySimilarityTolerance = 0.75;

	{
		// this is being used as a way to smooth out the binary image.  Speeds things up quite a bit
		thresholding.scale = 0.85;
	}

	public ConfigCircleRegularGrid(int numRows, int numCols,
								   double circleDiameter, double centerDistance )
	{
		this.numRows = numRows;
		this.numCols = numCols;
		this.circleDiameter = circleDiameter;
		this.centerDistance = centerDistance;
	}

	@Override
	public void checkValidity() {
		if( numCols <= 0 || numRows <= 0 )
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
