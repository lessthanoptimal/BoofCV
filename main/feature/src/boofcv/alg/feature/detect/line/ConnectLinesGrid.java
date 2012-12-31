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

package boofcv.alg.feature.detect.line;


import boofcv.struct.feature.MatrixOfList;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;

import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Given a grid of detected line segments connect line segments together if they appear to be
 * apart of the same line.  First lines inside the same segment are considered and connected together
 * then lines in neighboring segments are linked together.   Connectivity rules are inspired by [1] with
 * a couple of departures and optimizations.
 * </p>
 *
 * <p>
 * Two lines are considered to belong to the same line if:
 * <ol>
 * <li>Line slopes are similar to within tolerance.</li>
 * <li>If the closest point on each line are within tangential and longitudinal distance tolerances.</li>
 * <ul>
 *     <li>Orientation is determined by the first line segment's slope.</li>
 * </ul>
 * </ol>
 * When searching for matches for a line segment which is the closest to the line is the one in which
 * it is merged with. Two lines are merged together by creating a new line segment whose end points are the two points
 * in each line which are farthest apart from each other.
 * </p>
 *
 * <p>
 * [1] M. Hirzer, "Marker Detection for Augmented Reality Applications" Seminar/Project Image Analysis Graz,
 * October 27, 2008.
 * </p>
 * @author Peter Abeles
 */
// todo compute stan once for every line
public class ConnectLinesGrid {
	// how similar two lines need to be, in radians
	float lineSlopeAngleTol;
	// distance tolerance along longitudinal axis
	float parallelTol;
	// distance tolerance along tangent axis
	float tangentTol;

	// used to store results of an internal function
	float dist[] = new float[4];
	int closestIndex;
	int farthestIndex;

	// input grid of lines.
	MatrixOfList<LineSegment2D_F32> grid;

	/**
	 * Specify line connecting parameters.
	 *
	 * @param lineSlopeAngleTol How similar the slope two lines need to be in radians.  Try 0.062.
	 * @param tangentTol Tolerance along tangential axis.  Try 1.
	 * @param parallelTol Tolerance along longitudinal axis.  Try 8.
	 */
	public ConnectLinesGrid(double lineSlopeAngleTol, double tangentTol, double parallelTol ) {
		this.lineSlopeAngleTol = (float)lineSlopeAngleTol;
		this.tangentTol = (float)(tangentTol);
		this.parallelTol = (float)(parallelTol);
	}

	public void process( MatrixOfList<LineSegment2D_F32> grid ) {
		this.grid = grid;

		// first connect lines inside the same element
		for( int i = 0; i < grid.height; i++ ) {
			for( int j = 0; j < grid.width; j++ ) {
				connectInSameElement(grid.get(j,i));
			}
		}

		// connect neighboring grid cells
		for( int i = 0; i < grid.height; i++ ) {
			for( int j = 0; j < grid.width; j++ ) {
				connectToNeighbors(j, i);
			}
		}
	}

	/**
	 * Connect lines in the target region to lines in neighboring regions.  Regions are selected such that
	 * no two regions are compared against each other more than once.
	 *
	 * @param x target region grid x-coordinate
	 * @param y target region grid y-coordinate
	 */
	private void connectToNeighbors(int x, int y ) {
		List<LineSegment2D_F32> lines = grid.get(x,y);

		Iterator<LineSegment2D_F32> iter = lines.iterator();
		while( iter.hasNext() )
		{
			LineSegment2D_F32 l = iter.next();
			boolean connected = false;

			if( connectTry(l,x+1,y) )
				connected = true;
			if( !connected && connectTry(l,x+1,y+1) )
				connected = true;
			if( !connected && connectTry(l,x,y+1) )
				connected = true;
			if( !connected && connectTry(l,x-1,y+1) )
				connected = true;

			// the line was added to the connecting grid
			// remove it to avoid double counting the line
			if( connected )
				iter.remove();
		}
	}

	/**
	 * See if there is a line that matches in this adjacent region.
	 *
	 * @param target Line being connected.
	 * @param x x-coordinate of adjacent region.
	 * @param y y-coordinate of adjacent region.
	 * @return true if a connection was made.
	 */
	private boolean connectTry( LineSegment2D_F32 target , int x , int y ) {
		if( !grid.isInBounds(x,y) )
			return false;

		List<LineSegment2D_F32> lines = grid.get(x,y);

		int index = findBestCompatible(target,lines,0);

		if( index == -1 )
			return false;

		LineSegment2D_F32 b = lines.remove(index);

		// join the two lines by connecting the farthest points from each other
		Point2D_F32 pt0 = farthestIndex < 2 ? target.a : target.b;
		Point2D_F32 pt1 = (farthestIndex %2) == 0 ? b.a : b.b;

		target.a.set(pt0);
		target.b.set(pt1);

		// adding the merged one back in allows it to be merged with other lines down
		// the line.  It will be compared against others in 'target's grid though
		lines.add(target);

		return true;
	}

