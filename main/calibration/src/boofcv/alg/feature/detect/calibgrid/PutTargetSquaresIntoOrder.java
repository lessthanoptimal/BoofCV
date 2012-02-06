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

import georegression.metric.ClosestPoint2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import pja.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Given a complete set of {@link SquareBlob} that is in arbitrary order and composes an entire calibration target,
 * find a logical ordering of the squares and check the target's validity,  The corners in each blob are also put
 * into correct order.
 * </p>
 * 
 * <p>
 * Assumptions:
 * <ul>
 * <li> {@link SquareBlob} are unordered in the provided list.</li>
 * <li> Corner points are in a CCW order</li>
 * <li> Every row in calibration target has the same number of squares in it.</li>
 * <li> The whole target is observed.</li>
 * </ul>
 * </p>
 * .
 * <p>
 * Note that knowledge of the calibration target's dimension is not required or its orientation.  If the target
 * is rectangular then a transpose might be required to align the points with its definition.
 * </p>
 *
 * @author Peter Abeles
 */
public class PutTargetSquaresIntoOrder {
	// list of blobs provided
	private List<SquareBlob> blobs;

	// list of blobs put into a valid order
	private List<SquareBlob> blobsOrdered;

	// bounding quadrilateral 
	List<Point2D_I32> targetCorners;
	// sorting algorithm
	private QuickSort_F64 sort = new QuickSort_F64();

	// number of elements in each row
	private int numCols;
	// number of rows
	private int numRows;

	/**
	 * Processes the list of squares to find and orient corner points in the calibration target. Only
	 * corners in the target are assumed to be in this list.  If anything goes wrong an i{@link InvalidTarget}
	 * exception is thrown. Connections between the blobs needs to be computed already and must only refer
	 * to blobs inside this list.
	 * 
	 * @param blob List of square blobs that compose a target.  Connections are modified for book keeping.
	 * @throws InvalidTarget
	 */
	public void process( List<SquareBlob> blob ) throws InvalidTarget {
		// set up data structures
		this.blobs = new ArrayList<SquareBlob>(blob);
		
		// find the bounding quadrilateral around target blobs
		List<Point2D_I32> targetObs = toPointList(blobs);
		targetCorners = FindBoundingQuadrilateral.findCorners(targetObs);

		// connect blobs to each other making extraction of rows and columns easier later on
//		ConnectGridSquares.connect(blobs);
		blobsOrdered = putBlobsIntoOrder();
	}

	/**
	 * Given the bounding quadrilateral it orders the blobs and the corners inside each blobs
	 *
	 * @return  List of blobs in row-major order
	 * @throws InvalidTarget
	 */
	private List<SquareBlob> putBlobsIntoOrder() throws InvalidTarget {
		// select the blob in the top left corner of the target
		// Which corner is selected is arbitrary
		SquareBlob seed = findBlobWithPoint(targetCorners.get(0));

		// list in which the blobs are ordered
		List<SquareBlob> orderedBlob = new ArrayList<SquareBlob>();

		// extract the top most row, and the columns on the left and right side of the grid
		List<SquareBlob> topRow = findLine(seed,targetCorners.get(1));
		List<SquareBlob> leftCol = findLine(seed,targetCorners.get(3));
		List<SquareBlob> rightCol = findLine(topRow.get(topRow.size()-1),targetCorners.get(2));

		// perform a high level sanity check of what's been extracted so far
		sanityCheckTarget(topRow, leftCol, rightCol);
		
		// Corners of bounding quadrilateral, which are updated as rows are removed
		Point2D_I32 topLeft = targetCorners.get(0);
		Point2D_I32 topRight = targetCorners.get(1);
		Point2D_I32 bottomLeft = targetCorners.get(3);
		Point2D_I32 bottomRight = targetCorners.get(2);

		// order corners in the blobs in the first row
		orderBlobRow(topRow,topLeft,topRight);

		// extract the remaining rows
		numRows = 1;
		numCols = topRow.size();
		if( blobs.size() % numCols != 0 )
			throw new InvalidTarget("Total square not divisible by row size");

		while( true ) {
			orderedBlob.addAll(topRow);
			removeRow(topRow,blobs);
			topRow.clear();
			leftCol.remove(0);
			rightCol.remove(0);

			if( blobs.size() > 0 ) {
				numRows++;
				// find the next row
				topRow = findLine(leftCol.get(0), rightCol.get(0).center);

				// update the top corners used to order corners inside the row
				topLeft = selectFarthest(leftCol.get(0).corners, bottomLeft, bottomRight);
				topRight = selectFarthest(rightCol.get(0).corners, bottomRight , bottomLeft);

				// order corners inside the row
				orderBlobRow(topRow, topLeft, topRight);
				if( topRow.size() != numCols )
					throw new InvalidTarget("Unexpected row size: found "+topRow.size()+" expected "+numCols);
			} else {
				break;
			}
		}
		return orderedBlob;
	}

