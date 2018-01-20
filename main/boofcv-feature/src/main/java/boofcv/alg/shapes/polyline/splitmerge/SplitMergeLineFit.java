/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline.splitmerge;

import boofcv.struct.ConfigLength;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_B;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Base class for algorithm which employ a split and merge strategy to fitting a set of line segments onto an
 * ordered set of points.  The lines are an approximation of the original shape described by the point list.
 * This list can either be connected at the end (looped) or not, depending on the implementation.  The points
 * in the list are assumed to be ordered with each consecutive point connected to its neighbors.  The output is
 * a set of indexes which correspond to points in the original list that compose the line segments.  A minimum
 * of two indexes will be returned.
 *
 * The returned set of line segments is guaranteed to match the original set of points to within a user
 * specified tolerance.  That is, no point in the list will be more than 'tol' distance away from a line segment.
 * A line is split when a point between two end points is greater than the split distance.  A corner is removed (two
 * lines merged) if the corner is less than the split distance away from two of its adjacent neighbors. The split
 * threshold is specified as a fraction of line distance to maximize scale invariance.  The minimum split threshold
 * is specified in units of pixels because a simple ratio doesn't work well for small objects.
 *
 * Split and merge is repeated until there is no more change or the maximum number of iterations has been reached.

 * @author Peter Abeles
 */
public abstract class SplitMergeLineFit {

	// maximum number of split and merge iterations
	protected int maxIterations;

	// How far away a point is from the line before it is split.  In fractions of a line segment's length squared.
	protected double toleranceFractionSq;

	// The maximum allowed distance a point can be from a line as a function of the overall
	// contour length
	protected ConfigLength minimumSideLength;
	protected int minimumSideLengthPixel;

	// Reference to the input contour list
	protected List<Point2D_I32> contour;

	// used to compute distance from line
	protected LineParametric2D_F64 line = new LineParametric2D_F64();
	protected Point2D_F64 point2D = new Point2D_F64();

	// list of vertexes
	protected GrowQueue_I32 splits = new GrowQueue_I32();
	GrowQueue_I32 work = new GrowQueue_I32();

	// indicates which line segments need to be checked for splits
	protected GrowQueue_B changed = new GrowQueue_B();

	// if there are more splits than this amount just give up.  It's probably noise
	protected int abortSplits = Integer.MAX_VALUE;

	/**
	 * Configures algorithm
	 * @param splitFraction A line will be split if a point is more than this fraction of its
	 *                     length away from the line. Try 0.05
	 * @param minimumSideLength The minimum length of a side as a function of contour length
	 * @param maxIterations  Maximum number of split and merge refinements. Set to zero to disable refinement. Try 20
	 */
	public SplitMergeLineFit(double splitFraction,
							 ConfigLength minimumSideLength,
							 int maxIterations)
	{
		setSplitFraction(splitFraction);
		this.minimumSideLength = minimumSideLength;
		setMaxIterations(maxIterations);
	}

	/**
	 * Approximates the input list with a set of line segments
	 *
	 * @param list (Input) Ordered list of connected points.
	 * @param vertexes (Output) Indexes in the input list which are corners in the polyline
	 * @return true if it could fit a polygon to the points or false if not
	 */
	public boolean process( List<Point2D_I32> list , GrowQueue_I32 vertexes ) {
		this.contour = list;
		this.minimumSideLengthPixel = minimumSideLength.computeI(contour.size());
		splits.reset();

		boolean result = _process(list);

		// remove reference so that it can be freed
		this.contour = null;
		vertexes.setTo(splits);
		return result;
	}

	protected abstract boolean _process( List<Point2D_I32> list );

	/**
	 * Computes the split threshold from the end point of two lines
	 */
	protected double splitThresholdSq( Point2D_I32 a , Point2D_I32 b ) {
		return Math.max(2,a.distance2(b)* toleranceFractionSq);
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public void setSplitFraction(double toleranceSplit) {
		this.toleranceFractionSq = toleranceSplit*toleranceSplit;
	}

	public int getAbortSplits() {
		return abortSplits;
	}

	public void setAbortSplits(int abortSplits) {
		this.abortSplits = abortSplits;
	}
}
