/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.feature.detect.squares.*;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Given a binary image it detects the presence of chess board calibration grids. 1) Detect blobs
 * in binary image and select square like ones. 2) Create two grids, inner and outer, 3) Put the
 * grids together, 4) extract initial estimate for corners
 *
 * @author Peter Abeles
 */
public class DetectChessSquarePoints<T extends ImageSingleBand> {

	// detector for squares
	BinaryPolygonConvexDetector<T> detectorSquare;

	// Converts detected squares into a graph and into grids
	SquaresIntoClusters s2c;
	ClustersIntoGrids c2g;

	// size of square grids
	private int outerRows,outerCols;
	private int innerRows,innerCols;

	// bounding quadrilateral
	private Polygon2D_I32 boundPolygon = new Polygon2D_I32();

	SquareGridTools tools = new SquareGridTools();

	// bounding polygon of inner grid
	private Polygon2D_F64 innerPolygon = new Polygon2D_F64(4);

	// The chessboard grid.  Combination of inner and outer grids
	SquareGrid uberGrid = new SquareGrid();

	FastQueue<Point2D_F64> calibrationPoints = new FastQueue<Point2D_F64>(Point2D_F64.class,true);

	// maximum distance two corners can be from each other
	double maxCornerDistanceSq;

	// storage for the nodes which are used to align the two grids
	SquareNode seedInner,seedOuter;
	double seedScore;

	// List of nodes put into clusters
	List<List<SquareNode>> clusters;

	/**
	 * Configures chess board detector.
	 *
	 * @param numCols Number of columns in square grid
	 * @param numRows Number of rows in square grid
	 * @param maxCornerDistance Maximum distance in pixels that two "overlapping" corners can be from each other.
	 */
	public DetectChessSquarePoints(int numCols, int numRows, double maxCornerDistance,
								   BinaryPolygonConvexDetector<T> detectorSquare)
	{
		this.maxCornerDistanceSq = maxCornerDistance*maxCornerDistance;

		// number of black squares in rows/columns
		outerCols = numCols/2 + numCols%2;
		outerRows = numRows/2 + numRows%2;

		innerCols = numCols/2;
		innerRows = numRows/2;

		this.detectorSquare = detectorSquare;

		s2c = new SquaresIntoClusters(1.0,20);
		c2g = new ClustersIntoGrids(innerCols*innerRows);
	}

	/**
	 * Detects chessboard in the binary image.  Square corners must be disconnected.
	 * Returns true if a chessboard was found, false otherwise.
	 *
	 * @param input Original input image.
	 * @param binary Binary image of chessboard
	 * @return True if successful.
	 */
	public boolean process( T input , ImageUInt8 binary ) {
		boundPolygon.vertexes.reset();

		detectorSquare.process(input, binary);

		FastQueue<Polygon2D_F64> found = detectorSquare.getFound();

		clusters = s2c.process(found.toList());

		c2g.process(clusters);
		List<SquareGrid> grids = c2g.getGrids();

		// find inner and outer grids of squares
		SquareGrid inner = null;
		SquareGrid outer = null;

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);

