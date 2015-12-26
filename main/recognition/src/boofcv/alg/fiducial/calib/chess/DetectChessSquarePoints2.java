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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.fiducial.calib.squares.*;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
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
public class DetectChessSquarePoints2<T extends ImageSingleBand> {

	// detector for squares
	BinaryPolygonDetector<T> detectorSquare;

	// Converts detected squares into a graph and into grids
	SquaresIntoCrossClusters s2c;
	CrossClustersIntoGrids c2g;

	// size of square grids
	private int numRows,numCols;

	// bounding quadrilateral
	private Polygon2D_I32 boundPolygon = new Polygon2D_I32();

	SquareGridTools tools = new SquareGridTools();

	FastQueue<Point2D_F64> calibrationPoints = new FastQueue<Point2D_F64>(Point2D_F64.class,true);

	// maximum distance two corners can be from each other
	double maxCornerDistanceSq;

	// List of nodes put into clusters
	List<List<SquareNode>> clusters;

	/**
	 * Configures chess board detector.
	 *
	 * @param numRows Number of rows in square grid
	 * @param numCols Number of columns in square grid
	 * @param maxCornerDistance Maximum distance in pixels that two "overlapping" corners can be from each other.
	 */
	public DetectChessSquarePoints2(int numRows, int numCols, double maxCornerDistance,
									BinaryPolygonDetector<T> detectorSquare)
	{
		this.maxCornerDistanceSq = maxCornerDistance*maxCornerDistance;

		this.numRows = numRows;
		this.numCols = numCols;

		this.detectorSquare = detectorSquare;

		s2c = new SquaresIntoCrossClusters(maxCornerDistance,0.5,-1);
		c2g = new CrossClustersIntoGrids();
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

		FastQueue<Polygon2D_F64> found = detectorSquare.getFoundPolygons();

		clusters = s2c.process(found.toList());

		c2g.process(clusters);
		List<SquareGrid> grids = c2g.getGrids().toList();

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid grid = grids.get(i);
			if( grid.rows == numCols && grid.columns == numRows ) {
				tools.transpose(grid);
			}
			if( grid.rows == numRows && grid.columns == numCols ) {
				// this detector requires that the (0,0) grid cell has a square inside of it
				if( grid.get(0,0) == null ){
					if( grid.get(0,grid.columns-1) != null ) {
						tools.flipColumns(grid);
					} else {
						continue;
					}
				}

				// If symmetric, ensure that the (0,0) is closest to top-left image corner
				putIntoCanonical(grid);

				// now extract the calibration points
				return computeCalibrationPoints(grid);
			}
		}

		return false;
	}

	/**
	 * Examines the grid and makes sure the (0,0) square is the closest one ot the top left corner.
	 * Only flip operations are allowed along symmetric axises
	 */
	void putIntoCanonical( SquareGrid grid ) {

		boolean rowOdd = grid.rows%2 == 1;
		boolean colOdd = grid.columns%2 == 1;

		if( colOdd ) {
			double d0 = grid.get(0,0).center.normSq();
			double d1 = grid.get(0,-1).center.normSq();
			if( d1 < d0 ) {
				tools.flipColumns(grid);
			}
		}

		if( rowOdd ) {
			double d0 = grid.get(0,0).center.normSq();
			double d1 = grid.get(-1,0).center.normSq();
			if( d1 < d0 ) {
				tools.flipRows(grid);
			}
		}
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
	 * Find inner corner points across the grid.  Start from the "top" row and work its way down.  Corners
	 * are found by finding the average point between two adjacent corners on adjacent squares.
	 */
	boolean computeCalibrationPoints(SquareGrid grid) {
		calibrationPoints.reset();

		for (int row = 0; row < grid.rows-1; row++) {
			int offset = row%2;
			for (int col = offset; col < grid.columns; col += 2) {
				SquareNode a = grid.get(row,col);

				if( col > 0 ) {
					SquareNode b = grid.get(row+1,col-1);
					if( !setIntersection(a,b,calibrationPoints.grow()))
						return false;
				}

				if( col < grid.columns-1) {
					SquareNode b = grid.get(row+1,col+1);
					if( !setIntersection(a,b,calibrationPoints.grow()))
						return false;
				}
			}
		}

		return true;
	}

	private boolean setIntersection( SquareNode a , SquareNode n , Point2D_F64 point ) {
		for (int i = 0; i < 4; i++) {
			SquareEdge edge = a.edges[i];
			if( edge != null && edge.destination(a) == n ) {
				Point2D_F64 p0 = edge.a.corners.get(edge.sideA);
				Point2D_F64 p1 = edge.b.corners.get(edge.sideB);

				point.x = (p0.x+p1.x)/2.0;
				point.y = (p0.y+p1.y)/2.0;
				return true;
			}
		}
		return false;
	}

	public List<List<SquareNode>> getGraphs() {
		return clusters;
	}

	public SquaresIntoCrossClusters getShapeToClusters() {
		return s2c;
	}

	public CrossClustersIntoGrids getGrids() {
		return c2g;
	}

	public BinaryPolygonDetector<T> getDetectorSquare() {
		return detectorSquare;
	}

	public FastQueue<Point2D_F64> getCalibrationPoints() {
		return calibrationPoints;
	}

	public int getNumRows() {
		return numRows;
	}

	public int getNumCols() {
		return numCols;
	}
}
