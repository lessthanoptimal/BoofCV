/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.KeyPointsCircleRegularGrid.Tangents;
import boofcv.struct.BoofDefaults;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestKeyPointsCircleRegularGrid {
	@Test
	public void all() {
		int numRows=3,numCols=4;

		double space = 3,r=1;

		Grid g = createGrid(numRows,numCols,space,r);

		KeyPointsCircleRegularGrid alg = new KeyPointsCircleRegularGrid();

		alg.process(g);

		int index = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				double cx = col * space;
				double cy = row * space;

				Point2D_F64 a = alg.getKeyPoints().get(index++);
				Point2D_F64 b = alg.getKeyPoints().get(index++);
				Point2D_F64 c = alg.getKeyPoints().get(index++);
				Point2D_F64 d = alg.getKeyPoints().get(index++);

				assertTrue(a.distance(cx,cy+r) <= BoofDefaults.TEST_DOUBLE_TOL);
				assertTrue(b.distance(cx+r,cy) <= BoofDefaults.TEST_DOUBLE_TOL);
				assertTrue(c.distance(cx,cy-r) <= BoofDefaults.TEST_DOUBLE_TOL);
				assertTrue(d.distance(cx-r,cy) <= BoofDefaults.TEST_DOUBLE_TOL);
			}
		}
	}

	@Test
	public void horizontal() {
		int numRows=3,numCols=4;

		double space = 3,r=1;

		Grid g = createGrid(numRows,numCols,space,r);

		KeyPointsCircleRegularGrid alg = new KeyPointsCircleRegularGrid();

		alg.init(g);
		assertTrue(alg.horizontal(g));
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {

				Tangents t = alg.getTangents().get(row*numCols+col);

				if( col == 0 || col == numCols-1) {
					assertEquals(1,t.countL);
					assertEquals(1,t.countR);
				} else {
					assertEquals(2,t.countL);
					assertEquals(2,t.countR);
				}
			}
		}
	}

	@Test
	public void vertical() {
		int numRows=3,numCols=4;

		double space = 3,r=1;

		Grid g = createGrid(numRows,numCols,space,r);

		KeyPointsCircleRegularGrid alg = new KeyPointsCircleRegularGrid();

		alg.init(g);
		assertTrue(alg.vertical(g));
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {

				Tangents t = alg.getTangents().get(row*numCols+col);

				if( row == 0 || row == numRows-1) {
					assertEquals(1,t.countT);
					assertEquals(1,t.countB);
				} else {
					assertEquals(2,t.countT);
					assertEquals(2,t.countB);
				}
			}
		}
	}

	public static Grid createGrid(int numRows , int numCols , double space , double radius ) {
		Grid grid = new Grid();
		grid.rows = numRows;
		grid.columns = numCols;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				double x = col * space;
				double y = row * space;
				grid.ellipses.add(new EllipseRotated_F64(x,y,radius,radius,0));
			}
		}

		return grid;
	}
}