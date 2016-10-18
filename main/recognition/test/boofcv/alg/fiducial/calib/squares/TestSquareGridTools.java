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

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSquareGridTools {
	@Test
	public void computeSize() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);

		double w = TestSquareRegularClustersIntoGrids.DEFAULT_WIDTH;
		double expected = (2*w*(3-1))*(2*w*(2-1)); // see how the grid is constructed

		assertEquals(expected, alg.computeSize(grid), 1e-8);
	}

	@Test
	public void putIntoCanonical_square() {
		SquareGridTools alg = new SquareGridTools();

		// grids of different sizes.  smallest is 4x4 since that's one square
		for (int i = 4; i <= 8; i++) {
			// try different initial orientations
			for (int j = 0; j < 4; j++) {
				SquareGrid grid = createGrid(i,i);
				for (int k = 0; k < j; k++) {
					alg.rotateCCW(grid);
				}
				alg.putIntoCanonical(grid);
				assertTrue(i+" "+j,grid.get(0,0).center.norm() < 1e-8);
			}
		}
	}

	@Test
	public void putIntoCanonical_rectangle() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,5);
		alg.putIntoCanonical(grid);
		assertTrue(grid.get(0,0).center.norm() < 1e-8);

		alg.reverse(grid);
		alg.putIntoCanonical(grid);
		assertTrue(grid.get(0,0).center.norm() < 1e-8);
	}

	private List<Point2D_F64> createPoints( int rows , int cols ) {
		double x0 = 10;
		double y0 = 20;

		List<Point2D_F64> list = new ArrayList<>();

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				list.add( new Point2D_F64(x0+i,y0+j));
			}
		}

		return list;
	}

	@Test
	public void checkFlip() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);
		assertFalse(alg.checkFlip(grid));

		flipColumns(grid);
		assertTrue(alg.checkFlip(grid));

		alg.flipRows(grid);
		assertFalse(alg.checkFlip(grid));
	}

	void flipColumns( SquareGrid grid ) {
		List<SquareNode> tmp = new ArrayList<>();

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				tmp.add( grid.nodes.get( row*grid.columns + (grid.columns - col - 1)));
			}
		}

		grid.nodes.clear();
		grid.nodes.addAll(tmp);
	}

	@Test
	public void transpose() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);
		SquareGrid orig = copy(grid);

		alg.transpose(grid);

		assertEquals(orig.columns, grid.rows);
		assertEquals(orig.rows, grid.columns);

		for (int row = 0; row < orig.rows; row++) {
			for (int col = 0; col < orig.columns; col++) {
				assertTrue(orig.get(row,col)==grid.get(col,row));
			}
		}
	}

	@Test
	public void flipRows() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);
		SquareGrid orig = copy(grid);

		alg.flipRows(grid);

		for (int row = 0; row < orig.rows; row++) {
			for (int col = 0; col < orig.columns; col++) {
				assertTrue(orig.get(row, col) == grid.get(orig.rows - row - 1, col));
			}
		}
	}

		@Test
	public void flipColumns() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);
		SquareGrid orig = copy(grid);

		alg.flipColumns(grid);

		for (int row = 0; row < orig.rows; row++) {
			for (int col = 0; col < orig.columns; col++) {
				assertTrue(orig.get(row, col) == grid.get(row, orig.columns - col - 1));
			}
		}
	}

	@Test
	public void boundingPolygonCCW_rect() {
		SquareGridTools alg = new SquareGridTools();

		double w = TestSquareRegularClustersIntoGrids.DEFAULT_WIDTH;

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		for (int rows = 2; rows <= 4; rows++) {
			for (int cols = 2; cols <= 4; cols++) {
				SquareGrid grid = createGrid(rows,cols);

				for (int i = 0; i < 2; i++) {
					if( i == 1 )
						alg.transpose(grid);

					// ensure preconditions are meet
					if( alg.checkFlip(grid)) {
						alg.flipRows(grid);
					}
					alg.boundingPolygonCCW(grid, poly);

					double x0 = -w/2;
					double y0 = -w/2;

					double x1 = w*2*(cols-1) + w/2;
					double y1 = w*2*(rows-1) + w/2;

					Polygon2D_F64 expected = new Polygon2D_F64(4);
					expected.get(0).set(x0 ,y0);
					expected.get(1).set(x1 ,y0);
					expected.get(2).set(x1 ,y1);
					expected.get(3).set(x0 ,y1);

					assertTrue(UtilPolygons2D_F64.isEquivalent(expected,poly,1e-8));
				}

			}
		}
	}

	@Test
	public void boundingPolygonCCW_column() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(3,1);

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		alg.boundingPolygonCCW(grid, poly);

		double w = TestSquareRegularClustersIntoGrids.DEFAULT_WIDTH;
		assertTrue(poly.get(0).distance(- w/2,  - w/2) <= 1e-8);
		assertTrue(poly.get(1).distance(  w/2 , - w/2) <= 1e-8);
		assertTrue(poly.get(2).distance(  w/2 , w*4 + w / 2) <= 1e-8);
		assertTrue(poly.get(3).distance(- w/2 , w*4 + w / 2) <= 1e-8);
	}

	@Test
	public void boundingPolygonCCW_row() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(1,3);

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		alg.boundingPolygonCCW(grid, poly);

		double w = TestSquareRegularClustersIntoGrids.DEFAULT_WIDTH;
		assertTrue(poly.get(0).distance( -w/2      , -w/2) <= 1e-8);
		assertTrue(poly.get(1).distance( w*4 + w/2 , -w/2) <= 1e-8);
		assertTrue(poly.get(2).distance( w*4 + w/2 ,  w/2) <= 1e-8);
		assertTrue(poly.get(3).distance( -w/2      ,  w/2) <= 1e-8);
	}

	@Test
	public void extractCalibrationPoints() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);

		// shuffle the ordering some
		for (int i = 0; i < grid.nodes.size(); i++) {
			SquareNode n = grid.nodes.get(i);

			if( i%2 == 0 ) {
				UtilPolygons2D_F64.shiftDown(n.corners);
			}
		}

		assertTrue(alg.orderSquareCorners(grid));

		double w = TestSquareRegularClustersIntoGrids.DEFAULT_WIDTH;
		double x0 = -w/2;
		double y0 = -w/2;

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				SquareNode n = grid.get(row,col);

				double x = x0 + 2*col*w;
				double y = y0 + 2*row*w;

				assertTrue(n.corners.get(0).distance(new Point2D_F64(x  , y)) < 1e-8);
				assertTrue(n.corners.get(1).distance(new Point2D_F64(x+w, y)) < 1e-8);
				assertTrue(n.corners.get(2).distance(new Point2D_F64(x+w, y+w)) < 1e-8);
				assertTrue(n.corners.get(3).distance(new Point2D_F64(x  , y+w)) < 1e-8);
			}
		}
	}

	/**
	 * Exhaustively checks all situations it might encounter
	 */
	@Test
	public void orderNodeGrid() {
		SquareGridTools alg = new SquareGridTools();

		for( int rows = 1; rows <= 4; rows++ ) {
			for (int cols = 1; cols <= 4; cols++) {
				if( rows == 1 && cols == 1 )
					continue;

				SquareGrid grid = createGrid(rows, cols);

//				System.out.println("grid shape "+rows+" "+cols);

				for (int flip = 0; flip < 2; flip++) {
					for (int rotate = 0; rotate < 4; rotate++) {

//						System.out.println("  flip "+flip+" rotate "+rotate);
						for (int i = 0; i < rows; i++) {
							for (int j = 0; j < cols; j++) {
//								System.out.println("    element "+i+" "+j);
								alg.orderNodeGrid(grid, i, j);
								check_orderNodeGrid(alg.ordered,grid.get(i,j).center);
							}
						}
						for (int i = 0; i < rows; i++) {
							for (int j = 0; j < cols; j++) {
								UtilPolygons2D_F64.shiftDown(grid.get(i,j).corners);
							}
						}
					}
					for (int i = 0; i < rows; i++) {
						for (int j = 0; j < cols; j++) {
							UtilPolygons2D_F64.flip(grid.get(i, j).corners);
						}
					}
				}

			}
		}
	}

	public void check_orderNodeGrid(Point2D_F64 ordered[] , Point2D_F64 center) {

		Point2D_F64 c = center;

		assertTrue(ordered[0].x-c.x < 0 );
		assertTrue(ordered[0].y - c.y < 0);

		assertTrue(ordered[1].x - c.x > 0);
		assertTrue(ordered[1].y - c.y < 0);

		assertTrue(ordered[2].x - c.x > 0);
		assertTrue(ordered[2].y - c.y > 0);

		assertTrue(ordered[3].x-c.x < 0 );
		assertTrue(ordered[3].y-c.y > 0 );
	}

	@Test
	public void orderNode() {
		SquareNode target = new SquareNode();
		target.corners = new Polygon2D_F64(4);
		target.corners.get(0).set(-1,-1);
		target.corners.get(1).set( 1,-1);
		target.corners.get(2).set( 1, 1);
		target.corners.get(3).set(-1, 1);

		SquareNode up = new SquareNode();    up.center.set(0, 5);
		SquareNode down = new SquareNode();  down.center.set(0,-5);
		SquareNode left = new SquareNode();  left.center.set(-5, 0);
		SquareNode right = new SquareNode(); right.center.set( 5,0);

		SquareGridTools alg = new SquareGridTools();

		// try different orders and clockwiseness
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 4; j++) {
//				System.out.println("i = " + i + " j = " + j);
				alg.orderNode(target, right,true);
				checkOrder(alg.ordered, -1,-1,  1,-1,  1,1,  -1,1);
				alg.orderNode(target, left, true);
				checkOrder(alg.ordered, 1,1,  -1,1,  -1,-1,  1,-1);

				alg.orderNode(target, up,false);
				checkOrder(alg.ordered, -1,-1,  1,-1,  1,1,  -1,1);
				alg.orderNode(target, down,false);
				checkOrder(alg.ordered, 1,1,  -1,1,  -1,-1,  1,-1);

				UtilPolygons2D_F64.shiftDown(target.corners);
			}
			UtilPolygons2D_F64.flip(target.corners);
		}
	}

	private void checkOrder( Point2D_F64 ordered[] , double ...expected ) {
		for (int i = 0; i < 4; i++) {
			double expectedX = expected[i*2];
			double expectedY = expected[i*2+1];

			assertEquals(expectedX,ordered[i].x,1e-8);
			assertEquals(expectedY,ordered[i].y,1e-8);
		}
	}

	@Test
	public void findIntersection() {
		SquareGrid grid = createGrid(3,3);

		SquareNode center = grid.get(1, 1);

		SquareGridTools alg = new SquareGridTools();

		assertEquals(closestSide(center,grid.get(1,2).center),alg.findIntersection(center,grid.get(1, 2)));
		assertEquals(closestSide(center,grid.get(1,0).center),alg.findIntersection(center, grid.get(1, 0)));
		assertEquals(closestSide(center,grid.get(2,1).center),alg.findIntersection(center,grid.get(2, 1)));
		assertEquals(closestSide(center,grid.get(0,1).center),alg.findIntersection(center,grid.get(0, 1)));
	}

	private int closestSide( SquareNode node , Point2D_F64 point ) {
		int best = -1;
		double bestDistance = Double.MAX_VALUE;

		for (int i = 0; i < 4; i++) {
			int j = (i+1)%4;

			double distI = node.corners.get(i).distance(point);
			double distJ = node.corners.get(j).distance(point);

			double distance = distI+distJ;
			if( distance < bestDistance ) {
				bestDistance = distance;
				best = i;
			}
		}

		return best;
	}

	public static SquareGrid createGrid( int numRows , int numCols ) {
		SquareGrid grid = new SquareGrid();
		grid.nodes =  TestSquareRegularClustersIntoGrids.createGrid(numRows,numCols);
		grid.columns = numCols;
		grid.rows = numRows;
		return grid;
	}

	public static SquareGrid copy( SquareGrid orig ) {
		SquareGrid ret = new SquareGrid();
		ret.nodes.addAll(orig.nodes);
		ret.rows = orig.rows;
		ret.columns = orig.columns;
		return ret;
	}
}
