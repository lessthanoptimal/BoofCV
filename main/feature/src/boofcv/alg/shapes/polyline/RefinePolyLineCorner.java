/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline;

import boofcv.misc.CircularIndex;
import georegression.geometry.UtilLine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * <p>
 * Optimizing corner placements to a pixel level when given a contour and integer list of approximate
 * corner locations which define set of line segments.    Corners are optimized by searching for another near by
 * pixel in the provided contour which reduces the distance of the contour from the line segments.  This
 * is intended to optimize the output from {@link SplitMergeLineFit}.  Can be configured to
 * handle case where line segments form a loop or have disconnected end points.
 * </p>
 *
 * <p>
 * Each corner is individually optimized once per iteration.  The process is repeated until the maximum number of
 * iterations has been reached or there is no change in corner placements.
 * </p>
 *
 * @author Peter Abeles
 */
public class RefinePolyLineCorner {

	// maximum number of iterations
	private int maxIterations = 10;

	// maximum number of samples along the line
	private final int maxLineSamples = 20;

	// the radius it will search around. computed when contour is passed in
	protected int searchRadius;

	// local storage
	private LineSegment2D_F64 work = new LineSegment2D_F64();

	LineGeneral2D_F64 line0 = new LineGeneral2D_F64();
	LineGeneral2D_F64 line1 = new LineGeneral2D_F64();

	// do the line segments form a loop or not?
	boolean looping;

	/**
	 * Constructor with configurable parameters
	 *
	 * @param looping true if it loops or false if not
	 * @param maxIterations Number of internal EM iterations
	 */
	public RefinePolyLineCorner(boolean looping, int maxIterations) {
		this.looping = looping;
		this.maxIterations = maxIterations;
	}

	/**
	 * Constructor using default parameters
	 */
	public RefinePolyLineCorner(boolean looping) {
		this.looping = looping;
	}

	/**
	 * Fits a polygon to the contour given an initial set of candidate corners.  If not looping the corners
	 * must include the end points still.  Minimum of 3 points required.  Otherwise there's no corner!.
	 *
	 * @param contour Contours around the shape
	 * @param corners (Input) initial set of corners.  (output) refined set of corners
	 */
	public boolean fit( List<Point2D_I32> contour , GrowQueue_I32 corners )
	{
		if( corners.size() < 3 ) {
			return false;
		}
		searchRadius = Math.min(6,Math.max(contour.size()/12,3));

		int startCorner,endCorner;
		if( looping ) {
			startCorner = 0;
			endCorner = corners.size;
		} else {
			// the end point positions are fixed
			startCorner = 1;
			endCorner = corners.size-1;
		}

		boolean change = true;
		for( int iteration = 0; iteration < maxIterations && change; iteration++ ) {
			change = false;
			for (int i = startCorner; i < endCorner; i++) {
				int c0 = CircularIndex.minusPOffset(i, 1, corners.size());
				int c2 = CircularIndex.plusPOffset(i, 1, corners.size());

				int improved = optimize(contour, corners.get(c0), corners.get(i), corners.get(c2));
				if( improved != corners.get(i)) {
					corners.set(i,improved);
					change = true;
				}
			}
		}

		return true;
	}


	/**
	 * Searches around the current c1 point for the best place to put the corner
	 *
	 * @return location of best corner in local search region
	 */
	protected int optimize( List<Point2D_I32> contour , int c0,int c1,int c2 ) {

		double bestDistance = computeCost(contour,c0,c1,c2,0);
		int bestIndex = 0;
		for( int i = -searchRadius; i <= searchRadius; i++ ) {
			if( i == 0 ) {
				// if it found a better point in the first half stop the search since that's probably the correct
				// direction.  Could be improved by remember past search direction
				if( bestIndex != 0 )
					break;
			} else {
				double found = computeCost(contour, c0, c1, c2, i);
				if (found < bestDistance) {
					bestDistance = found;
					bestIndex = i;
				}
			}
		}
		return CircularIndex.addOffset(c1, bestIndex, contour.size());
	}

	/**
	 * Computes the distance between the two lines defined by corner points in the contour
	 * @param contour list of contour points
	 * @param c0 end point of line 0
	 * @param c1 start of line 0 and 1
	 * @param c2 end point of line 1
	 * @param offset added to c1 to make start of lines
	 * @return sum of distance of points along contour
	 */
	protected double computeCost(List<Point2D_I32> contour, int c0, int c1, int c2,
							   int offset)
	{
		c1 = CircularIndex.addOffset(c1, offset, contour.size());
		createLine(c0,c1,contour,line0);
		createLine(c1,c2,contour,line1);
		return distanceSum(line0,c0,c1,contour)+distanceSum(line1,c1,c2,contour);
	}

	/**
	 * Sum of Euclidean distance of contour points along the line
	 */
	protected double distanceSum( LineGeneral2D_F64 line , int c0 , int c1 , List<Point2D_I32> contour ) {
		double total = 0;
		if( c0 < c1  ) {
			int length = c1-c0+1;
			int samples = Math.min(maxLineSamples,length);

			for (int i = 0; i < samples; i++) {
				int index = c0 + i*(length-1)/(samples-1);
				total += distance(line,contour.get(index));
			}
		} else {
			int lengthFirst = contour.size()-c0;
			int lengthSecond = c1+1;

			int length = lengthFirst+c1+1;
			int samples = Math.min(maxLineSamples,length);

			int samplesFirst = samples*lengthFirst/length;
			int samplesSecond = samples*lengthSecond/length;

			for (int i = 0; i < samplesFirst; i++) {
				int index = c0 + i*lengthFirst/(samples-1);
				total += distance(line,contour.get(index));
			}
			for (int i = 0; i < samplesSecond; i++) {
				int index = i*lengthSecond/(samples-1);
				total += distance(line,contour.get(index));
			}
		}
		return total;
	}

	/**
	 * If A*A + B*B == 1 then a simplified distance formula can be used
	 */
	protected static double distance( LineGeneral2D_F64 line , Point2D_I32 p ) {
		return Math.abs(line.A*p.x + line.B*p.y + line.C);
	}

	/**
	 * Given segment information create a line in general notation which has been normalized
	 */
	private void createLine( int index0 , int index1 , List<Point2D_I32> contour , LineGeneral2D_F64 line )
	{
		if( index1 < 0 )
			System.out.println("SHIT");
		Point2D_I32 p0 = contour.get(index0);
		Point2D_I32 p1 = contour.get(index1);

//		System.out.println("createLine "+p0+" "+p1);

		work.a.set(p0.x, p0.y);
		work.b.set(p1.x, p1.y);

		UtilLine2D_F64.convert(work,line);

		// ensure A*A + B*B = 1
		line.normalize();
	}
}
