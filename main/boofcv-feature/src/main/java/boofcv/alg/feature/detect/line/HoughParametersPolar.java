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

import boofcv.struct.feature.CachedSineCosine_F32;
import boofcv.struct.image.GrayF32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

/**
 * {@link HoughTransformParameters} with a polar parameterization.
 *
 * @author Peter Abeles
 */
public class HoughParametersPolar implements HoughTransformParameters {
	// origin of the transform coordinate system
	int originX;
	int originY;
	// maximum allowed range
	float r_max;
	// lookup tables for sine and cosine functions
	CachedSineCosine_F32 tableTrig;

	// tuning parameters
	int numBinsAngle; // number of bins that discretize 180 degrees
	double rangeResolution; // number of pixels per bin

	// inferred parameter
	int numBinsRange;

	public HoughParametersPolar( double rangeResolution, int numBinsAngle ) {
		this.rangeResolution = rangeResolution;
		this.numBinsAngle = numBinsAngle;
		tableTrig = new CachedSineCosine_F32(0, (float)Math.PI, numBinsAngle);
	}

	@Override
	public void initialize( int width, int height, GrayF32 transform ) {
		this.originX = width/2;
		this.originY = height/2;
		this.r_max = (float)Math.sqrt(originX*originX + originY*originY);
		this.numBinsRange = (int)Math.ceil(r_max/rangeResolution);
		transform.reshape(numBinsRange, numBinsAngle);
	}

	@Override
	public boolean isTransformValid( int x, int y ) {
		return true;
	}

	@Override
	public void lineToCoordinate( LineParametric2D_F32 line, Point2D_F64 coordinate ) {

		float px = line.p.x - originX;
		float py = line.p.y - originY;

		// convert line info polar notation
		float top = line.slope.y*px - line.slope.x*py;
		float distance = top/line.slope.norm();
		float angle = (float)Math.atan2(-line.slope.x, line.slope.y);

		int w2 = numBinsRange/2;

		coordinate.x = Math.round(distance*w2/r_max + w2);
		coordinate.y = angle*numBinsAngle/Math.PI;
	}

	@Override
	public void transformToLine( float x, float y, LineParametric2D_F32 line ) {
		int w2 = numBinsRange/2;

		float r = r_max*(x - w2)/w2;
		float c = tableTrig.cosine(y);
		float s = tableTrig.sine(y);

		line.p.setTo(r*c + originX, r*s + originY);
		line.slope.setTo(-s, c);
	}

	@Override
	public void parameterize( int x, int y, GrayF32 transform ) {
		// put the point in a new coordinate system centered at the image's origin
		x -= originX;
		y -= originY;

		int w2 = transform.width/2;

		// The line's slope is encoded using the tangent angle. Those bins are along the image's y-axis
		for (int i = 0; i < transform.height; i++) {
			// distance of closest point on line from a line defined by the point (x,y) and
			// the tangent theta=PI*i/height
			double p = x*tableTrig.c[i] + y*tableTrig.s[i];

			int col = (int)Math.floor(p*w2/r_max) + w2;
			int index = transform.startIndex + i*transform.stride + col;
			transform.data[index]++;
		}
	}

	@Override
	public void parameterize( int x, int y, float derivX, float derivY, Point2D_F32 parameter ) {
		float px = x - originX;
		float py = y - originY;
		float sx = -derivY;
		float sy = derivX;

		float top = sy*px - sx*py;
		float distance = top/(float)Math.sqrt(sx*sx + sy*sy);
		float angle = (float)Math.atan2(-sx, sy);

		if (distance < 0) {
			distance = -distance;
			angle = UtilAngle.bound(angle + (float)Math.PI);
		}
		if (angle < 0) {
			distance = -distance;
			angle = UtilAngle.toHalfCircle(angle);
		}

		int w2 = numBinsRange/2;

		parameter.x = distance*w2/r_max + w2;

		double yy = angle*numBinsAngle;
		if (yy >= 1.0)
			yy -= 1.0;

		parameter.y = (float)(yy/Math.PI);
	}
}
