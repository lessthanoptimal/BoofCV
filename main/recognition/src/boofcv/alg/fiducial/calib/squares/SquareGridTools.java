/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.squares;

import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Area2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
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
			for (int i = 0; i < 4; i++) {
				SquareNode n = grid.getCornerByIndex(i);
				double d = n.center.normSq();
				if( d < bestDistance ) {
					best = i;
					bestDistance = d;
				}
			}

			for (int i = 0; i < best; i++) {
				rotateCCW(grid);
			}
		} else {
			double first = grid.get(0,0).center.normSq();
			double last = grid.getCornerByIndex(2).center.normSq();

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

		double x0 = b.x-a.x;
		double y0 = b.y-a.y;

		double x1 = c.x-a.x;
		double y1 = c.y-a.y;

		double z = x0 * y1 - y0 * x1;

		return z < 0;
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

		return Area2D_F64.polygonSimple(poly);
	}

	List<SquareNode> tmp = new ArrayList<>();
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
	 * Flips the order of columns
	 */
	public void flipColumns( SquareGrid grid ) {
		tmp.clear();

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				tmp.add( grid.get(row,grid.columns-col-1));
			}
		}

		grid.nodes.clear();
		grid.nodes.addAll(tmp);
	}

	/**
	 * Get outside corner polygon around the grid.  The grid is assumed to be in CCW orientation.
	 */
	public void boundingPolygonCCW(SquareGrid grid, Polygon2D_F64 bounding) {
		int w = grid.columns;
		int h = grid.rows;

		if( w == 1 && h == 1 ) {
			SquareNode n = grid.get(0,0);
			bounding.get(0).set(n.corners.get(0));
			bounding.get(1).set(n.corners.get(1));
			bounding.get(2).set(n.corners.get(2));
			bounding.get(3).set(n.corners.get(3));
		} else if( w == 1 ) {
			orderNode(grid.get(0, 0), grid.get(h - 1, 0), false);
			bounding.get(0).set(ordered[0]);
			bounding.get(1).set(ordered[1]);
			orderNode(grid.get(h - 1, 0), grid.get(0, 0), false);
			bounding.get(2).set(ordered[0]);
			bounding.get(3).set(ordered[1]);
		} else if( h == 1 ) {
			orderNode(grid.get(0, 0), grid.get(0, w - 1), true);
			bounding.get(0).set(ordered[0]);
			bounding.get(3).set(ordered[3]);
			orderNode(grid.get(0, w - 1), grid.get(0, 0), true);
			bounding.get(1).set(ordered[3]);
			bounding.get(2).set(ordered[0]);
		} else {
			orderNode(grid.get(0, 0), grid.get(0, w - 1), true);
			bounding.get(0).set(ordered[0]);
			orderNode(grid.get(0, w - 1), grid.get(h - 1, w - 1), true);
			bounding.get(1).set(ordered[0]);
			orderNode(grid.get(h - 1, w - 1), grid.get(h - 1, 0), true);
			bounding.get(2).set(ordered[0]);
			orderNode(grid.get(h - 1, 0), grid.get(0, 0), true);
			bounding.get(3).set(ordered[0]);
		}
	}

	/**
	 * Given the grid coordinate, order the corners for the node at that location.  Takes in handles situations
	 * where there are no neighbors.
	 */
	protected void orderNodeGrid(SquareGrid grid, int row, int col) {
		SquareNode node = grid.get(row,col);

		if(grid.rows==1 && grid.columns==1 ) {
			for (int i = 0; i < 4; i++) {
				ordered[i] = node.corners.get(i);
			}
		} else if( grid.columns==1 ) {
			if (row == grid.rows - 1) {
				orderNode(node, grid.get(row - 1, col), false);
				rotateTwiceOrdered();
			} else {
				orderNode(node, grid.get(row + 1, col), false);
			}
		} else {
			if( col == grid.columns-1) {
				orderNode(node, grid.get(row, col-1), true);
				rotateTwiceOrdered();
			} else {
				orderNode(node, grid.get(row, col+1), true);
			}
		}
	}

	/**
	 * Reorders the list by the equivalent of two rotations
	 */
	private void rotateTwiceOrdered() {
		Point2D_F64 a = ordered[0];
		Point2D_F64 b = ordered[1];
		Point2D_F64 c = ordered[2];
		Point2D_F64 d = ordered[3];

		ordered[0] = c;
		ordered[1] = d;
		ordered[2] = a;
		ordered[3] = b;
	}

	LineSegment2D_F64 lineCenters = new LineSegment2D_F64();
	LineSegment2D_F64 lineSide = new LineSegment2D_F64();
	Point2D_F64 dummy = new Point2D_F64();

	LineGeneral2D_F64 general = new LineGeneral2D_F64();
	Point2D_F64 ordered[] = new Point2D_F64[4];

	/**
	 * Fills the ordered list with the corners in target node in canonical order.
	 *
	 * @param pointingX true if 'node' is pointing along the x-axis from target.  false for point along y-axis
	 */
	protected void orderNode(SquareNode target, SquareNode node, boolean pointingX) {

		int index0 = findIntersection(target,node);
		int index1 = (index0+1)%4;

		int index2 = (index0+2)%4;
		int index3 = (index0+3)%4;

		if( index0 < 0 )
			throw new RuntimeException("Couldn't find intersection.  Probable bug");

		lineCenters.a = target.center;
		lineCenters.b = node.center;
		UtilLine2D_F64.convert(lineCenters,general);

		Polygon2D_F64 poly = target.corners;
		if( pointingX ) {
			if (sign(general, poly.get(index0)) > 0) {
				ordered[1] = poly.get(index1);
				ordered[2] = poly.get(index0);
			} else {
				ordered[1] = poly.get(index0);
				ordered[2] = poly.get(index1);
			}
			if (sign(general, poly.get(index2)) > 0) {
				ordered[3] = poly.get(index2);
				ordered[0] = poly.get(index3);
			} else {
				ordered[3] = poly.get(index3);
				ordered[0] = poly.get(index2);
			}
		} else {
			if (sign(general, poly.get(index0)) > 0) {
				ordered[2] = poly.get(index1);
				ordered[3] = poly.get(index0);
			} else {
				ordered[2] = poly.get(index0);
				ordered[3] = poly.get(index1);
			}
			if (sign(general, poly.get(index2)) > 0) {
				ordered[0] = poly.get(index2);
				ordered[1] = poly.get(index3);
			} else {
				ordered[0] = poly.get(index3);
				ordered[1] = poly.get(index2);
			}
		}
	}

	/**
	 * Finds the side which intersects the line segment from the center of target to center of node
	 */
	protected int findIntersection( SquareNode target , SquareNode node ) {
		lineCenters.a = target.center;
		lineCenters.b = node.center;

		for (int i = 0; i < 4; i++) {
			int j = (i+1)%4;

			lineSide.a = target.corners.get(i);
			lineSide.b = target.corners.get(j);

			if(Intersection2D_F64.intersection(lineCenters,lineSide,dummy) != null ) {
				return i;
			}
		}
		return -1;
	}


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
				orderNodeGrid(grid, row, col);
				Polygon2D_F64 square = grid.get(row,col).corners;

				for (int i = 0; i < 4; i++) {
					square.vertexes.data[i] = ordered[i];
				}
			}
		}

		return true;
	}

	public static int sign( LineGeneral2D_F64 line , Point2D_F64 p ) {
		double val = line.A*p.x + line.B*p.y + line.C;
		if( val > 0 )
			return 1;
		if( val < 0 )
			return -1;
		return 0;
	}

}
