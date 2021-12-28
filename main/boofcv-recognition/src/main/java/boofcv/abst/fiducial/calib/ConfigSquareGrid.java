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

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

import java.util.Objects;

/**
 * Calibration parameters for square-grid style calibration grid.
 *
 * @author Peter Abeles
 * @see boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial
 */
public class ConfigSquareGrid implements Configuration {

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = ConfigThreshold.local(ThresholdType.BLOCK_MEAN, ConfigLength.relative(0.02, 5));

	/**
	 * Configuration for square detector
	 *
	 * NOTE: Number of sides, clockwise, and convex are all set by the detector in its consturctor. Values
	 * specified here are ignored.
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector();

	{
		// this is being used as a way to smooth out the binary image. Speeds things up quite a bit
		thresholding.scale = 0.85;

		((ConfigPolylineSplitMerge)square.detector.contourToPoly).cornerScorePenalty = 0.5;
		square.detector.minimumContour = ConfigLength.fixed(10);

		Objects.requireNonNull(square.refineGray);
		square.refineGray.cornerOffset = 1;
		square.refineGray.lineSamples = 15;
		square.refineGray.convergeTolPixels = 0.2;
		square.refineGray.maxIterations = 10;
	}

	public ConfigSquareGrid setTo( ConfigSquareGrid src ) {
		this.thresholding.setTo(src.thresholding);
		this.square.setTo(src.square);
		return this;
	}

	@Override
	public void checkValidity() {
		thresholding.checkValidity();
		square.checkValidity();
	}
}
