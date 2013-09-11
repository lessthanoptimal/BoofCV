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

import georegression.metric.Distance2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Implementation of {@link SplitMergeLineFit} for lists in which the end points are not connected.
 *
 * @author Peter Abeles
 */
// TODO only check lines that changed for splitting
public class SplitMergeLineFitSegment extends SplitMergeLineFit {

	public SplitMergeLineFitSegment(double toleranceSplit, double toleranceMerge, int maxIterations) {
		super(toleranceSplit, toleranceMerge, maxIterations);
	}

	@Override
	public void process(List<Point2D_I32> list) {
		splits.reset();
		this.contour = list;
		if( list.size() <= 2 ) { // can't do anything with two or less points
			return;
		}

		// initial segmentation
		splits.add(0);
		splitPixels(0, list.size() - 1);
		splits.add(list.size()-1);

		for( int i = 0; i < maxIterations; i++ ) {
			if( !mergeSegments() )
				break;
			if( !splitSegments() )
				break;
		}
	}

	/**
	 * Recursively splits pixels.  Used in the initial segmentation.  Only split points between
	 * the two ends are added
	 */
	protected void splitPixels( int indexStart , int indexStop ) {
		// too short to split
		if( indexStart+1 >= indexStop )
			return;

		// end points of the line
		Point2D_I32 a = contour.get(indexStart);
		Point2D_I32 c = contour.get(indexStop);

		line.p.set(a.x,a.y);
		line.slope.set(c.x-a.x,c.y-a.y);

		int indexSplit = selectSplitOffset(indexStart,indexStop);

		if( indexSplit >= 0 ) {
			splitPixels(indexStart, indexSplit);
			splits.add(indexSplit);
			splitPixels(indexSplit, indexStop);
		}
	}

	/**
	 * Splits a line in two if there is a paint that is too far away
	 * @return true for change
	 */
	protected boolean splitSegments() {
		boolean change = false;

		work.reset();
		for( int i = 0; i < splits.size-1; i++ ) {
			int start = splits.data[i];
			int end = splits.data[i+1];

			Point2D_I32 a = contour.get(start);
			Point2D_I32 b = contour.get(end);

			line.p.set(a.x,a.y);
			line.slope.set(b.x-a.x,b.y-a.y);

			int bestIndex = selectSplitOffset(start,end);
			if( bestIndex >= 0 ) {
				change = true;
				work.add(start);
				work.add(bestIndex);
			} else {
				work.add(start);
			}
		}
		work.add(splits.data[ splits.size-1] );

		// swap the two lists
		GrowQueue_I32 tmp = work;
		work = splits;
		splits = tmp;

		return change;
	}

	/**
	 * Finds the point between indexStart and the end point which is the greater distance from the line
	 * (set up prior to calling).  Returns the index if the distance is less than tolerance, otherwise -1
	 */
	protected int selectSplitOffset( int indexStart , int indexEnd ) {
		int bestIndex = -1;
		double bestDistanceSq = 0;

		// don't try splitting at the two end points
		for( int i = indexStart+1; i < indexEnd; i++ ) {
			Point2D_I32 b = contour.get(i);
			point2D.set(b.x,b.y);

			double dist = Distance2D_F64.distanceSq(line, point2D);
			if( dist > bestDistanceSq ) {
				bestDistanceSq = dist;
				bestIndex = i;
			}
		}

		if( bestDistanceSq > toleranceSplitSq ) {
			return bestIndex;
		} else {
			return -1;
		}
	}

	/**
	 * Merges lines together which have an acute angle less than the threshold.
	 * @return true the list being changed
	 */
	protected boolean mergeSegments() {
		// can't merge a single line
		if( splits.size <= 2 )
			return false;

		boolean change = false;
		work.reset();

		// first point is always at the start
		work.add(splits.data[0]);

		Point2D_I32 a = contour.get(splits.data[0]);
		Point2D_I32 b = contour.get(splits.data[1]);

		for( int i = 0; i < splits.size-2; i++ ) {
			Point2D_I32 c = contour.get(splits.data[i+2]);

			double theta = computeAcute(a, b, c);

			if( theta <= toleranceMerge ) {
				// merge the two lines by not adding it
				change = true;
				b=c;
			} else {
				work.add(splits.data[i+1]);
				a=b;b=c;
			}
		}

		work.add(splits.data[splits.size-1]);

		// swap the two lists
		GrowQueue_I32 tmp = work;
		work = splits;
		splits = tmp;

		return change;
	}
}
