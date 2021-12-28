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
import boofcv.alg.fiducial.calib.chess.DetectChessboardBinaryPattern;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ConfigThresholdLocalOtsu;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Calibration parameters for chessboard style calibration grid.
 *
 * @author Peter Abeles
 * @see DetectChessboardBinaryPattern
 */
@Getter @Setter
public class ConfigChessboardBinary implements Configuration {
	/**
	 * The maximum distance in pixels that two corners can be from each other. In well focused image
	 * this number can be only a few pixels. The default value has been selected to handle blurred images.
	 *
	 * If relative it is relative to min(image.width,image.height)
	 */
	public ConfigLength maximumCornerDistance = ConfigLength.relative(8.0/800.0, 8);

	/**
	 * Configuration for thresholding the image
	 */
	public ConfigThreshold thresholding = new ConfigThresholdLocalOtsu(ConfigLength.relative(0.05, 10), 10);

	/**
	 * Configuration for square detector.
	 *
	 * NOTE: Number of sides, clockwise, and convex are all set by the detector in its constructor. Values
	 * specified here are ignored.
	 */
	public ConfigPolygonDetector square = new ConfigPolygonDetector();

	{
		// this is being used as a way to smooth out the binary image. Speeds things up quite a bit
		thresholding.scale = 0.85;

		((ConfigPolylineSplitMerge)square.detector.contourToPoly).cornerScorePenalty = 0.2;
		((ConfigPolylineSplitMerge)square.detector.contourToPoly).minimumSideLength = 2;
		((ConfigPolylineSplitMerge)square.detector.contourToPoly).thresholdSideSplitScore = 0;
		// max side error is increased for  shapes which are parially outside of the image, but the local threshold
		// makes them concave
		((ConfigPolylineSplitMerge)square.detector.contourToPoly).maxSideError = ConfigLength.relative(0.5, 4);
//		((ConfigPolylineSplitMerge)square.detector.contourToPoly).convexTest = 1000;
		square.detector.tangentEdgeIntensity = 2.5; // the initial contour is the result of being eroded
		square.detector.minimumContour = ConfigLength.fixed(10);
		square.detector.canTouchBorder = true;

		// defaults for if the user toggles it to lines
		Objects.requireNonNull(square.refineGray);
		square.refineGray.cornerOffset = 1;
		square.refineGray.sampleRadius = 3;
		square.refineGray.lineSamples = 15;
		square.refineGray.convergeTolPixels = 0.2;
		square.refineGray.maxIterations = 5;
	}

	public ConfigChessboardBinary setTo( ConfigChessboardBinary src ) {
		this.maximumCornerDistance.setTo(src.maximumCornerDistance);
		this.thresholding.setTo(src.thresholding);
		this.square.setTo(src.square);
		return this;
	}

	@Override
	public void checkValidity() {
		maximumCornerDistance.checkValidity();
		thresholding.checkValidity();
		square.checkValidity();
	}
}
