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

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSquareGridTools {
	@Test
	public void computeSize() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
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

		List<Point2D_F64> list = new ArrayList<Point2D_F64>();

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
		List<SquareNode> tmp = new ArrayList<SquareNode>();

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
	public void boundingPolygon_rect() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(2,3);

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		alg.boundingPolygon(grid, poly);

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
		assertTrue(poly.get(0).distance(- w / 2, -w / 2) <= 1e-8);
		assertTrue(poly.get(1).distance( w*4 + w / 2 , - w / 2) <= 1e-8);
		assertTrue(poly.get(2).distance( w*4 + w / 2 , w*2 + w / 2) <= 1e-8);
		assertTrue(poly.get(3).distance(-w/2, w*2 + w/2) <= 1e-8);
	}

	@Test
	public void boundingPolygon_column() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(3,1);

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		alg.boundingPolygon(grid, poly);

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
		assertTrue(poly.get(0).distance(- w/2,  - w/2) <= 1e-8);
		assertTrue(poly.get(1).distance(  w/2 , - w/2) <= 1e-8);
		assertTrue(poly.get(2).distance(  w/2 , w*4 + w / 2) <= 1e-8);
		assertTrue(poly.get(3).distance(- w/2 , w*4 + w / 2) <= 1e-8);
	}

		@Test
	public void boundingPolygon_row() {
		SquareGridTools alg = new SquareGridTools();

		SquareGrid grid = createGrid(1,3);

		Polygon2D_F64 poly = new Polygon2D_F64(4);
		alg.boundingPolygon(grid, poly);

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
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

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
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

	@Test
	public void sortCorners() {
		fail("implement");

	}

	@Test
	public void selectAxis() {
		fail("implement");
	}

	public static SquareGrid createGrid( int numRows , int numCols ) {
		SquareGrid grid = new SquareGrid();
		grid.nodes =  TestClustersIntoGrids.createGrid(numRows,numCols);
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
