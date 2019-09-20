/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

/**
 * Corner in a chessboard. Orientation is estimated unqiuely up to 180 degrees.
 *
 * @author Peter Abeles
 */
public class ChessboardCorner extends Point2D_F64 {
	/**
	 * Radian from pi to -pi
	 */
	public double orientation;
	/**
	 * Used to judge how good of a fit the corner is to an ideal chessboard corner. Higher the value
	 * the more x-corner like
	 */
	public double intensity; // TODO clarify the two intensities

	/**
	 * The white region subtracted the black region at the chessboard corner. Can be used later on
	 * for locally adaptive thresholds
	 */
	public double constrast;

	public double intensityXCorner;

	public double edgeIntensity;
	public double edgeRatio;

	/**
	 * The first and second level in the pyramid the corner was seen at. level1 <= level2
	 */
	public int level1, level2;
	/**
	 * Level with the maximum corner intensity
	 */
	public int levelMax;

	/**
	 * Internal book keeping. if true then this indicates that this is the first corner seen in this level
	 */
	public boolean first; // TODO prune? use levels as a flag instead
	public ChessboardCorner parent;

	public void reset() {
		orientation = Double.NaN;
		intensity = Double.NaN;
		intensityXCorner = Double.NaN;
		edgeIntensity = -1;
		edgeRatio = -1;
		constrast = 0;
		parent = null;
		first = false;
		level1 = level2 = levelMax = -1;
	}

	public void set(ChessboardCorner c) {
		super.set(c);
		this.orientation = c.orientation;
		this.intensity = c.intensity;
		this.constrast = c.constrast;
		this.level1 = c.level1;
		this.level2 = c.level2;
		this.levelMax = c.levelMax;
	}

	public void set(double x, double y, double angle, double intensity) {
		this.x = x;
		this.y = y;
		this.orientation = angle;
		this.intensity = intensity;
	}
}
