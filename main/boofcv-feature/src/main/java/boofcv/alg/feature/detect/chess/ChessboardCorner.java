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
	 * Used to judge how good of a fit the corner is to an ideal chessboard corner.
	 */
	public double intensity;

	public double intensityXCorner;

	public double edge;

	/**
	 * Internal book keeping. if true then this indicates that this is the first corner seen in this level
	 */
	public boolean first;

	public void reset() {
		orientation = Double.NaN;
		intensity = Double.NaN;
		intensityXCorner = Double.NaN;
		edge = -1;
		first = false;
	}

	public void set(ChessboardCorner c) {
		super.set(c);
		this.orientation = c.orientation;
		this.intensity = c.intensity;
	}

	public void set(double x, double y, double angle, double intensity) {
		this.x = x;
		this.y = y;
		this.orientation = angle;
		this.intensity = intensity;
	}
}
