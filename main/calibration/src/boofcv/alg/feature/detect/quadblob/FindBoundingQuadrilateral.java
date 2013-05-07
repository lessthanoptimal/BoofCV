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

package boofcv.alg.feature.detect.quadblob;

import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.feature.detect.grid.UtilCalibrationGrid.findFarthest;

/**
 * Given a set of points that are contained inside a quadrilateral and have at least one point
 * at each of the four extreme points on the quadrilateral, find the extreme points. These extreme points
 * are found by maximizing distance and area metrics in hopes of providing tolerance to noise.
 *
 * @author Peter Abeles
 */
public class FindBoundingQuadrilateral {
	/**
	 * Finds the corners of the quadrilateral.  Points are put into CCW order
	 *
	 * @param list List unordered corner points in the target. At least one point must be on a corner.
	 * @return The 4 corner points.
	 */
	public static List<Point2D_F64> findCorners(List<Point2D_F64> list) {

		// find the first corner
		Point2D_F64 corner0 = list.get(findFarthest(list.get(0), list));
		// and the second
		Point2D_F64 corner1 = list.get(findFarthest(corner0, list));

		// third point
		Point2D_F64 corner2 = maximizeArea(corner0,corner1,list);
		Point2D_F64 corner3 = maximizeForth(corner0, corner2, corner1, list);

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();
		ret.add(corner0);
		ret.add(corner1);
		ret.add(corner2);
		ret.add(corner3);

		// organize the corners
		Point2D_F64 center = UtilPoint2D_F64.mean(ret,null);
		UtilCalibrationGrid.sortByAngleCCW(center, ret);

		return ret;
	}

	/**
	 * Finds the point which will maximize the area of a triangle defined by a,b, and a point
	 * in the list.
	 *
	 * @param a Corner point in triangle
	 * @param b Corner point in triangle
	 * @param list Candidate points for the 3rd corner
	 * @return The point which maximizes the area
	 */
	public static Point2D_F64 maximizeArea( Point2D_F64 a , Point2D_F64 b , List<Point2D_F64> list )
	{
		double max = 0;
		Point2D_F64 maxPoint = null;

		for( Point2D_F64 c : list ) {
			double area = area(a,b,c);

			if( area > max ) {
				max = area;
				maxPoint = c;
			}
		}

		return maxPoint;
	}

	/**
	 * Finds the fourth point which maximizes the distance from a,b, and c.
	 */
	public static Point2D_F64 maximizeForth( Point2D_F64 a , Point2D_F64 b , Point2D_F64 c , List<Point2D_F64> list )
	{
		double max = 0;
		Point2D_F64 maxPoint = null;

		for( Point2D_F64 d : list ) {
			double l1 = a.distance(d);
			double l2 = b.distance(d);
			double l3 = c.distance(d);

			double score = l1+l2+l3;

			if( score > max ) {
				max = score;
				maxPoint = d;
			}
		}

		return maxPoint;
	}

	/**
	 * Computes the area of a triangle given its vertices.
	 */
	public static double area( Point2D_F64 a , Point2D_F64 b , Point2D_F64 c )
	{
		double top = a.x*(b.y-c.y) + b.x*(c.y-a.y) + c.x*(a.y-b.y);
		return Math.abs(top)/2.0;
	}
}
