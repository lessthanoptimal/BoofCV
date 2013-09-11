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
 * The returned set of line segments is guaranteed to match the original set of points to within a use
 * specified tolerance.  That is, no point in the list will be more than 'tol' distance away from a line segment.
 * The split and merge technique works by finding searching for the point which is the greatest distance
 * from the two end points.  If that point is more than 'tol' away the line is split.  This is recursively repeated
 * for each segment until no more are needed.
 *
 * After the initial segmentation the points are split and merged until no more changes happens or the maximum
 * number of iterations has been reached.  Lines are merged if the acute angle between them is less than a tolerance.
 * Then the lines are checked to see if they need to be merged.
 *
 *
 * @author Peter Abeles
 */
public abstract class SplitMergeLineFit {

	// maximum number of split and merge iterations
	protected int maxIterations = 100;

	// How far away a point is from the line before it is split.  pixels squared
	protected double toleranceSplitSq;

	// Tolerance for how similar two lines angles are before being merged.  in radians
	protected double toleranceMerge;

	// Reference to the input contour list
	protected List<Point2D_I32> contour;

	// used to compute distance from line
	protected LineParametric2D_F64 line = new LineParametric2D_F64();
	protected Point2D_F64 point2D = new Point2D_F64();

	// list of vertexes
	protected GrowQueue_I32 splits = new GrowQueue_I32();
	protected GrowQueue_I32 work = new GrowQueue_I32();

	// indicates which line segments need to be checked for splits
	protected GrowQueue_B changed = new GrowQueue_B();

	/**
	 *
	 * @param toleranceSplit A line is split if a point is found that is more than this distance (pixels)
	 *                       away from the line.
	 * @param toleranceMerge Two lines are merged together if their angle is less than or equal to this number.
	 * @param maxIterations  Maximum number of split and merge refinements. Set to zero to disable refinement
	 */
	public SplitMergeLineFit(double toleranceSplit,
							 double toleranceMerge,
							 int maxIterations)
	{
		this.toleranceSplitSq = toleranceSplit*toleranceSplit;
		this.toleranceMerge = toleranceMerge;
		this.maxIterations = maxIterations;
	}

	/**
	 * Approximates the input list with a set of line segments
	 *
	 * @param list Ordered list of connected points.
	 */
	public abstract void process( List<Point2D_I32> list );

	/**
	 * Returns the acute angle in radians between the two lines
	 */
	protected double computeAcute(Point2D_I32 a, Point2D_I32 b, Point2D_I32 c) {
		int dx0 = b.x-a.x;
		int dy0 = b.y-a.y;

		int dx1 = c.x-b.x;
		int dy1 = c.y-b.y;

		int dot = dx0*dx1 + dy0*dy1;
		double bottom = Math.sqrt(dx0*dx0 + dy0*dy0) * Math.sqrt(dx1*dx1 + dy1*dy1);
		return Math.acos( dot/bottom );
	}

	/**
	 * List of point indexes in the contour.  Each point is the end of a line segment.  A minimum of
	 * two points will be returned.
	 *
	 * NOTE: This list is modified the next time process is called.
	 *
	 * @return List of indexes
	 */
	public GrowQueue_I32 getSplits() {
		return splits;
	}
}
