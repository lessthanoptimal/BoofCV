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
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.fiducial.SquareBinary_to_FiducialDetector}.
 *
 * @see boofcv.alg.fiducial.DetectFiducialSquareBinary
 *
 * @author Peter Abeles
 */
public class ConfigFiducialBinary implements Configuration {
	/**
	 * Length of a targets size in world units.
	 */
	public double targetWidth;

	/**
	 * Value from 0 to 1.  0 is very strict and 1 is very relaxed.  Used when classifying a require block
	 * as black or white.  If it can't be classified then the shape is discarded
	 */
	public double ambiguousThreshold = 0.5;

	/**
	 * Configuration for square detector
	 */
	public ConfigPolygonDetector squareDetector = new ConfigPolygonDetector(4);

	{
		squareDetector.configRefineLines.cornerOffset = 2;
	}

	public ConfigFiducialBinary() {
	}

	public ConfigFiducialBinary(double targetWidth) {
		this.targetWidth = targetWidth;
	}

	@Override
	public void checkValidity() {
		if( ambiguousThreshold < 0 || ambiguousThreshold > 1 )
			throw new IllegalArgumentException("ambiguousThreshold must be from 0 to 1, inclusive");
	}

	public double getTargetWidth() {
		return targetWidth;
	}

	public void setTargetWidth(double targetWidth) {
		this.targetWidth = targetWidth;
	}

	public ConfigPolygonDetector getSquareDetector() {
		return squareDetector;
	}

	public void setSquareDetector(ConfigPolygonDetector squareDetector) {
		this.squareDetector = squareDetector;
	}
}
