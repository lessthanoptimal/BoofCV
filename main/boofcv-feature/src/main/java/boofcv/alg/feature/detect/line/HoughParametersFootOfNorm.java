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

package boofcv.alg.feature.detect.line;

import boofcv.struct.image.GrayF32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

/**
 * {@link HoughTransformParameters} with a foot-of-norm parameterization.
 *
 * @author Peter Abeles
 */
public class HoughParametersFootOfNorm implements HoughTransformParameters {
	// origin of the transform coordinate system
	int originX;
	int originY;

	int minDistanceFromOrigin;

	/**
	 * @param minDistanceFromOrigin Distance from the origin in which lines will not be estimated. In transform space. Try 5.
	 */
	public HoughParametersFootOfNorm( int minDistanceFromOrigin ) {
		this.minDistanceFromOrigin = minDistanceFromOrigin;
	}

	@Override
	public void initialize( int width, int height, GrayF32 transform ) {
		this.originX = width/2;
		this.originY = height/2;

		transform.reshape(width, height);
	}

	@Override
	public boolean isTransformValid( int x, int y ) {
		return Math.abs(x - originX) >= minDistanceFromOrigin || Math.abs(y - originX) >= minDistanceFromOrigin;
	}

	@Override
	public void lineToCoordinate( LineParametric2D_F32 line, Point2D_F64 coordinate ) {
		coordinate.setTo(line.p.x, line.p.y);
	}

	@Override
	public void transformToLine( float x, float y, LineParametric2D_F32 l ) {
		l.p.x = x;
		l.p.y = y;
		l.slope.x = -(l.p.y - originY);
		l.slope.y = l.p.x - originX;
	}

	@Override
	public void parameterize( int x, int y, GrayF32 transform ) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public void parameterize( int x, int y, float derivX, float derivY, Point2D_F32 parameter ) {
		// put the point in a new coordinate system centered at the image's origin
		// this minimizes error, which is a function of distance from origin
		x -= originX;
		y -= originY;

		float v = (x*derivX + y*derivY)/(derivX*derivX + derivY*derivY);
		parameter.x = v*derivX + originX;
		parameter.y = v*derivY + originY;
	}
}
