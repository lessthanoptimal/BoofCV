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

package boofcv.alg.feature.detect.squares;

import georegression.metric.Area2D_F64;
import georegression.metric.ClosestPoint2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for manipulating {@link SquareGrid}
 *
 * @author Peter Abeles
 */
public class SquareGridTools {

	/**
	 * There can be 2 or 4 possible orientations which are equally valid solutions.  For
	 * sake of consistency it will make the (0,0) coordinate be closest to the origin
	 * of the image coordinate system.
	 */
	public void putIntoCanonical( SquareGrid grid ) {
		if( grid.rows == grid.columns ) {
			int best = -1;
			double bestDistance = Double.MAX_VALUE;
			if( grid.get(0,0).center.normSq() < bestDistance ) {
				best = 0;
				bestDistance = grid.get(0,0).center.normSq();
			}
			if( grid.get(0, grid.columns  - 1).center.normSq() < bestDistance ) {
				best = 1;
				bestDistance = grid.get(0, grid.columns - 1).center.normSq();
			}
			if( grid.get(grid.rows - 1, grid.columns  - 1).center.normSq() < bestDistance ) {
				best = 2;
				bestDistance = grid.get(grid.rows - 1, grid.columns -1).center.normSq();
			}
			if( grid.get(grid.rows - 1,0).center.normSq() < bestDistance ) {
				best = 3;
			}
			for (int i = 0; i < best; i++) {
				rotateCCW(grid);
			}
		} else {
			double first = grid.get(0,0).center.normSq();
			double last = grid.get(grid.rows - 1, grid.columns -1).center.normSq();

			if( last < first ) {
				reverse(grid);
			}
		}
	}

	public void rotateCCW(SquareGrid grid) {
		tmp.clear();
		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				tmp.add(grid.get(col, grid.columns - row - 1));
			}
		}
		grid.nodes.clear();
		grid.nodes.addAll(tmp);
	}

	public void reverse(SquareGrid grid) {
		tmp.clear();
		int N = grid.columns*grid.rows;
		for (int i = 0; i < N; i++) {
			tmp.add( grid.nodes.get(N-i-1));
		}
		grid.nodes.clear();
		grid.nodes.addAll(tmp);
	}

	/**
	 * Checks to see if it needs to be flipped.  Flipping is required if X and Y axis in 2D grid
	 * are not CCW.
	 */
	public boolean checkFlip( SquareGrid grid ) {
		if( grid.columns == 1 || grid.rows == 1 )
			return false;

		Point2D_F64 a = grid.get(0,0).center;
		Point2D_F64 b = grid.get(0,grid.columns-1).center;
		Point2D_F64 c = grid.get(grid.rows-1,0).center;

		double angleAB = Math.atan2( b.y-a.y, b.x-a.x);
		double angleAC = Math.atan2( c.y-a.y, c.x-a.x);

		return UtilAngle.distanceCCW(angleAB, angleAC) > Math.PI * 0.75;
	}

	/**
	 * Compute the visual size of a polygon
	 */
	Polygon2D_F64 poly = new Polygon2D_F64(4);
	public double computeSize( SquareGrid grid ) {
		poly.vertexes.data[0] = grid.get(0,0).center;
		poly.vertexes.data[1] = grid.get(0,grid.columns-1).center;
		poly.vertexes.data[2] = grid.get(grid.rows-1,grid.columns-1).center;
		poly.vertexes.data[3] = grid.get(grid.rows-1,0).center;

		return Area2D_F64.polygonConvex(poly);
	}

	List<SquareNode> tmp = new ArrayList<SquareNode>();
	/**
	 * Transposes the grid
	 */
	public void transpose( SquareGrid grid ) {
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
	public void flipRows( SquareGrid grid ) {
		tmp.clear();

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				tmp.add( grid.nodes.get( (grid.rows - row - 1)*grid.columns + col));
			}
		}

		grid.nodes.clear();
		grid.nodes.addAll(tmp);
	}

	/**
	 * Get outside corner polygon around the grid
	 */
	public void boundingPolygon( SquareGrid grid , Polygon2D_F64 bounding ) {
		int w = grid.columns;
		int h = grid.rows;

		selectAxis(grid, 0,0);
		sortCorners(grid.get(0, 0).corners);
		bounding.get(0).set(sorted[0]);

		selectAxis(grid, 0,w-1);
		sortCorners(grid.get(0, w-1).corners);
		bounding.get(1).set(sorted[1]);

		selectAxis(grid, h-1,w-1);
		sortCorners(grid.get(h-1, w-1).corners);
		bounding.get(2).set(sorted[2]);

		selectAxis(grid, h-1,0);
		sortCorners(grid.get(h-1, 0).corners);
		bounding.get(3).set(sorted[3]);
	}

	// local storage for extractCalibrationPoints
	LineParametric2D_F64 axisX = new LineParametric2D_F64();
	LineParametric2D_F64 axisY = new LineParametric2D_F64();
	Point2D_F64 sorted[] = new Point2D_F64[4];

	/**
	 * Adjust the corners in the square's polygon so that they are aligned along the grids overall
	 * length
	 *
	 * @return true if valid grid or false if not
	 */
	public boolean orderSquareCorners( SquareGrid grid ) {

		// the first pass interleaves every other row
		for (int row = 0; row < grid.rows; row++) {

			for (int col = 0; col < grid.columns; col++) {
				selectAxis(grid, row, col);

				Polygon2D_F64 square = grid.get(row,col).corners;
				sortCorners(square);

				for (int i = 0; i < 4; i++) {
					if( sorted[i] == null)
					{
						return false;
					} else {
						square.vertexes.data[i] = sorted[i];
					}
				}

			}
		}

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
		axisX.slope.set(dx, dy);
		axisY.slope.set(-dy, dx);
	}
}
