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

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Configuration that describes how to detect a Hamming marker.
 *
 * @author Peter Abeles
 * @see boofcv.alg.fiducial.square.DetectFiducialSquareHamming
 */
public class ConfigFiducialHammingDetector implements Configuration {
	/**
	 * Fraction of border pixels which must be black.
	 */
	public double minimumBlackBorderFraction = 0.65;

	/** How much ambiguous bits increase the hamming error by. Their count is scaled by this much. */
	@Getter @Setter public double ambiguousPenaltyFrac = 0.5;

	/**
	 * Configuration for square detector
	 *
	 * NOTE: Number of sides, clockwise, and convex are all set by the detector in its consturctor. Values
	 * specified here are ignored.
	 */
	public ConfigPolygonDetector squareDetector = new ConfigPolygonDetector();

	public ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 21);

	{
		((ConfigPolylineSplitMerge)squareDetector.detector.contourToPoly).cornerScorePenalty = 0.2;
		((ConfigPolylineSplitMerge)squareDetector.detector.contourToPoly).thresholdSideSplitScore = 0;
		squareDetector.detector.minimumContour = ConfigLength.fixed(20);

		Objects.requireNonNull(squareDetector.refineGray).cornerOffset = 0;
	}

	public ConfigFiducialHammingDetector() {}

	public ConfigFiducialHammingDetector setTo( ConfigFiducialHammingDetector src ) {
		this.minimumBlackBorderFraction = src.minimumBlackBorderFraction;
		this.ambiguousPenaltyFrac = src.ambiguousPenaltyFrac;
		this.squareDetector.setTo(src.squareDetector);
		this.configThreshold.setTo(src.configThreshold);
		return this;
	}

	@Override
	public void checkValidity() {
		BoofMiscOps.checkTrue(minimumBlackBorderFraction >= 0);
		BoofMiscOps.checkTrue(ambiguousPenaltyFrac >= 0);
		squareDetector.checkValidity();
		configThreshold.checkValidity();
	}
}
