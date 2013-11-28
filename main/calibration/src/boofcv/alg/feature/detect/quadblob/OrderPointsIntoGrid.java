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

import boofcv.alg.feature.detect.InvalidCalibrationTarget;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;


/**
 * Given a set of points that form an approximate grid, put them into grid order.  The order shall be in a row
 * major format moving in the counter clockwise direction.  If the data is found to not be a grid then an exception
 * is thrown.  Besides ordering points, the number of rows and columns is determined.  The initial starting point
 * of the grid is arbitrary leading to the possibility that the orientation of the grid can be off.
 */
public class OrderPointsIntoGrid {
	//  Final list of ordered points
	private List<Point2D_F64> ordered;

	// bounding quadrilateral 
	List<Point2D_F64> targetCorners;

	// number of elements in each row
	private int numCols;
	// number of rows
	private int numRows;

	// internal copy of unordered list which can be modified.
	List<Point2D_F64> internal = new ArrayList<Point2D_F64>();

	/**
	 * Process the list of points and puts them into grid order
	 *
	 * @param unordered List of points not in any order.
	 * @return Puts which have been put into grid order.
	 * @throws InvalidCalibrationTarget Throw if the points are found to not be a grid.
	 */
	public List<Point2D_F64> process( List<Point2D_F64> unordered ) throws InvalidCalibrationTarget {

		internal.clear();
		internal.addAll(unordered);

		// Corners of calibration grid
		targetCorners = FindBoundingQuadrilateral.findCorners(internal);

		// connect blobs to each other making extraction of rows and columns easier later on
		ordered = putBlobsIntoOrder(internal);

		return ordered;
	}

	/**
	 * Puts the blobs into grid order using the previously found quadrilateral.
	 */
	private List<Point2D_F64> putBlobsIntoOrder( List<Point2D_F64> unordered ) throws InvalidCalibrationTarget {

		// number of total points
		final int N = unordered.size();

		// select the blob in the top left corner of the target
		// Which corner is selected is arbitrary
		Point2D_F64 seed = targetCorners.get(0);

		// list in which the blobs are ordered
		List<Point2D_F64> orderedBlob = new ArrayList<Point2D_F64>();

		// extract the top most row, and the columns on the left and right side of the grid
		List<Point2D_F64> topRow = findLine(seed,targetCorners.get(1), unordered);
		List<Point2D_F64> leftCol = findLine(seed,targetCorners.get(3), unordered);
		List<Point2D_F64> rightCol = findLine(topRow.get(topRow.size()-1),targetCorners.get(2), unordered);

		// perform a high level sanity check of what's been extracted so far
		sanityCheckTarget(topRow, leftCol, rightCol, N );

		// extract the remaining rows
		numRows = 1;
		numCols = topRow.size();

		while( true ) {
			if( topRow.size() != numCols )
				throw new InvalidCalibrationTarget("row with unexpected number of column");
			orderedBlob.addAll(topRow);
			removeRow(topRow, unordered);
			leftCol.remove(0);
			rightCol.remove(0);

			if( unordered.size() > 0 ) {
				numRows++;
				// find the next row
				topRow = findLine(leftCol.get(0), rightCol.get(0), unordered);
			} else {
				break;
			}
		}
		return orderedBlob;
	}

	/**
	 * Make sure what has been found so far meets expectations
	 */
	private void sanityCheckTarget(List<Point2D_F64> topRow,
								   List<Point2D_F64> leftCol,
								   List<Point2D_F64> rightCol,
								   int totalPoints )
			throws InvalidCalibrationTarget
	{
		if( leftCol.size() != rightCol.size() )
			throw new InvalidCalibrationTarget("Left and right columns have different length: "+leftCol.size()+" "+rightCol.size());

		int cols = topRow.size();
		int rows = leftCol.size();

		if( totalPoints != cols*rows )
			throw new InvalidCalibrationTarget("Total number of elements does not match number of rows/columns.");
	}

	/**
	 * Remove the specified row from the list and all connections to elements in the row
	 * @param row Squares which are to be removed.
	 * @param all List of all squares, is modified.
	 */
	private void removeRow( List<Point2D_F64> row , List<Point2D_F64> all ) {
		for( Point2D_F64 b : row ) {
			if( !all.remove(b) )
				throw new RuntimeException("Bug");
		}
	}

	/**
	 * Uses previously computed connections to find a row of blobs in the calibration target.  The squares
	 * in the returned row are from left to right.  The 'target' point provides the direction that the
	 * blobs connections should be traversed.
	 *
	 * @param startPt Left most point in the row.
	 * @param target Top right corner in calibration target.
	 * @return
	 */
	public List<Point2D_F64> findLine( Point2D_F64 startPt, Point2D_F64 target , List<Point2D_F64> candidates ) {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		// get the direction the graph should be traversed
		double targetAngle = Math.atan2( target.y - startPt.y , target.x - startPt.x );


		while( startPt != null ) {
			ret.add(startPt);
			if( startPt == target )
				break;

			double best = Double.MAX_VALUE;
			Point2D_F64 found = null;
			
			// see if any of the connected point towards the target
			for( Point2D_F64 c : candidates ) {
				if( ret.contains(c))
					continue;

				double angle = Math.atan2(c.y - startPt.y,c.x - startPt.x);
				double acute = UtilAngle.dist(targetAngle,angle);
				if( acute < Math.PI/3 ) {
					// straight line will have the shortest distance
					// there can be multiple candidates that are straight lines, so bias
					// it towards being closest to the start
					double dist = 2*c.distance(startPt) + c.distance(target);
					if( dist < best ) {
						best = dist;
						found = c;
					}
				}
			}
			startPt = found;
		}

		return ret;
	}



	/**
	 * Returns corners in bounding quadrilateral
	 */
	public List<Point2D_F64> getQuadrilateral() {
		return targetCorners;
	}

	public List<Point2D_F64> getOrdered() {
		return ordered;
	}

	/**
	 * Number of columns in found calibration grid
	 */
	public int getNumCols() {
		return numCols;
	}

	/**
	 * Number of rows in found calibration grid
	 */
	public int getNumRows() {
		return numRows;
	}
}
