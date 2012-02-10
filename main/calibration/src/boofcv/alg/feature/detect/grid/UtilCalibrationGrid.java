/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import pja.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class UtilCalibrationGrid {

	/**
	 * Note that when viewed on the monitor this will appear to be clockwise because
	 * the y-axis points down.
	 *
	 * @param points Calibration points on the grid
	 * @param numCols Number of columns
	 * @param numRows Number of rows
	 */
	public static void enforceClockwiseOrder(List<Point2D_F64> points,
											 int numCols, int numRows) {
		Point2D_F64 a = points.get(0);
		Point2D_F64 b = points.get(points.size()-1);
		Point2D_F64 c = points.get(numCols-1);

		double x_c = (a.x+b.x)/2;
		double y_c = (a.y+b.y)/2;

		double angA = Math.atan2(a.x-x_c,a.y-y_c);
		double angB = Math.atan2(c.x-x_c,c.y-y_c);

		double dist = UtilAngle.minus(angB, angA);

		if( dist > 0 )
			return;

		// flip the points
		for( int i = 0; i < numRows; i++ ) {
			int rowIndex = i*numCols;
			for( int j = 0; j < numCols/2; j++ ) {
				a = points.get(rowIndex+j);
				b = points.get(rowIndex+numCols-j-1);
				points.set(rowIndex+j,b);
				points.set(rowIndex+numCols-j-1,a);
			}
		}

	}

	/**
	 * Given a set of squares, transpose the squares and the corners inside the squares.
	 *
	 * @param orderedBlobs List of blobs in a valid order, but transposed
	 * @param numRows Number of rows in the desired grid
	 * @param numCols Number of cols in the desired grid
	 * @return Transposed detected target
	 */
	public static List<QuadBlob> transposeOrdered( List<QuadBlob> orderedBlobs , int numRows , int numCols ) {
		List<QuadBlob> temp = new ArrayList<QuadBlob>();

		for( int i = 0; i < numRows; i++ ) {
			for( int j = 0; j < numCols; j++ ) {
				QuadBlob b = orderedBlobs.get(j*numRows+i);
				temp.add(b);
				Point2D_I32 a0 = b.corners.get(0);
				Point2D_I32 a1 = b.corners.get(1);
				Point2D_I32 a2 = b.corners.get(2);
				Point2D_I32 a3 = b.corners.get(3);

				b.corners.clear();
				b.corners.add(a0);
				b.corners.add(a3);
				b.corners.add(a2);
				b.corners.add(a1);
			}
		}
		return temp;
	}
	
	/**
	 * Converts the list of square blobs that are in order with ordered corners into a list of points.
	 * Both the square and the points inside the squares must be in a logical ordering for a complete target.
	 *
	 * @param blobs Set of ordered blobs that compose the target
	 * @param points Output set of points.
	 * @param numCols Number of blobs per row (number of columns).
	 */
	public static void extractOrderedPoints( List<QuadBlob> blobs ,
											 List<Point2D_I32> points ,
											 int numCols ) {
		points.clear();
		for( int i = 0; i < blobs.size(); i += numCols ) {
			for( int j = 0; j < numCols; j++ ) {
				QuadBlob b = blobs.get(i+j);
				points.add(b.corners.get(0));
				points.add(b.corners.get(1));
			}
			for( int j = 0; j < numCols; j++ ) {
				QuadBlob b = blobs.get(i+j);
				points.add(b.corners.get(3));
				points.add(b.corners.get(2));
			}
		}
	}

	/**
	 * Converts the list of square blobs that are in order with ordered corners into a list of points.
	 * Both the square and the points inside the squares must be in a logical ordering for a complete target.
	 *
	 * @param blobs Set of ordered blobs that compose the target
	 * @param points Output set of points at sub-pixel accuracy.
	 * @param numCols Number of blobs per row (number of columns).
	 */
	public static void extractOrderedSubpixel( List<QuadBlob> blobs ,
											   List<Point2D_F64> points ,
											   int numCols ) {
		points.clear();
		for( int i = 0; i < blobs.size(); i += numCols ) {
			for( int j = 0; j < numCols; j++ ) {
				QuadBlob b = blobs.get(i + j);
				points.add(b.subpixel.get(0));
				points.add(b.subpixel.get(1));
			}
			for( int j = 0; j < numCols; j++ ) {
				QuadBlob b = blobs.get(i+j);
				points.add(b.subpixel.get(3));
				points.add(b.subpixel.get(2));
			}
		}
	}

	/**
	 * Returns the next point in the list assuming a cyclical list
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
	public static void sortByAngleCCW(Point2D_I32 center, List<Point2D_I32> contour) {
		double angles[] = new double[ contour.size() ];
		int indexes[] = new int[ angles.length ];

		for( int i = 0; i < contour.size(); i++ ) {
			Point2D_I32 c = contour.get(i);
			int dx = c.x-center.x;
			int dy = c.y-center.y;

			angles[i] = Math.atan2(dy,dx);
		}

		QuickSort_F64 sort = new QuickSort_F64();
		sort.sort(angles,angles.length,indexes);

		List<Point2D_I32> sorted = new ArrayList<Point2D_I32>(contour.size());
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
}
