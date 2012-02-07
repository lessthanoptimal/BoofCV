/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static boofcv.alg.feature.detect.grid.TestPutTargetSquaresIntoOrder.createBlob;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilCalibrationGrid {

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
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();

		for( int i = 0; i < 10; i++ )
			list.add( new Point2D_I32(i,0));

		Point2D_I32 c = new Point2D_I32(5,-10);

		// randomize the order
		Collections.shuffle(list, new Random(8234));

		// sanity check
		assertTrue(0 != list.get(0).x);

		UtilCalibrationGrid.sortByAngleCCW(c, list);

		for( int i = 0; i < 10; i++ )
			assertEquals(9-i, list.get(i).x);
	}

	@Test
	public void findFarthest() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		list.add( new Point2D_I32(1,2));
		list.add( new Point2D_I32(2,23));
		list.add( new Point2D_I32(8,8));

		int index = UtilCalibrationGrid.findFarthest(new Point2D_I32(1,3),list);

		assertEquals(1,index);
	}

	@Test
	public void extractOrderedPoints() {
		List<SquareBlob> blobs = new ArrayList<SquareBlob>();
		List<Point2D_I32> points = new ArrayList<Point2D_I32>();

		blobs.add( createBlob(5,5,10));
		blobs.add( createBlob(50,4,10));

		UtilCalibrationGrid.extractOrderedPoints(blobs,points,2);

		// add first row
		GeometryUnitTest.assertEquals(-5, 15, points.get(0));
		GeometryUnitTest.assertEquals(15,15,points.get(1));
		GeometryUnitTest.assertEquals(40,14,points.get(2));
		GeometryUnitTest.assertEquals(60,14,points.get(3));
		// add second row, and remember they are added in circular order, so some indexes are swapped
		GeometryUnitTest.assertEquals(-5,-5,points.get(4));
		GeometryUnitTest.assertEquals(15,-5,points.get(5));
	}

	@Test
	public void extractOrderedSubpixel() {
		List<SquareBlob> blobs = new ArrayList<SquareBlob>();
		List<Point2D_F32> points = new ArrayList<Point2D_F32>();

		blobs.add( createBlob(5,5,10));
		blobs.add( createBlob(50,4,10));

		UtilCalibrationGrid.extractOrderedSubpixel(blobs, points, 2);

		// add first row
		GeometryUnitTest.assertEquals(-5, 15, points.get(0),1e-8f);
		GeometryUnitTest.assertEquals(15,15,points.get(1),1e-8f);
		GeometryUnitTest.assertEquals(40,14,points.get(2),1e-8f);
		GeometryUnitTest.assertEquals(60,14,points.get(3),1e-8f);
		// add second row, and remember they are added in circular order, so some indexes are swapped
		GeometryUnitTest.assertEquals(-5,-5,points.get(4),1e-8f);
		GeometryUnitTest.assertEquals(15,-5,points.get(5),1e-8f);
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
