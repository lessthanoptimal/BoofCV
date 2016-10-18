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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

/**
 * Given the output from edge non-maximum suppression, perform hysteresis threshold along the edge and mark selected
 * pixels in a binary image.  Points are first marked in direction tangential to the edge's
 * direction, if no matches are found then a match is searched for using an 8-connect rule.  The direction
 * image must be the 4-direction type.
 *
 * @author Peter Abeles
 */
public class HysteresisEdgeTraceMark {

	// after an edge has been traversed it is set to this value
	public static final float MARK_TRAVERSED = -1;

	// reference to input intensity and direction images
	private GrayF32 intensity; // intensity after edge non-maximum suppression
	private GrayS8 direction; // 4-direction
	// output binary image
	private GrayU8 output;

	// lower threshold
	private float lower;

	// list of points which have yet to be explored
	private FastQueue<Point2D_I32> open = new FastQueue<>(Point2D_I32.class, true);

	// point which is current being examined
	private Point2D_I32 active = new Point2D_I32();

	/**
	 * Performs hysteresis thresholding using the provided lower and upper thresholds.
	 *
	 * @param intensity Intensity image after edge non-maximum suppression has been applied.  Modified.
	 * @param direction 4-direction image.  Not modified.
	 * @param lower Lower threshold.
	 * @param upper Upper threshold.
	 * @param output Output binary image. Modified.
	 */
	public void process(GrayF32 intensity , GrayS8 direction , float lower , float upper ,
						GrayU8 output ) {
		if( lower < 0 )
			throw new IllegalArgumentException("Lower must be >= 0!");
		InputSanityCheck.checkSameShape(intensity,direction,output);

		// set up internal data structures
		this.intensity = intensity;
		this.direction = direction;
		this.output = output;
		this.lower = lower;
		ImageMiscOps.fill(output,0);

		// step through each pixel in the image
		for( int y = 0; y < intensity.height; y++ ) {
			int indexInten = intensity.startIndex + y*intensity.stride;

			for( int x = 0; x < intensity.width; x++ , indexInten++ ) {
				// start a search if a pixel is found that's above the threshold
				if( intensity.data[indexInten] >= upper ) {
					trace( x,y,indexInten);
				}
			}
		}
	}

	/**
	 * Traces along object's contour starting at the specified seed.  As it does so it will set the intensity of
	 * points which are below the lower threshold to zero and add points to contour.
	 */
	protected void trace( int x , int y , int indexInten ) {

		int dx,dy;

		int indexOut = output.getIndex(x,y);
		open.grow().set(x,y);
		output.data[indexOut] = 1;
		intensity.data[ indexInten ] = MARK_TRAVERSED;

		while( open.size() > 0 ) {
			active.set(open.removeTail());
			indexInten = intensity.getIndex(active.x,active.y);
			int indexDir = direction.getIndex(active.x,active.y);

			boolean first = true;

			while( true ) {
				//----- First check along the direction of the edge.  Only need to check 2 points this way
				switch( direction.data[ indexDir ] ) {
					case  0: dx =  0;dy=  1; break;
					case  1: dx =  1;dy= -1; break;
					case  2: dx =  1;dy=  0; break;
					case -1: dx =  1;dy=  1; break;
					default: throw new RuntimeException("Unknown direction: "+direction.data[ indexDir ]);
				}

				int indexForward = indexInten + dy*intensity.stride + dx;
				int indexBackward = indexInten - dy*intensity.stride - dx;

				int prevIndexDir = indexDir;

				boolean match = false;

				// pixel coordinate of forward and backward point
				x = active.x; y = active.y;
				int fx = active.x+dx, fy = active.y+dy;
				int bx = active.x-dx, by = active.y-dy;

				if( intensity.isInBounds(fx,fy) && intensity.data[ indexForward ] >= lower ) {
					intensity.data[ indexForward ] = MARK_TRAVERSED;
					output.unsafe_set(fx, fy, 1);
					active.set(fx, fy);
					match = true;
					indexInten = indexForward;
					indexDir = prevIndexDir  + dy*intensity.stride + dx;
				}
				if( intensity.isInBounds(bx,by) && intensity.data[ indexBackward ] >= lower ) {
					intensity.data[ indexBackward ] = MARK_TRAVERSED;
					output.unsafe_set(bx,by,1);
					if( match ) {
						open.grow().set(bx,by);
					} else {
						active.set(bx,by);
						match = true;
						indexInten = indexBackward;
						indexDir = prevIndexDir  - dy*intensity.stride - dx;
					}
				}

				if( first || !match ) {
					boolean priorMatch = match;
					// Check local neighbors if its one of the end points, which would be the first point or
					// any point for which no matches were found
					match = checkAllNeighbors(x,y,match);

					if( !match )
						break;
					else {
						// if it was the first it's no longer the first
						first = false;

						// the point at the end was just added and is to be searched in the next iteration
						if( !priorMatch ) {
							indexInten = intensity.getIndex(active.x, active.y);
							indexDir = direction.getIndex(active.x,active.y);
						}
					}
				}
			}
		}
	}

	private boolean checkAllNeighbors( int x , int y , boolean match ) {
		match |= check(x+1,y,match);
		match |= check(x,y+1,match);
		match |= check(x-1,y,match);
		match |= check(x,y-1,match);

		match |= check(x+1,y+1,match);
		match |= check(x+1,y-1,match);
		match |= check(x-1,y+1,match);
		match |= check(x-1,y-1,match);

		return match;
	}

	/**
	 * Checks to see if the given coordinate is above the lower threshold.  If it is the point will be
	 * added to the current segment or be the start of a new segment.
	 *
	 * @param match Has a match to the current segment already been found?
	 * @return true if a match was found at this point
	 */
	private boolean check( int x , int y , boolean match ) {

		if( intensity.isInBounds(x,y)  ) {
			int index = intensity.getIndex(x,y);
			if( intensity.data[index] >= lower ) {
				intensity.data[index] = MARK_TRAVERSED;
				output.unsafe_set(x,y,1);

				if( match ) {
					open.grow().set(x, y);
				} else {
					active.set(x,y);
				}
				return true;
			}
		}
		return false;
	}
}
