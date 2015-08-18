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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.feature.detect.squares.ClustersIntoGrids;
import boofcv.alg.feature.detect.squares.SquareGrid;
import boofcv.alg.feature.detect.squares.SquareNode;
import boofcv.alg.feature.detect.squares.SquaresIntoClusters;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.struct.image.ImageSingleBand;
import georegression.metric.Area2D_F64;
import georegression.metric.ClosestPoint2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Detect a square grid calibration target and returns the corner points of each square.  This calibration grid is
 * specified by a set of squares which are organized in a grid pattern.  All squares are the same size.  The entire
 * grid must be visible.  Space between the squares is specified as a ratio of the square size. The grid will be
 * oriented so that returned points are in counter clockwise (CCW) ordering, which appears to be CW in the image.
 *
 * There is also always at least two solutions to the ordering.  For sake of consistency it will select
 * the orientation where index 0 is the closest to the origin.
 *
 * @author Peter Abeles
 */
// TODO tell the polygon detector that there should be no inner contour
public class DetectSquareGridFiducial<T extends ImageSingleBand> {

	// dimension of square grid.  This only refers to black squares and not the white space
	int numCols;
	int numRows;

	// detector for squares
	BinaryPolygonConvexDetector<T> detectorSquare;

	// Converts detected squares into a graph and into grids
	SquaresIntoClusters s2c;
	ClustersIntoGrids c2g;

	// output results.  Grid of calibration points in row-major order
	List<Point2D_F64> calibrationPoints = new ArrayList<Point2D_F64>();
	int calibRows;
	int calibCols;

	/**
	 * COnfigures the detector
	 *
	 * @param numRows Number of black squares in the grid rows
	 * @param numCols Number of black squares in the grid columns
	 * @param spaceToSquareRatio Ratio of spacing between the squares and the squares width
	 * @param detectorSquare Detects the squares in the image.  Must be configured to detect squares
	 */
	public DetectSquareGridFiducial(int numRows, int numCols, double spaceToSquareRatio,
									BinaryPolygonConvexDetector<T> detectorSquare) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.detectorSquare = detectorSquare;

