/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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


import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.FastQueue;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_I16;

/**
 * <p>
 * Hough transform which uses a polar line representation.  Each pixel that is identified as a potential line
 * in the image is transformed into parameter space as a line.  Local maximals are considered to be
 * true lines in the image.  Parameter space is discretized into a set of bins.  The range can vary from
 * +- the maximum range inside the image and the angle from 0 to PI radians.  How finely discretized an image
 * is effects line detection accuracy.  If too fine lines might not be detected or it will be too noisy.
 * </p>
 *
 * @author Peter Abeles
 */
public class HoughTransformLinePolar {
	// extracts line from the transform
	FeatureExtractor extractor;
	// stores returned lines
	FastQueue<LineParametric2D_F32> lines = new FastQueue<LineParametric2D_F32>(10,LineParametric2D_F32.class,true);
	// origin of the transform coordinate system
	int originX;
	int originY;
	// maximum allowed range
	double r_max;
	// contains a set of counts for detected lines in each pixel
	// floating point image used because that's what FeatureExtractor's take as input
	ImageFloat32 transform = new ImageFloat32(1,1);
	// found lines in transform space
	QueueCorner foundLines = new QueueCorner(10);

	// lookup tables for sine and cosine functions
	double tableCosine[];
	double tableSine[];

	/**
	 * Specifies parameters of transform.  The minimum number of points specified in the extractor
	 * is an important tuning parameter.
	 *
	 * @param extractor Extracts local maxima from transform space.
	 * @param numBinsRange
	 */
	public HoughTransformLinePolar(FeatureExtractor extractor , int numBinsRange , int numBinsAngle) {
		this.extractor = extractor;
		transform.reshape(numBinsRange,numBinsAngle);

		tableCosine = new double[numBinsAngle];
		tableSine = new double[numBinsAngle];

		for( int i = 0; i < numBinsAngle; i++ ) {
			double theta = Math.PI*i/numBinsAngle;
			tableCosine[i] = Math.cos(theta);
			tableSine[i] = Math.sin(theta);
		}
	}

	/**
	 * Computes the Hough transform of the image.
	 *
	 * @param binary Binary image that indicates which pixels lie on edges.
	 */
	public void transform( ImageUInt8 binary )
	{
		ImageTestingOps.fill(transform, 0);

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

		extractor.process(transform, null, -1, null, foundLines);

		int w2 = transform.width/2;

		for( int i = 0; i < foundLines.size(); i++ ) {
			Point2D_I16 p = foundLines.get(i);

			float r = (float)(r_max*(p.x-w2)/w2);
			float c = (float)tableCosine[p.y];
			float s = (float)tableSine[p.y];

			float x0 = r*c+originX;
			float y0 = r*s+originY;

			LineParametric2D_F32 l = lines.pop();
			l.p.set(x0,y0);
			l.slope.set(-s,c);
		}

		return lines;
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

		for( int i = 0; i < transform.height; i++ ) {
			double p = x*tableCosine[i] + y*tableSine[i];
			int col = (int)(p*w2/r_max) + w2;
			int index = transform.startIndex + i*transform.stride + col;
			transform.data[index]++;
		}
	}

	/**
	 * Returns the Hough transform image.
	 *
	 * @return Transform image.
	 */
	public ImageFloat32 getTransform() {
		return transform;
	}

}