	/**
	 * Make sure what has been found so far meets expectations
	 */
	private void sanityCheckTarget(List<SquareBlob> topRow, 
								   List<SquareBlob> leftCol, 
								   List<SquareBlob> rightCol) 
			throws InvalidTarget 
	{
		if( leftCol.size() != rightCol.size() )
			throw new InvalidTarget("Left and right columns have different length: "+leftCol.size()+" "+rightCol.size());

		int N = blobs.size();
		int cols = topRow.size();
		int rows = leftCol.size();

		if( N != cols*rows )
			throw new InvalidTarget("Total number of elements does not match number of rows/columns.");

		// See if the connection grid is valid
		int totalConnections = 0;
		for( SquareBlob b : blobs ) {
			totalConnections += b.conn.size();
		}
		int expected = N*4 - (2*cols + 2*N/cols);
		if( expected != totalConnections )
			throw new InvalidTarget("Bad connection graph. Unexpected number of connections.");

//		for( int i = 0; i < leftCol.size(); i++ ) {
//			int x0 = leftCol.get(i).center.x;
//			int y0 = leftCol.get(i).center.y;
//			int x1 = rightCol.get(i).center.x;
//			int y1 = rightCol.get(i).center.y;
//
//			System.out.println("columns "+x0+" "+y0+"  ,  "+x1+" "+y1);
//		}
	}

	/**
	 * Selects the point in the list which has the greatest combined distance from points 'a' and 'b'.
	 * Distance is computed as Manhattan distance, which mean it is computed independently along a coordinate
	 * system's axis.  The axis is specified by points a and b.
	 */
	protected static Point2D_I32 selectFarthest( List<Point2D_I32> corners , Point2D_I32 a , Point2D_I32 b )
	{
		LineParametric2D_F64 b_to_a = new LineParametric2D_F64(b.x,b.y,a.x-b.x,a.y-b.y);
		// normalize to make t in same units as everything else
		b_to_a.slope.normalize();
		LineParametric2D_F64 a_to_n = new LineParametric2D_F64(a.x,a.y,-b_to_a.slope.y,b_to_a.slope.x);
		
		Point2D_I32 best = null;
		double bestDist = 0;
		
		for( Point2D_I32 p : corners ) {
			
			double distA = ClosestPoint2D_F64.closestPointT(b_to_a,new Point2D_F64(p.x,p.y));
			double distB = ClosestPoint2D_F64.closestPointT(a_to_n,new Point2D_F64(p.x,p.y));
			distA = Math.abs(distA);
			distB = Math.abs(distB);
			
			double d = distA + distB;
			if( d > bestDist ) {
				bestDist = d;
				best = p;
			}
		}
		
		return best;
	}

	/**
	 * Orders the corners in each blob in the list.  It is assumed that all the blobs in the list belong
	 * to a single row in the grid and the two points (begin and end) represent the top left/right
	 * corners in the row.
	 *
	 * @param blobs List of blobs in the top row
	 * @param begin Top left corner of row
	 * @param end Top right corner of row
	 */
	protected void orderBlobRow( List<SquareBlob> blobs , Point2D_I32 begin , Point2D_I32 end )
	{
		LineParametric2D_F64 line = new LineParametric2D_F64(begin.x,begin.y,end.x-begin.x,end.y-begin.y);

		for( SquareBlob b : blobs )
			orderCorners(b,line);
	}
	
