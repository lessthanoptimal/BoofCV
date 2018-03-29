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
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestKeyPointsCircleRegularGrid {
	@Test
	public void all() {

		all(new Affine2D_F64());

		for (int i = 1; i < 10; i++) {
//			System.out.println("i = "+i);
			double yaw = 2.0*Math.PI*i/10.0;
			Affine2D_F64 rot = ConvertTransform_F64.convert(new Se2_F64(0,0,yaw),(Affine2D_F64)null);
			all(rot);
		}
	}

	public void all(Affine2D_F64 affine ) {
		int numRows=3,numCols=4;

		double space = 3,r=1;

		Grid g = createGrid(numRows,numCols,space,r);
		transform(affine,g);

		KeyPointsCircleRegularGrid alg = new KeyPointsCircleRegularGrid();

		alg.process(g);

		Point2D_F64 p = new Point2D_F64();
		int index = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				double cx = col * space;
				double cy = row * space;

				Point2D_F64 a = alg.getKeyPoints().get(index++);
				Point2D_F64 b = alg.getKeyPoints().get(index++);
				Point2D_F64 c = alg.getKeyPoints().get(index++);
				Point2D_F64 d = alg.getKeyPoints().get(index++);

				AffinePointOps_F64.transform(affine,cx,cy+r,p);
				assertTrue(a.distance(p.x,p.y) <= BoofDefaults.TEST_DOUBLE_TOL);
				AffinePointOps_F64.transform(affine,cx+r,cy,p);
				assertTrue(b.distance(p.x,p.y) <= BoofDefaults.TEST_DOUBLE_TOL);
				AffinePointOps_F64.transform(affine,cx,cy-r,p);
				assertTrue(c.distance(p.x,p.y) <= BoofDefaults.TEST_DOUBLE_TOL);
				AffinePointOps_F64.transform(affine,cx-r,cy,p);
				assertTrue(d.distance(p.x,p.y) <= BoofDefaults.TEST_DOUBLE_TOL);
			}
		}
	}

	public void transform(Affine2D_F64 affine , Grid g ) {
		for( EllipseRotated_F64 e : g.ellipses ) {
			AffinePointOps_F64.transform(affine,e.center.x,e.center.y,e.center);
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
					assertEquals(1,t.countT);
					assertEquals(1,t.countB);
				} else {
					assertEquals(2,t.countT);
					assertEquals(2,t.countB);
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
					assertEquals(1,t.countL);
					assertEquals(1,t.countR);
				} else {
					assertEquals(2,t.countL);
					assertEquals(2,t.countR);
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