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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Given the output from edge non-maximum suppression, perform hysteresis threshold along the edge and constructs
 * a list of pixels belonging to each contour.  Points are first connected in direction tangential to the edge's
 * direction, if no matches are found then a match is searched for using an 8-connect rule.  The direction
 * image must be the 4-direction type.  If multiple points in the local neighborhood can be added to edge then
 * a new edge segment is created.
 *
 * @author Peter Abeles
 */
/*
 * DESIGN NOTE: EdgeContour and EdgeSegment are not recycled because the internal arrays of their members can
 * grow to be quite large, but there is no way to assign objects with large internal arrays to newly found
 * lists which require large arrays.  That was a long sentence.
 */
public class HysteresisEdgeTracePoints {

	// after an edge has been traversed it is set to this value.  This is also why the lower threshold
	// must be >= 0
	public static final float MARK_TRAVERSED = -1;

	// reference to input intensity and direction images
	private GrayF32 intensity; // intensity after edge non-maximum suppression
	private GrayS8 direction; // 4-direction

	// List of found contours in the image
	private List<EdgeContour> contours = new ArrayList<>();

	// list of segments which have yet to be explored
	private List<EdgeSegment> open = new ArrayList<>();

	private FastQueue<Point2D_I32> queuePoints = new FastQueue<>(Point2D_I32.class, true);

	// the active contour which is being traced
	private EdgeContour e;

	// lower threshold
	private float lower;

	/**
	 * Performs hysteresis thresholding using the provided lower and upper thresholds.
	 *
	 * @param intensity Intensity image after edge non-maximum suppression has been applied.  Modified.
	 * @param direction 4-direction image.  Not modified.
	 * @param lower Lower threshold.
	 * @param upper Upper threshold.
	 */
	public void process(GrayF32 intensity , GrayS8 direction , float lower , float upper ) {
		if( lower < 0 )
			throw new IllegalArgumentException("Lower must be >= 0!");
		InputSanityCheck.checkSameShape(intensity, direction);

		// set up internal data structures
		this.intensity = intensity;
		this.direction = direction;
		this.lower = lower;
		queuePoints.reset();
		contours.clear();

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
	 *
	 * @param x x-coordinate of seed pixel above threshold
	 * @param y y-coordinate of seed pixel above threshold
	 * @param indexInten Pixel index in the image array of coordinate (x,y)
	 */
	protected void trace( int x , int y , int indexInten ) {

		e = new EdgeContour();
		contours.add(e);

		int dx,dy;

		addFirstSegment(x, y);
		intensity.data[ indexInten ] = MARK_TRAVERSED;

		while( open.size() > 0 ) {
			EdgeSegment s = open.remove( open.size()-1 );
			Point2D_I32 p = s.points.get(0);
			indexInten = intensity.getIndex(p.x,p.y);
			int indexDir = direction.getIndex(p.x,p.y);

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
				x = p.x; y = p.y;
				int fx = p.x+dx, fy = p.y+dy;
				int bx = p.x-dx, by = p.y-dy;

				// See if the forward point is in bounds and above the lower threshold
				if( intensity.isInBounds(fx,fy) && intensity.data[ indexForward ] >= lower ) {
					intensity.data[ indexForward ] = MARK_TRAVERSED;
					p = queuePoints.grow();
					p.set(fx,fy);
					s.points.add(p);
					match = true; // note that a match has already been found
					indexInten = indexForward;
					indexDir = prevIndexDir  + dy*intensity.stride + dx;
				}
				// See if the backwards point is in bounds and above the lower threshold
				if( intensity.isInBounds(bx,by) && intensity.data[ indexBackward ] >= lower ) {
					intensity.data[ indexBackward ] = MARK_TRAVERSED;
					if( match ) {
						// a match was found in the forwards direction, so start a new segment here
						startNewSegment(bx, by,s);
					} else {
						p = queuePoints.grow();
						p.set(bx,by);
						s.points.add(p);
						match = true;
						indexInten = indexBackward;
						indexDir = prevIndexDir  - dy*intensity.stride - dx;
					}
				}

				// if it failed to find a match in the forwards/backwards direction or its the first pixel
				// search the whole 8-neighborhood.
				if( first || !match ) {
					boolean priorMatch = match;
					// Check local neighbors if its one of the end points, which would be the first point or
					// any point for which no matches were found
					match = checkAllNeighbors(x,y,s,match);

					if( !match )
						break;
					else {
						// if it was the first it's no longer the first
						first = false;

						// the point at the end was just added and is to be searched in the next iteration
						if( !priorMatch ) {
							p = s.points.get( s.points.size()-1 );
							indexInten = intensity.getIndex(p.x, p.y);
							indexDir = direction.getIndex(p.x,p.y);
						}
					}
				}
			}
		}
	}

	private boolean checkAllNeighbors( int x , int y ,EdgeSegment parent , boolean match ) {
		match |= check(x+1,y,parent,match);
		match |= check(x,y+1,parent,match);
		match |= check(x-1,y,parent,match);
		match |= check(x,y-1,parent,match);

		match |= check(x+1,y+1,parent,match);
		match |= check(x+1,y-1,parent,match);
		match |= check(x-1,y+1,parent,match);
		match |= check(x-1,y-1,parent,match);

		return match;
	}

	/**
	 * Checks to see if the given coordinate is above the lower threshold.  If it is the point will be
	 * added to the current segment or be the start of a new segment.
	 *
	 * @param parent The edge segment which is being checked
	 * @param match Has a match to the current segment already been found?
	 * @return true if a match was found at this point
	 */
	private boolean check( int x , int y , EdgeSegment parent , boolean match ) {

		if( intensity.isInBounds(x,y)  ) {
			int index = intensity.getIndex(x,y);
			if( intensity.data[index] >= lower ) {
				intensity.data[index] = MARK_TRAVERSED;

				if( !match ) {
					Point2D_I32 p = queuePoints.grow();
					p.set(x,y);
					parent.points.add(p);
				} else {
					// a match was found so it can't just be added to the current edge
					startNewSegment(x,y,parent);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Starts a new segment at the first point in the contour
	 */
	private void addFirstSegment(int x, int y) {
		Point2D_I32 p = queuePoints.grow();
		p.set(x,y);
		EdgeSegment s = new EdgeSegment();
		s.points.add(p);
		s.index = 0;
		s.parent = s.parentPixel = -1;
		e.segments.add(s);
		open.add(s);
	}

	/**
	 * Starts a new segment in the contour at the specified coordinate.
	 */
	private void startNewSegment( int x , int y , EdgeSegment parent ) {
		// create the point which is the first
		Point2D_I32 p = queuePoints.grow();
		p.set(x,y);
		EdgeSegment s = new EdgeSegment();
		s.parent = parent.index;
		// if a new segment is created that means an extra point has been added to the end already, hence -2 and not -1
		s.parentPixel = parent.points.size()-2;
		s.index = e.segments.size();
		s.points.add(p);
		e.segments.add(s);
		open.add(s);
	}

	/**
	 * Returns the found contours.  Returned data structures are subject to modification next time process is called.
	 *
	 * @return List of found contours.
	 */
	public List<EdgeContour> getContours() {
		return contours;
	}
}
