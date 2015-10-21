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

import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.fiducial.SquareBinary_to_FiducialDetector}.
 *
 * @see boofcv.alg.fiducial.DetectFiducialSquareBinary
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
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector squareDetector = new ConfigPolygonDetector(4,4);


	{
		squareDetector.contour2Poly_splitFraction = 0.1;

		ConfigRefinePolygonLineToImage refineLine = new ConfigRefinePolygonLineToImage();
		refineLine.cornerOffset = 0;

		squareDetector.refine = refineLine;
	}

	public ConfigFiducialImage() {
	}

	@Override
	public void checkValidity() {

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
		return getClass().getSimpleName()+"{ maxErrorFraction="+maxErrorFraction+
				" squareDetector="+squareDetector+" }";
	}
}