		s2c = new SquaresIntoClusters(spaceToSquareRatio);
		c2g = new ClustersIntoGrids(numCols*numRows);
	}

	/**
	 * Process the image and detect the calibration target
	 *
	 * @param image Input image
	 * @return true if a calibration target was found and false if not
	 */
	public boolean process( T image ) {
		detectorSquare.process(image);

		FastQueue<Polygon2D_F64> found = detectorSquare.getFound();

		List<List<SquareNode>> clusters = s2c.process(found.toList());
		c2g.process(clusters);
		List<SquareGrid> grids = c2g.getGrids();

		SquareGrid match = null;
		double matchSize = 0;
		for( SquareGrid g : grids ) {
			if (g.columns != numCols || g.rows != numRows) {
				if( g.columns == numRows && g.rows == numCols ) {
					transpose(g);
				} else {
					continue;
				}
			}

			double size = computeSize(g);
			if( size > matchSize ) {
				matchSize = size;
				match = g;
			}
		}

		if( match != null ) {
			if( checkFlip(match) ) {
				flipRows(match);
			}
			if( extractCalibrationPoints(match) ) {
				resolveAmbiguity();
				return true;
			}
		}
		return false;
	}

	/**
	 * There can be 2 or 4 possible orientations which are equally valid solutions.  For
	 * sake of consistency it will make the (0,0) coordinate be closest to the origin
	 * of the image coordinate system.
	 */
	void resolveAmbiguity() {
		if( calibCols == calibRows ) {
			int best = -1;
			double bestDistance = Double.MAX_VALUE;
			if( getCalib(0,0).normSq() < bestDistance ) {
				best = 0;
				bestDistance = getCalib(0,0).normSq();
			}
			if( getCalib(0,calibCols-1).normSq() < bestDistance ) {
				best = 1;
				bestDistance = getCalib(0,calibCols-1).normSq();
			}
			if( getCalib(calibRows-1,calibCols-1).normSq() < bestDistance ) {
				best = 2;
				bestDistance = getCalib(calibRows-1,calibCols-1).normSq();
			}
			if( getCalib(calibRows-1,0).normSq() < bestDistance ) {
				best = 3;
			}
			for (int i = 0; i < best; i++) {
				rotateCalibSquareCCW();
			}
		} else {
			int N = calibrationPoints.size();
			double first = calibrationPoints.get(0).normSq();
			double last = calibrationPoints.get(N-1).normSq();

			if( last < first ) {
				reverseCalib();
			}
		}
	}

	private Point2D_F64 getCalib( int row , int col ) {
		return calibrationPoints.get( row*calibCols + col );
	}

	void rotateCalibSquareCCW() {
		tmpPts.clear();
		for (int row = 0; row < calibRows; row++) {
			for (int col = 0; col < calibCols; col++) {
				tmpPts.add(getCalib(col,calibCols-row-1));
			}
		}
		calibrationPoints.clear();
		calibrationPoints.addAll(tmpPts);
	}

	void reverseCalib() {
		tmpPts.clear();
		int N = calibCols*calibRows;
		for (int i = 0; i < N; i++) {
			tmpPts.add( calibrationPoints.get(N-i-1));
		}
		calibrationPoints.clear();
		calibrationPoints.addAll(tmpPts);
	}


	/**
	 * Compute the visual size of a polygon
	 */
	Polygon2D_F64 poly = new Polygon2D_F64(4);
	double computeSize( SquareGrid grid ) {
		poly.vertexes.data[0] = grid.get(0,0).center;
		poly.vertexes.data[1] = grid.get(0,grid.columns-1).center;
		poly.vertexes.data[2] = grid.get(grid.rows-1,grid.columns-1).center;
		poly.vertexes.data[3] = grid.get(grid.rows-1,0).center;

		return Area2D_F64.polygonConvex(poly);
	}

	/**
	 * Checks to see if it needs to be flipped.  Flipping is required if X and Y axis in 2D grid
	 * are not CCW.
	 */
	boolean checkFlip( SquareGrid grid ) {
		if( grid.columns == 1 || grid.rows == 1 )
			return false;

		Point2D_F64 a = grid.get(0,0).center;
		Point2D_F64 b = grid.get(0,grid.columns-1).center;
		Point2D_F64 c = grid.get(grid.rows-1,0).center;

		double angleAB = Math.atan2( b.y-a.y, b.x-a.x);
		double angleAC = Math.atan2( c.y-a.y, c.x-a.x);

		return UtilAngle.distanceCCW(angleAB, angleAC) > Math.PI * 0.75;
	}

	List<SquareNode> tmp = new ArrayList<SquareNode>();
	List<Point2D_F64> tmpPts = new ArrayList<Point2D_F64>();
	/**
	 * Transposes the grid
	 */
	void transpose( SquareGrid grid ) {
		tmp.clear();

		for (int col = 0; col < grid.columns; col++) {
			for (int row = 0; row < grid.rows; row++) {
				tmp.add(grid.get(row, col));
			}
		}

		grid.nodes.clear();
		grid.nodes.addAll(tmp);

		int a = grid.columns;
		grid.columns = grid.rows;
		grid.rows = a;
	}

	/**
	 * Flips the order of rows
	 */
	void flipRows( SquareGrid grid ) {
		tmp.clear();

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				tmp.add( grid.nodes.get( (grid.rows - row - 1)*grid.columns + col));
			}
		}

		grid.nodes.clear();
		grid.nodes.addAll(tmp);
	}

	// local storage for extractCalibrationPoints
	LineParametric2D_F64 axisX = new LineParametric2D_F64();
	LineParametric2D_F64 axisY = new LineParametric2D_F64();
	Point2D_F64 sorted[] = new Point2D_F64[4];
	List<Point2D_F64> row0 = new ArrayList<Point2D_F64>();
	List<Point2D_F64> row1 = new ArrayList<Point2D_F64>();

	/**
	 * Converts the grid into a list of calibration points.  Uses a local axis around the square
	 * to figure out the order.  The local axis is computed from the center of the square in question and
	 * it's adjacent squares.
	 *
	 * @return true if valid grid or false if not
	 */
	boolean extractCalibrationPoints( SquareGrid grid ) {

		calibrationPoints.clear();
		// the first pass interleaves every other row
		for (int row = 0; row < grid.rows; row++) {

			row0.clear();
			row1.clear();
			for (int col = 0; col < grid.columns; col++) {
				selectAxis(grid, row, col);

				Polygon2D_F64 square = grid.get(row,col).corners;
				sortCorners(square);

				for (int i = 0; i < 4; i++) {if( sorted[i] == null) {return false;}}
				row0.add(sorted[0]);
				row0.add(sorted[1]);
				row1.add(sorted[3]);
				row1.add(sorted[2]);

			}
			calibrationPoints.addAll(row0);
			calibrationPoints.addAll(row1);
		}

		calibCols = grid.columns*2;
		calibRows = grid.rows*2;
		return true;
	}

	/**
	 * Puts the corners into a specified order so that it can be placed into the grid.
	 * Uses local coordiant systems defined buy axisX and axisY.
	 */
	void sortCorners(Polygon2D_F64 square) {
		for (int i = 0; i < 4; i++) {
			sorted[i] = null;
		}
		for (int i = 0; i < 4; i++) {
			Point2D_F64 p = square.get(i);
			double coorX = ClosestPoint2D_F64.closestPointT(axisX, p);
			double coorY = ClosestPoint2D_F64.closestPointT(axisY, p);

			if( coorX < 0 ) {
				if( coorY < 0 ) {
					sorted[0] = p;
				} else {
					sorted[3] = p;
				}
			} else {
				if( coorY < 0 ) {
					sorted[1] = p;
				} else {
					sorted[2] = p;
				}
			}
		}
	}

	/**
	 * Select the local X and Y axis around the specified grid element.
	 */
	void selectAxis( SquareGrid grid, int row , int col ) {

		Point2D_F64 a = grid.get(row,col).center;
		axisX.p.set(a);
		axisY.p.set(a);

		double dx,dy;

		if( grid.columns == 1 && grid.rows == 1 ) {
			// just pick an axis arbitrarily from the corner points
			Polygon2D_F64 square = grid.nodes.get(0).corners;
			double px = (square.get(0).x+square.get(1).x)/2.0;
			double py = (square.get(0).y+square.get(1).y)/2.0;

			dx = px-a.x;
			dy = py-a.y;
		} else if( grid.columns == 1 ) {
			// find slope of y-axis
			double fx,fy;
			if( row == grid.rows-1 ) {
				Point2D_F64 b = grid.get(row-1,col).center;
				fx = a.x-b.x;
				fy = a.y-b.y;
			} else {
				Point2D_F64 b = grid.get(row+1,col).center;
				fx = b.x-a.x;
				fy = b.y-a.y;
			}
			// convert into x-axis slope
			dx = -fy;
			dy = fx;
		} else {
			if( col == grid.columns-1 ) {
				Point2D_F64 b = grid.get(row,col-1).center;
				dx = a.x-b.x;
				dy = a.y-b.y;
			} else {
				Point2D_F64 b = grid.get(row,col+1).center;
				dx = b.x-a.x;
				dy = b.y-a.y;
			}
		}
		// Y axis will be CCW of x-axis.  Which appears to be CW on the screen
		axisX.slope.set(dx,dy);
		axisY.slope.set(-dy,dx);
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return calibrationPoints;
	}

	public int getCalibrationRows() {
		return calibRows;
	}

	public int getCalibrationCols() {
		return calibCols;
	}
}
