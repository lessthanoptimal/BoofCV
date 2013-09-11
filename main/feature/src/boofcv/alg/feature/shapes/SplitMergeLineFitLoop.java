/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.shapes;

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

	public SplitMergeLineFitLoop(double toleranceSplit,
								 double toleranceMerge,
								 int maxIterations)
	{
		super(toleranceSplit, toleranceMerge, maxIterations);
	}


	@Override
	public void process( List<Point2D_I32> contour ) {
		this.contour = contour;
		this.N = contour.size();

		// ------------- find initial line segments
		splits.reset();

		// can't fit a line to a single point
		if( N <= 1 ) {
			return;
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
			return; // can't merge a single line
		for( int i = 0; i < maxIterations; i++ ) {
			if( !mergeSegments() )
				break;
			if( !splitSegments() )
				break;
		}
	}

	/**
	 * Recursively splits pixels between indexStart to indexStart+length.  A split happens if there is a pixel
	 * more than the desired distance away from the two end points. Results are placed into 'splits'
	 */
	protected void splitPixels(int indexStart, int length) {
		// too short to split
		if( length <= 1 )
			return;

		// end points of the line
		int indexEnd = (indexStart+length)%N;
		Point2D_I32 a = contour.get(indexStart);
		Point2D_I32 c = contour.get(indexEnd);

		line.p.set(a.x,a.y);
		line.slope.set(c.x-a.x,c.y-a.y);

		int splitOffset = selectSplitOffset(indexStart,length);

		if( splitOffset >= 0 ) {
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

		if( bestIndex == -1 )
			System.out.println();

		return bestIndex;
	}

	/**
	 * Merges lines together which have an acute angle less than the threshold.
	 * @return true the list being changed
	 */
	protected boolean mergeSegments() {
		boolean change = false;
		work.reset();

		Point2D_I32 a = contour.get(splits.data[splits.size - 1]);
		Point2D_I32 b = contour.get(splits.data[0]);
		Point2D_I32 c = contour.get(splits.data[1]);

		double theta = computeAcute(a, b, c);
		if( theta > toleranceMerge ) {
			work.add(splits.data[0]);
			a=b;b=c;
		} else {
			b=c;
			change = true;
		}

		for( int i = 0; i < splits.size-2; i++ ) {
			c = contour.get(splits.data[i+2]);

			theta = computeAcute(a, b, c);

			if( theta <= toleranceMerge ) {
				// merge the two lines by not adding it
				change = true;
				b=c;
			} else {
				work.add(splits.data[i+1]);
				a=b;b=c;
			}
		}

		c = contour.get(work.data[0]);
		theta = computeAcute(a, b, c);
		if( theta > toleranceMerge ) {
			work.add(splits.data[splits.size-1]);
		} else {
			change = true;
		}

		// swap the two lists
		GrowQueue_I32 tmp = work;
		work = splits;
		splits = tmp;

		return change;
	}

	/**
	 * Splits a line in two if there is a paint that is too far away
	 * @return true for change
	 */
	protected boolean splitSegments() {
		boolean change = false;

		work.reset();
		for( int i = 0; i < splits.size-1; i++ ) {
			change = checkSplit(change, i,i+1);
		}

		change = checkSplit(change, splits.size-1,0);

		// swap the two lists
		GrowQueue_I32 tmp = work;
		work = splits;
		splits = tmp;

		return change;
	}

	private boolean checkSplit(boolean change, int i0 , int i1) {
		int start = splits.data[i0];
		int end = splits.data[i1];
		int length = circularDistance(start,end);

		Point2D_I32 a = contour.get(start);
		Point2D_I32 b = contour.get(end);

		line.p.set(a.x,a.y);
		line.slope.set(b.x-a.x,b.y-a.y);

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
	 * (set up prior to calling).  Returns the index if the distance is less than tolerance, otherwise -1
	 */
	protected int selectSplitOffset( int indexStart , int length ) {
		int bestOffset = -1;
		double bestDistanceSq = 0;

		// don't try splitting at the two end points
		for( int i = 1; i < length; i++ ) {
			Point2D_I32 b = contour.get((indexStart+i)%N);
			point2D.set(b.x,b.y);

			double dist = Distance2D_F64.distanceSq(line, point2D);
			if( dist > bestDistanceSq ) {
				bestDistanceSq = dist;
				bestOffset = i;
			}
		}

		if( bestDistanceSq > toleranceSplitSq ) {
			return bestOffset;
		} else {
			return -1;
		}
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