			if( inner == null && ((g.columns == innerCols && g.rows == innerRows) ||
					(g.columns == innerRows && g.rows == innerCols))) {
				inner = g;
			} else if( outer == null && ((g.columns == outerCols && g.rows == outerRows) ||
					(g.columns == outerRows && g.rows == outerCols))) {
				outer = g;
			}
		}

		if( inner == null || outer == null ) {
			return false;
		}

		// make sure the rows/columns are correctly aligned
		if( inner.columns != innerCols ) {
			tools.transpose(inner);
		}

		if( outer.columns != outerCols ) {
			tools.transpose(outer);
		}

		// make sure the grids are in counter clockwise order
		if( tools.checkFlip(inner)) {
			tools.flipRows(inner);
		}
		if( tools.checkFlip(outer)) {
			tools.flipRows(outer);
		}

		// find a corner to align the two grids by
		tools.boundingPolygonCCW(inner, innerPolygon);
		selectZeroSeed(inner, outer, innerPolygon);
		// now align the two grids with adjacent zeros
		forceToZero(seedInner, inner);
		forceToZero(seedOuter, outer);

		// create one big grid for easier processing
		createUber(inner, outer, uberGrid);

		// put it into canonical order
		putIntoCanonical(uberGrid);
		orderUberCorners(uberGrid);

		// now extract the calibration points
		return computeCalibrationPoints(uberGrid);
	}

	/**
	 * Find all the corners which could be a valid 0 corner in the grid.  4 for square and 2 for rectangular grid
	 *
	 * grids must be in CCW order
	 */
	void selectZeroSeed(SquareGrid inner, SquareGrid outer, Polygon2D_F64 innerBounding) {
		if( outer.nodes.size() == 1 ) {
			seedInner = inner.get(0,0);
			seedOuter = outer.get(0,0);
		} else {
			seedScore = Double.MAX_VALUE;
			seedInner = seedOuter = null;
			if (outer.columns == outer.rows) {
				checkZeroSeed(0, outer, inner, innerBounding);
				checkZeroSeed(1, outer, inner, innerBounding);
				checkZeroSeed(2, outer, inner, innerBounding);
				checkZeroSeed(3, outer, inner, innerBounding);
			} else {
				checkZeroSeed(0, outer, inner, innerBounding);
				checkZeroSeed(2, outer, inner, innerBounding);
			}
		}
		if( seedInner == null )
			throw new RuntimeException("BUG!");
	}

	/**
	 * Looks to see if there is a good match to the specified corner in the inner grid.  If so it then
	 * sees if it beats the previously best zero seed pair
	 */
	void checkZeroSeed( int outerCorner ,
						SquareGrid outer ,
						SquareGrid inner , Polygon2D_F64 innerBounding ) {
		SquareNode outerN = outer.getCornerByIndex(outerCorner);
		Point2D_F64 c = outerN.center;

		double bestDistance = Double.MAX_VALUE;
		SquareNode best = null;

		// find the closest valid inner corner
		for (int i = 0; i < 4; i++) {
			// if inner is rectangular the only two can be seeds
			if( inner.columns != inner.rows ) {
				if( i == 1 || i ==3 )
					continue;
			}
			double d = innerBounding.get(i).distance2(c);

			if( d < bestDistance ) {
				best = inner.getCornerByIndex(i);
				bestDistance = d;
			}
		}

		Point2D_F64 nextCorner = outer.getCornerByIndex((outerCorner + 1)%4).center;
		boolean ccw;
		// special case with only two "corners" and the next corner isn't in the expected location
		if( nextCorner == c ) {
			nextCorner = outer.getCornerByIndex((outerCorner + 2)%4).center;
			ccw = isVectorsCCW(c, best.center, nextCorner);
		} else {
			ccw = isVectorsCCW(c,nextCorner,best.center);
		}

		if( ccw ) {
			if( seedScore > bestDistance ) {
				seedScore = bestDistance;
				seedInner = best;
				seedOuter = outerN;
			}
		}
	}

	/**
	 * Rotates or flips the grid until the specified node is the zero index node
	 */
	void forceToZero( SquareNode zero , SquareGrid grid) {

		int cornerIndex = grid.getCornerIndex(zero);

		if( cornerIndex != 0 ) {
			if( grid.rows != grid.columns ) {
				int corner = grid.getCornerIndex(zero);
				switch( corner ) {
					case 1:tools.flipColumns(grid);break;
					case 2:tools.reverse(grid);break;
					case 3:tools.flipRows(grid);break;
				}
			} else {
				for (int i = 0; i < cornerIndex; i++) {
					tools.rotateCCW(grid);
				}
			}
		}
		if( grid.get(0,0) != zero )
			throw new RuntimeException("BUG!");
	}

	/**
	 * Given the inner and outer grids create the "uber" grid.  Its a full chessboard pattern with null
	 * where there are no squares
	 *
	 */
	static void createUber(SquareGrid inner, SquareGrid outer, SquareGrid uber) {
		uber.columns = outer.columns + inner.columns;
		uber.rows = outer.rows + inner.rows;
		uber.nodes.clear();

		for (int row = 0; row < uber.rows; row++) {
			if( row % 2 == 0 ) {
				for (int col = 0; col < uber.columns; col++) {
					if( col % 2 == 0 )
						uber.nodes.add(outer.get(row/2,col/2));
					else
						uber.nodes.add(null);
				}
			} else {
				for (int col = 0; col < uber.columns; col++) {
					if( col % 2 == 1 )
						uber.nodes.add(inner.get(row/2,col/2));
					else
						uber.nodes.add(null);
				}
			}
		}
	}

	/**
	 * Checks to see if a->b and a->c is CCW
	 */
	public boolean isVectorsCCW(Point2D_F64 a, Point2D_F64 b, Point2D_F64 c ) {

		double angleAB = Math.atan2( b.y-a.y, b.x-a.x);
		double angleAC = Math.atan2( c.y-a.y, c.x-a.x);

		return UtilAngle.distanceCCW(angleAB, angleAC) < Math.PI/2;
	}



	/**
	 * Examines the uber grid and makes sure the 0 square is the closest one ot the top left corner.
	 * The current 0 node in uber is assumed to be a legit zero node and possible solution.  Any
	 * modification it makes to uber is also done to the inner and outer grids, which uber
	 * was derived from
	 */
	void putIntoCanonical( SquareGrid uber ) {

		boolean rowOdd = uber.rows%2 == 1;
		boolean colOdd = uber.columns%2 == 1;

		if( rowOdd == colOdd ) {
			// if odd and square then 4 solutions.  Otherwise just two solution that are on
			// opposite sides on the grid
			if( rowOdd && uber.rows == uber.columns ) {
				int best = -1;
				double bestDistance = Double.MAX_VALUE;
				for (int i = 0; i < 4; i++) {
					SquareNode n = uber.getCornerByIndex(i);
					double d = n.center.normSq();
					if( d < bestDistance ) {
						best = i;
						bestDistance = d;
					}
				}

				for (int i = 0; i < best; i++) {
					tools.rotateCCW(uber);
				}
			} else {
				double first = uber.get(0,0).center.normSq();
				double last = uber.getCornerByIndex(2).center.normSq();

				if( last < first ) {
					tools.reverse(uber);
				}
			}
		}
		// if only one is odd then there is a unique solution.  Since uber is already in a legit
		// configuration nothing needs ot be done
	}

	/**
	 * Adjust the corners in the square's polygon so that they are aligned along the grids overall
	 * length
	 *
	 * @return true if valid grid or false if not
	 */
	static boolean orderUberCorners(SquareGrid grid) {

		// the first pass interleaves every other row
		for (int row = 0; row < grid.rows; row++) {

			for (int col = row%2; col < grid.columns; col += 2) {
				SquareNode n = grid.get(row,col);

				boolean ordered = false;
				for (int diag = 0; diag < 4; diag++) {
					SquareNode d = getDiag(grid,row,col,diag);
					if( d != null ) {
						orderCorner(n,d.center,diag);
						ordered = true;
					}
				}

				if( !ordered )
					throw new IllegalArgumentException("BUG!");
			}
		}

		return true;
	}

	/**
	 * Ensures that the nodes in square are in CCW order and that the closest one to 'target' has
	 * the index 'diag'
	 */
	static void orderCorner( SquareNode node , Point2D_F64 target , int diag ) {

		// make sure it goes CCW
		if( !node.corners.isCCW() )
			node.corners.flip();

		// see which corner is the closest
		double closestDistance = Double.MAX_VALUE;
		int closest = -1;
		for (int i = 0; i < 4; i++) {
			double d = target.distance2(node.corners.get(i));
			if( d < closestDistance ) {
				closestDistance = d;
				closest = i;
			}
		}
		// rotate it until its at the specified diagonal
		int numRotate = diag-closest;
		if( numRotate < 0 )
			numRotate = 4 + numRotate;

		for (int i = 0; i < numRotate; i++) {
			UtilPolygons2D_F64.shiftDown(node.corners);
		}

	}

	/**
	 * Returns the node diagonal to the specified coordinate.  If it goes outside the grid then nul
	 * is returned
	 */
	static SquareNode getDiag( SquareGrid grid , int row , int col , int diag ) {
		int dx=0,dy=0;
		switch( diag ) {
			case 0: dx = -1; dy = -1; break;
			case 1: dx =  1; dy = -1; break;
			case 2: dx =  1; dy =  1; break;
			case 3: dx = -1; dy =  1; break;
		}

		int y = row + dy;
		int x = col + dx;

		if( y < 0 || y >= grid.rows )
			return null;
		if( x < 0 || x >= grid.columns )
			return null;
		return grid.get(y,x);
	}


	/**
	 * Extracts calibration points from the uber grid.  Calibration points are created from
	 * the inner corners in the chessboard.  Each point is computed from equivalent corners from
	 * two squares by averaging.
	 *
	 * Also checks to see if the two equivalent corners are close to each other.  If one is too far
	 * away false is returned.
	 */
	boolean computeCalibrationPoints(SquareGrid uber) {
		calibrationPoints.reset();

		for (int row = 1; row < uber.rows; row++) {
			for (int col = row%2; col < uber.columns; col += 2) {
				SquareNode n = uber.get(row, col);
				int left = col-1;
				int right = col+1;

				if( left >= 0 ) {
					Point2D_F64 a = uber.get(row-1,left).corners.get(2);
					Point2D_F64 b = n.corners.get(0);

					if( a.distance2(b) > maxCornerDistanceSq ) {
						return false;
					}

					Point2D_F64 p = calibrationPoints.grow();
					p.x = (a.x+b.x)/2.0;
					p.y = (a.y+b.y)/2.0;
				}
				if( right < uber.columns ) {
					Point2D_F64 a = uber.get(row-1,right).corners.get(3);
					Point2D_F64 b = n.corners.get(1);

					if( a.distance2(b) > maxCornerDistanceSq ) {
						return false;
					}

					Point2D_F64 p = calibrationPoints.grow();
					p.x = (a.x+b.x)/2.0;
					p.y = (a.y+b.y)/2.0;
				}
			}
		}

		return true;
	}

	public List<List<SquareNode>> getGraphs() {
		return clusters;
	}

	public SquaresIntoClusters getShapeToClusters() {
		return s2c;
	}

	public ClustersIntoGrids getGrids() {
		return c2g;
	}

	public BinaryPolygonConvexDetector<T> getDetectorSquare() {
		return detectorSquare;
	}

	public FastQueue<Point2D_F64> getCalibrationPoints() {
		return calibrationPoints;
	}

	public int getOuterRows() {
		return outerRows;
	}

	public int getOuterCols() {
		return outerCols;
	}

	public int getInnerRows() {
		return innerRows;
	}

	public int getInnerCols() {
		return innerCols;
	}
}
