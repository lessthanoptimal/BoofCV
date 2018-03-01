/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.shapes.polyline;

import boofcv.alg.shapes.polyline.splitmerge.TestPolylineSplitMerge;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.List;

import static boofcv.alg.shapes.polyline.splitmerge.TestPolylineSplitMerge.line;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class ChecksGenericPointsToPolyline {

	public abstract PointsToPolyline createAlg( boolean loop );

	/**
	 * Makes sure that the default values as specified in JavaDoc is true
	 */
	@Test
	public void checkDefaults() {
		PointsToPolyline alg = createAlg(true);

		assertEquals(3,alg.getMinimumSides());
		assertEquals(Integer.MAX_VALUE,alg.getMaximumSides());
		assertEquals(true,alg.isConvex());
		assertEquals(true,alg.isLoop());

		alg = createAlg(false);

		assertEquals(3,alg.getMinimumSides());
		assertEquals(Integer.MAX_VALUE,alg.getMaximumSides());
		assertEquals(true,alg.isConvex());
		assertEquals(false,alg.isLoop());
	}

	/**
	 * Checks to see if this feature can be changed and is enforced
	 */
	@Test
	public void checkMinVertexes_loop() {
		PointsToPolyline alg = createAlg(true);
		alg.setConvex(false);
		alg.setMinimumSides(10);

		List<Point2D_I32> contour = line(0,0,30,0);
		contour.addAll(line(30,0,30,10));
		contour.addAll(line(30,10,20,10));
		contour.addAll(line(20,10,20,30));
		contour.addAll(line(20,30,0,30));
		contour.addAll(line(0,30,0,0));

		GrowQueue_I32 found = new GrowQueue_I32();
		if( alg.process(contour,found)) {
			assertEquals(10, found.size);
		}
		alg.setMinimumSides(3);
		assertTrue(alg.process(contour,found));

		check(found,0,30,40,50,70,90);
	}

	/**
	 * Checks to see if this feature can be changed and is enforced
	 */
	@Test
	public void checkMinVertexes_sequence() {
		PointsToPolyline alg = createAlg(false);
		alg.setConvex(false);
		alg.setMinimumSides(10);

		List<Point2D_I32> contour = line(0,0,30,0);
		contour.addAll(line(30,0,30,10));
		contour.addAll(line(30,10,20,10));
		contour.addAll(line(20,10,20,30));
		contour.addAll(line(20,30,0,30));

		GrowQueue_I32 found = new GrowQueue_I32();
		if( alg.process(contour,found)) {
			assertEquals(9, found.size);
		}
		alg.setMinimumSides(3);
		assertTrue(alg.process(contour,found));

		check(found,0,30,40,50,70,89);
	}

	/**
	 * Checks to see if this feature can be changed and is enforced
	 */
	@Test
	public void checkMaxVertexes_loop() {
		PointsToPolyline alg = createAlg(true);
		alg.setMaximumSides(3);

		List<Point2D_I32> contour = TestPolylineSplitMerge.rect(0,0,10,20);
		GrowQueue_I32 found = new GrowQueue_I32();

		// will fail because the error is too large for 3 sides
		assertFalse(alg.process(contour,found));

		alg.setMaximumSides(4);
		assertTrue(alg.process(contour,found));
		check(found,0,10,30,40);
	}

	/**
	 * Checks to see if this feature can be changed and is enforced
	 */
	@Test
	public void checkIsConvex() {
		PointsToPolyline alg = createAlg(true);
		alg.setConvex(true);

		List<Point2D_I32> contour = line(0,0,30,0);
		contour.addAll(line(30,0,30,10));
		contour.addAll(line(30,10,20,10));
		contour.addAll(line(20,10,20,30));
		contour.addAll(line(20,30,0,30));
		contour.addAll(line(0,30,0,0));

		GrowQueue_I32 found = new GrowQueue_I32();
		assertFalse(alg.process(contour,found));

		alg.setConvex(false);
		assertTrue(alg.process(contour,found));
	}


	private void check( GrowQueue_I32 found , int ...expected) {
		assertEquals(expected.length,found.size());
		boolean matched[] = new boolean[expected.length];
		for (int i = 0; i < found.size(); i++) {
			int where = found.get(i);
			for (int j = 0; j < expected.length; j++) {
				if( expected[j] == where ) {
					matched[j] = true;
					break;
				}
			}
		}
		for (int i = 0; i < expected.length; i++) {
			assertTrue(matched[i]);
		}
	}
}