	/**
	 * Order the corners in the blob such that the first two are closest to the line and the
	 * first one is closest to the origin of the line.
	 */
	protected void orderCorners( SquareBlob blob , LineParametric2D_F64 line) {

		
		// find the two points closest to the line
		double distances[] = new double[4];
		for( int i = 0; i < 4; i++ ) {
			Point2D_I32 p = blob.corners.get(i);
			distances[i] = Distance2D_F64.distance(line,new Point2D_F64(p.x,p.y));
		}
		int indexes[] = new int[4];
		sort.sort(distances,4,indexes);
		
		Point2D_I32 closeA = blob.corners.get(indexes[0]);
		Point2D_I32 closeB = blob.corners.get(indexes[1]);
		Point2D_I32 farC = blob.corners.get(indexes[2]);
		Point2D_I32 farD = blob.corners.get(indexes[3]);
		
		// distance from line's start
		double distA = ClosestPoint2D_F64.closestPointT(line, new Point2D_F64(closeA.x,closeA.y));
		double distB = ClosestPoint2D_F64.closestPointT(line, new Point2D_F64(closeB.x,closeB.y));
		double distC = ClosestPoint2D_F64.closestPointT(line, new Point2D_F64(farC.x,farC.y));
		double distD = ClosestPoint2D_F64.closestPointT(line, new Point2D_F64(farD.x,farD.y));

		blob.corners.clear();
		if( distA < distB ) {
			blob.corners.add(closeA);
			blob.corners.add(closeB);
		} else {
			blob.corners.add(closeB);
			blob.corners.add(closeA);
		}
		if( distC < distD ) {
			blob.corners.add(farD);
			blob.corners.add(farC);
		} else {
			blob.corners.add(farC);
			blob.corners.add(farD);
		}
	}

	/**
	 * Remove the specified row from the list and all connections to elements in the row
	 * @param row Squares which are to be removed.
	 * @param all List of all squares, is modified.
	 */
	private void removeRow( List<SquareBlob> row , List<SquareBlob> all ) {
		for( SquareBlob b : row ) {
			for( SquareBlob c : b.conn ) {
				if( !c.conn.remove(b) )
					throw new RuntimeException("Bug, element not contained");
			}
			if( !all.remove(b) )
				throw new RuntimeException("Bug");
		}
	}

	/**
	 * Uses previously computed connections to find a row of blobs in the calibration target.  The squares
	 * in the returned row are from left to right.  The 'target' point provides the direction that the
	 * blobs connections should be traversed.
	 *
	 * @param blob Left most point in the row.
	 * @param target Top right corner in calibration target.
	 * @return
	 */
	public List<SquareBlob> findLine( SquareBlob blob , Point2D_I32 target ) {
		List<SquareBlob> ret = new ArrayList<SquareBlob>();

		// get the direction the graph should be traversed
		double targetAngle = Math.atan2( target.y - blob.center.y , target.x - blob.center.x );

		while( blob != null ) {
			ret.add(blob);
			double best = Double.MAX_VALUE;
			SquareBlob found = null;
			
			// see if any of  the connected point towards the target
			for( SquareBlob c : blob.conn ) {
				double angle = Math.atan2(c.center.y-blob.center.y,c.center.x-blob.center.x);
				double acute = UtilAngle.dist(targetAngle,angle);
				if( acute < best ) {
					best = acute;
					found = c;
				}
			}
			if( best < Math.PI/3 ) {
				blob = found;
			} else {
				blob = null;
			}
		}

		return ret;
	}

	/**
	 * Search through the list of blobs for a blob which contains the specified point
	 */
	public SquareBlob findBlobWithPoint( Point2D_I32 pt )
	{
		for( SquareBlob b : blobs ) {
			if (contains(pt, b))
				return b;
		}

		return null;
	}

	private boolean contains(Point2D_I32 pt, SquareBlob b) {
		for( Point2D_I32 c : b.corners ) {
			if( c.x == pt.x && c.y == pt.y ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts the list of squares into a list of points composed of the square's corners
	 */
	public static List<Point2D_I32> toPointList( List<SquareBlob> blobs ) {
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();
		
		for( SquareBlob s : blobs ) {
			ret.addAll(s.corners);
		}
		
		return ret;
	}

	/**
	 * Returns corners in bounding quadrilateral
	 */
	public List<Point2D_I32> getQuadrilateral() {
		return targetCorners;
	}

	public List<SquareBlob> getBlobsOrdered() {
		return blobsOrdered;
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
