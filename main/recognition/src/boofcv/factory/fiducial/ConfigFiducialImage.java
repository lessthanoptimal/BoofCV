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

package boofcv.factory.fiducial;

import boofcv.abst.fiducial.SquareBinary_to_FiducialDetector;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SquareBinary_to_FiducialDetector}.
 *
 * @see DetectFiducialSquareBinary
 *
 * @author Peter Abeles
 */
public class ConfigFiducialImage implements Configuration {

	/**
	 * If the difference between an candidate and a target is less than this amount it is considered
	 * a match.
	 */
	public double maxErrorFraction = 0.20;

	/**
	 * How wide the border is relative to the total fiducial width.  0.25 is standard and is a good compromise
	 * between ability to view at extreme angles and area to encode information.
	 */
	public double borderWidthFraction = 0.25;

	/**
	 * Fraction of border pixels which must be black.
	 */
	public double minimumBlackBorderFraction = 0.65;

	/**
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector squareDetector = new ConfigPolygonDetector(4,4);

	{
		squareDetector.contour2Poly_splitFraction = 0.1;
		squareDetector.contour2Poly_minimumSideFraction = 0.05;

		ConfigRefinePolygonLineToImage refineLine = new ConfigRefinePolygonLineToImage();
		refineLine.cornerOffset = 0;

		squareDetector.refine = refineLine;
	}

	public ConfigFiducialImage() {
	}

	@Override
	public void checkValidity() {
		if( borderWidthFraction <= 0 || borderWidthFraction >= 0.5 )
			throw new IllegalArgumentException("Border width fraction must be 0 < fraction < 0.5");
	}

	public double getMaxErrorFraction() {
		return maxErrorFraction;
	}

	public void setMaxErrorFraction(double maxErrorFraction) {
		this.maxErrorFraction = maxErrorFraction;
	}

	public ConfigPolygonDetector getSquareDetector() {
		return squareDetector;
	}

	public void setSquareDetector(ConfigPolygonDetector squareDetector) {
		this.squareDetector = squareDetector;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+
				"{ maxErrorFraction="+maxErrorFraction+
				" borderWidthFraction="+borderWidthFraction+
				" squareDetector="+squareDetector+" }";
	}
}
