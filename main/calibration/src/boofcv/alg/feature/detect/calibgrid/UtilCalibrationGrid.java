/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class UtilCalibrationGrid {

	/**
	 * Converts the list of square blobs that are in order with ordered corners into a list of points.
	 * Both the square and the points inside the squares must be in a logical ordering for a complete target.
	 *
	 * @param blobs Set of ordered blobs that compose the target
	 * @param points Output set of points.
	 * @param numCols Number of blobs per row (number of columns).
	 */
	public static void extractOrderedPoints( List<SquareBlob> blobs ,
											 List<Point2D_I32> points ,
											 int numCols ) {
		points.clear();
		for( int i = 0; i < blobs.size(); i += numCols ) {
			for( int j = 0; j < numCols; j++ ) {
				SquareBlob b = blobs.get(i+j);
				points.add(b.corners.get(0));
				points.add(b.corners.get(1));
			}
			for( int j = 0; j < numCols; j++ ) {
				SquareBlob b = blobs.get(i+j);
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
	public static void extractOrderedSubpixel( List<SquareBlob> blobs ,
											   List<Point2D_F32> points ,
											   int numCols ) {
		points.clear();
		for( int i = 0; i < blobs.size(); i += numCols ) {
			for( int j = 0; j < numCols; j++ ) {
				SquareBlob b = blobs.get(i + j);
				points.add(b.subpixel.get(0));
				points.add(b.subpixel.get(1));
			}
			for( int j = 0; j < numCols; j++ ) {
				SquareBlob b = blobs.get(i+j);
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
}
