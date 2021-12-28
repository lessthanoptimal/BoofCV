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
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Configuration for {@link boofcv.abst.fiducial.SquareImage_to_FiducialDetector}.
 *
 * @see boofcv.alg.fiducial.square.DetectFiducialSquareImage
 *
 * @author Peter Abeles
 */
@Getter @Setter
public class ConfigFiducialImage implements Configuration {

	/**
	 * If the difference between an candidate and a target is less than this amount it is considered
	 * a match.
	 */
	public double maxErrorFraction = 0.20;

	/**
	 * How wide the border is relative to the total fiducial width. 0.25 is standard and is a good compromise
	 * between ability to view at extreme angles and area to encode information.
	 */
	public double borderWidthFraction = 0.25;

	/**
	 * Fraction of border pixels which must be black.
	 */
	public double minimumBlackBorderFraction = 0.65;

	/**
	 * Configuration for square detector
	 *
	 * NOTE: Number of sides, clockwise, and convex are all set by the detector in its consturctor. Values
	 * specified here are ignored.
	 */
	public ConfigPolygonDetector squareDetector = new ConfigPolygonDetector();

	{
		((ConfigPolylineSplitMerge)squareDetector.detector.contourToPoly).cornerScorePenalty = 0.2;
		((ConfigPolylineSplitMerge)squareDetector.detector.contourToPoly).thresholdSideSplitScore = 0;
		squareDetector.detector.minimumContour = ConfigLength.fixed(20);
		Objects.requireNonNull(squareDetector.refineGray).cornerOffset = 0;
	}

	public ConfigFiducialImage() {}

	public ConfigFiducialImage setTo( ConfigFiducialImage src ) {
		this.maxErrorFraction = src.maxErrorFraction;
		this.borderWidthFraction = src.borderWidthFraction;
		this.minimumBlackBorderFraction = src.minimumBlackBorderFraction;
		this.squareDetector.setTo(src.squareDetector);
		return this;
	}

	@Override
	public void checkValidity() {
		if( borderWidthFraction <= 0 || borderWidthFraction >= 0.5 )
			throw new IllegalArgumentException("Border width fraction must be 0 < fraction < 0.5");
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+
				"{ maxErrorFraction="+maxErrorFraction+
				" borderWidthFraction="+borderWidthFraction+
				" squareDetector="+squareDetector+" }";
	}
}
