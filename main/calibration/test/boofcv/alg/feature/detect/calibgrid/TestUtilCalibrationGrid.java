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

package boofcv.alg.feature.detect.calibgrid;

import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.feature.detect.calibgrid.TestPutTargetSquaresIntoOrder.createBlob;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestUtilCalibrationGrid {
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
		fail("implement");
	}

	@Test
	public void distanceCircle() {
		fail("implement");
	}
}
