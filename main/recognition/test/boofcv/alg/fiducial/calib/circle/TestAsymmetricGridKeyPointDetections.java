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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.AsymmetricGridKeyPointDetections.Tangents;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.Grid;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAsymmetricGridKeyPointDetections {
	@Test
	public void all() {
		all(3,3);
		all(4,4);
		all(4,5);
		all(5,4);
		all(5,5);
	}

	public void all( int numRows , int numCols ) {
		AsymmetricGridKeyPointDetections alg = new AsymmetricGridKeyPointDetections();
		double r = 0.5;
		double s = 2.0;
		Grid grid = createGrid(numRows,numCols,s,r);

		alg.process(grid);

		int idx = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				if( row%2 == 0 && col%2 == 1)
					continue;
				if( row%2 == 1 && col%2 == 0)
					continue;

				Point2D_F64 p = alg.getKeyPoints().get(idx++);

				assertEquals(col*s, p.x, GrlConstants.DOUBLE_TEST_TOL);
				assertEquals(row*s, p.y, GrlConstants.DOUBLE_TEST_TOL);
			}
		}
	}

	@Test
	public void horizontal() {
		horizontal(4,5);
		horizontal(5,5);
		horizontal(5,4);
	}

	private void horizontal( int numRows , int numCols ) {
		AsymmetricGridKeyPointDetections alg = new AsymmetricGridKeyPointDetections();

		double r = 0.5;
		double s = 2.0;
		Grid grid = createGrid(numRows,numCols,s,r);
		alg.init(grid);
		alg.horizontal(grid);

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				int index = grid.getIndexOfEllipse(row,col);

				if( row%2==0 && col%2==0) {
					if( col == 0 || col == grid.columns-1-(1-grid.columns%2))
						assertEquals(2,alg.tangents.get(index).size);
					else
						assertEquals(row+" "+col,4,alg.tangents.get(index).size);
				} else if( row%2 == 1 && col%2 == 1 ) {
					if( col == 1 || col == grid.columns-1-(grid.columns%2))
						assertEquals(2,alg.tangents.get(index).size);
					else
						assertEquals(4,alg.tangents.get(index).size);
				}
			}
		}
	}

	@Test
	public void vertical() {
		vertical(4,5);
		vertical(5,5);
		vertical(5,4);
	}

	private void vertical( int numRows , int numCols ) {
		AsymmetricGridKeyPointDetections alg = new AsymmetricGridKeyPointDetections();

		double r = 0.5;
		double s = 2.0;
		Grid grid = createGrid(numRows,numCols,s,r);
		alg.init(grid);
		alg.vertical(grid);

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				int index = grid.getIndexOfEllipse(row,col);

				if( row%2==0 && col%2==0) {
					if( row == 0 || row == grid.rows-1-(1-grid.rows%2))
						assertEquals(2,alg.tangents.get(index).size);
					else
						assertEquals(4,alg.tangents.get(index).size);
				} else if( row%2 == 1 && col%2 == 1 ) {
					if( row == 1 || row == grid.rows-1-(grid.rows%2))
						assertEquals(2,alg.tangents.get(index).size);
					else
						assertEquals(4,alg.tangents.get(index).size);
				}
			}
		}
	}

	@Test
	public void diagonalLR() {
		diagonalLR(4,5);
		diagonalLR(5,5);
		diagonalLR(5,4);
	}

	private void diagonalLR( int numRows , int numCols ) {
		AsymmetricGridKeyPointDetections alg = new AsymmetricGridKeyPointDetections();

		double r = 0.5;
		double s = 2.0;
		Grid grid = createGrid(numRows,numCols,s,r);
		alg.init(grid);
		alg.diagonalLR(grid);

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				if( row%2 == 0 && col%2 == 1)
					continue;
				if( row%2 == 1 && col%2 == 0)
					continue;

				int index = grid.getIndexOfEllipse(row,col);

				int expectedNumber = 0;

				if( col >= 1 && row >= 1 )
					expectedNumber += 2;
				if( col < grid.columns-1 && row < grid.rows-1 )
					expectedNumber += 2;

				assertEquals(expectedNumber,alg.tangents.get(index).size);
			}
		}
	}

	@Test
	public void diagonalRL() {
		diagonalRL(4,5);
		diagonalRL(5,5);
		diagonalRL(5,4);
	}

	private void diagonalRL( int numRows , int numCols ) {
		AsymmetricGridKeyPointDetections alg = new AsymmetricGridKeyPointDetections();

		double r = 0.5;
		double s = 2.0;
		Grid grid = createGrid(numRows,numCols,s,r);
		alg.init(grid);
		alg.diagonalRL(grid);

		for (int row = 0; row < grid.rows; row++) {
			for (int col = 0; col < grid.columns; col++) {
				if( row%2 == 0 && col%2 == 1)
					continue;
				if( row%2 == 1 && col%2 == 0)
					continue;

				int index = grid.getIndexOfEllipse(row,col);

				int expectedNumber = 0;

				if( col >= 1 && row < grid.rows-1 )
					expectedNumber += 2;
				if( col < grid.columns-1 && row >= 1 )
					expectedNumber += 2;

				assertEquals(expectedNumber,alg.tangents.get(index).size);
			}
		}
	}

	@Test
	public void computeEllipseCenters() {
		AsymmetricGridKeyPointDetections alg = new AsymmetricGridKeyPointDetections();

		Tangents t = alg.tangents.grow();

		double r = 2.0;
		double cx = 2.5;
		double cy = 3.6;

		// create line segments from pairs of points along the diameter of a circle
		for (int i = 0; i < 10; i++) {
			double theta = Math.PI*i/10.0;
			double c = Math.cos(theta);
			double s = Math.sin(theta);

			t.grow().set( r*c - r*s + cx,  r*s + r*c + cy);
			t.grow().set(-r*c + r*s + cx, -r*s - r*c + cy);
		}

		alg.computeEllipseCenters();

		assertEquals(1,alg.keypoints.size);
		assertEquals(cx,alg.keypoints.get(0).x, GrlConstants.DOUBLE_TEST_TOL);
		assertEquals(cy,alg.keypoints.get(0).y, GrlConstants.DOUBLE_TEST_TOL);
	}

	public static Grid createGrid(int numRows , int numCols , double space , double radius ) {
		Grid grid = new Grid();
		grid.rows = numRows;
		grid.columns = numCols;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				if( row%2==0 && col%2 == 1)
					grid.ellipses.add(null);
				else if( row%2==1 && col%2 == 0)
					grid.ellipses.add(null);
				else {
					double x = col * space;
					double y = row * space;
					grid.ellipses.add(new EllipseRotated_F64(x,y,radius,radius,0));
				}
			}
		}

		return grid;
	}
}