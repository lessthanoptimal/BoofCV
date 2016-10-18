/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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


import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.feature.CachedSineCosine_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilLine2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;

/**
 * <p>
 * Hough transform which uses a polar line representation, distance from origin and angle (0 to 180 degrees).
 * Standard implementation of a hough transform.  1) Gradient intensity image is used to find edge pixels.
 * 2) All possible lines passing through that point are found. 3) Line parameters are summed up in the line image,
 * in which each pixel represents a coordinate in parameter space.
 * 3) Local maximums are found.
 * </p>
 * <p> By the nature of this algorithms, lines are forced to be discretized into parameter space.  The range
 * can vary from +- the maximum range inside the image and the angle from 0 to PI radians.  How
 * finely discretized an image is effects line detection accuracy.  If too fine lines might not be detected
 * or it will be too noisy.
 * </p>
 * <p>
 * In the line image, the transform from line parameter coordinate to pixel coordinate is as follow:<br>
 * x = r*cos(theta) + centerX<br>
 * y = r*sin(theta) + centerY<br>
 * </p>
 *
 * <p>
 * USAGE NOTE: Duplicate/very similar lines are possible due to angles being cyclical.  What happens is that if
 * a line's orientation lies along a boundary point its angles will be split up between top and bottom
 * of the transform.  When lines are extracted using non-maximum it will detects peaks at the top
 * and bottom.
 * </p>
 *
 * @author Peter Abeles
 */
public class HoughTransformLinePolar {
	// extracts line from the transform
	NonMaxSuppression extractor;
	// stores returned lines
	FastQueue<LineParametric2D_F32> lines = new FastQueue<>(10, LineParametric2D_F32.class, true);
	// origin of the transform coordinate system
	int originX;
	int originY;
	// maximum allowed range
	double r_max;
	// contains a set of counts for detected lines in each pixel
	// floating point image used because that's what FeatureExtractor's take as input
	GrayF32 transform = new GrayF32(1,1);
	// found lines in transform space
	QueueCorner foundLines = new QueueCorner(10);
	// line intensities for later pruning
	GrowQueue_F32 foundIntensity = new GrowQueue_F32(10);

	// lookup tables for sine and cosine functions
	CachedSineCosine_F32 tableTrig;

	/**
	 * Specifies parameters of transform.  The minimum number of points specified in the extractor
	 * is an important tuning parameter.
	 *
	 * @param extractor Extracts local maxima from transform space.
	 * @param numBinsRange How many bins are be used for line range.
	 * @param numBinsAngle How many bins are used for angle.
	 */
	public HoughTransformLinePolar(NonMaxSuppression extractor , int numBinsRange , int numBinsAngle) {
		this.extractor = extractor;
		transform.reshape(numBinsRange,numBinsAngle);

		tableTrig = new CachedSineCosine_F32(0,(float)Math.PI,numBinsAngle);
	}

	public int getNumBinsRange() {
		return transform.getWidth();
	}

	public int getNumBinsAngle() {
		return transform.getHeight();
	}

	/**
	 * Computes the Hough transform of the image.
	 *
	 * @param binary Binary image that indicates which pixels lie on edges.
	 */
	public void transform( GrayU8 binary )
	{
		ImageMiscOps.fill(transform, 0);

		originX = binary.width/2;
		originY = binary.height/2;
		r_max = Math.sqrt(originX*originX+originY*originY);

		for( int y = 0; y < binary.height; y++ ) {
			int start = binary.startIndex + y*binary.stride;
			int stop = start + binary.width;

			for( int index = start; index < stop; index++ ) {
				if( binary.data[index] != 0 ) {
					parameterize(index-start,y);
				}
			}
		}
	}

	/**
	 * Searches for local maximas and converts into lines.
	 *
	 * @return Found lines in the image.
	 */
	public FastQueue<LineParametric2D_F32> extractLines() {
		lines.reset();
		foundLines.reset();
		foundIntensity.reset();

		extractor.process(transform, null,null,null, foundLines);

		int w2 = transform.width/2;

		for( int i = 0; i < foundLines.size(); i++ ) {
			Point2D_I16 p = foundLines.get(i);

			float r = (float)(r_max*(p.x-w2)/w2);
			float c = tableTrig.c[p.y];
			float s = tableTrig.s[p.y];

			float x0 = r*c+originX;
			float y0 = r*s+originY;

			foundIntensity.push( transform.get(p.x,p.y));
			LineParametric2D_F32 l = lines.grow();
			l.p.set(x0,y0);
			l.slope.set(-s,c);

			Point2D_F64 p2 = new Point2D_F64();
			lineToCoordinate(l,p2);
		}

		return lines;
	}

	/**
	 * Compute the parameterized coordinate for the line
	 */
	public void lineToCoordinate(LineParametric2D_F32 line , Point2D_F64 coordinate ) {
		line = line.copy();
		line.p.x -= originX;
		line.p.y -= originY;
		LinePolar2D_F32 polar = new LinePolar2D_F32();
		UtilLine2D_F32.convert(line,polar);

		if( polar.angle < 0 ) {
			polar.distance = -polar.distance;
			polar.angle = UtilAngle.toHalfCircle(polar.angle);
		}

		int w2 = transform.width/2;

		coordinate.x = (int)Math.floor(polar.distance*w2/r_max) + w2;
		coordinate.y = polar.angle*transform.height/Math.PI;
	}

	/**
	 * Converts the pixel coordinate into a line in parameter space
	 */
	public void parameterize( int x , int y )
	{
		// put the point in a new coordinate system centered at the image's origin
		x -= originX;
		y -= originY;

		int w2 = transform.width/2;

		// The line's slope is encoded using the tangent angle.  Those bins are along the image's y-axis
		for( int i = 0; i < transform.height; i++ ) {
			// distance of closest point on line from a line defined by the point (x,y) and
			// the tangent theta=PI*i/height
			double p = x*tableTrig.c[i] + y*tableTrig.s[i];

			int col = (int)Math.floor(p * w2 / r_max) + w2;
			int index = transform.startIndex + i*transform.stride + col;
			transform.data[index]++;
		}
	}

	/**
//	 * Returns the Hough transform image.
	 *
	 * @return Transform image.
	 */
	public GrayF32 getTransform() {
		return transform;
	}

	/**
	 * Returns the intensity/edge count for each returned line.  Useful when doing
	 * post processing pruning.
	 *
	 * @return Array containing line intensities.
	 */
	public float[] getFoundIntensity() {
		return foundIntensity.data;
	}
}
