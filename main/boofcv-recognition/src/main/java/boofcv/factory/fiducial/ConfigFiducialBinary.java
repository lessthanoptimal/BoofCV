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

import boofcv.abst.fiducial.SquareBinary_to_FiducialDetector;
import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Configuration for {@link SquareBinary_to_FiducialDetector}.
 *
 * @author Peter Abeles
 * @see DetectFiducialSquareBinary
 */
@Getter @Setter
public class ConfigFiducialBinary implements Configuration {
	/**
	 * Length of a targets size in world units.
	 */
	public double targetWidth;

	/**
	 * Value from 0 to 1. 0 is very strict and 1 is very relaxed. Used when classifying a require block
	 * as black or white. If it can't be classified then the shape is discarded
	 */
	public double ambiguousThreshold = 0.75;

	/**
	 * Number of elements wide the encoded grid is. Grids widths of 3, 4, or 5 are common. 4 is the standard.
	 */
	public int gridWidth = 4;

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

	public ConfigFiducialBinary() {}

	public ConfigFiducialBinary( double targetWidth ) {
		this.targetWidth = targetWidth;
	}

	public ConfigFiducialBinary setTo( ConfigFiducialBinary src ) {
		this.targetWidth = src.targetWidth;
		this.ambiguousThreshold = src.ambiguousThreshold;
		this.gridWidth = src.gridWidth;
		this.borderWidthFraction = src.borderWidthFraction;
		this.minimumBlackBorderFraction = src.minimumBlackBorderFraction;
		this.squareDetector.setTo(src.squareDetector);
		return this;
	}

	@Override
	public void checkValidity() {
		if (ambiguousThreshold < 0 || ambiguousThreshold > 1)
			throw new IllegalArgumentException("ambiguousThreshold must be from 0 to 1, inclusive");
		if (gridWidth < 3 || gridWidth > 8)
			throw new IllegalArgumentException("Grid width must be at least 3 elements and at most 8");
		if (borderWidthFraction <= 0 || borderWidthFraction >= 0.5)
			throw new IllegalArgumentException("Border width fraction must be 0 < fraction < 0.5");
	}

	@Override
	public String toString() {
		return "ConfigFiducialBinary{" +
				"targetWidth=" + targetWidth +
				", ambiguousThreshold=" + ambiguousThreshold +
				", gridWidth=" + gridWidth +
				", borderWidthFraction=" + borderWidthFraction +
				", squareDetector=" + squareDetector +
				'}';
	}
}
