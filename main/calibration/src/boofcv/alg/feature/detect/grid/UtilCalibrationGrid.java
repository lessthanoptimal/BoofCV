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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.misc.GImageStatistics;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Various functions for computing calibration grids.
 *
 * @author Peter Abeles
 */
public class UtilCalibrationGrid {

	/**
	 * Selects a threshold using the image histogram.
	 */
	public static int selectThreshold( ImageSingleBand image , int histogram[] ) {
		GImageStatistics.histogram(image,0,histogram);

		int mean = 0;
		for( int i = 1; i < histogram.length; i++ ) {
			mean += i*histogram[i];
		}
		mean /= (image.width*image.height);

		// select the center of mass for the two regions
		int lower = selectMiddle(histogram,0,mean);
		int upper = selectMiddle(histogram,mean,histogram.length);

		// pick the point which maximizes the separation between the two regions
		return (lower + upper)/2;
	}

	private static int selectMiddle( int histogram[] , int begin , int end ) {
		int target = 0;
		for( int i = begin; i < end; i++ ) {
			target += histogram[i];
		}

		target /= 2;

		int sum = 0;
		for( int i = begin; i < end; i++ ) {
			sum += histogram[i];
			if( sum >= target )
				return i;
		}
		return end-1;
	}


	private static int firstNotZero( int counts[] , int indexes[] ) {
		for( int i = 0; i < counts.length; i++ ) {
			if( counts[indexes[i]] != 0 )
				return i;
		}
		return counts.length;
	}

	/**
	 * Automatically checks and adjusts the points.  if the number of rows/columns are swapped the
	 * grid is rotated.  If no change is needed the original list is returned.  Otherwise null is returned
	 * if some failure condition is present.
	 *
	 * @param points Ordered points in a grid.
	 * @param numRows Number of rows in grid.
	 * @param numCols Number of columns in grid.
	 * @param expectedRows Number of expected rows.
	 * @param expectedCols Number of expected columns.
	 * @return Points in the correct order/crientation.
	 */
	public static List<Point2D_F64> rotatePoints( List<Point2D_F64> points ,
												  int numRows , int numCols ,
												  int expectedRows , int expectedCols ) {
		if( expectedCols == numCols && expectedRows == numRows )
			return points;
		if( expectedCols == numRows && expectedRows == numCols ) {
			return UtilCalibrationGrid.rotatePoints(points,numRows,numCols);
		}
		return null;
	}

	/**
	 * Rotates the grid by 90 degrees in the counter clockwise direction.  Useful when the points are organized
	 * into a grid that has the number of rows and columns swapped.   The returned points will be a grid
	 * with the input rows/columns swapped.
	 *
	 * @param points Ordered points in a grid.
	 * @param numRows Number of rows in grid.
	 * @param numCols Number of columns in grid.
	 * @return Ordered points.
	 */
	public static List<Point2D_F64> rotatePoints( List<Point2D_F64> points ,
												  int numRows , int numCols ) {

		List<Point2D_F64> out = new ArrayList<Point2D_F64>();

		for( int i = 0; i < numCols; i++ ) {
			for( int j = 0; j < numRows; j++ ) {

				int index = j*numCols + (numCols-i-1);

				out.add( points.get(index));
			}
		}

		return out;
	}

	/**
	 * Returns the next point in the list assuming a cyclical list
	 *
	 * @param i current index
	 * @param dir Direction and amount of increment
	 * @param size Size of the list
	 * @return i+dir taking in account the list's cyclical nature
	 */
	public static int incrementCircle( int i , int dir , int size ) {
		i += dir;
		if( i < 0 ) i = size+i;
		else if( i >= size ) i = i - size;
		return i;
	}

	/**
	 *
	 * dist = (dir > 0 ) i1-i0 ? i0-i1;
	 * if( dist < 0 )
	 * 	distance = size+distance;
	 *
	 * @param i0 First point.
	 * @param i1 Second point.
	 * @param dir 0 > counting down, 0 < counting up
	 * @param size
	 * @return
	 */
	public static int distanceCircle( int i0 , int i1 , int dir , int size ) {

		int distance = ( dir > 0 ) ? i1-i0 : i0-i1;

		if( distance < 0 )
			distance = size+distance;

		return distance;
	}

	/**
	 * Distance between two elements in a circular list.  The closest distance in either direction
	 * is returned.
	 *
	 * @param i0
	 * @param i1
	 * @param size
	 * @return
	 */
	public static int distanceCircle( int i0 , int i1 , int size ) {

		int distanceA = distanceCircle(i0,i1,1,size);
		int distanceB = distanceCircle(i0,i1,-1,size);

		return Math.min(distanceA,distanceB);
	}

	/**
	 * Find the average of all the points in the list.
	 *
	 * @param contour
	 * @return
	 */
	public static Point2D_I32 findAverage(List<Point2D_I32> contour) {

		int x = 0;
		int y = 0;

		for( Point2D_I32 p : contour ) {
			x += p.x;
			y += p.y;
		}

		x /= contour.size();
		y /= contour.size();

		return new Point2D_I32(x,y);
	}

	/**
	 * Sorts the points in counter clockwise direction around the provided point
	 *
	 * @param center Point that the angle is computed relative to
	 * @param contour List of all the points which are to be sorted by angle
	 */
	public static void sortByAngleCCW(Point2D_F64 center, List<Point2D_F64> contour) {
		double angles[] = new double[ contour.size() ];
		int indexes[] = new int[ angles.length ];

		for( int i = 0; i < contour.size(); i++ ) {
			Point2D_F64 c = contour.get(i);
			double dx = c.x-center.x;
			double dy = c.y-center.y;

			angles[i] = Math.atan2(dy,dx);
		}

		QuickSort_F64 sort = new QuickSort_F64();
		sort.sort(angles,angles.length,indexes);

		List<Point2D_F64> sorted = new ArrayList<Point2D_F64>(contour.size());
		for( int i = 0; i < indexes.length; i++ ) {
			sorted.add( contour.get( indexes[i]));
		}

		contour.clear();
		contour.addAll(sorted);
	}

	/**
	 * Returns the index of the point farthest away from the sample point
	 */
	public static int findFarthest( Point2D_I32 a , List<Point2D_I32> contour ) {
		int best = -1;
		int index = -1;

		for( int i = 0; i < contour.size(); i++ ) {
			Point2D_I32 b = contour.get(i);

			int d = a.distance2(b);

			if( d > best ) {
				best = d;
				index = i;
			}
		}

		return index;
	}

	public static int findFarthest( Point2D_F64 a , List<Point2D_F64> contour ) {
		double best = -1;
		int index = -1;

		for( int i = 0; i < contour.size(); i++ ) {
			Point2D_F64 b = contour.get(i);

			double d = a.distance2(b);

			if( d > best ) {
				best = d;
				index = i;
			}
		}

		return index;
	}
}
