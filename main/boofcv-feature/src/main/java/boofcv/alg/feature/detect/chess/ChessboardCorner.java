/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import georegression.struct.point.Point2D_F64;

/// Corner in a chessboard. Orientation is estimated uniquely up to 180 degrees.
public class ChessboardCorner extends Point2D_F64 {
	/// Radian from pi to -pi
	public double orientation;
	/// Used to judge how good of a fit the corner is to an ideal chessboard corner. Higher the value
	/// the more x-corner like. Computed on a per-feature basis and should be more accurate than
	/// the x-corner detector intensity.
	public double intensity;

	/// The white region subtracted the black region at the chessboard corner. Can be used later on
	/// for locally adaptive thresholds
	public double contrast;

	/// Value of smallest Eigen value in edge detector
	public double edgeIntensity;

	/// Ratio of smallest and largest Eigen values in edge detector
	public double edgeRatio;

	/// The first and second level in the pyramid the corner was seen at. level1 <= level2
	public int level1, level2;

	/// Level with the maximum corner intensity
	public int levelMax;

	{reset();}

	/// Mark the corner as needing to be discarded
	public void markDiscard() {
		level1 = -1;
	}

	/// Returns true if it has been marked to be discarded
	public boolean isDiscarded() {
		return level1 == -1;
	}

	public void reset() {
		super.setTo(-1, -1);
		orientation = Double.NaN;
		intensity = Double.NaN;
		edgeIntensity = -1;
		edgeRatio = -1;
		contrast = 0;
		level1 = level2 = levelMax = -1;
	}

	public ChessboardCorner setTo( ChessboardCorner c ) {
		super.setTo(c);
		this.orientation = c.orientation;
		this.intensity = c.intensity;
		this.contrast = c.contrast;
		this.edgeIntensity = c.edgeIntensity;
		this.edgeRatio = c.edgeRatio;
		this.level1 = c.level1;
		this.level2 = c.level2;
		this.levelMax = c.levelMax;
		return this;
	}

	public ChessboardCorner setTo( double x, double y, double angle, double intensity ) {
		this.x = x;
		this.y = y;
		this.orientation = angle;
		this.intensity = intensity;
		return this;
	}
}
