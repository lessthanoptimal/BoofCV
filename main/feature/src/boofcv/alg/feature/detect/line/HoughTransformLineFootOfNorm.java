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
import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.*;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;

/**
 * <p>
 * Hough transform based line detector.  Lines are parameterized based upon the (x,y) coordinate
 * of the closest point to the origin.  This parametrization is more convenient since it corresponds
 * directly with the image space.  See [1] for more details.
 * </p>
 *
 * <p>
 * The line's direction is estimated using the gradient at each point flagged as belonging to a line.
 * To minimize error the image center is used as the coordinate system's center.  However lines which
 * lie too close to the center can't have their orientation accurately estimated.
 * </p>
 *
 * <p>
 * [1] Section 9.3 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd Ed. 2005
 * </p>
 *
 * @author Peter Abeles
 */
public class HoughTransformLineFootOfNorm {

	// extracts line from the transform
	NonMaxSuppression extractor;
	// stores returned lines
	FastQueue<LineParametric2D_F32> lines = new FastQueue<>(10, LineParametric2D_F32.class, true);
	// lines are ignored if they are less than this distance from the origin
	// because the orientation can't be accurately estimated
	int minDistanceFromOrigin;
	// origin of the transform coordinate system
	int originX;
	int originY;
	// contains a set of counts for detected lines in each pixel
	// floating point image used because that's what FeatureExtractor's take as input
	GrayF32 transform = new GrayF32(1,1);
	// found lines in transform space
	QueueCorner foundLines = new QueueCorner(10);
	// list of points in the transform with non-zero values
	QueueCorner candidates = new QueueCorner(10);
	// line intensities for later pruning
	GrowQueue_F32 foundIntensity = new GrowQueue_F32(10);

	/**
	 * Specifies parameters of transform.
	 *
	 * @param extractor Extracts local maxima from transform space.  A set of candidates is provided, but can be ignored.
	 * @param minDistanceFromOrigin Distance from the origin in which lines will not be estimated.  In transform space.  Try 5.
	 */
	public HoughTransformLineFootOfNorm(NonMaxSuppression extractor,
										int minDistanceFromOrigin) {
		this.extractor = extractor;
		this.minDistanceFromOrigin = minDistanceFromOrigin;
	}

	/**
	 * Computes the Hough transform using the image gradient and a binary image which flags pixels as being edges or not.
	 *
	 * @param derivX Image derivative along x-axis.
	 * @param derivY Image derivative along y-axis.
	 * @param binary Non-zero pixels are considered to be line pixels.
	 */
	public <D extends ImageGray> void transform(D derivX , D derivY , GrayU8 binary )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,binary);

		transform.reshape(derivX.width,derivY.height);
		ImageMiscOps.fill(transform,0);

		originX = derivX.width/2;
		originY = derivX.height/2;
		candidates.reset();

		if( derivX instanceof GrayF32)
			_transform((GrayF32)derivX,(GrayF32)derivY,binary);
		else if( derivX instanceof GrayS16)
			_transform((GrayS16)derivX,(GrayS16)derivY,binary);
		else if( derivX instanceof GrayS32)
			_transform((GrayS32)derivX,(GrayS32)derivY,binary);
		else
			throw new IllegalArgumentException("Unsupported derivative image type: "+derivX.getClass().getSimpleName());
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

		extractor.process(transform,null, candidates,null, foundLines);

		for( int i = 0; i < foundLines.size(); i++ ) {
			Point2D_I16 p = foundLines.get(i);

			int x0 = p.x - originX;
			int y0 = p.y - originY;

			if( Math.abs(x0) >= minDistanceFromOrigin ||
					Math.abs(y0) >= minDistanceFromOrigin ) {
				LineParametric2D_F32 l = lines.grow();
				l.p.set(p.x,p.y);
				l.slope.set(-y0,x0);
				foundIntensity.push(transform.get(p.x,p.y));
			}
		}

		return lines;
	}

	/**
	 * Takes the detected point along the line and its gradient and converts it into transform space.
	 * @param x point in image.
	 * @param y point in image.
	 * @param derivX gradient of point.
	 * @param derivY gradient of point.
	 */
	public void parameterize( int x , int y , float derivX , float derivY )
	{
		// put the point in a new coordinate system centered at the image's origin
		// this minimizes error, which is a function of distance from origin
		x -= originX;
		y -= originY;

		float v = (x*derivX + y*derivY)/(derivX*derivX + derivY*derivY);

		// finds the foot a line normal equation and put the point into image coordinates
		int x0 = (int)(v*derivX) + originX;
		int y0 = (int)(v*derivY) + originY;

		if( transform.isInBounds(x0,y0)) {
			int index = transform.startIndex+y0*transform.stride+x0;
			// keep track of candidate pixels so that a sparse search can be done
			// to detect lines
			if( transform.data[index]++ == 1 )
				candidates.add(x0,y0);
		}
	}

	/**
	 * Returns the Hough transform image.
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

	private void _transform(GrayF32 derivX , GrayF32 derivY , GrayU8 binary )
	{
		// apply the transform to the entire image
		for( int y = 0; y < binary.height; y++ ) {
			int start = binary.startIndex + y*binary.stride;
			int end = start + binary.width;

			for( int index = start; index < end; index++ ) {
				if( binary.data[index] != 0 ) {
					int x = index-start;
					parameterize(x,y,derivX.unsafe_get(x,y),derivY.unsafe_get(x,y));
				}
			}
		}
	}

	private void _transform(GrayS16 derivX , GrayS16 derivY , GrayU8 binary )
	{
		// apply the transform to the entire image
		for( int y = 0; y < binary.height; y++ ) {
			int start = binary.startIndex + y*binary.stride;
			int end = start + binary.width;

			for( int index = start; index < end; index++ ) {
				if( binary.data[index] != 0 ) {
					int x = index-start;
					parameterize(x,y,derivX.unsafe_get(x,y),derivY.unsafe_get(x,y));
				}
			}
		}
	}

	private void _transform(GrayS32 derivX , GrayS32 derivY , GrayU8 binary )
	{
		// apply the transform to the entire image
		for( int y = 0; y < binary.height; y++ ) {
			int start = binary.startIndex + y*binary.stride;
			int end = start + binary.width;

			for( int index = start; index < end; index++ ) {
				if( binary.data[index] != 0 ) {
					int x = index-start;
					parameterize(x,y,derivX.unsafe_get(x,y),derivY.unsafe_get(x,y));
				}
			}
		}
	}
}
