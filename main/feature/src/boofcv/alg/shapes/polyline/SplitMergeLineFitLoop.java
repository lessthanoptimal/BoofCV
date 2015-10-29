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

import georegression.geometry.UtilPoint2D_I32;
import georegression.metric.Distance2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Implementation of {@link SplitMergeLineFit} for looped lists of points.  The beginning and end of the list are
 * assumed to be connected.  An additional check is done in the start to find two points which are far apart.
 *
 * @author Peter Abeles
 */
// TODO only check lines that changed for splitting
public class SplitMergeLineFitLoop extends SplitMergeLineFit {

	// number of points in the contour
	protected int N;

	/**
	 * @see SplitMergeLineFit#SplitMergeLineFit(double,double, int)
	 */
	public SplitMergeLineFitLoop(double splitFraction,
								 double minimumSplitFraction,
								 int maxIterations)
	{
		super(splitFraction,minimumSplitFraction, maxIterations);
	}


	@Override
	public boolean process( List<Point2D_I32> contour ) {
		this.contour = contour;
		this.N = contour.size();
		this.minimumSideLengthPixel = (int)Math.ceil(N* minimumSideLengthFraction);

		// ------------- find initial line segments
		splits.reset();

		// can't fit a line to a single point
		if( N <= 1 ) {
			return false;
		}

		// Go around the contour looking for two points on opposite ends which are far apart
		int startIndex = selectFarthest(contour);
		int middleIndex = (startIndex+N/2)%N;

		// split each half recursively
		splits.add(startIndex);
		splitPixels(startIndex, N / 2);
		splits.add(middleIndex);
		splitPixels(middleIndex, N - (N / 2));

		// ------------  Refine the initial segments by splitting and merging each segment
		if( splits.size <= 2 )
			return false; // can't merge a single line

//		System.out.println("Enter loop");
		for( int i = 0; i < maxIterations; i++ ) {
			boolean merged = mergeSegments();

			if( splits.size() <= 0)
				return false;

			if( !merged && !splitSegments() )
				break;

			if( splits.size() <= 2 || splits.size() >= abortSplits )
				return false;
		}

		return true;
	}

	/**
	 * Recursively splits pixels between indexStart to indexStart+length.  A split happens if there is a pixel
	 * more than the desired distance away from the two end points. Results are placed into 'splits'
	 */
	protected void splitPixels(int indexStart, int length) {
		// too short to split
		if( length < minimumSideLengthPixel)
			return;

		// end points of the line
		int indexEnd = (indexStart+length)%N;

		int splitOffset = selectSplitOffset(indexStart,length);

		if( splitOffset >= 0 ) {
//			System.out.println("  splitting ");
			splitPixels(indexStart, splitOffset);
			int indexSplit = (indexStart+splitOffset)%N;
			splits.add(indexSplit);
			splitPixels(indexSplit, circularDistance(indexSplit, indexEnd));
		}
	}

	/**
	 * Computes the distance between pairs of points which are separated by 1/2 the contour list. The index of the
	 * first pixel in the pair with the greatest distance is returned
	 * @return Index of the first pixel which should be used to split the list.  The other end is ret + N/2
	 */
	protected int selectFarthest( List<Point2D_I32> contour ) {
		int bestIndex = -1;
		int bestDistance = 0;

		int N = contour.size();
		int half = N/2;

		for( int i = 0; i < half; i++ ) {

			int end = (i+half)%N;

			Point2D_I32 a = contour.get(i);
			Point2D_I32 b = contour.get(end);

			int dist = UtilPoint2D_I32.distanceSq(a.x,a.y,b.x,b.y);
			if( bestDistance < dist ) {
				bestIndex = i;
				bestDistance = dist;
			}
		}

//		if( bestIndex == -1 )
//			System.out.println();

		return bestIndex;
	}

	/**
	 * Merges lines together if the common corner is close to a common line
	 * @return true the list being changed
	 */
	protected boolean mergeSegments() {

		// See if merging will cause a degenerate case
		if( splits.size() <= 3 )
			return false;

		boolean change = false;
		work.reset();

		for( int i = 0; i < splits.size; i++ ) {
			int start = splits.data[i];
			int end = splits.data[(i+2)%splits.size];

			if( selectSplitOffset(start,circularDistance(start,end)) < 0 ) {
				// merge the two lines by not adding it
				change = true;
			} else {
				work.add(splits.data[(i + 1)%splits.size]);
			}

		}

		// swap the two lists
		GrowQueue_I32 tmp = work;
		work = splits;
		splits = tmp;

		return change;
	}

	/**
	 * Splits a line in two if there is a point that is too far away
	 * @return true for change
	 */
	protected boolean splitSegments() {
		boolean change = false;

		work.reset();
		for( int i = 0; i < splits.size-1; i++ ) {
			change |= checkSplit(change, i,i+1);
		}

		change |= checkSplit(change, splits.size - 1, 0);

		// swap the two lists
		GrowQueue_I32 tmp = work;
		work = splits;
		splits = tmp;

		return change;
	}

	private boolean checkSplit(boolean change, int i0 , int i1) {
		int start = splits.data[i0];
		int end = splits.data[i1];
		int length = circularDistance(start, end);

		int bestOffset = selectSplitOffset(start,length);
		if( bestOffset >= 0 ) {
			change = true;
			work.add(start);
			work.add((start+bestOffset)%N);
		} else {
			work.add(start);
		}
		return change;
	}


	/**
	 * Finds the point between indexStart and the end point which is the greater distance from the line
	 * (set up prior to calling).  Returns the index of the element with a distances greater than tolerance, otherwise -1
	 */
	protected int selectSplitOffset( int indexStart , int length ) {
		int bestOffset = -1;

		int indexEnd = (indexStart + length) % N;
		Point2D_I32 startPt = contour.get(indexStart);
		Point2D_I32 endPt = contour.get(indexEnd);

		line.p.set(startPt.x,startPt.y);
		line.slope.set(endPt.x-startPt.x,endPt.y-startPt.y);

		double bestDistanceSq = splitThresholdSq(contour.get(indexStart), contour.get(indexEnd));

		// adjusting using 'minimumSideLengthPixel' to ensure it doesn't create a new line which is too short
		int minLength = Math.max(1,minimumSideLengthPixel);// 1 is the minimum so that you don't split on the same corner
		length -= minLength;
		for( int i = minLength; i <= length; i++ ) {
			Point2D_I32 b = contour.get((indexStart+i)%N);
			point2D.set(b.x,b.y);

			double dist = Distance2D_F64.distanceSq(line, point2D);
			if( dist >= bestDistanceSq ) {
				bestDistanceSq = dist;
				bestOffset = i;
			}
		}

		return bestOffset;
	}

	/**
	 * Distance the two points are apart in clockwise direction
	 */
	protected int circularDistance( int start , int end ) {
		if( end >= start )
			return end-start;
		else
			return N-start+end;
	}
}