	/**
	 * Search for lines in the same region for it to be connected to.
	 *
	 * @param lines All the lines in the region.
	 */
	private void connectInSameElement(List<LineSegment2D_F32> lines ) {
		for( int i = 0; i < lines.size(); i++ ) {
			LineSegment2D_F32 a = lines.get(i);

			int index = findBestCompatible(a,lines,i+1);
			if( index == -1 )
				continue;

			// remove the line from the index which it is being connected to
			LineSegment2D_F32 b = lines.remove(index);

			// join the two lines by connecting the farthest points from each other
			Point2D_F32 pt0 = farthestIndex < 2 ? a.a : a.b;
			Point2D_F32 pt1 = (farthestIndex %2) == 0 ? b.a : b.b;

			a.a.set(pt0);
			a.b.set(pt1);
		}
	}

	/**
	 * Searches for a line in the list which the target is compatible with and can
	 * be connected to.
	 *
	 * @param target Line being connected to.
	 * @param candidates List of candidate lines.
	 * @param start First index in the candidate list it should start searching at.
	 * @return Index of the candidate it can connect to.  -1 if there is no match.
	 */
	private int findBestCompatible( LineSegment2D_F32 target ,
									List<LineSegment2D_F32> candidates ,
									int start )
	{
		int bestIndex = -1;
		double bestDistance = Double.MAX_VALUE;
		int bestFarthest = 0;

		float targetAngle = UtilAngle.atanSafe(target.slopeY(),target.slopeX());
		float cos = (float)Math.cos(targetAngle);
		float sin = (float)Math.sin(targetAngle);

		for( int i = start; i < candidates.size(); i++ ) {
			LineSegment2D_F32 c = candidates.get(i);

			float angle = UtilAngle.atanSafe(c.slopeY(),c.slopeX());

			// see if the two lines have the same slope
			if( UtilAngle.distHalf(targetAngle,angle) > lineSlopeAngleTol )
				continue;

			// see the distance the two lines are apart and if it could be the best line
			closestFarthestPoints(target, c);

			// two closest end points
			Point2D_F32 pt0 = closestIndex < 2 ? target.a : target.b;
			Point2D_F32 pt1 = (closestIndex %2) == 0 ? c.a : c.b;

			float xx = pt1.x-pt0.x;
			float yy = pt1.y-pt0.y;

			float distX = Math.abs(cos*xx - sin*yy);
			float distY = Math.abs(cos*yy + sin*xx);

			if( distX >= bestDistance ||
					distX > parallelTol || distY > tangentTol )
				continue;

			// check the angle of the combined line
			pt0 = farthestIndex < 2 ? target.a : target.b;
			pt1 = (farthestIndex %2) == 0 ? c.a : c.b;

			float angleCombined = UtilAngle.atanSafe(pt1.y-pt0.y,pt1.x-pt0.x);

			if( UtilAngle.distHalf(targetAngle,angleCombined) <= lineSlopeAngleTol ) {
				bestDistance = distX;
				bestIndex = i;
				bestFarthest = farthestIndex;
			}
		}

		if( bestDistance < parallelTol) {
			farthestIndex = bestFarthest;
			return bestIndex;
		}
		return -1;
	}

	/**
	 * Finds the points on each line which are closest and farthest away from each other.
	 */
	private void closestFarthestPoints(LineSegment2D_F32 a, LineSegment2D_F32 b)
	{
		dist[0] = a.a.distance2(b.a);
		dist[1] = a.a.distance2(b.b);
		dist[2] = a.b.distance2(b.a);
		dist[3] = a.b.distance2(b.b);

		// find the two points which are closest together and save which ones those are
		// for future reference
		farthestIndex = 0;
		float closest = dist[0];
		float farthest = dist[0];
		for( int i = 1; i < 4; i++ ) {
			float d = dist[i];
			if( d < closest ) {
				closest = d;
				closestIndex = i;
			}
			if( d > farthest ) {
				farthest = d;
				farthestIndex = i;
			}
		}
	}
}
