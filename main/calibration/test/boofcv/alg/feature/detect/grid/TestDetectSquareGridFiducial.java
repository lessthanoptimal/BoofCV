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

import boofcv.alg.feature.detect.squares.SquareGrid;
import boofcv.alg.feature.detect.squares.SquareNode;
import boofcv.alg.feature.detect.squares.TestClustersIntoGrids;
import boofcv.struct.image.ImageFloat32;
import georegression.metric.UtilAngle;
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
public class TestDetectSquareGridFiducial {
	@Test
	public void process() {
		// intentionally blank.  tested in the wrapper class
	}

	@Test
	public void computeSize() {
		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);

		SquareGrid grid = createGrid(2,3);

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
		double expected = (2*w*(3-1))*(2*w*(2-1)); // see how the grid is constructed

		assertEquals(expected, alg.computeSize(grid), 1e-8);
	}

	@Test
	public void resolveAmbiguity_square() {
		DetectSquareGridFiducial<ImageFloat32> alg = new DetectSquareGridFiducial<ImageFloat32>(1,1,1,null,null);

		// grids of different sizes.  smallest is 4x4 since that's one square
		for (int i = 4; i <= 8; i++) {
			alg.calibCols = i;
			alg.calibRows = i;

			// try different initial orientations
			for (int j = 0; j < 4; j++) {
				alg.calibrationPoints = createPoints(i,i);
				for (int k = 0; k < j; k++) {
					alg.rotateCalibSquareCCW();
				}
				alg.resolveAmbiguity();
				assertTrue(alg.calibrationPoints.get(0).distance(new Point2D_F64(10, 20)) < 1e-8);
			}
		}
	}

	@Test
	public void resolveAmbiguity_rectangle() {
		DetectSquareGridFiducial<ImageFloat32> alg = new DetectSquareGridFiducial<ImageFloat32>(1,1,1,null,null);

		alg.calibCols = 5;
		alg.calibRows = 2;
		alg.calibrationPoints = createPoints(alg.calibRows,alg.calibCols);
		alg.resolveAmbiguity();
		assertTrue(alg.calibrationPoints.get(0).distance(new Point2D_F64(10, 20)) < 1e-8);

		alg.reverseCalib();
		alg.resolveAmbiguity();
		assertTrue(alg.calibrationPoints.get(0).distance(new Point2D_F64(10, 20)) < 1e-8);
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
		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);

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
		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);

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
		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);

		SquareGrid grid = createGrid(2,3);
		SquareGrid orig = copy(grid);

		alg.flipRows(grid);

		for (int row = 0; row < orig.rows; row++) {
			for (int col = 0; col < orig.columns; col++) {
				assertTrue(orig.get(row,col)==grid.get(orig.rows-row-1,col));
			}
		}
	}

	@Test
	public void extractCalibrationPoints() {
		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);

		SquareGrid grid = createGrid(2,3);

		assertTrue(alg.extractCalibrationPoints(grid));

		assertEquals(4, alg.getCalibrationRows());
		assertEquals(6, alg.getCalibrationCols());

		List<Point2D_F64> pts = alg.getCalibrationPoints();

		assertEquals(4*6,pts.size());

		double w = TestClustersIntoGrids.DEFAULT_WIDTH;
		double x0 = -w/2;
		double y0 = -w/2;

		int index = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 6; col++, index++ ) {
				double x = x0 + col*w;
				double y = y0 + row*w;

				assertEquals("x index "+index,x,pts.get(index).x,1e-8);
				assertEquals("y index "+index,y,pts.get(index).y,1e-8);
			}
		}

	}

	@Test
	public void sortCorners() {
		Polygon2D_F64 poly = new Polygon2D_F64(-1,-1,1,-1,1,1,-1,1);

		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);
		alg.axisX.slope.set(1, 0);
		alg.axisY.slope.set(0, 1);

		alg.sortCorners(poly);
		checkOrder(alg,poly, 0,1,2,3);

		alg.axisX.slope.set( 0,1);
		alg.axisY.slope.set(-1,0);
		alg.sortCorners(poly);
		checkOrder(alg, poly, 1, 2, 3, 0);

		alg.axisX.slope.set( -1, 0);
		alg.axisY.slope.set(  0,-1);
		alg.sortCorners(poly);
		checkOrder(alg,poly, 2,3,0,1);

	}

	private void checkOrder( DetectSquareGridFiducial alg , Polygon2D_F64 poly , int ...order) {
		for (int i = 0; i < 4; i++) {
			assertTrue(""+i,alg.sorted[i] == poly.get(order[i]));
		}
	}

	@Test
	public void selectAxis() {
		DetectSquareGridFiducial alg = new DetectSquareGridFiducial(1,1,1,null,null);

		for (int numRows = 1; numRows <= 3; numRows++) {
			for (int numCols = 1; numCols <= 3; numCols++) {
				SquareGrid grid = createGrid(numRows,numCols);

				for (int i = 0; i < numRows; i++) {
					for (int j = 0; j < numCols; j++) {
						selectAxis(alg, grid, i, j);
					}
				}
			}
		}
	}

	// select and axis and check its properties
	protected void selectAxis( DetectSquareGridFiducial alg,
							   SquareGrid grid , int row , int col ) {
		alg.selectAxis(grid, row, col);

		SquareNode n = grid.get(row,col);
		assertEquals(0,alg.axisX.p.distance2(n.center),1e-8);
		assertEquals(0,alg.axisY.p.distance2(n.center),1e-8);

		double angleX = Math.atan2(alg.axisX.slope.y,alg.axisX.slope.x);
		double angleY = Math.atan2(alg.axisY.slope.y,alg.axisY.slope.x);

		assertEquals(Math.PI/2,UtilAngle.distanceCCW(angleX, angleY));

		// compare to the first square, should be very similar
		alg.selectAxis(grid, 0, 0);
		double angleX0 = Math.atan2(alg.axisX.slope.y,alg.axisX.slope.x);

		assertEquals(angleX0, angleX, 1e-6);
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
		ret.nodes = new ArrayList<SquareNode>();
		ret.nodes.addAll(orig.nodes);
		ret.rows = orig.rows;
		ret.columns = orig.columns;
		return ret;
	}
}
