/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilCalibrationGrid {

	/**
	 * Create a grid, then rotate and see if the points are expected
	 */
	@Test
	public void rotatePoints() {
		int numCols = 3;
		int numRows = 4;

		List<Point2D_F64> original = new ArrayList<Point2D_F64>();

		for( int i = 0; i < numRows; i++ ) {
			for( int j = 0; j < numCols; j++ ) {
				original.add(new Point2D_F64(j, i));
			}
		}

		List<Point2D_F64> rotated = UtilCalibrationGrid.rotatePoints(original,numRows,numCols);

		for( int i = 0; i < numCols; i++ ) {
			for( int j = 0; j < numRows; j++ ) {
				int index = j*numCols + (numCols-i-1);

				assertTrue(original.get(index) == rotated.get(i*numRows+j));
			}
		}
	}
	
	@Test
	public void findAverage() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		list.add( new Point2D_I32(1,2));
		list.add( new Point2D_I32(5,3));
		list.add( new Point2D_I32(2,8));

		Point2D_I32 c = UtilCalibrationGrid.findAverage(list);

		assertEquals(2,c.x);
		assertEquals(4, c.y);
	}

	@Test
	public void sortByAngle() {
		List<Point2D_F64> list = new ArrayList<Point2D_F64>();

		for( int i = 0; i < 10; i++ )
			list.add( new Point2D_F64(i,0));

		Point2D_F64 c = new Point2D_F64(5,-10);

		// randomize the order
		Collections.shuffle(list, new Random(8234));

		// sanity check
		assertTrue(0 != list.get(0).x);

		UtilCalibrationGrid.sortByAngleCCW(c, list);

		for( int i = 0; i < 10; i++ )
			assertEquals(9-i, list.get(i).x,1e-8);
	}

	@Test
	public void findFarthest() {
		List<Point2D_F64> list = new ArrayList<Point2D_F64>();
		list.add( new Point2D_F64(1,2));
		list.add( new Point2D_F64(2,23));
		list.add( new Point2D_F64(8,8));

		int index = UtilCalibrationGrid.findFarthest(new Point2D_F64(1,3),list);

		assertEquals(1, index);
	}

	@Test
	public void incrementCircle() {
		assertEquals(1, UtilCalibrationGrid.incrementCircle(0, 1, 8));
		assertEquals(7, UtilCalibrationGrid.incrementCircle(6, 1, 8));
		assertEquals(0, UtilCalibrationGrid.incrementCircle(7, 1, 8));
		assertEquals(7, UtilCalibrationGrid.incrementCircle(0, -1, 8));
		assertEquals(6, UtilCalibrationGrid.incrementCircle(7, -1, 8));
		assertEquals(0, UtilCalibrationGrid.incrementCircle(1, -1, 8));
	}

	@Test
	public void distanceCircle_dir() {
		assertEquals(0, UtilCalibrationGrid.distanceCircle(0, 0, 1 , 8));
		assertEquals(1, UtilCalibrationGrid.distanceCircle(0, 1, 1, 8));
		assertEquals(2, UtilCalibrationGrid.distanceCircle(0, 2, 1 , 8));
		assertEquals(0, UtilCalibrationGrid.distanceCircle(6, 6, 1 , 8));
		assertEquals(1, UtilCalibrationGrid.distanceCircle(6, 7, 1 , 8));
		assertEquals(2, UtilCalibrationGrid.distanceCircle(6, 0, 1 , 8));
		assertEquals(7, UtilCalibrationGrid.distanceCircle(6, 5, 1 , 8));

		assertEquals(0, UtilCalibrationGrid.distanceCircle(6, 6, -1 , 8));
		assertEquals(7, UtilCalibrationGrid.distanceCircle(6, 7, -1 , 8));
		assertEquals(6, UtilCalibrationGrid.distanceCircle(6, 0, -1 , 8));
		assertEquals(1, UtilCalibrationGrid.distanceCircle(6, 5, -1 , 8));
	}

	@Test
	public void distanceCircle() {
		assertEquals(0, UtilCalibrationGrid.distanceCircle(6, 6, 8));
		assertEquals(1, UtilCalibrationGrid.distanceCircle(6, 7, 8));
		assertEquals(2, UtilCalibrationGrid.distanceCircle(6, 0, 8));
		assertEquals(1, UtilCalibrationGrid.distanceCircle(6, 5, 8));
	}
}
